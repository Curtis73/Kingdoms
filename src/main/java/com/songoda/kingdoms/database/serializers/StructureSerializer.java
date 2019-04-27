package com.songoda.kingdoms.database.serializers;

import java.lang.reflect.Type;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.database.Serializer;
import com.songoda.kingdoms.manager.managers.KingdomManager;
import com.songoda.kingdoms.objects.kingdom.OfflineKingdom;
import com.songoda.kingdoms.objects.structures.Structure;
import com.songoda.kingdoms.objects.structures.StructureType;

public class StructureSerializer implements Serializer<Structure> {

	private final KingdomManager kingdomManager;

	public StructureSerializer() {
		this.kingdomManager = Kingdoms.getInstance().getManager("kingdom", KingdomManager.class);
	}

	@Override
	public JsonElement serialize(Structure structure, Type type, JsonSerializationContext context) {
		JsonObject json = new JsonObject();
		Location location = structure.getLocation();
		json.addProperty("type", structure.getType().name());
		json.addProperty("world", location.getWorld().getName());
		json.addProperty("x", location.getX());
		json.addProperty("y", location.getY());
		json.addProperty("z", location.getZ());
		OfflineKingdom kingdom = structure.getKingdom();
		if (kingdom != null)
			json.addProperty("kingdom", kingdom.getUniqueId() + "");
		return json;
	}

	@Override
	public Structure deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = json.getAsJsonObject();
		JsonElement kingdomElement = object.get("kingdom");
		if (kingdomElement == null || kingdomElement.isJsonNull())
			return null;
		UUID uuid = UUID.fromString(kingdomElement.getAsString());
		if (uuid == null)
			return null;
		OfflineKingdom kingdom = kingdomManager.getKingdom(uuid);
		if (kingdom == null)
			return null;
		JsonElement worldElement = object.get("world");
		if (worldElement == null || worldElement.isJsonNull())
			return null;
		World world = Bukkit.getWorld(worldElement.getAsString());
		if (world == null)
			return null;
		double x = object.get("x").getAsDouble();
		double y = object.get("y").getAsDouble();
		double z = object.get("z").getAsDouble();
		Location location = new Location(world, x, y, z);
		JsonElement typeElement = object.get("type");
		if (typeElement == null || typeElement.isJsonNull())
			return null;
		StructureType structureType = StructureType.valueOf(typeElement.getAsString());
		if (structureType == null)
			return null;
		return new Structure(kingdom, location, structureType);
	}

}