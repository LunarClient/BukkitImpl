package com.lunarclient.bukkitimpl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lunarclient.bukkitapi.LunarClientAPI;
import com.lunarclient.bukkitapi.nethandler.shared.LCPacketWaypointAdd;
import com.lunarclient.bukkitapi.nethandler.client.LCPacketModSettings;
import com.lunarclient.bukkitapi.nethandler.client.obj.ModSettings;
import com.lunarclient.bukkitapi.nethandler.client.obj.ServerRule;
import com.lunarclient.bukkitapi.object.LCWaypoint;
import com.lunarclient.bukkitapi.serverrule.LunarClientAPIServerRule;
import com.lunarclient.bukkitimpl.listener.LunarClientUserListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LunarClient extends JavaPlugin {

    @Getter
    private static LunarClient instance;
    @Getter
    private LCPacketModSettings packetModSettings = null;
    
    // TODO: We can acctually cache the packet to 
    //  send allowing easier implemntation of the
    //  waypoint's without creating tons of objects
    @Getter
    private final List<LCWaypoint> waypoints = new ArrayList<>();
    
    @Getter
    private final List<LCPacketWaypointAdd> waypointPackets = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        instance = this;


        loadWaypoints();
        loadServerRules();
        loadDisabledMods();

        Bukkit.getPluginManager().registerEvents(new LunarClientUserListener(this), this);
    }

    // TODO: Better loading implementation?
    public void loadWaypoints() {
        // if we don't have waypoints, don't continue.
        if (!getConfig().contains("waypoints")) {
            return;
        }

        // Get all the list of waypoints
        List<Map<?, ?>> maps = getConfig().getMapList("waypoints");
        for (Map<?, ?> map : maps) {
            // Create the waypoint.
            // This is super brittle, and could be done better most likely.
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                JsonObject object = new JsonParser().parse(String.valueOf(entry.getValue())).getAsJsonObject();

                LCWaypoint waypoint = new LCWaypoint((String) object.get("name").getAsString(), (Integer) object.get("x").getAsInt(), (Integer) object.get("y").getAsInt(), (Integer) object.get("z").getAsInt(), (String)  LunarClientAPI.getInstance().getWorldIdentifier(Bukkit.getWorld(object.get("world").getAsString())), (Integer) object.get("color").getAsInt(), (Boolean) object.get("forced").getAsBoolean(), (Boolean) object.get("visible").getAsBoolean());
                LCPacketWaypointAdd packet = new LCPacketWaypointAdd(waypoint.getName(), waypoint.getWorld(), waypoint.getColor(), waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.isForced(), waypoint.isVisible());
                
                waypoints.add(waypoint);
                waypointPackets.add(packet); // cache the packet to be used later
            }
        }
    }

    /**
     * We are going to load the server rules from the config, and
     * then immediately set them in the API and let that handle the
     * caching for us. This will allow us to simply just call a send
     * once the player is registered.
     */
    private void loadServerRules() {
        if (getConfig().contains("server-rules")) {
            for (ServerRule value : ServerRule.values()) {
                if (getConfig().contains("server-rules." + value.name()) && getConfig().isBoolean("server-rules." + value.name())) {
                    LunarClientAPIServerRule.setRule(value, getConfig().getBoolean("server-rules." + value.name()));
                }
            }
        }
    }

    /**
     * We are going to load the disabled mods from the config, and
     * cache the packet until we have users who have registered.
     * <p>
     * Once they registered, we will send the packet. We only
     * want to have 1 packet we send to a lot of users.
     */
    private void loadDisabledMods() {
        // If we have the disabled mods key, and its a list we want to set mod settings.
        if (getConfig().contains("force-disabled-mods") && getConfig().isList("force-disabled-mods")) {
            ModSettings modSettings = new ModSettings();
            // Go through all the items in the list, and disable each mod.
            for (String modId : getConfig().getStringList("force-disabled-mods")) {
                modSettings.addModSetting(modId, new ModSettings.ModSetting(false, new HashMap<>()));
            }
            packetModSettings = new LCPacketModSettings(modSettings);
        }
    }
}
