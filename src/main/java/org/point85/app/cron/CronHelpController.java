package org.point85.app.cron;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.point85.app.AppUtils;
import org.point85.app.designer.DesignerDialogController;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public class CronHelpController extends DesignerDialogController {
	private static final String FILE_NAME = "config/help/CronExpression.html";

	@FXML
	private WebView helpView;

	public void readHelpFile() {
		try {
			// read help text
			File help = new File(FILE_NAME);
			Path path = Paths.get(help.getCanonicalPath());
			byte[] bytes = Files.readAllBytes(path);
			String helpText = new String(bytes);

			helpView.getEngine().loadContent(helpText);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
