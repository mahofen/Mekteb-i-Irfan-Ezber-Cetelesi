package com.example.data.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CurriculumData
import com.example.data.model.CurriculumItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomCurriculumItem(
  val id: String,
  val title: String,
  val category: String,
  val isSelected: Boolean = true,
  val orderIndex: Int = 0
)

enum class CurriculumPreset(val displayName: String, val description: String, val badge: String) {
  DEFAULT_IRFAN_108(
    "Klasik Mekteb-i İrfan (108 Madde)",
    "Kur'an (28), Tesbihat (34) ve Risale (46) tam müfredatı",
    "🕌 108 Ders"
  ),
  NAMAZ_SURELERI_DUALARI(
    "Temel Namaz Sureleri & Duaları",
    "Fatiha, Fil, Kureyş, Maun, Kevser, İhlas, Felak, Nas, Ayetel Kürsi, Sübhaneke, Ettehiyyatü...",
    "📖 20 Ders"
  ),
  YAZ_KURSU_ELIFBA(
    "Yaz Kur'an Kursu & Elif-Bâ",
    "Harfler, Cezm, Şedde, Med, Tenvin, Tecvid kaideleri ve temel dualar",
    "🌟 24 Ders"
  ),
  HADIS_VE_ILMIHAL(
    "40 Hadis-i Şerif & İlmihal",
    "40 Hadis seçkisi, 32 Farz, 54 Farz, Abdest & Gusül Farzları",
    "📜 25 Ders"
  ),
  CUZLER_VE_ASR(
    "Aşr-ı Şerifler & Cüz Takibi",
    "Yâsîn, Mülk, Nebe, Fetih, Rahmân, Vâkıa, Cuma ve Seçme Cüzler",
    "🟢 15 Ders"
  )
}

