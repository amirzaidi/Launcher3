package com.google.android.apps.nexuslauncher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.ReflectedSdkLoader;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.util.ComponentKey;
import com.google.android.apps.nexuslauncher.clock.CustomClock;
import com.google.android.apps.nexuslauncher.clock.DynamicClock;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class CustomIconProvider extends DynamicIconProvider {
    public final static String DISABLE_PACK_PREF = "all_apps_disable_pack";

    private final Context mContext;
    private final PackageManager mPm;
    private CustomDrawableFactory mFactory;
    private final BroadcastReceiver mDateChangeReceiver;
    private int mDateOfMonth;

    public CustomIconProvider(Context context) {
        super(context);
        mContext = context;
        mPm = mContext.getPackageManager();
        mFactory = (CustomDrawableFactory) DrawableFactory.get(context);

        mDateChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!Utilities.ATLEAST_NOUGAT) {
                    int dateOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                    if (dateOfMonth == mDateOfMonth) {
                        return;
                    }
                    mDateOfMonth = dateOfMonth;
                }
                LauncherAppsCompat apps = LauncherAppsCompat.getInstance(mContext);
                LauncherModel model = LauncherAppState.getInstance(context).getModel();
                DeepShortcutManager shortcutManager = DeepShortcutManager.getInstance(context);
                for (UserHandle user : UserManagerCompat.getInstance(context).getUserProfiles()) {
                    Set<String> packages = new HashSet<>();
                    for (ComponentName componentName : mFactory.packCalendars.keySet()) {
                        String pkg = componentName.getPackageName();
                        if (!apps.getActivityList(pkg, user).isEmpty()) {
                            packages.add(pkg);
                        }
                    }
                    for (String pkg : packages) {
                        CustomIconUtils.reloadIcon(shortcutManager, model, user, pkg);
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (!Utilities.ATLEAST_NOUGAT) {
            intentFilter.addAction(Intent.ACTION_TIME_TICK);
        }
        mContext.registerReceiver(mDateChangeReceiver, intentFilter, null, new Handler(LauncherModel.getWorkerLooper()));
    }

    @Override
    public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
        mFactory.ensureInitialLoadComplete();

        String packageName = info.getApplicationInfo().packageName;
        ComponentName component = info.getComponentName();
        Drawable drawable = null;
        if (CustomIconUtils.usingValidPack(mContext) && isEnabledForApp(mContext, new ComponentKey(component, info.getUser()))) {
            try {
                Resources res = mPm.getResourcesForApplication(mFactory.iconPack);
                ReflectedSdkLoader.loadLatestSupported(res);

                if (mFactory.packCalendars.containsKey(component)) {
                    int drawableId = res.getIdentifier(mFactory.packCalendars.get(component)
                            + Calendar.getInstance().get(Calendar.DAY_OF_MONTH), "drawable", mFactory.iconPack);
                    if (drawableId != 0) {
                        drawable = res.getDrawableForDensity(drawableId, iconDpi);
                    }
                } else if (mFactory.packComponents.containsKey(component)) {
                    int drawableId = mFactory.packComponents.get(component);
                    drawable = res.getDrawableForDensity(drawableId, iconDpi);
                    if (Utilities.ATLEAST_NOUGAT && mFactory.packClocks.containsKey(drawableId)) {
                        drawable = CustomClock.getClock(mContext, drawable, mFactory.packClocks.get(drawableId), iconDpi);
                    }
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        if (drawable == null && !DynamicIconProvider.GOOGLE_CALENDAR.equals(packageName) && !DynamicClock.DESK_CLOCK.equals(component)) {
            drawable = CustomIconUtils.extractIconByTag(mPm, info.getComponentName(), iconDpi, "roundIcon");
        }
        return drawable == null
                ? super.getIcon(info, iconDpi, flattenDrawable)
                : drawable.mutate();
    }

    static void clearDisabledApps(Context context) {
        setDisabledApps(context, new HashSet<String>());
    }

    static boolean isEnabledForApp(Context context, ComponentKey key) {
        return !getDisabledApps(context).contains(key.toString());
    }

    static void setAppState(Context context, ComponentKey key, boolean enabled) {
        String comp = key.toString();
        Set<String> disabledApps = getDisabledApps(context);
        while (disabledApps.contains(comp)) {
            disabledApps.remove(comp);
        }
        if (!enabled) {
            disabledApps.add(comp);
        }
        setDisabledApps(context, disabledApps);
    }

    private static Set<String> getDisabledApps(Context context) {
        return new HashSet<>(Utilities.getPrefs(context).getStringSet(DISABLE_PACK_PREF, new HashSet<String>()));
    }

    private static void setDisabledApps(Context context, Set<String> disabledApps) {
        SharedPreferences.Editor editor = Utilities.getPrefs(context).edit();
        editor.putStringSet(DISABLE_PACK_PREF, disabledApps);
        editor.apply();
    }
}
