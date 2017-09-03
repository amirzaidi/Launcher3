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
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Utilities;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Performs operations related to deep shortcuts, such as querying for them, pinning them, etc.
 */
public class DeepShortcutManager {
    private static final String TAG = "DeepShortcutManager";

    private static final int FLAG_GET_ALL = ShortcutQuery.FLAG_MATCH_DYNAMIC
            | ShortcutQuery.FLAG_MATCH_MANIFEST | ShortcutQuery.FLAG_MATCH_PINNED;

    private static DeepShortcutManager sInstance;
    private static final Object sInstanceLock = new Object();

    public static DeepShortcutManager getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new DeepShortcutManager(context.getApplicationContext());
            }
            return sInstance;
        }
    }

    private final LauncherApps mLauncherApps;
    private boolean mWasLastCallSuccess;
    private Context mContext;

    private DeepShortcutManager(Context context) {
        mLauncherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        mContext = context;
    }

    public static boolean supportsShortcuts(ItemInfo info) {
        return info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                && !info.isDisabled();
    }

    public boolean wasLastCallSuccess() {
        return mWasLastCallSuccess;
    }

    public void onShortcutsChanged(List<ShortcutInfoCompat> shortcuts) {
        // mShortcutCache.removeShortcuts(shortcuts);
    }

    /**
     * Queries for the shortcuts with the package name and provided ids.
     *
     * This method is intended to get the full details for shortcuts when they are added or updated,
     * because we only get "key" fields in onShortcutsChanged().
     */
    public List<ShortcutInfoCompat> queryForFullDetails(String packageName,
            List<String> shortcutIds, UserHandle user) {
        return query(FLAG_GET_ALL, packageName, null, shortcutIds, user);
    }

    /**
     * Gets all the manifest and dynamic shortcuts associated with the given package and user,
     * to be displayed in the shortcuts container on long press.
     */
    public List<ShortcutInfoCompat> queryForShortcutsContainer(ComponentName activity,
            List<String> ids, UserHandle user) {
        return query(ShortcutQuery.FLAG_MATCH_MANIFEST | ShortcutQuery.FLAG_MATCH_DYNAMIC,
                activity.getPackageName(), activity, ids, user);
    }

    /**
     * Removes the given shortcut from the current list of pinned shortcuts.
     * (Runs on background thread)
     */
    @TargetApi(25)
    public void unpinShortcut(final ShortcutKey key) {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            String packageName = key.componentName.getPackageName();
            String id = key.getId();
            UserHandle user = key.user;
            List<String> pinnedIds = extractIds(queryForPinnedShortcuts(packageName, user));
            pinnedIds.remove(id);
            try {
                mLauncherApps.pinShortcuts(packageName, pinnedIds, user);
                mWasLastCallSuccess = true;
            } catch (SecurityException|IllegalStateException e) {
                Log.w(TAG, "Failed to unpin shortcut", e);
                mWasLastCallSuccess = false;
            }
        }
    }

    /**
     * Adds the given shortcut to the current list of pinned shortcuts.
     * (Runs on background thread)
     */
    @TargetApi(25)
    public void pinShortcut(final ShortcutKey key) {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            String packageName = key.componentName.getPackageName();
            String id = key.getId();
            UserHandle user = key.user;
            List<String> pinnedIds = extractIds(queryForPinnedShortcuts(packageName, user));
            pinnedIds.add(id);
            try {
                mLauncherApps.pinShortcuts(packageName, pinnedIds, user);
                mWasLastCallSuccess = true;
            } catch (SecurityException|IllegalStateException e) {
                Log.w(TAG, "Failed to pin shortcut", e);
                mWasLastCallSuccess = false;
            }
        }
    }

    @TargetApi(25)
    public void startShortcut(String packageName, String id, Intent intent,
          Bundle startActivityOptions, UserHandle user) {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            try {
                mLauncherApps.startShortcut(packageName, id, intent.getSourceBounds(),
                        startActivityOptions, user);
                mWasLastCallSuccess = true;
            } catch (SecurityException|IllegalStateException e) {
                Log.e(TAG, "Failed to start shortcut", e);
                mWasLastCallSuccess = false;
            }
        } else
            mContext.startActivity(intent, startActivityOptions);
    }

    @TargetApi(25)
    public Drawable getShortcutIconDrawable(ShortcutInfoCompat shortcutInfo, int density) {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            try {
                Drawable icon = mLauncherApps.getShortcutIconDrawable(
                        shortcutInfo.getShortcutInfo(), density);
                mWasLastCallSuccess = true;
                return icon;
            } catch (SecurityException|IllegalStateException e) {
                Log.e(TAG, "Failed to get shortcut icon", e);
                mWasLastCallSuccess = false;
            }
        } else {
            return ((ShortcutInfoBackport) shortcutInfo).getIcon(density);
        }

        return null;
    }

    /**
     * Returns the id's of pinned shortcuts associated with the given package and user.
     *
     * If packageName is null, returns all pinned shortcuts regardless of package.
     */
    public List<ShortcutInfoCompat> queryForPinnedShortcuts(String packageName, UserHandle user) {
        return query(ShortcutQuery.FLAG_MATCH_PINNED, packageName, null, null, user);
    }

    public List<ShortcutInfoCompat> queryForAllShortcuts(UserHandle user) {
        return query(FLAG_GET_ALL, null, null, null, user);
    }

    private List<String> extractIds(List<ShortcutInfoCompat> shortcuts) {
        List<String> shortcutIds = new ArrayList<>(shortcuts.size());
        for (ShortcutInfoCompat shortcut : shortcuts) {
            shortcutIds.add(shortcut.getId());
        }
        return shortcutIds;
    }

    /**
     * Query the system server for all the shortcuts matching the given parameters.
     * If packageName == null, we query for all shortcuts with the passed flags, regardless of app.
     *
     * TODO: Use the cache to optimize this so we don't make an RPC every time.
     */
    @TargetApi(25)
    private List<ShortcutInfoCompat> query(int flags, String packageName,
            ComponentName activity, List<String> shortcutIds, UserHandle user) {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            ShortcutQuery q = new ShortcutQuery();
            q.setQueryFlags(flags);
            if (packageName != null) {
                q.setPackage(packageName);
                q.setActivity(activity);
                q.setShortcutIds(shortcutIds);
            }
            List<ShortcutInfo> shortcutInfos = null;
            try {
                shortcutInfos = mLauncherApps.getShortcuts(q, user);
                mWasLastCallSuccess = true;
            } catch (SecurityException|IllegalStateException e) {
                Log.e(TAG, "Failed to query for shortcuts", e);
                mWasLastCallSuccess = false;
            }
            if (shortcutInfos == null) {
                return Collections.EMPTY_LIST;
            }
            List<ShortcutInfoCompat> shortcutInfoCompats = new ArrayList<>(shortcutInfos.size());
            for (ShortcutInfo shortcutInfo : shortcutInfos) {
                shortcutInfoCompats.add(new ShortcutInfoCompat(shortcutInfo));
            }
            return shortcutInfoCompats;
        } else {
            List<ShortcutInfoCompat> shortcutInfoCompats = new ArrayList<>();
            try {
                if (packageName == null) {
                    List<LauncherActivityInfo> infoList = mLauncherApps.getActivityList(null, android.os.Process.myUserHandle());
                    for (LauncherActivityInfo info : infoList)
                        parsePackageXml(info.getComponentName().getPackageName(), info.getComponentName(), shortcutInfoCompats);
                } else
                    parsePackageXml(packageName, activity, shortcutInfoCompats);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return shortcutInfoCompats;
        }
    }

    private void parsePackageXml(String packageName, ComponentName activity, List<ShortcutInfoCompat> shortcutInfoCompats) throws Exception {
        Resources resourcesForApplication = mContext.getPackageManager().getResourcesForApplication(packageName);
        AssetManager assets = resourcesForApplication.getAssets();
        XmlResourceParser parseXml = assets.openXmlResourceParser("AndroidManifest.xml");
        int eventType;
        String resource = null;
        String currActivity = "";
        String searchActivity = activity.getClassName();
        List<String> exportedActivities = new ArrayList<>();
        while ((eventType = parseXml.nextToken()) != XmlPullParser.END_DOCUMENT)
            if (eventType == XmlPullParser.START_TAG) {
                if (parseXml.getName().equals("activity")|| parseXml.getName().equals("activity-alias")) {
                    boolean exported = false;
                    for (int i = 0; i < parseXml.getAttributeCount(); i++) {
                        String attributeName = parseXml.getAttributeName(i);
                        if (attributeName.equals("name"))
                            currActivity = parseXml.getAttributeValue(i);
                        else if (attributeName.equals("exported"))
                            exported = parseXml.getAttributeValue(i).toLowerCase().equals("true");
                    }
                    if (exported)
                        exportedActivities.add(currActivity);
                } else if (parseXml.getName().equals("meta-data") && currActivity.equals(searchActivity)) {
                    boolean found = false;
                    String tempResource = null;
                    for (int i = 0; i < parseXml.getAttributeCount(); i++)
                        if (parseXml.getAttributeName(i).equals("name") && parseXml.getAttributeValue(i).equals("android.app.shortcuts"))
                            found = true;
                        else if (parseXml.getAttributeName(i).equals("resource"))
                            tempResource = parseXml.getAttributeValue(i);

                    if (found && tempResource != null)
                        resource = tempResource;
                }
            }

        if (resource != null) {
            parseXml = resourcesForApplication.getXml(Integer.parseInt(resource.substring(1)));
            while ((eventType = parseXml.nextToken()) != XmlPullParser.END_DOCUMENT)
                if (eventType == XmlPullParser.START_TAG && parseXml.getName().equals("shortcut")) {
                    ShortcutInfoBackport info = new ShortcutInfoBackport(mContext, resourcesForApplication, packageName, activity, parseXml);
                    String shortcutActivity = info.getRealActivity().getClassName();
                    for (String s : exportedActivities)
                        if (shortcutActivity.equals(s)) {
                            shortcutInfoCompats.add(info);
                            break;
                        }
                }
        }
    }

    @TargetApi(25)
    public boolean hasHostPermission() {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            try {
                return mLauncherApps.hasShortcutHostPermission();
            } catch (SecurityException|IllegalStateException e) {
                Log.e(TAG, "Failed to make shortcut manager call", e);
            }
        } else
            return true;
        return false;
    }
}
