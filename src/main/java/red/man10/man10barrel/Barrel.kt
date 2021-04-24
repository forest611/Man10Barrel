package red.man10.man10barrel

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import red.man10.man10barrel.Man10Barrel.Companion.plugin
import red.man10.man10barrel.Utility.sendMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.text.Charsets.UTF_8

object Barrel {

    const val title = "§e§l特殊樽"
    private const val maxByteSize = 65536

    val opened = mutableListOf<Triple<Int,Int,Int>>()

    fun setStorageItem(inv:Inventory,block:Block){

        val list = mutableListOf<ItemStack>()

        for (i in 0..53){
            val item = inv.getItem(i)?:continue
//            if (item == null){
//                //list.add(ItemStack(Material.AIR))
//                continue
//            }
            if (item.type == Material.WRITTEN_BOOK){
                Bukkit.getLogger().warning("WRITTEN BOOK ERROR")
                continue
            }
            list.add(item)
        }

        val state = block.state
        if (state !is Barrel)return

        val base64 = itemStackArrayToBase64(list.toTypedArray())

        if (base64.toByteArray(UTF_8).size>= maxByteSize){
            Bukkit.getLogger().warning("Too many bytes error")
            return
        }

        state.persistentDataContainer.set(NamespacedKey(plugin,"storage"), PersistentDataType.STRING,base64)

        state.update()
    }

    fun openStorage(barrel:Barrel, p:Player){

        val inv = getStorage(barrel)

        p.openInventory(inv?:Bukkit.createInventory(null,54, title))

        val loc = barrel.location
        opened.add(Triple(loc.blockX,loc.blockY,loc.blockZ))

    }

    fun getStorage(state:Barrel):Inventory?{

        if (!isSpecialBarrel(state))return state.inventory

        val inv = Bukkit.createInventory(null,54, title)

        val storage = state.persistentDataContainer[NamespacedKey(plugin,"storage"), PersistentDataType.STRING]?:return inv

        val items = itemStackArrayFromBase64(storage)

        for (item in items){
            inv.addItem(item)
        }

        return inv
    }

    fun isSpecialBarrel(state:Barrel):Boolean{

        if ((state.customName?:return false) != title)return false
        return true
    }

//    fun isSpecialBarrel(block:Block):Boolean{
//
//
//        if (block.type != Material.BARREL)return false
//
//        val barrelState = block.state
//        if (barrelState !is Barrel)return false
//
//        if(!isSpecialBarrel(barrelState))return false
//
//        if ((barrelState.customName?:return false) != title)return false
//        return true
//    }
//
    fun hasItem(barrel: Barrel):Boolean{
        val storage = barrel.persistentDataContainer[NamespacedKey(plugin,"storage"), PersistentDataType.STRING]?:return false

        val items = itemStackArrayFromBase64(storage)

        if (items.isEmpty())return false

        return true

    }
//
//    fun dropStorage(barrel: Barrel){
//        val storage = barrel.persistentDataContainer[NamespacedKey(plugin,"storage"), PersistentDataType.STRING]?:return
//
//        val items = itemStackArrayFromBase64(storage)
//
//        items.forEach { if (it.type != Material.AIR) {barrel.world.dropItem(barrel.location, it) }}
//    }

    fun hasPermission(p:Player,barrel: Barrel):Boolean{

        val owners = barrel.persistentDataContainer[NamespacedKey(plugin,"owners"), PersistentDataType.STRING]?.split(";")?:return false

        if (owners.contains(p.uniqueId.toString()))return true

        return false

    }

    fun removePermission(p:Player,barrel: Barrel){
        val owners = barrel.persistentDataContainer[NamespacedKey(plugin,"owners"), PersistentDataType.STRING]?:""
        owners.replace("${p.uniqueId};","")

        barrel.persistentDataContainer.set(NamespacedKey(plugin,"owners"), PersistentDataType.STRING,owners)

        barrel.update()
    }

    fun addPermission(p:Player,barrel: Barrel){

        var owners = barrel.persistentDataContainer[NamespacedKey(plugin,"owners"), PersistentDataType.STRING]?:""
        owners += "${p.uniqueId};"

        barrel.persistentDataContainer.set(NamespacedKey(plugin,"owners"), PersistentDataType.STRING,owners)

        barrel.update()

    }

    fun addPermission(owner:Player,barrel:Barrel,paper: ItemStack){

        val names = paper.itemMeta.displayName.replace("§o","").split(";")
        for (name in names){

            val p = Bukkit.getPlayer(name)?:continue

            if (p == owner)continue

            if (hasPermission(p,barrel)){
                removePermission(p,barrel)
                sendMessage(p,"§c§l特殊樽の権限が削除されました")

                sendMessage(owner,"§c§l${p.name}削除")
                continue
            }

            addPermission(p,barrel)

            sendMessage(p,"§e§l特殊樽の権限が追加されました！")

            sendMessage(owner,"§e§l${p.name}追加")
        }

    }

    fun isOpen(loc:Location):Boolean{
        return opened.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))
    }

    fun removeMap(loc: Location){
        opened.remove(Triple(loc.blockX,loc.blockY,loc.blockZ))
    }


    ////////////////////////////////////////
    //base64 stack
    /////////////////////////////////////////
    @Throws(IllegalStateException::class)
    fun itemStackArrayToBase64(items: Array<ItemStack>): String {
        try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)

            // Write the size of the inventory
            dataOutput.writeInt(items.size)

            // Save every element in the list
            for (i in items.indices) {
                dataOutput.writeObject(items[i])
            }

            // Serialize that array
            dataOutput.close()
            return Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("Unable to save item stacks.", e)
        }
    }

    @Throws(IOException::class)
    fun itemStackArrayFromBase64(data: String): MutableList<ItemStack> {
        try {
            val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val items = arrayOfNulls<ItemStack>(dataInput.readInt())

            // Read the serialized inventory
            for (i in items.indices) {
                items[i] = dataInput.readObject() as ItemStack
            }

            dataInput.close()
            return unwrapItemStackMutableList(items.toMutableList())
        } catch (e: ClassNotFoundException) {
            throw IOException("Unable to decode class type.", e)
        }

    }

    private fun unwrapItemStackMutableList(list: MutableList<ItemStack?>): MutableList<ItemStack>{
        val unwrappedList = mutableListOf<ItemStack>()
        for (item in list) {
            if (item != null) {
                unwrappedList.add(item)
            }
        }
        return unwrappedList
    }

}