package org.point85.app.collector;

import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class CollectorApplication {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(CollectorApplication.class);

	// the collector
	private CollectorServer collector;

	@FXML
	private Button btStartup;

	@FXML
	private Button btShutdown;

	@FXML
	private Button btRestart;

	@FXML
	private Button btStopMonitoring;

	@FXML
	private Button btStartMonitoring;

	public CollectorApplication() {

	}

	public void start(Stage primaryStage) {
		try {
			AnchorPane mainLayout = (AnchorPane) FXMLLoader.load(getClass().getResource("CollectorApplication.fxml"));

			// Show the scene containing the root layout.
			Scene scene = new Scene(mainLayout);

			primaryStage.setScene(scene);
			primaryStage.setTitle("OEE Collector Application");
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
			stop();
		}
	}

	// called by Java FX
	public void initialize() throws Exception {
		setImages();
	}

	private void setImages() throws Exception {
		// startup
		btStartup.setGraphic(ImageManager.instance().getImageView(Images.STARTUP));
		btStartup.setContentDisplay(ContentDisplay.LEFT);

		// shutdown
		btShutdown.setGraphic(ImageManager.instance().getImageView(Images.SHUTDOWN));
		btShutdown.setContentDisplay(ContentDisplay.LEFT);

		// start
		btStartMonitoring.setGraphic(ImageManager.instance().getImageView(Images.START));
		btStartMonitoring.setContentDisplay(ContentDisplay.LEFT);

		// stop
		btStopMonitoring.setGraphic(ImageManager.instance().getImageView(Images.STOP));
		btStopMonitoring.setContentDisplay(ContentDisplay.LEFT);

		// restart
		btRestart.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
		btRestart.setContentDisplay(ContentDisplay.LEFT);
	}

	public void stop() {
		try {
			if (collector != null) {
				collector.stopDataCollection();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void onStartup() {
		// create the collector
		collector = new CollectorServer();

		try {
			// start collector
			collector.startup();

			// enable buttons
			btShutdown.setDisable(false);
			btStartMonitoring.setDisable(false);
			btStopMonitoring.setDisable(false);
			btRestart.setDisable(false);

		} catch (Exception any) {
			logger.error(any.getMessage());
			any.printStackTrace();
			try {
				onShutdown();
			} catch (Exception e) {
				logger.error(any.getMessage());
				e.printStackTrace();
			}
		}
	};

	@FXML
	private void onShutdown() throws Exception {
		if (collector != null) {
			collector.shutdown();
		}
	}

	@FXML
	private void onRestart() throws Exception {
		if (collector != null) {
			collector.restartDataCollection();
		}
	}

	@FXML
	private void onStopMonitoring() throws Exception {
		if (collector != null) {
			collector.unsubscribeFromDataSource();
		}
	}

	@FXML
	private void onStartMonitoring() throws Exception {
		if (collector != null) {
			collector.subscribeToDataSource();
		}
	}
}
