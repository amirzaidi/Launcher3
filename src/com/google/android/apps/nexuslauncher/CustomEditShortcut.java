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
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.widget.WidgetsBottomSheet;

import java.util.ArrayList;
import java.util.List;

public class CustomEditShortcut extends SystemShortcut {
    private AlertDialog picker;

    private enum EditSelection {
        DisablePack,
        EnablePack,
        HideApp
    }

    public CustomEditShortcut() {
        super(R.drawable.ic_edit_no_shadow, R.string.action_edit);
    }

    @Override
    public View.OnClickListener getOnClickListener(final Launcher launcher, final ItemInfo itemInfo) {
        if (CustomIconUtils.isPackProvider(launcher, CustomIconUtils.getCurrentPack(launcher))) {
            CustomDrawableFactory factory = (CustomDrawableFactory) DrawableFactory.get(launcher);
            factory.ensureInitialLoadComplete();

            final String comp = itemInfo.getTargetComponent().toString();
            if (factory.packCalendars.containsKey(comp) || factory.packComponents.containsKey(comp) || itemInfo.container == ItemInfo.NO_ID) {
                return new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AbstractFloatingView.closeAllOpenViews(launcher);
                        CustomBottomSheet sheet = (CustomBottomSheet) launcher.getLayoutInflater().inflate(R.layout.app_edit_bottom_sheet, launcher.getDragLayer(), false);
                        sheet.populateAndShow(itemInfo);
                    }
                };
            }

            /*final Resources res = launcher.getResources();
            final List<EditSelection> values = new ArrayList<>();

            values.add(EditSelection.DisablePack);
            if (factory.packCalendars.containsKey(comp) || factory.packComponents.containsKey(comp)) {
                values.add(EditSelection.EnablePack);
            }
            if (itemInfo.container == ItemInfo.NO_ID) {
                values.add(EditSelection.HideApp);
            }
            if (values.size() > 1) {
                final String pkg = itemInfo.getTargetComponent().getPackageName();

                AlertDialog.Builder builder = new AlertDialog.Builder(launcher);
                builder.setSingleChoiceItems(titles,
                        values.contains(EditSelection.EnablePack) && CustomIconPack.isEnabledForApp(launcher, comp) ? 1 : 0,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        switch (values.get(item)) {
                            case DisablePack:
                                if (values.contains(EditSelection.EnablePack)) {
                                    CustomIconPack.disableForApp(launcher, comp);
                                    CustomIconUtils.reloadIcons(launcher, pkg);
                                }
                                break;
                            case EnablePack:
                                CustomIconPack.enableForApp(launcher, comp);
                                CustomIconUtils.reloadIcons(launcher, pkg);
                                break;
                            case HideApp:
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

                return new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AbstractFloatingView.closeAllOpenViews(launcher);
                        picker.show();
                    }
                };
            }*/
        }

        return null;
    }
}
