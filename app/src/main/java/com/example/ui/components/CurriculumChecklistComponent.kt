package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurriculumData
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.ui.theme.*

@Composable
fun CurriculumChecklistView(
  student: Student,
  records: List<MemorizationRecord>,
  todayDate: String,
  onSaveRecord: (MemorizationRecord) -> Unit,
  onOpenAddEdit: (MemorizationRecord) -> Unit,
  modifier: Modifier = Modifier
) {
  val activeCategories = remember { CurriculumData.getActiveCategories() }
  var selectedCategory by remember { mutableStateOf(activeCategories.firstOrNull() ?: "Kur'an") }
  var searchQuery by remember { mutableStateOf("") }
  var filterStatus by remember { mutableStateOf("Tümü") } // "Tümü", "Tamamlanan", "Devam Eden", "Başlanmayan"

  // If activeCategories changed and selected is not in it, reset
  LaunchedEffect(activeCategories) {
    if (selectedCategory !in activeCategories && activeCategories.isNotEmpty()) {
      selectedCategory = activeCategories.first()
    }
  }

  val categoryItems = CurriculumData.getItemsForCategory(selectedCategory)

  val categoryColor = when (selectedCategory.lowercase()) {
    "kuran", "kur'an" -> FeatureAttendanceGreen
    "tesbihat" -> FeatureMemorizationPurple
    "risale" -> FeatureReportsOrange
    "hadis", "40 hadis" -> Color(0xFF0284C7)
    "sureler", "kısa sureler" -> Color(0xFF0D9488)
    "dualar" -> Color(0xFF8B5CF6)
    "elifba", "elif-bâ & tecvid" -> Color(0xFFE11D48)
    else -> FeatureStudentsBlue
  }

  val studentCategoryRecords = records.filter { record ->
    val recCat = record.category.trim()
    recCat.equals(selectedCategory, ignoreCase = true) ||
      normalizeCategory(recCat) == normalizeCategory(selectedCategory)
  }

  // Calculate totals
  val completedInCat = categoryItems.count { title ->
    studentCategoryRecords.any { it.title.equals(title, ignoreCase = true) && (it.status == "TAMAMLANDI" || it.progressPercent >= 100) }
  }
  val inProgressInCat = categoryItems.count { title ->
    studentCategoryRecords.any { it.title.equals(title, ignoreCase = true) && (it.status == "DEVAM_EDIYOR" || it.progressPercent in 1..99) }
  }
  val notStartedInCat = categoryItems.size - completedInCat - inProgressInCat

  val filteredItems = categoryItems.mapIndexed { index, title ->
    val existingRecord = studentCategoryRecords.firstOrNull { it.title.equals(title, ignoreCase = true) }
    val status = when {
      existingRecord?.status == "TAMAMLANDI" || (existingRecord?.progressPercent ?: 0) >= 100 -> "TAMAMLANDI"
      existingRecord != null && existingRecord.progressPercent > 0 -> "DEVAM_EDIYOR"
      else -> "BASLANMADI"
    }
    CurriculumEntry(
      index = index + 1,
      title = title,
      category = selectedCategory,
      record = existingRecord,
      statusKey = status
    )
  }.filter { entry ->
    val matchesSearch = searchQuery.isBlank() || entry.title.contains(searchQuery.trim(), ignoreCase = true)
    val matchesStatus = when (filterStatus) {
      "Tamamlanan" -> entry.statusKey == "TAMAMLANDI"
      "Devam Eden" -> entry.statusKey == "DEVAM_EDIYOR"
      "Başlanmayan" -> entry.statusKey == "BASLANMADI"
      else -> true
    }
    matchesSearch && matchesStatus
  }

  Column(modifier = modifier.fillMaxWidth()) {
    // 1. Dynamic Category Switcher Chips (Sabit Boyutlu)
    LazyRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(activeCategories) { catKey ->
        val isSelected = selectedCategory == catKey
        val activeCol = when (catKey.lowercase()) {
          "kuran", "kur'an" -> FeatureAttendanceGreen
          "tesbihat" -> FeatureMemorizationPurple
          "risale" -> FeatureReportsOrange
          "hadis", "40 hadis" -> Color(0xFF0284C7)
          "sureler", "kısa sureler" -> Color(0xFF0D9488)
          "dualar" -> Color(0xFF8B5CF6)
          else -> FeatureStudentsBlue
        }
        val iconEmoji = when (catKey.lowercase()) {
          "kuran", "kur'an" -> "📖"
          "tesbihat" -> "📿"
          "risale" -> "📚"
          "hadis", "40 hadis" -> "📜"
          "dualar" -> "🤲"
          "elifba", "elif-bâ & tecvid" -> "🌟"
          else -> "🏷️"
        }
        Surface(
          onClick = { selectedCategory = catKey },
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) activeCol else CanvasSurface,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeCol else BorderLight
          ),
          modifier = Modifier.height(34.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = "$iconEmoji $catKey",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (isSelected) Color.White else TextPrimary,
              maxLines = 1
            )
          }
        }
      }
    }

    // 2. Summary Status Card for this category
    Surface(
      color = categoryColor.copy(alpha = 0.08f),
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.25f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "$selectedCategory Müfredatı:",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "$completedInCat / ${categoryItems.size} Tamamlandı",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = categoryColor
            )
          }

          Surface(
            color = categoryColor,
            shape = RoundedCornerShape(8.dp)
          ) {
            val pct = if (categoryItems.isNotEmpty()) (completedInCat * 100) / categoryItems.size else 0
            Text(
              text = "%$pct",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Multi-segmented or linear indicator
        val pctFloat = if (categoryItems.isNotEmpty()) completedInCat.toFloat() / categoryItems.size else 0f
        LinearProgressIndicator(
          progress = { pctFloat },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = categoryColor,
          trackColor = SurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mini badges
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("✅ $completedInCat Tamamlandı", fontSize = 11.sp, color = FeatureAttendanceGreen, fontWeight = FontWeight.SemiBold)
          Text("⏳ $inProgressInCat Devam Ediyor", fontSize = 11.sp, color = FeatureReportsOrange, fontWeight = FontWeight.SemiBold)
          Text("⭕ $notStartedInCat Başlanmadı", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // 3. Search & Filter Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("$selectedCategory içinde ara...", fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
            }
          }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedPlaceholderColor = TextMuted,
          unfocusedPlaceholderColor = TextMuted,
          focusedContainerColor = CanvasSurface,
          unfocusedContainerColor = CanvasSurface,
          focusedBorderColor = FeatureMemorizationPurple,
          unfocusedBorderColor = BorderLight,
          cursorColor = FeatureMemorizationPurple
        ),
        modifier = Modifier
          .weight(1f)
          .height(50.dp)
      )
    }

    // Status Sub-filter Chips (Sabit Boyutlu & Eşit Genişlikli - Asla Boyutu Değişmez)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      listOf("Tümü", "Tamamlanan", "Devam Eden", "Başlanmayan").forEach { st ->
        val isSel = filterStatus == st
        val selColor = when (st) {
          "Tamamlanan" -> FeatureAttendanceGreen
          "Devam Eden" -> FeatureReportsOrange
          "Başlanmayan" -> StatusExcusedAmber
          else -> DeepBlueNavy
        }
        Surface(
          onClick = { filterStatus = st },
          shape = RoundedCornerShape(8.dp),
          color = if (isSel) selColor else CanvasSurface,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSel) selColor else BorderLight
          ),
          modifier = Modifier
            .weight(1f)
            .height(32.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 2.dp)
          ) {
            Text(
              text = st,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (isSel) Color.White else TextPrimary,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    }

    // 4. Checklist Items List
    if (filteredItems.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("Aramaya uygun müfredat maddesi bulunamadı", color = TextSecondary, fontSize = 13.sp)
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filteredItems, key = { "${it.category}_${it.index}_${it.title}" }) { entry ->
          CurriculumChecklistCard(
            entry = entry,
            student = student,
            categoryColor = categoryColor,
            todayDate = todayDate,
            onQuickComplete = {
              val updated = (entry.record ?: MemorizationRecord(
                studentId = student.id,
                title = entry.title,
                category = entry.category,
                status = "TAMAMLANDI",
                rating = 5,
                progressPercent = 100,
                teacherNotes = "Müfredat çetelesinden tamamlandı.",
                date = todayDate
              )).copy(
                status = "TAMAMLANDI",
                progressPercent = 100,
                rating = if (entry.record?.rating ?: 0 == 0) 5 else entry.record!!.rating
              )
              onSaveRecord(updated)
            },
            onQuickStart = {
              val newRec = MemorizationRecord(
                studentId = student.id,
                title = entry.title,
                category = entry.category,
                status = "DEVAM_EDIYOR",
                rating = 5,
                progressPercent = 25,
                teacherNotes = "Ezber çalışması başlatıldı.",
                date = todayDate
              )
              onSaveRecord(newRec)
            },
            onOpenEdit = {
              val rec = entry.record ?: MemorizationRecord(
                studentId = student.id,
                title = entry.title,
                category = entry.category,
                status = "DEVAM_EDIYOR",
                rating = 5,
                progressPercent = 50,
                teacherNotes = "",
                date = todayDate
              )
              onOpenAddEdit(rec)
            }
          )
        }
      }
    }
  }
}

