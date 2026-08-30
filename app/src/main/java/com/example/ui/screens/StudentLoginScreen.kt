package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentLoginScreen(
  students: List<Student>,
  onLoginSuccess: () -> Unit,
  onQuickSelectStudent: (Student) -> Unit,
  onNavigateToTeacherPortal: () -> Unit,
  onLoginSubmit: (String, String) -> Result<*>,
  modifier: Modifier = Modifier
) {
  var usernameInput by remember { mutableStateOf("") }
  var codeInput by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showInfoDialog by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  fun performLogin() {
    focusManager.clearFocus()
    errorMessage = null

    if (usernameInput.isBlank()) {
      errorMessage = "Lütfen kullanıcı adınızı giriniz."
      return
    }
    if (codeInput.isBlank()) {
      errorMessage = "Lütfen size verilen 4 haneli giriş kodunu giriniz."
      return
    }

    val result = onLoginSubmit(usernameInput, codeInput)
    if (result.isFailure) {
      errorMessage = result.exceptionOrNull()?.message ?: "Giriş yapılamadı. Bilgilerinizi kontrol ediniz."
    } else {
      onLoginSuccess()
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
      // Decorative background gradient accent
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(260.dp)
          .background(EmeraldGradient)
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Seljuk star header with Quran book icon
        SeljukStarBox(
          size = 72.dp,
          backgroundColor = TezhipGold
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = "Mekteb-i İrfan Logo",
            tint = DarkSlateNavy,
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Mekteb-i İrfan",
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          textAlign = TextAlign.Center
        )

        Text(
          text = "Talebe Ezber ve Görev Takip Portalı",
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = LightGoldAccent,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Login Card
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(
              elevation = 12.dp,
              shape = RoundedCornerShape(24.dp),
              spotColor = Color.Black.copy(alpha = 0.15f)
            ),
          color = CanvasSurface,
          shape = RoundedCornerShape(24.dp),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldSubtleBorderGradient)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "Talebe Girişi",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Kullanıcı adınız ve kodunuzla giriş yapın",
                  fontSize = 12.5.sp,
                  color = TextSecondary
                )
              }

              IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.HelpOutline,
                  contentDescription = "Giriş Yardımı",
                  tint = EmeraldGreen
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error banner
            AnimatedVisibility(
              visible = errorMessage != null,
              enter = fadeIn() + expandVertically(),
              exit = fadeOut() + shrinkVertically()
            ) {
              errorMessage?.let { errorText ->
                Surface(
                  color = StatusRed.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(12.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.ErrorOutline,
                      contentDescription = "Hata",
                      tint = StatusRed,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = errorText,
                      fontSize = 13.sp,
                      color = StatusRed,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }
            }

            // Username input
            OutlinedTextField(
              value = usernameInput,
              onValueChange = {
                usernameInput = it
                errorMessage = null
              },
              label = { Text("Kullanıcı Adı veya İsim") },
              placeholder = { Text("Örn: ahmet, mustafa, yusuf") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = "Kullanıcı",
                  tint = EmeraldGreen
                )
              },
              trailingIcon = {
                if (usernameInput.isNotEmpty()) {
                  IconButton(onClick = { usernameInput = "" }) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Temizle",
                      tint = TextMuted
                    )
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
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
                .testTag("student_login_username_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Access Code input
            OutlinedTextField(
              value = codeInput,
              onValueChange = {
                if (it.length <= 8) {
                  codeInput = it
                  errorMessage = null
                }
              },
              label = { Text("Giriş Kodu (PIN)") },
              placeholder = { Text("Varsayılan: 1234") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Giriş Kodu",
                  tint = TezhipGold
                )
              },
              trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                  Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Gizle" else "Göster",
                    tint = TextMuted
                  )
                }
              },
              visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TezhipGold,
                unfocusedBorderColor = BorderStroke,
                focusedContainerColor = CanvasBackground,
                unfocusedContainerColor = CanvasBackground
              ),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(
                onDone = { performLogin() }
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("student_login_code_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Login Button
            Button(
              onClick = { performLogin() },
              colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = EmeraldGreen.copy(alpha = 0.4f))
                .testTag("student_login_submit_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Text(
                  text = "Giriş Yap",
                  fontSize = 16.sp,
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

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Student Selector for fast testing & instant access
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = CanvasSurface,
          shape = RoundedCornerShape(20.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              SeljukStarBox(
                size = 28.dp,
                backgroundColor = FeatureStudentsBlue
              ) {
                Icon(
                  imageVector = Icons.Default.People,
                  contentDescription = "Talebeler",
                  tint = Color.White,
                  modifier = Modifier.size(15.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Hızlı Talebe Seçimi",
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Tek dokunuşla talebe profiline bağlan",
                  fontSize = 11.5.sp,
                  color = TextSecondary
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Student list chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              students.take(6).forEach { student ->
                val avatarColor = getAvatarColor(student.avatarColorIndex)

                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                      onQuickSelectStudent(student)
                    }
                    .testTag("quick_student_item_${student.id}"),
                  color = CanvasBackground,
                  shape = RoundedCornerShape(12.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(36.dp)
                          .clip(CircleShape)
                          .background(avatarColor),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = student.fullName.take(1).uppercase(),
                          fontWeight = FontWeight.Bold,
                          color = Color.White,
                          fontSize = 15.sp
                        )
                      }
                      Spacer(modifier = Modifier.width(12.dp))
                      Column {
                        Text(
                          text = student.fullName,
                          fontSize = 13.5.sp,
                          fontWeight = FontWeight.SemiBold,
                          color = TextPrimary
                        )
                        Text(
                          text = "${student.grade} • Kullanıcı: ${if (student.username.isNotBlank()) student.username else student.fullName.lowercase().take(6)}",
                          fontSize = 11.sp,
                          color = TextSecondary
                        )
                      }
                    }

                    Surface(
                      color = EmeraldGreen.copy(alpha = 0.12f),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text(
                        text = "Seç",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Teacher Admin Portal link
        TextButton(
          onClick = onNavigateToTeacherPortal,
          modifier = Modifier.padding(bottom = 24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AdminPanelSettings,
            contentDescription = "Eğitmen Girişi",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Eğitmen & Yönetici Paneline Geç",
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }

  // Info Dialog
  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Bilgi",
            tint = EmeraldGreen
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Talebe Giriş Bilgileri", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Talebe girişi için eğitmeniniz tarafından tanımlanan kullanıcı adınızı ve 4 haneli PIN kodunuzu giriniz.",
            fontSize = 13.5.sp,
            color = TextPrimary
          )
          Divider(color = BorderStroke)
          Text(
            "• Varsayılan giriş kodu: 1234\n• Kullanıcı adı olarak isminizi (örn: 'ahmet', 'mustafa') yazabilirsiniz.\n• Dilerseniz aşağıdaki 'Hızlı Talebe Seçimi' listesinden isminize dokunarak hemen bağlanabilirsiniz.",
            fontSize = 12.5.sp,
            color = TextSecondary
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showInfoDialog = false }) {
          Text("Anladım", fontWeight = FontWeight.Bold, color = EmeraldGreen)
        }
      }
    )
  }
}
