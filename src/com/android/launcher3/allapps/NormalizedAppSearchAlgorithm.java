package com.android.launcher3.allapps;

import android.support.annotation.NonNull;

import com.android.launcher3.AppInfo;
import com.android.launcher3.util.ComponentKey;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by vmacias on 9/30/17.
 */

public class NormalizedAppSearchAlgorithm extends DefaultAppSearchAlgorithm {

	private Pattern unicodeComplementaryGlyphs;

	public NormalizedAppSearchAlgorithm(List<AppInfo> apps) {
		super(apps);

		unicodeComplementaryGlyphs = Pattern.compile("\\p{M}");

	}

	@Override
	protected ArrayList<ComponentKey> getTitleMatchResult(String query) {
		// Do an intersection of the words in the query and each title, and filter out all the
		// apps that don't match all of the words in the query.

		// Normalize the query before matching begins
		final String queryTextNormalized = normalizeStringForSearch(query.toLowerCase());

		final ArrayList<ComponentKey> result = new ArrayList<>();
		for (AppInfo info : mApps) {
			if (matches(info, queryTextNormalized)) {
				result.add(info.toComponentKey());
			}
		}
		return result;
	}

	@Override
	protected boolean matches(AppInfo info, String query) {
		int queryLength = query.length();

		String title = info.title.toString();

		final String normalizedTitle = normalizeStringForSearch(title);

		int titleLength = normalizedTitle.length();

		if (titleLength < queryLength || queryLength <= 0) {
			return false;
		}

		int lastType;
		int thisType = Character.UNASSIGNED;
		int nextType = Character.getType(normalizedTitle.codePointAt(0));

		int end = titleLength - queryLength;
		for (int i = 0; i <= end; i++) {
			lastType = thisType;
			thisType = nextType;
			nextType = i < (titleLength - 1) ?
					Character.getType(normalizedTitle.codePointAt(i + 1)) : Character.UNASSIGNED;
			if (isBreak(thisType, lastType, nextType) &&
					normalizedTitle.substring(i, i + queryLength).equalsIgnoreCase(query)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Provides consistent normalization for both queries and component names for filtering
	 * @param stringToNormalize
	 * @return Normalized string for use with {@link NormalizedAppSearchAlgorithm} methods
	 */
	protected String normalizeStringForSearch(@NonNull String stringToNormalize) {

		final String normalizedString = Normalizer.normalize(stringToNormalize, Normalizer.Form.NFKD);

		return unicodeComplementaryGlyphs.matcher(normalizedString).replaceAll("");

	}
}
