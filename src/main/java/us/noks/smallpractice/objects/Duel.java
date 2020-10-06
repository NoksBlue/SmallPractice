package us.noks.smallpractice.objects;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import us.noks.smallpractice.arena.Arena.Arenas;
import us.noks.smallpractice.enums.PlayerStatus;
import us.noks.smallpractice.objects.managers.PlayerManager;

public class Duel {
	private Arenas arena;
	private UUID firstTeamPartyLeaderUUID;
    private UUID secondTeamPartyLeaderUUID;
	private List<UUID> firstTeam;
	private List<UUID> secondTeam;
	private List<UUID> firstTeamAlive;
	private List<UUID> secondTeamAlive;
	private boolean ranked;
	private List<UUID> spectators = Lists.newArrayList();
	private int timeBeforeDuel = 5;
	private List<Item> drops;
	
	public Duel(Arenas arena, UUID firstTeamPartyLeaderUUID, UUID secondTeamPartyLeaderUUID, List<UUID> firstTeam, List<UUID> secondTeam, boolean ranked) {
		this.arena = arena;
		this.firstTeamPartyLeaderUUID = firstTeamPartyLeaderUUID;
		this.secondTeamPartyLeaderUUID = secondTeamPartyLeaderUUID;
		this.firstTeam = Lists.newArrayList(firstTeam);
		this.secondTeam = Lists.newArrayList(secondTeam);
		this.firstTeamAlive = Lists.newArrayList(firstTeam);
		this.secondTeamAlive = Lists.newArrayList(secondTeam);
		this.ranked = ranked;
		this.drops = Lists.newArrayList();
	}
	
	public Arenas getArena() {
		return this.arena;
	}
	
	public List<UUID> getFirstTeam() {
		return firstTeam;
	}
	
	public List<UUID> getSecondTeam() {
		return secondTeam;
	}
	
	public List<UUID> getFirstAndSecondTeams() {
		List<UUID> teams = Lists.newArrayList(this.firstTeam);
		teams.addAll(this.secondTeam);
		return teams;
	}
	
	public List<UUID> getFirstTeamAlive() {
		return firstTeamAlive;
	}
	
	public List<UUID> getSecondTeamAlive() {
		return secondTeamAlive;
	}
	
	public void killFirstTeamPlayer(UUID killedUUID) {
		this.firstTeamAlive.remove(killedUUID);
	}
	
	public void killSecondTeamPlayer(UUID killedUUID) {
		this.secondTeamAlive.remove(killedUUID);
	}

	public boolean isRanked() {
		return ranked;
	}

	public void setRanked(boolean ranked) {
		this.ranked = ranked;
	}
	
	public void addSpectator(UUID spec) {
		this.spectators.add(spec);
	}
	
	public void removeSpectator(UUID spec) {
		this.spectators.remove(spec);
	}
	
	public boolean hasSpectator() {
		return !this.spectators.isEmpty();
	}
	
	public List<UUID> getAllSpectators() {
		return this.spectators;
	}
	
	public void sendMessage(String message) {
		sendSoundedMessage(message, null);
	}
	
	public void sendSoundedMessage(String message, Sound sound) {
		sendSoundedMessage(message, sound, 1.0f, 1.0f);
	}
	public void sendSoundedMessage(String message, Sound sound, float volume, float pitch) {
		List<UUID> duelPlayers = Lists.newArrayList(getFirstAndSecondTeams());
		duelPlayers.addAll(getAllSpectators());
		
		for (UUID uuid : duelPlayers) {
			final Player player = Bukkit.getPlayer(uuid);
			if (player == null) continue;
			
			player.sendMessage(message);
			if (sound != null) player.playSound(player.getLocation(), sound, volume, pitch);
		}
		duelPlayers.clear();
	}
	
	public void showDuelPlayer() {
		if (!isValid()) {
			return;
		}
		for (UUID firstUUID : this.firstTeamAlive) {
			for (UUID secondUUID : this.secondTeamAlive) {
                Player first = Bukkit.getPlayer(firstUUID);
                Player second = Bukkit.getPlayer(secondUUID);
				first.showPlayer(second);
				second.showPlayer(first);
			}
		}
	}

	public UUID getFirstTeamPartyLeaderUUID() {
		return firstTeamPartyLeaderUUID;
	}

	public void setFirstTeamPartyLeaderUUID(UUID firstTeamPartyLeaderUUID) {
		this.firstTeamPartyLeaderUUID = firstTeamPartyLeaderUUID;
	}

	public UUID getSecondTeamPartyLeaderUUID() {
		return secondTeamPartyLeaderUUID;
	}

	public void setSecondTeamPartyLeaderUUID(UUID secondTeamPartyLeaderUUID) {
		this.secondTeamPartyLeaderUUID = secondTeamPartyLeaderUUID;
	}
	
	public boolean containPlayer(Player player) {
		Preconditions.checkNotNull(player, "Player cannot be null");
		return (this.firstTeam.contains(player.getUniqueId()) || this.secondTeam.contains(player.getUniqueId()));
	}
	
	public boolean isValid() {
		return (!this.firstTeam.isEmpty() && !this.secondTeam.isEmpty() && !this.firstTeamAlive.isEmpty() && !this.secondTeamAlive.isEmpty());
	}
	
	public void setDuelPlayersStatusTo(PlayerStatus status) {
		for (UUID playersUUID : getFirstAndSecondTeams()) {
			PlayerManager.get(playersUUID).setStatus(status);
		}
	}
	
	public int getTimeBeforeDuel() {
		return this.timeBeforeDuel;
	}
	
	public void addDrops(List<ItemStack> item) {
		for (ItemStack items : item) {
			this.drops.add((Item) items);
		}
	}
	
	public void addDrops(Item item) {
		this.drops.add(item);
	}
	
	public boolean containDrops(Item item) {
		return this.drops.contains(item);
	}
	
	public void clearDrops() {
		Iterator<Item> it = this.drops.iterator();
		while (it.hasNext()) {
			Item items = it.next();
			items.remove();
		}
	}
}
