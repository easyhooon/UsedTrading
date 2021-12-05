package kr.ac.konkuk.usedtrading.chatdetail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kr.ac.konkuk.usedtrading.DBKeys.Companion.DB_CHATS
import kr.ac.konkuk.usedtrading.R

class ChatRoomActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private val chatList = mutableListOf<ChatItem>()
    private val adapter = ChatItemAdapter()
    private var chatDB: DatabaseReference?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        val chatKey = intent.getLongExtra("chatKey", -1)

        //채팅방 KEY를 하나 받아옴
        chatDB = Firebase.database.reference.child(DB_CHATS).child("$chatKey")

        chatDB?.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatItem = snapshot.getValue(ChatItem::class.java)
                chatItem ?: return

                chatList.add(chatItem)
                //갱신
                adapter.submitList(chatList)
                //key가 없어 DiffUtil이 제대로 동작안할 수도 있기 때문에
                adapter.notifyDataSetChanged()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        findViewById<RecyclerView>(R.id.chatRecyclerView).adapter = adapter
        findViewById<RecyclerView>(R.id.chatRecyclerView).layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.sendButton).setOnClickListener {
            val chatItem = ChatItem(
                senderId = auth.currentUser!!.uid,
                message = findViewById<EditText>(R.id.messageEditText).text.toString()
            )
            chatDB?.push()?.setValue(chatItem)
        }
    }
}