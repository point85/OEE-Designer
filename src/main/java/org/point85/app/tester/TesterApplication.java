package org.point85.app.tester;

import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class TesterApplication {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(TesterApplication.class);

	// controller for main screen
	private TesterController testerController;

	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = FXMLLoaderFactory.testerApplicationLoader();
			AnchorPane mainLayout = loader.getRoot();
			
			testerController = loader.getController();
			testerController.initialize();

			Scene scene = new Scene(mainLayout);

			primaryStage.setTitle(TesterLocalizer.instance().getLangString("test.app.title"));
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			logger.error(e.getMessage());
			stop();
		}
	}

	public void stop() {
		if (testerController != null) {
			testerController.stop();
		}
	}
}
