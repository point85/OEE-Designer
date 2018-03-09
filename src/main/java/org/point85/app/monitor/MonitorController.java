package org.point85.app.monitor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.LoaderFactory;
import org.point85.app.dashboard.DashboardController;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.messaging.CollectorNotificationMessage;
import org.point85.domain.messaging.CollectorResolvedEventMessage;
import org.point85.domain.messaging.CollectorServerStatusMessage;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.performance.EquipmentLoss;
import org.point85.domain.performance.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.PlantEntity;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Pagination;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

public class MonitorController {
	private static final int DEFAULT_MSG_LIMIT = 200;
	private static final int DEFAULT_MSGS_PER_PAGE = 20;

	private MonitorApplication app;

	// list of notifications
	private ObservableList<CollectorNotification> notifications = FXCollections.observableArrayList(new ArrayList<>());

	// list of server status'
	private ObservableList<CollectorServerStatus> serverStatus = FXCollections.observableArrayList(new ArrayList<>());

	// list of data collectors
	private ObservableList<DataCollector> serverCollectors = FXCollections.observableArrayList(new ArrayList<>());

	// dashboard
	private DashboardController dashboardController;

	@FXML
	private TreeView<EntityNode> tvEntities;

	@FXML
	private TabPane tpMonitor;

	@FXML
	private Tab tbDashboard;

	@FXML
	private Tab tbNotifications;

	@FXML
	private Tab tbCollectorStatus;

	@FXML
	private AnchorPane apDashboard;

	@FXML
	private TextField tfMaxCount;

	@FXML
	private TextField tfMessagesPerPage;

	@FXML
	private Pagination pgMessages;
	// TODO
	@FXML
	private Button btGenerateData;

	@FXML
	private Button btClearMessages;

	@FXML
	private TextField tfMessageCount;

	@FXML
	private AnchorPane apMessageTable;

	// notification table (programmatic)
	private TableView<CollectorNotification> tvNotifications = new TableView<>();

	// server status table
	@FXML
	private TableView<CollectorServerStatus> tvServerStatus;

	@FXML
	private TableColumn<CollectorServerStatus, String> tcCollectorHostName;

	@FXML
	private TableColumn<CollectorServerStatus, String> tcCollectorHostIP;

	@FXML
	private TableColumn<CollectorServerStatus, String> tcCollectorTimestamp;

	@FXML
	private TableColumn<CollectorServerStatus, String> tcCollectorUsedMemory;

	@FXML
	private TableColumn<CollectorServerStatus, String> tcCollectorFreeMemory;

	@FXML
	private TableColumn<CollectorServerStatus, String> tcCollectorLoad;

	// collector status table
	@FXML
	private TableView<DataCollector> tvCollectorStatus;

	@FXML
	private TableColumn<DataCollector, String> tcCollectorName;

	@FXML
	private TableColumn<DataCollector, String> tcCollectorDescription;

	@FXML
	private TableColumn<DataCollector, CollectorState> tcCollectorState;

	@FXML
	private TableColumn<DataCollector, String> tcBrokerHost;

	@FXML
	private TableColumn<DataCollector, Integer> tcBrokerPort;

	@FXML
	private Button btRefresh;

	public MonitorController() {

	}

