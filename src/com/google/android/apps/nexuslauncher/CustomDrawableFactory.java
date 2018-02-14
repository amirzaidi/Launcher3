package com.google.android.apps.nexuslauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;

import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherModel;
import com.google.android.apps.nexuslauncher.clock.DynamicClock;
import com.google.android.apps.nexuslauncher.utils.ActionIntentFilter;

import java.util.HashMap;
import java.util.Map;

public class CustomDrawableFactory extends DynamicDrawableFactory {
    private final Context mContext;
    private final BroadcastReceiver mAutoUpdatePack;
    private boolean mRegistered = false;

    String iconPack;
    final Map<String, Integer> packComponents = new HashMap<>();
    final Map<String, String> packCalendars = new HashMap<>();

    public CustomDrawableFactory(Context context) {
        super(context);
        mContext = context;
        mAutoUpdatePack = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!CustomIconUtils.isPackProvider(context, CustomIconUtils.getCurrentPack(context))) {
                    CustomIconUtils.setCurrentPack(context, "");
                }
                CustomIconUtils.applyIconPack(context);
            }
        };

        reloadIconPackCache();
    }

    synchronized void reloadIconPackCache() {
        iconPack = CustomIconUtils.getCurrentPack(mContext);

        if (mRegistered) {
            mContext.unregisterReceiver(mAutoUpdatePack);
            mRegistered = false;
        }
        if (!iconPack.isEmpty()) {
            mContext.registerReceiver(mAutoUpdatePack, ActionIntentFilter.newInstance(iconPack,
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_FULLY_REMOVED),
                    null,
                    new Handler(LauncherModel.getWorkerLooper()));
            mRegistered = true;
        }

        packComponents.clear();
        packCalendars.clear();

        if (CustomIconUtils.isPackProvider(mContext, iconPack)) {
            CustomIconPackParser.parse(packComponents, packCalendars, mContext.getPackageManager(), iconPack);
        }
    }

    synchronized void ensureIconPackCached() {
    }

    @Override
    public FastBitmapDrawable newIcon(Bitmap icon, ItemInfo info) {
        ensureIconPackCached();
        String clockComp = DynamicClock.DESK_CLOCK.toString();
        if (packComponents.containsKey(DynamicClock.DESK_CLOCK.toString()) && CustomIconPackParser.enabledIconPack(mContext, clockComp)) {
            return new FastBitmapDrawable(icon);
        }
        return super.newIcon(icon, info);
    }
}
