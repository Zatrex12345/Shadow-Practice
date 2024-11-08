package me.funky.praxi.party.menu;

import lombok.AllArgsConstructor;
import me.funky.praxi.Praxi;
import me.funky.praxi.party.PartyEvent;
import me.funky.praxi.profile.Profile;
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

public class PartyEventSelectEventMenu extends Menu {

	@Override
	public String getTitle(Player player) {
		return Praxi.get().getMenusConfig().getString("PARTY.SELECT-EVENT.TITLE");
	}

	@Override
    public int getSize() {
		return Praxi.get().getMenusConfig().getInteger("PARTY.SELECT-EVENT.SIZE");
    }

	@Override
	public Map<Integer, Button> getButtons(Player player) {
		Map<Integer, Button> buttons = new HashMap<>();
		for (int k = 0; k < 27; k++) {
			buttons.put(k, Constants.BLACK_PANE);
		}
		buttons.put(12, new SelectEventButton(PartyEvent.FFA));
		buttons.put(14, new SelectEventButton(PartyEvent.SPLIT));
		return buttons;
	}

	@AllArgsConstructor
	private class SelectEventButton extends Button {

		private PartyEvent partyEvent;

		@Override
		public ItemStack getButtonItem(Player player) {
			List<String> lore = new ArrayList<>();
			lore.add("");
			lore.add(" &aClick here to select.");
			return new ItemBuilder(partyEvent == PartyEvent.FFA ? Material.QUARTZ : Material.REDSTONE)
					.name("&b&l" + partyEvent.getName())
					.lore(lore)
					.build();
		}

		@Override
		public void clicked(Player player, ClickType clickType) {
			Profile profile = Profile.getByUuid(player.getUniqueId());

			if (profile.getParty() == null) {
				player.sendMessage(CC.RED + "You are not in a party.");
				return;
			}

			new PartyEventSelectKitMenu(partyEvent).openMenu(player);
		}

	}

}
