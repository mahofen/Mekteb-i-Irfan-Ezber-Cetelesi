package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.CurriculumData
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.CurriculumManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

  /**
   * Generates a single student's official A4 Report Card (Talebe Karnesi) PDF
   * and opens the Android share / print chooser sheet.
   */
  fun exportStudentReportCardPdf(
    context: Context,
    student: Student,
    memorizations: List<MemorizationRecord>,
    attendanceList: List<AttendanceRecord>,
    rankIndex: Int = 1,
    totalStudents: Int = 1
  ) {
    try {
      val pdfDocument = PdfDocument()
      val pageWidth = 595 // A4 standard width in points
      val pageHeight = 842 // A4 standard height in points
      val marginX = 32f
      val usableWidth = pageWidth - (marginX * 2) // 531 pt

      val studentAtt = attendanceList.filter { it.studentId == student.id }
      val totalAttDays = studentAtt.size
      val presentDays = studentAtt.count { it.status == "GELDI" }
      val attendanceRate = if (totalAttDays > 0) (presentDays * 100) / totalAttDays else 100

      val studentMems = memorizations.filter { it.studentId == student.id }
      val completedCount = studentMems.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
      val inProgressCount = studentMems.count { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
      val avgRating = if (studentMems.isNotEmpty()) studentMems.map { it.rating }.average() else 5.0

      // Dynamic Active Curriculum from CurriculumManager
      val activeCurriculum = CurriculumManager.getInstance(context).getActiveItems().ifEmpty {
        CurriculumManager.getInstance(context).itemsFlow.value
      }
      val totalTargetCount = activeCurriculum.size.coerceAtLeast(1)

      val totalCurriculumDone = activeCurriculum.count { curItem ->
        studentMems.any { m -> m.title.equals(curItem.title.trim(), ignoreCase = true) && (m.status == "TAMAMLANDI" || m.progressPercent >= 100) }
      }
      val overallCurriculumPct = ((totalCurriculumDone.toFloat() / totalTargetCount) * 100).toInt().coerceIn(0, 100)

      // Determine Honor/Degree badge
      val degreeText = when {
        overallCurriculumPct >= 80 || (completedCount >= 30 && attendanceRate >= 90) -> "ÜSTÜN BAŞARI & İFTİHAR"
        overallCurriculumPct >= 50 || (completedCount >= 15 && attendanceRate >= 80) -> "TAKDİR VE TEBRİK"
        attendanceRate >= 90 -> "DÜZENLİ DEVAM & GAYRET"
        else -> "GAYRETLİ TALEBE"
      }

      // Paints
      val textPaint = Paint().apply { isAntiAlias = true }
      val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
      val strokePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }

      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas: Canvas = page.canvas

      var currentY = marginX

      // 1. OUTSIDE DECORATIVE CERTIFICATE BORDER
      strokePaint.color = Color.rgb(212, 160, 23) // Islamic Gold
      strokePaint.strokeWidth = 2.5f
      canvas.drawRoundRect(RectF(marginX - 10f, marginX - 10f, marginX + usableWidth + 10f, pageHeight - marginX + 10f), 12f, 12f, strokePaint)

      strokePaint.color = Color.rgb(226, 232, 240) // Slate light border
      strokePaint.strokeWidth = 1f
      canvas.drawRoundRect(RectF(marginX - 6f, marginX - 6f, marginX + usableWidth + 6f, pageHeight - marginX + 6f), 8f, 8f, strokePaint)

      // 2. HEADER BANNER
      fillPaint.color = Color.rgb(15, 23, 42) // Deep Navy
      val headerRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 72f)
      canvas.drawRoundRect(headerRect, 8f, 8f, fillPaint)

      // Gold Bottom Accent Stripe on Header
      fillPaint.color = Color.rgb(212, 160, 23)
      canvas.drawRect(marginX, currentY + 68f, marginX + usableWidth, currentY + 72f, fillPaint)

      // Header Texts
      textPaint.color = Color.rgb(212, 160, 23) // Gold Title
      textPaint.textSize = 15f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("MEKTEB-İ İRFAN", marginX + 16f, currentY + 26f, textPaint)

      textPaint.color = Color.WHITE
      textPaint.textSize = 12f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("TALEBE GELİŞİM, DEVAM VE EZBER KARNESİ", marginX + 16f, currentY + 45f, textPaint)

      textPaint.color = Color.rgb(203, 213, 225)
      textPaint.textSize = 9f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      val currentDateStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date())
      canvas.drawText("Eğitim Dönemi Takip Raporu • Tanzim: $currentDateStr", marginX + 16f, currentY + 60f, textPaint)

      // Right Header Badge (Degree / Honor)
      val badgeRect = RectF(marginX + usableWidth - 165f, currentY + 16f, marginX + usableWidth - 14f, currentY + 54f)
      fillPaint.color = Color.rgb(30, 41, 59)
      canvas.drawRoundRect(badgeRect, 6f, 6f, fillPaint)
      strokePaint.color = Color.rgb(212, 160, 23)
      strokePaint.strokeWidth = 1.2f
      canvas.drawRoundRect(badgeRect, 6f, 6f, strokePaint)

      textPaint.color = Color.rgb(251, 191, 36)
      textPaint.textSize = 8.5f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      textPaint.textAlign = Paint.Align.CENTER
      canvas.drawText(degreeText, badgeRect.centerX(), currentY + 38f, textPaint)
      textPaint.textAlign = Paint.Align.LEFT

      currentY += 82f

      // 3. STUDENT PROFILE INFORMATION BOX
      fillPaint.color = Color.rgb(248, 250, 252)
      val infoBoxRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 68f)
      canvas.drawRoundRect(infoBoxRect, 8f, 8f, fillPaint)
      strokePaint.color = Color.rgb(226, 232, 240)
      strokePaint.strokeWidth = 1f
      canvas.drawRoundRect(infoBoxRect, 8f, 8f, strokePaint)

      // Left Col: Student Name & Grade
      textPaint.color = Color.rgb(100, 116, 139)
      textPaint.textSize = 8f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      canvas.drawText("TALEBE ADI SOYADI:", marginX + 14f, currentY + 18f, textPaint)

      textPaint.color = Color.rgb(15, 23, 42)
      textPaint.textSize = 13f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText(student.fullName.uppercase(Locale("tr")), marginX + 14f, currentY + 36f, textPaint)

      textPaint.color = Color.rgb(37, 99, 235)
      textPaint.textSize = 9f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      val gradeStr = if (student.grade.isNotBlank()) "Sınıfı: ${student.grade}" else "Sınıf: Belirtilmedi"
      canvas.drawText(gradeStr, marginX + 14f, currentY + 54f, textPaint)

      // Middle Col: Parent Info & Contact
      val midColX = marginX + 220f
      textPaint.color = Color.rgb(100, 116, 139)
      textPaint.textSize = 8f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      canvas.drawText("VELİ BİLGİSİ & İLETİŞİM:", midColX, currentY + 18f, textPaint)

      textPaint.color = Color.rgb(15, 23, 42)
      textPaint.textSize = 10f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      val parentNameStr = if (student.parentName.isNotBlank()) student.parentName else "—"
      canvas.drawText("Veli: $parentNameStr", midColX, currentY + 34f, textPaint)

      textPaint.color = Color.rgb(71, 85, 105)
      textPaint.textSize = 8.5f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      val phoneStr = when {
        student.parentPhone.isNotBlank() -> "Tel: ${student.parentPhone}"
        student.phone.isNotBlank() -> "Öğr. Tel: ${student.phone}"
        else -> "Tel: —"
      }
      canvas.drawText(phoneStr, midColX, currentY + 50f, textPaint)

      // Right Col: Ranking & Status
      val rightColX = marginX + usableWidth - 110f
      textPaint.color = Color.rgb(100, 116, 139)
      textPaint.textSize = 8f
      canvas.drawText("SINIF SIRALAMASI:", rightColX, currentY + 18f, textPaint)

      textPaint.color = Color.rgb(217, 119, 6)
      textPaint.textSize = 12f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("$rankIndex. / $totalStudents Talebe", rightColX, currentY + 36f, textPaint)

      textPaint.color = Color.rgb(16, 185, 129)
      textPaint.textSize = 8.5f
      canvas.drawText("Durum: ${student.status}", rightColX, currentY + 52f, textPaint)

      currentY += 76f

      // 4. FOUR KEY KPI METRIC CARDS
      val cardWidth = (usableWidth - 24f) / 4f
      val cardHeight = 48f

      val kpis = listOf(
        Triple("MÜFREDAT EZBER", "$totalCurriculumDone / 108 (%$overallCurriculumPct)", Color.rgb(16, 185, 129)),
        Triple("DEVAMLILIK ORANI", "%$attendanceRate ($presentDays/$totalAttDays Gün)", Color.rgb(37, 99, 235)),
        Triple("BAŞARI PUANI", String.format(Locale.US, "%.1f / 5.0 ⭐", avgRating), Color.rgb(217, 119, 6)),
        Triple("KAYITLI EZBERLER", "$completedCount Bitti • $inProgressCount Devam", Color.rgb(147, 51, 234))
      )

      kpis.forEachIndexed { index, (label, valText, color) ->
        val cx = marginX + index * (cardWidth + 8f)
        val rect = RectF(cx, currentY, cx + cardWidth, currentY + cardHeight)

        fillPaint.color = Color.WHITE
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
        strokePaint.color = color
        strokePaint.strokeWidth = 1.2f
        canvas.drawRoundRect(rect, 6f, 6f, strokePaint)

        // Top mini colored header
        fillPaint.color = color
        val topBand = RectF(cx, currentY, cx + cardWidth, currentY + 14f)
        canvas.drawRoundRect(topBand, 6f, 6f, fillPaint)
        canvas.drawRect(cx, currentY + 8f, cx + cardWidth, currentY + 14f, fillPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 6.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, rect.centerX(), currentY + 10.5f, textPaint)

        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(valText, rect.centerX(), currentY + 34f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
      }

      currentY += cardHeight + 12f

      // 5. DYNAMIC CURRICULUM CATEGORY BREAKDOWN BARS
      val distinctCategories = activeCurriculum.map { it.category.trim() }.distinct().take(4)
      val categoryBarData = distinctCategories.map { catName ->
        val catItems = activeCurriculum.filter { it.category.equals(catName, ignoreCase = true) }
        val targetC = catItems.size.coerceAtLeast(1)
        val doneC = catItems.count { curItem ->
          studentMems.any { m -> m.title.equals(curItem.title.trim(), ignoreCase = true) && (m.status == "TAMAMLANDI" || m.progressPercent >= 100) }
        }
        val pct = ((doneC.toFloat() / targetC) * 100).toInt().coerceIn(0, 100)
        val iconEmoji = when {
          catName.contains("Kur", ignoreCase = true) || catName.contains("Sure", ignoreCase = true) -> "📖"
          catName.contains("Risale", ignoreCase = true) || catName.contains("Nur", ignoreCase = true) -> "📚"
          catName.contains("Tesbihat", ignoreCase = true) || catName.contains("Dua", ignoreCase = true) -> "📿"
          catName.contains("Hadis", ignoreCase = true) -> "💬"
          else -> "📌"
        }
        val catColor = when {
          catName.contains("Kur", ignoreCase = true) || catName.contains("Sure", ignoreCase = true) -> Color.rgb(16, 185, 129)
          catName.contains("Risale", ignoreCase = true) || catName.contains("Nur", ignoreCase = true) -> Color.rgb(217, 119, 6)
          catName.contains("Tesbihat", ignoreCase = true) || catName.contains("Dua", ignoreCase = true) -> Color.rgb(147, 51, 234)
          catName.contains("Hadis", ignoreCase = true) -> Color.rgb(13, 148, 136)
          else -> Color.rgb(37, 99, 235)
        }
        Triple("$iconEmoji $catName", "$doneC / $targetC Tamamlandı (%$pct)", Pair(pct, catColor))
      }

      val boxH = if (categoryBarData.size > 3) 100f else 86f
      fillPaint.color = Color.rgb(248, 250, 252)
      val catBoxRect = RectF(marginX, currentY, marginX + usableWidth, currentY + boxH)
      canvas.drawRoundRect(catBoxRect, 8f, 8f, fillPaint)
      strokePaint.color = Color.rgb(226, 232, 240)
      strokePaint.strokeWidth = 1f
      canvas.drawRoundRect(catBoxRect, 8f, 8f, strokePaint)

      textPaint.color = Color.rgb(15, 23, 42)
      textPaint.textSize = 9.5f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("AKTİF EZBER MÜFREDATI GELİŞİM GRAFİĞİ", marginX + 12f, currentY + 16f, textPaint)

      var catBarY = currentY + 30f
      categoryBarData.forEach { (catTitle, catDetail, progressPair) ->
        val pct = progressPair.first
        val barColor = progressPair.second

        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(catTitle, marginX + 12f, catBarY + 3f, textPaint)

        textPaint.color = barColor
        textPaint.textSize = 7.5f
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(catDetail, marginX + usableWidth - 12f, catBarY + 3f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        // Progress Bar Background
        val barX = marginX + 130f
        val barW = usableWidth - 250f
        val barH = 7f
        fillPaint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(RectF(barX, catBarY - 5f, barX + barW, catBarY - 5f + barH), 3f, 3f, fillPaint)

        // Progress Bar Fill
        val filledW = (barW * (pct.coerceIn(0, 100) / 100f))
        if (filledW > 0) {
          fillPaint.color = barColor
          canvas.drawRoundRect(RectF(barX, catBarY - 5f, barX + filledW, catBarY - 5f + barH), 3f, 3f, fillPaint)
        }

        catBarY += 17f
      }

      currentY += boxH + 10f

      // 6. DETAILED MEMORIZATION TABLE
      textPaint.color = Color.rgb(15, 23, 42)
      textPaint.textSize = 9.5f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("EZBERLENEN SURE, RİSALE VE DUA KAYITLARI ÇİZELGESİ", marginX + 2f, currentY + 2f, textPaint)

      currentY += 10f

      val tableHeaderHeight = 18f
      val tableRowHeight = 19f

      // Table Header
      fillPaint.color = Color.rgb(30, 41, 59)
      val tHeaderRect = RectF(marginX, currentY, marginX + usableWidth, currentY + tableHeaderHeight)
      canvas.drawRoundRect(tHeaderRect, 4f, 4f, fillPaint)

      textPaint.color = Color.WHITE
      textPaint.textSize = 7.5f
      textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

      val colNoW = 24f
      val colTitleW = 160f
      val colCatW = 85f
      val colStatusW = 80f
      val colRatingW = 55f
      val colNotesW = 127f

      var tx = marginX
      canvas.drawText("NO", tx + 4f, currentY + 12f, textPaint)
      tx += colNoW
      canvas.drawText("EZBER / BAŞLIK", tx + 4f, currentY + 12f, textPaint)
      tx += colTitleW
      canvas.drawText("KATEGORİ", tx + 4f, currentY + 12f, textPaint)
      tx += colCatW
      canvas.drawText("DURUM / ORAN", tx + 4f, currentY + 12f, textPaint)
      tx += colStatusW
      canvas.drawText("PUAN", tx + 4f, currentY + 12f, textPaint)
      tx += colRatingW
      canvas.drawText("EĞİTMEN NOTU", tx + 4f, currentY + 12f, textPaint)

      currentY += tableHeaderHeight

      // Compute dynamic pagination for student report card:
      // If student has <= 12 items, everything fits on Page 1 along with evaluation & signatures.
      // If student has > 12 items, Page 1 fills table up to 20 items, and remaining items flow to Page 2, Page 3, etc.
      val p1CapacitySingle = 12
      val p1CapacityMulti = 20
      val contCapacityWithSignatures = 30
      val contCapacityWithoutSignatures = 36

      val isSinglePage = studentMems.size <= p1CapacitySingle

      val (p1Items, remainingAfterP1) = if (isSinglePage) {
        studentMems to emptyList()
      } else {
        studentMems.take(p1CapacityMulti) to studentMems.drop(p1CapacityMulti)
      }

      // Calculate continuation pages
      val continuationPages = mutableListOf<List<MemorizationRecord>>()
      var rem = remainingAfterP1
      while (rem.isNotEmpty()) {
        if (rem.size <= contCapacityWithSignatures) {
          continuationPages.add(rem)
          rem = emptyList()
        } else {
          val takeCount = contCapacityWithoutSignatures
          continuationPages.add(rem.take(takeCount))
          rem = rem.drop(takeCount)
        }
      }

      val totalPages = 1 + continuationPages.size

      if (p1Items.isEmpty()) {
        fillPaint.color = Color.WHITE
        val emptyRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 36f)
        canvas.drawRect(emptyRect, fillPaint)
        strokePaint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(emptyRect, strokePaint)

        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Henüz sisteme girilmiş bireysel ezber kaydı bulunmamaktadır.", marginX + usableWidth / 2f, currentY + 22f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        currentY += 36f
      } else {
        p1Items.forEachIndexed { i, mem ->
          val isEven = (i % 2 == 0)
          val rowRect = RectF(marginX, currentY, marginX + usableWidth, currentY + tableRowHeight)
          fillPaint.color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
          canvas.drawRect(rowRect, fillPaint)
          strokePaint.color = Color.rgb(226, 232, 240)
          canvas.drawRect(rowRect, strokePaint)

          var cx = marginX
          // No
          textPaint.color = Color.rgb(100, 116, 139)
          textPaint.textSize = 7.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          canvas.drawText("${i + 1}", cx + 4f, currentY + 13f, textPaint)
          cx += colNoW

          // Title
          textPaint.color = Color.rgb(15, 23, 42)
          val dispTitle = if (mem.title.length > 25) mem.title.take(23) + "..." else mem.title
          canvas.drawText(dispTitle, cx + 4f, currentY + 13f, textPaint)
          cx += colTitleW

          // Category
          textPaint.color = Color.rgb(71, 85, 105)
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          canvas.drawText(mem.category, cx + 4f, currentY + 13f, textPaint)
          cx += colCatW

          // Status & Percent
          val isDone = mem.status == "TAMAMLANDI" || mem.progressPercent >= 100
          textPaint.color = if (isDone) Color.rgb(16, 185, 129) else Color.rgb(217, 119, 6)
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          val statStr = if (isDone) "Tamamlandı" else "%${mem.progressPercent} Devam"
          canvas.drawText(statStr, cx + 4f, currentY + 13f, textPaint)
          cx += colStatusW

          // Puan (Stars)
          textPaint.color = Color.rgb(217, 119, 6)
          val starStr = "★".repeat(mem.rating) + "☆".repeat(5 - mem.rating)
          canvas.drawText(starStr, cx + 4f, currentY + 13f, textPaint)
          cx += colRatingW

          // Teacher Notes
          textPaint.color = Color.rgb(100, 116, 139)
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          val noteStr = if (mem.teacherNotes.isNotBlank()) {
            if (mem.teacherNotes.length > 24) mem.teacherNotes.take(22) + "..." else mem.teacherNotes
          } else "—"
          canvas.drawText(noteStr, cx + 4f, currentY + 13f, textPaint)

          currentY += tableRowHeight
        }
      }

      currentY += 8f

      // If single-page report, draw Teacher Evaluation, Badges, Signatures on Page 1
      if (isSinglePage) {
        val earnedBadges = com.example.data.model.BadgeHelper.calculateBadges(student, memorizations, attendanceList).filter { it.isEarned }
        val badgeNamesStr = if (earnedBadges.isNotEmpty()) {
          "🏅 Kazanılan Rozetler: " + earnedBadges.joinToString(", ") { "${it.title}" }
        } else {
          "🏅 Rozet Durumu: İlk ezber rozetleri hedeflenmektedir."
        }

        fillPaint.color = Color.rgb(254, 252, 232)
        val noteBoxRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 52f)
        canvas.drawRoundRect(noteBoxRect, 6f, 6f, fillPaint)
        strokePaint.color = Color.rgb(254, 240, 138)
        canvas.drawRoundRect(noteBoxRect, 6f, 6f, strokePaint)

        textPaint.color = Color.rgb(133, 77, 14)
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EĞİTMEN DEĞERLENDİRMESİ & BAŞARI ROZETLERİ:", marginX + 8f, currentY + 13f, textPaint)

        textPaint.color = Color.rgb(69, 26, 3)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val commentText = if (student.notes.isNotBlank()) {
          "\"${student.notes}\""
        } else {
          "Talebemiz derslerine düzenli devam etmekte olup ahlak, edep ve ezber gayreti takdire şayandır. Başarılarının devamını dileriz."
        }
        canvas.drawText(commentText, marginX + 8f, currentY + 28f, textPaint)

        // Badges line
        textPaint.color = Color.rgb(180, 83, 9)
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(badgeNamesStr, marginX + 8f, currentY + 43f, textPaint)

        currentY += 60f

        // 8. OFFICIAL SIGNATURE & APPROVAL SECTION
        val signBoxW = usableWidth / 3f
        val signBoxH = 48f

        val signCols = listOf(
          "Ders Hocası / Eğitmen" to "İmza: ....................",
          "Kurum Müdürü / Yetkili" to "Mühür & İmza",
          "Veli Onayı" to "İmza: ...................."
        )

        signCols.forEachIndexed { i, (title, sub) ->
          val sx = marginX + i * signBoxW
          val rect = RectF(sx + 4f, currentY, sx + signBoxW - 4f, currentY + signBoxH)
          fillPaint.color = Color.WHITE
          canvas.drawRoundRect(rect, 4f, 4f, fillPaint)
          strokePaint.color = Color.rgb(203, 213, 225)
          canvas.drawRoundRect(rect, 4f, 4f, strokePaint)

          textPaint.color = Color.rgb(15, 23, 42)
          textPaint.textSize = 8f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          textPaint.textAlign = Paint.Align.CENTER
          canvas.drawText(title, rect.centerX(), currentY + 16f, textPaint)

          textPaint.color = Color.rgb(148, 163, 184)
          textPaint.textSize = 7.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          canvas.drawText(sub, rect.centerX(), currentY + 36f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT
        }
      } else {
        // Multi-page notice at bottom of Page 1
        fillPaint.color = Color.rgb(241, 245, 249)
        val contNoteRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 22f)
        canvas.drawRoundRect(contNoteRect, 4f, 4f, fillPaint)
        strokePaint.color = Color.rgb(203, 213, 225)
        canvas.drawRoundRect(contNoteRect, 4f, 4f, strokePaint)

        textPaint.color = Color.rgb(37, 99, 235)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        val remCount = studentMems.size - p1Items.size
        canvas.drawText("➡️ Kalan $remCount ezber kaydı Sayfa 2'de devam etmektedir...", marginX + usableWidth / 2f, currentY + 14f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
      }

      // 9. FOOTER
      val footerY = pageHeight - marginX + 2f
      strokePaint.color = Color.rgb(226, 232, 240)
      canvas.drawLine(marginX, footerY - 10f, marginX + usableWidth, footerY - 10f, strokePaint)

      textPaint.color = Color.rgb(148, 163, 184)
      textPaint.textSize = 7.5f
      canvas.drawText("Mekteb-i İrfan Talebe Takip & Ezber Yönetim Sistemi • Resmi Karne Çıktısı", marginX, footerY, textPaint)

      textPaint.textAlign = Paint.Align.RIGHT
      canvas.drawText("Sayfa 1 / $totalPages", marginX + usableWidth, footerY, textPaint)
      textPaint.textAlign = Paint.Align.LEFT

      pdfDocument.finishPage(page)

      // Multi-page continuation if student has > 12 items
      if (totalPages > 1) {
        var processedSoFar = p1Items.size

        continuationPages.forEachIndexed { pageIdx, pageItems ->
          val p = pageIdx + 2
          val isLastPage = (p == totalPages)
          val contPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, p).create()
          val contPage = pdfDocument.startPage(contPageInfo)
          val contCanvas = contPage.canvas

          var contY = marginX

          // Outer border
          strokePaint.color = Color.rgb(212, 160, 23)
          strokePaint.strokeWidth = 2.5f
          contCanvas.drawRoundRect(RectF(marginX - 10f, marginX - 10f, marginX + usableWidth + 10f, pageHeight - marginX + 10f), 12f, 12f, strokePaint)

          strokePaint.color = Color.rgb(226, 232, 240)
          strokePaint.strokeWidth = 1f
          contCanvas.drawRoundRect(RectF(marginX - 6f, marginX - 6f, marginX + usableWidth + 6f, pageHeight - marginX + 6f), 8f, 8f, strokePaint)

          // Continuation Header Banner
          fillPaint.color = Color.rgb(15, 23, 42)
          val contHeaderRect = RectF(marginX, contY, marginX + usableWidth, contY + 42f)
          contCanvas.drawRoundRect(contHeaderRect, 6f, 6f, fillPaint)

          fillPaint.color = Color.rgb(212, 160, 23)
          contCanvas.drawRect(marginX, contY + 38f, marginX + usableWidth, contY + 42f, fillPaint)

          textPaint.color = Color.rgb(212, 160, 23)
          textPaint.textSize = 12f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          contCanvas.drawText("MEKTEB-İ İRFAN", marginX + 14f, contY + 18f, textPaint)

          textPaint.color = Color.WHITE
          textPaint.textSize = 9.5f
          contCanvas.drawText("${student.fullName.uppercase(Locale("tr"))} • EZBER ÇİZELGESİ DEVAMI", marginX + 14f, contY + 32f, textPaint)

          textPaint.color = Color.rgb(203, 213, 225)
          textPaint.textSize = 8.5f
          textPaint.textAlign = Paint.Align.RIGHT
          contCanvas.drawText("Sayfa $p / $totalPages", marginX + usableWidth - 14f, contY + 25f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT

          contY += 50f

          // Table Header
          fillPaint.color = Color.rgb(30, 41, 59)
          val tHeaderRect = RectF(marginX, contY, marginX + usableWidth, contY + tableHeaderHeight)
          contCanvas.drawRoundRect(tHeaderRect, 4f, 4f, fillPaint)

          textPaint.color = Color.WHITE
          textPaint.textSize = 7.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

          var contTx = marginX
          contCanvas.drawText("NO", contTx + 4f, contY + 12f, textPaint)
          contTx += colNoW
          contCanvas.drawText("EZBER / BAŞLIK", contTx + 4f, contY + 12f, textPaint)
          contTx += colTitleW
          contCanvas.drawText("KATEGORİ", contTx + 4f, contY + 12f, textPaint)
          contTx += colCatW
          contCanvas.drawText("DURUM / ORAN", contTx + 4f, contY + 12f, textPaint)
          contTx += colStatusW
          contCanvas.drawText("PUAN", contTx + 4f, contY + 12f, textPaint)
          contTx += colRatingW
          contCanvas.drawText("EĞİTMEN NOTU", contTx + 4f, contY + 12f, textPaint)

          contY += tableHeaderHeight

          pageItems.forEachIndexed { idx, mem ->
            val globalIdx = processedSoFar + idx
            val isEven = (idx % 2 == 0)
            val rowRect = RectF(marginX, contY, marginX + usableWidth, contY + tableRowHeight)
            fillPaint.color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
            contCanvas.drawRect(rowRect, fillPaint)
            strokePaint.color = Color.rgb(226, 232, 240)
            contCanvas.drawRect(rowRect, strokePaint)

            var cx = marginX
            textPaint.color = Color.rgb(100, 116, 139)
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            contCanvas.drawText("${globalIdx + 1}", cx + 4f, contY + 13f, textPaint)
            cx += colNoW

            textPaint.color = Color.rgb(15, 23, 42)
            val dispTitle = if (mem.title.length > 25) mem.title.take(23) + "..." else mem.title
            contCanvas.drawText(dispTitle, cx + 4f, contY + 13f, textPaint)
            cx += colTitleW

            textPaint.color = Color.rgb(71, 85, 105)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            contCanvas.drawText(mem.category, cx + 4f, contY + 13f, textPaint)
            cx += colCatW

            val isDone = mem.status == "TAMAMLANDI" || mem.progressPercent >= 100
            textPaint.color = if (isDone) Color.rgb(16, 185, 129) else Color.rgb(217, 119, 6)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val statStr = if (isDone) "Tamamlandı" else "%${mem.progressPercent} Devam"
            contCanvas.drawText(statStr, cx + 4f, contY + 13f, textPaint)
            cx += colStatusW

            textPaint.color = Color.rgb(217, 119, 6)
            val starStr = "★".repeat(mem.rating) + "☆".repeat(5 - mem.rating)
            contCanvas.drawText(starStr, cx + 4f, contY + 13f, textPaint)
            cx += colRatingW

            textPaint.color = Color.rgb(100, 116, 139)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val noteStr = if (mem.teacherNotes.isNotBlank()) {
              if (mem.teacherNotes.length > 24) mem.teacherNotes.take(22) + "..." else mem.teacherNotes
            } else "—"
            contCanvas.drawText(noteStr, cx + 4f, contY + 13f, textPaint)

            contY += tableRowHeight
          }

          processedSoFar += pageItems.size

          // If last page, draw evaluation and signature boxes
          if (isLastPage) {
            contY += 8f

            val earnedBadges = com.example.data.model.BadgeHelper.calculateBadges(student, memorizations, attendanceList).filter { it.isEarned }
            val badgeNamesStr = if (earnedBadges.isNotEmpty()) {
              "🏅 Kazanılan Rozetler: " + earnedBadges.joinToString(", ") { "${it.title}" }
            } else {
              "🏅 Rozet Durumu: İlk ezber rozetleri hedeflenmektedir."
            }

            fillPaint.color = Color.rgb(254, 252, 232)
            val noteBoxRect = RectF(marginX, contY, marginX + usableWidth, contY + 52f)
            contCanvas.drawRoundRect(noteBoxRect, 6f, 6f, fillPaint)
            strokePaint.color = Color.rgb(254, 240, 138)
            contCanvas.drawRoundRect(noteBoxRect, 6f, 6f, strokePaint)

            textPaint.color = Color.rgb(133, 77, 14)
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            contCanvas.drawText("EĞİTMEN DEĞERLENDİRMESİ & BAŞARI ROZETLERİ:", marginX + 8f, contY + 13f, textPaint)

            textPaint.color = Color.rgb(69, 26, 3)
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val commentText = if (student.notes.isNotBlank()) {
              "\"${student.notes}\""
            } else {
              "Talebemiz derslerine düzenli devam etmekte olup ahlak, edep ve ezber gayreti takdire şayandır. Başarılarının devamını dileriz."
            }
            contCanvas.drawText(commentText, marginX + 8f, contY + 28f, textPaint)

            textPaint.color = Color.rgb(180, 83, 9)
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            contCanvas.drawText(badgeNamesStr, marginX + 8f, contY + 43f, textPaint)

            contY += 62f

            // Official Signatures
            val signBoxW = usableWidth / 3f
            val signBoxH = 50f
            val signCols = listOf(
              "Ders Hocası / Eğitmen" to "İmza: ....................",
              "Kurum Müdürü / Yetkili" to "Mühür & İmza",
              "Veli Onayı" to "İmza: ...................."
            )

            signCols.forEachIndexed { i, (title, sub) ->
              val sx = marginX + i * signBoxW
              val rect = RectF(sx + 4f, contY, sx + signBoxW - 4f, contY + signBoxH)
              fillPaint.color = Color.WHITE
              contCanvas.drawRoundRect(rect, 4f, 4f, fillPaint)
              strokePaint.color = Color.rgb(203, 213, 225)
              contCanvas.drawRoundRect(rect, 4f, 4f, strokePaint)

              textPaint.color = Color.rgb(15, 23, 42)
              textPaint.textSize = 8f
              textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
              textPaint.textAlign = Paint.Align.CENTER
              contCanvas.drawText(title, rect.centerX(), contY + 16f, textPaint)

              textPaint.color = Color.rgb(148, 163, 184)
              textPaint.textSize = 7.5f
              textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
              contCanvas.drawText(sub, rect.centerX(), contY + 38f, textPaint)
              textPaint.textAlign = Paint.Align.LEFT
            }
          }

          // Footer
          val footerY = pageHeight - marginX + 2f
          strokePaint.color = Color.rgb(226, 232, 240)
          contCanvas.drawLine(marginX, footerY - 10f, marginX + usableWidth, footerY - 10f, strokePaint)

          textPaint.color = Color.rgb(148, 163, 184)
          textPaint.textSize = 7.5f
          contCanvas.drawText("Mekteb-i İrfan Talebe Takip & Ezber Yönetim Sistemi • Resmi Karne Çıktısı", marginX, footerY, textPaint)

          textPaint.textAlign = Paint.Align.RIGHT
          contCanvas.drawText("Sayfa $p / $totalPages", marginX + usableWidth, footerY, textPaint)
          textPaint.textAlign = Paint.Align.LEFT

          pdfDocument.finishPage(contPage)
        }
      }

      // Write to cache file
      val safeStudentName = student.fullName.replace(" ", "_").lowercase(Locale("tr"))
      val file = File(context.cacheDir, "karne_${safeStudentName}.pdf")
      val fileOutputStream = FileOutputStream(file)
      pdfDocument.writeTo(fileOutputStream)
      fileOutputStream.close()
      pdfDocument.close()

      // Share or Print Intent
      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_SUBJECT, "${student.fullName} - Talebe Karnesi (PDF)")
        putExtra(
          Intent.EXTRA_TEXT,
          "Mekteb-i İrfan Talebe Gelişim & Ezber Karnesi: ${student.fullName} öğrencimizin resmi karnesi PDF olarak ektedir."
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(
        Intent.createChooser(shareIntent, "${student.fullName} Karnesini PDF Olarak Paylaş / Yazdır")
      )

      Toast.makeText(context, "${student.fullName} için karne PDF olarak hazırlandı!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "PDF Karne oluşturulurken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
  }

  /**
   * Generates a multi-page PDF containing the report cards of all students in the class.
   */
  fun exportAllStudentsReportCardsPdf(
    context: Context,
    students: List<Student>,
    memorizations: List<MemorizationRecord>,
    attendanceList: List<AttendanceRecord>
  ) {
    if (students.isEmpty()) {
      Toast.makeText(context, "Kayıtlı öğrenci bulunamadı.", Toast.LENGTH_SHORT).show()
      return
    }

    try {
      val pdfDocument = PdfDocument()
      val pageWidth = 595
      val pageHeight = 842
      val marginX = 32f
      val usableWidth = pageWidth - (marginX * 2)

      val textPaint = Paint().apply { isAntiAlias = true }
      val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
      val strokePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }

      val totalPages = students.size
      val currentDateStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date())

      students.forEachIndexed { pageIndex, student ->
        val pageNumber = pageIndex + 1
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val studentAtt = attendanceList.filter { it.studentId == student.id }
        val totalAttDays = studentAtt.size
        val presentDays = studentAtt.count { it.status == "GELDI" }
        val attendanceRate = if (totalAttDays > 0) (presentDays * 100) / totalAttDays else 100

        val studentMems = memorizations.filter { it.studentId == student.id }
        val completedCount = studentMems.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
        val inProgressCount = studentMems.count { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
        val avgRating = if (studentMems.isNotEmpty()) studentMems.map { it.rating }.average() else 5.0

        // Dynamic Active Curriculum from CurriculumManager
        val activeCurriculum = CurriculumManager.getInstance(context).getActiveItems().ifEmpty {
          CurriculumManager.getInstance(context).itemsFlow.value
        }
        val totalTargetCount = activeCurriculum.size.coerceAtLeast(1)

        val totalCurriculumDone = activeCurriculum.count { curItem ->
          studentMems.any { m -> m.title.equals(curItem.title.trim(), ignoreCase = true) && (m.status == "TAMAMLANDI" || m.progressPercent >= 100) }
        }
        val overallCurriculumPct = ((totalCurriculumDone.toFloat() / totalTargetCount) * 100).toInt().coerceIn(0, 100)

        val degreeText = when {
          overallCurriculumPct >= 80 || (completedCount >= 30 && attendanceRate >= 90) -> "ÜSTÜN BAŞARI & İFTİHAR"
          overallCurriculumPct >= 50 || (completedCount >= 15 && attendanceRate >= 80) -> "TAKDİR VE TEBRİK"
          attendanceRate >= 90 -> "DÜZENLİ DEVAM & GAYRET"
          else -> "GAYRETLİ TALEBE"
        }

        var currentY = marginX

        // 1. OUTER BORDER
        strokePaint.color = Color.rgb(212, 160, 23)
        strokePaint.strokeWidth = 2.5f
        canvas.drawRoundRect(RectF(marginX - 10f, marginX - 10f, marginX + usableWidth + 10f, pageHeight - marginX + 10f), 12f, 12f, strokePaint)

        // 2. HEADER BANNER
        fillPaint.color = Color.rgb(15, 23, 42)
        val headerRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 72f)
        canvas.drawRoundRect(headerRect, 8f, 8f, fillPaint)

        fillPaint.color = Color.rgb(212, 160, 23)
        canvas.drawRect(marginX, currentY + 68f, marginX + usableWidth, currentY + 72f, fillPaint)

        textPaint.color = Color.rgb(212, 160, 23)
        textPaint.textSize = 15f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MEKTEB-İ İRFAN", marginX + 16f, currentY + 26f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TALEBE GELİŞİM, DEVAM VE EZBER KARNESİ", marginX + 16f, currentY + 45f, textPaint)

        textPaint.color = Color.rgb(203, 213, 225)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Talebe: ${pageNumber} / ${totalPages} • Tanzim: $currentDateStr", marginX + 16f, currentY + 60f, textPaint)

        // Right Honor Badge
        val badgeRect = RectF(marginX + usableWidth - 165f, currentY + 16f, marginX + usableWidth - 14f, currentY + 54f)
        fillPaint.color = Color.rgb(30, 41, 59)
        canvas.drawRoundRect(badgeRect, 6f, 6f, fillPaint)
        strokePaint.color = Color.rgb(212, 160, 23)
        strokePaint.strokeWidth = 1.2f
        canvas.drawRoundRect(badgeRect, 6f, 6f, strokePaint)

        textPaint.color = Color.rgb(251, 191, 36)
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(degreeText, badgeRect.centerX(), currentY + 38f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        currentY += 82f

        // 3. STUDENT PROFILE BOX
        fillPaint.color = Color.rgb(248, 250, 252)
        val infoBoxRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 68f)
        canvas.drawRoundRect(infoBoxRect, 8f, 8f, fillPaint)
        strokePaint.color = Color.rgb(226, 232, 240)
        strokePaint.strokeWidth = 1f
        canvas.drawRoundRect(infoBoxRect, 8f, 8f, strokePaint)

        textPaint.color = Color.rgb(100, 116, 139)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TALEBE ADI SOYADI:", marginX + 14f, currentY + 18f, textPaint)

        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(student.fullName.uppercase(Locale("tr")), marginX + 14f, currentY + 36f, textPaint)

        textPaint.color = Color.rgb(37, 99, 235)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val gradeStr = if (student.grade.isNotBlank()) "Sınıfı: ${student.grade}" else "Sınıf: Belirtilmedi"
        canvas.drawText(gradeStr, marginX + 14f, currentY + 54f, textPaint)

        val midColX = marginX + 220f
        textPaint.color = Color.rgb(100, 116, 139)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("VELİ BİLGİSİ & İLETİŞİM:", midColX, currentY + 18f, textPaint)

        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val parentNameStr = if (student.parentName.isNotBlank()) student.parentName else "—"
        canvas.drawText("Veli: $parentNameStr", midColX, currentY + 34f, textPaint)

        textPaint.color = Color.rgb(71, 85, 105)
        textPaint.textSize = 8.5f
        val phoneStr = when {
          student.parentPhone.isNotBlank() -> "Tel: ${student.parentPhone}"
          student.phone.isNotBlank() -> "Öğr. Tel: ${student.phone}"
          else -> "Tel: —"
        }
        canvas.drawText(phoneStr, midColX, currentY + 50f, textPaint)

        val rightColX = marginX + usableWidth - 110f
        textPaint.color = Color.rgb(100, 116, 139)
        textPaint.textSize = 8f
        canvas.drawText("KAYIT / DURUM:", rightColX, currentY + 18f, textPaint)

        textPaint.color = Color.rgb(16, 185, 129)
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(student.status, rightColX, currentY + 36f, textPaint)

        currentY += 76f

        // 4. KPIS
        val cardWidth = (usableWidth - 24f) / 4f
        val cardHeight = 48f
        val kpis = listOf(
          Triple("MÜFREDAT EZBER", "$totalCurriculumDone / $totalTargetCount (%$overallCurriculumPct)", Color.rgb(16, 185, 129)),
          Triple("DEVAMLILIK ORANI", "%$attendanceRate ($presentDays/$totalAttDays)", Color.rgb(37, 99, 235)),
          Triple("BAŞARI PUANI", String.format(Locale.US, "%.1f / 5.0 ⭐", avgRating), Color.rgb(217, 119, 6)),
          Triple("KAYITLI EZBERLER", "$completedCount Bitti • $inProgressCount Devam", Color.rgb(147, 51, 234))
        )

        kpis.forEachIndexed { idx, (label, valText, color) ->
          val cx = marginX + idx * (cardWidth + 8f)
          val rect = RectF(cx, currentY, cx + cardWidth, currentY + cardHeight)

          fillPaint.color = Color.WHITE
          canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
          strokePaint.color = color
          strokePaint.strokeWidth = 1.2f
          canvas.drawRoundRect(rect, 6f, 6f, strokePaint)

          fillPaint.color = color
          val topBand = RectF(cx, currentY, cx + cardWidth, currentY + 14f)
          canvas.drawRoundRect(topBand, 6f, 6f, fillPaint)
          canvas.drawRect(cx, currentY + 8f, cx + cardWidth, currentY + 14f, fillPaint)

          textPaint.color = Color.WHITE
          textPaint.textSize = 6.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          textPaint.textAlign = Paint.Align.CENTER
          canvas.drawText(label, rect.centerX(), currentY + 10.5f, textPaint)

          textPaint.color = Color.rgb(15, 23, 42)
          textPaint.textSize = 8.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          canvas.drawText(valText, rect.centerX(), currentY + 34f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT
        }

        currentY += cardHeight + 12f

        // 5. DYNAMIC CATEGORY PROGRESS
        val distinctCategories = activeCurriculum.map { it.category.trim() }.distinct().take(4)
        val categoryBarData = distinctCategories.map { catName ->
          val catItems = activeCurriculum.filter { it.category.equals(catName, ignoreCase = true) }
          val targetC = catItems.size.coerceAtLeast(1)
          val doneC = catItems.count { curItem ->
            studentMems.any { m -> m.title.equals(curItem.title.trim(), ignoreCase = true) && (m.status == "TAMAMLANDI" || m.progressPercent >= 100) }
          }
          val pct = ((doneC.toFloat() / targetC) * 100).toInt().coerceIn(0, 100)
          val iconEmoji = when {
            catName.contains("Kur", ignoreCase = true) || catName.contains("Sure", ignoreCase = true) -> "📖"
            catName.contains("Risale", ignoreCase = true) || catName.contains("Nur", ignoreCase = true) -> "📚"
            catName.contains("Tesbihat", ignoreCase = true) || catName.contains("Dua", ignoreCase = true) -> "📿"
            catName.contains("Hadis", ignoreCase = true) -> "💬"
            else -> "📌"
          }
          val catColor = when {
            catName.contains("Kur", ignoreCase = true) || catName.contains("Sure", ignoreCase = true) -> Color.rgb(16, 185, 129)
            catName.contains("Risale", ignoreCase = true) || catName.contains("Nur", ignoreCase = true) -> Color.rgb(217, 119, 6)
            catName.contains("Tesbihat", ignoreCase = true) || catName.contains("Dua", ignoreCase = true) -> Color.rgb(147, 51, 234)
            catName.contains("Hadis", ignoreCase = true) -> Color.rgb(13, 148, 136)
            else -> Color.rgb(37, 99, 235)
          }
          Triple("$iconEmoji $catName", "$doneC / $targetC Tamamlandı (%$pct)", Pair(pct, catColor))
        }

        val boxH = if (categoryBarData.size > 3) 100f else 86f
        fillPaint.color = Color.rgb(248, 250, 252)
        val catBoxRect = RectF(marginX, currentY, marginX + usableWidth, currentY + boxH)
        canvas.drawRoundRect(catBoxRect, 8f, 8f, fillPaint)
        strokePaint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(catBoxRect, 8f, 8f, strokePaint)

        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AKTİF EZBER MÜFREDATI GELİŞİM GRAFİĞİ", marginX + 12f, currentY + 16f, textPaint)

        var catBarY = currentY + 30f
        categoryBarData.forEach { (catTitle, catDetail, progressPair) ->
          val pct = progressPair.first
          val barColor = progressPair.second

          textPaint.color = Color.rgb(15, 23, 42)
          textPaint.textSize = 8f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          canvas.drawText(catTitle, marginX + 12f, catBarY + 3f, textPaint)

          textPaint.color = barColor
          textPaint.textSize = 7.5f
          textPaint.textAlign = Paint.Align.RIGHT
          canvas.drawText(catDetail, marginX + usableWidth - 12f, catBarY + 3f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT

          val barX = marginX + 130f
          val barW = usableWidth - 250f
          val barH = 7f
          fillPaint.color = Color.rgb(226, 232, 240)
          canvas.drawRoundRect(RectF(barX, catBarY - 5f, barX + barW, catBarY - 5f + barH), 3f, 3f, fillPaint)

          val filledW = (barW * (pct.coerceIn(0, 100) / 100f))
          if (filledW > 0) {
            fillPaint.color = barColor
            canvas.drawRoundRect(RectF(barX, catBarY - 5f, barX + filledW, catBarY - 5f + barH), 3f, 3f, fillPaint)
          }

          catBarY += 17f
        }

        currentY += boxH + 10f

        // 6. MEMORIZATION TABLE
        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EZBERLENEN SURE, RİSALE VE DUA KAYITLARI ÇİZELGESİ", marginX + 2f, currentY + 2f, textPaint)

        currentY += 10f

        val tableHeaderHeight = 18f
        val tableRowHeight = 19f

        fillPaint.color = Color.rgb(30, 41, 59)
        val tHeaderRect = RectF(marginX, currentY, marginX + usableWidth, currentY + tableHeaderHeight)
        canvas.drawRoundRect(tHeaderRect, 4f, 4f, fillPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val colNoW = 24f
        val colTitleW = 160f
        val colCatW = 85f
        val colStatusW = 80f
        val colRatingW = 55f

        var tx = marginX
        canvas.drawText("NO", tx + 4f, currentY + 12f, textPaint)
        tx += colNoW
        canvas.drawText("EZBER / BAŞLIK", tx + 4f, currentY + 12f, textPaint)
        tx += colTitleW
        canvas.drawText("KATEGORİ", tx + 4f, currentY + 12f, textPaint)
        tx += colCatW
        canvas.drawText("DURUM / ORAN", tx + 4f, currentY + 12f, textPaint)
        tx += colStatusW
        canvas.drawText("PUAN", tx + 4f, currentY + 12f, textPaint)
        tx += colRatingW
        canvas.drawText("EĞİTMEN NOTU", tx + 4f, currentY + 12f, textPaint)

        currentY += tableHeaderHeight

        val maxDisplayMems = studentMems.take(11)
        if (maxDisplayMems.isEmpty()) {
          fillPaint.color = Color.WHITE
          val emptyRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 36f)
          canvas.drawRect(emptyRect, fillPaint)
          strokePaint.color = Color.rgb(226, 232, 240)
          canvas.drawRect(emptyRect, strokePaint)

          textPaint.color = Color.rgb(148, 163, 184)
          textPaint.textSize = 8.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
          textPaint.textAlign = Paint.Align.CENTER
          canvas.drawText("Henüz sisteme girilmiş bireysel ezber kaydı bulunmamaktadır.", marginX + usableWidth / 2f, currentY + 22f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT
          currentY += 36f
        } else {
          maxDisplayMems.forEachIndexed { i, mem ->
            val isEven = (i % 2 == 0)
            val rowRect = RectF(marginX, currentY, marginX + usableWidth, currentY + tableRowHeight)
            fillPaint.color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(rowRect, fillPaint)
            strokePaint.color = Color.rgb(226, 232, 240)
            canvas.drawRect(rowRect, strokePaint)

            var cx = marginX
            textPaint.color = Color.rgb(100, 116, 139)
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${i + 1}", cx + 4f, currentY + 13f, textPaint)
            cx += colNoW

            textPaint.color = Color.rgb(15, 23, 42)
            val dispTitle = if (mem.title.length > 25) mem.title.take(23) + "..." else mem.title
            canvas.drawText(dispTitle, cx + 4f, currentY + 13f, textPaint)
            cx += colTitleW

            textPaint.color = Color.rgb(71, 85, 105)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(mem.category, cx + 4f, currentY + 13f, textPaint)
            cx += colCatW

            val isDone = mem.status == "TAMAMLANDI" || mem.progressPercent >= 100
            textPaint.color = if (isDone) Color.rgb(16, 185, 129) else Color.rgb(217, 119, 6)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val statStr = if (isDone) "Tamamlandı" else "%${mem.progressPercent} Devam"
            canvas.drawText(statStr, cx + 4f, currentY + 13f, textPaint)
            cx += colStatusW

            textPaint.color = Color.rgb(217, 119, 6)
            val starStr = "★".repeat(mem.rating) + "☆".repeat(5 - mem.rating)
            canvas.drawText(starStr, cx + 4f, currentY + 13f, textPaint)
            cx += colRatingW

            textPaint.color = Color.rgb(100, 116, 139)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val noteStr = if (mem.teacherNotes.isNotBlank()) {
              if (mem.teacherNotes.length > 24) mem.teacherNotes.take(22) + "..." else mem.teacherNotes
            } else "—"
            canvas.drawText(noteStr, cx + 4f, currentY + 13f, textPaint)

            currentY += tableRowHeight
          }

          if (studentMems.size > 11) {
            textPaint.color = Color.rgb(100, 116, 139)
            textPaint.textSize = 7f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("ve ${studentMems.size - 11} diğer ezber maddesi daha tamamlanmıştır.", marginX + 4f, currentY + 10f, textPaint)
            currentY += 12f
          }
        }

        currentY += 10f

        // 7. NOTE & EARNED BADGES
        val earnedBadges = com.example.data.model.BadgeHelper.calculateBadges(student, memorizations, attendanceList).filter { it.isEarned }
        val badgeNamesStr = if (earnedBadges.isNotEmpty()) {
          "🏅 Kazanılan Rozetler: " + earnedBadges.joinToString(", ") { "${it.title}" }
        } else {
          "🏅 Rozet Durumu: İlk ezber rozetleri hedeflenmektedir."
        }

        fillPaint.color = Color.rgb(254, 252, 232)
        val noteBoxRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 52f)
        canvas.drawRoundRect(noteBoxRect, 6f, 6f, fillPaint)
        strokePaint.color = Color.rgb(254, 240, 138)
        canvas.drawRoundRect(noteBoxRect, 6f, 6f, strokePaint)

        textPaint.color = Color.rgb(133, 77, 14)
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EĞİTMEN DEĞERLENDİRMESİ & BAŞARI ROZETLERİ:", marginX + 8f, currentY + 13f, textPaint)

        textPaint.color = Color.rgb(69, 26, 3)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val commentText = if (student.notes.isNotBlank()) {
          "\"${student.notes}\""
        } else {
          "Talebemiz derslerine düzenli devam etmekte olup ahlak, edep ve ezber gayreti takdire şayandır. Başarılarının devamını dileriz."
        }
        canvas.drawText(commentText, marginX + 8f, currentY + 28f, textPaint)

        // Badges line
        textPaint.color = Color.rgb(180, 83, 9)
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(badgeNamesStr, marginX + 8f, currentY + 43f, textPaint)

        currentY += 62f

        // 8. SIGNATURES
        val signBoxW = usableWidth / 3f
        val signBoxH = 50f
        val signCols = listOf(
          "Ders Hocası / Eğitmen" to "İmza: ....................",
          "Kurum Müdürü / Yetkili" to "Mühür & İmza",
          "Veli Onayı" to "İmza: ...................."
        )

        signCols.forEachIndexed { i, (title, sub) ->
          val sx = marginX + i * signBoxW
          val rect = RectF(sx + 4f, currentY, sx + signBoxW - 4f, currentY + signBoxH)
          fillPaint.color = Color.WHITE
          canvas.drawRoundRect(rect, 4f, 4f, fillPaint)
          strokePaint.color = Color.rgb(203, 213, 225)
          canvas.drawRoundRect(rect, 4f, 4f, strokePaint)

          textPaint.color = Color.rgb(15, 23, 42)
          textPaint.textSize = 8f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          textPaint.textAlign = Paint.Align.CENTER
          canvas.drawText(title, rect.centerX(), currentY + 16f, textPaint)

          textPaint.color = Color.rgb(148, 163, 184)
          textPaint.textSize = 7.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          canvas.drawText(sub, rect.centerX(), currentY + 38f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT
        }

        // 9. FOOTER
        val footerY = pageHeight - marginX + 2f
        strokePaint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(marginX, footerY - 10f, marginX + usableWidth, footerY - 10f, strokePaint)

        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 7.5f
        canvas.drawText("Mekteb-i İrfan Talebe Takip & Ezber Yönetim Sistemi • Resmi Karne Çıktısı", marginX, footerY, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Sayfa $pageNumber / $totalPages", marginX + usableWidth, footerY, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        pdfDocument.finishPage(page)
      }

      val file = File(context.cacheDir, "mekteb_irfan_tum_talebeler_karneleri.pdf")
      val fileOutputStream = FileOutputStream(file)
      pdfDocument.writeTo(fileOutputStream)
      fileOutputStream.close()
      pdfDocument.close()

      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_SUBJECT, "Mekteb-i İrfan Tüm Talebe Karneleri (${students.size} Talebe)")
        putExtra(
          Intent.EXTRA_TEXT,
          "Mekteb-i İrfan bünyesindeki ${students.size} talebeye ait tüm resmi gelişim ve ezber karneleri PDF dosyası ektedir."
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(
        Intent.createChooser(shareIntent, "Tüm Talebelerin Karnelerini PDF Olarak Paylaş / Yazdır")
      )

      Toast.makeText(context, "${students.size} talebenin karnesi tek PDF olarak hazırlandı!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "Toplu karne PDF oluşturulurken hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
  }

  /**
   * Generates a clean, professional A4 PDF for Student & Parent Contact List (İletişim Listesi)
   * and opens the Android share/export sheet.
   */
  fun exportStudentContactListPdf(
    context: Context,
    students: List<Student>,
    filterGradeTitle: String = "Tüm Sınıflar"
  ) {
    if (students.isEmpty()) {
      Toast.makeText(context, "Dışa aktarılacak öğrenci kaydı bulunamadı.", Toast.LENGTH_SHORT).show()
      return
    }

    try {
      val pdfDocument = PdfDocument()
      val pageWidth = 595 // A4 standard width in points (72 dpi)
      val pageHeight = 842 // A4 standard height in points
      val marginX = 36f
      val usableWidth = pageWidth - (marginX * 2) // 523 pt

      // Paints
      val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.rgb(30, 41, 59)
      }

      val headerPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

      val fillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
      }

      val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 0.75f
        color = Color.rgb(226, 232, 240)
      }

      // Column widths: Total = 523
      val colNoWidth = 28f
      val colStudentWidth = 155f
      val colGradeWidth = 65f
      val colParentWidth = 135f
      val colContactWidth = 140f

      val rowHeight = 32f
      val headerSectionHeight = 84f
      val tableHeaderHeight = 24f
      val footerHeight = 36f

      val rowsPerPageFirst = ((pageHeight - marginX - headerSectionHeight - tableHeaderHeight - footerHeight) / rowHeight).toInt().coerceAtLeast(1)
      val rowsPerPageOther = ((pageHeight - marginX - 45f - tableHeaderHeight - footerHeight) / rowHeight).toInt().coerceAtLeast(1)

      // Calculate total pages
      val studentChunks = mutableListOf<List<Student>>()
      var remainingStudents = students
      var isFirst = true

      while (remainingStudents.isNotEmpty()) {
        val count = if (isFirst) rowsPerPageFirst else rowsPerPageOther
        val chunk = remainingStudents.take(count)
        studentChunks.add(chunk)
        remainingStudents = remainingStudents.drop(count)
        isFirst = false
      }

      val totalPages = studentChunks.size
      val currentDateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
      var studentCounter = 1

      // Render each page
      studentChunks.forEachIndexed { pageIndex, pageStudents ->
        val pageNumber = pageIndex + 1
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        var currentY = marginX

        if (pageNumber == 1) {
          // Top Header Banner
          fillPaint.color = Color.rgb(15, 23, 42) // Deep Navy
          val headerRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 70f)
          canvas.drawRoundRect(headerRect, 8f, 8f, fillPaint)

          // Decorative Gold Bar
          fillPaint.color = Color.rgb(212, 160, 23) // Islamic Gold
          canvas.drawRect(marginX, currentY + 67f, marginX + usableWidth, currentY + 70f, fillPaint)

          // Header Texts
          headerPaint.textSize = 15f
          headerPaint.color = Color.WHITE
          headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          canvas.drawText("MEKTEB-İ İRFAN", marginX + 16f, currentY + 26f, headerPaint)

          headerPaint.textSize = 11f
          headerPaint.color = Color.rgb(226, 232, 240)
          headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          canvas.drawText("Öğrenci & Veli İletişim Rehberi (${filterGradeTitle})", marginX + 16f, currentY + 44f, headerPaint)

          // Date & Total Count on top right
          textPaint.color = Color.rgb(203, 213, 225)
          textPaint.textSize = 9f
          textPaint.textAlign = Paint.Align.RIGHT
          canvas.drawText("Tarih: $currentDateStr", marginX + usableWidth - 16f, currentY + 26f, textPaint)
          canvas.drawText("Toplam Kayıt: ${students.size} Talebe", marginX + usableWidth - 16f, currentY + 44f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT

          currentY += headerSectionHeight
        } else {
          // Subsequent page header
          fillPaint.color = Color.rgb(15, 23, 42)
          val subHeaderRect = RectF(marginX, currentY, marginX + usableWidth, currentY + 30f)
          canvas.drawRoundRect(subHeaderRect, 6f, 6f, fillPaint)

          headerPaint.textSize = 10f
          headerPaint.color = Color.WHITE
          headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          canvas.drawText("MEKTEB-İ İRFAN • Öğrenci ve Veli İletişim Listesi (Devam)", marginX + 12f, currentY + 19f, headerPaint)

          textPaint.color = Color.rgb(203, 213, 225)
          textPaint.textSize = 8.5f
          textPaint.textAlign = Paint.Align.RIGHT
          canvas.drawText("Sayfa $pageNumber / $totalPages", marginX + usableWidth - 12f, currentY + 19f, textPaint)
          textPaint.textAlign = Paint.Align.LEFT

          currentY += 38f
        }

        // Table Column Header Bar
        fillPaint.color = Color.rgb(30, 58, 138) // Royal Navy Blue
        val tableHeaderRect = RectF(marginX, currentY, marginX + usableWidth, currentY + tableHeaderHeight)
        canvas.drawRoundRect(tableHeaderRect, 4f, 4f, fillPaint)

        headerPaint.textSize = 8.5f
        headerPaint.color = Color.WHITE
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var colX = marginX
        // No Header
        canvas.drawText("NO", colX + 6f, currentY + 16f, headerPaint)
        colX += colNoWidth

        // Öğrenci Adı Soyadı
        canvas.drawText("ÖĞRENCİ ADI SOYADI", colX + 6f, currentY + 16f, headerPaint)
        colX += colStudentWidth

        // Sınıfı
        canvas.drawText("SINIFI", colX + 6f, currentY + 16f, headerPaint)
        colX += colGradeWidth

        // Veli Adı Soyadı
        canvas.drawText("VELİ ADI SOYADI", colX + 6f, currentY + 16f, headerPaint)
        colX += colParentWidth

        // İletişim Numaraları
        canvas.drawText("İLETİŞİM / TELEFON", colX + 6f, currentY + 16f, headerPaint)

        currentY += tableHeaderHeight

        // Table Rows
        pageStudents.forEachIndexed { index, student ->
          val isEven = (index % 2 == 0)
          val rowRect = RectF(marginX, currentY, marginX + usableWidth, currentY + rowHeight)

          // Background Fill
          fillPaint.color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
          canvas.drawRect(rowRect, fillPaint)

          // Bottom Border
          canvas.drawLine(marginX, currentY + rowHeight, marginX + usableWidth, currentY + rowHeight, strokePaint)

          var cellX = marginX

          // 1. Column: Row Number
          textPaint.color = Color.rgb(100, 116, 139)
          textPaint.textSize = 8.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          canvas.drawText(String.format("%02d", studentCounter), cellX + 6f, currentY + 20f, textPaint)
          cellX += colNoWidth

          // Vertical divider
          canvas.drawLine(cellX, currentY, cellX, currentY + rowHeight, strokePaint)

          // 2. Column: Student Full Name
          textPaint.color = Color.rgb(15, 23, 42)
          textPaint.textSize = 9f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          val displayName = if (student.fullName.length > 28) student.fullName.take(26) + "..." else student.fullName
          canvas.drawText(displayName, cellX + 6f, currentY + 20f, textPaint)
          cellX += colStudentWidth

          // Vertical divider
          canvas.drawLine(cellX, currentY, cellX, currentY + rowHeight, strokePaint)

          // 3. Column: Grade (Sınıf)
          val gradeText = if (student.grade.isNotBlank()) student.grade else "Belirtilmedi"
          // Grade pill
          fillPaint.color = Color.rgb(238, 242, 255)
          val gradePillRect = RectF(cellX + 4f, currentY + 6f, cellX + colGradeWidth - 6f, currentY + 25f)
          canvas.drawRoundRect(gradePillRect, 4f, 4f, fillPaint)

          textPaint.color = Color.rgb(37, 99, 235)
          textPaint.textSize = 7.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
          canvas.drawText(gradeText, cellX + 7f, currentY + 19f, textPaint)
          cellX += colGradeWidth

          // Vertical divider
          canvas.drawLine(cellX, currentY, cellX, currentY + rowHeight, strokePaint)

          // 4. Column: Parent Full Name (Veli)
          textPaint.color = Color.rgb(51, 65, 85)
          textPaint.textSize = 8.5f
          textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          val parentName = if (student.parentName.isNotBlank()) student.parentName else "—"
          val displayParent = if (parentName.length > 24) parentName.take(22) + "..." else parentName
          canvas.drawText(displayParent, cellX + 6f, currentY + 20f, textPaint)
          cellX += colParentWidth

          // Vertical divider
          canvas.drawLine(cellX, currentY, cellX, currentY + rowHeight, strokePaint)

          // 5. Column: Contact Phones (Öğrenci Tel / Veli Tel)
          textPaint.textSize = 8f
          val studentPhone = student.phone.trim()
          val parentPhone = student.parentPhone.trim()

          if (parentPhone.isNotBlank() && studentPhone.isNotBlank()) {
            // Two-line compact contact
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.rgb(16, 185, 129) // Green for parent
            canvas.drawText("Veli: $parentPhone", cellX + 6f, currentY + 13f, textPaint)

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(71, 85, 105)
            canvas.drawText("Öğr: $studentPhone", cellX + 6f, currentY + 25f, textPaint)
          } else if (parentPhone.isNotBlank()) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.rgb(16, 185, 129)
            canvas.drawText("Veli: $parentPhone", cellX + 6f, currentY + 20f, textPaint)
          } else if (studentPhone.isNotBlank()) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText("Öğr: $studentPhone", cellX + 6f, currentY + 20f, textPaint)
          } else {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(148, 163, 184)
            canvas.drawText("Tel girilmemiş", cellX + 6f, currentY + 20f, textPaint)
          }

          // Outer table border for row
          canvas.drawRect(marginX, currentY, marginX + usableWidth, currentY + rowHeight, strokePaint)

          currentY += rowHeight
          studentCounter++
        }

        // Page Footer
        val footerY = pageHeight - marginX
        canvas.drawLine(marginX, footerY - 14f, marginX + usableWidth, footerY - 14f, strokePaint)

        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Mekteb-i İrfan Talebe & İletişim Takip Sistemi • Gizlidir", marginX, footerY, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Sayfa $pageNumber / $totalPages", marginX + usableWidth, footerY, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        pdfDocument.finishPage(page)
      }

      // Write to cache file
      val file = File(context.cacheDir, "mekteb_irfan_ogrenci_iletisim_listesi.pdf")
      val fileOutputStream = FileOutputStream(file)
      pdfDocument.writeTo(fileOutputStream)
      fileOutputStream.close()
      pdfDocument.close()

      // Share or Open via FileProvider
      val contentUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        putExtra(Intent.EXTRA_SUBJECT, "Mekteb-i İrfan Öğrenci & Veli İletişim Listesi")
        putExtra(
          Intent.EXTRA_TEXT,
          "Mekteb-i İrfan Talebe ve Veli İletişim Listesi (${students.size} Talebe) PDF belgesi ektedir."
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(
        Intent.createChooser(shareIntent, "İletişim Listesini PDF Olarak Paylaş / Dışa Aktar")
      )

      Toast.makeText(context, "İletişim listesi PDF olarak hazırlandı!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "PDF oluşturulurken hata oluştu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
  }

  /**
   * Generates student's WhatsApp report message and attaches/shares the PDF report card directly via WhatsApp.
   */
  fun shareReportCardViaWhatsApp(
    context: Context,
    student: Student,
    memorizations: List<MemorizationRecord>,
    attendanceList: List<AttendanceRecord>,
    rankIndex: Int = 1,
    totalStudents: Int = 1
  ) {
    try {
      val studentAtt = attendanceList.filter { it.studentId == student.id }
      val totalAttDays = studentAtt.size
      val presentDays = studentAtt.count { it.status == "GELDI" }
      val attendanceRate = if (totalAttDays > 0) (presentDays * 100) / totalAttDays else 100

      val studentMems = memorizations.filter { it.studentId == student.id }
      val completedCount = studentMems.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }

      // Dynamic Active Curriculum from CurriculumManager
      val activeCurriculum = CurriculumManager.getInstance(context).getActiveItems().ifEmpty {
        CurriculumManager.getInstance(context).itemsFlow.value
      }
      val totalTargetCount = activeCurriculum.size.coerceAtLeast(1)

      val totalCurriculumDone = activeCurriculum.count { curItem ->
        studentMems.any { m -> m.title.equals(curItem.title.trim(), ignoreCase = true) && (m.status == "TAMAMLANDI" || m.progressPercent >= 100) }
      }
      val overallCurriculumPct = ((totalCurriculumDone.toFloat() / totalTargetCount) * 100).toInt().coerceIn(0, 100)

      val distinctCategories = activeCurriculum.map { it.category.trim() }.distinct()
      val catSummaryLines = distinctCategories.map { catName ->
        val catItems = activeCurriculum.filter { it.category.equals(catName, ignoreCase = true) }
        val targetC = catItems.size.coerceAtLeast(1)
        val doneC = catItems.count { curItem ->
          studentMems.any { m -> m.title.equals(curItem.title.trim(), ignoreCase = true) && (m.status == "TAMAMLANDI" || m.progressPercent >= 100) }
        }
        val pct = ((doneC.toFloat() / targetC) * 100).toInt().coerceIn(0, 100)
        val iconEmoji = when {
          catName.contains("Kur", ignoreCase = true) || catName.contains("Sure", ignoreCase = true) -> "📖"
          catName.contains("Risale", ignoreCase = true) || catName.contains("Nur", ignoreCase = true) -> "📚"
          catName.contains("Tesbihat", ignoreCase = true) || catName.contains("Dua", ignoreCase = true) -> "📿"
          catName.contains("Hadis", ignoreCase = true) -> "💬"
          else -> "📌"
        }
        "$iconEmoji *$catName:* %$pct ($doneC / $targetC)"
      }

      val badges = com.example.data.model.BadgeHelper.calculateBadges(student, memorizations, attendanceList)
      val earnedBadges = badges.filter { it.isEarned }
      val badgeStr = if (earnedBadges.isNotEmpty()) {
        earnedBadges.joinToString("\n") { "  ${it.emoji} *${it.title}*: ${it.description}" }
      } else {
        "  🎯 *İlk Hedef*: İlk ezber ve devamlılık rozeti yolunda gayret ediyor."
      }

      val formattedMessage = buildString {
        appendLine("السلام عليكم ورحمة الله وبركاته")
        appendLine("Sayın Velimiz,")
        appendLine()
        appendLine("*Mekteb-i İrfan Ezber & Ahlak Takip Sistemi*")
        appendLine("Talebemiz: *${student.fullName}* (${student.grade.ifBlank { "Talebe" }})")
        appendLine("Tarih: ${SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date())}")
        appendLine()
        appendLine("📊 *GELİŞİM VE İLERLEME RAPORU:*")
        catSummaryLines.forEach { appendLine(it) }
        appendLine("🌟 *Genel Müfredat:* %$overallCurriculumPct ($totalCurriculumDone / $totalTargetCount Ezber)")
        appendLine("📅 *Ders Devam Oranı:* %$attendanceRate ($presentDays / $totalAttDays Gün)")
        appendLine()
        appendLine("🏆 *KAZANILAN ROZETLER:*")
        appendLine(badgeStr)
        appendLine()
        appendLine("⭐ *Hoca Notu & Tebrik:*")
        appendLine("Talebemizin derslerdeki edep, dikkat ve ezber gayretinden memnunuz, maşallah. Cenab-ı Hak muvaffak eylesin.")
        appendLine()
        appendLine("📄 *Detaylı A4 Karne PDF belgesi ekte bilgilerinize sunulmuştur.*")
      }

      // Generate the single student report card PDF file
      exportStudentReportCardPdf(
        context = context,
        student = student,
        memorizations = memorizations,
        attendanceList = attendanceList,
        rankIndex = rankIndex,
        totalStudents = totalStudents
      )

      // Also copy text or launch WhatsApp direct intent
      val phoneClean = student.parentPhone.replace("[^0-9+]".toRegex(), "")
      val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        `package` = "com.whatsapp"
        putExtra(Intent.EXTRA_TEXT, formattedMessage)
      }

      try {
        context.startActivity(whatsappIntent)
      } catch (e: Exception) {
        // Fallback to general share if WhatsApp is not directly targeting package
        val generalIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, formattedMessage)
          putExtra(Intent.EXTRA_SUBJECT, "${student.fullName} Mekteb-i İrfan Karnesi")
        }
        context.startActivity(Intent.createChooser(generalIntent, "Karneyi WhatsApp veya Mesaj ile Paylaş"))
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Toast.makeText(context, "WhatsApp paylaşımı hazırlanırken hata: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
  }
}
