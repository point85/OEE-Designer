package org.point85.app.opc.da;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.da.OPCITEMSTATE;
import org.point85.app.AppUtils;
import org.point85.app.ImageEnum;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.domain.opc.da.OpcDaBrowserLeaf;
import org.point85.domain.opc.da.OpcDaClient;
import org.point85.domain.opc.da.OpcDaMonitoredGroup;
import org.point85.domain.opc.da.OpcDaServerStatus;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaTagTreeBranch;
import org.point85.domain.opc.da.OpcDaTreeBrowser;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.persistence.PersistencyService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;

public class OpcDaBrowserController extends OpcDaController {

	// selected tag
	private OpcDaBrowserLeaf selectedTag;

	// list of prog ids
	private ObservableList<String> progIds = FXCollections.observableArrayList(new ArrayList<>());

	// list of all Opc items at a leaf node
	private ObservableList<OpcDaBrowserLeaf> availableTags = FXCollections.observableArrayList();

	// list of OPC DA tags being monitored
	private List<String> monitoredItemIds = new ArrayList<>();

	// tree browser
	private OpcDaTreeBrowser treeBrowser;

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfProgId;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private ComboBox<String> cbProgIds;

	@FXML
	private TextField tfDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private TreeView<OpcDaTagTreeBranch> tvBrowser;

	@FXML
	private Label lbState;

	@FXML
	private Label lbStartTime;

	@FXML
	private Label lbVendor;

	@FXML
	private Label lbVersion;

	@FXML
	private Button btConnect;

	@FXML
	private Button btDisconnect;

	@FXML
	private Button btCancelConnect;

	@FXML
	private ProgressIndicator piConnection;

	@FXML
	private ListView<OpcDaBrowserLeaf> lvAvailableTags;

	@FXML
	private Label lbTagQuality;

	@FXML
	private Label lbTagTimestamp;

	@FXML
	private Label lbTagType;

	@FXML
	private TextArea taTagValue;

	public void initialize(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// button images
		setImages();

		initializeConnection();

		// retrieve the defined data sources
		populateDataSources();

		initializeTreeView();
	}

	private void initializeConnection() {
		// listener for connection progress
		piConnection.setVisible(false);

		cbProgIds.setItems(progIds);
	}

	private void initializeTreeView() throws Exception {

		tvBrowser.setShowRoot(false);

		// tree node listener
		tvBrowser.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> populateAvailableTags(newValue));

		lvAvailableTags.setItems(availableTags);

		// add the listener for tag selection
		lvAvailableTags.getSelectionModel().selectedItemProperty()
				.addListener((observableValue, oldValue, newValue) -> {
					try {
						onSelectTag(newValue);
					} catch (Exception e) {
						AppUtils.showErrorDialog(e);
					}
				});

