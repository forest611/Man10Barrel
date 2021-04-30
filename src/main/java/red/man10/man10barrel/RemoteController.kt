package red.man10.man10barrel

import com.google.gson.Gson
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.man10barrel.Man10Barrel.Companion.plugin
import red.man10.man10barrel.Man10Barrel.Companion.title
import red.man10.man10barrel.Man10Barrel.Companion.votingDiamond
import red.man10.man10barrel.Utility.sendMessage
import red.man10.man10barrel.upgrade.PasswordUpgrade
import red.man10.man10barrel.upgrade.SearchUpgrade
import red.man10.man10barrel.upgrade.Upgrade

object RemoteController : Listener{

    private const val controllerName = "§bRemoteController"
    private const val customModel = 370

    private const val key = "Ver.1.0"

    private val checkingMap = HashMap<Player,String>()
    private val invMap = HashMap<Player, InvMap>()

    private val gson = Gson()

    val password = PasswordUpgrade()
    val search = SearchUpgrade()

    private const val keyController = "controller"
    private const val keyLocation = "location"

    class InvMap{
        var nowPage = 0
        lateinit var controller : ItemStack
    }

    fun getController():ItemStack{

        val controller = ItemStack(Material.IRON_HOE)

        controller.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        controller.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
        controller.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)

        val meta = controller.itemMeta
        meta.setCustomModelData(customModel)
        meta.displayName(Component.text(controllerName))
        meta.isUnbreakable = true

        val list = mutableListOf<Component>()

        list.add(Component.text("§f[1]ボタンで前のページ"))
        list.add(Component.text("§f[2]ボタンで次のページ"))

        meta.lore(list)

        meta.persistentDataContainer.set(NamespacedKey(plugin, keyController), PersistentDataType.STRING, key)

        controller.itemMeta = meta

        Upgrade.addUpgrade(password.getUpgrade(),controller)

