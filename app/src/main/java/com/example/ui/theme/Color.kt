package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// 🕌 GELENEKSEL TEZHİP & GEOMETRİK SELÇUKLU ZARAFETİ (Manevi & Klasik Tasarım)
// Tasarım Felsefesi: Selçuklu firuze çinileri, altın varak tezhip işçiliği,
// aharli parşömen zeminleri ve vakur isli mürekkep tonları.
// =========================================================================

// Aharli Parşömen & Fildişi Zeminler
val CanvasBackground = Color(0xFFFAF7F0)     // Sıcak aharli parşömen & fildişi zemin
val CanvasSurface = Color(0xFFFFFFFF)        // Saf ve aydınlık zemin
val GlassSurface = Color(0xF2FFFFFF)         // %95 opak fildişi mat cam
val GlassSurfaceSubtle = Color(0xCCFDFBF7)   // %80 opak hafif parşömen cam
val SurfaceVariantColor = Color(0xFFF3EDE2)  // Sıcak tezhip gömme panel zemini
val BorderLight = Color(0xFFE5DAC7)          // İnce altın varak / parşömen kenarlık

// Selçuklu & Tezhip Gradyanları
val GradientIndigoPink = listOf(Color(0xFF0F2C59), Color(0xFF1E3A8A), Color(0xFF0E7490)) // Lapis Lazuli & Firuze
val GradientCyanBlue = listOf(Color(0xFF0F766E), Color(0xFF0E7490))                       // Selçuklu Çinisi & Turkuaz
val GradientEmeraldLime = listOf(Color(0xFF166534), Color(0xFF15803D))                     // Zümrüt & Manevi Yeşil
val GradientSunsetRose = listOf(Color(0xFFB45309), Color(0xFFD4AF37))                      // Tezhip Altını & Varak
val GradientPurpleViolet = listOf(Color(0xFF581C87), Color(0xFF7E22CE))                    // Mürdüm & Hat Sanatı Tonu
val GradientTealAzure = listOf(Color(0xFF0D9488), Color(0xFF0284C7))                       // Firuze Çini Mavisi

// Vurgu & Maneviyat Renkleri
val ElectricEmerald = Color(0xFF166534)      // Zümrüt Yeşili (İslam Yeşili)
val ElectricBlue = Color(0xFF0E7490)         // Selçuklu Firuzesi / Turkuaz
val ElectricCyan = Color(0xFF0D9488)         // Çini Mavisi / Akik
val ElectricViolet = Color(0xFF0F2C59)       // Lapis Lazuli / Derin Dergâh Mavisi
val ElectricRose = Color(0xFF991B1B)         // Yakut Kırmızısı / Kök Lal
val ElectricAmber = Color(0xFFC59B27)        // Hakiki Tezhip Varak Altını
val ElectricPink = Color(0xFF78350F)         // Hat Mürekkebi / Amber

// Modül Vurguları (Özellik Renkleri)
val FeatureStudentsBlue = Color(0xFF0F2C59)        // Talebeler: Lapis Lazuli Mavisi
val FeatureAttendanceGreen = Color(0xFF166534)     // Yoklama: Zümrüt Yeşili
val FeatureMemorizationPurple = Color(0xFFC59B27)  // Ezber: Tezhip Varak Altını
val FeatureDevelopmentTeal = Color(0xFF0E7490)     // Gelişim: Selçuklu Firuzesi
val FeatureReportsOrange = Color(0xFF991B1B)       // Raporlar: Yakut / Lal

// Pastel & Parşömen Arka Plan Değişkenleri
val PastelFjordBg = Color(0xFFF0F6F9)
val PastelFjordBorder = Color(0xFFC8DBE5)
val PastelSageBg = Color(0xFFF0F7F2)
val PastelSageBorder = Color(0xFFC9DEC0)
val PastelLilacBg = Color(0xFFF9F5EC)
val PastelLilacBorder = Color(0xFFE9DCBF)
val PastelTealBg = Color(0xFFEEF7F6)
val PastelTealBorder = Color(0xFFC4E4E0)
val PastelTerracottaBg = Color(0xFFFDF2F2)
val PastelTerracottaBorder = Color(0xFFF5CDCD)

