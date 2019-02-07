package us.noks.smallpractice;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import us.noks.smallpractice.commands.AcceptCommand;
import us.noks.smallpractice.commands.BuildCommand;
import us.noks.smallpractice.commands.DuelCommand;
import us.noks.smallpractice.commands.InventoryCommand;
import us.noks.smallpractice.commands.ModerationCommand;
import us.noks.smallpractice.commands.PingCommand;
import us.noks.smallpractice.commands.ReportCommand;
import us.noks.smallpractice.commands.SeeallCommand;
import us.noks.smallpractice.commands.SpawnCommand;
import us.noks.smallpractice.commands.SpectateCommand;
import us.noks.smallpractice.enums.PlayerStatus;
import us.noks.smallpractice.listeners.ChatListener;
import us.noks.smallpractice.listeners.DuelListener;
import us.noks.smallpractice.listeners.EnderDelay;
import us.noks.smallpractice.listeners.PlayerListener;
import us.noks.smallpractice.listeners.ServerListeners;
import us.noks.smallpractice.objects.managers.DuelManager;
import us.noks.smallpractice.objects.managers.PlayerManager;
import us.noks.smallpractice.utils.InvView;

public class Main extends JavaPlugin {
	
	private Location arena1_Pos1, arena2_Pos1;
	private Location arena1_Pos2, arena2_Pos2;
	private Location spawnLocation;
	
	public List<Player> queue = Lists.newArrayList();
	public Map<Integer, Location[]> arenaList = Maps.newConcurrentMap();
	
	public static Main instance;
	public static Main getInstance() {
		return instance;
	}
	
	@Override
	public void onEnable() {
		instance = this;
		
		setupArena();
		registerCommands();
		registerListers();
	}
	
	@Override
	public void onDisable() {
		this.queue.clear();
		this.arenaList.clear();
	}
	
	private void setupArena() {
		arena1_Pos1 = new Location(Bukkit.getWorld("world"), -549.5D, 4.0D, 113.5D, 90.0F, 0.0F);
	    arena1_Pos2 = new Location(Bukkit.getWorld("world"), -608.5D, 4.0D, 115.5D, -90.0F, -1.0F);
	    arena2_Pos1 = new Location(Bukkit.getWorld("world"), 72.5D, 4.0D, 74.5D, 0.0F, 0.0F);
	    arena2_Pos2 = new Location(Bukkit.getWorld("world"), 70.5D, 4.0D, 154.5D, 180.0F, 0.0F);
		spawnLocation = new Location(Bukkit.getWorld("world"), -215.5D, 6.5D, 84.5D, 180.0F, 0.0F);
		
		arenaList.put(1, new Location[] {arena1_Pos1, arena1_Pos2});
		arenaList.put(2, new Location[] {arena2_Pos1, arena2_Pos2});
	}
	
	private void registerCommands() {
		getCommand("duel").setExecutor(new DuelCommand());
		getCommand("accept").setExecutor(new AcceptCommand());
		getCommand("build").setExecutor(new BuildCommand());
		getCommand("ping").setExecutor(new PingCommand());
		getCommand("inventory").setExecutor(new InventoryCommand());
		getCommand("spawn").setExecutor(new SpawnCommand());
		getCommand("seeall").setExecutor(new SeeallCommand());
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("spectate").setExecutor(new SpectateCommand());
		getCommand("mod").setExecutor(new ModerationCommand());
	}
	
	private void registerListers() {
		PluginManager pm = Bukkit.getPluginManager();
		
		pm.registerEvents(new PlayerListener(), this);
		pm.registerEvents(new ServerListeners(), this);
		pm.registerEvents(new EnderDelay(), this);
		pm.registerEvents(new InvView(), this);
		pm.registerEvents(new ChatListener(), this);
		pm.registerEvents(new DuelListener(), this);
	}

	public void sendDuelRequest(Player requester, Player requested) {
		if (PlayerManager.get(requester).getStatus() != PlayerStatus.SPAWN || PlayerManager.get(requested).getStatus() != PlayerStatus.SPAWN) {
			requester.sendMessage(ChatColor.RED + "Either you or that player is not in spawn!");
			return;
		}
		PlayerManager.get(requester).setRequestTo(requested);
		
		TextComponent l1 = new TextComponent();
		l1.setText(requester.getName());
		l1.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
	    
		TextComponent l1a = new TextComponent();
		l1a.setText(" has requested to duel you! ");
		l1a.setColor(net.md_5.bungee.api.ChatColor.DARK_AQUA);
	    
		TextComponent l1b = new TextComponent();
		l1b.setText("Click here to accept.");
		l1b.setColor(net.md_5.bungee.api.ChatColor.GREEN);
		l1b.setBold(true);
		l1b.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(net.md_5.bungee.api.ChatColor.GREEN + "Click this message to accept " + requester.getName()).create()));
		l1b.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accept " + requester.getName()));
	    
		l1.addExtra(l1a);
		l1.addExtra(l1b);
	    
		requested.spigot().sendMessage(l1);
		requester.sendMessage(ChatColor.DARK_AQUA + "You sent a duel request to " + ChatColor.YELLOW + requested.getName());
	}
	
	public void acceptDuelRequest(Player requested, Player requester) {
		if (PlayerManager.get(requester).getStatus() != PlayerStatus.SPAWN || PlayerManager.get(requested).getStatus() != PlayerStatus.SPAWN) {
			requester.sendMessage(ChatColor.RED + "Either you or this player is not in spawn!");
			return;
		}
		if (!PlayerManager.get(requester).hasRequest(requested)) {
			requested.sendMessage(ChatColor.RED + "This player doesn't request you to duel!");
			return;
		}
		List<UUID> firstTeam = Lists.newArrayList();
		firstTeam.add(requester.getUniqueId());
		List<UUID> secondTeam = Lists.newArrayList();
		secondTeam.add(requested.getUniqueId());
		
		DuelManager.getInstance().startDuel(null, null, firstTeam, secondTeam);
	}
	
	public void addQueue(Player p) {
		if (PlayerManager.get(p).getStatus() != PlayerStatus.SPAWN) {
			return;
		}
		if (!this.queue.contains(p)) {
			this.queue.add(p);
			PlayerManager.get(p).setStatus(PlayerStatus.QUEUE);
			if (this.queue.size() == 1) {
				PlayerManager.get(p).giveQueueItem();
			}
			p.sendMessage(ChatColor.GREEN + "You have been added to the queue. Waiting for another player..");
		}
		if (this.queue.size() == 1 && this.queue.contains(p)) {
			addQueue(p);
		} else if (this.queue.size() == 2) {
			Player p1 = this.queue.get(0);
			Player p2 = this.queue.get(1);
			
			if (p1 == p && p2 == p1) {
				this.queue.clear();
				addQueue(p);
				return;
			}
			List<UUID> firstTeam = Lists.newArrayList();
			firstTeam.add(p1.getUniqueId());
			List<UUID> secondTeam = Lists.newArrayList();
			secondTeam.add(p2.getUniqueId());
			
			DuelManager.getInstance().startDuel(null, null, firstTeam, secondTeam);
			this.queue.remove(p1);
			this.queue.remove(p2);
		}
	}
	
	public void quitQueue(Player p) {
		if (this.queue.contains(p)) {
			this.queue.remove(p);
			PlayerManager.get(p).setStatus(PlayerStatus.SPAWN);
			PlayerManager.get(p).giveSpawnItem();
			p.sendMessage(ChatColor.RED + "You have been removed from the queue.");
		}
	}
	
	public Location getSpawnLocation() {
		return this.spawnLocation;
	}
}
