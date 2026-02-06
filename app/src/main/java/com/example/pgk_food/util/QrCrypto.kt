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
}
