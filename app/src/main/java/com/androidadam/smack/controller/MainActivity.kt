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
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.androidadam.smack.R
import com.androidadam.smack.adapters.MessageAdapter
import com.androidadam.smack.model.Channel
import com.androidadam.smack.model.Message
import com.androidadam.smack.services.AuthService
import com.androidadam.smack.services.MessageService
import com.androidadam.smack.services.UserDataService
import com.androidadam.smack.utilities.BROADCAST_USER_DATA_CHANGE
import com.androidadam.smack.utilities.SOCKET_URL
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*


class MainActivity : AppCompatActivity(){

    val socket = IO.socket(SOCKET_URL)
    lateinit var channelAdapater: ArrayAdapter<Channel>
    lateinit var  messageAdapter: MessageAdapter
    var selectedChannel: Channel? = null

    private fun setupAdapters(){
        channelAdapater = ArrayAdapter(this, android.R.layout.simple_list_item_1, MessageService.channels)
        channel_list.adapter = channelAdapater

        messageAdapter = MessageAdapter(this,MessageService.messages)
        messageListView.adapter = messageAdapter
        val layoutManager = LinearLayoutManager(this)
        messageListView.layoutManager = layoutManager


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        socket.connect()
        socket.on("channelCreated", onNewChannel)
        socket.on("messageCreated", onNewMessage)

        channel_list.setOnItemClickListener{adapterView, view, i, l ->
            selectedChannel = MessageService.channels[i]
            drawer_layout.closeDrawer(GravityCompat.START)
            updateWithChannel()
        }
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

               MessageService.getChannels{
                   if (it){
                       if(MessageService.channels.count() > 0){
                           selectedChannel = MessageService.channels[0]
                           channelAdapater.notifyDataSetChanged()
                           updateWithChannel()
                       }


                   }
               }

           }
        }
    }

    fun updateWithChannel(){
        mainChannelName.text = "${selectedChannel?.name}"
        if(selectedChannel != null){
            MessageService.getMessages(selectedChannel!!.id){
                if(it){
                     messageAdapter.notifyDataSetChanged()
                    if(messageAdapter.itemCount > 0){
                        messageListView.smoothScrollToPosition(messageAdapter.itemCount-1)
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
            channelAdapater.notifyDataSetChanged()
            messageAdapter.notifyDataSetChanged()
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
        if(App.sharedPreferences.isLoggedIn){
            runOnUiThread{
            val channelName = args[0] as String
            val channelDescription = args[1] as String
            val channelId = args[2] as String

            val newChannel = Channel(channelName,channelDescription,channelId)
            MessageService.channels.add(newChannel)
            channelAdapater.notifyDataSetChanged()
        }
        }
    }

    private val onNewMessage = Emitter.Listener {
        if(App.sharedPreferences.isLoggedIn){
        runOnUiThread{
            val channelId = it[2] as String
            if(channelId== selectedChannel?.id){
                val msgBody = it[0] as String
                val userName = it[3] as String
                val userAvatar = it[4]  as String
                val userAvatarColor = it[5]  as String
                val id = it[6]  as String
                val timeStamp = it[7]  as String
                val newMessage = Message(msgBody, userName, channelId, userAvatar, userAvatarColor, id, timeStamp)
                MessageService.messages.add(newMessage)
                messageAdapter.notifyDataSetChanged()
                messageListView.smoothScrollToPosition(messageAdapter.itemCount-1)
            }
        }
        }
    }

    fun sendMessageButtonClicked(view: View){
        if(App.sharedPreferences.isLoggedIn && messageTextField.text.isNotEmpty() && selectedChannel != null){
            val userId = UserDataService.id
            val channelId = selectedChannel!!.id
            socket.emit("newMessage", messageTextField.text.toString(), userId, channelId, UserDataService.name, UserDataService.avatarName, UserDataService.avatarColor)
            messageTextField.text.clear()
            hideKeyboard()
        }

    }

    fun hideKeyboard(){
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if(inputManager.isAcceptingText){
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }

    }

}
