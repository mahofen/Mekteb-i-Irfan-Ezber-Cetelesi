package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// Model for 1-month weekly / interval breakdown data point
data class MonthWeekPacePoint(
  val weekLabel: String,         // e.g. "1. Hafta (1-7)", "2. Hafta (8-14)", etc.
  val shortLabel: String,        // e.g. "1. Hft", "2. Hft"
  val dateRangeLabel: String,    // e.g. "25 Tem - 31 Tem"
  val completedCount: Int,       // count of memorizations completed in this window
  val inProgressCount: Int,      // count of memorizations actively worked in this window
  val totalPagesOrUnits: Int,    // estimated volume (pages/verses/items)
  val cumulativeTotal: Int,      // cumulative completed memorizations at this point
  val records: List<MemorizationRecord> // records belonging to this period
)

data class StudentPaceMetrics(
  val totalCompletedLastMonth: Int,
  val weeklyAveragePace: Float,    // memorizations completed per week
  val paceStatusLabel: String,     // e.g. "Yüksek Tempolu 🚀", "İstikrarlı & Dengeli 🌿"
  val paceStatusColor: Color,
  val estimatedDaysPerItem: Float, // average days to complete one memorization
  val monthGrowthPercent: Int,     // percentage increase in total memorization archive
  val kuranCompletedMonth: Int,
  val tesbihatCompletedMonth: Int,
  val risaleCompletedMonth: Int
)

/**
 * Calculates last 30 days memorization progression and speed metrics.
 */
fun calculateStudentMonthPace(
  records: List<MemorizationRecord>,
  todayMillis: Long = Calendar.getInstance().timeInMillis
): Pair<StudentPaceMetrics, List<MonthWeekPacePoint>> {
  val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val displayFormat = SimpleDateFormat("dd MMM", Locale("tr"))

  val oneDayMs = 24 * 60 * 60 * 1000L
  val thirtyDaysAgoMs = todayMillis - (30 * oneDayMs)

  // Filter records within the last 30 days
  val lastMonthRecords = records.filter { rec ->
    val t = try {
      dateFormat.parse(rec.date)?.time ?: 0L
    } catch (e: Exception) {
      todayMillis - (5 * oneDayMs)
    }
    t in (thirtyDaysAgoMs - oneDayMs)..todayMillis || (rec.status == "TAMAMLANDI" && t >= thirtyDaysAgoMs)
  }

  // All completed records before 30 days ago to compute baseline cumulative
  val baselineCompleted = records.filter { rec ->
    val t = try { dateFormat.parse(rec.date)?.time ?: 0L } catch (e: Exception) { 0L }
    (rec.status == "TAMAMLANDI" || rec.progressPercent >= 100) && t < thirtyDaysAgoMs
  }.size

  // Split last 30 days into 4 weekly blocks:
  // Week 1: 30 - 23 days ago
  // Week 2: 22 - 15 days ago
  // Week 3: 14 - 7 days ago
  // Week 4: 6 - 0 days ago (current week)
  val weekBlocks = listOf(
    Pair(thirtyDaysAgoMs, thirtyDaysAgoMs + (7 * oneDayMs)),
    Pair(thirtyDaysAgoMs + (7 * oneDayMs), thirtyDaysAgoMs + (14 * oneDayMs)),
    Pair(thirtyDaysAgoMs + (14 * oneDayMs), thirtyDaysAgoMs + (21 * oneDayMs)),
    Pair(thirtyDaysAgoMs + (21 * oneDayMs), todayMillis + (oneDayMs))
  )

  var runningCumulative = baselineCompleted
  val points = mutableListOf<MonthWeekPacePoint>()

  weekBlocks.forEachIndexed { index, (startMs, endMs) ->
    val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMs.coerceAtMost(todayMillis) }
    val rangeText = "${displayFormat.format(startCal.time)} - ${displayFormat.format(endCal.time)}"

    val weekRecs = records.filter { rec ->
      val t = try { dateFormat.parse(rec.date)?.time ?: 0L } catch (e: Exception) { 0L }
      t in startMs until endMs
    }

    val completedInWeek = weekRecs.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
    val inProgInWeek = weekRecs.count { it.status == "DEVAM_EDIYOR" || (it.progressPercent in 1..99) }
    runningCumulative += completedInWeek

    // Estimation of pages / units
    val volume = completedInWeek * 2 + inProgInWeek

    points.add(
      MonthWeekPacePoint(
        weekLabel = "${index + 1}. Hafta",
        shortLabel = "${index + 1}. Hft",
        dateRangeLabel = rangeText,
        completedCount = completedInWeek,
        inProgressCount = inProgInWeek,
        totalPagesOrUnits = volume,
        cumulativeTotal = runningCumulative,
        records = weekRecs
      )
    )
  }

  // Calculate totals and metrics
  val totalCompletedMonth = points.sumOf { it.completedCount }.let {
    if (it > 0) it else lastMonthRecords.count { r -> r.status == "TAMAMLANDI" || r.progressPercent >= 100 }
  }

  val weeklyAvg = (totalCompletedMonth.toFloat() / 4.0f).coerceAtLeast(0f)
  val daysPerItem = if (totalCompletedMonth > 0) 30f / totalCompletedMonth else 7.0f

  val (statusLabel, statusColor) = when {
    weeklyAvg >= 2.5f -> Pair("Çok Yüksek Tempolu ⚡", TezhipGold)
    weeklyAvg >= 1.5f -> Pair("Hızlı & İstikrarlı 🚀", FeatureAttendanceGreen)
    weeklyAvg >= 0.75f -> Pair("Dengeli & Düzenli 🌿", FeatureMemorizationPurple)
    else -> Pair("Geliştirilmeye Açık 🎯", FeatureReportsOrange)
  }

  val totalStudentArchive = records.count { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
  val growthPercent = if (totalStudentArchive > 0) {
    ((totalCompletedMonth.toFloat() / totalStudentArchive.toFloat()) * 100).roundToInt().coerceIn(0, 100)
  } else if (totalCompletedMonth > 0) 100 else 0

  val kuranCount = lastMonthRecords.count {
    (it.status == "TAMAMLANDI" || it.progressPercent >= 100) &&
      (it.category.contains("Sure", true) || it.category.contains("Cüz", true) || it.category.contains("Kuran", true) || it.category.contains("Aşır", true))
  }
  val tesbihatCount = lastMonthRecords.count {
    (it.status == "TAMAMLANDI" || it.progressPercent >= 100) &&
      (it.category.contains("Tesbihat", true) || it.category.contains("Dua", true))
  }
  val risaleCount = lastMonthRecords.count {
    (it.status == "TAMAMLANDI" || it.progressPercent >= 100) &&
      (it.category.contains("Risale", true) || it.category.contains("Hadis", true) || it.category.contains("Vecize", true))
  }

  val metrics = StudentPaceMetrics(
    totalCompletedLastMonth = totalCompletedMonth,
    weeklyAveragePace = weeklyAvg,
    paceStatusLabel = statusLabel,
    paceStatusColor = statusColor,
    estimatedDaysPerItem = daysPerItem,
    monthGrowthPercent = growthPercent,
    kuranCompletedMonth = kuranCount,
    tesbihatCompletedMonth = tesbihatCount,
    risaleCompletedMonth = risaleCount
  )

  return Pair(metrics, points)
}

