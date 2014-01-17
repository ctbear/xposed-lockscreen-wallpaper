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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;

public class SettingsActivity extends Activity {

    public static final String PREF_CAT_KEY_LOCKSCREEN_BACKGROUND = "pref_cat_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND = "pref_lockscreen_background";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR = "pref_lockscreen_bg_color";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE = "pref_lockscreen_bg_image";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE_BLUR = "pref_lockscreen_bg_image_blur";
    public static final String PREF_KEY_LOCKSCREEN_BACKGROUND_SEE_THROUGH_TINT = "pref_lockscreen_bg_see_through_tint";
    public static final String LOCKSCREEN_BG_DEFAULT = "default";
    public static final String LOCKSCREEN_BG_COLOR = "color";
    public static final String LOCKSCREEN_BG_IMAGE = "image";
    public static final String LOCKSCREEN_BG_SEE_THROUGH = "see-through";
    public static final String LOCKSCREEN_BG_SEE_THROUGH_TINT_DARK = "dark";
    public static final String LOCKSCREEN_BG_SEE_THROUGH_TINT_LIGHT = "light";

    private static final int REQ_LOCKSCREEN_BACKGROUND = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()).commit();
    }

    public static class PrefsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        private SharedPreferences mPrefs;
        private AlertDialog mDialog;
        private PreferenceCategory mPrefCatLockscreenBg;
        private ListPreference mPrefLockscreenBg;
        private ColorPickerPreference mPrefLockscreenBgColor;
        private Preference mPrefLockscreenBgImage;
        private CheckBoxPreference mPrefLockscreenBgImageBlur;
        private ListPreference mPrefLockscreenBgSeeThruTint;
        private File wallpaperImage;
        private File wallpaperTemporary;

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // this is important because although the handler classes that read these settings
            // are in the same package, they are executed in the context of the hooked package
            getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
            addPreferencesFromResource(R.xml.prefs);

            mPrefs = getPreferenceScreen().getSharedPreferences();

            mPrefCatLockscreenBg =
                    (PreferenceCategory) findPreference(PREF_CAT_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBg = (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND);
            mPrefLockscreenBgColor =
                    (ColorPickerPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_COLOR);
            mPrefLockscreenBgImage =
                    (Preference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE);
            mPrefLockscreenBgImageBlur =
                    (CheckBoxPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_IMAGE_BLUR);
            mPrefLockscreenBgSeeThruTint =
                    (ListPreference) findPreference(PREF_KEY_LOCKSCREEN_BACKGROUND_SEE_THROUGH_TINT);

            wallpaperImage = new File(getActivity().getFilesDir() + "/lockwallpaper");
            wallpaperTemporary = new File(getActivity().getCacheDir() + "/lockwallpaper.tmp");
            recycleSeeThroughImage();
        }

        @Override
        public void onResume() {
            super.onResume();

            updatePreferences(null);
            mPrefs.registerOnSharedPreferenceChangeListener(this);

            recycleSeeThroughImage();
        }

        @Override
        public void onPause() {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);

            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }

            super.onPause();
        }

        private void updatePreferences(String key) {
            if (key == null || key.equals(PREF_KEY_LOCKSCREEN_BACKGROUND)) {
                mPrefLockscreenBg.setSummary(mPrefLockscreenBg.getEntry());
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgColor);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgImage);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgImageBlur);
                mPrefCatLockscreenBg.removePreference(mPrefLockscreenBgSeeThruTint);
                String option = mPrefs.getString(PREF_KEY_LOCKSCREEN_BACKGROUND, LOCKSCREEN_BG_DEFAULT);
                if (option.equals(LOCKSCREEN_BG_COLOR)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgColor);
                } else if (option.equals(LOCKSCREEN_BG_IMAGE)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgImage);
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgImageBlur);
                } else if (option.equals(LOCKSCREEN_BG_SEE_THROUGH)) {
                    mPrefCatLockscreenBg.addPreference(mPrefLockscreenBgSeeThruTint);
                }
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            updatePreferences(key);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen prefScreen, Preference pref) {
            Intent intent = null;
            if (pref == mPrefLockscreenBgImage) {
                setCustomLockscreenImage();
                return true;
            }

            if (intent != null) {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
                return true;
            }

            return super.onPreferenceTreeClick(prefScreen, pref);
        }

        @SuppressWarnings("deprecation")
        private void setCustomLockscreenImage() {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            Rect rect = new Rect();
            Window window = getActivity().getWindow();
            window.getDecorView().getWindowVisibleDisplayFrame(rect);
            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            // Lock screen for tablets visible section are different in landscape/portrait,
            // image need to be cropped correctly, like wallpaper setup for scrolling in background in home screen
            // other wise it does not scale correctly
            if (Utils.isTabletUI(getActivity())) {
                width = getActivity().getWallpaperDesiredMinimumWidth();
                height = getActivity().getWallpaperDesiredMinimumHeight();
                float spotlightX = (float) display.getWidth() / width;
                float spotlightY = (float) display.getHeight() / height;
                intent.putExtra("aspectX", width);
                intent.putExtra("aspectY", height);
                intent.putExtra("outputX", width);
                intent.putExtra("outputY", height);
                intent.putExtra("spotlightX", spotlightX);
                intent.putExtra("spotlightY", spotlightY);
            } else {
                boolean isPortrait = getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_PORTRAIT;
                intent.putExtra("aspectX", isPortrait ? width : height - titleBarHeight);
                intent.putExtra("aspectY", isPortrait ? height - titleBarHeight : width);
            }
            try {
                wallpaperTemporary.createNewFile();
                wallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(wallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQ_LOCKSCREEN_BACKGROUND);
            } catch (Exception e) {
                Toast.makeText(getActivity(), getString(
                        R.string.lockscreen_background_result_not_successful),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

        private void recycleSeeThroughImage() {
            File seeThroughImage = new File(getActivity().getFilesDir(), "seethroughimage");
            try {
                if (seeThroughImage.exists()) {
                    seeThroughImage.delete();
                }
                seeThroughImage.createNewFile();
                seeThroughImage.setReadable(true, false);
                seeThroughImage.setWritable(true, false);
            } catch (Exception e) {
                Log.e("SettingsActivity", e.getLocalizedMessage());
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQ_LOCKSCREEN_BACKGROUND) {
                if (resultCode == Activity.RESULT_OK) {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.renameTo(wallpaperImage);
                    }
                    wallpaperImage.setReadable(true, false);
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_successful),
                            Toast.LENGTH_SHORT).show();
                } else {
                    if (wallpaperTemporary.exists()) {
                        wallpaperTemporary.delete();
                    }
                    Toast.makeText(getActivity(), getString(
                            R.string.lockscreen_background_result_not_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
