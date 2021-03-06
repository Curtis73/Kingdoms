package com.songoda.kingdoms.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.songoda.kingdoms.objects.kingdom.Kingdom;
import com.songoda.kingdoms.objects.land.Land;
import com.songoda.kingdoms.objects.player.KingdomPlayer;
import com.songoda.kingdoms.objects.structures.Structure;

public class StructurePlaceEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final KingdomPlayer player;
	private final Structure structure;
	private final Kingdom kingdom;
	private boolean cancelled;
	private final Land land;
	
	public StructurePlaceEvent(Land land, Structure structure, Kingdom kingdom, KingdomPlayer player) {
		this.structure = structure;
		this.player = player;
		this.kingdom = kingdom;
		this.land = land;
	}
	
	public Land getLand() {
		return land;
	}
	
	public Kingdom getKingdom() {
		return kingdom;
	}
	
	public Structure getStructure() {
		return structure;
	}
	
	public KingdomPlayer getKingdomPlayer() {
		return player;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
        return handlers;
	}

}
