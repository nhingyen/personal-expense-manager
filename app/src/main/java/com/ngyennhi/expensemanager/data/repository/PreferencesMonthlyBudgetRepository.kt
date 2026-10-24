package com.ngyennhi.expensemanager.data.repository

import android.content.SharedPreferences
import com.ngyennhi.expensemanager.domain.repository.MonthlyBudgetRepository
import java.io.IOException
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PreferencesMonthlyBudgetRepository(private val preferences: SharedPreferences) : MonthlyBudgetRepository {
    override fun observeLimits() = callbackFlow {
        fun publish() {
            try {
                val limits = preferences.all.filterKeys { it.startsWith(PREFIX) }.map { (key, value) ->
                    val month = YearMonth.parse(key.removePrefix(PREFIX))
                    val amount = value as? Long ?: error("Invalid stored budget")
                    require(amount > 0)
                    month to amount
                }.toMap()
                trySend(limits)
            } catch (cause: Exception) { close(cause) }
        }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.startsWith(PREFIX)) publish()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        publish()
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    override suspend fun setLimit(month: YearMonth, amountVnd: Long?) = withContext(Dispatchers.IO) {
        require(amountVnd == null || amountVnd > 0)
        val editor = preferences.edit()
        val key = "$PREFIX$month"
        if (amountVnd == null) editor.remove(key) else editor.putLong(key, amountVnd)
        if (!editor.commit()) throw IOException("Budget could not be persisted")
    }

    private companion object { const val PREFIX = "monthly_budget:" }
}
