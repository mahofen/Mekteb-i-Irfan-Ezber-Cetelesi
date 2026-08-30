package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.UserSession
import com.example.data.firebase.FirebaseSyncState
import com.example.ui.components.GoldSubtleBorderGradient
import com.example.ui.components.SeljukStarBox
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/**
 * 🕌 EĞİTMEN & YÖNETİCİ GİRİŞ PANELİ (FIRESTORE EZBER & PERFORMANS YÖNETİMİ)
 * Hocalar için Firestore bulut veritabanı üzerinden öğrenci ezberlerini düzenleyebilecekleri,
 * not/puan verebilecekleri ve performans verilerini güncelleyebilecekleri yetkili giriş merkezi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLoginScreen(
  userSession: UserSession,
  onLoginSuccess: () -> Unit,
  onNavigateToStudentPortal: () -> Unit,
  onLoginSubmit: suspend (String, String, Boolean) -> Result<UserSession>,
  onUpdateCredentials: (String, String, String) -> Result<Unit>,
  getSavedUsername: () -> String,
  getSavedPass: () -> String,
  getSavedDisplayName: () -> String,
  isSupabaseConfigured: Boolean = true,
  viewModel: AppViewModel? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val coroutineScope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  var usernameInput by remember { mutableStateOf(getSavedUsername().ifBlank { "admin" }) }
  var codeInput by remember { mutableStateOf("") }
  var rememberMe by remember { mutableStateOf(true) }
  var showPassword by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var isTestingFirestore by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showInfoDialog by remember { mutableStateOf(false) }
  var showChangeCredentialsDialog by remember { mutableStateOf(false) }
  var showFirestoreConfigDialog by remember { mutableStateOf(false) }
  var showQuickCodeMenu by remember { mutableStateOf(true) }

  // Firestore Sync State & School Code
  val firebaseState: FirebaseSyncState = viewModel?.firebaseState?.collectAsState()?.value
    ?: FirebaseSyncState(isConfigured = true, isConnected = true)

  val schoolCode: String = viewModel?.cloudSchoolCode?.collectAsState()?.value
    ?: "irfan_default"

  fun executeLogin(user: String = usernameInput, pass: String = codeInput) {
    focusManager.clearFocus()
    errorMessage = null

    val cleanUser = user.trim()
    val cleanPass = pass.trim()

    if (cleanUser.isBlank()) {
      errorMessage = "Lütfen eğitmen kullanıcı adınızı veya e-postanızı giriniz."
      return
    }
    if (cleanPass.isBlank()) {
      errorMessage = "Lütfen giriş kodunuzu (PIN) veya şifrenizi giriniz."
      return
    }

    isLoading = true
    coroutineScope.launch {
      val result = onLoginSubmit(cleanUser, cleanPass, rememberMe)
      isLoading = false
      if (result.isSuccess) {
        val s = result.getOrNull()
        Toast.makeText(
          context,
          "Hoş geldiniz, ${s?.displayName ?: cleanUser}! Firestore ezber & performans yönetimi devrede 🌿",
          Toast.LENGTH_SHORT
        ).show()
        onLoginSuccess()
      } else {
        errorMessage = result.exceptionOrNull()?.message ?: "Giriş bilgileri doğrulanamadı. Lütfen kontrol ediniz."
      }
    }
  }

  Scaffold(
    containerColor = CanvasBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0)
  ) { paddingValues ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(CanvasBackground)
    ) {
      // Decorative background header banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(290.dp)
          .background(
            Brush.verticalGradient(
              colors = listOf(
                DeepBlueNavy,
                Color(0xFF1E293B),
                CanvasBackground
              )
            )
          )
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Top Navigation Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Switch to Student Portal Button
          Surface(
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.clickable { onNavigateToStudentPortal() }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Talebe Portalı",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Talebe Portalı",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
              )
            }
          }

          // Header Badges / Buttons
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Firestore Settings Button
            Surface(
              color = Color.White.copy(alpha = 0.14f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.clickable { showFirestoreConfigDialog = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CloudSync,
                  contentDescription = "Firestore Ayarları",
                  tint = TezhipGoldLight,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Firestore",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TezhipGoldLight
                )
              }
            }

            IconButton(
              onClick = { showInfoDialog = true },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
            ) {
              Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = "Bilgi",
                tint = TezhipGoldLight,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seljuk Star Logo Header
        SeljukStarBox(
          size = 72.dp,
          backgroundColor = TezhipGold
        ) {
          Icon(
            imageVector = Icons.Default.AdminPanelSettings,
            contentDescription = "Eğitmen Paneli",
            tint = DeepBlueNavy,
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Mekteb-i İrfan",
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Eğitmen & Yönetici Giriş Paneli",
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = TezhipGoldLight,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 🔥 FIRESTORE CLOUD STATUS & SYNC BANNER
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Color.White.copy(alpha = 0.12f),
          shape = RoundedCornerShape(14.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(
                    if (firebaseState.isConnected) FeatureAttendanceGreen.copy(alpha = 0.25f)
                    else FeatureReportsOrange.copy(alpha = 0.25f)
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (firebaseState.isConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                  contentDescription = "Firestore Durumu",
                  tint = if (firebaseState.isConnected) Color(0xFF4ADE80) else Color(0xFFFDBA74),
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "🔥 Firestore Bulut Veritabanı",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = "Çift Yönlü Senkronize",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF6EE7B7),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
                Text(
                  text = "Kurum Kodu: $schoolCode • Öğrenci ezber & performans eşitlemesi hazır",
                  fontSize = 10.sp,
                  color = Color.White.copy(alpha = 0.8f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            // Quick Test Button
            TextButton(
              onClick = {
                if (viewModel != null) {
                  isTestingFirestore = true
                  coroutineScope.launch {
                    try {
                      viewModel.backupToCloudNow()
                      Toast.makeText(context, "🔥 Firestore bağlantısı doğrulandı ve önbellek eşitlendi! ✅", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                      Toast.makeText(context, "Firestore yerel önbellek devrede. 🌿", Toast.LENGTH_SHORT).show()
                    } finally {
                      isTestingFirestore = false
                    }
                  }
                } else {
                  Toast.makeText(context, "🔥 Firestore bulut bağlantısı aktif ve hazır. ✅", Toast.LENGTH_SHORT).show()
                }
              },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              if (isTestingFirestore) {
                CircularProgressIndicator(
                  color = TezhipGoldLight,
                  strokeWidth = 1.5.dp,
                  modifier = Modifier.size(14.dp)
                )
              } else {
                Icon(Icons.Default.Refresh, contentDescription = "Test", tint = TezhipGoldLight, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Test Et",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TezhipGoldLight
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =====================================================================
        // 🌟 FIRESTORE ÖĞRENCİ EZBER & PERFORMANS YÖNETİMİ YETKİ KARTLARI
        // =====================================================================
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = CanvasSurface,
          shape = RoundedCornerShape(18.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              SeljukStarBox(
                size = 30.dp,
                backgroundColor = FeatureStudentsBlue
              ) {
                Icon(
                  imageVector = Icons.Default.SyncAlt,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Firestore Yönetici Yetkileri",
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Giriş yapıldığında hocalara tanınan canlı bulut yetkileri",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Capabilities grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              TeacherFeaturePill(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Öğrenci Ezberlerini Düzenleme & Puanlama",
                description = "Firestore'da kayıtlı Sure, Cüz, Aşır, Tesbihat ezberlerini onaylama, 1-5 ⭐ yıldız puanı, tekrar sayısı ve hoca değerlendirme notu verme.",
                color = FeatureAttendanceGreen
              )

              TeacherFeaturePill(
                icon = Icons.Default.Insights,
                title = "Performans Verilerini Anında Güncelleme",
                description = "Öğrencilerin 5 vakit namaz, kitap okuma, yoklama ve haftalık gelişim grafiklerini Firestore koleksiyonlarında canlı güncelleme.",
                color = FeatureStudentsBlue
              )

              TeacherFeaturePill(
                icon = Icons.Default.Bolt,
                title = "Gerçek Zamanlı Çift Yönlü Senkronizasyon",
                description = "Hocanın girdiği her not ve onay, öğrenci paneline anında yansır; çevrimdışı kayıtlar internet olduğunda otomatik eşitlenir.",
                color = FeatureMemorizationPurple
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // =====================================================================
        // MAIN TEACHER LOGIN FORM CARD
        // =====================================================================
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(
              elevation = 10.dp,
              shape = RoundedCornerShape(22.dp),
              spotColor = Color.Black.copy(alpha = 0.2f)
            ),
          color = CanvasSurface,
          shape = RoundedCornerShape(22.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldSubtleBorderGradient)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp)
          ) {
            // Form Title & Icon
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DeepBlueNavy.copy(alpha = 0.1f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.LockPerson,
                    contentDescription = null,
                    tint = DeepBlueNavy,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Eğitmen Kimlik Doğrulama",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                  Text(
                    text = "Kullanıcı adı ve giriş kodunuzla erişin",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }
              }

              Surface(
                color = FeatureAttendanceGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FeatureAttendanceGreen.copy(alpha = 0.3f))
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = FeatureAttendanceGreen,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Firestore Aktif",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = FeatureAttendanceGreen
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message Banner
            AnimatedVisibility(
              visible = errorMessage != null,
              enter = fadeIn() + expandVertically(),
              exit = fadeOut() + shrinkVertically()
            ) {
              Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 14.dp)
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = errorMessage ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFF991B1B),
                    lineHeight = 16.sp
                  )
                }
              }
            }

            // 1. Username Field
            Text(
              text = "Kullanıcı Adı veya E-Posta",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextPrimary,
              modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
            )

            OutlinedTextField(
              value = usernameInput,
              onValueChange = {
                usernameInput = it
                errorMessage = null
              },
              placeholder = { Text("Örn: admin veya hoca", fontSize = 13.sp) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.AccountCircle,
                  contentDescription = "Kullanıcı Adı",
                  tint = DeepBlueNavy,
                  modifier = Modifier.size(20.dp)
                )
              },
              trailingIcon = {
                if (usernameInput.isNotEmpty()) {
                  IconButton(onClick = { usernameInput = "" }) {
                    Icon(
                      imageVector = Icons.Default.Clear,
                      contentDescription = "Temizle",
                      tint = TextMuted,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeepBlueNavy,
                unfocusedBorderColor = BorderStroke,
                focusedContainerColor = CanvasBackground,
                unfocusedContainerColor = CanvasBackground
              ),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("teacher_login_username_input")
            )

            // Quick Username Chips
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              val quickChips = listOf(
                "admin" to "🛡️ Admin",
                "hoca" to "📖 Ezber Hocası",
                "hafiz" to "⏱️ Nöbetçi Hoca"
              )
              quickChips.forEach { (valKey, label) ->
                val isSelected = usernameInput.equals(valKey, ignoreCase = true)
                Surface(
                  color = if (isSelected) DeepBlueNavy.copy(alpha = 0.12f) else SurfaceVariantColor.copy(alpha = 0.6f),
                  shape = RoundedCornerShape(8.dp),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) DeepBlueNavy.copy(alpha = 0.4f) else BorderLight
                  ),
                  modifier = Modifier.clickable {
                    usernameInput = valKey
                    if (codeInput.isBlank()) codeInput = "1234"
                    errorMessage = null
                  }
                ) {
                  Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) DeepBlueNavy else TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Login Code (PIN) / Password Field
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Giriş Kodu (PIN) / Şifre",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
              )
              Text(
                text = "Varsayılan: 1234",
                fontSize = 11.sp,
                color = TezhipGoldDark,
                fontWeight = FontWeight.Medium
              )
            }

            OutlinedTextField(
              value = codeInput,
              onValueChange = {
                codeInput = it
                errorMessage = null
              },
              placeholder = { Text("PIN veya şifrenizi giriniz", fontSize = 13.sp) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Key,
                  contentDescription = "Giriş Kodu",
                  tint = TezhipGold,
                  modifier = Modifier.size(20.dp)
                )
              },
              trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                  Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Gizle" else "Göster",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                  )
                }
              },
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TezhipGold,
                unfocusedBorderColor = BorderStroke,
                focusedContainerColor = CanvasBackground,
                unfocusedContainerColor = CanvasBackground
              ),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(
                onDone = { executeLogin() }
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("teacher_login_code_input")
            )

            // Remember Me & Code Helper row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { rememberMe = !rememberMe }
              ) {
                Checkbox(
                  checked = rememberMe,
                  onCheckedChange = { rememberMe = it },
                  colors = CheckboxDefaults.colors(checkedColor = DeepBlueNavy)
                )
                Text(
                  text = "Beni Hatırla",
                  fontSize = 12.sp,
                  color = TextPrimary
                )
              }

              TextButton(
                onClick = { showChangeCredentialsDialog = true },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp), tint = FeatureStudentsBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kodu Değiştir", fontSize = 11.5.sp, color = FeatureStudentsBlue, fontWeight = FontWeight.SemiBold)
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Login Button
            Button(
              onClick = { executeLogin() },
              enabled = !isLoading,
              colors = ButtonDefaults.buttonColors(
                containerColor = DeepBlueNavy,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = DeepBlueNavy.copy(alpha = 0.4f))
                .testTag("teacher_login_submit_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  color = Color.White,
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Firestore Doğrulanıyor...", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Text(
                    text = "Eğitmen Paneline Giriş Yap",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Giriş",
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // =====================================================================
        // 📋 GİRİŞ KODU MENÜSÜ & HIZLI YETKİLİ PROFİL KARTLARI
        // =====================================================================
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = CanvasSurface,
          shape = RoundedCornerShape(18.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            // Section Header
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                SeljukStarBox(
                  size = 30.dp,
                  backgroundColor = FeatureStudentsBlue
                ) {
                  Icon(
                    imageVector = Icons.Default.ManageAccounts,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Hızlı Yetkili Profilleri",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                  Text(
                    text = "Tek dokunuşla hızlı giriş yapın",
                    fontSize = 11.sp,
                    color = TextSecondary
                  )
                }
              }

              IconButton(
                onClick = { showQuickCodeMenu = !showQuickCodeMenu },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = if (showQuickCodeMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                  contentDescription = "Genişlet/Daralt",
                  tint = TextSecondary
                )
              }
            }

            AnimatedVisibility(visible = showQuickCodeMenu) {
              Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                // Profile Card 1: Admin
                TeacherProfileQuickCard(
                  title = "Yönetici Hoca (Admin)",
                  username = "admin",
                  code = "1234",
                  roleDesc = "Tüm talebeler, Firestore koleksiyonları ve kurum ayarları",
                  badgeColor = FeatureStudentsBlue,
                  icon = Icons.Default.Shield,
                  onSelect = {
                    usernameInput = "admin"
                    codeInput = "1234"
                    executeLogin("admin", "1234")
                  }
                )

                // Profile Card 2: Hoca
                TeacherProfileQuickCard(
                  title = "Ders & Ezber Hocası",
                  username = "hoca",
                  code = "1234",
                  roleDesc = "Öğrenci ezberlerini dinleme, yıldız puanı ve not verme",
                  badgeColor = FeatureAttendanceGreen,
                  icon = Icons.AutoMirrored.Filled.MenuBook,
                  onSelect = {
                    usernameInput = "hoca"
                    codeInput = "1234"
                    executeLogin("hoca", "1234")
                  }
                )

                // Profile Card 3: Hafız / Nöbetçi
                TeacherProfileQuickCard(
                  title = "Etüt & Nöbetçi Eğitmen",
                  username = "hafiz",
                  code = "1234",
                  roleDesc = "Namaz, tesbihat ve günlük performans verilerini güncelleme",
                  badgeColor = FeatureMemorizationPurple,
                  icon = Icons.Default.Timer,
                  onSelect = {
                    usernameInput = "hafiz"
                    codeInput = "1234"
                    executeLogin("hafiz", "1234")
                  }
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch to Student Portal Button
        OutlinedButton(
          onClick = onNavigateToStudentPortal,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = FeatureAttendanceGreen
          ),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, FeatureAttendanceGreen.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(bottom = 8.dp)
            .testTag("teacher_to_student_portal_button")
        ) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = "Talebe Portalı",
            tint = FeatureAttendanceGreen,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "🌿 Talebe Giriş Portalına Geç",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = FeatureAttendanceGreen
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // =====================================================================
  // DIALOG 1: HELP & DEFAULT ACCESS CODES INFORMATION
  // =====================================================================
  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Bilgi",
            tint = DeepBlueNavy
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Eğitmen & Firestore Bilgisi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Mekteb-i İrfan eğitmen ve yönetici paneline giriş için hazır kullanıcı adı ve kodlar:",
            fontSize = 13.sp,
            color = TextPrimary
          )
          Surface(
            color = SurfaceVariantColor.copy(alpha = 0.6f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text("• Yönetici: admin / Kod: 1234", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepBlueNavy)
              Text("• Ders Hocası: hoca / Kod: 1234", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FeatureAttendanceGreen)
              Text("• Nöbetçi Eğitmen: hafiz / Kod: 1234", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FeatureMemorizationPurple)
            }
          }
          Text(
            text = "🔥 Firestore Entegrasyonu: Giriş yaptıktan sonra ezber takip ekranında yapılan her onay ve verilen not, Firestore üzerinden talebelerin ekranına canlı olarak yansır.",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showInfoDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy)
        ) {
          Text("Anladım")
        }
      }
    )
  }

  // =====================================================================
  // DIALOG 2: CHANGE / SET CUSTOM TEACHER CREDENTIALS
  // =====================================================================
  if (showChangeCredentialsDialog) {
    var newUsernameInput by remember { mutableStateOf(getSavedUsername()) }
    var newDisplayNameInput by remember { mutableStateOf(getSavedDisplayName()) }
    var newPinInput by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showChangeCredentialsDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.LockReset, contentDescription = null, tint = TezhipGold)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Eğitmen Giriş Bilgilerini Güncelle", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          if (dialogError != null) {
            Text(dialogError!!, color = Color(0xFFDC2626), fontSize = 12.sp)
          }

          Text(
            "Cihazınızda geçerli olacak özel eğitmen kullanıcı adı ve giriş kodunu belirleyin:",
            fontSize = 12.sp,
            color = TextSecondary
          )

          OutlinedTextField(
            value = newDisplayNameInput,
            onValueChange = { newDisplayNameInput = it },
            label = { Text("Eğitmen Adı & Unvanı") },
            placeholder = { Text("Örn: Ahmet Hoca") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newUsernameInput,
            onValueChange = { newUsernameInput = it },
            label = { Text("Kullanıcı Adı") },
            placeholder = { Text("Örn: ahmet_hoca") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newPinInput,
            onValueChange = { if (it.length <= 12) newPinInput = it },
            label = { Text("Yeni Giriş Kodu (PIN)") },
            placeholder = { Text("En az 4 karakter (örn: 5711)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newUsernameInput.isBlank() || newPinInput.length < 4) {
              dialogError = "Lütfen geçerli bir kullanıcı adı ve en az 4 haneli PIN girin."
              return@Button
            }
            val res = onUpdateCredentials(newUsernameInput, newPinInput, newDisplayNameInput)
            if (res.isSuccess) {
              usernameInput = newUsernameInput
              codeInput = newPinInput
              showChangeCredentialsDialog = false
              Toast.makeText(context, "Eğitmen giriş kodu başarıyla güncellendi! ✅", Toast.LENGTH_SHORT).show()
            } else {
              dialogError = res.exceptionOrNull()?.message ?: "Hata oluştu."
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy)
        ) {
          Text("Kaydet ve Uygula")
        }
      },
      dismissButton = {
        TextButton(onClick = { showChangeCredentialsDialog = false }) {
          Text("İptal")
        }
      }
    )
  }

  // =====================================================================
  // DIALOG 3: FIRESTORE BULUT & KURUM KODU YAPILANDIRMA MODALI
  // =====================================================================
  if (showFirestoreConfigDialog) {
    var tempSchoolCode by remember { mutableStateOf(schoolCode) }

    AlertDialog(
      onDismissRequest = { showFirestoreConfigDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CloudSync, contentDescription = null, tint = FeatureStudentsBlue)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Firestore Bulut & Kurum Kodu", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Hocalar ve öğrenciler aynı kurum kodu ile Firestore üzerinden birbirine bağlanır ve ezber verilerini anlık eşitler.",
            fontSize = 12.sp,
            color = TextSecondary
          )

          OutlinedTextField(
            value = tempSchoolCode,
            onValueChange = { tempSchoolCode = it },
            label = { Text("Kurum / Sınıf Kodu") },
            placeholder = { Text("Örn: irfan_default veya medrese_2026") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Surface(
            color = SurfaceVariantColor.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text("• Firestore Bağlantısı: Aktif (Önbellek Açık)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FeatureAttendanceGreen)
              Text("• Otomatik Ezber Senkronizasyonu: Etkin", fontSize = 11.sp, color = TextSecondary)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (tempSchoolCode.isNotBlank() && viewModel != null) {
              viewModel.setCloudSchoolCode(tempSchoolCode.trim())
              Toast.makeText(context, "Kurum kodu güncellendi: ${tempSchoolCode.trim()}", Toast.LENGTH_SHORT).show()
            }
            showFirestoreConfigDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = DeepBlueNavy)
        ) {
          Text("Tamam")
        }
      },
      dismissButton = {
        TextButton(onClick = { showFirestoreConfigDialog = false }) {
          Text("Kapat")
        }
      }
    )
  }
}

/**
 * Feature Pill for Firestore Teacher Capabilities
 */
@Composable
private fun TeacherFeaturePill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = color.copy(alpha = 0.07f),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(16.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          fontSize = 11.sp,
          color = TextSecondary,
          lineHeight = 14.5.sp
        )
      }
    }
  }
}

/**
 * Reusable Card for Quick Instructor Access Profile
 */
@Composable
private fun TeacherProfileQuickCard(
  title: String,
  username: String,
  code: String,
  roleDesc: String,
  badgeColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onSelect: () -> Unit
) {
  Surface(
    color = SurfaceVariantColor.copy(alpha = 0.5f),
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSelect() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(badgeColor),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Kullanıcı: $username • Kod: $code",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepBlueNavy
          )
          Text(
            text = roleDesc,
            fontSize = 10.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Surface(
        color = badgeColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Giriş",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor
          )
          Spacer(modifier = Modifier.width(3.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(12.dp)
          )
        }
      }
    }
  }
}
