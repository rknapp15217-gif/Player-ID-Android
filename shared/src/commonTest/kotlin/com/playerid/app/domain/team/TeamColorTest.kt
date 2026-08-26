package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamColorTest {
    @Test
    fun hueToHexUsesExistingTeamColorSaturationAndValue() {
        assertEquals("#F21818", teamHueToHex(0f))
        assertEquals("#F2F218", teamHueToHex(60f))
        assertEquals("#18F218", teamHueToHex(120f))
        assertEquals("#1885F2", teamHueToHex(210f))
        assertEquals("#F218F2", teamHueToHex(300f))
    }

    @Test
    fun hueToHexNormalizesHueOutsideOneTurn() {
        assertEquals(teamHueToHex(30f), teamHueToHex(390f))
        assertEquals(teamHueToHex(330f), teamHueToHex(-30f))
    }

    @Test
    fun hexToHueHandlesRgbColorsAndGrayscale() {
        assertEquals(0f, teamHexToHue("#FF0000"), 0.001f)
        assertEquals(120f, teamHexToHue("#00FF00"), 0.001f)
        assertEquals(240f, teamHexToHue("#0000FF"), 0.001f)
        assertEquals(0f, teamHexToHue("#FFFFFF"), 0.001f)
    }

    @Test
    fun hexToHueUsesFallbackForInvalidInput() {
        assertEquals(210f, teamHexToHue("not-a-color"))
        assertEquals(42f, teamHexToHue("#123", fallbackHue = 42f))
    }
}