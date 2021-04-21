package com.lunarclient.bukkitimpl.listener;

import com.lunarclient.bukkitapi.LunarClientAPI;
import com.lunarclient.bukkitapi.event.LCPlayerRegisterEvent;
import com.lunarclient.bukkitapi.object.LCWaypoint;
import com.lunarclient.bukkitapi.serverrule.LunarClientAPIServerRule;
import com.lunarclient.bukkitimpl.LunarClient;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


@RequiredArgsConstructor
public class LunarClientUserListener implements Listener {

    private final LunarClient client;

    @EventHandler
    public void onPlayerRegister(PlayerJoinEvent event) {
       Player player = event.getPlayer(); 
        
        // When the player connects, send them our disabled mods.
        if (client.getPacketModSettings() != null) {
            LunarClientAPI.getInstance().sendPacket(player, client.getPacketModSettings());
        }

        LunarClientAPIServerRule.sendServerRule(player);
        
        // Send our cached packets when they connect.
        for (LCPacketWaypointAdd packet : client.getWaypointPackets()) {
            LunarClientAPI.getInstance().sendPacket(player, packet);
        }
    }
}
