package com.androidadam.smack.services

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.androidadam.smack.controller.App
import com.androidadam.smack.model.Channel
import com.androidadam.smack.model.Message
import com.androidadam.smack.utilities.URL_GET_CHANNELS
import com.androidadam.smack.utilities.URL_GET_MESSAGES
import org.json.JSONException

object MessageService {

    val channels = ArrayList<Channel>()
    val messages = ArrayList<Message>()

    fun getChannels(complete: (Boolean) -> Unit) {
        val channelsRequest = object : JsonArrayRequest(Method.GET, URL_GET_CHANNELS, null, Response.Listener {
            try {
                for (x in 0 until it.length()) {
                    val channel = it.getJSONObject(x)
                    val name = channel.getString("name")
                    val channelDescription = channel.getString("description")
                    val channelId = channel.getString("_id")

                    val newChannel = Channel(name, channelDescription, channelId)
                    this.channels.add(newChannel)
                }
                complete(true)
            } catch (e: JSONException) {
                Log.d("JSON", "EXE + ${e.localizedMessage}")
            }
        }, Response.ErrorListener {
            Log.d("ERROR", "Could not retrieve channels")
            complete(false)
        }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer ${App.sharedPreferences.authToken}")
                return headers
            }

        }
        App.sharedPreferences.requestQueue.add(channelsRequest)
    }

    fun getMessages(channelID: String, complete: (Boolean) -> Unit) {
        val url = "$URL_GET_MESSAGES$channelID"
         clearMessages()
        val messagesRequest = object : JsonArrayRequest(Method.GET, url, null, Response.Listener {
            try {
                for (x in 0 until it.length()) {
                    val message = it.getJSONObject(x)
                    val messageBody = message.getString("messageBody")
                    val channelID = message.getString("channelID")
                    val id = message.getString("_id")
                    val userName = message.getString("userName")
                    val userAvatar = message.getString("userAvatar")
                    val userAvatarColor = message.getString("userAvatarColor")
                    val timeStamp = message.getString("timestamp")

                    val newMessage = Message(messageBody, userName, channelID, userAvatar, userAvatarColor, id, timeStamp)
                    this.messages.add(newMessage)
                    complete(true)
                }

            } catch (e: JSONException) {
                Log.d("JSON", "EXE + ${e.localizedMessage}")
            }
        }, Response.ErrorListener {
            Log.d("ERROR", "Could not retrieve channels")
            complete(false)
        }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Authorization", "Bearer ${App.sharedPreferences.authToken}")
                return headers
            }

        }
        App.sharedPreferences.requestQueue.add(messagesRequest)
    }

    fun clearMessages(){
        messages.clear()
    }

    fun clearChannels(){
        channels.clear()
    }
}