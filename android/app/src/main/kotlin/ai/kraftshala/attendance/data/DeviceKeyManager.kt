package ai.kraftshala.attendance.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

/**
 * Manages the device's ECDSA P-256 key pair using Android Keystore.
 * Private key is hardware-backed where supported, never leaves the device.
 */
class DeviceKeyManager {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    /** Returns true if a key pair already exists for this app. */
    fun hasKeyPair(): Boolean = keyStore.containsAlias(KEY_ALIAS)

    /** Generates a new key pair. Called once at onboarding. */
    fun generateKeyPair(): PublicKey {
        val gen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        gen.initialize(spec)
        return gen.generateKeyPair().public
    }

    /** Returns the public key in PEM format for upload to the backend. */
    fun getPublicKeyPem(): String {
        val publicKey = (keyStore.getCertificate(KEY_ALIAS)?.publicKey)
            ?: throw IllegalStateException("No key pair found; call generateKeyPair() first.")
        val b64 = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n$b64\n-----END PUBLIC KEY-----"
    }

    /** Signs the given payload bytes with the device private key. Returns Base64 signature. */
    fun sign(payload: ByteArray): String {
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(privateKey)
        sig.update(payload)
        return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
    }

    /** Removes the key pair. Used on device re-pairing. */
    fun deleteKeyPair() {
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "kraftshala_attendance_device_key"
    }
}
