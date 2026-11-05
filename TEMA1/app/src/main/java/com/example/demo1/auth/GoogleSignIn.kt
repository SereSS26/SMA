package com.example.demo1.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * The OAuth part of "Sign in with Google": Credential Manager shows Google's account picker, the user
 * authenticates with Google, and the app receives a signed ID token, which Firebase then exchanges
 * for its own session. Needs an Activity context, so it is called from the UI, never from a ViewModel.
 */
object GoogleSignIn {

    suspend fun requestIdToken(activityContext: Context): GoogleSignInResult {
        val webClientId = webClientId(activityContext) ?: return GoogleSignInResult.NotConfigured
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = webClientId).build())
            .build()
        return try {
            // The wrapper avoids keeping the Activity alive if it is recreated during the picker.
            val response = CredentialManager.create(activityContext)
                .getCredential(MutableContextWrapper(activityContext), request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Error("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.NoAccount
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Error(e.message ?: e.type)
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Error(e.message ?: "Invalid Google ID token")
        }
    }

    /** Lets Google show the full account picker again next time instead of reusing this account. */
    suspend fun clearCredentialState(context: Context) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Nothing to clear (e.g. no credential provider on the device); logout still succeeds.
        }
    }

    /**
     * The Web OAuth client ID that the google-services plugin generates from google-services.json.
     * Looked up by name so the project still compiles before that file is added.
     */
    @SuppressLint("DiscouragedApi")
    private fun webClientId(context: Context): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id).takeIf { it.isNotBlank() }
    }
}
