package com.portalgg.nexobedrock.mechanics.furniture

import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat.Companion.SEAT_KEY
import com.portalgg.nexobedrock.mechanics.furniture.BedrockFurnitureMechanic.Companion.FURNITURE_KEY
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.persistence.PersistentDataType
import java.util.*

object BedrockFurnitureSeats {

    fun spawnSeats(baseEntity: ArmorStand, mechanic: BedrockFurnitureMechanic) {
        val location = baseEntity.location
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