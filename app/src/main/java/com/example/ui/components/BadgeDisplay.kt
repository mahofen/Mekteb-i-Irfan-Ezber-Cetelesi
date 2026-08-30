package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentBadge
import com.example.ui.theme.*

@Composable
fun KarneBadgesShowcaseSection(
  badges: List<StudentBadge>,
  modifier: Modifier = Modifier
) {
  val earnedBadges = badges.filter { it.isEarned }
  val lockedBadges = badges.filter { !it.isEarned }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFFFAF8F3), // Warm parchment
    border = androidx.compose.foundation.BorderStroke(1.dp, GoldStar.copy(alpha = 0.5f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(GoldStar.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = null,
              tint = Color(0xFFD97706),
              modifier = Modifier.size(13.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "🏅 KAZANILAN ROZETLER",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = DeepBlueNavy,
            letterSpacing = 0.3.sp
          )
        }

        Surface(
          color = DeepBlueNavy,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "${earnedBadges.size} / ${badges.size} Rozet",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFDE68A),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      if (earnedBadges.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
          contentAlignment = Alignment.CenterStart
        ) {
          Text(
            text = "🌱 Henüz kazanılmış rozet bulunmuyor. Ezberler tamamlandıkça burada listelenecektir.",
            fontSize = 10.5.sp,
            color = TextSecondary
          )
        }
      } else {
        // Compact 2-column grid layout for earned badges to fit comfortably on screen
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          earnedBadges.chunked(2).forEach { rowBadges ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              rowBadges.forEach { badge ->
                val badgeColor = Color(badge.colorHex)
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = CanvasSurface,
                  border = androidx.compose.foundation.BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.35f)),
                  shadowElevation = 0.5.dp,
                  modifier = Modifier.weight(1f)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(text = badge.emoji, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = badge.title,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                      )
                      Text(
                        text = badge.description,
                        fontSize = 8.5.sp,
                        color = TextSecondary,
                        maxLines = 1
                      )
                    }
                  }
                }
              }
              if (rowBadges.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }

      // Next Milestone if there are locked badges (compact single line)
      if (lockedBadges.isNotEmpty()) {
        val nextBadge = lockedBadges.maxByOrNull { it.progressPercent } ?: lockedBadges.first()
        Surface(
          color = Color(0xFFF1F5F9),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Sıradaki: ${nextBadge.title} (%${nextBadge.progressPercent})",
              fontSize = 9.sp,
              color = TextSecondary,
              fontWeight = FontWeight.Medium,
              maxLines = 1
            )
          }
        }
      }
    }
  }
}

@Composable
fun StudentBadgesSection(
  badges: List<StudentBadge>,
  modifier: Modifier = Modifier,
  onBadgeClick: (StudentBadge) -> Unit = {}
) {
  val earnedBadges = badges.filter { it.isEarned }
  val lockedBadges = badges.filter { !it.isEarned }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = null,
          tint = GoldStar,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "🏆 Başarı Rozetleri",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
      }
      Text(
        text = "${earnedBadges.size}/${badges.size} Kazanıldı",
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = FeatureStudentsBlue
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(badges) { badge ->
        BadgeItemChip(
          badge = badge,
          onClick = { onBadgeClick(badge) }
        )
      }
    }
  }
}

@Composable
fun BadgeItemChip(
  badge: StudentBadge,
  onClick: () -> Unit = {}
) {
  val badgeColor = Color(badge.colorHex)
  val isEarned = badge.isEarned

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    color = if (isEarned) badgeColor.copy(alpha = 0.12f) else CanvasBackground,
    border = androidx.compose.foundation.BorderStroke(
      width = if (isEarned) 1.dp else 0.8.dp,
      color = if (isEarned) badgeColor else BorderLight
    ),
    shadowElevation = if (isEarned) 0.5.dp else 0.dp,
    modifier = Modifier
      .width(66.dp)
      .testTag("badge_item_${badge.id}")
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .background(if (isEarned) badgeColor.copy(alpha = 0.2f) else Color(0x15000000)),
        contentAlignment = Alignment.Center
      ) {
        if (isEarned) {
          Text(text = badge.emoji, fontSize = 11.sp)
        } else {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Kilitli",
            tint = TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = badge.title,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = if (isEarned) TextPrimary else TextSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1
      )

      if (isEarned) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Verified, contentDescription = null, tint = badgeColor, modifier = Modifier.size(7.dp))
          Spacer(modifier = Modifier.width(1.dp))
          Text("Aldı", fontSize = 7.sp, color = badgeColor, fontWeight = FontWeight.Bold)
        }
      } else {
        Text(
          text = "%${badge.progressPercent}",
          fontSize = 7.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
