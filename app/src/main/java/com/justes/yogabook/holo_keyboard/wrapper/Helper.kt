package com.justes.yogabook.holo_keyboard.wrapper

import android.content.Context

class Helper
{
    companion object
    {
        val LAST_LAYOUT_KEY = "last_layout"
        val NOTIFICATION_POS_X_KEY = "notification_pos_x"
        val NOTIFICATION_POS_Y_KEY = "notification_pos_y"

        fun getString(context: Context?, name: String?): String
        {
            if (context == null || name == null)
            {
                return ""
            }
            val id = context.resources.getIdentifier(name, "string", context.packageName)
            if (id == 0)
            {
                return ""
            }
            return context.resources.getString(id)
        }
        fun parseEnabledLanguages(languages: String): MutableList<String>
        {
            return languages.split(",").toMutableList()
        }
    }
}