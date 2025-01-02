package com.portalgg.nexobedrock.mechanics.furniture

import com.jeff_media.customblockdata.CustomBlockData
import com.jeff_media.morepersistentdatatypes.DataType
import com.nexomc.nexo.mechanics.Mechanic
import com.nexomc.nexo.mechanics.MechanicFactory
import com.nexomc.nexo.mechanics.MechanicsManager
import com.nexomc.nexo.mechanics.limitedplacing.LimitedPlacing
import com.portalgg.nexobedrock.NexoBedrock
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.inventory.ItemStack

class BedrockFurnitureFactory(
    val furnitureLimit: RadiusLimitation = NexoBedrock.instance().config.getConfigurationSection("bedrock_furniture.furniture_limit").let(::RadiusLimitation)
) : MechanicFactory("bedrock_furniture") {

    class RadiusLimitation(section: ConfigurationSection?) {
        val radius = section?.getDouble("radius", -1.0) ?: -1.0
        val amount = section?.getInt("amount", -1) ?: -1
    }

    init {
        MechanicsManager.registerListeners(NexoBedrock.instance(), mechanicID, BedrockFurnitureListener(this))
    }

    override fun getMechanic(itemID: String?): BedrockFurnitureMechanic? = super.getMechanic(itemID) as? BedrockFurnitureMechanic
    override fun getMechanic(itemStack: ItemStack?): BedrockFurnitureMechanic? = super.getMechanic(itemStack) as? BedrockFurnitureMechanic
    fun getMechanic(entity: Entity?): BedrockFurnitureMechanic? = getMechanic(entity?.persistentDataContainer?.get(BedrockFurnitureMechanic.FURNITURE_KEY, DataType.STRING))
    fun getMechanic(block: Block): BedrockFurnitureMechanic? = getMechanic(CustomBlockData(block, NexoBedrock.instance()).get(BedrockFurnitureMechanic.FURNITURE_KEY, DataType.STRING))
    fun getMechanic(location: Location): BedrockFurnitureMechanic? = getMechanic(location.block)
        ?: location.world.getNearbyEntitiesByType(ArmorStand::class.java, location, 1.0,1.0,1.0).firstOrNull()?.let(::getMechanic)
    fun isFurniture(entity: Entity?) = entity is ArmorStand && getMechanic(entity) != null
    fun isFurniture(itemStack: ItemStack?) = getMechanic(itemStack) != null
    fun isFurniture(itemId: String?) = getMechanic(itemId) != null
    fun getBaseEntity(block: Block): ArmorStand? = CustomBlockData(block, NexoBedrock.instance()).get(BedrockFurnitureMechanic.BASE_ENTITY_KEY, DataType.UUID)?.let(Bukkit::getEntity) as? ArmorStand
    fun getBaseEntity(interaction: Interaction): ArmorStand? = interaction.persistentDataContainer.get(BedrockFurnitureMechanic.BASE_ENTITY_KEY, DataType.UUID)?.let(Bukkit::getEntity) as? ArmorStand

    override fun parse(section: ConfigurationSection) = BedrockFurnitureMechanic(this, section).apply(::addToImplemented)
}