package me.funky.praxi.arena.runnables;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import me.funky.praxi.arena.Arena;
import me.funky.praxi.arena.impl.StandaloneArena;
import me.funky.praxi.util.*;

public class StandalonePasteRunnable extends TaskTicker {
    private final Player player;
    private final Arena arena;

    public StandalonePasteRunnable(Player player, Arena arena) {
        super(0, 1, false);
        this.player = player;
        this.arena = arena;
    }

    @Override
    public void onRun() {
        cancel();
        new DuplicateArenaRunnable(arena, 10000, 10000, 500, 500) {
            @Override
            public void onComplete() {
                World world = arena.getLocation1().getWorld();

                double minX = arena.getLocation1().getX() + this.getOffsetX();
                double minZ = arena.getLocation1().getZ() + this.getOffsetZ();
                double maxX = arena.getLocation2().getX() + this.getOffsetX();
                double maxZ = arena.getLocation2().getZ() + this.getOffsetZ();

                double aX = arena.getSpawnA().getX() + this.getOffsetX();
                double aZ = arena.getSpawnA().getZ() + this.getOffsetZ();
                double bX = arena.getSpawnB().getX() + this.getOffsetX();
                double bZ = arena.getSpawnB().getZ() + this.getOffsetZ();

                Location min = new Location(world, minX, arena.getLocation1().getY(), minZ, arena.getLocation1().getYaw(), arena.getLocation1().getPitch());
                Location max = new Location(world, maxX, arena.getLocation2().getY(), maxZ, arena.getLocation2().getYaw(), arena.getLocation2().getPitch());
                Location a = new Location(world, aX, arena.getSpawnA().getY(), aZ, arena.getSpawnA().getYaw(), arena.getSpawnA().getPitch());
                Location b = new Location(world, bX, arena.getSpawnB().getY(), bZ, arena.getSpawnB().getYaw(), arena.getSpawnB().getPitch());
	
                StandaloneArena duplicate = new StandaloneArena(arena.getName(), min, max, a, b);
            }
        }.run();
    }

    @Override
    public void preRun() {
        player.sendMessage(CC.translate("&aGenerating"));
    }

    @Override
    public TickType getTickType() {
        return TickType.NONE;
    }

    @Override
    public int getStartTick() {
        return 0;
    }
}
