package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.CurriculumPreset
import com.example.data.util.CustomCurriculumItem
import com.example.data.util.ImportMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

/**
 * Dialog for selecting/unselecting and managing active curriculum items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumSelectionDialog(
  viewModel: AppViewModel,
  onDismiss: () -> Unit,
  onOpenImport: () -> Unit
) {
  val context = LocalContext.current
  val items by viewModel.curriculumItems.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryFilter by remember { mutableStateOf("Tümü") }

  var showAddNewItemDialog by remember { mutableStateOf(false) }
  var showAssignToStudentsDialog by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<CustomCurriculumItem?>(null) }
  var deletingItem by remember { mutableStateOf<CustomCurriculumItem?>(null) }

  val allCategories = remember(items) {
    val cats = items.map { it.category }.distinct()
    if (cats.isEmpty()) listOf("Kur'an", "Tesbihat", "Risale") else cats
  }

  val totalSelected = items.count { it.isSelected }
  val totalCount = items.size

  val filteredItems = items.filter { item ->
    val matchesCat = selectedCategoryFilter == "Tümü" || item.category.equals(selectedCategoryFilter, ignoreCase = true)
    val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery.trim(), ignoreCase = true) || item.category.contains(searchQuery.trim(), ignoreCase = true)
    matchesCat && matchesSearch
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      shape = RoundedCornerShape(24.dp),
      color = CanvasBackground
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // 1. Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FeatureMemorizationPurple.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Checklist,
                contentDescription = null,
                tint = FeatureMemorizationPurple,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Ezber Müfredatını Seç & Düzenle",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DeepBlueNavy
              )
              Text(
                text = "Takip edilecek ezberleri işaretleyin ($totalSelected / $totalCount seçili)",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Search & Add New Item Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Ezber veya sure adı ara...", fontSize = 13.sp) },
            leadingIcon = {
              Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                }
              }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .height(52.dp),
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
            )
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Add New Button
          Button(
            onClick = { showAddNewItemDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(52.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = "Ekle", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Yeni", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Category Filter Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            val isSel = selectedCategoryFilter == "Tümü"
            Surface(
              onClick = { selectedCategoryFilter = "Tümü" },
              shape = RoundedCornerShape(8.dp),
              color = if (isSel) DeepBlueNavy else CanvasSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) DeepBlueNavy else BorderLight),
              modifier = Modifier.height(30.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "Tümü (${items.size})",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSel) Color.White else TextPrimary,
                  maxLines = 1
                )
              }
            }
          }

          items(allCategories) { category ->
            val isSel = selectedCategoryFilter == category
            val count = items.count { it.category.equals(category, ignoreCase = true) }
            val selectedCount = items.count { it.category.equals(category, ignoreCase = true) && it.isSelected }
            Surface(
              onClick = { selectedCategoryFilter = category },
              shape = RoundedCornerShape(8.dp),
              color = if (isSel) FeatureStudentsBlue else CanvasSurface,
              border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) FeatureStudentsBlue else BorderLight),
              modifier = Modifier.height(30.dp)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "$category ($selectedCount/$count)",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSel) Color.White else TextPrimary,
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Quick Bulk Selection Bar (Tümünü Seç / Tümünü Kaldır / Şablon Yükle)
        Surface(
          color = CanvasSurface,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              TextButton(
                onClick = {
                  if (selectedCategoryFilter == "Tümü") {
                    viewModel.selectAllCurriculum(true)
                  } else {
                    viewModel.setCategoryCurriculumSelection(selectedCategoryFilter, true)
                  }
                  Toast.makeText(context, "Tümü seçildi", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tümünü Seç", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              }

              TextButton(
                onClick = {
                  if (selectedCategoryFilter == "Tümü") {
                    viewModel.selectAllCurriculum(false)
                  } else {
                    viewModel.setCategoryCurriculumSelection(selectedCategoryFilter, false)
                  }
                  Toast.makeText(context, "Seçimler kaldırıldı", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Icon(Icons.Default.Deselect, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Seçimi Kaldır", fontSize = 11.5.sp, color = StatusAbsentRed)
              }
            }

            OutlinedButton(
              onClick = {
                onDismiss()
                onOpenImport()
              },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureStudentsBlue)
            ) {
              Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Şablon Yükle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Items List
        if (filteredItems.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextMuted
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text("Aramanıza uygun ezber maddesi bulunamadı.", color = TextSecondary, fontSize = 13.sp)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(filteredItems, key = { it.id }) { item ->
              CurriculumItemRow(
                item = item,
                onToggle = { viewModel.toggleCurriculumItem(item.id) },
                onEdit = { editingItem = item },
                onDelete = { deletingItem = item }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Bottom Action Buttons (Talebeye Ezber Ata + Kaydet)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { showAssignToStudentsDialog = true },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FeatureMemorizationPurple),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, FeatureMemorizationPurple),
            modifier = Modifier
              .weight(1.1f)
              .height(48.dp)
              .testTag("curriculum_assign_to_students_button")
          ) {
            Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Talebeye Ezber Ata 🎯", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          }

          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy),
            modifier = Modifier
              .weight(1.2f)
              .height(48.dp)
              .testTag("curriculum_save_and_finish_button")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Kaydet ($totalSelected Madde)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }

  // DIALOG: ASSIGN CURRICULUM ITEMS TO STUDENTS
  if (showAssignToStudentsDialog) {
    val allStudents by viewModel.allStudents.collectAsState()
    val activeCurriculumSelected = items.filter { it.isSelected }

    AssignCurriculumToStudentsDialog(
      students = allStudents,
      selectedCurriculumItems = activeCurriculumSelected,
      onDismiss = { showAssignToStudentsDialog = false },
      onAssign = { targetStudentIds, notes ->
        viewModel.assignMemorizationsBulk(
          studentIds = targetStudentIds,
          items = activeCurriculumSelected.map { it.title to it.category },
          status = "VERILDI",
          notes = notes
        )
        showAssignToStudentsDialog = false
        val targetName = if (targetStudentIds.size == allStudents.size) "tüm sınıfa" else "${targetStudentIds.size} talebeye"
        Toast.makeText(context, "${activeCurriculumSelected.size} ezber $targetName başarıyla görev olarak atandı! 🎯", Toast.LENGTH_LONG).show()
      }
    )
  }

  // DIALOG: ADD NEW CUSTOM ITEM
  if (showAddNewItemDialog) {
    AddEditCurriculumItemDialog(
      initialItem = null,
      existingCategories = allCategories,
      onDismiss = { showAddNewItemDialog = false },
      onSave = { title, cat, selected ->
        viewModel.addCustomCurriculumItem(title, cat, selected)
        showAddNewItemDialog = false
        Toast.makeText(context, "'$title' müfredata eklendi.", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // DIALOG: EDIT ITEM
  editingItem?.let { item ->
    AddEditCurriculumItemDialog(
      initialItem = item,
      existingCategories = allCategories,
      onDismiss = { editingItem = null },
      onSave = { title, cat, selected ->
        viewModel.updateCustomCurriculumItem(item.id, title, cat, selected)
        editingItem = null
        Toast.makeText(context, "Ezber güncellendi.", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // DIALOG: DELETE ITEM CONFIRMATION
  deletingItem?.let { item ->
    AlertDialog(
      onDismissRequest = { deletingItem = null },
      title = { Text("Ezber Maddesini Sil?", fontWeight = FontWeight.Bold) },
      text = { Text("'${item.title}' (${item.category}) listeden kaldırılacaktır. Devam etmek istiyor musunuz?") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteCustomCurriculumItem(item.id)
            deletingItem = null
            Toast.makeText(context, "Ezber silindi.", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusAbsentRed)
        ) {
          Text("Sil")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingItem = null }) {
          Text("İptal")
        }
      }
    )
  }
}

@Composable
fun CurriculumItemRow(
  item: CustomCurriculumItem,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val catColor = when (item.category.lowercase()) {
    "kuran", "kur'an" -> FeatureAttendanceGreen
    "tesbihat" -> FeatureMemorizationPurple
    "risale" -> FeatureReportsOrange
    "hadis", "40 hadis" -> Color(0xFF0284C7)
    "sureler", "kısa sureler" -> Color(0xFF0D9488)
    "elifba", "elif-bâ & tecvid" -> Color(0xFFE11D48)
    else -> FeatureStudentsBlue
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (item.isSelected) CanvasSurface else CanvasSurface.copy(alpha = 0.6f),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (item.isSelected) catColor.copy(alpha = 0.4f) else BorderLight
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggle() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Checkbox
      Checkbox(
        checked = item.isSelected,
        onCheckedChange = { onToggle() },
        colors = CheckboxDefaults.colors(checkedColor = catColor)
      )

      Spacer(modifier = Modifier.width(6.dp))

      // Title & Category tag
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.title,
          fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 14.sp,
          color = if (item.isSelected) DeepBlueNavy else TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(top = 2.dp)
        ) {
          Surface(
            color = catColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = item.category,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              color = catColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
          }
        }
      }

      // Edit Button
      IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Düzenle",
          tint = TextMuted,
          modifier = Modifier.size(16.dp)
        )
      }

      // Delete Button
      IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Sil",
          tint = StatusAbsentRed.copy(alpha = 0.7f),
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

/**
 * Dialog to Add or Edit a single curriculum item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCurriculumItemDialog(
  initialItem: CustomCurriculumItem?,
  existingCategories: List<String>,
  onDismiss: () -> Unit,
  onSave: (title: String, category: String, isSelected: Boolean) -> Unit
) {
  var title by remember { mutableStateOf(initialItem?.title ?: "") }
  var category by remember { mutableStateOf(initialItem?.category ?: existingCategories.firstOrNull() ?: "Kur'an") }
  var isSelected by remember { mutableStateOf(initialItem?.isSelected ?: true) }
  var isCustomCat by remember { mutableStateOf(false) }
  var customCatText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (initialItem == null) "Yeni Ezber Ekle" else "Ezber Maddesini Düzenle",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Ezber Başlığı / Sûre Adı") },
          placeholder = { Text("örn: Fatiha Sûresi, Subhaneke...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Category selection
        Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(existingCategories) { cat ->
            FilterChip(
              selected = !isCustomCat && category == cat,
              onClick = {
                isCustomCat = false
                category = cat
              },
              label = { Text(cat, fontSize = 11.5.sp) }
            )
          }

          item {
            FilterChip(
              selected = isCustomCat,
              onClick = { isCustomCat = true },
              label = { Text("+ Yeni Kategori", fontSize = 11.5.sp) }
            )
          }
        }

        if (isCustomCat) {
          OutlinedTextField(
            value = customCatText,
            onValueChange = { customCatText = it },
            label = { Text("Yeni Kategori Adı") },
            placeholder = { Text("örn: Hadis, Elifba...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { isSelected = !isSelected }
        ) {
          Checkbox(checked = isSelected, onCheckedChange = { isSelected = it })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Ezber Takibinde Aktif Olsun", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalCat = if (isCustomCat) customCatText.trim().ifBlank { "Özel" } else category
          if (title.isNotBlank()) {
            onSave(title.trim(), finalCat, isSelected)
          }
        },
        enabled = title.isNotBlank() && (!isCustomCat || customCatText.isNotBlank())
      ) {
        Text("Kaydet")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("İptal")
      }
    }
  )
}

/**
 * Comprehensive Import Dialog for loading custom curriculum lists, presets, JSON, CSV or pasted text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumImportDialog(
  viewModel: AppViewModel,
  onDismiss: () -> Unit,
  onCompleted: () -> Unit
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Hazır Şablonlar, 1: Hızlı Metin Yapıştır, 2: JSON & CSV

  // Text Paste state
  var pastedText by remember { mutableStateOf("") }
  var pasteDefaultCategory by remember { mutableStateOf("Kur'an") }
  var pasteOverwriteMode by remember { mutableStateOf(false) }

  // JSON/CSV state
  var rawDataText by remember { mutableStateOf("") }
  var formatChoice by remember { mutableStateOf("JSON") } // "JSON", "CSV"
  var jsonOverwriteMode by remember { mutableStateOf(false) }

  // Sample View state
  var showSampleViewerDialog by remember { mutableStateOf<String?>(null) }

  // File Picker for reading curriculum files (.json, .csv, .txt)
  val curriculumFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
          stringBuilder.append(line).append("\n")
        }
        reader.close()
        inputStream?.close()

        val fileContent = stringBuilder.toString().trim()
        if (fileContent.isNotBlank()) {
          if (fileContent.startsWith("[") || fileContent.startsWith("{")) {
            selectedTab = 2
            formatChoice = "JSON"
            rawDataText = fileContent
            Toast.makeText(context, "JSON dosyası yüklendi, aktarmak için butona basın.", Toast.LENGTH_SHORT).show()
          } else if (fileContent.contains(",") || fileContent.contains(";")) {
            selectedTab = 2
            formatChoice = "CSV"
            rawDataText = fileContent
            Toast.makeText(context, "CSV dosyası yüklendi, aktarmak için butona basın.", Toast.LENGTH_SHORT).show()
          } else {
            selectedTab = 1
            pastedText = fileContent
            Toast.makeText(context, "Metin dosyası yüklendi, aktarmak için butona basın.", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Dosya okuma hatası: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      shape = RoundedCornerShape(24.dp),
      color = CanvasBackground
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FeatureStudentsBlue.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                tint = FeatureStudentsBlue,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Farklı Ezber Listesi Yükle",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DeepBlueNavy
              )
              Text(
                text = "Hazır şablon seçin veya kendi listenizi yapıştırın",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        PrimaryTabRow(
          selectedTabIndex = selectedTab,
          containerColor = CanvasSurface,
          contentColor = FeatureStudentsBlue
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("🏛️ Hazır Şablonlar", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("📋 Metin Yapıştır", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("💾 JSON / CSV", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) }
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Contents
        when (selectedTab) {
          0 -> {
            // TAB 0: READY-MADE PRESETS
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              item {
                Text(
                  text = "Aşağıdaki hazır müfredat paketlerinden birini seçerek anında sisteminize yükleyebilirsiniz:",
                  fontSize = 12.5.sp,
                  color = TextSecondary
                )
              }

              items(CurriculumPreset.values()) { preset ->
                PresetCard(
                  preset = preset,
                  onApply = { overwrite ->
                    viewModel.loadCurriculumPreset(preset, overwrite)
                    Toast.makeText(context, "${preset.displayName} müfredatı yüklendi!", Toast.LENGTH_SHORT).show()
                    onCompleted()
                  }
                )
              }
            }
          }

          1 -> {
            // TAB 1: QUICK PLAIN TEXT PASTE
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Surface(
                color = FeatureStudentsBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Info, contentDescription = null, tint = FeatureStudentsBlue, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Ezberlemek istediğiniz sure/dua/ders isimlerini alt alta yapıştırın. İsteğe göre [Kategori] başlığı da kullanabilirsiniz.",
                    fontSize = 11.5.sp,
                    color = DeepBlueNavy
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("Varsayılan Kategori:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  listOf("Kur'an", "Dualar", "Hadis", "Risale", "Elifba").forEach { cat ->
                    FilterChip(
                      selected = pasteDefaultCategory == cat,
                      onClick = { pasteDefaultCategory = cat },
                      label = { Text(cat, fontSize = 11.sp) }
                    )
                  }
                }
              }

              OutlinedTextField(
                value = pastedText,
                onValueChange = { pastedText = it },
                placeholder = {
                  Text(
                    "Örnek Liste:\n[Kur'an]\nFatiha Sûresi\nAyetel Kürsi\nAmenerrasulü\n\n[Dualar]\nSübhaneke\nEttehiyyâtü",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                  )
                },
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.clickable { pasteOverwriteMode = !pasteOverwriteMode }
                ) {
                  Checkbox(checked = pasteOverwriteMode, onCheckedChange = { pasteOverwriteMode = it })
                  Text("Mevcut listeyi silip üzerine yaz", fontSize = 11.5.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  OutlinedButton(
                    onClick = { curriculumFilePickerLauncher.launch("*/*") },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dosya Seç", fontSize = 11.sp)
                  }

                  TextButton(
                    onClick = {
                      val sample = viewModel.getSampleCurriculumPlainText()
                      pastedText = sample
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Örnek Şablon", fontSize = 11.sp)
                  }
                }
              }

              Button(
                onClick = {
                  val result = viewModel.importCurriculumPlainText(pastedText, pasteDefaultCategory, pasteOverwriteMode)
                  if (result.isSuccess) {
                    Toast.makeText(context, "${result.getOrNull()} adet ezber başarıyla aktarıldı!", Toast.LENGTH_LONG).show()
                    onCompleted()
                  } else {
                    Toast.makeText(context, "Hata: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                  }
                },
                enabled = pastedText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen)
              ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Listeyi Sisteme Yükle ve Başlat", fontWeight = FontWeight.Bold)
              }
            }
          }

          2 -> {
            // TAB 2: JSON & CSV IMPORT
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  FilterChip(
                    selected = formatChoice == "JSON",
                    onClick = {
                      formatChoice = "JSON"
                      if (rawDataText.isBlank()) rawDataText = viewModel.getSampleCurriculumJson()
                    },
                    label = { Text("JSON Formatı") }
                  )
                  FilterChip(
                    selected = formatChoice == "CSV",
                    onClick = {
                      formatChoice = "CSV"
                      if (rawDataText.isBlank()) rawDataText = viewModel.getSampleCurriculumCsv()
                    },
                    label = { Text("CSV / Excel") }
                  )
                }

                TextButton(
                  onClick = {
                    val sample = if (formatChoice == "JSON") viewModel.getSampleCurriculumJson() else viewModel.getSampleCurriculumCsv()
                    showSampleViewerDialog = sample
                  }
                ) {
                  Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Örnek Şablonu İncele", fontSize = 11.5.sp)
                }
              }

              OutlinedTextField(
                value = rawDataText,
                onValueChange = { rawDataText = it },
                placeholder = { Text("JSON veya CSV verisini buraya yapıştırın...", fontSize = 12.sp) },
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.clickable { jsonOverwriteMode = !jsonOverwriteMode }
                ) {
                  Checkbox(checked = jsonOverwriteMode, onCheckedChange = { jsonOverwriteMode = it })
                  Text("Tüm eski listeyi silip üzerine yaz", fontSize = 11.5.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  OutlinedButton(
                    onClick = { curriculumFilePickerLauncher.launch("*/*") },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dosya Seç", fontSize = 11.sp)
                  }

                  TextButton(
                    onClick = {
                      rawDataText = if (formatChoice == "JSON") viewModel.getSampleCurriculumJson() else viewModel.getSampleCurriculumCsv()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("Örnek Veri", fontSize = 11.sp)
                  }
                }
              }

              Button(
                onClick = {
                  val result = if (formatChoice == "JSON") {
                    viewModel.importCurriculumJson(rawDataText, jsonOverwriteMode)
                  } else {
                    viewModel.importCurriculumCsv(rawDataText, jsonOverwriteMode)
                  }

                  if (result.isSuccess) {
                    Toast.makeText(context, "${result.getOrNull()} adet ezber başarıyla yüklendi!", Toast.LENGTH_LONG).show()
                    onCompleted()
                  } else {
                    Toast.makeText(context, "İçe aktarma hatası: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                  }
                },
                enabled = rawDataText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen)
              ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("$formatChoice Verisini Sisteme Aktar", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  // DIALOG: SAMPLE TEMPLATE VIEWER & COPIER
  showSampleViewerDialog?.let { sampleText ->
    AlertDialog(
      onDismissRequest = { showSampleViewerDialog = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Code, contentDescription = null, tint = FeatureStudentsBlue)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Örnek Şablon Formatı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            "Bu şablonu kopyalayarak Notepad veya Excel'de düzenleyebilir ve doğrudan sisteme yükleyebilirsiniz:",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
          ) {
            LazyColumn(modifier = Modifier.padding(10.dp)) {
              item {
                Text(
                  text = sampleText,
                  color = Color(0xFFE2E8F0),
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.5.sp
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Curriculum Template", sampleText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Örnek şablon panoya kopyalandı!", Toast.LENGTH_SHORT).show()
            showSampleViewerDialog = null
          }
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Şablonu Kopyala")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSampleViewerDialog = null }) {
          Text("Kapat")
        }
      }
    )
  }
}

@Composable
fun PresetCard(
  preset: CurriculumPreset,
  onApply: (overwrite: Boolean) -> Unit
) {
  var showConfirmDialog by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = CanvasSurface,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = preset.displayName,
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp,
          color = DeepBlueNavy
        )
        Surface(
          color = FeatureStudentsBlue.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = preset.badge,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureStudentsBlue,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(text = preset.description, fontSize = 12.sp, color = TextSecondary)

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { onApply(false) },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Text("Mevcuda Ekle", fontSize = 11.5.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = { showConfirmDialog = true },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.height(34.dp),
          colors = ButtonDefaults.buttonColors(containerColor = FeatureStudentsBlue)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Bu Paketi Yükle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (showConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmDialog = false },
      title = { Text("Müfredat Paketini Yükle?", fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "'${preset.displayName}' paketi yüklenecektir. Eski liste sıfırlanıp yerine bu paket getirilsin mi?"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showConfirmDialog = false
            onApply(true)
          }
        ) {
          Text("Evet, Sıfırdan Yükle")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDialog = false }) {
          Text("İptal")
        }
      }
    )
  }
}

