package com.portalgg.nexobedrock.mechanics.furniture

import com.jeff_media.customblockdata.CustomBlockData
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.api.NexoItems
import com.nexomc.nexo.items.ItemBuilder
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.breakable.BreakableMechanic
import com.nexomc.nexo.mechanics.furniture.FurnitureHelpers
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.mechanics.furniture.hitbox.BarrierHitbox
import com.nexomc.nexo.mechanics.furniture.hitbox.FurnitureHitbox
import com.nexomc.nexo.mechanics.furniture.seats.FurnitureSeat
import com.nexomc.nexo.mechanics.light.LightMechanic
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing
import com.nexomc.nexo.utils.AdventureUtils
import com.nexomc.nexo.utils.BlockHelpers
import com.nexomc.nexo.utils.BlockHelpers.toBlockLocation
import com.nexomc.nexo.utils.BlockHelpers.toCenterBlockLocation
import com.nexomc.nexo.utils.ItemUtils.displayName
import com.nexomc.nexo.utils.ItemUtils.editItemMeta
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.drops.Drop
import com.nexomc.nexo.utils.drops.Loot
import com.nexomc.nexo.utils.logs.Logs
import com.nexomc.nexo.utils.wrappers.EnchantmentWrapper
import com.portalgg.nexobedrock.NexoBedrock
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class BedrockFurnitureMechanic(factory: MechanicFactory, section: ConfigurationSection) : Mechanic(factory, section, { itemBuilder: ItemBuilder ->
    itemBuilder.customTag<Byte, Byte>(FURNITURE_KEY, PersistentDataType.BYTE, 1.toByte())
}) {
    val hitbox: FurnitureHitbox = section.getConfigurationSection("hitbox")?.let(::FurnitureHitbox) ?: FurnitureHitbox.EMPTY
    val limitedPlacing: LimitedPlacing? = section.getConfigurationSection("limited_placing")?.let(::LimitedPlacing)
    val light: LightMechanic
    val lightIsToggleable: Boolean = section.getBoolean("lights_toggleable")
    val restrictedRotation: FurnitureMechanic.RestrictedRotation = section.getString("restricted_rotation")?.let(
        FurnitureMechanic.RestrictedRotation::fromString) ?: FurnitureMechanic.RestrictedRotation.STRICT
    val breakable: BreakableMechanic = BreakableMechanic(section)
    val seats = section.getStringList("seats").mapNotNull(FurnitureSeat::getSeat)

    init {
        val barrierHitboxes = hitbox.barriers().map(BarrierHitbox::toVector3f)
        val lightBlocks = LightMechanic(section).lightBlocks
        val overlap = lightBlocks.filter { it.toVector3f() in barrierHitboxes }.joinToString()
        if (overlap.isNotEmpty()) {
            Logs.logError("Furniture $itemID has lights that overlap with the barrierHitboxes at: $overlap")
            Logs.logWarn("Nexo will ignore any lights that conflict with a barrier...")
        }

        this.light = lightBlocks.filter { it.toVector3f() !in barrierHitboxes }.let(::LightMechanic)
    }

    fun place(location: Location, yaw: Float, facing: BlockFace, checkSpace: Boolean): ArmorStand? {
        if (!location.isWorldLoaded()) return null
        if (checkSpace && this.notEnoughSpace(location, yaw)) return null
        checkNotNull(location.getWorld())

        val baseEntity = location.getWorld().spawn(correctedSpawnLocation(location, facing), ArmorStand::class.java) { e ->
            setBaseFurnitureData(e, yaw)
        }

        BedrockFurnitureSeats.spawnSeats(baseEntity, this)

        return baseEntity
    }

    fun spawnHitboxes(baseEntity: ArmorStand, yaw: Float) {
        baseEntity.persistentDataContainer.set(FURNITURE_KEY, PersistentDataType.STRING, itemID)
        hitbox.barrierLocations(baseEntity.location, yaw).also {
            baseEntity.persistentDataContainer.set(BARRIER_KEY, DataType.LOCATION_ARRAY, it.toTypedArray())
        }.forEach {
            it.block.type = Material.BARRIER
            CustomBlockData(it.block, NexoBedrock.instance()).apply {
                set(FURNITURE_KEY, DataType.STRING, itemID)
                set(BASE_ENTITY_KEY, DataType.UUID, baseEntity.uniqueId)
            }
        }
        hitbox.interactions().map { hitbox ->
            baseEntity.world.spawn(baseEntity.location.add(hitbox.offset(yaw)), Interaction::class.java) {
                it.interactionWidth = hitbox.width
                it.interactionHeight = hitbox.height
                it.persistentDataContainer.apply {
                    set(FURNITURE_KEY, DataType.STRING, itemID)
                    set(BASE_ENTITY_KEY, DataType.UUID, baseEntity.uniqueId)
                }
            }.uniqueId
        }.also {
            baseEntity.persistentDataContainer.set(INTERACTION_KEY, DataType.asList(DataType.UUID), it)
        }
    }

    fun removeBaseEntity(baseEntity: ArmorStand) {
        BedrockFurnitureSeats.removeSeats(baseEntity)
        removeHitboxes(baseEntity)

        if (!baseEntity.isDead) baseEntity.remove()
    }

    fun removeHitboxes(baseEntity: ArmorStand) {
        baseEntity.persistentDataContainer.get(INTERACTION_KEY, DataType.asList(DataType.UUID))?.forEach {
            Bukkit.getEntity(it)?.remove()
        }
        baseEntity.persistentDataContainer.get(BARRIER_KEY, DataType.LOCATION_ARRAY)?.forEach {
            if (it.block.type != Material.BARRIER) return@forEach
            it.block.type = Material.AIR
            CustomBlockData(it.block, NexoBedrock.instance()).clear()
        }
    }



    private fun correctedSpawnLocation(baseLocation: Location, facing: BlockFace): Location {
        val isWall = limitedPlacing?.isWall == true
        val isRoof = limitedPlacing?.isRoof == true
        val solidBelow = baseLocation.block.getRelative(BlockFace.DOWN).isSolid
        val hitboxOffset = (hitbox.hitboxHeight() - 1).toFloat().takeUnless { isRoof && facing == BlockFace.DOWN } ?: -0.49f

        return BlockHelpers.toCenterBlockLocation(baseLocation).apply {
            if (isRoof && facing == BlockFace.DOWN) y += -hitboxOffset
        }
    }

    private fun setBaseFurnitureData(baseEntity: ArmorStand, yaw: Float) {
        baseEntity.isPersistent = true
        baseEntity.isInvulnerable = true
        baseEntity.isSilent = true
        baseEntity.isCustomNameVisible = false
        baseEntity.isInvisible = true
        baseEntity.setGravity(false)
        val item = NexoItems.itemFromId(itemID)
        val customName = item?.itemName ?: item?.displayName ?: Component.text(itemID)
        if (VersionUtil.isPaperServer) baseEntity.customName(customName)
        else baseEntity.customName = AdventureUtils.LEGACY_SERIALIZER.serialize(customName)

        baseEntity.setRotation(yaw, 0f)
        baseEntity.setItem(EquipmentSlot.HEAD, NexoItems.itemFromId(itemID)?.build())

        spawnHitboxes(baseEntity, yaw)
    }

    fun notEnoughSpace(rootLocation: Location, yaw: Float): Boolean {
        return hitbox.hitboxLocations(rootLocation.clone(), yaw).any { !BlockHelpers.isReplaceable(it.block) }
    }

    companion object {
        val FURNITURE_KEY = NamespacedKey(NexoBedrock.instance(), "furniture")
        val BASE_ENTITY_KEY = NamespacedKey(NexoBedrock.instance(), "base_entity")
        val INTERACTION_KEY = NamespacedKey(NexoBedrock.instance(), "interaction")
        val BARRIER_KEY = NamespacedKey(NexoBedrock.instance(), "barrier")

        fun furnitureSpawns(baseEntity: ArmorStand, drop: Drop, itemInHand: ItemStack) {
            val baseItem = NexoItems.itemFromId(drop.sourceID)!!.build()
            val location = toBlockLocation(baseEntity.location)
            val furnitureItem = baseEntity.getItem(EquipmentSlot.HEAD).takeIf { it.type != Material.AIR } ?: NexoItems.itemFromId(drop.sourceID)?.build() ?: return
            editItemMeta(furnitureItem) { itemMeta: ItemMeta ->
                baseItem.itemMeta?.takeIf(ItemMeta::hasDisplayName)?.let { displayName(itemMeta, it) }
            }

            if (!drop.canDrop(itemInHand) || !location.isWorldLoaded) return
            checkNotNull(location.world)

            when {
                drop.isSilktouch && itemInHand.itemMeta?.hasEnchant(EnchantmentWrapper.SILK_TOUCH) == true ->
                    location.world.dropItemNaturally(toCenterBlockLocation(location), baseItem)
                else -> {
                    drop.dropLoot(drop.loots().filter { it.itemStack() != baseItem }, location, drop.fortuneMultiplier(itemInHand))
                    drop.dropLoot(drop.loots().filter { it.itemStack() == baseItem }.map { Loot(drop.sourceID, furnitureItem, it.probability, it.amount) }, location, drop.fortuneMultiplier(itemInHand))
                }
            }
        }
    }
}