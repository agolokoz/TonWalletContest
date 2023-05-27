package org.ton.wallet.lib.core

import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ton.lib.security.TonSecurity
import org.ton.wallet.AppLifecycleDetector
import org.ton.wallet.R
import org.ton.wallet.uikit.dialog.AlertDialog
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

object SecurityUtils {

    private const val AndroidKeyStore = "AndroidKeyStore"

    private const val ArgonHashSize = 32
    private const val ArgonIterations = 10000
    private const val ArgonMemoryTwoDegree = 12
    private const val ArgonParallelism = 1

    private const val Authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
    private const val BiometricKeyAlias = "a"
    private const val EncryptionAlgorithm = KeyProperties.KEY_ALGORITHM_AES
    private const val EncryptionBlockMode = KeyProperties.BLOCK_MODE_GCM
    private const val EncryptionPadding = KeyProperties.ENCRYPTION_PADDING_NONE

    private val keyStore = KeyStore.getInstance(AndroidKeyStore)
    private val secureRandom = SecureRandom()

    private val _isBiometricsAvailableFlow = MutableStateFlow(false)
    val isBiometricsAvailableFlow: StateFlow<Boolean> = _isBiometricsAvailableFlow

    private lateinit var ctx: Application

    fun init(context: Application) {
        ctx = context
        keyStore.load(null)
        AppLifecycleDetector.isAppForegroundFlow
            .onEach { isForeground ->
                if (isForeground) {
                    val isAvailable = isBiometricsAvailableOnDevice(context) && !isBiometricsNoneEnrolled(context)
                    _isBiometricsAvailableFlow.value = isAvailable
                }
            }
            .launchIn(ThreadUtils.appCoroutineScope)
    }

    fun getArgonHash(password: ByteArray, salt: ByteArray): ByteArray? {
        return TonSecurity.nativeGetArgonHash(password, salt, ArgonIterations, ArgonMemoryTwoDegree, ArgonParallelism, ArgonHashSize)
    }

    fun randomBytesSecured(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    // Biometric
    fun clearBiometricKey() {
        keyStore.deleteEntry(BiometricKeyAlias)
    }

    fun isBiometricsAvailableOnDevice(context: Context): Boolean {
        val authStatus = BiometricManager.from(context).canAuthenticate(Authenticators)
        return authStatus == BiometricManager.BIOMETRIC_SUCCESS || authStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    fun isBiometricsNoneEnrolled(context: Context): Boolean {
        return BiometricManager.from(context).canAuthenticate(Authenticators) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        description: String,
        callback: BiometricPrompt.AuthenticationCallback,
    ) {
        if (isBiometricsNoneEnrolled(activity)) {
            showBiometricEnrollAlert(activity)
        } else {
            showBiometricPromptInternal(activity, description, callback)
        }
    }

    fun showBiometricEnrollAlert(context: Context): AlertDialog {
        val alertDialog = AlertDialog.Builder(
            title = Res.str(R.string.enable_biometrics),
            message = Res.str(R.string.biometric_enroll_message),
            positiveButton = Res.str(R.string.open_settings) to DialogInterface.OnClickListener { dialog, _ ->
                showBiometricEnroll(context)
                dialog.dismiss()
            },
            negativeButton = Res.str(R.string.cancel) to DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() }
        ).build(context)
        alertDialog.show()
        return alertDialog
    }

    private fun showBiometricPromptInternal(
        activity: FragmentActivity,
        description: String,
        callback: BiometricPrompt.AuthenticationCallback,
    ) {
        val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setAllowedAuthenticators(Authenticators)
            .setTitle(Res.str(R.string.biometric_prompt_title))
            .setSubtitle(Res.str(R.string.app_name))
            .setDescription(description)
            .setNegativeButtonText(Res.str(R.string.cancel))
            .build()
        try {
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(getBiometricCipher()))
        } catch (e: KeyPermanentlyInvalidatedException) {
            clearBiometricKey()
        }
    }

    private fun showBiometricEnroll(context: Context) {
        val intent: Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
            intent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, Authenticators)
        } else {
            intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun getBiometricCipher(): Cipher {
        val cipher = Cipher.getInstance("${EncryptionAlgorithm}/${EncryptionBlockMode}/${EncryptionPadding}")
        var secretKey = keyStore.getKey(BiometricKeyAlias, null)
        if (secretKey == null) {
            val purpose = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(BiometricKeyAlias, purpose).apply {
                setBlockModes(EncryptionBlockMode)
                setEncryptionPaddings(EncryptionPadding)
                setUserAuthenticationRequired(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setInvalidatedByBiometricEnrollment(true)
                }
            }.build()
            val keyGenerator = KeyGenerator.getInstance(EncryptionAlgorithm, AndroidKeyStore)
            keyGenerator.init(keyGenParameterSpec)
            secretKey = keyGenerator.generateKey()
        }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }
}