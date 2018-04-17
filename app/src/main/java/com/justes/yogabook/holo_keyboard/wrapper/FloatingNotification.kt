package com.justes.yogabook.holo_keyboard.wrapper

import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.*
import android.widget.TextView
import java.lang.ref.WeakReference

class FloatingNotification : Service(), SharedPreferences.OnSharedPreferenceChangeListener
{
    private companion object
    {
        val LANGUAGE_CHANGED_INTENT = "com.lenovo.holo_keyboard.intent.KEYBOARD_CHANGED"
        val CURRENT_LANGUAGE = "current_layout"
    }

    private lateinit var SHORT_2_SYMBOLS_ENABLED_KEY: String

    private lateinit var mWindowManager:     WindowManager
    private lateinit var mFloatingView:      View
    private lateinit var mBroadcastReceiver: BroadcastReceiver
    private lateinit var mTextView:          TextView
    private lateinit var mLayoutMap:         HashMap<String, ApplicationWrapper.LayoutDescription>
    private lateinit var mParams:            WindowManager.LayoutParams
    private lateinit var mPreferences:       SharedPreferences

    private var mShort2Symbols = false
    private var mCurrentLayout = ""

    override fun onBind(intent: Intent?): IBinder?
    {
        return null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
    {
        when (key)
        {
            SHORT_2_SYMBOLS_ENABLED_KEY ->
            {
                mShort2Symbols = sharedPreferences?.getBoolean(SHORT_2_SYMBOLS_ENABLED_KEY, false) ?: false
                setLayout(mCurrentLayout)
            }
            else                        ->
            {
                return
            }
        }
    }

    override fun onCreate()
    {
        super.onCreate()
        mLayoutMap = (application as ApplicationWrapper).getLayoutMap()
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)
        mTextView = mFloatingView.findViewById(R.id.textView) as TextView
        mPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        SHORT_2_SYMBOLS_ENABLED_KEY = applicationContext.resources.getString(R.string.short_2_symbols_enabled_key)

        mShort2Symbols = mPreferences.getBoolean(SHORT_2_SYMBOLS_ENABLED_KEY, false)

        mParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        mParams.gravity = Gravity.START or Gravity.TOP
        mParams.x = mPreferences.getInt(Helper.NOTIFICATION_POS_X_KEY, 0)
        mParams.y = mPreferences.getInt(Helper.NOTIFICATION_POS_Y_KEY, 0)

        mWindowManager.addView(mFloatingView, mParams)
        mPreferences.registerOnSharedPreferenceChangeListener(this)
        mFloatingView.setOnTouchListener(CustomOnTouchListener())
        mBroadcastReceiver = CustomBroadcastReceiver()
        registerReceiver(mBroadcastReceiver, IntentFilter(LANGUAGE_CHANGED_INTENT))
        setLayout(PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(Helper.LAST_LAYOUT_KEY, ""))
        (application as ApplicationWrapper).registerFloatingNotification(WeakReference(this))
    }

    override fun onDestroy()
    {
        (application as ApplicationWrapper).unregisterFloatingNotification()
        mPreferences.unregisterOnSharedPreferenceChangeListener(this)
        unregisterReceiver(mBroadcastReceiver)
        mWindowManager.removeView(mFloatingView)
        super.onDestroy()
    }

    private fun setLayout(layout: String)
    {
        var text = mLayoutMap[layout]?.short?.toUpperCase() ?: ""
        if (text.isEmpty())
        {
            return
        }
        mCurrentLayout = layout
        if (mShort2Symbols)
        {
            text = text.substring(0,2)
        }
        mTextView.text = text
    }

    private inner class CustomBroadcastReceiver: BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?)
        {
            val action = intent?.action ?: ""
            when (action)
            {
                LANGUAGE_CHANGED_INTENT ->
                {
                    setLayout(intent?.getStringExtra(CURRENT_LANGUAGE) ?: "")
                }
                else                    ->
                {
                    return
                }
            }
        }
    }

    private inner class CustomOnTouchListener: View.OnTouchListener
    {
        private var mInitialX = 0
        private var mInitialY = 0
        private var mInitialTouchX = .0f
        private var mInitialTouchY = .0f

        override fun onTouch(v: View?, event: MotionEvent?): Boolean
        {
            var ret = false
            if (event != null)
            {
                when (event.action)
                {
                    MotionEvent.ACTION_DOWN ->
                    {
                        mInitialX = mParams.x
                        mInitialY = mParams.y
                        mInitialTouchX = event.rawX
                        mInitialTouchY = event.rawY
                        ret =  true
                    }
                    MotionEvent.ACTION_MOVE ->
                    {
                        mParams.x = mInitialX + (event.rawX - mInitialTouchX).toInt()
                        mParams.y = mInitialY + (event.rawY - mInitialTouchY).toInt()
                        mWindowManager.updateViewLayout(mFloatingView, mParams)
                        ret = true
                    }
                    MotionEvent.ACTION_UP   ->
                    {
                        val location = IntArray(2)
                        mFloatingView.getLocationOnScreen(location)
                        mParams.x = location[0]
                        mParams.y = location[1]
                        mWindowManager.updateViewLayout(mFloatingView, mParams)
                        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                        preferences.edit().putInt(Helper.NOTIFICATION_POS_X_KEY, mParams.x).apply()
                        preferences.edit().putInt(Helper.NOTIFICATION_POS_Y_KEY, mParams.y).apply()
                        v?.performClick()
                        ret = true
                    }
                    else                    ->
                    {
                        ret =  false
                    }
                }
            }
            return ret
        }
    }
}