package com.portalgg.nexobedrock.mechanics.furniture

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat.Companion.SEAT_KEY
import com.nexomc.nexo.utils.BlockHelpers
import com.portalgg.nexobedrock.mechanics.furniture.BedrockFurnitureMechanic.Companion.FURNITURE_KEY
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.*
import org.bukkit.persistence.PersistentDataType
import java.util.*

object BedrockFurnitureSeats {

    fun sitOnSeat(baseEntity: ArmorStand, player: Player, interactionPoint: Location?) {
        val centeredLoc = BlockHelpers.toCenterLocation(interactionPoint ?: baseEntity.location)
        baseEntity.persistentDataContainer.get(SEAT_KEY, DataType.asList(DataType.UUID))
            ?.mapNotNull { Bukkit.getEntity(it).takeIf { i -> i is Interaction && i.passengers.isEmpty() } }
            ?.minWithOrNull(Comparator.comparingDouble { centeredLoc.distanceSquared(it.location) })
            ?.addPassenger(player)
    }

    fun spawnSeats(baseEntity: ArmorStand, mechanic: BedrockFurnitureMechanic) {
        val location = baseEntity.location.add(0.0, 0.5, 0.0)
        val yaw = baseEntity.location.yaw
        val uuid = baseEntity.uniqueId
        val seatUUIDs = mutableListOf<UUID>()
        mechanic.seats.forEach { seat: FurnitureSeat ->
            location.getWorld().spawn(
                location.clone().add(seat.offset(yaw)),
                Interaction::class.java) { i: Interaction ->
                i.interactionHeight = 0.1f
                i.interactionWidth = 0.1f
                i.isPersistent = true
                i.persistentDataContainer.set(FURNITURE_KEY, PersistentDataType.STRING, mechanic.itemID)
                i.persistentDataContainer.set(SEAT_KEY, DataType.UUID, uuid)
                seatUUIDs.add(i.uniqueId)
            }
        }
        baseEntity.persistentDataContainer.set(SEAT_KEY, DataType.asList(DataType.UUID), seatUUIDs)
    }

    fun removeSeats(baseEntity: ArmorStand) {
        baseEntity.persistentDataContainer.getOrDefault(SEAT_KEY, DataType.asList(DataType.UUID), listOf())
            .map(Bukkit::getEntity).filterIsInstance<Interaction>().forEach { seat: Entity ->
                seat.passengers.forEach(seat::removePassenger)
                if (!seat.isDead) seat.remove()
            }
    }
}