package com.portalgg.nexobedrock.nms.v1_21_R1.furniture

import net.minecraft.network.protocol.game.*

class BedrockFurniturePacket(
    var entityId: Int,
    var addEntity: ClientboundAddEntityPacket,
    var metadata: ClientboundSetEntityDataPacket,
    var equipmentPacket: ClientboundSetEquipmentPacket
) {
    fun bundlePackets(): ClientboundBundlePacket {
        return ClientboundBundlePacket(listOf(ClientboundRemoveEntitiesPacket(entityId), addEntity, metadata, equipmentPacket))
    }
}