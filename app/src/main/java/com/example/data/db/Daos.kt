package com.example.data.db

import androidx.room.*
import com.example.data.model.AttendanceRecord
import com.example.data.model.DailyDutyRecord
import com.example.data.model.MemorizationRecord
import com.example.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
  @Query("SELECT * FROM students ORDER BY fullName ASC")
  fun getAllStudents(): Flow<List<Student>>

  @Query("SELECT * FROM students WHERE id = :id")
  suspend fun getStudentById(id: Long): Student?

  @Query("SELECT * FROM students WHERE LOWER(username) = LOWER(:username) OR LOWER(fullName) = LOWER(:username) LIMIT 1")
  suspend fun getStudentByUsername(username: String): Student?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudent(student: Student): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudents(students: List<Student>): List<Long>

  @Update
  suspend fun updateStudent(student: Student)

  @Delete
  suspend fun deleteStudent(student: Student)

  @Query("DELETE FROM students")
  suspend fun clearAllStudents()

  @Query("SELECT COUNT(*) FROM students WHERE status = 'Aktif'")
  fun getActiveStudentCount(): Flow<Int>
}

@Dao
interface AttendanceDao {
  @Query("SELECT * FROM attendance_records WHERE date = :date")
  fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

  @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY date DESC")
  fun getAttendanceByStudent(studentId: Long): Flow<List<AttendanceRecord>>

  @Query("SELECT * FROM attendance_records")
  fun getAllAttendance(): Flow<List<AttendanceRecord>>

  @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND date = :date LIMIT 1")
  suspend fun getAttendanceForStudentAndDate(studentId: Long, date: String): AttendanceRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateAttendance(record: AttendanceRecord): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttendanceList(records: List<AttendanceRecord>): List<Long>

  @Query("DELETE FROM attendance_records WHERE studentId = :studentId AND date = :date")
  suspend fun deleteAttendance(studentId: Long, date: String)

  @Query("DELETE FROM attendance_records")
  suspend fun clearAllAttendance()
}

@Dao
interface MemorizationDao {
  @Query("SELECT * FROM memorization_records ORDER BY id DESC")
  fun getAllMemorizationRecords(): Flow<List<MemorizationRecord>>

  @Query("SELECT * FROM memorization_records WHERE studentId = :studentId ORDER BY id DESC")
  fun getMemorizationByStudent(studentId: Long): Flow<List<MemorizationRecord>>

  @Query("SELECT * FROM memorization_records WHERE studentId = :studentId AND status = 'TAMAMLANDI' ORDER BY id DESC")
  fun getCompletedMemorizationsByStudent(studentId: Long): Flow<List<MemorizationRecord>>

  @Query("SELECT * FROM memorization_records WHERE studentId = :studentId AND status != 'TAMAMLANDI' ORDER BY id DESC")
  fun getActiveMemorizationsByStudent(studentId: Long): Flow<List<MemorizationRecord>>

  @Query("SELECT * FROM memorization_records WHERE studentId = :studentId AND LOWER(title) = LOWER(:title) LIMIT 1")
  suspend fun getMemorizationForStudentAndTitle(studentId: Long, title: String): MemorizationRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemorization(record: MemorizationRecord): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemorizationList(records: List<MemorizationRecord>): List<Long>

  @Update
  suspend fun updateMemorization(record: MemorizationRecord)

  @Delete
  suspend fun deleteMemorization(record: MemorizationRecord)

  @Query("DELETE FROM memorization_records")
  suspend fun clearAllMemorization()

  @Query("SELECT COUNT(*) FROM memorization_records WHERE status = 'TAMAMLANDI'")
  fun getCompletedCount(): Flow<Int>
}

@Dao
interface DailyDutyDao {
  @Query("SELECT * FROM daily_duty_records WHERE studentId = :studentId ORDER BY date DESC")
  fun getDutiesByStudent(studentId: Long): Flow<List<DailyDutyRecord>>

  @Query("SELECT * FROM daily_duty_records")
  fun getAllDuties(): Flow<List<DailyDutyRecord>>

  @Query("SELECT * FROM daily_duty_records WHERE studentId = :studentId AND date = :date LIMIT 1")
  suspend fun getDutyForStudentAndDate(studentId: Long, date: String): DailyDutyRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateDuty(record: DailyDutyRecord): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDutyList(records: List<DailyDutyRecord>): List<Long>

  @Delete
  suspend fun deleteDuty(record: DailyDutyRecord)

  @Query("DELETE FROM daily_duty_records")
  suspend fun clearAllDuties()
}

