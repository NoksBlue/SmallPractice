package us.noks.smallpractice.objects.managers;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.google.common.collect.Lists;

import net.minecraft.util.com.google.common.collect.Maps;
import us.noks.smallpractice.Main;
import us.noks.smallpractice.enums.PlayerStatus;
import us.noks.smallpractice.listeners.EnderDelay;
import us.noks.smallpractice.objects.Duel;
import us.noks.smallpractice.party.Party;
import us.noks.smallpractice.party.PartyState;
import us.noks.smallpractice.utils.InvView;

public class DuelManager {
	
	public static DuelManager instance = new DuelManager();
	public static DuelManager getInstance() {
		return instance;
	}
	
	private Map<UUID, Duel> uuidIdentifierToDuel = Maps.newHashMap();
	public Duel getDuelFromPlayerUUID(UUID uuid) {
        return this.uuidIdentifierToDuel.get(uuid);
    }
	
	public void startDuel(UUID firstPartyLeaderUUID, UUID secondPartyLeaderUUID, List<UUID> firstTeam, List<UUID> secondTeam, boolean ranked, int round) {
		Scoreboard firstPlayerScoreboard = Main.getInstance().getServer().getScoreboardManager().getNewScoreboard();
		Team red1 = firstPlayerScoreboard.registerNewTeam("red");
		red1.setPrefix(ChatColor.RED.toString());
		Team green1 = firstPlayerScoreboard.registerNewTeam("green");
		green1.setPrefix(ChatColor.GREEN.toString());
		green1.setAllowFriendlyFire(false);
        
		Scoreboard secondPlayerScoreboard = Main.getInstance().getServer().getScoreboardManager().getNewScoreboard();
		Team red2 = secondPlayerScoreboard.registerNewTeam("red");
		red2.setPrefix(ChatColor.RED.toString());
		Team green2 = secondPlayerScoreboard.registerNewTeam("green");
		green2.setPrefix(ChatColor.GREEN.toString());
		green2.setAllowFriendlyFire(false);
        
		round--;
		List<Player> duelPlayers = Lists.newArrayList();
		
		for (UUID firstUUID : firstTeam) {
			Player first = Bukkit.getPlayer(firstUUID);
			
			if (first == null) continue;
			
			PlayerManager fm = PlayerManager.get(first);
			fm.removeRequest();
			fm.setStatus(PlayerStatus.WAITING);
			
			first.setGameMode(GameMode.SURVIVAL);
			first.sendMessage(ChatColor.DARK_AQUA + "Starting duel against " + ChatColor.YELLOW + (secondPartyLeaderUUID != null ? Bukkit.getPlayer(secondPartyLeaderUUID).getName() + "'s party" : Bukkit.getPlayer(secondTeam.get(0)).getName()) + (round != 0 ? " (Round left: " + round + ")" : ""));
			fm.heal();
			
			green1.addEntry(first.getName());
			red2.addEntry(first.getName());
			first.setScoreboard(firstPlayerScoreboard);
			duelPlayers.add(first);
		}
		for (UUID secondUUID : secondTeam) {
			Player second = Bukkit.getPlayer(secondUUID);
			
			if (second == null) continue;
			
			PlayerManager sm = PlayerManager.get(second);
			sm.removeRequest();
			sm.setStatus(PlayerStatus.WAITING);
			
			second.setGameMode(GameMode.SURVIVAL);
			second.sendMessage(ChatColor.DARK_AQUA + "Starting duel against " + ChatColor.YELLOW + (firstPartyLeaderUUID != null ? Bukkit.getPlayer(firstPartyLeaderUUID).getName() + "'s party" : Bukkit.getPlayer(firstTeam.get(0)).getName()) + (round != 0 ? " (Round left: " + round + ")" : ""));
			sm.heal();
			
			green2.addEntry(second.getName());
			red1.addEntry(second.getName());
			second.setScoreboard(secondPlayerScoreboard);
			duelPlayers.add(second);
		}
		if (firstPartyLeaderUUID != null) {
            Party party = PartyManager.getInstance().getParty(firstPartyLeaderUUID);
            if (party != null) {
                party.setPartyState(PartyState.DUELING);
            }
        }
        if (secondPartyLeaderUUID != null) {
        	Party party = PartyManager.getInstance().getParty(secondPartyLeaderUUID);
            if (party != null) {
                party.setPartyState(PartyState.DUELING);
            }
        }
        for (Player team : duelPlayers) {
        	if (team == null) continue;
            for (Player ally : duelPlayers) {
            	if (ally == null) continue;
                team.showPlayer(ally);
                ally.showPlayer(team);
            }
        }
		duelPlayers.clear();
		teleportRandomArena(new Duel(firstPartyLeaderUUID, secondPartyLeaderUUID, firstTeam, secondTeam, ranked, round));
	}
	
