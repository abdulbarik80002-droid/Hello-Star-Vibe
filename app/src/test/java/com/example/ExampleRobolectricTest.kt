package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xhdpi")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun authScreen3DLogo_otpVerificationAndLoginFlow_andGoogleButton() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Hello Star Vibe", context.getString(R.string.app_name))

    var authenticatedIdentity = ""
    var loginCompletedCount = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          onAuthSuccess = { identity, _ ->
            authenticatedIdentity = identity
            loginCompletedCount += 1
          },
          introDelayMillis = 0L,
        )
      }
    }

    // 1) Verify 3D Logo and "HELLO STAR VIBE" text are displayed first
    composeTestRule.onNodeWithTag("animated_3d_star_logo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hello_star_vibe_3d_text").assertIsDisplayed()
    composeTestRule.onNodeWithText("HELLO STAR VIBE").assertIsDisplayed()

    // 2) Verify two clear options: Login and Sign Up
    composeTestRule.onNodeWithTag("auth_mode_login_chip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_mode_signup_chip").assertIsDisplayed()

    // 3) Verify input fields for Mobile Number and Email Address, plus Continue (Send OTP) button
    composeTestRule.onNodeWithTag("mobile_number_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("email_address_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("continue_button").assertIsDisplayed()

    // 4) Verify clearly visible "Continue with Google" button and no unexpected auto-redirect
    composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()
    assertEquals(0, loginCompletedCount)

    // 5) Enter Mobile Number & Email Address and tap Continue to open the OTP input section
    composeTestRule.onNodeWithTag("mobile_number_input").performTextInput("9876543210")
    composeTestRule.onNodeWithTag("email_address_input").performTextInput("abdulbarik80000@gmail.com")
    composeTestRule.onNodeWithTag("continue_button").performScrollTo().performClick()

    // Verify OTP verification card and OTP input box appear
    composeTestRule.onNodeWithTag("otp_verification_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("otp_code_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("verify_otp_button").performScrollTo().assertIsDisplayed()

    // Tap Auto-Fill OTP and then Verify OTP & Login (with rapid double-tap check)
    composeTestRule.onNodeWithTag("autofill_otp_button").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("verify_otp_button").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("verify_otp_button").performClick()

    assertEquals(1, loginCompletedCount)
    assertEquals("abdulbarik80000@gmail.com", authenticatedIdentity)
  }
}
