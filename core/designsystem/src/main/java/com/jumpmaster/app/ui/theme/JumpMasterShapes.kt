package com.jumpmaster.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 卡片与主按钮统一大圆角（≥24dp） */
val JumpMasterShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

val JumpMasterCardShape = RoundedCornerShape(28.dp)
val JumpMasterButtonShape = RoundedCornerShape(26.dp)
