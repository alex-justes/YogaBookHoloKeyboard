# YogaBook Holo Keyboard

This application will turn your Holo Keyboard into usual physical keyboard (no touchpal, no smart input, etc).

It will allow you to type faster (cause of lack of smart features), and to switch to another layouts with custom shortcuts.

It comes with movable floating notification with current keyboard layout.

No root required.

## Notice

IT IS NOT A VIRTUAL KEYBOARD!

You need to switch to other input method (swift keyboard, gboard, etc) to use virtual keyboard.

This only will work on Lenovo Yoga Book YB1 with Android onboard.

It was tested on YB1-X90L with Nougat 7.1.1 (software version: YB-X90L_171013) but it should also work on F version and older android (probably).

## Features

1. Switch to different layout (supported by holo keyboard)
2. Customize some shortcuts
3. Floating notification with current layout
4. Switching modes: Global or Per application
5. No touchal (it is just plain physical keyboard now)
6. No root required

## Installation

1. Download and check MD5 checksum of the latest build ([link](https://github.com/alex-justes/YogaBookHoloKeyboard/raw/master/apk/YogaBook_HoloKeyboard_0.1a.apk)):
```
    APK: apk/YogaBook_HoloKeyboard_0.1a.apk 
    MD5: 39b36e2fab27de1e74d97e19eae157e0
```
2. Enable installation from Unknown sources (Settings -> Security -> Unknown sources)
3. Upload apk to your tablet and install 
4. Go to Settings -> Languages & input -> Virtual keyboard -> Manage keyboards 
5. Enable YogaBook Holo Keyboard and go back
6. Tap on the YogaBook Holo Keyboard entry to open Settings
7. Add layouts that you need by clicking Add layout button (you can reoder them later or remove)
8. Go to Nofications and enable Floating Notification (you will be prompted to give it some permissions (to draw over other apps))
9. When notification will appear in bottom-left corner you can move it to any desired location by touch-and-drag it.
10. Go to Shortcuts and make sure, that your configuration is correct
11. Switch to YogaBook Holo Keyboard input method (i.e. by pressing F10)
12. Enjoy!

## Additional information

Available layouts were decompiled from firmware, so that list may be wrong or incomplete for some layouts.

Not all shortcuts will work due to Lenovo's code (i.e. ctrl+space, some specific keys, etc).
Maybe i will make some workarounds, but you shouldn't rely on this.

## Future work

1. Add custom shortcuts for different actions (open an app, etc)
2. Clean up code

## For developers

That was hardcoded by lenovo. To change layots and get notifications about it you can do the following in your own code:

To switch to other layout (available layouts you can get from resources/raw/holo_layouts.json): 

```
val SWITCH_LAYOUT_INTENT = "com.cootek.smartinputv5.intent.LANGUAGE_CHANGED"
val CURRENT_LANGUAGE_EXTRA = "CURRENT_LANGUAGE"
val intent = Intent(SWITCH_LAYOUT_INTENT)
intent.putExtra(CURRENT_LANGUAGE_EXTRA, layout)
context.sendBroadcast(intent)
```

To get current layout, when it was changed by the firmware you should subscribe for the following intent: "com.lenovo.holo_keyboard.intent.KEYBOARD_CHANGED" and get its "current_layout" extra:

```
class CustomBroadcastReceiver: BroadcastReceiver()
{
    private var mLayout = ""
    override fun onReceive(context: Context?, intent: Intent?)
    {
        val LANGUAGE_CHANGED_INTENT = "com.lenovo.holo_keyboard.intent.KEYBOARD_CHANGED"
        val CURRENT_LANGUAGE = "current_layout"
        val action = intent?.action ?: ""
        when (action)
        {
            LANGUAGE_CHANGED_INTENT ->
            {
                mLayout = intent?.getStringExtra(CURRENT_LANGUAGE) ?: ""
            }
            else                    ->
            {
                return
            }
        }
    }
}
```

## Licensing 

The code is open and provided under the MIT License (LICENSE.md).

## P.S.

It is my first android application, so it is more proof-of-concept and experimental app, and yes, you can do it better ;)

## P.P.S.

Found any bugs? Open an issue!

