package com.example.data.model

import java.util.UUID

enum class DayOfWeekTr(val code: Int, val shortName: String, val fullName: String) {
  MONDAY(1, "Pzt", "Pazartesi"),
  TUESDAY(2, "Sal", "Salı"),
  WEDNESDAY(3, "Çar", "Çarşamba"),
  THURSDAY(4, "Per", "Perşembe"),
  FRIDAY(5, "Cum", "Cuma"),
  SATURDAY(6, "Cmt", "Cumartesi"),
  SUNDAY(7, "Paz", "Pazar");

  companion object {
    fun fromDayCode(code: Int): DayOfWeekTr = values().find { it.code == code } ?: MONDAY

    fun fromCalendarDay(calendarDay: Int): DayOfWeekTr = when (calendarDay) {
      java.util.Calendar.MONDAY -> MONDAY
      java.util.Calendar.TUESDAY -> TUESDAY
      java.util.Calendar.WEDNESDAY -> WEDNESDAY
      java.util.Calendar.THURSDAY -> THURSDAY
      java.util.Calendar.FRIDAY -> FRIDAY
      java.util.Calendar.SATURDAY -> SATURDAY
      java.util.Calendar.SUNDAY -> SUNDAY
      else -> MONDAY
    }
  }
}

enum class LessonCategory(val title: String, val badgeColorHex: Long) {
  MEMORIZATION("Ezber & Hamle", 0xFF8B5CF6),       // Mor
  TECVİD("Tecvid & Mahreç", 0xFF0D9488),          // Firuze / Teal
  QURAN_READING("Kur'an & Aşır", 0xFF166534),      // Zümrüt Yeşili
  ISLAMIC_STUDIES("İlmihal & Ahlak", 0xFFB45309),  // Altın / Amber
  HADITH_RISALE("Hadis & Risale", 0xFF0F2C59),     // Derin Lapis
  NAMAZ_TESBIHAT("Namaz & Tesbihat", 0xFF059669),  // Zümrüt
  MUTALAA_ETUT("Mütalaa & Etüt", 0xFFD97706),     // Turuncu
  GENERAL("Genel Ders", 0xFF475569);              // Slate

  companion object {
    fun fromString(str: String): LessonCategory {
      return values().find { it.name.equals(str, ignoreCase = true) || it.title.equals(str, ignoreCase = true) }
        ?: GENERAL
    }
  }
}

data class ScheduleLesson(
  val id: String = UUID.randomUUID().toString(),
  val day: DayOfWeekTr = DayOfWeekTr.MONDAY,
  val startTime: String = "08:30", // HH:mm
  val endTime: String = "10:00",   // HH:mm
  val title: String = "Has Hafızlık & Cüz Dinleme",
  val category: LessonCategory = LessonCategory.MEMORIZATION,
  val targetGrade: String = "Tüm Sınıflar", // "5. Sınıf", "6. Sınıf", "Hafızlık Grubu", "Tüm Sınıflar"
  val classroom: String = "Derslik 1 (Mescid)",
  val teacherName: String = "Yetkili Hoca",
  val description: String = "Yeni sayfa teslimi ve hamle kontrolü.",
  val isMandatoryAttendance: Boolean = true,
  val orderIndex: Int = 0
)
