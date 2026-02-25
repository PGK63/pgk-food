package com.example.pgk_food.util

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

object QrCrypto {
    fun generateSignature(
        userId: String,
        timestamp: Long,
        mealType: String,
        nonce: String,
        privateKeyBase64: String
    ): String {
        return try {
            val data = "$userId:$timestamp:$mealType:$nonce"
            val keyBytes = Base64.decode(privateKeyBase64, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val privateKey = keyFactory.generatePrivate(keySpec)

            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            signature.update(data.toByteArray())

            val signatureBytes = signature.sign()
            Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("QrCrypto", "Failed to generate QR signature", e)
            ""
        }
    }

    fun verifySignature(
        userId: String,
        timestamp: Long,
        mealType: String,
        nonce: String,
        signatureBase64: String,
        publicKeyBase64: String
    ): Boolean {
        return try {
            val data = "$userId:$timestamp:$mealType:$nonce"
            val keyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
            val keySpec = java.security.spec.X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(keySpec)

            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(data.toByteArray())

            val signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            Log.e("QrCrypto", "Failed to verify signature", e)
            false
        }
    }

    fun generateTransactionHash(
        userId: String,
        timestamp: Long,
        mealType: String,
        nonce: String
    ): String {
        return try {
            val data = "$userId:$timestamp:$mealType:$nonce"
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(data.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("QrCrypto", "Failed to hash tx", e)
            ""
        }
    }

    fun isTimestampValid(timestamp: Long, toleranceSeconds: Long = 120): Boolean {
        val currentSeconds = System.currentTimeMillis() / 1000
        return Math.abs(currentSeconds - (timestamp)) <= toleranceSeconds
    }
}
