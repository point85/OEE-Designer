package org.point85.app.web;

import java.time.OffsetDateTime;

import org.point85.app.AppUtils;
import org.point85.app.LoaderFactory;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.script.ScriptResolver;
import org.point85.domain.web.WebSource;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;

public class WebTrendController extends DesignerDialogController implements DataSubscriber {
	private static final String WEB_SERVER_STATE = "RUNNING";

	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	// request item
	@FXML
	private Label lbSourceId;

	@FXML
	private Label lbState;

	@FXML
	private TextField tfValue;

	@FXML
	private Button btTest;

	private boolean isSubscribed = true;

	// connection status
	@FXML
	private ProgressIndicator piConnection;

	public SplitPane initializeTrend() throws Exception {
		if (trendChartController == null) {
			// Load the fxml file and create the anchor pane
			FXMLLoader loader = LoaderFactory.trendChartLoader();
			spTrendChart = (SplitPane) loader.getRoot();

			trendChartController = loader.getController();
			trendChartController.initialize(getApp());

			// data provider
			trendChartController.setProvider(this);

			setImages();

			lbState.setText(WEB_SERVER_STATE);
			lbState.setTextFill(STARTED_COLOR);
		}
		return spTrendChart;
	}

	public void setScriptResolver(ScriptResolver scriptResolver) throws Exception {
		trendChartController.setScriptResolver(scriptResolver);

		lbSourceId.setText("Equipment: " + scriptResolver.getEquipment().getName() + ", Source Id: "
				+ scriptResolver.getSourceId());
	}

	@Override
	@FXML
	protected void onOK() {
		// onStopServer();
		super.onOK();
	}

	@Override
	@FXML
	protected void onCancel() {
		// onStopServer();
		super.onCancel();
	}

	/*
	 * public void onStartServer() { try { if (httpServer == null) {
	 * piConnection.setVisible(true);
	 * 
	 * HttpSource dataSource = (HttpSource)
	 * trendChartController.getScriptResolver().getDataSource();
	 * 
	 * int port = dataSource.getPort();
	 * 
	 * httpServer = new OeeHttpServer(port); httpServer.setDataChangeListener(this);
	 * httpServer.startup(); lbState.setText(httpServer.getState().toString());
	 * lbState.setTextFill(STARTED_COLOR);
	 * 
	 * // start the trend trendChartController.onStartTrending(); } } catch
	 * (Exception e) { AppUtils.showErrorDialog(e); } finally {
	 * piConnection.setVisible(false); } }
	 */

	/*
	 * private void onStopServer() { if (httpServer != null) { // stop the trend
	 * trendChartController.onStopTrending();
	 * 
	 * httpServer.shutdown(); lbState.setText(httpServer.getState().toString());
	 * lbState.setTextFill(STOPPED_COLOR); httpServer = null; } }
	 */

	@Override
	public boolean isSubscribed() {
		return isSubscribed;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		isSubscribed = true;
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		isSubscribed = false;
	}

	@FXML
	private void onTest() {
		try {
			ScriptResolver scriptResolver = trendChartController.getScriptResolver();
			WebSource dataSource = (WebSource) scriptResolver.getDataSource();

			String dataValue = tfValue.getText();

			OffsetDateTime timestamp = OffsetDateTime.now();

			ResolutionService service = new ResolutionService(scriptResolver.getSourceId(), dataValue, timestamp);

			service.setOnFailed(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
					Throwable t = event.getSource().getException();

					if (t != null) {
						// connection failed
						t.printStackTrace();
					}
				}
			});

			// run on application thread
			service.start();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<Void> {
		private String sourceId;
		private String dataValue;
		private OffsetDateTime timestamp;

		private ResolutionService(String sourceId, String dataValue, OffsetDateTime timestamp) {
			this.sourceId = sourceId;
			this.dataValue = dataValue;
			this.timestamp = timestamp;
		}

		@Override
		protected Task<Void> createTask() {
			Task<Void> resolutionTask = new Task<Void>() {

				@Override
				protected Void call() {
					try {
						trendChartController.invokeResolver(getApp().getAppContext(), dataValue, timestamp);
					} catch (Exception e) {
						Platform.runLater(() -> {
							AppUtils.showErrorDialog(e);
						});
					}
					return null;
				}
			};
			return resolutionTask;
		}
	}
}
