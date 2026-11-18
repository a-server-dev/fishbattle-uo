package dev.taikin.mcplugins.fishbattle;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class FishBattlePlugin extends JavaPlugin {

    private BattleManager battles;
    private FishMotion motion;
    private NamespacedKey captureRecipeKey;

    @Override
    public void onEnable() {
        FishKeys.init(this);
        loadSpeciesConfig();
        loadTechniqueConfig();
        this.motion = new FishMotion(this);
        this.battles = new BattleManager(this, motion);
        motion.start();
        motion.trackLoaded();
        PetMenu menu = new PetMenu(this, motion);
        menu.setBattles(battles);

        CaptureListener capture = new CaptureListener(this, motion, menu);
        capture.setBattles(battles);
        getServer().getPluginManager().registerEvents(capture, this);
        FishItemGuard itemGuard = new FishItemGuard();
        getServer().getPluginManager().registerEvents(itemGuard, this);
        itemGuard.sweepLoaded();
        getServer().getPluginManager().registerEvents(new BattleListener(battles, motion), this);
        getServer().getPluginManager().registerEvents(battles.handMenu(), this);
        getServer().getPluginManager().registerEvents(new PetMenuListener(this, menu), this);

        FishCommand command = new FishCommand(battles, motion, menu);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.node(), "Bucket fish and battle", List.of()));

        registerCaptureRecipe();
        getLogger().info("FishBattle enabled.");
    }

    @Override
    public void onDisable() {
        if (motion != null) {
            motion.stop();
        }
        if (captureRecipeKey != null) {
            Bukkit.removeRecipe(captureRecipeKey);
        }
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (battles != null) {
                battles.endQuietly(player);
            }
        }
        getLogger().info("FishBattle disabled.");
    }

    public void reloadSpeciesConfig() {
        reloadConfig();
        loadSpeciesConfig();
        loadTechniqueConfig();
    }

    private void loadSpeciesConfig() {
        saveDefaultConfig();
        FishSpecies.setExpSettings(
                getConfig().getInt("exp.reference-total", 150),
                getConfig().getDouble("exp.base-multiplier", 0.5),
                getConfig().getDouble("exp.per-point", 0.08),
                getConfig().getDouble("exp.min-multiplier", 0.45));
        FishSpecies.loadConfig(getConfig().getConfigurationSection("species"));
        getLogger().info("Loaded species stats from config.yml.");
    }

    private void loadTechniqueConfig() {
        var file = new java.io.File(getDataFolder(), "techniques.yml");
        if (!file.exists()) {
            saveResource("techniques.yml", false);
        }
        var yaml = new org.bukkit.configuration.file.YamlConfiguration();
        try {
            yaml.load(file);
        } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            getLogger().warning("techniques.yml を読み込めませんでした: " + e.getMessage());
            Technique.loadConfig(null);
            return;
        }
        Technique.loadConfig(yaml.getConfigurationSection("techniques"));
        getLogger().info("Loaded " + Technique.all().size() + " techniques from techniques.yml.");
    }

    private void registerCaptureRecipe() {
        this.captureRecipeKey = new NamespacedKey(this, "capture_bucket");
        Bukkit.removeRecipe(captureRecipeKey);
        ShapelessRecipe recipe = new ShapelessRecipe(captureRecipeKey, FishItems.captureBucket());
        recipe.addIngredient(Material.BUCKET);
        recipe.addIngredient(Material.KELP);
        Bukkit.addRecipe(recipe);
        getLogger().info("Capture bucket recipe: bucket + kelp.");
    }
}
