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
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class Utils {

    public static final int MAX_BLUR_WIDTH = 900;
    public static final int MAX_BLUR_HEIGHT = 1600;

    // Device types
    private static final int DEVICE_PHONE = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;

    // Device type reference
    private static int mDeviceType = -1;

    private static int getScreenType(Context con) {
        if (mDeviceType == -1) {
            WindowManager wm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            int shortSize = Math.min(outMetrics.heightPixels, outMetrics.widthPixels);
            int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / outMetrics.densityDpi;
            if (shortSizeDp < 600) {
                // 0-599dp: "phone" UI with a separate status & navigation bar
                mDeviceType =  DEVICE_PHONE;
            } else if (shortSizeDp < 720) {
                // 600-719dp: "phone" UI with modifications for larger screens
                mDeviceType = DEVICE_HYBRID;
            } else {
                // 720dp: "tablet" UI with a single combined status & navigation bar
                mDeviceType = DEVICE_TABLET;
            }
        }
        return mDeviceType;
    }

    public static boolean isTabletUI(Context con) {
        return getScreenType(con) == DEVICE_TABLET;
    }

    public static Bitmap blurBitmap(Bitmap bmp, int radius, Context context) {
        if (bmp.getWidth() > MAX_BLUR_WIDTH) {
            bmp = bmp.createScaledBitmap(bmp, MAX_BLUR_WIDTH, MAX_BLUR_HEIGHT, true);
        }
        Bitmap out = Bitmap.createBitmap(bmp);
        RenderScript rs = RenderScript.create(context);

        Allocation input = Allocation.createFromBitmap(
                rs, bmp, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(rs, input.getType());

        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setInput(input);
        script.setRadius(radius);
        script.forEach(output);

        output.copyTo(out);

        rs.destroy();
        return out;
    }

}

