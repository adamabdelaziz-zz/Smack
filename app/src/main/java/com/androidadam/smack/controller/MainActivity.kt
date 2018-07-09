package com.androidadam.smack.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import com.androidadam.smack.R
import com.androidadam.smack.model.Channel
import com.androidadam.smack.services.AuthService
import com.androidadam.smack.services.MessageService
import com.androidadam.smack.services.UserDataService
import com.androidadam.smack.utilities.BROADCAST_USER_DATA_CHANGE
import com.androidadam.smack.utilities.SOCKET_URL
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.nav_header_main.*


class MainActivity : AppCompatActivity(){

    val socket = IO.socket(SOCKET_URL)
    lateinit var channelAdapater: ArrayAdapter<Channel>

    private fun setupAdapters(){
        channelAdapater = ArrayAdapter(this, android.R.layout.simple_list_item_1, MessageService.channels)
        channel_list.adapter = channelAdapater

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        socket.connect()
        socket.on("channelCreated", onNewChannel)


        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        setupAdapters()

        if(App.sharedPreferences.isLoggedIn){
            AuthService.findUserByEmail(this){}
        }
    }

    override fun onResume(){
        LocalBroadcastManager.getInstance(this).registerReceiver(userDataChangeReceiver, IntentFilter(BROADCAST_USER_DATA_CHANGE))
        super.onResume()

    }

    override fun onDestroy() {
        socket.disconnect()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userDataChangeReceiver)
        super.onDestroy()

    }
    private val userDataChangeReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent?) {
           if (App.sharedPreferences.isLoggedIn){
               userNameNavHeader.text = UserDataService.name
               userEmailNavHeader.text = UserDataService.email
               val resourceId = resources.getIdentifier(UserDataService.avatarName,"drawable", packageName)
               userImageNavHeader.setImageResource(resourceId)
               userImageNavHeader.setBackgroundColor(UserDataService.returnAvatarColor(UserDataService.avatarColor))
               loginButtonNavHeader.text = "Logout"

               MessageService.getChannels(context){
                   if (it){
                       channelAdapater.notifyDataSetChanged()
                   }
               }

           }
        }
    }
    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun loginButtonNavClicked(view: View){
        if(App.sharedPreferences.isLoggedIn){
            UserDataService.logout()
            userNameNavHeader.text =""
            userEmailNavHeader.text = ""
            userImageNavHeader.setImageResource(R.drawable.profiledefault)
            userImageNavHeader.setBackgroundColor(Color.TRANSPARENT)
            loginButtonNavHeader.text = "Log In"
        }else {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }

    fun addChannelClicked(view: View){
        if(App.sharedPreferences.isLoggedIn){
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.add_channel_dialog,null)

            builder.setView(dialogView)
                    .setPositiveButton("Add"){dialogInterface, i ->
                        //perform logic when  clicked
                        val nameTextField = dialogView.findViewById<EditText>(R.id.addChannelNameText)
                        val descriptionTextField = dialogView.findViewById<EditText>(R.id.addChannelDescriptionText)
                        val channelName =  nameTextField.text.toString()
                        val channelDescription = descriptionTextField.text.toString()

                        //Create channel with the channel name and description
                        socket.emit("newChannel", channelName, channelDescription)


                    }
                    .setNegativeButton("Cancel"){dialogInterface, i ->
                        //Cancel and close the dialog

                    }
                    .show()
        }else{
            Toast.makeText(this, "Please log in first",Toast.LENGTH_SHORT).show()
        }
    }

    private val onNewChannel = Emitter.Listener { args ->
        runOnUiThread{
            val channelName = args[0] as String
            val channelDescription = args[1] as String
            val channelId = args[2] as String

            val newChannel = Channel(channelName,channelDescription,channelId)
            MessageService.channels.add(newChannel)
            channelAdapater.notifyDataSetChanged()
        }
    }

    fun sendMessageButtonClicked(view: View){
        hideKeyboard()
    }

    fun hideKeyboard(){
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if(inputManager.isAcceptingText){
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }

    }

}
