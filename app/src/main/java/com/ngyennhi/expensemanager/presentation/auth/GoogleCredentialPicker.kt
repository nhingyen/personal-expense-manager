package com.ngyennhi.expensemanager.presentation.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.ngyennhi.expensemanager.BuildConfig
import com.ngyennhi.expensemanager.domain.model.AuthError
import com.ngyennhi.expensemanager.domain.model.AuthFailure
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException

/** Activity-bound Credential Manager request. No token or Activity is retained after return. */
internal object GoogleCredentialPicker {
    @SuppressLint("DiscouragedApi")
    suspend fun requestIdToken(activity: Activity): String? {
        if (!BuildConfig.GOOGLE_SEPARATE_ACCOUNTS_CONFIRMED) throw AuthFailure(AuthError.NOT_CONFIGURED)
        // google-services generates this resource only when the Firebase JSON contains a Web OAuth client.
        val resourceId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (resourceId == 0) throw AuthFailure(AuthError.NOT_CONFIGURED)
        val clientId = activity.getString(resourceId)
        if (clientId.isBlank()) throw AuthFailure(AuthError.NOT_CONFIGURED)
        val nonce = ByteArray(32).also(SecureRandom()::nextBytes)
        val option = GetSignInWithGoogleOption.Builder(clientId)
            .setNonce(Base64.encodeToString(nonce, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = try {
            CredentialManager.create(activity.applicationContext).getCredential(activity, request)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: GetCredentialCancellationException) { return null
        } catch (_: NoCredentialException) { throw AuthFailure(AuthError.GOOGLE_UNAVAILABLE)
        } catch (_: GetCredentialProviderConfigurationException) { throw AuthFailure(AuthError.NOT_CONFIGURED)
        } catch (_: GetCredentialUnsupportedException) { throw AuthFailure(AuthError.GOOGLE_UNAVAILABLE)
        } catch (_: GetCredentialException) { throw AuthFailure(AuthError.GOOGLE_FAILED) }
        val credential = response.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthFailure(AuthError.GOOGLE_FAILED)
        }
        val token = try { GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (_: GoogleIdTokenParsingException) { throw AuthFailure(AuthError.INVALID_CREDENTIALS) }
        if (token.isBlank()) throw AuthFailure(AuthError.INVALID_CREDENTIALS)
        return token
    }
}
