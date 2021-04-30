package red.man10.man10barrel

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import red.man10.man10barrel.upgrade.Upgrade

class Man10Barrel : JavaPlugin() {

    companion object{
        lateinit var plugin : Man10Barrel
        lateinit var votingDiamond : ItemStack
        const val title = "§e§lSpecialBarrel"
    }

    override fun onEnable() {

        saveDefaultConfig()

        plugin = this

        server.pluginManager.registerEvents(BarrelEvent,this)
        server.pluginManager.registerEvents(RemoteController.search,this)
        server.pluginManager.registerEvents(RemoteController.password,this)
        server.pluginManager.registerEvents(RemoteController,this)

        votingDiamond = config.getItemStack("votingDiamond")?: ItemStack(Material.DIAMOND)
        // Plugin startup logic
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (args.isNullOrEmpty()){

            return true
        }

        when(args[0]){

            "get" ->{

                if (sender !is Player)return false

                val barrel = ItemStack(Material.BARREL)
                val meta = barrel.itemMeta
                meta.displayName(Component.text(title))
                barrel.itemMeta = meta
                sender.inventory.addItem(barrel)

                return true
            }

            "remote" ->{
                if (sender !is Player)return false

                sender.inventory.addItem(RemoteController.getController())
                return true
            }

            "upgrade" ->{
                if (sender !is Player)return false

                sender.inventory.addItem(RemoteController.search.getUpgrade())
            }

            "setvd" ->{
                if (sender !is Player)return false

                votingDiamond = sender.inventory.itemInMainHand
                config.set("votingDiamond",sender.inventory.itemInMainHand)
                saveConfig()
            }

            "status" ->{
                if (sender !is Player)return false

                val item = sender.inventory.itemInMainHand

                if (!RemoteController.isController(item))return true

                Utility.sendMessage(sender, "pages:${RemoteController.getStringLocationList(item).size}")
                Utility.sendMessage(sender, "upgrades:${Upgrade.getAllUpgrades(item)}")

            }

        }

        return true

    }

}