package me.funky.praxi.profile.option.trail.menu;

import me.funky.praxi.Praxi;
import me.funky.praxi.profile.option.trail.Trail;
import me.funky.praxi.util.Constants;
import me.funky.praxi.util.menu.Button;
import me.funky.praxi.util.menu.Menu;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class TrailMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "&8Projectile Trails";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<Integer, Button>();
        int y = 1;
        int x = 1;
        for (Trail trail : Trail.values()) {
            if (trail.hasPermission(player)) {
                buttons.put(this.getSlot(x++, y), new TrailButton(trail));
            }
            if (x != 8) continue;
            ++y;
            x = 1;
        }
        for (int i = 0; i < 27; ++i) {
            buttons.putIfAbsent(i, Constants.BLACK_PANE);
        }
        return buttons;
    }
}
