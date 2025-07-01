package com.meteordevelopments.duels.queue;

import com.meteordevelopments.duels.DuelsPlugin;
import com.meteordevelopments.duels.util.StringUtil;
import com.meteordevelopments.duels.util.gui.AbstractGui;
import com.meteordevelopments.duels.util.inventory.InventoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomQueueGui extends AbstractGui<DuelsPlugin> {
  private final String title;
  private final List<Queue> queues;
  private final int[] queueSlots = {11, 12, 13, 14, 15};
  private Inventory inventory;
  private ItemStack glassPane;

  public CustomQueueGui(DuelsPlugin plugin, String title, int rows, List<Queue> queues) {
    super(plugin);
    this.title = title;
    this.queues = queues;
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
    updateQueueItems();
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

  private void updateQueueItems() {
    for (int i = 0; i < queueSlots.length; i++) {
      if (i < queues.size()) {
        Queue queue = queues.get(i);
        set(inventory, queueSlots[i], queue);
      } else {
        inventory.setItem(queueSlots[i], null);
      }
    }
  }

  @Override
  public void open(Player... players) {
    calculatePages();
    for (Player player : players) {
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
    for (int queueSlot : queueSlots) {
      if (slot == queueSlot) {
        Queue queue = getQueueAtSlot(slot);
        if (queue != null) {
          queue.onClick(player);
        }
        return;
      }
    }
  }

  private Queue getQueueAtSlot(int slot) {
    for (int i = 0; i < queueSlots.length; i++) {
      if (queueSlots[i] == slot && i < queues.size()) {
        return queues.get(i);
      }
    }
    return null;
  }
}