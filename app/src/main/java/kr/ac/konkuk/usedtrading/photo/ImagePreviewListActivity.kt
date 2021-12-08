package kr.ac.konkuk.usedtrading.photo

import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import kr.ac.konkuk.usedtrading.R
import kr.ac.konkuk.usedtrading.adapter.ImageViewPagerAdapter
import kr.ac.konkuk.usedtrading.databinding.ActivityImagePreviewListBinding
import kr.ac.konkuk.usedtrading.util.PathUtil
import java.io.File
import java.io.FileNotFoundException

class ImagePreviewListActivity : AppCompatActivity() {

    companion object {
        private const val URI_LIST_KEY = "uriList"

        fun newIntent(activity: Activity, uriList: List<Uri>) =
            Intent(activity, ImagePreviewListActivity::class.java).apply {
                putExtra(URI_LIST_KEY, ArrayList<Uri>().apply { uriList.forEach { add(it)} })
            }
    }

    private lateinit var binding: ActivityImagePreviewListBinding
    private lateinit var imageViewPagerAdapter: ImageViewPagerAdapter

    private val uriList by lazy<List<Uri>> { intent.getParcelableArrayListExtra(URI_LIST_KEY)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }
    private fun initViews() {
        //actionBar 로 toolbar 를 이용
        setSupportActionBar(binding.toolbar)
        setupImageList()
    }

    private fun setupImageList() = with(binding) {
        if(::imageViewPagerAdapter.isInitialized.not()) {
            imageViewPagerAdapter = ImageViewPagerAdapter(uriList)
        }
        imageViewPager.adapter = imageViewPagerAdapter

        //biding with indicator
        indicator.setViewPager(imageViewPager)
        imageViewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                toolbar.title = getString(R.string.image_page, position + 1, imageViewPagerAdapter.uriList.size)
            }
        })

        deleteButton.setOnClickListener {
            removeImage(uriList[imageViewPager.currentItem])
        }

        confirmButton.setOnClickListener {
            setResult(Activity.RESULT_OK, Intent().apply{
                putExtra(URI_LIST_KEY, ArrayList<Uri>(imageViewPagerAdapter.uriList))
            })
            finish()
        }
    }

    private fun removeImage(uri: Uri) {
        try {
            val file = File(PathUtil.getPath(this, uri) ?: throw FileNotFoundException())
            file.delete()
            imageViewPagerAdapter.uriList.let{
                val imageList = it.toMutableList()
                imageList.remove(uri)
                imageViewPagerAdapter.uriList = imageList
                imageViewPagerAdapter.notifyDataSetChanged()
            }
            //notify
            MediaScannerConnection.scanFile(this, arrayOf(file.path), arrayOf("image/jpeg"), null)
            //삭제할때마다 indicator 도 반영 (개수)
            binding.indicator.setViewPager(binding.imageViewPager)
            //남은 사진이 한장일 경우 삭제 후 액티비티 종료
            if(imageViewPagerAdapter.uriList.isEmpty()) {
                Toast.makeText(this, "삭제할 이미지가 없습니다", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "이미지가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}