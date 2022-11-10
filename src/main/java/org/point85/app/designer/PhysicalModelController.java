package org.point85.app.designer;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.EntityNode;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.email.EmailSource;
import org.point85.domain.exim.Exporter;
import org.point85.domain.exim.Importer;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.HttpSource;
import org.point85.domain.jms.JmsSource;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Area;
import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Reason;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;
import org.point85.domain.proficy.ProficySource;
import org.point85.domain.rmq.RmqSource;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.EventResolver;
import org.point85.domain.socket.WebSocketSource;
import org.point85.domain.uom.UnitOfMeasure;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

/**
 * Controller for editing and viewing the physical model and its associated
 * entities.
 * 
 * @author Kent Randall
 *
 */
public class PhysicalModelController extends DesignerController {
	// list of edited plant entities
	private final Set<TreeItem<EntityNode>> editedEntityItems = new HashSet<>();

	// entity being edited or viewed
	private TreeItem<EntityNode> selectedEntityItem;

	// equipment material controller
	private EquipmentMaterialController equipmentMaterialController;

	// controller for availability
	private EquipmentResolverController resolverController;

	// equipment work schedules
	private EntityWorkScheduleController entityWorkScheduleController;

	// menu bar
	@FXML
	private Menu mnSource;

	@FXML
	private Menu mnEditor;

	@FXML
	private Menu mnTool;

	@FXML
	private Menu mnHelp;

	@FXML
	private MenuItem miMaterialEditor;

	@FXML
	private MenuItem miReasonEditor;

	@FXML
	private MenuItem miScheduleEditor;

	@FXML
	private Button btClearSchedule;

	@FXML
	private MenuItem miUomEditor;

	@FXML
	private MenuItem miOpcDaBrowser;

	@FXML
	private MenuItem miOpcUaBrowser;

	@FXML
	private MenuItem miHttpServerEditor;

	@FXML
	private MenuItem miRmqBrokerEditor;

	@FXML
	private MenuItem miJMSBrokerEditor;

	@FXML
	private MenuItem miKafkaServerEditor;

	@FXML
	private MenuItem miEmailServerEditor;

	@FXML
	private MenuItem miProficyBrowserEditor;

	@FXML
	private MenuItem miMQTTBrokerEditor;

	@FXML
	private MenuItem miModbusEditor;

	@FXML
	private MenuItem miDatabaseServerEditor;

	@FXML
	private MenuItem miFileShareEditor;

	@FXML
	private MenuItem miCronEditor;

	@FXML
	private MenuItem miWebSocketEditor;

	@FXML
	private MenuItem miCollectorEditor;

	@FXML
	private MenuItem miUomConverter;

	@FXML
	private MenuItem miRestore;

	@FXML
	private MenuItem miBackup;

	@FXML
	private MenuItem miScriptEditor;

	@FXML
	private MenuItem miAboutDialog;

	// entity section
	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btRefresh;

	@FXML
	private Button btDelete;

	@FXML
	private Button btBackup;

	// context menu
	@FXML
	private MenuItem miClearSelection;

	@FXML
	private MenuItem miRefreshAll;

	@FXML
	private MenuItem miSaveAll;

	@FXML
	private TreeView<EntityNode> tvEntities;

	@FXML
	private ComboBox<EntityLevel> cbEntityTypes;

	@FXML
	private TextField tfEntityName;

	@FXML
	private TextArea taEntityDescription;

	@FXML
	private TabPane tpEntity;

	// equipment processed material tab
	@FXML
	private AnchorPane apEquipMaterial;

	@FXML
	private Tab tbEquipMaterials;

	// availability tab
	@FXML
	private AnchorPane apAvailability;

	@FXML
	private Tab tbAvailability;

	// work schedule tab
	@FXML
	private AnchorPane apWorkSchedules;

	@FXML
	private Tab tbWorkSchedules;

	// class reasons tab
	@FXML
	private AnchorPane apClassReasons;