class CurriculumManager private constructor(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("custom_curriculum_prefs", Context.MODE_PRIVATE)

  private val _itemsFlow = MutableStateFlow<List<CustomCurriculumItem>>(emptyList())
  val itemsFlow: StateFlow<List<CustomCurriculumItem>> = _itemsFlow.asStateFlow()

  init {
    loadFromStorage()
  }

  companion object {
    @Volatile
    private var INSTANCE: CurriculumManager? = null

    fun getInstance(context: Context): CurriculumManager {
      return INSTANCE ?: synchronized(this) {
        val instance = CurriculumManager(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }
  }

  private fun loadFromStorage() {
    val json = prefs.getString("curriculum_items_json", null)
    if (json.isNullOrBlank()) {
      // Seed with standard 108 items
      val defaultItems = createDefaultIrfanItems()
      saveItems(defaultItems)
    } else {
      try {
        val list = mutableListOf<CustomCurriculumItem>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            CustomCurriculumItem(
              id = obj.optString("id", UUID.randomUUID().toString()),
              title = obj.getString("title"),
              category = obj.optString("category", "Kur'an"),
              isSelected = obj.optBoolean("isSelected", true),
              orderIndex = obj.optInt("orderIndex", i)
            )
          )
        }
        _itemsFlow.value = list
      } catch (e: Exception) {
        val defaultItems = createDefaultIrfanItems()
        saveItems(defaultItems)
      }
    }
  }

  fun saveItems(list: List<CustomCurriculumItem>) {
    _itemsFlow.value = list
    val array = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("title", item.title)
      obj.put("category", item.category)
      obj.put("isSelected", item.isSelected)
      obj.put("orderIndex", item.orderIndex)
      array.put(obj)
    }
    prefs.edit().putString("curriculum_items_json", array.toString()).apply()
  }

  fun getAllItems(): List<CustomCurriculumItem> = _itemsFlow.value

  fun getActiveItems(): List<CustomCurriculumItem> = _itemsFlow.value.filter { it.isSelected }

  fun getActiveCategories(): List<String> {
    val categories = _itemsFlow.value.filter { it.isSelected }.map { it.category }.distinct()
    return if (categories.isEmpty()) listOf("Kur'an", "Tesbihat", "Risale") else categories
  }

  fun getAllCategories(): List<String> {
    val categories = _itemsFlow.value.map { it.category }.distinct()
    return if (categories.isEmpty()) listOf("Kur'an", "Tesbihat", "Risale") else categories
  }

  fun getSelectedTitlesForCategory(category: String): List<String> {
    return _itemsFlow.value
      .filter { it.isSelected && it.category.equals(category, ignoreCase = true) }
      .sortedBy { it.orderIndex }
      .map { it.title }
  }

  fun toggleItemSelection(id: String) {
    val updated = _itemsFlow.value.map {
      if (it.id == id) it.copy(isSelected = !it.isSelected) else it
    }
    saveItems(updated)
  }

  fun setItemSelection(id: String, selected: Boolean) {
    val updated = _itemsFlow.value.map {
      if (it.id == id) it.copy(isSelected = selected) else it
    }
    saveItems(updated)
  }

  fun setCategorySelection(category: String, selected: Boolean) {
    val updated = _itemsFlow.value.map {
      if (it.category.equals(category, ignoreCase = true)) it.copy(isSelected = selected) else it
    }
    saveItems(updated)
  }

  fun selectAll(selected: Boolean) {
    val updated = _itemsFlow.value.map { it.copy(isSelected = selected) }
    saveItems(updated)
  }

  fun addItem(title: String, category: String, isSelected: Boolean = true): CustomCurriculumItem {
    val current = _itemsFlow.value.toMutableList()
    val newItem = CustomCurriculumItem(
      id = "custom_" + UUID.randomUUID().toString().take(8),
      title = title.trim(),
      category = category.trim().ifBlank { "Özel" },
      isSelected = isSelected,
      orderIndex = current.size
    )
    current.add(newItem)
    saveItems(current)
    return newItem
  }

  fun updateItem(id: String, newTitle: String, newCategory: String, isSelected: Boolean) {
    val updated = _itemsFlow.value.map {
      if (it.id == id) {
        it.copy(title = newTitle.trim(), category = newCategory.trim(), isSelected = isSelected)
      } else it
    }
    saveItems(updated)
  }

  fun deleteItem(id: String) {
    val updated = _itemsFlow.value.filter { it.id != id }
    saveItems(updated)
  }

  fun resetToDefault() {
    saveItems(createDefaultIrfanItems())
  }

  fun loadPreset(preset: CurriculumPreset, overwrite: Boolean = true) {
    val presetItems = when (preset) {
      CurriculumPreset.DEFAULT_IRFAN_108 -> createDefaultIrfanItems()
      CurriculumPreset.NAMAZ_SURELERI_DUALARI -> createNamazSureleriDualariItems()
      CurriculumPreset.YAZ_KURSU_ELIFBA -> createYazKursuElifbaItems()
      CurriculumPreset.HADIS_VE_ILMIHAL -> createHadisVeIlmihalItems()
      CurriculumPreset.CUZLER_VE_ASR -> createCuzlerVeAsrItems()
    }

    if (overwrite) {
      saveItems(presetItems)
    } else {
      // Merge unique by title & category
      val current = _itemsFlow.value.toMutableList()
      for (p in presetItems) {
        if (current.none { it.title.equals(p.title, ignoreCase = true) && it.category.equals(p.category, ignoreCase = true) }) {
          current.add(p.copy(id = "preset_" + UUID.randomUUID().toString().take(8), orderIndex = current.size))
        }
      }
      saveItems(current)
    }
  }

  // -------------------------------------------------------------
  // IMPORT IMPLEMENTATIONS
  // -------------------------------------------------------------

  fun importFromJson(jsonStr: String, overwrite: Boolean = false): Result<Int> {
    return try {
      val parsedItems = mutableListOf<CustomCurriculumItem>()
      val cleanJson = jsonStr.trim()
      val array = if (cleanJson.startsWith("[")) {
        JSONArray(cleanJson)
      } else if (cleanJson.startsWith("{")) {
        val root = JSONObject(cleanJson)
        root.optJSONArray("items") ?: root.optJSONArray("curriculum") ?: JSONArray()
      } else {
        throw IllegalArgumentException("Geçersiz JSON biçimi")
      }

      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val title = obj.getString("title")
        val category = obj.optString("category", "Genel")
        val isSelected = obj.optBoolean("isSelected", true)
        parsedItems.add(
          CustomCurriculumItem(
            id = "imp_" + UUID.randomUUID().toString().take(8),
            title = title,
            category = category,
            isSelected = isSelected,
            orderIndex = i
          )
        )
      }

      if (parsedItems.isEmpty()) {
        return Result.failure(Exception("JSON içerisinde geçerli ezber kaydı bulunamadı."))
      }

      if (overwrite) {
        saveItems(parsedItems)
      } else {
        val current = _itemsFlow.value.toMutableList()
        var addedCount = 0
        for (item in parsedItems) {
          if (current.none { it.title.equals(item.title, ignoreCase = true) && it.category.equals(item.category, ignoreCase = true) }) {
            current.add(item.copy(orderIndex = current.size))
            addedCount++
          }
        }
        saveItems(current)
      }

      Result.success(parsedItems.size)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun importFromCsv(csvStr: String, overwrite: Boolean = false): Result<Int> {
    return try {
      val lines = csvStr.lines().map { it.trim() }.filter { it.isNotBlank() }
      val parsedItems = mutableListOf<CustomCurriculumItem>()

      for ((index, line) in lines.withIndex()) {
        // Skip header if matches Kategori/Category
        if (index == 0 && (line.contains("Kategori", ignoreCase = true) || line.contains("Category", ignoreCase = true))) {
          continue
        }

        val parts = line.split(Regex("[,;\t]")).map { it.trim().trim('"', '\'') }
        if (parts.isEmpty()) continue

        val category: String
        val title: String
        val isSelected: Boolean

        if (parts.size >= 2) {
          category = parts[0].ifBlank { "Genel" }
          title = parts[1]
          isSelected = if (parts.size >= 3) {
            parts[2].equals("true", ignoreCase = true) || parts[2].equals("1") || parts[2].equals("evet", ignoreCase = true)
          } else true
        } else {
          category = "Genel"
          title = parts[0]
          isSelected = true
        }

        if (title.isNotBlank()) {
          parsedItems.add(
            CustomCurriculumItem(
              id = "csv_" + UUID.randomUUID().toString().take(8),
              title = title,
              category = category,
              isSelected = isSelected,
              orderIndex = parsedItems.size
            )
          )
        }
      }

      if (parsedItems.isEmpty()) {
        return Result.failure(Exception("CSV içerisinde ezber maddesi tespit edilemedi."))
      }

      if (overwrite) {
        saveItems(parsedItems)
      } else {
        val current = _itemsFlow.value.toMutableList()
        for (item in parsedItems) {
          if (current.none { it.title.equals(item.title, ignoreCase = true) && it.category.equals(item.category, ignoreCase = true) }) {
            current.add(item.copy(orderIndex = current.size))
          }
        }
        saveItems(current)
      }

      Result.success(parsedItems.size)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun importFromPlainText(
    text: String,
    defaultCategory: String = "Kur'an",
    overwrite: Boolean = false
  ): Result<Int> {
    return try {
      val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
      var currentCategory = defaultCategory
      val parsedItems = mutableListOf<CustomCurriculumItem>()

      for (line in lines) {
        // Check for category header [Kategori Adı] or # Kategori Adı
        if (line.startsWith("[") && line.endsWith("]")) {
          currentCategory = line.substring(1, line.length - 1).trim()
          continue
        }
        if (line.startsWith("#")) {
          currentCategory = line.removePrefix("#").trim()
          continue
        }

        // Check for "Kategori: Başlık" format
        if (line.contains(":") && !line.startsWith("http")) {
          val parts = line.split(":", limit = 2)
          val cat = parts[0].trim()
          val ttl = parts[1].trim()
          if (cat.length in 2..30 && ttl.isNotBlank()) {
            parsedItems.add(
              CustomCurriculumItem(
                id = "txt_" + UUID.randomUUID().toString().take(8),
                title = ttl,
                category = cat,
                isSelected = true,
                orderIndex = parsedItems.size
              )
            )
            continue
          }
        }

        // Standard bullet or item line (e.g. "1. Fatiha", "- Fatiha", "Fatiha")
        val cleanTitle = line.replace(Regex("^(\\d+[.)-]\\s*|[-*•]\\s*)"), "").trim()
        if (cleanTitle.isNotBlank()) {
          parsedItems.add(
            CustomCurriculumItem(
              id = "txt_" + UUID.randomUUID().toString().take(8),
              title = cleanTitle,
              category = currentCategory,
              isSelected = true,
              orderIndex = parsedItems.size
            )
          )
        }
      }

      if (parsedItems.isEmpty()) {
        return Result.failure(Exception("Metinden herhangi bir ezber maddesi okunamadı."))
      }

      if (overwrite) {
        saveItems(parsedItems)
      } else {
        val current = _itemsFlow.value.toMutableList()
        for (item in parsedItems) {
          if (current.none { it.title.equals(item.title, ignoreCase = true) && it.category.equals(item.category, ignoreCase = true) }) {
            current.add(item.copy(orderIndex = current.size))
          }
        }
        saveItems(current)
      }

      Result.success(parsedItems.size)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // -------------------------------------------------------------
  // EXPORT & SAMPLE TEMPLATES
  // -------------------------------------------------------------

  fun exportToJson(): String {
    val items = _itemsFlow.value
    val array = JSONArray()
    for (item in items) {
      val obj = JSONObject()
      obj.put("title", item.title)
      obj.put("category", item.category)
      obj.put("isSelected", item.isSelected)
      array.put(obj)
    }
    return array.toString(2)
  }

  fun exportToCsv(): String {
    val sb = StringBuilder()
    sb.append("Kategori,Ezber Başlığı,Seçili\n")
    for (item in _itemsFlow.value) {
      sb.append("\"${item.category}\",\"${item.title}\",${if (item.isSelected) "EVET" else "HAYIR"}\n")
    }
    return sb.toString()
  }

  fun exportToPlainText(): String {
    val sb = StringBuilder()
    val grouped = _itemsFlow.value.groupBy { it.category }
    for ((cat, items) in grouped) {
      sb.append("[$cat]\n")
      for (it in items) {
        val prefix = if (it.isSelected) "✓ " else "○ "
        sb.append("$prefix${it.title}\n")
      }
      sb.append("\n")
    }
    return sb.toString().trim()
  }

  fun getSampleTemplateJson(): String {
    return """
[
  {
    "category": "Kur'an",
    "title": "Fatiha Sûresi",
    "isSelected": true
  },
  {
    "category": "Kur'an",
    "title": "Ayetel Kürsi (Bakara 255)",
    "isSelected": true
  },
  {
    "category": "Kur'an",
    "title": "Amenerrasulü (Bakara 285-286)",
    "isSelected": true
  },
  {
    "category": "Kur'an",
    "title": "Hüvallahüllezi (Haşr 22-24)",
    "isSelected": true
  },
  {
    "category": "Dualar",
    "title": "Sübhaneke Duası",
    "isSelected": true
  },
  {
    "category": "Dualar",
    "title": "Ettehiyyâtü",
    "isSelected": true
  },
  {
    "category": "Hadis",
    "title": "1. Hadis: Ameller Niyetlere Göredir",
    "isSelected": true
  }
]
    """.trimIndent()
  }

  fun getSampleTemplateCsv(): String {
    return """
Kategori,Ezber Başlığı,Seçili
Kur'an,Fatiha Sûresi,EVET
Kur'an,Ayetel Kürsi,EVET
Kur'an,Amenerrasulü,EVET
Kur'an,İhlas Sûresi,EVET
Kur'an,Felak Sûresi,EVET
Kur'an,Nas Sûresi,EVET
Dualar,Sübhaneke,EVET
Dualar,Ettehiyyâtü,EVET
Dualar,Allahümme Salli & Barik,EVET
Dualar,Rabbena Duaları,EVET
Hadis,1. Hadis: Din Samimiyettir,EVET
    """.trimIndent()
  }

  fun getSampleTemplatePlainText(): String {
    return """
[Kur'an]
Fatiha Sûresi
Duhâ Sûresi
İnşirâh Sûresi
Tin Sûresi
Kadir Sûresi
Fil Sûresi
Kureyş Sûresi
Mâûn Sûresi
Kevser Sûresi
Kâfirûn Sûresi
Nasr Sûresi
Tebbet Sûresi
İhlâs Sûresi
Felâk Sûresi
Nâs Sûresi
Ayetel Kürsi
Amenerrasulü

[Namaz Duaları]
Sübhaneke
Ettehiyyâtü
Allahümme Salli
Allahümme Barik
Rabbena Âtinâ
Rabbic'alnî
Kunut Duaları
Ezan & Kâmet

[40 Hadis & İlmihal]
1. Hadis: Ameller Niyetlere Göredir
2. Hadis: Müslüman Müslümanın Kardeşidir
3. Hadis: Temizlik İmandandır
32 Farz
54 Farz
    """.trimIndent()
  }

  // -------------------------------------------------------------
  // DEFAULT PRESETS GENERATOR
  // -------------------------------------------------------------

  private fun createDefaultIrfanItems(): List<CustomCurriculumItem> {
    val list = mutableListOf<CustomCurriculumItem>()
    var order = 0

    // 1. Kur'an (28)
    val kuranList = listOf(
      "Duhâ Sûresi", "İnşirâh Sûresi", "Tin Sûresi", "Alak Sûresi", "Kadir Sûresi",
      "Beyyine Sûresi", "Zilzâl Sûresi", "Âdiyât Sûresi", "Kâria Sûresi", "Tekâsür Sûresi",
      "Asr Sûresi", "Hümeze Sûresi", "Fil Sûresi", "Kureyş Sûresi", "Mâûn Sûresi",
      "Kevser Sûresi", "Kâfirûn Sûresi", "Nasr Sûresi", "Tebbet Sûresi", "İhlâs Sûresi",
      "Felâk Sûresi", "Nâs Sûresi", "Legat Sadegallah", "İnnelil Müttegine", "Amenarrasulü",
      "Layestevi", "Fatiha", "Elif-Lam-Mim"
    )
    for (t in kuranList) {
      list.add(CustomCurriculumItem("kuran_${order}", t, "Kur'an", true, order++))
    }

    // 2. Tesbihat (34)
    val tesbihatList = listOf(
      "Subhaneke", "Ettehiyyatü", "Salli", "Barik", "Rabbena Atina", "Rabbic Alni",
      "Allahümme La Tuhricna", "Kunut 1", "Kunut 2", "Ezan ve Kamet", "Ezan ve Kamet Duası",
      "Salaten Tüncina", "Salaten Nariye", "Ayetel Kürsi", "Tesbihat (Subhanallah-Elham.)",
      "Namaz Duası", "İstiaze Duası", "Yemek Duası (Arapça-Osmanlıca)", "Tesbihat Ortak Kısım",
      "Dua-yı Tercuman-ı İsm-i A'zam", "Dua-yı İsm-i A'zam", "Sabah Namazı Sünnet-Farz Arası Dua",
      "Akşam ile Yatsı Arası Dua", "Akşam ile Yatsı Arası Tesbihler", "Vitir Namazından sonra Dua",
      "Abdest Duaları", "Namaz için Arapça Niyetler", "İstigfar Duası", "Sefer Duası",
      "Eve girilirken okunacak dua", "Sabah ve akşam okunacak Dua", "Cenaze Duası",
      "Kabristanda okunacak Dua", "Şifa Ayetleri"
    )
    for (t in tesbihatList) {
      list.add(CustomCurriculumItem("tesbihat_${order}", t, "Tesbihat", true, order++))
    }

    // 3. Risale (46)
    val risaleList = listOf(
      "Birinci Söz", "Yazı Mektubu", "Onuncu Huccet-i Îmâniye", "Mukaddime", "Birinci Kelime",
      "İkinci Kelime", "Üçüncü Kelime", "Dördüncü Kelime", "Beşinci Kelime", "Altıncı Kelime",
      "Yedinci Kelime", "Sekizinci Kelime", "Dokuzuncu Kelime", "Onuncu Kelime", "On Birinci Kelime",
      "Altıncı Huccet-i Îmâniye", "Mu'cizât-ı Ahmediye (asm)", "Birinci Reşha", "İkinci Reşha",
      "Üçüncü Reşha", "Dördüncü Reşha", "Beşinci Reşha", "Altıncı Reşha", "Yedinci Reşha",
      "Sekizinci Reşha", "Dokuzuncu Reşha", "Onuncu Reşha", "On Birinci Reşha", "On İkinci Reşha",
      "On Üçüncü Reşha", "On Dördüncü Reşha", "İhlâs Risâlesi", "Birinci Düstûrunuz",
      "İkinci Düstûrunuz", "Üçüncü Düstûrunuz", "Dördüncü Düstûrunuz", "İhlâsı Kazanmanın Birinci Sebebi",
      "İkinci Sebeb", "İhlâsı Kıran Birinci Mâni'", "İkinci Mâni'", "Üçüncü Mâni'", "Tabiat Risâlesi",
      "Birinci Mes'ele", "İkinci Mes'ele", "Üçüncü Mes'ele", "Duâ"
    )
    for (t in risaleList) {
      list.add(CustomCurriculumItem("risale_${order}", t, "Risale", true, order++))
    }

    return list
  }

  private fun createNamazSureleriDualariItems(): List<CustomCurriculumItem> {
    val list = mutableListOf<CustomCurriculumItem>()
    var order = 0

    val sureler = listOf(
      "Fatiha Sûresi", "Fil Sûresi", "Kureyş Sûresi", "Mâûn Sûresi", "Kevser Sûresi",
      "Kâfirûn Sûresi", "Nasr Sûresi", "Tebbet Sûresi", "İhlâs Sûresi", "Felâk Sûresi",
      "Nâs Sûresi", "Ayetel Kürsi", "Amenerrasulü", "Hüvallahüllezi"
    )
    for (s in sureler) {
      list.add(CustomCurriculumItem("namaz_s_${order}", s, "Sureler", true, order++))
    }

    val dualar = listOf(
      "Sübhaneke", "Ettehiyyâtü", "Allahümme Salli", "Allahümme Barik",
      "Rabbena Atina", "Rabbic'alnî", "Kunut Duaları (1 & 2)", "Ezan & Kâmet"
    )
    for (d in dualar) {
      list.add(CustomCurriculumItem("namaz_d_${order}", d, "Dualar", true, order++))
    }

    return list
  }

  private fun createYazKursuElifbaItems(): List<CustomCurriculumItem> {
    val list = mutableListOf<CustomCurriculumItem>()
    var order = 0

    val elifba = listOf(
      "Harflerin Müstakil İsimleri", "Harflerin Başta-Ortada-Sonda Yazılışı",
      "Harekeler: Üstün", "Harekeler: Esre", "Harekeler: Ötre",
      "Cezm (Sükun)", "Şedde", "Tenvinler (İki Üstün, İki Esre, İki Ötre)",
      "Uzatma (Med) Harfleri (Elif, Vav, Ya)", "Çeker (Asar)",
      "Lamelif ve Zamir Kuralı", "Medd-i Muttasıl & Munfasıl",
      "İhfa, İzhar, İdgam Kaideleri"
    )
    for (e in elifba) {
      list.add(CustomCurriculumItem("elifba_${order}", e, "Elif-Bâ & Tecvid", true, order++))
    }

    val kisaSureler = listOf(
      "Fatiha Sûresi", "Fil Sûresi", "Kureyş Sûresi", "Mâûn Sûresi", "Kevser Sûresi",
      "Kâfirûn Sûresi", "Nasr Sûresi", "Tebbet Sûresi", "İhlâs Sûresi", "Felâk Sûresi", "Nâs Sûresi"
    )
    for (s in kisaSureler) {
      list.add(CustomCurriculumItem("elifba_s_${order}", s, "Kısa Sureler", true, order++))
    }

    return list
  }

  private fun createHadisVeIlmihalItems(): List<CustomCurriculumItem> {
    val list = mutableListOf<CustomCurriculumItem>()
    var order = 0

    val hadisler = listOf(
      "1. Hadis: Ameller niyetlere göredir",
      "2. Hadis: Din samimiyettir",
      "3. Hadis: Müslüman elinden ve dilinden emin olunan kimsedir",
      "4. Hadis: Temizlik imanın yarısıdır",
      "5. Hadis: Komşusu açken tok yatan bizden değildir",
      "6. Hadis: İlim öğrenmek her Müslümana farzdır",
      "7. Hadis: Tebessüm etmek sadakadır",
      "8. Hadis: Kolaylaştırınız güçleştirmeyiniz",
      "9. Hadis: Cennet annelerin ayakları altındadır",
      "10. Hadis: Hayra vesile olan hayrı yapan gibidir"
    )
    for (h in hadisler) {
      list.add(CustomCurriculumItem("hadis_${order}", h, "40 Hadis", true, order++))
    }

    val ilmihal = listOf(
      "İmanın 6 Şartı", "İslam'ın 5 Şartı", "Abdestin 4 Farzı", "Guslün 3 Farzı",
      "Teyemmümün 2 Farzı", "Namazın 12 Farzı (İçindeki & Dışındaki)", "32 Farz Özeti", "54 Farz"
    )
    for (i in ilmihal) {
      list.add(CustomCurriculumItem("ilmihal_${order}", i, "İlmihal", true, order++))
    }

    return list
  }

  private fun createCuzlerVeAsrItems(): List<CustomCurriculumItem> {
    val list = mutableListOf<CustomCurriculumItem>()
    var order = 0

    val asrlar = listOf(
      "Yâsîn-i Şerif (1. Sayfa)", "Yâsîn-i Şerif (Tamamı)", "Mülk (Tebâreke) Sûresi",
      "Nebe (Amme) Sûresi", "Fetih Sûresi (İlk Sayfa)", "Rahmân Sûresi", "Vâkıa Sûresi",
      "Cuma Sûresi", "Kıyâme Sûresi", "İnsan Sûresi"
    )
    for (a in asrlar) {
      list.add(CustomCurriculumItem("asr_${order}", a, "Aşr-ı Şerifler", true, order++))
    }

    val cuzler = listOf(
      "30. Cüz (Amme Cüzü)", "29. Cüz (Tebâreke Cüzü)", "1. Cüz (Elif Lam Mim)", "2. Cüz (Seyekûlu)"
    )
    for (c in cuzler) {
      list.add(CustomCurriculumItem("cuz_${order}", c, "Cüz Takibi", true, order++))
    }

    return list
  }
}
