package com.example

import com.example.data.CreatorRewardCalculator
import com.example.ui.screens.PostAuthDecision
import com.example.ui.screens.evaluatePostGoogleAuthDecision
import com.example.ui.screens.generateSixDigitOtp
import com.example.ui.screens.validateContactContinueInputs
import com.example.ui.screens.verifyOtpInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun creatorRewardCalculator_matchesApprovedRateSchedule() {
    assertEquals(80L, CreatorRewardCalculator.calculateProposedPaiseForViews(1_000L))
    assertEquals(800L, CreatorRewardCalculator.calculateProposedPaiseForViews(10_000L))
    assertEquals(8_000L, CreatorRewardCalculator.calculateProposedPaiseForViews(100_000L))
    assertEquals(40_000L, CreatorRewardCalculator.calculateProposedPaiseForViews(500_000L))
    assertEquals(80_000L, CreatorRewardCalculator.calculateProposedPaiseForViews(1_000_000L))
    assertEquals(800_000L, CreatorRewardCalculator.calculateProposedPaiseForViews(10_000_000L))
  }

  @Test
  fun googleSignUp_whenAccountAlreadyExists_promptsLoginOptionWithoutAutoRedirect() {
    val decision = evaluatePostGoogleAuthDecision(
      authMode = "signup",
      profileAlreadyExists = true,
      email = "creator@gmail.com",
    )
    assertTrue(decision is PostAuthDecision.AccountAlreadyExistsPromptLogin)
    val existingDecision = decision as PostAuthDecision.AccountAlreadyExistsPromptLogin
    assertEquals("creator@gmail.com", existingDecision.existingEmail)
  }

  @Test
  fun googleSignUp_whenNewAccount_proceedsToHomeWithGmailAndNewFlag() {
    val decision = evaluatePostGoogleAuthDecision(
      authMode = "signup",
      profileAlreadyExists = false,
      email = "newstar@gmail.com",
    )
    assertTrue(decision is PostAuthDecision.ProceedToHome)
    val proceed = decision as PostAuthDecision.ProceedToHome
    assertEquals("newstar@gmail.com", proceed.authenticatedEmail)
    assertEquals(true, proceed.isNewSignup)
  }

  @Test
  fun googleLogin_whenExistingAccount_proceedsToHomeWithoutDuplicateCreationPrompt() {
    val decision = evaluatePostGoogleAuthDecision(
      authMode = "login",
      profileAlreadyExists = true,
      email = "existing@gmail.com",
    )
    assertTrue(decision is PostAuthDecision.ProceedToHome)
    val proceed = decision as PostAuthDecision.ProceedToHome
    assertEquals("existing@gmail.com", proceed.authenticatedEmail)
    assertEquals(false, proceed.isNewSignup)
  }

  @Test
  fun validateContactAndOtpVerification_worksCorrectly() {
    val emptyResult = validateContactContinueInputs("", "", "login")
    assertTrue(emptyResult.isFailure)

    val badEmail = validateContactContinueInputs("", "invalid-email", "login")
    assertTrue(badEmail.isFailure)

    val validResult = validateContactContinueInputs("+91 9876543210", "star@gmail.com", "signup")
    assertTrue(validResult.isSuccess)

    val otp = generateSixDigitOtp()
    assertEquals(6, otp.length)
    assertTrue(otp.all { it.isDigit() })

    assertTrue(verifyOtpInput("123", otp).isFailure)
    assertTrue(verifyOtpInput("000000", "999999").isFailure)
    assertTrue(verifyOtpInput(otp, otp).isSuccess)
  }
}
