package com.playerid.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorTest {
    @Test
    fun brandColorsRemainStableAcrossPlatforms() {
        assertEquals(Color(0xFF173B57), SpotrPrimaryBlue)
        assertEquals(Color(0xFFFF6B5B), SpotrHighlightOrange)
    }
}
