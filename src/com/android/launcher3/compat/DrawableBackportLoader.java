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
public class DrawableBackportLoader {
    private static int sFeatureLevel = -1;

    private static Method sSetConfiguration;
    private static Method sGetDrawableInflater;
    private static Class<?> sDrawableInflater;
    private static Field sClassLoaderField;
    private static ClassLoader sAdaptiveClassLoader;

    private static boolean sIconShapeOverride;

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
                sFeatureLevel = Build.VERSION_CODES.N_MR1;

                // This allows converting Oreo drawables into working adaptive icons.
                sGetDrawableInflater = Resources.class.getDeclaredMethod("getDrawableInflater");
                sDrawableInflater = DrawableBackportLoader.class.getClassLoader().loadClass("android.graphics.drawable.DrawableInflater");
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
                sFeatureLevel = Build.VERSION_CODES.O;
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            }
        }
    }

    public static void setIconShapeOverride(boolean enabled) {
        sIconShapeOverride = enabled;
    }

    public static boolean supportsAdaptiveBackport() {
        return sFeatureLevel >= Build.VERSION_CODES.O;
    }

    public static boolean adaptiveBackportEnabled() {
        return supportsAdaptiveBackport() && sIconShapeOverride;
    }

    /**
     * Apply the reflected methods on the Resources instance.
     * This will allow loading shortcut drawables from the v26 or v25 directory.
     * It will also make the classloader load an AdaptiveIconDrawable for the adaptive-icon tag on Nougat.
     */
    public static void setLatestSupported(Resources res) {
        if (adaptiveBackportEnabled()) {
            overrideSdk(res, Build.VERSION_CODES.O);
            enableAdaptiveIcons(res);
        } else if (sFeatureLevel > 0) {
            overrideSdk(res, Build.VERSION_CODES.N_MR1);
        }
    }

    /**
     * Will go through the possible reflection levels to try to load the best drawable.
     */
    public static Drawable loadLatestDrawable(Resources res, int id, int density) {
        switch (sFeatureLevel) {
            case Build.VERSION_CODES.O:
                if (adaptiveBackportEnabled()) {
                    overrideSdk(res, Build.VERSION_CODES.O);
                    enableAdaptiveIcons(res);
                    try {
                        return res.getDrawableForDensity(id, density);
                    } catch (Resources.NotFoundException ignored) {
                    }
                }
            case Build.VERSION_CODES.N_MR1:
                // Fall back to N_MR1
                overrideSdk(res, Build.VERSION_CODES.N_MR1);
                try {
                    return res.getDrawableForDensity(id, density);
                } catch (Resources.NotFoundException ignored) {
                }
                // Reset to default SDK version
                overrideSdk(res, Build.VERSION.SDK_INT);
            default:
                return res.getDrawableForDensity(id, density);
        }
    }

    private static void enableAdaptiveIcons(Resources res) {
        try {
            sClassLoaderField.set(sGetDrawableInflater.invoke(res), sAdaptiveClassLoader);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
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
}
