package com.google.android.apps.nexuslauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import com.android.launcher3.Utilities;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomIconPackParser {
    public final static String DISABLE_PACK_PREF = "all_apps_disable_pack";

    static void clearDisabledApps(Context context) {
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putStringSet(DISABLE_PACK_PREF, new HashSet<String>());
        editor.apply();
    }

    static boolean enabledIconPack(Context context, String comp) {
        return !getDisabledApps(context).contains(comp);
    }

    static void enableIconPack(Context context, String comp) {
        Set<String> hiddenApps = getDisabledApps(context);
        hiddenApps.remove(comp);
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putStringSet(DISABLE_PACK_PREF, hiddenApps);
        editor.apply();
    }

    static void disableIconPack(Context context, String comp) {
        Set<String> hiddenApps = getDisabledApps(context);
        hiddenApps.add(comp);
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putStringSet(DISABLE_PACK_PREF, hiddenApps);
        editor.apply();
    }

    private static Set<String> getDisabledApps(Context context) {
        return new HashSet<>(Utilities.getPrefs(context).getStringSet(DISABLE_PACK_PREF, new HashSet<String>()));
    }

    static void parse(Map<String, Integer> packComponents, Map<String, String> packCalendars, PackageManager pm, String iconPack) {
        try {
            Resources res = pm.getResourcesForApplication(iconPack);
            int resId = res.getIdentifier("appfilter", "xml", iconPack);
            if (resId != 0) {
                XmlResourceParser parseXml = pm.getXml(iconPack, resId, null);
                while (parseXml.next() != XmlPullParser.END_DOCUMENT) {
                    if (parseXml.getEventType() == XmlPullParser.START_TAG) {
                        boolean isCalendar = parseXml.getName().equals("calendar");
                        if (isCalendar || parseXml.getName().equals("item")) {
                            String componentName = parseXml.getAttributeValue(null, "component");
                            String drawableName = parseXml.getAttributeValue(null, isCalendar ? "prefix" : "drawable");
                            if (componentName != null && drawableName != null) {
                                if (isCalendar) {
                                    packCalendars.put(componentName, drawableName);
                                } else {
                                    int drawableId = res.getIdentifier(drawableName, "drawable", iconPack);
                                    if (drawableId != 0) {
                                        packComponents.put(componentName, drawableId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException | XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}
