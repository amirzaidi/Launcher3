package com.android.launcher3;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;

public class LockScreen extends DeviceAdminReceiver {

    public LockScreen() {
        super();
    }

    public LockScreen(final Context context) {
        super();
        DevicePolicyManager dPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            dPM.lockNow();
        } catch (SecurityException se) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getWho(context));
            context.startActivity(intent);
        }
    }
}
