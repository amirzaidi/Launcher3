package android.graphics.drawable;

import android.content.res.Resources;
import android.support.annotation.Nullable;

import com.android.launcher3.graphics.AdaptiveIconDrawableCompat;

public class AdaptiveIconDrawable extends AdaptiveIconDrawableCompat {
    public AdaptiveIconDrawable() {
        super();
    }

    public AdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
        super(backgroundDrawable, foregroundDrawable);
    }

    public AdaptiveIconDrawable(@Nullable LayerState state, @Nullable Resources res) {
        super(state, res);
    }
}
