package com.songoda.kingdoms.command.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.command.AdminCommand;
import com.songoda.kingdoms.manager.managers.PlayerManager;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.utils.ListMessageBuilder;
import com.songoda.kingdoms.utils.MessageBuilder;

public class CommandAdminToggle extends AdminCommand {
	public CommandAdminToggle() {
		super(false, "adminmode");
	}

	@Override
	protected ReturnType runCommand(Kingdoms instance, CommandSender sender, String... arguments) {
		Player player = (Player) sender;
		KingdomPlayer kingdomPlayer = instance.getManager(PlayerManager.class).getKingdomPlayer(player);
			if (kingdomPlayer.hasAdminMode()) {
				kingdomPlayer.setAdminMode(false);
				new MessageBuilder("commands.admintoggle.toggle-off")
						.send(player);
				return ReturnType.SUCCESS;
			} else if (!kingdomPlayer.hasAdminMode())
				kingdomPlayer.setAdminMode(true);
			new MessageBuilder("commands.admintoggle.toggle-on")
					.send(player);
			return ReturnType.SUCCESS;
	}

	@Override
	public String getConfigurationNode() {
		return "admintoggle";
	}

	@Override
	public String[] getPermissionNodes() {
		return new String[] {"kingdoms.adminmode", "kingdoms.admin"};
	}

}
