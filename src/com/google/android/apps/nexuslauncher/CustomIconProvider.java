package com.google.android.apps.nexuslauncher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.google.android.apps.nexuslauncher.clock.CustomClock;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class CustomIconProvider extends DynamicIconProvider {
    public final static String DISABLE_PACK_PREF = "all_apps_disable_pack";

    private CustomDrawableFactory mFactory;
    private final BroadcastReceiver mDateChangeReceiver;
    private int mDateOfMonth;

    public CustomIconProvider(Context context) {
        super(context);
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
    public Drawable getIcon(LauncherActivityInfo launcherActivityInfo, int iconDpi, boolean flattenDrawable) {
        mFactory.ensureInitialLoadComplete();

        String packageName = launcherActivityInfo.getApplicationInfo().packageName;
        ComponentName component = launcherActivityInfo.getComponentName();
        Drawable drawable = null;
        if (isEnabledForApp(mContext, component.toString())) {
            PackageManager pm = mContext.getPackageManager();
            if (mFactory.packCalendars.containsKey(component)) {
                try {
                    Resources res = pm.getResourcesForApplication(mFactory.iconPack);
                    int drawableId = res.getIdentifier(mFactory.packCalendars.get(component)
                            + Calendar.getInstance().get(Calendar.DAY_OF_MONTH), "drawable", mFactory.iconPack);
                    if (drawableId != 0) {
                        drawable = pm.getDrawable(mFactory.iconPack, drawableId, null);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            } else if (mFactory.packComponents.containsKey(component)) {
                int drawableId = mFactory.packComponents.get(component);
                drawable = pm.getDrawable(mFactory.iconPack, mFactory.packComponents.get(component), null);
                if (Utilities.ATLEAST_OREO && mFactory.packClocks.containsKey(drawableId)) {
                    drawable = CustomClock.getClock(mContext, drawable, mFactory.packClocks.get(drawableId), iconDpi);
                }
            }
        }

        if (drawable == null && !"com.google.android.calendar".equals(packageName)) {
            drawable = getRoundIcon(packageName, iconDpi);
        }
        return drawable == null ? super.getIcon(launcherActivityInfo, iconDpi, flattenDrawable) : drawable;
    }

    private Drawable getRoundIcon(String packageName, int iconDpi) {
        try {
            Resources resourcesForApplication = mContext.getPackageManager().getResourcesForApplication(packageName);
            AssetManager assets = resourcesForApplication.getAssets();
            XmlResourceParser parseXml = assets.openXmlResourceParser("AndroidManifest.xml");
            while (parseXml.next() != XmlPullParser.END_DOCUMENT)
                if (parseXml.getEventType() == XmlPullParser.START_TAG && parseXml.getName().equals("application"))
                    for (int i = 0; i < parseXml.getAttributeCount(); i++)
                        if (parseXml.getAttributeName(i).equals("roundIcon"))
                            return resourcesForApplication.getDrawableForDensity(Integer.parseInt(parseXml.getAttributeValue(i).substring(1)), iconDpi);
            parseXml.close();
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException | IOException | XmlPullParserException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    static void clearDisabledApps(Context context) {
        setDisabledApps(context, new HashSet<String>());
    }

    static boolean isEnabledForApp(Context context, String comp) {
        return !getDisabledApps(context).contains(comp);
    }

    static void setAppState(Context context, String comp, boolean enabled) {
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
