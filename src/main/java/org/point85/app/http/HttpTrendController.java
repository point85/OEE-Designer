package org.point85.app.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.DomainUtils;
import org.point85.domain.http.EquipmentEventRequestDto;
import org.point85.domain.http.HttpEventListener;
import org.point85.domain.http.HttpSource;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.script.EventResolver;
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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;

public class HttpTrendController extends DesignerDialogController implements HttpEventListener, DataSubscriber {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(HttpTrendController.class);

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
			FXMLLoader loader = FXMLLoaderFactory.trendChartLoader();
			spTrendChart = (SplitPane) loader.getRoot();

			trendChartController = loader.getController();
			trendChartController.initialize(getApp());

			// data provider
			trendChartController.setProvider(this);

			setImages();
		}
		return spTrendChart;
	}

	@Override
	protected void setImages() throws Exception {
		super.setImages();

		// loopback test
		btLoopback.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btLoopback.setContentDisplay(ContentDisplay.LEFT);
	}

	public void setScriptResolver(EventResolver eventResolver) throws Exception {
		eventResolver.setWatchMode(true);
		trendChartController.setScriptResolver(eventResolver);

		lbSourceId.setText(
				"Equipment: " + eventResolver.getEquipment().getName() + ", Source Id: " + eventResolver.getSourceId());
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

				HttpSource dataSource = (HttpSource) trendChartController.getEventResolver().getDataSource();

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
		HttpURLConnection conn = null;
		try {
			// get the HTTP data source
			EventResolver eventResolver = trendChartController.getEventResolver();
			HttpSource dataSource = (HttpSource) eventResolver.getDataSource();

			// build the URL for an equipment event
			URL url = new URL(
					"http://" + dataSource.getHost() + ":" + dataSource.getPort() + '/' + OeeHttpServer.EVENT_EP);

			// create a connection for a JSON POST request
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			// the value to send (must match the configured resolver)
			String value = tfLoopbackValue.getText();

			// timestamp when sent
			String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now());

			// create the data transfer event object
			EquipmentEventRequestDto dto = new EquipmentEventRequestDto(eventResolver.getSourceId(), value, timestamp);

			// serialize the body
			Gson gson = new Gson();
			String payload = gson.toJson(dto);

			// make the request
			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			if (logger.isInfoEnabled()) {
				logger.info("Posted equipment event request to URL " + url + " with value " + value);
			}

			// check the response code
			int codeGroup = conn.getResponseCode() / 100;

			if (codeGroup != 2) {
				String msg = "Post failed, error code : " + conn.getResponseCode() + "\nEquipment event response ...";

				BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
				String output;

				while ((output = br.readLine()) != null) {
					msg += "\n" + output;
				}
				throw new Exception(msg);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		} finally {
			conn.disconnect();
		}
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<Void> {
		private final String dataValue;
		private final OffsetDateTime timestamp;

		public ResolutionService(String sourceId, String dataValue, OffsetDateTime timestamp) {
			this.dataValue = dataValue;
			this.timestamp = timestamp;
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

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
		}
	}
}
