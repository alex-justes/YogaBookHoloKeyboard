package com.justes.yogabook.holo_keyboard.wrapper

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.view.MenuItem
import android.provider.Settings


/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity()
{
    private lateinit var mLayoutMap: HashMap<String, ApplicationWrapper.LayoutDescription>

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setupActionBar()
        mLayoutMap = (application as ApplicationWrapper).getLayoutMap()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar()
    {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean
    {
        return isLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>)
    {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean
    {
        return PreferenceFragment::class.java.name == fragmentName
                || LanguagesPreferenceFragment::class.java.name == fragmentName
                || ShortcutsPreferenceFragment::class.java.name == fragmentName
                || NotificationPreferenceFragment::class.java.name == fragmentName
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class LanguagesPreferenceFragment : PreferenceFragment()
    {
        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_layouts)
            (findPreference(resources.getString(R.string.available_layouts_key)) as AvailableLayouts).initialize((activity.application as ApplicationWrapper).getLayoutMap())
            (findPreference(resources.getString(R.string.enabled_layouts_key)) as EnabledLayouts).initialize((activity.application as ApplicationWrapper).getLayoutMap())
            setHasOptionsMenu(true)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean
        {
            val id = item.itemId
            if (id == android.R.id.home)
            {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class NotificationPreferenceFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener
    {

        private val PERMISSIONS_REQUEST_SYSTEM_ALERT_WINDOW = 0
        private lateinit var FLOATING_NOTIFICATION_ENABLED_KEY: String

        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            FLOATING_NOTIFICATION_ENABLED_KEY = context.resources.getString(R.string.floating_notification_enabled_key)
            addPreferencesFromResource(R.xml.pref_notification)
            setHasOptionsMenu(true)
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onDestroy()
        {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean
        {
            val id = item.itemId
            if (id == android.R.id.home)
            {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
        {
            if (key == FLOATING_NOTIFICATION_ENABLED_KEY && sharedPreferences != null)
            {
                if (sharedPreferences.getBoolean(FLOATING_NOTIFICATION_ENABLED_KEY, false))
                {
                    if (!Settings.canDrawOverlays(context))
                    {
                        askPermission()
                    }
                    else
                    {
                        context.startService(Intent(context, FloatingNotification::class.java))
                    }
                }
                else
                {
                    context.stopService(Intent(context, FloatingNotification::class.java))
                }
            }
        }

        private fun askPermission()
        {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"))
            startActivityForResult(intent, PERMISSIONS_REQUEST_SYSTEM_ALERT_WINDOW)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
        {
            when (requestCode)
            {
                PERMISSIONS_REQUEST_SYSTEM_ALERT_WINDOW ->
                {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                    if (preferences.getBoolean(FLOATING_NOTIFICATION_ENABLED_KEY, false)
                            && Settings.canDrawOverlays(context))
                    {
                        context.startService(Intent(context, FloatingNotification::class.java))
                    }
                }
                else ->
                {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class ShortcutsPreferenceFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener
    {
        private lateinit var SWITCH_TO_NEXT_LAYOUT_SHORTCUT_KEY: String
        private lateinit var ENABLED_LANGUAGES_KEY: String // = resources.getString(R.string.enabled_layouts_key)
        private lateinit var mOldSwitch2NextLayoutShortcut: String

        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)

            SWITCH_TO_NEXT_LAYOUT_SHORTCUT_KEY = context.resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_key)
            ENABLED_LANGUAGES_KEY = resources.getString(R.string.enabled_layouts_key)

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.registerOnSharedPreferenceChangeListener(this)
            mOldSwitch2NextLayoutShortcut = preferences.getString(SWITCH_TO_NEXT_LAYOUT_SHORTCUT_KEY, "")
            preferences.registerOnSharedPreferenceChangeListener(this)

            preferenceScreen = preferenceManager.createPreferenceScreen(context)

            val layoutSwitchingCat = PreferenceCategory(context)
            layoutSwitchingCat.title = resources.getString(R.string.pref_shortcuts_layout_switching_label)
            preferenceScreen.addPreference(layoutSwitchingCat)


            val switchingModePref = ListPreference(context)
            switchingModePref.setDefaultValue(resources.getString(R.string.shortcuts_layout_switching_mode_globaly_val))
            switchingModePref.entries = resources.getStringArray(R.array.pref_shortcuts_layout_switching_mode_titles)
            switchingModePref.entryValues = resources.getStringArray(R.array.pref_shortcuts_layout_switching_mode_values)
            switchingModePref.key = resources.getString(R.string.layout_switching_mode_key)
            switchingModePref.negativeButtonText = null
            switchingModePref.positiveButtonText = null
            switchingModePref.title = resources.getString(R.string.pref_shortcuts_layout_switching_mode_label)

            layoutSwitchingCat.addPreference(switchingModePref)

            val switchingMainShortcutPref = ListPreference(context)
            val entryValuesMainShortcut = resources.getStringArray(R.array.pref_shortcuts_layout_switching_to_next_layout_shortcut_values)
            val entriesMainShortcut = entryValuesMainShortcut.map { s -> ShortcutPreference.translateShortcut(context, s) }
            switchingMainShortcutPref.setDefaultValue(resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_default))
            switchingMainShortcutPref.entries = entriesMainShortcut.toTypedArray()
            switchingMainShortcutPref.entryValues = entryValuesMainShortcut
            switchingMainShortcutPref.key = resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_key)
            switchingMainShortcutPref.negativeButtonText = null
            switchingMainShortcutPref.positiveButtonText = null
            switchingMainShortcutPref.title = resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_label)

            layoutSwitchingCat.addPreference(switchingMainShortcutPref)


            val genericShortcutsCat = PreferenceCategory(context)
            genericShortcutsCat.title = resources.getString(R.string.pref_shortcuts_generic_shortcuts_label)
            preferenceScreen.addPreference(genericShortcutsCat)

            val additionalShortcutsSize = Helper.parseEnabledLanguages(preferences.getString(ENABLED_LANGUAGES_KEY, "")).size
            for (i in 1..additionalShortcutsSize)
            {
                val additionalShortcut = ShortcutPreference(context)
                additionalShortcut.setDefaultValue("")
                additionalShortcut.isPersistent = true
                additionalShortcut.applicationWrapper = activity.application as ApplicationWrapper
                additionalShortcut.key = resources.getString(R.string.pref_shortcuts_layout_switching_to_some_layout_shortcut_key, i)
                additionalShortcut.action = "${HoloIME.SWITCH_TO_LAYOUT_COMMAND},$i"
                additionalShortcut.title = resources.getString(R.string.pref_shortcuts_layout_switching_to_some_layout_shortcut_label, i)
                genericShortcutsCat.addPreference(additionalShortcut)
                bindPreferenceSummaryToValue(additionalShortcut)
            }

            val showHideFloatingNotification = ShortcutPreference(context)
            showHideFloatingNotification.setDefaultValue("")
            showHideFloatingNotification.isPersistent = true
            showHideFloatingNotification.applicationWrapper = activity.application as ApplicationWrapper
            showHideFloatingNotification.key = resources.getString(R.string.pref_shortcuts_layout_switching_show_hide_floating_notification_key)
            showHideFloatingNotification.action = HoloIME.SHOW_HIDE_FLOATING_NOTIFICATION_COMMAND
            showHideFloatingNotification.title = resources.getString(R.string.pref_shortcuts_layout_switching_show_hide_floating_notification_label)


            genericShortcutsCat.addPreference(showHideFloatingNotification)

            bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.layout_switching_mode_key)))
            bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.pref_shortcuts_layout_switching_to_next_layout_shortcut_key)))
            bindPreferenceSummaryToValue(findPreference(resources.getString(R.string.pref_shortcuts_layout_switching_show_hide_floating_notification_key)))

            setHasOptionsMenu(true)
        }

        override fun onDestroy()
        {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            preferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean
        {
            val id = item.itemId
            if (id == android.R.id.home)
            {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
        {
            if (key == SWITCH_TO_NEXT_LAYOUT_SHORTCUT_KEY && sharedPreferences != null)
            {
                val newShortcut = sharedPreferences.getString(SWITCH_TO_NEXT_LAYOUT_SHORTCUT_KEY, "")
                if (newShortcut.isNotEmpty() && newShortcut != mOldSwitch2NextLayoutShortcut)
                {
                    (activity.application as ApplicationWrapper).changeShortcut(mOldSwitch2NextLayoutShortcut, newShortcut, HoloIME.SWITCH_TO_NEXT_LAYOUT_COMMAND)
                    mOldSwitch2NextLayoutShortcut = newShortcut
                }
            }
        }
    }

    companion object
    {

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            when(preference)
            {
                is ListPreference ->
                {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    val listPreference = preference
                    val index = listPreference.findIndexOfValue(stringValue)

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            if (index >= 0)
                                listPreference.entries[index]
                            else
                                null)
                }
                is ShortcutPreference ->
                {
                    preference.summary = ShortcutPreference.translateShortcut(preference.context, stringValue)
                }
                else ->
                {
                    preference.summary = stringValue
                }
            }
            true
        }


        private fun isLargeTablet(context: Context): Boolean
        {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference)
        {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }
}
