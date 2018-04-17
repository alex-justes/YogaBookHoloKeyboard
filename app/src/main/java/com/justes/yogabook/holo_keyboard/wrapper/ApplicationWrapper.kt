package com.justes.yogabook.holo_keyboard.wrapper

import android.app.Application
import android.content.Context
import android.graphics.Point
import android.preference.PreferenceManager
import android.util.Log
import android.view.WindowManager
import org.json.JSONArray
import java.lang.ref.WeakReference

class ApplicationWrapper : Application()
{
    data class LayoutDescription(val label: String, val short: String)

    private var mLayoutMap = mutableMapOf<String, LayoutDescription>()
    private var mHoloIME = WeakReference<HoloIME>(null)
    private var mFloatingNotification = WeakReference<FloatingNotification>(null)

    override fun onCreate()
    {
        super.onCreate()
        val inputStream = resources.openRawResource(R.raw.holo_layouts)
        val size = inputStream.available()
        val b = ByteArray(size)
        inputStream.read(b, 0, size)
        val json = JSONArray(String(b))
        for (i in 0 until json.length())
        {
            val item = json.getJSONObject(i)
            mLayoutMap[item.getString("holo_layout")] = LayoutDescription(item.getString("label"),
                    item.getString("short"))
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (!preferences.contains(Helper.LAST_LAYOUT_KEY))
        {
            @Suppress()
            preferences.edit().putString(Helper.LAST_LAYOUT_KEY, "english").commit()
        }
        if (!preferences.contains(Helper.NOTIFICATION_POS_X_KEY))
        {
            @Suppress()
            preferences.edit().putInt(Helper.NOTIFICATION_POS_X_KEY, 0).commit()
        }
        if (!preferences.contains(Helper.NOTIFICATION_POS_Y_KEY))
        {
            val screenSize = Point()
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getSize(screenSize)
            @Suppress()
            preferences.edit().putInt(Helper.NOTIFICATION_POS_Y_KEY, screenSize.y).commit()
        }
        if (!preferences.contains(resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_key)))
        {
            @Suppress()
            preferences.edit().putString(resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_key),
                                         resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_default)).commit()
        }
    }

    fun getLayoutMap() = HashMap<String, LayoutDescription>(mLayoutMap)

    fun registerHoloIME(ime: WeakReference<HoloIME>)
    {
        mHoloIME = ime
    }

    fun unregisterHoloIME()
    {
        mHoloIME = WeakReference<HoloIME>(null)
    }

    fun registerFloatingNotification(notification: WeakReference<FloatingNotification>)
    {
        mFloatingNotification = notification
    }

    fun unregisterFloatingNotification()
    {
        mFloatingNotification = WeakReference<FloatingNotification>(null)
    }

    fun isFloatingNotificationActive(): Boolean
    {
        val floatingNotification = mFloatingNotification.get()
        if (floatingNotification != null)
        {
            return true
        }
        return false
    }

    fun isHoloIMEActive(): Boolean
    {
        val holoIme = mHoloIME.get()
        if (holoIme != null)
        {
            return true
        }
        return false
    }

    fun addShortcut(shortcut: Shortcut): Boolean
    {
        val holoIme = mHoloIME.get()
        if (holoIme != null)
        {
            return holoIme.addShortcut(shortcut)
        }
        return false
    }

    fun addShortcut(shortcut: String, action: String): Boolean
    {
        val holoIme = mHoloIME.get()
        if (holoIme != null)
        {
            return holoIme.addShortcut(shortcut, action)
        }
        return false
    }

    fun removeShortcut(shortcut: Shortcut): Boolean
    {
        val holoIme = mHoloIME.get()
        if (holoIme != null)
        {
            return holoIme.removeShortcut(shortcut)
        }
        return false
    }

    fun removeShortcut(shortcut: String): Boolean
    {
        val holoIme = mHoloIME.get()
        if (holoIme != null)
        {
            return holoIme.removeShortcut(shortcut)
        }
        return false
    }

    fun changeShortcut(old: String, new: String, action: String): Boolean
    {
        val holoIme = mHoloIME.get()
        if (holoIme != null)
        {
            return holoIme.changeShortcut(old, new, action)
        }
        return false
    }

    fun changeShortcut(old: Shortcut, new: Shortcut): Boolean
    {
        val holoIme = mHoloIME.get()
        if (holoIme != null)
        {
            return holoIme.changeShortcut(old, new)
        }
        return false
    }
}
