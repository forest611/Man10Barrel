package red.man10.man10barrel

import org.bukkit.Material
import org.bukkit.block.Barrel
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.man10barrel.Barrel.addPermission
import red.man10.man10barrel.Barrel.closeStorage
import red.man10.man10barrel.Barrel.hasItem
import red.man10.man10barrel.Barrel.hasPermission
import red.man10.man10barrel.Barrel.isOpened
import red.man10.man10barrel.Barrel.isSpecialBarrel
import red.man10.man10barrel.Barrel.openStorage
import red.man10.man10barrel.Man10Barrel.Companion.title
import red.man10.man10barrel.Utility.sendMessage

object BarrelEvent:Listener {


    @EventHandler
    fun setBarrelEvent(e:BlockPlaceEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        addPermission(e.player,barrelState)
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    fun openBarrelEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val block = e.clickedBlock?:return

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return


        val p = e.player

        if (e.isCancelled)return

        if(!isSpecialBarrel(barrelState))return

        if (!p.isSneaking){

            e.isCancelled = true

            if (!hasPermission(p,barrelState)){
                sendMessage(p,"§c§lあなたはこの樽を開く権限がありません！")
                return
            }

            val loc = block.location

            if (isOpened(loc)){
                sendMessage(p,"§c§l現在他のプレイヤーが開いています！")
                return
            }

            openStorage(barrelState,p)

            return
        }

        if (e.hasItem()){

            val item = e.item!!

            if ( item.type == Material.PAPER){
                if (!hasPermission(p,barrelState))return

                addPermission(p,barrelState,e.item!!)

                sendMessage(p,"§e§l権限の設定に成功しました！")

                e.isCancelled = true
                return
            }

            if (RemoteController.isController(item)){

                if (!hasPermission(p,barrelState)){
                    sendMessage(p,"§c§lあなたはこの樽を登録する権限がありません！")
                    return
                }

                val ret = RemoteController.editLocation(item, block.location, p)

                sendMessage(p,when(ret){

                    0 -> "端末を持っていなかった"
                    1 -> "端末から特殊樽を削除した！"
                    2 -> "端末に特殊樽を登録した！"
                    3 -> "投票ダイヤを持っていなかった！"
                    4 -> "確認のため、もう一度シフトクリックしてください"


                    else -> "不明エラー $ret"
                })

                e.isCancelled = true
                return
            }

        }else{
            e.isCancelled = true
        }

        return
    }

    @EventHandler
    fun closeInventory(e:InventoryCloseEvent){

        if (e.view.title != title)return

        val p = e.player as Player

        closeStorage(e.inventory,p)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun breakBarrel(e:BlockBreakEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val state = block.state
        if (state !is Barrel)return

        if ((state.customName?:return) != title)return

        val loc = block.location

        val p = e.player

        if (!hasPermission(p,state)){
            sendMessage(p,"§c§lあなたはこの樽を壊す権限がありません")
            e.isCancelled = true
            return
        }

        if (isOpened(loc)){
            sendMessage(p,"§c§l現在他のプレイヤーが開いています！")
            e.isCancelled = true
            return
        }

        if(hasItem(state)){
            sendMessage(p,"§c§l中にアイテムが入っています！")
            e.isCancelled = true
            return
        }

    }

}