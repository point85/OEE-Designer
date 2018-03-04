package org.point85.app.opc.da;

import org.point85.app.AppUtils;
import org.point85.app.Images;
import org.point85.app.ImageManager;
import org.point85.app.LoaderFactory;
import org.point85.app.charts.DataSubscriber;
import org.point85.app.charts.TrendChartController;
import org.point85.app.designer.DataSourceConnectionController;
import org.point85.domain.opc.da.OpcDaDataChangeListener;
import org.point85.domain.opc.da.OpcDaMonitoredGroup;
import org.point85.domain.opc.da.OpcDaMonitoredItem;
import org.point85.domain.opc.da.OpcDaServerStatus;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.opc.da.OpcDaVariantType;
import org.point85.domain.opc.da.TagGroupInfo;
import org.point85.domain.opc.da.TagItemInfo;
import org.point85.domain.plant.Equipment;
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

public class OpcDaTrendController extends OpcDaController implements OpcDaDataChangeListener, DataSubscriber {
	// trend chart
	private TrendChartController trendChartController;

	// trend chart pane
	private SplitPane spTrendChart;

	// the tag being monitored
	private TagItemInfo monitoredTag;

	// the monitored group
	private OpcDaMonitoredGroup opcDaGroup;

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

	@FXML
	public void initialize() {
		// called by javaFX
	}

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
		btConnect.setGraphic(ImageManager.instance().getImageView(Images.CONNECT));
		btConnect.setContentDisplay(ContentDisplay.RIGHT);

		// disconnect
		btDisconnect.setGraphic(ImageManager.instance().getImageView(Images.DISCONNECT));
		btDisconnect.setContentDisplay(ContentDisplay.RIGHT);

		// cancel connect
		btCancelConnect.setGraphic(ImageManager.instance().getImageView(Images.CANCEL));
		btCancelConnect.setContentDisplay(ContentDisplay.RIGHT);
	}

	public void setScriptResolver(ScriptResolver scriptResolver) throws Exception {
		trendChartController.setScriptResolver(scriptResolver);

		OpcDaSource dataSource = (OpcDaSource) scriptResolver.getDataSource();
		setSource(dataSource);

		String pathName = scriptResolver.getSourceId();
		TagItemInfo tagItem = new TagItemInfo(pathName);
		setMonitoredTag(tagItem);

		String trendedItem = dataSource.getId() + " [" + tagItem.toString() + "]";
		lbSourceId.setText(trendedItem);

		updateConnectionStatus(ConnectionState.DISCONNECTED);
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

			OpcDaServerStatus status = getApp().getOpcDaClient().getServerStatus();

			if (status != null) {
				// state
				lbState.setText(status.getServerState());
				lbState.setTextFill(DataSourceConnectionController.CONNECTED_COLOR);
			} else {
				lbState.setText(ConnectionState.DISCONNECTED.toString());
				lbState.setTextFill(DataSourceConnectionController.DISCONNECTED_COLOR);
			}
			break;

		case CONNECTING:
			piConnection.setVisible(true);
			lbState.setText(ConnectionState.CONNECTING.toString());
			lbState.setTextFill(DataSourceConnectionController.CONNECTING_COLOR);
			break;

		case DISCONNECTED:
			piConnection.setVisible(false);
			lbState.setText(ConnectionState.DISCONNECTED.toString());
			lbState.setTextFill(DataSourceConnectionController.DISCONNECTED_COLOR);
			break;

		default:
			break;
		}
	}

	@Override
	public void onOpcDaDataChange(OpcDaMonitoredItem item) {
		try {
			ResolutionService service = new ResolutionService(item);

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

	public void setMonitoredTag(TagItemInfo tagItem) throws Exception {
		this.monitoredTag = tagItem;
	}

	private String createGroupName() throws Exception {
		Equipment equipment = (Equipment) getApp().getPhysicalModelController().getSelectedEntity();
		String name = equipment.getName() + '.' + trendChartController.getScriptResolver().getSourceId();
		return name;
	}

	@Override
	public boolean isSubscribed() {
		return opcDaGroup != null ? true : false;
	}

	@Override
	public void subscribeToDataSource() throws Exception {
		if (opcDaGroup == null) {
			// new group using equipment name
			int updatePeriod = trendChartController.getScriptResolver().getUpdatePeriod();
			TagGroupInfo tagGroup = new TagGroupInfo(createGroupName());
			tagGroup.setUpdatePeriod(updatePeriod);
			tagGroup.addTagItem(monitoredTag);

			// register for data change events
			opcDaGroup = getApp().getOpcDaClient().registerTags(tagGroup, this);
		}

		// start monitoring
		opcDaGroup.startMonitoring();
	}

	@Override
	public void unsubscribeFromDataSource() throws Exception {
		if (opcDaGroup == null) {
			return;
		}
		opcDaGroup.stopMonitoring();
		opcDaGroup = null;
	}

	// service class for callbacks on received data
	private class ResolutionService extends Service<String> {

		private OpcDaMonitoredItem item;

		public ResolutionService(OpcDaMonitoredItem item) {
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
						OpcDaVariant varientValue = item.getValue();

						Object value = null;

						if (varientValue.getDataType().equals(OpcDaVariantType.STRING)) {
							value = varientValue.getValueAsString();
						} else {
							value = varientValue.getValueAsNumber();
						}

						trendChartController.invokeResolver(getApp().getAppContext(), value, item.getLocalTimestamp());
					} catch (Exception e) {
						errorMessage = e.getMessage();
					}
					return errorMessage;
				}
			};
			return resolutionTask;
		}
	}

}
