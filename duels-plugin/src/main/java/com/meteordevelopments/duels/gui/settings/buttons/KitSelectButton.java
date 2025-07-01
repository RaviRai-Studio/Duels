package com.meteordevelopments.duels.gui.settings.buttons;

import com.meteordevelopments.duels.DuelsPlugin;
import com.meteordevelopments.duels.Permissions;
import com.meteordevelopments.duels.gui.BaseButton;
import com.meteordevelopments.duels.setting.Settings;
import com.meteordevelopments.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class KitSelectButton extends BaseButton {

    public KitSelectButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Material.DIAMOND_SWORD).name(plugin.getLang().getMessage("GUI.settings.buttons.kit-selector.name")).build());
    }

    @Override
    public void update(final Player player) {
        if (config.isKitSelectingUsePermission() && !player.hasPermission(Permissions.KIT_SELECTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            setLore(lang.getMessage("GUI.settings.buttons.kit-selector.lore-no-permission").split("\n"));
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        final String kit = settings.getKit() != null ? settings.getKit().getName() : lang.getMessage("GENERAL.not-selected");
        final String lore = lang.getMessage("GUI.settings.buttons.kit-selector.lore", "kit", kit);
        setLore(lore.split("\n"));
    }

    @Override
    public void onClick(final Player player) {
        if (config.isKitSelectingUsePermission() && !player.hasPermission(Permissions.KIT_SELECTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            lang.sendMessage(player, "ERROR.no-permission", "permission", Permissions.KIT_SELECTING);
            return;
        }

        final Settings settings = settingManager.getSafely(player);

        if (config.isSkipRequestSettings()) {
            if (settings.getTarget() == null) {
                settings.reset();
                player.closeInventory();
                return;
            }

            final Player target = Bukkit.getPlayer(settings.getTarget());

            if (target == null) {
                settings.reset();
                player.closeInventory();
                lang.sendMessage(player, "ERROR.player.no-longer-online");
                return;
            }

            if (!settings.isOwnInventory() && settings.getKit() == null) {
                player.closeInventory();
                lang.sendMessage(player, "ERROR.duel.mode-unselected");
                return;
            }

            player.closeInventory();
            requestManager.send(player, target, settings);
        } else {
            kitManager.getGui().open(player);
        }
    }
}