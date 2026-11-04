package com.mustafacanyucel.firefly_shortcuts.sync

import androidx.room.withTransaction
import com.mustafacanyucel.firefly_shortcuts.data.api.AccountAttributes
import com.mustafacanyucel.firefly_shortcuts.data.api.FireflyApi
import com.mustafacanyucel.firefly_shortcuts.data.api.Resource
import com.mustafacanyucel.firefly_shortcuts.data.db.AccountEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.AppDatabase
import com.mustafacanyucel.firefly_shortcuts.data.db.BillEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.BudgetEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.CategoryEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.PiggybankEntity
import com.mustafacanyucel.firefly_shortcuts.data.db.ReferenceDao
import com.mustafacanyucel.firefly_shortcuts.data.db.TagEntity
import com.mustafacanyucel.firefly_shortcuts.data.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.time.Instant

enum class SyncTable { ACCOUNTS, BILLS, BUDGETS, CATEGORIES, PIGGY_BANKS, TAGS }

sealed interface TableStatus {
    data object Syncing: TableStatus
    data class Done(val count:Int): TableStatus
    data class Failed(var error: Exception): TableStatus

}

data class SyncState(
    val running: Boolean = false,
    /** Empty until the first sync since the app started */
    val tables: Map<SyncTable, TableStatus> = emptyMap()
) {
    val failed: Boolean get() = tables.values.any { it is TableStatus.Failed }
}

class SyncManager(
    private val api: FireflyApi,
    private val database: AppDatabase,
    private val settings: SettingsStore
) {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val mutex = Mutex()

    /**
     * Downloads the reference data and makes the local tables match it.
     * Tables download in parallel and fail independently; a failed table
     * keeps its old rows. Does nothing if a sync is already running.
     */
    suspend fun syncAll() {
        if (!mutex.tryLock()) return
        try {
            _state.value = SyncState(
                running = true,
                tables = SyncTable.entries.associateWith { TableStatus.Syncing },
            )
            coroutineScope {
                launch { sync(SyncTable.ACCOUNTS) { syncAccounts() } }
                launch {
                    sync(SyncTable.BILLS) {
                        val dao = database.billDao()
                        replace(dao, api.getBills().map { BillEntity(it.id, it.attributes.name) }, BillEntity::id) { dao.getAll() }
                    }
                }
                launch {
                    sync(SyncTable.BUDGETS) {
                        val dao = database.budgetDao()
                        replace(dao, api.getBudgets().map {
                            BudgetEntity(
                                it.id,
                                it.attributes.name
                            )
                        }, BudgetEntity::id) { dao.getAll() }
                    }
                }
                launch {
                    sync(SyncTable.CATEGORIES) {
                        val dao = database.categoryDao()
                        replace(dao, api.getCategories().map { CategoryEntity(it.id, it.attributes.name) }, CategoryEntity::id) { dao.getAll() }
                    }
                }
                launch {
                    sync(SyncTable.PIGGY_BANKS) {
                        val dao = database.piggybankDao()
                        replace(dao, api.getPiggyBanks().map { PiggybankEntity(it.id, it.attributes.name) }, PiggybankEntity::id) { dao.getAll() }
                    }
                }
                launch {
                    sync(SyncTable.TAGS) {
                        val dao = database.tagDao()
                        replace(dao, api.getTags().map { TagEntity(it.id, it.attributes.tag) }, TagEntity::id) { dao.getAll() }
                    }
                }
            }
            if (!_state.value.failed) settings.setLastSync(Instant.now())
        } finally {
            _state.update { it.copy(running = false) }
            mutex.unlock()
        }
    }

    /**
     * Runs one table's sync and records how it went. Never throws, except to cancel.
     */
    private suspend fun sync(table: SyncTable, block: suspend()->Int) {
        val status = try {
            TableStatus.Done(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TableStatus.Failed(e)
        }
        _state.update { it.copy(tables = it.tables + (table to status)) }
    }

    /**
     * Makes the table hold exactly [fresh]. Rows the server no longer has are
     * deleted, which also clears them from shortcuts (SET_NULL / CASCADE).
     */
    private suspend fun <T> replace(
        dao: ReferenceDao<T>,
        fresh: List<T>,
        id: (T) -> String,
        local: suspend () -> List<T>
    ) : Int {
        val freshIds = fresh.mapTo(HashSet(), id)
        database.withTransaction {
            dao.deleteAll(local().filter { id(it) !in freshIds})
            dao.upsertAll(fresh)
        }
        return fresh.size
    }

    /**
     * Like [replace], but a deleted account that a shortcut still uses is kept as inactive:
     * deleting it would delete these shortcuts, too.
     */
    private suspend fun syncAccounts(): Int {
        val dao = database.accountDao()
        val fresh = api.getAccounts().map { it.toEntity() }
        val freshIds = fresh.mapTo(HashSet()) { it.id }
        database.withTransaction {
            val used = dao.usedIds().toSet()
            val (keep, drop) = dao.getAll()
                .filter { it.id !in freshIds }
                .partition { it.id in used }
            dao.deleteAll(drop)
            dao.upsertAll(fresh + keep.map { it.copy(active = false)})
        }
        return fresh.size
    }
}

private fun Resource<AccountAttributes>.toEntity() = AccountEntity(
    id = id,
    name = attributes.name,
    accountType = attributes.type,
    accountRole = attributes.accountRole,
    currencyCode = attributes.currencyCode,
    currencySymbol = attributes.currencySymbol,
    active = attributes.active
)
