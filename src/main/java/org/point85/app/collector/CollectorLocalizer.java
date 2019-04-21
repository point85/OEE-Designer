package org.point85.app.collector;

import org.point85.domain.i18n.Localizer;

/**
 * Provides localization services for the Collector application classes
 */
public class CollectorLocalizer extends Localizer {
	// name of resource bundle with translatable strings for text
	private static final String LANG_BUNDLE_NAME = "org.point85.i18n.CollectorLang";

	// name of resource bundle with translatable strings for exception messages
	private static final String ERROR_BUNDLE_NAME = "org.point85.i18n.CollectorError";

	// Singleton
	private static CollectorLocalizer localizer;

	private CollectorLocalizer() {
		setLangBundle(LANG_BUNDLE_NAME);
		setErrorBundle(ERROR_BUNDLE_NAME);
	}

	public static CollectorLocalizer instance() {
		if (localizer == null) {
			localizer = new CollectorLocalizer();
		}
		return localizer;
	}

	@Override
	public String getLangBundleName() {
		return LANG_BUNDLE_NAME;
	}
	
	@Override
	public String getErrorBundleName() {
		return ERROR_BUNDLE_NAME;
	}
}
