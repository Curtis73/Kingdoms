package com.songoda.kingdoms.command.commands;

import java.util.Arrays;

import org.bukkit.command.CommandSender;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.command.AbstractCommand;
import com.songoda.kingdoms.command.AdminCommand;
import com.songoda.kingdoms.utils.Formatting;
import com.songoda.kingdoms.utils.MessageBuilder;

public class KingdomsCommand extends AbstractCommand {
	
	public KingdomsCommand() {
		super(true, "kingdoms", "k", "kingdom");
	}

	@Override
	protected ReturnType runCommand(Kingdoms instance, CommandSender sender, String... args) {
		sender.sendMessage("");
		new MessageBuilder("messages.version")
				.replace("%version%", instance.getDescription().getVersion())
				.send(sender);
		for (AbstractCommand command : instance.getCommandHandler().getCommands()) {
			if (command instanceof AdminCommand)
				continue;
			if (command.getConfigurationNode() == null)
				continue;
			if (command.getPermissionNodes() == null || Arrays.stream(command.getPermissionNodes()).parallel().anyMatch(permission -> sender.hasPermission(permission))) {
				sender.sendMessage(Formatting.color("&8 - &6" + command.getSyntax(sender) + "&7 - " + command.getDescription(sender)));
			}
		}
		sender.sendMessage("");
		return ReturnType.SUCCESS;
	}

	@Override
	public String[] getPermissionNodes() {
		return null;
	}

	@Override
	public String getConfigurationNode() {
		return "kingdoms";
	}

}
