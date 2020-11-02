package org.point85.app.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
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
	protected void setImages() {
		super.setImages();

		// loopback test
		btLoopback.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btLoopback.setContentDisplay(ContentDisplay.LEFT);
	}

	public void setScriptResolver(EventResolver eventResolver) {
		eventResolver.setWatchMode(true);
		trendChartController.setEventResolver(eventResolver);

		lbSourceId.setText(DesignerLocalizer.instance().getLangString("event.source",
				eventResolver.getEquipment().getName(), eventResolver.getSourceId()));
	}

	@Override
	@FXML
	protected void onOK() {
		try {
			onStopServer();
			super.onOK();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	@FXML
	protected void onCancel() {
		try {
			onStopServer();
			super.onCancel();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	public void onStartServer() {
		HttpSource dataSource = (HttpSource) trendChartController.getEventResolver().getDataSource();
		Integer port = dataSource.getPort();
		Integer httpsPort = dataSource.getHttpsPort();

		// check to see if already started
		Collection<OeeHttpServer> servers = getApp().getAppContext().getHttpServers();

		Iterator<OeeHttpServer> iter = servers.iterator();

		while (iter.hasNext()) {
			OeeHttpServer server = iter.next();
			if (server.getListeningPort() == port) {
				httpServer = server;
				break;
			}
		}

		try {
			if (httpServer == null) {
				piConnection.setVisible(true);

				httpServer = new OeeHttpServer(port);

				if (httpsPort != null) {
					httpServer.setHttpsPort(httpsPort);
				}

				OeeHttpServer.setDataChangeListener(this);
				httpServer.startup();
				lbState.setText(httpServer.getState().toString());
				lbState.setTextFill(STARTED_COLOR);

				// add to context
				getApp().getAppContext().addHttpServer(httpServer);

				// start the trend
				trendChartController.onStartTrending();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		} finally {
			piConnection.setVisible(false);
		}
	}

	private void onStopServer() throws Exception {
		if (httpServer != null) {
			// stop the trend
			trendChartController.onStopTrending();

			httpServer.shutdown();

			// remove from context
			getApp().getAppContext().removeHttpServer(httpServer);

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
	public void onHttpEquipmentEvent(EquipmentEventRequestDto dto) {
		String timestamp = dto.getTimestamp();
		OffsetDateTime odt = null;

		if (timestamp.length() > DomainUtils.LOCAL_DATE_TIME_8601.length()) {
			odt = DomainUtils.offsetDateTimeFromString(dto.getTimestamp(), DomainUtils.OFFSET_DATE_TIME_8601);
		} else {
			LocalDateTime ldt = DomainUtils.localDateTimeFromString(dto.getTimestamp(),
					DomainUtils.LOCAL_DATE_TIME_8601);
			odt = DomainUtils.fromLocalDateTime(ldt);
		}

		ResolutionService service = new ResolutionService(dto.getValue(), odt, dto.getReason());

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

	private String createPayload(EventResolver eventResolver) throws Exception {
		// the value to send (must match the configured resolver)
		String input = tfLoopbackValue.getText();
		String[] values = AppUtils.parseCsvInput(input);

		// create the data transfer event object
		EquipmentEventRequestDto dto = new EquipmentEventRequestDto(eventResolver.getSourceId(), values[0]);
		String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(), DomainUtils.OFFSET_DATE_TIME_8601);
		dto.setTimestamp(timestamp);
		dto.setReason(values[1]);

		// serialize the body
		Gson gson = new Gson();
		String payload = gson.toJson(dto);

		return payload;
	}

	@FXML
	private void onLoopbackTest() {
		HttpURLConnection conn = null;

		try {
			// get the HTTP data source
			EventResolver eventResolver = trendChartController.getEventResolver();
			HttpSource dataSource = (HttpSource) eventResolver.getDataSource();

			// build the URL for an equipment event, only HTTP supported
			URL url = new URL(
					"http://" + dataSource.getHost() + ":" + dataSource.getPort() + '/' + OeeHttpServer.EVENT_EP);

			// create a connection for a JSON POST request
			conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			String payload = createPayload(eventResolver);

			// make the request
			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			if (logger.isInfoEnabled()) {
				logger.info("Posted equipment event request to URL " + url + " with payload " + payload);
			}

			// check the response code
			int codeGroup = conn.getResponseCode() / 100;

			if (codeGroup != 2) {
				String msg = DesignerLocalizer.instance().getErrorString("post.failed", conn.getResponseCode()) + "\n";

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
		private final String reason;

		private ResolutionService(String dataValue, OffsetDateTime timestamp, String reason) {
			this.dataValue = dataValue;
			this.timestamp = timestamp;
			this.reason = reason;
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() {
					try {
						trendChartController.invokeResolver(getApp().getAppContext(), dataValue, timestamp, reason);
					} catch (Exception e) {
						Platform.runLater(() -> AppUtils.showErrorDialog(e));
					}
					return null;
				}
			};
		}
	}
}
