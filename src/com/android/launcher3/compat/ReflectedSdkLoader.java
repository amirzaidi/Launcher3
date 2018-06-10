/*
 * Credits for this class to Paphonb and The Lawnchair Team.
 */

package com.android.launcher3.compat;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;

import com.android.launcher3.Utilities;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public class ReflectedSdkLoader {
    public enum FEATURE_LEVEL {
        O,
        N_MR1,
        DEFAULT
    }

    public static FEATURE_LEVEL sFeatureLevel = FEATURE_LEVEL.DEFAULT;

    private static Method sSetConfiguration;
    private static Method sGetDrawableInflater;
    private static Class<?> sDrawableInflater;
    private static Field sClassLoaderField;
    private static ClassLoader sAdaptiveClassLoader;

    /**
     * Load all necessary reflection methods once.
     */
    static {
        // Only do this pre-Oreo, there is nothing to backport on Oreo
        if (!Utilities.ATLEAST_OREO) {
            try {
                // This allows loading drawables restricted to newer SDKs
                sSetConfiguration = AssetManager.class.getDeclaredMethod("setConfiguration",
                        /* mcc */ int.class, /* mnc */ int.class, /* locale */ String.class,
                        /* orientation */ int.class, /* touchscreen */ int.class, /* density */ int.class, /* keyboard */ int.class,
                        /* keyboardHidden */ int.class, /* navigation */ int.class, /* screenWidth */ int.class, /* screenHeight */ int.class,
                        /* smallestScreenWidthDp */ int.class, /* screenWidthDp */ int.class, /* screenHeightDp */ int.class,
                        /* screenLayout */ int.class, /* uiMode */ int.class, /* majorVersion */ int.class);
                sFeatureLevel = FEATURE_LEVEL.N_MR1;

                // This allows converting Oreo drawables into working adaptive icons.
                sGetDrawableInflater = Resources.class.getDeclaredMethod("getDrawableInflater");
                sDrawableInflater = ReflectedSdkLoader.class.getClassLoader().loadClass("android.graphics.drawable.DrawableInflater");
                sClassLoaderField = sDrawableInflater.getDeclaredField("mClassLoader");
                sClassLoaderField.setAccessible(true);
                sAdaptiveClassLoader = new ClassLoader() {
                    @SuppressLint("NewApi")
                    @Override
                    public Class<?> loadClass(String name) throws ClassNotFoundException {
                        return "adaptive-icon".equals(name)
                                ? AdaptiveIconDrawable.class
                                : loadClass(name, false);
                    }
                };
                sFeatureLevel = FEATURE_LEVEL.O;
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Apply the reflected methods on the Resources instance.
     * This will allow loading shortcut drawables from the v26 or v25 directory.
     * It will also make the classloader load an AdaptiveIconDrawable for the adaptive-icon tag on Nougat.
     */
    public static void loadLatestSupported(Resources res) {
        if (sFeatureLevel == FEATURE_LEVEL.O) {
            overrideSdk(res, Build.VERSION_CODES.O);
            enableAdaptiveIcons(res);
        } else if (sFeatureLevel == FEATURE_LEVEL.N_MR1) {
            overrideSdk(res, Build.VERSION_CODES.N_MR1);
        }
    }

    /**
     * Will go through the possible reflection levels to try to load the best drawable.
     */
    public static Drawable attemptDrawableLoad(Resources res, int id, int density) {
        switch (sFeatureLevel) {
            case O:
                overrideSdk(res, Build.VERSION_CODES.O);
                enableAdaptiveIcons(res);
                try {
                    return res.getDrawableForDensity(id, density);
                } catch (Resources.NotFoundException ignored) {
                }
            case N_MR1:
                overrideSdk(res, Build.VERSION_CODES.N_MR1);
                try {
                    return res.getDrawableForDensity(id, density);
                } catch (Resources.NotFoundException ignored) {
                }
            default:
                overrideSdk(res, Build.VERSION.SDK_INT);
                return res.getDrawableForDensity(id, density);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static void overrideSdk(Resources res, int version) {
        Configuration config = res.getConfiguration();
        DisplayMetrics dm = res.getDisplayMetrics();
        AssetManager assets = res.getAssets();

        int width, height;
        if (dm.widthPixels >= dm.heightPixels) {
            width = dm.widthPixels;
            height = dm.heightPixels;
        } else {
            width = dm.heightPixels;
            height = dm.widthPixels;
        }

        try {
            sSetConfiguration.invoke(assets, config.mcc, config.mnc,
                    config.locale.toLanguageTag(),
                    config.orientation,
                    config.touchscreen,
                    config.densityDpi, config.keyboard,
                    config.keyboardHidden, config.navigation, width, height,
                    config.smallestScreenWidthDp,
                    config.screenWidthDp, config.screenHeightDp,
                    config.screenLayout, config.uiMode, version);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static void enableAdaptiveIcons(Resources res) {
        try {
            sClassLoaderField.set(sGetDrawableInflater.invoke(res), sAdaptiveClassLoader);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
