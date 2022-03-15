package org.point85.app.cron;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.point85.app.AppUtils;
import org.point85.app.designer.DesignerDialogController;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public class CronHelpController extends DesignerDialogController {

	@FXML
	private WebView helpView;

	public void readHelpFile() {
		// read help text
		URL url = getClass().getResource("/help/CronExpression.html");

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {

			String helpText = "";
			String line;
			while ((line = reader.readLine()) != null) {
				helpText += line;
			}

			reader.close();

			helpView.getEngine().loadContent(helpText);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
