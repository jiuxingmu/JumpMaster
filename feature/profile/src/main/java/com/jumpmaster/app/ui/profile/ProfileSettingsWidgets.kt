package com.jumpmaster.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        SettingsItem(
            icon = Icons.Default.WorkspacePremium,
            label = "会员资格",
            iconTint = Color(0xFFF59E0B),
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        SettingsItem(
            icon = Icons.Default.Share,
            label = "分享使用体验送会员",
            iconTint = Color(0xFF10B981),
            badge = "新",
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        SettingsItem(
            icon = Icons.Default.Info,
            label = "入门指南",
            iconTint = Color(0xFF3B82F6),
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        SettingsItem(
            icon = Icons.Default.FitnessCenter,
            label = "授权访问健康数据",
            iconTint = Color(0xFFEC4899),
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        SettingsItem(
            icon = Icons.Default.Favorite,
            label = "心率区间",
            iconTint = Color(0xFFEF4444),
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        SettingsItem(
            icon = Icons.Default.Redeem,
            label = "兑换码",
            iconTint = Color(0xFF8B5CF6),
        )
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        SettingsItem(
            icon = Icons.Default.Settings,
            label = "设置",
            iconTint = Color(0xFF6B7280),
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = Color.Unspecified,
    badge: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            badge?.let {
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFFEF4444),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
