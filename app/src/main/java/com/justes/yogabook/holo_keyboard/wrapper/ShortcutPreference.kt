package com.justes.yogabook.holo_keyboard.wrapper

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.preference.DialogPreference
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast


class ShortcutPreference(context: Context): DialogPreference(context)
{
    private lateinit var mTextView: TextView
    private lateinit var mBroadcastReceiver: CustomBroadcastReceiver
    lateinit var applicationWrapper: ApplicationWrapper
    lateinit var action: String

    private var mCurrentShortcut = ""

    private lateinit var mTitleView: TextView
    private lateinit var mDeleteView: ImageView
    private var mSummaryView: TextView? = null
    private var mSummary = ""


    companion object
    {
        const val SHORTCUT_CAPTURED_INTENT = "com.justes.yogabook.holo_keyboard.wrapper.SHORTCUT_CAPTURED"
        const val SHORTCUT_CAPTURED_INTENT_EXTRA = "SHORTCUT"

        fun translateShortcut(context: Context, shortcut: String): String
        {
            val tokens = shortcut.split(",").map { s ->
                val tr = Helper.getString(context, s.toLowerCase() + "_label")
                if (tr.isEmpty())
                {
                    s
                }
                else
                {
                    tr
                }
            }
            var res = ""
            if (tokens.isNotEmpty())
            {
                val sb = StringBuilder(tokens.first())
                for (s in 1 until tokens.size)
                {
                    sb.append(" + ${tokens[s]}")
                }
                res = sb.toString()
            }
            if (res.isNotEmpty())
            {
                return res
            }
            return context.resources.getString(R.string.none_label)
        }
    }




    fun translateShortcut(shortcut: String): String
    {
        return ShortcutPreference.translateShortcut(context, shortcut)
    }

    override fun onCreateView(parent: ViewGroup?): View
    {
        layoutResource = R.layout.language_item
        dialogLayoutResource = R.layout.text_preference_item
        //icon = context.resources.getDrawable(R.drawable.ic_delete_forever_black_24dp)
        //setIcon(R.drawable.ic_delete_forever_black_24dp)
        return super.onCreateView(parent)
    }

    override fun onBindView(view: View?)
    {
        super.onBindView(view)
        mTitleView = view!!.findViewById(R.id.title) as TextView
        mDeleteView = view.findViewById(R.id.deleteItem) as ImageView
        mSummaryView = view.findViewById(R.id.summary) as TextView
        mTitleView.text = title
        mSummaryView?.text = mSummary
        (view.findViewById(R.id.moveItem) as ImageView).visibility = View.INVISIBLE

        mDeleteView.setOnClickListener { removeShortcut() }
    }

    private fun setCurrentShortcut()
    {
        val previousShortcut = getPersistedString("")
        if (previousShortcut != mCurrentShortcut)
        {
            callChangeListener(mCurrentShortcut)
            persistString(mCurrentShortcut)

            if (previousShortcut.isEmpty())
            {
                applicationWrapper.addShortcut(mCurrentShortcut, action)
            }
            else if (previousShortcut.isNotEmpty() && mCurrentShortcut.isEmpty())
            {
                applicationWrapper.removeShortcut(previousShortcut)
            }
            else if (previousShortcut.isNotEmpty() && mCurrentShortcut.isNotEmpty())
            {
                applicationWrapper.changeShortcut(previousShortcut, mCurrentShortcut, action)
            }
        }
    }

    private fun removeShortcut()
    {
        mCurrentShortcut = ""
        setCurrentShortcut()
    }

    override fun setSummary(summary: CharSequence?)
    {
        mSummary = summary.toString()
        mSummaryView?.text = summary
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder?)
    {
        builder?.setOnKeyListener(
                                  {
                                      _, keyCode, event ->
                                      var ret = false
                                      if (event.action == KeyEvent.ACTION_DOWN && !event.hasNoModifiers() && !KeyEvent.isModifierKey(keyCode))
                                      {
                                          val shortcut = ShortcutFactory.create(event)?.toString() ?: ""
                                          val intent = Intent(SHORTCUT_CAPTURED_INTENT)
                                          intent.putExtra(SHORTCUT_CAPTURED_INTENT_EXTRA, shortcut)
                                          applicationWrapper.sendBroadcast(intent)
                                          ret = true
                                      }
                                      ret
                                  })
    }

    override fun onBindDialogView(view: View?)
    {
        super.onBindDialogView(view)
        mCurrentShortcut = ""
        mTextView = view!!.findViewById(R.id.text) as TextView
        (view.findViewById(R.id.title) as TextView).text = title
        mTextView.text = context.resources.getString(R.string.wait_fot_shortcut_label)
    }

    override fun showDialog(state: Bundle?)
    {
        super.showDialog(state)
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        mBroadcastReceiver = CustomBroadcastReceiver()
        applicationWrapper.registerReceiver(mBroadcastReceiver, IntentFilter(SHORTCUT_CAPTURED_INTENT))
    }

    override fun onDialogClosed(positiveResult: Boolean)
    {
        applicationWrapper.unregisterReceiver(mBroadcastReceiver)
        if (positiveResult)
        {
            setCurrentShortcut()
        }
    }

    private inner class CustomBroadcastReceiver : BroadcastReceiver()
    {
        override fun onReceive(context: Context?, intent: Intent?)
        {
            val action = intent?.action ?: ""
            when (action)
            {
                SHORTCUT_CAPTURED_INTENT ->
                {
                    val shortcut = intent?.getStringExtra(SHORTCUT_CAPTURED_INTENT_EXTRA) ?: ""
                    if (shortcut.isNotEmpty())
                    {
                        mTextView.text = translateShortcut(shortcut)
                        mCurrentShortcut = shortcut
                        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }
                    else
                    {
                        Toast.makeText(context, R.string.invalid_shortcut_label, Toast.LENGTH_SHORT).show()
                    }
                }
                else                     ->
                {
                    return
                }
            }
        }
    }
}