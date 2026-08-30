package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppLogoEmblem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onBackClick: () -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Uygulama Hakkında",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Mekteb-i İrfan & Sürüm Bilgileri",
              fontSize = 11.5.sp,
              color = Color(0xFF93C5FD)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("about_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Geri",
              tint = Color.White
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = DeepBlueNavy
        )
      )
    },
    containerColor = CanvasBackground
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // -------------------------------------------------------------
      // 1. HERO BRANDING & VERSION CARD
      // -------------------------------------------------------------
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Mekteb-i İrfan",
              fontSize = 22.sp,
              fontWeight = FontWeight.ExtraBold,
              color = TextPrimary,
              textAlign = TextAlign.Center
            )

            Text(
              text = "Ezber Çetelesi & Talebe Takip Sistemi",
              fontSize = 13.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = FeatureStudentsBlue,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Version Badge
            Surface(
              color = FeatureStudentsBlue.copy(alpha = 0.10f),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, FeatureStudentsBlue.copy(alpha = 0.25f))
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = null,
                  tint = FeatureStudentsBlue,
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Sürüm v1.0.0 (Kararlı Sürüm)",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = FeatureStudentsBlue
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Kur'an-ı Kerim sureleri, namaz tesbihatları ve Risale-i Nur ezberlerinin hocalar ve talebeler tarafından kolay, nizamlı ve şeffaf şekilde takip edilmesi amacıyla hazırlanmıştır.",
              fontSize = 13.sp,
              color = TextSecondary,
              textAlign = TextAlign.Center,
              lineHeight = 18.sp
            )
          }
        }
      }

      // -------------------------------------------------------------
      // 2. TEMEL ÖZELLİKLER & MODÜLLER
      // -------------------------------------------------------------
      item {
        Text(
          text = "🌟 Öne Çıkan Özellikler",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
          border = BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FeatureHighlightRow(
              icon = Icons.AutoMirrored.Filled.MenuBook,
              color = FeatureMemorizationPurple,
              title = "108 Maddelik Zengin Müfredat",
              description = "30. Cüz Amme cüzü, Yasin, Mülk, Nebe, Fetih, Rahman, Namaz Tesbihatı, Cevşen ve Risale bölümleri."
            )

            HorizontalDivider(color = BorderLight)

            FeatureHighlightRow(
              icon = Icons.Default.FactCheck,
              color = FeatureAttendanceGreen,
              title = "Günlük Hızlı Yoklama",
              description = "Geldi, Gelmedi, İzinli ve Geç durumlarını tek dokunuşla kaydetme ve aylık devam istatistikleri."
            )

            HorizontalDivider(color = BorderLight)

            FeatureHighlightRow(
              icon = Icons.Default.TrendingUp,
              color = FeatureStudentsBlue,
              title = "Gelişim & Tahmini Bitiş Analizi",
              description = "Ezber hızına göre hedeflenen müfredatın tahmini tamamlanma tarihini otomatik hesaplar."
            )

            HorizontalDivider(color = BorderLight)

            FeatureHighlightRow(
              icon = Icons.Default.Assessment,
              color = FeatureReportsOrange,
              title = "Karne & Resmi PDF Raporlama",
              description = "Velilere sunulmak üzere talebe karnesi ve sınıf genel durum listesini PDF olarak yazdırma/paylaşma."
            )

            HorizontalDivider(color = BorderLight)

            FeatureHighlightRow(
              icon = Icons.Default.Backup,
              color = DeepBlueLight,
              title = "Yedekleme & Cihazlar Arası Aktarım",
              description = "Tüm verileri JSON olarak dışa aktarıp farklı bir cihaza tek tıkla yükleme imkanı."
            )
          }
        }
      }

      // -------------------------------------------------------------
      // 3. TEKNİK BİLGİLER VE GÜVENLİK
      // -------------------------------------------------------------
      item {
        Text(
          text = "⚙️ Teknik Bilgiler & Güvenlik",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = CanvasSurface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
          border = BorderStroke(1.dp, BorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TechDetailRow(label = "Uygulama Adı", value = "Mekteb-i İrfan Ezber Çetelesi")
            TechDetailRow(label = "Sürüm Numarası", value = "1.0.0 (Release Build 2026)")
            TechDetailRow(label = "Geliştirme Altyapısı", value = "Kotlin & Jetpack Compose (M3)")
            TechDetailRow(label = "Veritabanı", value = "Android Room SQLite (Yerel)")
            TechDetailRow(label = "İnternet İhtiyacı", value = "Çevrimdışı (Offline-First, %100 Güvenli)")
            TechDetailRow(label = "Hedef Kitle", value = "Kur'an Kursları, Medreseler, Talebeler ve Veliler")
          }
        }
      }

      // -------------------------------------------------------------
      // 4. HİKMETLİ SÖZ & TELİF BİLGİSİ
      // -------------------------------------------------------------
      item {
        Surface(
          color = Color(0xFFEFF6FF),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "“Sizin en hayırlınız, Kur'an'ı öğrenen ve öğreteninizdir.”",
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              color = DeepBlueNavy,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "(Hadis-i Şerif — Buhârî, Fezâilü'l-Kur'ân, 21)",
              fontSize = 11.5.sp,
              color = Color(0xFF1E40AF),
              textAlign = TextAlign.Center
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "© 2026 Mekteb-i İrfan Talebe Takip Sistemi • Tüm hakları mahfuzdur.",
          fontSize = 11.sp,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  color: Color,
  title: String,
  description: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(color.copy(alpha = 0.12f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(20.dp)
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        fontSize = 12.sp,
        color = TextSecondary,
        lineHeight = 16.sp
      )
    }
  }
}

@Composable
private fun TechDetailRow(
  label: String,
  value: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      color = TextSecondary,
      fontWeight = FontWeight.Medium
    )
    Text(
      text = value,
      fontSize = 12.sp,
      color = TextPrimary,
      fontWeight = FontWeight.SemiBold
    )
  }
}
