package kr.ac.konkuk.usedtrading.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kr.ac.konkuk.usedtrading.DBKeys.Companion.CHILD_CHAT
import kr.ac.konkuk.usedtrading.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.usedtrading.DBKeys.Companion.DB_USERS
import kr.ac.konkuk.usedtrading.R
import kr.ac.konkuk.usedtrading.chatlist.ChatListItem
import kr.ac.konkuk.usedtrading.databinding.FragmentHomeBinding


class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var userDB: DatabaseReference
    private lateinit var articleDB: DatabaseReference
    private lateinit var articleAdapter: ArticleAdapter

    private val articleList = mutableListOf<ArticleModel>()

    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            //model 클래스 자체를 업로드하고 다운받음
            val articleModel = snapshot.getValue(ArticleModel::class.java)
            articleModel ?: return

            articleList.add(articleModel)
            articleAdapter.submitList(articleList)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onChildRemoved(snapshot: DataSnapshot) {}
        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        override fun onCancelled(error: DatabaseError) {}

    }

    private var binding: FragmentHomeBinding? = null

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentHomeBinding = FragmentHomeBinding.bind(view)
        binding = fragmentHomeBinding

        //초기화를 해주지 않으면 이미 값이 들어있어서 계속 해서 아이템이 추가됨
        articleList.clear()
        articleDB = Firebase.database.reference.child(DB_ARTICLES)
        userDB = Firebase.database.reference.child(DB_USERS)
        //초기화 코드
        articleAdapter = ArticleAdapter { articleModel ->
            if (auth.currentUser != null) {
                //로그인을 한 상태
                if (auth.currentUser?.uid != articleModel.userId) {
                    //채팅방을 염
                    val chatRoom = ChatListItem(
                        buyerId = auth.currentUser!!.uid,
                        sellerId = articleModel.userId,
                        itemTitle = articleModel.title,
                        key = System.currentTimeMillis()
                    )

                    userDB.child(auth.currentUser!!.uid)
                        .child(CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)

                    userDB.child(articleModel.userId)
                        .child(CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)

                    Snackbar.make(view, "채팅방이 생성되었습니다. 채팅앱에서 확인해주세요.", Snackbar.LENGTH_LONG).show()

                } else {
                    //내가 올린 아이템임
                    Snackbar.make(view, "내가 올린 아이템입니다.", Snackbar.LENGTH_LONG).show()
                }
            } else {
                //로그인을 안한 상태
                Snackbar.make(view, "로그인 후 사용해주세요", Snackbar.LENGTH_LONG).show()
            }
        }

        binding!!.articleRecyclerView.layoutManager = LinearLayoutManager(context)
        binding!!.articleRecyclerView.adapter = articleAdapter

        binding!!.addFloatingButton.setOnClickListener {

            context?.let {
                if(auth.currentUser != null){
                startActivity(Intent(it, AddArticleActivity::class.java))
                } else {
                    Snackbar.make(view, "로그인 후 사용해주세요", Snackbar.LENGTH_LONG).show()
                }

                //이것도 가능
                //startActivity(Intent(requireContext(),ArticleAddActivity::class.java))
            }

        }

        //데이터를 가져옴
        //addSingleValueListener -> 즉시성, 1회만 호출
        //addChildEventListener -> 한번 등록해놓으면 계속 이벤트가 발생할때마다 등록이된다.
        //activity의 경우 activity가 종료되면 이벤트가 다 날라가고 view가 다 destroy됨
        //fragment는 재사용이 되기때문에 onviewcreated가 호출될때마다 중복으로 데이터를 가져오게됨
        //따라서 eventlistener를 전역으로 정의를 해놓고 viewcreated될때마다 attach를 하고 destroy가 될때마다 remove를 해주는 방식을 채택
        articleDB.addChildEventListener(listener)
    }

    override fun onResume() {
        super.onResume()

        //view가 다시 보일때마다 뷰를 다시 그림
        articleAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        articleDB.removeEventListener(listener)
    }
}