package com.portalgg.nexobedrock

import org.bukkit.Bukkit
import org.bukkit.entity.ItemDisplay
import java.util.*

class BedrockFurnitureEntity(
    val baseUuid: UUID,
    val baseId: Int,
    val entityId: Int,
) {

    constructor(baseEntity: ItemDisplay, entityId: Int) : this(baseEntity.uniqueId, baseEntity.entityId, entityId)

    fun equalsBase(baseEntity: ItemDisplay): Boolean {
        return baseUuid == baseEntity.uniqueId && baseId == baseEntity.entityId
    }

    fun baseEntity(): ItemDisplay? {
        return Bukkit.getEntity(baseUuid) as ItemDisplay?
    }
}