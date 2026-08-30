package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
  val exportDate: String,
  val students: List<Student>,
  val attendance: List<AttendanceRecord>,
  val memorization: List<MemorizationRecord>
)

data class ImportResult(
  val isSuccess: Boolean,
  val studentCount: Int = 0,
  val attendanceCount: Int = 0,
  val memorizationCount: Int = 0,
  val message: String = ""
)

enum class ImportMode {
  MERGE,      // Mevcut verilere ekle / güncelle
  OVERWRITE   // Tüm mevcut verileri sil ve sıfırdan yükle
}

object BackupManager {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
  private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())

  // =========================================================================
  // JSON BACKUP & EXPORT / IMPORT
  // =========================================================================

  /**
   * Serializes all application data to a formatted JSON string
   */
  fun generateBackupJson(
    students: List<Student>,
    attendance: List<AttendanceRecord>,
    memorization: List<MemorizationRecord>
  ): String {
    val root = JSONObject()
    val now = Date()

    root.put("appName", "Mekteb-i İrfan Ezber Çetelesi")
    root.put("version", "1.0")
    root.put("exportDate", dateFormat.format(now))
    root.put("timestamp", now.time)

    // Summary
    val summary = JSONObject().apply {
      put("studentCount", students.size)
      put("attendanceCount", attendance.size)
      put("memorizationCount", memorization.size)
    }
    root.put("summary", summary)

    // Students Array
    val studentsArray = JSONArray()
    students.forEach { s ->
      val sObj = JSONObject().apply {
        put("id", s.id)
        put("fullName", s.fullName)
        put("grade", s.grade)
        put("phone", s.phone)
        put("parentName", s.parentName)
        put("parentPhone", s.parentPhone)
        put("enrollmentDate", s.enrollmentDate)
        put("status", s.status)
        put("notes", s.notes)
        put("avatarColorIndex", s.avatarColorIndex)
      }
      studentsArray.put(sObj)
    }
    root.put("students", studentsArray)

    // Attendance Array
    val attendanceArray = JSONArray()
    attendance.forEach { a ->
      val aObj = JSONObject().apply {
        put("id", a.id)
        put("studentId", a.studentId)
        put("date", a.date)
        put("status", a.status)
      }
      attendanceArray.put(aObj)
    }
    root.put("attendance", attendanceArray)

    // Memorization Array
    val memorizationArray = JSONArray()
    memorization.forEach { m ->
      val mObj = JSONObject().apply {
        put("id", m.id)
        put("studentId", m.studentId)
        put("title", m.title)
        put("category", m.category)
        put("status", m.status)
        put("rating", m.rating)
        put("progressPercent", m.progressPercent)
        put("teacherNotes", m.teacherNotes)
        put("date", m.date)
      }
      memorizationArray.put(mObj)
    }
    root.put("memorization", memorizationArray)

    return root.toString(2)
  }

  /**
   * Parses JSON string back into entity objects with robust error recovery
   */
  fun parseBackupJson(jsonString: String): Result<BackupData> {
    return try {
      val root = JSONObject(jsonString)

      val exportDate = root.optString("exportDate", dateFormat.format(Date()))

      // Parse Students
      val students = mutableListOf<Student>()
      if (root.has("students")) {
        val sArr = root.getJSONArray("students")
        for (i in 0 until sArr.length()) {
          val sObj = sArr.getJSONObject(i)
          students.add(
            Student(
              id = sObj.optLong("id", 0L),
              fullName = sObj.optString("fullName", ""),
              grade = sObj.optString("grade", "5. Sınıf"),
              phone = sObj.optString("phone", ""),
              parentName = sObj.optString("parentName", ""),
              parentPhone = sObj.optString("parentPhone", ""),
              enrollmentDate = sObj.optString("enrollmentDate", ""),
              status = sObj.optString("status", "Aktif"),
              notes = sObj.optString("notes", ""),
              avatarColorIndex = sObj.optInt("avatarColorIndex", 0)
            )
          )
        }
      }

      // Parse Attendance
      val attendance = mutableListOf<AttendanceRecord>()
      if (root.has("attendance")) {
        val aArr = root.getJSONArray("attendance")
        for (i in 0 until aArr.length()) {
          val aObj = aArr.getJSONObject(i)
          attendance.add(
            AttendanceRecord(
              id = aObj.optLong("id", 0L),
              studentId = aObj.optLong("studentId", 0L),
              date = aObj.optString("date", ""),
              status = aObj.optString("status", "GELDI")
            )
          )
        }
      }

      // Parse Memorization
      val memorization = mutableListOf<MemorizationRecord>()
      if (root.has("memorization")) {
        val mArr = root.getJSONArray("memorization")
        for (i in 0 until mArr.length()) {
          val mObj = mArr.getJSONObject(i)
          memorization.add(
            MemorizationRecord(
              id = mObj.optLong("id", 0L),
              studentId = mObj.optLong("studentId", 0L),
              title = mObj.optString("title", ""),
              category = mObj.optString("category", "Kur'an"),
              status = mObj.optString("status", "DEVAM_EDIYOR"),
              rating = mObj.optInt("rating", 5),
              progressPercent = mObj.optInt("progressPercent", 0),
              teacherNotes = mObj.optString("teacherNotes", ""),
              date = mObj.optString("date", "")
            )
          )
        }
      }

      Result.success(
        BackupData(
          exportDate = exportDate,
          students = students,
          attendance = attendance,
          memorization = memorization
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // =========================================================================
  // CSV GENERATION (EXCEL COMPATIBLE)
  // =========================================================================

  private fun escapeCsv(value: String): String {
    val escaped = value.replace("\"", "\"\"")
    return if (escaped.contains(",") || escaped.contains(";") || escaped.contains("\n") || escaped.contains("\"")) {
      "\"$escaped\""
    } else {
      escaped
    }
  }

  /**
   * Generates CSV for Students (Öğrenci Listesi)
   */
  fun generateStudentsCsv(students: List<Student>): String {
    val sb = StringBuilder()
    // UTF-8 BOM for Excel Turkish character compatibility
    sb.append('\uFEFF')
    sb.append("ID,Ad Soyad,Sınıf,Telefon,Veli Adı,Veli Telefonu,Kayıt Tarihi,Durum,Notlar\n")
    students.forEach { s ->
      sb.append("${s.id},")
      sb.append("${escapeCsv(s.fullName)},")
      sb.append("${escapeCsv(s.grade)},")
      sb.append("${escapeCsv(s.phone)},")
      sb.append("${escapeCsv(s.parentName)},")
      sb.append("${escapeCsv(s.parentPhone)},")
      sb.append("${escapeCsv(s.enrollmentDate)},")
      sb.append("${escapeCsv(s.status)},")
      sb.append("${escapeCsv(s.notes)}\n")
    }
    return sb.toString()
  }

  /**
   * Generates CSV for Attendance Records (Yoklama Kayıtları)
   */
  fun generateAttendanceCsv(attendance: List<AttendanceRecord>, students: List<Student>): String {
    val studentMap = students.associateBy { it.id }
    val sb = StringBuilder()
    sb.append('\uFEFF')
    sb.append("ID,Talebe ID,Talebe Adı Soyadı,Tarih,Yoklama Durumu\n")
    attendance.forEach { a ->
      val sName = studentMap[a.studentId]?.fullName ?: "Bilinmeyen Talebe"
      val statusName = when (a.status) {
        "GELDI" -> "Geldi"
        "GELMEDI" -> "Gelmedi"
        "IZINLI" -> "İzinli"
        "GECIKTI" -> "Geç"
        else -> a.status
      }
      sb.append("${a.id},${a.studentId},${escapeCsv(sName)},${escapeCsv(a.date)},${escapeCsv(statusName)}\n")
    }
    return sb.toString()
  }

  /**
   * Generates CSV for Memorization Records (Ezber Takip Kayıtları)
   */
  fun generateMemorizationCsv(memorization: List<MemorizationRecord>, students: List<Student>): String {
    val studentMap = students.associateBy { it.id }
    val sb = StringBuilder()
    sb.append('\uFEFF')
    sb.append("ID,Talebe ID,Talebe Adı Soyadı,Kategori,Ezber Başlığı,Durum,İlerleme %,Puan,Hoca Notu,Tarih\n")
    memorization.forEach { m ->
      val sName = studentMap[m.studentId]?.fullName ?: "Bilinmeyen Talebe"
      val statusName = when (m.status) {
        "TAMAMLANDI" -> "Tamamlandı"
        "DEVAM_EDIYOR" -> "Devam Ediyor"
        "BASLANMADI" -> "Başlanmadı"
        else -> m.status
      }
      sb.append("${m.id},${m.studentId},${escapeCsv(sName)},${escapeCsv(m.category)},${escapeCsv(m.title)},${escapeCsv(statusName)},${m.progressPercent},${m.rating},${escapeCsv(m.teacherNotes)},${escapeCsv(m.date)}\n")
    }
    return sb.toString()
  }

  /**
   * Generates complete combined CSV with distinct sections
   */
  fun generateCombinedCsv(
    students: List<Student>,
    attendance: List<AttendanceRecord>,
    memorization: List<MemorizationRecord>
  ): String {
    val sb = StringBuilder()
    sb.append('\uFEFF')
    sb.append("### MEKTEB-İ İRFAN SİSTEM RAPORU VE TÜM VERİLER ###\n")
    sb.append("Oluşturulma Tarihi: ${dateFormat.format(Date())}\n\n")

    sb.append("--- TALEBE LİSTESİ (${students.size} Kişi) ---\n")
    sb.append("ID,Ad Soyad,Sınıf,Telefon,Veli Adı,Veli Telefonu,Kayıt Tarihi,Durum,Notlar\n")
    students.forEach { s ->
      sb.append("${s.id},${escapeCsv(s.fullName)},${escapeCsv(s.grade)},${escapeCsv(s.phone)},${escapeCsv(s.parentName)},${escapeCsv(s.parentPhone)},${escapeCsv(s.enrollmentDate)},${escapeCsv(s.status)},${escapeCsv(s.notes)}\n")
    }

    sb.append("\n--- YOKLAMA KAYITLARI (${attendance.size} Kayıt) ---\n")
    sb.append(generateAttendanceCsv(attendance, students).removePrefix("\uFEFF"))

    sb.append("\n--- EZBER TAKİP KAYITLARI (${memorization.size} Kayıt) ---\n")
    sb.append(generateMemorizationCsv(memorization, students).removePrefix("\uFEFF"))

    return sb.toString()
  }

  // =========================================================================
  // CSV PARSING & IMPORT
  // =========================================================================

  /**
   * Parses CSV string of Students (supports comma, semicolon, tab delimiters and quoted tokens)
   */
  fun parseStudentsCsv(csvContent: String): Result<List<Student>> {
    return try {
      val cleanContent = csvContent.removePrefix("\uFEFF").trim()
      if (cleanContent.isBlank()) {
        return Result.failure(IllegalArgumentException("CSV dosyası boş!"))
      }

      val lines = cleanContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
      if (lines.isEmpty()) {
        return Result.failure(IllegalArgumentException("Geçerli bir CSV satırı bulunamadı."))
      }

      // Determine delimiter from first line (, or ; or \t)
      val headerLine = lines.first()
      val delimiter = when {
        headerLine.count { it == ';' } > headerLine.count { it == ',' } -> ';'
        headerLine.count { it == '\t' } > headerLine.count { it == ',' } -> '\t'
        else -> ','
      }

      val studentList = mutableListOf<Student>()
      var startIndex = 0

      // Check if first row is a header
      val firstRowTokens = parseCsvLine(headerLine, delimiter)
      val isHeader = firstRowTokens.any { token ->
        val lower = token.lowercase(Locale.ROOT)
        lower.contains("ad") || lower.contains("soyad") || lower.contains("isim") || lower.contains("name") || lower.contains("öğrenci") || lower.contains("talebe")
      }
      if (isHeader) {
        startIndex = 1
      }

      for (i in startIndex until lines.size) {
        val line = lines[i]
        if (line.startsWith("---") || line.startsWith("###")) continue // Skip separator banners

        val tokens = parseCsvLine(line, delimiter)
        if (tokens.isEmpty()) continue

        // Check if token[0] is an ID integer or a Name
        var id = 0L
        var nameIdx = 0
        if (tokens.isNotEmpty() && tokens[0].toLongOrNull() != null) {
          id = tokens[0].toLong()
          nameIdx = 1
        }

        if (nameIdx >= tokens.size) continue
        val fullName = tokens.getOrNull(nameIdx)?.trim() ?: ""
        if (fullName.isBlank()) continue

        val grade = tokens.getOrNull(nameIdx + 1)?.trim().takeIf { !it.isNullOrBlank() } ?: "5. Sınıf"
        val phone = tokens.getOrNull(nameIdx + 2)?.trim() ?: ""
        val parentName = tokens.getOrNull(nameIdx + 3)?.trim() ?: ""
        val parentPhone = tokens.getOrNull(nameIdx + 4)?.trim() ?: ""
        val enrollmentDate = tokens.getOrNull(nameIdx + 5)?.trim().takeIf { !it.isNullOrBlank() } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val status = tokens.getOrNull(nameIdx + 6)?.trim().takeIf { !it.isNullOrBlank() } ?: "Aktif"
        val notes = tokens.getOrNull(nameIdx + 7)?.trim() ?: ""

        studentList.add(
          Student(
            id = id,
            fullName = fullName,
            grade = grade,
            phone = phone,
            parentName = parentName,
            parentPhone = parentPhone,
            enrollmentDate = enrollmentDate,
            status = status,
            notes = notes,
            avatarColorIndex = (studentList.size % 8)
          )
        )
      }

      if (studentList.isEmpty()) {
        Result.failure(IllegalArgumentException("CSV içerisinde geçerli öğrenci bilgisi bulunamadı."))
      } else {
        Result.success(studentList)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun parseCsvLine(line: String, delimiter: Char): List<String> {
    val tokens = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false

    var i = 0
    while (i < line.length) {
      val c = line[i]
      if (c == '\"') {
        if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
          sb.append('\"')
          i++
        } else {
          inQuotes = !inQuotes
        }
      } else if (c == delimiter && !inQuotes) {
        tokens.add(sb.toString().trim())
        sb.clear()
      } else {
        sb.append(c)
      }
      i++
    }
    tokens.add(sb.toString().trim())
    return tokens
  }

  // =========================================================================
  // SHARING & FILENAME HELPERS
  // =========================================================================

  fun getBackupFileName(extension: String = "json"): String {
    return "mektebi_irfan_yedek_${fileDateFormat.format(Date())}.$extension"
  }

  fun shareBackupData(context: Context, data: String, fileName: String, mimeType: String = "application/json") {
    try {
      // Create export cache directory
      val exportDir = File(context.cacheDir, "exports")
      if (!exportDir.exists()) {
        exportDir.mkdirs()
      }

      val file = File(exportDir, fileName)
      FileOutputStream(file).use { fos ->
        fos.write(data.toByteArray(Charsets.UTF_8))
        fos.flush()
      }

      val fileUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, fileUri)
        putExtra(Intent.EXTRA_SUBJECT, "Mekteb-i İrfan Yedek Dosyası ($fileName)")
        putExtra(Intent.EXTRA_TEXT, "Mekteb-i İrfan veri yedekleme dosyası: $fileName")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newRawUri(fileName, fileUri)
      }

      val chooser = Intent.createChooser(intent, "Dosyayı Paylaş / Farklı Cihaza Aktar ($fileName)").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      // Grant permission explicitly to matching packages
      val resInfoList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
      for (resolveInfo in resInfoList) {
        val packageName = resolveInfo.activityInfo.packageName
        context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      context.startActivity(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
      // Fallback to text intent if file creation fails
      try {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_SUBJECT, "Mekteb-i İrfan Yedek ($fileName)")
          putExtra(Intent.EXTRA_TEXT, data)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Yedek Paylaş"))
      } catch (ex: Exception) {
        ex.printStackTrace()
      }
    }
  }
}

