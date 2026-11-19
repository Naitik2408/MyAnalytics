package com.lifeanalytics.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lifeanalytics.app.core.common.SupportedCurrency
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.domain.model.AppThemeOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "life_analytics_prefs")

class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.dataStore)

    private object Keys {
        val THEME_OPTION = stringPreferencesKey("theme_option_v12")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val MONTHLY_BUDGET_MINOR = longPreferencesKey("monthly_budget_minor")
    }

    val preferencesFlow: Flow<AppPreferences> = dataStore.data.map { prefs ->
        val themeRaw = prefs[Keys.THEME_OPTION]
        val theme = AppThemeOption.entries.firstOrNull { it.name == themeRaw } ?: AppThemeOption.LIGHT
        val currency = SupportedCurrency.fromCode(prefs[Keys.CURRENCY_CODE]).code
        val budget = prefs[Keys.MONTHLY_BUDGET_MINOR]?.takeIf { it > 0L }
        AppPreferences(
            themeOption = theme,
            currencyCode = currency,
            monthlyBudgetMinor = budget
        )
    }

    suspend fun setThemeOption(option: AppThemeOption) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_OPTION] = option.name
        }
    }

    suspend fun setCurrencyCode(currencyCode: String) {
        val valid = SupportedCurrency.fromCode(currencyCode).code
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CODE] = valid
        }
    }

    suspend fun setMonthlyBudgetMinor(budgetMinor: Long?) {
        dataStore.edit { prefs ->
            if (budgetMinor == null || budgetMinor <= 0L) {
                prefs.remove(Keys.MONTHLY_BUDGET_MINOR)
            } else {
                prefs[Keys.MONTHLY_BUDGET_MINOR] = budgetMinor
            }
        }
    }

    suspend fun restorePreferences(preferences: AppPreferences) {
        val validCurrency = SupportedCurrency.fromCode(preferences.currencyCode).code
        dataStore.edit { prefs ->
            prefs[Keys.THEME_OPTION] = preferences.themeOption.name
            prefs[Keys.CURRENCY_CODE] = validCurrency
            val budget = preferences.monthlyBudgetMinor
            if (budget != null && budget > 0L) {
                prefs[Keys.MONTHLY_BUDGET_MINOR] = budget
            } else {
                prefs.remove(Keys.MONTHLY_BUDGET_MINOR)
            }
        }
    }
}
