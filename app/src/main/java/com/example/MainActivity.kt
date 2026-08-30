package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.components.StudentBottomNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.ThemeMode

class MainActivity : ComponentActivity() {
  private val viewModel: AppViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

      val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = isDark) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = CanvasBackground
        ) {
          MainAppContent(viewModel)
        }
      }
    }
  }
}

@Composable
fun MainAppContent(viewModel: AppViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val userSession by viewModel.userSession.collectAsStateWithLifecycle()
  val studentSession by viewModel.studentSession.collectAsStateWithLifecycle()
  val currentLoggedInStudent by viewModel.currentLoggedInStudent.collectAsStateWithLifecycle()
  val studentProgressStats by viewModel.studentProgressStats.collectAsStateWithLifecycle()
  val studentDuties by viewModel.studentDuties.collectAsStateWithLifecycle()
  val studentActiveMemorizations by viewModel.studentActiveMemorizations.collectAsStateWithLifecycle()
  val studentCompletedMemorizations by viewModel.studentCompletedMemorizations.collectAsStateWithLifecycle()

  val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
  val students by viewModel.allStudents.collectAsStateWithLifecycle()
  val allAttendance by viewModel.allAttendance.collectAsStateWithLifecycle()
  val allMemorization by viewModel.allMemorization.collectAsStateWithLifecycle()
  val allDuties by viewModel.allDuties.collectAsStateWithLifecycle()
  val selectedDate by viewModel.selectedAttendanceDate.collectAsStateWithLifecycle()
  val attendanceForSelectedDate by viewModel.attendanceForSelectedDate.collectAsStateWithLifecycle()
  val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
  val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
  val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
  val reminderAudience by viewModel.reminderAudience.collectAsStateWithLifecycle()
  val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
  val cloudSchoolCode by viewModel.cloudSchoolCode.collectAsStateWithLifecycle()
  val scheduleLessons by viewModel.allScheduleLessons.collectAsStateWithLifecycle()
  val selectedScheduleDay by viewModel.selectedScheduleDay.collectAsStateWithLifecycle()

  val isStudentScreen = currentScreen == AppScreen.STUDENT_DASHBOARD ||
    currentScreen == AppScreen.STUDENT_UPCOMING_MEMORIZATION ||
    currentScreen == AppScreen.STUDENT_PAST_MEMORIZATION ||
    currentScreen == AppScreen.STUDENT_DAILY_DUTIES ||
    currentScreen == AppScreen.STUDENT_PROFILE

  val isTeacherScreen = currentScreen == AppScreen.MAIN_MENU ||
    currentScreen == AppScreen.SCHEDULE ||
    currentScreen == AppScreen.STUDENTS ||
    currentScreen == AppScreen.ATTENDANCE ||
    currentScreen == AppScreen.MEMORIZATION ||
    currentScreen == AppScreen.DEVELOPMENT ||
    currentScreen == AppScreen.REPORTS

  // Handle system back navigation
  BackHandler(enabled = currentScreen != AppScreen.SPLASH && currentScreen != AppScreen.STUDENT_DASHBOARD && currentScreen != AppScreen.MAIN_MENU && currentScreen != AppScreen.STUDENT_LOGIN && currentScreen != AppScreen.TEACHER_LOGIN) {
    if (isStudentScreen) {
      viewModel.navigateTo(AppScreen.STUDENT_DASHBOARD)
    } else {
      viewModel.navigateTo(AppScreen.MAIN_MENU)
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = CanvasBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      if (isStudentScreen) {
        StudentBottomNavigationBar(
          currentScreen = currentScreen,
          onNavigate = { targetScreen ->
            viewModel.navigateTo(targetScreen)
          }
        )
      } else if (isTeacherScreen) {
        AppBottomNavigationBar(
          currentScreen = currentScreen,
          onNavigate = { targetScreen ->
            viewModel.navigateTo(targetScreen)
          }
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
        when (screen) {
          AppScreen.SPLASH -> {
            SplashScreen(
              onEnterClick = {
                if (studentSession.isLoggedIn) {
                  viewModel.navigateTo(AppScreen.STUDENT_DASHBOARD)
                } else if (userSession.isLoggedIn) {
                  viewModel.navigateTo(AppScreen.MAIN_MENU)
                } else {
                  viewModel.navigateTo(AppScreen.STUDENT_LOGIN)
                }
              }
            )
          }

          AppScreen.STUDENT_LOGIN -> {
            StudentLoginScreen(
              students = students,
              onLoginSuccess = {
                viewModel.navigateTo(AppScreen.STUDENT_DASHBOARD)
              },
              onQuickSelectStudent = { selectedStudent ->
                viewModel.quickLoginStudent(selectedStudent)
              },
              onNavigateToTeacherPortal = {
                viewModel.navigateTo(AppScreen.TEACHER_LOGIN)
              },
              onLoginSubmit = { username, code ->
                viewModel.loginStudent(username, code)
              }
            )
          }

          AppScreen.TEACHER_LOGIN -> {
            TeacherLoginScreen(
              userSession = userSession,
              onLoginSuccess = {
                viewModel.navigateTo(AppScreen.MAIN_MENU)
              },
              onNavigateToStudentPortal = {
                viewModel.navigateTo(AppScreen.STUDENT_LOGIN)
              },
              onLoginSubmit = { username, code, rememberMe ->
                viewModel.loginTeacher(username, code, rememberMe)
              },
              onUpdateCredentials = { username, code, displayName ->
                viewModel.updateTeacherCredentials(username, code, displayName)
              },
              getSavedUsername = { viewModel.getSavedTeacherUsername() },
              getSavedPass = { viewModel.getSavedTeacherPass() },
              getSavedDisplayName = { viewModel.getSavedTeacherDisplayName() },
              isSupabaseConfigured = viewModel.isSupabaseConfigured(),
              viewModel = viewModel
            )
          }

          AppScreen.STUDENT_DASHBOARD -> {
            StudentDashboardScreen(
              student = currentLoggedInStudent,
              studentSession = studentSession,
              stats = studentProgressStats,
              activeMemorizations = studentActiveMemorizations,
              completedMemorizations = studentCompletedMemorizations,
              recentDuties = studentDuties,
              todayDate = viewModel.todayDate,
              onNavigateToMemorization = { isUpcoming ->
                if (isUpcoming) {
                  viewModel.navigateTo(AppScreen.STUDENT_UPCOMING_MEMORIZATION)
                } else {
                  viewModel.navigateTo(AppScreen.STUDENT_PAST_MEMORIZATION)
                }
              },
              onNavigateToDuties = {
                viewModel.navigateTo(AppScreen.STUDENT_DAILY_DUTIES)
              },
              onNavigateToProfile = {
                viewModel.navigateTo(AppScreen.STUDENT_PROFILE)
              },
              onLogout = {
                viewModel.logoutStudent()
              },
              viewModel = viewModel
            )
          }

          AppScreen.STUDENT_UPCOMING_MEMORIZATION -> {
            StudentMemorizationScreen(
              student = currentLoggedInStudent,
              activeMemorizations = studentActiveMemorizations,
              completedMemorizations = studentCompletedMemorizations,
              initialTabUpcoming = true,
              onBackClick = { viewModel.navigateTo(AppScreen.STUDENT_DASHBOARD) },
              viewModel = viewModel
            )
          }

          AppScreen.STUDENT_PAST_MEMORIZATION -> {
            StudentMemorizationScreen(
              student = currentLoggedInStudent,
              activeMemorizations = studentActiveMemorizations,
              completedMemorizations = studentCompletedMemorizations,
              initialTabUpcoming = false,
              onBackClick = { viewModel.navigateTo(AppScreen.STUDENT_DASHBOARD) },
              viewModel = viewModel
            )
          }

          AppScreen.STUDENT_DAILY_DUTIES -> {
            StudentDutiesScreen(
              student = currentLoggedInStudent,
              duties = studentDuties,
              todayDate = viewModel.todayDate,
              onBackClick = { viewModel.navigateTo(AppScreen.STUDENT_DASHBOARD) },
              viewModel = viewModel
            )
          }

          AppScreen.STUDENT_PROFILE -> {
            StudentProfileScreen(
              student = currentLoggedInStudent,
              studentSession = studentSession,
              stats = studentProgressStats,
              memorizations = studentCompletedMemorizations + studentActiveMemorizations,
              onBackClick = { viewModel.navigateTo(AppScreen.STUDENT_DASHBOARD) },
              onLogout = { viewModel.logoutStudent() }
            )
          }

          AppScreen.MAIN_MENU -> {
            MainMenuScreen(
              students = students,
              attendanceList = allAttendance,
              memorizationList = allMemorization,
              todayDate = viewModel.todayDate,
              allDuties = allDuties,
              onNavigateStudents = { viewModel.navigateTo(AppScreen.STUDENTS) },
              onNavigateAttendance = { viewModel.navigateTo(AppScreen.ATTENDANCE) },
              onNavigateSchedule = { viewModel.navigateTo(AppScreen.SCHEDULE) },
              onNavigateMemorization = { viewModel.navigateTo(AppScreen.MEMORIZATION) },
              onNavigateDevelopment = { viewModel.navigateTo(AppScreen.DEVELOPMENT) },
              onNavigateReports = { viewModel.navigateTo(AppScreen.REPORTS) },
              onNavigateSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
              onNavigateAbout = { viewModel.navigateTo(AppScreen.ABOUT) },
              onNavigateStudentPortal = { viewModel.navigateTo(AppScreen.STUDENT_LOGIN) },
              userSession = userSession,
              onLogoutTeacher = { viewModel.logoutTeacher() }
            )
          }

          AppScreen.SCHEDULE -> {
            ScheduleScreen(
              lessons = scheduleLessons,
              students = students,
              attendanceForSelectedDate = attendanceForSelectedDate,
              allAttendanceList = allAttendance,
              selectedDay = selectedScheduleDay,
              selectedDate = selectedDate,
              todayDate = viewModel.todayDate,
              onDaySelected = { viewModel.selectScheduleDay(it) },
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
              onSetAttendance = { studentId, date, status ->
                viewModel.setAttendance(studentId, date, status)
              },
              onMarkAllStatus = { date, studentList, status ->
                viewModel.markAllStatus(date, studentList, status)
              },
              onSaveLesson = { viewModel.saveScheduleLesson(it) },
              onDeleteLesson = { viewModel.deleteScheduleLesson(it) },
              onResetToDefault = { viewModel.resetScheduleToDefault() },
              onDateChange = { viewModel.setSelectedDate(it) }
            )
          }

          AppScreen.STUDENTS -> {
            StudentsScreen(
              students = students,
              attendanceList = allAttendance,
              memorizationList = allMemorization,
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
              onSaveStudent = { viewModel.saveStudent(it) },
              onDeleteStudent = { viewModel.deleteStudent(it) },
              onBulkSaveStudents = { list, overwrite ->
                viewModel.saveStudentsBulk(list, overwrite)
              }
            )
          }

          AppScreen.ATTENDANCE -> {
            AttendanceScreen(
              students = students,
              allAttendanceList = allAttendance,
              attendanceForSelectedDate = attendanceForSelectedDate,
              selectedDate = selectedDate,
              todayDate = viewModel.todayDate,
              allDuties = allDuties,
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
              onDateChange = { viewModel.setSelectedDate(it) },
              onSetAttendance = { studentId, date, status ->
                viewModel.setAttendance(studentId, date, status)
              },
              onMarkAllPresent = { date, studentList ->
                viewModel.markAllPresent(date, studentList)
              },
              onMarkAllStatus = { date, studentList, status ->
                viewModel.markAllStatus(date, studentList, status)
              },
              onSaveDuty = { viewModel.saveDailyDuty(it) },
              onUpdateDuty = { studentId, date, update ->
                viewModel.updateStudentDailyDuty(studentId, date, update)
              },
              onMarkAllPrayers = { date, studentList, allDone ->
                viewModel.markAllDutiesPrayers(date, studentList, allDone)
              },
              onNavigateSchedule = { viewModel.navigateTo(AppScreen.SCHEDULE) },
              viewModel = viewModel
            )
          }

          AppScreen.MEMORIZATION -> {
            MemorizationScreen(
              students = students,
              memorizationList = allMemorization,
              todayDate = viewModel.todayDate,
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
              onSaveMemorization = { viewModel.saveMemorization(it) },
              onDeleteMemorization = { viewModel.deleteMemorization(it) },
              viewModel = viewModel
            )
          }

          AppScreen.DEVELOPMENT -> {
            DevelopmentScreen(
              students = students,
              memorizationList = allMemorization,
              todayDate = viewModel.todayDate,
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
              viewModel = viewModel
            )
          }

          AppScreen.REPORTS -> {
            ReportsScreen(
              students = students,
              attendanceList = allAttendance,
              memorizationList = allMemorization,
              todayDate = viewModel.todayDate,
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
            )
          }

          AppScreen.SETTINGS -> {
            SettingsScreen(
              themeMode = themeMode,
              students = students,
              attendanceList = allAttendance,
              memorizationList = allMemorization,
              userSession = userSession,
              syncStatus = syncStatus,
              reminderEnabled = reminderEnabled,
              reminderHour = reminderHour,
              reminderMinute = reminderMinute,
              reminderAudience = reminderAudience,
              autoBackupEnabled = autoBackupEnabled,
              cloudSchoolCode = cloudSchoolCode,
              onAutoBackupEnabledChange = { viewModel.setAutoBackupEnabled(it) },
              onCloudSchoolCodeChange = { viewModel.setCloudSchoolCode(it) },
              onBackupToCloudNow = { code -> viewModel.backupToCloudNow(code) },
              onRestoreFromCloudNow = { code, mode -> viewModel.restoreFromCloudNow(code, mode) },
              onThemeModeChange = { viewModel.setThemeMode(it) },
              onReminderEnabledChange = { viewModel.setReminderEnabled(it) },
              onReminderTimeChange = { h, m -> viewModel.setReminderTime(h, m) },
              onReminderAudienceChange = { viewModel.setReminderAudience(it) },
              onSendTestNotification = { viewModel.sendTestNotification() },
              onSyncWithCloud = { viewModel.syncWithCloud() },
              onExportWebPortal = { viewModel.generateWebPortalHtml() },
              onLogout = { viewModel.logout() },
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
              onExportBackupJson = { viewModel.exportBackupJson() },
              onExportStudentsCsv = { viewModel.exportStudentsCsv() },
              onExportAttendanceCsv = { viewModel.exportAttendanceCsv() },
              onExportMemorizationCsv = { viewModel.exportMemorizationCsv() },
              onExportCombinedCsv = { viewModel.exportCombinedCsv() },
              onImportBackup = { json, mode -> viewModel.importBackupJson(json, mode) },
              onImportStudentsCsv = { csv, mode -> viewModel.importStudentsCsv(csv, mode) },
              onResetToSampleData = { viewModel.resetToDefaultSampleData() },
              onClearAllData = { viewModel.clearAllData() },
              onNavigateAbout = { viewModel.navigateTo(AppScreen.ABOUT) },
              viewModel = viewModel
            )
          }

          AppScreen.ABOUT -> {
            AboutScreen(
              onBackClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
            )
          }
        }
      }
    }
  }
}

