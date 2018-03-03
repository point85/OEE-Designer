package org.point85.app.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.point85.app.AppUtils;
import org.point85.app.LoaderFactory;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.http.EquipmentEventRequestDto;
import org.point85.domain.http.HttpEventListener;
import org.point85.domain.http.HttpSource;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.script.ScriptResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

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

public class HttpTrendController extends DesignerDialogController implements HttpEventListener, DataSubscriber {
	// logger
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// http server
	private OeeHttpServer httpServer;

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
	private TextField tfLoopbackValue;

	@FXML
	private Button btLoopback;

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
		}
		return spTrendChart;
	}

	// images for buttons
	@Override
	protected void setImages() throws Exception {
		super.setImages();
	}

	public void setScriptResolver(ScriptResolver scriptResolver) throws Exception {
		trendChartController.setScriptResolver(scriptResolver);

		lbSourceId.setText("Equipment: " + scriptResolver.getEquipment().getName() + ", Source Id: "
				+ scriptResolver.getSourceId());
	}

	@Override
	@FXML
	protected void onOK() {
		onStopServer();
		super.onOK();
	}

	@Override
	@FXML
	protected void onCancel() {
		onStopServer();
		super.onCancel();
	}

	public void onStartServer() {
		try {
			if (httpServer == null) {
				piConnection.setVisible(true);

				HttpSource dataSource = (HttpSource) trendChartController.getScriptResolver().getDataSource();

				int port = dataSource.getPort();

				httpServer = new OeeHttpServer(port);
				httpServer.setDataChangeListener(this);
				httpServer.startup();
				lbState.setText(httpServer.getState().toString());
				lbState.setTextFill(STARTED_COLOR);

				// start the trend
				trendChartController.onStartTrending();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		} finally {
			piConnection.setVisible(false);
		}
	}

	private void onStopServer() {
		if (httpServer != null) {
			// stop the trend
			trendChartController.onStopTrending();

			httpServer.shutdown();
			lbState.setText(httpServer.getState().toString());
			lbState.setTextFill(STOPPED_COLOR);
			httpServer = null;
		}
	}

	@Override
	public boolean isSubscribed() {
		return httpServer.isAcceptingEventRequests();
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		httpServer.setAcceptingEventRequests(true);
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (httpServer != null) {
			httpServer.setAcceptingEventRequests(false);
		}
	}

	@Override
	public void onHttpEquipmentEvent(String sourceId, String dataValue, OffsetDateTime timestamp) {
		ResolutionService service = new ResolutionService(sourceId, dataValue, timestamp);

		service.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				Throwable t = event.getSource().getException();

				if (t != null) {
					// connection failed
					logger.error(t.getMessage());
				}
			}
		});

		// run on application thread
		service.start();
	}

	@FXML
	private void onLoopbackTest() {
		try {
			// POST event
			ScriptResolver scriptResolver = trendChartController.getScriptResolver();
			HttpSource dataSource = (HttpSource) scriptResolver.getDataSource();

			URL url = new URL(
					"http://" + dataSource.getHost() + ":" + dataSource.getPort() + '/' + OeeHttpServer.EVENT_EP);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			String value = tfLoopbackValue.getText();

			OffsetDateTime odt = OffsetDateTime.now();
			String timestamp = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

			EquipmentEventRequestDto dto = new EquipmentEventRequestDto(scriptResolver.getSourceId(), value, timestamp);
			Gson gson = new Gson();
			String payload = gson.toJson(dto);

			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			int codeGroup = conn.getResponseCode() / 100;

			if (codeGroup != 2) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			if (logger.isInfoEnabled()) {
				logger.info("Server returned code " + conn.getResponseCode());
				BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

				String output;
				logger.info("Equipment event response ...");
				while ((output = br.readLine()) != null) {
					logger.info(output);
				}
			}

			conn.disconnect();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<Void> {
		private String sourceId;
		private String dataValue;
		private OffsetDateTime timestamp;

		public ResolutionService(String sourceId, String dataValue, OffsetDateTime timestamp) {
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
