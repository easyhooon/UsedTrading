package kr.ac.konkuk.usedtrading.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kr.ac.konkuk.usedtrading.DBKeys.Companion.DB_ARTICLES
import kr.ac.konkuk.usedtrading.adapter.PhotoListAdapter
import kr.ac.konkuk.usedtrading.databinding.ActivityAddArticleBinding
import kr.ac.konkuk.usedtrading.gallery.GalleryActivity
import kr.ac.konkuk.usedtrading.photo.CameraActivity
import java.lang.Exception

class AddArticleActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1000
        const val GALLERY_REQUEST_CODE = 1001
        const val CAMERA_REQUEST_CODE = 1002

        const val URI_LIST_KEY = "uriList"

    }

    private var imageUriList: ArrayList<Uri> = arrayListOf()
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    private val photoListAdapter = PhotoListAdapter { uri -> removePhoto(uri)}

    private lateinit var binding: ActivityAddArticleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() = with(binding) {

        // adapter 장착
        photoRecyclerView.adapter = photoListAdapter

        imageAddButton.setOnClickListener {
            showPictureUploadDialog()
        }

        submitButton.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val content = binding.contentEditText.text.toString()
            val userId = auth.currentUser?.uid.orEmpty()

            showProgress()

            //중간에 이미지가 있으면 업로드 과정을 추가
            if (imageUriList.isNotEmpty()) {
                // 여러장을 한꺼번에 올려주기 위해서 비동기 처리
                    lifecycleScope.launch {
                        val results = uploadPhoto(imageUriList)
                        // 제네릭으로 해당 타입에 해당하는 것만 빼줌
                        afterUploadPhoto(results, title, content, userId)
                    }
//                uploadPhoto(imageUriList.first(),
//                    //내부 비동기
//                    successHandler = { uri ->
//                        //이미지 uri를 첨부해서 업로드
//                        //이미지가 있는 상황
//                        //업로드한 url을 가져와서 같이 넣어줬기 때문에 url도 함께 들어갈 수 있음
//                        uploadArticle(userId, title, content, uri)
//                    },
//                    errorHandler = {
//                        //작업을 취소
//                        Toast.makeText(this@AddArticleActivity, "사진 업로드에 실패했습니다", Toast.LENGTH_SHORT).show()
//                        hideProgress()
//                    }
//                )
            } else {
                //동기
                //이미지가 없는 상황
                uploadArticle(userId, title, content, listOf())
            }
        }
    }


    //successHandler 의 반환값 String
    /*
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
    */

    private suspend fun uploadPhoto(uriList: List<Uri>) = withContext(Dispatchers.IO) {
        //파일명이 중복이 되지 않도록
        val uploadDeferred: List<Deferred<Any>> = uriList.mapIndexed { index, uri ->
            // async 는 어떠한 값을 반환을 해주고 await 으로 기다릴 수 있다.
            lifecycleScope.async {
                try {
                    val fileName = "image${index}.png"
                    return@async storage
                        .reference.child("article/photo")
                        .child(fileName)
                        .putFile(uri)
                        .await()
                        .storage
                        .downloadUrl
                        .await()
                        .toString()
                    //deferred 에는 uri 가 들어가게 됨
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@async Pair(uri, e)
                }
            }
        }
        // task 가 순차적으로 이뤄지지 않고 동시에 이루어짐
        // 마지막 task 까지 완료되면 그 결과값을 반환
        return@withContext uploadDeferred.awaitAll()
    }

    // upload photo error handling
    private fun afterUploadPhoto(results: List<Any>, title: String, content: String, userId: String) {
        val errorResults = results.filterIsInstance<Pair<Uri, Exception>>()
        val successResults = results.filterIsInstance<String>()

        when {
            errorResults.isNotEmpty() && successResults.isNotEmpty() -> {
                photoUploadErrorButContinueDialog(errorResults, successResults, title, content, userId)
            }
            errorResults.isNotEmpty() && successResults.isEmpty() -> {
                // 사진 업로드 실패한 경우
                uploadError()
            }
            // 성공적으로 업로드
            else -> {
                uploadArticle(userId, title, content, successResults)
            }
        }
    }

    private fun uploadArticle(userId: String, title: String, content: String, imageUrlList: List<String>) {
        val model = ArticleModel(userId, title, System.currentTimeMillis(), content, imageUrlList)

        //임의의 키값 아래에 model 이 들어가게 됨
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
            PERMISSION_REQUEST_CODE ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startGalleryScreen()
                }else {
                    Toast.makeText(this, "권한을 거부하셨습니다", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun startCameraScreen() {
        startActivityForResult(
            CameraActivity.newIntent(this),
            CAMERA_REQUEST_CODE
        )
    }

    private fun startGalleryScreen() {
        // 내부 로컬 스토리지에 접근하는 방법
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "image/*"
//        startActivityForResult(intent, GALLERY_REQUEST_CODE)
        startActivityForResult(
            GalleryActivity.newIntent(this),
            GALLERY_REQUEST_CODE
        )
    }

//    private fun startContentProvider() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "image/*"
//        startActivityForResult(intent, GALLERY_REQUEST_CODE)
//    }

    private fun showProgress() {
        binding.progressBar.isVisible = true
    }

    private fun hideProgress() {
        binding.progressBar.isVisible = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode != Activity.RESULT_OK){
            return
        }

        when(requestCode) {
            GALLERY_REQUEST_CODE -> {
                // 내부 로컬 스토리지에 접근할 경우 처리 방법
                //data안에 사진의 uri가 넘어온것
                //우선 null 처리
//                val uri = data?.data
//                if (uri != null) {
//                    imageUriList.add(uri)
//                    // RecyclerView Adapter 에 데이터 반영
//                    photoListAdapter.setPhotoList(imageUriList)
//                } else {
//                    Toast.makeText(this, "사진을 가져오지 못했습니다.",Toast.LENGTH_SHORT).show()
//                }
                data?.let { intent ->
                    val uriList = intent.getParcelableArrayListExtra<Uri>(URI_LIST_KEY)
                    uriList?.let {list ->
                        imageUriList.addAll(list)
                        photoListAdapter.setPhotoList((imageUriList))
                    }
                } ?: kotlin.run {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_REQUEST_CODE -> {
                // 촬영된 사진들을 프리뷰에서 봤고 확인버튼을 누르면 값을 넘겨받을 수 있도록
                data?.let { intent ->
                    val uriList = intent.getParcelableArrayListExtra<Uri>(URI_LIST_KEY)
                    uriList?.let {list ->
                        imageUriList.addAll(list)
                        photoListAdapter.setPhotoList((imageUriList))
                    }
                } ?: kotlin.run {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
            .create()
            .show()
    }

    // 람다함수를 파라미터로 받음
    private fun checkExternalStoragePermission(uploadAction: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                uploadAction()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                showPermissionContextPopup()
            }
            else -> {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun showPictureUploadDialog() {
        AlertDialog.Builder(this)
            .setTitle("사진 첨부")
            .setMessage("사진첨부할 방식을 선택하세요")
            .setPositiveButton("카메라") { _ , _ ->
                checkExternalStoragePermission {
                    startCameraScreen()
                }
            }
            .setNegativeButton("갤러리") { _ , _ ->
                checkExternalStoragePermission {
                    startGalleryScreen()
                }
            }
            .create()
            .show()
    }
    
    private fun photoUploadErrorButContinueDialog(
        errorResults: List<Pair<Uri, Exception>>,
        successResults: List<String>,
        title: String,
        content: String,
        userId: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("특정 이미지 업로드 실패")
            .setMessage("업로드에 실패한 이미지가 있습니다." + errorResults.map { (uri, _) ->
            "$uri\n"
        } + "그럼에도 불구하고 업로드 하시겠습니까?")
            .setPositiveButton("업로드") { _, _ ->
                uploadArticle(userId, title, content, successResults)
            }
            .create()
            .show()
    }

    private fun uploadError() {
        Toast.makeText(this, "사진 업로드에 실패하였습니다.", Toast.LENGTH_SHORT).show()
        hideProgress()
    }

    private fun removePhoto(uri: Uri) {
        imageUriList.remove(uri)
        photoListAdapter.setPhotoList(imageUriList)
    }
}