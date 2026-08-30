package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  @ColumnInfo(name = "fullName")
  val fullName: String = "",
  @ColumnInfo(name = "grade")
  val grade: String = "5. Sınıf", // 5. Sınıf - 12. Sınıf
  @ColumnInfo(name = "phone")
  val phone: String = "",
  @ColumnInfo(name = "parentName")
  val parentName: String = "",
  @ColumnInfo(name = "parentPhone")
  val parentPhone: String = "",
  @ColumnInfo(name = "enrollmentDate")
  val enrollmentDate: String = "",
  @ColumnInfo(name = "status")
  val status: String = "Aktif", // Aktif, Pasif, Mezun
  @ColumnInfo(name = "notes")
  val notes: String = "",
  @ColumnInfo(name = "avatarColorIndex")
  val avatarColorIndex: Int = 0,
  @ColumnInfo(name = "username")
  val username: String = "",
  @ColumnInfo(name = "accessCode")
  val accessCode: String = "1234"
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  @ColumnInfo(name = "studentId")
  val studentId: Long = 0,
  @ColumnInfo(name = "date")
  val date: String = "", // yyyy-MM-dd
  @ColumnInfo(name = "status")
  val status: String = "GELDI" // GELDI, GELMEDI, IZINLI, GEC
)

@Entity(tableName = "memorization_records")
data class MemorizationRecord(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  @ColumnInfo(name = "studentId")
  val studentId: Long = 0,
  @ColumnInfo(name = "title")
  val title: String = "",
  @ColumnInfo(name = "category")
  val category: String = "Sure", // Cüz, Sure, Aşır, Sayfa, Hadis
  @ColumnInfo(name = "status")
  val status: String = "DEVAM_EDIYOR", // VERILDI, DEVAM_EDIYOR, TAMAMLANDI, TEKRAR
  @ColumnInfo(name = "rating")
  val rating: Int = 5, // 1..5
  @ColumnInfo(name = "progressPercent")
  val progressPercent: Int = 50, // 0..100
  @ColumnInfo(name = "teacherNotes")
  val teacherNotes: String = "",
  @ColumnInfo(name = "date")
  val date: String = "",
  @ColumnInfo(name = "repeatCount")
  val repeatCount: Int = 0 // Tekrar sayısı
)

@Entity(tableName = "daily_duty_records")
data class DailyDutyRecord(
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  val id: Long = 0,
  @ColumnInfo(name = "studentId")
  val studentId: Long = 0,
  @ColumnInfo(name = "date")
  val date: String = "", // yyyy-MM-dd
  @ColumnInfo(name = "fajr")
  val fajr: Boolean = false, // Sabah Namazı
  @ColumnInfo(name = "dhuhr")
  val dhuhr: Boolean = false, // Öğle Namazı
  @ColumnInfo(name = "asr")
  val asr: Boolean = false, // İkindi Namazı
  @ColumnInfo(name = "maghrib")
  val maghrib: Boolean = false, // Akşam Namazı
  @ColumnInfo(name = "isha")
  val isha: Boolean = false, // Yatsı Namazı
  @ColumnInfo(name = "quranPages")
  val quranPages: Int = 0, // Okunan Kur'an sayfası
  @ColumnInfo(name = "tesbihatDone")
  val tesbihatDone: Boolean = false, // Namaz tesbihatı
  @ColumnInfo(name = "risalePages")
  val risalePages: Int = 0, // Okunan Risale sayfası
  @ColumnInfo(name = "memorizationRepeats")
  val memorizationRepeats: Int = 0, // Ezber tekrar sayısı
  @ColumnInfo(name = "cevsenDone")
  val cevsenDone: Boolean = false, // Cevşen / Evrad
  @ColumnInfo(name = "salavatCount")
  val salavatCount: Int = 0, // Salavat zikri
  @ColumnInfo(name = "notes")
  val notes: String = ""
)

