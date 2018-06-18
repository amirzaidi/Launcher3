package com.google.android.apps.nexuslauncher;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.android.launcher3.IconProvider;
import com.android.launcher3.compat.ReflectedSdkLoader;

import java.lang.reflect.Field;

public class AdaptiveIconProvider extends IconProvider {
    private static Field sActivityInfo;

    static {
        if (ReflectedSdkLoader.sFeatureLevel == ReflectedSdkLoader.FEATURE_LEVEL.O) {
            try {
                sActivityInfo = LauncherActivityInfo.class.getDeclaredField("mActivityInfo");
                sActivityInfo.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    private final Context mContext;
    private final PackageManager mPm;

    public AdaptiveIconProvider(Context context) {
        mContext = context;
        mPm = mContext.getPackageManager();
    }

    @Override
    public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
        Drawable drawable = null;
        if (ReflectedSdkLoader.sFeatureLevel == ReflectedSdkLoader.FEATURE_LEVEL.O) {
            // Best implementation that extracts the drawable from the manifest
            drawable = CustomIconUtils.extractIconByTag(mPm, info.getComponentName(), iconDpi, "icon");
            if (drawable == null && sActivityInfo != null) {
                try {
                    // Fallback implementation which does not work for recursive references
                    ActivityInfo reflectedInfo = (ActivityInfo) sActivityInfo.get(info);
                    final int iconRes = reflectedInfo.getIconResource();
                    if (iconDpi != 0 && iconRes != 0) {
                        final Resources resources = mPm.getResourcesForApplication(reflectedInfo.applicationInfo);
                        ReflectedSdkLoader.loadLatestSupported(resources);
                        drawable = resources.getDrawableForDensity(iconRes, iconDpi);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return drawable == null
                ? super.getIcon(info, iconDpi, flattenDrawable)
                : drawable.mutate();
    }
}
