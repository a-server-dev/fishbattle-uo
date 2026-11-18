package dev.taikin.mcplugins.fishbattle;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

public final class BattleListener implements Listener {

    private final BattleManager battles;
    private final FishMotion motion;

    public BattleListener(BattleManager battles, FishMotion motion) {
        this.battles = battles;
        this.motion = motion;
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        battles.mash(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (battles.isMashing(event.getPlayer())) {
            event.setCancelled(true);
            battles.mash(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (battles.isMashing(event.getPlayer())) {
            event.setCancelled(true);
            battles.mash(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Player player) {
            if (DemoVillager.isDemoVillager(event.getEntity())) {
                event.setCancelled(true);
                if (DemoVillager.inRemovalMode(player.getUniqueId())
                        && player.hasPermission("uobattle.admin")) {
                    if (event.getEntity() instanceof org.bukkit.entity.Villager villager) {
                        DemoVillager.remove(villager, motion);
                    } else {
                        event.getEntity().remove();
                    }
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                            "デモ村人を消しました（削除モード継続中）。",
                            net.kyori.adventure.text.format.NamedTextColor.GRAY));
                }
                return;
            }
            battles.mash(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        battles.handMenu().close(event.getPlayer());
        battles.endQuietly(event.getPlayer());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (var entity : event.getEntities()) {
            if (FishMotion.hasData(entity)) {
                motion.track(entity);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (FishMotion.hasData(event.getEntity())) {
            event.setCancelled(true);
        }
        if (event.getEntity().getPersistentDataContainer()
                .has(FishKeys.DEMO, org.bukkit.persistence.PersistentDataType.BYTE)
                || DemoVillager.isDemoVillager(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
