package me.funky.praxi.util.menu.pagination;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import me.funky.praxi.util.ItemBuilder;
import me.funky.praxi.util.menu.Button;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
public class PageButton extends Button {

	private int mod;
	private PaginatedMenu menu;

	@Override
	public ItemStack getButtonItem(Player player) {
		if (this.mod > 0) {
			if (hasNext(player)) {
				return new ItemBuilder(Material.PAPER)
						.name(ChatColor.GREEN + "Next Page")
						.lore(Arrays.asList(
								ChatColor.WHITE + "Click here to jump",
								ChatColor.WHITE + "to the next page."
						))
						.build();
			} else {
				return new ItemBuilder(Material.PAPER)
						.name(ChatColor.GRAY + "Next Page")
						.lore(Arrays.asList(
								ChatColor.WHITE + "There is no available",
								ChatColor.WHITE + "next page."
						))
						.build();
			}
		} else {
			if (hasPrevious(player)) {
				return new ItemBuilder(Material.PAPER)
						.name(ChatColor.GREEN + "Previous Page")
						.lore(Arrays.asList(
								ChatColor.WHITE + "Click here to jump",
								ChatColor.WHITE + "to the previous page."
						))
						.build();
			} else {
				return new ItemBuilder(Material.PAPER)
						.name(ChatColor.GRAY + "Previous Page")
						.lore(Arrays.asList(
								ChatColor.WHITE + "There is no available",
								ChatColor.WHITE + "previous page."
						))
						.build();
			}
		}
	}

	@Override
	public void clicked(Player player, ClickType clickType) {
		if (this.mod > 0) {
			if (hasNext(player)) {
				this.menu.modPage(player, this.mod);
				Button.playNeutral(player);
			} else {
				Button.playFail(player);
			}
		} else {
			if (hasPrevious(player)) {
				this.menu.modPage(player, this.mod);
				Button.playNeutral(player);
			} else {
				Button.playFail(player);
			}
		}
	}

	private boolean hasNext(Player player) {
		int pg = this.menu.getPage() + this.mod;
		return this.menu.getPages(player) >= pg;
	}

	private boolean hasPrevious(Player player) {
		int pg = this.menu.getPage() + this.mod;
		return pg > 0;
	}

}
