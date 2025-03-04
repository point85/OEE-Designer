package org.point85.app.tester;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.script.ScriptEngine;

import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.cron.CronEventClient;
import org.point85.domain.cron.CronEventListener;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.db.DatabaseEvent;
import org.point85.domain.db.DatabaseEventClient;
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.dto.AreaDto;
import org.point85.domain.dto.EnterpriseDto;
import org.point85.domain.dto.EquipmentDto;
import org.point85.domain.dto.EquipmentEventRequestDto;
import org.point85.domain.dto.PlantEntityResponseDto;
import org.point85.domain.dto.ProductionLineDto;
import org.point85.domain.dto.ReasonDto;
import org.point85.domain.dto.ReasonResponseDto;
import org.point85.domain.dto.SiteDto;
import org.point85.domain.dto.SourceIdResponseDto;
import org.point85.domain.dto.WorkCellDto;
import org.point85.domain.email.EmailClient;
import org.point85.domain.email.EmailSource;
import org.point85.domain.exim.ExportImportContent;
import org.point85.domain.exim.Importer;
import org.point85.domain.file.FileEventClient;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.HttpEventListener;
import org.point85.domain.http.HttpOeeClient;
import org.point85.domain.http.HttpSource;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.jms.JmsClient;
import org.point85.domain.jms.JmsMessageListener;
import org.point85.domain.kafka.KafkaMessageListener;
import org.point85.domain.kafka.KafkaOeeClient;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.modbus.ModbusEndpoint;
import org.point85.domain.modbus.ModbusMaster;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.modbus.ModbusUtils;
import org.point85.domain.modbus.ModbusVariant;
import org.point85.domain.mqtt.MqttMessageListener;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Area;
import org.point85.domain.plant.Enterprise;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.ProductionLine;
import org.point85.domain.plant.Reason;
import org.point85.domain.plant.Site;
import org.point85.domain.plant.WorkCell;
import org.point85.domain.proficy.ProficyClient;
import org.point85.domain.proficy.ProficySource;
import org.point85.domain.rmq.RmqClient;
import org.point85.domain.rmq.RmqMessageListener;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.OeeContext;
import org.point85.domain.script.ResolverFunction;
import org.point85.domain.socket.WebSocketMessageListener;
import org.point85.domain.socket.WebSocketOeeClient;
import org.point85.domain.socket.WebSocketSource;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class TesterController implements RmqMessageListener, JmsMessageListener, MqttMessageListener,
		KafkaMessageListener, CronEventListener, WebSocketMessageListener, HttpEventListener {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(TesterController.class);

	// RMQ message publisher/subscriber
	private final Map<String, RmqClient> rmqClients = new HashMap<>();

	// JMS message publisher/subscriber
	private final Map<String, JmsClient> jmsClients = new HashMap<>();

	// Kafka message publisher/subscriber
	private final Map<String, KafkaOeeClient> kafkaClients = new HashMap<>();

	// Email server
	private final Map<String, EmailClient> emailClients = new HashMap<>();

	// MQTT message publisher/subscriber
	private final Map<String, MqttOeeClient> mqttClients = new HashMap<>();

	// database clients
	private final Map<String, DatabaseEventClient> databaseClients = new HashMap<>();

	// file clients
	private final Map<String, FileEventClient> fileClients = new HashMap<>();

	// cron clients
	private final Map<String, CronEventClient> cronClients = new HashMap<>();

	// Modbus master
	private final Map<String, ModbusMaster> modbusMasters = new HashMap<>();

	// OPC UA clients
	private final Map<String, UaOpcClient> uaClients = new HashMap<>();

	// OPC DA clients
	private final Map<String, DaOpcClient> daClients = new HashMap<>();

	// Proficy clients
	private final Map<String, ProficyClient> proficyClients = new HashMap<>();

	// web socket clients
	private final Map<String, WebSocketOeeClient> webSocketClients = new HashMap<>();

	// HTTP test client
	private HttpOeeClient httpClient;

	// materials
	private final ObservableList<Material> materials = FXCollections.observableList(new ArrayList<>());

	// data sources
	private final ObservableList<CollectorDataSource> collectorDataSources = FXCollections
			.observableList(new ArrayList<>());

	// selected data source
	private CollectorDataSource selectedDataSource;

	// selected equipment
	private PlantEntity selectedEntity;

	// source for GETs
	private HttpSource httpSource;

	// JSON parser
	private final Gson gson = new Gson();

	// entities
	@FXML
	private TreeTableView<PlantEntity> ttvEntities;

	@FXML
	private TreeTableColumn<PlantEntity, String> ttcEntityName;

	@FXML
	private TreeTableColumn<PlantEntity, String> ttcEntityDescription;

	@FXML
	private TreeTableColumn<PlantEntity, String> ttcEntityLevel;

	// reasons
	@FXML
	private TreeTableView<Reason> ttvReasons;

	@FXML
	private TreeTableColumn<Reason, String> ttcReasonName;

	@FXML
	private TreeTableColumn<Reason, String> ttcLossCategory;

	@FXML
	private TreeTableColumn<Reason, String> ttcReasonDescription;

	// material
	@FXML
	private TableView<Material> tvMaterials;

	@FXML
	private TableColumn<Material, String> tcMaterialName;

	@FXML
	private TableColumn<Material, String> tcMaterialDescription;

	@FXML
	private TableColumn<Material, String> tcMaterialCategory;

	// data source
	@FXML
	private ComboBox<CollectorDataSource> cbHost;

	// source ids
	@FXML
	private ComboBox<String> cbSourceId;

	@FXML
	private TextField tfValue;

	@FXML
	private TextField tfReason;

	@FXML
	private TextField tfLoadRate;

	@FXML
	private Button btTest;

	@FXML
	private Button btLoadTest;

	@FXML
	private Button btReset;

	@FXML
	private Button btGetMaterials;

	@FXML
	private Button btExecuteScript;

	@FXML
	private Button btGetReasons;

	@FXML
	private Button btGetEntities;

	@FXML
	private Label lbNotification;

	@FXML
	private ComboBox<DataSourceType> cbDataSourceType;

	// list of data source types
	private final ObservableList<DataSourceType> dataSourceTypes = FXCollections.observableArrayList(new ArrayList<>());

	// timer to broadcast status
	private Timer loadTimer;

	private boolean isRunning = false;

	// associated task
	private LoadTask loadTask;

	// script execution context
	private OeeContext appContext;

	// the JavaScript engine
	private ScriptEngine scriptEngine;

	// script editor
	@FXML
	private TextArea taScript;

	@FXML
	private Object onExecuteScript() throws Exception {
		EventResolver eventResolver = new EventResolver();
		Object result = null;
		String script = taScript.getText();

		if (script == null || script.length() == 0) {
			return result;
		}

		try {
			// create the functions
			String functionScript = ResolverFunction.functionFromBody(script);
			ResolverFunction resolver = new ResolverFunction(functionScript);

			// invoke script function
			result = resolver.invoke(scriptEngine, appContext, new String(""), eventResolver);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
		return result;
	}

	void initialize() {
		// script engine and context
		scriptEngine = new NashornScriptEngineFactory().getScriptEngine();
		appContext = new OeeContext();

		setImages();

		initializeEntityTable();
		initializeMaterialTable();
		initializeReasonTable();

		cbHost.getSelectionModel().selectedIndexProperty().addListener((Observable o) -> {
			int idx = cbHost.getSelectionModel().getSelectedIndex();

			if (idx != -1) {
				selectedDataSource = cbHost.getSelectionModel().getSelectedItem();
			}
		});

		dataSourceTypes.addAll(DataSourceType.values());
		Collections.sort(dataSourceTypes);
		cbDataSourceType.setItems(dataSourceTypes);

		// insert 4 spaces instead of a 8 char tab
		taScript.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			final KeyCombination combo = new KeyCodeCombination(KeyCode.TAB);

			@Override
			public void handle(KeyEvent event) {
				// check for only tab key
				if (combo.match(event)) {
					taScript.insertText(taScript.getCaretPosition(), "    ");
					event.consume();
				}
			}
		});
	}

	private void setImages() {
		// entity
		btGetEntities.setGraphic(ImageManager.instance().getImageView(Images.EQUIPMENT));
		btGetEntities.setContentDisplay(ContentDisplay.LEFT);

		// materials
		btGetMaterials.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
		btGetMaterials.setContentDisplay(ContentDisplay.LEFT);

		// reasons
		btGetReasons.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		btGetReasons.setContentDisplay(ContentDisplay.LEFT);

		// execute
		btTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btTest.setContentDisplay(ContentDisplay.LEFT);

		// reset
		btReset.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));
		btReset.setContentDisplay(ContentDisplay.LEFT);

		// load test
		btLoadTest.setGraphic(ImageManager.instance().getImageView(Images.STARTUP));
		btLoadTest.setContentDisplay(ContentDisplay.LEFT);

		// script editor
		btExecuteScript.setGraphic(ImageManager.instance().getImageView(Images.SCRIPT));
		btExecuteScript.setContentDisplay(ContentDisplay.LEFT);
	}

	@FXML
	private void onSelectSourceType() {
		try {
			DataSourceType sourceType = cbDataSourceType.getSelectionModel().getSelectedItem();
			populateDataSources(sourceType);

			cbSourceId.getSelectionModel().select(null);
			tfValue.clear();
			tfReason.clear();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSelectDataSourceType() {
		try {
			onSelectSourceType();

			/*
			 * tvMaterials.getSelectionModel().clearSelection();
			 * ttvEntities.getSelectionModel().clearSelection();
			 * ttvReasons.getSelectionModel().clearSelection();
			 */
			lbNotification.setText(null);

			// selectedEntity = null;
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private HttpSource launchHttpServer() throws Exception {
		HttpSource dataSource = new HttpSource();
		dataSource.setPort(OeeHttpServer.DEFAULT_PORT);
		dataSource.setHost("localhost");
		dataSource.setDescription("internal server");

		OeeHttpServer httpServer = new OeeHttpServer(OeeHttpServer.DEFAULT_PORT);
		httpServer.startup();
		OeeHttpServer.setDataChangeListener(this);

		return dataSource;
	}

	@FXML
	private void onSelectSource() {
		try {
			// populate source ids for this source type
			DataSourceType sourceType = cbDataSourceType.getSelectionModel().getSelectedItem();

			if (sourceType == null) {
				return;
			}
			populateSourceIds(sourceType);

			if (sourceType.equals(DataSourceType.HTTP)) {
				// save the selected HTTP server
				int idx = cbHost.getSelectionModel().getSelectedIndex();
				if (idx != -1) {
					httpSource = (HttpSource) cbHost.getItems().get(idx);
				} else {
					// use internal server
					httpSource = launchHttpServer();
				}
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	public void stop() {
		try {
			// disconnect RMQ brokers
			for (Entry<String, RmqClient> entry : rmqClients.entrySet()) {
				entry.getValue().disconnect();
			}
			rmqClients.clear();

			// disconnect JMS brokers
			for (Entry<String, JmsClient> entry : jmsClients.entrySet()) {
				entry.getValue().disconnect();
			}
			jmsClients.clear();

			// disconnect Kafka brokers
			for (Entry<String, KafkaOeeClient> entry : kafkaClients.entrySet()) {
				entry.getValue().disconnect();
			}
			kafkaClients.clear();

			// disconnect MQTT brokers
			for (Entry<String, MqttOeeClient> entry : mqttClients.entrySet()) {
				entry.getValue().disconnect();
			}
			mqttClients.clear();

			// stop email checking
			for (Entry<String, EmailClient> entry : emailClients.entrySet()) {
				entry.getValue().stopPolling();
			}
			emailClients.clear();

			// disconnect web socket clients
			for (Entry<String, WebSocketOeeClient> entry : webSocketClients.entrySet()) {
				entry.getValue().closeConnection();
			}
			webSocketClients.clear();

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	private void initializeEntityTable() {
		// add the table view listener
		ttvEntities.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				if (newValue != null) {
					onSelectEntity(newValue.getValue());
				}
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		// entity name column
		ttcEntityName.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getValue().getName()));

		// entity description column
		ttcEntityDescription.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getValue().getDescription()));

		// entity level column
		ttcEntityLevel.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(
				cellDataFeatures.getValue().getValue().getLevel().toString()));

		PlantEntity rootEntity = new PlantEntity("root", "root", null);
		TreeItem<PlantEntity> root = new TreeItem<>(rootEntity);
		ttvEntities.setShowRoot(false);

		ttvEntities.setRoot(root);
	}

	private void initializeReasonTable() {

		// add the table view listener
		ttvReasons.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				if (newValue != null) {
					onSelectReason(newValue.getValue());
				}
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		// reason name column
		ttcReasonName.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getValue().getName()));

		// reason description column
		ttcReasonDescription.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getValue().getDescription()));

		// reason loss category
		ttcLossCategory.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;

			Reason reason = cellDataFeatures.getValue().getValue();

			if (reason != null) {
				TimeLoss loss = reason.getLossCategory();

				if (loss != null) {
					property = new SimpleStringProperty(loss.name());
				}
			}
			return property;
		});

		TreeItem<Reason> root = new TreeItem<>(new Reason("root", "root"));
		ttvReasons.setShowRoot(false);
		ttvReasons.setRoot(root);
	}

	private void initializeMaterialTable() {
		tvMaterials.setItems(materials);

		// add the table view listener for reason resolver selection
		tvMaterials.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				if (newValue != null) {
					onSelectMaterial(newValue);
				}
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		// material name column
		tcMaterialName.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getName()));

		// material description column
		tcMaterialDescription.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDescription()));

		// material category column
		tcMaterialCategory.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getCategory()));
	}

	private String buildHttpUrl(String endpoint) throws Exception {
		return "http://" + httpSource.getHost() + ":" + httpSource.getPort() + '/' + endpoint;
	}

	protected List<String> getSourceIds(DataSourceType sourceType) throws Exception {
		SourceIdResponseDto idDto = getHttpClient().getSourceIds(selectedEntity, sourceType);
		return idDto.getSourceIds();
	}

	private List<String> fetchSourceIds(DataSourceType sourceType) throws Exception {
		return PersistenceService.instance().fetchResolverSourceIds(selectedEntity.getName(), sourceType);
	}

	private void populateSourceIds(DataSourceType sourceType) throws Exception {
		if (sourceType == null) {
			return;
		}

		if (selectedEntity == null) {
			lbNotification.setText(TesterLocalizer.instance().getLangString("no.entity"));
			return;
		}

		List<String> sources = fetchSourceIds(sourceType);

		cbSourceId.getSelectionModel().clearSelection();

		cbSourceId.getItems().clear();
		cbSourceId.getItems().addAll(sources);

		if (sources.size() == 1) {
			cbSourceId.getSelectionModel().select(0);
		}
	}

	private void onSelectEntity(PlantEntity entity) {
		try {
			lbNotification.setText(null);

			selectedEntity = entity;

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void onSelectMaterial(Material material) {
		if (material != null) {
			tfValue.setText(material.getName());
			tfReason.clear();
		}
	}

	private void onSelectReason(Reason reason) {
		if (reason != null) {
			tfValue.setText(reason.getName());
			tfReason.setText(reason.getName());
		}
	}

	@FXML
	private void onTest() {
		try {
			DataSourceType sourceType = cbDataSourceType.getSelectionModel().getSelectedItem();

			if (sourceType == null) {
				return;
			}

			switch (sourceType) {
			case CRON:
				onTriggerCronEvent();
				break;
			case DATABASE:
				onWriteDatabaseEvent();
				break;
			case FILE:
				onWriteFileEvent();
				break;
			case HTTP:
				onHttpPostEvent();
				break;
			case JMS:
			case KAFKA:
			case MQTT:
			case RMQ:
			case WEB_SOCKET:
			case EMAIL:
				onSendEquipmentEventMsg();
				break;
			case MODBUS:
				onWriteModbusEvent();
				break;
			case OPC_DA:
				onWriteOpcDaEvent();
				break;
			case OPC_UA:
				onWriteOpcUaEvent();
				break;
			case PROFICY:
				onWriteProficyEvent();
				break;
			default:
				break;
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void startStopLoadTimer() {
		if (loadTimer == null) {
			// create timer and task
			int period = Integer.parseInt(tfLoadRate.getText());

			loadTimer = new Timer();
			loadTask = new LoadTask();
			loadTimer.schedule(loadTask, (long) period * 1000, (long) period * 1000);

			isRunning = true;

			if (logger.isInfoEnabled()) {
				logger.info("Scheduled load test task for interval (sec): " + period);
			}
		} else {
			// stop the test
			loadTask.cancel();
			loadTimer = null;

			isRunning = false;

			if (logger.isInfoEnabled()) {
				logger.info("Load test task cancelled.");
			}
		}
	}

	@FXML
	private void onLoadTest() {
		try {
			startStopLoadTimer();

			if (isRunning) {
				btLoadTest.setText(TesterLocalizer.instance().getLangString("load.test.stop"));
			} else {
				btLoadTest.setText(TesterLocalizer.instance().getLangString("load.test.start"));
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void onWriteDatabaseEvent() throws Exception {
		DatabaseEventSource source = (DatabaseEventSource) selectedDataSource;

		if (source == null) {
			return;
		}

		String jdbcConn = source.getHost();

		DatabaseEventClient databaseClient = databaseClients.get(jdbcConn);

		if (databaseClient == null) {
			databaseClient = new DatabaseEventClient();
			databaseClient.connectToServer(source.getHost(), source.getUserName(), source.getUserPassword());
			databaseClients.put(jdbcConn, databaseClient);
		}

		// write to the interface table
		DatabaseEvent databaseEvent = new DatabaseEvent();
		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();
		databaseEvent.setSourceId(sourceId);
		databaseEvent.setInputValue(tfValue.getText());
		databaseEvent.setEventTime(OffsetDateTime.now());
		databaseEvent.setReason(tfReason.getText());

		databaseClient.save(databaseEvent);

		if (logger.isInfoEnabled()) {
			logger.info("Wrote database event record for source id " + sourceId);
		}

		postNotification(TesterLocalizer.instance().getLangString("inserted.event", sourceId));
	}

	private void onTriggerCronEvent() throws Exception {
		CronEventSource source = (CronEventSource) selectedDataSource;

		if (source == null) {
			return;
		}

		String expression = source.getCronExpression();
		String jobName = source.getName();

		CronEventClient cronClient = cronClients.get(jobName);

		if (cronClient == null) {
			List<String> expressions = new ArrayList<>();
			expressions.add(expression);
			cronClient = new CronEventClient(this, expressions);
			cronClients.put(jobName, cronClient);
		}

		cronClient.runJob(jobName);
	}

	private void onWriteFileEvent() throws Exception {
		FileEventSource source = (FileEventSource) selectedDataSource;

		if (source == null) {
			return;
		}

		String filePath = source.getHost();

		FileEventClient fileClient = fileClients.get(filePath);

		if (fileClient == null) {
			fileClient = new FileEventClient();
			fileClients.put(filePath, fileClient);
		}

		// value is the file name
		String fileName = "File" + System.currentTimeMillis();

		File file = new File(fileName);

		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(tfValue.getText());
		} finally {
			if (writer != null) {
				writer.close();
			}
		}

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();

		// move the file to READY folder
		fileClient.moveFileToReadyFolder(file, source, sourceId);

		if (logger.isInfoEnabled()) {
			logger.info("Moved file " + fileName + " to ready folder.");
		}

		postNotification(TesterLocalizer.instance().getLangString("moved.file", fileName));
	}

	private void onWriteModbusEvent() throws Exception {
		ModbusSource source = (ModbusSource) selectedDataSource;

		if (source == null) {
			return;
		}

		String name = source.getName();

		ModbusMaster master = modbusMasters.get(name);

		if (master == null) {
			master = new ModbusMaster(source);
			master.connect();
			modbusMasters.put(name, master);
		}

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();
		String value = tfValue.getText();

		ModbusEndpoint endpoint = new ModbusEndpoint(sourceId);
		ModbusVariant variant = ModbusUtils.toVariant(endpoint.getDataType(), value);

		List<ModbusVariant> values = new ArrayList<>();
		values.add(variant);

		master.writeDataSource(endpoint, values);

		if (logger.isInfoEnabled()) {
			logger.info("Wrote " + value + " to endpoint " + endpoint);
		}

		postNotification(TesterLocalizer.instance().getLangString("wrote.register", value, endpoint.toString()));
	}

	private void onWriteProficyEvent() throws Exception {
		ProficySource source = (ProficySource) selectedDataSource;

		if (source == null) {
			return;
		}

		String name = source.getName();

		ProficyClient proficyClient = proficyClients.get(name);

		if (proficyClient == null) {
			proficyClient = new ProficyClient(source);
			proficyClients.put(name, proficyClient);
		}

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();
		String value = tfValue.getText();

		proficyClient.writeTag(sourceId, value);

		if (logger.isInfoEnabled()) {
			logger.info("Wrote " + value + " to tag " + sourceId);
		}

		postNotification(TesterLocalizer.instance().getLangString("wrote.proficy", value, sourceId));
	}

	private void onWriteOpcDaEvent() throws Exception {
		OpcDaSource source = (OpcDaSource) selectedDataSource;

		if (source == null) {
			return;
		}

		String name = source.getName();

		DaOpcClient client = daClients.get(name);

		if (client == null) {
			client = new DaOpcClient();
			client.connect(source);
			daClients.put(name, client);
		}

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();
		String value = tfValue.getText();

		// read tag to get the data type
		OpcDaVariant readVariant = client.readSynch(sourceId);
		OpcDaVariant writeVariant = null;

		switch (readVariant.getDataType()) {
		case BOOLEAN:
			writeVariant = new OpcDaVariant(Boolean.valueOf(value));
			break;
		case BYTE:
			writeVariant = new OpcDaVariant(Byte.valueOf(value));
			break;
		case I1:
			writeVariant = new OpcDaVariant(Byte.valueOf(value));
			break;
		case I2:
			writeVariant = new OpcDaVariant(Short.valueOf(value));
			break;
		case I4:
			writeVariant = new OpcDaVariant(Integer.parseInt(value));
			break;
		case I8:
			writeVariant = new OpcDaVariant(Long.parseLong(value));
			break;
		case R4:
			writeVariant = new OpcDaVariant(Float.valueOf(value));
			break;
		case R8:
			writeVariant = new OpcDaVariant(Double.valueOf(value));
			break;
		case STRING:
			writeVariant = new OpcDaVariant(value);
			break;
		case DATE:
		case UNKNOWN:
		default:
			break;
		}

		if (writeVariant == null) {
			throw new Exception(TesterLocalizer.instance().getErrorString("no.data.type", value));
		}

		// write the value
		client.writeSynch(sourceId, writeVariant);

		if (logger.isInfoEnabled()) {
			logger.info("Wrote " + value + " to " + sourceId);
		}

		postNotification(TesterLocalizer.instance().getLangString("wrote.opc.da", value, sourceId));
	}

	private void onWriteOpcUaEvent() throws Exception {
		OpcUaSource source = (OpcUaSource) selectedDataSource;

		if (source == null) {
			return;
		}

		String name = source.getName();

		UaOpcClient client = uaClients.get(name);

		if (client == null) {
			client = new UaOpcClient();
			client.connect(source);
			uaClients.put(name, client);
		}

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();

		if (sourceId == null) {
			return;
		}

		NodeId nodeId = NodeId.parse(sourceId);

		String value = tfValue.getText();

		// read node to get data type
		DataValue dataValue = client.readSynch(nodeId);

		// data type
		Optional<ExpandedNodeId> dataType = dataValue.getValue().getDataType();

		ExpandedNodeId nodeDataType = null;
		if (dataType.isPresent()) {
			nodeDataType = dataType.get();
		} else {
			return;
		}
		Class<?> javaType = BuiltinDataType.getBackingClass(nodeDataType);

		if (javaType == null) {
			return;
		}

		// java value
		Variant variant = null;
		if (javaType.equals(Boolean.class)) {
			variant = new Variant(Boolean.valueOf(value));
		} else if (javaType.equals(Byte.class)) {
			variant = new Variant(Byte.valueOf(value));
		} else if (javaType.equals(Short.class)) {
			variant = new Variant(Short.valueOf(value));
		} else if (javaType.equals(Integer.class)) {
			variant = new Variant(Integer.valueOf(value));
		} else if (javaType.equals(Long.class)) {
			variant = new Variant(Long.valueOf(value));
		} else if (javaType.equals(Float.class)) {
			variant = new Variant(Float.valueOf(value));
		} else if (javaType.equals(Double.class)) {
			variant = new Variant(Double.valueOf(value));
		} else if (javaType.equals(String.class)) {
			variant = new Variant(value);
		}

		// write to node
		client.writeSynch(nodeId, variant);

		if (logger.isInfoEnabled()) {
			logger.info("Wrote " + value + " to " + nodeId);
		}

		postNotification(TesterLocalizer.instance().getLangString("wrote.opc.ua", value, nodeId.toParseableString()));
	}

	private void onHttpPostEvent() throws Exception {
		HttpURLConnection conn = null;
		try {
			// POST event
			String urlString = buildHttpUrl(OeeHttpServer.EVENT_EP);

			if (logger.isInfoEnabled()) {
				logger.info("Posting to URL: " + urlString);
			}

			URL url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			String sourceId = cbSourceId.getSelectionModel().getSelectedItem();
			String input = tfValue.getText();

			if (input == null || input.isBlank()) {
				postNotification(TesterLocalizer.instance().getErrorString("no.value", urlString));
				return;
			}

			// for production, a reason can be attached
			EquipmentEventRequestDto dto = new EquipmentEventRequestDto(sourceId, input);
			String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(),
					DomainUtils.OFFSET_DATE_TIME_8601);
			dto.setTimestamp(timestamp);
			if (tfReason.getText() != null && !tfReason.getText().isEmpty()) {
				dto.setReason(tfReason.getText());
			}

			String payload = gson.toJson(dto);

			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			checkResponseCode(conn);

			postNotification(TesterLocalizer.instance().getLangString("posted.message", urlString));

			if (logger.isInfoEnabled()) {
				logger.info("Posted: " + payload);
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private void addChildReasonItems(List<ReasonDto> childDtos, TreeItem<Reason> parentItem) {

		for (ReasonDto childDto : childDtos) {
			// avoid a database query
			Reason childReason = new Reason(childDto.getName(), childDto.getDescription());

			if (childDto.getLossCategory() != null) {
				TimeLoss timeLoss = TimeLoss.valueOf(childDto.getLossCategory());
				childReason.setLossCategory(timeLoss);
			}
			TreeItem<Reason> childItem = new TreeItem<>(childReason);
			parentItem.getChildren().add(childItem);

			addChildReasonItems(childDto.getChildren(), childItem);
		}
	}

	private void addChildEntityItems(Set<PlantEntity> entities, TreeItem<PlantEntity> parentItem) throws Exception {
		for (PlantEntity childEntity : entities) {

			TreeItem<PlantEntity> childItem = new TreeItem<>(childEntity);
			parentItem.getChildren().add(childItem);

			addChildEntityItems(childEntity.getChildren(), childItem);
		}
	}

	private void addChildEntities(Set<PlantEntity> children, TreeItem<PlantEntity> parentItem) {
		for (PlantEntity childEntity : children) {
			TreeItem<PlantEntity> childItem = new TreeItem<>(childEntity);
			parentItem.getChildren().add(childItem);

			addChildEntities(childEntity.getChildren(), childItem);
		}
	}

	private void addChildReasons(Set<Reason> children, TreeItem<Reason> parentItem) {
		for (Reason childEntity : children) {
			TreeItem<Reason> childItem = new TreeItem<>(childEntity);
			parentItem.getChildren().add(childItem);

			addChildReasons(childEntity.getChildren(), childItem);
		}
	}

	private void checkResponseCode(HttpURLConnection conn) throws Exception {
		int codeGroup = conn.getResponseCode() / 100;

		if (codeGroup != 2) {
			String msg = TesterLocalizer.instance().getErrorString("failed.code", conn.getResponseCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output;

			while ((output = br.readLine()) != null) {
				msg += "\n" + output;
			}
			throw new Exception(msg);
		}
	}

	private void fetchPlantEntities() throws Exception {
		List<PlantEntity> entities = PersistenceService.instance().fetchAllPlantEntities();

		ttvEntities.getRoot().getChildren().clear();

		for (PlantEntity entity : entities) {
			if (entity.getParent() == null) {
				// top level
				TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);
				addChildEntities(entity.getChildren(), entityItem);
				ttvEntities.getRoot().getChildren().add(entityItem);
			}
		}
		ttvEntities.refresh();
	}

	private void fetchReasons() throws Exception {
		List<Reason> reasons = PersistenceService.instance().fetchAllReasons();

		ttvReasons.getRoot().getChildren().clear();

		for (Reason reason : reasons) {
			if (reason.getParent() == null) {
				// top level
				TreeItem<Reason> entityItem = new TreeItem<>(reason);
				addChildReasons(reason.getChildren(), entityItem);
				ttvReasons.getRoot().getChildren().add(entityItem);
			}
		}
		ttvReasons.refresh();
	}

	@FXML
	private void onGetPlantEntities() {
		try {
			// from database
			fetchPlantEntities();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private HttpOeeClient getHttpClient() throws Exception {
		if (httpClient == null) {
			httpClient = new HttpOeeClient(httpSource);
		}
		return httpClient;
	}

	protected void getPlantEntities() throws Exception {
		PlantEntityResponseDto responseDto = getHttpClient().getPlantEntities();

		ExportImportContent content = responseDto.getContent();

		ttvEntities.getRoot().getChildren().clear();

		// top-level entities
		for (EnterpriseDto dto : content.getEnterprises()) {
			Enterprise entity = new Enterprise(dto);
			Importer.instance().addSites(entity, dto.getSites());

			TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);
			addChildEntityItems(entity.getChildren(), entityItem);

			ttvEntities.getRoot().getChildren().add(entityItem);
		}

		for (SiteDto dto : content.getSites()) {
			Site entity = new Site(dto);
			Importer.instance().addAreas(entity, dto.getAreas());

			TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);
			addChildEntityItems(entity.getChildren(), entityItem);

			ttvEntities.getRoot().getChildren().add(entityItem);
		}

		for (AreaDto dto : content.getAreas()) {
			Area entity = new Area(dto);
			Importer.instance().addProductionLines(entity, dto.getProductionLines());

			TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);
			addChildEntityItems(entity.getChildren(), entityItem);

			ttvEntities.getRoot().getChildren().add(entityItem);
		}

		for (ProductionLineDto dto : content.getProductionLines()) {
			ProductionLine entity = new ProductionLine(dto);
			Importer.instance().addWorkCells(entity, dto.getWorkCells());

			TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);
			addChildEntityItems(entity.getChildren(), entityItem);

			ttvEntities.getRoot().getChildren().add(entityItem);
		}

		for (WorkCellDto dto : content.getWorkCells()) {
			WorkCell entity = new WorkCell(dto);
			Importer.instance().addEquipment(entity, dto.getEquipment());

			TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);
			addChildEntityItems(entity.getChildren(), entityItem);

			ttvEntities.getRoot().getChildren().add(entityItem);
		}

		for (EquipmentDto dto : content.getEquipment()) {
			Equipment entity = new Equipment(dto);

			TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);

			ttvEntities.getRoot().getChildren().add(entityItem);
		}

		ttvEntities.refresh();
	}

	@FXML
	private void onGetReasons() {
		try {
			fetchReasons();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	protected void getReasons() throws Exception {
		ReasonResponseDto listDto = getHttpClient().getReasons();
		List<ReasonDto> dtoList = listDto.getReasonList();

		ttvReasons.getRoot().getChildren().clear();

		// top-level reasons
		for (ReasonDto dto : dtoList) {
			// avoid database query
			Reason reason = new Reason(dto.getName(), dto.getDescription());

			if (dto.getLossCategory() != null) {
				TimeLoss timeLoss = TimeLoss.valueOf(dto.getLossCategory());
				reason.setLossCategory(timeLoss);
			}

			TreeItem<Reason> reasonItem = new TreeItem<>(reason);
			addChildReasonItems(dto.getChildren(), reasonItem);

			ttvReasons.getRoot().getChildren().add(reasonItem);
		}
		ttvReasons.refresh();
	}

	private List<Material> fetchMaterials() throws Exception {
		return PersistenceService.instance().fetchAllMaterials();
	}

	@FXML
	private void onGetMaterials() {
		try {
			materials.clear();
			materials.addAll(fetchMaterials());
			tvMaterials.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	public void onRmqMessage(ApplicationMessage message) {
		postNotification(message);
	}

	@FXML
	private void onSendEquipmentEventMsg() throws Exception {
		if (selectedDataSource == null) {
			return;
		}

		DataSourceType sourceType = cbDataSourceType.getSelectionModel().getSelectedItem();
		if (sourceType == null) {
			return;
		}

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();

		// for production, a reason can be attached
		EquipmentEventMessage msg = new EquipmentEventMessage();
		msg.setSourceId(sourceId);
		msg.setValue(tfValue.getText());

		if (tfReason.getText() != null && !tfReason.getText().isEmpty()) {
			msg.setReason(tfReason.getText());
		}

		String hostPort = selectedDataSource.getHost() + ":" + selectedDataSource.getPort();

		String notification = null;

		switch (sourceType) {
		case EMAIL: {
			EmailClient emailClient = emailClients.get(hostPort);
			EmailSource emailSource = (EmailSource) selectedDataSource;

			if (emailClient == null) {
				emailClient = new EmailClient(emailSource);
				emailClients.put(hostPort, emailClient);
			}
			emailClient.sendEvent(emailSource.getUserName(),
					TesterLocalizer.instance().getLangString("email.test.message.subject"), msg);
			notification = TesterLocalizer.instance().getLangString("sent.message", selectedDataSource.getHost(),
					selectedDataSource.getPort());
			break;
		}

		case JMS: {
			JmsClient jmsClient = jmsClients.get(hostPort);

			if (jmsClient == null) {
				jmsClient = new JmsClient();
				jmsClients.put(hostPort, jmsClient);
				jmsClient.registerListener(this);

				jmsClient.connect(selectedDataSource.getHost(), selectedDataSource.getPort(),
						selectedDataSource.getUserName(), selectedDataSource.getUserPassword());
			}
			jmsClient.sendEventMessage(msg);
			notification = TesterLocalizer.instance().getLangString("sent.message", selectedDataSource.getHost(),
					selectedDataSource.getPort());
			break;
		}
		case KAFKA: {
			KafkaOeeClient kafkaClient = kafkaClients.get(hostPort);

			if (kafkaClient == null) {
				kafkaClient = new KafkaOeeClient();
				kafkaClients.put(hostPort, kafkaClient);

				// to send equipment event messages
				kafkaClient.createProducer((KafkaSource) selectedDataSource, KafkaOeeClient.EVENT_TOPIC);

				// to consume notifications
				// subscribe to event messages
				kafkaClient.registerListener(this);
				kafkaClient.createConsumer((KafkaSource) selectedDataSource, KafkaOeeClient.NOTIFICATION_TOPIC);
				kafkaClient.startPolling();
			}
			kafkaClient.sendEventMessage(msg);
			notification = TesterLocalizer.instance().getLangString("sent.message", selectedDataSource.getHost(),
					selectedDataSource.getPort());
			break;
		}

		case MQTT: {
			MqttOeeClient mqttClient = mqttClients.get(hostPort);

			if (mqttClient == null) {
				mqttClient = new MqttOeeClient();
				mqttClients.put(hostPort, mqttClient);
				mqttClient.registerListener(this);

				MqttSource server = (MqttSource) selectedDataSource;
				mqttClient.setAuthenticationConfiguration(server.getUserName(), server.getUserPassword());
				mqttClient.setSSLConfiguration(server.getKeystore(), server.getKeystorePassword(),
						server.getKeyPassword());

				mqttClient.connect(selectedDataSource.getHost(), selectedDataSource.getPort());
			}
			mqttClient.sendEventMessage(msg);
			notification = TesterLocalizer.instance().getLangString("sent.message", selectedDataSource.getHost(),
					selectedDataSource.getPort());
			break;
		}

		case RMQ: {
			RmqClient rmqClient = rmqClients.get(hostPort);

			if (rmqClient == null) {
				rmqClient = new RmqClient();
				rmqClients.put(hostPort, rmqClient);
				rmqClient.registerListener(this);

				rmqClient.connect(selectedDataSource.getHost(), selectedDataSource.getPort(),
						selectedDataSource.getUserName(), selectedDataSource.getUserPassword());
			}
			rmqClient.sendEquipmentEventMessage(msg);
			notification = TesterLocalizer.instance().getLangString("sent.message", selectedDataSource.getHost(),
					selectedDataSource.getPort());
			break;
		}
		case WEB_SOCKET: {
			WebSocketOeeClient wsClient = webSocketClients.get(hostPort);
			WebSocketSource source = (WebSocketSource) selectedDataSource;

			if (wsClient == null) {
				wsClient = new WebSocketOeeClient(source);
				wsClient.registerListener(this);
				webSocketClients.put(hostPort, wsClient);

				wsClient.openConnection();
			}

			wsClient.sendEventMessage(msg);

			notification = TesterLocalizer.instance().getLangString("sent.message", selectedDataSource.getHost(),
					selectedDataSource.getPort());
			break;
		}
		default:
			return;
		}

		postNotification(notification);

	}

	private void populateDataSources(DataSourceType type) {
		if (type == null) {
			return;
		}

		try {
			// query db
			List<CollectorDataSource> dataSources = PersistenceService.instance().fetchDataSources(type);

			collectorDataSources.clear();

			for (CollectorDataSource dataSource : dataSources) {
				// data sources
				collectorDataSources.add(dataSource);
			}
			cbHost.setItems(collectorDataSources);

			cbSourceId.getSelectionModel().clearSelection();
			cbSourceId.getItems().clear();

			if (collectorDataSources.size() == 1) {
				cbHost.getSelectionModel().select(0);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onReset() {
		try {
			onSelectSourceType();

			tvMaterials.getSelectionModel().clearSelection();
			ttvEntities.getSelectionModel().clearSelection();
			ttvReasons.getSelectionModel().clearSelection();

			lbNotification.setText(null);

			selectedEntity = null;
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	public void onJmsMessage(ApplicationMessage message) {
		postNotification(message);
	}

	@Override
	public void onMqttMessage(ApplicationMessage message) {
		postNotification(message);
	}

	@Override
	public void resolveCronEvent(JobExecutionContext context) {
		Platform.runLater(() -> {
			String jobName = context.getJobDetail().getKey().getName();
			String sourceId = (String) context.get(CronEventClient.SOURCE_ID_KEY);

			logger.info("Cron callback for source id '" + sourceId + "' for job " + jobName);

			int idx = cbHost.getSelectionModel().getSelectedIndex();

			String expression = "";
			if (idx != -1) {
				CronEventSource source = (CronEventSource) cbHost.getItems().get(idx);
				expression = source.getCronExpression();
			}

			lbNotification.setText(TesterLocalizer.instance().getLangString("triggered.job", jobName, expression));
		});
	}

	private void postNotification(ApplicationMessage message) {
		Platform.runLater(() -> lbNotification
				.setText(TesterLocalizer.instance().getLangString("received.message") + ", " + message.toString()));

		if (logger.isInfoEnabled()) {
			logger.info("Received message: " + message.toString());
		}
	}

	private void postNotification(String message) {
		Platform.runLater(() -> lbNotification.setText(message));
	}

	@Override
	public void onKafkaMessage(ApplicationMessage message) {
		postNotification(message);
	}

	@Override
	public void onWebSocketMessage(ApplicationMessage message) {
		postNotification(message);
	}

	@Override
	public void onHttpEquipmentEvent(EquipmentEventRequestDto dto) throws Exception {
		postNotification(dto.toString());
	}

	/********************* Load Testing Task ***********************************/
	private class LoadTask extends TimerTask {
		@Override
		public void run() {
			try {
				DataSourceType sourceType = cbDataSourceType.getSelectionModel().getSelectedItem();
				if (sourceType == null) {
					return;
				}

				switch (sourceType) {
				case DATABASE:
					onWriteDatabaseEvent();
					break;
				case FILE:
					onWriteFileEvent();
					break;
				case HTTP:
					onHttpPostEvent();
					break;
				case EMAIL:
				case JMS:
				case KAFKA:
				case MQTT:
				case RMQ:
					onSendEquipmentEventMsg();
					break;
				case MODBUS:
					onWriteModbusEvent();
					break;
				case PROFICY:
					onWriteProficyEvent();
					break;
				case CRON:
				case OPC_DA:
				case OPC_UA:
				case WEB_SOCKET:
				default:
					break;
				}
			} catch (Exception e) {
				postNotification(e.getMessage());
			}
		}
	}
}
