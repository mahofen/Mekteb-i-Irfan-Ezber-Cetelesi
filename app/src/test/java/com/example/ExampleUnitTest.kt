package com.example

import com.example.data.model.DayOfWeekTr
import com.example.data.model.LessonCategory
import com.example.data.model.ScheduleLesson
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun dayOfWeekTr_fromCalendarDay_isCorrect() {
    assertEquals(DayOfWeekTr.MONDAY, DayOfWeekTr.fromCalendarDay(Calendar.MONDAY))
    assertEquals(DayOfWeekTr.FRIDAY, DayOfWeekTr.fromCalendarDay(Calendar.FRIDAY))
    assertEquals(DayOfWeekTr.SUNDAY, DayOfWeekTr.fromCalendarDay(Calendar.SUNDAY))
  }

  @Test
  fun lessonCategory_fromString_isCorrect() {
    assertEquals(LessonCategory.MEMORIZATION, LessonCategory.fromString("MEMORIZATION"))
    assertEquals(LessonCategory.TECVİD, LessonCategory.fromString("Tecvid & Mahreç"))
    assertEquals(LessonCategory.GENERAL, LessonCategory.fromString("UnknownCategory"))
  }

  @Test
  fun scheduleLesson_defaultValues_areValid() {
    val lesson = ScheduleLesson(
      day = DayOfWeekTr.MONDAY,
      startTime = "08:30",
      endTime = "10:00",
      title = "Has Hafızlık & Cüz Dinleme"
    )
    assertEquals(DayOfWeekTr.MONDAY, lesson.day)
    assertEquals("08:30", lesson.startTime)
    assertEquals("10:00", lesson.endTime)
    assertTrue(lesson.isMandatoryAttendance)
  }
}
