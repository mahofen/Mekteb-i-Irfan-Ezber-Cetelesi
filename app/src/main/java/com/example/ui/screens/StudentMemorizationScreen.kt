package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.util.CustomCurriculumItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMemorizationScreen(
  student: Student?,
  activeMemorizations: List<MemorizationRecord>,
  completedMemorizations: List<MemorizationRecord>,
  initialTabUpcoming: Boolean,
  onBackClick: () -> Unit,
  viewModel: AppViewModel,
  modifier: Modifier = Modifier
) {
  // 0: Tamamlanan / Yapılan Ezberler, 1: Hoca Müfredat Çetelesi, 2: Sıradaki & Aktif Ezberler
  var selectedTab by remember { mutableIntStateOf(if (initialTabUpcoming) 2 else 0) }
  var selectedCategory by remember { mutableStateOf("Tümü") }
  var searchQuery by remember { mutableStateOf("") }
  var curriculumStatusFilter by remember { mutableStateOf("Tümü") } // "Tümü", "Tamamlanan", "Sırada"

  // Fetch full curriculum from ViewModel
  val customCurriculumList by viewModel.curriculumItems.collectAsState()
  val activeCurriculumItems = remember(customCurriculumList) {
    val active = customCurriculumList.filter { it.isSelected }.sortedBy { it.orderIndex }
    if (active.isNotEmpty()) active else customCurriculumList
  }

  // Active categories in curriculum
  val activeCategories = remember(activeCurriculumItems) {
    val cats = activeCurriculumItems.map { it.category.trim() }.distinct()
    if (cats.isEmpty()) listOf("Kur'an-ı Kerim", "Namaz Tesbihatı", "Risale-i Nur", "Hadis-i Şerif")
    else cats
  }

  var selectedCurriculumCategoryIndex by remember { mutableIntStateOf(0) }
  val activeCurriculumCategory = remember(activeCategories, selectedCurriculumCategoryIndex) {
    if (selectedCurriculumCategoryIndex in activeCategories.indices) activeCategories[selectedCurriculumCategoryIndex]
    else activeCategories.firstOrNull() ?: "Kur'an-ı Kerim"
  }

  val allStudentRecords = remember(activeMemorizations, completedMemorizations) {
    activeMemorizations + completedMemorizations
  }

  // Helper matching functions
  fun getItemRecord(title: String, cat: String): MemorizationRecord? {
    return allStudentRecords.find { record ->
      record.title.equals(title.trim(), ignoreCase = true) &&
          (record.category.equals(cat.trim(), ignoreCase = true) ||
              normalizeCategoryKey(record.category) == normalizeCategoryKey(cat))
    }
  }

  fun isItemCompleted(title: String, cat: String): Boolean {
    val rec = getItemRecord(title, cat)
    return rec != null && (rec.status == "TAMAMLANDI" || rec.progressPercent >= 100)
  }

  // Statistics calculation
  val totalCurriculumCount = activeCurriculumItems.size.coerceAtLeast(1)
  val totalCompletedCount = completedMemorizations.size
  val progressPercent = ((totalCompletedCount.toFloat() / totalCurriculumCount.toFloat()) * 100).toInt().coerceIn(0, 100)

  val ratings = completedMemorizations.map { it.rating }.filter { it > 0 }
  val avgRatingFormatted = if (ratings.isNotEmpty()) {
    String.format(Locale.getDefault(), "%.1f", ratings.average())
  } else "5.0"

  val totalRepeats = allStudentRecords.sumOf { it.repeatCount }

  val filterCategories = listOf("Tümü", "Kur'an-ı Kerim", "Sure", "Cüz", "Namaz Tesbihatı", "Risale-i Nur", "Hadis-i Şerif")

  // Filtered Completed list
  val filteredCompletedList = completedMemorizations.filter { record ->
    val matchesCategory = (selectedCategory == "Tümü") ||
        record.category.contains(selectedCategory, ignoreCase = true) ||
        normalizeCategoryKey(record.category) == normalizeCategoryKey(selectedCategory)
    val matchesSearch = searchQuery.isBlank() || record.title.contains(searchQuery, ignoreCase = true)
    matchesCategory && matchesSearch
  }

  // Filtered Active list
  val filteredActiveList = activeMemorizations.filter { record ->
    val matchesCategory = (selectedCategory == "Tümü") ||
        record.category.contains(selectedCategory, ignoreCase = true) ||
        normalizeCategoryKey(record.category) == normalizeCategoryKey(selectedCategory)
    val matchesSearch = searchQuery.isBlank() || record.title.contains(searchQuery, ignoreCase = true)
    matchesCategory && matchesSearch
  }

  // Curriculum items for the selected category tab
  val currentCurriculumCategoryItems = remember(activeCurriculumItems, activeCurriculumCategory, allStudentRecords) {
    val fromCurriculum = activeCurriculumItems.filter { it.category.equals(activeCurriculumCategory, ignoreCase = true) }
    val extraStudentRecords = allStudentRecords.filter { record ->
      record.category.equals(activeCurriculumCategory, ignoreCase = true) &&
          fromCurriculum.none { it.title.equals(record.title, ignoreCase = true) }
    }.map { rec ->
      CustomCurriculumItem(
        id = rec.id.toString(),
        title = rec.title,
        category = rec.category,
        orderIndex = 999,
        isSelected = true
      )
    }
    fromCurriculum + extraStudentRecords
  }

  val filteredCurriculumItems = currentCurriculumCategoryItems.filter { item ->
    val isCompleted = isItemCompleted(item.title, activeCurriculumCategory)
    val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
    val matchesStatus = when (curriculumStatusFilter) {
      "Tamamlanan" -> isCompleted
      "Sırada" -> !isCompleted
      else -> true
    }
    matchesSearch && matchesStatus
  }

  val categoryThemeColor = getCategoryThemeColor(activeCurriculumCategory)

  Scaffold(
    containerColor = CanvasBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Ezber Takip & Müfredat",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "${student?.fullName ?: "Talebe"} • $totalCompletedCount Tamamlanan / $totalCurriculumCount Müfredat",
              fontSize = 11.5.sp,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = TextPrimary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = CanvasSurface
        )
      )
    }
  ) { paddingValues ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // 🕌 Top Progress Overview Header Banner
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp)
          .shadow(3.dp, RoundedCornerShape(16.dp)),
        color = CanvasSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              SeljukStarBox(
                size = 34.dp,
                backgroundColor = TezhipGold
              ) {
                Icon(
                  imageVector = Icons.Default.EmojiEvents,
                  contentDescription = "Başarı",
                  tint = DarkSlateNavy,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Ezber & Gelişim Tablosu",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Hoca onaylı teslim edilen ezberler",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            Surface(
              color = FeatureAttendanceGreen.copy(alpha = 0.15f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "%$progressPercent Tamamlandı",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = FeatureAttendanceGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Progress Bar
          LinearProgressIndicator(
            progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = FeatureAttendanceGreen,
            trackColor = CanvasBackground
          )

          Spacer(modifier = Modifier.height(10.dp))

          // 4 Mini Summary Metrics
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StudentMiniMetric(
              title = "Yapılan Ezber",
              value = "$totalCompletedCount Adet",
              color = FeatureAttendanceGreen,
              modifier = Modifier.weight(1f)
            )
            StudentMiniMetric(
              title = "Müfredat",
              value = "$totalCurriculumCount Ders",
              color = FeatureStudentsBlue,
              modifier = Modifier.weight(1f)
            )
            StudentMiniMetric(
              title = "Hoca Puanı",
              value = "⭐ $avgRatingFormatted",
              color = TezhipGold,
              modifier = Modifier.weight(1f)
            )
            StudentMiniMetric(
              title = "Toplam Tekrar",
              value = "🔄 $totalRepeats",
              color = FeatureMemorizationPurple,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // 🔘 3 Main Navigation Tabs
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 4.dp),
        color = CanvasSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
        ) {
          // Tab 0: Yapılan & Tamamlanan Ezberler
          TabPill(
            title = "Yapılan Ezberler (${completedMemorizations.size})",
            icon = Icons.Default.Verified,
            isSelected = selectedTab == 0,
            activeColor = FeatureAttendanceGreen,
            onClick = { selectedTab = 0 },
            modifier = Modifier.weight(1f)
          )

          // Tab 1: Hoca Müfredat Çetelesi
          TabPill(
            title = "Müfredat Çetelesi (${activeCurriculumItems.size})",
            icon = Icons.Default.ChecklistRtl,
            isSelected = selectedTab == 1,
            activeColor = FeatureStudentsBlue,
            onClick = { selectedTab = 1 },
            modifier = Modifier.weight(1f)
          )

          // Tab 2: Gelecek & Sıradaki
          TabPill(
            title = "Sıradaki (${activeMemorizations.size})",
            icon = Icons.Default.HourglassTop,
            isSelected = selectedTab == 2,
            activeColor = FeatureMemorizationPurple,
            onClick = { selectedTab = 2 },
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Search & Search / Filter Row
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(
            text = when (selectedTab) {
              0 -> "Yapılan ezberlerde ara (Fatiha, Yasin, Ayetel Kürsi...)"
              1 -> "Müfredat çetelesinde ara..."
              else -> "Sıradaki ezberlerde ara..."
            },
            fontSize = 12.5.sp
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Ara",
            tint = TextMuted,
            modifier = Modifier.size(17.dp)
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Temizle",
                tint = TextMuted,
                modifier = Modifier.size(17.dp)
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = EmeraldGreen,
          unfocusedBorderColor = BorderStroke,
          focusedContainerColor = CanvasSurface,
          unfocusedContainerColor = CanvasSurface
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 4.dp)
          .height(48.dp)
      )

      // =========================================================================
      // TAB 0: YAPILAN & TAMAMLANAN EZBERLER (COMPLETED ARCHIVE)
      // =========================================================================
      if (selectedTab == 0) {
        // Category filter chips
        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          contentPadding = PaddingValues(horizontal = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(filterCategories) { category ->
            val isSelected = selectedCategory == category
            FilterChip(
              selected = isSelected,
              onClick = { selectedCategory = category },
              label = {
                Text(
                  text = category,
                  fontSize = 11.5.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = FeatureAttendanceGreen.copy(alpha = 0.15f),
                selectedLabelColor = FeatureAttendanceGreen
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                borderColor = BorderStroke,
                selectedBorderColor = FeatureAttendanceGreen
              )
            )
          }
        }

        if (filteredCompletedList.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              SeljukStarBox(
                size = 56.dp,
                backgroundColor = FeatureAttendanceGreen.copy(alpha = 0.2f)
              ) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = "Boş Liste",
                  tint = FeatureAttendanceGreen,
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = if (searchQuery.isNotBlank() || selectedCategory != "Tümü")
                  "Filtreye uygun yapılan ezber bulunamadı."
                else
                  "Henüz tamamlanmış ezber kaydınız bulunmuyor.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Hocanıza dinlettiğiniz ve onaylanan ezberler burada listelenecektir.",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredCompletedList, key = { it.id }) { record ->
              StudentCompletedMemorizationDetailCard(record = record)
            }
          }
        }
      }

      // =========================================================================
      // TAB 1: HOCA MÜFREDAT ÇETELESİ (FULL CURRICULUM CHECKLIST AS IN TEACHER MODE)
      // =========================================================================
      else if (selectedTab == 1) {
        // Dynamic category tabs
        if (activeCategories.size <= 3) {
          PrimaryTabRow(
            selectedTabIndex = selectedCurriculumCategoryIndex,
            containerColor = CanvasSurface,
            contentColor = categoryThemeColor,
            indicator = {
              TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selectedCurriculumCategoryIndex),
                color = categoryThemeColor,
                height = 3.dp
              )
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            activeCategories.forEachIndexed { index, catName ->
              val isSelected = selectedCurriculumCategoryIndex == index
              val catIcon = getCategoryIcon(catName)
              val catColor = getCategoryThemeColor(catName)
              val catCount = activeCurriculumItems.count { it.category.equals(catName, ignoreCase = true) }

              Tab(
                selected = isSelected,
                onClick = { selectedCurriculumCategoryIndex = index },
                text = {
                  Text(
                    text = "$catIcon $catName ($catCount)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = if (isSelected) catColor else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              )
            }
          }
        } else {
          ScrollableTabRow(
            selectedTabIndex = selectedCurriculumCategoryIndex,
            containerColor = CanvasSurface,
            contentColor = categoryThemeColor,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
              if (selectedCurriculumCategoryIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                  modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCurriculumCategoryIndex]),
                  color = categoryThemeColor,
                  height = 3.dp
                )
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            activeCategories.forEachIndexed { index, catName ->
              val isSelected = selectedCurriculumCategoryIndex == index
              val catIcon = getCategoryIcon(catName)
              val catColor = getCategoryThemeColor(catName)
              val catCount = activeCurriculumItems.count { it.category.equals(catName, ignoreCase = true) }

              Tab(
                selected = isSelected,
                onClick = { selectedCurriculumCategoryIndex = index },
                text = {
                  Text(
                    text = "$catIcon $catName ($catCount)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = if (isSelected) catColor else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              )
            }
          }
        }

        // Sub-filter chips: Tümü, Tamamlanan, Sırada
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterStatusChip(
            title = "Tümü (${currentCurriculumCategoryItems.size})",
            isSelected = curriculumStatusFilter == "Tümü",
            color = DeepBlueNavy,
            onClick = { curriculumStatusFilter = "Tümü" },
            modifier = Modifier.weight(1f)
          )
          FilterStatusChip(
            title = "Tamamlanan ✅",
            isSelected = curriculumStatusFilter == "Tamamlanan",
            color = FeatureAttendanceGreen,
            onClick = { curriculumStatusFilter = "Tamamlanan" },
            modifier = Modifier.weight(1f)
          )
          FilterStatusChip(
            title = "Sırada ⏳",
            isSelected = curriculumStatusFilter == "Sırada",
            color = FeatureStudentsBlue,
            onClick = { curriculumStatusFilter = "Sırada" },
            modifier = Modifier.weight(1f)
          )
        }

        if (filteredCurriculumItems.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Bu kategoride gösterilecek ezber maddesi bulunamadı.",
              fontSize = 13.5.sp,
              color = TextSecondary,
              textAlign = TextAlign.Center
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(filteredCurriculumItems, key = { "${it.category}_${it.id}_${it.title}" }) { item ->
              val record = getItemRecord(item.title, activeCurriculumCategory)
              val isCompleted = isItemCompleted(item.title, activeCurriculumCategory)

              StudentCurriculumItemCard(
                title = item.title,
                category = activeCurriculumCategory,
                isCompleted = isCompleted,
                record = record,
                categoryColor = categoryThemeColor
              )
            }
          }
        }
      }

      // =========================================================================
      // TAB 2: GELECEK & SIRADAKİ AKTİF EZBERLER (UPCOMING / IN-PROGRESS)
      // =========================================================================
      else {
        if (filteredActiveList.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              SeljukStarBox(
                size = 56.dp,
                backgroundColor = FeatureMemorizationPurple.copy(alpha = 0.2f)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircleOutline,
                  contentDescription = "Tamamlandı",
                  tint = FeatureAttendanceGreen,
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Tüm aktif ezberleriniz tamamlanmış!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Yeni ezber hedefleriniz ders halkasında hocanız tarafından tanımlanacaktır.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredActiveList, key = { it.id }) { record ->
              StudentActiveMemorizationDetailCard(record = record)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StudentMiniMetric(
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.08f),
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        fontSize = 10.sp,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun FilterStatusChip(
  title: String,
  isSelected: Boolean,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) color else CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else BorderStroke),
    modifier = modifier.height(32.dp)
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun StudentCurriculumItemCard(
  title: String,
  category: String,
  isCompleted: Boolean,
  record: MemorizationRecord?,
  categoryColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = if (isCompleted) CanvasSurface else CanvasSurface.copy(alpha = 0.9f),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isCompleted) FeatureAttendanceGreen.copy(alpha = 0.5f)
      else if (record != null && record.progressPercent > 0) FeatureStudentsBlue.copy(alpha = 0.4f)
      else BorderStroke
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
              if (isCompleted) FeatureAttendanceGreen.copy(alpha = 0.15f)
              else if (record != null && record.progressPercent > 0) FeatureStudentsBlue.copy(alpha = 0.15f)
              else CanvasBackground
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle
            else if (record != null && record.progressPercent > 0) Icons.Default.HourglassTop
            else Icons.Default.MenuBook,
            contentDescription = null,
            tint = if (isCompleted) FeatureAttendanceGreen
            else if (record != null && record.progressPercent > 0) FeatureStudentsBlue
            else TextMuted,
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.SemiBold,
            color = TextPrimary
          )
          if (isCompleted && record != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Teslim: ${record.date.ifBlank { "Tamamlandı" }}",
                fontSize = 11.sp,
                color = TextSecondary
              )
              if (record.rating > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                repeat(record.rating.coerceIn(1, 5)) {
                  Text("⭐", fontSize = 9.sp)
                }
              }
              if (record.repeatCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("• 🔄 ${record.repeatCount} Tekrar", fontSize = 10.5.sp, color = FeatureMemorizationPurple)
              }
            }
          } else if (record != null && record.progressPercent > 0) {
            Text(
              text = "Çalışılıyor: %${record.progressPercent} • ${record.teacherNotes.ifBlank { "Ders devam ediyor" }}",
              fontSize = 11.sp,
              color = FeatureStudentsBlue
            )
          } else {
            Text(
              text = "Müfredat Sırasında Bekliyor",
              fontSize = 11.sp,
              color = TextMuted
            )
          }
        }
      }

      // Status pill badge
      Surface(
        color = if (isCompleted) FeatureAttendanceGreen.copy(alpha = 0.15f)
        else if (record != null && record.progressPercent > 0) FeatureStudentsBlue.copy(alpha = 0.12f)
        else CanvasBackground,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (isCompleted) FeatureAttendanceGreen.copy(alpha = 0.3f)
          else if (record != null && record.progressPercent > 0) FeatureStudentsBlue.copy(alpha = 0.3f)
          else BorderStroke
        )
      ) {
        Text(
          text = if (isCompleted) "Tamamlandı ✅"
          else if (record != null && record.progressPercent > 0) "%${record.progressPercent} ⏳"
          else "Sırada 📖",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          color = if (isCompleted) FeatureAttendanceGreen
          else if (record != null && record.progressPercent > 0) FeatureStudentsBlue
          else TextSecondary,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
      }
    }
  }
}

