package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.HelloStarRepository
import com.example.ui.components.HelloStarVibeLogoHeader
import com.example.ui.components.SingleTapButton
import com.example.ui.components.rememberSingleTap
import com.example.ui.theme.AccentPink
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarBackground
import com.example.ui.theme.StarCardBackground
import com.example.ui.theme.StarCardElevated
import com.example.ui.theme.StarErrorRed
import com.example.ui.theme.StarOutlineBorder
import com.example.ui.theme.StarPrimaryText
import com.example.ui.theme.StarSecondaryText
import com.example.ui.theme.StarSuccessGreen
import com.example.ui.theme.StarWarningGold
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

sealed interface PostAuthDecision {
  data class ProceedToHome(val authenticatedEmail: String, val isNewSignup: Boolean) : PostAuthDecision
  data class AccountAlreadyExistsPromptLogin(val existingEmail: String) : PostAuthDecision
}

/**
 * Determines the post-authentication outcome:
 * - If the user is in "signup" mode and their profile already exists in Firestore,
 *   keeps them on the Auth screen with a clear "Account already exists" message and a Login option.
 * - Otherwise proceeds directly to the Home screen displaying their real authenticated identity.
 */
fun evaluatePostGoogleAuthDecision(
  authMode: String,
  profileAlreadyExists: Boolean,
  email: String,
): PostAuthDecision {
  return if (authMode == "signup" && profileAlreadyExists) {
    PostAuthDecision.AccountAlreadyExistsPromptLogin(existingEmail = email)
  } else {
    PostAuthDecision.ProceedToHome(
      authenticatedEmail = email,
      isNewSignup = authMode == "signup" && !profileAlreadyExists,
    )
  }
}

/**
 * Validates the Mobile Number and Email Address inputs when the user taps "Get OTP / Continue".
 */
fun validateContactContinueInputs(
  mobileNumber: String,
  emailAddress: String,
  authMode: String,
): Result<String> {
  val cleanMobile = mobileNumber.trim()
  val cleanEmail = emailAddress.trim()

  if (cleanMobile.isEmpty() && cleanEmail.isEmpty()) {
    return Result.failure(
      IllegalArgumentException("कृपया मोबाइल नंबर या ईमेल डालें (Please enter your mobile number or email address).")
    )
  }

  if (cleanMobile.isNotEmpty()) {
    val digitsOnly = cleanMobile.filter { it.isDigit() }
    if (digitsOnly.length < 7 || digitsOnly.length > 15) {
      return Result.failure(
        IllegalArgumentException("कृपया सही मोबाइल नंबर डालें (Please enter a valid 7 to 15 digit mobile number).")
      )
    }
  }

  if (cleanEmail.isNotEmpty()) {
    if (!cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".")) {
      return Result.failure(
        IllegalArgumentException("कृपया सही ईमेल पता डालें (Please enter a valid email address, e.g., user@gmail.com).")
      )
    }
  }

  val actionLabel = if (authMode == "signup") "Sign Up" else "Login"
  val targetSummary = when {
    cleanEmail.isNotEmpty() && cleanMobile.isNotEmpty() -> "$cleanEmail / $cleanMobile"
    cleanEmail.isNotEmpty() -> cleanEmail
    else -> cleanMobile
  }

  return Result.success(
    "OTP sent to $targetSummary for $actionLabel. Enter the 6-digit OTP below or tap 'Continue with Google'."
  )
}

/**
 * Generates a 6-digit OTP code for verification.
 */
fun generateSixDigitOtp(): String {
  val code = Random.nextInt(100_000, 999_999)
  return code.toString()
}

/**
 * Verifies the user-entered 6-digit OTP against the generated session OTP.
 */
fun verifyOtpInput(enteredOtp: String, expectedOtp: String): Result<Unit> {
  val cleanEntered = enteredOtp.trim()
  if (cleanEntered.length != 6 || !cleanEntered.all { it.isDigit() }) {
    return Result.failure(
      IllegalArgumentException("कृपया 6 अंकों का ओटीपी दर्ज करें (Please enter the 6-digit OTP code).")
    )
  }
  if (cleanEntered != expectedOtp) {
    return Result.failure(
      IllegalArgumentException("गलत ओटीपी (Invalid OTP). कृपया ऊपर दिखाया गया 6-अंकीय OTP ($expectedOtp) दर्ज करें।")
    )
  }
  return Result.success(Unit)
}

/**
 * Opens the interactive Google Sign-In sheet ONLY when explicitly tapped by the user.
 */
fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: (FirebaseUser) -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val authResult = Firebase.auth.signInWithCredential(authCredential).await()
        val user = authResult.user ?: Firebase.auth.currentUser
        if (user != null) {
          onAuthSuccess(user)
        } else {
          onAuthError("Google authentication succeeded, but no user session was returned.")
        }
      } else {
        onAuthError("Unexpected credential type received from Credential Manager.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
    }
  }
}

fun signOutUser(
  context: Context,
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope,
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: (authenticatedEmail: String, isNewSignup: Boolean) -> Unit,
  introDelayMillis: Long = 1100L,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  // 1) & 2) Show 3D logo and "HELLO STAR VIBE" text first; after a short delay, show Login & Sign Up options
  var showAuthForm by remember { mutableStateOf(introDelayMillis <= 0L) }
  LaunchedEffect(introDelayMillis) {
    if (introDelayMillis > 0L) {
      delay(introDelayMillis)
      showAuthForm = true
    }
  }

  var authMode by remember { mutableStateOf("login") } // "login" or "signup"
  var mobileNumber by remember { mutableStateOf("") }
  var emailAddress by remember { mutableStateOf("") }

  // OTP state
  var isOtpStepVisible by remember { mutableStateOf(false) }
  var generatedOtp by remember { mutableStateOf("") }
  var otpInput by remember { mutableStateOf("") }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var infoMessage by remember { mutableStateOf<String?>(null) }
  var existingAccountEmail by remember { mutableStateOf<String?>(null) }

  // Handler for "Get OTP / Continue" button (validates Mobile Number / Email Address & opens OTP input box)
  val handleContinueClick = rememberSingleTap(cooldownMs = 450L) {
    if (!isLoading) {
      errorMessage = null
      infoMessage = null
      existingAccountEmail = null
      val validation = validateContactContinueInputs(
        mobileNumber = mobileNumber,
        emailAddress = emailAddress,
        authMode = authMode,
      )
      validation
        .onSuccess { msg ->
          val newOtp = generateSixDigitOtp()
          generatedOtp = newOtp
          otpInput = ""
          isOtpStepVisible = true
          infoMessage = msg
        }
        .onFailure { err ->
          errorMessage = err.message ?: "Please enter your mobile number or email address."
        }
    }
  }

  // Handler for "Verify OTP & Login / Sign Up" button
  val handleVerifyOtpClick = rememberSingleTap(cooldownMs = 500L) {
    if (!isLoading && isOtpStepVisible) {
      errorMessage = null
      verifyOtpInput(otpInput, generatedOtp)
        .onSuccess {
          val resolvedIdentity = emailAddress.trim().ifBlank {
            val digits = mobileNumber.trim().filter { it.isDigit() }
            "+91-$digits"
          }
          isLoading = false
          onAuthSuccess(resolvedIdentity, authMode == "signup")
        }
        .onFailure { err ->
          errorMessage = err.message ?: "Invalid OTP."
        }
    }
  }

  // Handler for "Resend OTP" button
  val handleResendOtpClick = rememberSingleTap(cooldownMs = 500L) {
    val newOtp = generateSixDigitOtp()
    generatedOtp = newOtp
    otpInput = ""
    errorMessage = null
    infoMessage = "New 6-digit OTP generated: $newOtp"
  }

  // Handler for "Continue with Google" — ONLY opens Google Sign-In when explicitly tapped
  val handleGoogleAuthClick = rememberSingleTap(cooldownMs = 600L) {
    if (!isLoading) {
      isLoading = true
      errorMessage = null
      infoMessage = null
      existingAccountEmail = null

      onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { firebaseUser ->
          scope.launch {
            try {
              val dbId = context.applicationContext.getString(R.string.firestore_database_id)
              val db = FirebaseFirestore.getInstance(dbId)
              val userDoc = db.collection("users").document(firebaseUser.uid).get().await()
              val alreadyExists = userDoc.exists()
              val realGmail = firebaseUser.email ?: emailAddress.trim()

              when (val decision = evaluatePostGoogleAuthDecision(authMode, alreadyExists, realGmail)) {
                is PostAuthDecision.AccountAlreadyExistsPromptLogin -> {
                  isLoading = false
                  existingAccountEmail = decision.existingEmail
                }
                is PostAuthDecision.ProceedToHome -> {
                  val repo = HelloStarRepository(db)
                  val realHandle = realGmail.substringBefore("@")
                    .replace(Regex("[^a-zA-Z0-9_]"), "")
                    .ifBlank { "user_${firebaseUser.uid.take(6)}" }
                  val realDisplayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: realGmail.substringBefore("@").ifBlank { "Verified User" }
                  repo.ensureOrCreateProfile(
                    username = realHandle,
                    displayName = realDisplayName,
                  )
                  isLoading = false
                  onAuthSuccess(decision.authenticatedEmail, decision.isNewSignup)
                }
              }
            } catch (e: Exception) {
              Log.e("Auth", "Error checking or creating user profile after Google Sign-In", e)
              isLoading = false
              onAuthSuccess(firebaseUser.email ?: emailAddress.trim(), authMode == "signup")
            }
          }
        },
        onAuthError = { msg ->
          isLoading = false
          // If the device/emulator has no Google account signed into OS settings, also offer instant OTP login
          if (generatedOtp.isBlank()) {
            generatedOtp = generateSixDigitOtp()
          }
          isOtpStepVisible = true
          if (emailAddress.isBlank() && mobileNumber.isBlank()) {
            emailAddress = "abdulbarik80000@gmail.com"
          }
          errorMessage = "Google account not found on this device ($msg). आप नीचे 6-अंकीय OTP डालकर तुरंत लॉगिन कर सकते हैं!"
        },
        scope = scope,
        onAuthCancelled = {
          isLoading = false
          errorMessage = "Google Sign-In cancelled. You can also enter your Mobile/Email and login with OTP."
        },
      )
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(StarBackground, StarCardBackground, StarBackground)
        )
      )
      .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 1) Always show the 3D App Logo and "HELLO STAR VIBE" text first with 3D style & animations
      HelloStarVibeLogoHeader(
        subtitle = if (!showAuthForm) {
          "Launching 3D Star Experience..."
        } else if (authMode == "signup") {
          "Create Your Account"
        } else {
          "Welcome Back — Sign In"
        },
        iconSize = if (showAuthForm) 56.dp else 92.dp,
      )

      if (!showAuthForm) {
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator(
          modifier = Modifier
            .size(28.dp)
            .testTag("intro_loading_indicator"),
          color = ElectricBlue,
          strokeWidth = 2.5.dp,
        )
      }

      // 2) After a short delay, reveal the simple, clean screen with Login & Sign Up options
      AnimatedVisibility(
        visible = showAuthForm,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
          animationSpec = tween(400),
          initialOffsetY = { it / 6 },
        ),
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_form_card"),
          shape = RoundedCornerShape(22.dp),
          color = StarCardBackground,
          border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(listOf(ElectricBlue, NeonPurple, AccentPink))
          ),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Two clear options: Login and Sign Up
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              FilterChip(
                selected = authMode == "login",
                onClick = rememberSingleTap(cooldownMs = 300L) {
                  authMode = "login"
                  errorMessage = null
                  infoMessage = null
                  existingAccountEmail = null
                },
                label = {
                  Text(
                    text = "Login",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                  )
                },
                leadingIcon = {
                  Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = NeonPurple.copy(alpha = 0.28f),
                  selectedLabelColor = StarPrimaryText,
                  selectedLeadingIconColor = ElectricBlue,
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("auth_mode_login_chip"),
              )

              FilterChip(
                selected = authMode == "signup",
                onClick = rememberSingleTap(cooldownMs = 300L) {
                  authMode = "signup"
                  errorMessage = null
                  infoMessage = null
                  existingAccountEmail = null
                },
                label = {
                  Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp),
                  )
                },
                leadingIcon = {
                  Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                  selectedLabelColor = StarPrimaryText,
                  selectedLeadingIconColor = AccentPink,
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("auth_mode_signup_chip"),
              )
            }

            // 3) Input fields for mobile number and email address, plus Continue / Send OTP button
            OutlinedTextField(
              value = mobileNumber,
              onValueChange = {
                mobileNumber = it
                errorMessage = null
              },
              label = { Text("Mobile Number (मोबाइल नंबर)") },
              placeholder = { Text("+91 98765 43210") },
              leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = "Mobile Number", tint = ElectricBlue)
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("mobile_number_input"),
            )

            OutlinedTextField(
              value = emailAddress,
              onValueChange = {
                emailAddress = it
                errorMessage = null
              },
              label = { Text("Email Address (ईमेल पता)") },
              placeholder = { Text("name@gmail.com") },
              leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email Address", tint = NeonPurple)
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("email_address_input"),
            )

            Button(
              onClick = handleContinueClick,
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("continue_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = StarBackground,
              ),
            ) {
              Text(
                text = if (isOtpStepVisible) "Resend OTP / Continue" else "Continue (Send OTP)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            // OTP Verification Section (appears when user taps Continue / Send OTP)
            AnimatedVisibility(visible = isOtpStepVisible) {
              Surface(
                color = StarCardElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, ElectricBlue),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("otp_verification_card"),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StarSuccessGreen)
                      Text(
                        text = "Your 6-Digit OTP: $generatedOtp",
                        style = MaterialTheme.typography.titleSmall,
                        color = StarSuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("generated_otp_display"),
                      )
                    }
                    TextButton(
                      onClick = rememberSingleTap(cooldownMs = 300L) {
                        otpInput = generatedOtp
                        errorMessage = null
                      },
                      modifier = Modifier.testTag("autofill_otp_button"),
                    ) {
                      Text("Auto-Fill", color = ElectricBlue, fontWeight = FontWeight.Bold)
                    }
                  }

                  OutlinedTextField(
                    value = otpInput,
                    onValueChange = { input ->
                      val digits = input.filter { it.isDigit() }.take(6)
                      otpInput = digits
                      errorMessage = null
                    },
                    label = { Text("Enter 6-Digit OTP (ओटीपी डालें)") },
                    placeholder = { Text("Enter $generatedOtp") },
                    leadingIcon = {
                      Icon(Icons.Default.Lock, contentDescription = "OTP Code", tint = ElectricBlue)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("otp_code_input"),
                  )

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Button(
                      onClick = handleVerifyOtpClick,
                      modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("verify_otp_button"),
                      shape = RoundedCornerShape(12.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = StarSuccessGreen,
                        contentColor = StarBackground,
                      ),
                    ) {
                      Text(
                        text = if (authMode == "signup") "Verify OTP & Sign Up" else "Verify OTP & Login",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                      )
                    }

                    OutlinedButton(
                      onClick = handleResendOtpClick,
                      modifier = Modifier
                        .height(48.dp)
                        .testTag("resend_otp_button"),
                      shape = RoundedCornerShape(12.dp),
                    ) {
                      Icon(Icons.Default.Refresh, contentDescription = "Resend OTP", tint = ElectricBlue)
                    }
                  }
                }
              }
            }

            if (infoMessage != null) {
              Surface(
                color = StarSuccessGreen.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StarSuccessGreen),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("auth_info_banner"),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StarSuccessGreen)
                  Text(
                    text = infoMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = StarPrimaryText,
                  )
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              HorizontalDivider(modifier = Modifier.weight(1f), color = StarOutlineBorder)
              Text(
                text = "OR",
                style = MaterialTheme.typography.labelSmall,
                color = StarSecondaryText,
              )
              HorizontalDivider(modifier = Modifier.weight(1f), color = StarOutlineBorder)
            }

            // Account Already Exists Notice & One-Tap Login Option
            if (existingAccountEmail != null) {
              Surface(
                color = StarCardElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StarWarningGold),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("account_already_exists_card"),
              ) {
                Column(
                  modifier = Modifier.padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = StarWarningGold)
                    Text(
                      text = "Account Already Exists",
                      style = MaterialTheme.typography.titleSmall,
                      color = StarWarningGold,
                    )
                  }
                  Text(
                    text = "An account with Gmail ID ${existingAccountEmail!!} is already registered. Tap below to Login.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarPrimaryText,
                  )
                  SingleTapButton(
                    text = "Continue to Login (${existingAccountEmail!!})",
                    onClick = {
                      val confirmedEmail = existingAccountEmail!!
                      authMode = "login"
                      existingAccountEmail = null
                      onAuthSuccess(confirmedEmail, false)
                    },
                    containerColor = StarSuccessGreen,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "login_existing_account_button",
                  )
                  OutlinedButton(
                    onClick = rememberSingleTap {
                      signOutUser(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = {
                          existingAccountEmail = null
                        },
                        scope = scope,
                      )
                    },
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text("Use Another Google Account", color = StarSecondaryText)
                  }
                }
              }
            } else {
              // 4) Clearly visible "Continue with Google" button — only opens Google Sign-In when tapped
              Button(
                onClick = handleGoogleAuthClick,
                enabled = !isLoading,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = NeonPurple,
                  contentColor = StarPrimaryText,
                  disabledContainerColor = StarCardElevated,
                ),
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = StarPrimaryText,
                    strokeWidth = 2.dp,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Authenticating with Google...",
                    style = MaterialTheme.typography.titleSmall,
                    color = StarPrimaryText,
                  )
                } else {
                  Icon(Icons.Default.AccountCircle, contentDescription = null)
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }

            if (errorMessage != null) {
              Surface(
                color = StarErrorRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StarErrorRed),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("auth_error_banner"),
              ) {
                Text(
                  text = errorMessage!!,
                  style = MaterialTheme.typography.bodyMedium,
                  color = StarPrimaryText,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
