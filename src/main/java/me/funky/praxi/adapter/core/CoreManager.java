package me.funky.praxi.adapter.core;

import lombok.Getter;
import lombok.Setter;
import me.funky.praxi.adapter.core.impl.Default;
import me.funky.praxi.adapter.core.impl.Volcano;
import me.funky.praxi.adapter.core.impl.Nebula;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class CoreManager {

    @Getter @Setter public static CoreManager instance;
    @Getter @Setter private Plugin plugin;
    @Getter @Setter private String coreSystem;
    @Getter @Setter private Core core;

    public CoreManager() {
        instance = this;
        loadRank();
    }

	public static CoreManager getInstance() {
        if (instance == null) {
            instance = new CoreManager();
        }
        return instance;
    }

    public void loadRank() {
		if (Bukkit.getPluginManager().getPlugin("Volcano") != null) {
                    this.setCore(new Volcano());
                    setCoreSystem("Volcano");
                } else if (Bukkit.getPluginManager().getPlugin("Nebula") != null) {
                    this.setCore(new Nebula());
                    setCoreSystem("Nebula");
		} else {
                       this.setCore(new Default());
                       setCoreSystem("Default");
		}
   }

}
