package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Person
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PersonProfileExporter {

    suspend fun savePersonProfileImage(context: Context, bitmap: Bitmap, person: Person): Uri? = withContext(Dispatchers.IO) {
        try {
            AppLogger.i("PROFILE_EXPORT", "ذخیره عکس پروفایل برای: ${person.fullName}")
            val safeName = person.fullName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_")
            val filename = "profile_${safeName}_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/درختشجره")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                AppLogger.i("PROFILE_EXPORT", "عکس پروفایل با موفقیت در گالری ذخیره شد: $uri")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "عکس پروفایل در گالری ذخیره شد", Toast.LENGTH_SHORT).show()
                }
                uri
            } else {
                AppLogger.e("PROFILE_EXPORT", "امکان ایجاد URI در MediaStore وجود نداشت")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در ذخیره تصویر در گالری", Toast.LENGTH_SHORT).show()
                }
                null
            }
        } catch (e: Exception) {
            AppLogger.e("PROFILE_EXPORT", "خطا در ذخیره تصویر پروفایل", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "خطا در ذخیره تصویر پروفایل", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }

    suspend fun sharePersonProfileImage(context: Context, bitmap: Bitmap, person: Person) = withContext(Dispatchers.IO) {
        try {
            AppLogger.i("PROFILE_EXPORT", "اشتراک‌گذاری عکس پروفایل برای: ${person.fullName}")
            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeName = person.fullName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_")
            val file = File(cacheDir, "profile_${safeName}_${System.currentTimeMillis()}.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "اشتراک‌گذاری پروفایل ${person.fullName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            AppLogger.e("PROFILE_EXPORT", "خطا در اشتراک‌گذاری عکس پروفایل", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "خطا در اشتراک‌گذاری تصویر", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
