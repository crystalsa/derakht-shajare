package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.ui.common.toFarsiNumbers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TreePdfExporter {

    suspend fun saveBitmapToPdf(
        context: Context,
        bitmap: Bitmap,
        groupName: String,
        personCount: Int = 0
    ): File = withContext(Dispatchers.IO) {
        AppLogger.i("PDF_EXPORT", "شروع ذخیره بیت‌مپ تک درخت به PDF تک صفحه‌ای. ابعاد: ${bitmap.width}x${bitmap.height}")

        if (bitmap.width <= 0 || bitmap.height <= 0) {
            val err = "ابعاد تصویر شجره‌نامه نامعتبر است (${bitmap.width}x${bitmap.height})"
            AppLogger.e("PDF_EXPORT", err)
            throw IllegalArgumentException(err)
        }

        val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            AppLogger.i("PDF_EXPORT", "تبدیل بیت‌مپ سخت‌افزاری به بیت‌مپ نرم‌افزاری ARGB_8888...")
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        } ?: throw IllegalStateException("خطا در کپی بیت‌مپ به حالت نرم‌افزاری")

        try {
            val dpi = 150f
            val maxPageDimension = 14400f // Ceiling for PDF page size in points (200 inches at 72dpi)

            val headerHeightPt = 54f
            val footerHeightPt = 22f
            val marginPt = 28f * 2f

            var bitmapWidthPt = softwareBitmap.width * 72f / dpi
            var bitmapHeightPt = softwareBitmap.height * 72f / dpi

            var requiredWidthPt = bitmapWidthPt + marginPt
            var requiredHeightPt = bitmapHeightPt + headerHeightPt + footerHeightPt

            if (requiredWidthPt > maxPageDimension || requiredHeightPt > maxPageDimension) {
                val scaleFactor = minOf(maxPageDimension / requiredWidthPt, maxPageDimension / requiredHeightPt)
                bitmapWidthPt *= scaleFactor
                bitmapHeightPt *= scaleFactor
                requiredWidthPt = bitmapWidthPt + marginPt
                requiredHeightPt = bitmapHeightPt + headerHeightPt + footerHeightPt
            }

            val pageWidth = requiredWidthPt.toInt().coerceAtLeast(200)
            val pageHeight = requiredHeightPt.toInt().coerceAtLeast(200)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(android.graphics.Color.WHITE)

            val greenColor = android.graphics.Color.rgb(46, 125, 50)
            val grayColor = android.graphics.Color.rgb(120, 120, 120)

            val topBarPaint = Paint().apply { color = greenColor }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, topBarPaint)

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 15f
                isFakeBoldText = true
                color = greenColor
                textAlign = Paint.Align.RIGHT
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = grayColor
                textAlign = Paint.Align.RIGHT
            }

            val footerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                color = grayColor
                textAlign = Paint.Align.CENTER
            }

            val rightMargin = pageWidth - 28f
            var topY = 28f

            canvas.drawText("نمودار شجره‌نامه: $groupName", rightMargin, topY, titlePaint)
            topY += 14f

            val currentDateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            val metaText = if (personCount > 0) {
                "تعداد اعضا: ${personCount.toString().toFarsiNumbers()} نفر • تاریخ تنظیم: ${currentDateStr.toFarsiNumbers()}"
            } else {
                "تاریخ تنظیم: ${currentDateStr.toFarsiNumbers()}"
            }
            canvas.drawText(metaText, rightMargin, topY, subtitlePaint)
            topY += 12f

            val linePaint = Paint().apply {
                color = android.graphics.Color.rgb(210, 230, 210)
                strokeWidth = 1f
            }
            canvas.drawLine(28f, topY, rightMargin, topY, linePaint)
            topY += 10f

            val availableWidth = pageWidth - 56f
            val footerHeight = 22f
            val availableHeight = pageHeight - topY - footerHeight - 8f

            val scale = minOf(availableWidth / softwareBitmap.width.toFloat(), availableHeight / softwareBitmap.height.toFloat()).coerceAtMost(1f)
            val drawWidth = softwareBitmap.width * scale
            val drawHeight = softwareBitmap.height * scale

            val drawX = 28f + (availableWidth - drawWidth) / 2f
            val drawY = topY + (availableHeight - drawHeight) / 2f

            val destRect = RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight)

            val bitmapPaint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }

            canvas.drawBitmap(softwareBitmap, null, destRect, bitmapPaint)

            canvas.drawText("صفحه ۱ از ۱ — گزارش تصویری شجره‌نامه $groupName", pageWidth / 2f, pageHeight - 10f, footerPaint)

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
            AppLogger.i("PDF_EXPORT", "فایل PDF تک صفحه درخت ایجاد شد: ${file.name}")
            file
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
            val chooser = Intent.createChooser(intent, "اشتراک‌گذاری / چاپ گزارش و پشتیبان PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            AppLogger.e("PDF_EXPORT", "خطا در اشتراک‌گذاری فایل PDF", e)
            throw e
        }
    }
}
