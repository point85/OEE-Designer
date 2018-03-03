package org.point85.app.opc.ua;

import java.time.OffsetDateTime;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.point85.app.AppUtils;
import org.point85.app.ImageEnum;
import org.point85.app.ImageManager;
import org.point85.app.LoaderFactory;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.domain.DomainUtils;
import org.point85.domain.opc.ua.OpcUaAsynchListener;
import org.point85.domain.opc.ua.OpcUaServerStatus;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.script.ScriptResolver;

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

	private NodeId monitoredNodeId;

	private ExtensionObject filter = null;

	private double publishingInterval = 3000.0d;

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

		// connect
		btConnect.setGraphic(ImageManager.instance().getImageView(ImageEnum.CONNECT));
		btConnect.setContentDisplay(ContentDisplay.RIGHT);

		// disconnect
		btDisconnect.setGraphic(ImageManager.instance().getImageView(ImageEnum.DISCONNECT));
		btDisconnect.setContentDisplay(ContentDisplay.RIGHT);

		// cancel connect
		btCancelConnect.setGraphic(ImageManager.instance().getImageView(ImageEnum.CANCEL));
		btCancelConnect.setContentDisplay(ContentDisplay.RIGHT);
	}

	public void unsubscribeFromNode() throws Exception {
		getApp().getOpcUaClient().unsubscribe(monitoredNodeId);
		monitoredNodeId = null;
	}

	@Override
	public boolean isSubscribed() {
		return monitoredNodeId != null ? true : false;
	}
	
	

	public void subscribeToDataSource() throws Exception {
		getApp().getOpcUaClient().registerAsynchListener(this);
		getApp().getOpcUaClient().subscribe(getMonitoredNode(), publishingInterval, filter);
	}

	public void unsubscribeFromDataSource() throws Exception {
		getApp().getOpcUaClient().unsubscribe(monitoredNodeId);
		getApp().getOpcUaClient().unregisterAsynchListener(this);
		monitoredNodeId = null;
	}
	
	private NodeId getMonitoredNode()  {
		NodeId nodeId = monitoredNodeId;
		
		if (nodeId == null) {
			String nodeName = trendChartController.getScriptResolver().getSourceId();
			nodeId = NodeId.parse(nodeName);
			setMonitoredNode(nodeId);
		}
		return nodeId;
	}

	public void setMonitoredNode(NodeId nodeId) {
		this.monitoredNodeId = nodeId;
	}

	public void setScriptResolver(ScriptResolver scriptResolver) throws Exception {
		trendChartController.setScriptResolver(scriptResolver);

		OpcUaSource dataSource = (OpcUaSource) scriptResolver.getDataSource();
		setSource(dataSource);

		String nodeName = scriptResolver.getSourceId();
		NodeId nodeId = NodeId.parse(nodeName);
		setMonitoredNode(nodeId);

		String trendedItem = dataSource.getId() + " [" + nodeId.toParseableString() + "]";
		lbSourceId.setText(trendedItem);

		updateConnectionStatus(ConnectionState.DISCONNECTED);
	}

	@Override
	public void onOpcUaRead(List<DataValue> dataValues) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOpcUaWrite(List<StatusCode> statusCodes) {
		// TODO Auto-generated method stub

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
				lbState.setText(serverState.toString());
				lbState.setTextFill(CONNECTED_COLOR);
			} else {
				lbState.setText(ConnectionState.DISCONNECTED.toString());
				lbState.setTextFill(DISCONNECTED_COLOR);
			}
			break;

		case CONNECTING:
			piConnection.setVisible(true);
			lbState.setText(ConnectionState.CONNECTING.toString());
			lbState.setTextFill(CONNECTING_COLOR);
			break;

		case DISCONNECTED:
			piConnection.setVisible(false);
			lbState.setText(ConnectionState.DISCONNECTED.toString());
			lbState.setTextFill(DISCONNECTED_COLOR);
			break;

		default:
			break;
		}
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<String> {

		private DataValue dataValue;

		private UaMonitoredItem item;

		public ResolutionService(DataValue dataValue, UaMonitoredItem item) {
			this.dataValue = dataValue;
			this.item = item;
		}

		@Override
		protected Task<String> createTask() {
			Task<String> resolutionTask = new Task<String>() {

				@Override
				protected String call() throws Exception {
					String errorMessage = NO_ERROR;

					try {
						// resolve the input value into a reason
						Object javaValue = dataValue.getValue().getValue();
						String itemId = item.getReadValueId().getNodeId().toParseableString();
						DateTime dt = dataValue.getServerTime();
						OffsetDateTime odt = DomainUtils.localTimeFromDateTime(dt);

						trendChartController.invokeResolver(getApp().getAppContext(), javaValue, odt);
					} catch (Exception e) {
						errorMessage = e.getMessage();

						if (errorMessage == null) {
							errorMessage = "Point85 Exception";
						}
					}
					return errorMessage;
				}
			};
			return resolutionTask;
		}
	}

}
