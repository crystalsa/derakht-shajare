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
        personCount: Int = 0,
        reportSummary: FamilyReportSummary? = null
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
            val maxPageDimension = 14400f // Ceiling for PDF page size in points (200 inches at 72dpi)
            val headerHeightPt = 54f
            val footerHeightPt = 22f
            val sidePaddingPt = 16f

            // Dynamic page size based directly on the tree bitmap (without 10% extra):
            var treeWidthPt = softwareBitmap.width.toFloat()
            var treeHeightPt = softwareBitmap.height.toFloat()

            val headerAndFooterPt = headerHeightPt + footerHeightPt

            var requiredWidthPt = treeWidthPt + (sidePaddingPt * 2f)
            var requiredHeightPt = treeHeightPt + headerAndFooterPt

            // Ensure page is large enough if comprehensive report card is present
            if (reportSummary != null && reportSummary.totalCount > 0) {
                requiredWidthPt = maxOf(requiredWidthPt, 480f)
                requiredHeightPt = maxOf(requiredHeightPt, 420f)
            }

            if (requiredWidthPt > maxPageDimension || requiredHeightPt > maxPageDimension) {
                val scaleFactor = minOf(maxPageDimension / requiredWidthPt, maxPageDimension / requiredHeightPt)
                requiredWidthPt *= scaleFactor
                requiredHeightPt *= scaleFactor
            }

            val pageWidth = requiredWidthPt.toInt().coerceAtLeast(250)
            val pageHeight = requiredHeightPt.toInt().coerceAtLeast(250)

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

            val leftMargin = sidePaddingPt
            val rightMargin = pageWidth - leftMargin
            var topY = 24f

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
            canvas.drawLine(leftMargin, topY, rightMargin, topY, linePaint)
            topY += 10f

            val availableWidth = pageWidth - (leftMargin * 2f)
            val footerHeight = 22f
            val availableHeight = pageHeight - topY - footerHeight - 8f

            val scale = minOf(
                availableWidth / softwareBitmap.width.toFloat(),
                availableHeight / softwareBitmap.height.toFloat()
            )
            val drawWidth = softwareBitmap.width * scale
            val drawHeight = softwareBitmap.height * scale

            val drawX = leftMargin + (availableWidth - drawWidth) / 2f
            val drawY = topY + (availableHeight - drawHeight) / 2f

            val destRect = RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight)

            val bitmapPaint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }

            canvas.drawBitmap(softwareBitmap, null, destRect, bitmapPaint)

            if (reportSummary != null && reportSummary.totalCount > 0) {
                drawComprehensiveReportCard(
                    canvas = canvas,
                    stats = reportSummary,
                    groupName = groupName,
                    pageWidth = pageWidth.toFloat(),
                    pageHeight = pageHeight.toFloat(),
                    footerHeight = footerHeightPt,
                    leftMargin = leftMargin
                )
            }

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

    private fun drawComprehensiveReportCard(
        canvas: android.graphics.Canvas,
        stats: FamilyReportSummary,
        groupName: String,
        pageWidth: Float,
        pageHeight: Float,
        footerHeight: Float,
        leftMargin: Float = 28f
    ) {
        data class ReportItem(
            val title: String,
            val value: String,
            val icon: String
        )

        val items = mutableListOf<ReportItem>()

        if (stats.totalCount > 0) {
            items.add(ReportItem(
                title = "تعداد کل اعضای خانواده",
                value = "${stats.totalCount.toString().toFarsiNumbers()} نفر",
                icon = "👥"
            ))
        }
        if (stats.maleCount > 0) {
            items.add(ReportItem(
                title = "تعداد اعضای مذکر (آقایان)",
                value = "${stats.maleCount.toString().toFarsiNumbers()} نفر",
                icon = "👨"
            ))
        }
        if (stats.femaleCount > 0) {
            items.add(ReportItem(
                title = "تعداد اعضای مونث (بانوان)",
                value = "${stats.femaleCount.toString().toFarsiNumbers()} نفر",
                icon = "👩"
            ))
        }
        if (stats.totalGenerations > 0) {
            items.add(ReportItem(
                title = "تعداد نسل‌ها در شجره‌نامه",
                value = "${stats.totalGenerations.toString().toFarsiNumbers()} نسل",
                icon = "🧬"
            ))
        }
        if (stats.bridesCount > 0) {
            items.add(ReportItem(
                title = "تعداد عروس‌های خانواده",
                value = "${stats.bridesCount.toString().toFarsiNumbers()} نفر",
                icon = "👰"
            ))
        }
        if (stats.groomsCount > 0) {
            items.add(ReportItem(
                title = "تعداد دامادهای خانواده",
                value = "${stats.groomsCount.toString().toFarsiNumbers()} نفر",
                icon = "🤵"
            ))
        }
        if (stats.childrenCount > 0) {
            items.add(ReportItem(
                title = "تعداد فرزندان (نسل اول)",
                value = "${stats.childrenCount.toString().toFarsiNumbers()} نفر",
                icon = "👶"
            ))
        }
        if (stats.grandchildrenCount > 0) {
            items.add(ReportItem(
                title = "تعداد نوه‌ها (نسل دوم)",
                value = "${stats.grandchildrenCount.toString().toFarsiNumbers()} نفر",
                icon = "🪁"
            ))
        }
        if (stats.greatGrandchildrenCount > 0) {
            items.add(ReportItem(
                title = "تعداد نبیره‌ها (نسل سوم)",
                value = "${stats.greatGrandchildrenCount.toString().toFarsiNumbers()} نفر",
                icon = "🧸"
            ))
        }
        if (stats.greatGreatGrandchildrenCount > 0) {
            items.add(ReportItem(
                title = "تعداد ندیده‌ها (نسل چهارم)",
                value = "${stats.greatGreatGrandchildrenCount.toString().toFarsiNumbers()} نفر",
                icon = "🐣"
            ))
        }
        if (stats.livingCount > 0) {
            items.add(ReportItem(
                title = "اعضای در قید حیات",
                value = "${stats.livingCount.toString().toFarsiNumbers()} نفر",
                icon = "🌱"
            ))
        }
        if (stats.deceasedCount > 0) {
            items.add(ReportItem(
                title = "تعداد درگذشتگان (آسمانی شده)",
                value = "${stats.deceasedCount.toString().toFarsiNumbers()} نفر",
                icon = "🕯️"
            ))
        }
        if (stats.averageAge != null && stats.averageAge > 0) {
            items.add(ReportItem(
                title = "میانگین سن اعضای خانواده",
                value = "${stats.averageAge.toString().toFarsiNumbers()} سال",
                icon = "📅"
            ))
        }
        if (!stats.topBirthPlace.isNullOrBlank()) {
            items.add(ReportItem(
                title = "زادگاه اصلی و غالب خاندان",
                value = stats.topBirthPlace,
                icon = "🏡"
            ))
        }
        if (!stats.topOccupation.isNullOrBlank()) {
            items.add(ReportItem(
                title = "پیشه و مشاغل غالب خاندان",
                value = stats.topOccupation,
                icon = "💼"
            ))
        }

        if (items.isEmpty()) return

        // 2x Dimensions (approx 450pt width, double the previous 220pt)
        val maxAvailableCardWidth = pageWidth - (leftMargin * 2f)
        val cardWidth = minOf(450f, maxAvailableCardWidth * 0.50f).coerceAtLeast(minOf(360f, maxAvailableCardWidth))
        val headerHeight = 44f
        val itemHeight = 31f
        val itemGap = 7f
        val outerPadding = 16f
        val dividerHeight = 12f

        val totalContentHeight = headerHeight + dividerHeight + (items.size * (itemHeight + itemGap)) + outerPadding
        val cardHeight = totalContentHeight

        // Bottom-left corner placement
        val cardLeft = leftMargin
        val cardRight = cardLeft + cardWidth
        val cardBottom = pageHeight - footerHeight - 12f
        val cardTop = cardBottom - cardHeight

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val cornerRadius = 18f

        // 1. Soft Drop Shadows (Multi-layered for realism)
        val shadowPaintLarge = Paint().apply {
            color = android.graphics.Color.argb(24, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(cardLeft + 3f, cardTop + 4f, cardRight + 3f, cardBottom + 4f),
            cornerRadius,
            cornerRadius,
            shadowPaintLarge
        )
        val shadowPaintSmall = Paint().apply {
            color = android.graphics.Color.argb(16, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(cardLeft + 1.5f, cardTop + 2f, cardRight + 1.5f, cardBottom + 2f),
            cornerRadius,
            cornerRadius,
            shadowPaintSmall
        )

        // 2. Card Background (Pure White matching app dialog)
        val cardBgPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardBgPaint)

        // 3. Card Border (2dp matching app: .border(2.dp, accentColor, RoundedCornerShape(24.dp)))
        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50) // Emerald green accent
            strokeWidth = 2.4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 4. Header Section: Title & Bar Chart Icon
        val titleTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 15.5f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(30, 41, 59) // App textColor slate-900
            textAlign = Paint.Align.RIGHT
        }
        val headerTitle = "گزارش جامع $groupName"
        val headerY = cardTop + 28f
        val headerTextRight = cardRight - outerPadding - 24f
        canvas.drawText(headerTitle, headerTextRight, headerY, titleTextPaint)

        // Draw Chart Icon representation (3 ascending bars) next to title
        val chartPaint = Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val iconRight = cardRight - outerPadding
        val iconBottom = headerY
        canvas.drawRoundRect(RectF(iconRight - 4f, iconBottom - 16f, iconRight, iconBottom), 1.5f, 1.5f, chartPaint)
        canvas.drawRoundRect(RectF(iconRight - 10f, iconBottom - 11f, iconRight - 6f, iconBottom), 1.5f, 1.5f, chartPaint)
        canvas.drawRoundRect(RectF(iconRight - 16f, iconBottom - 7f, iconRight - 12f, iconBottom), 1.5f, 1.5f, chartPaint)

        // Subtle close indicator dot or accent pill on top left of header
        val headerPillPaint = Paint().apply {
            color = android.graphics.Color.rgb(235, 245, 235)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cardLeft + outerPadding + 6f, cardTop + 23f, 7f, headerPillPaint)
        val headerPillDotPaint = Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cardLeft + outerPadding + 6f, cardTop + 23f, 3.5f, headerPillDotPaint)

        // 5. Horizontal Divider matching app dialog: HorizontalDivider(color = textColor.copy(alpha = 0.1f))
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.argb(35, 100, 116, 139)
            strokeWidth = 1f
        }
        val dividerY = cardTop + headerHeight + 2f
        canvas.drawLine(cardLeft + outerPadding, dividerY, cardRight - outerPadding, dividerY, dividerPaint)

        // 6. StatRowItems (Each in a rounded card with containerColor = Color(0xFFF8FAF8))
        val rowBgPaint = Paint().apply {
            color = android.graphics.Color.rgb(248, 250, 248) // 0xFFF8FAF8
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val rowBorderPaint = Paint().apply {
            color = android.graphics.Color.argb(25, 46, 125, 50)
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val emojiPaint = Paint().apply {
            isAntiAlias = true
            textSize = 14f
            textAlign = Paint.Align.RIGHT
        }
        val itemTitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            color = android.graphics.Color.rgb(71, 85, 105) // Slate-600 (textColor.copy(alpha = 0.8f))
            textAlign = Paint.Align.RIGHT
        }
        val itemValuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(46, 125, 50) // accentColor
            textAlign = Paint.Align.LEFT
        }

        val rowLeft = cardLeft + outerPadding
        val rowRight = cardRight - outerPadding
        val rowRadius = 8f
        var currentY = dividerY + 8f

        for (item in items) {
            val itemRect = RectF(rowLeft, currentY, rowRight, currentY + itemHeight)
            
            // Draw background card for row
            canvas.drawRoundRect(itemRect, rowRadius, rowRadius, rowBgPaint)
            canvas.drawRoundRect(itemRect, rowRadius, rowRadius, rowBorderPaint)

            val textBaseline = currentY + (itemHeight / 2f) + 4.2f

            // Right side: Emoji + Title
            val emojiX = rowRight - 10f
            canvas.drawText(item.icon, emojiX, textBaseline, emojiPaint)

            val titleX = emojiX - 22f
            val valueX = rowLeft + 10f
            
            // Measure value width so title does not collide
            val valueStr = item.value
            val valueWidth = itemValuePaint.measureText(valueStr)
            val maxTitleWidth = (titleX - (valueX + valueWidth + 12f)).coerceAtLeast(50f)

            var displayTitle = item.title
            if (itemTitlePaint.measureText(displayTitle) > maxTitleWidth) {
                while (displayTitle.length > 4 && itemTitlePaint.measureText("$displayTitle…") > maxTitleWidth) {
                    displayTitle = displayTitle.dropLast(1)
                }
                displayTitle = "$displayTitle…"
            }

            canvas.drawText(displayTitle, titleX, textBaseline, itemTitlePaint)

            // Left side: Value (bold, accentColor, LTR aligned on left)
            canvas.drawText(valueStr, valueX, textBaseline, itemValuePaint)

            currentY += itemHeight + itemGap
        }
    }
}

