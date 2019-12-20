package org.point85.app.collector;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextArea;

public class CollectorController {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(CollectorController.class);

	// the collector service
	private CollectorService collectorService;

	// name of a specific collector on this host
	private String collectorName;

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

	void initialize(String collectorName) throws Exception {
		setImages();
		setCollectorName(collectorName);
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
			if (collectorService != null) {
				collectorService.stopDataCollection();
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

	private String getCollectorId() {
		return collectorName != null ? collectorName : "default";
	}

	@FXML
	private void onStartup() {
		postNotification(CollectorLocalizer.instance().getLangString("starting.collector", getCollectorId()));

		// create the collector
		collectorService = new CollectorService(collectorName);

		try {
			// start collector with all HTTP servers
			collectorService.startup();

			// enable buttons
			btShutdown.setDisable(false);
			btStartMonitoring.setDisable(false);
			btStopMonitoring.setDisable(false);
			btRestart.setDisable(false);
			postNotification(CollectorLocalizer.instance().getLangString("started.collector", getCollectorId()));

		} catch (Exception any) {
			AppUtils.showErrorDialog(any);
			String msg = CollectorLocalizer.instance().getErrorString("failed.collector", getCollectorId());
			collectorService.onException(msg, any);
			postNotification(msg);

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
		if (collectorService != null) {
			collectorService.shutdown();

			// disable buttons
			btShutdown.setDisable(true);
			btStartMonitoring.setDisable(true);
			btStopMonitoring.setDisable(true);
			btRestart.setDisable(true);

			postNotification(CollectorLocalizer.instance().getLangString("shutdown.collector", getCollectorId()));
		}
	}

	@FXML
	private void onRestart() throws Exception {
		if (collectorService != null) {
			collectorService.restartDataCollection();
			postNotification(CollectorLocalizer.instance().getLangString("restarted.collector", getCollectorId()));
		}
	}

	@FXML
	private void onStopMonitoring() throws Exception {
		if (collectorService != null) {
			collectorService.unsubscribeFromDataSource();
			postNotification(CollectorLocalizer.instance().getLangString("stopped.events", getCollectorId()));
		}
	}

	@FXML
	private void onStartMonitoring() throws Exception {
		if (collectorService != null) {
			collectorService.subscribeToDataSource();
			postNotification(CollectorLocalizer.instance().getLangString("started.events", getCollectorId()));
		}
	}

	public String getCollectorName() {
		return collectorName;
	}

	public void setCollectorName(String collectorName) {
		this.collectorName = collectorName;
	}
}
