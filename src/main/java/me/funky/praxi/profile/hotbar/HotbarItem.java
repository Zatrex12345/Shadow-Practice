package me.funky.praxi.profile.hotbar;

import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;

public enum HotbarItem {

	QUEUE_JOIN_RANKED(null),
	QUEUE_JOIN_UNRANKED(null),
	QUEUE_LEAVE(null),
	PARTY_EVENTS(null),
	PARTY_CREATE("party create"),
	PARTY_MANAGE("partymanage"),
	PARTY_DISBAND("party disband"),
	PARTY_LEAVE("party leave"),
	PARTY_INFORMATION("party info"),
	OTHER_PARTIES(null),
	KIT_EDITOR(null),
    SPECTATE_MENU("spectate menu"),
	SPECTATE_STOP("stopspectating"),
	PROFILE_SETTINGS("profilesettings"),
	VIEW_INVENTORY(null),
	EVENT_JOIN("event join"),
	EVENT_LEAVE("event leave"),
	MAP_SELECTION(null),
	REMATCH_REQUEST("rematch"),
	REMATCH_ACCEPT("rematch"),
	KIT_SELECTION(null);

	@Getter private String command;
	@Getter @Setter private Pattern pattern;

	HotbarItem(String command) {
		this.command = command;
	}

}
