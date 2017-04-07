package com.android.launcher3.allapps;


import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.launcher3.Launcher;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.util.ComponentKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredictiveAppsProvider {
    private static final int NUM_PREDICTIVE_APPS_TO_HOLD = 9; // since we can't have more than 9 columns

    private static final String PREDICTIVE_APPS_KEY = "predictive_apps";
    private static final String TOP_PREDICTIVE_APPS_KEY = "top_predictive_apps";

    private SharedPreferences sharedPreferences;
    private Context context;

    public PredictiveAppsProvider(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void updateComponentCount(ComponentName component) {
        String key = buildComponentString(component);
        long current = sharedPreferences.getLong(key, 0);

        sharedPreferences.edit().putLong(key, current + 1).commit();

        // ensure that the set of predictive apps contains this one
        Set<String> predictiveApps =
                sharedPreferences.getStringSet(PREDICTIVE_APPS_KEY, new HashSet<String>());
        if (!predictiveApps.contains(key)) {
            predictiveApps.add(key);
            sharedPreferences.edit().putStringSet(PREDICTIVE_APPS_KEY, predictiveApps).commit();
        }
    }

    public void updateTopPredictedApps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List< PredictedApp > allPredictions = new ArrayList<>();
                Set<String> predictiveAppsSet =
                        sharedPreferences.getStringSet(PREDICTIVE_APPS_KEY, new HashSet<String>());

                for (String s : predictiveAppsSet) {
                    allPredictions.add(new PredictedApp(buildComponentFromString(s),
                            sharedPreferences.getLong(s, 0)));
                }

                Collections.sort(allPredictions, new Comparator<PredictedApp>() {
                    public int compare(PredictedApp result1, PredictedApp result2) {
                        return Long.valueOf(result2.count).compareTo(Long.valueOf(result1.count));
                    }
                });

                if (allPredictions.size() > NUM_PREDICTIVE_APPS_TO_HOLD) {
                    allPredictions = allPredictions.subList(0, NUM_PREDICTIVE_APPS_TO_HOLD);
                }

                sharedPreferences.edit().putString(TOP_PREDICTIVE_APPS_KEY, buildStringFromAppList(allPredictions)).commit();
            }
        }).start();
    }

    public List<ComponentKey> getPredictions() {
        String predictions = sharedPreferences.getString(TOP_PREDICTIVE_APPS_KEY, "");
        if (predictions.isEmpty()) {
            return new ArrayList<>();
        }

        String[] topPredictions = predictions.split(" ");
        List<ComponentKey> keys = new ArrayList<>();

        for (int i = 0; i < topPredictions.length - 1; i++) {
            keys.add(buildComponentKey(topPredictions[i] + " " + topPredictions[i + 1]));
        }

        return keys;
    }

    private String buildStringFromAppList(List<PredictedApp> apps) {
        String string = "";
        for (PredictedApp app : apps) {
            string += buildComponentString(app.component) + " ";
        }

        try {
            return string.substring(0, string.length() - 1);
        } catch (StringIndexOutOfBoundsException e) {
            return "";
        }
    }

    private String buildComponentString(ComponentName component) {
        return component.getPackageName() + " " + component.getClassName();
    }

    private ComponentName buildComponentFromString(String key) {
        String[] arr = key.split(" ");
        return new ComponentName(arr[0], arr[1]);
    }

    private ComponentKey buildComponentKey(String key) {
        return buildComponentKey(buildComponentFromString(key));
    }

    private ComponentKey buildComponentKey(ComponentName component) {
        return new ComponentKey(component, UserHandleCompat.myUserHandle());
    }

    private class PredictedApp {
        public ComponentName component;
        public long count;

        public PredictedApp(ComponentName component, long count) {
            this.component = component;
            this.count = count;
        }
    }
}
