package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.data.model.CurriculumData
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import com.example.data.model.AttendanceRecord

data class StudentBadge(
  val id: String,
  val title: String,
  val description: String,
  val emoji: String,
  val category: String,
  val isEarned: Boolean,
  val earnedDate: String? = null,
  val progressPercent: Int = 0,
  val colorHex: Long = 0xFF2563EB
)

object BadgeHelper {

  fun calculateBadges(
    student: Student,
    memorizations: List<MemorizationRecord>,
    attendanceList: List<AttendanceRecord>
  ): List<StudentBadge> {
    val studentMems = memorizations.filter { it.studentId == student.id }
    val studentAtt = attendanceList.filter { it.studentId == student.id }

    val completedMems = studentMems.filter { it.status == "TAMAMLANDI" || it.progressPercent >= 100 }
    val completedTitles = completedMems.map { it.title.lowercase().trim() }

    // 1. Amme Cüzü Fatihi
    // Check key 30th juz surahs: Nebe, Naziat, Abese, Tekvir, Mutaffifin, Buruc, Tarık, Ala, Fecr, Beled, Şems, Leyl, Duha, İnşirah, Tin, Alak, Kadir, Beyyine, Zilzal, Adiyat, Karia, Tekasür, Asr, Hümeze, Fil, Kureyş, Maun, Kevser, Kafirun, Nasr, Tebbet, İhlas, Felak, Nas
    val ammeSurahs = listOf("nebe", "naziat", "fecr", "duha", "inşirah", "tin", "alak", "kadir", "fil", "kureyş", "maun", "kevser", "kafirun", "nasr", "tebbet", "ihlas", "felak", "nas")
    val ammeCompletedCount = ammeSurahs.count { surah -> completedTitles.any { it.contains(surah) } }
    val ammeEarned = ammeCompletedCount >= 6 || completedMems.count { it.category.contains("kuran", ignoreCase = true) || it.category.contains("sure", ignoreCase = true) } >= 10
    val ammeProgress = ((ammeCompletedCount.toFloat() / ammeSurahs.size.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)

    // 2. Namaz Tesbihatı Ustası
    val tesbihatCompletedCount = completedMems.count { 
      it.category.contains("tesbihat", ignoreCase = true) || 
      it.title.contains("tesbihat", ignoreCase = true) ||
      it.title.contains("ayetel kursi", ignoreCase = true) ||
      it.title.contains("dua", ignoreCase = true)
    }
    val tesbihatEarned = tesbihatCompletedCount >= 4
    val tesbihatProgress = ((tesbihatCompletedCount.toFloat() / 5f) * 100).toInt().coerceIn(0, 100)

    // 3. Yasin & Mülk Muhafızı
    val hasYasin = completedTitles.any { it.contains("yasin") || it.contains("yasîn") }
    val hasMulk = completedTitles.any { it.contains("mülk") || it.contains("tebareke") }
    val yasinMulkEarned = hasYasin && hasMulk
    val yasinMulkProgress = when {
      hasYasin && hasMulk -> 100
      hasYasin || hasMulk -> 50
      else -> 0
    }

    // 4. Risale-i Nur Hatibi
    val risaleCompletedCount = completedMems.count { 
      it.category.contains("risale", ignoreCase = true) || 
      it.title.contains("vecize", ignoreCase = true) ||
      it.title.contains("söz", ignoreCase = true) ||
      it.title.contains("mektubat", ignoreCase = true) ||
      it.title.contains("lem'a", ignoreCase = true)
    }
    val risaleEarned = risaleCompletedCount >= 5
    val risaleProgress = ((risaleCompletedCount.toFloat() / 5f) * 100).toInt().coerceIn(0, 100)

    // 5. 15 Gün Kesintisiz Devamlılık
    val presentDays = studentAtt.count { it.status == "GELDI" }
    val attendanceRate = if (studentAtt.isNotEmpty()) (presentDays * 100) / studentAtt.size else 100
    val continuityEarned = presentDays >= 10 && attendanceRate >= 85
    val continuityProgress = ((presentDays.toFloat() / 15f) * 100).toInt().coerceIn(0, 100)

    // 6. Hatasız Ezber Yıldızı (5 Yıldızlı Notlar)
    val perfectScoresCount = completedMems.count { it.rating >= 5 }
    val starEarned = perfectScoresCount >= 5
    val starProgress = ((perfectScoresCount.toFloat() / 5f) * 100).toInt().coerceIn(0, 100)

    // 7. İlk Ezber Zaferi
    val firstStepEarned = completedMems.isNotEmpty()
    val firstStepProgress = if (firstStepEarned) 100 else 0

    return listOf(
      StudentBadge(
        id = "first_step",
        title = "İlk Ezber Zaferi",
        description = "İlk Kur'an veya tesbihat ezberini başarıyla verdi.",
        emoji = "🌱",
        category = "Başlangıç",
        isEarned = firstStepEarned,
        progressPercent = firstStepProgress,
        colorHex = 0xFF0D9488
      ),
      StudentBadge(
        id = "amme_fatih",
        title = "Amme Cüzü Fatihi",
        description = "Amme Cüzü surelerini ve temel ezberleri tamamladı.",
        emoji = "🏆",
        category = "Kur'an",
        isEarned = ammeEarned,
        progressPercent = ammeProgress,
        colorHex = 0xFFD97706
      ),
      StudentBadge(
        id = "yasin_mulk",
        title = "Yasin & Mülk Muhafızı",
        description = "Yasin ve Mülk (Tebareke) surelerini ezberledi.",
        emoji = "📖",
        category = "Kur'an",
        isEarned = yasinMulkEarned,
        progressPercent = yasinMulkProgress,
        colorHex = 0xFF16A34A
      ),
      StudentBadge(
        id = "tesbihat_usta",
        title = "Namaz Tesbihatı Ustası",
        description = "Namaz tesbihatlarını ve dualarını eksiksiz öğrendi.",
        emoji = "📿",
        category = "Tesbihat",
        isEarned = tesbihatEarned,
        progressPercent = tesbihatProgress,
        colorHex = 0xFF7C3AED
      ),
      StudentBadge(
        id = "risale_hatip",
        title = "Risale-i Nur Hatibi",
        description = "İman hakikatleri ve vecizelerden 5+ bölüm ezberledi.",
        emoji = "💎",
        category = "Risale",
        isEarned = risaleEarned,
        progressPercent = risaleProgress,
        colorHex = 0xFF0284C7
      ),
      StudentBadge(
        id = "continuity",
        title = "Devamlılık Kahramanı",
        description = "Derslere aksatmadan yüksek katılım gösterdi.",
        emoji = "🔥",
        category = "Ahlak & Devam",
        isEarned = continuityEarned,
        progressPercent = continuityProgress,
        colorHex = 0xFFEA580C
      ),
      StudentBadge(
        id = "star_reciter",
        title = "Hatasız Ezber Yıldızı",
        description = "Hocadan tam puan (5 yıldız) alarak ezber verdi.",
        emoji = "⭐",
        category = "Başarı",
        isEarned = starEarned,
        progressPercent = starProgress,
        colorHex = 0xFFCA8A04
      )
    )
  }
}
