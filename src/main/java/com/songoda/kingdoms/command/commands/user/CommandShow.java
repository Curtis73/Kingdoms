package com.songoda.kingdoms.command.commands.user;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.command.AbstractCommand;
import com.songoda.kingdoms.manager.managers.KingdomManager;
import com.songoda.kingdoms.manager.managers.PlayerManager;
import com.songoda.kingdoms.objects.kingdom.Kingdom;
import com.songoda.kingdoms.objects.kingdom.OfflineKingdom;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.objects.player.OfflineKingdomPlayer;
import com.songoda.kingdoms.utils.ListMessageBuilder;
import com.songoda.kingdoms.utils.MessageBuilder;

public class CommandShow extends AbstractCommand {
	//INFO shows information about YOUR kingdom (or other KINGDOMS), and takes a KINGDOM as input.
	//SHOW shows information about OTHER PLAYER's kingdoms, and takes a PLAYER'S username as input.
	public CommandShow() {
		super(false, "show");
	}

	@Override
	protected ReturnType runCommand(Kingdoms instance, CommandSender sender, String... arguments) {
		Player player = (Player) sender;
		KingdomPlayer kingdomPlayer = instance.getManager(PlayerManager.class).getKingdomPlayer(player);
		if (arguments.length > 0) {
			String name = String.join(" ", arguments);
			//Optional<OfflineKingdom> find = instance.getManager(KingdomManager.class).getOfflineKingdom(name);
			OfflineKingdomPlayer find = instance.getManager(PlayerManager.class).getOfflineKingdomPlayer(Bukkit.getOfflinePlayer(name)).get();
			Optional<OfflineKingdom> fin2d = instance.getManager(KingdomManager.class).getOfflineKingdom(find.getKingdom().getName());
			if (!fin2d.isPresent()) {
				new MessageBuilder("commands.info.no-kingdom-found")
						.setPlaceholderObject(kingdomPlayer)
						.replace("%kingdom%", name)
						.send(player);
				return ReturnType.FAILURE;
			}
			new ListMessageBuilder(false, "commands.info.info")
					.setKingdom(find.getKingdom())
					.send(player);
			return ReturnType.SUCCESS;
		}
		Kingdom kingdom = kingdomPlayer.getKingdom();
		if (kingdom == null) {
			new MessageBuilder("commands.info.no-kingdom")
					.setPlaceholderObject(kingdomPlayer)
					.send(player);
			return ReturnType.FAILURE;
		}
		new ListMessageBuilder(false, "commands.info.info")
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.send(player);
		return ReturnType.SUCCESS;
	}

	@Override
	public String getConfigurationNode() {
		return "info";
	}

	@Override
	public String[] getPermissionNodes() {
		return new String[] {"kingdoms.info", "kingdoms.player"};
	}

}
