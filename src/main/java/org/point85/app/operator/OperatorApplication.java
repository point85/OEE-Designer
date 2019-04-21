package org.point85.app.operator;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class OperatorApplication {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(OperatorApplication.class);

	// controller for main screen
	private OperatorController operatorController;

	public void start(Stage primaryStage) {
		try {
			// load FXML
			FXMLLoader loader = FXMLLoaderFactory.operatorApplicationLoader();
			AnchorPane mainLayout = (AnchorPane) loader.getRoot();
			
			operatorController = loader.getController();
			operatorController.initialize();

			Scene scene = new Scene(mainLayout);

			primaryStage.setTitle(OperatorLocalizer.instance().getLangString("operator.app.title"));
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));
			primaryStage.setScene(scene);
			primaryStage.show();

			if (logger.isInfoEnabled()) {
				logger.info("Populating top entity nodes.");
			}

			Platform.runLater(() -> {
				try {
					operatorController.populateTopEntityNodes();
				} catch (Exception e) {
					AppUtils.showErrorDialog(
							OperatorLocalizer.instance().getErrorString("no.db.connection") + "  " + e.getMessage());
				}
			});
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void stop() {
	}
}
