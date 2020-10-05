package us.noks.smallpractice.enums;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Ladders {
	NODEBUFF("NoDebuff", ChatColor.AQUA, new ItemStack(Material.POTION, 1, (short) 16421), true),
	SUMO("Sumo", ChatColor.YELLOW, new ItemStack(Material.STICK, 1), false),
	ARCHER("Archer", ChatColor.RED, new ItemStack(Material.BOW, 1), false),
	AXE("Axe", ChatColor.GREEN, new ItemStack(Material.IRON_AXE, 1), false),
	SOUP("Soup", ChatColor.GOLD, new ItemStack(Material.MUSHROOM_SOUP, 1), false);
	
	private String name;
	private ChatColor color;
	private ItemStack icon;
	private boolean enable; // this will be removed
	
	Ladders(String name, ChatColor color, ItemStack icon, boolean enable) {
		this.name = name;
		this.color = color;
		this.icon = icon;
		this.enable = enable;
	}
	
	public String getName() {
		return this.name;
	}
	
	public ChatColor getColor() {
		return this.color;
	}
	
	public ItemStack getIcon() {
		return this.icon;
	}
	
	public boolean isEnable() {
		return this.enable;
	}
}