	public void createSplitTeamsDuel(Party party) {
		List<UUID> shuffle = Lists.newArrayList(party.getAllMembersOnline());
        Collections.shuffle(shuffle);
        
        List<UUID> firstTeam = shuffle.subList(0, (int)(shuffle.size() / 2.0));
        List<UUID> secondTeam = shuffle.subList((int)(shuffle.size() / 2.0), shuffle.size());
        
        startDuel(party.getLeader(), party.getLeader(), firstTeam, secondTeam, false, 1);
	}
	
	public void endDuel(Duel duel, int winningTeamNumber) {
		InvView.getInstance().deathMsg(duel, winningTeamNumber);
		
		if (duel.isRanked()) {
			UUID winnerUUID = (winningTeamNumber == 1 ? duel.getFirstTeam().get(0) : duel.getSecondTeam().get(0));
			UUID loserUUID = (winnerUUID == duel.getFirstTeam().get(0) ? duel.getSecondTeam().get(0) : duel.getFirstTeam().get(0));
			
			EloManager.getInstance().tranferElo(winnerUUID, loserUUID);
		}
		
		Iterator<UUID> specIt = duel.getAllSpectators().iterator();
		while (specIt.hasNext()) {
			Player spec = Bukkit.getPlayer(specIt.next());
			PlayerManager sm = PlayerManager.get(spec);
			
			spec.setAllowFlight(false);
			spec.setFlying(false);
			sm.setStatus(PlayerStatus.SPAWN);
			sm.showAllPlayer();
			sm.setSpectate(null);
			spec.teleport(Main.getInstance().getSpawnLocation());
			sm.giveSpawnItem();
			specIt.remove();
		}
		if (duel.getFirstTeamPartyLeaderUUID() != null) {
            Party firstTeamParty = PartyManager.getInstance().getParty(duel.getFirstTeamPartyLeaderUUID());
            if (firstTeamParty != null) {
                firstTeamParty.setPartyState(PartyState.LOBBY);
            }
        }
        if (duel.getSecondTeamPartyLeaderUUID() != null) {
            Party secondTeamParty = PartyManager.getInstance().getParty(duel.getSecondTeamPartyLeaderUUID());
            if (secondTeamParty != null) {
                secondTeamParty.setPartyState(PartyState.LOBBY);
            }
        }
        
        Iterator<UUID> firstIt = duel.getFirstTeam().iterator(); 
		while (firstIt.hasNext()) {
			Player first = Bukkit.getPlayer(firstIt.next());
			if (first == null) continue;
			PlayerManager pmf = PlayerManager.get(first);
			
			first.setScoreboard(Main.getInstance().getServer().getScoreboardManager().getNewScoreboard());
			first.extinguish();
			first.clearPotionEffect();
			
			pmf.setStatus(PlayerStatus.SPAWN);
			pmf.showAllPlayer();
			pmf.resetDuelStats();
			
			EnderDelay.getInstance().removeCooldown(first);
			this.uuidIdentifierToDuel.remove(first.getUniqueId());
		}
		
		Iterator<UUID> secondIt = duel.getSecondTeam().iterator(); 
		while (secondIt.hasNext()) {
			Player second = Bukkit.getPlayer(secondIt.next());
			if (second == null) continue;
			PlayerManager pms = PlayerManager.get(second);
			
			second.setScoreboard(Main.getInstance().getServer().getScoreboardManager().getNewScoreboard());
			second.extinguish();
			second.clearPotionEffect();
			
			pms.setStatus(PlayerStatus.SPAWN);
			pms.showAllPlayer();
			pms.resetDuelStats();
			
			EnderDelay.getInstance().removeCooldown(second);
			this.uuidIdentifierToDuel.remove(second.getUniqueId());
		}
		if (duel.hasRemainingRound() && (duel.getFirstTeam().isEmpty() || duel.getSecondTeam().isEmpty())) {
			duel.sendMessage(ChatColor.RED + "The duel has been cancelled due to an empty team.");
			return;
		}
		if (duel.hasRemainingRound() && !duel.getFirstTeam().isEmpty() && !duel.getSecondTeam().isEmpty()) {
			startDuel(duel.getFirstTeamPartyLeaderUUID(), duel.getSecondTeamPartyLeaderUUID(), duel.getFirstTeam(), duel.getSecondTeam(), duel.isRanked(), duel.getRound());
		}
	}
	
	public void sendWaitingMessage(Duel duel) {
		Map<Duel, Integer> cooldown = Maps.newHashMap();
		List<UUID> duelPlayers = Lists.newArrayList(duel.getFirstTeamAlive());
		duelPlayers.addAll(duel.getSecondTeamAlive());
		
		cooldown.put(duel, 5);
		
		new BukkitRunnable() {
			int num = cooldown.get(duel);
			
			@Override
			public void run() {
				if (!cooldown.containsKey(duel)) {
					this.cancel();
				}
				if (!duel.isValid()) {
					this.cancel();
					duel.sendMessage(ChatColor.RED + "The current duel has been cancelled due to his invalidity.");
					endDuel(duel, (duel.getFirstTeamAlive().isEmpty() ? 2 : 1));
				}
				if (num <= 0) {
					duel.sendSoundedMessage(ChatColor.GREEN + "Duel has stated!", Sound.FIREWORK_BLAST);
					for (UUID uuid : duelPlayers) {
						Player player = Bukkit.getPlayer(uuid);
						if (player == null) continue;
						PlayerManager.get(player).setStatus(PlayerStatus.DUEL);
					}
					duel.showDuelPlayer();
					cooldown.remove(duel);
					duelPlayers.clear();
					this.cancel();
				}
				if (num > 0) {
					duel.sendSoundedMessage(ChatColor.DARK_AQUA + "Duel start in " + ChatColor.YELLOW + num + ChatColor.DARK_AQUA + " second" + (num > 1 ? "s.." : ".."), Sound.NOTE_PLING);
					cooldown.put(duel, num--);
				}
			}
		}.runTaskTimer(Main.getInstance(), 15L, 20L);
	}
	
