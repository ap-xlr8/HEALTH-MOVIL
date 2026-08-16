package com.healthos.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class InMemoryTokenStore : SecureTokenStore() {
    private var tokenMap = mutableMapOf<String, String>()

    override fun save(
        accessToken: String,
        refreshToken: String,
        role: String,
        userId: String?,
    ) {
        tokenMap["access_token"] = accessToken
        tokenMap["refresh_token"] = refreshToken
        tokenMap["role"] = role
        if (userId != null) tokenMap["user_id"] = userId
    }

    override fun accessToken(): String? = tokenMap["access_token"]

    override fun refreshToken(): String? = tokenMap["refresh_token"]

    override fun role(): String? = tokenMap["role"]

    private var bioEnabled = false
    private var bioDismissed = false

    override fun isBiometricEnabled(): Boolean = bioEnabled

    override fun setBiometricEnabled(enabled: Boolean) {
        bioEnabled = enabled
    }

    override fun isBiometricOptInDismissed(): Boolean = bioDismissed

    override fun setBiometricOptInDismissed(dismissed: Boolean) {
        bioDismissed = dismissed
    }

    override fun clear() {
        tokenMap.clear()
    }
}

class SecureStoresTest {
    @Test
    fun testTokenStoreSaveAndRetrieve() {
        val store = InMemoryTokenStore()
        assertNull(store.accessToken())
        assertNull(store.refreshToken())
        assertNull(store.role())

        store.save("acc_123", "ref_456", "PATIENT")
        assertEquals("acc_123", store.accessToken())
        assertEquals("ref_456", store.refreshToken())
        assertEquals("PATIENT", store.role())

        store.clear()
        assertNull(store.accessToken())
        assertNull(store.refreshToken())
        assertNull(store.role())
    }

    @Test
    fun testBiometricPreferencesToggle() {
        val store = InMemoryTokenStore()
        assertTrue(!store.isBiometricEnabled())
        assertTrue(!store.isBiometricOptInDismissed())

        store.setBiometricEnabled(true)
        store.setBiometricOptInDismissed(true)

        assertTrue(store.isBiometricEnabled())
        assertTrue(store.isBiometricOptInDismissed())

        store.setBiometricEnabled(false)
        assertTrue(!store.isBiometricEnabled())
    }

    @Test
    fun testPassphraseGenerationEntropy() {
        val bytes1 = ByteArray(32)
        val bytes2 = ByteArray(32)
        val rng = SecureRandom()
        rng.nextBytes(bytes1)
        rng.nextBytes(bytes2)

        assertEquals(32, bytes1.size)
        assertEquals(32, bytes2.size)
        // Two independent random 256-bit passphrases must not be equal
        assertTrue(!bytes1.contentEquals(bytes2))
    }
}
