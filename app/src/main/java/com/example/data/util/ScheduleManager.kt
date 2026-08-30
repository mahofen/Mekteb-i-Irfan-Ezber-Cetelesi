package com.example.data.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DayOfWeekTr
import com.example.data.model.LessonCategory
import com.example.data.model.ScheduleLesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ScheduleManager private constructor(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("lesson_schedule_prefs", Context.MODE_PRIVATE)

  private val _lessonsFlow = MutableStateFlow<List<ScheduleLesson>>(emptyList())
  val lessonsFlow: StateFlow<List<ScheduleLesson>> = _lessonsFlow.asStateFlow()

  init {
    loadFromStorage()
  }

  companion object {
    @Volatile
    private var INSTANCE: ScheduleManager? = null

    fun getInstance(context: Context): ScheduleManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: ScheduleManager(context.applicationContext).also { INSTANCE = it }
      }
    }
  }

  private fun loadFromStorage() {
    val jsonString = prefs.getString("saved_schedule_lessons", null)
    if (jsonString.isNullOrBlank()) {
      val defaultLessons = generateDefaultSchedule()
      _lessonsFlow.value = defaultLessons
      saveToStorage(defaultLessons)
    } else {
      try {
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<ScheduleLesson>()
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          list.add(
            ScheduleLesson(
              id = obj.optString("id", UUID.randomUUID().toString()),
              day = DayOfWeekTr.valueOf(obj.optString("day", DayOfWeekTr.MONDAY.name)),
              startTime = obj.optString("startTime", "08:30"),
              endTime = obj.optString("endTime", "10:00"),
              title = obj.optString("title", "Ders"),
              category = LessonCategory.fromString(obj.optString("category", LessonCategory.GENERAL.name)),
              targetGrade = obj.optString("targetGrade", "Tüm Sınıflar"),
              classroom = obj.optString("classroom", "Derslik 1"),
              teacherName = obj.optString("teacherName", "Yetkili Hoca"),
              description = obj.optString("description", ""),
              isMandatoryAttendance = obj.optBoolean("isMandatoryAttendance", true),
              orderIndex = obj.optInt("orderIndex", i)
            )
          )
        }
        if (list.isEmpty()) {
          val defaultLessons = generateDefaultSchedule()
          _lessonsFlow.value = defaultLessons
          saveToStorage(defaultLessons)
        } else {
          _lessonsFlow.value = list.sortedWith(compareBy({ it.day.code }, { it.startTime }))
        }
      } catch (e: Exception) {
        val defaultLessons = generateDefaultSchedule()
        _lessonsFlow.value = defaultLessons
        saveToStorage(defaultLessons)
      }
    }
  }

  fun saveToStorage(list: List<ScheduleLesson>) {
    try {
      val jsonArray = JSONArray()
      list.forEach { lesson ->
        val obj = JSONObject().apply {
          put("id", lesson.id)
          put("day", lesson.day.name)
          put("startTime", lesson.startTime)
          put("endTime", lesson.endTime)
          put("title", lesson.title)
          put("category", lesson.category.name)
          put("targetGrade", lesson.targetGrade)
          put("classroom", lesson.classroom)
          put("teacherName", lesson.teacherName)
          put("description", lesson.description)
          put("isMandatoryAttendance", lesson.isMandatoryAttendance)
          put("orderIndex", lesson.orderIndex)
        }
        jsonArray.put(obj)
      }
      prefs.edit().putString("saved_schedule_lessons", jsonArray.toString()).apply()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun saveLesson(lesson: ScheduleLesson) {
    val current = _lessonsFlow.value.toMutableList()
    val existingIndex = current.indexOfFirst { it.id == lesson.id }
    if (existingIndex >= 0) {
      current[existingIndex] = lesson
    } else {
      current.add(lesson)
    }
    val sorted = current.sortedWith(compareBy({ it.day.code }, { it.startTime }))
    _lessonsFlow.value = sorted
    saveToStorage(sorted)
  }

  fun deleteLesson(lessonId: String) {
    val current = _lessonsFlow.value.filter { it.id != lessonId }
    _lessonsFlow.value = current
    saveToStorage(current)
  }

  fun resetToDefault() {
    val defaults = generateDefaultSchedule()
    _lessonsFlow.value = defaults
    saveToStorage(defaults)
  }

  fun getLessonsForDay(day: DayOfWeekTr): List<ScheduleLesson> {
    return _lessonsFlow.value.filter { it.day == day }.sortedBy { it.startTime }
  }

  private fun generateDefaultSchedule(): List<ScheduleLesson> {
    val lessons = mutableListOf<ScheduleLesson>()

    // Hafta İçi ve Hafta Sonu Zengin Mekteb-i İrfan Programı
    listOf(
      DayOfWeekTr.MONDAY,
      DayOfWeekTr.TUESDAY,
      DayOfWeekTr.WEDNESDAY,
      DayOfWeekTr.THURSDAY,
      DayOfWeekTr.FRIDAY
    ).forEach { day ->
      // 1. Fecr & Sabah Mütalaası
      lessons.add(
        ScheduleLesson(
          id = UUID.randomUUID().toString(),
          day = day,
          startTime = "06:00",
          endTime = "07:30",
          title = "Fecr Dersi & Sabah Mütalaası",
          category = LessonCategory.NAMAZ_TESBIHAT,
          targetGrade = "Hafızlık Grubu",
          classroom = "Ana Mescid",
          teacherName = "Hafız Hoca",
          description = "Sabah Namazı cemaatle eda, sabah tesbihatı & 1. Tur ezber dinleme.",
          isMandatoryAttendance = true
        )
      )

      // 2. Has Hafızlık & Cüz Dinleme
      lessons.add(
        ScheduleLesson(
          id = UUID.randomUUID().toString(),
          day = day,
          startTime = "08:30",
          endTime = "10:00",
          title = "Has Hafızlık & Cüz Dinleme",
          category = LessonCategory.MEMORIZATION,
          targetGrade = "6. Sınıf",
          classroom = "Derslik 1",
          teacherName = "Yönetici Hoca",
          description = "Yeni sayfa teslimi, ham hamle dinlemesi ve haslama kontrolü.",
          isMandatoryAttendance = true
        )
      )

      // 3. Tecvid & Mahreç Dersi
      lessons.add(
        ScheduleLesson(
          id = UUID.randomUUID().toString(),
          day = day,
          startTime = "10:15",
          endTime = "11:45",
          title = "Tecvid, Mahreç & Kıraat Talimi",
          category = LessonCategory.TECVİD,
          targetGrade = "5. Sınıf",
          classroom = "Derslik 2",
          teacherName = "Ahmet Hoca",
          description = "Tashih-i huruf, cezeriye ve Karabaş tecvidi tatbikatı.",
          isMandatoryAttendance = true
        )
      )

      // 4. Öğle Namazı & Aşır Tilaveti
      lessons.add(
        ScheduleLesson(
          id = UUID.randomUUID().toString(),
          day = day,
          startTime = "12:15",
          endTime = "13:30",
          title = "Öğle Namazı & Aşır Tilaveti",
          category = LessonCategory.QURAN_READING,
          targetGrade = "Tüm Sınıflar",
          classroom = "Ana Mescid",
          teacherName = "Nöbetçi Hoca",
          description = "Cemaatle namaz, tesbihat ve aşr-ı şerif tilaveti.",
          isMandatoryAttendance = true
        )
      )

      // 5. İkindi Sonrası Sure & Cüz Tekrarı
      lessons.add(
        ScheduleLesson(
          id = UUID.randomUUID().toString(),
          day = day,
          startTime = "14:00",
          endTime = "15:30",
          title = "Sure Ezberi & Pişirme Dersi",
          category = LessonCategory.MEMORIZATION,
          targetGrade = "7. Sınıf",
          classroom = "Derslik 1",
          teacherName = "Hafız Hoca",
          description = "Önceki cüzlerin ve surelerin pişirilmesi, kuvvetlendirme dinlemesi.",
          isMandatoryAttendance = true
        )
      )

      // 6. Risale-i Nur & Temel Dini Bilgiler
      lessons.add(
        ScheduleLesson(
          id = UUID.randomUUID().toString(),
          day = day,
          startTime = "16:00",
          endTime = "17:15",
          title = if (day == DayOfWeekTr.FRIDAY) "Cuma Sohbeti & Hadis Dersi" else "Risale-i Nur & Ahlak Dersi",
          category = if (day == DayOfWeekTr.FRIDAY) LessonCategory.HADITH_RISALE else LessonCategory.ISLAMIC_STUDIES,
          targetGrade = "Tüm Sınıflar",
          classroom = "Konferans Salonu",
          teacherName = "Yönetici Hoca",
          description = "İtikat, ibadet, ahlak ve manevi gelişim dersi.",
          isMandatoryAttendance = true
        )
      )

      // 7. Akşam / Yatsı Etüdü & Günlük Vazife
      lessons.add(
        ScheduleLesson(
          id = UUID.randomUUID().toString(),
          day = day,
          startTime = "18:45",
          endTime = "20:30",
          title = "Gece Mütalaası & Vazife Kontrolü",
          category = LessonCategory.MUTALAA_ETUT,
          targetGrade = "Tüm Sınıflar",
          classroom = "Etüt Salonu",
          teacherName = "Nöbetçi Eğitmen",
          description = "Günlük namaz, vird, tesbihat ve sayfa okuma çetelelerinin teslimi.",
          isMandatoryAttendance = true
        )
      )
    }

    // Cumartesi Programı (Haftalık Değerlendirme & Özel Ezber)
    lessons.add(
      ScheduleLesson(
        id = UUID.randomUUID().toString(),
        day = DayOfWeekTr.SATURDAY,
        startTime = "09:00",
        endTime = "11:00",
        title = "Haftalık Cüz & Hatim Dinleme",
        category = LessonCategory.MEMORIZATION,
        targetGrade = "Hafızlık Grubu",
        classroom = "Ana Mescid",
        teacherName = "Yönetici Hoca",
        description = "Haftalık toplu cüz dinleme ve seviye tespit sınavı.",
        isMandatoryAttendance = true
      )
    )
    lessons.add(
      ScheduleLesson(
        id = UUID.randomUUID().toString(),
        day = DayOfWeekTr.SATURDAY,
        startTime = "11:15",
        endTime = "12:45",
        title = "Tashih-i Huruf & Makam Eğitimi",
        category = LessonCategory.TECVİD,
        targetGrade = "Tüm Sınıflar",
        classroom = "Derslik 1",
        teacherName = "Ahmet Hoca",
        description = "Ezan, kamet, aşır makamları ve kıraat incelikleri.",
        isMandatoryAttendance = false
      )
    )
    lessons.add(
      ScheduleLesson(
        id = UUID.randomUUID().toString(),
        day = DayOfWeekTr.SATURDAY,
        startTime = "14:00",
        endTime = "16:00",
        title = "Kardeşlik & Manevi Sohbet",
        category = LessonCategory.ISLAMIC_STUDIES,
        targetGrade = "Tüm Sınıflar",
        classroom = "Konferans Salonu",
        teacherName = "Yönetici Hoca",
        description = "Haftalık ahlak dersi ve hasbihal.",
        isMandatoryAttendance = false
      )
    )

    // Pazar Programı (Serbest Çalışma & Bireysel Takviye)
    lessons.add(
      ScheduleLesson(
        id = UUID.randomUUID().toString(),
        day = DayOfWeekTr.SUNDAY,
        startTime = "10:00",
        endTime = "12:00",
        title = "Bireysel Ezber Takviye & Telafi",
        category = LessonCategory.MEMORIZATION,
        targetGrade = "Tüm Sınıflar",
        classroom = "Derslik 2",
        teacherName = "Nöbetçi Eğitmen",
        description = "Eksik dersleri tamamlama ve hafta başı hazırlığı.",
        isMandatoryAttendance = false
      )
    )
    lessons.add(
      ScheduleLesson(
        id = UUID.randomUUID().toString(),
        day = DayOfWeekTr.SUNDAY,
        startTime = "14:00",
        endTime = "16:30",
        title = "Serbest Mütalaa & Kitap Okuma Saati",
        category = LessonCategory.MUTALAA_ETUT,
        targetGrade = "Tüm Sınıflar",
        classroom = "Kütüphane",
        teacherName = "Nöbetçi Eğitmen",
        description = "Serbest okuma, Risale ve ilmihal mütalaası.",
        isMandatoryAttendance = false
      )
    )

    return lessons
  }
}
