package me.funky.praxi.event.game.menu;

import lombok.AllArgsConstructor;
import me.funky.praxi.Praxi;
import me.funky.praxi.adapter.lunar.EventNotification;
import me.funky.praxi.profile.Profile;
import me.funky.praxi.event.Event;
import me.funky.praxi.event.game.EventGame;
import me.funky.praxi.event.game.map.EventGameMap;
import me.funky.praxi.event.game.map.vote.EventGameMapVoteData;
import me.funky.praxi.util.CC;
import me.funky.praxi.util.ItemBuilder;
import me.funky.praxi.util.TextSplitter;
import me.funky.praxi.util.menu.Button;
import me.funky.praxi.util.menu.button.DisplayButton;
import me.funky.praxi.util.menu.Menu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHostMenu extends Menu {

    {
        setPlaceholder(true);
    }

    @Override
    public String getTitle(Player player) {
        return Praxi.get().getMenusConfig().getString("EVENTS.TITLE");
    }

    @Override
    public int getSize() {
        return Praxi.get().getMenusConfig().getInteger("EVENTS.SIZE");
    }


	@Override
	public Map<Integer, Button> getButtons(Player player) {
		Map<Integer, Button> buttons = new HashMap<>();

		int pos = 10;

		for (Event event : Event.events) {
			buttons.put(pos++, new SelectEventButton(event));
		}

		if (pos <= 17) {
			for (int i = pos; i < 17; i++) {
				buttons.put(i, new DisplayButton(new ItemBuilder(Material.STAINED_GLASS_PANE)
						.durability(7).name(" ").build(), true));
			}
		}

		return buttons;
	}

    private int getHostSlots(Player host) {
        int slots = 32;
        FileConfiguration config = Praxi.get().getEventsConfig().getConfiguration();

        for (String key : config.getConfigurationSection("HOST_SLOTS").getKeys(false)) {
            if (host.hasPermission(config.getString("HOST_SLOTS." + key + ".PERMISSION"))) {
                if (config.getInt("HOST_SLOTS." + key + ".SLOTS") > slots) {
                    slots = config.getInt("HOST_SLOTS." + key + ".SLOTS");
                }
            }
        }

        return slots;
    }

    @AllArgsConstructor
    private class SelectEventButton extends Button {

        private final EventNotification eventNotification = Praxi.get().getEventNotification();
        private Event event;

        @Override
        public ItemStack getButtonItem(Player player) {
            Profile profile = Profile.getByUuid(player.getUniqueId());
            List<String> lore = new ArrayList<>();
            lore.add(CC.MENU_BAR);

            for (String descriptionLine : TextSplitter.split(28, event.getDescription(), "&f", " ")) {
                lore.add(" " + descriptionLine);
            }

            lore.add("");

            if (event.canHost(player) || profile.getEventTokens() > 0) {
                lore.add(CC.translate(Praxi.get().getMenusConfig().getString("EVENTS.CAN-HOST")));
            } else {
                for (String cantHost : Praxi.get().getMenusConfig().getStringList("EVENTS.CANT-HOST")) {
			        lore.add(CC.translate(cantHost));
		        }
            }

            lore.add(CC.MENU_BAR);

            return new ItemBuilder(event.getIcon().clone())
		            .name(Praxi.get().getMenusConfig().getString("EVENTS.EVENT-NAME").replace("{event}", event.getDisplayName()))
                    .lore(lore)
                    .clearFlags()
                    .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            Profile profile = Profile.getByUuid(player.getUniqueId());
            if (event.canHost(player)) {

                if (player.hasMetadata("frozen")) {
                    player.sendMessage(ChatColor.RED + "You can't host an event while frozen.");
                    return;
                }

                if (EventGame.getActiveGame() != null) {
                    player.sendMessage(CC.RED + "There is already an active event.");
                    return;
                }

                if (!EventGame.getCooldown().hasExpired()) {
                    player.sendMessage(CC.RED + "The event cooldown is active.");
                    return;
                }

                if (event == null) {
                    player.sendMessage(CC.RED + "That type of event does not exist.");
                    player.sendMessage(CC.RED + "Types: sumo, spleef, brackets, gulag");
                    return;
                }

                if (EventGameMap.getMaps().isEmpty()) {
                    player.sendMessage(CC.RED + "There are no available event maps.");
                    return;
                }

                List<EventGameMap> validMaps = new ArrayList<>();

                for (EventGameMap gameMap : EventGameMap.getMaps()) {
                    if (event.getAllowedMaps().contains(gameMap.getMapName())) {
                        validMaps.add(gameMap);
                    }
                }

                if (validMaps.isEmpty()) {
                    player.sendMessage(CC.RED + "There are no available event maps.");
                    return;
                }

                try {
                    EventGame game = new EventGame(event, player, getHostSlots(player));

                    for (EventGameMap gameMap : validMaps) {
                        game.getVotesData().put(gameMap, new EventGameMapVoteData());
                    }

                    game.broadcastJoinMessage();
                    game.start();
                    game.getGameLogic().onJoin(player);
			    	if (Praxi.get().isLunar()) { // check if Apollo-Bukkit enabled
                        this.eventNotification.displayNotification(player); // send notification if Apollo-Bukkit enabled and the player is using lunar client
                    }
                } catch (Exception ignored) {
                }
		    } else if (profile.getEventTokens() > 0) {
                if (player.hasMetadata("frozen")) {
                    player.sendMessage(ChatColor.RED + "You cannot host an event while frozen.");
                    return;
                }

                if (EventGame.getActiveGame() != null) {
                    player.sendMessage(CC.RED + "There is already an active event.");
                    return;
                }

                if (!EventGame.getCooldown().hasExpired()) {
                    player.sendMessage(CC.RED + "The event cooldown is active.");
                    return;
                }

                if (event == null) {
                    player.sendMessage(CC.RED + "That type of event does not exist.");
                    player.sendMessage(CC.RED + "Types: sumo, spleef, brackets, gulag");
                    return;
                }

                if (EventGameMap.getMaps().isEmpty()) {
                    player.sendMessage(CC.RED + "There are no available event maps.");
                    return;
                }

                List<EventGameMap> validMaps = new ArrayList<>();

                for (EventGameMap gameMap : EventGameMap.getMaps()) {
                    if (event.getAllowedMaps().contains(gameMap.getMapName())) {
                        validMaps.add(gameMap);
                    }
                }

                if (validMaps.isEmpty()) {
                    player.sendMessage(CC.RED + "There are no available event maps.");
                    return;
                }

                try {
                    EventGame game = new EventGame(event, player, getHostSlots(player));

                    for (EventGameMap gameMap : validMaps) {
                        game.getVotesData().put(gameMap, new EventGameMapVoteData());
                    }

                    game.broadcastJoinMessage();
                    game.start();
                    game.getGameLogic().onJoin(player);
			    	if (Praxi.get().isLunar()) { // check if Apollo-Bukkit enabled
                        this.eventNotification.displayNotification(player); // send notification if Apollo-Bukkit enabled and the player is using lunar client
                    }
                } catch (Exception ignored) {
                }
            } else {
                player.sendMessage(CC.translate("&cYou do not have permission or any event tokens to host this event."));
            }
        }
    }
}