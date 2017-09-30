package com.android.launcher3.allapps;

/**
 * Created by vmacias on 9/30/17.
 */

public class NormalizedAppSearchController extends DefaultAppSearchController {

	@Override
	public DefaultAppSearchAlgorithm onInitializeSearch() {
		return new NormalizedAppSearchAlgorithm(mApps.getApps());
	}

}
