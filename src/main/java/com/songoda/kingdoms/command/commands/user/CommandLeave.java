package com.songoda.kingdoms.command.commands.user;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.command.AbstractCommand;
import com.songoda.kingdoms.events.MemberLeaveEvent;
import com.songoda.kingdoms.manager.inventories.ConfirmationManager;
import com.songoda.kingdoms.manager.managers.KingdomManager;
import com.songoda.kingdoms.manager.managers.PlayerManager;
import com.songoda.kingdoms.objects.kingdom.Kingdom;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.utils.MessageBuilder;

public class CommandLeave extends AbstractCommand {

	public CommandLeave() {
		super(false, "leave", "part");
	}

	@Override
	protected ReturnType runCommand(Kingdoms instance, CommandSender sender, String... arguments) {
		Player player = (Player) sender;
		KingdomPlayer kingdomPlayer = instance.getManager(PlayerManager.class).getKingdomPlayer(player);
		Kingdom kingdom = kingdomPlayer.getKingdom();
		if (kingdom == null) {
			new MessageBuilder("commands.leave.no-kingdom")
					.setPlaceholderObject(kingdomPlayer)
					.send(player);
			return ReturnType.FAILURE;
		}
		if (arguments.length != 0)
			return ReturnType.SYNTAX_ERROR;
		instance.getManager(ConfirmationManager.class).openConfirmation(kingdomPlayer, result -> {
			if (!result) {
				new MessageBuilder("commands.leave.cancelled")
						.setPlaceholderObject(kingdomPlayer)
						.send(kingdomPlayer);
				return;
			}
			new MessageBuilder("commands.leave.leave-broadcast")
					.setPlaceholderObject(kingdomPlayer)
					.send(kingdom.getOnlinePlayers());
			new MessageBuilder("commands.leave.left")
					.setPlaceholderObject(kingdomPlayer)
					.send(kingdomPlayer);
			kingdomPlayer.onKingdomLeave();
			kingdomPlayer.setKingdom(null);
			kingdomPlayer.setRank(null);
			instance.getManager(PlayerManager.class).save(kingdomPlayer);
			KingdomManager kingdomManager = instance.getManager(KingdomManager.class);
			kingdomManager.onPlayerLeave(kingdomPlayer, kingdom);
			kingdom.removeMember(kingdomPlayer);
			if (kingdom.getMembers().isEmpty()) {
				kingdomManager.deleteKingdom(kingdom.getName());
				return;
			}
			Bukkit.getPluginManager().callEvent(new MemberLeaveEvent(kingdomPlayer, kingdom));
		});
		return ReturnType.SUCCESS;
	}

	@Override
	public String getConfigurationNode() {
		return "leave";
	}

	@Override
	public String[] getPermissionNodes() {
		return null;
	}

}
