/*
 * MakeMeDown Text Reader
 * Copyright (C) 2026 Hui-Hong You
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ghostsinthelab.app.makedown.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Compose helper that prepares a one-shot device-credential / biometric
 * authentication launcher.
 *
 * The launcher pops a [BiometricPrompt] configured with
 * `setDeviceCredentialAllowed(true)`, which means the user can satisfy it
 * with **any** of:
 *  - an enrolled fingerprint / face,
 *  - the system unlock PIN, pattern or password.
 *
 * `setDeviceCredentialAllowed` is deprecated in favour of
 * `setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)`, but the
 * combined-authenticators flag is only valid on Android 11+ — on Android
 * 9 and 10 it throws. Since we still support API 26, the deprecated form
 * is the one path that works on every supported release. The androidx
 * library handles the per-API-level wrapping internally.
 *
 * Returned function:
 *  - Calls [onSuccess] on the main thread when authentication succeeds.
 *  - Calls [onError] when the user cancels or auth fails irrecoverably.
 *  - Silently swallows individual retry failures so the prompt stays open.
 */
@Composable
fun rememberDeviceAuthLauncher(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    return remember(activity, title, subtitle, onSuccess, onError) {
        launcher@{
            if (activity == null) {
                onError("Cannot launch authentication: no FragmentActivity in scope")
                return@launcher
            }
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult,
                    ) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        // Terminal error: user cancelled, locked out, no
                        // credential set up, hardware unavailable, etc.
                        onError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        // Single attempt failed but the prompt stays open
                        // for another try — no callback yet.
                    }
                },
            )

            @Suppress("DEPRECATION")
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDeviceCredentialAllowed(true)
                .build()

            prompt.authenticate(info)
        }
    }
}

/** Walk the [Context] wrapper chain looking for a [FragmentActivity]. */
private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
