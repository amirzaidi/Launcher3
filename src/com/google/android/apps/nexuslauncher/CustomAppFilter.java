package com.google.android.apps.nexuslauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import com.android.launcher3.Utilities;

import java.util.HashSet;
import java.util.Set;

public class CustomAppFilter extends NexusAppFilter {
    public final static String HIDE_APPS_PREF = "all_apps_hide";
    private final Context mContext;

    public CustomAppFilter(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean shouldShowApp(ComponentName componentName) {
        return super.shouldShowApp(componentName) &&
                !getHiddenApps(mContext).contains(componentName.toString()) &&
                showIconPack(componentName.getPackageName());
    }

    private boolean showIconPack(String pkg) {
        String iconPack = CustomIconUtils.getCurrentPack(mContext);
        return iconPack.isEmpty() || iconPack.equals(pkg) || !CustomIconUtils.isPackProvider(mContext, pkg);
    }

    static void emptyAppFilter(Context context) {
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putStringSet(HIDE_APPS_PREF, new HashSet<String>());
        editor.apply();
    }

    static void hideComponentName(Context context, String comp) {
        Set<String> hiddenApps = getHiddenApps(context);
        hiddenApps.add(comp);
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putStringSet(HIDE_APPS_PREF, hiddenApps);
        editor.apply();
    }

    private static Set<String> getHiddenApps(Context context) {
        return new HashSet<>(Utilities.getPrefs(context).getStringSet(HIDE_APPS_PREF, new HashSet<String>()));
    }
}
