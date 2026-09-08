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

        val trimmedBitmap = trimWhiteBorders(softwareBitmap)

        try {
            val maxPageDimension = 14400f // Ceiling for PDF page size in points (200 inches at 72dpi)

            // Dynamic page size based directly on the trimmed tree bitmap:
            var treeWidthPt = trimmedBitmap.width.toFloat()
            var treeHeightPt = trimmedBitmap.height.toFloat()

            // Tree scale boost: Ensure tree nodes are large, bold, and easily readable in PDF
            val minDesiredTreeWidth = if (reportSummary != null && reportSummary.totalCount > 0) 860f else 660f
            val minDesiredTreeHeight = if (reportSummary != null && reportSummary.totalCount > 0) 660f else 480f

            val boostX = if (treeWidthPt < minDesiredTreeWidth) minDesiredTreeWidth / treeWidthPt else 1.0f
            val boostY = if (treeHeightPt < minDesiredTreeHeight) minDesiredTreeHeight / treeHeightPt else 1.0f
            val treeScaleBoost = maxOf(boostX, boostY).coerceIn(1.0f, 2.5f)

            val scaledTreeWidth = treeWidthPt * treeScaleBoost
            val scaledTreeHeight = treeHeightPt * treeScaleBoost

            val sidePaddingPt = maxOf(32f, scaledTreeWidth * 0.035f)
            val headerHeightPt = 60f
            val footerHeightPt = 26f

            var requiredWidthPt = scaledTreeWidth + (sidePaddingPt * 2f)
            var requiredHeightPt = scaledTreeHeight + headerHeightPt + footerHeightPt + 24f

            // Ensure page dimensions comfortably accommodate both the tree and the report card
            if (reportSummary != null && reportSummary.totalCount > 0) {
                requiredWidthPt = maxOf(requiredWidthPt + 70f, 960f)
                requiredHeightPt = maxOf(requiredHeightPt + 50f, 780f)
            }

            if (requiredWidthPt > maxPageDimension || requiredHeightPt > maxPageDimension) {
                val scaleFactor = minOf(maxPageDimension / requiredWidthPt, maxPageDimension / requiredHeightPt)
                requiredWidthPt *= scaleFactor
                requiredHeightPt *= scaleFactor
            }

            val pageWidth = requiredWidthPt.toInt().coerceAtLeast(300)
            val pageHeight = requiredHeightPt.toInt().coerceAtLeast(300)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(android.graphics.Color.WHITE)

            val greenColor = android.graphics.Color.rgb(46, 125, 50)
            val grayColor = android.graphics.Color.rgb(120, 120, 120)

            val topBarPaint = Paint().apply { color = greenColor }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, topBarPaint)

            val dynamicTitleSize = (pageWidth * 0.016f).coerceIn(15f, 22f)
            val dynamicSubtitleSize = (pageWidth * 0.010f).coerceIn(9.5f, 14f)
            val dynamicFooterSize = (pageWidth * 0.009f).coerceIn(8.5f, 12f)

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = dynamicTitleSize
                isFakeBoldText = true
                color = greenColor
                textAlign = Paint.Align.RIGHT
            }

            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                textSize = dynamicSubtitleSize
                color = grayColor
                textAlign = Paint.Align.RIGHT
            }

            val footerPaint = Paint().apply {
                isAntiAlias = true
                textSize = dynamicFooterSize
                color = grayColor
                textAlign = Paint.Align.CENTER
            }

            val leftMargin = sidePaddingPt
            val rightMargin = pageWidth - leftMargin
            var topY = 26f

            canvas.drawText("نمودار شجره‌نامه: $groupName", rightMargin, topY, titlePaint)
            topY += dynamicTitleSize * 1.05f

            val currentDateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            val metaText = if (personCount > 0) {
                "تعداد اعضا: ${personCount.toString().toFarsiNumbers()} نفر • تاریخ تنظیم: ${currentDateStr.toFarsiNumbers()}"
            } else {
                "تاریخ تنظیم: ${currentDateStr.toFarsiNumbers()}"
            }
            canvas.drawText(metaText, rightMargin, topY, subtitlePaint)
            topY += dynamicSubtitleSize * 1.15f

            val linePaint = Paint().apply {
                color = android.graphics.Color.rgb(210, 230, 210)
                strokeWidth = 1f
            }
            canvas.drawLine(leftMargin, topY, rightMargin, topY, linePaint)
            topY += 12f

            val availableWidth = pageWidth - (leftMargin * 2f)
            val availableHeight = pageHeight - topY - footerHeightPt - 16f

            val scale = minOf(
                availableWidth / trimmedBitmap.width.toFloat(),
                availableHeight / trimmedBitmap.height.toFloat()
            )
            val drawWidth = trimmedBitmap.width * scale
            val drawHeight = trimmedBitmap.height * scale

            val drawX = leftMargin + (availableWidth - drawWidth) / 2f
            val drawY = topY + (availableHeight - drawHeight) / 2f

            val destRect = RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight)

            val bitmapPaint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }

            canvas.drawBitmap(trimmedBitmap, null, destRect, bitmapPaint)

            if (reportSummary != null && reportSummary.totalCount > 0) {
                drawComprehensiveReportCard(
                    canvas = canvas,
                    stats = reportSummary,
                    groupName = groupName,
                    pageWidth = pageWidth.toFloat(),
                    pageHeight = pageHeight.toFloat(),
                    headerBottomY = topY,
                    footerHeight = footerHeightPt,
                    leftMargin = leftMargin,
                    treeBitmap = trimmedBitmap,
                    treeDestRect = destRect
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
            if (trimmedBitmap != softwareBitmap && trimmedBitmap != bitmap) {
                trimmedBitmap.recycle()
            }
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

    private fun isContentPixel(pixel: Int): Boolean {
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha < 30) return false
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        return r < 246 || g < 246 || b < 246
    }

    private fun trimWhiteBorders(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 20 || height <= 20) return bitmap

        var top = 0
        var bottom = height - 1
        var left = 0
        var right = width - 1

        val rowBuffer = IntArray(width)

        // Find top bound
        topLoop@ while (top < height) {
            bitmap.getPixels(rowBuffer, 0, width, 0, top, width, 1)
            for (x in 0 until width) {
                if (isContentPixel(rowBuffer[x])) break@topLoop
            }
            top++
        }

        if (top >= height) return bitmap // Entire bitmap is blank

        // Find bottom bound
        bottomLoop@ while (bottom > top) {
            bitmap.getPixels(rowBuffer, 0, width, 0, bottom, width, 1)
            for (x in 0 until width) {
                if (isContentPixel(rowBuffer[x])) break@bottomLoop
            }
            bottom--
        }

        // Find left & right bounds
        val colHeight = bottom - top + 1
        val colBuffer = IntArray(colHeight)

        leftLoop@ while (left < width) {
            bitmap.getPixels(colBuffer, 0, 1, left, top, 1, colHeight)
            for (y in 0 until colHeight) {
                if (isContentPixel(colBuffer[y])) break@leftLoop
            }
            left++
        }

        rightLoop@ while (right > left) {
            bitmap.getPixels(colBuffer, 0, 1, right, top, 1, colHeight)
            for (y in 0 until colHeight) {
                if (isContentPixel(colBuffer[y])) break@rightLoop
            }
            right--
        }

        // Add 24px safety padding around content
        val pad = 24
        val safeLeft = (left - pad).coerceAtLeast(0)
        val safeTop = (top - pad).coerceAtLeast(0)
        val safeRight = (right + pad).coerceAtMost(width - 1)
        val safeBottom = (bottom + pad).coerceAtMost(height - 1)

        val cropWidth = safeRight - safeLeft + 1
        val cropHeight = safeBottom - safeTop + 1

        if (cropWidth <= 0 || cropHeight <= 0 || (cropWidth == width && cropHeight == height)) {
            return bitmap
        }

        return try {
            Bitmap.createBitmap(bitmap, safeLeft, safeTop, cropWidth, cropHeight)
        } catch (e: Throwable) {
            AppLogger.e("PDF_EXPORT", "خطا در برش حاشیه سفید تصویر: ${e.message}")
            bitmap
        }
    }

    private fun calculateOverlapScore(
        cardRect: RectF,
        treeBitmap: Bitmap,
        treeDestRect: RectF
    ): Int {
        val intersectRect = RectF()
        if (!intersectRect.setIntersect(cardRect, treeDestRect)) {
            return 0 // Completely outside the tree bounding box
        }

        val drawWidth = treeDestRect.width()
        val drawHeight = treeDestRect.height()
        if (drawWidth <= 0f || drawHeight <= 0f) return 0

        val scaleX = treeBitmap.width.toFloat() / drawWidth
        val scaleY = treeBitmap.height.toFloat() / drawHeight

        val bmLeft = ((intersectRect.left - treeDestRect.left) * scaleX).toInt().coerceIn(0, treeBitmap.width - 1)
        val bmRight = ((intersectRect.right - treeDestRect.left) * scaleX).toInt().coerceIn(0, treeBitmap.width - 1)
        val bmTop = ((intersectRect.top - treeDestRect.top) * scaleY).toInt().coerceIn(0, treeBitmap.height - 1)
        val bmBottom = ((intersectRect.bottom - treeDestRect.top) * scaleY).toInt().coerceIn(0, treeBitmap.height - 1)

        if (bmRight <= bmLeft || bmBottom <= bmTop) return 0

        val stepX = maxOf(1, (bmRight - bmLeft) / 36)
        val stepY = maxOf(1, (bmBottom - bmTop) / 36)
        var overlapCount = 0

        for (y in bmTop..bmBottom step stepY) {
            for (x in bmLeft..bmRight step stepX) {
                if (isContentPixel(treeBitmap.getPixel(x, y))) {
                    overlapCount++
                }
            }
        }

        return overlapCount
    }

    private fun drawComprehensiveReportCard(
        canvas: android.graphics.Canvas,
        stats: FamilyReportSummary,
        groupName: String,
        pageWidth: Float,
        pageHeight: Float,
        headerBottomY: Float,
        footerHeight: Float,
        leftMargin: Float = 28f,
        treeBitmap: Bitmap? = null,
        treeDestRect: RectF? = null
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

        // Generous, beautiful margins from page edges so it never touches or hugs the boundaries
        val cardMarginLeft = maxOf(40f, pageWidth * 0.04f)
        val cardMarginRight = cardMarginLeft
        val cardMarginBottom = footerHeight + maxOf(28f, pageHeight * 0.035f)
        val cardMarginTop = headerBottomY + 14f

        // Well-proportioned, elegant dimensions
        val maxAvailableCardWidth = (pageWidth - cardMarginLeft - cardMarginRight) * 0.44f
        val cardWidth = minOf(360f, maxAvailableCardWidth).coerceAtLeast(minOf(280f, pageWidth - (cardMarginLeft * 2f)))

        val headerHeight = 38f
        val itemHeight = 25f
        val itemGap = 5f
        val outerPadding = 14f
        val dividerHeight = 8f

        val totalContentHeight = headerHeight + dividerHeight + (items.size * (itemHeight + itemGap)) + outerPadding
        val cardHeight = totalContentHeight

        // Candidate positions across the corners and sides of the page
        data class CornerCandidate(val name: String, val rect: RectF)

        val candidates = mutableListOf(
            CornerCandidate(
                "پایین-چپ",
                RectF(cardMarginLeft, pageHeight - cardMarginBottom - cardHeight, cardMarginLeft + cardWidth, pageHeight - cardMarginBottom)
            ),
            CornerCandidate(
                "بالا-چپ",
                RectF(cardMarginLeft, cardMarginTop, cardMarginLeft + cardWidth, cardMarginTop + cardHeight)
            ),
            CornerCandidate(
                "پایین-راست",
                RectF(pageWidth - cardMarginRight - cardWidth, pageHeight - cardMarginBottom - cardHeight, pageWidth - cardMarginRight, pageHeight - cardMarginBottom)
            ),
            CornerCandidate(
                "بالا-راست",
                RectF(pageWidth - cardMarginRight - cardWidth, cardMarginTop, pageWidth - cardMarginRight, cardMarginTop + cardHeight)
            )
        )

        // Add middle candidates if the page has sufficient vertical height
        val midTop = cardMarginTop + (pageHeight - cardMarginTop - cardMarginBottom - cardHeight) / 2f
        if (midTop > cardMarginTop + 30f && (midTop + cardHeight) < (pageHeight - cardMarginBottom - 30f)) {
            candidates.add(
                CornerCandidate("وسط-چپ", RectF(cardMarginLeft, midTop, cardMarginLeft + cardWidth, midTop + cardHeight))
            )
            candidates.add(
                CornerCandidate("وسط-راست", RectF(pageWidth - cardMarginRight - cardWidth, midTop, pageWidth - cardMarginRight, midTop + cardHeight))
            )
        }

        // Intelligently select the corner that has ZERO (or minimal) overlap with the tree content
        var selectedCandidate = candidates[0]
        if (treeBitmap != null && treeDestRect != null) {
            var minOverlap = Int.MAX_VALUE
            for (candidate in candidates) {
                val overlap = calculateOverlapScore(candidate.rect, treeBitmap, treeDestRect)
                AppLogger.d("PDF_EXPORT", "بررسی موقعیت ${candidate.name}: هم‌پوشانی = $overlap")
                if (overlap == 0) {
                    selectedCandidate = candidate
                    minOverlap = 0
                    break
                }
                if (overlap < minOverlap) {
                    minOverlap = overlap
                    selectedCandidate = candidate
                }
            }
            AppLogger.i("PDF_EXPORT", "موقعیت کادر گزارش انتخاب شد: ${selectedCandidate.name} (هم‌پوشانی: $minOverlap)")
        }

        val cardRect = selectedCandidate.rect
        val cardLeft = cardRect.left
        val cardTop = cardRect.top
        val cardRight = cardRect.right
        val cardBottom = cardRect.bottom
        val cornerRadius = 18f

        // 1. Soft Drop Shadows (Multi-layered for realism)
        val shadowPaintLarge = Paint().apply {
            color = android.graphics.Color.argb(22, 0, 0, 0)
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            RectF(cardLeft + 3f, cardTop + 4f, cardRight + 3f, cardBottom + 4f),
            cornerRadius,
            cornerRadius,
            shadowPaintLarge
        )
        val shadowPaintSmall = Paint().apply {
            color = android.graphics.Color.argb(14, 0, 0, 0)
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

        // 3. Card Border (matching app theme)
        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50) // Emerald green accent
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 4. Header Section: Title & Bar Chart Icon
        val titleTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 13.5f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(30, 41, 59) // App textColor slate-900
            textAlign = Paint.Align.RIGHT
        }
        val headerTitle = "گزارش جامع $groupName"
        val headerY = cardTop + 25f
        val headerTextRight = cardRight - outerPadding - 22f
        canvas.drawText(headerTitle, headerTextRight, headerY, titleTextPaint)

        // Draw Chart Icon representation (3 ascending bars) next to title
        val chartPaint = Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val iconRight = cardRight - outerPadding
        val iconBottom = headerY
        canvas.drawRoundRect(RectF(iconRight - 4f, iconBottom - 14f, iconRight, iconBottom), 1.5f, 1.5f, chartPaint)
        canvas.drawRoundRect(RectF(iconRight - 9f, iconBottom - 10f, iconRight - 5f, iconBottom), 1.5f, 1.5f, chartPaint)
        canvas.drawRoundRect(RectF(iconRight - 14f, iconBottom - 6f, iconRight - 10f, iconBottom), 1.5f, 1.5f, chartPaint)

        // Accent pill on top left of header
        val headerPillPaint = Paint().apply {
            color = android.graphics.Color.rgb(235, 245, 235)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cardLeft + outerPadding + 6f, cardTop + 20f, 6.5f, headerPillPaint)
        val headerPillDotPaint = Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(cardLeft + outerPadding + 6f, cardTop + 20f, 3f, headerPillDotPaint)

        // 5. Horizontal Divider matching app dialog
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.argb(35, 100, 116, 139)
            strokeWidth = 0.8f
        }
        val dividerY = cardTop + headerHeight
        canvas.drawLine(cardLeft + outerPadding, dividerY, cardRight - outerPadding, dividerY, dividerPaint)

        // 6. StatRowItems (Each in a rounded card)
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
            textSize = 12.5f
            textAlign = Paint.Align.RIGHT
        }
        val itemTitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = android.graphics.Color.rgb(71, 85, 105) // Slate-600
            textAlign = Paint.Align.RIGHT
        }
        val itemValuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.5f
            isFakeBoldText = true
            color = android.graphics.Color.rgb(46, 125, 50) // accentColor
            textAlign = Paint.Align.LEFT
        }

        val rowLeft = cardLeft + outerPadding
        val rowRight = cardRight - outerPadding
        val rowRadius = 7f
        var currentY = dividerY + 6f

        for (item in items) {
            val itemRect = RectF(rowLeft, currentY, rowRight, currentY + itemHeight)
            
            // Draw background card for row
            canvas.drawRoundRect(itemRect, rowRadius, rowRadius, rowBgPaint)
            canvas.drawRoundRect(itemRect, rowRadius, rowRadius, rowBorderPaint)

            val textBaseline = currentY + (itemHeight / 2f) + 3.6f

            // Right side: Emoji + Title
            val emojiX = rowRight - 8f
            canvas.drawText(item.icon, emojiX, textBaseline, emojiPaint)

            val titleX = emojiX - 18f
            val valueX = rowLeft + 8f
            
            // Measure value width so title does not collide
            val valueStr = item.value
            val valueWidth = itemValuePaint.measureText(valueStr)
            val maxTitleWidth = (titleX - (valueX + valueWidth + 10f)).coerceAtLeast(40f)

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

