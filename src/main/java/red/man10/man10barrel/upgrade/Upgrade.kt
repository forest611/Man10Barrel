package red.man10.man10barrel.upgrade

import com.google.gson.Gson
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.man10barrel.Man10Barrel.Companion.plugin

abstract class Upgrade {

    abstract val upgradeName : String


    fun getUpgrade(displayName:String,lore:MutableList<String>):ItemStack{

        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin,keyName), PersistentDataType.STRING,upgradeName)
        meta.displayName(Component.text(displayName))
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        meta.addEnchant(Enchantment.LUCK,1,false)

        meta.lore = lore

        item.itemMeta = meta
        return item

    }

    companion object{


        private const val keyName = "name"
        private const val keyUpgrades = "upgrades"

        fun itemToUpgradeName(itemStack: ItemStack): String? {
            return itemStack.itemMeta.persistentDataContainer[NamespacedKey(plugin, keyName), PersistentDataType.STRING]
        }

        fun getAllUpgrades(controller:ItemStack):List<String>{

            val jsonStr = controller.itemMeta!!.persistentDataContainer[NamespacedKey(plugin, keyUpgrades), PersistentDataType.STRING]?:return emptyList()

            return Gson().fromJson(jsonStr,Array<String>::class.java).toList()
        }

        fun addUpgrade(upgrade:ItemStack,controller: ItemStack):Boolean{

            val upgrades = getAllUpgrades(controller).toMutableList()
            val upgradeName = itemToUpgradeName(upgrade) ?:return false

            //すでにアップグレードがついていたらfalseを返す
            if (upgrades.contains(upgradeName))return false

            upgrades.add(upgradeName)

            val meta = controller.itemMeta!!

            meta.persistentDataContainer.set(NamespacedKey(plugin, keyUpgrades), PersistentDataType.STRING, Gson().toJson(upgrades))

            controller.itemMeta = meta

            upgrade.amount = upgrade.amount-1

            return true
        }
    }

}