        return controller

    }

    fun isController(item:ItemStack):Boolean{

        if (item.hasItemMeta()){
            val key = item.itemMeta.persistentDataContainer[NamespacedKey(plugin, keyController), PersistentDataType.STRING]

            if (key == RemoteController.key)return true
        }
        return false
    }

    fun editLocation(controller:ItemStack,loc:Location,p:Player):Int{

        var ret = 0

        if (!isController(controller))return ret

        val jsonLoc = Utility.locationToJson(loc)

        val list = getStringLocationList(controller).toMutableList()


        if (list.contains(jsonLoc)){

            ret = 4

            //確認画面を出す
            if (checkingRemove(p,jsonLoc)){
                list.remove(jsonLoc)
                ret = 1
            }

        }else{

            var removed = false

            for (item in p.inventory){
                if (item == null|| item.type == Material.AIR)continue

                if (item.isSimilar(votingDiamond)){
                    removed = true
                    item.amount = item.amount -1
                    break
                }
            }

            ret = if (removed){
                list.add(jsonLoc)
                2
            }else{ 3 }

        }

        setStringLocationList(controller,list)

        return ret
    }

    private fun checkingRemove(p:Player, loc:String):Boolean{

        if (checkingMap[p]==null || checkingMap[p] != loc){
            checkingMap[p] = loc
            return false
        }
        checkingMap.remove(p)
        return true
    }

    fun getStringLocationList(controller: ItemStack):List<String>{

        if (!isController(controller))return emptyList()

        val str = controller.itemMeta!!.persistentDataContainer[NamespacedKey(plugin, keyLocation), PersistentDataType.STRING]?:return emptyList()

        return gson.fromJson(str, Array<String>::class.java).toList()
    }

    private fun setStringLocationList(controller: ItemStack, locList:List<String>){

        if (!isController(controller))return

        val meta = controller.itemMeta
        meta.persistentDataContainer.set(NamespacedKey(plugin, keyLocation), PersistentDataType.STRING, gson.toJson(locList))
        controller.itemMeta = meta

    }

    private fun removeLocation(controller: ItemStack, page:Int){
        val newList = getStringLocationList(controller).toMutableList()
        newList.removeAt(page)
        setStringLocationList(controller, newList)

    }

    fun openInventory(controller:ItemStack,p:Player,page:Int){

        if (!isController(controller))return

        val locList = getStringLocationList(controller)

        if (locList.isNullOrEmpty()){
            sendMessage(p,"特殊樽を登録してください")
            return
        }

        val loc = Utility.jsonToLocation(locList[page])

        val block = loc.block

        if (block.type != Material.BARREL){
            sendMessage(p, "§cなくなった${title}§cがありました")
            removeLocation(controller,page)
            openInventory(controller, p, 0)
            return
        }

        val barrelState = block.state

        if(barrelState !is org.bukkit.block.Barrel || !Barrel.isSpecialBarrel(barrelState)){
            sendMessage(p, "§cなくなった${title}がありました")

            removeLocation(controller,page)
            openInventory(controller, p, 0)
            return
        }

        if (Barrel.isOpened(loc)){
            sendMessage(p, "§c§l現在他のプレイヤーが開いています！")
            return
        }

        Barrel.openStorage(barrelState, p)

        val data = InvMap()

        data.controller = controller
        data.nowPage = page

        invMap[p] = data

    }

    @EventHandler
    fun changePageEvent(e:InventoryClickEvent){

        if (e.view.title != title)return

        val p = e.whoClicked as Player

        val map = invMap[p]?:return
        val page = map.nowPage
        val controller = map.controller

        //コントローラーはさわれないようにする
        if (e.hotbarButton >= 0){ e.isCancelled = true}

        val current = e.currentItem

        if (current !=null){
            if (current.isSimilar(controller))e.isCancelled = true

            val upgrade = Upgrade.itemToUpgradeName(current)

            if (upgrade!=null){
                val success = Upgrade.addUpgrade(current,controller)
                if (success){
                    sendMessage(p,"アップグレード${upgrade}をセットしました！")
                    return
                }
                sendMessage(p,"アップグレード${upgrade}のセットに失敗しました！")
            }

        }

        when(e.hotbarButton){
            0 ->{//ページ戻る

                val newPage = page - 1

                if (newPage<0)return

                val list = getStringLocationList(controller)

                if (Barrel.isOpened(Utility.jsonToLocation(list[newPage]).block.location)){
                    sendMessage(p, "§c§l現在他のプレイヤーが開いています！")
                    return
                }

                p.playSound(p.location, Sound.UI_BUTTON_CLICK,0.3F,1.0F)

                openInventory(controller,p, newPage)

            }

            1 ->{//ページ進む

                val list = getStringLocationList(controller)

                val newPage = page + 1

                if (list.size<=(newPage))return

                if (Barrel.isOpened(Utility.jsonToLocation(list[newPage]).block.location)){
                    sendMessage(p, "§c§l現在他のプレイヤーが開いています！")
                    return
                }

                p.playSound(p.location, Sound.UI_BUTTON_CLICK,0.3F,1.0F)

                openInventory(controller,p, newPage)

            }

            2 ->{//現在のページ確認
                sendMessage(p,"現在のページ:$page")
                return
            }

            3 ->{

                //パスワードをデフォルトで実装する
                if (Upgrade.getAllUpgrades(controller).contains("password")){
                    password.openPasswordSetting(p,controller)
                    return
                }
            }

            4 ->{

                if (Upgrade.getAllUpgrades(controller).contains("search")){
                    search.startSearch(p,controller)
                    return
                }
            }

        }

    }

    @EventHandler
    fun openController(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_AIR)return

        val item = e.item?:return

        if (!isController(item))return

        val p = e.player

        //パスワードの確認処理追加
        if (password.hasPassword(item)){
            password.openCheckingPassword(p,item)
            return
        }

        openInventory(item,p,0)

    }

    @EventHandler
    fun closeController(e:InventoryCloseEvent){

        if (e.view.title().toString() != title)return

        val p = e.player as Player

        Barrel.closeStorage(e.inventory,p)

        invMap.remove(p)

    }

    @EventHandler
    fun dropEvent(e:PlayerDropItemEvent){

        val item = e.itemDrop.itemStack

        if (isController(item)){
            e.isCancelled = true
            return
        }

    }


}