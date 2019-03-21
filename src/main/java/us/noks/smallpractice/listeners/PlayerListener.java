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
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionEffectAddEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import net.minecraft.util.com.google.common.collect.Lists;
import us.noks.smallpractice.Main;
import us.noks.smallpractice.enums.PlayerStatus;
import us.noks.smallpractice.objects.Duel;
import us.noks.smallpractice.objects.managers.DuelManager;
import us.noks.smallpractice.objects.managers.PartyManager;
import us.noks.smallpractice.objects.managers.PlayerManager;
import us.noks.smallpractice.objects.managers.QueueManager;
import us.noks.smallpractice.party.Party;
import us.noks.smallpractice.party.PartyState;
import us.noks.smallpractice.utils.Messages;

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
		PlayerManager.get(player.getUniqueId()).giveSpawnItem();
		
		player.sendMessage(Messages.getInstance().WELCOME_MESSAGE);
		player.setPlayerListName(PlayerManager.get(player.getUniqueId()).getPrefixColors() + player.getName());
		
		for (Player allPlayers : Bukkit.getOnlinePlayers()) {
			PlayerManager pmAll = PlayerManager.get(allPlayers.getUniqueId());
			if (pmAll.getStatus() == PlayerStatus.WAITING || pmAll.getStatus() == PlayerStatus.DUEL || pmAll.getStatus() == PlayerStatus.MODERATION) {
				player.hidePlayer(allPlayers);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onQuit(PlayerQuitEvent event) {
		event.setQuitMessage(null);
		Player player = event.getPlayer();
		
        if (PartyManager.getInstance().hasParty(player.getUniqueId())) {
        	Party party = PartyManager.getInstance().getParty(player.getUniqueId());
            if (party.getLeader().equals(player.getUniqueId())) {
            	PartyManager.getInstance().transferLeader(player.getUniqueId());
            } else {
            	PartyManager.getInstance().leaveParty(player.getUniqueId());
            }
        }
		if (QueueManager.getInstance().queue.contains(player.getUniqueId())) {
			QueueManager.getInstance().queue.remove(player.getUniqueId());
		}
		if ((PlayerManager.get(player.getUniqueId()).getStatus() == PlayerStatus.DUEL || PlayerManager.get(player.getUniqueId()).getStatus() == PlayerStatus.WAITING)) {
			DuelManager.getInstance().removePlayerFromDuel(player, true);
		}
		PlayerManager.get(player.getUniqueId()).remove();
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onDeath(PlayerDeathEvent event) {
		event.setDeathMessage(null);
		event.setDroppedExp(0);
		
		if (event.getEntity() instanceof Player) {
			Player killed = event.getEntity();
			DuelManager.getInstance().removePlayerFromDuel(killed, false);
		}
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		
		if (DuelManager.getInstance().getDuelFromPlayerUUID(player.getUniqueId()) == null || !DuelManager.getInstance().getDuelFromPlayerUUID(player.getUniqueId()).hasRemainingRound()) {
			player.getInventory().clear();
			player.getInventory().setArmorContents(null);

			player.setHealth(20.0D);
			player.setFoodLevel(20);
			player.setSaturation(10000f);
			player.teleport(Main.getInstance().getSpawnLocation());
			PlayerManager.get(player.getUniqueId()).giveSpawnItem();
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onVoidDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			PlayerManager pm = PlayerManager.get(player.getUniqueId());
			
			if (pm.getStatus() == PlayerStatus.SPAWN || pm.getStatus() == PlayerStatus.QUEUE) {
				switch (event.getCause()) {
				case FALL:
					event.setCancelled(true);
					break;
				case VOID:
					event.setCancelled(true);
					player.teleport(Main.getInstance().getSpawnLocation());
					break;
				default:
					break;
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onDamage(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
			Player attacked = (Player) event.getEntity();
			Player attacker = (Player) event.getDamager();
				
			if (PlayerManager.get(attacker.getUniqueId()).getStatus() == PlayerStatus.MODERATION) {
				event.setDamage(0.0D);
				return;
			}
			if (PlayerManager.get(attacker.getUniqueId()).getStatus() == PlayerStatus.SPECTATE) {
				event.setCancelled(true);
				return;
			}
			if (PlayerManager.get(attacker.getUniqueId()).getStatus() != PlayerStatus.DUEL && PlayerManager.get(attacked.getUniqueId()).getStatus() != PlayerStatus.DUEL) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onReceiveDrop(PlayerPickupItemEvent event) {
		if (event.getItem().getOwner() instanceof Player) {
			Player receiver = event.getPlayer();
			Player owner = (Player) event.getItem().getOwner();
			PlayerManager pm = PlayerManager.get(receiver.getUniqueId());
			
			if (pm.getStatus() != PlayerStatus.DUEL && pm.getStatus() != PlayerStatus.WAITING && !pm.isCanBuild()) {
				event.setCancelled(true);
				return;
			}
			if (DuelManager.getInstance().getDuelFromPlayerUUID(receiver.getUniqueId()) != null) {
				Duel currentDuel = DuelManager.getInstance().getDuelFromPlayerUUID(receiver.getUniqueId());
				
				if (!currentDuel.getFirstTeam().contains(owner.getUniqueId()) && !currentDuel.getSecondTeam().contains(owner.getUniqueId())) event.setCancelled(true);
				return;
			}
			if (!receiver.canSee(owner)) event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onDrop(PlayerDropItemEvent event) {
		if (event.getPlayer().getGameMode() != GameMode.CREATIVE && PlayerManager.get(event.getPlayer().getUniqueId()).getStatus() != PlayerStatus.WAITING && PlayerManager.get(event.getPlayer().getUniqueId()).getStatus() != PlayerStatus.DUEL) {
			event.setCancelled(true);
		}
		if (event.getItemDrop().getItemStack().getType() == Material.GLASS_BOTTLE) event.getItemDrop().remove();
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onClickItem(PlayerInteractEvent event) {
		Player player = event.getPlayer();
        if (player.getInventory().getItemInHand() == null || !player.getInventory().getItemInHand().hasItemMeta() || !player.getInventory().getItemInHand().getItemMeta().hasDisplayName()) {
            return;
        }
        ItemStack item = player.getItemInHand();
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
        	PlayerManager pm = PlayerManager.get(player.getUniqueId());
        	
        	switch (pm.getStatus()) {
			case SPAWN:
				if (!PartyManager.getInstance().hasParty(player.getUniqueId())) {
					if (item.getType() == Material.IRON_SWORD && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.YELLOW + "unranked direct queue")) {
		                event.setCancelled(true);
		                QueueManager.getInstance().addToQueue(player, false);
		                break;
		            }
					if (item.getType() == Material.DIAMOND_SWORD && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.YELLOW + "ranked direct queue")) {
		                event.setCancelled(true);
		                player.sendMessage(ChatColor.GOLD + "This action comming soon ^^");
		                break;
		            }
					if (item.getType() == Material.NAME_TAG && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.YELLOW + "create party")) {
		                event.setCancelled(true);
		                PartyManager.getInstance().createParty(player.getUniqueId(), player.getName());
		                pm.giveSpawnItem();
		                break;
		            }
				} else {
					Party currentParty = PartyManager.getInstance().getParty(player.getUniqueId());
					boolean isPartyLeader = currentParty.getLeader() == player.getUniqueId();
					
					if (item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.YELLOW + "split teams")) {
						if (!isPartyLeader) {
							player.sendMessage(ChatColor.RED + "You are not the leader of this party!");
							break;
						}
						if (currentParty.getPartyState() == PartyState.DUELING) {
                            player.sendMessage(ChatColor.RED + "Your party is currently busy and cannot fight.");
                            break;
                        }
                        if (currentParty.getMembers().isEmpty()) {
                            player.sendMessage(ChatColor.RED + "There must be at least 2 players in your party to do this.");
                            break;
                        }
                        DuelManager.getInstance().createSplitTeamsDuel(currentParty);
                        break;
		            }
					if (item.getType() == Material.BOOK && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.YELLOW + "fight other parties")) {
						player.openInventory(PartyManager.getInstance().getPartiesInventory());
						break;
					}
					if (item.getType() == Material.PAPER && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.YELLOW + "party information")) {
						Bukkit.dispatchCommand(player, "party info");
						break;
					}
					if (item.getType() == Material.REDSTONE && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.RED + "leave party")) {
						event.setCancelled(true);
						if (isPartyLeader) {
							PartyManager.getInstance().transferLeader(player.getUniqueId());
						} else {
							currentParty.removeMember(player.getUniqueId());
						}
						pm.giveSpawnItem();
					}
				}
				break;
			case QUEUE:
				if (item.getType() == Material.REDSTONE && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.RED + "leave queue")) {
	                event.setCancelled(true);
	                QueueManager.getInstance().quitQueue(player);
	            }
				break;
			case SPECTATE:
				if (item.getType() == Material.REDSTONE && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.RED + "leave spectate")) {
	                event.setCancelled(true);
	                Player spectatePlayer = pm.getSpectate();
	                Duel spectatedDuel = DuelManager.getInstance().getDuelFromPlayerUUID(spectatePlayer.getUniqueId());
	                
	        		spectatedDuel.sendMessage(ChatColor.YELLOW + player.getName() + ChatColor.DARK_AQUA + " is no longer spectating.");
	        		spectatedDuel.removeSpectator(player.getUniqueId());
	        		
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
				if (item.getType() == Material.REDSTONE && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.RED + "leave moderation")) {
	                event.setCancelled(true);
	                player.performCommand("mod");
	                Bukkit.dispatchCommand(player, "mod");
	                break;
	            }
				if (item.getType() == Material.WATCH && item.getItemMeta().getDisplayName().toLowerCase().equals(ChatColor.RED + "see random player")) {
	                event.setCancelled(true);
	                List<Player> online = Lists.newArrayList();
	                
	                for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
	                	if (onlinePlayers == player) continue;
	                	
	                	PlayerManager om = PlayerManager.get(onlinePlayers.getUniqueId());
	                	if (om.getStatus() == PlayerStatus.MODERATION) continue;
	                	
	                	online.add(onlinePlayers);
	                }
	                if (online.isEmpty()) {
	                	player.sendMessage(ChatColor.RED + "No player to agree.");
	                	return;
	                }
	                Player tooked = online.get(new Random().nextInt(online.size()));
	                
	                player.teleport(tooked.getLocation().add(0, 2, 0));
	                player.sendMessage(ChatColor.GREEN + "You've been teleported to " + tooked.getName());
	                online.clear();
	            }
				break;
			default:
				break;
			}
        }
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onFeed(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			
			if (PlayerManager.get(player.getUniqueId()).getStatus() != PlayerStatus.DUEL) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPotionEffectAdd(PotionEffectAddEvent event) {
		if (event.getEntity() instanceof Player) {
			Player affected = (Player) event.getEntity();
			PlayerManager pm = PlayerManager.get(affected.getUniqueId());
			
			if (pm.getStatus() != PlayerStatus.DUEL && pm.getStatus() != PlayerStatus.WAITING) {
				event.setCancelled(true);
			}
		}
	}
}