/**
 * Dialog for assigning selected curriculum items in bulk to one or all students.
 */
@Composable
fun AssignCurriculumToStudentsDialog(
  students: List<com.example.data.model.Student>,
  selectedCurriculumItems: List<CustomCurriculumItem>,
  onDismiss: () -> Unit,
  onAssign: (studentIds: List<Long>, notes: String) -> Unit
) {
  var assignToAll by remember { mutableStateOf(false) }
  var selectedStudentIds by remember {
    mutableStateOf(if (students.isNotEmpty()) setOf(students.first().id) else emptySet())
  }
  var notesText by remember { mutableStateOf("Hoca tarafından müfredat ezber görevi olarak atandı") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = CanvasSurface,
      border = androidx.compose.foundation.BorderStroke(1.2.dp, BorderLight),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(FeatureMemorizationPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AssignmentInd,
              contentDescription = null,
              tint = FeatureMemorizationPurple,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Müfredattan Ezber Görevi Ata",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = TextPrimary
            )
            Text(
              text = "${selectedCurriculumItems.size} ezber maddesi atanacak",
              fontSize = 11.5.sp,
              color = FeatureAttendanceGreen,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hedef Talebe Seçimi: [ Tüm Sınıf ] veya Tekil Seçim
        Text(
          text = "Hedef Talebe / Sınıf:",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            onClick = {
              assignToAll = true
              selectedStudentIds = students.map { it.id }.toSet()
            },
            shape = RoundedCornerShape(8.dp),
            color = if (assignToAll) DeepBlueNavy else CanvasBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (assignToAll) DeepBlueNavy else BorderLight),
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = if (assignToAll) Color.White else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Tüm Sınıfa Ata (${students.size})",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (assignToAll) Color.White else TextPrimary
              )
            }
          }

          Surface(
            onClick = { assignToAll = false },
            shape = RoundedCornerShape(8.dp),
            color = if (!assignToAll) FeatureStudentsBlue else CanvasBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (!assignToAll) FeatureStudentsBlue else BorderLight),
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (!assignToAll) Color.White else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Seçili Talebeler",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (!assignToAll) Color.White else TextPrimary
              )
            }
          }
        }

        if (!assignToAll) {
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(students) { st ->
              val isSel = selectedStudentIds.contains(st.id)
              Surface(
                onClick = {
                  selectedStudentIds = if (isSel) {
                    if (selectedStudentIds.size > 1) selectedStudentIds - st.id else selectedStudentIds
                  } else {
                    selectedStudentIds + st.id
                  }
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isSel) FeatureStudentsBlue.copy(alpha = 0.15f) else CanvasBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) FeatureStudentsBlue else BorderLight)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (isSel) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = FeatureStudentsBlue, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                  }
                  Text(
                    text = st.fullName,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) FeatureStudentsBlue else TextPrimary
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hoca Notu / Talimatı
        OutlinedTextField(
          value = notesText,
          onValueChange = { notesText = it },
          label = { Text("Hoca Notu / Talimat", fontSize = 11.sp) },
          placeholder = { Text("Örn: Haftaya kadar dinlenecek", fontSize = 11.5.sp) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Atanacak Ezber Maddeleri Önizleme
        Surface(
          color = CanvasBackground,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 120.dp)
        ) {
          LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            item {
              Text(
                text = "Atanacak Ezberler (${selectedCurriculumItems.size}):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
            }
            items(selectedCurriculumItems.take(20)) { item ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FeatureAttendanceGreen, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${item.title} (${item.category})",
                  fontSize = 11.5.sp,
                  color = TextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            if (selectedCurriculumItems.size > 20) {
              item {
                Text(
                  text = "... ve ${selectedCurriculumItems.size - 20} diğer ezber maddesi",
                  fontSize = 10.5.sp,
                  color = TextSecondary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Butonlar (İptal / Ata)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("İptal", color = TextSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val targetIds = if (assignToAll) students.map { it.id } else selectedStudentIds.toList()
              if (targetIds.isNotEmpty() && selectedCurriculumItems.isNotEmpty()) {
                onAssign(targetIds, notesText.trim())
              }
            },
            enabled = (assignToAll || selectedStudentIds.isNotEmpty()) && selectedCurriculumItems.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = FeatureAttendanceGreen),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Görevleri Ata 🎯", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }
}

