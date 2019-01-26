package us.noks.smallpractice.listeners;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.minecraft.util.com.google.common.collect.Lists;
import us.noks.smallpractice.Main;
import us.noks.smallpractice.objects.managers.DuelManager;
import us.noks.smallpractice.objects.managers.PlayerManager;
import us.noks.smallpractice.utils.InvView;
import us.noks.smallpractice.utils.PlayerStatus;

public class PlayerListener implements Listener {
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onJoin(PlayerJoinEvent event) {
		event.setJoinMessage(null);
		Player player = event.getPlayer();
		
		player.setExp(0.0F);
		player.setLevel(0);
		
		player.setHealth(20.0D);
		player.setFoodLevel(20);
		player.setSaturation(1000f);
		player.extinguish();
		player.clearPotionEffect();
		player.setAllowFlight(false);
		player.setFlying(false);
		
		player.setGameMode(GameMode.SURVIVAL);
		
		player.setScoreboard(Main.getInstance().getServer().getScoreboardManager().getNewScoreboard());
		
		player.teleport(Main.getInstance().getSpawnLocation());
		PlayerManager.get(player).giveSpawnItem();
		
		player.sendMessage(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------------------------------------");
		player.sendMessage(ChatColor.YELLOW + "Welcome on the " + ChatColor.DARK_AQUA.toString() + ChatColor.BOLD + "Halka" + ChatColor.YELLOW + " practice " + Main.getInstance().getDescription().getVersion() + " server");
		player.sendMessage("   ");
		player.sendMessage(ChatColor.YELLOW + "Noksio (Creator) Twitter -> " + ChatColor.DARK_AQUA + "https://twitter.com/NotNoksio");
		player.sendMessage(ChatColor.YELLOW + "Noksio (Creator) Discord -> " + ChatColor.DARK_AQUA + "https://discord.gg/TZhyPnB");
		player.sendMessage(ChatColor.RED + "-> Keep in mind this is a beta ^^");
		player.sendMessage(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "--------------------------------------------");
		
		player.setPlayerListName(PlayerManager.get(player).getColorPrefix() + player.getName());
		
		for (Player allPlayers : Bukkit.getOnlinePlayers()) {
			PlayerManager pmAll = PlayerManager.get(allPlayers);
			if (pmAll.getStatus() == PlayerStatus.WAITING || pmAll.getStatus() == PlayerStatus.DUEL || pmAll.getStatus() == PlayerStatus.MODERATION) {
				allPlayers.hidePlayer(player);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onDeath(PlayerDeathEvent event) {
		event.setDeathMessage(null);
		event.getDrops().clear();
		event.setDroppedExp(0);
		
		if (event.getEntity() instanceof Player) {
			Player killed = event.getEntity();
			Player killer = PlayerManager.get(killed).getOpponent();
			
			if (killer != null) {
				InvView.getInstance().deathMsg(killer, killed);
				DuelManager.getInstance().endDuel(DuelManager.getInstance().getDuelByUUID(killer.getUniqueId()), killer);
				
				new BukkitRunnable() {
					
					@Override
					public void run() {
						if (killed.isDead() && killed != null) {
							killed.spigot().respawn();
						}
					}
				}.runTaskLater(Main.getInstance(), 50L);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onRespawn(PlayerRespawnEvent event) {
		Player p = event.getPlayer();
		
		p.getInventory().clear();
		p.getInventory().setArmorContents(null);
		new BukkitRunnable() {
			
			@Override
			public void run() {
				p.setHealth(20.0D);
				p.setFoodLevel(20);
				p.setSaturation(10000f);
				p.teleport(Main.getInstance().getSpawnLocation());
				PlayerManager.get(p).giveSpawnItem();
			}
		}.runTaskLater(Main.getInstance(), 1L);
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onQuit(PlayerQuitEvent event) {
		event.setQuitMessage(null);
		
		if (Main.getInstance().queue.contains(event.getPlayer())) {
			Main.getInstance().queue.remove(event.getPlayer());
		}
		if ((PlayerManager.get(event.getPlayer()).getStatus() == PlayerStatus.DUEL || PlayerManager.get(event.getPlayer()).getStatus() == PlayerStatus.WAITING) && PlayerManager.get(event.getPlayer()).getOpponent() != null) {
			Player op = PlayerManager.get(event.getPlayer()).getOpponent();
			
			InvView.getInstance().deathMsg(op, event.getPlayer());
			/*
			PlayerManager.get(event.getPlayer()).setOldOpponent(op);
			InvView.getInstance().saveInv(event.getPlayer());
			
			Iterator<Player> it = PlayerManager.get(event.getPlayer()).getAllSpectators().iterator();
			
			while (it.hasNext()) {
				Player spec = it.next();
				PlayerManager sm = PlayerManager.get(spec);
				
				spec.setAllowFlight(false);
				spec.setFlying(false);
				sm.setStatus(PlayerStatus.SPAWN);
				sm.showAllPlayer();
				sm.setSpectate(null);
				spec.getInventory().clear();
				spec.teleport(Main.getInstance().spawnLocation);
				
				it.remove();
			}
			*/
			DuelManager.getInstance().endDuel(DuelManager.getInstance().getDuelByUUID(op.getUniqueId()), op);
		}
		//PlayerManager.remove(event.getPlayer());
		PlayerManager.get(event.getPlayer()).remove();
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onVoidDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			PlayerManager pm = PlayerManager.get(player);
			
			if (event.getCause() == DamageCause.FALL && (pm.getStatus() == PlayerStatus.SPAWN || pm.getStatus() == PlayerStatus.QUEUE)) {
				event.setCancelled(true);
			}
			if (event.getCause() == DamageCause.VOID && (pm.getStatus() == PlayerStatus.SPAWN || pm.getStatus() == PlayerStatus.QUEUE)) {
				event.setCancelled(true);
				player.teleport(Main.getInstance().getSpawnLocation());
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onDamage(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
			Player rec = (Player) event.getEntity();
			Player attacker = (Player) event.getDamager();
				
			if (PlayerManager.get(attacker).getStatus() == PlayerStatus.MODERATION) {
				event.setDamage(0.0D);
				return;
			}
			if (PlayerManager.get(attacker).getStatus() == PlayerStatus.SPECTATE) {
				event.setCancelled(true);
				return;
			}
			if (PlayerManager.get(attacker).getStatus() != PlayerStatus.DUEL && PlayerManager.get(rec).getStatus() != PlayerStatus.DUEL) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onReceiveDrop(PlayerPickupItemEvent event) {
		if (event.getItem().getOwner() instanceof Player) {
			Player owner = (Player) event.getItem().getOwner();
			
			if (!event.getPlayer().canSee(owner)) event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onDrop(PlayerDropItemEvent event) {
		if (event.getPlayer().getGameMode() != GameMode.CREATIVE && PlayerManager.get(event.getPlayer()).getStatus() != PlayerStatus.WAITING && PlayerManager.get(event.getPlayer()).getStatus() != PlayerStatus.DUEL) {
			event.setCancelled(true);
		}
		if (event.getItemDrop().getItemStack().getType() == Material.GLASS_BOTTLE) event.getItemDrop().remove();
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onClickItem(PlayerInteractEvent event) {
		Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        if (player.getInventory().getItemInHand() == null || !player.getInventory().getItemInHand().hasItemMeta() || !player.getInventory().getItemInHand().getItemMeta().hasDisplayName()) {
            return;
        }
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
        	PlayerManager pm = PlayerManager.get(player);
        	
        	switch (pm.getStatus()) {
			case SPAWN:
				if (item.getType() == Material.DIAMOND_SWORD && item.getItemMeta().getDisplayName().equals(ChatColor.YELLOW + "Direct Queue")) {
	                event.setCancelled(true);
	                Main.getInstance().addQueue(player);
	            }
				break;
			case QUEUE:
				if (item.getType() == Material.REDSTONE && item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Leave Queue")) {
	                event.setCancelled(true);
	                Main.getInstance().quitQueue(player);
	            }
				break;
			case SPECTATE:
				if (item.getType() == Material.REDSTONE && item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Leave Spectate")) {
	                event.setCancelled(true);
	                Player spectatePlayer = pm.getSpectate();
	        		DuelManager.getInstance().getDuelByUUID(spectatePlayer.getUniqueId()).removeSpectator(player.getUniqueId());
	        		DuelManager.getInstance().getDuelByUUID(spectatePlayer.getUniqueId()).sendMessage(ChatColor.YELLOW + player.getName() + ChatColor.DARK_AQUA + " is no longer spectating.");
	        		
	        		player.setAllowFlight(false);
	        		player.setFlying(false);
	        		pm.setStatus(PlayerStatus.SPAWN);
	        		pm.showAllPlayer();
	        		pm.setSpectate(null);
	        		player.teleport(Main.getInstance().getSpawnLocation());
	        		pm.giveSpawnItem();
	            }
				break;
			case MODERATION:
				if (item.getType() == Material.REDSTONE && item.getItemMeta().getDisplayName().equals(ChatColor.RED + "Leave Moderation")) {
	                event.setCancelled(true);
	                player.teleport(Main.getInstance().getSpawnLocation());
	                pm.setStatus(PlayerStatus.SPAWN);
	                pm.giveSpawnItem();
	            }
				if (item.getType() == Material.WATCH && item.getItemMeta().getDisplayName().equals(ChatColor.RED + "See Random Player")) {
	                event.setCancelled(true);
	                List<Player> online = Lists.newArrayList();
	                online.addAll(Bukkit.getOnlinePlayers());
	                
	                Player tooked = online.get(new Random().nextInt(Bukkit.getOnlinePlayers().size()));
	                
	                player.teleport(tooked.getLocation().add(0, 2, 0));
	                player.sendMessage(ChatColor.GREEN + "Teleport to " + tooked.getName());
	            }
				break;
			default:
				break;
			}
        }
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onDrag(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		if (event.getInventory().getType().equals(InventoryType.CREATIVE) || event.getInventory().getType().equals(InventoryType.CRAFTING) || event.getInventory().getType().equals(InventoryType.PLAYER)) {
			PlayerManager pm = PlayerManager.get(p);
			
			if (pm.getStatus() != PlayerStatus.DUEL && pm.getStatus() != PlayerStatus.WAITING && !pm.isCanBuild()) {
				event.setCancelled(true);
				p.updateInventory();
			}
			if (pm.getStatus() == PlayerStatus.MODERATION) {
				event.setCancelled(true);
				p.updateInventory();
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onFeed(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			
			if (PlayerManager.get(player).getStatus() != PlayerStatus.DUEL) {
				event.setCancelled(true);
			}
		}
	}
}
