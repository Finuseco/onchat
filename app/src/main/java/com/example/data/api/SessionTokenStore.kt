package com.example.data.api

import android.content.Context
import android.content.SharedPreferences

object SessionTokenStore {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("onchat_session_prefs", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs?.edit()?.putString("auth_token", token)?.apply()
    }

    fun getToken(): String? {
        return prefs?.getString("auth_token", null)
    }

    fun saveWalletId(walletId: String) {
        prefs?.edit()?.putString("wallet_id", walletId)?.apply()
    }

    fun getWalletId(): String {
        return prefs?.getString("wallet_id", "") ?: ""
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
    }

    fun saveUserPhone(phone: String) {
        prefs?.edit()?.putString("user_phone", phone)?.apply()
    }

    fun getUserPhone(): String? {
        return prefs?.getString("user_phone", null)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs?.edit()?.putBoolean("onboarding_completed", completed)?.apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs?.getBoolean("onboarding_completed", false) ?: false
    }
}
