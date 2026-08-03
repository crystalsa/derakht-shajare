package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object TreePdfExporter {

    suspend fun saveBitmapToPdf(
        context: Context,
        bitmap: Bitmap,
        groupName: String
    ): File {
        AppLogger.i("PDF_EXPORT", "شروع ذخیره بیت‌مپ به PDF. ابعاد: ${bitmap.width}x${bitmap.height}, کانفیگ: ${bitmap.config}")

        if (bitmap.width <= 0 || bitmap.height <= 0) {
            val err = "ابعاد تصویر شجره‌نامه نامعتبر است (${bitmap.width}x${bitmap.height})"
            AppLogger.e("PDF_EXPORT", err)
            throw IllegalArgumentException(err)
        }

        // Convert hardware bitmap to software bitmap (ARGB_8888) if necessary
        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            AppLogger.i("PDF_EXPORT", "تبدیل بیت‌مپ سخت‌افزاری به بیت‌مپ نرم‌افزاری ARGB_8888...")
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        } ?: throw IllegalStateException("خطا در کپی بیت‌مپ به حالت نرم‌افزاری")

        try {
            // High resolution PDF generation: 150 DPI scale factor
            val dpi = 150
            val pageWidthPt = maxOf((softwareBitmap.width * 72f / dpi).toInt(), 200)
            val pageHeightPt = maxOf((softwareBitmap.height * 72f / dpi).toInt(), 200)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Scale canvas to map bitmap dimensions into PDF page points
            val scaleX = pageWidthPt.toFloat() / softwareBitmap.width
            val scaleY = pageHeightPt.toFloat() / softwareBitmap.height
            canvas.scale(scaleX, scaleY)

            // Single drawBitmap call on PDF software canvas
            canvas.drawBitmap(softwareBitmap, 0f, 0f, null)
            document.finishPage(page)

            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < threshold) {
                    file.delete()
                }
            }

            val safeGroupName = groupName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_")
            val file = File(cacheDir, "derakht_${safeGroupName}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            AppLogger.i("PDF_EXPORT", "فایل PDF با موفقیت ایجاد شد: ${file.name} (${file.length()} بایت)")
            return file
        } finally {
            if (softwareBitmap != bitmap) {
                softwareBitmap.recycle()
            }
        }
    }

    fun shareTreePdf(context: Context, file: File) {
        try {
            AppLogger.i("PDF_EXPORT", "ارسال قصد اشتراک‌گذاری PDF: ${file.name}")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "اشتراک‌گذاری / چاپ شجره‌نامه").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            AppLogger.e("PDF_EXPORT", "خطا در اشتراک‌گذاری فایل PDF", e)
            throw e
        }
    }
}