@Composable
private fun TabPill(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  activeColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() },
    color = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
    shape = RoundedCornerShape(10.dp),
    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.5f)) else null
  ) {
    Row(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (isSelected) activeColor else TextSecondary,
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = title,
        fontSize = 11.5.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) activeColor else TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun StudentActiveMemorizationDetailCard(
  record: MemorizationRecord,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = CanvasSurface,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldSubtleBorderGradient)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = FeatureMemorizationPurple.copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = record.category,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FeatureMemorizationPurple,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
              color = if (record.status == "TEKRAR") TezhipGold.copy(alpha = 0.15f) else StatusBlue.copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = if (record.status == "TEKRAR") "Tekrar Aşamasında" else "Çalışılıyor",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (record.status == "TEKRAR") DarkSlateNavy else StatusBlue,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = record.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
        }

        // Hoca Ataması Rozeti
        Surface(
          color = EmeraldGreen.copy(alpha = 0.12f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "Hoca Ataması",
              tint = EmeraldGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Hoca Hedefi",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = EmeraldGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Progress bar
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Ezber İlerlemesi (Hoca Notu)",
            fontSize = 11.5.sp,
            color = TextSecondary
          )
          Text(
            text = "%${record.progressPercent}",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureMemorizationPurple
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
          progress = { (record.progressPercent / 100f).coerceIn(0f, 1f) },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = FeatureMemorizationPurple,
          trackColor = CanvasBackground
        )
      }

      if (record.teacherNotes.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          color = CanvasBackground,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.Notes,
              contentDescription = "Eğitmen Talimatı",
              tint = TezhipGold,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Eğitmen Talimatı & Not:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = record.teacherNotes,
                fontSize = 12.sp,
                color = TextPrimary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Informative footer for student
      Surface(
        color = FeatureAttendanceGreen.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.RecordVoiceOver,
            contentDescription = "Dinletme",
            tint = FeatureAttendanceGreen,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Ezberinizi hazırlayınca ders halkasında hocanıza dinletiniz.",
            fontSize = 11.5.sp,
            color = FeatureAttendanceGreen,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}

@Composable
fun StudentCompletedMemorizationDetailCard(
  record: MemorizationRecord,
  modifier: Modifier = Modifier
) {
  val catColor = getCategoryThemeColor(record.category)
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = CanvasSurface,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.4f)),
    shadowElevation = 1.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(FeatureAttendanceGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = "Tamamlandı",
              tint = FeatureAttendanceGreen,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = record.title,
              fontSize = 15.5.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                color = catColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = record.category,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = catColor,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Teslim: ${record.date.ifBlank { "Kayıtlı" }}",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }
        }

        Surface(
          color = FeatureAttendanceGreen.copy(alpha = 0.15f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = "Kabul Edildi ✅",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureAttendanceGreen,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Rating Stars & Repeat count
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Eğitmen Notu: ",
            fontSize = 11.5.sp,
            color = TextSecondary
          )
          repeat(record.rating.coerceIn(1, 5)) {
            Text(text = "⭐", fontSize = 12.sp)
          }
        }

        if (record.repeatCount > 0) {
          Surface(
            color = FeatureMemorizationPurple.copy(alpha = 0.1f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "🔄 ${record.repeatCount} Tekrar",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = FeatureMemorizationPurple,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        } else {
          Text(
            text = "%100 Başarı",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureAttendanceGreen
          )
        }
      }

      if (record.teacherNotes.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = CanvasBackground,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Comment, contentDescription = null, tint = TezhipGold, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Hoca Notu: ${record.teacherNotes}",
              fontSize = 11.5.sp,
              color = TextSecondary
            )
          }
        }
      }
    }
  }
}