	// initialize app
	void initializeApplication(MonitorApplication app) throws Exception {
		this.app = app;

		// images
		setImages();

		// add the tree view listener for entity selection
		tvEntities.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectEntity(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
		tvEntities.setShowRoot(false);

		// add the tab pane listener
		tpMonitor.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				if (newValue.equals(tbDashboard)) {
					onSelectDashboard();
				} else if (newValue.equals(tbNotifications)) {
					onSelectNotifications();
				} else if (newValue.equals(tbCollectorStatus)) {
					onSelectCollectorStatus();
				}
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		tfMaxCount.setText(String.valueOf(DEFAULT_MSG_LIMIT));
		tfMessagesPerPage.setText(String.valueOf(DEFAULT_MSGS_PER_PAGE));

		pgMessages.setPageFactory(this::createMessagesPage);

		// table of notifications
		createNotificationTable();

		// collector server status table
		initializeStatusTable();

		// select dashboard
		try {
			onSelectDashboard();

			populateTopEntityNodes();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeStatusTable() {
		// server status table
		tvServerStatus.setItems(serverStatus);

		// host selection listener
		tvServerStatus.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectServer(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		// collector host
		tcCollectorHostName.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getCollectorHost());
		});

		// collector IP
		tcCollectorHostIP.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getCollectorIpAddress());
		});

		// time
		tcCollectorTimestamp.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getTimestamp());
		});

		// used memory
		this.tcCollectorUsedMemory.setCellValueFactory(cellDataFeatures -> {
			String memory = String.format("%5.1f", cellDataFeatures.getValue().getUsedMemory());
			return new SimpleStringProperty(memory);
		});

		// free memory
		this.tcCollectorFreeMemory.setCellValueFactory(cellDataFeatures -> {
			String memory = String.format("%5.1f", cellDataFeatures.getValue().getFreeMemory());
			return new SimpleStringProperty(memory);
		});

		// load (%)
		this.tcCollectorLoad.setCellValueFactory(cellDataFeatures -> {
			String load = String.format("%4.1f", cellDataFeatures.getValue().getSystemLoadAvg() * 100.0d);
			return new SimpleStringProperty(load);
		});

		// data collector table
		tvCollectorStatus.setItems(serverCollectors);

		// collector name
		tcCollectorName.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getName());
		});

		// collector description
		tcCollectorDescription.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getDescription());
		});

		// collector state
		tcCollectorState.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<CollectorState>(cellDataFeatures.getValue().getCollectorState());
		});

		// RMQ broker host
		tcBrokerHost.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getBrokerHost());
		});

		// RMQ broker host
		tcBrokerPort.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<Integer>(cellDataFeatures.getValue().getBrokerPort());
		});

	}

	// notifications
	private void createNotificationTable() {
		// collector host
		TableColumn<CollectorNotification, String> tcCollector = new TableColumn<>("Collector");
		tvNotifications.getColumns().add(tcCollector);

		TableColumn<CollectorNotification, String> tcCollectorHost = new TableColumn<>("Host");
		tcCollectorHost.setPrefWidth(150);
		tcCollectorHost.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getCollectorHost());
		});
		tcCollector.getColumns().add(tcCollectorHost);

		// collector IP
		TableColumn<CollectorNotification, String> tcCollectorIp = new TableColumn<>("IP Address");
		tcCollectorIp.setPrefWidth(150);
		tcCollectorIp.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getCollectorIpAddress());
		});
		tcCollector.getColumns().add(tcCollectorIp);

		// timestamp
		TableColumn<CollectorNotification, String> tcTimestamp = new TableColumn<>("Timestamp");
		tcTimestamp.setPrefWidth(250);
		tcTimestamp.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getTimestamp());
		});
		tvNotifications.getColumns().add(tcTimestamp);

		// severity
		TableColumn<CollectorNotification, NotificationSeverity> tcSeverity = new TableColumn<>("Severity");
		tcSeverity.setPrefWidth(100);
		tcSeverity.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<NotificationSeverity>(cellDataFeatures.getValue().getSeverity());
		});
		tvNotifications.getColumns().add(tcSeverity);

		// message
		TableColumn<CollectorNotification, String> tcText = new TableColumn<>("Message");
		tcText.setPrefWidth(600);
		tcText.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getText());
		});
		tvNotifications.getColumns().add(tcText);
	}

	private void onSelectDashboard() throws Exception {
		if (dashboardController == null) {
			FXMLLoader loader = LoaderFactory.dashboardLoader();
			SplitPane pane = (SplitPane) loader.getRoot();

			apDashboard.getChildren().add(pane);

			AnchorPane.setTopAnchor(pane, 0.0);
			AnchorPane.setBottomAnchor(pane, 0.0);
			AnchorPane.setLeftAnchor(pane, 0.0);
			AnchorPane.setRightAnchor(pane, 0.0);

			dashboardController = loader.getController();
			// dashboardController.addTabPaneListener();
			// dashboardController.buildDashboardTiles();
		}
	}

	private void onSelectNotifications() throws Exception {

	}

	private void onSelectCollectorStatus() {
		this.onRefresh();
	}

	private void setImages() {

	}

	private void setEntityGraphic(TreeItem<EntityNode> item) throws Exception {
		ImageView nodeView = null;
		PlantEntity entity = item.getValue().getPlantEntity();
		EntityLevel level = entity.getLevel();

		if (level == null) {
			return;
		}

		switch (level) {
		case AREA:
			nodeView = ImageManager.instance().getImageView(Images.AREA);
			break;
		case ENTERPRISE:
			nodeView = ImageManager.instance().getImageView(Images.ENTERPRISE);
			break;
		case EQUIPMENT:
			nodeView = ImageManager.instance().getImageView(Images.EQUIPMENT);
			break;

		case PRODUCTION_LINE:
			nodeView = ImageManager.instance().getImageView(Images.LINE);
			break;

		case SITE:
			nodeView = ImageManager.instance().getImageView(Images.SITE);
			break;

		case WORK_CELL:
			nodeView = ImageManager.instance().getImageView(Images.CELL);
			break;

		default:
			break;
		}
		item.setGraphic(nodeView);
	}

	// display top-level entities
	private void populateTopEntityNodes() throws Exception {
		tvEntities.getSelectionModel().clearSelection();

		// fetch the entities
		List<PlantEntity> entities = PersistenceService.instance().fetchTopPlantEntities();
		Collections.sort(entities);

		// add them to the root entity
		ObservableList<TreeItem<EntityNode>> children = getRootEntityItem().getChildren();
		children.clear();

		for (PlantEntity entity : entities) {
			TreeItem<EntityNode> entityItem = new TreeItem<>(new EntityNode(entity));
			children.add(entityItem);
			setEntityGraphic(entityItem);
		}

		// refresh tree view
		getRootEntityItem().setExpanded(true);
		tvEntities.refresh();
	}

	// the single root for all entities
	private TreeItem<EntityNode> getRootEntityItem() throws Exception {
		if (tvEntities.getRoot() == null) {
			PlantEntity rootEntity = new PlantEntity();
			rootEntity.setName(PlantEntity.ROOT_ENTITY_NAME);
			tvEntities.setRoot(new TreeItem<>(new EntityNode(rootEntity)));
		}
		return tvEntities.getRoot();
	}

	private void onSelectServer(CollectorServerStatus oldItem, CollectorServerStatus newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		List<String> hostNames = new ArrayList<>();

		if (newItem.getCollectorHost() != null) {
			hostNames.add(newItem.getCollectorHost());
		}

		if (newItem.getCollectorIpAddress() != null) {
			hostNames.add(newItem.getCollectorIpAddress());
		}

		List<CollectorState> states = new ArrayList<>();
		states.add(CollectorState.READY);
		states.add(CollectorState.RUNNING);
		states.add(CollectorState.DEV);

		List<DataCollector> collectors = PersistenceService.instance().fetchCollectorsByHostAndState(hostNames, states);

		serverCollectors.clear();
		serverCollectors.addAll(collectors);
		tvCollectorStatus.refresh();
	}

	private void onSelectEntity(TreeItem<EntityNode> oldItem, TreeItem<EntityNode> newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		// new attributes
		PlantEntity selectedEntity = newItem.getValue().getPlantEntity();

		if (selectedEntity == null) {
			// load the entity
			String name = newItem.getValue().getEntityName();
			selectedEntity = PersistenceService.instance().fetchPlantEntityByName(name);
			newItem.getValue().setPlantEntity(selectedEntity);
		}

		// show the children too
		Set<PlantEntity> children = selectedEntity.getChildren();
		List<PlantEntity> sortedChildren = new ArrayList<>(children);
		Collections.sort(sortedChildren);

		boolean hasTreeChildren = newItem.getChildren().size() > 0 ? true : false;

		// check to see if the node's children have been previously shown
		if (!hasTreeChildren) {
			newItem.getChildren().clear();
			for (PlantEntity child : children) {
				TreeItem<EntityNode> entityItem = new TreeItem<>(new EntityNode(child));
				newItem.getChildren().add(entityItem);
				this.setEntityGraphic(entityItem);
			}
		}
		newItem.setExpanded(true);

		if (selectedEntity instanceof Equipment) {
			dashboardController.setEquipmentLoss(getEquipmentLoss((Equipment) selectedEntity));
			dashboardController.displayLosses();
		}
	}

	@FXML
	private void onClearMessages() {
		this.notifications.clear();
		tvNotifications.setItems(notifications);
		this.tvNotifications.refresh();
		this.pgMessages.setPageCount(0);
		pgMessages.setCurrentPageIndex(0);
	}

	void handleCollectorStatus(CollectorServerStatusMessage message) {
		for (int i = 0; i < serverStatus.size(); i++) {
			CollectorServerStatus status = serverStatus.get(i);

			if (status.getCollectorHost().equalsIgnoreCase(message.getSenderHostName())) {
				serverStatus.remove(i);
				break;
			}
		}

		CollectorServerStatus status = new CollectorServerStatus(message);
		serverStatus.add(status);
		tvServerStatus.refresh();

		tvServerStatus.getSelectionModel().clearSelection();

		serverCollectors.clear();
		tvCollectorStatus.refresh();
	}

	void handleNotification(CollectorNotificationMessage message) {
		// add to complete list
		CollectorNotification notification = new CollectorNotification(message);
		notifications.add(0, notification);

		// check for over limit
		int maxCount = Integer.valueOf(tfMaxCount.getText());

		int delta = notifications.size() - maxCount;

		if (delta > 0) {
			// delete oldest ones
			notifications.remove(maxCount, notifications.size());
		}
		tvNotifications.setItems(notifications);

		// calculate page count
		int numOfPages = 1;
		int messagesPerPage = Integer.valueOf(tfMessagesPerPage.getText());
		int messageCount = notifications.size();

		if (messageCount % messagesPerPage == 0) {
			numOfPages = messageCount / messagesPerPage;
		} else if (messageCount > messagesPerPage) {
			numOfPages = messageCount / messagesPerPage + 1;
		}

		pgMessages.setPageCount(numOfPages);

	}

	private Node createMessagesPage(int pageIndex) {
		int messagesPerPage = Integer.valueOf(tfMessagesPerPage.getText());

		int fromIndex = pageIndex * messagesPerPage;
		int toIndex = Math.min(fromIndex + messagesPerPage, notifications.size());

		if (fromIndex <= toIndex) {
			tvNotifications.setItems(FXCollections.observableArrayList(notifications.subList(fromIndex, toIndex)));
		}

		return new BorderPane(tvNotifications);
	}

	void updateDashboard(CollectorResolvedEventMessage message) {
		dashboardController.update((CollectorResolvedEventMessage) message);
	}

	@FXML
	private void onRefresh() {
		List<DataCollector> collectors = PersistenceService.instance().fetchAllDataCollectors();

		serverStatus.clear();
		for (DataCollector collector : collectors) {
			CollectorServerStatus status = new CollectorServerStatus(collector);

			if (!serverStatus.contains(status)) {
				serverStatus.add(status);
			}
		}
		tvServerStatus.refresh();

		tvServerStatus.getSelectionModel().clearSelection();

		serverCollectors.clear();
		tvCollectorStatus.refresh();
	}

	// TODO remove
	private EquipmentLoss getEquipmentLoss(Equipment equipment) {
		EquipmentLoss equipmentLoss = new EquipmentLoss();
		equipmentLoss.setTotalTime(Duration.ofSeconds(240 * 60));
		equipmentLoss.setLoss(TimeLoss.UNSCHEDULED, Duration.ofSeconds(40 * 60));
		equipmentLoss.setLoss(TimeLoss.MINOR_STOPPAGES, Duration.ofSeconds(40 * 60));
		equipmentLoss.setLoss(TimeLoss.PLANNED_DOWNTIME, Duration.ofSeconds(10 * 60));
		equipmentLoss.setLoss(TimeLoss.SETUP, Duration.ofSeconds(16 * 60));
		equipmentLoss.setLoss(TimeLoss.UNPLANNED_DOWNTIME, Duration.ofSeconds(24 * 60));
		equipmentLoss.setLoss(TimeLoss.REDUCED_SPEED, Duration.ofSeconds(8 * 60));
		equipmentLoss.setLoss(TimeLoss.REJECT_REWORK, Duration.ofSeconds(2 * 60));

		return equipmentLoss;
	}

	// TODO
	@FXML
	void onGenerateData() {
		app.generateData();
	}

	// the wrapped PlantEntity
	private class EntityNode {
		private String name;

		private PlantEntity entity;

		private EntityNode(String name) {
			this.name = name;
		}

		private EntityNode(PlantEntity entity) {
			setPlantEntity(entity);
		}

		private PlantEntity getPlantEntity() {
			return entity;
		}

		private void setPlantEntity(PlantEntity reason) {
			this.entity = reason;
		}

		private String getEntityName() {
			return name;
		}

		@Override
		public String toString() {
			if (entity != null) {
				return entity.getName() + " (" + entity.getDescription() + ")";
			} else {
				return name;
			}
		}
	}
}
