package com.songoda.kingdoms.manager.managers;

import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.events.LandLoadEvent;
import com.songoda.kingdoms.events.TurretBreakEvent;
import com.songoda.kingdoms.events.TurretPlaceEvent;
import com.songoda.kingdoms.manager.Manager;
import com.songoda.kingdoms.manager.managers.RankManager.Rank;
import com.songoda.kingdoms.manager.managers.external.CitizensManager;
import com.songoda.kingdoms.objects.kingdom.Kingdom;
import com.songoda.kingdoms.objects.kingdom.OfflineKingdom;
import com.songoda.kingdoms.objects.land.Land;
import com.songoda.kingdoms.objects.land.Turret;
import com.songoda.kingdoms.objects.land.TurretType;
import com.songoda.kingdoms.objects.land.TurretType.TargetType;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.objects.player.OfflineKingdomPlayer;
import com.songoda.kingdoms.placeholders.Placeholder;
import com.songoda.kingdoms.utils.DeprecationUtils;
import com.songoda.kingdoms.utils.Formatting;
import com.songoda.kingdoms.utils.MessageBuilder;
import com.songoda.kingdoms.utils.Utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Skull;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.Metadatable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TurretManager extends Manager {
	
	static {
		registerManager("turret", new TurretManager());
	}
	
	private final String METADATA_CONQUEST = "conquest-arrow";
	private final String METADATA_KINGDOM = "turret-kingdom";
	private final String METADATA_DAMAGE = "turret-damage";
	private final Set<TurretType> types = new HashSet<>();
	private final InvadingManager invadingManager;
	private final CitizensManager citizensManager;
	private final KingdomManager kingdomManager;
	private final PlayerManager playerManager;
	private final LandManager landManager;
	private final Kingdoms instance;
	
	protected TurretManager() {
		super(true);
		this.instance = Kingdoms.getInstance();
		this.landManager = instance.getManager("land", LandManager.class);
		this.playerManager = instance.getManager("player", PlayerManager.class);
		this.kingdomManager = instance.getManager("kingdom", KingdomManager.class);
		this.citizensManager = instance.getManager("citizens", CitizensManager.class);
		this.invadingManager = instance.getManager("invading", InvadingManager.class);
		for (String turret : configuration.getConfigurationSection("turrets.turrets").getKeys(false)) {
			types.add(new TurretType(turret));
		}
	}
	
	public Optional<Double> getProjectileDamage(Metadatable metadatable) {
		return metadatable.getMetadata(METADATA_DAMAGE).parallelStream()
				.filter(metadata -> metadata.getOwningPlugin().equals(instance))
				.map(metadata -> metadata.asDouble())
				.findFirst();
	}
	
	public Optional<OfflineKingdom> getProjectileKingdom(Metadatable metadatable) {
		return metadatable.getMetadata(METADATA_KINGDOM).parallelStream()
				.filter(metadata -> metadata.getOwningPlugin().equals(instance))
				.map(metadata -> metadata.asString())
				.map(string -> UUID.fromString(string))
				.map(uuid -> kingdomManager.getOfflineKingdom(uuid))
				.filter(kingdom -> kingdom.isPresent())
				.map(optional -> optional.get())
				.findFirst();
	}
	
	public long getTurretCount(Land land, TurretType type) {
		return land.getTurrets().parallelStream()
				.filter(turret -> turret.getType() != null)
				.filter(turret -> turret.getType().equals(type))
				.count();
	}
	
	boolean isTurret(Block block) {
		Location location = block.getLocation();
		Land land = landManager.getLand(location.getChunk());
		return land.getTurret(location) != null;
	}
	
	public void breakTurret(Turret turret) {
		Location location = turret.getLocation();
		Land land = landManager.getLand(location.getChunk());
		World world = location.getWorld();
		TurretType type = turret.getType();
		world.dropItem(location, type.build(land.getKingdomOwner(), false));
		location.getBlock().setType(Material.AIR);
		land.removeTurret(turret);
	}
	
	public TurretType getTurretTypeFrom(ItemStack item) {
		if (item == null)
			return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return null;
		List<String> lores = meta.getLore();
		if (lores == null || lores.isEmpty())
			return null;
		for (TurretType type : types) {
			if (type.getMaterial() == item.getType()) {
				if (Formatting.stripColor(meta.getDisplayName()).equalsIgnoreCase(Formatting.colorAndStrip(type.getTitle()))) {
					return type;
				}
			}
		}
		return null;
	}
	
	public boolean canBeTargeteded(Turret turret, Player target) {
		if (target.isDead() || !target.isValid())
			return false;
		if (invadingManager.isDefender(target))
			return false;
		Location location = turret.getLocation();
		if (!location.getWorld().equals(target.getWorld()))
			return false;
		if (target.getLocation().distanceSquared(location) > Math.pow(turret.getType().getRange(), 2))
			return false;
		TurretType type = turret.getType();
		KingdomPlayer kingdomPLayer = playerManager.getKingdomPlayer(target);
		if (kingdomPLayer.hasAdminMode() || kingdomPLayer.isVanished())
			return false;
		Land land = landManager.getLand(location.getChunk());
		OfflineKingdom landKingdom = land.getKingdomOwner();
		Kingdom kingdom = kingdomPLayer.getKingdom();
		GameMode gamemode = target.getGameMode();
		if (gamemode != GameMode.SURVIVAL && gamemode != GameMode.ADVENTURE)
			return false;
		if (type.getTargets().contains(TargetType.ALLIANCE)) {
			if (kingdom == null)
				return false;
			if (landKingdom.equals(kingdom))
				return true;
			return landKingdom.isAllianceWith(kingdom);
		} else if (type.getTargets().contains(TargetType.ENEMIES)) {
			if (kingdom == null)
				return true;
			if (landKingdom.equals(kingdom))
				return false;
			return !landKingdom.isAllianceWith(kingdom);
		}
		return false;
	}

	public boolean canBeTargeted(OfflineKingdom kingdom, Entity target) {
		if (!(target instanceof LivingEntity))
			return false;
		if (target.isDead() || !target.isValid())
			return false;
		if (citizensManager.isCitizen(target))
			return false;
		if (invadingManager.isDefender(target))
			return false;
		OfflineKingdom playerKingdom = null;
		if (target instanceof Player) {
			Player player = (Player) target;
			KingdomPlayer kingdomPlayer = playerManager.getKingdomPlayer(player);
			playerKingdom = kingdomPlayer.getKingdom();
			if (kingdomPlayer.hasAdminMode() || kingdomPlayer.isVanished())
				return false;
			GameMode gamemode = player.getGameMode();
			if (gamemode != GameMode.SURVIVAL && gamemode != GameMode.ADVENTURE)
				return false;
		} else if (target instanceof Wolf) {
			Wolf wolf = (Wolf) target;
			AnimalTamer owner = wolf.getOwner();
			if (owner != null) {
				OfflineKingdomPlayer kingdomPlayer = playerManager.getOfflineKingdomPlayer(owner.getUniqueId());
				playerKingdom = kingdomPlayer.getKingdom();
			}
		}
		if (playerKingdom == null)
			return true;
		if (kingdom.equals(playerKingdom))
			return false;
		return !kingdom.isAllianceWith(playerKingdom);
	}
	
	@EventHandler
	public void onTurretBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (isTurret(block)) {
			Player player = event.getPlayer();
			Location location = block.getLocation();
			Land land = landManager.getLand(location.getChunk());
			Turret turret = land.getTurret(location);
			if (turret == null)
				return;
			KingdomPlayer kingdomPlayer = playerManager.getKingdomPlayer(player);
			OfflineKingdom landKingdom = land.getKingdomOwner();
			if (landKingdom == null) {
				TurretBreakEvent breakEvent = new TurretBreakEvent(land, turret, kingdomPlayer);
				Bukkit.getPluginManager().callEvent(breakEvent);
				if (!breakEvent.isCancelled())
					breakTurret(turret);
			}
			Kingdom kingdom = kingdomPlayer.getKingdom();
			if (kingdomPlayer.hasAdminMode())
				return;
			event.setCancelled(true);
			if (kingdom == null) {
				new MessageBuilder("kingdoms.no-kingdom").send(player);
				return;
			}
			if (!kingdom.getUniqueId().equals(landKingdom.getUniqueId())) {
				new MessageBuilder("kingdoms.not-in-land")
						.setKingdom(kingdom)
						.send(player);
				return;
			}
			if (!kingdom.getPermissions(kingdomPlayer.getRank()).canBuildStructures()) {
				new MessageBuilder("kingdoms.rank-too-low-structure-build")
						.withPlaceholder(kingdom.getLowestRankFor(rank -> rank.canBuildStructures()), new Placeholder<Optional<Rank>>("%rank%") {
							@Override
							public String replace(Optional<Rank> rank) {
								if (rank.isPresent())
									return rank.get().getName();
								return "(Not attainable)";
							}
						})
						.setKingdom(kingdom)
						.send(player);
				return;
			}
			TurretBreakEvent breakEvent = new TurretBreakEvent(land, turret, kingdomPlayer, kingdom);
			Bukkit.getPluginManager().callEvent(breakEvent);
			if (!breakEvent.isCancelled())
				breakTurret(turret);
		}
	}

	@EventHandler
	public void onPlaceTurret(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		if (event.getBlockFace() != BlockFace.UP)
			return;
		Player player = event.getPlayer();
		TurretType type = getTurretTypeFrom(DeprecationUtils.getItemInMainHand(player));
		if (type == null)
			return;
		KingdomPlayer kingdomPlayer = playerManager.getKingdomPlayer(player);
		Kingdom kingdom = kingdomPlayer.getKingdom();
		if (!kingdomPlayer.hasAdminMode() && kingdom == null) {
			new MessageBuilder("kingdoms.no-kingdom").send(player);
			return;
		}
		Block block = event.getClickedBlock();
		Material material = block.getType();
		Block turretBlock = block.getRelative(0, 1, 0);
		boolean postCreated = false;
		if (!material.name().contains("FENCE") && !material.name().contains("COBBLESTONE_WALL")) {
			if (!material.isSolid())
				return;
			if (!material.isOccluding())
				return;
			if (configuration.getStringList("turrets.illegal-placements").contains(material.name())) {
				new MessageBuilder("turrets.illegal-placement")
						.replace("%material%", material.name().toLowerCase())
						.setPlaceholderObject(kingdomPlayer)
						.send(player);
				event.setCancelled(true);
				return;
			}
			if (turretBlock.getType() != Material.AIR || turretBlock.getRelative(0, 1, 0).getType() != Material.AIR) {
				new MessageBuilder("turrets.already-occupied")
						.setKingdom(kingdom)
						.send(player);
				return;
			}
			Material post = Utils.materialAttempt(configuration.getString("turrets.default-post", "FENCE"), "FENCE");
			turretBlock.setType(post);
			turretBlock = turretBlock.getRelative(0, 1, 0);
			postCreated = true;
		} else if (turretBlock.getType() != Material.AIR) {
			new MessageBuilder("turrets.already-occupied")
					.setPlaceholderObject(kingdomPlayer)
					.setKingdom(kingdom)
					.send(player);
			return;
		}	
		Land land = landManager.getLand(turretBlock.getChunk());
		OfflineKingdom landKingdom = land.getKingdomOwner();
		if (!kingdomPlayer.hasAdminMode() && landKingdom == null || !kingdom.getUniqueId().equals(landKingdom.getUniqueId())) {
			new MessageBuilder("kingdoms.not-in-land")
					.setPlaceholderObject(kingdomPlayer)
					.setKingdom(landKingdom)
					.send(player);
			return;
		}
		if (getTurretCount(land, type) >= type.getMaximum()) {
			new MessageBuilder("turrets.turret-limit")
					.replace("%type%", Formatting.color(type.getTitle()))
					.replace("%amount%", type.getMaximum())
					.setPlaceholderObject(kingdomPlayer)
					.setKingdom(landKingdom)
					.send(player);
			return;
		}
		Turret turret = new Turret(turretBlock.getLocation(), type, postCreated);
		TurretPlaceEvent placeEvent = new TurretPlaceEvent(land, turret, kingdomPlayer, kingdom);
		Bukkit.getPluginManager().callEvent(placeEvent);
		if (placeEvent.isCancelled())
			return;
		ItemStack item = DeprecationUtils.getItemInMainHand(player);
		int amount = item.getAmount();
		if (amount > 1)
			item.setAmount(amount - 1);
		else
			DeprecationUtils.setItemInMainHand(player, null);
		land.addTurret(turret);
		Material head = Utils.materialAttempt("SKELETON_SKULL", "SKULL");
		turretBlock.setType(head);
		BlockState state = turretBlock.getState();
		// 1.8 users...
		if (head.name().equalsIgnoreCase("SKULL"))
			DeprecationUtils.setupOldSkull(state);
		if (state instanceof Skull) {
			Skull skull = (Skull) state;
			skull.setOwningPlayer(type.getSkullOwner());
			state.update(true);
		}
		type.getPlacingSounds().playAt(block.getLocation());
	}
	
	@EventHandler
	public void onDispense(BlockDispenseEvent event) {
		Block block = event.getBlock();
		BlockState state = block.getState();
		if (!(state instanceof Dispenser))
			return;
		Dispenser dispenser = (Dispenser) state;
		BlockFace face = ((org.bukkit.material.Dispenser) dispenser.getData()).getFacing();
		if (isTurret(block.getRelative(face)))
			event.setCancelled(true);
	}

	// Fixes heads not being removed from water
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onBucketPlace(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			ItemStack item;
			try {
				item = player.getItemInHand();
			} catch (Exception e) {
				item = player.getInventory().getItemInMainHand();
			}
			if (item != null) {
				Material type = item.getType();
				if (type == Material.WATER_BUCKET || type == Material.LAVA_BUCKET) {
					if (isTurret(event.getClickedBlock().getRelative(event.getBlockFace()))) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onWaterPassThrough(BlockFromToEvent event) {
		if (isTurret(event.getToBlock()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onProjectileLand(ProjectileHitEvent event) {
		Entity projectile = event.getEntity();
		if (projectile.hasMetadata(METADATA_KINGDOM) || projectile.hasMetadata(METADATA_CONQUEST)) {
			Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
			@Override
				public void run() {
					projectile.remove();
				}
			}, 1);
		}
	}

	@EventHandler
	public void onTurretHit(EntityDamageByEntityEvent event) {
		Entity attacker = event.getDamager();
		Optional<OfflineKingdom> kingdom = getProjectileKingdom(attacker);
		Optional<Double> damage = getProjectileDamage(attacker);
		if (damage.isPresent() && kingdom.isPresent()) {
			if (canBeTargeted(kingdom.get(), event.getEntity())) {
				event.setDamage(damage.get());
			} else {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onTurretProjectileBurn(EntityCombustByEntityEvent event) {
		Entity combuster = event.getCombuster();
		Optional<OfflineKingdom> kingdom = getProjectileKingdom(combuster);
		if (kingdom.isPresent()) {
			if (!canBeTargeted(kingdom.get(), event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onLandLoad(LandLoadEvent event) {
		for (Turret turret : event.getLand().getTurrets()) {
			Block block = turret.getLocation().getBlock();
			if (block.getType() != Utils.materialAttempt("SKELETON_SKULL", "SKULL")) {
				breakTurret(turret);				
			}
		}
	}
	
	@Override
	public void onDisable() {
		types.clear();
	}

	/*
	@EventHandler
	public void onLandLoadOld(LandLoadEvent event) {
		Iterator<Turret> iter = e.getLand().getTurrets().iterator();
		Turret turret = null;
		ArrayList<Turret> remove = new ArrayList();
		while(iter.hasNext()){
			turret = iter.next();
			if(turret == null) continue;
	
			if(remove != null && remove.size() > 0){
				remove.addAll(initTurret(e.getLand(), turret));
			}
		}
		for(Turret t : remove){
			t.destroy();
		}
		//2016-08-11
		//loadQueue.add(e.getLand());
	}

	private ArrayList<Turret> initTurret(Land land, Turret turret) {
		Set<Turret> turrets = land.getTurrets();
		if (turrets.isEmpty())
			return null;
		Set<Turret> remove = new HashSet<>();
		for(Turret t : turrets) {
			TurretType type = t.getType();
			if (type == null) {
				remove.add(t);
				continue;
			}
			if (Config.getConfig().getBoolean("destroy-extra-turrets-to-enforce-max")) {
				if (isOverHitInLand(land, type)) {
					toBeRemoved.add(t);
					continue;
				}
			}
			Block turretBlock = t.getLoc().toLocation().getBlock();
			if(turretBlock.getType().isSolid() &&
				turretBlock.getType() != Materials.SKELETON_SKULL.parseMaterial() &&
				turretBlock.getType() != Materials.OAK_PRESSURE_PLATE.parseMaterial() &&
				turretBlock.getType() != Materials.STONE_PRESSURE_PLATE.parseMaterial()){
			toBeRemoved.add(t);
			Kingdoms.logInfo("A turret at " + t.getLoc().toString() + " is not a skull or a pressure plate! Removing.");
			continue;
			}
			if(type == TurretType.MINE_CHEMICAL){
			turretBlock.setType(Materials.OAK_PRESSURE_PLATE.parseMaterial());
			}
			else if(type == TurretType.MINE_PRESSURE){
			turretBlock.setType(Materials.STONE_PRESSURE_PLATE.parseMaterial());
			}
			else{
			if(turretBlock.getType() != Materials.SKELETON_SKULL.parseMaterial()){
				turretBlock.setType(Materials.SKELETON_SKULL.parseMaterial());
				//turretBlock.setData((byte) 1);
			}
			}
	
		}
		return toBeRemoved;
	}

	public void onChunkLoad(ChunkLoadEvent event) {
		Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
			@Override
			public void run() {
				Land land = GameManagement.getLandManager().getLand(new SimpleChunkLocation(event.getChunk()));
				if (land.getTurrets().isEmpty())
					return;
				for (Turret t : land.getTurrets()) {
					TurretType type = t.getType();
					Block turretBlock = t.getLoc().toLocation().getBlock();
					if (type != TurretType.MINE_CHEMICAL && type != TurretType.MINE_PRESSURE) {
						if (!(turretBlock.getState() instanceof Skull)) {
							turretBlock.setType(Materials.SKELETON_SKULL.parseMaterial());
							//turretBlock.setData((byte) 1);
						}
						Skull s = (Skull) turretBlock.getState();
						if (s == null)
							continue;
						if (type.getSkin() == null)
							continue;
						//s.setOwner(type.getSkin());
						//s.update();
					}
				}
			}
		}, 1L);
	}
	
	plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
		@Override
		public void run(){
		for(SimpleChunkLocation loc : GameManagement.getLandManager().getAllLandLoc()){
			Land land = GameManagement.getLandManager().getOrLoadLand(loc);
			World world = Bukkit.getWorld(loc.getWorld());
			if(world == null) continue;
			if(!world.isChunkLoaded(loc.getX(), loc.getZ())) continue;
			Chunk c = loc.toChunk();

			if(canChunkBeUnloaded(c)){
			continue;
			}

			if(land.getOwnerUUID() == null)
			continue;
			if(!land.getLoc().toChunk().isLoaded()) continue;
			Iterator<Turret> iter = land.getTurrets().iterator();


			while(iter.hasNext()){
			Turret turret = iter.next();
			if(turret == null){
				iter.remove();
				continue;
			}
			if(turret.getType() == null){
				iter.remove();
				continue;
			}
			if(!turret.getType().isEnabled()) continue;
			if(turret.getType().toString().startsWith("MINE")) continue;
			if(!turret.isValid()) continue;
			Collection<Entity> nearby = getNearbyChunkEntities(c, turret.getType().getRange());
			Bukkit.getScheduler().runTaskAsynchronously(plugin, new BukkitRunnable() {
				@Override
				public void run(){

				if(turret.aim(nearby)){
					Bukkit.getScheduler().runTask(plugin, new BukkitRunnable() {
					@Override
					public void run(){
						turret.fire();
					}
					});

				}
				}
			});
			}
		}
		}
	}, 0, 5L);
	*/
	
	/*
	public Set<Entity> getChunkEntities(Chunk chunk, int range) {
		Set<Entity> entities = new HashSet<>();
		int radius = 1;
		double newRadius = (double) range / 16D;
		if (newRadius > radius) {
			radius = (int) Math.ceil(newRadius);
			if (radius < 1)
				radius = 1;
		}
		for(int x = -radius; x <= radius; x++){
			for(int z = -radius; z <= radius; z++){
				Chunk c = chunk.getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
				for(Entity e : c.getEntities()){
					if(e instanceof Player) mobs.add(e);
				}
			}
		}
		return entities;
	}
	
	@EventHandler
	public void onBlockUnderPressurePlateBreak(BlockBreakEvent event) {
		Block block = event.getBlock().getRelative(0, 1, 0);
		if (block.getType().toString().contains("PLATE") && isTurret(block)) {
			Location location = block.getLocation();
			Land land = landManager.getLand(location.getChunk());
			Turret turret = land.getTurret(location);
			turret.breakTurret();
		}
	}
	
	@EventHandler
	public void onMineTrigger(PlayerInteractEvent event){
		if (event.getAction() != Action.PHYSICAL)
			return;
		Block mine = event.getClickedBlock();
		Location location = mine.getLocation();
		Land land = landManager.getLand(location.getChunk());
		OfflineKingdom landKingdom = land.getKingdomOwner();
		if (landKingdom == null)
			return;
		Turret turret = land.getTurret(location);
		if (turret == null)
			return;
		if (!canBeTargeted(landKingdom, event.getPlayer()))
			return;
		event.setCancelled(true);
		TurretType type = turret.getType();
		if (!type.isEnabled())
			return;
		Player player = event.getPlayer();
		if (type is a chemical mine) {
			int dur = 100;
			if (landKingdom.getTurretUpgrades().isVirulentPlague())
				dur = 200;
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, dur, Config.getConfig().getInt("turret-specs.chemicalmine.poison-potency")));
	
		} else if (type is a regular mine) {
			World world = mine.getWorld();
			world.createExplosion(location, type.getDamage(), false);
			if (landKingdom.getTurretUpgrades().isConcentratedBlast())
				world.createExplosion(location, type.getDamage(), false);
		}
		breakTurret(turret);
	}
	*/

}
