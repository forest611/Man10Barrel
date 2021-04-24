package red.man10.man10barrel

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

object Utility {

    val gson = Gson()

    fun jsonToLocation(string: String): Location {

        val jsonObj = gson.fromJson(string, LocationProperty::class.java)

        return Location(Bukkit.getWorld(jsonObj.world), jsonObj.x, jsonObj.y, jsonObj.z,jsonObj.yaw,jsonObj.pitch)
    }

    //現在のロケーションをJsonデータに変換
    fun locationToJson(location: Location):String{

        val property = LocationProperty()

        property.world = location.world.name
        property.x = location.x
        property.y = location.y
        property.z = location.z
        property.pitch = location.pitch
        property.yaw = location.yaw

        return gson.toJson(property)
    }

    fun sendMessage(p:Player,msg:String){

    }

    class LocationProperty{

        var world : String = ""

        var x  = 0.0
        var y  = 0.0
        var z  = 0.0

        var pitch = 0.0F
        var yaw = 0.0F

    }

}