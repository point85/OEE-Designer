package org.point85.app.collector;

import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class CollectorApplication {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(CollectorApplication.class);

	// main controller
	private CollectorController collectorController;

	public CollectorApplication() {
	}

	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = FXMLLoaderFactory.collectorApplicationLoader();
			AnchorPane mainLayout = loader.getRoot();
			
			collectorController = loader.getController();
			collectorController.initialize();

			Scene scene = new Scene(mainLayout);

			primaryStage.setTitle(CollectorLocalizer.instance().getLangString("collector.app.title"));
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			logger.error(e.getMessage());
			stop();
		}
	}

	public void stop() {
		if (collectorController != null) {
			collectorController.stop();
		}
	}
}
