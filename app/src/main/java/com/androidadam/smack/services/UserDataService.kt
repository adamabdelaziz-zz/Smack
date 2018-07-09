package com.androidadam.smack.services

import android.graphics.Color
import com.androidadam.smack.controller.App
import java.util.*

object UserDataService {

    var id = ""
    var avatarColor = ""
    var avatarName = ""
    var email = ""
    var name = ""

    fun logout(){
        var id = ""
        var avatarColor = ""
        var avatarName = ""
        var email = ""
        var name = ""
        App.sharedPreferences.authToken = ""
        App.sharedPreferences.userEmail = ""
        App.sharedPreferences.isLoggedIn = false

    }
    fun returnAvatarColor(components: String): Int{
        val strippedColor = components
                .replace("[", "")
                .replace("]", "")
                .replace(",", "")

        var r= 0
        var g= 0
        var b= 0

        val scanner = Scanner(strippedColor)

        if(scanner.hasNext()){
            r = (scanner.nextDouble()*255).toInt()
            g = (scanner.nextDouble()*255).toInt()
            b = (scanner.nextDouble()*255).toInt()
        }

        return Color.rgb(r,g,b)

    }
}