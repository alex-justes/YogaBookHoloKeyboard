package com.justes.yogabook.holo_keyboard.wrapper

import android.util.Log
import android.view.KeyEvent
import java.lang.StringBuilder

open class Key internal constructor(val code: Int, val name: String)
{
    companion object
    {
        const val KEYCODE_NONE = 0
    }
}

class ModifierKey internal constructor(code: Int, name: String, val mask: Int) : Key(code, name)
{}

private class ModifierHelper private constructor()
{
    companion object
    {
        const val PREFIX = "metaState=META_"
        const val SUFFIX = "_ON"
        private const val MASK_FILTER = (KeyEvent.META_ALT_ON or KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON).inv()
        private var mName2Mask = mutableMapOf<String, Int>()
        private var mMask2Name = mutableMapOf<Int, String>()
        init
        {
            val mask = KeyEvent.getModifierMetaStateMask().toString(2).reversed()
            for (i in 0 until mask.length)
            {
                if (mask[i] == '1')
                {
                    val maskValue = 1 shl i
                    val ke = KeyEvent(0, 0, 0, 0, 0, maskValue)
                    val tokens = ke.toString().split(",")
                    for (token in tokens)
                    {
                        val t = token.trim()
                        if (t.startsWith(PREFIX) && t.endsWith(SUFFIX))
                        {
                            val keyName = t.removeSurrounding(PREFIX, SUFFIX)
                            mName2Mask[keyName] = maskValue
                            mMask2Name[maskValue] = keyName
                        }
                    }
                }
            }
        }

        fun name2mask(keyName: String): Int
        {
            return mName2Mask[keyName] ?: 0
        }
        fun mask2string(mask: Int): String
        {
            val filteredMask = mask and MASK_FILTER
            val sb = StringBuilder()
            val maskStr = filteredMask.toString(2).reversed()
            for (i in maskStr.length - 1 downTo 0)
            {
                if (maskStr[i] == '1')
                {
                    val maskValue = 1 shl i
                    val keyName = mMask2Name[maskValue] ?: ""
                    if (keyName.isNotEmpty())
                    {
                        sb.append("$keyName,")
                    }
                }
            }
            val s = sb.toString()
            return if (s.isEmpty()) "" else s.substring(0, s.length - 1)
        }
    }
}

private class KeyFactory private constructor()
{
    companion object
    {
        const val VALID_PREFIX = "KEYCODE_"
        private fun isValidKeyName(name: String): Boolean
        {
            return name.startsWith(VALID_PREFIX)
        }

        fun createKey(keyCode: Int): Key?
        {
            var keyName = KeyEvent.keyCodeToString(keyCode)
            var key: Key? = null
            if (isValidKeyName(keyName))
            {
                keyName = keyName.removePrefix(VALID_PREFIX)
                key = if (KeyEvent.isModifierKey(keyCode))
                { ModifierKey(keyCode, keyName, ModifierHelper.name2mask(keyName)) }
                else
                { Key(keyCode, keyName) }
            }
            return key
        }
    }
}

interface ShortcutAction
{
    val command: String get() = ""
    fun act(): Boolean {return false}
}


class Shortcut(private val mModifiers: List<ModifierKey>, private val mKey: Key, val action: ShortcutAction)
{
    private val mMask = mModifiers.fold(0, { v, k -> v or k.mask })

    override fun equals(other: Any?): Boolean
    {
        if (other is Shortcut)
        {
            return mMask == other.mMask && mKey.code == other.mKey.code
        }
        return super.equals(other)
    }

    override fun toString(): String
    {
        return ModifierHelper.mask2string(mMask) + if (mKey.name.isNotEmpty()) ",${mKey.name}" else ""
    }

    fun canAct(event: KeyEvent): Boolean
    {
        return event.hasModifiers(mMask) && when (mKey.code)
        {
            Key.KEYCODE_NONE ->
            {
                KeyEvent.isModifierKey(event.keyCode)
            }
            else ->
            {
                event.keyCode == mKey.code
            }
        }
    }
    fun act(): Boolean
    {
        return action.act()
    }
}

class ShortcutFactory private constructor()
{
    companion object
    {
        private const val TAG = "HOLO_SC"
        private val mName2Key: Map<String, Key>
        private val mCode2Key: Map<Int, Key>
        private val KEY_NONE = Key(Key.KEYCODE_NONE, "")
        init
        {
            val name2Key = mutableMapOf<String, Key>()
            val code2Key = mutableMapOf<Int, Key>()
            KeyEvent.KEYCODE_VOLUME_DOWN
            for (keyCode in KeyEvent.KEYCODE_UNKNOWN + 1 .. KeyEvent.getMaxKeyCode())
            {
                val k = KeyFactory.createKey(keyCode)
                if (k != null)
                {
                    name2Key[k.name] = k
                    code2Key[k.code] = k
                }
            }
            mName2Key = name2Key
            mCode2Key = code2Key
        }
        fun create(description: String, action: ShortcutAction? = object: ShortcutAction {}): Shortcut?
        {
            if (action == null)
            {
                Log.d(TAG, "Action is null")
                return null
            }
            val modifiers = mutableListOf<ModifierKey>()
            val keys = mutableListOf<Key>()
            for (token in description.split(","))
            {
                val key = mName2Key[token]
                when (key)
                {
                    is ModifierKey ->
                    {
                        if (!modifiers.contains(key))
                            modifiers.add(key)
                    }
                    is Key ->
                    {
                        if (!keys.contains(key))
                            keys.add(key)
                    }
                    else ->
                    {
                        Log.d(TAG, "Invalid key: $token")
                    }
                }
            }
            if (keys.size > 1 || modifiers.isEmpty() || (modifiers.size == 1 && keys.isEmpty()))
            {
                Log.d(TAG, "Invalid shortcut description: $description")
                return null
            }
            // Special case of SHIFT and any printable key
            if (modifiers.size == 1 && keys.size == 1)
            {
                val modifierEvent = KeyEvent(0, 0,0,0,0, KeyEvent.normalizeMetaState(modifiers.last().mask))
                val keyEvent = KeyEvent(0, keys.last().code)
                if (modifierEvent.isShiftPressed && keyEvent.isPrintingKey)
                {
                    return null
                }
            }
            val key = if (keys.isEmpty()) KEY_NONE else keys.last()
            return Shortcut(modifiers, key, action)
        }
        fun create(event: KeyEvent): Shortcut?
        {
            val keyString =
                if (!KeyEvent.isModifierKey(event.keyCode) && mCode2Key.containsKey(event.keyCode))
                {
                    ",${mCode2Key[event.keyCode]!!.name}"
                }
                else
                {
                    ""
                }
            val eventString = ModifierHelper.mask2string(event.modifiers) + keyString
            return create(eventString)
        }
    }
}