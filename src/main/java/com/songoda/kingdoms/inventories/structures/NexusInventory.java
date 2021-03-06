package com.songoda.kingdoms.inventories.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.songoda.kingdoms.inventories.DefenderUpgradeMenu;
import com.songoda.kingdoms.inventories.MembersMenu;
import com.songoda.kingdoms.inventories.MiscUpgradeMenu;
import com.songoda.kingdoms.inventories.PermissionsMenu;
import com.songoda.kingdoms.inventories.StructureShopMenu;
import com.songoda.kingdoms.inventories.TurretShopMenu;
import com.songoda.kingdoms.manager.inventories.InventoryManager;
import com.songoda.kingdoms.manager.inventories.StructureInventory;
import com.songoda.kingdoms.manager.managers.ChestManager;
import com.songoda.kingdoms.manager.managers.KingdomManager;
import com.songoda.kingdoms.manager.managers.MasswarManager;
import com.songoda.kingdoms.manager.managers.PlayerManager;
import com.songoda.kingdoms.manager.managers.RankManager.Rank;
import com.songoda.kingdoms.objects.kingdom.Kingdom;
import com.songoda.kingdoms.objects.kingdom.KingdomChest;
import com.songoda.kingdoms.objects.kingdom.OfflineKingdom;
import com.songoda.kingdoms.objects.kingdom.Powerup;
import com.songoda.kingdoms.objects.kingdom.PowerupType;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.placeholders.Placeholder;
import com.songoda.kingdoms.utils.DeprecationUtils;
import com.songoda.kingdoms.utils.ItemStackBuilder;
import com.songoda.kingdoms.utils.MessageBuilder;

public class NexusInventory extends StructureInventory implements Listener {

	private final Map<UUID, String> donations = new HashMap<>(); //KingdomPlayer, OfflineKingdom

	public NexusInventory() {
		super(InventoryType.CHEST, "nexus", 27);
		instance.getServer().getPluginManager().registerEvents(this, instance);
	}

