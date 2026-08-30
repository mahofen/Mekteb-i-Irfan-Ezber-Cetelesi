package com.example.data.model

import android.content.Context
import com.example.data.util.CurriculumManager
import com.example.data.util.CustomCurriculumItem
import java.util.Locale

data class CurriculumItem(
  val id: Int,
  val title: String,
  val category: String, // "Kur'an", "Tesbihat", "Risale", etc.
  val orderIndex: Int
)

object CurriculumData {

  private var manager: CurriculumManager? = null

  fun init(context: Context) {
    manager = CurriculumManager.getInstance(context)
  }

  // Base fallback items
  private val DEFAULT_KURAN = listOf(
    "Duhâ Sûresi", "İnşirâh Sûresi", "Tin Sûresi", "Alak Sûresi", "Kadir Sûresi",
    "Beyyine Sûresi", "Zilzâl Sûresi", "Âdiyât Sûresi", "Kâria Sûresi", "Tekâsür Sûresi",
    "Asr Sûresi", "Hümeze Sûresi", "Fil Sûresi", "Kureyş Sûresi", "Mâûn Sûresi",
    "Kevser Sûresi", "Kâfirûn Sûresi", "Nasr Sûresi", "Tebbet Sûresi", "İhlâs Sûresi",
    "Felâk Sûresi", "Nâs Sûresi", "Legat Sadegallah", "İnnelil Müttegine", "Amenarrasulü",
    "Layestevi", "Fatiha", "Elif-Lam-Mim"
  )

  private val DEFAULT_TESBIHAT = listOf(
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

  private val DEFAULT_RISALE = listOf(
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

  val KURAN_ITEMS: List<String>
    get() {
      val mgr = manager ?: return DEFAULT_KURAN
      val items = mgr.getSelectedTitlesForCategory("Kur'an")
      return if (items.isNotEmpty()) items else DEFAULT_KURAN
    }

  val TESBIHAT_ITEMS: List<String>
    get() {
      val mgr = manager ?: return DEFAULT_TESBIHAT
      val items = mgr.getSelectedTitlesForCategory("Tesbihat")
      return if (items.isNotEmpty()) items else DEFAULT_TESBIHAT
    }

  val RISALE_ITEMS: List<String>
    get() {
      val mgr = manager ?: return DEFAULT_RISALE
      val items = mgr.getSelectedTitlesForCategory("Risale")
      return if (items.isNotEmpty()) items else DEFAULT_RISALE
    }

  val ALL_CURRICULUM_COUNT: Int
    get() {
      val mgr = manager ?: return DEFAULT_KURAN.size + DEFAULT_TESBIHAT.size + DEFAULT_RISALE.size
      val count = mgr.getActiveItems().size
      return if (count > 0) count else 108
    }

  fun getActiveCategories(): List<String> {
    return manager?.getActiveCategories() ?: listOf("Kur'an", "Tesbihat", "Risale")
  }

  fun getAllCategories(): List<String> {
    return manager?.getAllCategories() ?: listOf("Kur'an", "Tesbihat", "Risale")
  }

  fun getItemsForCategory(category: String): List<String> {
    val mgr = manager
    if (mgr != null) {
      val items = mgr.getSelectedTitlesForCategory(category)
      if (items.isNotEmpty()) return items
    }
    return when (category.lowercase(Locale.ROOT)) {
      "kuran", "kur'an" -> DEFAULT_KURAN
      "tesbihat" -> DEFAULT_TESBIHAT
      "risale" -> DEFAULT_RISALE
      else -> manager?.getSelectedTitlesForCategory(category) ?: DEFAULT_KURAN
    }
  }
}
