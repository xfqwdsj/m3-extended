package top.ltfan.m3x.compose.material3

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

public val CardColorsLowest: CardColors
    @Composable
    inline get() =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)

public val ListItemColorsTransparent: ListItemColors
    @Composable
    inline get() =
        ListItemDefaults.colors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            selectedContainerColor = Color.Transparent,
            draggedContainerColor = Color.Transparent,
        )
