package com.androidadam.smack.services

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.androidadam.smack.model.Channel
import com.androidadam.smack.utilities.URL_GET_CHANNELS
import org.json.JSONException

object MessageService {

    val channels = ArrayList<Channel>()

    fun getChannels(context: Context, complete: (Boolean) -> Unit){
            val channelsRequest = object : JsonArrayRequest(Method.GET, URL_GET_CHANNELS, null, Response.Listener {
                try{
                    for(x in 0 until it.length()){
                        val channel = it.getJSONObject(x)
                        val name = channel.getString("name")
                        val channelDescription = channel.getString("description")
                        val channelId = channel.getString("_id")

                        val newChannel = Channel(name, channelDescription, channelId)
                        this.channels.add(newChannel)
                    }
                    complete(true)
                }catch (e:JSONException){
                    Log.d("JSON", "EXE + ${e.localizedMessage}")
                }
            }, Response.ErrorListener {
                Log.d("ERROR", "Could not retrieve channels")
                complete(false)
            }){
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String,String>()
                    headers.put("Authorization", "Bearer ${AuthService.authToken}")
                    return headers
                }

            }
        Volley.newRequestQueue(context).add(channelsRequest)
    }

}