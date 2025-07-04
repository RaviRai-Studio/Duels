package com.meteordevelopments.duels.gui.settings;

import com.meteordevelopments.duels.DuelsPlugin;
import com.meteordevelopments.duels.config.Config;
import com.meteordevelopments.duels.gui.BaseButton;
import com.meteordevelopments.duels.gui.settings.buttons.*;
import com.meteordevelopments.duels.setting.Settings;
import com.meteordevelopments.duels.util.compat.Items;
import com.meteordevelopments.duels.util.gui.SinglePageGui;
import com.meteordevelopments.duels.util.inventory.Slots;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SettingsGui extends SinglePageGui<DuelsPlugin> {

    private static final int[][] PATTERNS = {
        {13},
        {12, 14},
        {12, 13, 14},
        {12, 13, 14, 22}
    };

    public SettingsGui(final DuelsPlugin plugin) {
        super(plugin, plugin.getLang().getMessage("GUI.settings.title"), 3);
        final Config config = plugin.getConfiguration();
        final ItemStack spacing = Items.from(config.getSettingsFillerType(), config.getSettingsFillerData());
        Slots.run(2, 7, slot -> inventory.setItem(slot, spacing));
        Slots.run(11, 16, slot -> inventory.setItem(slot, spacing));
        Slots.run(20, 25, slot -> inventory.setItem(slot, spacing));
        set(4, new RequestDetailsButton(plugin));

        final List<BaseButton> buttons = new ArrayList<>();

        if (config.isKitSelectingEnabled()) {
            buttons.add(new KitSelectButton(plugin));
        }

        if (config.isOwnInventoryEnabled()) {
            buttons.add(new OwnInventoryButton(plugin));
        }

        if (config.isArenaSelectingEnabled()) {
            buttons.add(new ArenaSelectButton(plugin));
        }

        if (config.isItemBettingEnabled()) {
            buttons.add(new ItemBettingButton(plugin));
        }

        if (!buttons.isEmpty()) {
            final int[] pattern = PATTERNS[buttons.size() - 1];

            for (int i = 0; i < buttons.size(); i++) {
                set(pattern[i], buttons.get(i));
            }
        }

        set(0, 2, 3, new RequestSendButton(plugin));
        set(7, 9, 3, new CancelButton(plugin));
    }

    public void open(final Player player) {
        final Config config = plugin.getConfiguration();

        if (config.isSkipRequestSettings()) {
            final Settings settings = plugin.getSettingManager().getSafely(player);
            final RequestSendButton sendButton = new RequestSendButton(plugin);
            sendButton.sendDirectRequest(player);
            return;
        }

        super.open(player);
    }

    public static boolean shouldSkipSettings(final DuelsPlugin plugin) {
        return plugin.getConfiguration().isSkipRequestSettings();
    }
}