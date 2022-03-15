package org.point85.app.operator;

import org.point85.domain.i18n.Localizer;

/**
 * Provides localization services for the Operator application classes
 */
public class OperatorLocalizer extends Localizer {
	// text
	private static final String LANG_BUNDLE_NAME = "i18n.OperatorLang";
	
	// exception strings
	private static final String ERROR_BUNDLE_NAME = "i18n.OperatorError";
	
	// Singleton
	private static OperatorLocalizer localizer;
	
	private OperatorLocalizer() {
		setLangBundle(LANG_BUNDLE_NAME);
		setErrorBundle(ERROR_BUNDLE_NAME);
	}

	public static OperatorLocalizer instance() {
		if (localizer == null) {
			localizer = new OperatorLocalizer();
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
