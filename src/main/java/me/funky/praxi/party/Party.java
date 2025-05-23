package me.funky.praxi.party;

import lombok.Getter;
import lombok.Setter;
import me.funky.praxi.Locale;
import me.funky.praxi.Praxi;
import me.funky.praxi.adapter.core.CoreManager;
import me.funky.praxi.duel.DuelRequest;
import me.funky.praxi.profile.Profile;
import me.funky.praxi.profile.ProfileState;
import me.funky.praxi.profile.hotbar.Hotbar;
import me.funky.praxi.profile.visibility.VisibilityLogic;
import me.funky.praxi.util.ReplaceUtil;
import me.funky.praxi.util.CC;
import me.funky.praxi.util.ChatComponentBuilder;
import me.funky.praxi.util.ChatHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Party {

	@Getter private static List<Party> parties = new ArrayList<>();

	@Getter @Setter private Player leader;
	private final List<UUID> players;
	private final List<PartyInvite> invites;
	private PartyPrivacy privacy;
	private boolean disbanded = false;
	private int Limit = 10;
	private boolean isPublic = false;

	public Party(Player player) {
		this.leader = player;
		this.players = new ArrayList<>();
		this.invites = new ArrayList<>();
		this.privacy = PartyPrivacy.CLOSED;

		this.players.add(player.getUniqueId());

		parties.add(this);
	}

	public void setPrivacy(PartyPrivacy privacy) {
		this.privacy = privacy;

		sendMessage(Locale.PARTY_PRIVACY_CHANGE.format(privacy.getReadable()));
	}

	public boolean containsPlayer(UUID uuid) {
		return players.contains(uuid);
	}

	public PartyInvite getInvite(UUID uuid) {
		Iterator<PartyInvite> iterator = invites.iterator();

		while (iterator.hasNext()) {
			PartyInvite invite = iterator.next();

			if (invite.getUuid().equals(uuid)) {
				if (invite.hasExpired()) {
					iterator.remove();
					return null;
				} else {
					return invite;
				}
			}
		}

		return null;
	}

	public void invite(Player target) {
		invites.add(new PartyInvite(target.getUniqueId()));

		for (String msg : Locale.PARTY_INVITE.formatLines(leader.getName(), getColoredName(leader))) {
			if (msg.contains("%CLICKABLE%")) {
				msg = msg.replace("%CLICKABLE%", "");

				target.spigot().sendMessage(new ChatComponentBuilder("")
						.parse(msg)
						.attachToEachPart(ChatHelper.click("/party join " + leader.getName()))
						.attachToEachPart(ChatHelper.hover(Locale.PARTY_INVITE_HOVER.format()))
						.create());
			} else {
				target.sendMessage(msg);
			}
		}

		sendMessage(Locale.PARTY_INVITE_BROADCAST.format(getColoredName(target)));
	}

	public void join(Player player) {
		invites.removeIf(invite -> invite.getUuid().equals(player.getUniqueId()));
	    if (getPlayers().size() >= getLimit()) {
            player.sendMessage(CC.translate("&cThis party is full"));
        } else {
		    players.add(player.getUniqueId());

		    sendMessage(Locale.PARTY_JOIN.format(getColoredName(player)));

		    Profile profile = Profile.getByUuid(player.getUniqueId());
		    profile.setParty(this);

		    if (profile.getState() == ProfileState.LOBBY || profile.getState() == ProfileState.QUEUEING) {
			    Hotbar.giveHotbarItems(player);

			    for (Player otherPlayer : getListOfPlayers()) {
				    VisibilityLogic.handle(player, otherPlayer);
			    }
		    }

		    VisibilityLogic.handle(player);

		    for (Player otherPlayer : getListOfPlayers()) {
			    VisibilityLogic.handle(otherPlayer, player);
		    }
        }
	}

	public void leave(Player player, boolean kick) {
		sendMessage(Locale.PARTY_LEAVE.format(getColoredName(player), (kick ? "been kicked from" : "left")));

		players.removeIf(uuid -> uuid.equals(player.getUniqueId()));

		Profile profile = Profile.getByUuid(player.getUniqueId());
		profile.setParty(null);

		if (profile.getState() == ProfileState.LOBBY || profile.getState() == ProfileState.QUEUEING) {
			Hotbar.giveHotbarItems(player);
		}

		VisibilityLogic.handle(player);

		for (Player otherPlayer : getListOfPlayers()) {
			VisibilityLogic.handle(otherPlayer, player);
		}
	}

	public void disband() {
		parties.remove(this);

		sendMessage(Locale.PARTY_DISBAND.format());

		// Remove any party duel requests
		Profile leaderProfile = Profile.getByUuid(leader.getUniqueId());

		if (leaderProfile != null) {
			leaderProfile.getDuelRequests().removeIf(DuelRequest::isParty);
		}

		// Reset player profiles
		for (Player player : getListOfPlayers()) {
			Profile profile = Profile.getByUuid(player.getUniqueId());
			profile.setParty(null);

			if (profile.getState() == ProfileState.LOBBY || profile.getState() == ProfileState.QUEUEING) {
				Hotbar.giveHotbarItems(player);
			}
		}

		for (Player player : getListOfPlayers()) {
			VisibilityLogic.handle(player);
		}
		disbanded = true;
	}

	public void sendInformation(Player sendTo) {
		StringBuilder builder = new StringBuilder();

		for (Player player : getListOfPlayers()) {
			builder.append(CC.RESET)
			       .append(player.getName())
			       .append(CC.GRAY)
			       .append(", ");
		}

		String[] lines = new String[]{
				CC.CHAT_BAR,
				CC.AQUA + "Party Information",
				CC.DARK_AQUA + "Privacy: " + CC.GRAY + privacy.getReadable(),
				CC.DARK_AQUA + "Leader: " + CC.RESET + leader.getName(),
				CC.DARK_AQUA + "Members: " + CC.GRAY + "(" + getPlayers().size() + ") " +
				builder.substring(0, builder.length() - 2),
				CC.CHAT_BAR
		};

		sendTo.sendMessage(lines);
	}

	public void sendChat(Player player, String message) {
		sendMessage(Locale.PARTY_CHAT_PREFIX.format() +
		            player.getDisplayName() + ChatColor.RESET + ": " + message);
	}

	public void sendMessage(String message) {
		for (Player player : getListOfPlayers()) {
			player.sendMessage(message);
		}
	}

	public List<Player> getListOfPlayers() {
		List<Player> players = new ArrayList<>();

		for (UUID uuid : this.players) {
			Player player = Bukkit.getPlayer(uuid);

			if (player != null) {
				players.add(player);
			}
		}

		return players;
	}

	public static void init() {
		// Remove expired invites from each party every 2 seconds
		new BukkitRunnable() {
			@Override
			public void run() {
				Party.getParties().forEach(party -> party.getInvites().removeIf(PartyInvite::hasExpired));
			}
		}.runTaskTimerAsynchronously(Praxi.get(), 20L * 2, 20L * 2);
	}

	private String getColoredName(Player player) {
		return CoreManager.getInstance().getCore().getColoredName(player.getUniqueId());
	}

	private String getRealName(Player player) {
		return CoreManager.getInstance().getCore().getRealName(player);
	}
}
