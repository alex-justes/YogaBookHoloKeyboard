package com.justes.yogabook.holo_keyboard.wrapper

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceManager
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*

class EnabledLayouts(private val mContext: Context, private val mAttrs: AttributeSet) : Preference(mContext, mAttrs),
                                                                                        SharedPreferences.OnSharedPreferenceChangeListener
{
    private lateinit var mEnabledLayoutsView: RecyclerView
    private lateinit var mAdapter: CustomAdapter
    private lateinit var mLayoutMap: HashMap<String, ApplicationWrapper.LayoutDescription>
    private lateinit var mEnabledLayouts: MutableList<String>
    private lateinit var mItemTouchHelper: ItemTouchHelper

    private val AVAILABLE_LAYOUTS_KEY = mContext.resources.getString(R.string.available_layouts_key)

    fun initialize(layoutMap: HashMap<String, ApplicationWrapper.LayoutDescription>)
    {
        mLayoutMap = layoutMap
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?)
    {
        if (key == AVAILABLE_LAYOUTS_KEY && sharedPreferences != null)
        {
            val newLayout = sharedPreferences.getString(AVAILABLE_LAYOUTS_KEY, "")
            if (newLayout.isNotEmpty())
            {
                sharedPreferences.edit().putString(AVAILABLE_LAYOUTS_KEY, "").apply()
                if (!mEnabledLayouts.contains(newLayout))
                {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                    val enabledLayouts = preferences.getString(this.key, "")
                    persistString("$enabledLayouts,$newLayout")
                    mEnabledLayouts.add(newLayout)
                    mAdapter.notifyItemInserted(mEnabledLayouts.size - 1)
                }
            }
        }
    }

    private fun save()
    {
        persistString(mEnabledLayouts.joinToString(","))
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?)
    {
        val enabledLayouts: String
        if (restorePersistedValue)
        {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            enabledLayouts = preferences.getString(key, "")
        }
        else
        {
            enabledLayouts = defaultValue.toString()
            if (shouldPersist())
            {
                persistString(defaultValue.toString())
            }
        }
        mEnabledLayouts = Helper.parseEnabledLanguages(enabledLayouts)
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any
    {
        return a?.getString(index) ?: ""
    }

    override fun onBindView(view: View?)
    {
        super.onBindView(view)
        val viewManager = LinearLayoutManager(context)
        val itemDecoration = DividerItemDecoration(context, LinearLayout.VERTICAL)
        mEnabledLayoutsView = view!!.findViewById(R.id.enabledLanguagesView)
        mEnabledLayoutsView.addItemDecoration(itemDecoration)
        mAdapter = CustomAdapter()
        mEnabledLayoutsView.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = mAdapter
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)
        mItemTouchHelper = ItemTouchHelper(CustomItemTouchHelperCallback(mAdapter))
        mItemTouchHelper.attachToRecyclerView(mEnabledLayoutsView)
    }

    override fun onPrepareForRemoval()
    {
        super.onPrepareForRemoval()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateView(parent: ViewGroup?): View
    {
        layoutResource = R.layout.enabled_languages_preference_item
        return super.onCreateView(parent)
    }

    inner class CustomAdapter : RecyclerView.Adapter<ViewHolder>()
    {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
        {
            val sampleView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.language_item, parent, false)
            return ViewHolder(sampleView)
        }

        override fun getItemCount(): Int
        {
            return mEnabledLayouts.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int)
        {
            val layout = mEnabledLayouts[position]
            val layoutInfo = mLayoutMap[layout]
            holder.textViewLabel.text = Helper.getString(mContext, layoutInfo?.label)
            holder.textViewShort.text = layoutInfo?.short?.toUpperCase() ?: ""
            holder.deleteItem.setOnClickListener({ _ -> remove(layout) })

            @Suppress("")
            // Redundant warning
            holder.moveItem.setOnTouchListener { v, event ->
                                                   if (mEnabledLayouts.size > 1 && event?.action == MotionEvent.ACTION_DOWN)
                                                   {
                                                       mItemTouchHelper.startDrag(holder)
                                                       v?.performClick()
                                                       return@setOnTouchListener true
                                                   }
                                                   return@setOnTouchListener false
                                               }
        }

        fun move(from: Int, to: Int): Boolean
        {
            mEnabledLayouts[from] = mEnabledLayouts[to].also { mEnabledLayouts[to] = mEnabledLayouts[from]}
            notifyItemMoved(from, to)
            save()
            return true
        }

        fun remove(layout: String): Boolean
        {
            val idx = mEnabledLayouts.indexOf(layout)
            if (idx != -1)
            {
                return remove(idx)
            }
            return false
        }

        fun remove(position: Int): Boolean
        {
            if (mEnabledLayouts.size > 1)
            {
                mEnabledLayouts.removeAt(position)
                notifyItemRemoved(position)
                save()
                return true
            }
            return false
        }
        fun isSwipeAllowed(): Boolean
        {
            return mEnabledLayouts.size > 1
        }
    }

    class CustomItemTouchHelperCallback(private val mAdapter: CustomAdapter):
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                                           ItemTouchHelper.START or ItemTouchHelper.END)
    {
        override fun isItemViewSwipeEnabled(): Boolean
        {
            return mAdapter.isSwipeAllowed()
        }

        override fun isLongPressDragEnabled(): Boolean
        {
            return true
        }

        override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean
        {
            mAdapter.move(viewHolder!!.adapterPosition, target!!.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int)
        {
            mAdapter.remove(viewHolder!!.adapterPosition)
        }
    }


    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
    {
        val textViewLabel = view.findViewById(R.id.title) as TextView
        val textViewShort = view.findViewById(R.id.summary) as TextView
        val deleteItem = view.findViewById(R.id.deleteItem) as ImageView
        val moveItem = view.findViewById(R.id.moveItem) as ImageView
    }
}