	// OEE dashboard
	@FXML
	private Button btDashboard;

	@FXML
	private TextField tfRetention;

	// current work schedule
	@FXML
	private Label lbCurrentSchedule;

	// extract the PlantEntity name from the tree item
	public PlantEntity getSelectedEntity() {
		PlantEntity entity = null;

		if (selectedEntityItem != null) {
			entity = selectedEntityItem.getValue().getPlantEntity();
		}
		return entity;
	}

	// the single root for all entities
	private TreeItem<EntityNode> getRootEntityItem() {
		if (tvEntities.getRoot() == null) {
			PlantEntity rootEntity = new PlantEntity();
			rootEntity.setName(PlantEntity.ROOT_ENTITY_NAME);
			tvEntities.setRoot(new TreeItem<>(new EntityNode(rootEntity)));
		}
		return tvEntities.getRoot();
	}

	// initialize app
	void initialize(DesignerApplication app) {
		// main app
		setApp(app);

		// images
		setImages();

		// toolbar
		initializeMenubar();

		// add the tree view listener for entity selection
		tvEntities.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectEntity(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
		tvEntities.setShowRoot(false);

		// entity types
		cbEntityTypes.getItems().add(EntityLevel.ENTERPRISE);
		cbEntityTypes.getItems().add(EntityLevel.SITE);
		cbEntityTypes.getItems().add(EntityLevel.AREA);
		cbEntityTypes.getItems().add(EntityLevel.PRODUCTION_LINE);
		cbEntityTypes.getItems().add(EntityLevel.WORK_CELL);
		cbEntityTypes.getItems().add(EntityLevel.EQUIPMENT);

		// add the tab pane listener
		tpEntity.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				if (newValue.equals(tbEquipMaterials)) {
					onSelectEquipmentMaterial();
				} else if (newValue.equals(tbAvailability)) {
					onSelectEquipmentResolver();
				} else if (newValue.equals(tbWorkSchedules)) {
					onSelectWorkSchedules();
				}
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		// disable tabs
		tbEquipMaterials.setDisable(true);
		tbAvailability.setDisable(true);
		tbWorkSchedules.setDisable(true);
	}

	// display top-level entities
	void populateTopEntityNodes() throws Exception {
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
		tvEntities.getSelectionModel().clearSelection();
		tvEntities.refresh();

		if (entities.size() == 1 && (entities.get(0) instanceof Equipment)) {
			tvEntities.getSelectionModel().select(0);
		}
	}

	private void onSelectEntity(TreeItem<EntityNode> oldItem, TreeItem<EntityNode> newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		// new attributes
		selectedEntityItem = newItem;
		PlantEntity selectedEntity = newItem.getValue().getPlantEntity();
		displayAttributes(selectedEntity);

		// show the children too
		Set<PlantEntity> children = selectedEntity.getChildren();
		List<PlantEntity> sortedChildren = new ArrayList<>(children);
		Collections.sort(sortedChildren);

		boolean hasTreeChildren = !newItem.getChildren().isEmpty();

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

		tbWorkSchedules.setDisable(false);

		if (selectedEntity instanceof Equipment) {
			tbAvailability.setDisable(false);
			tbEquipMaterials.setDisable(false);
			btDashboard.setDisable(false);

			tpEntity.getSelectionModel().select(tbEquipMaterials);
			onSelectEquipmentMaterial();
		} else {
			tbAvailability.setDisable(true);
			tbEquipMaterials.setDisable(true);
			btDashboard.setDisable(true);

			tpEntity.getSelectionModel().select(tbWorkSchedules);
			onSelectWorkSchedules();
		}
	}

	private void initializeMenubar() {
		// menu bar
		mnSource.setGraphic(ImageManager.instance().getImageView(Images.SOURCE_MENU));

		mnEditor.setGraphic(ImageManager.instance().getImageView(Images.EDITOR_MENU));

		mnTool.setGraphic(ImageManager.instance().getImageView(Images.TOOL_MENU));

		mnHelp.setGraphic(ImageManager.instance().getImageView(Images.HELP_MENU));

		// menu items
		miMaterialEditor.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));