	private void teleportRandomArena(Duel duel) {
		Random random = new Random();
		int pickedArena = random.nextInt(Main.getInstance().arenaList.size()) + 1;
		
		for (UUID firstUUID : duel.getFirstTeam()) {
			Player first = Bukkit.getPlayer(firstUUID);
			
			if (first == null) continue;
			
			this.uuidIdentifierToDuel.put(firstUUID, duel);
			PlayerManager pmf = PlayerManager.get(first);
			
			pmf.heal();
			first.setNoDamageTicks(50);
			
			pmf.hideAllPlayer();
			pmf.giveKit();
			
			first.teleport(Main.getInstance().arenaList.get(pickedArena)[0]);
			first.setSneaking(false);
		}
		for (UUID secondUUID : duel.getSecondTeam()) {
			Player second = Bukkit.getPlayer(secondUUID);
			
			if (second == null) continue;
			
			this.uuidIdentifierToDuel.put(secondUUID, duel);
			PlayerManager pms = PlayerManager.get(second);
			
			pms.heal();
			second.setNoDamageTicks(50);
			
			pms.hideAllPlayer();
			pms.giveKit();
			
			second.teleport(Main.getInstance().arenaList.get(pickedArena)[1]);
			second.setSneaking(false);
		}
		sendWaitingMessage(duel);
	}
	
	public void removePlayerFromDuel(Player player, boolean disconnect) {
		Duel currentDuel = getDuelFromPlayerUUID(player.getUniqueId());
		
		if (currentDuel == null) return;
		this.uuidIdentifierToDuel.remove(player.getUniqueId());
		
		InvView.getInstance().saveInv(player);
		currentDuel.sendMessage(player.getName() + " has been killed" + ((player.getKiller() != null) ? (" by " + player.getKiller().getName()) : ""));
		
		if (currentDuel.getFirstTeamAlive().isEmpty()) {
			for (UUID lastPlayersUUID : currentDuel.getSecondTeamAlive()) {
                Player lastPlayers = Bukkit.getPlayer(lastPlayersUUID);
                InvView.getInstance().saveInv(lastPlayers);
                
                if (lastPlayers == null) continue;
                
                lastPlayers.setHealth(20.0D);
                lastPlayers.setFoodLevel(20);
                lastPlayers.setSaturation(10000f);
        			
                if (currentDuel.hasRemainingRound()) {
                	continue;
                }
                	
                new BukkitRunnable() {
        				
                	@Override
                	public void run() {
                		if (lastPlayers != null) {
                			lastPlayers.teleport(Main.getInstance().getSpawnLocation());
                			PlayerManager.get(lastPlayers).giveSpawnItem();
                		}
                	}
                }.runTaskLater(Main.getInstance(), 40L);
            }
			endDuel(currentDuel, 2);
		} else if (currentDuel.getSecondTeamAlive().isEmpty()) {
			for (UUID lastPlayersUUID : currentDuel.getFirstTeamAlive()) {
                Player lastPlayers = Bukkit.getPlayer(lastPlayersUUID);
                InvView.getInstance().saveInv(lastPlayers);
                
                if (lastPlayers == null) continue;
                
                lastPlayers.setHealth(20.0D);
                lastPlayers.setFoodLevel(20);
                lastPlayers.setSaturation(10000f);
        			
                if (currentDuel.hasRemainingRound()) {
                	continue;
                }
                	
                new BukkitRunnable() {
        				
                	@Override
                	public void run() {
                		if (lastPlayers != null) {
                			lastPlayers.teleport(Main.getInstance().getSpawnLocation());
                			PlayerManager.get(lastPlayers).giveSpawnItem();
                		}
                	}
                }.runTaskLater(Main.getInstance(), 40L);
            }
			endDuel(currentDuel, 1);
		}
		if (currentDuel.getFirstTeam().contains(player.getUniqueId())) {
			currentDuel.killFirstTeamPlayer(player.getUniqueId());
			if (disconnect) currentDuel.removeFirstTeamPlayer(player.getUniqueId());
		} else {
			currentDuel.killSecondTeamPlayer(player.getUniqueId());
			if (disconnect) currentDuel.removeSecondTeamPlayer(player.getUniqueId());
		}
	}
}
