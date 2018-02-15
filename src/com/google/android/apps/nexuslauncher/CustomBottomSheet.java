/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.nexuslauncher;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.widget.WidgetsBottomSheet;

public class CustomBottomSheet extends WidgetsBottomSheet {
    private Launcher mLauncher;
    private FragmentManager mFragmentManager;

    public CustomBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
        mFragmentManager = mLauncher.getFragmentManager();
    }

    @Override
    public void populateAndShow(ItemInfo itemInfo) {
        super.populateAndShow(itemInfo);
        ((TextView) findViewById(R.id.title)).setText(itemInfo.title);
        ((PrefsFragment) mFragmentManager.findFragmentById(R.id.sheet_prefs)).loadForApp(itemInfo);
    }

    @Override
    protected void handleClose(boolean animate) {
        super.handleClose(animate);
        try {
            Fragment remove = mFragmentManager.findFragmentById(R.id.sheet_prefs);
            if (remove != null) {
                mFragmentManager.beginTransaction().remove(remove).commit();
            }
        } catch (IllegalStateException ignored) {
        }
    }

    public static class PrefsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener {
        private final static String PREF_PACK = "pref_app_icon_pack";
        private final static String PREF_HIDE = "pref_app_hide";
        private SwitchPreference mPrefPack;
        private SwitchPreference mPrefHide;
        private ItemInfo mItemInfo;
        private String mComponentName;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.app_edit_prefs);

            mPrefPack = (SwitchPreference) findPreference(PREF_PACK);
            mPrefHide = (SwitchPreference) findPreference(PREF_HIDE);
        }

        public void loadForApp(ItemInfo itemInfo) {
            mItemInfo = itemInfo;
            mComponentName = itemInfo.getTargetComponent().toString();

            CustomDrawableFactory factory = (CustomDrawableFactory) DrawableFactory.get(getContext());

            mPrefPack.setEnabled(factory.packCalendars.containsKey(mComponentName) || factory.packComponents.containsKey(mComponentName));
            mPrefPack.setChecked(mPrefPack.isEnabled() && CustomIconPack.isEnabledForApp(getContext(), mComponentName));

            mPrefHide.setChecked(CustomAppFilter.isHiddenApp(getContext(), mComponentName));

            mPrefPack.setOnPreferenceChangeListener(this);
            mPrefHide.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean enabled = (boolean) newValue;
            Launcher launcher = Launcher.getLauncher(getContext());
            String[] pkgs = new String[] { mItemInfo.getTargetComponent().getPackageName() };
            switch (preference.getKey()) {
                case PREF_PACK:
                    CustomIconPack.setAppState(launcher, mComponentName, enabled);
                    CustomIconUtils.reloadIcons(launcher, pkgs[0]);
                    break;
                case PREF_HIDE:
                    CustomAppFilter.setComponentNameState(launcher, mComponentName, !enabled);
                    for (UserHandle user : UserManagerCompat.getInstance(launcher).getUserProfiles()) {
                        if (!LauncherAppsCompat.getInstance(launcher).getActivityList(pkgs[0], user).isEmpty()) {
                            launcher.getModel().onPackagesUnavailable(pkgs, user, false);
                            launcher.getModel().onPackagesAvailable(pkgs, user, false);
                        }
                    }
                    break;
            }
            return true;
        }
    }

    @Override
    protected void onWidgetsBound() {
    }
}
