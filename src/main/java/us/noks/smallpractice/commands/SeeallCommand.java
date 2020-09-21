package us.noks.smallpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SeeallCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		if (!sender.hasPermission("command.seeall")) {
			sender.sendMessage(ChatColor.RED + "No permission.");
			return false;
		}
		if (args.length != 0) {
			sender.sendMessage(ChatColor.RED + "Usage: /seeall");
			return false;
		}
		Player player = (Player) sender;
		for (Player allPlayers : Bukkit.getOnlinePlayers()) {
			player.showPlayer(allPlayers);
		}
		player.sendMessage(ChatColor.GREEN + "You see everyone right now.");
		return true;
	}
}
