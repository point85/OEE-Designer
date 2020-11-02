package org.point85.app.designer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.opc.ua.OpcUaTreeNode;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.HttpSource;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.modbus.ModbusEndpoint;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.opc.da.OpcDaBrowserLeaf;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeEventType;
import org.point85.domain.script.ResolverFunction;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class EquipmentResolverController extends DesignerController {
	// equipment event resolvers
	private final ObservableList<EventResolver> eventResolvers = FXCollections.observableArrayList(new ArrayList<>());

	// event resolver being edited
	private EventResolver selectedEventResolver;

	@FXML
	private ComboBox<OeeEventType> cbResolverTypes;

	@FXML
	private ComboBox<DataSourceType> cbDataSources;

	@FXML
	private ComboBox<DataCollector> cbCollectors;

	@FXML
	private Button btBrowseSource;

	@FXML
	private TextField tfSourceId;

	@FXML
	private TextField tfServerId;

	@FXML
	private TextField tfUpdatePeriod;

	@FXML
	private Button btEditCollector;

	@FXML
	private Button btEditScript;

	@FXML
	private Label lbScript;

	@FXML
	private Label lbDataType;

	@FXML
	private Button btNewResolver;

	@FXML
	private Button btAddResolver;

	@FXML
	private Button btRemoveResolver;

	@FXML
	private TableView<EventResolver> tvResolvers;

	@FXML
	private TableColumn<EventResolver, String> tcCollector;

	@FXML
	private TableColumn<EventResolver, String> tcResolverType;

	@FXML
	private TableColumn<EventResolver, String> tcDataSourceType;

	@FXML
	private TableColumn<EventResolver, String> tcServer;

	@FXML
	private TableColumn<EventResolver, String> tcSourceId;

	@FXML
	private TableColumn<EventResolver, String> tcScript;

	@FXML
	private TableColumn<EventResolver, Integer> tcUpdatePeriod;

	@FXML
	private TableColumn<EventResolver, String> tcDataType;

	@FXML
	private Button btRun;

	public EventResolver getSelectedResolver() {
		if (selectedEventResolver == null) {
			// new resolver
			selectedEventResolver = new EventResolver();
		}
		return selectedEventResolver;
	}

	private void setDataCollectors() throws Exception {
		List<DataCollector> collectors = PersistenceService.instance().fetchAllDataCollectors();
		cbCollectors.getItems().clear();
		cbCollectors.getItems().addAll(collectors);

		if (collectors.size() == 1) {
			cbCollectors.getSelectionModel().select(0);
		}
	}

	void initialize(DesignerApplication app) throws Exception {
		setApp(app);

		// data collectors
		setDataCollectors();

		// data sources
		cbDataSources.getItems().addAll(DataSourceType.values());

		// resolver types
		cbResolverTypes.getItems().addAll(OeeEventType.values());

		// images
		setImages();

		// class reasons
		initializeResolverTable();
	}

	private void initializeResolverTable() {
		// bind to list of event resolvers
		tvResolvers.setItems(eventResolvers);

		// add the table view listener for reason resolver selection
		tvResolvers.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue != null) {
				try {
					onSelectScriptResolver(newValue);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			} else {
				clearEditor();
			}
		});

		// collector column
		tcCollector.setCellValueFactory(cellDataFeatures -> {
			DataCollector collector = cellDataFeatures.getValue().getCollector();
			SimpleStringProperty property = null;

			if (collector != null) {
				property = new SimpleStringProperty(collector.getName());
			}
			return property;
		});

		// resolver type column
		tcResolverType.setCellValueFactory(cellDataFeatures -> {
			OeeEventType type = cellDataFeatures.getValue().getType();
			SimpleStringProperty property = null;

			if (type != null) {
				property = new SimpleStringProperty(type.toString());
			}

			return property;
		});

		// data source type column
		tcDataSourceType.setCellValueFactory(cellDataFeatures -> {
			CollectorDataSource dataSource = cellDataFeatures.getValue().getDataSource();
			SimpleStringProperty property = null;

			if (dataSource != null && dataSource.getDataSourceType() != null) {
				property = new SimpleStringProperty(dataSource.getDataSourceType().toString());
			}

			return property;
		});

		// server column
		tcServer.setCellValueFactory(cellDataFeatures -> {
			CollectorDataSource dataSource = cellDataFeatures.getValue().getDataSource();
			SimpleStringProperty property = null;

			if (dataSource != null) {
				property = new SimpleStringProperty(dataSource.getId());
			}

			return property;
		});

		// data source id column
		tcSourceId.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getSourceId()));

		// data type column
		tcDataType.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDataType()));

		// script column
		tcScript.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			String functionScript = cellDataFeatures.getValue().getScript();
			if (functionScript != null) {
				ResolverFunction resolver;
				try {
					resolver = new ResolverFunction(functionScript);
					property = new SimpleStringProperty(resolver.getDisplayString());
				} catch (Exception e) {
					// ignore
				}
			}
			return property;
		});

		// update column
		tcUpdatePeriod.setCellValueFactory(cellDataFeatures -> {
			Integer updatePeriod = cellDataFeatures.getValue().getUpdatePeriod();
			return new SimpleObjectProperty<Integer>(updatePeriod);
		});
	}

	private void onSelectScriptResolver(EventResolver eventResolver) {
		if (eventResolver == null) {
			return;
		}
		selectedEventResolver = eventResolver;

		// collector
		cbCollectors.getSelectionModel().select(eventResolver.getCollector());

		// resolver type
		cbResolverTypes.getSelectionModel().select(eventResolver.getType());

		CollectorDataSource source = eventResolver.getDataSource();

		// data source type
		if (source != null) {
			tfServerId.setText(source.getId());
			this.cbDataSources.getSelectionModel().select(source.getDataSourceType());
		}

		// source id
		this.tfSourceId.setText(eventResolver.getSourceId());

		// data type
		this.lbDataType.setText(eventResolver.getDataType());

		// script
		String functionScript = eventResolver.getScript();
		lbScript.setText(null);
		if (functionScript != null) {
			try {
				ResolverFunction resolver = new ResolverFunction(functionScript);
				lbScript.setText(resolver.getDisplayString());
			} catch (Exception e) {
				// ignore
			}
		}

		// update period
		Integer period = eventResolver.getUpdatePeriod();

		if (period == null) {
			period = CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC;
		}
		tfUpdatePeriod.setText(String.valueOf(period));

		btAddResolver.setText(DesignerLocalizer.instance().getLangString("update"));
	}

	void showResolvers(Equipment equipment) {
		if (equipment == null) {
			return;
		}

		clearEditor();

		eventResolvers.clear();
		for (EventResolver resolver : equipment.getScriptResolvers()) {
			eventResolvers.add(resolver);
		}
		tvResolvers.refresh();
	}

	// Remove reason
	@FXML
	private void onRemoveResolver() {
		try {
			if (selectedEventResolver == null) {
				AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.resolver"));
				return;
			}

			// remove from entity
			PlantEntity selectedEntity = getApp().getPhysicalModelController().getSelectedEntity();
			((Equipment) selectedEntity).removeScriptResolver(selectedEventResolver);

			// remove from list
			eventResolvers.remove(selectedEventResolver);
			selectedEventResolver = null;
			tvResolvers.refresh();
			tvResolvers.getSelectionModel().clearSelection();

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	protected void setImages() {
		// new resolver
		btNewResolver.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewResolver.setContentDisplay(ContentDisplay.RIGHT);

		// add resolver
		btAddResolver.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddResolver.setContentDisplay(ContentDisplay.RIGHT);

		// remove resolver
		btRemoveResolver.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveResolver.setContentDisplay(ContentDisplay.RIGHT);

		// watch script execution
		btRun.setGraphic(ImageManager.instance().getImageView(Images.WATCH));
		btRun.setContentDisplay(ContentDisplay.RIGHT);

		// browse to data source
		btBrowseSource.setGraphic(ImageManager.instance().getImageView(Images.SOURCE));
		btBrowseSource.setContentDisplay(ContentDisplay.LEFT);

		// script editor
		btEditScript.setGraphic(ImageManager.instance().getImageView(Images.SCRIPT));
		btEditScript.setContentDisplay(ContentDisplay.LEFT);

		// collector editor
		btEditCollector.setGraphic(ImageManager.instance().getImageView(Images.COLLECTOR));
		btEditCollector.setContentDisplay(ContentDisplay.LEFT);
	}

	@FXML
	private void onSelectResolverType() {
		OeeEventType type = this.cbResolverTypes.getSelectionModel().getSelectedItem();

		if (type != null && getSelectedResolver() != null) {
			getSelectedResolver().setType(type);
		}
	}

	@FXML
	private void onSelectDataSource() {
		DataSourceType sourceType = this.cbDataSources.getSelectionModel().getSelectedItem();

		if (sourceType == null) {
			return;
		}

		ImageView buttonImage = null;
		boolean setUpdatePeriod = false;
		switch (sourceType) {
		case HTTP:
			buttonImage = ImageManager.instance().getImageView(Images.HTTP);
			break;
		case RMQ:
			buttonImage = ImageManager.instance().getImageView(Images.RMQ);
			break;
		case JMS:
			buttonImage = ImageManager.instance().getImageView(Images.JMS);
			break;
		case KAFKA:
			buttonImage = ImageManager.instance().getImageView(Images.KAFKA);
			break;
		case MQTT:
			buttonImage = ImageManager.instance().getImageView(Images.MQTT);
			break;
		case OPC_DA:
			buttonImage = ImageManager.instance().getImageView(Images.OPC_DA);
			setUpdatePeriod = true;
			break;
		case OPC_UA:
			buttonImage = ImageManager.instance().getImageView(Images.OPC_UA);
			setUpdatePeriod = true;
			break;
		case DATABASE:
			buttonImage = ImageManager.instance().getImageView(Images.DB);
			setUpdatePeriod = true;
			break;
		case FILE:
			buttonImage = ImageManager.instance().getImageView(Images.FILE);
			setUpdatePeriod = true;
			break;
		case CRON:
			buttonImage = ImageManager.instance().getImageView(Images.CRON);
			setUpdatePeriod = true;
			break;
		case MODBUS:
			buttonImage = ImageManager.instance().getImageView(Images.MODBUS);
			setUpdatePeriod = true;
			break;
		default:
			break;
		}
		btBrowseSource.setGraphic(buttonImage);

		if (setUpdatePeriod) {
			tfUpdatePeriod.setText(String.valueOf(CollectorDataSource.DEFAULT_UPDATE_PERIOD_MSEC));
		} else {
			tfUpdatePeriod.clear();
		}
	}

	// browse to data source
	@FXML
	private void onBrowseSource() {
		try {
			Equipment equipment = (Equipment) getApp().getPhysicalModelController().getSelectedEntity();
			if (equipment == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.equipment"));
			}

			DataSourceType sourceType = cbDataSources.getSelectionModel().getSelectedItem();
			if (sourceType == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.data.source"));
			}

			if (sourceType.equals(DataSourceType.OPC_DA)) {
				OpcDaBrowserLeaf sourceTag = getApp().showOpcDaDataSourceBrowser();

				if (sourceTag == null) {
					return;
				}

				// leaf tag
				tfSourceId.setText(sourceTag.getPathName());

				// its data source
				OpcDaSource dataSource = getApp().getOpcDaBrowserController().getSource();
				getSelectedResolver().setDataSource(dataSource);
				tfServerId.setText(dataSource.getId());

				// data type
				OpcDaVariant nodeDataType = sourceTag.getDataType();

				if (nodeDataType != null) {
					Class<?> javaType = nodeDataType.getDataType().getJavaClass();
					lbDataType.setText(javaType.getSimpleName());
				} else {
					lbDataType.setText(null);
				}

			} else if (sourceType.equals(DataSourceType.OPC_UA)) {
				OpcUaTreeNode node = getApp().showOpcUaDataSourceBrowser();

				if (node == null) {
					return;
				}

				// variable node
				NamespaceTable nst = getApp().getOpcUaClient().getNamespaceTable();

				if (nst != null) {
					tfSourceId.setText(node.getNodeId(nst).toParseableString());
				}

				// its data source
				OpcUaSource dataSource = getApp().getOpcUaBrowserController().getSource();
				getSelectedResolver().setDataSource(dataSource);
				tfServerId.setText(dataSource.getId());

				// data type
				ExpandedNodeId nodeDataType = node.getNodeDataType();
				Class<?> javaType = BuiltinDataType.getBackingClass(nodeDataType);
				lbDataType.setText(javaType.getSimpleName());

			} else if (sourceType.equals(DataSourceType.HTTP)) {
				// show HTTP server editor
				HttpSource dataSource = getApp().showHttpServerEditor();
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();

			} else if (sourceType.equals(DataSourceType.RMQ) || sourceType.equals(DataSourceType.JMS)
					|| sourceType.equals(DataSourceType.MQTT)) {
				// show MQ broker editor
				CollectorDataSource dataSource = getApp().showMQBrokerEditor(sourceType);
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();

			} else if (sourceType.equals(DataSourceType.DATABASE)) {
				// show database server editor
				DatabaseEventSource dataSource = getApp().showDatabaseServerEditor();
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();

			} else if (sourceType.equals(DataSourceType.FILE)) {
				// show file share editor
				FileEventSource dataSource = getApp().showFileShareEditor();
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();

			} else if (sourceType.equals(DataSourceType.MODBUS)) {
				// show Modbus master editor
				ModbusSource dataSource = getApp().showModbusEditor();
				tfServerId.setText(dataSource.getId());

				EventResolver resolver = getSelectedResolver();
				resolver.setDataSource(dataSource);

				ModbusEndpoint endPoint = dataSource.getEndpoint();
				String sourceId = endPoint.buildSourceId();
				resolver.setSourceId(sourceId);

				lbDataType.setText(endPoint.getDataType().toString());
				tfSourceId.setText(sourceId);
			} else if (sourceType.equals(DataSourceType.CRON)) {
				// show Cron editor
				CronEventSource dataSource = getApp().showCronEditor();
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();

			} else if (sourceType.equals(DataSourceType.KAFKA)) {
				// show Kafka server editor
				KafkaSource dataSource = getApp().showKafkaServerEditor();
				tfServerId.setText(dataSource.getId());

				getSelectedResolver().setDataSource(dataSource);

				lbDataType.setText(String.class.getSimpleName());

				setDefaultSourceId();

			} else {
				throw new Exception(DesignerLocalizer.instance().getErrorString("unknown.type", sourceType));
			}

		} catch (

		Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void setDefaultSourceId() {
		PlantEntity entity = getApp().getPhysicalModelController().getSelectedEntity();

		String sourceId = "";

		if (entity != null) {
			OeeEventType resolverType = cbResolverTypes.getSelectionModel().getSelectedItem();
			DataSourceType sourceType = cbDataSources.getSelectionModel().getSelectedItem();

			if (resolverType != null) {
				sourceId = entity.getName() + "." + sourceType.name() + "." + resolverType.name();
			} else {
				sourceId = entity.getName() + "." + sourceType.name();
			}
			tfSourceId.setText(sourceId);
		}
	}

	// show collector editor
	@FXML
	private void onEditCollector() {
		try {
			getApp().showCollectorEditor();
			setDataCollectors();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// show script editor
	@FXML
	private void onEditScript() {
		try {
			PlantEntity entity = getApp().getPhysicalModelController().getSelectedEntity();

			if (!(entity instanceof Equipment)) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.equipment"));
			}

			// resolver type
			OeeEventType resolverType = cbResolverTypes.getSelectionModel().getSelectedItem();

			if (resolverType == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.type"));
			}

			EventResolver eventResolver = getSelectedResolver();

			eventResolver.setType(resolverType);
			eventResolver.setDataType(lbDataType.getText());

			if (eventResolver.getScript() == null || eventResolver.getScript().length() == 0) {
				if (resolverType.isAvailability()) {
					eventResolver.setScript(EventResolver.createDefaultAvailabilityFunction());
				} else if (resolverType.isProduction()) {
					eventResolver.setScript(EventResolver.createDefaultProductionFunction());
				} else if (resolverType.isMaterial()) {
					eventResolver.setScript(EventResolver.createDefaultMaterialFunction());
				} else if (resolverType.isJob()) {
					eventResolver.setScript(EventResolver.createDefaultJobFunction());
				} else {
					eventResolver.setScript(EventResolver.createDefaultFunction());
				}
			}

			// display the editor
			String scriptFunction = getApp().showScriptEditor(eventResolver);

			if (scriptFunction == null) {
				return;
			}

			eventResolver.setScript(scriptFunction);

			ResolverFunction resolverFunction = new ResolverFunction(scriptFunction);

			// display up to first newline
			this.lbScript.setText(resolverFunction.getDisplayString());

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void clear() {
		clearEditor();

		eventResolvers.clear();
		selectedEventResolver = null;
	}

	void clearEditor() {
		this.cbCollectors.getSelectionModel().clearSelection();
		this.cbCollectors.getSelectionModel().select(null);

		this.cbResolverTypes.getSelectionModel().clearSelection();
		this.cbResolverTypes.getSelectionModel().select(null);

		this.cbDataSources.getSelectionModel().clearSelection();
		this.cbDataSources.getSelectionModel().select(null);

		this.tfSourceId.setText(null);
		this.tfServerId.setText(null);
		this.lbDataType.setText(null);
		this.lbScript.setText(null);

		this.tfUpdatePeriod.setText(null);

		this.btAddResolver.setText(DesignerLocalizer.instance().getLangString("add"));

		this.tvResolvers.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewResolver() {
		try {
			clearEditor();

			selectedEventResolver = null;

			this.btAddResolver.setText(DesignerLocalizer.instance().getLangString("add"));

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// add a new resolver or update an existing one
	@FXML
	private void onAddOrUpdateResolver() {
		try {
			PlantEntity plantEntity = getApp().getPhysicalModelController().getSelectedEntity();

			if (!(plantEntity instanceof Equipment)) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.equipment"));
			}

			// equipment
			Equipment equipment = (Equipment) plantEntity;

			// ensure that we have a resolver
			getSelectedResolver();

			// collector
			selectedEventResolver.setCollector(cbCollectors.getSelectionModel().getSelectedItem());

			// resolver type
			selectedEventResolver.setType(cbResolverTypes.getSelectionModel().getSelectedItem());

			// source id
			selectedEventResolver.setSourceId(tfSourceId.getText());

			// data type
			selectedEventResolver.setDataType(lbDataType.getText());

			// update period
			String updateText = tfUpdatePeriod.getText();

			if (updateText != null && updateText.trim().length() > 0) {
				selectedEventResolver.setUpdatePeriod(Integer.valueOf(updateText));
			}

			// add this resolver
			selectedEventResolver.setEquipment(equipment);

			if (!eventResolvers.contains(selectedEventResolver)) {
				eventResolvers.add(selectedEventResolver);
			}

			Set<EventResolver> resolvers = new HashSet<>();
			resolvers.addAll(eventResolvers);
			equipment.setScriptResolvers(resolvers);

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			tvResolvers.getSelectionModel().clearSelection();
			selectedEventResolver = null;

			tvResolvers.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRun() {
		try {
			if (selectedEventResolver == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.resolver"));
			}

			DataSourceType type = selectedEventResolver.getDataSource().getDataSourceType();

			if (type == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.source.type"));
			}

			if (type.equals(DataSourceType.OPC_DA)) {
				getApp().showOpcDaTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.OPC_UA)) {
				getApp().showOpcUaTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.HTTP)) {
				getApp().showHttpTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.RMQ)) {
				getApp().showMessagingTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.JMS)) {
				getApp().showJMSTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.MQTT)) {
				getApp().showMQTTTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.DATABASE)) {
				getApp().showDatabaseTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.FILE)) {
				getApp().showFileTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.MODBUS)) {
				getApp().showModbusTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.CRON)) {
				getApp().showCronTrendDialog(selectedEventResolver);
			} else if (type.equals(DataSourceType.KAFKA)) {
				getApp().showKafkaTrendDialog(selectedEventResolver);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
