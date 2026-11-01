package com.example.data.backup

import org.bouncycastle.crypto.PBEParametersGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupCipher(private val random: SecureRandom = SecureRandom()) {
    fun encrypt(snapshot: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 12) { "Use a backup password with at least 12 characters." }
        require(snapshot.isNotEmpty() && snapshot.size <= MAX_PAYLOAD_BYTES) { "Backup exceeds the supported size." }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).putInt(FORMAT_VERSION).put(salt).put(nonce).array()
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(header)
            header + cipher.doFinal(snapshot)
        } finally {
            key.fill(0)
        }
    }

    fun decrypt(backup: ByteArray, password: CharArray): ByteArray {
        require(backup.size in (HEADER_BYTES + 16)..MAX_BACKUP_BYTES) { "Invalid or oversized backup." }
        val header = backup.copyOfRange(0, HEADER_BYTES)
        val buffer = ByteBuffer.wrap(header)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        require(magic.contentEquals(MAGIC) && buffer.int == FORMAT_VERSION) { "Unsupported backup format." }
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val nonce = ByteArray(NONCE_BYTES).also(buffer::get)
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(header)
            cipher.doFinal(backup, HEADER_BYTES, backup.size - HEADER_BYTES)
        } finally {
            key.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val encoded = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password)
        return try {
            val generator = PKCS5S2ParametersGenerator(SHA256Digest())
            generator.init(encoded, salt, KDF_ITERATIONS)
            (generator.generateDerivedParameters(256) as KeyParameter).key
        } finally {
            encoded.fill(0)
        }
    }

    companion object {
        private val MAGIC = "NEWMEBAK".toByteArray(Charsets.US_ASCII)
        private const val FORMAT_VERSION = 1
        private const val KDF_ITERATIONS = 600_000
        private const val SALT_BYTES = 32
        private const val NONCE_BYTES = 12
        private const val HEADER_BYTES = 8 + 4 + SALT_BYTES + NONCE_BYTES
        const val MAX_PAYLOAD_BYTES = 64 * 1024 * 1024
        const val MAX_BACKUP_BYTES = MAX_PAYLOAD_BYTES + HEADER_BYTES + 16
    }
}