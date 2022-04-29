package org.point85.app.socket;

import java.net.InetAddress;
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
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.script.EventResolver;
import org.point85.domain.socket.WebSocketMessageListener;
import org.point85.domain.socket.WebSocketOeeClient;
import org.point85.domain.socket.WebSocketOeeServer;
import org.point85.domain.socket.WebSocketSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class WebSocketTrendController extends DesignerDialogController
		implements WebSocketMessageListener, DataSubscriber {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(WebSocketTrendController.class);

	// web socket client
	private WebSocketOeeClient wsClient;

	// web socket server
	private WebSocketOeeServer wsServer;

	// trend chart
	private TrendChartController trendChartController;

	// subscribed flag
	private boolean isSubscribed = false;

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
		WebSocketSource dataSource = (WebSocketSource) trendChartController.getEventResolver().getDataSource();
		Integer port = dataSource.getPort();

		// check to see if already started
		Collection<WebSocketOeeServer> servers = getApp().getAppContext().getWebSocketServers();

		Iterator<WebSocketOeeServer> iter = servers.iterator();

		while (iter.hasNext()) {
			WebSocketOeeServer server = iter.next();
			if (server.getPort() == port) {
				wsServer = server;
				break;
			}
		}

		try {
			if (wsServer == null) {
				piConnection.setVisible(true);

				// create and start the server
				wsServer = new WebSocketOeeServer(dataSource);

				// register listener
				subscribeToDataSource();

				wsServer.startup();

				lbState.setText(DesignerLocalizer.instance().getLangString("ws.server.started"));
				lbState.setTextFill(STARTED_COLOR);

				// add to context
				getApp().getAppContext().addWebSocketServer(wsServer);

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
		if (wsServer != null) {
			// stop the trend
			trendChartController.onStopTrending();

			wsServer.shutdown();

			// remove from context
			getApp().getAppContext().removeWebSocketServer(wsServer);

			lbState.setText(DesignerLocalizer.instance().getLangString("ws.server.stopped"));
			lbState.setTextFill(STOPPED_COLOR);
			wsServer = null;
		}
	}

	@Override
	public boolean isSubscribed() {
		return isSubscribed;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		wsServer.registerListener(this);

		isSubscribed = true;

		// enable stopping the trend
		trendChartController.enableTrending(true);

		// allow stopping the trend
		trendChartController.toggleTrendButton();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		wsServer.unregisterListener();
		isSubscribed = false;

		trendChartController.enableTrending(true);

		// allow stopping the trend
		trendChartController.toggleTrendButton();
	}

	@FXML
	private void onLoopbackTest() {
		try {
			// get the web socket data source
			EventResolver eventResolver = trendChartController.getEventResolver();
			WebSocketSource source = (WebSocketSource) eventResolver.getDataSource();

			wsClient = new WebSocketOeeClient(source);
			wsClient.openConnection();

			String input = tfLoopbackValue.getText();
			String[] values = AppUtils.parseCsvInput(input);

			EquipmentEventMessage message = new EquipmentEventMessage();
			String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(),
					DomainUtils.OFFSET_DATE_TIME_8601);
			message.setTimestamp(timestamp);

			message.setReason(values[1]);
			message.setSenderHostName(InetAddress.getLocalHost().getHostName());
			message.setSourceId(source.getId());
			message.setValue(values[0]);

			wsClient.sendEventMessage(message);

			if (logger.isInfoEnabled()) {
				logger.info("Sent equipment event request to URL " + source.getHost() + ":" + source.getPort());
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		} finally {
			try {
				wsClient.closeConnection();
			} catch (Exception e) {
				// log it
				logger.error(e.getMessage());
			}
		}
	}

	@Override
	public void onWebSocketMessage(ApplicationMessage appMessage) {
		if (!(appMessage instanceof EquipmentEventMessage)) {
			// ignore it
			return;
		}

		EquipmentEventMessage message = (EquipmentEventMessage) appMessage;

		ResolutionService service = new ResolutionService(message.getValue(), message.getTimestamp(),
				message.getReason());

		service.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				Throwable t = event.getSource().getException();

				if (t != null) {
					// connection failed
					AppUtils.showErrorDialog(t.getMessage());
				}
			}
		});

		// run on application thread
		service.start();
	}

	// service class for callbacks on received data
	protected class ResolutionService extends Service<Void> {
		private final String dataValue;
		private final String timestamp;
		private final String reason;

		public ResolutionService(String dataValue, String timestamp, String reason) {
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
						OffsetDateTime odt = DomainUtils.offsetDateTimeFromString(timestamp,
								DomainUtils.OFFSET_DATE_TIME_8601);
						trendChartController.invokeResolver(getApp().getAppContext(), dataValue, odt, reason);
					} catch (Exception e) {
						Platform.runLater(() -> AppUtils.showErrorDialog(e));
					}
					return null;
				}
			};
		}
	}
}
