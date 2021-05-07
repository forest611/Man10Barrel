package red.man10.man10barrel

import net.kyori.adventure.text.Component
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
import red.man10.man10barrel.Man10Barrel.Companion.title
import red.man10.man10barrel.Utility.gson
import red.man10.man10barrel.Utility.sendMessage
import red.man10.realestate.RealEstateAPI
import red.man10.realestate.region.User
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.Reader
import java.util.*
import kotlin.text.Charsets.UTF_8

object Barrel {

    private const val maxByteSize = 65536

    private val blockMap = HashMap<Player,Block>()
    private val isOpen = mutableListOf<Location>()


    fun openStorage(barrel:Barrel, p:Player){

        val inv = getStorage(barrel)

        p.openInventory(inv)

        val loc = barrel.location
        isOpen.add(loc)
        blockMap[p] = barrel.block

    }

    fun closeStorage(inv:Inventory,p:Player){

        setStorageItem(inv,p)

        isOpen.remove(blockMap[p]!!.location)
        blockMap.remove(p)

    }

    private fun setStorageItem(inv:Inventory, p:Player){

        val list = mutableListOf<ItemStack>()

        for (i in 0..53){
            val item = inv.getItem(i)?:continue
            if (item.type == Material.AIR){ continue }
            if (item.type == Material.WRITTEN_BOOK){
                Bukkit.getLogger().warning("WRITTEN BOOK ERROR")
                continue
            }
            list.add(item)
        }

        val state = blockMap[p]!!.state
        if (state !is Barrel)return

        val base64 = itemStackArrayToBase64(list.toTypedArray())

        if (base64.toByteArray(UTF_8).size>= maxByteSize){
            Bukkit.getLogger().warning("Too many bytes error")
            return
        }

        state.persistentDataContainer.set(NamespacedKey(plugin,"storage"), PersistentDataType.STRING,base64)

        state.update()
    }

    fun getStorage(state:Barrel):Inventory{

        if (!isSpecialBarrel(state))return state.inventory

        val inv = Bukkit.createInventory(null,54, Component.text(title))

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

    fun hasItem(barrel: Barrel):Boolean{
        val storage = barrel.persistentDataContainer[NamespacedKey(plugin,"storage"), PersistentDataType.STRING]?:return false

        val items = itemStackArrayFromBase64(storage)

        if (items.isEmpty())return false

        return true

    }

    fun hasPermission(p:Player,barrel: Barrel):Boolean{

        if (RealEstateAPI.hasPermission(p,barrel.location,User.Permission.ALL))return true

        val owners = getPermissions(barrel)?:return false

        if (owners.contains(p.uniqueId.toString()))return true

        return false

    }

    private fun removePermission(p:Player, barrel: Barrel){
        val perms = getPermissions(barrel)?:return

        perms.remove(p.uniqueId.toString())

        barrel.persistentDataContainer.set(NamespacedKey(plugin,"owners"), PersistentDataType.STRING, gson.toJson(perms))

        barrel.update()
    }

    fun addPermission(p:Player,barrel: Barrel){

        val perms = getPermissions(barrel)?:return

        perms.add(p.uniqueId.toString())

        barrel.persistentDataContainer.set(NamespacedKey(plugin,"owners"), PersistentDataType.STRING, gson.toJson(perms))

        barrel.update()

    }

    private fun getPermissions(barrel: Barrel): MutableList<String>? {

        if (!isSpecialBarrel(barrel)) return null

        val str = barrel.persistentDataContainer[NamespacedKey(plugin, "owners"), PersistentDataType.STRING] ?: return mutableListOf()

        return gson.fromJson(str, Array<String>::class.java).toMutableList()
    }

    fun addPermission(owner:Player,barrel:Barrel,paper: ItemStack){

        val names = paper.itemMeta.displayName().toString().replace("§o","").split(";")

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

    fun isOpened(loc:Location):Boolean{
        return isOpen.contains(loc)
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