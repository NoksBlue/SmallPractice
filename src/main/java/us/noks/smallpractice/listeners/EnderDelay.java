package us.noks.smallpractice.listeners;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EnderpearlLandEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.minecraft.util.com.google.common.collect.Maps;
import us.noks.smallpractice.enums.PlayerStatus;
import us.noks.smallpractice.objects.managers.PlayerManager;

public class EnderDelay implements Listener {

	static EnderDelay instance = new EnderDelay();
	public static EnderDelay getInstance() {
		return instance;
	}
	
	private Map<UUID, Long> enderpearlCooldown = Maps.newConcurrentMap();
	private int cooldowntime = 14;

	@EventHandler(priority=EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getPlayer() instanceof Player) {
			Player player = event.getPlayer();
			if (event.hasItem()) {
				ItemStack item = event.getItem();
				if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && item.getType() == Material.ENDER_PEARL && player.getGameMode() != GameMode.CREATIVE) {
					if (PlayerManager.get(player.getUniqueId()).getStatus() == PlayerStatus.DUEL) {
						if (isEnderPearlCooldownActive(player)) {
							event.setUseItemInHand(Result.DENY);
							double time = getEnderPearlCooldown(player) / 1000.0D;
							DecimalFormat df = new DecimalFormat("0.0");
							player.sendMessage(ChatColor.DARK_AQUA + "Pearl cooldown: " + ChatColor.YELLOW + df.format(time) + " second" + (time > 1.0D ? "s" : ""));
							player.updateInventory();
						} else {
							applyCooldown(player);
						}
					} else {
						event.setUseItemInHand(Result.DENY);
						player.sendMessage(ChatColor.RED + "You cannot use enderpearl here!");
						player.updateInventory();
					}
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onTeleport(EnderpearlLandEvent event) {
		if (event.getEntity().getShooter() instanceof Player) {
			Player player = (Player) event.getEntity().getShooter();
			
			if (PlayerManager.get(player.getUniqueId()).getStatus() != PlayerStatus.DUEL) {
				event.setCancelled(true);
			}
		}
	}

	public boolean isEnderPearlCooldownActive(Player player) {
		if (!this.enderpearlCooldown.containsKey(player.getUniqueId())) {
			return false;
		}
		return this.enderpearlCooldown.get(player.getUniqueId()).longValue() > System.currentTimeMillis();
	}

	public long getEnderPearlCooldown(Player player) {
		if (this.enderpearlCooldown.containsKey(player.getUniqueId())) {
			return Math.max(0L, this.enderpearlCooldown.get(player.getUniqueId()).longValue() - System.currentTimeMillis());
		}
		return 0L;
	}

	public void applyCooldown(Player player) {
		this.enderpearlCooldown.put(player.getUniqueId(), Long.valueOf(System.currentTimeMillis() + this.cooldowntime * 1000));
	}

	public void removeCooldown(Player player) {
		if (this.enderpearlCooldown.containsKey(player.getUniqueId())) this.enderpearlCooldown.remove(player.getUniqueId());
	}
}
