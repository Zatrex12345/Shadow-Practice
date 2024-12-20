package me.funky.praxi.party.menu;

import lombok.AllArgsConstructor;
import me.funky.praxi.Praxi;
import me.funky.praxi.party.Party;
import me.funky.praxi.profile.Profile;
import me.funky.praxi.util.CC;
import me.funky.praxi.util.ItemBuilder;
import me.funky.praxi.util.menu.Button;
import me.funky.praxi.util.menu.pagination.PaginatedMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OtherPartiesMenu extends PaginatedMenu {

	@Override
	public String getPrePaginatedTitle(Player player) {
		return Praxi.get().getMenusConfig().getString("PARTY.OTHERS.TITLE");
	}

	@Override
	public Map<Integer, Button> getAllPagesButtons(Player player) {
		Profile profile = Profile.getByUuid(player.getUniqueId());

		Map<Integer, Button> buttons = new HashMap<>();

		Party.getParties().forEach(party -> {
			if (!party.equals(profile.getParty())) {
				buttons.put(buttons.size(), new PartyDisplayButton(party));
			}
		});

		return buttons;
	}

	@AllArgsConstructor
	public static class PartyDisplayButton extends Button {

		private Party party;

		@Override
		public ItemStack getButtonItem(Player player) {
			List<String> lore = new ArrayList<>();
			int added = 0;

			for (Player partyPlayer : party.getListOfPlayers()) {
				if (added >= 10) {
					break;
				}

				lore.add(CC.GRAY + " - " + CC.RESET + partyPlayer.getPlayer().getName());

				added++;
			}

			if (party.getPlayers().size() != added) {
				lore.add(CC.GRAY + " and " + (party.getPlayers().size() - added) + " others...");
			}

			return new ItemBuilder(Material.SKULL_ITEM)
					.name("&fParty of &b" + party.getLeader().getName())
					.amount(party.getPlayers().size())
					.durability(3)
					.lore(lore)
					.build();
		}

		@Override
		public void clicked(Player player, ClickType clickType) {
			Profile profile = Profile.getByUuid(player.getUniqueId());

			if (profile.getParty() != null) {
				if (!profile.getParty().equals(party)) {
					if (profile.getParty().getLeader().equals(player)) {
						player.chat("/duel " + party.getLeader().getName());
					} else {
						player.sendMessage(ChatColor.RED + "You are not the leader of your party.");
					}
				}
			}
		}

	}
}
