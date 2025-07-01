package com.meteordevelopments.duels.player;

import com.google.common.base.Charsets;
import lombok.Getter;
import com.meteordevelopments.duels.DuelsPlugin;
import com.meteordevelopments.duels.config.Config;
import com.meteordevelopments.duels.data.LocationData;
import com.meteordevelopments.duels.data.PlayerData;
import com.meteordevelopments.duels.hook.hooks.EssentialsHook;
import com.meteordevelopments.duels.teleport.Teleport;
import com.meteordevelopments.duels.util.Loadable;
import com.meteordevelopments.duels.util.Log;
import com.meteordevelopments.duels.util.PlayerUtil;
import com.meteordevelopments.duels.util.io.FileUtil;
import com.meteordevelopments.duels.util.json.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerInfoManager implements Loadable {

    private static final String CACHE_FILE_NAME = "player-cache.yml";
    private static final String LOBBY_FILE_NAME = "lobby.json";

    private static final String ERROR_LOBBY_LOAD = "Could not load lobby location!";
    private static final String ERROR_LOBBY_SAVE = "Could not save lobby location!";
    private static final String ERROR_LOBBY_DEFAULT = "Lobby location was not set, using %s's spawn location as default. Use the command /duels setlobby in-game to set the lobby location.";

    private final DuelsPlugin plugin;
    private final Config config;
    private final File cacheFile;
    private final File lobbyFile;

    private final Map<UUID, PlayerInfo> cache = new HashMap<>();

    private Teleport teleport;
    private EssentialsHook essentials;

    @Getter
    private Location lobby;

    public PlayerInfoManager(final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.cacheFile = new File(plugin.getDataFolder(), CACHE_FILE_NAME);
        this.lobbyFile = new File(plugin.getDataFolder(), LOBBY_FILE_NAME);
        plugin.doSyncAfter(() -> Bukkit.getPluginManager().registerEvents(new PlayerInfoListener(), plugin), 1L);
    }

    @Override
    public void handleLoad() throws IOException {
        this.teleport = plugin.getTeleport();
        this.essentials = plugin.getHookManager().getHook(EssentialsHook.class);

        if (FileUtil.checkNonEmpty(cacheFile, false)) {
            try (final Reader reader = new InputStreamReader(Files.newInputStream(cacheFile.toPath()), Charsets.UTF_8)) {
                Yaml yaml = new Yaml();
                Map<UUID, PlayerData> data = yaml.load(reader);

                if (data != null) {
                    for (final Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
                        cache.put(entry.getKey(), entry.getValue().toPlayerInfo());
                    }
                }
            }

            cacheFile.delete();
        }

        if (FileUtil.checkNonEmpty(lobbyFile, false)) {
            try (final Reader reader = new InputStreamReader(Files.newInputStream(lobbyFile.toPath()), Charsets.UTF_8)) {
                this.lobby = JsonUtil.getObjectMapper().readValue(reader, LocationData.class).toLocation();
            } catch (IOException ex) {
                Log.error(this, ERROR_LOBBY_LOAD, ex);
            }
        }

        if (lobby == null || lobby.getWorld() == null) {
            final World world = Bukkit.getWorlds().get(0);
            this.lobby = world.getSpawnLocation();
            Log.warn(this, String.format(ERROR_LOBBY_DEFAULT, world.getName()));
        }
    }

    @Override
    public void handleUnload() throws IOException {
        try {
            Bukkit.getOnlinePlayers().stream().filter(Player::isDead).forEach(player -> {
                try {
                    final PlayerInfo info = remove(player);

                    if (info != null) {
                        try {
                            player.spigot().respawn();
                        } catch (Exception e) {
                            Log.error(this, "Failed to respawn player during unload: " + player.getName(), e);
                        }

                        try {
                            if (teleport != null) {
                                teleport.tryTeleport(player, info.getLocation());
                            }
                        } catch (Exception e) {
                            Log.error(this, "Failed to teleport player during unload: " + player.getName(), e);
                        }

                        try {
                            PlayerUtil.reset(player);
                        } catch (Exception e) {
                            Log.error(this, "Failed to reset player during unload: " + player.getName(), e);
                        }

                        try {
                            info.restore(player);
                        } catch (Exception e) {
                            Log.error(this, "Failed to restore player info during unload: " + player.getName(), e);
                        }
                    }
                } catch (Exception e) {
                    Log.error(this, "Error processing dead player during unload: " + player.getName(), e);
                }
            });
        } catch (Exception e) {
            Log.error(this, "Error processing dead players during unload", e);
        }

        if (cache.isEmpty()) {
            return;
        }

        try {
            Map<UUID, PlayerData> data = new HashMap<>();

            for (final Map.Entry<UUID, PlayerInfo> entry : cache.entrySet()) {
                try {
                    data.put(entry.getKey(), PlayerData.fromPlayerInfo(entry.getValue()));
                } catch (Exception e) {
                    Log.error(this, "Failed to convert PlayerInfo to PlayerData for UUID: " + entry.getKey(), e);
                }
            }

            if (!data.isEmpty()) {
                try (final Writer writer = new OutputStreamWriter(Files.newOutputStream(cacheFile.toPath()), Charsets.UTF_8)) {
                    Yaml yaml = new Yaml();
                    yaml.dump(data, writer);
                    writer.flush();
                }
            }
        } catch (Exception e) {
            Log.error(this, "Failed to save player cache during unload", e);
        } finally {
            cache.clear();
        }
    }

    public boolean setLobby(final Player player) {
        final Location lobby = player.getLocation().clone();

        try (final Writer writer = new OutputStreamWriter(Files.newOutputStream(lobbyFile.toPath()), Charsets.UTF_8)) {
            JsonUtil.getObjectWriter().writeValue(writer, LocationData.fromLocation(lobby));
            writer.flush();
            this.lobby = lobby;
            return true;
        } catch (IOException ex) {
            Log.error(this, ERROR_LOBBY_SAVE, ex);
            return false;
        }
    }

    public PlayerInfo get(final Player player) {
        return cache.get(player.getUniqueId());
    }

    public void create(final Player player, final boolean excludeInventory) {
        final PlayerInfo info = new PlayerInfo(player, excludeInventory);

        if (!config.isTeleportToLastLocation()) {
            info.setLocation(lobby.clone());
        }

        cache.put(player.getUniqueId(), info);
    }

    public void create(final Player player) {
        create(player, false);
    }

    public PlayerInfo remove(final Player player) {
        return cache.remove(player.getUniqueId());
    }

    private class PlayerInfoListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void on(final PlayerJoinEvent event) {
            final Player player = event.getPlayer();

            if (player.isDead()) {
                return;
            }

            final PlayerInfo info = remove(player);

            if (info == null) {
                return;
            }

            plugin.doSyncAfter(() -> {
                if (player.isOnline()) {
                    try {
                        teleport.tryTeleport(player, info.getLocation());
                        DuelsPlugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).runDelayed(() -> {
                            if (player.isOnline()) {
                                try {
                                    info.restore(player);
                                } catch (Exception e) {
                                    Log.error((Loadable) this, "Failed to restore player info on join: " + player.getName(), e);
                                }
                            }
                        }, null, 5L);
                    } catch (Exception e) {
                        Log.error((Loadable) this, "Failed to handle player join: " + player.getName(), e);
                    }
                }
            }, 5L);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void on(final PlayerRespawnEvent event) {
            final Player player = event.getPlayer();
            final PlayerInfo info = get(player);

            if (info == null) {
                return;
            }

            try {
                event.setRespawnLocation(info.getLocation());

                if (essentials != null) {
                    essentials.setBackLocation(player, event.getRespawnLocation());
                }

                plugin.doSyncAfter(() -> {
                    if (!player.isOnline()) {
                        return;
                    }

                    remove(player);
                    DuelsPlugin.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(() -> {
                        if (player.isOnline()) {
                            try {
                                info.restore(player);
                            } catch (Exception e) {
                                Log.error((Loadable) this, "Failed to restore player info on respawn: " + player.getName(), e);
                            }
                        }
                    }, null);
                }, 1L);
            } catch (Exception e) {
                Log.error((Loadable) this, "Failed to handle player respawn: " + player.getName(), e);
            }
        }
    }
}