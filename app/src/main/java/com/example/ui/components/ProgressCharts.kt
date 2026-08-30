package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurriculumData
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.CustomCurriculumItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// Helper model for categorized progress calculation
data class CategoryProgressSummary(
  val categoryName: String,
  val categoryKey: String, // "KURAN", "TESBIHAT", "RISALE" or custom key
  val icon: ImageVector,
  val color: Color,
  val averagePercent: Int,
  val completedCount: Int,
  val inProgressCount: Int,
  val totalRecordedCount: Int,
  val officialTargetCount: Int,
  val records: List<MemorizationRecord> = emptyList()
)

fun normalizeCategory(raw: String): String {
  val lower = raw.lowercase(Locale("tr"))
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") ||
      lower.contains("cüz") || lower.contains("cuz") || lower.contains("aşır") ||
      lower.contains("asir") || lower.contains("sayfa") || lower.contains("ayet") -> "KURAN"

    lower.contains("tesbihat") || lower.contains("tesbih") || lower.contains("namaz") ||
      lower.contains("vird") || lower.contains("cevşen") || lower.contains("cevsen") ||
      lower.contains("dua") -> "TESBIHAT"

    lower.contains("risale") || lower.contains("nur") || lower.contains("vecize") ||
      lower.contains("sözler") || lower.contains("sozler") || lower.contains("mektubat") ||
      lower.contains("lema") || lower.contains("şua") -> "RISALE"

    lower.contains("hadis") -> "HADIS"
    lower.contains("elif") || lower.contains("elifba") || lower.contains("tecvid") -> "ELIFBA"

    else -> raw.trim().uppercase(Locale.ROOT)
  }
}

fun getCategoryIconVector(category: String): ImageVector {
  val lower = category.lowercase(Locale("tr"))
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") || lower.contains("cüz") -> Icons.AutoMirrored.Filled.MenuBook
    lower.contains("risale") || lower.contains("sozler") || lower.contains("nur") -> Icons.Default.MenuBook
    lower.contains("tesbihat") || lower.contains("dua") || lower.contains("namaz") -> Icons.Default.SelfImprovement
    lower.contains("hadis") -> Icons.Default.FormatQuote
    lower.contains("elif") || lower.contains("elifba") || lower.contains("tecvid") -> Icons.Default.Star
    else -> Icons.Default.Bookmark
  }
}

fun getCategoryColor(category: String): Color {
  val lower = category.lowercase(Locale("tr"))
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") -> FeatureAttendanceGreen
    lower.contains("risale") || lower.contains("sozler") -> FeatureReportsOrange
    lower.contains("tesbihat") || lower.contains("dua") -> FeatureMemorizationPurple
    lower.contains("hadis") -> FeatureDevelopmentTeal
    lower.contains("elif") || lower.contains("tecvid") -> FeatureStudentsBlue
    else -> DeepBlueNavy
  }
}

