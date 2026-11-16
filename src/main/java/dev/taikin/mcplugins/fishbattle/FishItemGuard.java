package dev.taikin.mcplugins.fishbattle;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;

public final class FishItemGuard implements Listener {

    public void sweepLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    protect(item);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpawn(ItemSpawnEvent event) {
        protect(event.getEntity());
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Item item) {
                protect(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) {
            return;
        }
        if (!isProtected(item.getItemStack())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAttemptPickup(PlayerAttemptPickupItemEvent event) {
        Item item = event.getItem();
        if (!isProtected(item.getItemStack())) {
            return;
        }
        item.setUnlimitedLifetime(true);
    }

    private void protect(Item item) {
        if (!isProtected(item.getItemStack())) {
            return;
        }
        if (!item.isInvulnerable()) {
            item.setInvulnerable(true);
        }
        if (!item.isUnlimitedLifetime()) {
            item.setUnlimitedLifetime(true);
        }
        if (!item.isPersistent()) {
            item.setPersistent(true);
        }
    }

    static boolean isProtected(ItemStack item) {
        return item != null
                && (FishItems.isCaptureBucket(item) || FishItems.isFish(item));
    }
}
