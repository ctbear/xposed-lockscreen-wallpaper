/**
 * Copyright 2014 Jerry Hung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jh.xposed.lockscreenwallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ModLockscreen {
    public static final String PACKAGE_NAME = "com.android.keyguard";

    private static final String TAG = "ModLockscreen";
    private static final String CLASS_KGVIEW_MANAGER = "com.android.keyguard.KeyguardViewManager";
    private static final boolean DEBUG = false;

    private static XSharedPreferences mPrefs;
    private static Context mContext;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            mPrefs = prefs;
            final Class<?> kgViewManagerClass = XposedHelpers.findClass(CLASS_KGVIEW_MANAGER, classLoader);

            XposedHelpers.findAndHookMethod(kgViewManagerClass, "maybeCreateKeyguardLocked",
                    boolean.class, boolean.class, Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    mPrefs.reload();
                    final String bgType = mPrefs.getString(
                            SettingsActivity.PREF_KEY_LOCKSCREEN_BACKGROUND,
                            SettingsActivity.LOCKSCREEN_BG_DEFAULT);

                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (context != null && mContext == null) {
                        mContext = context.createPackageContext(XposedLockscreenWallpaper.PACKAGE_NAME, 0);
                    }

                    if (!bgType.equals(SettingsActivity.LOCKSCREEN_BG_DEFAULT)) {
                        Bitmap background;
                        if (bgType.equals(SettingsActivity.LOCKSCREEN_BG_COLOR)) {
                            final int color = mPrefs.getInt(
                                    SettingsActivity.PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR, Color.BLACK);
                            Drawable d = new ColorDrawable(color);
                            int w = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : 1;
                            int h = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : 1;
                            background = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(background);
                            d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                            d.draw(canvas);
                        } else {
                            String wallpaperFile = "";
                            if (bgType.equals(SettingsActivity.LOCKSCREEN_BG_IMAGE)) {
                                wallpaperFile = mContext.getFilesDir() + "/lockwallpaper";
                            } else if (bgType.equals(SettingsActivity.LOCKSCREEN_BG_SEE_THROUGH)) {
                                wallpaperFile = mContext.getFilesDir() + "/seethroughimage";
                            }

                            background = BitmapFactory.decodeFile(wallpaperFile);
                            if (DEBUG) log("Wallpaper file null: " + (background == null));
                            if (background != null) {
                                int blurAmount = mPrefs.getInt(
                                        SettingsActivity.PREF_KEY_LOCKSCREEN_BLUR_AMOUNT, 100) / 4;
                                blurAmount = blurAmount == 0 ? 1 : blurAmount;
                                background = Utils.blurBitmap(background, blurAmount, mContext);
                                if (bgType.equals(SettingsActivity.LOCKSCREEN_BG_SEE_THROUGH)) {
                                    final String tint = mPrefs.getString(
                                            SettingsActivity.PREF_KEY_LOCKSCREEN_BACKGROUND_SEE_THROUGH_TINT,
                                            SettingsActivity.LOCKSCREEN_BG_SEE_THROUGH_TINT_DARK);
                                    Bitmap bitmapOverlay = Bitmap.createBitmap(
                                            background.getWidth(), background.getHeight(), background.getConfig());
                                    Canvas canvas = new Canvas(bitmapOverlay);
                                    canvas.drawBitmap(background, new Matrix(), null);
                                    if (tint.equals(SettingsActivity.LOCKSCREEN_BG_SEE_THROUGH_TINT_DARK)) {
                                        canvas.drawARGB(127, 0, 0, 0);
                                    } else if (tint.equals(SettingsActivity.LOCKSCREEN_BG_SEE_THROUGH_TINT_LIGHT)) {
                                        canvas.drawARGB(127, 255, 255, 255);
                                    }
                                    background = bitmapOverlay;
                                }
                            }
                        }
                        if (background != null) {
                            setLockscreenBitmap(background, context);
                        }
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void setLockscreenBitmap(Bitmap bmp, Context context) {
        final Class<?> keyguardUpdateMonitorClass = XposedHelpers.findClass("com.android.keyguard.KeyguardUpdateMonitor",
                context.getClassLoader());
        Object keyguardUpdateMonitor = XposedHelpers.callStaticMethod(keyguardUpdateMonitorClass, "getInstance", context);
        XposedHelpers.callMethod(keyguardUpdateMonitor, "dispatchSetBackground", bmp);
    }

}

