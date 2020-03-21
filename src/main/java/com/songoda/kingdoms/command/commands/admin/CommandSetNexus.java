package com.songoda.kingdoms.command.commands.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.command.AdminCommand;
import com.songoda.kingdoms.manager.managers.NexusManager;
import com.songoda.kingdoms.manager.managers.PlayerManager;
import com.songoda.kingdoms.objects.kingdom.Kingdom;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.utils.MessageBuilder;

public class CommandSetNexus extends AdminCommand {
	public CommandSetNexus() {
		super(false, "setnexus");
	}

	@Override
	protected ReturnType runCommand(Kingdoms instance, CommandSender sender, String... arguments) {
		Player player = (Player) sender;
		KingdomPlayer kingdomPlayer = instance.getManager(PlayerManager.class).getKingdomPlayer(player);
		Kingdom kingdom = kingdomPlayer.getKingdom();
		if (kingdom == null) {
			new MessageBuilder("commands.info.no-kingdom")
					.setPlaceholderObject(kingdomPlayer)
					.send(player);
			return ReturnType.FAILURE;
		}
		//kingdom.setNexusLocation(kingdomPlayer.getLocation());
		instance.getManager(NexusManager.class).startNexusSet(player.getUniqueId());
		//new ListMessageBuilder(false, "commands.setnexus.set")
		//		.setPlaceholderObject(kingdomPlayer)
		//		.setKingdom(kingdom)
		//		.send(player);
		return ReturnType.SUCCESS;
	}

	@Override
	public String getConfigurationNode() {
		return "nexus";
	}

	@Override
	public String[] getPermissionNodes() {
		return new String[] {"kingdoms.setnexus", "kingdoms.admin"};
	}

}
