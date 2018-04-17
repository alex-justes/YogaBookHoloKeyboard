package com.justes.yogabook.holo_keyboard.wrapper


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import java.lang.ref.WeakReference


class HoloIME : InputMethodService()
{
    private lateinit var FLOATING_NOTIFICATION_ENABLED_KEY: String
    private lateinit var SHOW_HIDE_FLOATING_NOTIFICATION_SHORTCUT_KEY: String
    private lateinit var mLayoutSwitcher: LayoutSwitcher
    private val mShortcuts = mutableListOf<Shortcut>()

    private lateinit var mPreferences: SharedPreferences

    companion object
    {
        const val SWITCH_TO_NEXT_LAYOUT_COMMAND = "SWITCH_TO_NEXT_LAYOUT"
        const val SWITCH_TO_LAYOUT_COMMAND = "SWITCH_TO_LAYOUT"
        const val SHOW_HIDE_FLOATING_NOTIFICATION_COMMAND = "SHOW_HIDE_FLOATING_NOTIFICATION"
    }

    override fun onCreate()
    {
        super.onCreate()

        FLOATING_NOTIFICATION_ENABLED_KEY = resources.getString(R.string.floating_notification_enabled_key)
        SHOW_HIDE_FLOATING_NOTIFICATION_SHORTCUT_KEY = resources.getString(R.string.pref_shortcuts_layout_switching_show_hide_floating_notification_key)

        mPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val showHideFloatingNotificationShortcut = ShortcutFactory.create(mPreferences.getString(SHOW_HIDE_FLOATING_NOTIFICATION_SHORTCUT_KEY, ""),
                                                                          toAction(SHOW_HIDE_FLOATING_NOTIFICATION_COMMAND))
        if (showHideFloatingNotificationShortcut != null)
        {
            mShortcuts.add(showHideFloatingNotificationShortcut)
        }

        mLayoutSwitcher = LayoutSwitcher(applicationContext)

        mPreferences.registerOnSharedPreferenceChangeListener(mLayoutSwitcher)

        showNotification(true)
        mLayoutSwitcher.reset()


        (application as ApplicationWrapper).registerHoloIME(WeakReference(this))
    }

    fun showNotification(show: Boolean)
    {
        if (show && mPreferences.getBoolean(FLOATING_NOTIFICATION_ENABLED_KEY, false) && Settings.canDrawOverlays(this))
        {
            startService(Intent(this@HoloIME, FloatingNotification::class.java))
        }
        else if (!show)
        {
            stopService(Intent(this@HoloIME, FloatingNotification::class.java))
        }
    }

    private fun toAction(description: String): ShortcutAction?
    {
        val tokens = description.split(",")
        if (tokens.isEmpty())
        {
            return null
        }
        val command = tokens.first()
        when (command)
        {
            SWITCH_TO_NEXT_LAYOUT_COMMAND ->
            {
                return object: ShortcutAction
                {
                    override val command: String
                        get() = command

                    override fun act(): Boolean
                    {
                        mLayoutSwitcher.next()
                        return true
                    }
                }
            }
            SWITCH_TO_LAYOUT_COMMAND ->
            {
                if (tokens.size < 2)
                {
                    return null
                }
                val layout = tokens[1].toInt() - 1
                return object: ShortcutAction
                {
                    override val command: String
                        get() = command
                    override fun act(): Boolean
                    {
                        mLayoutSwitcher.set(layout)
                        return true
                    }
                }
            }
            SHOW_HIDE_FLOATING_NOTIFICATION_COMMAND ->
            {
                return object: ShortcutAction
                {
                    override val command: String
                        get() = command
                    override fun act(): Boolean
                    {
                        if ((application as ApplicationWrapper).isFloatingNotificationActive())
                        {
                            showNotification(false)
                        }
                        else
                        {
                            showNotification(true)
                        }
                        return true
                    }
                }
            }
            else ->
            {
                return null
            }
        }
    }

    fun addShortcut(shortcut: String, action: String): Boolean
    {
        val sc = ShortcutFactory.create(shortcut, toAction(action))
        if (sc != null)
        {
            addShortcut(sc)
        }
        return false
    }

    @Synchronized
    fun addShortcut(shortcut: Shortcut): Boolean
    {
        mShortcuts.remove(shortcut)
        mShortcuts.add(shortcut)
        return true
    }

