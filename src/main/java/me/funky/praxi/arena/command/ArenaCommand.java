package me.funky.praxi.arena.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.funky.praxi.Praxi;
import me.funky.praxi.arena.Arena;
import me.funky.praxi.arena.ArenaType;
import me.funky.praxi.arena.runnables.StandalonePasteRunnable;
import me.funky.praxi.arena.generator.ArenaGenerator;
import me.funky.praxi.arena.generator.Schematic;
import me.funky.praxi.arena.impl.SharedArena;
import me.funky.praxi.arena.impl.StandaloneArena;
import me.funky.praxi.arena.selection.Selection;
import me.funky.praxi.kit.Kit;
import me.funky.praxi.util.CC;
import me.funky.praxi.util.ChatComponentBuilder;
import me.funky.praxi.util.ChatHelper;
import me.funky.praxi.util.TaskUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Objects;

@CommandAlias("arena")
@CommandPermission("shadow.admin.arena")
@Description("Command to manage arenas.")
public class ArenaCommand extends BaseCommand {

    @HelpCommand
    @Syntax("[page]")
    public void help(Player player, CommandHelp help) {
        help.showHelp();
    }

	@Subcommand("create")
    @Syntax("<arena> <arenaType>")
    public void create(Player player, String arenaName, ArenaType arenaType) {
        if (Arena.getByName(arenaName) == null) {
            Selection selection = Selection.createOrGetSelection(player);

            if (selection.isFullObject()) {
                Arena arena;
                if (arenaType.equals(ArenaType.SHARED)) {
                    arena = new SharedArena(arenaName, selection.getPoint1(), selection.getPoint2());
                } else {
                    arena = new StandaloneArena(arenaName, selection.getPoint1(), selection.getPoint2());
                }
                Arena.getArenas().add(arena);

                player.sendMessage(CC.translate("&fCreated new arena&c " + arenaName));
            } else {
                player.sendMessage(CC.translate("&cYour selection is incomplete."));
            }
        } else {
            player.sendMessage(CC.translate("&cAn arena with that name already exists."));
        }
    }

