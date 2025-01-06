package com.portalgg.nexobedrock

import com.nexomc.nexo.configs.Settings
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic
import com.nexomc.nexo.utils.VersionUtil
import com.nexomc.nexo.utils.logs.Logs
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player

open class IBedrockFurniturePacketManager {

    companion object {
        val bedrockFurnitureIdMap: ObjectOpenHashSet<BedrockFurnitureEntity> = ObjectOpenHashSet()

        private val SUPPORTED_VERSION = VersionUtil.NMSVersion.entries.toTypedArray()
        private var manager: IBedrockFurniturePacketManager = setupManager()
        var version: String? = null

        @JvmStatic
        fun manager() = manager

        private fun setupManager(): IBedrockFurniturePacketManager {
            SUPPORTED_VERSION.forEach { selectedVersion ->
                if (!VersionUtil.matchesServer(selectedVersion)) return@forEach

                version = selectedVersion.name
                runCatching {
                    manager = Class.forName("com.portalgg.nexobedrock.nms.$version.furniture.BedrockFurniturePacketManager").getConstructor().newInstance() as IBedrockFurniturePacketManager
                    Logs.logSuccess("Version $version has been detected.")
                    Logs.logInfo("NexoBedrock will use the NMSHandler for this version.", true)
                    return manager
                }.onFailure {
                    if (Settings.DEBUG.toBool()) it.printStackTrace()
                    Logs.logWarn("NexoBedrock does not support this version of Minecraft ($version) yet.")
                    Logs.logWarn("NMS features will be disabled...", true)
                    manager = IBedrockFurniturePacketManager()
                }
            }

            return manager
        }
    }

    fun baseEntityFromBedrock(bedrockEntity: Int): ItemDisplay? =
        bedrockFurnitureIdMap.firstOrNull { bedrockEntity == it.entityId }?.baseEntity()

    open fun sendBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }

    open fun sendBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }

    open fun removeBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic, player: Player) {
    }

    open fun removeBedrockFurnitureEntityPacket(baseEntity: ItemDisplay, mechanic: FurnitureMechanic) {
    }
}