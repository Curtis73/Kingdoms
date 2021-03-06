package com.songoda.kingdoms.placeholders;

import java.util.Optional;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.manager.managers.RankManager;
import com.songoda.kingdoms.manager.managers.RankManager.Rank;
import com.songoda.kingdoms.objects.kingdom.Kingdom;
import com.songoda.kingdoms.objects.kingdom.OfflineKingdom;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.objects.player.OfflineKingdomPlayer;
import com.songoda.kingdoms.utils.Formatting;
import com.songoda.kingdoms.utils.LocationUtils;
import com.songoda.kingdoms.utils.MessageBuilder;

public class DefaultPlaceholders {

	private static boolean initalized;

	public static boolean hasInitalized() {
		return initalized;
	}

	public static void initalize() {
		if (initalized)
			return;
		initalized = true;
		Kingdoms instance = Kingdoms.getInstance();
		FileConfiguration configuration = instance.getConfig();
		RankManager rankManager = instance.getManager(RankManager.class);
		Placeholders.registerPlaceholder(new SimplePlaceholder("%prefix%") {
			@Override
			public String get() {
				Optional<FileConfiguration> messages = instance.getConfiguration("messages");
				if (messages.isPresent())
					return messages.get().getString("messages.prefix", "&7[&6Kingdoms&7] &r");
				return "&7[&6Kingdoms&7] &r";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%kingdom%", "%playerkingdom%", "%player-kingdom%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				OfflineKingdom kingdom = player.getKingdom();
				if (kingdom == null)
					return Formatting.color("&c&lNo Kingdom");
				return kingdom.getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%rank%", "%playerrank%", "player-rank") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				Rank rank = player.getRank();
				if (rank == null)
					return "No rank";
				return rank.getName();
			}
		});

		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%rankcolor%", "%rankcolour%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				Rank rank = player.getRank();
				if (rank == null)
					return ChatColor.WHITE + "";
				return rank.getColor() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%offlinecount%", "%membercount%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.getMembers().size() - kingdom.getKingdom().getOnlinePlayers().size() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%maxmembers%", "%max-members%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				int max = kingdom.getMaxMembers();
				String string = max + "";
				if (max <= 0)
					string = "\u221E";
				return string;
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%points%", "%resourcepoints%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.getResourcePoints() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%online-state%", "%online%") { //FIXME displaying online whether they're offline or not
			@Override
			public String replace(OfflineKingdom kingdom) {
				if (kingdom.isOnline()) {
					return new MessageBuilder(false, "kingdoms.online")
							.setKingdom(kingdom)
							.get();
				} else {
					return new MessageBuilder(false, "kingdoms.offline")
							.setKingdom(kingdom)
							.get();
				}
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%description%", "%lore%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.getLore();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%owner%", "%king%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				Optional<OfflineKingdomPlayer> owner = kingdom.getOwner();
				if (!owner.isPresent())
					return "";
				return owner.get().getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdomPlayer>("%player%") {
			@Override
			public String replace(OfflineKingdomPlayer player) {
				return player.getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%onlinecount%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.getKingdom().getOnlinePlayers().size() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%kingdom%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%members%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				Set<OfflineKingdomPlayer> members = kingdom.getMembers();
				if (members.isEmpty())
					return "";
				return rankManager.list(members);
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<Kingdom>("%onlinemembers%") {
			@Override
			public String replace(Kingdom kingdom) {
				Set<KingdomPlayer> members = kingdom.getOnlinePlayers();
				if (members.isEmpty())
					return "";
				return rankManager.listOnline(members);
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%claims%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.getClaims().size() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%enemies%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				StringBuilder builder = new StringBuilder();
				Set<OfflineKingdom> enemies = kingdom.getEnemies();
				if (enemies.isEmpty())
					return "No Enemies";
				enemies.forEach(enemy -> builder.append(enemy.getName()));
				return builder.toString();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%allies%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				StringBuilder builder = new StringBuilder();
				Set<OfflineKingdom> allies = kingdom.getAllies();
				if (allies.isEmpty())
					return "No Alliances";
				allies.forEach(ally -> builder.append(ally.getName()));
				return builder.toString();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<OfflineKingdom>("%neutral%") {
			@Override
			public String replace(OfflineKingdom kingdom) {
				return kingdom.isNeutral() + "";
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<CommandSender>("%sender%") {
			@Override
			public String replace(CommandSender sender) {
				return sender.getName();
			}
		});
		Placeholders.registerPlaceholder(new Placeholder<KingdomPlayer>("%world%") {
			@Override
			public String replace(KingdomPlayer player) {
				return player.getPlayer().getWorld().getName();
			}
		});
		Placeholders.registerPlaceholder(new SimplePlaceholder("%maxclaims%") {
			@Override
			public String get() {
				int max = configuration.getInt("claiming.maximum-claims", -1);
				String string = max + "";
				if (max <= 0)
					string = "\u221E";
				return string;
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

}
