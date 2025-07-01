package com.meteordevelopments.duels.api.event.kit;

import com.meteordevelopments.duels.api.kit.Kit;
import dev.ravirai.ultrakits.managers.KitManager;
import dev.ravirai.ultrakits.mcfUltraKITS;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class KitEquipEvent extends KitEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player source;
    private boolean cancelled;

    public KitEquipEvent(@NotNull final Player source, @NotNull final Kit kit) {
        super(source, kit);
        Objects.requireNonNull(source, "source");
        this.source = source;

        CompletableFuture.runAsync(() -> {
            CompletableFuture.delayedExecutor(220, TimeUnit.MILLISECONDS).execute(() -> {
                mcfUltraKITS ultraKitsPlugin = (mcfUltraKITS) Bukkit.getPluginManager().getPlugin("mcfUltraKITS");
                if (ultraKitsPlugin != null && ultraKitsPlugin.isEnabled()) {
                    KitManager.giveKitAsync(source, kit.getName(), false)
                        .exceptionally(throwable -> {
                            ultraKitsPlugin.getLogger().warning("Failed to give kit " + kit.getName() + " to player " + source.getName() + ": " + throwable.getMessage());
                            return null;
                        });
                }
            });
        });
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @NotNull
    @Override
    public Player getSource() {
        return source;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}