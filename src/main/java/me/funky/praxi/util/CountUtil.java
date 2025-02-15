package me.funky.praxi.util;

import lombok.experimental.UtilityClass;
//import net.citizensnpcs.nms.v1_8_R3.util.NMSImpl;
import me.funky.praxi.Praxi;
import me.funky.praxi.util.NumberUtil;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.apache.commons.io.IOUtils;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@UtilityClass
public final class CountUtil {

    public final Predicate<ItemStack> INSTANT_HEAL_POTION_PREDICATE =item -> {
        if (item.getType() != Material.POTION) {
            return false;
        }

        PotionType potionType = Potion.fromItemStack(item).getType();
        return potionType == PotionType.INSTANT_HEAL;
    };

    public final Predicate<ItemStack> SOUP_PREDICATE = item -> item.getType() == Material.MUSHROOM_SOUP;

    public final Predicate<ItemStack> DEBUFF_POTION_PREDICATE = item -> {
        if (item.getType() == Material.POTION) {
            PotionType type = Potion.fromItemStack(item).getType();
            return type == PotionType.WEAKNESS || type == PotionType.SLOWNESS
                    || type == PotionType.POISON || type == PotionType.INSTANT_DAMAGE;
        } else {
            return false;
        }
    };

    public final Predicate<ItemStack> EDIBLE_PREDICATE = item -> item.getType().isEdible();

    public int countStacksMatching(ItemStack[] items, Predicate<ItemStack> predicate) {
        if (items == null) {
            return 0;
        }

        int amountMatching = 0;

        for (ItemStack item : items) {
            if (item != null && predicate.test(item)) {
                amountMatching++;
            }
        }

        return amountMatching;
    }

    public ItemData[] repeat(Material material, int times) {
        return repeat(material, (byte) 0, times);
    }

    public ItemData[] repeat(Material material, byte data, int times) {
        ItemData[] itemData = new ItemData[times];

        for (int i = 0; i < times; i++) {
            itemData[i] = new ItemData(material, data);
        }

        return itemData;

    }

    public ItemData[] armorOf(ArmorPart part) {
        List<ItemData> data = new ArrayList<>();

        for (ArmorType at : ArmorType.values()) {
            data.add(new ItemData(Material.valueOf(at.name() + "_" + part.name()), (short) 0));
        }

        return data.toArray(new ItemData[data.size()]);
    }

    public ItemData[] swords() {
        List<ItemData> data = new ArrayList<>();

        for (SwordType at : SwordType.values()) {
            data.add(new ItemData(Material.valueOf(at.name() + "_SWORD"), (short) 0));
        }

        return data.toArray(new ItemData[data.size()]);
    }

    public String getName(ItemStack item) {
        if (item.getDurability() != 0) {
            net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);

            if (nmsStack != null) {
                String name = nmsStack.getName();

                if (name.contains(".")) {
                    name = WordUtils.capitalize(item.getType().toString().toLowerCase().replace("_", " "));
                }

                return name;
            }
        }

        String string = item.getType().toString().replace("_", " ");
        char[] chars = string.toLowerCase().toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i]) || chars[i] == '.' || chars[i] == '\'') {
                found = false;
            }
        }
        return String.valueOf(chars);
    }

    public void setDisplayName(ItemStack itemStack, String string) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(string);
        itemStack.setItemMeta(itemMeta);
    }

    /*public ItemBuilder builder(Material type) {
        return ItemBuilder.of(type);
    }*/

    @Getter
    @AllArgsConstructor
    public class ItemData {

        private final Material material;
        private final short data;

        public String getName() {
            return CountUtil.getName(toItemStack());
        }

        public boolean matches(ItemStack item) {
            return item != null && item.getType() == material && item.getDurability() == data;
        }

        public ItemStack toItemStack() {
            return new ItemStack(material, 1, data);
        }

    }

    public enum ArmorPart {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS
    }

    public enum ArmorType {
        DIAMOND, IRON, GOLD, LEATHER
    }

    public enum SwordType {
        DIAMOND, IRON, GOLD, STONE
    }


    public boolean isEating(Player player) {
        EntityPlayer entityPlayer = ((CraftPlayer)player).getHandle();
        ItemStack item = player.getItemInHand();
        boolean eatingProtocol = (entityPlayer.getDataWatcher().getByte(0) & 0x10) != 0;
        return eatingProtocol && (isFood(item) || isSpeedOrFire(item));
    }

    private boolean isSpeedOrFire(ItemStack item) {
        if (item == null) return false;

        if (item.getType() != Material.POTION) {
            return false;
        }

        PotionType potionType = Potion.fromItemStack(item).getType();
        return potionType == PotionType.FIRE_RESISTANCE || potionType == PotionType.SPEED;
    }

    public boolean isHeal(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();
        if (type.equals(Material.POTION)) return item.getDurability() == 16421 || item.getDurability() == 16453;

        return type.equals(Material.MUSHROOM_SOUP) || type.equals(Material.GOLDEN_APPLE);
    }

    public boolean isFood(ItemStack item) {
        Material type = item.getType();
        return type.equals(Material.COOKED_FISH)
                || type.equals(Material.GRILLED_PORK)
                || type.equals(Material.COOKED_CHICKEN)
                || type.equals(Material.COOKED_BEEF)
                || type.equals(Material.PUMPKIN_PIE)
                || type.equals(Material.BAKED_POTATO)
                || type.equals(Material.GOLDEN_CARROT);
    }

    public void swapItem(Inventory inv, int i, int j) {
        ItemStack item1 = inv.getItem(i);
        ItemStack item2 = inv.getItem(j);

        inv.setItem(i, item2);
        inv.setItem(j, item1);
    }


}
