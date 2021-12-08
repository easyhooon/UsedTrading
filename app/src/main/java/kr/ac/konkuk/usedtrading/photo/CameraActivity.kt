package kr.ac.konkuk.usedtrading.photo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import kr.ac.konkuk.usedtrading.databinding.ActivityCameraBinding
import kr.ac.konkuk.usedtrading.extensions.loadCenterCrop
import kr.ac.konkuk.usedtrading.util.PathUtil
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    companion object {

        const val TAG = "CameraActivity"
        private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        // 후면 카메라
        private val LENS_FACING: Int = CameraSelector.LENS_FACING_BACK

        const val CONFIRM_IMAGE_REQUEST_CODE = 3000

        private const val URI_LIST_KEY = "uriList"

        fun newIntent(activity: Activity) = Intent(activity, CameraActivity::class.java)


    }

    private lateinit var binding: ActivityCameraBinding

    private lateinit var cameraExecutor: ExecutorService

    private val cameraMainExecutor by lazy { ContextCompat.getMainExecutor(this)}

    private val cameraProviderFuture by lazy { ProcessCameraProvider.getInstance(this)}

    private lateinit var imageCapture: ImageCapture

    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    // Camera Config
    private var displayId: Int = -1

    // lifecycle 관리를 위한
    private var camera: Camera? = null

    private var root: View? = null

    private var isCapturing: Boolean = false

    private var isFlashEnabled: Boolean = false

    // 상세화면 쪽에서 미리보기 이미지를 보기 위함
    private val uriList = mutableListOf<Uri>()

    // landScape 모드도 대응 해야 함
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (this@CameraActivity.displayId == displayId) {
                // 이미지를 캡처하고 있는 화면에 대한 회전값의 대한 설정(대응)
                if (::imageCapture.isInitialized && root != null) {
                    //잘못된 Rotation 의 대한 예외처리 포함
                    imageCapture.targetRotation =
                        root?.display?.rotation ?: ImageOutputConfig.INVALID_ROTATION
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        root = binding.root

        if(allPermissionsGranted()) {
            startCamera(binding.viewFinder)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera(viewFinder: PreviewView) {
        // Display 에 띄워줄 수 있는 Listener 를 하나 받아줌
        displayManager.registerDisplayListener(displayListener, null)

        cameraExecutor = Executors.newSingleThreadExecutor()

        viewFinder.postDelayed({
            displayId = viewFinder.display.displayId
            bindCameraUseCase()
        }, 10)
    }

    private fun bindCameraUseCase() = with(binding) {
        // 화면 회전 값 체크
        val rotation = viewFinder.display.rotation
        // 카메라 설정 후면
        val cameraSelector = CameraSelector.Builder().requireLensFacing(LENS_FACING).build()

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().apply{
                setTargetAspectRatio(AspectRatio.RATIO_4_3)
                setTargetRotation(rotation)
            }.build()

            // 카메라 이미지 캡처 수단
            // imageCapture Init
            val imageCaptureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                //Preview 와 동일한 비율
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)

            imageCapture = imageCaptureBuilder.build()

            try {
                cameraProvider.unbindAll() // 기존 바인딩 되어 있는 카메라는 해제
                camera = cameraProvider.bindToLifecycle(
                    this@CameraActivity, cameraSelector, preview, imageCapture
                )
                //화면 상에 맺힐 상 지정
                preview.setSurfaceProvider(viewFinder.surfaceProvider)
                bindCaptureListener()
                bindZoomListener()

                //플래시 기능이 존재하는지 확인
                initFlashAndAddListener()

                bindPreviewImageViewClickListener()
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }, cameraMainExecutor)
    }

    private fun initFlashAndAddListener() = with(binding) {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() ?: false

        flashSwitch.isGone = hasFlash.not()
        if (hasFlash) {
            flashSwitch.setOnCheckedChangeListener { _, isChecked ->
                isFlashEnabled = isChecked
            }
        } else {
            //쓰지 못하도록 막음
            isFlashEnabled = false
            flashSwitch.setOnCheckedChangeListener(null)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindZoomListener() = with(binding) {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {

                val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f

                // 얼마나 움직였는지 처리
                val delta = detector.scaleFactor
                camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)

                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(this@CameraActivity, listener)
        viewFinder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    private fun bindCaptureListener() = with(binding) {
        captureButton.setOnClickListener {
            if (isCapturing.not()) {
                //Capture 중 이지 않을 때만 호출
                isCapturing = true
                captureCamera()
            }
        }
    }

    private var contentUri: Uri? = null

    private fun captureCamera() {
        if (::imageCapture.isInitialized.not()) return
        val photoFile = File(
            //외장 디렉토리에 저장할 수 있도록 구현 setup
            PathUtil.getOutputDirectory(this),
            //파일명
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.KOREA
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        //플래시가 필요할때
        if (isFlashEnabled) {
            flashLight(true)
        }

        //콜백을  달아줌
        imageCapture.takePicture(outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                contentUri = savedUri
                updateSavedImageContent()
            }

            override fun onError(e : ImageCaptureException) {
                e.printStackTrace()
                isCapturing = false
                flashLight(false)
            }

        })
    }

    private fun flashLight(light: Boolean) {
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() ?: false
        if (hasFlash) {
            camera?.cameraControl?.enableTorch(light)
        }
    }

    private fun updateSavedImageContent() {
        contentUri?.let {
            isCapturing = try {
                // string path 를 File 에 넣음
                val file = File(PathUtil.getPath(this, it) ?: throw FileNotFoundException())
                // 파일을 외부에서도 볼 수 있게 처리
                MediaScannerConnection.scanFile(this, arrayOf(file.path), arrayOf("image/jpeg"), null)
                Handler(Looper.getMainLooper()).post {
                    //Main Thread 에서 이미지 처리
                    binding.previewImageView.loadCenterCrop(url = it.toString(), corner = 4f)
                }
                if(isFlashEnabled) {
                    flashLight(false)
                }
                uriList.add(it)
                // 미리보기 이미지 구성
                false

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun bindPreviewImageViewClickListener() = with(binding) {
        previewImageView.setOnClickListener {
            startActivityForResult(
                ImagePreviewListActivity.newIntent(this@CameraActivity, uriList),
                CONFIRM_IMAGE_REQUEST_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIRM_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            Toast.makeText(this, "카메라 권한이 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}