class AvailableLayouts(private val mContext: Context, attrs: AttributeSet) : ListPreference(mContext, attrs)
{

    private lateinit var mEntryValues: Array<String>
    private lateinit var mEntries: Array<String>
    private lateinit var mLayoutMap: HashMap<String, ApplicationWrapper.LayoutDescription>
    private val ENABLED_LAYOUTS_KEY = mContext.resources.getString(R.string.enabled_layouts_key)

    fun initialize(layoutMap: HashMap<String, ApplicationWrapper.LayoutDescription>)
    {
        mLayoutMap = layoutMap
        mEntryValues = mLayoutMap.keys.toTypedArray()
        mEntries = Array(mLayoutMap.size, { _ -> "" })
        mEntryValues.sortBy { v -> Helper.getString(mContext, mLayoutMap[v]?.label) }
        for (i in 0 until mEntries.size)
        {
            mEntries[i] = Helper.getString(mContext, mLayoutMap[mEntryValues[i]]?.label)
        }
    }

    override fun onCreateView(parent: ViewGroup?): View
    {
        layoutResource = R.layout.button_preference_item
        val view = super.onCreateView(parent)
        (view.findViewById(R.id.button) as Button).setOnClickListener({ onClick() })
        return view
    }


    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder?)
    {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val enabledLayouts = Helper.parseEnabledLanguages(preferences.getString(ENABLED_LAYOUTS_KEY, ""))
        entries =  Array(mEntries.size - enabledLayouts.size, { _ -> "" })
        entryValues =  Array(mEntries.size - enabledLayouts.size, { _ -> "" })
        var c = 0
        for (i in 0 until mEntries.size)
        {
            if (!enabledLayouts.contains(mEntryValues[i]))
            {
                entries[c] = mEntries[i]
                entryValues[c] = mEntryValues[i]
                c += 1
            }
        }
        builder?.setAdapter(CustomListAdapter(), { _, _ -> })
        builder?.setSingleChoiceItems(mEntries, -1, { _, _ -> })
        builder?.setPositiveButton(null, null)
    }

    override fun showDialog(state: Bundle?)
    {
        super.showDialog(state)
        val dialog = (dialog as AlertDialog)
        val listView = dialog.listView
        listView.divider = ColorDrawable(Color.DKGRAY)
        listView.dividerHeight = 1
    }

    private fun setLayout(position: Int)
    {
        persistString(entryValues[position].toString())
    }

    inner class CustomListAdapter : BaseAdapter()
    {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View
        {
            val view: View
            if (convertView != null)
            {
                view = convertView
            }
            else
            {
                view = LayoutInflater.from(parent?.context).inflate(R.layout.language_item, parent, false)
                (view.findViewById(R.id.moveItem) as View).visibility = View.INVISIBLE
                (view.findViewById(R.id.deleteItem) as View).visibility = View.INVISIBLE
            }
            view.setOnClickListener(
                    { _ ->
                        setLayout(position)
                        dialog.dismiss()
                    }
            )
            (view.findViewById(R.id.title) as TextView).text = entries[position]
            (view.findViewById(R.id.summary) as TextView).text = mLayoutMap[entryValues[position]]?.short?.toUpperCase() ?: ""
            return view
        }

        override fun getItem(position: Int): Any
        {
            return entryValues[position]
        }

        override fun getItemId(position: Int): Long
        {
            return position.toLong()
        }

        override fun getCount(): Int
        {
            return entries.size
        }

    }
}


