package com.playerid.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ExampleUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTextDisplayed() {
        composeTestRule.setContent {
            // Replace with your composable
            androidx.compose.material3.Text("Hello World")
        }
        composeTestRule.onNodeWithText("Hello World").assertExists()
    }
}
