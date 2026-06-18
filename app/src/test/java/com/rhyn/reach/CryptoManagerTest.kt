package com.rhyn.reach

import com.rhyn.reach.core.crypto.CryptoManager
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

class CryptoManagerTest {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun testEd25519Signing() {
        val cryptoManager = CryptoManager()
        val (pub, priv) = cryptoManager.generateEd25519KeyPair()
        
        val payload = "This is a test payload for routing"
        val signature = cryptoManager.signData(payload, priv)
        
        println("Generated Signature: $signature")
        
        val isValid = cryptoManager.verifySignature(payload, signature, pub)
        assertTrue("Signature should be valid", isValid)
        
        val isInvalid = cryptoManager.verifySignature("Tampered payload", signature, pub)
        assertFalse("Tampered signature should be invalid", isInvalid)
    }
}
