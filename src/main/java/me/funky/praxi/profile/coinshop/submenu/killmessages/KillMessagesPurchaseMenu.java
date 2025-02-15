package me.funky.praxi.profile.coinshop.submenu.killmessages;

import me.funky.praxi.Praxi;
import me.funky.praxi.profile.Profile;
import me.funky.praxi.profile.coinshop.CoinShopMenu;
import me.funky.praxi.profile.coinshop.submenu.killeffects.SpecialEffectsPurchaseMenu;
import me.funky.praxi.profile.option.killmessages.KillMessages;
import me.funky.praxi.util.CC;
import me.funky.praxi.util.Constants;
import me.funky.praxi.util.ItemBuilder;
import me.funky.praxi.util.menu.Button;
import me.funky.praxi.util.menu.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KillMessagesPurchaseMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "&8Purchase Kill Messages";
    }

    public int size(Map<Integer, Button> buttons) {
        return 27;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();
        for (int k = 0; k < 27; k++) {
            buttons.put(k, Constants.BLACK_PANE);
        }
        int y = 1;
        int x = 1;
        for (KillMessages messages : KillMessages.values()) {
            if (messages.equals(KillMessages.NONE)) continue;
            buttons.put(this.getSlot(x++, y), new KillMessagePurchaseButton(messages));
            if (x != 8) continue;
            ++y;
            x = 1;
        }
        buttons.put(0, new BacksButton());

        return buttons;
    }
    private static class BacksButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            List<String> lore = new ArrayList<>();
            Profile profile = Profile.getByUuid(player.getUniqueId());
            lore.add("");
            lore.add("&aClick Here to go to the previous page");
            return new ItemBuilder(Material.ARROW).name(CC.translate("&c&lGo Back")).lore(lore).build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            new CoinShopMenu().openMenu(player);
        }
    }
}