// Geriye Dönük Uyumluluk Tokenları
val OttomanLapis = FeatureStudentsBlue
val OttomanLapisBg = PastelFjordBg
val OttomanLapisBorder = PastelFjordBorder
val OttomanEmerald = FeatureAttendanceGreen
val OttomanEmeraldBg = PastelSageBg
val OttomanEmeraldBorder = PastelSageBorder
val OttomanRuby = FeatureReportsOrange
val OttomanRubyBg = PastelTerracottaBg
val OttomanRubyBorder = PastelTerracottaBorder
val OttomanTurquoise = FeatureDevelopmentTeal
val OttomanTurquoiseBg = PastelTealBg
val OttomanTurquoiseBorder = PastelTealBorder
val OttomanAmber = ElectricAmber
val OttomanAmberBg = Color(0xFFFEF9EE)
val OttomanAmberBorder = Color(0xFFF6E4BA)
val OttomanPlum = Color(0xFF581C87)
val OttomanPlumBg = PastelLilacBg
val OttomanPlumBorder = PastelLilacBorder

// Tezhip Varak & Altın Tonları
val TezhipGold = Color(0xFFC59B27)
val TezhipGoldLight = Color(0xFFFEF3C7)
val TezhipGoldDark = Color(0xFF92400E)
val TezhipGoldBorder = Color(0xFFE0CE9F)

// Yapısal & Marka Renkleri
val DeepBlueNavy = Color(0xFF0F2C59)         // Lapis Dergâh Laciverti
val DeepBlueDark = Color(0xFF0B1728)         // Gece / Koyu Klasik Zemin (Dark mode)
val DeepBlueLight = Color(0xFF0E7490)        // Selçuklu Turkuazı

// Puanlama & Yıldız
val GoldStar = Color(0xFFD4AF37)             // Tezhip Altın Yıldız
val GoldStarLight = Color(0xFFFEF3C7)
val GoldStarDark = Color(0xFF92400E)

// Yoklama & Durum Renkleri (Geleneksel Tonlar)
val StatusPresentGreen = Color(0xFF166534)   // Geldi: Zümrüt Yeşili
val StatusAbsentRed = Color(0xFF991B1B)      // Gelmedi: Yakut Kırmızısı
val StatusExcusedAmber = Color(0xFFC59B27)   // İzinli: Varak Altını
val StatusLateBlue = Color(0xFF0E7490)       // Geç: Firuze Çinisi

// Tipografi & Kontrast Tonları
val TextPrimary = Color(0xFF1C1814)          // Sıcak İsli Mürekkep Siyahı (Yüksek Okunabilirlik)
val TextSecondary = Color(0xFF574D43)        // Klasik Hat Mürekkebi / Kurşuni
val TextMuted = Color(0xFF8C7E72)            // Yumuşak Parşömen Grisi

// Yumuşak Geleneksel Gölgeler
val NeumorphLight = Color(0xFFFFFFFF)
val NeumorphDarkShadow = Color(0x141C1814)
val NeumorphInnerShadow = Color(0x0A1C1814)

// Convenience aliases for student portal
val EmeraldGreen = FeatureAttendanceGreen
val DarkSlateNavy = DeepBlueNavy
val LightGoldAccent = TezhipGoldLight
val StatusRed = StatusAbsentRed
val StatusBlue = StatusLateBlue
val BorderStroke = BorderLight
val EmeraldGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
  listOf(Color(0xFF166534), Color(0xFF14532D), Color(0xFF052E16))
)

fun getAvatarColor(index: Int): Color {
  val colors = listOf(
    Color(0xFF0F2C59),
    Color(0xFF166534),
    Color(0xFF0E7490),
    Color(0xFFC59B27),
    Color(0xFF991B1B),
    Color(0xFF581C87),
    Color(0xFF0D9488),
    Color(0xFFD97706)
  )
  return colors[index.coerceIn(0, colors.lastIndex)]
}


