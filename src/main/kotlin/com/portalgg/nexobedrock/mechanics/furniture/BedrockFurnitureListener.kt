package com.portalgg.nexobedrock.mechanics.furniture

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.nexomc.nexo.api.NexoFurniture
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.configs.Message
import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic.RestrictedRotation
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing.LimitedPlacingType
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.EventUtils.call
import com.nexomc.nexo.utils.Utils.swingHand
import com.nexomc.nexo.utils.VersionUtil
import com.portalgg.nexobedrock.NexoBedrock
import io.th0rgal.protectionlib.ProtectionLib
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.inventory.EquipmentSlot

class BedrockFurnitureListener(private val factory: BedrockFurnitureFactory) : Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun PlayerInteractEvent.onLimitedPlacing() {
        val (block, itemId) = (clickedBlock ?: return) to (item?.let(NexoItems::idFromItem) ?: return)

        if (hand != EquipmentSlot.HAND || action != Action.RIGHT_CLICK_BLOCK) return
        if (!player.isSneaking && BlockHelpers.isInteractable(clickedBlock)) return

        val mechanic = factory.getMechanic(itemId) ?: return
        val limitedPlacing = mechanic.limitedPlacing ?: return
        val belowPlaced = block.getRelative(blockFace).getRelative(BlockFace.DOWN)

        when {
            limitedPlacing.isNotPlacableOn(block, blockFace) -> isCancelled = true
            limitedPlacing.type == LimitedPlacingType.ALLOW && !limitedPlacing.checkLimited(belowPlaced) ->
                isCancelled = true
            limitedPlacing.type == LimitedPlacingType.DENY && limitedPlacing.checkLimited(belowPlaced) ->
                isCancelled = true
            limitedPlacing.isRadiusLimited -> {
                val (radius, amount) = limitedPlacing.radiusLimitation!!.let { it.radius.toDouble() to it.amount.toDouble()}
                if (block.world.getNearbyEntities(block.location, radius, radius, radius)
                        .filter { factory.getMechanic(it)?.itemID == mechanic.itemID }
                        .count { it.location.distanceSquared(block.location) <= radius * radius } >= amount
                ) isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun PlayerInteractEvent.onFurniturePlace() {
        val block = clickedBlock?.let { if (BlockHelpers.isReplaceable(it)) it else it.getRelative(blockFace) } ?: return
        val (item, hand) = (item ?: return) to (hand?.takeIf { it == EquipmentSlot.HAND } ?: return)
        val mechanic = factory.getMechanic(item) ?: return

        if (action != Action.RIGHT_CLICK_BLOCK || (useInteractedBlock() == Event.Result.DENY && !player.isSneaking)) return
        if (useItemInHand() == Event.Result.DENY || BlockHelpers.isStandingInside(player, block)) return
        if (!player.isSneaking && BlockHelpers.isInteractable(clickedBlock)) return
        if (!ProtectionLib.canBuild(player, block.location)) return
        if (!player.isSneaking && NexoFurniture.furnitureMechanic(block)?.isInteractable == true) return


        val currentBlockData = block.blockData
        val blockPlaceEvent = BlockPlaceEvent(block, block.state, block.getRelative(blockFace), item, player, true, hand)
        val rotation = getRotation(player.eyeLocation.yaw.toDouble(), mechanic)
        val yaw = FurnitureHelpers.rotationToYaw(rotation)

        if (player.gameMode == GameMode.ADVENTURE) blockPlaceEvent.isCancelled = true
        if (mechanic.notEnoughSpace(block.location, yaw)) {
            blockPlaceEvent.isCancelled = true
            Message.NOT_ENOUGH_SPACE.send(player)
        }

        if (!blockPlaceEvent.canBuild() || !blockPlaceEvent.call()) return

        val baseEntity = mechanic.place(block.location, yaw, blockFace, false) ?: return

        swingHand(player, hand)

        if (player.gameMode != GameMode.CREATIVE) item.amount -= 1
        setUseInteractedBlock(Event.Result.DENY)
        if (VersionUtil.isPaperServer) baseEntity.world.sendGameEvent(player, GameEvent.BLOCK_PLACE, baseEntity.location.toVector())
    }

    @EventHandler
    fun BlockBreakEvent.onBreak() {
        val mechanic = factory.getMechanic(block) ?: return
        val baseEntity = factory.getBaseEntity(block) ?: return
        mechanic.removeBaseEntity(baseEntity)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun EntityTeleportEvent.onTeleport() {
        val entity = entity as? Interaction ?: entity as? ArmorStand ?: return
        val mechanic = factory.getMechanic(entity) ?: return
        val baseEntity = (entity as? Interaction)?.let(factory::getBaseEntity) ?: entity as? ArmorStand ?: return

        if (entity.uniqueId != baseEntity.uniqueId) isCancelled = true
        else {
            BedrockFurnitureSeats.removeSeats(baseEntity)
            mechanic.removeHitboxes(baseEntity)

            Bukkit.getScheduler().runTaskLater(NexoBedrock.instance(), Runnable {
                BedrockFurnitureSeats.spawnSeats(baseEntity, mechanic)
                mechanic.spawnHitboxes(baseEntity, baseEntity.location.yaw)
            }, 2L)
        }
    }

    @EventHandler
    fun EntityRemoveFromWorldEvent.onRemove() {
        val entity = entity as? Interaction ?: entity as? ArmorStand ?: return
        val mechanic = factory.getMechanic(entity) ?: return
        val baseEntity = (entity as? Interaction)?.let(factory::getBaseEntity) ?: entity as? ArmorStand ?: return

        mechanic.removeBaseEntity(baseEntity)
    }

    @EventHandler
    fun EntityDamageByEntityEvent.onInteract() {
        val entity = entity as? Interaction ?: return
        val mechanic = factory.getMechanic(entity) ?: return
        val baseEntity = factory.getBaseEntity(entity) ?: return

        mechanic.removeBaseEntity(baseEntity)
    }

    @EventHandler
    fun PlayerQuitEvent.onPlayerQuitEvent() {
        if (factory.isFurniture(player.vehicle)) player.leaveVehicle()
    }

    @EventHandler
    fun EntitiesLoadEvent.onInvalidFurniture() {
        if (Settings.REMOVE_INVALID_FURNITURE.toBool()) entities.filterIsInstance<ArmorStand>().forEach {
            if (!it.persistentDataContainer.has(BedrockFurnitureMechanic.FURNITURE_KEY) || factory.isFurniture(it)) return@forEach
            it.remove()
        }
    }

    companion object {

        private fun getRotation(yaw: Double, mechanic: BedrockFurnitureMechanic): Rotation {
            val restrictedRotation = mechanic.restrictedRotation
            var id = (((Location.normalizeYaw(yaw.toFloat()) + 180) * 8 / 360) + 0.5).toInt() % 8
            val offset = if (restrictedRotation == RestrictedRotation.STRICT) 0 else 1
            if (restrictedRotation != RestrictedRotation.NONE && id % 2 != 0) id -= offset
            return Rotation.entries[id]
        }
    }
}