package org.point85.app.opc.ua;

import java.time.OffsetDateTime;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.ConnectionState;
import org.point85.domain.DomainUtils;
import org.point85.domain.opc.ua.OpcUaAsynchListener;
import org.point85.domain.opc.ua.OpcUaServerStatus;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.script.EventResolver;

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

public class OpcUaTrendController extends OpcUaController implements OpcUaAsynchListener, DataSubscriber {
	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	// monitored node
	private NodeId monitoredNodeId;

	@FXML
	private Button btConnect;

	@FXML
	private Button btDisconnect;

	@FXML
	private Button btCancelConnect;

	// subscribed item
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

	public void setUpdatePeriodMsec(Integer millis) {
		if (millis != null) {
			trendChartController.setUpdatePeriodMsec(millis);
		}
	}

	// images for buttons
	@Override
	protected void setImages() {
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

	public void unsubscribeFromNode() throws Exception {
		getApp().getOpcUaClient().unsubscribe(monitoredNodeId);
		monitoredNodeId = null;
	}

	@Override
	public boolean isSubscribed() {
		return monitoredNodeId != null;
	}

	public void subscribeToDataSource() throws Exception {
		getApp().getOpcUaClient().registerAsynchListener(this);

		double publishingInterval = trendChartController.getEventResolver().getUpdatePeriod();

		// filter is not being used
		getApp().getOpcUaClient().subscribe(getMonitoredNode(), publishingInterval, null);
	}

	public void unsubscribeFromDataSource() throws Exception {
		getApp().getOpcUaClient().unsubscribe(monitoredNodeId);
		getApp().getOpcUaClient().unregisterAsynchListener(this);
		monitoredNodeId = null;
	}

	private NodeId getMonitoredNode() {
		NodeId nodeId = monitoredNodeId;

		if (nodeId == null) {
			String nodeName = trendChartController.getEventResolver().getSourceId();
			nodeId = NodeId.parse(nodeName);
			setMonitoredNode(nodeId);
		}
		return nodeId;
	}

	public void setMonitoredNode(NodeId nodeId) {
		this.monitoredNodeId = nodeId;
	}

	public void setScriptResolver(EventResolver eventResolver) throws Exception {
		eventResolver.setWatchMode(true);
		trendChartController.setEventResolver(eventResolver);

		OpcUaSource dataSource = (OpcUaSource) eventResolver.getDataSource();
		setSource(dataSource);

		String nodeName = eventResolver.getSourceId();
		NodeId nodeId = NodeId.parse(nodeName);
		setMonitoredNode(nodeId);

		String trendedItem = dataSource.getId() + " [" + nodeId.toParseableString() + "]";
		lbSourceId.setText(trendedItem);

		updateConnectionStatus(ConnectionState.DISCONNECTED);
	}

	@Override
	public void onOpcUaRead(List<DataValue> dataValues) {
		// nothing to do
	}

	@Override
	public void onOpcUaWrite(List<StatusCode> statusCodes) {
		// nothing to do
	}

	// on background thread
	@Override
	public void onOpcUaSubscription(DataValue dataValue, UaMonitoredItem item) {
		try {
			ResolutionService service = new ResolutionService(dataValue, item);

			service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
					String value = (String) event.getSource().getValue();

					if (!value.equals(NO_ERROR)) {
						// connection failed
						AppUtils.showErrorDialog(value);
					}
				}
			});

			service.start();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	protected void onConnectionSucceeded() throws Exception {
		updateConnectionStatus(ConnectionState.CONNECTED);

		// subscribe for data change events
		subscribeToDataSource();
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
			unsubscribeFromDataSource();

			// disconnect
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

	private void updateConnectionStatus(ConnectionState state) throws Exception {
		connectionState = state;

		switch (state) {
		case CONNECTED:
			piConnection.setVisible(false);

			OpcUaServerStatus status = getApp().getOpcUaClient().getServerStatus();

			if (status != null) {
				// state
				ServerState serverState = status.getState();
				lbState.setText(serverState != null ? serverState.toString() : "");
				lbState.setTextFill(ConnectionState.CONNECTED_COLOR);
				trendChartController.enableTrending(true);
			} else {
				lbState.setText(ConnectionState.DISCONNECTED.toString());
				lbState.setTextFill(ConnectionState.DISCONNECTED_COLOR);
				trendChartController.enableTrending(false);
			}
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

	// service class for callbacks on received data
	private class ResolutionService extends Service<String> {

		private final DataValue dataValue;

		public ResolutionService(DataValue dataValue, UaMonitoredItem item) {
			this.dataValue = dataValue;
		}

		@Override
		protected Task<String> createTask() {
			return new Task<String>() {

				@Override
				protected String call() throws Exception {
					String errorMessage = NO_ERROR;

					try {
						// resolve the input value into a reason
						Object javaValue = UaOpcClient.getJavaObject(dataValue.getValue());
						DateTime dt = dataValue.getServerTime();
						OffsetDateTime odt = DomainUtils.localTimeFromDateTime(dt);

						trendChartController.invokeResolver(getApp().getAppContext(), javaValue, odt, null);
					} catch (Exception e) {
						errorMessage = e.getMessage();

						if (errorMessage == null) {
							errorMessage = getClass().getSimpleName();
						}
					}
					return errorMessage;
				}
			};
		}
	}
}