		lvAvailableTags.setCellFactory(param -> new ListCell<OpcDaBrowserLeaf>() {
			private ImageView imageView = new ImageView(Images.tagImage);

			@Override
			public void updateItem(OpcDaBrowserLeaf leaf, boolean empty) {
				super.updateItem(leaf, empty);
				if (empty) {
					setText(null);
					setGraphic(null);
				} else {
					setText(leaf.getItemId());
					setGraphic(imageView);
				}
			}
		});
	}

	private void clearTagData() {
		// clear attributes
		this.lbTagQuality.setText(null);
		this.lbTagType.setText(null);
		this.taTagValue.setText(null);
		this.lbTagTimestamp.setText(null);
	}

	private void onSelectTag(OpcDaBrowserLeaf tag) throws Exception {
		if (tag == null) {
			return;
		}
		selectedTag = tag;

		// create a group only for the read
		String groupName = Long.toHexString(System.currentTimeMillis());
		OpcDaMonitoredGroup group = getApp().getOpcDaClient().addGroup(groupName, true, 1, 0.0f);

		OpcDaBrowserLeaf[] itemIds = new OpcDaBrowserLeaf[1];
		itemIds[0] = tag;
		group.addItems(itemIds, true);
		KeyedResultSet<Integer, OPCITEMSTATE> itemState = group.synchRead();

		getApp().getOpcDaClient().removeGroup(group);

		// just one result is expected
		for (KeyedResult<Integer, OPCITEMSTATE> itemStateEntry : itemState) {
			int errorCode = itemStateEntry.getErrorCode();
			if (errorCode != 0) {
				throw new Exception("Unable to read value, error code: " + String.format("%08X", errorCode));
			}

			// quality
			short quality = itemStateEntry.getValue().getQuality();
			this.lbTagQuality.setText(String.valueOf(quality));

			// data type
			OpcDaVariant variant = new OpcDaVariant(itemStateEntry.getValue().getValue());
			tag.setDataType(variant);
			int typeFlag = itemStateEntry.getValue().getValue().getType();
			String displayType = OpcDaVariant.getDisplayType(typeFlag);
			this.lbTagType.setText(displayType);

			// value
			String value = variant.getValueAsString();
			this.taTagValue.setText(value);

			// timestamp
			ZonedDateTime zdt = OpcDaClient.fromFiletime(itemStateEntry.getValue().getTimestamp());
			this.lbTagTimestamp.setText(zdt.toString());
		}
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

		// new
		btNew.setGraphic(ImageManager.instance().getImageView(ImageEnum.NEW));
		btNew.setContentDisplay(ContentDisplay.LEFT);

		// save
		btSave.setGraphic(ImageManager.instance().getImageView(ImageEnum.SAVE));
		btSave.setContentDisplay(ContentDisplay.LEFT);

		// delete
		btDelete.setGraphic(ImageManager.instance().getImageView(ImageEnum.DELETE));
		btDelete.setContentDisplay(ContentDisplay.LEFT);
	}

	private void updateConnectionStatus(ConnectionState state) throws Exception {
		switch (state) {
		case CONNECTED:
			piConnection.setVisible(false);

			OpcDaServerStatus status = getApp().getOpcDaClient().getServerStatus();

			if (status != null) {
				String serverState = status.getServerState();
				lbState.setText(serverState);
				lbState.setTextFill(CONNECTED_COLOR);
				lbStartTime.setText(status.getStartTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
				lbVendor.setText(status.getVendorInfo());
				lbVersion.setText(status.getVersion());
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

			lbTagQuality.setText(null);
			lbTagTimestamp.setText(null);
			lbTagType.setText(null);
			taTagValue.setText(null);
			lbStartTime.setText(null);
			lbVendor.setText(null);
			lbVersion.setText(null);

			tvBrowser.setRoot(null);
			availableTags.clear();
			break;

		default:
			break;
		}
	}

	public OpcDaBrowserLeaf getSelectedTag() {
		return selectedTag;
	}

	private void showRootTags() throws Exception {
		if (treeBrowser == null) {
			// browse root tags
			treeBrowser = getApp().getOpcDaClient().getTreeBrowser();
			OpcDaTagTreeBranch rootBranch = treeBrowser.browseBranches();
			OpcDaTagTreeItem treeRoot = new OpcDaTagTreeItem(rootBranch, treeBrowser, new ImageView(Images.folderImage));

			tvBrowser.setRoot(treeRoot);
			treeRoot.setExpanded(true);
		}
	}

	protected void onConnectionSucceeded() throws Exception {
		updateConnectionStatus(ConnectionState.CONNECTED);
		showRootTags();
	}

	@FXML
	private void onConnect() {
		try {
			// update status display
			updateConnectionStatus(ConnectionState.CONNECTING);

			startConnectionService();
		} catch (Exception e) {
			piConnection.setVisible(false);
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDisconnect() {
		try {
			// disconnect
			terminateConnectionService();

			updateConnectionStatus(ConnectionState.DISCONNECTED);

			// reset tree browser
			getApp().getOpcDaClient().releaseTreeBrowser();

			// reset attributes
			this.selectedTag = null;
			this.progIds.clear();
			this.availableTags.clear();
			this.monitoredItemIds.clear();
			this.treeBrowser = null;
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
		this.selectedTag = null;
		super.onCancel();
	}

	@FXML
	private void onSelectProgId() {

		if (getSelectedProgId() == null || getSelectedProgId().length() == 0) {
			return;
		}

		// retrieve data source by name (ProgId)
		OpcDaSource source = null;
		try {
			source = (OpcDaSource) PersistencyService.instance().fetchByName(OpcDaSource.DA_SRC_BY_NAME,
					getSelectedProgId());
			setSource(source);
		} catch (Exception e) {
			// not saved yet
			return;
		}

		this.tfHost.setText(source.getHost());
		this.tfProgId.setText(source.getProgId());
		this.tfUserName.setText(source.getUserName());
		this.pfPassword.setText(source.getPassword());
		this.tfDescription.setText(source.getDescription());
	}

	@FXML
	private void onDeleteDataSource() {
		try {
			// delete
			OpcDaSource source = getSource();
			if (source != null) {
				PersistencyService.instance().delete(source);
				progIds.remove(getSource().getProgId());

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewDataSource() {
		try {
			this.cbProgIds.getSelectionModel().select(null);
			this.tfHost.clear();
			this.tfProgId.clear();
			this.tfUserName.clear();
			this.pfPassword.clear();
			this.tfDescription.clear();

			this.setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			if (getSource() == null) {
				setSource(new OpcDaSource());
			}
			OpcDaSource dataSource = getSource();
			dataSource.setHost(getHost());
			dataSource.setUserName(getUserName());
			dataSource.setPassword(getPassword());
			dataSource.setProgId(getProgId());
			dataSource.setDescription(getDescription());

			PersistencyService.instance().save(dataSource);

			populateDataSources();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources() {
		// fetch the prog ids
		List<String> ids = PersistencyService.instance().fetchProgIds();

		progIds.clear();
		for (String id : ids) {
			progIds.add(id);
		}

		if (progIds.size() == 1) {
			this.cbProgIds.getSelectionModel().select(0);
			onSelectProgId();
		}
	}

	String getHost() {
		return this.tfHost.getText();
	}

	String getSelectedProgId() {
		return this.cbProgIds.getSelectionModel().getSelectedItem();
	}

	String getProgId() {
		return this.tfProgId.getText().trim();
	}

	String getUserName() {
		return this.tfUserName.getText();
	}

	String getPassword() {
		return this.pfPassword.getText();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	private void populateAvailableTags(TreeItem<OpcDaTagTreeBranch> selectedItem) {
		try {
			clearTagData();
			availableTags.clear();

			if (selectedItem != null && selectedItem.isLeaf()) {

				// fill in the possible tags
				OpcDaTagTreeBranch selectedNode = selectedItem.getValue();

				OpcDaTreeBrowser treeBrowser = getApp().getOpcDaClient().getTreeBrowser();

				Collection<OpcDaBrowserLeaf> tags = treeBrowser.getLeaves(selectedNode);

				availableTags.clear();
				for (OpcDaBrowserLeaf tag : tags) {
					if (!monitoredItemIds.contains(tag.getItemId())) {
						availableTags.add(tag);
					}
				}
			}
			lvAvailableTags.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
