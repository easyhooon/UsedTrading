package kr.ac.konkuk.usedtrading.gallery

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.ac.konkuk.usedtrading.GlobalApplication.Companion.appContext

class GalleryViewModel: ViewModel() {

    private val galleryPhotoRepository by lazy { GalleryPhotoRepository(appContext!!) }

    // 상태 관리 용도
    private lateinit var photoList:MutableList<GalleryPhoto>

    val galleryStateLiveData = MutableLiveData<GalleryState>(GalleryState.Uninitialized)

    fun fetchData() = viewModelScope.launch {
        setState(
            GalleryState.Loading
        )
        photoList = galleryPhotoRepository.getAllPhoto()
        setState(
            GalleryState.Success(
                photoList
            )
        )
    }

    fun selectPhoto(galleryPhoto: GalleryPhoto) {
        val findGalleryPhoto = photoList.find { it.id == galleryPhoto.id }
        findGalleryPhoto?.let { photo ->
            photoList[photoList.indexOf(photo)] =
                photo.copy(
                    isSelected = photo.isSelected.not()
                )
            setState(
                GalleryState.Success(
                    photoList
                )
            )
        }
    }

    fun confirmCheckedPhotos() {
        // 리스트에서 해당 사진을 찾음
        setState(
            GalleryState.Loading
        )
        setState(
            GalleryState.Confirm(
                photoList = photoList.filter { it.isSelected }
            )
        )
    }

    private fun setState(state: GalleryState) {
        galleryStateLiveData.postValue(state)
    }

}