private fun normalizeCategoryKey(cat: String): String {
  val lower = cat.lowercase(Locale.ROOT)
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") || lower.contains("cüz") -> "kuran"
    lower.contains("risale") || lower.contains("sozler") || lower.contains("nur") -> "risale"
    lower.contains("tesbihat") || lower.contains("dua") -> "tesbihat"
    lower.contains("hadis") -> "hadis"
    lower.contains("elif") || lower.contains("elifba") || lower.contains("tecvid") -> "elifba"
    else -> lower.trim()
  }
}

private fun getCategoryThemeColor(category: String): Color {
  val lower = category.lowercase(Locale.ROOT)
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") -> FeatureAttendanceGreen
    lower.contains("risale") || lower.contains("sozler") -> FeatureReportsOrange
    lower.contains("tesbihat") || lower.contains("dua") -> FeatureMemorizationPurple
    lower.contains("hadis") -> FeatureDevelopmentTeal
    lower.contains("elif") || lower.contains("tecvid") -> FeatureStudentsBlue
    else -> DeepBlueNavy
  }
}

private fun getCategoryIcon(category: String): String {
  val lower = category.lowercase(Locale.ROOT)
  return when {
    lower.contains("kuran") || lower.contains("kur'an") || lower.contains("sure") -> "📖"
    lower.contains("risale") || lower.contains("sozler") -> "📚"
    lower.contains("tesbihat") || lower.contains("dua") -> "📿"
    lower.contains("hadis") -> "📜"
    lower.contains("elif") || lower.contains("tecvid") -> "🌟"
    else -> "🏷️"
  }
}
