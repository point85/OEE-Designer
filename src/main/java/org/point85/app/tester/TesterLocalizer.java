package org.point85.app.tester;

import org.point85.domain.i18n.Localizer;

/**
 * Provides localization services for the Tester application classes
 */
public class TesterLocalizer extends Localizer {
	// name of resource bundle with translatable strings for text
	private static final String LANG_BUNDLE_NAME = "org.point85.i18n.TesterLang";

	// name of resource bundle with translatable strings for exception messages
	private static final String ERROR_BUNDLE_NAME = "org.point85.i18n.TesterError";

	// Singleton
	private static TesterLocalizer localizer;

	private TesterLocalizer() {
		setLangBundle(LANG_BUNDLE_NAME);
		setErrorBundle(ERROR_BUNDLE_NAME);
	}

	public static TesterLocalizer instance() {
		if (localizer == null) {
			localizer = new TesterLocalizer();
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
