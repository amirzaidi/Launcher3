package com.android.launcher3;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import java.util.Locale;

public class IconProvider {

    private static final boolean DBG = false;
    private static final String TAG = "IconProvider";
    protected final Context mContext;

    protected String mSystemState;

    public IconProvider(Context context) {
        mContext = context;
        updateSystemStateString();
    }

    public void updateSystemStateString() {
        mSystemState = Locale.getDefault().toString() + "," + Build.VERSION.SDK_INT;
    }

    public String getIconSystemState(String packageName) {
        return mSystemState;
    }

    /**
     * @param flattenDrawable true if the caller does not care about the specification of the
     *                        original icon as long as the flattened version looks the same.
     */
    public Drawable getIcon(LauncherActivityInfo info, int iconDpi, boolean flattenDrawable) {
        try {
            PackageManager pm = mContext.getPackageManager();
            return pm.getActivityIcon(info.getComponentName());
        } catch (PackageManager.NameNotFoundException e) {
            return info.getIcon(iconDpi);
        }
    }
}
