package org.point85.app.collector;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class CollectorApplication {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(CollectorApplication.class);

	// the collector
	private CollectorService collector;

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

	@FXML
	private TextArea taNotification;

	public CollectorApplication() {
		// nothing to initialize
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
			AppUtils.showErrorDialog(e);
			logger.error(e.getMessage());
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
			AppUtils.showErrorDialog(e);
			logger.error(e.getMessage());
		}
	}

	private void postNotification(String notification) {
		taNotification.clear();
		taNotification.setText(notification);
	}

	@FXML
	private void onStartup() {
		postNotification("Starting collector");
		// create the collector
		collector = new CollectorService();

		try {
			// start collector
			collector.startup();

			// enable buttons
			btShutdown.setDisable(false);
			btStartMonitoring.setDisable(false);
			btStopMonitoring.setDisable(false);
			btRestart.setDisable(false);
			postNotification("Started the collector");

		} catch (Exception any) {
			AppUtils.showErrorDialog(any);
			collector.onException("Failed to start collector", any);
			postNotification("Failed to start collector.  Shutting down.");

			try {
				onShutdown();
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
				logger.error(e.getMessage());
			}
		}
	};

	@FXML
	private void onShutdown() throws Exception {
		if (collector != null) {
			collector.shutdown();

			// disable buttons
			btShutdown.setDisable(true);
			btStartMonitoring.setDisable(true);
			btStopMonitoring.setDisable(true);
			btRestart.setDisable(true);

			postNotification("Shut down the collector");
		}
	}

	@FXML
	private void onRestart() throws Exception {
		if (collector != null) {
			collector.restartDataCollection();
			postNotification("Restarted the collector");
		}
	}

	@FXML
	private void onStopMonitoring() throws Exception {
		if (collector != null) {
			collector.unsubscribeFromDataSource();
			postNotification("Stopped monitoring events");
		}
	}

	@FXML
	private void onStartMonitoring() throws Exception {
		if (collector != null) {
			collector.subscribeToDataSource();
			postNotification("Started monitoring events");
		}
	}
}
