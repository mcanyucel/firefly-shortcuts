package com.mustafacanyucel.firefly_shortcuts.data.api

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FireflyApiTest {
    private val server = MockWebServer()
    private lateinit var api: FireflyApi

    @Before
    fun setUp() {
        server.start()
        val base = server.url("/").toString().trimEnd('/')
        api = FireflyApi(OkHttpClient(), { base }, { "token" })
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun fetchesAllPages() = runBlocking<Unit> {
        server.enqueue(page("""{"id":"1","attributes":{"name":"Food"}}""", current = 1, total = 2))
        server.enqueue(page("""{"id":"2","attributes":{"name":"Rent"}}""", current = 2, total = 2))

        val categories = api.getCategories()

        assertEquals(listOf("Food", "Rent"), categories.map { it.attributes.name })
        val first = server.takeRequest()
        assertEquals("/api/v1/categories", first.url.encodedPath)
        assertEquals("1", first.url.queryParameter("page"))
        assertEquals("Bearer token", first.headers["Authorization"])
        assertEquals("2", server.takeRequest().url.queryParameter("page"))
    }

    @Test
    fun duplicateCountsAsSuccess() = runBlocking<Unit> {
        server.enqueue(
            MockResponse.Builder()
                .code(422)
                .body("""{"message":"The given data was invalid.","errors":{"transactions.0.description":["Duplicate of transaction #12."]}}""")
                .build()
        )

        api.createTransaction(sampleRequest())

        val body = server.takeRequest().body!!.utf8()
        assertTrue(""""error_if_duplicate_hash":true""" in body)
        assertFalse("category_id" in body)
    }

    @Test
    fun errorStatusThrows() {
        server.enqueue(MockResponse.Builder().code(401).build())

        val e = assertThrows(ApiException::class.java) { runBlocking { api.getTags() } }

        assertTrue(e.isUnauthorized)
    }

    private fun page(item: String, current: Int, total: Int) = MockResponse.Builder()
        .body("""{"data":[$item],"meta":{"pagination":{"current_page":$current,"total_pages":$total}}}""")
        .build()

    private fun sampleRequest() = TransactionRequest(
        listOf(
            TransactionSplit(
                type = "withdrawal",
                date = "2026-11-04T12:00:00+03:00",
                amount = "10.00",
                description = "Test",
                sourceId = "1",
                sourceName = "Bank",
                destinationId = "2",
                destinationName = "Shop",
            )
        )
    )
}
