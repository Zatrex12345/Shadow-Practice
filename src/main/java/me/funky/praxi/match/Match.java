package me.funky.praxi.match;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import lombok.Getter;
import lombok.Setter;
import me.jumper251.replay.api.ReplayAPI;
import me.jumper251.replay.filesystem.saving.ReplaySaver;
import me.funky.praxi.Locale;
import me.funky.praxi.Praxi;
import me.funky.praxi.arena.Arena;
import me.funky.praxi.kit.Kit;
import me.funky.praxi.adapter.spigot.SpigotManager;
import me.funky.praxi.match.participant.MatchGamePlayer;
import me.funky.praxi.match.task.MatchLogicTask;
import me.funky.praxi.match.task.MatchPearlCooldownTask;
import me.funky.praxi.match.task.MatchResetTask;
import me.funky.praxi.match.task.MatchSnapshotCleanupTask;
import me.funky.praxi.participant.GameParticipant;
import me.funky.praxi.participant.GamePlayer;
import me.funky.praxi.profile.Profile;
import me.funky.praxi.profile.ProfileState;
import me.funky.praxi.profile.hotbar.Hotbar;
import me.funky.praxi.profile.meta.ProfileKitData;
import me.funky.praxi.profile.option.killeffect.SpecialEffects;
import me.funky.praxi.profile.visibility.VisibilityLogic;
import me.funky.praxi.queue.Queue;
import me.funky.praxi.util.*;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public abstract class Match {

    @Getter protected static List<Match> matches = new ArrayList<>();

    protected final Kit kit;
    protected final Arena arena;
    protected final boolean ranked;
    protected final List<MatchSnapshot> snapshots;
    protected final List<UUID> spectators;
    protected final List<Item> droppedItems;
    private final UUID matchId = UUID.randomUUID();
    private final Queue queue;
    private final List<Location> placedBlocks;
    private final List<BlockState> changedBlocks;
	protected boolean bedABroken;
    protected boolean bedBBroken;
	protected boolean eggABroken;
    protected boolean eggBBroken;
	public int portalCountA = 0;
	public int portalCountB = 0;
    public int playerALives = 5;
    public int playerBLives = 5;
    public int roundNumberBridge = 0;
    public int playerADead = 0;
    public int playerBDead = 0;
    public int playerAKills = 0;
    public int playerBKills = 0;
	private int winnerCoins = 36;
	private int loserCoins = 10;
	private int winnerExp = 85;
	private int loserExp = 55;
    @Setter
    protected MatchState state = MatchState.STARTING_ROUND;
    protected long timeData;
    protected MatchLogicTask logicTask;
	private boolean duel;

    public Match(Queue queue, Kit kit, Arena arena, boolean ranked, boolean duel) {
        this.queue = queue;
        this.kit = kit;
        this.arena = arena;
        this.ranked = ranked;
        this.duel = duel;
		this.bedABroken = false;
        this.bedBBroken = false;
        this.snapshots = new ArrayList<>();
        this.spectators = new ArrayList<>();
        this.droppedItems = new ArrayList<>();
        this.placedBlocks = new ArrayList<>();
        this.changedBlocks = new ArrayList<>();

        Praxi.get().getCache().getMatches().add(this);
    }


    public static void init() {
        new MatchPearlCooldownTask().runTaskTimerAsynchronously(Praxi.get(), 2L, 2L);
        new MatchSnapshotCleanupTask().runTaskTimerAsynchronously(Praxi.get(), 20L * 5, 20L * 5);
    }

    public static void cleanup() {
        for (Match match : Praxi.get().getCache().getMatches()) {
            match.getPlacedBlocks().forEach(location -> location.getBlock().setType(Material.AIR));
            match.getChangedBlocks().forEach((blockState) -> blockState.getLocation().getBlock().setType(blockState.getType()));
            match.getDroppedItems().forEach(Entity::remove);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Profile profile = Profile.getByUuid(player.getUniqueId());
            profile.save();
        }
    }

    public static void cleanupThis(Match match) {
        match.getDroppedItems().forEach(Entity::remove);
        match.getPlacedBlocks().forEach(location -> location.getBlock().setType(Material.AIR));
    }

    public static void matchesCleanup() {
        for (Match match : Praxi.get().getCache().getMatches()) {
            match.getPlacedBlocks().forEach(location -> location.getBlock().setType(Material.AIR));
            match.getChangedBlocks().forEach((blockState) -> blockState.getLocation().getBlock().setType(blockState.getType()));
            match.getDroppedItems().forEach(Entity::remove);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Profile profile = Profile.getByUuid(player.getUniqueId());
            profile.save();
        }
    }

    public static int getInFightsCount(Queue queue) {
        int i = 0;

        for (Match match : Praxi.get().getCache().getMatches()) {
            if (match.getQueue() != null &&
                    (match.getState() == MatchState.STARTING_ROUND || match.getState() == MatchState.PLAYING_ROUND)) {
                if (match.getQueue().equals(queue)) {
                    for (GameParticipant<? extends GamePlayer> gameParticipant : match.getParticipants()) {
                        i += gameParticipant.getPlayers().size();
                    }
                }
            }
        }

        return i;
    }

    public static BaseComponent[] generateInventoriesComponents(String prefix, GameParticipant<MatchGamePlayer> participant) {
        return generateInventoriesComponents(prefix, Collections.singletonList(participant));
    }

    public static BaseComponent[] generateInventoriesComponents(String prefix, List<GameParticipant<MatchGamePlayer>> participants) {
        ChatComponentBuilder builder = new ChatComponentBuilder(prefix);

        int totalPlayers = 0;
        int processedPlayers = 0;

        for (GameParticipant<MatchGamePlayer> gameParticipant : participants) {
            totalPlayers += gameParticipant.getPlayers().size();
        }

        for (GameParticipant<MatchGamePlayer> gameParticipant : participants) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                processedPlayers++;

                ChatComponentBuilder current = new ChatComponentBuilder(
						Locale.MATCH_CLICK_TO_VIEW_NAME.format(gamePlayer.getUsername()))
						.attachToEachPart(ChatHelper.hover(Locale.MATCH_CLICK_TO_VIEW_HOVER.format(gamePlayer.getUsername())))
                        .attachToEachPart(ChatHelper.click("/viewinv " + gamePlayer.getUuid().toString()));

                builder.append(current.create());

                if (processedPlayers != totalPlayers) {
                    builder.append(", ");
                    builder.getCurrent().setClickEvent(null);
                    builder.getCurrent().setHoverEvent(null);
                }
            }
        }

        return builder.create();
    }

    public void setupPlayer(Player player) {
        // Set the player as alive
        MatchGamePlayer gamePlayer = getGamePlayer(player);
        gamePlayer.setDead(false);

        // If the player disconnected, skip any operations for them
        if (gamePlayer.isDisconnected()) {
            return;
        }

        // Reset the player's inventory
        PlayerUtil.reset(player);

        Profile profile = Profile.getByUuid(player.getUniqueId());

        // Set the player's max damage ticks
        player.setMaximumNoDamageTicks(getKit().getGameRules().getHitDelay());

	    // Set the player's knockback
	    if (getKit().getKnockbackProfile() != null) {
			SpigotManager.getSpigot().setKnockback(player, getKit().getKnockbackProfile());
		}

        // If the player has no kits, apply the default kit, otherwise
        // give the player a list of kit books to choose from
        if (!getKit().getGameRules().isSumo()) {
            ProfileKitData kitData = profile.getKitData().get(getKit());

            if (kitData.getKitCount() > 0) {
                profile.getKitData().get(getKit()).giveBooks(player);
            } else if (getKit().getGameRules().isBedfight()) {
                player.getInventory().setContents(getKit().getKitLoadout().getContents());
                KitUtils.giveBedFightKit(player);
                player.sendMessage(Locale.MATCH_GIVE_KIT.format(player, "Default", getKit().getName()));
                player.updateInventory();
                profile.getMatch().getGamePlayer(player).setKitLoadout(kit.getKitLoadout());
            } else if (getKit().getGameRules().isBridge() || getKit().getGameRules().isBattleRush()) {
                player.getInventory().setContents(getKit().getKitLoadout().getContents());
                KitUtils.giveBridgeKit(player);
                player.sendMessage(Locale.MATCH_GIVE_KIT.format(player, "Default", getKit().getName()));
                player.updateInventory();
                profile.getMatch().getGamePlayer(player).setKitLoadout(kit.getKitLoadout());
            } else if (getKit().getGameRules().isStickFight()) {
                player.getInventory().setContents(getKit().getKitLoadout().getContents());
                KitUtils.giveLivesKit(player);
                player.sendMessage(Locale.MATCH_GIVE_KIT.format(player, "Default", getKit().getName()));
                player.updateInventory();
                profile.getMatch().getGamePlayer(player).setKitLoadout(kit.getKitLoadout());
            } else {
                player.getInventory().setArmorContents(getKit().getKitLoadout().getArmor());
                player.getInventory().setContents(getKit().getKitLoadout().getContents());
				player.sendMessage(Locale.MATCH_GIVE_KIT.format(player, "Default", getKit().getName()));

                player.updateInventory();
                profile.getMatch().getGamePlayer(player).setKitLoadout(kit.getKitLoadout());
            }
        }

        // some replay stuffs
        if (Praxi.get().isReplay() && !kit.getGameRules().isBuild()) {
                if (ReplaySaver.exists(profile.getUuid().toString())) {
                    ReplaySaver.delete(profile.getUuid().toString());
            }
        }
    }

    public Player getOpponent(Player player) {
        GameParticipant<MatchGamePlayer> playerParticipant = getParticipant(player);
        if (playerParticipant != null) {
            for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
                if (!gameParticipant.equals(playerParticipant)) {
                    for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                        if (!gamePlayer.isDisconnected()) {
                            return gamePlayer.getPlayer();
                        }
                    }
                }
            }
        }
        return null;
    }

	public Player getOpponentUUID(UUID playerUUID) {
        GameParticipant<MatchGamePlayer> playerParticipant = getParticipant(Bukkit.getPlayer(playerUUID));
        if (playerParticipant == null) {
            return null;
        }

        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            if (!gameParticipant.equals(playerParticipant)) {
                for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                    if (!gamePlayer.isDisconnected()) {
                        return gamePlayer.getPlayer();
                    }
                }
            }
        }
        return null;
    }


    public ArrayList<Player> getOpponent(Player player, boolean yes) {
        yes = false;
        ArrayList<Player> players = new ArrayList<>();
        GameParticipant<MatchGamePlayer> playerParticipant = getParticipant(player);
        if (playerParticipant != null) {
            for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
                if (!gameParticipant.equals(playerParticipant)) {
                    for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                        if (!gamePlayer.isDisconnected()) {
                            players.add(player);
                        }
                    }
                }
                return players;
            }
        }
        return null;
    }

    public void start() {
        // Set state
        state = MatchState.STARTING_ROUND;

        // Start logic task
        logicTask = new MatchLogicTask(this);
        logicTask.runTaskTimer(Praxi.get(), 0L, 20L);

        // Set arena as active
        arena.setActive(true);

        // Send arena message
        sendMessage(Locale.MATCH_PLAYING_ARENA.format(arena.getName()));

        // Setup players
	 	for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
         for (MatchGamePlayer participantGamePlayer : gameParticipant.getPlayers()) {
             Player player = participantGamePlayer.getPlayer();

             if (player != null) {
                 Profile profile = Profile.getByUuid(player.getUniqueId());
                 profile.setState(ProfileState.FIGHTING);
                 profile.getDuelRequests().clear();
                 profile.setMatch(this);
                 Match match = profile.getMatch();

                 setupPlayer(player);
            
                 if (Praxi.get().isReplay()) {
                 ReplayAPI.getInstance().recordReplay(
                       profile.getUuid().toString(), 
                       getParticipants().stream()
                          .flatMap(participant -> participant.getPlayers().stream())
                          .filter(mpGamePlayer -> !mpGamePlayer.isDisconnected())
                          .map(MatchGamePlayer::getPlayer)
                          .collect(Collectors.toList())
                      );
                  }
              }
           }
        }

        // Handle player visibility
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                Player player = gamePlayer.getPlayer();

                if (player != null) {
                    VisibilityLogic.handle(player);
                }
				if (kit.getGameRules().isBuild()) {
                    arena.takeSnapshot();
                }
            }
        }
    }

    public void end() {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player player = gamePlayer.getPlayer();
                    if (player != null) {
                        player.setFireTicks(0);
                        player.updateInventory();

                        Profile profile = Profile.getByUuid(player.getUniqueId());
                        Player opponent = this.getOpponent(player);
                        String rivalName = (opponent != null) ? opponent.getName() : "null";
                        String matchKitName = this.getKit().getName();
                        profile.setState(ProfileState.LOBBY);
						PlayerUtil.allowMovement(player);
                        profile.setMatch(null);
                        profile.setEnderpearlCooldown(new Cooldown(0));
                        if (Praxi.get().isReplay() && !kit.getGameRules().isBuild()) {
                            ReplayAPI.getInstance().stopReplay(profile.getUuid().toString(), true, true, "Test", matchKitName, rivalName);
                       }
					}
                } else {
                    Player player = gamePlayer.getPlayer();
                    if (player != null) {
                        player.setFireTicks(0);
                        player.updateInventory();


                        Profile profile = Profile.getByUuid(player.getUniqueId());
                        profile.setState(ProfileState.LOBBY);
                        profile.setMatch(null);
                        profile.setEnderpearlCooldown(new Cooldown(0));
						PlayerUtil.allowMovement(player);
                    }
                }
            }
        }

        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player player = gamePlayer.getPlayer();

                    if (player != null) {
                        VisibilityLogic.handle(player);
                        Hotbar.giveHotbarItems(player);
                        Praxi.get().getEssentials().teleportToSpawn(player);
                    }
                }
            }
        }

        for (Player player : getSpectatorsAsPlayers()) {
            removeSpectator(player);
        }

        Praxi.get().getCache().getMatches().remove(this);
    }

    public abstract boolean canEndMatch();

   public void onRoundStart() {
		// Reset snapshots
		snapshots.clear();

        // Check for followers
        checkFollowers();

		// Reset each game participant
		for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
		 for (GamePlayer gamePlayer : gameParticipant.getPlayers()) {
		   	PlayerUtil.allowMovement(gamePlayer.getPlayer());
			gameParticipant.reset();
		  }
		}

		// Set time data
		timeData = System.currentTimeMillis();
	}

    public abstract boolean canStartRound();

    public void onRoundEnd() {
        // Snapshot alive players' inventories
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player player = gamePlayer.getPlayer();
                    //Profile playerProfile = Profile.getByUuid(player.getUniqueId());

                    if (player != null) {
                        if (!gamePlayer.isDead()) {
                            MatchSnapshot snapshot = new MatchSnapshot(player, this.getOpponent(player), false);
                            snapshot.setPotionsThrown(gamePlayer.getPotionsThrown());
                            snapshot.setPotionsMissed(gamePlayer.getPotionsMissed());
                            snapshot.setLongestCombo(gamePlayer.getLongestCombo());
                            snapshot.setTotalHits(gamePlayer.getHits());

                            snapshots.add(snapshot);
                        }
                    }
                }
            }
        }

        // Make all snapshots available
        for (MatchSnapshot snapshot : snapshots) {
            snapshot.setCreatedAt(System.currentTimeMillis());
            MatchSnapshot.getSnapshots().put(snapshot.getUuid(), snapshot);
        }

        List<BaseComponent[]> endingMessages = generateEndComponents();

        // Send ending messages to game participants
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player player = gamePlayer.getPlayer();

                    if (player != null) {
                        for (BaseComponent[] components : endingMessages) {
                            player.sendMessage(components);
                        }
                    }
                }
            }
        }

        // Send ending messages to spectators
        for (Player player : getSpectatorsAsPlayers()) {
            for (BaseComponent[] components : endingMessages) {
                player.spigot().sendMessage(components);
            }

            removeSpectator(player);
        }
    }

	public void respawn(UUID playerUUID) {
        Profile profile = Profile.getByUuid(playerUUID);
        Match match = profile.getMatch();
        Player player = Bukkit.getPlayer(playerUUID);
        MatchGamePlayer gamePlayer = profile.getMatch().getGamePlayer(player);
        boolean aTeam = getParticipantA().containsPlayer(player.getUniqueId());
        boolean bedGone = aTeam ? bedABroken : bedBBroken;
        Location spawn = aTeam ? getArena().getSpawnA() : getArena().getSpawnB();
        String playerColor = aTeam ? "&c" : "&9";
        if (gamePlayer.isRespawned()) {
            return;
        }
        if (state != MatchState.PLAYING_ROUND) {
            return;
        }
        hidePlayer(playerUUID);
        gamePlayer.setRespawned(true);

        Player killer = PlayerUtil.getLastAttacker(player);
        if (PlayerUtil.getLastAttacker(player) != null) {
            Profile killerProfile = Profile.getByUuid(killer.getUniqueId());
            killerProfile.getKitData().get(killerProfile.getMatch().getKit()).incrementKills();
            if (aTeam) {
                this.playerBKills++;
            } else {
                this.playerAKills++;
            }
        }
        sendDeathMessage(player, player, killer);

        player.teleport(spawn);

        PlayerUtil.setLastAttacker(player, null); // this will fix some shit

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setHealth(20);

        PlayerUtil.enableSpectator(player);

        new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendMessage(CC.translate(Praxi.get().getMainConfig().getString("MATCH.RESPAWN_TIMER")
                        .replace("{countdown}", String.valueOf(countdown))
                        .replace("{theme}", "&" + String.valueOf(profile.getOptions().theme().getColor().getChar()))));
                    PlayerUtil.sendTitle(player, CC.translate("&" + profile.getOptions().theme().getColor().getChar()) + ("&c&lYOU DIED"), "&fRespawning in " + countdown + "...", 20);
                    Location spawn = aTeam ? getArena().getSpawnA() : getArena().getSpawnB();
                    if (!gamePlayer.isRespawned()) {
                        gamePlayer.setRespawned(false);
                        this.cancel();
                    }
                    if (match == null || state == MatchState.ENDING_MATCH) {
                        this.cancel();
                    }
                    countdown--;
                } else {
                    player.playSound(player.getLocation(), Sound.FALL_BIG, 1.0f, 1.0f);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    boolean aTeam = getParticipantA().containsPlayer(player.getUniqueId());
					Location spawn = aTeam ? getArena().getSpawnA() : getArena().getSpawnB();
                    player.teleport(spawn);
                    if (getKit().getGameRules().isBedfight()) {
                        player.getInventory().setContents(getKit().getKitLoadout().getContents());
                        KitUtils.giveBedFightKit(player);
                    } else if (getKit().getGameRules().isBridge() || getKit().getGameRules().isBattleRush()) {
                        player.getInventory().setContents(getKit().getKitLoadout().getContents());
                        KitUtils.giveBridgeKit(player);
                    } else if (getKit().getGameRules().isStickFight()) {
                        player.getInventory().setContents(getKit().getKitLoadout().getContents());
                        KitUtils.giveLivesKit(player);
                    } else {
                        player.getInventory().setArmorContents(getKit().getKitLoadout().getArmor());
                        player.getInventory().setContents(getKit().getKitLoadout().getContents());
                    }
                    player.updateInventory();
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setFoodLevel(20);
                    player.setHealth(20);
                    showPlayer(playerUUID);
                    gamePlayer.setRespawned(false);
                    if (Praxi.get().getSettingsConfig().getBoolean("MATCH.RESPAWN-TITLE")) { // send respawn title if it's enabled from config
                        PlayerUtil.sendTitle(player, CC.translate("&aRespawned!"), "", 20);
                    }
                    if (Praxi.get().getSettingsConfig().getBoolean("MATCH.BROADCAST-RESPAWN")) { // send broadcast respawn message if it's enabled from config
                        broadcast(playerColor + player.getName() + " &aRespawned");
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(Praxi.get(), 0L, 20L);
    }

    public void newRound(UUID playerUUID) {
      Profile profile = Profile.getByUuid(playerUUID);
      Player player = Bukkit.getPlayer(playerUUID);
      MatchGamePlayer gamePlayer = profile.getMatch().getGamePlayer(player);
      boolean aTeam = getParticipantA().containsPlayer(player.getUniqueId());
      Location spawn = aTeam ? getArena().getSpawnA() : getArena().getSpawnB();
      if (Praxi.get().getSettingsConfig().getBoolean("MATCH.CLEAR-BLOCKS")) { // restore snapshot if clear blocks is enabled
          cleanupThis(this);
          arena.restoreSnapshot();
	  }
      player.getInventory().clear();
      player.getInventory().setArmorContents(null);
      player.updateInventory();
      player.setFoodLevel(20);
      player.setHealth(20);
      player.teleport(spawn);
      player.getInventory().setContents(getKit().getKitLoadout().getContents());
      KitUtils.giveBridgeKit(player);
      player.updateInventory();
      PlayerUtil.denyMovement(player);
      new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (countdown > 0) {
                    /*player.sendMessage(CC.translate(Praxi.get().getMainConfig().getString("MATCH.BRIDGE_STARTING")
                        .replace("{countdown}", String.valueOf(countdown))
                        .replace("{theme}", "&" + String.valueOf(profile.getOptions().theme().getColor().getChar()))));*/
                    PlayerUtil.sendTitle(player, CC.translate("&" + profile.getOptions().theme().getColor().getChar()) + ("&lStarting"), "&fin " + countdown + "...", 20);
                    countdown--;
                } else {
                    PlayerUtil.allowMovement(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(Praxi.get(), 0L, 20L);
    }

    public void newRoundStickFight(UUID playerUUID) {
      Profile profile = Profile.getByUuid(playerUUID);
      Player player = Bukkit.getPlayer(playerUUID);
      MatchGamePlayer gamePlayer = profile.getMatch().getGamePlayer(player);
      boolean aTeam = getParticipantA().containsPlayer(player.getUniqueId());
      Location spawn = aTeam ? getArena().getSpawnA() : getArena().getSpawnB();
      cleanupThis(this);
      arena.restoreSnapshot();
      player.getInventory().clear();
      player.getInventory().setArmorContents(null);
      player.updateInventory();
      player.setFoodLevel(20);
      player.setHealth(20);
      player.teleport(spawn);
      player.getInventory().setContents(getKit().getKitLoadout().getContents());
      KitUtils.giveLivesKit(player);
      player.updateInventory();
      PlayerUtil.denyMovement(player);
      new BukkitRunnable() {
            int countdown = 3;

            @Override
            public void run() {
                if (countdown > 0) {
                    PlayerUtil.sendTitle(player, CC.translate("&" + profile.getOptions().theme().getColor().getChar()) + ("&lStarting"), "&fin " + countdown + "...", 20);
                    countdown--;
                } else {
                    PlayerUtil.allowMovement(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(Praxi.get(), 0L, 20L);
    }

	public void broadcast(String message) {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                gamePlayer.getPlayer().sendMessage(CC.translate(message));
            }
        }

        for (Player player : getSpectatorsAsPlayers()) {
            player.sendMessage(CC.translate(message));
        }
    }

	public void hidePlayer(UUID playerUUID) {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                gamePlayer.getPlayer().hidePlayer(Bukkit.getPlayer(playerUUID));
            }
        }

        for (Player player : getSpectatorsAsPlayers()) {
            player.hidePlayer(Bukkit.getPlayer(playerUUID));
        }
    }

	public void showPlayer(UUID playerUUID) {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                gamePlayer.getPlayer().showPlayer(Bukkit.getPlayer(playerUUID));
            }
        }

        for (Player player : getSpectatorsAsPlayers()) {
            player.showPlayer(Bukkit.getPlayer(playerUUID));
        }
    }

    public abstract boolean canEndRound();

    public void onDisconnect(Player dead) {
        // Don't continue if the match is already ending
        if (!(state == MatchState.STARTING_ROUND || state == MatchState.PLAYING_ROUND)) {
            return;
        }

        MatchGamePlayer deadGamePlayer = getGamePlayer(dead);
		Player killer = PlayerUtil.getLastAttacker(dead);

        if (deadGamePlayer != null) {
            deadGamePlayer.setDisconnected(true);

            if (!deadGamePlayer.isDead()) {
                onDeath(dead);
            }
        }
    }

	public void onDeath(Player dead) {
        // Don't continue if the match is already ending
        if (!(state == MatchState.STARTING_ROUND || state == MatchState.PLAYING_ROUND)) {
            return;
        }

        MatchGamePlayer deadGamePlayer = getGamePlayer(dead);

        Player killer = PlayerUtil.getLastAttacker(dead);
        Player rival = this.getOpponent(dead);

        // Store snapshot of player inventory and stats
        MatchSnapshot snapshot = new MatchSnapshot(dead, rival, true);
        snapshot.setPotionsMissed(deadGamePlayer.getPotionsMissed());
        snapshot.setPotionsThrown(deadGamePlayer.getPotionsThrown());
        snapshot.setLongestCombo(deadGamePlayer.getLongestCombo());
        snapshot.setTotalHits(deadGamePlayer.getHits());
        Profile loserProfile = Profile.getByUuid(deadGamePlayer.getUuid());
        PlayerUtil.sendTitle(dead, CC.translate("&cDEFEAT!"), "&fgit gud!", 70);
        if (!Praxi.get().getCache().getMatch(matchId).isDuel() || loserProfile.getParty() == null) {
            loserProfile.getKitData().get(loserProfile.getMatch().getKit()).incrementLost();
            loserProfile.getKitData().get(loserProfile.getMatch().getKit()).resetWinStreak();
			loserProfile.setCoins(loserProfile.getCoins() + loserCoins);
			loserProfile.setExperience(loserProfile.getExperience() + loserExp);
			dead.sendMessage(CC.translate("&b(XP) &fYou received &b" + loserExp + " &fexperience!"));
			dead.sendMessage(CC.translate("&e(Coins) &fYou received &e" + loserCoins + " &fcoins!"));
        }
        dead.setItemInHand(null);

        // Add snapshot to list
        snapshots.add(snapshot);

        // Don't continue if the player is already dead
        if (deadGamePlayer.isDead()) {
            return;
        }

        // Set player as dead
        deadGamePlayer.setDead(true);

        PlayerUtil.reset(dead);

        PlayerUtil.doVelocityChange(dead);


        // Handle visibility for match players
        // Send death message
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player player = gamePlayer.getPlayer();

                    if (player != null) {
                        VisibilityLogic.handle(player, dead);
                        if (player != dead) {
                            Profile winnerProfile = Profile.getByUuid(player.getUniqueId());
							SpecialEffects specialEffect = winnerProfile.getOptions().killEffect();
							if (specialEffect != null && !specialEffect.getName().equalsIgnoreCase("none")) {
								playKillEffect(dead, this, specialEffect);
							}
                            PlayerUtil.sendTitle(player, CC.translate("&aVICTORY!"), " &fGood Game.", 70);
                            if (killer != null) {
                                Profile killerProfile = Profile.getByUuid(killer.getUniqueId());
                                killerProfile.getKitData().get(killerProfile.getMatch().getKit()).incrementKills();
                                killer.playSound(player.getLocation(), Sound.EXPLODE, 1.0f, 1.0f);
                                PlayerUtil.doVelocityChange(player);
                                PlayerUtil.setLastAttacker(dead, null);
                            }
                            if (!Praxi.get().getCache().getMatch(matchId).isDuel() || winnerProfile.getParty() == null) {
                                winnerProfile.getKitData().get(winnerProfile.getMatch().getKit()).incrementWon();
                                winnerProfile.getKitData().get(winnerProfile.getMatch().getKit()).incrementWinStreak();
								winnerProfile.setExperience(winnerProfile.getExperience() + winnerExp);
								winnerProfile.setCoins(winnerProfile.getCoins() + winnerCoins);
								player.sendMessage(CC.translate("&b(XP) &fYou received &b" + winnerExp + " &fexperience!"));
								player.sendMessage(CC.translate("&e(Coins) &fYou received &e" + winnerCoins + " &fcoins!"));
                            }
                        }
                    }
                }
            }
        }

        // Handle visibility for spectators
        // Send death message
        // Restore arena snapshot
        for (Player player : getSpectatorsAsPlayers()) {
            VisibilityLogic.handle(player, dead);
        }
        if (kit.getGameRules().isBuild()) {
            cleanupThis(this);
            arena.restoreSnapshot();
            arena.setActive(false);
        }
		for (Player player : getSpectatorsAsPlayers()) {
			VisibilityLogic.handle(player, dead);
            Player realKiller = PlayerUtil.getLastAttacker(dead);
            if (realKiller != null) {
			    sendDeathMessage(player, dead, realKiller);
            } else {
                sendDeathMessage(player, dead, null);
            }
			if (killer != null) {
				Profile winnerProfile = Profile.getByUuid(killer.getUniqueId());
				SpecialEffects specialEffect = winnerProfile.getOptions().killEffect();
				if (specialEffect != null && !specialEffect.getName().equalsIgnoreCase("none")) {
					playKillEffect(dead, this, specialEffect);
				}
			}
		}


        if (canEndRound()) {
            state = MatchState.ENDING_ROUND;
            timeData = System.currentTimeMillis() - timeData;
            onRoundEnd();

            if (canEndMatch()) {
                state = MatchState.ENDING_MATCH;
                logicTask.setNextAction(4);
            } else {
                logicTask.setNextAction(4);
            }
        } else {
            Praxi.get().getHotbar().giveHotbarItems(dead);
        }
    }

	public void playKillEffect(Player player, Match match, SpecialEffects specialEffect) {
		for (GameParticipant<MatchGamePlayer> matchTeam : match.getParticipants()) {
			for (MatchGamePlayer matchGamePlayer : matchTeam.getPlayers()) {
				specialEffect.getCallable().call(player, matchGamePlayer.getPlayer());
			}
			for (UUID uuid : match.getSpectators()) {
				specialEffect.getCallable().call(player, Bukkit.getPlayer(uuid));
			}
		}
	}

    public abstract boolean isOnSameTeam(Player first, Player second);

    public abstract List<GameParticipant<MatchGamePlayer>> getParticipants();

    public GameParticipant<MatchGamePlayer> getParticipant(Player player) {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            if (gameParticipant.containsPlayer(player.getUniqueId())) {
                return gameParticipant;
            }
        }

        return null;
    }

	public GameParticipant<MatchGamePlayer> getParticipantB() {
        return getParticipants().get(1);
    }

    public MatchGamePlayer getGamePlayer(Player player) {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (gamePlayer.getUuid().equals(player.getUniqueId())) {
                    return gamePlayer;
                }
            }
        }

        return null;
    }

    public abstract ChatColor getRelationColor(Player viewer, Player target);

    public void addSpectator(Player spectator, Player target) {
        spectators.add(spectator.getUniqueId());

        Profile profile = Profile.getByUuid(spectator.getUniqueId());
        profile.setMatch(this);
        profile.setState(ProfileState.SPECTATING);

        Hotbar.giveHotbarItems(spectator);

        spectator.teleport(target.getLocation().clone().add(0, 2, 0));
        spectator.setGameMode(GameMode.SURVIVAL);
        spectator.setAllowFlight(true);
        spectator.setFlying(true);
        spectator.updateInventory();

        VisibilityLogic.handle(spectator);

        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (GamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player bukkitPlayer = gamePlayer.getPlayer();
                    Profile bukkitPlayerProfile = Profile.getByUuid(bukkitPlayer.getUniqueId());

                    if (bukkitPlayer != null) {
                        VisibilityLogic.handle(bukkitPlayer);
                        if (!spectator.hasPermission("shadow.spectate.bypass") || bukkitPlayerProfile.getOptions().spectatorMessages()) { // Don't show spectate message if player has this perm (can be used to check for cheaters)
                            bukkitPlayer.sendMessage(Locale.MATCH_NOW_SPECTATING.format(bukkitPlayer, spectator.getName()));
                        }
                    }
                }
            }
        }
    }

    public void removeSpectator(Player spectator) {
        spectators.remove(spectator.getUniqueId());

        Profile profile = Profile.getByUuid(spectator.getUniqueId());
        profile.setState(ProfileState.LOBBY);
        profile.setMatch(null);

        PlayerUtil.reset(spectator);
        Hotbar.giveHotbarItems(spectator);
        Praxi.get().getEssentials().teleportToSpawn(spectator);

        VisibilityLogic.handle(spectator);

        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player bukkitPlayer = gamePlayer.getPlayer();
                    Profile bukkitPlayerProfile = Profile.getByUuid(bukkitPlayer.getUniqueId());

                    if (bukkitPlayer != null) {
                        VisibilityLogic.handle(bukkitPlayer);

                        if (state != MatchState.ENDING_MATCH) {
                            if (!spectator.hasPermission("shadow.spectate.bypass") || bukkitPlayerProfile.getOptions().spectatorMessages()) { // Don't show spectate message if player has this perm (can be used to check for cheaters)
                                bukkitPlayer.sendMessage(Locale.MATCH_NO_LONGER_SPECTATING.format(bukkitPlayer, spectator.getName()));
                            }
                        }
                    }
                }
            }
        }
    }

    public String getDuration() {
        if (state.equals(MatchState.STARTING_ROUND)) {
            return "Starting"; // Default Praxi "00:00"
        } else if (state.equals(MatchState.ENDING_ROUND)) {
            return "Ending";
        } else if (state.equals(MatchState.PLAYING_ROUND)) {
            return TimeUtil.millisToTimer(System.currentTimeMillis() - this.timeData);
        }
        return "Ending";
    }

    public void sendMessage(String message) {
        for (GameParticipant gameParticipant : getParticipants()) {
            gameParticipant.sendMessage(message);
        }

        for (Player player : getSpectatorsAsPlayers()) {
            ArrayList<String> list = new ArrayList<>();
            list.add(CC.translate(message));
            player.sendMessage(ReplaceUtil.format(list, player).toString().replace("[", "").replace("]", ""));
        }
    }

	public void sendTitleA(String header, String footer, int duration) {
        getParticipantA().sendTitle(header, footer, duration);
    }

    public void sendTitleB(String header, String footer, int duration) {
        getParticipantB().sendTitle(header, footer, duration);
    }

    public void sendSound(Sound sound, float volume, float pitch) {
        for (GameParticipant gameParticipant : getParticipants()) {
            gameParticipant.sendSound(sound, volume, pitch);
        }

        for (Player player : getSpectatorsAsPlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

	public GameParticipant<MatchGamePlayer> getParticipantA() {
        return getParticipants().get(0);
    }

    protected List<Player> getSpectatorsAsPlayers() {
        List<Player> players = new ArrayList<>();

        for (UUID uuid : spectators) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    public abstract List<BaseComponent[]> generateEndComponents();

    public void sendDeathPackets(Player player, Location location) {
        PacketContainer lightningPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_WEATHER);
        lightningPacket.getIntegers().write(0, -500)
                .write(1, MathHelper.floor(location.getX() * 32.0D))
                .write(2, MathHelper.floor(location.getX() * 32.0D))
                .write(3, MathHelper.floor(location.getX() * 32.0D))
                .write(4, 0);

        PacketContainer statusPacket = new PacketContainer(PacketType.Play.Server.ENTITY_STATUS);
        statusPacket.getIntegers().write(0, player.getEntityId());
        statusPacket.getBytes().write(0, (byte) 3);

        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (GamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!gamePlayer.isDisconnected()) {
                    Player bukkitPlayer = gamePlayer.getPlayer();

                    if (bukkitPlayer != null) {
                        try {
                            ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, lightningPacket);

                            if (!bukkitPlayer.equals(player)) {
                                ProtocolLibrary.getProtocolManager().sendServerPacket(bukkitPlayer, statusPacket);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        bukkitPlayer.playSound(location, Sound.AMBIENCE_THUNDER, 1.0F, 1.0F);
                    }
                }
            }
        }

        for (Player spectator : getSpectatorsAsPlayers()) {
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(spectator, lightningPacket);

                if (!spectator.equals(player)) {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(spectator, statusPacket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            spectator.playSound(location, Sound.AMBIENCE_THUNDER, 1.0F, 1.0F);
        }
    }

	public void sendDeathMessage(Player player, Player dead, Player killer) {
		String deathMessage;

		if (killer == null) {
			Profile deadprofile = Profile.getByUuid(dead.getUniqueId());
			deathMessage = deadprofile.getOptions().killMessage().getCallable().getFormatted(dead.getName(), dead.getName(), false);
		} else {
			Profile killerProfile = Profile.getByUuid(killer.getUniqueId());
			deathMessage = killerProfile.getOptions().killMessage().getCallable().getFormatted(dead.getName(), killer.getName(), true);
		}

		broadcast(deathMessage);
	}

	public void sendTitle(String header, String footer, int duration) {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (MatchGamePlayer gamePlayer : gameParticipant.getPlayers()) {
                PlayerUtil.sendTitle(gamePlayer.getPlayer(), header, footer, duration);
            }
        }
    }

    public int getRivalCountsBridge(Player player) {
        Profile profile = Profile.getByUuid(player.getUniqueId());
		Match match = profile.getMatch();
        Player rival = match.getOpponent(player);
        Profile rivalProfile = Profile.getByUuid(rival.getUniqueId());
        boolean isRivalATeam = match.getParticipantA().containsPlayer(rival.getUniqueId());
        if (isRivalATeam) {
            return portalCountA;
        } else {
            return portalCountB;
        }
    }

    public void checkFollowers() {
        for (GameParticipant<MatchGamePlayer> gameParticipant : getParticipants()) {
            for (GamePlayer gamePlayer : gameParticipant.getPlayers()) {
                if (!Profile.getByUuid(gamePlayer.getUuid()).getFollowers().isEmpty()) {
                    for (UUID playerUUID : Profile.getByUuid(gamePlayer.getUuid()).getFollowers()) {
                        Bukkit.getPlayer(playerUUID).chat("/spec " + gamePlayer.getUsername());
                    }
                }
            }
        }
    }
}