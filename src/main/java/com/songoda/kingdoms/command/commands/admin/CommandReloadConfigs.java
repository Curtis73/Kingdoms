package com.songoda.kingdoms.command.commands.admin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.command.AdminCommand;


public class CommandReloadConfigs extends AdminCommand {

	//19/02/2020
	
	public CommandReloadConfigs() {
		super(true, "reload");
	}

	@Override
	protected ReturnType runCommand(Kingdoms instance, CommandSender sender, String... arguments) {
		Player player = (Player) sender;
		for (String name : Arrays.asList("config", "messages", "turrets", "structures", "defender-upgrades", "ranks", "arsenal-items", "inventories", "powerups", "misc-upgrades", "map", "sounds")) {
			FileConfiguration configuration = instance.getConfiguration(name).get();
			File file = new File(instance.getDataFolder(), name + ".yml");
			try {
				player.sendMessage(ChatColor.GREEN + "Reloading " + name);
				configuration.load(file);
			} catch (IOException | InvalidConfigurationException e) {
				player.sendMessage(ChatColor.RED + "There was an error reloading " + name);
				e.printStackTrace();
			}}
		return ReturnType.SUCCESS;
	}

	@Override
	public String[] getPermissionNodes() {
		return new String[] {"kingdoms.admin"};
	}

	@Override
	public String getConfigurationNode() {
		return "reload";
	}

}
