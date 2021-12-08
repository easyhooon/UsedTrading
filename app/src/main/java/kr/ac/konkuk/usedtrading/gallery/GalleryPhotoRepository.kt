package kr.ac.konkuk.usedtrading.gallery

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GalleryPhotoRepository(
    private val context: Context
) {
    suspend fun getAllPhoto(): MutableList<GalleryPhoto> = withContext(Dispatchers.IO) {
        val galleryPhotoList = mutableListOf<GalleryPhoto>()
        val uriExternal: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        //query 를 통해서 indexing(색인) 하나하나 꺼낼때마다 데이터의 정보를 알 수 있도록
        val query: Cursor?
        val projection = arrayOf(
            MediaStore.Images.ImageColumns.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.SIZE,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns._ID
        )

        // 가져온 데이터를 content resolver 를 통해 꺼내줌
        val resolver = context.contentResolver
        query = resolver?.query(uriExternal, projection, null, null,"${MediaStore.Images.ImageColumns.DATE_ADDED} DESC")
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)

            while(cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val size = cursor.getInt(sizeColumn)
                val date = cursor.getString(dateColumn)

                // 외장 메모리 쪽에 접근해 id 와 uri를 조합해서 추가
                val contentUri = ContentUris.withAppendedId(uriExternal, id)

                galleryPhotoList.add(
                    GalleryPhoto(
                        id,
                        uri = contentUri,
                        name = name,
                        size = size,
                        date = date ?: ""
                    )
                )
            }
        }

        galleryPhotoList
    }
}