package org.point85.app.designer;

import org.point85.domain.i18n.Localizer;

/**
 * Provides localization services for the Designer application classes
 */
public class DesignerLocalizer extends Localizer {
	// name of resource bundle with translatable strings for text
	private static final String LANG_BUNDLE_NAME = "i18n.DesignerLang";

	// name of resource bundle with translatable strings for exception messages
	private static final String ERROR_BUNDLE_NAME = "i18n.DesignerError";

	// Singleton
	private static DesignerLocalizer localizer;

	private DesignerLocalizer() {
		setLangBundle(LANG_BUNDLE_NAME);
		setErrorBundle(ERROR_BUNDLE_NAME);
	}

	public static DesignerLocalizer instance() {
		if (localizer == null) {
			localizer = new DesignerLocalizer();
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
