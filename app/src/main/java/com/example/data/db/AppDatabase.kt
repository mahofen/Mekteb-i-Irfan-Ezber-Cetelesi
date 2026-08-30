package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyDutyRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
  entities = [Student::class, AttendanceRecord::class, MemorizationRecord::class, DailyDutyRecord::class],
  version = 3,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun studentDao(): StudentDao
  abstract fun attendanceDao(): AttendanceDao
  abstract fun memorizationDao(): MemorizationDao
  abstract fun dailyDutyDao(): DailyDutyDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "mektebi_irfan_db"
        )
          .addCallback(AppDatabaseCallback(scope))
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class AppDatabaseCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(
              database.studentDao(),
              database.attendanceDao(),
              database.memorizationDao(),
              database.dailyDutyDao()
            )
          }
        }
      }
    }

    suspend fun populateInitialData(
      studentDao: StudentDao,
      attendanceDao: AttendanceDao,
      memorizationDao: MemorizationDao,
      dailyDutyDao: DailyDutyDao? = null
    ) {
      val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      val today = dateFormat.format(Date())

      val s1 = studentDao.insertStudent(
        Student(
          fullName = "Ahmet Faruk Yılmaz",
          grade = "6. Sınıf",
          phone = "0532 111 22 33",
          parentName = "Mehmet Yılmaz (Babası)",
          parentPhone = "0533 222 33 44",
          enrollmentDate = "2024-09-10",
          status = "Aktif",
          notes = "Tecvid kurallarına çok hakim. Hızlı kavrıyor.",
          avatarColorIndex = 0,
          username = "ahmet",
          accessCode = "1234"
        )
      )

      val s2 = studentDao.insertStudent(
        Student(
          fullName = "Mustafa Talha Demir",
          grade = "7. Sınıf",
          phone = "0541 333 44 55",
          parentName = "İbrahim Demir (Babası)",
          parentPhone = "0542 444 55 66",
          enrollmentDate = "2024-09-12",
          status = "Aktif",
          notes = "Mülk Suresi ezberinde çok başarılı. Mahreçleri temiz.",
          avatarColorIndex = 1,
          username = "mustafa",
          accessCode = "1234"
        )
      )

      val s3 = studentDao.insertStudent(
        Student(
          fullName = "Yusuf Eren Kaya",
          grade = "5. Sınıf",
          phone = "0551 555 66 77",
          parentName = "Zeynep Kaya (Annesi)",
          parentPhone = "0552 666 77 88",
          enrollmentDate = "2024-10-01",
          status = "Aktif",
          notes = "Amme cüzünde son 10 sure kaldı.",
          avatarColorIndex = 2,
          username = "yusuf",
          accessCode = "1234"
        )
      )

      val s4 = studentDao.insertStudent(
        Student(
          fullName = "Ömer Faruk Çelik",
          grade = "9. Sınıf",
          phone = "0505 777 88 99",
          parentName = "Kemal Çelik (Babası)",
          parentPhone = "0506 888 99 00",
          enrollmentDate = "2024-08-15",
          status = "Aktif",
          notes = "Yasin Suresini tam teslim etti.",
          avatarColorIndex = 3,
          username = "omer",
          accessCode = "1234"
        )
      )

      val s5 = studentDao.insertStudent(
        Student(
          fullName = "Hamza Ali Şahin",
          grade = "8. Sınıf",
          phone = "0536 999 00 11",
          parentName = "Murat Şahin (Babası)",
          parentPhone = "0537 111 22 44",
          enrollmentDate = "2024-11-05",
          status = "Aktif",
          notes = "Kısa sureler bitti, Nebe Suresine başladı.",
          avatarColorIndex = 4,
          username = "hamza",
          accessCode = "1234"
        )
      )

      val s6 = studentDao.insertStudent(
        Student(
          fullName = "Tarık Buğra Aslan",
          grade = "12. Sınıf",
          phone = "0544 222 33 11",
          parentName = "Hasan Aslan (Babası)",
          parentPhone = "0545 333 44 22",
          enrollmentDate = "2023-09-01",
          status = "Mezun",
          notes = "108 derslik temel ezber müfredatını başarıyla tamamladı.",
          avatarColorIndex = 5,
          username = "tarik",
          accessCode = "1234"
        )
      )

      val cal = Calendar.getInstance()
      fun daysAgo(days: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -days)
        return dateFormat.format(c.time)
      }

      // Initial Attendance for Today and past dates
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s1, date = today, status = "GELDI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s2, date = today, status = "GELDI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s3, date = today, status = "GELDI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s4, date = today, status = "IZINLI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s5, date = today, status = "GEC"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s1, date = daysAgo(3), status = "GELDI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s2, date = daysAgo(3), status = "GELDI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s3, date = daysAgo(3), status = "GELDI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s1, date = daysAgo(7), status = "GELDI"))
      attendanceDao.insertOrUpdateAttendance(AttendanceRecord(studentId = s2, date = daysAgo(7), status = "GELDI"))

      // Initial Memorization Records for Kur'an, Tesbihat, and Risale with historical progress
      // Student 1 - Ahmet Faruk Yılmaz
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Fatiha",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Kusursuz tilavet ve mahreç.",
          date = daysAgo(28)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Elif-Lam-Mim",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Başarıyla teslim edildi.",
          date = daysAgo(24)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Amenarrasulü",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Tecvid kurallarına riayet edildi.",
          date = daysAgo(18)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Duhâ Sûresi",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Teslim edildi.",
          date = daysAgo(14)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "İnşirâh Sûresi",
          category = "Kur'an",
          status = "DEVAM_EDIYOR",
          rating = 5,
          progressPercent = 85,
          teacherNotes = "Son 2 ayet kaldı.",
          date = today
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Subhaneke",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Tam ezberlendi.",
          date = daysAgo(26)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Ettehiyyatü",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Tam ezberlendi.",
          date = daysAgo(22)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Salli",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Tam ezberlendi.",
          date = daysAgo(20)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Barik",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Tam ezberlendi.",
          date = daysAgo(16)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Salaten Tüncina",
          category = "Tesbihat",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 75,
          teacherNotes = "Gayet akıcı gidiyor.",
          date = today
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Birinci Söz",
          category = "Risale",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Bismillah bahsi ezberlendi.",
          date = daysAgo(25)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Yazı Mektubu",
          category = "Risale",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Metin ezberi tamam.",
          date = daysAgo(18)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "İhlâs Risâlesi",
          category = "Risale",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Giriş metni tamam.",
          date = daysAgo(12)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s1,
          title = "Birinci Düstûrunuz",
          category = "Risale",
          status = "DEVAM_EDIYOR",
          rating = 5,
          progressPercent = 80,
          teacherNotes = "Rıza-yı İlahî bahsi çalışılıyor.",
          date = today
        )
      )

      // Student 2 - Mustafa Talha Demir
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Fatiha",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          date = daysAgo(25)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "İhlâs Sûresi",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          date = daysAgo(20)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Felâk Sûresi",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          date = daysAgo(15)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Nâs Sûresi",
          category = "Kur'an",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 65,
          teacherNotes = "Ezber devam ediyor.",
          date = today
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Subhaneke",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Kusursuz ezber.",
          date = daysAgo(20)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Ayetel Kürsi",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Vird ezberi tamam.",
          date = daysAgo(14)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Tesbihat (Subhanallah-Elham.)",
          category = "Tesbihat",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 60,
          teacherNotes = "Çalışmaya devam ediyor.",
          date = today
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Birinci Söz",
          category = "Risale",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Ezber tamamlandı.",
          date = daysAgo(10)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s2,
          title = "Onuncu Huccet-i Îmâniye",
          category = "Risale",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 60,
          teacherNotes = "Giriş kısmı tamam.",
          date = today
        )
      )

      // Student 3 - Yusuf Eren Kaya
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s3,
          title = "Fatiha",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          date = daysAgo(21)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s3,
          title = "Kevser Sûresi",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 4,
          progressPercent = 100,
          date = daysAgo(12)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s3,
          title = "Kâfirûn Sûresi",
          category = "Kur'an",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 50,
          teacherNotes = "Tekrar yapılacak.",
          date = today
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s3,
          title = "Subhaneke",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Ezberi tam.",
          date = daysAgo(15)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s3,
          title = "Birinci Söz",
          category = "Risale",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 40,
          teacherNotes = "Metin ezberi devam ediyor.",
          date = today
        )
      )

      // Student 4 - Ömer Faruk Çelik
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s4,
          title = "Mülk Suresi",
          category = "Kur'an",
          status = "TEKRAR",
          rating = 3,
          progressPercent = 50,
          teacherNotes = "Orta kısımlar tekrar edilecek.",
          date = daysAgo(5)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s4,
          title = "Sabah Tesbihatı",
          category = "Tesbihat",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 60,
          teacherNotes = "Lafızlar iyi gidiyor.",
          date = today
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s4,
          title = "23. Söz'den Vecizeler",
          category = "Risale",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "İman ve ubudiyet bahisleri tamam.",
          date = daysAgo(8)
        )
      )

      // Student 5 - Hamza Ali Şahin
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s5,
          title = "Ayetel Kürsi & Amenerrasulü",
          category = "Kur'an",
          status = "TAMAMLANDI",
          rating = 5,
          progressPercent = 100,
          teacherNotes = "Çok güzel okudu.",
          date = daysAgo(12)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s5,
          title = "İkindi & Yatsı Tesbihatı",
          category = "Tesbihat",
          status = "TAMAMLANDI",
          rating = 4,
          progressPercent = 100,
          teacherNotes = "Kusursuz ezber.",
          date = daysAgo(7)
        )
      )
      memorizationDao.insertMemorization(
        MemorizationRecord(
          studentId = s5,
          title = "Münazarat Vecizeleri",
          category = "Risale",
          status = "DEVAM_EDIYOR",
          rating = 4,
          progressPercent = 75,
          teacherNotes = "Ezber devam ediyor.",
          date = today
        )
      )

      // Sample Daily Duty Records for students
      dailyDutyDao?.let { dao ->
        dao.insertOrUpdateDuty(
          DailyDutyRecord(
            studentId = s1,
            date = today,
            fajr = true,
            dhuhr = true,
            asr = true,
            maghrib = true,
            isha = false,
            quranPages = 4,
            tesbihatDone = true,
            risalePages = 5,
            memorizationRepeats = 3,
            cevsenDone = true,
            salavatCount = 100,
            notes = "İnşirah suresi 3 kez tekrar edildi."
          )
        )
        dao.insertOrUpdateDuty(
          DailyDutyRecord(
            studentId = s1,
            date = daysAgo(1),
            fajr = true,
            dhuhr = true,
            asr = true,
            maghrib = true,
            isha = true,
            quranPages = 5,
            tesbihatDone = true,
            risalePages = 10,
            memorizationRepeats = 4,
            cevsenDone = true,
            salavatCount = 150,
            notes = "Duha ve İnşirah tekrar edildi."
          )
        )
        dao.insertOrUpdateDuty(
          DailyDutyRecord(
            studentId = s1,
            date = daysAgo(2),
            fajr = true,
            dhuhr = true,
            asr = true,
            maghrib = true,
            isha = true,
            quranPages = 3,
            tesbihatDone = true,
            risalePages = 4,
            memorizationRepeats = 2,
            cevsenDone = false,
            salavatCount = 100
          )
        )
        dao.insertOrUpdateDuty(
          DailyDutyRecord(
            studentId = s2,
            date = today,
            fajr = true,
            dhuhr = true,
            asr = true,
            maghrib = false,
            isha = false,
            quranPages = 2,
            tesbihatDone = true,
            risalePages = 5,
            memorizationRepeats = 2,
            cevsenDone = true,
            salavatCount = 100
          )
        )
      }
    }
  }
}
