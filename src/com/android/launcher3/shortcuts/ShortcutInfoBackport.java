/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3.shortcuts;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;

/**
 * Wrapper class for {@link ShortcutInfo}, representing deep shortcuts into apps.
 *
 * Not to be confused with {@link com.android.launcher3.ShortcutInfo}.
 */
@TargetApi(Build.VERSION_CODES.N)
public class ShortcutInfoBackport extends ShortcutInfoCompat {
    private static final String INTENT_CATEGORY = "com.android.launcher3.DEEP_SHORTCUT";

    private Context mContext;
    private String mPackageName;
    private ComponentName mActivity;
    private HashMap<String, String> xmlData = new HashMap<>();
    private String mShortLabel = "";
    private String mLongLabel;

    public ShortcutInfoBackport(Context context, Resources resources, String packageName, ComponentName activity, XmlResourceParser parseXml) throws XmlPullParserException, IOException {
        super(null);
        mContext = context;
        mPackageName = packageName;
        mActivity = activity;
        for (int i = 0; i < parseXml.getAttributeCount(); i++)
            xmlData.put(parseXml.getAttributeName(i), parseXml.getAttributeValue(i));
        parseXml.nextToken();
        for (int i = 0; i < parseXml.getAttributeCount(); i++)
            xmlData.put(parseXml.getAttributeName(i), parseXml.getAttributeValue(i));

        if (xmlData.containsKey("shortcutShortLabel"))
            mShortLabel = resources.getString(Integer.valueOf(xmlData.get("shortcutShortLabel").substring(1)));

        if (xmlData.containsKey("shortcutLongLabel"))
            mLongLabel = resources.getString(Integer.valueOf(xmlData.get("shortcutLongLabel").substring(1)));
        else
            mLongLabel = mShortLabel;
    }

    public Drawable getIcon(int density) {
        try {
            if (xmlData.containsKey("icon"))
                return mContext.getPackageManager().getResourcesForApplication(mPackageName).getDrawableForDensity(Integer.valueOf(xmlData.get("icon").substring(1)), density);
        } catch (PackageManager.NameNotFoundException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public Intent makeIntent() {
        return new Intent(xmlData.containsKey("action") ? xmlData.get("action") : Intent.ACTION_MAIN)
                .addCategory(INTENT_CATEGORY)
                .setComponent(getRealActivity())
                .setPackage(getPackage())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME)
                .putExtra(EXTRA_SHORTCUT_ID, getId())
                .setData(getData());
    }

    private Uri getData() {
        if (xmlData.containsKey("data"))
            return Uri.parse(xmlData.get("data"));
        return null;
    }

    @Override
    public String getPackage() {
        if (xmlData.containsKey("targetPackage"))
            return xmlData.get("targetPackage");
        return mPackageName;
    }

    @Override
    public String getId() {
        return xmlData.get("shortcutId");
    }

    @Override
    public CharSequence getShortLabel() {
        return mShortLabel;
    }

    @Override
    public CharSequence getLongLabel() {
        return mLongLabel;
    }

    @Override
    public ComponentName getActivity() {
        return mActivity;
    }

    public ComponentName getRealActivity() {
        if (xmlData.containsKey("targetClass"))
            return new ComponentName(getPackage(), xmlData.get("targetClass")); //"org.chromium.chrome.browser.document.ChromeLauncherActivity"
        return getActivity();
    }

    @Override
    public UserHandle getUserHandle() {
        return android.os.Process.myUserHandle();
    }

    @Override
    public boolean isPinned() {
        return false;
    }

    @Override
    public boolean isDeclaredInManifest() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        if (xmlData.containsKey("enabled"))
            return xmlData.get("enabled").toLowerCase().equals("true");
        return true;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public CharSequence getDisabledMessage() {
        return "Disabled";
    }

    @Override
    public String toString() {
        return "";
    }
}
