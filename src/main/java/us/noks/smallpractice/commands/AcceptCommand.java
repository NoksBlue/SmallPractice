package us.noks.smallpractice.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.noks.smallpractice.Main;
import us.noks.smallpractice.utils.Messages;

public class AcceptCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			
			if (args.length != 1) {
				player.sendMessage(ChatColor.RED + "Usage: /accept <player>");
				return false;
			}
			Player dueler = Bukkit.getPlayer(args[0]);
			
			if (dueler == null) {
				player.sendMessage(Messages.PLAYER_NOT_ONLINE);
				return false;
			}
			if (dueler == player) {
				player.sendMessage(Messages.NOT_YOURSELF);
				return false;
			}
			Main.getInstance().acceptDuelRequest(player, dueler);
		}
		return true;
	}
}
