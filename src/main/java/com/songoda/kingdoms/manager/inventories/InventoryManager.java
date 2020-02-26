package com.songoda.kingdoms.manager.inventories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import com.songoda.kingdoms.manager.Manager;
import com.songoda.kingdoms.manager.inventories.KingdomInventory.SlotAction;

public class InventoryManager extends Manager {

	private final Map<UUID, KingdomInventory> opened = new HashMap<>();
	ArrayList<UUID> inventoryHolders = new ArrayList<UUID>();
	private final Set<KingdomInventory> inventories = new HashSet<>();

	public InventoryManager() {
		super(true);
	}

	@SuppressWarnings("unchecked")
	public <C extends KingdomInventory> C getInventory(Class<C> inventory) {
		return (C) inventories.parallelStream()
				.filter(kingdomInventory -> kingdomInventory.getClass().equals(inventory))
				.findFirst().orElseGet(() -> {
					KingdomInventory kingdomInventory;
					try {
						kingdomInventory = inventory.newInstance();
						inventories.add(kingdomInventory);
						return kingdomInventory;
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
					return null;
				});
	}

	public <T extends KingdomInventory> void opening(UUID uuid, T inventory) {
		opened.put(uuid, inventory);
		inventoryHolders.add(uuid);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		//for (UUID e : opened.keySet()) {
		//	Bukkit.broadcastMessage("1Player: " + e.toString());
		//}
		Player player = (Player) event.getWhoClicked();
		UUID uuid = player.getUniqueId();
		if (!opened.keySet().stream().anyMatch(test -> test.equals(uuid))) {
			//for (UUID e : opened.keySet()) {
			//	Bukkit.broadcastMessage("Player: " + e.toString());
			//}
			return;}
		if (event.isShiftClick())
			event.setCancelled(true);
		Inventory clicked = event.getClickedInventory();
		if (clicked == null)
			return;
		event.setCancelled(true);
		if (event.getRawSlot() >= clicked.getSize())
			return;
		Optional<KingdomInventory> optional = Optional.ofNullable(opened.get(uuid));
		if (!optional.isPresent())
			return;
		KingdomInventory inventory = optional.get();
		Optional<SlotAction> action = inventory.getAction(clicked, uuid, event.getSlot());
		if (!action.isPresent())
			return;
		action.get().accept(event);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		opened.values().forEach(inventory -> inventory.close(uuid));
		opened.remove(uuid);
		inventoryHolders.remove(uuid);
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
	
//		Iterator<Entry<UUID, KingdomInventory>> iterator = opened.entrySet().iterator();
//		while (iterator.hasNext()) {
//			Entry<UUID, KingdomInventory> entry = iterator.next();
//			KingdomInventory inventory = entry.getValue();
//			if (inventory.isReopening()) 
//				continue;
//			inventory.close(uuid);
//			iterator.remove();
//		}		

		for (UUID uuids : opened.keySet()) {
			if (uuid == uuids) {
				KingdomInventory inventory = opened.get(uuid);
				if (inventory.isReopening()) {
					continue;
				}
				inventory.close(uuid);			
			}
		}
		opened.remove(uuid);
	}

	@Override
	public void initalize() {}

	@Override
	public void onDisable() {
		opened.keySet().parallelStream()
				.map(uuid -> Bukkit.getPlayer(uuid))
				.filter(player -> player != null)
				.forEach(player -> player.closeInventory());
		opened.clear();
		inventoryHolders.clear();
		inventories.clear();
	}

}
