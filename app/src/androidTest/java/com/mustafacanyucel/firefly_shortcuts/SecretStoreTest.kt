package com.mustafacanyucel.firefly_shortcuts

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.platform.app.InstrumentationRegistry
import com.mustafacanyucel.firefly_shortcuts.data.secrets.KeystoreCipher
import com.mustafacanyucel.firefly_shortcuts.data.secrets.Secret
import com.mustafacanyucel.firefly_shortcuts.data.secrets.SecretStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.security.KeyStore

class SecretStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val file = context.preferencesDataStoreFile("secrets_test")

    @After
    fun tearDown() {
        file.delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("secrets_test")
    }

    @Test
    fun storesEncryptedAndRoundTrips() = runBlocking {
        val dataStore = PreferenceDataStoreFactory.create { file }
        val store = SecretStore(dataStore, KeystoreCipher("secrets_test"))

        store.put(Secret.ACCESS_TOKEN to "abc", Secret.REFRESH_TOKEN to "def")
        assertEquals("abc", store.get(Secret.ACCESS_TOKEN))
        assertEquals("def", store.get(Secret.REFRESH_TOKEN))

        val raw = dataStore.data.first().asMap().values
        assertFalse(raw.any { it == "abc" || it == "def" })

        store.put(Secret.ACCESS_TOKEN to null)
        assertNull(store.get(Secret.ACCESS_TOKEN))
        assertEquals("def", store.get(Secret.REFRESH_TOKEN))
    }
}