data class CurriculumEntry(
  val index: Int,
  val title: String,
  val category: String,
  val record: MemorizationRecord?,
  val statusKey: String // "TAMAMLANDI", "DEVAM_EDIYOR", "BASLANMADI"
)

@Composable
private fun CurriculumChecklistCard(
  entry: CurriculumEntry,
  student: Student,
  categoryColor: Color,
  todayDate: String,
  onQuickComplete: () -> Unit,
  onQuickStart: () -> Unit,
  onOpenEdit: () -> Unit
) {
  val isCompleted = entry.statusKey == "TAMAMLANDI"
  val isInProgress = entry.statusKey == "DEVAM_EDIYOR"

  val (statusBg, statusBorder, statusText) = when {
    isCompleted -> Triple(Color(0xFFD1FAE5), FeatureAttendanceGreen, "Tamamlandı ✅")
    isInProgress -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Devam Ediyor (%${entry.record?.progressPercent ?: 0})")
    else -> Triple(SurfaceVariantColor, BorderLight, "Başlanmadı ⭕")
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = CanvasSurface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCompleted) FeatureAttendanceGreen.copy(alpha = 0.4f) else BorderLight),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Number Badge + Title + Subtitle
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Order Index Badge (1..28, 1..34, 1..46)
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isCompleted) FeatureAttendanceGreen else SurfaceVariantColor),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${entry.index}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCompleted) Color.White else TextSecondary
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = entry.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Spacer(modifier = Modifier.height(2.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = statusBg,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) Color(0xFF065F46) else if (isInProgress) Color(0xFF92400E) else TextMuted,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }

            if (isCompleted && (entry.record?.rating ?: 0) > 0) {
              Spacer(modifier = Modifier.width(6.dp))
              Row {
                repeat(entry.record?.rating ?: 5) {
                  Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(11.dp))
                }
              }
            }
          }

          if (entry.record?.teacherNotes?.isNotEmpty() == true) {
            Text(
              text = "“${entry.record.teacherNotes}”",
              fontSize = 11.sp,
              color = TextSecondary,
              maxLines = 1
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Right: Action Buttons
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isCompleted) {
          IconButton(
            onClick = {
              if (isInProgress) onQuickComplete() else onQuickStart()
            },
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = if (isInProgress) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
              contentDescription = if (isInProgress) "Tamamla" else "Başlat",
              tint = if (isInProgress) FeatureAttendanceGreen else categoryColor,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        IconButton(
          onClick = onOpenEdit,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Düzenle / Puan Ver",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
