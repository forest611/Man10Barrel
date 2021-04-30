package red.man10.man10barrel.upgrade

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import red.man10.man10barrel.Barrel
import red.man10.man10barrel.Man10Barrel.Companion.plugin
import red.man10.man10barrel.RemoteController
import red.man10.man10barrel.Utility
import red.man10.man10barrel.Utility.sendMessage

class SearchUpgrade : Upgrade() ,Listener{

    override val upgradeName: String = "search"

    private val searchUser = HashMap<Player,ItemStack>()


    fun getUpgrade(): ItemStack {
        return super.getUpgrade("§eSearchApp",mutableListOf("検索したアイテムが格納されている","特殊樽を表示します"))
    }

    private fun searchItems(p:Player, controller:ItemStack, keyword:String){

        val jsonList = RemoteController.getStringLocationList(controller)

        val list = mutableListOf<String>()

        sendMessage(p,"現在検索中§kX")

        for (str in jsonList){

            val loc = Utility.jsonToLocation(str)

            val block = loc.block

            if (block.type != Material.BARREL)continue

            val barrelState = block.state
            if (barrelState !is org.bukkit.block.Barrel)continue

            if(!Barrel.isSpecialBarrel(barrelState))continue

            val inv = Barrel.getStorage(barrelState)

            for (item in inv){

                if (item == null || item.type == Material.AIR)continue

                if (item.i18NDisplayName!!.equals(keyword,true)){
                    list.add(Utility.locationToJson(loc))
                    break
                }

                val meta = item.itemMeta?:continue

                if (meta.displayName().toString().contains(keyword,true)){
                    list.add(Utility.locationToJson(loc))
                    break
                }

                if (meta.lore?.contains(keyword) == true){
                    list.add(Utility.locationToJson(loc))
                    break
                }
            }
        }

        if (list.isEmpty()){
            sendMessage(p,"検索結果:0")
            return
        }

        RemoteController.openInventory(controller,p,0)

    }

    fun startSearch(p:Player,controller: ItemStack){

        p.closeInventory()
        sendMessage(p,"チャット欄にキーワードを入れて検索をしてください")
        searchUser[p] = controller
    }

    @EventHandler
    fun chatEvent(e:AsyncChatEvent){

        val p = e.player

        if (!searchUser.containsKey(p))return

        val item = searchUser[p]!!

        e.isCancelled = true

        val keyword = e.message().toString()

        if (keyword == "cancel"){
            searchUser.remove(p)
            sendMessage(p,"検索をキャンセルしました")
            return
        }

        Bukkit.getScheduler().runTask(plugin, Runnable {
            searchItems(p,item,keyword)
            searchUser.remove(p)
        })

    }


}