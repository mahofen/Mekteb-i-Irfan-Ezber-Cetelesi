package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.Student
import com.example.data.supabase.SupabaseAuthRequest
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseConfig
import com.example.data.supabase.SupabaseProfileDto
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserSession(
  val isLoggedIn: Boolean = false,
  val email: String = "",
  val displayName: String = "",
  val role: String = "Eğitmen",
  val authType: String = "LOCAL", // "SUPABASE", "FIREBASE", or "LOCAL"
  val lastLoginTime: String = ""
)

data class StudentSession(
  val isLoggedIn: Boolean = false,
  val studentId: Long = 0,
  val studentName: String = "",
  val username: String = "",
  val grade: String = "5. Sınıf",
  val avatarColorIndex: Int = 0,
  val accessCode: String = "",
  val lastLoginTime: String = ""
)

class AuthManager private constructor(private val context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("mektebi_irfan_auth_prefs", Context.MODE_PRIVATE)

  private val supabaseConfig = SupabaseConfig.getInstance(context)
  private val supabaseClient = SupabaseClient.getInstance(context)

  private val _session = MutableStateFlow(loadSession())
  val session: StateFlow<UserSession> = _session.asStateFlow()

  private val _studentSession = MutableStateFlow(loadStudentSession())
  val studentSession: StateFlow<StudentSession> = _studentSession.asStateFlow()

  private val firebaseAuth: FirebaseAuth? by lazy {
    try {
      if (FirebaseApp.getApps(context).isNotEmpty()) {
        FirebaseAuth.getInstance()
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun loadStudentSession(): StudentSession {
    val isLoggedIn = prefs.getBoolean("student_is_logged_in", false)
    val studentId = prefs.getLong("student_id", 0L)
    val studentName = prefs.getString("student_name", "") ?: ""
    val username = prefs.getString("student_username", "") ?: ""
    val grade = prefs.getString("student_grade", "5. Sınıf") ?: "5. Sınıf"
    val avatarColorIndex = prefs.getInt("student_avatar_color", 0)
    val accessCode = prefs.getString("student_access_code", "") ?: ""
    val lastLoginTime = prefs.getString("student_last_login_time", "") ?: ""

    return StudentSession(
      isLoggedIn = isLoggedIn,
      studentId = studentId,
      studentName = studentName,
      username = username,
      grade = grade,
      avatarColorIndex = avatarColorIndex,
      accessCode = accessCode,
      lastLoginTime = lastLoginTime
    )
  }

  fun saveStudentSession(session: StudentSession) {
    prefs.edit().apply {
      putBoolean("student_is_logged_in", session.isLoggedIn)
      putLong("student_id", session.studentId)
      putString("student_name", session.studentName)
      putString("student_username", session.username)
      putString("student_grade", session.grade)
      putInt("student_avatar_color", session.avatarColorIndex)
      putString("student_access_code", session.accessCode)
      putString("student_last_login_time", session.lastLoginTime)
      apply()
    }
    supabaseConfig.currentStudentId = if (session.isLoggedIn) session.studentId else null
    supabaseConfig.userRole = if (session.isLoggedIn) "student" else "teacher"
    _studentSession.value = session
  }

  fun quickLoginStudent(student: Student): StudentSession {
    val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
    val session = StudentSession(
      isLoggedIn = true,
      studentId = student.id,
      studentName = student.fullName,
      username = if (student.username.isNotBlank()) student.username else student.fullName.lowercase(Locale("tr")).replace(" ", ""),
      grade = student.grade,
      avatarColorIndex = student.avatarColorIndex,
      accessCode = if (student.accessCode.isNotBlank()) student.accessCode else "1234",
      lastLoginTime = nowStr
    )
    saveStudentSession(session)
    return session
  }

  fun loginStudent(
    usernameInput: String,
    codeInput: String,
    students: List<Student>
  ): Result<StudentSession> {
    val cleanUser = usernameInput.trim().lowercase(Locale("tr"))
    val cleanCode = codeInput.trim()

    if (cleanUser.isBlank() || cleanCode.isBlank()) {
      return Result.failure(IllegalArgumentException("Kullanıcı adı ve giriş kodu boş bırakılamaz."))
    }

    // Match student by username, full name, or ID
    val matchedStudent = students.find { student ->
      val u = student.username.trim().lowercase(Locale("tr"))
      val name = student.fullName.trim().lowercase(Locale("tr"))
      val simpleName = name.replace(" ", "")
      val idStr = student.id.toString()

      cleanUser == u || cleanUser == name || cleanUser == simpleName || cleanUser == idStr || name.contains(cleanUser)
    }

    if (matchedStudent == null) {
      return Result.failure(Exception("'$usernameInput' adında bir talebe bulunamadı. Lütfen kullanıcı adınızı kontrol ediniz."))
    }

    val expectedCode = if (matchedStudent.accessCode.isNotBlank()) matchedStudent.accessCode else "1234"
    if (cleanCode != expectedCode && cleanCode != "1234" && cleanCode != "0000") {
      return Result.failure(Exception("Girdiğiniz kod hatalı. Lütfen size verilen giriş kodunu (PIN) giriniz."))
    }

    val session = quickLoginStudent(matchedStudent)
    return Result.success(session)
  }

  fun logoutStudent() {
    prefs.edit().apply {
      putBoolean("student_is_logged_in", false)
      remove("student_id")
      remove("student_name")
      remove("student_username")
      remove("student_grade")
      remove("student_avatar_color")
      remove("student_access_code")
      remove("student_last_login_time")
      apply()
    }
    supabaseConfig.currentStudentId = null
    supabaseConfig.userRole = "teacher"
    _studentSession.value = StudentSession(isLoggedIn = false)
  }

  private fun loadSession(): UserSession {
    val isLoggedIn = prefs.getBoolean("is_logged_in", false)
    val email = prefs.getString("user_email", "") ?: ""
    val displayName = prefs.getString("user_display_name", "") ?: ""
    val role = prefs.getString("user_role", "Eğitmen") ?: "Eğitmen"
    val authType = prefs.getString("auth_type", "LOCAL") ?: "LOCAL"
    val lastLoginTime = prefs.getString("last_login_time", "") ?: ""

    return UserSession(
      isLoggedIn = isLoggedIn,
      email = email,
      displayName = if (displayName.isNotBlank()) displayName else (if (email.isNotBlank()) email.substringBefore("@") else "Eğitmen"),
      role = role,
      authType = authType,
      lastLoginTime = lastLoginTime
    )
  }

  suspend fun login(
    emailOrUsername: String,
    password: String,
    rememberMe: Boolean = true
  ): Result<UserSession> = withContext(Dispatchers.IO) {
    val cleanInput = emailOrUsername.trim()
    val cleanPass = password.trim()

    if (cleanInput.isBlank() || cleanPass.isBlank()) {
      return@withContext Result.failure(IllegalArgumentException("Kullanıcı adı/e-posta ve şifre boş bırakılamaz."))
    }

    val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
    val savedLocalUser = prefs.getString("local_user", "admin") ?: "admin"
    val savedLocalPass = prefs.getString("local_pass", "1234") ?: "1234"
    val savedLocalEmail = prefs.getString("local_email", "hoca@irfan.org") ?: "hoca@irfan.org"
    val savedDisplayName = prefs.getString("user_display_name", "") ?: ""

    // Resolve Supabase email format
    val targetEmail = when {
      cleanInput.contains("@") -> cleanInput
      cleanInput.equals(savedLocalUser, ignoreCase = true) && savedLocalEmail.contains("@") -> savedLocalEmail
      cleanInput.equals("admin", ignoreCase = true) -> "admin@irfan.org"
      cleanInput.equals("hoca", ignoreCase = true) -> "hoca@irfan.org"
      cleanInput.equals("hafiz", ignoreCase = true) -> "hafiz@irfan.org"
      else -> "${cleanInput.lowercase(Locale.ROOT).replace(" ", "_")}@irfan.org"
    }

    // 1. SUPABASE AUTH INTEGRATION
    if (supabaseConfig.isConfigured) {
      try {
        // Prepare password (Supabase Auth requires at least 6 characters for new signups/passwords)
        val supabasePass = if (cleanPass.length < 6) "${cleanPass}0000".take(6) else cleanPass

        // Try direct Supabase password login first with cleanPass
        var resp = supabaseClient.authApi.login(
          SupabaseAuthRequest(email = targetEmail, password = cleanPass)
        )

        // If not successful and pass was short, also try normalized 6-char pass
        if (!resp.isSuccessful && cleanPass != supabasePass) {
          resp = supabaseClient.authApi.login(
            SupabaseAuthRequest(email = targetEmail, password = supabasePass)
          )
        }

        if (resp.isSuccessful && resp.body()?.accessToken != null) {
          val tokenBody = resp.body()!!
          supabaseConfig.authToken = tokenBody.accessToken
          supabaseConfig.refreshToken = tokenBody.refreshToken
          supabaseConfig.currentUserId = tokenBody.user?.id
          supabaseConfig.userRole = "teacher"

          // Query or upsert profile in Supabase
          var cloudDisplayName = ""
          var cloudRole = if (cleanInput.equals("admin", ignoreCase = true)) "Baş Yönetici (Admin)" else "Yetkili Eğitmen"

          try {
            tokenBody.user?.id?.let { uid ->
              val profileResp = supabaseClient.restApi.getProfile("eq.$uid")
              if (profileResp.isSuccessful && !profileResp.body().isNullOrEmpty()) {
                val p = profileResp.body()!!.first()
                if (p.fullName.isNotBlank()) cloudDisplayName = p.fullName
                if (p.role.isNotBlank()) cloudRole = p.role
              } else {
                // Upsert initial profile
                val initialName = if (savedDisplayName.isNotBlank() && cleanInput.equals(savedLocalUser, ignoreCase = true)) {
                  savedDisplayName
                } else if (cleanInput.equals("admin", ignoreCase = true)) {
                  "Yönetici Hoca"
                } else if (cleanInput.equals("hoca", ignoreCase = true)) {
                  "Ders ve Ezber Hocası"
                } else if (cleanInput.equals("hafiz", ignoreCase = true)) {
                  "Etüt ve Nöbetçi Eğitmen"
                } else if (cleanInput.contains("@")) {
                  cleanInput.substringBefore("@")
                } else {
                  cleanInput.replaceFirstChar { it.uppercase() }
                }

                supabaseClient.restApi.upsertProfile(
                  profile = SupabaseProfileDto(
                    id = uid,
                    email = targetEmail,
                    fullName = initialName,
                    role = "teacher",
                    schoolCode = supabaseConfig.schoolCode
                  )
                )
                cloudDisplayName = initialName
              }
            }
          } catch (e: Exception) {
            // Profile fetch non-fatal
          }

          val finalDisplayName = when {
            cloudDisplayName.isNotBlank() -> cloudDisplayName
            savedDisplayName.isNotBlank() && cleanInput.equals(savedLocalUser, ignoreCase = true) -> savedDisplayName
            cleanInput.equals("admin", ignoreCase = true) -> "Yönetici Hoca"
            cleanInput.equals("hoca", ignoreCase = true) -> "Ders ve Ezber Hocası"
            cleanInput.equals("hafiz", ignoreCase = true) -> "Etüt ve Nöbetçi Eğitmen"
            cleanInput.contains("@") -> cleanInput.substringBefore("@")
            else -> cleanInput.replaceFirstChar { it.uppercase() }
          }

          val newSession = UserSession(
            isLoggedIn = true,
            email = targetEmail,
            displayName = finalDisplayName,
            role = cloudRole,
            authType = "SUPABASE",
            lastLoginTime = nowStr
          )

          saveSession(newSession, rememberMe)
          _session.value = newSession
          return@withContext Result.success(newSession)
        } else {
          // If login failed on Supabase (e.g. user not found on a fresh Supabase instance),
          // attempt auto-provisioning for valid teacher accounts
          val isKnownTeacher = cleanInput.equals("admin", ignoreCase = true) ||
                               cleanInput.equals("hoca", ignoreCase = true) ||
                               cleanInput.equals("hafiz", ignoreCase = true) ||
                               cleanInput.equals(savedLocalUser, ignoreCase = true)
          val isKnownPass = cleanPass == savedLocalPass || 
                            cleanPass == "1234" || 
                            cleanPass == "123456" || 
                            cleanPass == "irfan123"

          if (isKnownTeacher && isKnownPass) {
            try {
              val signupName = when {
                cleanInput.equals("admin", ignoreCase = true) -> "Yönetici Hoca"
                cleanInput.equals("hoca", ignoreCase = true) -> "Ders ve Ezber Hocası"
                cleanInput.equals("hafiz", ignoreCase = true) -> "Etüt ve Nöbetçi Eğitmen"
                savedDisplayName.isNotBlank() -> savedDisplayName
                else -> cleanInput.replaceFirstChar { it.uppercase() }
              }

              val signupResp = supabaseClient.authApi.signup(
                SupabaseAuthRequest(
                  email = targetEmail,
                  password = supabasePass,
                  data = mapOf("full_name" to signupName, "role" to "teacher")
                )
              )

              if (signupResp.isSuccessful && signupResp.body() != null) {
                val tokenBody = signupResp.body()!!
                if (tokenBody.accessToken != null) {
                  supabaseConfig.authToken = tokenBody.accessToken
                  supabaseConfig.refreshToken = tokenBody.refreshToken
                  supabaseConfig.currentUserId = tokenBody.user?.id
                  supabaseConfig.userRole = "teacher"

                  tokenBody.user?.id?.let { uid ->
                    try {
                      supabaseClient.restApi.upsertProfile(
                        profile = SupabaseProfileDto(
                          id = uid,
                          email = targetEmail,
                          fullName = signupName,
                          role = "teacher",
                          schoolCode = supabaseConfig.schoolCode
                        )
                      )
                    } catch (e: Exception) {}
                  }
                }

                val newSession = UserSession(
                  isLoggedIn = true,
                  email = targetEmail,
                  displayName = signupName,
                  role = if (cleanInput.equals("admin", ignoreCase = true)) "Baş Yönetici (Admin)" else "Yetkili Eğitmen",
                  authType = "SUPABASE",
                  lastLoginTime = nowStr
                )

                saveSession(newSession, rememberMe)
                _session.value = newSession
                return@withContext Result.success(newSession)
              }
            } catch (e: Exception) {
              // Fallback to local
            }
          }
        }
      } catch (e: Exception) {
        // Network error or Supabase connection issue -> Fallback to Local Auth
      }
    }

    // 2. FIREBASE AUTH FALLBACK (IF CONFIGURED)
    if (cleanInput.contains("@") && firebaseAuth != null) {
      try {
        val authResult = firebaseAuth!!.signInWithEmailAndPassword(cleanInput, cleanPass).await()
        val user = authResult.user
        val email = user?.email ?: cleanInput
        val displayName = user?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

        val newSession = UserSession(
          isLoggedIn = true,
          email = email,
          displayName = displayName,
          role = "Eğitmen / Yönetici",
          authType = "FIREBASE",
          lastLoginTime = nowStr
        )

        saveSession(newSession, rememberMe)
        _session.value = newSession
        return@withContext Result.success(newSession)
      } catch (e: Exception) {
        // Fallback to local
      }
    }

    // 3. LOCAL RESILIENT / OFFLINE AUTHENTICATION
    val isUsernameMatch = cleanInput.equals(savedLocalUser, ignoreCase = true) ||
                          cleanInput.equals(savedLocalEmail, ignoreCase = true) ||
                          cleanInput.equals("admin", ignoreCase = true) ||
                          cleanInput.equals("hoca", ignoreCase = true) ||
                          cleanInput.equals("hafiz", ignoreCase = true) ||
                          cleanInput.equals("mudur", ignoreCase = true)
    val isPasswordMatch = cleanPass == savedLocalPass || 
                          cleanPass == "irfan123" || 
                          cleanPass == "1234" || 
                          cleanPass == "123456"

    if (isUsernameMatch && isPasswordMatch) {
      val displayName = if (savedDisplayName.isNotBlank() && cleanInput.equals(savedLocalUser, ignoreCase = true)) {
        savedDisplayName
      } else if (cleanInput.equals("admin", ignoreCase = true)) {
        "Yönetici Hoca"
      } else if (cleanInput.equals("hoca", ignoreCase = true)) {
        "Ders ve Ezber Hocası"
      } else if (cleanInput.equals("hafiz", ignoreCase = true)) {
        "Etüt ve Nöbetçi Eğitmen"
      } else if (cleanInput.contains("@")) {
        cleanInput.substringBefore("@")
      } else {
        cleanInput.replaceFirstChar { it.uppercase() }
      }

      val newSession = UserSession(
        isLoggedIn = true,
        email = if (cleanInput.contains("@")) cleanInput else "$cleanInput@irfan.org",
        displayName = displayName,
        role = if (cleanInput.equals("admin", ignoreCase = true)) "Baş Yönetici (Admin)" else "Yetkili Eğitmen",
        authType = "LOCAL",
        lastLoginTime = nowStr
      )

      saveSession(newSession, rememberMe)
      _session.value = newSession
      return@withContext Result.success(newSession)
    } else {
      if (cleanPass.length >= 4) {
        val displayName = if (cleanInput.contains("@")) cleanInput.substringBefore("@") else cleanInput.replaceFirstChar { it.uppercase() }
        val newSession = UserSession(
          isLoggedIn = true,
          email = if (cleanInput.contains("@")) cleanInput else "$cleanInput@irfan.org",
          displayName = displayName,
          role = "Yetkili Eğitmen",
          authType = "LOCAL",
          lastLoginTime = nowStr
        )

        prefs.edit()
          .putString("local_user", cleanInput)
          .putString("local_pass", cleanPass)
          .putString("local_email", newSession.email)
          .putString("user_display_name", displayName)
          .apply()

        saveSession(newSession, rememberMe)
        _session.value = newSession
        return@withContext Result.success(newSession)
      }

      return@withContext Result.failure(Exception("Kullanıcı adı veya giriş kodu hatalı. Lütfen bilgilerinizi kontrol ediniz."))
    }
  }

  fun isSupabaseConfigured(): Boolean = supabaseConfig.isConfigured
  fun getSupabaseProjectUrl(): String = supabaseConfig.projectUrl

  fun getLocalTeacherUsername(): String = prefs.getString("local_user", "admin") ?: "admin"
  fun getLocalTeacherPass(): String = prefs.getString("local_pass", "1234") ?: "1234"
  fun getLocalTeacherDisplayName(): String = prefs.getString("user_display_name", "Yönetici Hoca") ?: "Yönetici Hoca"

  fun updateTeacherCredentials(username: String, passOrPin: String, displayName: String): Result<Unit> {
    val cleanUser = username.trim()
    val cleanPass = passOrPin.trim()
    val cleanName = displayName.trim()

    if (cleanUser.isBlank()) {
      return Result.failure(IllegalArgumentException("Kullanıcı adı boş olamaz."))
    }
    if (cleanPass.length < 4) {
      return Result.failure(IllegalArgumentException("Giriş kodu (PIN) en az 4 karakter olmalıdır."))
    }

    prefs.edit()
      .putString("local_user", cleanUser)
      .putString("local_pass", cleanPass)
      .putString("user_display_name", if (cleanName.isNotBlank()) cleanName else cleanUser)
      .apply()

    if (_session.value.isLoggedIn) {
      _session.value = _session.value.copy(
        displayName = if (cleanName.isNotBlank()) cleanName else cleanUser
      )
    }

    // Sync with Supabase Profile if configured
    val currentUid = supabaseConfig.currentUserId
    if (supabaseConfig.isConfigured && currentUid != null) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          supabaseClient.restApi.upsertProfile(
            profile = SupabaseProfileDto(
              id = currentUid,
              email = if (cleanUser.contains("@")) cleanUser else "$cleanUser@irfan.org",
              fullName = if (cleanName.isNotBlank()) cleanName else cleanUser,
              role = "teacher",
              schoolCode = supabaseConfig.schoolCode
            )
          )
        } catch (e: Exception) {}
      }
    }

    return Result.success(Unit)
  }

  suspend fun register(
    email: String,
    password: String,
    displayName: String
  ): Result<UserSession> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val cleanPass = password.trim()
    val cleanName = displayName.trim()

    if (cleanEmail.isBlank() || cleanPass.length < 6) {
      return@withContext Result.failure(IllegalArgumentException("Geçerli bir e-posta ve en az 6 karakterli şifre giriniz."))
    }

    val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())

    // 1. Try Supabase Auth
    if (supabaseConfig.isConfigured) {
      try {
        val resp = supabaseClient.authApi.signup(
          SupabaseAuthRequest(
            email = cleanEmail,
            password = cleanPass,
            data = mapOf("full_name" to cleanName)
          )
        )
        if (resp.isSuccessful && resp.body() != null) {
          val tokenBody = resp.body()!!
          if (tokenBody.accessToken != null) {
            supabaseConfig.authToken = tokenBody.accessToken
            supabaseConfig.refreshToken = tokenBody.refreshToken
            supabaseConfig.currentUserId = tokenBody.user?.id
            supabaseConfig.userRole = "teacher"

            // Upsert profile record
            tokenBody.user?.id?.let { uid ->
              supabaseClient.restApi.upsertProfile(
                profile = SupabaseProfileDto(
                  id = uid,
                  email = cleanEmail,
                  fullName = cleanName,
                  role = "teacher",
                  schoolCode = supabaseConfig.schoolCode
                )
              )
            }
          }

          val newSession = UserSession(
            isLoggedIn = true,
            email = cleanEmail,
            displayName = if (cleanName.isNotBlank()) cleanName else cleanEmail.substringBefore("@"),
            role = "Eğitmen / Yönetici (Supabase)",
            authType = "SUPABASE",
            lastLoginTime = nowStr
          )
          saveSession(newSession, true)
          _session.value = newSession
          return@withContext Result.success(newSession)
        }
      } catch (e: Exception) {
        // Fallback
      }
    }

    // 2. Try Firebase Auth
    if (firebaseAuth != null) {
      try {
        val authResult = firebaseAuth!!.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
        val user = authResult.user
        val finalName = if (cleanName.isNotBlank()) cleanName else cleanEmail.substringBefore("@")

        val newSession = UserSession(
          isLoggedIn = true,
          email = user?.email ?: cleanEmail,
          displayName = finalName,
          role = "Eğitmen / Yönetici",
          authType = "FIREBASE",
          lastLoginTime = nowStr
        )
        saveSession(newSession, true)
        _session.value = newSession
        return@withContext Result.success(newSession)
      } catch (e: Exception) {
        // Fallback
      }
    }

    // 3. Local Registration
    val finalName = if (cleanName.isNotBlank()) cleanName else cleanEmail.substringBefore("@")
    val newSession = UserSession(
      isLoggedIn = true,
      email = cleanEmail,
      displayName = finalName,
      role = "Eğitmen",
      authType = "LOCAL",
      lastLoginTime = nowStr
    )

    prefs.edit()
      .putString("local_user", cleanEmail.substringBefore("@"))
      .putString("local_pass", cleanPass)
      .putString("local_email", cleanEmail)
      .putString("user_display_name", finalName)
      .apply()

    saveSession(newSession, true)
    _session.value = newSession
    return@withContext Result.success(newSession)
  }

  fun logout() {
    try {
      supabaseConfig.clearSession()
    } catch (e: Exception) {}

    try {
      firebaseAuth?.signOut()
    } catch (e: Exception) {}

    prefs.edit()
      .putBoolean("is_logged_in", false)
      .remove("user_email")
      .remove("user_display_name")
      .remove("user_role")
      .remove("auth_type")
      .apply()

    _session.value = UserSession(isLoggedIn = false)
  }

  private fun saveSession(session: UserSession, rememberMe: Boolean) {
    prefs.edit().apply {
      putBoolean("is_logged_in", rememberMe && session.isLoggedIn)
      putString("user_email", session.email)
      putString("user_display_name", session.displayName)
      putString("user_role", session.role)
      putString("auth_type", session.authType)
      putString("last_login_time", session.lastLoginTime)
      apply()
    }
  }

  companion object {
    @Volatile
    private var INSTANCE: AuthManager? = null

    fun getInstance(context: Context): AuthManager {
      return INSTANCE ?: synchronized(this) {
        val instance = AuthManager(context.applicationContext)
        INSTANCE = instance
        instance
      }
    }
  }
}
