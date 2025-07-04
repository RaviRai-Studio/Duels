package com.meteordevelopments.duels.util.inventory;

import com.meteordevelopments.duels.util.EnumUtil;
import com.meteordevelopments.duels.util.StringUtil;
import com.meteordevelopments.duels.util.compat.CompatUtil;
import com.meteordevelopments.duels.util.compat.Items;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Consumer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class ItemBuilder {

    private final ItemStack result;
    private static JavaPlugin plugin;

    private ItemBuilder(final Material type, final int amount, final short durability) {
        this.result = new ItemStack(type, amount);
        Items.setDurability(result, durability);
    }

    private ItemBuilder(final String type, final int amount, final short durability) {
        this(Material.matchMaterial(type), amount, durability);
    }

    private ItemBuilder(final ItemStack item) {
        this.result = item;
    }

    public static void setPlugin(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static ItemBuilder of(final Material type) {
        return of(type, 1);
    }

    public static ItemBuilder of(final Material type, final int amount) {
        return of(type, amount, (short) 0);
    }

    public static ItemBuilder of(final Material type, final int amount, final short durability) {
        return new ItemBuilder(type, amount, durability);
    }

    public static ItemBuilder of(final String type, final int amount, final short durability) {
        return new ItemBuilder(type, amount, durability);
    }

    public static ItemBuilder of(final ItemStack item) {
        return new ItemBuilder(item);
    }

    public ItemBuilder editMeta(final Consumer<ItemMeta> consumer) {
        final ItemMeta meta = result.getItemMeta();
        consumer.accept(meta);
        result.setItemMeta(meta);
        return this;
    }

    public ItemBuilder name(final String name) {
        return editMeta(meta -> meta.setDisplayName(StringUtil.color(name)));
    }

    public ItemBuilder lore(final List<String> lore) {
        return editMeta(meta -> meta.setLore(StringUtil.color(lore)));
    }

    public ItemBuilder lore(final String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder enchant(final Enchantment enchantment, final int level) {
        result.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder unbreakable() {
        return editMeta(meta -> {
            if (CompatUtil.isPre1_12()) {
                meta.setUnbreakable(true);
            } else {
                meta.setUnbreakable(true);
            }
        });
    }

    public ItemBuilder hideAttributes() {
        return editMeta(meta -> {
            try {
                AttributeModifier attackSpeedModifier = new AttributeModifier(UUID.randomUUID(), "generic.attackSpeed", 0.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
                AttributeModifier attackDamageModifier = new AttributeModifier(UUID.randomUUID(), "generic.attackDamage", 0.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeedModifier);
                meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, attackDamageModifier);
                meta.addAttributeModifier(Attribute.GENERIC_ARMOR, new AttributeModifier(UUID.randomUUID(), "generic.armor", 0.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
                meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), "generic.armorToughness", 0.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
                meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(UUID.randomUUID(), "generic.knockbackResistance", 0.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD));
            } catch (Exception e) {
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
    }

    public ItemBuilder head(final String owner) {
        return editMeta(meta -> {
            if (owner != null && Items.equals(Items.HEAD, result)) {
                final SkullMeta skullMeta = (SkullMeta) meta;
                skullMeta.setOwner(owner);
            }
        });
    }

    public ItemBuilder leatherArmorColor(final String color) {
        return editMeta(meta -> {
            final LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) meta;
            if (color != null) {
                leatherArmorMeta.setColor(DyeColor.valueOf(color).getColor());
            }
        });
    }

    public ItemBuilder potion(final PotionType type, final boolean extended, final boolean upgraded) {
        PotionMeta meta = (PotionMeta) result.getItemMeta();
        meta.setBasePotionData(new PotionData(type, extended, upgraded));
        result.setItemMeta(meta);
        return this;
    }

    public ItemBuilder attribute(final String name, final int operation, final double amount, final String slotName) {
        return editMeta(meta -> {
            final Attribute attribute = EnumUtil.getByName(attributeNameToEnum(name), Attribute.class);
            if (attribute == null) {
                return;
            }
            final AttributeModifier modifier;
            if (slotName != null) {
                final EquipmentSlot slot = EnumUtil.getByName(slotName, EquipmentSlot.class);
                if (slot == null) {
                    return;
                }
                modifier = new AttributeModifier(UUID.randomUUID(), name, amount, AttributeModifier.Operation.values()[operation], slot);
            } else {
                modifier = new AttributeModifier(UUID.randomUUID(), name, amount, AttributeModifier.Operation.values()[operation]);
            }
            meta.addAttributeModifier(attribute, modifier);
        });
    }

    private String attributeNameToEnum(String name) {
        int len = name.length();
        int capitalLetterIndex = -1;
        for (int i = 0; i < len; i++) {
            if (Character.isUpperCase(name.charAt(i))) {
                capitalLetterIndex = i;
                break;
            }
        }
        if (capitalLetterIndex != -1) {
            name = name.substring(0, capitalLetterIndex) + "_" + name.substring(capitalLetterIndex);
        }
        return name.replace(".", "_").toUpperCase();
    }

    public ItemStack build() {
        return result;
    }
}