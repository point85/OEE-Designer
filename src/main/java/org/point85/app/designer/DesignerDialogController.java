package org.point85.app.designer;

import org.point85.app.DialogController;

import javafx.scene.paint.Color;

public abstract class DesignerDialogController extends DialogController {
	protected static final Color STARTED_COLOR = Color.GREEN;
	protected static final Color STOPPED_COLOR = Color.BLACK;

	// Reference to the main application
	private DesignerApplication app;

	public DesignerApplication getApp() {
		return this.app;
	}

	public void setApp(DesignerApplication app) {
		this.app = app;
	}
}
