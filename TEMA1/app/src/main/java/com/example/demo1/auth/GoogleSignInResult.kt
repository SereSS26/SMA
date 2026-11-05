package com.example.demo1.auth

/** Why getting a Google ID token did not work, in terms the UI can show. */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    /** The user closed the Google account picker. */
    data object Cancelled : GoogleSignInResult
    /** google-services.json has no Web client ID (Google sign-in not enabled in Firebase). */
    data object NotConfigured : GoogleSignInResult
    /** No Google account on the device. */
    data object NoAccount : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}
