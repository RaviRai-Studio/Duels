package com.meteordevelopments.duels.teleport;

import com.meteordevelopments.duels.DuelsPlugin;
import com.meteordevelopments.duels.hook.hooks.EssentialsHook;
import com.meteordevelopments.duels.util.Loadable;
import com.meteordevelopments.duels.util.Log;
import com.meteordevelopments.duels.util.metadata.MetadataUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class Teleport implements Loadable, Listener {

    public static final String METADATA_KEY = "Duels-Teleport";

    private final DuelsPlugin plugin;

    private EssentialsHook essentials;

    public Teleport(final DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleLoad() {
        this.essentials = plugin.getHookManager().getHook(EssentialsHook.class);

        plugin.doSyncAfter(() -> plugin.registerListener(this), 1L);
    }

    @Override
    public void handleUnload() {
    }

    public void tryTeleport(final Player player, final Location location) {
        if (location == null || location.getWorld() == null) {
            Log.warn(this, "Could not teleport " + player.getName() + "! Location is null");
            return;
        }

        if (!player.isOnline()) {
            Log.warn(this, "Could not teleport " + player.getName() + "! Player is not online");
            return;
        }

        for (Entity entity : player.getPassengers()) {
            player.removePassenger(entity);
        }

        player.closeInventory();

        if (essentials != null) {
            essentials.setBackLocation(player, location);
        }

        MetadataUtil.put(plugin, player, METADATA_KEY, location.clone());

        boolean isFolia = DuelsPlugin.getMorePaperLib().scheduling().isUsingFolia();

        if (isFolia) {
            try {
                player.teleportAsync(location).thenAccept(success -> {
                    if (!success) {
                        Log.warn(this, "Could not teleport " + player.getName() + "! TeleportAsync failed.");
                        fallbackTeleport(player, location);
                    }
                }).exceptionally(throwable -> {
                    Log.warn(this, "TeleportAsync error for " + player.getName() + ": " + throwable.getMessage());
                    fallbackTeleport(player, location);
                    return null;
                });
            } catch (Exception e) {
                Log.warn(this, "Exception during async teleport for " + player.getName() + ": " + e.getMessage());
                fallbackTeleport(player, location);
            }
        } else {
            if (!player.teleport(location)) {
                Log.warn(this, "Could not teleport " + player.getName() + "! Player is dead or is vehicle");
                fallbackTeleport(player, location);
            }

        if (!player.teleport(location)) {
            Log.warn(this, "Could not teleport " + player.getName() + "! Player is dead or is vehicle");
        }
    }

    private void fallbackTeleport(final Player player, final Location location) {
        plugin.doSyncAfter(() -> {
            if (player.isOnline()) {
                try {
                    if (!player.teleport(location)) {
                        Log.warn(this, "Fallback teleport failed for " + player.getName());
                    }
                } catch (Exception e) {
                    Log.warn(this, "Exception during fallback teleport for " + player.getName() + ": " + e.getMessage());
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        final Object value = MetadataUtil.removeAndGet(plugin, player, METADATA_KEY);

        if (!event.isCancelled() || value == null) {
            return;
        }

        event.setCancelled(false);
        event.setTo((Location) value);
    }
}