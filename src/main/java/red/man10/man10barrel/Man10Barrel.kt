package red.man10.man10barrel

import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class Man10Barrel : JavaPlugin() {

    companion object{
        lateinit var plugin : Man10Barrel
        lateinit var votingDiamond : ItemStack
    }

    override fun onEnable() {

        plugin = this

        // Plugin startup logic
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}