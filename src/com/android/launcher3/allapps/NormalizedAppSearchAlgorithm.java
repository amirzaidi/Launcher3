package com.android.launcher3.allapps;

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
		final String queryTextLower = query.toLowerCase();

		// Normalize the query before matching begins
		final String queryTextNormalized = unicodeComplementaryGlyphs.matcher(
				Normalizer.normalize(queryTextLower, Normalizer.Form.NFKD)
		).replaceAll("");

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
		int titleLength = title.length();

		if (titleLength < queryLength || queryLength <= 0) {
			return false;
		}

		final String normalizedTitle = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

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
}
