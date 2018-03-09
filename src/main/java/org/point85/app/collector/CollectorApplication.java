package org.point85.app.collector;

import org.point85.domain.collector.CollectorServer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class CollectorApplication {
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

	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("OEE Collector Application");

		AnchorPane mainLayout = (AnchorPane) FXMLLoader.load(getClass().getResource("CollectorApplication.fxml"));

		// Show the scene containing the root layout.
		Scene scene = new Scene(mainLayout);
		primaryStage.setScene(scene);

		// show the converter
		primaryStage.show();
	}

	public void stop() throws Exception {
		if (collector != null) {
			collector.stopDataCollection();
		}
	}

	@FXML
	private void onStartup() {
		Runnable launcher = () -> {
			// create the collector
			collector = new CollectorServer();

			try {
				// start collector
				collector.startup();

			} catch (Exception any) {
				any.printStackTrace();
			} finally {
				try {
					// pause thread
					Thread.sleep(Long.MAX_VALUE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		// start the thread
		new Thread(launcher).start();
	}

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
