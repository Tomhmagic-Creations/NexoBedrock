package com.portalgg.nexobedrock

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.nexomc.nexo.api.NexoFurniture
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTeleportEvent
import org.geysermc.floodgate.api.FloodgateApi

class NexoBedrockFurnitureListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun PlayerTrackEntityEvent.onTrackEntity() {
        if (!NexoBedrock.instance().bedrockHandler.isBedrockPlayer(player)) return
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return

        Bukkit.getScheduler().runTaskLater(NexoBedrock.instance(), Runnable {
            NexoBedrock.instance().packetManager.sendBedrockFurnitureEntityPacket(baseEntity, mechanic, player)
        }, 2L)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun PlayerUntrackEntityEvent.onUntrackEntity() {
        if (!NexoBedrock.instance().bedrockHandler.isBedrockPlayer(player)) return
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return

        NexoBedrock.instance().packetManager.removeBedrockFurnitureEntityPacket(baseEntity, mechanic, player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun EntityRemoveFromWorldEvent.onRemoveFromWorld() {
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return

        NexoBedrock.instance().packetManager.removeBedrockFurnitureEntityPacket(baseEntity, mechanic)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityTeleportEvent.onTeleport() {
        val baseEntity = entity as? ItemDisplay ?: return
        val mechanic = NexoFurniture.furnitureMechanic(entity) ?: return

        NexoBedrock.instance().packetManager.removeBedrockFurnitureEntityPacket(baseEntity, mechanic)

        Bukkit.getScheduler().runTaskLater(NexoBedrock.instance(), Runnable {
            NexoBedrock.instance().packetManager.sendBedrockFurnitureEntityPacket(baseEntity, mechanic)
        }, 2L)
    }
}