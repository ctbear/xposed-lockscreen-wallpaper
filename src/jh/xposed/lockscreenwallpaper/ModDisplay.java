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
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.io.File;
import java.io.FileOutputStream;

public class ModDisplay {

    private static final String TAG = "ModDisplay";
    private static final String CLASS_DISPLAY_POWER_CONTROLLER = "com.android.server.power.DisplayPowerController";
    private static final String CLASS_DISPLAY_POWER_REQUEST = "com.android.server.power.DisplayPowerRequest";
    private static final String CLASS_SURFACE_CONTROL = "android.view.SurfaceControl";
    private static final boolean DEBUG = false;

    private static XSharedPreferences mPrefs;
    private static Context mContext;
    private static int SCREEN_STATE_OFF;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(XSharedPreferences prefs) {
        try {
            mPrefs = prefs;
            final Class<?> dpcClass = XposedHelpers.findClass(CLASS_DISPLAY_POWER_CONTROLLER, null);
            final Class<?> dprClass = XposedHelpers.findClass(CLASS_DISPLAY_POWER_REQUEST, null);
            SCREEN_STATE_OFF = XposedHelpers.getStaticIntField(dprClass, "SCREEN_STATE_OFF");

            XposedHelpers.findAndHookMethod(dpcClass, "requestPowerState", dprClass, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            mPrefs.reload();
                            final String bgType = mPrefs.getString(
                                    SettingsActivity.PREF_KEY_LOCKSCREEN_BACKGROUND,
                                    SettingsActivity.LOCKSCREEN_BG_DEFAULT);
                            if (bgType.equals(SettingsActivity.LOCKSCREEN_BG_SEE_THROUGH)) {
                                if (mContext == null) {
                                    mContext = ((Context) XposedHelpers.getObjectField(param.thisObject, "mContext"))
                                            .createPackageContext(XposedLockscreenWallpaper.PACKAGE_NAME, 0);
                                }

                                Object request = param.args[0];
                                boolean waitForNegativeProximity = (Boolean) param.args[1];
                                boolean pendingWaitForNegativeProximity = XposedHelpers.getBooleanField(
                                        param.thisObject, "mPendingWaitForNegativeProximityLocked");
                                boolean displayReadyLocked = XposedHelpers.getBooleanField(param.thisObject,
                                        "mDisplayReadyLocked");
                                boolean pendingRequestChangedLocked = XposedHelpers.getBooleanField(param.thisObject,
                                        "mPendingRequestChangedLocked");
                                Object pendingRequestLocked =  XposedHelpers.getObjectField(param.thisObject,
                                        "mPendingRequestLocked");
                                if ((displayReadyLocked || !pendingRequestChangedLocked) &&
                                        (waitForNegativeProximity &&
                                                !pendingWaitForNegativeProximity ||
                                                pendingRequestLocked == null ||
                                                !pendingRequestLocked.equals(request))) {
                                    int screenState = XposedHelpers.getIntField(request, "screenState");
                                    if (DEBUG) log("Screen state: " + screenState);
                                    if (screenState == SCREEN_STATE_OFF) {
                                        File seeThroughImage = new File(mContext.getFilesDir(), "seethroughimage");
                                        FileOutputStream out = new FileOutputStream(seeThroughImage);

                                        if (screenState == SCREEN_STATE_OFF) {
                                            Object displayManager = XposedHelpers.getObjectField(param.thisObject, "mDisplayManager");
                                            int[] displayIds = (int[]) XposedHelpers.callMethod(displayManager, "getDisplayIds");
                                            Object displayInfo = XposedHelpers.callMethod(displayManager, "getDisplayInfo", displayIds[0]);
                                            int naturalWidth = (Integer) XposedHelpers.callMethod(displayInfo, "getNaturalWidth");
                                            int naturalHeight = (Integer) XposedHelpers.callMethod(displayInfo, "getNaturalHeight");
                                            final Class<?> surfaceControlClass = XposedHelpers.findClass(CLASS_SURFACE_CONTROL, null);
                                            Bitmap bmp = (Bitmap) XposedHelpers.callStaticMethod(surfaceControlClass, "screenshot",
                                                    naturalWidth, naturalHeight, 0, 22000);
                                            boolean saveSuccess = bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
                                            if (DEBUG) log("Bitmap saved: " + saveSuccess);

                                            try {
                                                out.flush();
                                                out.close();
                                            } catch (Exception e) {
                                                log(e.getLocalizedMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
