package com.android.launcher3.allapps;

import android.content.ComponentName;
import android.test.InstrumentationTestCase;

import com.android.launcher3.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vmacias on 9/30/17.
 */

public class NormalizedAppSearchAlgorithmTest extends InstrumentationTestCase {

	private List<AppInfo> mAppsList;
	private NormalizedAppSearchAlgorithm mAlgorithm;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mAppsList = new ArrayList<>();
		getInstrumentation().runOnMainSync(new Runnable() {
			@Override
			public void run() {
				mAlgorithm = new NormalizedAppSearchAlgorithm(mAppsList);
			}
		});
	}

	public void testMatches() {
		assertTrue(mAlgorithm.matches(getInfo("white cow"), "cow"));
		assertTrue(mAlgorithm.matches(getInfo("whiteCow"), "cow"));
		assertTrue(mAlgorithm.matches(getInfo("whiteCOW"), "cow"));
		assertTrue(mAlgorithm.matches(getInfo("whitecowCOW"), "cow"));
		assertTrue(mAlgorithm.matches(getInfo("white2cow"), "cow"));

		assertFalse(mAlgorithm.matches(getInfo("whitecow"), "cow"));
		assertFalse(mAlgorithm.matches(getInfo("whitEcow"), "cow"));

		assertTrue(mAlgorithm.matches(getInfo("whitecowCow"), "cow"));
		assertTrue(mAlgorithm.matches(getInfo("whitecow cow"), "cow"));
		assertFalse(mAlgorithm.matches(getInfo("whitecowcow"), "cow"));
		assertFalse(mAlgorithm.matches(getInfo("whit ecowcow"), "cow"));

		assertTrue(mAlgorithm.matches(getInfo("cats&dogs"), "dog"));
		assertTrue(mAlgorithm.matches(getInfo("cats&Dogs"), "dog"));
		assertTrue(mAlgorithm.matches(getInfo("cats&Dogs"), "&"));

		assertTrue(mAlgorithm.matches(getInfo("2+43"), "43"));
		assertFalse(mAlgorithm.matches(getInfo("2+43"), "3"));

		assertTrue(mAlgorithm.matches(getInfo("Q"), "q"));
		assertTrue(mAlgorithm.matches(getInfo("  Q"), "q"));

		// match lower case words
		assertTrue(mAlgorithm.matches(getInfo("elephant"), "e"));

		assertTrue(mAlgorithm.matches(getInfo("电子邮件"), "电"));
		assertTrue(mAlgorithm.matches(getInfo("电子邮件"), "电子"));
		assertFalse(mAlgorithm.matches(getInfo("电子邮件"), "子"));
		assertFalse(mAlgorithm.matches(getInfo("电子邮件"), "邮件"));


		// What about accents? We assume a Normalized query here

		String normalizedQuery = mAlgorithm.normalizeStringForSearch("Play Mús");

		assertTrue(mAlgorithm.matches(getInfo("Play Música"), normalizedQuery));

		normalizedQuery = mAlgorithm.normalizeStringForSearch("Play Mus");

		assertTrue(mAlgorithm.matches(getInfo("Play Música"), normalizedQuery));

		normalizedQuery = mAlgorithm.normalizeStringForSearch("Cinép");

		assertTrue(mAlgorithm.matches(getInfo("Cinépolis"), normalizedQuery));

		normalizedQuery = mAlgorithm.normalizeStringForSearch("Cinep");

		assertTrue(mAlgorithm.matches(getInfo("Cinépolis"), normalizedQuery));

	}

	private AppInfo getInfo(String title) {
		AppInfo info = new AppInfo();
		info.title = title;
		info.componentName = new ComponentName("Test", title);
		return info;
	}

}
