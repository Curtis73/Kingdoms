package com.songoda.kingdoms.database.serializers;

import java.lang.reflect.Type;
import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.songoda.kingdoms.Kingdoms;
import com.songoda.kingdoms.database.Serializer;
import com.songoda.kingdoms.manager.managers.KingdomManager;
import com.songoda.kingdoms.objects.kingdom.OfflineKingdom;
import com.songoda.kingdoms.objects.kingdom.Powerup;
import com.songoda.kingdoms.objects.kingdom.PowerupType;

public class PowerupSerializer implements Serializer<Powerup> {

	private final KingdomManager kingdomManager;

	public PowerupSerializer() {
		this.kingdomManager = Kingdoms.getInstance().getManager("kingdom", KingdomManager.class);
	}

	@Override
	public JsonElement serialize(Powerup powerup, Type type, JsonSerializationContext context) {
		JsonObject json = new JsonObject();
		JsonObject powrups = new JsonObject();
		for (PowerupType powerupType : PowerupType.values()) {
			powrups.addProperty(powerupType.name(), powerup.getLevel(powerupType));
		}
		json.add("powerups", powrups);
		OfflineKingdom kingdom = powerup.getKingdom();
		if (kingdom != null)
			json.addProperty("kingdom", kingdom.getUniqueId() + "");
		return json;
	}

	@Override
	public Powerup deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
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
		Powerup powerup = new Powerup(kingdom);
		JsonElement powerupsElement = object.get("powerups");
		if (powerupsElement == null || powerupsElement.isJsonNull() || !powerupsElement.isJsonObject())
			return powerup;
		JsonObject powerupsObject = powerupsElement.getAsJsonObject();
		for (PowerupType powerupType : PowerupType.values()) {
			JsonElement element = powerupsObject.get(powerupType.name());
			if (element == null || element.isJsonNull())
				continue;
			powerup.setLevel(element.getAsInt(), powerupType);
		}
		return powerup;
	}

}