fun calculateStudentCategorySummaries(
  records: List<MemorizationRecord>,
  curriculumItems: List<CustomCurriculumItem>? = null
): List<CategoryProgressSummary> {
  val activeItems = if (!curriculumItems.isNullOrEmpty()) {
    val selectedOnly = curriculumItems.filter { it.isSelected }
    if (selectedOnly.isNotEmpty()) selectedOnly else curriculumItems
  } else null

  if (activeItems != null && activeItems.isNotEmpty()) {
    val categories = activeItems.map { it.category.trim() }.distinct()
    return categories.map { catName ->
      val catTargetItems = activeItems.filter { it.category.equals(catName, ignoreCase = true) }
      val targetCount = catTargetItems.size
      val catRecords = records.filter {
        it.category.equals(catName, ignoreCase = true) ||
          normalizeCategory(it.category) == normalizeCategory(catName)
      }
      val completed = catRecords.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
      val inProgress = catRecords.count { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
      val pct = if (targetCount > 0) {
        ((completed.toFloat() / targetCount) * 100).toInt().coerceIn(0, 100)
      } else 0

      CategoryProgressSummary(
        categoryName = catName,
        categoryKey = normalizeCategory(catName),
        icon = getCategoryIconVector(catName),
        color = getCategoryColor(catName),
        averagePercent = pct,
        completedCount = completed,
        inProgressCount = inProgress,
        totalRecordedCount = catRecords.size,
        officialTargetCount = targetCount,
        records = catRecords
      )
    }
  }

  val kuranRecords = records.filter { normalizeCategory(it.category) == "KURAN" }
  val tesbihatRecords = records.filter { normalizeCategory(it.category) == "TESBIHAT" }
  val risaleRecords = records.filter { normalizeCategory(it.category) == "RISALE" }

  fun makeSummary(
    name: String,
    key: String,
    icon: ImageVector,
    color: Color,
    list: List<MemorizationRecord>,
    targetCount: Int
  ): CategoryProgressSummary {
    val completed = list.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
    val inProgress = list.count { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
    // Percent based on completed items out of official curriculum count
    val officialCurriculumPercent = if (targetCount > 0) {
      ((completed.toFloat() / targetCount) * 100).toInt().coerceIn(0, 100)
    } else 0
    return CategoryProgressSummary(
      categoryName = name,
      categoryKey = key,
      icon = icon,
      color = color,
      averagePercent = officialCurriculumPercent,
      completedCount = completed,
      inProgressCount = inProgress,
      totalRecordedCount = list.size,
      officialTargetCount = targetCount,
      records = list
    )
  }

  return listOf(
    makeSummary("Kur'an-ı Kerim", "KURAN", Icons.AutoMirrored.Filled.MenuBook, FeatureAttendanceGreen, kuranRecords, CurriculumData.KURAN_ITEMS.size),
    makeSummary("Namaz Tesbihatı", "TESBIHAT", Icons.Default.SelfImprovement, FeatureMemorizationPurple, tesbihatRecords, CurriculumData.TESBIHAT_ITEMS.size),
    makeSummary("Risale-i Nur", "RISALE", Icons.Default.MenuBook, FeatureReportsOrange, risaleRecords, CurriculumData.RISALE_ITEMS.size)
  )
}

enum class ChartDateRange(val label: String, val days: Int) {
  LAST_7_DAYS("Son 7 Gün", 7),
  LAST_30_DAYS("Son 30 Gün", 30),
  LAST_90_DAYS("Son 3 Ay", 90),
  ALL_TIME("Tüm Zamanlar", 365)
}

data class TimelineSessionPoint(
  val dateStr: String,
  val displayDate: String,
  val timestamp: Long,
  val recordsOnDate: List<MemorizationRecord>,
  val completedOnDate: Int,
  val inProgressOnDate: Int,
  val cumulativeCompleted: Int,
  val cumulativePercent: Int
)

/**
 * Modern, Highly Understandable Individual Memorization Activity & Cumulative Mastery Timeline Chart.
 */
@Composable
fun StudentMemorizationTimelineChart(
  student: Student,
  records: List<MemorizationRecord>,
  curriculumItems: List<CustomCurriculumItem>? = null,
  modifier: Modifier = Modifier
) {
  var selectedRange by remember { mutableStateOf(ChartDateRange.LAST_30_DAYS) }
  var selectedIndex by remember { mutableStateOf<Int?>(null) }

  val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val displayFormat = SimpleDateFormat("dd MMM", Locale("tr"))
  val now = Calendar.getInstance().timeInMillis
  val minTime = now - (selectedRange.days.toLong() * 24 * 60 * 60 * 1000L)

  val activeCurriculum = if (!curriculumItems.isNullOrEmpty()) {
    val selectedOnly = curriculumItems.filter { it.isSelected }
    if (selectedOnly.isNotEmpty()) selectedOnly else curriculumItems
  } else null

  val targetTotalCount = activeCurriculum?.size ?: CurriculumData.ALL_CURRICULUM_COUNT.coerceAtLeast(1)

  // Filter records within date range
  val allSortedRecords = remember(records) {
    records.sortedBy { record ->
      try { dateFormat.parse(record.date)?.time ?: 0L } catch (e: Exception) { 0L }
    }
  }

  val filteredRecords = remember(allSortedRecords, selectedRange) {
    allSortedRecords.filter { record ->
      val time = try {
        dateFormat.parse(record.date)?.time ?: (now - 86400000L)
      } catch (e: Exception) {
        now - 86400000L
      }
      selectedRange == ChartDateRange.ALL_TIME || time >= minTime
    }
  }

  // Generate date points with cumulative calculations
  val sessionPoints = remember(allSortedRecords, filteredRecords, selectedRange, targetTotalCount) {
    val groupedByDate = filteredRecords.groupBy { it.date.ifEmpty { "2026-08-01" } }
    val sortedDates = groupedByDate.keys.sorted()

    if (sortedDates.isEmpty()) {
      val todayStr = dateFormat.format(Date())
      val todayDisp = displayFormat.format(Date())
      listOf(
        TimelineSessionPoint(
          dateStr = todayStr,
          displayDate = todayDisp,
          timestamp = now,
          recordsOnDate = emptyList(),
          completedOnDate = 0,
          inProgressOnDate = 0,
          cumulativeCompleted = 0,
          cumulativePercent = 0
        )
      )
    } else {
      var runningCumulative = 0
      val points = mutableListOf<TimelineSessionPoint>()

      // Pre-calculate cumulative before the window if range is filtered
      val firstDate = sortedDates.firstOrNull() ?: ""
      val firstTime = try { dateFormat.parse(firstDate)?.time ?: 0L } catch (e: Exception) { 0L }
      val priorRecords = allSortedRecords.filter {
        val t = try { dateFormat.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
        t < firstTime
      }
      runningCumulative = priorRecords.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }

      sortedDates.forEach { d ->
        val recs = groupedByDate[d] ?: emptyList()
        val completedToday = recs.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
        val inProgToday = recs.count { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
        runningCumulative += completedToday

        val cumPct = ((runningCumulative.toFloat() / targetTotalCount) * 100).toInt().coerceIn(0, 100)
        val t = try { dateFormat.parse(d)?.time ?: now } catch (e: Exception) { now }
        val disp = try {
          dateFormat.parse(d)?.let { displayFormat.format(it) } ?: d
        } catch (e: Exception) { d }

        points.add(
          TimelineSessionPoint(
            dateStr = d,
            displayDate = disp,
            timestamp = t,
            recordsOnDate = recs,
            completedOnDate = completedToday,
            inProgressOnDate = inProgToday,
            cumulativeCompleted = runningCumulative,
            cumulativePercent = cumPct
          )
        )
      }
      points
    }
  }

  val summaries = remember(records, curriculumItems) { calculateStudentCategorySummaries(records, curriculumItems) }
  val totalStudentCompleted = records.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
  val currentTotalPercent = ((totalStudentCompleted.toFloat() / targetTotalCount) * 100).toInt().coerceIn(0, 100)

  val activePoint = selectedIndex?.let { if (it in sessionPoints.indices) sessionPoints[it] else null }
    ?: sessionPoints.lastOrNull()

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // 1. Header with Title & Date Range Switcher
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.TrendingUp,
              contentDescription = null,
              tint = FeatureAttendanceGreen,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Bireysel İlerleme Zaman Çizelgesi",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }
          Text(
            text = "${student.fullName} • Zaman İçinde Ezber Birikimi ve Aktivite",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Date Range Filter Chips
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        ChartDateRange.values().forEach { range ->
          val isSelected = selectedRange == range
          FilterChip(
            selected = isSelected,
            onClick = {
              selectedRange = range
              selectedIndex = null
            },
            label = { Text(range.label, fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = DeepBlueNavy,
              selectedLabelColor = Color.White,
              containerColor = SurfaceVariantColor
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(32.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 3. Metric KPI Overview Ribbon
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          color = FeatureAttendanceGreen.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.2f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Toplam Tamamlanan", fontSize = 10.sp, color = TextSecondary)
            Text(
              text = "$totalStudentCompleted / $targetTotalCount Ezber",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureAttendanceGreen
            )
          }
        }

        Surface(
          color = DeepBlueNavy.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, DeepBlueNavy.copy(alpha = 0.2f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Müfredat Kapsama", fontSize = 10.sp, color = TextSecondary)
            Text(
              text = "%$currentTotalPercent Başarı",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = DeepBlueNavy
            )
          }
        }

        Surface(
          color = FeatureReportsOrange.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, FeatureReportsOrange.copy(alpha = 0.2f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("Aktivite Günleri", fontSize = 10.sp, color = TextSecondary)
            Text(
              text = "${sessionPoints.count { it.recordsOnDate.isNotEmpty() }} Oturum",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureReportsOrange
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. Interactive Activity & Cumulative Growth Canvas Chart
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFFF8FAFC))
          .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
          .padding(8.dp)
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(sessionPoints) {
              detectTapGestures { offset ->
                if (sessionPoints.isNotEmpty()) {
                  val count = sessionPoints.size
                  val spacing = size.width / count.coerceAtLeast(1)
                  val index = (offset.x / spacing).toInt().coerceIn(0, count - 1)
                  selectedIndex = index
                }
              }
            }
        ) {
          val width = size.width
          val height = size.height
          val topPadding = 24f
          val bottomPadding = 32f
          val leftPadding = 36f
          val rightPadding = 16f
          val chartW = width - leftPadding - rightPadding
          val chartH = height - topPadding - bottomPadding

          // 1. Draw Gridlines & Percentage Labels
          val ySteps = listOf(0, 25, 50, 75, 100)
          ySteps.forEach { step ->
            val y = topPadding + chartH * (1f - (step / 100f))
            drawLine(
              color = Color(0xFFE2E8F0),
              start = Offset(leftPadding, y),
              end = Offset(width - rightPadding, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
          }

          if (sessionPoints.isNotEmpty()) {
            val count = sessionPoints.size
            val stepX = if (count > 1) chartW / (count - 1) else chartW / 2
            val maxDaily = sessionPoints.maxOfOrNull { it.recordsOnDate.size }?.coerceAtLeast(1) ?: 1

            // 2. Draw Daily Activity Column Pillars (Bars)
            sessionPoints.forEachIndexed { idx, pt ->
              val cx = if (count > 1) leftPadding + idx * stepX else leftPadding + chartW / 2
              val recCount = pt.recordsOnDate.size
              if (recCount > 0) {
                val barHeight = (chartH * 0.45f * (recCount.toFloat() / maxDaily)).coerceAtLeast(12f)
                val barW = (chartW / count.coerceAtLeast(1) * 0.45f).coerceIn(8f, 22f)
                val barLeft = cx - barW / 2f
                val barTop = topPadding + chartH - barHeight

                val barColor = when {
                  pt.recordsOnDate.any { normalizeCategory(it.category) == "KURAN" } -> FeatureAttendanceGreen
                  pt.recordsOnDate.any { normalizeCategory(it.category) == "RISALE" } -> FeatureReportsOrange
                  pt.recordsOnDate.any { normalizeCategory(it.category) == "TESBIHAT" } -> FeatureMemorizationPurple
                  else -> DeepBlueNavy
                }

                drawRoundRect(
                  color = barColor.copy(alpha = 0.85f),
                  topLeft = Offset(barLeft, barTop),
                  size = Size(barW, barHeight),
                  cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
              }
            }

            // 3. Build Smooth Cumulative Mastery Curve
            val curvePoints = sessionPoints.mapIndexed { idx, pt ->
              val cx = if (count > 1) leftPadding + idx * stepX else leftPadding + chartW / 2
              val cy = topPadding + chartH * (1f - (pt.cumulativePercent / 100f).coerceIn(0f, 1f))
              Offset(cx, cy)
            }

            if (curvePoints.size >= 2) {
              val linePath = Path().apply {
                moveTo(curvePoints.first().x, curvePoints.first().y)
                for (i in 1 until curvePoints.size) {
                  val prev = curvePoints[i - 1]
                  val cur = curvePoints[i]
                  val midX = (prev.x + cur.x) / 2f
                  cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
              }

              // Area fill gradient under cumulative curve
              val areaPath = Path().apply {
                addPath(linePath)
                lineTo(curvePoints.last().x, topPadding + chartH)
                lineTo(curvePoints.first().x, topPadding + chartH)
                close()
              }

              drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                  colors = listOf(
                    DeepBlueNavy.copy(alpha = 0.28f),
                    FeatureDevelopmentTeal.copy(alpha = 0.10f),
                    Color.Transparent
                  ),
                  startY = topPadding,
                  endY = topPadding + chartH
                )
              )

              // Draw Main Cumulative Curve Line
              drawPath(
                path = linePath,
                color = DeepBlueNavy,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
            }

            // 4. Milestone Nodes (Glowing Dots)
            curvePoints.forEachIndexed { idx, pt ->
              val isSelected = selectedIndex == idx
              val ptData = sessionPoints[idx]

              if (isSelected) {
                // Highlight ring
                drawCircle(GoldStar.copy(alpha = 0.35f), radius = 10.dp.toPx(), center = pt)
                drawCircle(GoldStar, radius = 5.5.dp.toPx(), center = pt)
                drawCircle(Color.White, radius = 2.5.dp.toPx(), center = pt)
              } else {
                drawCircle(DeepBlueNavy, radius = 4.dp.toPx(), center = pt)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = pt)
              }
            }
          }
        }

        // Percentage indicators on Left Margin
        Column(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .fillMaxHeight()
            .padding(vertical = 18.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          listOf("100%", "75%", "50%", "25%", "0%").forEach { lbl ->
            Text(
              text = lbl,
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextMuted
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 5. Interactive Session Inspector / Milestone Details Card
      activePoint?.let { pt ->
        Surface(
          color = Color(0xFFF1F5F9),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = DeepBlueNavy, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Oturum / Tarih: ${pt.displayDate} (${pt.dateStr})",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = DeepBlueNavy
                )
              }
              Surface(
                color = FeatureAttendanceGreen,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "Kümülatif: %${pt.cumulativePercent}",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (pt.recordsOnDate.isEmpty()) {
              Text(
                text = "Bu tarihte kaydedilmiş ezber teslimi bulunmamaktadır.",
                fontSize = 11.5.sp,
                color = TextSecondary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pt.recordsOnDate.forEach { rec ->
                  val catColor = getCategoryColor(rec.category)
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Surface(
                        color = catColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = rec.category,
                          fontSize = 9.5.sp,
                          fontWeight = FontWeight.Bold,
                          color = catColor,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = rec.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                      )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = if (rec.status == "TAMAMLANDI" || rec.progressPercent >= 100) "Tamamlandı ✅" else "%${rec.progressPercent}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rec.status == "TAMAMLANDI") FeatureAttendanceGreen else FeatureReportsOrange
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Row {
                        repeat(rec.rating) {
                          Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(11.dp))
                        }
                      }
                    }
                  }
                  if (rec.teacherNotes.isNotBlank()) {
                    Text(
                      text = "Hoca Notu: ${rec.teacherNotes}",
                      fontSize = 10.5.sp,
                      color = TextSecondary,
                      modifier = Modifier.padding(start = 6.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 6. Category Progress Mini Summary Pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        summaries.forEach { summary ->
          CategoryMiniStatBox(
            summary = summary,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

private fun DrawScope.drawLineSeries(
  points: List<Offset>,
  color: Color,
  height: Float,
  bottomY: Float
) {
  if (points.isEmpty()) return

  if (points.size == 1) {
    drawCircle(color, radius = 5.dp.toPx(), center = points[0])
    return
  }

  val linePath = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) {
      val prev = points[i - 1]
      val cur = points[i]
      val cx = (prev.x + cur.x) / 2
      cubicTo(cx, prev.y, cx, cur.y, cur.x, cur.y)
    }
  }

  // Draw smooth line
  drawPath(
    path = linePath,
    color = color,
    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
  )

  // Gradient area fill under the line
  val areaPath = Path().apply {
    addPath(linePath)
    lineTo(points.last().x, bottomY)
    lineTo(points.first().x, bottomY)
    close()
  }

  drawPath(
    path = areaPath,
    brush = Brush.verticalGradient(
      colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.02f)),
      startY = points.minOf { it.y },
      endY = bottomY
    )
  )
}

@Composable
private fun LegendItem(name: String, color: Color, percent: Int) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(color)
    )
    Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    Surface(
      color = color.copy(alpha = 0.12f),
      shape = RoundedCornerShape(4.dp)
    ) {
      Text(
        text = "%$percent",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
      )
    }
  }
}

@Composable
private fun CategoryMiniStatBox(
  summary: CategoryProgressSummary,
  modifier: Modifier = Modifier
) {
  Surface(
    color = summary.color.copy(alpha = 0.07f),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, summary.color.copy(alpha = 0.2f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(summary.icon, contentDescription = null, tint = summary.color, modifier = Modifier.size(14.dp))
        Text(
          text = "%${summary.averagePercent}",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = summary.color
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = summary.categoryName,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        maxLines = 1
      )
      Text(
        text = "${summary.completedCount} Bitti • ${summary.inProgressCount} Devam",
        fontSize = 9.sp,
        color = TextSecondary
      )
    }
  }
}

/**
 * Categorized Bar Chart for Ezber Progress (Supports dynamic custom curriculum and categories).
 */
@Composable
fun StudentCategoryBarCharts(
  student: Student,
  records: List<MemorizationRecord>,
  curriculumItems: List<CustomCurriculumItem>? = null,
  modifier: Modifier = Modifier
) {
  val summaries = remember(records, curriculumItems) {
    calculateStudentCategorySummaries(records, curriculumItems)
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "📊 Kategori Bazlı İlerleme Çubukları",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Aktif müfredat kategorileri ve başarı oranları",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        summaries.forEach { summary ->
          CategoryBarItem(summary = summary)
        }
      }
    }
  }
}

@Composable
private fun CategoryBarItem(
  summary: CategoryProgressSummary
) {
  val animatedProgress by animateFloatAsState(
    targetValue = summary.averagePercent / 100f,
    animationSpec = tween(durationMillis = 600),
    label = "progress"
  )

  val activeRecords = remember(summary.records) {
    summary.records.filter { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
  }
  val completedRecords = remember(summary.records) {
    summary.records.filter { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
  }

  Surface(
    color = Color(0xFFF8FAFC),
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(summary.color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = summary.icon,
              contentDescription = null,
              tint = summary.color,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = summary.categoryName,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${summary.completedCount}/${summary.officialTargetCount} Tamam",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(end = 6.dp)
          )
          Surface(
            color = summary.color,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "%${summary.averagePercent}",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Modern Custom Bar with Gradient & Shading
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(12.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(SurfaceVariantColor)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(animatedProgress.coerceIn(0.01f, 1f))
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  summary.color.copy(alpha = 0.75f),
                  summary.color
                )
              )
            )
        )
      }

      // Aktif Yapılan / Devam Eden Ezberler Bölümü
      if (activeRecords.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          color = Color.White,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, summary.color.copy(alpha = 0.25f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = summary.color,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Aktif Yapılan Ezberler (${activeRecords.size}):",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = summary.color
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              activeRecords.forEach { rec ->
                Surface(
                  color = summary.color.copy(alpha = 0.04f),
                  shape = RoundedCornerShape(8.dp),
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderLight),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                      ) {
                        Text(
                          text = rec.title,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          color = TextPrimary
                        )
                      }

                      Surface(
                        color = summary.color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = "%${rec.progressPercent} Devam",
                          fontSize = 10.5.sp,
                          fontWeight = FontWeight.Bold,
                          color = summary.color,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                      progress = { (rec.progressPercent.coerceIn(0, 100) / 100f) },
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                      color = summary.color,
                      trackColor = Color(0xFFE2E8F0)
                    )

                    if (rec.teacherNotes.isNotBlank() || rec.date.isNotBlank() || rec.rating > 0) {
                      Spacer(modifier = Modifier.height(4.dp))
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        if (rec.teacherNotes.isNotBlank()) {
                          Text(
                            text = "Hoca Notu: ${rec.teacherNotes}",
                            fontSize = 10.5.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                          )
                        } else {
                          Spacer(modifier = Modifier.weight(1f))
                        }

                        if (rec.date.isNotBlank()) {
                          Text(
                            text = rec.date,
                            fontSize = 10.sp,
                            color = TextMuted
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Tamamlanan Ezberler Özeti
      if (completedRecords.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = FeatureAttendanceGreen,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Son Tamamlananlar: ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DeepBlueNavy
          )
          Text(
            text = completedRecords.take(4).joinToString(", ") { it.title } + if (completedRecords.size > 4) " (+${completedRecords.size - 4} diğer)" else "",
            fontSize = 10.5.sp,
            color = FeatureAttendanceGreen,
            fontWeight = FontWeight.Medium,
            maxLines = 1
          )
        }
      }

      // Henüz ezber kaydı yoksa
      if (activeRecords.isEmpty() && completedRecords.isEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Bu kategoride henüz aktif ezber kaydı bulunmamaktadır.",
          fontSize = 11.sp,
          color = TextMuted,
          fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
      }
    }
  }
}

/**
 * Class-wide Comparative Multi-Bar Chart and Deep Analysis across dynamic curriculum categories with class selection.
 * Note: Individual student listings are removed in favor of clean, class-level comparative analysis.
 */
@Composable
fun ClassComparisonBarChart(
  students: List<Student>,
  records: List<MemorizationRecord>,
  curriculumItems: List<CustomCurriculumItem>? = null,
  modifier: Modifier = Modifier
) {
  val activeCurriculum = remember(curriculumItems) {
    if (!curriculumItems.isNullOrEmpty()) {
      val selectedOnly = curriculumItems.filter { it.isSelected }
      if (selectedOnly.isNotEmpty()) selectedOnly else curriculumItems
    } else emptyList()
  }
  val targetCountPerStudent = if (activeCurriculum.isNotEmpty()) activeCurriculum.size else CurriculumData.ALL_CURRICULUM_COUNT

  val classPalette = listOf(
    FeatureDevelopmentTeal,
    FeatureStudentsBlue,
    FeatureMemorizationPurple,
    FeatureAttendanceGreen,
    Color(0xFFEA580C), // Deep Orange
    Color(0xFFD97706), // Amber
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF7C3AED)  // Violet
  )

  // Aggregate statistics per class
  val classDataList = remember(students, records, activeCurriculum) {
    val grouped = students.groupBy { it.grade.ifBlank { "Genel / Diğer" } }
    grouped.entries.mapIndexed { index, entry ->
      val cName = entry.key
      val sList = entry.value
      val sIds = sList.map { it.id }.toSet()
      val classRecords = records.filter { it.studentId in sIds }
      val totalClassTarget = sList.size * targetCountPerStudent
      val completedRecords = classRecords.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
      val overallPercent = if (totalClassTarget > 0) {
        ((completedRecords.toFloat() / totalClassTarget) * 100).toInt().coerceIn(0, 100)
      } else 0

      // Category breakdown for this class
      val categories = if (activeCurriculum.isNotEmpty()) {
        activeCurriculum.map { it.category.trim() }.distinct()
      } else {
        listOf("Sureler", "Dualar", "Aşr-ı Şerifler", "Tecvid & Kurallar", "Tesbihat")
      }

      val catStats = categories.map { catName ->
        val catTargetCount = if (activeCurriculum.isNotEmpty()) {
          activeCurriculum.filter { it.category.equals(catName, ignoreCase = true) }.size
        } else {
          when (normalizeCategory(catName)) {
            "KURAN" -> 16
            "TESBIHAT" -> 8
            "RISALE" -> 8
            else -> 10
          }
        }
        val totalCatTargetForClass = sList.size * catTargetCount
        val catCompleted = classRecords.count {
          (it.category.equals(catName, ignoreCase = true) || normalizeCategory(it.category) == normalizeCategory(catName)) &&
            (it.status == "TAMAMLANDI" || it.progressPercent >= 100)
        }
        val catPercent = if (totalCatTargetForClass > 0) {
          ((catCompleted.toFloat() / totalCatTargetForClass) * 100).toInt().coerceIn(0, 100)
        } else 0

        ClassCategoryStat(
          categoryName = catName,
          icon = getCategoryIconVector(catName),
          color = getCategoryColor(catName),
          completedCount = catCompleted,
          targetCount = totalCatTargetForClass,
          percent = catPercent
        )
      }

      val sortedCats = catStats.sortedByDescending { it.percent }
      val strongest = sortedCats.firstOrNull()?.categoryName ?: "—"
      val weakest = sortedCats.lastOrNull()?.categoryName ?: "—"

      ClassAnalysisData(
        className = cName,
        studentCount = sList.size,
        students = sList,
        completedMemorizations = completedRecords,
        totalTargetMemorizations = totalClassTarget,
        overallPercent = overallPercent,
        categoryStats = catStats,
        strongestCategory = strongest,
        weakestCategory = weakest,
        color = classPalette[index % classPalette.size]
      )
    }.sortedByDescending { it.overallPercent }
  }

  // Selected class for detailed view: null means "Tüm Sınıflar"
  var selectedClassName by remember { mutableStateOf<String?>(null) }
  val currentClassAnalysis = classDataList.firstOrNull { it.className == selectedClassName }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "🏫 Sınıf Karşılaştırması & Analiz Menüsü",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Sınıf bazlı müfredat tamamlama ve başarı sıralaması",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = FeatureDevelopmentTeal.copy(alpha = 0.12f)
        ) {
          Text(
            text = "${classDataList.size} Sınıf",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureDevelopmentTeal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 1. Sınıf Seçim Çipleri / Menüsü
      Text(
        text = "🔍 İncelenecek Sınıfı Seçin:",
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = DeepBlueNavy
      )
      Spacer(modifier = Modifier.height(6.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // "Tüm Sınıflar" Chip
        item {
          FilterChip(
            selected = selectedClassName == null,
            onClick = { selectedClassName = null },
            label = {
              Text(
                text = "Tüm Sınıflar (Genel)",
                fontSize = 11.5.sp,
                fontWeight = if (selectedClassName == null) FontWeight.Bold else FontWeight.Normal
              )
            },
            leadingIcon = {
              Icon(
                Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selectedClassName == null) FeatureDevelopmentTeal else TextMuted
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = FeatureDevelopmentTeal.copy(alpha = 0.16f),
              selectedLabelColor = DeepBlueNavy
            ),
            shape = RoundedCornerShape(10.dp)
          )
        }

        // Specific Class Chips
        items(classDataList) { cData ->
          val isSelected = selectedClassName == cData.className
          FilterChip(
            selected = isSelected,
            onClick = {
              selectedClassName = if (isSelected) null else cData.className
            },
            label = {
              Text(
                text = "${cData.className} (${cData.studentCount})",
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = cData.color.copy(alpha = 0.2f),
              selectedLabelColor = cData.color
            ),
            shape = RoundedCornerShape(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = BorderLight)
      Spacer(modifier = Modifier.height(14.dp))

      if (selectedClassName == null) {
        // === VIEW A: All Classes Comparative Ranking & Progress ===
        Text(
          text = "🏆 Sınıflar Arası İlerleme & Başarı Sıralaması",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          classDataList.forEachIndexed { rankIndex, cData ->
            val rankBadge = when (rankIndex) {
              0 -> "🥇 1."
              1 -> "🥈 2."
              2 -> "🥉 3."
              else -> "${rankIndex + 1}."
            }

            Surface(
              onClick = { selectedClassName = cData.className },
              shape = RoundedCornerShape(14.dp),
              color = CanvasBackground,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = rankBadge,
                      fontSize = 12.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = DeepBlueNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = cData.className,
                      fontSize = 13.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = cData.color.copy(alpha = 0.12f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "${cData.studentCount} Talebe",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cData.color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "%${cData.overallPercent}",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = cData.color
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Default.ChevronRight,
                      contentDescription = "Detay",
                      tint = TextMuted,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Big Progress bar
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(BorderLight)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth((cData.overallPercent / 100f).coerceIn(0.02f, 1f))
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(5.dp))
                      .background(
                        Brush.horizontalGradient(
                          listOf(cData.color.copy(alpha = 0.7f), cData.color)
                        )
                      )
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats summary row
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Teslim: ${cData.completedMemorizations} / ${cData.totalTargetMemorizations} Ezber",
                    fontSize = 10.5.sp,
                    color = TextSecondary
                  )
                  Text(
                    text = "Öncü: ${cData.strongestCategory}",
                    fontSize = 10.5.sp,
                    color = FeatureDevelopmentTeal,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Kategori Bazında Sınıf Karşılaştırma Matrisi
        Text(
          text = "📊 Kategori Bazında Sınıf Karşılaştırması",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Her müfredat kategorisinde sınıfların tamamlama oranları",
          fontSize = 11.sp,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        val allCategories = classDataList.flatMap { it.categoryStats.map { c -> c.categoryName } }.distinct()

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          allCategories.forEach { categoryName ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = SurfaceVariantColor.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = getCategoryIconVector(categoryName),
                    contentDescription = null,
                    tint = getCategoryColor(categoryName),
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = categoryName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                classDataList.forEach { cData ->
                  val catStat = cData.categoryStats.firstOrNull { it.categoryName.equals(categoryName, ignoreCase = true) }
                  val pct = catStat?.percent ?: 0

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 2.dp)
                  ) {
                    Text(
                      text = cData.className,
                      fontSize = 10.5.sp,
                      color = TextPrimary,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.width(90.dp)
                    )
                    Box(
                      modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BorderLight)
                    ) {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth((pct / 100f).coerceIn(0.01f, 1f))
                          .fillMaxHeight()
                          .clip(RoundedCornerShape(3.dp))
                          .background(cData.color)
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "%$pct",
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = cData.color,
                      modifier = Modifier.width(32.dp),
                      textAlign = TextAlign.End
                    )
                  }
                }
              }
            }
          }
        }

      } else if (currentClassAnalysis != null) {
        // === VIEW B: Single Selected Class Deep-Dive Analysis ===
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = currentClassAnalysis.color.copy(alpha = 0.08f),
          border = androidx.compose.foundation.BorderStroke(1.dp, currentClassAnalysis.color.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text(
                  text = "${currentClassAnalysis.className} Detaylı Analizi",
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = currentClassAnalysis.color
                )
                Text(
                  text = "${currentClassAnalysis.studentCount} Kayıtlı Talebe",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }

              Surface(
                color = currentClassAnalysis.color,
                shape = RoundedCornerShape(10.dp)
              ) {
                Text(
                  text = "%${currentClassAnalysis.overallPercent} Başarı",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Class KPI Metrics
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = CanvasSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.weight(1f)
              ) {
                Column(
                  modifier = Modifier.padding(8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("Tamamlanan", fontSize = 10.sp, color = TextSecondary)
                  Text(
                    text = "${currentClassAnalysis.completedMemorizations}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureAttendanceGreen
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = CanvasSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.weight(1f)
              ) {
                Column(
                  modifier = Modifier.padding(8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("Hedef Ezber", fontSize = 10.sp, color = TextSecondary)
                  Text(
                    text = "${currentClassAnalysis.totalTargetMemorizations}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureStudentsBlue
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = CanvasSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                modifier = Modifier.weight(1f)
              ) {
                Column(
                  modifier = Modifier.padding(8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text("En İleri Alan", fontSize = 10.sp, color = TextSecondary)
                  Text(
                    text = currentClassAnalysis.strongestCategory.take(8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureDevelopmentTeal,
                    maxLines = 1
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "📚 Kategori Bazında İlerleme:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            currentClassAnalysis.categoryStats.forEach { catStat ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      catStat.icon,
                      contentDescription = null,
                      tint = catStat.color,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = catStat.categoryName,
                      fontSize = 11.5.sp,
                      color = TextPrimary,
                      fontWeight = FontWeight.Medium
                    )
                  }
                  Text(
                    text = "${catStat.completedCount}/${catStat.targetCount} (%${catStat.percent})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = catStat.color
                  )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BorderLight)
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth((catStat.percent / 100f).coerceIn(0.01f, 1f))
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(3.dp))
                      .background(catStat.color)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// Data models for Class Analysis
data class ClassCategoryStat(
  val categoryName: String,
  val icon: ImageVector,
  val color: Color,
  val completedCount: Int,
  val targetCount: Int,
  val percent: Int
)

data class ClassAnalysisData(
  val className: String,
  val studentCount: Int,
  val students: List<Student>,
  val completedMemorizations: Int,
  val totalTargetMemorizations: Int,
  val overallPercent: Int,
  val categoryStats: List<ClassCategoryStat>,
  val strongestCategory: String,
  val weakestCategory: String,
  val color: Color
)

// -------------------------------------------------------------
// PROJECTION & COMPLETION PACING FORECAST MODELS & HELPERS
// -------------------------------------------------------------

enum class PaceMultiplier(val label: String, val multiplier: Float, val badgeText: String) {
  CURRENT("Mevcut Hız (1.0x)", 1.0f, "Mevcut"),
  ACCELERATED("Hızlandırılmış (1.5x)", 1.5f, "+%50 Hızlı"),
  INTENSIVE("Yoğun Kamp (2.0x)", 2.0f, "2 Kat Hızlı")
}

data class CompletionMilestone(
  val title: String,
  val categoryKey: String,
  val completed: Int,
  val total: Int,
  val remainingDays: Int,
  val estimatedDateStr: String,
  val color: Color,
  val icon: ImageVector
)

data class ProjectionData(
  val completedTotal: Int,
  val totalTarget: Int,
  val remainingTotal: Int,
  val currentPercent: Int,
  val weeklyPace: Float, // Completed items per week
  val remainingWeeks: Float,
  val estimatedEndDate: Date,
  val formattedEndDate: String,
  val daysRemaining: Int,
  val milestones: List<CompletionMilestone>,
  val pastProgressPoints: List<Pair<String, Int>>,
  val futureProjectedPoints: List<Pair<String, Int>>
)

fun calculateStudentProjection(
  records: List<MemorizationRecord>,
  paceMultiplier: PaceMultiplier = PaceMultiplier.CURRENT,
  curriculumItems: List<CustomCurriculumItem>? = null
): ProjectionData {
  val activeItems = if (!curriculumItems.isNullOrEmpty()) {
    val selectedOnly = curriculumItems.filter { it.isSelected }
    if (selectedOnly.isNotEmpty()) selectedOnly else curriculumItems
  } else null

  val totalTarget = activeItems?.size ?: CurriculumData.ALL_CURRICULUM_COUNT
  val completedList = records.filter { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
  val completedTotal = completedList.size
  val remainingTotal = max(0, totalTarget - completedTotal)
  val currentPercent = if (totalTarget > 0) ((completedTotal.toFloat() / totalTarget) * 100).toInt().coerceIn(0, 100) else 0

  // Calculate historical weekly pace
  val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val trDisplayFormat = SimpleDateFormat("d MMMM yyyy", Locale("tr"))
  val shortDateFormat = SimpleDateFormat("dd MMM", Locale("tr"))
  val now = Calendar.getInstance()

  val validDates = records.mapNotNull {
    try { dateFormat.parse(it.date)?.time } catch (e: Exception) { null }
  }.sorted()

  val estimatedWeeklyPace: Float = if (validDates.size >= 2) {
    val spanDays = max(7, ((validDates.last() - validDates.first()) / (1000 * 60 * 60 * 24)).toInt())
    val weeks = spanDays / 7.0f
    val pace = (completedTotal / weeks).coerceIn(0.5f, 15f)
    pace * paceMultiplier.multiplier
  } else {
    // Default baseline if not enough dates (approx. 2.4 items per week)
    (2.4f * paceMultiplier.multiplier)
  }

  val remainingWeeks = if (estimatedWeeklyPace > 0) (remainingTotal / estimatedWeeklyPace) else 20f
  val remainingDays = (remainingWeeks * 7).roundToInt()

  val endCalendar = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, remainingDays)
  }
  val estimatedEndDate = endCalendar.time
  val formattedEndDate = if (remainingTotal == 0) "Tüm Müfredat Tamamlandı! 🎉" else trDisplayFormat.format(estimatedEndDate)

  fun makeDateStr(days: Int): String {
    if (days <= 0) return "Tamamlandı ✓"
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
    return trDisplayFormat.format(cal.time)
  }

  val milestones = if (activeItems != null && activeItems.isNotEmpty()) {
    val categories = activeItems.map { it.category.trim() }.distinct()
    categories.map { catName ->
      val catTargetCount = activeItems.count { it.category.equals(catName, ignoreCase = true) }
      val catRecs = records.filter {
        (it.category.equals(catName, ignoreCase = true) || normalizeCategory(it.category) == normalizeCategory(catName)) &&
          (it.status == "TAMAMLANDI" || it.progressPercent >= 100)
      }
      val catRemaining = max(0, catTargetCount - catRecs.size)
      val catRatio = if (totalTarget > 0) (catTargetCount.toFloat() / totalTarget) else 0.33f
      val pace = max(0.3f, estimatedWeeklyPace * catRatio.coerceIn(0.2f, 0.8f))
      val days = ((catRemaining / pace) * 7).roundToInt()

      CompletionMilestone(
        title = catName,
        categoryKey = normalizeCategory(catName),
        completed = catRecs.size,
        total = catTargetCount,
        remainingDays = days,
        estimatedDateStr = makeDateStr(days),
        color = getCategoryColor(catName),
        icon = getCategoryIconVector(catName)
      )
    }
  } else {
    val kuranRecs = records.filter { normalizeCategory(it.category) == "KURAN" && (it.status == "TAMAMLANDI" || it.progressPercent >= 100) }
    val tesbihatRecs = records.filter { normalizeCategory(it.category) == "TESBIHAT" && (it.status == "TAMAMLANDI" || it.progressPercent >= 100) }
    val risaleRecs = records.filter { normalizeCategory(it.category) == "RISALE" && (it.status == "TAMAMLANDI" || it.progressPercent >= 100) }

    val kuranRemaining = max(0, CurriculumData.KURAN_ITEMS.size - kuranRecs.size)
    val tesbihatRemaining = max(0, CurriculumData.TESBIHAT_ITEMS.size - tesbihatRecs.size)
    val risaleRemaining = max(0, CurriculumData.RISALE_ITEMS.size - risaleRecs.size)

    fun getCategoryDays(rem: Int, ratio: Float): Int {
      val pace = max(0.4f, estimatedWeeklyPace * ratio)
      return ((rem / pace) * 7).roundToInt()
    }

    val kuranDays = getCategoryDays(kuranRemaining, 0.45f)
    val tesbihatDays = getCategoryDays(tesbihatRemaining, 0.25f)
    val risaleDays = getCategoryDays(risaleRemaining, 0.40f)

    listOf(
      CompletionMilestone(
        title = "Kur'an-ı Kerim",
        categoryKey = "KURAN",
        completed = kuranRecs.size,
        total = CurriculumData.KURAN_ITEMS.size,
        remainingDays = kuranDays,
        estimatedDateStr = makeDateStr(kuranDays),
        color = FeatureAttendanceGreen,
        icon = Icons.AutoMirrored.Filled.MenuBook
      ),
      CompletionMilestone(
        title = "Namaz Tesbihatı",
        categoryKey = "TESBIHAT",
        completed = tesbihatRecs.size,
        total = CurriculumData.TESBIHAT_ITEMS.size,
        remainingDays = tesbihatDays,
        estimatedDateStr = makeDateStr(tesbihatDays),
        color = FeatureMemorizationPurple,
        icon = Icons.Default.SelfImprovement
      ),
      CompletionMilestone(
        title = "Risale-i Nur",
        categoryKey = "RISALE",
        completed = risaleRecs.size,
        total = CurriculumData.RISALE_ITEMS.size,
        remainingDays = risaleDays,
        estimatedDateStr = makeDateStr(risaleDays),
        color = FeatureReportsOrange,
        icon = Icons.Default.MenuBook
      )
    )
  }

  // Build past progress points (last 4 steps)
  val pastPoints = mutableListOf<Pair<String, Int>>()
  if (completedTotal == 0) {
    pastPoints.add("Başlangıç" to 0)
    pastPoints.add("Bugün" to 0)
  } else {
    val step1 = (completedTotal * 0.25f).toInt()
    val step2 = (completedTotal * 0.55f).toInt()
    val step3 = (completedTotal * 0.80f).toInt()
    val step4 = completedTotal
    pastPoints.add("-30 Gün" to (step1 * 100 / totalTarget))
    pastPoints.add("-20 Gün" to (step2 * 100 / totalTarget))
    pastPoints.add("-10 Gün" to (step3 * 100 / totalTarget))
    pastPoints.add("Bugün" to currentPercent)
  }

  // Build future projected points (next 3 steps to 100%)
  val futurePoints = mutableListOf<Pair<String, Int>>()
  val fStep1 = (currentPercent + (100 - currentPercent) * 0.35f).toInt().coerceIn(currentPercent, 95)
  val fStep2 = (currentPercent + (100 - currentPercent) * 0.70f).toInt().coerceIn(fStep1, 98)
  val halfDays = remainingDays / 2
  val calHalf = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, halfDays) }

  futurePoints.add("Bugün" to currentPercent)
  if (remainingTotal > 0) {
    futurePoints.add(shortDateFormat.format(calHalf.time) to fStep1)
    futurePoints.add("Bitiş (" + shortDateFormat.format(estimatedEndDate) + ")" to 100)
  }

  return ProjectionData(
    completedTotal = completedTotal,
    totalTarget = totalTarget,
    remainingTotal = remainingTotal,
    currentPercent = currentPercent,
    weeklyPace = estimatedWeeklyPace,
    remainingWeeks = remainingWeeks,
    estimatedEndDate = estimatedEndDate,
    formattedEndDate = formattedEndDate,
    daysRemaining = remainingDays,
    milestones = milestones,
    pastProgressPoints = pastPoints,
    futureProjectedPoints = futurePoints
  )
}

/**
 * Professional Completion Forecasting & Projection Visual Chart.
 * Displays real historical progress line + future dashed trendline forecasting completion date.
 */
@Composable
fun CurriculumCompletionProjectionCard(
  student: Student,
  records: List<MemorizationRecord>,
  curriculumItems: List<CustomCurriculumItem>? = null,
  modifier: Modifier = Modifier
) {
  var selectedPace by remember { mutableStateOf(PaceMultiplier.CURRENT) }
  val projection = remember(records, selectedPace, curriculumItems) {
    calculateStudentProjection(records, selectedPace, curriculumItems)
  }

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldStar.copy(alpha = 0.4f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // Header with Forecast Badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = GoldStar.copy(alpha = 0.15f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "Projeksiyon",
                tint = Color(0xFFD97706),
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "🎯 Müfredat Bitiş Tahmini & Projeksiyon",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "${projection.totalTarget} Maddelik Aktif Müfredat İlerleme ve Gelecek Analizi",
              fontSize = 11.5.sp,
              color = TextSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Estimated Finish Hero Box
      Surface(
        color = DeepBlueNavy,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "TAHMİNİ MEZUNİYET / BİTİŞ TARİHİ",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = GoldStar,
              letterSpacing = 0.8.sp
            )
            Surface(
              color = Color.White.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "${projection.daysRemaining} Gün Kaldı",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = projection.formattedEndDate,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = if (projection.remainingTotal == 0) {
              "${student.fullName} ${projection.totalTarget} maddelik müfredatın tamamını üstün başarıyla tamamladı!"
            } else {
              "Bu çalışma temposuyla (${String.format(Locale.US, "%.1f", projection.weeklyPace)} ezber/hafta) devam edildiğinde ${projection.totalTarget} maddenin tamamı bu tarihte bitecektir."
            },
            fontSize = 11.5.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 16.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Pace Selector Buttons (1.0x, 1.5x, 2.0x)
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Çalışma Temposu Senaryosu:",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary,
          modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          PaceMultiplier.values().forEach { pace ->
            val isSelected = selectedPace == pace
            FilterChip(
              selected = isSelected,
              onClick = { selectedPace = pace },
              label = {
                Text(
                  text = pace.badgeText,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FeatureDevelopmentTeal,
                selectedLabelColor = Color.White,
                containerColor = SurfaceVariantColor
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Visual Canvas Graph: Historical Line (Solid) + Future Projection (Dashed with Flag)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(200.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF0F172A))
          .padding(horizontal = 12.dp, vertical = 10.dp)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height
          val topPad = 24f
          val botPad = 28f
          val plotH = h - topPad - botPad

          // 1. Grid Lines at 0%, 50%, 100%
          listOf(0, 50, 100).forEach { step ->
            val y = topPad + plotH * (1f - (step / 100f))
            drawLine(
              color = Color.White.copy(alpha = 0.12f),
              start = Offset(0f, y),
              end = Offset(w, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
          }

          // Split width: 50% for past progress, 50% for future projection
          val pastW = w * 0.48f
          val futureW = w * 0.52f
          val splitX = pastW

          // Draw "BUGÜN" vertical divider line
          drawLine(
            color = GoldStar,
            start = Offset(splitX, topPad - 6f),
            end = Offset(splitX, topPad + plotH + 4f),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
          )

          // 2. Plot Past Line (Solid Teal/Green)
          val pastPoints = projection.pastProgressPoints
          val pastOffsets = pastPoints.mapIndexed { idx, pair ->
            val x = if (pastPoints.size > 1) idx * (pastW / (pastPoints.size - 1)) else pastW / 2
            val y = topPad + plotH * (1f - (pair.second / 100f).coerceIn(0f, 1f))
            Offset(x, y)
          }

          if (pastOffsets.isNotEmpty()) {
            val pastPath = Path().apply {
              moveTo(pastOffsets.first().x, pastOffsets.first().y)
              for (i in 1 until pastOffsets.size) {
                val p0 = pastOffsets[i - 1]
                val p1 = pastOffsets[i]
                val midX = (p0.x + p1.x) / 2
                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
              }
            }

            // Draw Area under past line
            val pastArea = Path().apply {
              addPath(pastPath)
              lineTo(pastOffsets.last().x, topPad + plotH)
              lineTo(pastOffsets.first().x, topPad + plotH)
              close()
            }
            drawPath(
              path = pastArea,
              brush = Brush.verticalGradient(
                colors = listOf(FeatureDevelopmentTeal.copy(alpha = 0.4f), FeatureDevelopmentTeal.copy(alpha = 0.05f)),
                startY = topPad,
                endY = topPad + plotH
              )
            )

            // Draw solid line
            drawPath(
              path = pastPath,
              color = FeatureDevelopmentTeal,
              style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw dots on past line
            pastOffsets.forEach { pt ->
              drawCircle(Color.White, radius = 3.5.dp.toPx(), center = pt)
              drawCircle(FeatureDevelopmentTeal, radius = 2.dp.toPx(), center = pt)
            }
          }

          // 3. Plot Future Forecast Line (Dashed Golden Line to 100%)
          val todayOffset = pastOffsets.lastOrNull() ?: Offset(splitX, topPad + plotH * (1f - (projection.currentPercent / 100f)))
          val targetOffset = Offset(w - 6.dp.toPx(), topPad) // 100% target at the end

          val futurePath = Path().apply {
            moveTo(todayOffset.x, todayOffset.y)
            val cX = (todayOffset.x + targetOffset.x) / 2
            cubicTo(cX, todayOffset.y, cX, targetOffset.y, targetOffset.x, targetOffset.y)
          }

          // Future glowing area
          val futureArea = Path().apply {
            addPath(futurePath)
            lineTo(targetOffset.x, topPad + plotH)
            lineTo(todayOffset.x, topPad + plotH)
            close()
          }
          drawPath(
            path = futureArea,
            brush = Brush.verticalGradient(
              colors = listOf(GoldStar.copy(alpha = 0.25f), GoldStar.copy(alpha = 0.02f)),
              startY = topPad,
              endY = topPad + plotH
            )
          )

          // Draw dashed future projection line
          drawPath(
            path = futurePath,
            color = GoldStar,
            style = Stroke(
              width = 2.5.dp.toPx(),
              cap = StrokeCap.Round,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
          )

          // Target Star / Flag at 100%
          drawCircle(GoldStar, radius = 6.dp.toPx(), center = targetOffset)
          drawCircle(Color.White, radius = 3.dp.toPx(), center = targetOffset)
        }

        // Top chart legends
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopStart),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FeatureDevelopmentTeal))
            Text("Geçmiş Gerçekleşen", fontSize = 10.sp, color = Color(0xFF94A3B8))
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldStar))
            Text("Gelecek Projeksiyonu (%100 Hedef)", fontSize = 10.sp, color = GoldStar, fontWeight = FontWeight.Bold)
          }
        }

        // Bottom timeline labels (Geçmiş -> Bugün -> Hedef)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Geçmiş Çalışmalar", fontSize = 9.5.sp, color = Color(0xFF64748B))
          Text("📍 BUGÜN (%${projection.currentPercent})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldStar)
          Text("🏁 %100 Bitiş", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = FeatureAttendanceGreen)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Category Milestones Cards
      Text(
        text = "Kategori Bazlı Tahmini Bitiş Takvimi:",
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      Spacer(modifier = Modifier.height(8.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        projection.milestones.forEach { m ->
          Surface(
            color = m.color.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, m.color.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(m.color.copy(alpha = 0.18f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(m.icon, contentDescription = null, tint = m.color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(text = m.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                  Text(
                    text = "${m.completed}/${m.total} Tamamlandı (${m.total - m.completed} Kalan)",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = m.estimatedDateStr,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (m.completed >= m.total) FeatureAttendanceGreen else m.color
                )
                Text(
                  text = if (m.completed >= m.total) "Tamamlandı" else "~${m.remainingDays} gün sonra",
                  fontSize = 10.sp,
                  color = TextMuted
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Weekly Study Velocity & Ezber Performance Trend Chart.
 * Analyzes weekly ezber count delivery and average star quality rating.
 */
@Composable
fun StudyVelocityAndActivityChart(
  student: Student,
  records: List<MemorizationRecord>,
  modifier: Modifier = Modifier
) {
  // Group records by week (last 6 weeks)
  val weeklyData = remember(records) {
    val list = mutableListOf<Pair<String, Int>>() // Week label -> count
    val ratingList = mutableListOf<Float>()
    val cal = Calendar.getInstance()
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    for (w in 5 downTo 0) {
      val wCalStart = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(w * 7 + 6)) }
      val wCalEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(w * 7)) }
      val label = if (w == 0) "Bu Hafta" else if (w == 1) "Geçen Hafta" else "$w Hf Önce"

      val count = records.count { r ->
        try {
          val t = df.parse(r.date)?.time ?: 0L
          t in wCalStart.timeInMillis..wCalEnd.timeInMillis
        } catch (e: Exception) { false }
      }
      val validRatings = records.mapNotNull { if (it.rating > 0) it.rating else null }
      val avgRating = if (validRatings.isNotEmpty()) validRatings.average().toFloat() else 4.5f

      list.add(label to count)
      ratingList.add(avgRating)
    }
    list to ratingList
  }

  val totalRecorded = records.size
  val avgStars = if (records.isNotEmpty()) records.map { it.rating }.filter { it > 0 }.average().let { if (it.isNaN()) 5.0 else it } else 5.0

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = "⚡ Haftalık Çalışma Hızı & Teslim Yoğunluğu",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Haftalık verilen ezber miktarı ve kalite değerlendirmesi",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
        Surface(
          color = Color(0xFFFEF3C7),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = String.format(Locale.US, "%.1f", avgStars),
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              color = Color(0xFF92400E)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Weekly Column Chart
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(150.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(SurfaceVariantColor.copy(alpha = 0.5f))
          .padding(10.dp)
      ) {
        val pairs = weeklyData.first
        val maxVal = max(4, pairs.maxOfOrNull { it.second } ?: 4)

        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.Bottom
        ) {
          pairs.forEach { (label, count) ->
            val barHeightFraction = (count.toFloat() / maxVal).coerceIn(0.08f, 1f)
            val isCurrent = label == "Bu Hafta"

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Bottom,
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) FeatureAttendanceGreen else TextSecondary
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .width(22.dp)
                  .fillMaxHeight(barHeightFraction * 0.72f)
                  .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                  .background(
                    if (isCurrent) {
                      Brush.verticalGradient(listOf(FeatureAttendanceGreen, FeatureAttendanceGreen.copy(alpha = 0.6f)))
                    } else {
                      Brush.verticalGradient(listOf(FeatureDevelopmentTeal, DeepBlueNavy))
                    }
                  )
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = label.split(" ").firstOrNull() ?: label,
                fontSize = 9.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) TextPrimary else TextMuted,
                maxLines = 1
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Quick insights row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          color = FeatureAttendanceGreen.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Toplam Kayıt", fontSize = 10.sp, color = TextSecondary)
            Text("$totalRecorded Ezber", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FeatureAttendanceGreen)
          }
        }

        Surface(
          color = FeatureMemorizationPurple.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Haftalık Hedef", fontSize = 10.sp, color = TextSecondary)
            Text("3 Ezber/Hafta", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FeatureMemorizationPurple)
          }
        }

        Surface(
          color = FeatureReportsOrange.copy(alpha = 0.08f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ezber Sağlamlığı", fontSize = 10.sp, color = TextSecondary)
            Text("%${(avgStars * 20).toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FeatureReportsOrange)
          }
        }
      }
    }
  }
}


