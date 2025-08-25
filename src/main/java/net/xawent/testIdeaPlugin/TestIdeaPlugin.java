package net.xawent.testIdeaPlugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class TestIdeaPlugin extends JavaPlugin {
    public static TestIdeaPlugin instance;


    @Override
    public void onEnable() {
        instance = this;

        getCommand("meteor").setExecutor(new MeteorCommand());
        getCommand("specialmeteor").setExecutor(new SpecialMeteorCommand());

    }


}