		miReasonEditor.setGraphic(ImageManager.instance().getImageView(Images.REASON));

		miScheduleEditor.setGraphic(ImageManager.instance().getImageView(Images.SCHEDULE));

		miUomEditor.setGraphic(ImageManager.instance().getImageView(Images.UOM));

		miOpcDaBrowser.setGraphic(ImageManager.instance().getImageView(Images.OPC_DA));

		miOpcUaBrowser.setGraphic(ImageManager.instance().getImageView(Images.OPC_UA));

		miHttpServerEditor.setGraphic(ImageManager.instance().getImageView(Images.HTTP));

		miRmqBrokerEditor.setGraphic(ImageManager.instance().getImageView(Images.RMQ));

		miJMSBrokerEditor.setGraphic(ImageManager.instance().getImageView(Images.JMS));

		miKafkaServerEditor.setGraphic(ImageManager.instance().getImageView(Images.KAFKA));

		miEmailServerEditor.setGraphic(ImageManager.instance().getImageView(Images.EMAIL));

		miProficyBrowserEditor.setGraphic(ImageManager.instance().getImageView(Images.PROFICY));

		miMQTTBrokerEditor.setGraphic(ImageManager.instance().getImageView(Images.MQTT));

		miModbusEditor.setGraphic(ImageManager.instance().getImageView(Images.MODBUS));

		miDatabaseServerEditor.setGraphic(ImageManager.instance().getImageView(Images.DB));

		miFileShareEditor.setGraphic(ImageManager.instance().getImageView(Images.FILE));

		miCronEditor.setGraphic(ImageManager.instance().getImageView(Images.CRON));

		miWebSocketEditor.setGraphic(ImageManager.instance().getImageView(Images.CONNECT));

		miCollectorEditor.setGraphic(ImageManager.instance().getImageView(Images.COLLECTOR));

		miUomConverter.setGraphic(ImageManager.instance().getImageView(Images.CONVERT));

		miScriptEditor.setGraphic(ImageManager.instance().getImageView(Images.SCRIPT));

