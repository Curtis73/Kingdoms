package com.songoda.kingdoms.placeholders;

import java.util.Optional;

import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.manager.Manager;
import com.songoda.kingdoms.objects.kingdom.OfflineKingdom;
import com.songoda.kingdoms.objects.player.OfflineKingdomPlayer;
import com.songoda.kingdoms.utils.LocationUtils;

public class DefaultPlaceholders extends Manager {

	static {
		registerManager("defaultplaceholders", new DefaultPlaceholders());
		Placeholders.registerPlaceholder(new SimplePlaceholder("%prefix%") {
			@Override
			public String get() {
				Optional<FileConfiguration> messages = Kingdoms.getInstance().getConfiguration("messages");
				if (messages.isPresent())
					return messages.get().getString("messages.prefix", "&7[&6Kingdoms&7] &r");
				return "&7[&6Kingdoms&7] &r";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%points%", "%resourcepoints%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				return player.getKingdom().getResourcePoints() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%points%", "%resourcepoints%") {
			@Override
			public String replace(OfflineKingdom Kingdom) {
				return Kingdom.getResourcePoints() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%kingdom%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				return player.getKingdom().getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%claims%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				return player.getKingdom().getClaims() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%player%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				return player.getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%rank%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				return player.getRank().getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%claims%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.getClaims() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%might%") {
			@Override
			public String replace(OfflineKingdom Kingdom) {
				return Kingdom.getMight() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<CommandSender>("%sender%") {
			@Override
			public String replace(CommandSender sender) {
				return sender.getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%lore%") {
			@Override
			public String replace(OfflineKingdom Kingdom) {
				return Kingdom.getLore();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<Player>("%player%") {
			@Override
			public String replace(Player player) {
				return player.getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<String>("%string%") {
			@Override
			public String replace(String string) {
				return string;
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<Chunk>("%chunk%") {
			@Override
			public String replace(Chunk chunk) {
				return LocationUtils.chunkToString(chunk);
			}
		});
	}
	
	protected DefaultPlaceholders() {
		super(false);
	}

	@Override
	public void onDisable() {}
	
}