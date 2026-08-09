package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.FamilyFolder
import com.example.data.FamilyGroup
import com.example.data.Person
import com.example.data.Relationship
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
            val isLandscape = softwareBitmap.width > softwareBitmap.height
            val pageWidth = if (isLandscape) 842 else 595 // A4 standard points
            val pageHeight = if (isLandscape) 595 else 842

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

            val scale = minOf(availableWidth / softwareBitmap.width.toFloat(), availableHeight / softwareBitmap.height.toFloat())
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

    suspend fun generateComprehensivePdf(
        context: Context,
        allFolders: List<FamilyFolder>,
        allGroups: List<FamilyGroup>,
        allPersons: List<Person>,
        allRelationships: List<Relationship>,
        groupBitmaps: Map<Long, Bitmap>
    ): File = withContext(Dispatchers.IO) {
        AppLogger.i("PDF_EXPORT", "شروع تولید فایل PDF پشتیبان جامع (شامل تمام پوشه‌ها، درخت‌ها و اعضا)...")
        val document = PdfDocument()
        var pageCounter = 1

        val pageWidth = 595 // A4 portrait width in points
        val pageHeight = 842 // A4 portrait height in points
        val margin = 36f
        val rightMargin = pageWidth - margin

        val greenColor = android.graphics.Color.rgb(46, 125, 50)
        val darkColor = android.graphics.Color.rgb(30, 30, 30)
        val grayColor = android.graphics.Color.rgb(110, 110, 110)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            isFakeBoldText = true
            color = greenColor
            textAlign = Paint.Align.RIGHT
        }

        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            color = grayColor
            textAlign = Paint.Align.RIGHT
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
            color = darkColor
            textAlign = Paint.Align.RIGHT
        }

        val boldPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            isFakeBoldText = true
            color = darkColor
            textAlign = Paint.Align.RIGHT
        }

        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            color = grayColor
            textAlign = Paint.Align.CENTER
        }

        val topBarPaint = Paint().apply {
            color = greenColor
        }

        val boxBgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.rgb(248, 250, 248)
        }

        val boxBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = android.graphics.Color.rgb(200, 225, 200)
        }

        val linePaint = Paint().apply {
            color = android.graphics.Color.rgb(220, 220, 220)
            strokeWidth = 0.8f
        }

        // ================= PAGE 1: COVER & OVERALL DIRECTORY =================
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCounter++).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas

        canvas1.drawRect(0f, 0f, pageWidth.toFloat(), 16f, topBarPaint)

        var y = 50f
        canvas1.drawText("گزارش جامع و پشتیبان تصویری شجره‌نامه", rightMargin, y, titlePaint)
        y += 20f
        val currentDateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date())
        canvas1.drawText("پشتیبان تمام پوشه‌ها، درخت‌های فامیلی و اعضا • $currentDateStr", rightMargin, y, subtitlePaint)
        y += 24f

        canvas1.drawLine(margin, y, rightMargin, y, linePaint)
        y += 18f

        // Stats summary box
        val boxRect = RectF(margin, y, rightMargin, y + 85f)
        canvas1.drawRoundRect(boxRect, 10f, 10f, boxBgPaint)
        canvas1.drawRoundRect(boxRect, 10f, 10f, boxBorderPaint)

        val boxY = y + 22f
        canvas1.drawText("خلاصه آمار و اطلاعات کلی سیستم:", rightMargin - 14f, boxY, boldPaint)

        val col1X = rightMargin - 20f
        val col2X = rightMargin - 260f

        canvas1.drawText("• تعداد کل پوشه‌ها: ${allFolders.size.toString().toFarsiNumbers()}", col1X, boxY + 22f, textPaint)
        canvas1.drawText("• تعداد گروه‌ها (درخت‌ها): ${allGroups.size.toString().toFarsiNumbers()}", col1X, boxY + 42f, textPaint)

        canvas1.drawText("• کل اعضای ثبت‌شده: ${allPersons.size.toString().toFarsiNumbers()}", col2X, boxY + 22f, textPaint)
        canvas1.drawText("• تعداد کل روابط: ${allRelationships.size.toString().toFarsiNumbers()}", col2X, boxY + 42f, textPaint)

        y += 105f

        canvas1.drawText("فهرست و ساختار تمام پوشه‌ها و گروه‌ها:", rightMargin, y, boldPaint)
        y += 20f

        val rootFolders = allFolders.filter { it.parentId == null }
        val rootGroups = allGroups.filter { it.folderId == null }

        fun drawFolderTree(folder: FamilyFolder, indentLevel: Int) {
            if (y > pageHeight - 60f) return
            val indent = indentLevel * 16f
            canvas1.drawText("📁 ${folder.name}", rightMargin - indent, y, boldPaint)
            y += 18f

            val subfolders = allFolders.filter { it.parentId == folder.id }
            val groupsInF = allGroups.filter { it.folderId == folder.id }

            for (g in groupsInF) {
                if (y > pageHeight - 60f) return
                val pCount = allPersons.count { it.groupId == g.id }
                canvas1.drawText("🌳 ${g.name} (${pCount.toString().toFarsiNumbers()} عضو)", rightMargin - indent - 16f, y, textPaint)
                y += 16f
            }

            for (sub in subfolders) {
                drawFolderTree(sub, indentLevel + 1)
            }
        }

        for (rf in rootFolders) {
            drawFolderTree(rf, 0)
        }

        if (rootGroups.isNotEmpty() && y < pageHeight - 70f) {
            canvas1.drawText("گروه‌های بدون پوشه (صفحه اصلی):", rightMargin, y, boldPaint)
            y += 18f
            for (rg in rootGroups) {
                if (y > pageHeight - 60f) break
                val pCount = allPersons.count { it.groupId == rg.id }
                canvas1.drawText("🌳 ${rg.name} (${pCount.toString().toFarsiNumbers()} عضو)", rightMargin - 16f, y, textPaint)
                y += 16f
            }
        }

        canvas1.drawText("صفحه ۱ - پشتیبان کامل شجره‌نامه", pageWidth / 2f, pageHeight - 20f, footerPaint)
        document.finishPage(page1)

        // ================= PAGES FOR EACH FAMILY GROUP / TREE =================
        val groupsToExport = allGroups.ifEmpty {
            listOf(FamilyGroup(id = 0, name = "خاندان عمومی"))
        }

        for (group in groupsToExport) {
            val personsInGroup = allPersons.filter { it.groupId == group.id }

            // 1. Group Info & Members List Page
            val pageInfoG = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCounter++).create()
            val pageG = document.startPage(pageInfoG)
            val canvasG = pageG.canvas

            canvasG.drawRect(0f, 0f, pageWidth.toFloat(), 16f, topBarPaint)
            var gy = 48f

            val groupHeader = "گروه / درخت فامیلی: ${group.name}"
            canvasG.drawText(groupHeader, rightMargin, gy, titlePaint)
            gy += 20f

            val folderName = allFolders.find { it.id == group.folderId }?.name ?: "صفحه اصلی"
            canvasG.drawText("مکان: پوشه «$folderName» • تعداد اعضا: ${personsInGroup.size.toString().toFarsiNumbers()} نفر", rightMargin, gy, subtitlePaint)
            gy += 22f

            if (!group.description.isNullOrBlank()) {
                canvasG.drawText("توضیحات: ${group.description}", rightMargin, gy, textPaint)
                gy += 20f
            }

            canvasG.drawLine(margin, gy, rightMargin, gy, linePaint)
            gy += 18f

            canvasG.drawText("اسامی و مشخصات اعضای این گروه:", rightMargin, gy, boldPaint)
            gy += 20f

            for ((idx, p) in personsInGroup.withIndex()) {
                if (gy > pageHeight - 50f) {
                    canvasG.drawText("صفحه ${pageCounter - 1} - $groupHeader", pageWidth / 2f, pageHeight - 20f, footerPaint)
                    document.finishPage(pageG)

                    // Start continuation page
                    val contPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCounter++).create()
                    val contPage = document.startPage(contPageInfo)
                    val contCanvas = contPage.canvas
                    contCanvas.drawRect(0f, 0f, pageWidth.toFloat(), 16f, topBarPaint)
                    gy = 48f
                    contCanvas.drawText("ادامه اعضای $groupHeader", rightMargin, gy, boldPaint)
                    gy += 22f
                }

                val itemNum = (idx + 1).toString().toFarsiNumbers()
                val fullName = "${p.firstName} ${p.lastName}".trim()
                val genderStr = if (p.gender == "FEMALE") "ماده/مونث" else "نر/مذکر"
                val genStr = "نسل ${p.generation.toString().toFarsiNumbers()}"
                val birthStr = if (!p.birthDate.isNullOrBlank()) "متولد: ${p.birthDate}" else ""
                val deathStr = if (p.isDeceased) " (متوفی)" else ""

                val lineText = "$itemNum. $fullName - $genStr $birthStr$deathStr"
                canvasG.drawText(lineText, rightMargin - 10f, gy, textPaint)
                gy += 18f
            }

            canvasG.drawText("صفحه ${pageCounter - 1} - $groupHeader", pageWidth / 2f, pageHeight - 20f, footerPaint)
            document.finishPage(pageG)

            // 2. Visual Tree Diagram Page (Landscape A4)
            val bitmap = groupBitmaps[group.id]
            if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
                val landWidth = 842
                val landHeight = 595
                val pageInfoTree = PdfDocument.PageInfo.Builder(landWidth, landHeight, pageCounter++).create()
                val pageTree = document.startPage(pageInfoTree)
                val canvasTree = pageTree.canvas

                val treeHeaderPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 14f
                    isFakeBoldText = true
                    color = greenColor
                    textAlign = Paint.Align.RIGHT
                }
                canvasTree.drawText("نمودار تصویری شجره‌نامه: ${group.name}", landWidth - 30f, 30f, treeHeaderPaint)

                val availW = landWidth - 60f
                val availH = landHeight - 70f
                val scale = minOf(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())

                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale
                val drawX = (landWidth - drawW) / 2f
                val drawY = 40f + (availH - drawH) / 2f

                val destRect = RectF(drawX, drawY, drawX + drawW, drawY + drawH)

                val swBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else bitmap

                if (swBitmap != null) {
                    canvasTree.drawBitmap(swBitmap, null, destRect, null)
                    if (swBitmap != bitmap) swBitmap.recycle()
                }

                canvasTree.drawText("صفحه ${pageCounter - 1} - نمودار شجره‌نامه ${group.name}", landWidth / 2f, landHeight - 15f, footerPaint)
                document.finishPage(pageTree)
            }
        }

        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        cacheDir.listFiles()?.forEach { file ->
            if (file.lastModified() < threshold) {
                file.delete()
            }
        }

        val file = File(cacheDir, "derakht_koli_backup_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        AppLogger.i("PDF_EXPORT", "فایل PDF جامع شجره‌نامه با موفقیت ایجاد شد: ${file.name}")
        file
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
