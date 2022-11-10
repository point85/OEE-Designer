package org.point85.app.designer;

import java.io.File;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.domain.exim.Exporter;

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

	protected void backupToFile(Class<?> clazz) {
		try {
			// show file chooser
			File file = AppUtils.showFileSaveDialog(getApp().getLastDirectory());

			if (file != null) {
				getApp().setLastDirectory(file.getParentFile());

				// backup
				Exporter.instance().backup(clazz, file);

				AppUtils.showInfoDialog(
						DesignerLocalizer.instance().getLangString("backup.successful", file.getCanonicalPath()));
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