/**
 * Modern, Interactive Jetpack Compose Chart Component for Student Memorization Pace and Last 1-Month Progress.
 */
@Composable
fun StudentMemorizationPaceChart(
  student: Student,
  records: List<MemorizationRecord>,
  modifier: Modifier = Modifier
) {
  var selectedWeekIndex by remember { mutableStateOf<Int?>(null) }
  var isExpandedInfo by remember { mutableStateOf(false) }

  val (metrics, weekPoints) = remember(records) {
    calculateStudentMonthPace(records)
  }

  val selectedPoint = selectedWeekIndex?.let {
    if (it in weekPoints.indices) weekPoints[it] else null
  } ?: weekPoints.lastOrNull()

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient),
    modifier = modifier
      .fillMaxWidth()
      .testTag("student_pace_chart_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // 1. Header with Icon, Title, and Pace Status Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          SeljukStarBox(
            size = 36.dp,
            backgroundColor = metrics.paceStatusColor
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = "Ezber Hızı",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = "Ezber Hızı & 1 Aylık Gelişim",
              fontSize = 15.5.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Son 30 günlük tempo, teslimat ve ilerleme",
              fontSize = 11.5.sp,
              color = TextSecondary
            )
          }
        }

        Surface(
          color = metrics.paceStatusColor.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, metrics.paceStatusColor.copy(alpha = 0.4f))
        ) {
          Text(
            text = metrics.paceStatusLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (metrics.paceStatusColor == TezhipGold) DarkSlateNavy else metrics.paceStatusColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Pace & Monthly Growth KPI Summary (3 Quick Metrics)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Metric 1: Weekly Pace
        Surface(
          color = FeatureAttendanceGreen.copy(alpha = 0.08f),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.25f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Haftalık Hız", fontSize = 10.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = String.format("%.1f", metrics.weeklyAveragePace) + " Ezber/Hft",
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureAttendanceGreen
            )
            Text("Ortalama teslimat", fontSize = 9.5.sp, color = TextMuted)
          }
        }

        // Metric 2: Last Month Total Completed
        Surface(
          color = FeatureMemorizationPurple.copy(alpha = 0.08f),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, FeatureMemorizationPurple.copy(alpha = 0.25f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Son 30 Gün", fontSize = 10.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "+${metrics.totalCompletedLastMonth} Ezber",
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              color = FeatureMemorizationPurple
            )
            Text("Yeni kabul edilen", fontSize = 9.5.sp, color = TextMuted)
          }
        }

        // Metric 3: Average Completion Duration
        Surface(
          color = TezhipGold.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, TezhipGold.copy(alpha = 0.35f)),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Teslim Süresi", fontSize = 10.5.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "~" + String.format("%.1f", metrics.estimatedDaysPerItem) + " Gün",
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              color = DarkSlateNavy
            )
            Text("Ezber başına süre", fontSize = 9.5.sp, color = TextMuted)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Custom Interactive Canvas Chart: Weekly Pace Columns & Cumulative Growth Line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(CanvasBackground)
          .border(1.dp, BorderStroke, RoundedCornerShape(14.dp))
          .padding(top = 12.dp, bottom = 8.dp, start = 10.dp, end = 10.dp)
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(weekPoints) {
              detectTapGestures { offset ->
                if (weekPoints.isNotEmpty()) {
                  val count = weekPoints.size
                  val spacing = size.width / count.coerceAtLeast(1)
                  val index = (offset.x / spacing).toInt().coerceIn(0, count - 1)
                  selectedWeekIndex = index
                }
              }
            }
        ) {
          val width = size.width
          val height = size.height
          val topPadding = 20f
          val bottomPadding = 30f
          val leftPadding = 28f
          val rightPadding = 16f
          val chartW = width - leftPadding - rightPadding
          val chartH = height - topPadding - bottomPadding

          // Draw Horizontal Background Reference Grid Lines
          val gridSteps = listOf(0f, 0.33f, 0.66f, 1f)
          gridSteps.forEach { step ->
            val y = topPadding + chartH * step
            drawLine(
              color = Color(0xFFE2E8F0),
              start = Offset(leftPadding, y),
              end = Offset(width - rightPadding, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
          }

          if (weekPoints.isNotEmpty()) {
            val count = weekPoints.size
            val maxCompleted = weekPoints.maxOfOrNull { it.completedCount }?.coerceAtLeast(3) ?: 3
            val maxCumulative = weekPoints.maxOfOrNull { it.cumulativeTotal }?.coerceAtLeast(4) ?: 4
            val stepX = if (count > 1) chartW / (count - 1) else chartW / 2

            // A. Draw Weekly Volume & Pace Bar Pillars
            weekPoints.forEachIndexed { idx, pt ->
              val cx = if (count > 1) leftPadding + idx * stepX else leftPadding + chartW / 2
              val barHeightRatio = (pt.completedCount.toFloat() / maxCompleted.toFloat()).coerceIn(0.1f, 1.0f)
              val barH = chartH * 0.55f * barHeightRatio
              val barW = (chartW / count * 0.40f).coerceIn(16f, 32f)
              val barLeft = cx - barW / 2f
              val barTop = topPadding + chartH - barH

              val isSelected = selectedWeekIndex == idx || (selectedWeekIndex == null && idx == count - 1)

              // Bar background pill
              drawRoundRect(
                brush = Brush.verticalGradient(
                  colors = if (isSelected) {
                    listOf(FeatureAttendanceGreen, FeatureAttendanceGreen.copy(alpha = 0.6f))
                  } else {
                    listOf(FeatureAttendanceGreen.copy(alpha = 0.7f), FeatureAttendanceGreen.copy(alpha = 0.3f))
                  },
                  startY = barTop,
                  endY = barTop + barH
                ),
                topLeft = Offset(barLeft, barTop),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
              )

              if (isSelected) {
                drawRoundRect(
                  color = TezhipGold,
                  topLeft = Offset(barLeft - 1.dp.toPx(), barTop - 1.dp.toPx()),
                  size = Size(barW + 2.dp.toPx(), barH + 2.dp.toPx()),
                  style = Stroke(width = 1.5.dp.toPx()),
                  cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
              }
            }

            // B. Draw Cumulative Growth Curve
            val linePoints = weekPoints.mapIndexed { idx, pt ->
              val cx = if (count > 1) leftPadding + idx * stepX else leftPadding + chartW / 2
              val ratio = (pt.cumulativeTotal.toFloat() / maxCumulative.toFloat()).coerceIn(0.1f, 1.0f)
              val cy = topPadding + chartH * (1f - (ratio * 0.85f))
              Offset(cx, cy)
            }

            if (linePoints.size >= 2) {
              val linePath = Path().apply {
                moveTo(linePoints.first().x, linePoints.first().y)
                for (i in 1 until linePoints.size) {
                  val prev = linePoints[i - 1]
                  val cur = linePoints[i]
                  val midX = (prev.x + cur.x) / 2f
                  cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
              }

              // Cumulative line gradient fill
              val areaPath = Path().apply {
                addPath(linePath)
                lineTo(linePoints.last().x, topPadding + chartH)
                lineTo(linePoints.first().x, topPadding + chartH)
                close()
              }

              drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                  colors = listOf(
                    FeatureMemorizationPurple.copy(alpha = 0.22f),
                    FeatureMemorizationPurple.copy(alpha = 0.04f),
                    Color.Transparent
                  ),
                  startY = topPadding,
                  endY = topPadding + chartH
                )
              )

              // Draw Main Cumulative Curve
              drawPath(
                path = linePath,
                color = FeatureMemorizationPurple,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
              )
            }

            // C. Draw Node Dots and Star Milestones
            linePoints.forEachIndexed { idx, pt ->
              val isSelected = selectedWeekIndex == idx || (selectedWeekIndex == null && idx == count - 1)
              if (isSelected) {
                drawCircle(TezhipGold.copy(alpha = 0.4f), radius = 9.dp.toPx(), center = pt)
                drawCircle(TezhipGold, radius = 5.dp.toPx(), center = pt)
                drawCircle(Color.White, radius = 2.5.dp.toPx(), center = pt)
              } else {
                drawCircle(FeatureMemorizationPurple, radius = 4.dp.toPx(), center = pt)
                drawCircle(Color.White, radius = 2.dp.toPx(), center = pt)
              }
            }
          }
        }

        // X-Axis Week Labels (Bottom Margin)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(start = 24.dp, end = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          weekPoints.forEachIndexed { idx, pt ->
            val isSelected = selectedWeekIndex == idx || (selectedWeekIndex == null && idx == weekPoints.size - 1)
            Text(
              text = pt.shortLabel,
              fontSize = 10.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) FeatureAttendanceGreen else TextSecondary,
              modifier = Modifier.clickable { selectedWeekIndex = idx }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 4. Interactive Inspector Panel for Selected Week
      selectedPoint?.let { pt ->
        Surface(
          color = CanvasSurface,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = null,
                  tint = FeatureAttendanceGreen,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${pt.weekLabel} (${pt.dateRangeLabel})",
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }

              Surface(
                color = FeatureAttendanceGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "${pt.completedCount} Teslim Edildi",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = FeatureAttendanceGreen,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (pt.records.isEmpty()) {
              Text(
                text = "Bu haftalık dilimde kaydedilmiş ezber kaydı bulunmamaktadır.",
                fontSize = 11.5.sp,
                color = TextSecondary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pt.records.forEach { rec ->
                  val isDone = rec.status == "TAMAMLANDI" || rec.progressPercent >= 100
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = if (isDone) FeatureAttendanceGreen else FeatureMemorizationPurple,
                        modifier = Modifier.size(13.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "${rec.category}: ${rec.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = if (isDone) "Kabul Edildi" else "%${rec.progressPercent}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) FeatureAttendanceGreen else FeatureMemorizationPurple
                      )
                      if (rec.rating > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "⭐ ${rec.rating}", fontSize = 10.5.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 5. Category Breakdown Ribbon (Kur'an, Tesbihat, Risale in last 30 days)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        CategoryPaceBadge(
          title = "Kur'an",
          count = metrics.kuranCompletedMonth,
          icon = Icons.AutoMirrored.Filled.MenuBook,
          color = FeatureAttendanceGreen,
          modifier = Modifier.weight(1f)
        )

        CategoryPaceBadge(
          title = "Tesbihat",
          count = metrics.tesbihatCompletedMonth,
          icon = Icons.Default.SelfImprovement,
          color = FeatureMemorizationPurple,
          modifier = Modifier.weight(1f)
        )

        CategoryPaceBadge(
          title = "Risale/Hadis",
          count = metrics.risaleCompletedMonth,
          icon = Icons.Default.MenuBook,
          color = FeatureReportsOrange,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun CategoryPaceBadge(
  title: String,
  count: Int,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.08f),
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = color,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "$title: ",
        fontSize = 11.sp,
        color = TextSecondary
      )
      Text(
        text = "$count",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color
      )
    }
  }
}
