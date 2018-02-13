package com.google.android.apps.nexuslauncher;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.UserHandle;
import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.popup.SystemShortcut;

import java.util.ArrayList;
import java.util.List;

public class CustomEditShortcut extends SystemShortcut {
    public CustomEditShortcut() {
        super(R.drawable.ic_edit_no_shadow, R.string.action_edit);
    }

    @Override
    public View.OnClickListener getOnClickListener(final Launcher launcher, final ItemInfo itemInfo) {
        if (CustomIconUtils.isPackProvider(launcher, CustomIconUtils.getCurrentPack(launcher))) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AbstractFloatingView.closeAllOpenViews(launcher);

                    AlertDialog picker;
                    AlertDialog.Builder builder = new AlertDialog.Builder(launcher);
                    final Resources res = launcher.getResources();
                    List<CharSequence> values = new ArrayList<>();
                    values.add(res.getString(R.string.pref_icon_pack));
                    values.add(res.getString(R.string.icon_shape_system_default));
                    if (itemInfo.container == ItemInfo.NO_ID) {
                        values.add(res.getString(R.string.hide_app_sum));
                    }

                    final String pkg = itemInfo.getTargetComponent().getPackageName();
                    final String comp = itemInfo.getTargetComponent().toString();
                    builder.setSingleChoiceItems(values.toArray(new CharSequence[0]), CustomIconPackParser.enabledIconPack(launcher, comp) ? 0 : 1, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            switch (item) {
                                case 0:
                                    CustomIconPackParser.enableIconPack(launcher, comp);
                                    CustomIconUtils.reloadIcons(launcher, pkg);
                                    break;
                                case 1:
                                    CustomIconPackParser.disableIconPack(launcher, comp);
                                    CustomIconUtils.reloadIcons(launcher, pkg);
                                    break;
                                case 2:
                                    CustomAppFilter.hideComponentName(launcher, comp);
                                    for (UserHandle user : UserManagerCompat.getInstance(launcher).getUserProfiles()) {
                                        if (!LauncherAppsCompat.getInstance(launcher).getActivityList(pkg, user).isEmpty()) {
                                            String[] pkgs = new String[] { pkg };
                                            launcher.getModel().onPackagesUnavailable(pkgs, user, false);
                                            launcher.getModel().onPackagesAvailable(pkgs, user, false);
                                        }
                                    }
                                    break;
                            }
                            dialog.dismiss();
                        }
                    });
                    picker = builder.create();
                    picker.show();
                }
            };
        }

        return null;
    }
}