    fun removeShortcut(shortcut: String): Boolean
    {
        val sc = ShortcutFactory.create(shortcut)
        if (sc != null)
        {
            removeShortcut(sc)
        }
        return false
    }

    @Synchronized
    fun removeShortcut(shortcut: Shortcut): Boolean
    {
        mShortcuts.remove(shortcut)
        return true
    }

    fun changeShortcut(old: String, new: String, action: String): Boolean
    {
        val oldShortcut = ShortcutFactory.create(old)
        val newShortcut = ShortcutFactory.create(new, toAction(action))
        if (oldShortcut != null && newShortcut != null)
        {
            return changeShortcut(oldShortcut, newShortcut)
        }
        return false
    }

    @Synchronized
    fun changeShortcut(old: Shortcut, new: Shortcut): Boolean
    {
        val idxOld = mShortcuts.indexOf(old)
        val idxNew = mShortcuts.indexOf(new)
        // Just in case
        if (idxNew != -1)
        {
            mShortcuts.removeAt(idxNew)
        }
        if (idxOld != -1)
        {
            mShortcuts[idxOld] = new
        }
        else
        {
            mShortcuts.add(new)
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
    {
        if (event != null && !event.isCanceled)
        {
            val result = synchronized(this)
                {
                    var result = false
                    for (shortcut in mShortcuts)
                    {
                        if (shortcut.canAct(event))
                        {
                            result = shortcut.act()
                            if (result)
                            {
                                break
                            }
                        }
                    }
                    result
                }
            if (result)
            {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean)
    {
        val pkgName = attribute?.packageName ?: ""
        mLayoutSwitcher.setPkg(pkgName)
        super.onStartInput(attribute, restarting)
    }

    override fun onDestroy()
    {
        (application as ApplicationWrapper).unregisterHoloIME()
        mPreferences.unregisterOnSharedPreferenceChangeListener(mLayoutSwitcher)
        super.onDestroy()
    }

    inner class LayoutSwitcher(val context: Context): SharedPreferences.OnSharedPreferenceChangeListener
    {
        private val ENABLED_LANGUAGES_KEY = resources.getString(R.string.enabled_layouts_key)
        private val LAYOUT_SWITCHING_MODE_KEY = resources.getString(R.string.layout_switching_mode_key)
        private val SWITCH_TO_NEXT_LAYOUT_SHORTCUT_KEY = resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_key)
        private val mPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        private val SWITCH_PER_APPLICATION = 0
        private val SWITCH_GLOBALY = 1

        private val SWITCH_PER_APPLICATION_STR = context.resources.getString(R.string.shortcuts_layout_switching_mode_per_application_val)
        private val SWITCH_GLOBALY_STR = context.resources.getString(R.string.shortcuts_layout_switching_mode_globaly_val)

        private val SWITCH_LAYOUT_INTENT = "com.cootek.smartinputv5.intent.LANGUAGE_CHANGED"
        private val CURRENT_LANGUAGE_EXTRA = "CURRENT_LANGUAGE"

        private var mSwitchMode = SWITCH_GLOBALY
        private var mCurrentPkgName = ""

        private var mCurrentLayout = 0
        private var mPkgLayout = mutableMapOf<String, Int>()
        private var mEnabledLayouts = Helper.parseEnabledLanguages(mPreferences.getString(ENABLED_LANGUAGES_KEY, ""))

        init
        {
            mShortcuts.add(ShortcutFactory.create(mPreferences.getString(SWITCH_TO_NEXT_LAYOUT_SHORTCUT_KEY, ""),
                                                  toAction(SWITCH_TO_NEXT_LAYOUT_COMMAND)) as Shortcut)

            resetGenericShortcutsForLayoutSwitching()
        }

        private fun resetGenericShortcutsForLayoutSwitching()
        {
            mShortcuts.removeAll(mShortcuts.filter { s -> s.action.command == HoloIME.SWITCH_TO_LAYOUT_COMMAND })
            for (i in 1 .. mEnabledLayouts.size)
            {
                val key = context.resources.getString(R.string.pref_shortcuts_layout_switching_to_some_layout_shortcut_key, i)
                val action = "${HoloIME.SWITCH_TO_LAYOUT_COMMAND},$i"
                val sc = mPreferences.getString(key, "")
                val act = toAction(action)
                val shortcut = ShortcutFactory.create(mPreferences.getString(key, ""), toAction(action))
                if (shortcut != null)
                {
                    mShortcuts.add(shortcut)
                }
            }
        }

        fun enabledLayoutsSize(): Int
        {
            return mEnabledLayouts.size
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
        {
            when (key)
            {
                ENABLED_LANGUAGES_KEY ->
                {
                    val newLayouts = Helper.parseEnabledLanguages(sharedPreferences?.getString(key, "")
                                                                          ?: "" )
                    for (k in mPkgLayout.keys)
                    {
                        val currentLayout = mEnabledLayouts[mPkgLayout[k] ?: 0]
                        var layoutIdx = newLayouts.indexOf(currentLayout)
                        if (layoutIdx == -1)
                        {
                            layoutIdx = 0
                        }
                        mPkgLayout[k] = layoutIdx
                    }
                    val currentLayout = mEnabledLayouts[mCurrentLayout]
                    var layoutIdx = newLayouts.indexOf(currentLayout)
                    if (layoutIdx == -1)
                    {
                        layoutIdx = 0
                    }
                    mCurrentLayout = layoutIdx
                    mEnabledLayouts = newLayouts
                    resetGenericShortcutsForLayoutSwitching()
                    current()
                }
                LAYOUT_SWITCHING_MODE_KEY ->
                {
                    when(sharedPreferences?.getString(key, ""))
                    {
                        SWITCH_PER_APPLICATION_STR ->
                        {
                            mSwitchMode = SWITCH_PER_APPLICATION
                        }
                        SWITCH_GLOBALY_STR ->
                        {
                            mSwitchMode = SWITCH_GLOBALY
                        }
                    }
                }
                else                  ->
                {
                    return
                }
            }
        }

        fun setPkg(pkgName: String)
        {
            if (mSwitchMode != SWITCH_PER_APPLICATION)
            {
                return
            }
            val tmpCurrentPkgName = mCurrentPkgName
            mCurrentPkgName = pkgName
            if (!mPkgLayout.containsKey(pkgName))
            {
                mPkgLayout[pkgName] = 0
            }
            if (mSwitchMode == SWITCH_PER_APPLICATION)
            {
                if (tmpCurrentPkgName != pkgName)
                {
                    current()
                }
            }
        }

        fun current()
        {
            updateLayout()
        }

        fun next()
        {
            when (mSwitchMode)
            {
                SWITCH_PER_APPLICATION ->
                {
                    if (mPkgLayout.containsKey(mCurrentPkgName))
                    {
                        val v = mPkgLayout[mCurrentPkgName] ?: 0
                        mPkgLayout[mCurrentPkgName] = v + 1
                    }
                }
                SWITCH_GLOBALY ->
                {
                    mCurrentLayout += 1
                }
            }
            updateLayout()
        }
        fun set(layout: Int)
        {
            when (mSwitchMode)
            {
                SWITCH_PER_APPLICATION ->
                {
                    if (mPkgLayout.containsKey(mCurrentPkgName))
                    {
                        mPkgLayout[mCurrentPkgName] = layout
                    }
                }
                SWITCH_GLOBALY         ->
                {
                    mCurrentLayout = layout
                }
            }
            updateLayout()
        }
        fun reset()
        {
            set(0)
        }
        private fun updateLayout()
        {
            var layout = 0
            when (mSwitchMode)
            {
                SWITCH_PER_APPLICATION ->
                {
                    if (mPkgLayout.containsKey(mCurrentPkgName))
                    {
                        layout = mPkgLayout[mCurrentPkgName] ?: 0
                        if (layout >= mEnabledLayouts.size)
                        {
                            mPkgLayout[mCurrentPkgName] = 0
                            layout = 0
                        }
                    }
                }
                SWITCH_GLOBALY         ->
                {
                    if (mCurrentLayout >= mEnabledLayouts.size)
                    {
                        mCurrentLayout = 0
                    }
                    layout = mCurrentLayout
                }
            }
            val intent = Intent(SWITCH_LAYOUT_INTENT)
            intent.putExtra(CURRENT_LANGUAGE_EXTRA, mEnabledLayouts[layout])
            context.sendBroadcast(intent)
            mPreferences.edit().putString(Helper.LAST_LAYOUT_KEY, mEnabledLayouts[layout]).apply()
        }
    }
}