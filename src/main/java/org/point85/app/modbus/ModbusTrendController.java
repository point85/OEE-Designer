package org.point85.app.modbus;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.ConnectionState;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.modbus.ModbusEvent;
import org.point85.domain.modbus.ModbusEventListener;
import org.point85.domain.modbus.ModbusMaster;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.script.EventResolver;

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

public class ModbusTrendController extends ModbusController implements ModbusEventListener, DataSubscriber {
	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	@FXML
	private Button btConnect;

	@FXML
	private Button btDisconnect;

	@FXML
	private Button btCancelConnect;

	// end point
	@FXML
	private Label lbSourceId;

	// connection status
	@FXML
	private ProgressIndicator piConnection;

	@FXML
	private Label lbState;

	public SplitPane initializeTrend() throws Exception {
		if (trendChartController == null) {
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

	// images for buttons
	@Override
	protected void setImages() throws Exception {
		super.setImages();

		// connect
		btConnect.setGraphic(ImageManager.instance().getImageView(Images.CONNECT));
		btConnect.setContentDisplay(ContentDisplay.LEFT);

		// disconnect
		btDisconnect.setGraphic(ImageManager.instance().getImageView(Images.DISCONNECT));
		btDisconnect.setContentDisplay(ContentDisplay.LEFT);

		// cancel connect
		btCancelConnect.setGraphic(ImageManager.instance().getImageView(Images.CANCEL));
		btCancelConnect.setContentDisplay(ContentDisplay.LEFT);
	}

	public void setEventResolver(EventResolver eventResolver) throws Exception {
		eventResolver.setWatchMode(true);
		trendChartController.setEventResolver(eventResolver);

		lbSourceId.setText(DesignerLocalizer.instance().getLangString("event.source",
				eventResolver.getEquipment().getName(), eventResolver.getSourceId()));
	}

	@Override
	@FXML
	protected void onOK() {
		super.onOK();
		try {
			unsubscribeFromDataSource();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	@FXML
	protected void onCancel() {
		super.onCancel();

		try {
			unsubscribeFromDataSource();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void updateConnectionStatus(ConnectionState state) throws Exception {
		connectionState = state;

		switch (state) {
		case CONNECTED:
			piConnection.setVisible(false);
			lbState.setText(ConnectionState.CONNECTED.toString());
			lbState.setTextFill(ConnectionState.CONNECTED_COLOR);
			trendChartController.enableTrending(true);
			break;

		case CONNECTING:
			piConnection.setVisible(true);
			lbState.setText(ConnectionState.CONNECTING.toString());
			lbState.setTextFill(ConnectionState.CONNECTING_COLOR);
			trendChartController.enableTrending(false);
			break;

		case DISCONNECTED:
			piConnection.setVisible(false);
			lbState.setText(ConnectionState.DISCONNECTED.toString());
			lbState.setTextFill(ConnectionState.DISCONNECTED_COLOR);
			trendChartController.enableTrending(false);
			break;

		default:
			break;
		}
	}

	@FXML
	private void onConnect() {
		try {
			if (connectionState.equals(ConnectionState.CONNECTED)) {
				// disconnect first
				onDisconnect();
			}

			// connect
			updateConnectionStatus(ConnectionState.CONNECTING);
			startConnectionService();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDisconnect() {
		try {
			trendChartController.onStopTrending();

			// disconnect
			disconnectFromDataSource();
			terminateConnectionService();
			updateConnectionStatus(ConnectionState.DISCONNECTED);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	protected void onCancelConnect() {
		try {
			cancelConnectionService();
			updateConnectionStatus(ConnectionState.DISCONNECTED);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	public boolean isSubscribed() {
		return getApp().getModbusMaster().isPolling();
	}

	public void createModbusMaster() throws Exception {
		EventResolver eventResolver = trendChartController.getEventResolver();
		ModbusSource source = (ModbusSource) eventResolver.getDataSource();

		Integer period = eventResolver.getUpdatePeriod();
		if (period == null) {
			period = CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC;
		}
		List<Integer> pollingPeriods = new ArrayList<>(1);
		pollingPeriods.add(period);

		List<String> sourceIds = new ArrayList<>(1);
		sourceIds.add(eventResolver.getSourceId());

		ModbusMaster modbusMaster = new ModbusMaster(this, source, sourceIds, pollingPeriods);

		// add to context
		getApp().getAppContext().getModbusMasters().clear();
		getApp().getAppContext().addModbusMaster(modbusMaster);

		// start the trend
		trendChartController.enableTrending(true);
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		ModbusMaster modbusMaster = getApp().getModbusMaster();
		if (modbusMaster == null || !modbusMaster.isConnected()) {
			return;
		}

		// start the trend
		trendChartController.enableTrending(true);

		modbusMaster.startPolling();

		// stop the trend
		trendChartController.toggleTrendButton();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		ModbusMaster modbusMaster = getApp().getModbusMaster();
		if (modbusMaster == null || !modbusMaster.isConnected()) {
			return;
		}

		modbusMaster.stopPolling();

		// stop the trend
		trendChartController.toggleTrendButton();
	}

	@Override
	public void resolveModbusEvents(ModbusEvent event) {
		ResolutionService service = new ResolutionService(event);

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

	@Override
	protected void onConnectionSucceeded() throws Exception {
		updateConnectionStatus(ConnectionState.CONNECTED);

		// start polling for events
		getApp().getModbusMaster().startPolling();
	}

	// service class for callbacks on received events
	private class ResolutionService extends Service<Void> {
		private final ModbusEvent modbusEvent;

		public ResolutionService(ModbusEvent modbusEvent) {
			this.modbusEvent = modbusEvent;
		}

		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {

				@Override
				protected Void call() {
					try {
						// execute script, with only one value
						trendChartController.invokeResolver(getApp().getAppContext(), modbusEvent.getValues(),
								modbusEvent.getEventTime(), modbusEvent.getReason());
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
