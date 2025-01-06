@file:Suppress("UnstableApiUsage")

package com.portalgg.nexobedrock.nms.v1_21_R2.furniture

import com.mojang.datafixers.util.Pair
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.filterFast
import com.portalgg.nexobedrock.BedrockFurnitureEntity
import com.portalgg.nexobedrock.IBedrockFurniturePacketManager
import com.portalgg.nexobedrock.NexoBedrock
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.Vec3
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import java.util.*

class BedrockFurniturePacketManager : IBedrockFurniturePacketManager() {

    private val bedrockFurniturePacketMap: Object2ObjectOpenHashMap<UUID, BedrockFurniturePacket> = Object2ObjectOpenHashMap()

    override fun sendBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
        baseEntity.world.players.filterFast { it.canSee(baseEntity) && NexoBedrock.instance().bedrockHandler.isBedrockPlayer(it) }.forEach {
            sendBedrockFurnitureEntityPacket(baseEntity, mechanic, it)
        }
    }

    override fun sendBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
        bedrockFurniturePacketMap.computeIfAbsent(baseEntity.uniqueId) { uuid: UUID ->
            val entityId = bedrockFurnitureIdMap.firstOrNull { it.baseUuid == uuid }?.entityId ?: Entity.nextEntityId().also {
                bedrockFurnitureIdMap += BedrockFurnitureEntity(baseEntity, it)
            }
            val loc = baseEntity.location.apply { y -= 0.47 }

            val addEntityPacket = ClientboundAddEntityPacket(
                entityId, UUID.randomUUID(),
                loc.x(), loc.y(), loc.z(), loc.pitch, loc.yaw,
                EntityType.ARMOR_STAND, 1, Vec3.ZERO, 0.0
            )

            val metadataPacket = ClientboundSetEntityDataPacket(
                entityId, listOf(
                    SynchedEntityData.DataValue(0, EntityDataSerializers.BYTE, 0x20),
                    SynchedEntityData.DataValue(15, EntityDataSerializers.BYTE, 0x10)
                )
            )

            val equipmentPacket = ClientboundSetEquipmentPacket(
                entityId, mutableListOf(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(FurnitureHelpers.furnitureItem(baseEntity))))
            )

            BedrockFurniturePacket(entityId, addEntityPacket, metadataPacket, equipmentPacket)
        }?.also { (player as CraftPlayer).handle.connection.send(it.bundlePackets()) }
    }

    override fun removeBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
        val subEntity = bedrockFurnitureIdMap.firstOrNull { it.baseUuid == baseEntity.uniqueId } ?: return
        val removePacket = ClientboundRemoveEntitiesPacket(subEntity.entityId)
        baseEntity.world.players.filterFast { it.canSee(baseEntity) && NexoBedrock.instance().bedrockHandler.isBedrockPlayer(it) }.forEach { player ->
            (player as CraftPlayer).handle.connection.send(removePacket)
        }
        bedrockFurnitureIdMap.remove(subEntity)
        bedrockFurniturePacketMap.remove(baseEntity.uniqueId)
    }

    override fun removeBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
        if (!NexoBedrock.instance().bedrockHandler.isBedrockPlayer(player)) return
        bedrockFurnitureIdMap.firstOrNull { s -> s.baseUuid == baseEntity.uniqueId }?.also { subEntity ->
            (player as CraftPlayer).handle.connection.send(ClientboundRemoveEntitiesPacket(subEntity.entityId))
        }
    }

}