	@Override
	public Inventory build(Inventory inventory, KingdomPlayer kingdomPlayer) {
		InventoryManager inventoryManager = instance.getManager(InventoryManager.class);
		Player player = kingdomPlayer.getPlayer();
		Kingdom kingdom = kingdomPlayer.getKingdom(); // Can't be null.
		if (section.getBoolean("use-filler", true)) {
			ItemStack filler = new ItemStackBuilder(section.getConfigurationSection("filler"))
					.setPlaceholderObject(kingdomPlayer)
					.setKingdom(kingdom)
					.build();
			for (int i = 0; i < inventory.getType().getDefaultSize(); i++)
				inventory.setItem(i, filler);
		}
		inventory.setItem(0,  new ItemStackBuilder(section.getConfigurationSection("converter"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 0, event -> openDonateInventory(kingdom, kingdomPlayer));
		inventory.setItem(8, new ItemStackBuilder(section.getConfigurationSection("permissions"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 8, event -> {
			if (!kingdom.getPermissions(kingdomPlayer.getRank()).canEditPermissions()) {
				new MessageBuilder("kingdoms.rank-too-low-edit-permissions")
						.withPlaceholder(kingdom.getLowestRankFor(low -> low.canEditPermissions()), new Placeholder<Optional<Rank>>("%rank%") {
							@Override
							public String replace(Optional<Rank> rank) {
								if (rank.isPresent())
									return rank.get().getName();
								return "(Not attainable)";
							}
						})
						.setKingdom(kingdom)
						.send(kingdomPlayer);
				return;
			}
			inventoryManager.getInventory(PermissionsMenu.class).open(kingdomPlayer);
		});
		inventory.setItem(9, new ItemStackBuilder(section.getConfigurationSection("defender-upgrades"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 9, event ->  inventoryManager.getInventory(DefenderUpgradeMenu.class).open(kingdomPlayer));
		inventory.setItem(10, new ItemStackBuilder(section.getConfigurationSection("misc-upgrades"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 10, event ->  inventoryManager.getInventory(MiscUpgradeMenu.class).open(kingdomPlayer));
		inventory.setItem(11, new ItemStackBuilder(section.getConfigurationSection("structures"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 11, event -> inventoryManager.getInventory(StructureShopMenu.class).open(kingdomPlayer));
		inventory.setItem(12, new ItemStackBuilder(section.getConfigurationSection("turrets"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 12, event -> inventoryManager.getInventory(TurretShopMenu.class).open(kingdomPlayer));
		inventory.setItem(13, new ItemStackBuilder(section.getConfigurationSection("members"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 13, event -> inventoryManager.getInventory(MembersMenu.class).open(kingdomPlayer));
		MasswarManager masswarManager = instance.getManager(MasswarManager.class);
		ItemStackBuilder masswar = new ItemStackBuilder(section.getConfigurationSection("masswar-on"))
				.replace("%time%", masswarManager.getTimeLeftInString())
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom);
		if (!masswarManager.isWarOn())
			masswar.setConfigurationSection(section.getConfigurationSection("masswar-off"));
		inventory.setItem(14, masswar.build());
		inventory.setItem(15, new ItemStackBuilder(section.getConfigurationSection("resource-points"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		inventory.setItem(16, new ItemStackBuilder(section.getConfigurationSection("chest"))
				.setPlaceholderObject(kingdomPlayer)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 16, event -> openKingdomChest(kingdomPlayer));
		KingdomChest kingdomChest = kingdom.getKingdomChest();
		int size = kingdomChest.getSize();
		int cost = configuration.getInt("kingdoms.chest-size-upgrade-cost", 30);
		cost += configuration.getInt("kingdoms.chest-size-upgrade-multiplier", 10) * ((size / 9) - 3);
		int chestCost = cost;
		int max = configuration.getInt("kingdoms.max-members-via-upgrade", 30);
		inventory.setItem(17, new ItemStackBuilder(section.getConfigurationSection("chest-size"))
				.setPlaceholderObject(kingdomPlayer)
				.replace("%cost%", chestCost)
				.replace("%size%", size)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 17, event -> {
			if (chestCost > kingdom.getResourcePoints()) {
				new MessageBuilder("kingdoms.not-enough-resourcepoints-chest-upgrade")
						.setPlaceholderObject(kingdomPlayer)
						.replace("%cost%", chestCost)
						.setKingdom(kingdom)
						.send(player);
				return;
			}
			if (size + 9 > max) {
				new MessageBuilder("kingdoms.nexus-chest-maxed")
						.setPlaceholderObject(kingdomPlayer)
						.setKingdom(kingdom)
						.send(player);
				return;
			}
			kingdom.subtractResourcePoints(chestCost);
			kingdomChest.setSize(size + 9);
			new MessageBuilder("kingdoms.chest-size-upgraded")
					.setPlaceholderObject(kingdomPlayer)
					.replace("%size%", (size + 9) / 9)
					.setKingdom(kingdom)
					.send(player);
			reopen(kingdomPlayer);
		});
		if (configuration.getBoolean("kingdoms.allow-pacifist")) {
			ItemStackBuilder builder = new ItemStackBuilder(section.getConfigurationSection("neutral-off"))
					.replace("%status%", kingdom.isNeutral())
					.setPlaceholderObject(kingdomPlayer)
					.setKingdom(kingdom);
			if (kingdom.isNeutral()) {
				builder = new ItemStackBuilder(section.getConfigurationSection("neutral-on"))
						.replace("%status%", kingdom.isNeutral())
						.setPlaceholderObject(kingdomPlayer)
						.setKingdom(kingdom);
			}
			inventory.setItem(4, builder.build());
			setAction(player.getUniqueId(), 4, event -> {
				if (kingdom.hasInvaded()) {
					new MessageBuilder("kingdoms.cannot-be-neutral")
							.replace("%status%", kingdom.isNeutral())
							.setPlaceholderObject(kingdomPlayer)
							.setKingdom(kingdom)
							.send(player);
					return;
				}
				kingdom.setNeutral(!kingdom.isNeutral());
				new MessageBuilder("kingdoms.neutral-toggled")
						.replace("%status%", kingdom.isNeutral())
						.setPlaceholderObject(kingdomPlayer)
						.setKingdom(kingdom)
						.send(player);
				reopen(kingdomPlayer);
				return;
			});
		}
		int memberCost = configuration.getInt("kingdoms.cost-per-max-member-upgrade", 10);
		inventory.setItem(22, new ItemStackBuilder(section.getConfigurationSection("max-members"))
				.setPlaceholderObject(kingdomPlayer)
				.replace("%cost%", memberCost)
				.replace("%max%", max)
				.setKingdom(kingdom)
				.build());
		setAction(player.getUniqueId(), 22, event -> {
			long p = kingdom.getResourcePoints();
			if (memberCost > p) {
				new MessageBuilder("structures.nexus-max-member-cant-afford")
						.setPlaceholderObject(kingdomPlayer)
						.replace("%cost%", memberCost)
						.replace("%max%", max)
						.setKingdom(kingdom)
						.send(player);
				return;
			}
			if (kingdom.getMaxMembers() + 1 > max) {
				new MessageBuilder("structures.max-members-reached")
						.setPlaceholderObject(kingdomPlayer)
						.replace("%cost%", memberCost)
						.replace("%max%", max)
						.setKingdom(kingdom)
						.send(player);
				return;
			}
			kingdom.subtractResourcePoints(memberCost);
			kingdom.setMaxMembers(kingdom.getMaxMembers() + 1);
			new MessageBuilder("structures.max-members-purchase")
					.setPlaceholderObject(kingdomPlayer)
					.replace("%cost%", memberCost)
					.replace("%max%", max)
					.setKingdom(kingdom)
					.send(player);
			reopen(kingdomPlayer);
		});
		Powerup powerup = kingdom.getPowerup();
		for (PowerupType type: PowerupType.values()) {
			if (!type.isEnabled())
				continue;
			int level = powerup.getLevel(type);
			inventory.setItem(type.getSlot(), type.getItemStackBuilder()
					.setPlaceholderObject(kingdomPlayer)
					.replace("%amount%", level)
					.replace("%level%", level)
					.setKingdom(kingdom)
					.build());
			setAction(inventory, player.getUniqueId(), type.getSlot(), event -> {
				if (type.getCost() > kingdom.getResourcePoints()) {
					new MessageBuilder("kingdoms.not-enough-resourcepoints-powerup")
							.replace("%powerup%", type.name().toLowerCase().replaceAll("_", "-"))
							.setPlaceholderObject(kingdomPlayer)
							.replace("%cost%", type.getCost())
							.setKingdom(kingdom)
							.send(player);
					return;
				}
				if (level + 1 > type.getMax()) {
					new MessageBuilder("kingdoms.powerup-maxed")
							.replace("%powerup%", type.name().toLowerCase().replaceAll("_", "-"))
							.setPlaceholderObject(kingdomPlayer)
							.replace("%cost%", type.getCost())
							.setKingdom(kingdom)
							.send(player);
					return;
				}
				kingdom.subtractResourcePoints(type.getCost());
				powerup.setLevel(level + 1, type);
				reopen(kingdomPlayer);
			});
		}
		return inventory;
	}

	public void openKingdomChest(KingdomPlayer kingdomPlayer) {
		Kingdom kingdom = kingdomPlayer.getKingdom();
		if (kingdom == null)
			return;
		if (!kingdom.getPermissions(kingdomPlayer.getRank()).hasChestAccess()) {
			new MessageBuilder("kingdoms.rank-too-low-chest-access")
					.withPlaceholder(kingdom.getLowestRankFor(rank -> rank.hasChestAccess()), new Placeholder<Optional<Rank>>("%rank%") {
						@Override
						public String replace(Optional<Rank> rank) {
							if (rank.isPresent())
								return rank.get().getName();
							return "(Not attainable)";
						}
					})
					.setPlaceholderObject(kingdomPlayer)
					.setKingdom(kingdom)
					.send(kingdomPlayer);
			return;
		}
		instance.getManager(ChestManager.class).openChest(kingdomPlayer, kingdom);
	}

	public void openDonateInventory(OfflineKingdom kingdom, KingdomPlayer kingdomPlayer) {
		String title = new MessageBuilder(false, "inventories.nexus.donate-title")
				.setPlaceholderObject(kingdomPlayer)
				.fromConfiguration(inventories)
				.setKingdom(kingdom)
				.get();
		Player player = kingdomPlayer.getPlayer();
		Inventory inventory = instance.getServer().createInventory(null, 54, title);
		player.openInventory(inventory);
		donations.put(player.getUniqueId(), kingdom.getName());
	}

	private int consumeDonationItems(Inventory inventory, KingdomPlayer kingdomPlayer) {
		ConfigurationSection section = configuration.getConfigurationSection("kingdoms.resource-donation");
		ItemStack[] items = inventory.getContents();
		Set<ItemStack> returning = new HashSet<>();
		Player player = kingdomPlayer.getPlayer();
		int worth = 0;
		List<ItemStack> contents = new ArrayList<>();
		for (ItemStack item : items) {
			if (item == null) //air
				continue;
			contents.add(item);
		}
		if (contents.isEmpty())
			return -1;

		// Calculate
		Set<Material> added = new HashSet<>();
		if (section.getBoolean("use-list", false)) {
			ConfigurationSection list = section.getConfigurationSection("list");
			Set<String> nodes = list.getKeys(false);
			for (ItemStack item : contents) {
				Material type = item.getType();
				if (added.contains(type))
					continue;
				Optional<Double> points = nodes.parallelStream()
						.filter(node -> node.equalsIgnoreCase(type.name()))
						.map(node -> list.getDouble(node, 3))
						.findFirst();
				if (!points.isPresent()) {
					returning.add(item);
					continue;
				}
				int amount = 0;
				for (ItemStack i : inventory.all(type).values())
					amount += i.getAmount();
				worth += amount * points.get();
				added.add(type);
			}
		} else {
			double points = section.getDouble("points-per-item", 3);
			for (ItemStack item : contents) {
				Material type = item.getType();
				if (added.contains(type))
					continue;
				String name = type.name();
				if (section.getStringList("blacklist").contains(name)) {
					returning.add(item);
					continue;
				}
				int amount = 0;
				for (ItemStack i : inventory.all(type).values())
					amount = amount + i.getAmount();
				worth += amount * points;
				added.add(type);
			}
		}

		// Return if worth is not enough
		if (worth < 1) {
			contents.forEach(item -> player.getInventory().addItem(item));
			new MessageBuilder("kingdoms.donate-not-enough")
					.setPlaceholderObject(kingdomPlayer)
					.send(player);
			return -1;
		}

		// Message and Return
		Set<Material> displayed = new HashSet<>();
		for (ItemStack item : returning) {
			// Count
			Material material = item.getType();
			int amount = 0;
			for (ItemStack i : inventory.all(material).values())
				amount += i.getAmount();

			String name = amount + " of " + material.name().toLowerCase(Locale.forLanguageTag(player.getLocale().replace("_", "-")));
			ItemMeta meta = item.getItemMeta();
			boolean modified = false;
			if (meta != null && meta.hasDisplayName()) {
				name = meta.getDisplayName();
				modified = true;
			} else {
				short durability = DeprecationUtils.getDurability(item);
				if (durability > 0) {
					name += ":" + durability;
					modified = true;
				}
			}
			if (!displayed.contains(material) || modified)
				new MessageBuilder("kingdoms.cannot-be-donated")
						.setPlaceholderObject(kingdomPlayer)
						.replace("%item%", name)
						.send(player);
			displayed.add(material);
			player.getInventory().addItem(item);
		}
		return (int) Math.ceil(worth);
	}

	@EventHandler
	public void onDonateInventoryClose(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		UUID uuid = player.getUniqueId();
		PlayerManager playerManager = instance.getManager(PlayerManager.class);
		KingdomPlayer kingdomPlayer = playerManager.getKingdomPlayer(player);
		if (donations.containsKey(uuid)) {
			int donated = consumeDonationItems(event.getInventory(), kingdomPlayer);
			if (donated <= 0) {
				donations.remove(uuid);
				return;
			}
			Kingdom kingdom = kingdomPlayer.getKingdom();
			if (kingdom == null) {
				donations.remove(uuid);
				return;
			}
			Optional<Kingdom> optional = instance.getManager(KingdomManager.class).getKingdom(donations.get(uuid));
			if (!optional.isPresent()) { // If the Kingdom got deleted while donating.
				donations.remove(uuid);
				return;
			}
			Kingdom donatingTo = optional.get();
			donations.remove(uuid);
			if (kingdom.equals(donatingTo)) {
				kingdom.addResourcePoints(donated);
				new MessageBuilder("kingdoms.donated-kingdom")
						.toKingdomPlayers(donatingTo.getKingdom().getOnlinePlayers())
						.setPlaceholderObject(kingdomPlayer)
						.replace("%amount%", donated)
						.ignoreSelf(kingdomPlayer)
						.setKingdom(kingdom)
						.send();
			} else { // It's an ally donating.
				donatingTo.addResourcePoints(donated);
				new MessageBuilder("kingdoms.donated-alliance")
						.toKingdomPlayers(donatingTo.getKingdom().getOnlinePlayers())
						.setPlaceholderObject(kingdomPlayer)
						.replace("%amount%", donated)
						.setKingdom(kingdom)
						.send();
			}
			new MessageBuilder("kingdoms.donated-self")
					.setPlaceholderObject(kingdomPlayer)
					.replace("%amount%", donated)
					.setKingdom(kingdom)
					.send(player);
			//TODO make a donation tracker and add the donation time, who, kingdom and amount from here.
			return;
		}
	}

}