		miAboutDialog.setGraphic(ImageManager.instance().getImageView(Images.ABOUT));
	}

	// images for editor buttons
	protected void setImages() {
		// new entity
		btNew.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNew.setContentDisplay(ContentDisplay.RIGHT);

		// save entity
		btSave.setGraphic(ImageManager.instance().getImageView(Images.SAVE));
		btSave.setContentDisplay(ContentDisplay.RIGHT);

		// save all
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
		btRefresh.setContentDisplay(ContentDisplay.RIGHT);

		// delete entity
		btDelete.setGraphic(ImageManager.instance().getImageView(Images.DELETE));
		btDelete.setContentDisplay(ContentDisplay.RIGHT);

		// dashboard
		btDashboard.setGraphic(ImageManager.instance().getImageView(Images.DASHBOARD));
		btDashboard.setContentDisplay(ContentDisplay.RIGHT);

		// context menu
		miSaveAll.setGraphic(ImageManager.instance().getImageView(Images.SAVE_ALL));
		miRefreshAll.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));
		miClearSelection.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));

		// backup & restore
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.RIGHT);

		miBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		miRestore.setGraphic(ImageManager.instance().getImageView(Images.RESTORE));
	}

	@FXML
	private void onShowCollectorEditor() {
		try {
			this.getApp().showCollectorEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowHttpServerEditor() {
		try {
			this.getApp().showHttpServerEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowRmqBrokerEditor() {
		try {
			this.getApp().showMQBrokerEditor(DataSourceType.RMQ);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowJMSBrokerEditor() {
		try {
			this.getApp().showMQBrokerEditor(DataSourceType.JMS);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowMQTTBrokerEditor() {
		try {
			this.getApp().showMqttServerEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowWebSocketEditor() {
		try {
			this.getApp().showWebSocketEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowModbusEditor() {
		try {
			this.getApp().showModbusEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowDatabaseServerEditor() {
		try {
			this.getApp().showDatabaseServerEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowFileShareEditor() {
		try {
			this.getApp().showFileShareEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowCronEditor() {
		try {
			this.getApp().showCronEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowMaterialEditor() {
		try {
			this.getApp().showMaterialEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowReasonEditor() {
		try {
			this.getApp().showReasonEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowScheduleEditor() {
		try {
			this.getApp().showScheduleEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowScriptEditor() {
		try {
			EventResolver eventResolver = new EventResolver();

			getApp().showScriptEditor(eventResolver);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowUomEditor() {
		try {
			this.getApp().showUomEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowOpcDaBrowser() {
		try {
			this.getApp().showOpcDaDataSourceBrowser();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowOpcUaBrowser() {
		try {
			this.getApp().showOpcUaDataSourceBrowser();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowUomConverter() {
		try {
			this.getApp().showUomConverter();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowAboutDialog() {
		try {
			this.getApp().showAboutDialog();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewEntity() {
		try {
			// main editing
			tfEntityName.clear();
			taEntityDescription.setText(null);
			cbEntityTypes.getSelectionModel().clearSelection();
			cbEntityTypes.getSelectionModel().select(null);
			cbEntityTypes.requestFocus();
			tfRetention.clear();
			lbCurrentSchedule.setText(null);

			// no entity item selection
			selectedEntityItem = null;

			// equipment materials
			if (equipmentMaterialController != null) {
				equipmentMaterialController.clear();
			}

			// work schedules
			if (entityWorkScheduleController != null) {
				entityWorkScheduleController.clear();
			}

			// data collection
			getResolverController().clear();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private boolean createEntity() {
		try {
			// chosen level of child
			EntityLevel childLevel = this.cbEntityTypes.getSelectionModel().getSelectedItem();

			// parent entity
			TreeItem<EntityNode> parentItem = this.tvEntities.getSelectionModel().getSelectedItem();

			if (parentItem == null) {
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(
						DesignerLocalizer.instance().getLangString("add.entity", childLevel.toString()));

				if (type.equals(ButtonType.CANCEL)) {
					return false;
				}

				// add to all entities
				parentItem = tvEntities.getRoot();

				if (parentItem == null) {
					throw new Exception(DesignerLocalizer.instance().getErrorString("create.entity"));
				}
			} else {
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(DesignerLocalizer.instance()
						.getLangString("add.entity.parent", parentItem.getValue().getPlantEntity().getName()));

				if (type.equals(ButtonType.CANCEL)) {
					return false;
				}
			}

			PlantEntity parentEntity = parentItem.getValue().getPlantEntity();
			EntityLevel parentLevel = parentEntity.getLevel();

			// create and add child entity
			PlantEntity newEntity = null;

			if (parentLevel != null) {
				switch (parentLevel) {
				case ENTERPRISE:
					childLevel = EntityLevel.SITE;
					break;
				case SITE:
					childLevel = EntityLevel.AREA;
					break;
				case AREA:
					childLevel = EntityLevel.PRODUCTION_LINE;
					break;
				case PRODUCTION_LINE:
					childLevel = EntityLevel.WORK_CELL;
					break;
				case WORK_CELL:
					childLevel = EntityLevel.EQUIPMENT;
					break;
				default:
					return false;
				}
			}

			if (childLevel == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.level"));
			}

			switch (childLevel) {
			case ENTERPRISE:
				newEntity = new Enterprise();
				break;
			case SITE:
				newEntity = new Site();
				break;
			case AREA:
				newEntity = new Area();
				break;
			case PRODUCTION_LINE:
				newEntity = new ProductionLine();
				break;
			case WORK_CELL:
				newEntity = new WorkCell();
				break;
			case EQUIPMENT:
				newEntity = new Equipment();
				break;
			default:
				return false;
			}

			// set the new entity's attributes
			selectedEntityItem = new TreeItem<>(new EntityNode(newEntity));
			setAttributes(selectedEntityItem);

			// add new child entity if not a top level
			if (!parentEntity.getName().equals(PlantEntity.ROOT_ENTITY_NAME)) {
				parentEntity.addChild(newEntity);
				parentItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
			}

			// add to tree view
			parentItem.getChildren().add(selectedEntityItem);
			selectedEntityItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
			addEditedPlantEntity(selectedEntityItem);

			parentItem.setExpanded(true);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
		return true;
	}

	private void setEntityGraphic(TreeItem<EntityNode> item) {
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

	@FXML
	private void onClearSelection() {
		tvEntities.getSelectionModel().clearSelection();
		selectedEntityItem = null;
	}

	@FXML
	private void onRefreshEntity() {
		try {
			PlantEntity selectedEntity = getSelectedEntity();
			if (selectedEntity == null) {
				onNewEntity();
				return;
			}

			PlantEntity refreshed = PersistenceService.instance().fetchPlantEntityByName(selectedEntity.getName());
			selectedEntityItem.getValue().setPlantEntity(refreshed);
			selectedEntity = refreshed;
			displayAttributes(selectedEntity);

			removeEditedPlantEntity(selectedEntityItem);

			tvEntities.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRefreshAllEntities() {
		try {
			populateTopEntityNodes();
			onNewEntity();
		} catch (Exception e) {
			AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("fetch.entities", e.getMessage()));
		}
	}

	// Delete button clicked
	@FXML
	private void onDeleteEntity() {
		PlantEntity selectedEntity = getSelectedEntity();

		if (selectedEntity == null) {
			AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.entity"));
			return;
		}

		// confirm
		ButtonType type = AppUtils.showConfirmationDialog(
				DesignerLocalizer.instance().getLangString("confirm.delete", selectedEntity.getName()));

		if (type.equals(ButtonType.CANCEL)) {
			return;
		}

		try {
			// delete from db if previously saved
			PlantEntity parentEntity = selectedEntity.getParent();
			if (parentEntity != null) {
				// remove from parent with orphan removal
				parentEntity.removeChild(selectedEntity);
				PersistenceService.instance().save(parentEntity);
			} else {
				// cascade delete
				PersistenceService.instance().delete(selectedEntity);
			}

			// remove this entity from the tree
			TreeItem<EntityNode> entityItem = tvEntities.getSelectionModel().getSelectedItem();
			TreeItem<EntityNode> parentNode = entityItem.getParent();
			parentNode.getChildren().remove(entityItem);
			tvEntities.getSelectionModel().clearSelection();

			tvEntities.refresh();
			parentNode.setExpanded(true);

			onNewEntity();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void fillPersistenceContext(PlantEntity entity) {
		// bring children into the persistence context
		for (PlantEntity child : entity.getChildren()) {
			fillPersistenceContext(child);
		}
	}

	// Save button clicked
	@FXML
	private void onSaveEntity() {
		try {
			if (selectedEntityItem == null) {
				// create
				if (!createEntity()) {
					return;
				}
			} else {
				// update
				setAttributes(selectedEntityItem);
			}

			// save modified entity
			PlantEntity entity = getSelectedEntity();

			// bring children into persistence context (required by JPA)
			fillPersistenceContext(entity);

			PlantEntity saved = (PlantEntity) PersistenceService.instance().save(entity);

			selectedEntityItem.getValue().setPlantEntity(saved);
			setEntityGraphic(selectedEntityItem);

			if (selectedEntityItem.getParent() != null) {
				setEntityGraphic(selectedEntityItem.getParent());
			} else {
				// is a top-level entity
				getRootEntityItem().getChildren().add(selectedEntityItem);
			}

			editedEntityItems.remove(selectedEntityItem);

			// re-display attributes
			onRefreshEntity();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveAllEntities() {
		try {
			// current entity could have been edited
			setAttributes(selectedEntityItem);

			// save all modified entities
			for (TreeItem<EntityNode> editedEntityItem : editedEntityItems) {
				EntityNode node = editedEntityItem.getValue();
				PlantEntity entity = node.getPlantEntity();
				PlantEntity saved = (PlantEntity) PersistenceService.instance().save(entity);
				node.setPlantEntity(saved);
				setEntityGraphic(editedEntityItem);
			}

			editedEntityItems.clear();

			tvEntities.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// show the entity attributes
	private void displayAttributes(PlantEntity entity) {
		if (entity == null) {
			return;
		}

		// name
		this.tfEntityName.setText(entity.getName());

		// description
		this.taEntityDescription.setText(entity.getDescription());

		// level
		cbEntityTypes.getSelectionModel().select(entity.getLevel());

		// retention period
		if (entity.getRetentionDuration() != null) {
			long days = entity.getRetentionDuration().toDays();
			this.tfRetention.setText(String.valueOf(days));
		} else {
			this.tfRetention.setText(null);
		}

		// current work schedule
		WorkSchedule schedule = entity.findWorkSchedule();
		if (schedule != null) {
			this.lbCurrentSchedule.setText(schedule.getName());
		} else {
			this.lbCurrentSchedule.setText(null);
		}

		if (entity instanceof Equipment) {
			if (equipmentMaterialController != null) {
				equipmentMaterialController.showMaterial((Equipment) entity);
			}

			if (resolverController != null) {
				resolverController.showResolvers((Equipment) entity);
			}
		}
	}

	private void setAttributes(TreeItem<EntityNode> entityItem) throws Exception {
		if (entityItem == null) {
			return;
		}
		PlantEntity entity = entityItem.getValue().getPlantEntity();

		// name
		String name = tfEntityName.getText().trim();

		if (name.length() == 0) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.name"));
		}
		entity.setName(name);

		// description
		String description = taEntityDescription.getText();
		entity.setDescription(description);

		// retention period
		String period = tfRetention.getText();

		if (period != null && period.length() > 0) {
			Long days = AppUtils.stringToLong(period.trim());

			if (days < 0) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("invalid.retention"));
			}
			entity.setRetentionDuration(Duration.ofDays(days));
		}

		addEditedPlantEntity(entityItem);
	}

	private void addEditedPlantEntity(TreeItem<EntityNode> item) {
		if (item != null && !editedEntityItems.contains(item)) {
			item.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
			editedEntityItems.add(item);
		}
	}

	private void removeEditedPlantEntity(TreeItem<EntityNode> item) {
		if (item != null && editedEntityItems.contains(item)) {
			setEntityGraphic(item);
			editedEntityItems.remove(item);
		}
	}

	void markSelectedPlantEntity() {
		this.addEditedPlantEntity(selectedEntityItem);
	}

	private void onSelectEquipmentMaterial() throws Exception {
		if (equipmentMaterialController == null) {
			// Load the fxml file and create the anchor pane
			FXMLLoader loader = FXMLLoaderFactory.equipmentMaterialLoader();
			AnchorPane pane = (AnchorPane) loader.getRoot();
			tbEquipMaterials.setContent(pane);

			equipmentMaterialController = loader.getController();

			equipmentMaterialController.initialize(getApp());
		}

		// show materials
		if (getSelectedEntity() instanceof Equipment) {
			equipmentMaterialController.showMaterial((Equipment) getSelectedEntity());
		} else {
			equipmentMaterialController.clearEditor();
		}
	}

	private void onSelectWorkSchedules() throws Exception {
		if (entityWorkScheduleController == null) {
			// Load the fxml file and create the anchor pane
			FXMLLoader loader = FXMLLoaderFactory.entityWorkScheduleLoader();
			AnchorPane pane = (AnchorPane) loader.getRoot();
			tbWorkSchedules.setContent(pane);

			entityWorkScheduleController = loader.getController();

			entityWorkScheduleController.initialize(getApp());
		}

		// show schedules
		entityWorkScheduleController.showSchedules(getSelectedEntity());
	}

	private EquipmentResolverController getResolverController() throws Exception {
		if (resolverController == null) {
			// Load the fxml file and create the anchor pane
			FXMLLoader loader = FXMLLoaderFactory.equipmentResolverLoader();
			AnchorPane pane = (AnchorPane) loader.getRoot();
			tbAvailability.setContent(pane);

			resolverController = loader.getController();
			resolverController.initialize(getApp());
		}
		return resolverController;
	}

	private void onSelectEquipmentResolver() throws Exception {

		// show entity resolvers
		if (getSelectedEntity() instanceof Equipment) {
			getResolverController().showResolvers((Equipment) getSelectedEntity());
		} else {
			getResolverController().clearEditor();
		}
	}

	@FXML
	private void onShowDashboard() {
		try {
			getApp().showDashboard();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowKafkaServerEditor() {
		try {
			this.getApp().showKafkaServerEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowEmailServerEditor() {
		try {
			this.getApp().showEmailServerEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowProficyBrowserEditor() {
		try {
			this.getApp().showProficyEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRestore() {
		try {
			// show file chooser
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle(DesignerLocalizer.instance().getLangString("filechooser.backup"));
			fileChooser.setInitialDirectory(getApp().getLastDirectory());
			File file = fileChooser.showOpenDialog(null);

			if (file == null) {
				return;
			}
			getApp().setLastDirectory(file.getParentFile());

			// restore content
			Importer.instance().restore(file);

			AppUtils.showInfoDialog(
					DesignerLocalizer.instance().getLangString("restore.successful", file.getCanonicalPath()));
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onBackupAll() {
		try {
			// show file chooser
			File file = AppUtils.showFileSaveDialog(getApp().getLastDirectory());

			if (file == null) {
				return;
			}
			getApp().setLastDirectory(file.getParentFile());

			// backup all design time objects
			Exporter.instance().prepare(Material.class);
			Exporter.instance().prepare(Reason.class);
			Exporter.instance().prepare(UnitOfMeasure.class);
			Exporter.instance().prepare(WorkSchedule.class);
			Exporter.instance().prepare(PlantEntity.class);
			Exporter.instance().prepare(DataCollector.class);

			// data sources
			Exporter.instance().prepare(CronEventSource.class);
			Exporter.instance().prepare(HttpSource.class);
			Exporter.instance().prepare(DatabaseEventSource.class);
			Exporter.instance().prepare(EmailSource.class);
			Exporter.instance().prepare(FileEventSource.class);
			Exporter.instance().prepare(JmsSource.class);
			Exporter.instance().prepare(KafkaSource.class);
			Exporter.instance().prepare(ModbusSource.class);
			Exporter.instance().prepare(MqttSource.class);
			Exporter.instance().prepare(OpcDaSource.class);
			Exporter.instance().prepare(OpcUaSource.class);
			Exporter.instance().prepare(ProficySource.class);
			Exporter.instance().prepare(RmqSource.class);
			Exporter.instance().prepare(WebSocketSource.class);

			Exporter.instance().backup(file);
			
			AppUtils.showInfoDialog(
					DesignerLocalizer.instance().getLangString("backup.successful", file.getCanonicalPath()));

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onBackup() {
		try {
			// show file chooser
			File file = AppUtils.showFileSaveDialog(getApp().getLastDirectory());

			if (file == null) {
				return;
			}
			getApp().setLastDirectory(file.getParentFile());

			// backup
			Exporter.instance().backup(PlantEntity.class, file);

			AppUtils.showInfoDialog(
					DesignerLocalizer.instance().getLangString("backup.successful", file.getCanonicalPath()));
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