	@Subcommand("delete")
	@CommandCompletion("@arenas")
    @Syntax("<arena>")
    public void delete(Player player, String arenaName) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exist!"));
            return;
        }
        Arena arena = Arena.getByName(arenaName);
        arena.delete();

        player.sendMessage(CC.translate("&fDeleted arena&c " + arenaName));
    }

	@Subcommand("save")
    public void save(Player player) {
        for (Arena arena : Arena.getArenas()) {
            arena.save();
        }

        player.sendMessage(CC.GREEN + "Saved all arenas!");
    }

	@Subcommand("wand")
    public void wand(Player player) {
        if (player.getInventory().first(Selection.SELECTION_WAND) != -1) {
            player.getInventory().remove(Selection.SELECTION_WAND);
        } else {
            player.getInventory().addItem(Selection.SELECTION_WAND);
        }
        player.sendMessage(CC.translate("&aSuccessfully given selection wand!"));
        player.updateInventory();
    }

	@Subcommand("setspawn")
    @Syntax("<arena> <pos>")
    public void setSpawn(Player player, String arenaName, String pos) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exists!"));
            return;
        }
        Arena arena = Arena.getByName(arenaName);
        if (pos.equals("A") || pos.equals("a") || pos.equals("first")) {
            arena.setSpawnA(player.getLocation());
        } else if (pos == "B" || pos == "b" || pos == "second") {
            arena.setSpawnB(player.getLocation());
	    } else {
            player.sendMessage(CC.translate("&cInvalid pos please select A and B"));
        }

        arena.save();

        player.sendMessage(CC.GREEN + "Updated spawn point " + pos + " for arena " + arena.getName());
    }

    @Subcommand("setduplicatespawn")
    @Syntax("<arena> <pos> <number>")
    public void setDuplicateSpawn(Player player, String arenaName, String pos, Integer number) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exists!"));
            return;
        }
        Arena arena = Arena.getByName(arenaName);
        Arena duplicate = ((StandaloneArena)arena).getDuplicates().get(number - 2);
        if (duplicate != null) {
            Location spawn = new Location(player.getLocation().getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
            if (pos == "A" || pos == "a" || pos == "first") {
                duplicate.setSpawnA(spawn);
            } else if (pos == "B" || pos == "b" || pos == "second") {
                duplicate.setSpawnB(spawn);
            } else {
                player.sendMessage(CC.translate("&cInvalid pos please select A and B"));
            }
            player.sendMessage(CC.GREEN + "Updated spawn point " + pos + " for arena " + arena.getName());
            arena.save();
        }
    }

	@Subcommand("addKit")
	@CommandCompletion("@arenas @kits")
    @Syntax("<arena> <kits>")
    public void addKit(Player player, String arenaName, String kitName) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exists!"));
            return;
        }
        if (!Kit.getKits().contains(Kit.getByName(kitName))) {
            player.sendMessage(CC.translate("&cKit doesn't exists!"));
            return;
        }
        Arena arena = Arena.getByName(arenaName);
        Kit kit = Kit.getByName(kitName);
        if (kit == null) return;
        if (arena == null) return;

        arena.getKits().add(kit.getName());
        arena.save();

        player.sendMessage(CC.GREEN + "Added kit " + kit.getName() +
                " to arena " + arena.getName());
    }

	@Subcommand("removekit")
	@CommandCompletion("@arenas @kits")
    @Syntax("<arena> <kits>")
    public void removeKit(Player player, String arenaName, String kitName) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exists!"));
            return;
        }
        if (!Kit.getKits().contains(Kit.getByName(kitName))) {
            player.sendMessage(CC.translate("&cKit doesn't exists!"));
            return;
        }
        Arena arena = Arena.getByName(arenaName);
        Kit kit = Kit.getByName(kitName);
        if (kit == null) return;
        if (arena == null) return;

        arena.getKits().remove(kit.getName());
        arena.save();

        player.sendMessage(CC.WHITE + "Removed kit " + CC.AQUA + kit.getName() + CC.WHITE +
                " from arena " + arena.getName());
    }

	@Subcommand("status")
	@CommandCompletion("@arenas")
    @Syntax("<arena>")
    public void status(Player player, String arenaName) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exists!"));
            return;
        }
        Arena arena = Arena.getByName(arenaName);
        player.sendMessage(CC.GREEN + CC.BOLD + "Arena Status " + CC.GRAY + "(" +
                (arena.isSetup() ? CC.GREEN : CC.RED) + arena.getName() + CC.GRAY + ")");

        player.sendMessage(CC.GREEN + "Cuboid Lower Location: " + CC.YELLOW +
                (arena.getLowerCorner() == null ?
                        StringEscapeUtils.unescapeJava("✗") :
                        StringEscapeUtils.unescapeJava("✓")));

        player.sendMessage(CC.GREEN + "Cuboid Upper Location: " + CC.YELLOW +
                (arena.getUpperCorner() == null ?
                        StringEscapeUtils.unescapeJava("✗") :
                        StringEscapeUtils.unescapeJava("✓")));

        player.sendMessage(CC.GREEN + "Spawn A Location: " + CC.YELLOW +
                (arena.getSpawnA() == null ?
                        StringEscapeUtils.unescapeJava("✗") :
                        StringEscapeUtils.unescapeJava("✓")));

        player.sendMessage(CC.GREEN + "Spawn B Location: " + CC.YELLOW +
                (arena.getSpawnB() == null ?
                        StringEscapeUtils.unescapeJava("✗") :
                        StringEscapeUtils.unescapeJava("✓")));

        player.sendMessage(CC.GREEN + "Kits: " + CC.YELLOW + StringUtils.join(arena.getKits(), ", "));
    }

	@Subcommand("genhelper")
    public void genhelper(Player player) {
        Block origin = player.getLocation().getBlock();
        Block up = origin.getRelative(BlockFace.UP);

        origin.setType(Material.SPONGE);
        up.setType(Material.SIGN_POST);

        if (up.getState() instanceof Sign) {
            Sign sign = (Sign) up.getState();
            sign.setLine(0, ((int) player.getLocation().getPitch()) + "");
            sign.setLine(1, ((int) player.getLocation().getYaw()) + "");
            sign.update();

            player.sendMessage(CC.GREEN + "Generator helper placed.");
        }
    }

	@Subcommand("generate")
    public void generate(CommandSender sender) {
		File schematicsFolder = new File(Praxi.get().getDataFolder().getPath() + File.separator + "schematics");

		if (!schematicsFolder.exists()) {
			sender.sendMessage(CC.RED + "The schematics folder does not exist.");
			return;
		}

		for (File file : schematicsFolder.listFiles()) {
			if (!file.isDirectory() && file.getName().contains(".schematic")) {
				boolean duplicate = file.getName().endsWith("_duplicate.schematic");

				String name = file.getName()
				                  .replace(".schematic", "")
				                  .replace("_duplicate", "");

				Arena parent = Arena.getByName(name);

				if (parent != null) {
					if (!(parent instanceof StandaloneArena)) {
						System.out.println("Skipping " + name + " because it's not duplicate and an arena with that name already exists.");
						continue;
					}
				}

				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							new ArenaGenerator(name, Bukkit.getWorlds().get(0), new Schematic(file), duplicate ?
									(parent != null ? ArenaType.DUPLICATE : ArenaType.STANDALONE) : ArenaType.SHARED)
									.generate(file, (StandaloneArena) parent);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.runTask(Praxi.get());
			}
		}

		sender.sendMessage(CC.GREEN + "Generating arenas... See console for details.");
	}
    @Subcommand("test")
    @Syntax("<arena> <amount>")
    public void test(Player player, String arenaName, int amount) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exists!"));
            return;
        }

        Arena arena = Arena.getByName(arenaName);

        if (!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit") && !Bukkit.getPluginManager().isPluginEnabled("WorldEdit") ) {
            player.sendMessage(CC.translate("&cWorld Edit or FAWE not found, Arena Generating will not work!"));
            return;
        }

        if (amount > 30) {
            player.sendMessage(CC.translate("&cThat amount is too high, you can only place 30 arenas at a time due to performance issues."));
            return;
        }

        if (arena.getType() == ArenaType.SHARED/* || arena.isDuplicate()*/) {
            player.sendMessage(CC.translate("&cYou can't paste that type of Arena!"));
            return;
        }

        if (!arena.isSetup()) {
            player.sendMessage(CC.translate("&cPlease fully setup your arena before pasting!"));
            return;
        }

         new StandalonePasteRunnable(player, arena);

        player.sendMessage(CC.translate("&aPasting...."));
        /*for (Arena arenas : Arena.getArenas()) {
            arenas.save();
        }*/
    }

	@Subcommand("list")
    public void list(Player player) {
        player.sendMessage(CC.RED + CC.BOLD + "Arenas:");

        if (Arena.getArenas().isEmpty()) {
            player.sendMessage(CC.GRAY + "There are no arenas.");
            return;
        }

        for (Arena arena : Arena.getArenas()) {
            if (arena.getType() != ArenaType.DUPLICATE) {
                ChatComponentBuilder builder = new ChatComponentBuilder("")
                        .parse("&7- " + (arena.isSetup() ? "&a" : "&c") + arena.getName() +
                                "&7(" + arena.getType().name() + ") ");

                ChatComponentBuilder status = new ChatComponentBuilder("").parse("&7[&6STATUS&7]");
                status.attachToEachPart(ChatHelper.hover("&aClick to view this arena's status."));
                status.attachToEachPart(ChatHelper.click("/arena status " + arena.getName()));

                builder.append(" ");

                for (BaseComponent component : status.create()) {
                    builder.append((TextComponent) component);
                }

                player.spigot().sendMessage(builder.create());
            }
        }
    }

	@Subcommand("tp")
	@CommandCompletion("@arenas")
    @Syntax("<arena>")
    public void tp(Player player, String arenaName) {
        if (arenaCheck(arenaName)) {
            player.sendMessage(CC.translate("&cArena doesn't exists!"));
            return;
        }

        Arena arena = Arena.getByName(arenaName);
        if (arena == null) return;

        player.teleport(arena.getSpawnA());
        player.sendMessage(CC.GREEN + "Teleported to arena " + arenaName);
    }

	private boolean arenaCheck(String arena) {
        return !Arena.getArenas().contains(Arena.getByName(arena));
    }
}