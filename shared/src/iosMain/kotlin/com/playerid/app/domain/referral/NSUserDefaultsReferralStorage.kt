package com.playerid.app.domain.referral

import platform.Foundation.NSUserDefaults

class NSUserDefaultsReferralStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : ReferralStorage {
    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.integerForKey(key).toInt()

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }
}