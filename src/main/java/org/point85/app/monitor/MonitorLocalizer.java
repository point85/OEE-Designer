package org.point85.app.monitor;

import org.point85.domain.i18n.Localizer;

/**
 * Provides localization services for the Monitor application classes
 */
public class MonitorLocalizer extends Localizer {
	// name of resource bundle with translatable strings for text
	private static final String LANG_BUNDLE_NAME = "org.point85.i18n.MonitorLang";

	// name of resource bundle with translatable strings for exception messages
	private static final String ERROR_BUNDLE_NAME = "org.point85.i18n.MonitorError";

	// Singleton
	private static MonitorLocalizer localizer;

	private MonitorLocalizer() {
		setLangBundle(LANG_BUNDLE_NAME);
		setErrorBundle(ERROR_BUNDLE_NAME);
	}

	public static MonitorLocalizer instance() {
		if (localizer == null) {
			localizer = new MonitorLocalizer();
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
