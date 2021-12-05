package kr.ac.konkuk.usedtrading.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.DatabaseErrorHandler
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kr.ac.konkuk.usedtrading.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.usedtrading.R

class AddArticleActivity : AppCompatActivity() {

    private var selectedUri: Uri? = null
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_add)

        findViewById<Button>(R.id.imageAddButton).setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //Permission이 허용이 됬을 경우, ContentProvider로 넘어감
                    startContentProvider()
                }
                //교육용 팝업이 필요한 경우
                shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) ->{
                    showPermissionContextPopup()
                }
                else -> {
                  requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1010)
                }

            }
        }
        findViewById<Button>(R.id.submitButton).setOnClickListener {
            val title = findViewById<EditText>(R.id.titleEditText).text.toString()
            val content = findViewById<EditText>(R.id.contentEditText).text.toString()
            val sellerId = auth.currentUser?.uid.orEmpty()

            showProgress()

            //중간에 이미지가 있으면 업로드 과정을 추가
            if(selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadPhoto(photoUri,
                    //내부 비동기
                    successHandler =  { uri ->
                        //이미지 uri를 첨부해서 업로드
                        //이미지가 있는 상황
                        //업로드한 url을 가져와서 같이 넣어줬기 때문에 url도 함께 들어갈 수 있음
                        uploadArticle(sellerId, title, content, uri)
                    },
                    errorHandler = {
                        //작업을 취소
                        Toast.makeText(this, "사진 업로드에 실패했습니다", Toast.LENGTH_SHORT).show()
                        hideProgress()
                    }
                )
            }else {
                //동기
                //이미지가 없는 상황
                uploadArticle(sellerId, title, content, "")
            }
        }
    }

    //successHandler의 반환값 String
    private fun uploadPhoto(uri: Uri, successHandler: (String) -> Unit, errorHandler: () -> Unit) {
        //파일명이 중복이 되지 않도록
        val fileName = "${System.currentTimeMillis()}.png"
        storage.reference.child("article/photo").child(fileName)
            .putFile(uri)
                //성공했는지 여부를 체크
            .addOnCompleteListener{
                if(it.isSuccessful){
                    storage.reference.child("article/photo").child(fileName).downloadUrl
                        .addOnSuccessListener {uri->
                            //업로드를 성공하면 download url을 가져옴
                            successHandler(uri.toString())
                        }.addOnFailureListener{
                            errorHandler()
                        }
                } else {
                    errorHandler()
                }
            }
    }

    private fun uploadArticle(sellerId: String, title: String, content: String, imageUrl:String) {
        val model = ArticleModel(sellerId, title, System.currentTimeMillis(), "$content 원", imageUrl)

        //임의의 키값 아래에 model이 들어가게 됨
        articleDB.push().setValue(model)

        hideProgress()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            1010 ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startContentProvider()
                }else {
                    Toast.makeText(this, "권한을 거부하셨습니다", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun startContentProvider() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 2020)
    }

    private fun showProgress() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = true
    }

    private fun hideProgress() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode != Activity.RESULT_OK){
            return
        }

        when(requestCode) {
            2020 -> {
                //data안에 사진의 uri가 넘어온것
                //우선 null 처리
                val uri = data?.data
                if (uri != null) {
                    findViewById<ImageView>(R.id.photoImageView).setImageURI(uri)
                    selectedUri = uri
                } else {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.",Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "사진을 가져오지 못했습니다.",Toast.LENGTH_SHORT).show()
            }
        }
    }

    //교육용 팝업
    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("사진을 가져오기 위해 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1010)
            }
            .create()
            .show()
    }
}