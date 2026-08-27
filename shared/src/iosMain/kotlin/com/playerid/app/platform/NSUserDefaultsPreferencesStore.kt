package com.playerid.app.platform

import platform.Foundation.NSUserDefaults

class NSUserDefaultsPreferencesStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : PreferencesStore {
    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.integerForKey(key)

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) defaultValue else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}