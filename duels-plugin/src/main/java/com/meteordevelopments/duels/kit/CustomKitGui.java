package com.meteordevelopments.duels.kit;

import com.meteordevelopments.duels.DuelsPlugin;
import com.meteordevelopments.duels.util.StringUtil;
import com.meteordevelopments.duels.util.gui.AbstractGui;
import com.meteordevelopments.duels.util.inventory.InventoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class CustomKitGui extends AbstractGui<DuelsPlugin> {
  private final String title;
  private final Collection<KitImpl> kits;
  private final int[] kitSlots = {11, 12, 13, 14, 15};
  private Inventory inventory;
  private ItemStack glassPane;

  public CustomKitGui(DuelsPlugin plugin, String title, int rows, Collection<KitImpl> kits) {
    super(plugin);
    this.title = title;
    this.kits = kits;
    this.inventory = InventoryBuilder.of(StringUtil.color(title), rows * 9).build();
  }

  public void setStaticItem(int slot, ItemStack item) {
    if (this.glassPane == null) {
      this.glassPane = item;
    }
    if (inventory != null) {
      inventory.setItem(slot, item);
    }
  }

  public void calculatePages() {
    setupLayout();
    updateKitItems();
  }

  private void setupLayout() {
    if (glassPane == null) return;

    for (int i = 0; i < 9; i++) {
      inventory.setItem(i, glassPane);
    }

    inventory.setItem(9, glassPane);
    inventory.setItem(10, glassPane);
    inventory.setItem(16, glassPane);
    inventory.setItem(17, glassPane);

    for (int i = 18; i < 27; i++) {
      inventory.setItem(i, glassPane);
    }
  }

  private void updateKitItems() {
    KitImpl[] kitArray = kits.toArray(new KitImpl[0]);

    for (int i = 0; i < kitSlots.length; i++) {
      if (i < kitArray.length) {
        KitImpl kit = kitArray[i];
        set(inventory, kitSlots[i], kit);
      } else {
        inventory.setItem(kitSlots[i], null);
      }
    }
  }

  @Override
  public void open(Player... players) {
    calculatePages();
    for (Player player : players) {
      update(player);
      player.openInventory(inventory);
    }
  }

  @Override
  public boolean isPart(Inventory inventory) {
    return this.inventory.equals(inventory);
  }

  @Override
  public void on(Player player, Inventory top, InventoryClickEvent event) {
    event.setCancelled(true);

    int slot = event.getSlot();
    for (int kitSlot : kitSlots) {
      if (slot == kitSlot) {
        KitImpl kit = getKitAtSlot(slot);
        if (kit != null) {
          kit.onClick(player);
        }
        return;
      }
    }
  }

  private KitImpl getKitAtSlot(int slot) {
    KitImpl[] kitArray = kits.toArray(new KitImpl[0]);

    for (int i = 0; i < kitSlots.length; i++) {
      if (kitSlots[i] == slot && i < kitArray.length) {
        return kitArray[i];
      }
    }
    return null;
  }
}