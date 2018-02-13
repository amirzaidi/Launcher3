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

public class CustomDrawableFactory extends DynamicDrawableFactory implements Runnable {
    private final Context mContext;
    private final BroadcastReceiver mAutoUpdatePack;
    private boolean mRegistered = false;

    String iconPack;
    final Map<String, Integer> packComponents = new HashMap<>();
    final Map<String, String> packCalendars = new HashMap<>();

    private Thread mThread;

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
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_CHANGED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_REMOVED),
                    null,
                    new Handler(LauncherModel.getWorkerLooper()));
            mRegistered = true;
        }

        packComponents.clear();
        packCalendars.clear();

        mThread = new Thread(this);
        mThread.start();
    }

    synchronized void ensureIconPackCached() {
        if (mThread != null) {
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
        }
    }

    @Override
    public void run() {
        if (CustomIconUtils.isPackProvider(mContext, iconPack)) {
            CustomIconPackParser.parse(packComponents, packCalendars, mContext.getPackageManager(), iconPack);
        }
    }

    @Override
    public FastBitmapDrawable newIcon(Bitmap icon, ItemInfo info) {
        ensureIconPackCached();
        if (packComponents.containsKey(DynamicClock.DESK_CLOCK.toString())) {
            return new FastBitmapDrawable(icon);
        }
        return super.newIcon(icon, info);
    }
}
