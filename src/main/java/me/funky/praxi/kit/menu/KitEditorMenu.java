package me.funky.praxi.kit.menu;

import lombok.AllArgsConstructor;
import me.funky.praxi.Locale;
import me.funky.praxi.Praxi;
import me.funky.praxi.kit.Kit;
import me.funky.praxi.kit.KitLoadout;
import me.funky.praxi.profile.Profile;
import me.funky.praxi.profile.ProfileState;
import me.funky.praxi.profile.hotbar.Hotbar;
import me.funky.praxi.profile.meta.ProfileKitData;
import me.funky.praxi.util.BukkitReflection;
import me.funky.praxi.util.CC;
import me.funky.praxi.util.ItemBuilder;
import me.funky.praxi.util.PlayerUtil;
import me.funky.praxi.util.InventoryUtil;
import me.funky.praxi.util.menu.Button;
import me.funky.praxi.util.menu.Menu;
import me.funky.praxi.util.menu.button.DisplayButton;
import org.bukkit.Material;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class KitEditorMenu extends Menu {

    private static final int[] ITEM_POSITIONS = new int[]{
        10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
    };
    private static final int[] BORDER_POSITIONS = new int[]{ 
        1, 7, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 35, 38, 39, 40, 41, 42, 43, 44, 45, 46, 50, 51, 53 
    };
    private static final Button BORDER_BUTTON = Button.placeholder(Material.STAINED_GLASS_PANE, (byte) 7, " ");

    private int index;

    {
        setUpdateAfterClick(false);
    }

    @Override
    public String getTitle(Player player) {
        Profile profile = Profile.getByUuid(player.getUniqueId());
        return Praxi.get().getMenusConfig().getString("KIT-EDITOR.EDITOR.TITLE").replace("{kit}", profile.getKitEditorData().getSelectedKit().getName());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (int border : BORDER_POSITIONS) {
            buttons.put(border, BORDER_BUTTON);
        }

        Profile profile = Profile.getByUuid(player.getUniqueId());
        Kit kit = profile.getKitEditorData().getSelectedKit();
        KitLoadout kitLoadout = profile.getKitEditorData().getSelectedKitLoadout();

        buttons.put(0, new CancelButton(index));
        buttons.put(2, new RenameKitButton(kit, kitLoadout));
        buttons.put(3, new DeleteKitButton(kit, kitLoadout));
        buttons.put(4, new SaveButton());
        buttons.put(5, new ClearInventoryButton());
        buttons.put(6, new LoadDefaultKitButton());
        buttons.put(8, new CancelButton(index));

        buttons.put(47, new ArmorDisplayButton(kitLoadout.getArmor()[3]));
        buttons.put(48, new ArmorDisplayButton(kitLoadout.getArmor()[2]));
        buttons.put(49, new ArmorDisplayButton(kitLoadout.getArmor()[1]));
        buttons.put(50, new ArmorDisplayButton(kitLoadout.getArmor()[0]));

        List<ItemStack> items = kit.getEditRules().getEditorItems();

        if (!items.isEmpty()) {
            for (int i = 0; i < items.size() && i < ITEM_POSITIONS.length; i++) {
                buttons.put(ITEM_POSITIONS[i], new InfiniteItemButton(items.get(i)));
            }
        }

        return buttons;
    }

    @Override
    public void onOpen(Player player) {
        if (!isClosedByMenu()) {
            PlayerUtil.reset(player);

            Profile profile = Profile.getByUuid(player.getUniqueId());
            profile.getKitEditorData().setActive(true);

            if (profile.getKitEditorData().getSelectedKit() != null) {
                player.getInventory().setContents(profile.getKitEditorData().getSelectedKitLoadout().getContents());
            }

            player.updateInventory();
        }
    }

    @Override
    public void onClose(Player player) {
        Profile profile = Profile.getByUuid(player.getUniqueId());
        profile.getKitEditorData().setActive(false);

        if (profile.getState() != ProfileState.FIGHTING) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Hotbar.giveHotbarItems(player);
                }
            }.runTask(Praxi.get());
        }
    }

    @AllArgsConstructor
    private class ArmorDisplayButton extends Button {

        private ItemStack itemStack;

        @Override
        public ItemStack getButtonItem(Player player) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                return new ItemStack(Material.AIR);
            }

            return new ItemBuilder(itemStack.clone())
                    .name(CC.AQUA + BukkitReflection.getItemStackName(itemStack))
                    .lore(CC.YELLOW + "This is automatically equipped.")
                    .build();
        }

    }

    @AllArgsConstructor
    private class DeleteKitButton extends Button {

        private Kit kit;
        private KitLoadout kitLoadout;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.REDSTONE_BLOCK)
                .name(Praxi.get().getMenusConfig().getString("KIT-EDITOR.MANAGEMENT.BUTTONS.DELETE-BUTTON"))
                .durability(14)
                .lore(Arrays.asList(
                    "&cClick to delete this kit.",
                    "&cYou will &lNOT &cbe able to",
                    "&crecover this kit Loadout."
                ))
                .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            Profile profile = Profile.getByUuid(player.getUniqueId());
            profile.getKitData().get(kit).deleteKit(kitLoadout);

            new KitManagementMenu(kit).openMenu(player);
        }

    }

    @AllArgsConstructor
    private class RenameKitButton extends Button {

        private Kit kit;
        private KitLoadout kitLoadout;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.NAME_TAG)
                .name(Praxi.get().getMenusConfig().getString("KIT-EDITOR.EDITOR.BUTTONS.RENAME-BUTTON"))
                .lore("&aClick to rename this kit.")
                .build();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarSlot) {
            Menu.currentlyOpenedMenus.get(player.getName()).setClosedByMenu(true);

            player.closeInventory();
            player.sendMessage(Locale.KIT_EDITOR_START_RENAMING.format(kitLoadout.getCustomName()));

            Profile profile = Profile.getByUuid(player.getUniqueId());
            profile.getKitEditorData().setSelectedKit(kit);
            profile.getKitEditorData().setSelectedKitLoadout(kitLoadout);
            profile.getKitEditorData().setActive(true);
            profile.getKitEditorData().setRename(true);
        }

    }

    @AllArgsConstructor
    private class ClearInventoryButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.INK_SACK)
                .durability(1)
                .name(Praxi.get().getMenusConfig().getString("KIT-EDITOR.EDITOR.BUTTONS.CLEAR-BUTTON"))
                .lore(Arrays.asList(
                    "&fThis will clear your inventory",
                    "&fso you can start over."
                ))
                .build();
        }

        @Override
        public void clicked(Player player, int i, ClickType clickType, int hb) {
            Button.playNeutral(player);
            player.getInventory().setContents(new ItemStack[36]);
            player.updateInventory();
        }

        @Override
        public boolean shouldUpdate(Player player, ClickType clickType) {
            return true;
        }

    }

    @AllArgsConstructor
    private class LoadDefaultKitButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.INK_SACK)
                .durability(6)
                .name(Praxi.get().getMenusConfig().getString("KIT-EDITOR.EDITOR.BUTTONS.LOAD-DEFAULT-BUTTON"))
                .lore(Arrays.asList(
                    "&fClick this to load the default kit",
                    "&finto the kit editing menu."
                ))
                .build();
        }

        @Override
        public void clicked(Player player, int i, ClickType clickType, int hb) {
            Button.playNeutral(player);

            Profile profile = Profile.getByUuid(player.getUniqueId());
            player.getInventory().setContents(profile.getKitEditorData().getSelectedKit().getKitLoadout().getContents());
            player.updateInventory();
        }

        @Override
        public boolean shouldUpdate(Player player, ClickType clickType) {
            return true;
        }

    }

    @AllArgsConstructor
    private class SaveButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.INK_SACK)
                .durability(10)
                .name(Praxi.get().getMenusConfig().getString("KIT-EDITOR.EDITOR.BUTTONS.SAVE-BUTTON"))
                .lore("&fClick this to save your kit.")
                .build();
        }

        @Override
        public void clicked(Player player, int i, ClickType clickType, int hb) {
            Button.playNeutral(player);
            player.closeInventory();

            Profile profile = Profile.getByUuid(player.getUniqueId());

            if (profile.getKitEditorData().getSelectedKitLoadout() != null) {
                profile.getKitEditorData().getSelectedKitLoadout().setContents(player.getInventory().getContents());
            }

            Hotbar.giveHotbarItems(player);

            new KitManagementMenu(profile.getKitEditorData().getSelectedKit()).openMenu(player);
        }

    }

    @AllArgsConstructor
    private class CancelButton extends Button {

        private int index;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.STAINED_CLAY)
                .durability(14)
                .name(Praxi.get().getMenusConfig().getString("KIT-EDITOR.EDITOR.BUTTONS.CANCEL-BUTTON"))
                .lore(Arrays.asList(
                    "&fClick this to abort editing your kit,",
                    "&fand return to the kit menu."
                ))
                .build();
        }

        @Override
        public void clicked(Player player, int i, ClickType clickType, int hb) {
            Button.playNeutral(player);

            Profile profile = Profile.getByUuid(player.getUniqueId());

            if (profile.getKitEditorData().getSelectedKit() != null) {
                ProfileKitData kitData = profile.getKitData().get(profile.getKitEditorData().getSelectedKit());
                kitData.replaceKit(index, null);

                new KitManagementMenu(profile.getKitEditorData().getSelectedKit()).openMenu(player);
            }
        }

    }

    private class InfiniteItemButton extends DisplayButton {

        InfiniteItemButton(ItemStack itemStack) {
            super(itemStack, false);
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbar) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            ItemStack itemStack = inventory.getItem(slot);

            inventory.setItem(slot, itemStack);

            player.setItemOnCursor(itemStack);
            player.updateInventory();
        }

    }
}

