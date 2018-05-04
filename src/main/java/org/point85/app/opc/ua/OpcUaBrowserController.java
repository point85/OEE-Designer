package org.point85.app.opc.ua;

import java.lang.reflect.Array;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ServerState;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.opc.ua.OpcUaServerStatus;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OpcUaBrowserController extends OpcUaController {

	// selected node
	private OpcUaTreeNode selectedTreeNode;

	// list of servers and ports
	private ObservableList<String> servers = FXCollections.observableArrayList(new ArrayList<>());

	// list of OPC DA tags being monitored
	private List<String> monitoredItemIds = new ArrayList<>();
	
	@FXML
	private TextField tfConnectionName;

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private TextField tfPort;

	@FXML
	private TextField tfPath;

	@FXML
	private ComboBox<String> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private TreeView<OpcUaTreeNode> tvBrowser;

	@FXML
	private Label lbState;

	@FXML
	private Label lbEndpoint;

	@FXML
	private Label lbStartTime;

	@FXML
	private Label lbProduct;

	@FXML
	private Label lbManufacturer;

	@FXML
	private Button btConnect;

	@FXML
	private Button btDisconnect;

	@FXML
	private Button btCancelConnect;

	@FXML
	private ProgressIndicator piConnection;

	@FXML
	private Label lbNodeId;

	@FXML
	private Label lbNodeDescription;

	@FXML
	private Label lbNodeType;

	@FXML
	private TextArea taNodeValue;

	@FXML
	private Label lbNodeTimestamp;

	public void initialize(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// button images
		setImages();

		// init OPC UA connection
		initializeConnection();

		// retrieve the defined data sources
		populateDataSources();

		initializeTreeView();
	}

	private void initializeConnection() {
		// indicator for connection progress
		piConnection.setVisible(false);

		// defined servers
		cbDataSources.setItems(servers);
	}

	private void initializeTreeView() throws Exception {
		tvBrowser.setShowRoot(false);

		// tree node listener
		tvBrowser.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> populateAvailableNodes(newValue));
	}

	public static void arrayToStringRecursive(Object someArray, StringBuilder sb) {
		if (someArray == null) {
			sb.append("null");
			return;
		}

		Class<?> clazz = someArray.getClass();
		if (clazz.isArray()) {
			// iterate over its elements
			int length = Array.getLength(someArray);

			sb.append('[');
			for (int i = 0; i < length; i++) {
				// lets test if array is multidimensional
				if (clazz.getComponentType().isArray()) {
					arrayToStringRecursive(Array.get(someArray, i), sb);
				} else {
					// not an array
					sb.append(Array.get(someArray, i));
					if (i < length - 1)
						sb.append(", ");
				}
			}
			sb.append(']');
		} else {
			sb.append(someArray + " is not an array!");
		}
	}

	private String arrayToString(Object[] values) {
		String valueText = null;
		StringBuilder sb = new StringBuilder();

		sb.append('[');

		int end = values.length;

		for (int i = 0; i < end; i++) {
			valueText = values[i].toString();
			sb.append(valueText);

			if (i < (end - 1)) {
				sb.append(", ");
			}
		}
		sb.append(']');

		return sb.toString();
	}

	private void onSelectNode(OpcUaTreeNode treeNode) throws Exception {
		selectedTreeNode = treeNode;

		NodeId nodeId = treeNode.getNodeId();

		// fill in attributes
		ReferenceDescription ref = treeNode.getReferenceDescription();

		NodeClass nodeClass = ref.getNodeClass();
		NodeId nodeDataType = null;
		Class<?> javaType = null;
		boolean clazzIsArray = false;

		Object value = null;
		OffsetDateTime zdt = null;
		String valueText = null;
		String typeText = null;

		if (nodeClass.equals(NodeClass.Variable)) {
			DataValue dataValue = getApp().getOpcUaClient().readSynch(nodeId);
			Variant variant = dataValue.getValue();
			value = variant.getValue();

			if (value != null) {
				clazzIsArray = value.getClass().isArray();

				// data type
				// nodeDataType = variant.getDataType().get();
				Optional<NodeId> dataType = dataValue.getValue().getDataType();

				if (dataType.isPresent()) {
					nodeDataType = dataType.get();
				}
				javaType = BuiltinDataType.getBackingClass(nodeDataType);

				selectedTreeNode.setNodeDataType(nodeDataType);

				// timestamp
				zdt = DomainUtils.localTimeFromDateTime(dataValue.getServerTime());
			}
		}

		if (value != null) {
			if (!clazzIsArray) {
				typeText = javaType.getSimpleName();

				if (javaType.equals(DateTime.class)) {
					valueText = DomainUtils.utcTimeFromDateTime((DateTime) value).toString();
				} else {
					valueText = value.toString();
				}
			} else {
				// array or matrix
				UInteger[] dims = getApp().getOpcUaClient().getArrayDimensions(nodeId);

				// check for matrix
				if (dims != null) {
					if (dims.length == 1) {
						typeText = "Array of " + javaType.getSimpleName() + " with dimension " + arrayToString(dims);
					} else {
						typeText = "Matrix of " + javaType.getSimpleName() + " with dimensions " + arrayToString(dims);
					}
				}
				StringBuilder sb = new StringBuilder();
				arrayToStringRecursive(value, sb);
				valueText = sb.toString();
			}
		}

		this.lbNodeId.setText(nodeId.toParseableString());
		this.lbNodeDescription.setText(ref.getBrowseName().getName());

		this.lbNodeType.setText(typeText);

		if (valueText != null) {
			this.taNodeValue.setText(valueText);
		} else {
			this.taNodeValue.clear();
		}

		if (zdt != null) {
			this.lbNodeTimestamp.setText(zdt.toString());
		} else {
			this.lbNodeTimestamp.setText(null);
		}
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

		// new
		btNew.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNew.setContentDisplay(ContentDisplay.LEFT);

		// save
		btSave.setGraphic(ImageManager.instance().getImageView(Images.SAVE));
		btSave.setContentDisplay(ContentDisplay.LEFT);

		// delete
		btDelete.setGraphic(ImageManager.instance().getImageView(Images.DELETE));
		btDelete.setContentDisplay(ContentDisplay.LEFT);
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

				// start time
				ZonedDateTime start = DomainUtils.utcTimeFromDateTime(status.getStartTime());
				lbStartTime.setText(DomainUtils.zonedDateTimeToString(start));

				// product & manufacturer
				BuildInfo info = status.getBuildInfo();
				lbProduct.setText(info.getProductName() != null ? info.getProductName() : "");
				lbManufacturer.setText(info.getManufacturerName() != null ? info.getManufacturerName() : "");

				// endpoint
				this.lbEndpoint.setText(getSource().getEndpointUrl());

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
			setSource(null);

			piConnection.setVisible(false);
			lbState.setText(ConnectionState.DISCONNECTED.toString());
			lbState.setTextFill(DISCONNECTED_COLOR);

			lbNodeId.setText(null);
			lbNodeDescription.setText(null);
			lbNodeType.setText(null);
			taNodeValue.clear();
			lbNodeTimestamp.setText(null);

			tvBrowser.setRoot(null);
			break;

		default:
			break;
		}
	}

	private Image getNodeImage(ReferenceDescription ref) throws Exception {
		NodeClass nodeClass = ref.getNodeClass();
		Image image = null;
		if (nodeClass.equals(NodeClass.Object)) {
			image = ImageManager.instance().getImage(Images.FOLDER);
		} else if (nodeClass.equals(NodeClass.Variable)) {
			image = ImageManager.instance().getImage(Images.VALUE);
		}
		return image;
	}

	private void showRootNodes() throws Exception {
		TreeItem<OpcUaTreeNode> rootItem = new TreeItem<>();
		tvBrowser.setRoot(rootItem);
		rootItem.setExpanded(true);

		// browse root folder
		List<ReferenceDescription> refs = getApp().getOpcUaClient().browseSynch(Identifiers.RootFolder);

		for (ReferenceDescription ref : refs) {
			// child nodes
			TreeItem<OpcUaTreeNode> childItem = new TreeItem<>(new OpcUaTreeNode(ref));
			ImageView imageView = new ImageView(getNodeImage(ref));
			childItem.setGraphic(imageView);

			rootItem.getChildren().add(childItem);
		}
	}

	protected void onConnectionSucceeded() throws Exception {
		updateConnectionStatus(ConnectionState.CONNECTED);
		showRootNodes();
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
			onNewDataSource();
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
	@FXML
	protected void onCancel() {
		// close dialog with current tag set to null
		this.selectedTreeNode = null;
		super.onCancel();
	}

	@FXML
	private void onSelectDataSource() {
		String name = getDataSourceId();
		if (name == null || name.length() == 0) {
			return;
		}

		// retrieve data source by name
		OpcUaSource source = PersistenceService.instance().fetchOpcUaSourceByName(name);

		if (source != null) {
			setSource(source);
		} else {
			// not saved yet
			return;
		}

		this.tfConnectionName.setText(source.getName());
		this.tfHost.setText(source.getHost());
		this.tfUserName.setText(source.getUserName());
		this.tfPort.setText(String.valueOf(source.getPort()));
		this.pfPassword.setText(source.getPassword());
		this.tfDescription.setText(source.getDescription());
		this.tfPath.setText(source.getPath());
	}

	@FXML
	private void onDeleteDataSource() {
		try {
			// delete
			OpcUaSource source = getSource();
			if (source != null) {
				PersistenceService.instance().delete(source);
				servers.remove(getSource().getId());

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewDataSource() {
		try {
			this.tfConnectionName.clear();
			this.tfHost.clear();
			this.tfUserName.clear();
			this.pfPassword.clear();
			this.tfDescription.clear();
			this.tfPort.clear();
			this.tfPath.clear();
			this.cbDataSources.getSelectionModel().clearSelection();

			this.setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			OpcUaSource dataSource = getSource();

			dataSource.setName(getConnectionName());
			dataSource.setHost(getHost());
			dataSource.setUserName(getUserName());
			dataSource.setPassword(getPassword());
			dataSource.setPort(getPort());
			dataSource.setDescription(getDescription());
			dataSource.setPath(getPath());			

			// save data source
			PersistenceService.instance().save(dataSource);

			// update list
			populateDataSources();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources() {
		// fetch the sources
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.OPC_UA);

		servers.clear();
		for (CollectorDataSource source : sources) {
			servers.add(((OpcUaSource) source).getName());
		}

		if (servers.size() == 1) {
			this.cbDataSources.getSelectionModel().select(0);
			onSelectDataSource();
		}
	}
	
	String getConnectionName() {
		return this.tfConnectionName.getText();
	}

	String getHost() {
		return this.tfHost.getText();
	}

	String getUserName() {
		return this.tfUserName.getText();
	}

	String getPassword() {
		return this.pfPassword.getText();
	}

	String getPath() {
		return this.tfPath.getText();
	}

	Integer getPort() {
		return Integer.valueOf(tfPort.getText());
	}

	String getDataSourceId() {
		return this.cbDataSources.getSelectionModel().getSelectedItem();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	private void populateAvailableNodes(TreeItem<OpcUaTreeNode> selectedItem) {
		try {
			if (selectedItem == null) {
				return;
			}
			OpcUaTreeNode treeNode = selectedItem.getValue();

			// update node info
			onSelectNode(treeNode);

			if (treeNode.isBrowsed()) {
				// check to see if a variable node to update the value
				NodeClass nodeClass = treeNode.getReferenceDescription().getNodeClass();

				if (!nodeClass.equals(NodeClass.Variable)) {
					return;
				}
			}

			// fill in the child nodes
			ReferenceDescription parentRef = treeNode.getReferenceDescription();

			NodeId nodeId = parentRef.getNodeId().local().orElse(null);

			// keep browsing down the tree
			List<ReferenceDescription> childRefs = getApp().getOpcUaClient().browseSynch(nodeId);
			treeNode.setBrowsed(true);

			for (ReferenceDescription childRef : childRefs) {
				NodeId childId = childRef.getNodeId().local().orElse(null);

				String id = childId.toString();
				if (!monitoredItemIds.contains(id)) {
					TreeItem<OpcUaTreeNode> childItem = new TreeItem<>(new OpcUaTreeNode(childRef));
					childItem.setGraphic(new ImageView(getNodeImage(childRef)));
					selectedItem.getChildren().add(childItem);
				}
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	public OpcUaTreeNode getSelectedNodeId() {
		return selectedTreeNode;
	}

}
