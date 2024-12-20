package me.funky.praxi.util;

import me.funky.praxi.util.ItemBuilder;
import me.funky.praxi.util.menu.Button;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class Constants {

    @Getter private static Random random = new Random();

    public static final Button BLACK_PANE = new Button() {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.STAINED_GLASS_PANE)
                    .durability(7)
                    .name(" ")
                    .build();
        }
    };

}
