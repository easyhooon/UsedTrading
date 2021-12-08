package kr.ac.konkuk.usedtrading.chatlist

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kr.ac.konkuk.usedtrading.DBKeys.Companion.CHILD_CHAT
import kr.ac.konkuk.usedtrading.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.usedtrading.R
import kr.ac.konkuk.usedtrading.chatdetail.ChatRoomActivity
import kr.ac.konkuk.usedtrading.databinding.FragmentChatListBinding

class ChatListFragment : Fragment(R.layout.fragment_chat_list) {

    private var binding: FragmentChatListBinding?=null
    private lateinit var chatListAdapter: ChatListAdapter

    private val chatRoomList = mutableListOf<ChatListItem>()

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentChatListBinding = FragmentChatListBinding.bind(view)
        binding = fragmentChatListBinding

        chatListAdapter = ChatListAdapter(onItemClicked = { chatRoom ->
            //채팅방으로 이동 하는 코드
            //this 불가능
            //context를 넣자니 nullable
            //따라서 null check걸어주고 그안에서 구현
            context?.let {
                val intent = Intent(it, ChatRoomActivity::class.java)
                intent.putExtra("chatKey", chatRoom.key)
                startActivity(intent)
            }
        })
        chatRoomList.clear()

        fragmentChatListBinding.chatListRecyclerView.adapter = chatListAdapter
        fragmentChatListBinding.chatListRecyclerView.layoutManager = LinearLayoutManager(context)

        //채팅 리스트를 불러옴
        if(auth.currentUser == null){
            return
        }

        val chatDB = Firebase.database.reference.child(DB_USERS).child(auth.currentUser!!.uid).child(CHILD_CHAT)

        chatDB.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val model = it.getValue(ChatListItem::class.java)
                    model ?: return

                    chatRoomList.add(model)
                }

                chatListAdapter.submitList(chatRoomList)
                chatListAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })


    }

    override fun onResume() {
        super.onResume()

        chatListAdapter.notifyDataSetChanged()
    }
}