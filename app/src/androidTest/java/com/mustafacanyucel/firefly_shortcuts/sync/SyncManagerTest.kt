package com.mustafacanyucel.firefly_shortcuts.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.mustafacanyucel.firefly_shortcuts.data.api.FireflyApi
import com.mustafacanyucel.firefly_shortcuts.data.db.AccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.AppDatabase
import com.mustafacanyucel.firefly_shortcuts.data.db.CategoryEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.ShortcutEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.TagEntity
import com.mustafacanyucel.firefly_shortcuts.data.settings.SettingsStore
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File


class SyncManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val settingsFile =
        File(context.cacheDir, "sync_test_${System.nanoTime()}.preferences_pb")
    private val settings = SettingsStore(PreferenceDataStoreFactory.create { settingsFile })

    // Fake Firefly: the items per API path. Paths not listed return an empty list.
    private val responses = mutableMapOf<String, String>()
    private val failing = mutableSetOf<String>()

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath.removePrefix("/api/v1/")
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(if (path in failing) 500 else 200)
                .message("")
                .body(page(responses[path].orEmpty()).toResponseBody())
                .build()
        }
        .build()

    private val sync = SyncManager(
        FireflyApi(client, { "https://firefly.test" }, { "token" }),
        db,
        settings,
    )

    @After
    fun tearDown() {
        db.close()
        settingsFile.delete()
    }

    @Test
    fun replacesReferenceDataAndKeepsUsedAccounts() = runBlocking {
        db.accountDao().upsertAll(
            listOf(account("1", "Old name"), account("2", "Closed"), account("3", "Unused"))
        )
        db.categoryDao().upsertAll(listOf(CategoryEntity("10", "Gone")))
        db.shortcutDao().save(
            ShortcutEntity(
                name = "Coffee",
                amount = "3.50",
                fromAccountId = "1",
                toAccountId = "2",
                categoryId = "10",
                transactionType = TransactionType.WITHDRAWAL,
            ),
            emptyList(),
        )
        responses["accounts"] = """{"id":"1","attributes":{"name":"New name","type":"asset"}}"""
        responses["categories"] = """{"id":"11","attributes":{"name":"Food"}}"""

        sync.syncAll()

        val accounts = db.accountDao().getAll().associateBy { it.id }
        assertEquals(setOf("1", "2"), accounts.keys)
        assertEquals("New name", accounts.getValue("1").name)
        assertFalse(accounts.getValue("2").active)
        assertEquals(listOf(CategoryEntity("11", "Food")), db.categoryDao().getAll())
        assertNull(db.shortcutDao().getAll().single().shortcut.categoryId)
        assertFalse(sync.state.value.failed)
        assertNotNull(settings.current().lastSync)
    }

    @Test
    fun failedTableKeepsItsRowsAndOthersStillSync() = runBlocking {
        db.tagDao().upsertAll(listOf(TagEntity("5", "old")))
        failing += "tags"
        responses["bills"] = """{"id":"7","attributes":{"name":"Rent"}}"""

        sync.syncAll()

        val state = sync.state.value
        assertTrue(state.tables[SyncTable.TAGS] is TableStatus.Failed)
        assertEquals(TableStatus.Done(1), state.tables[SyncTable.BILLS])
        assertEquals(listOf(TagEntity("5", "old")), db.tagDao().getAll())
        assertNull(settings.current().lastSync)
    }

    private fun account(id: String, name: String) =
        AccountEntity(id, name, "asset", null, null, null, active = true)

    private fun page(items: String) =
        """{"data":[$items],"meta":{"pagination":{"current_page":1,"total_pages":1}}}"""

}
