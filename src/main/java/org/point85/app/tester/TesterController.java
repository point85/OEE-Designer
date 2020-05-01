package org.point85.app.tester;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
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
import org.point85.domain.file.FileEventClient;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.http.EquipmentEventRequestDto;
import org.point85.domain.http.HttpSource;
import org.point85.domain.http.MaterialDto;
import org.point85.domain.http.MaterialResponseDto;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.http.PlantEntityDto;
import org.point85.domain.http.PlantEntityResponseDto;
import org.point85.domain.http.ReasonDto;
import org.point85.domain.http.ReasonResponseDto;
import org.point85.domain.http.SourceIdResponseDto;
import org.point85.domain.jms.JmsClient;
import org.point85.domain.jms.JmsMessageListener;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.modbus.ModbusEndpoint;
import org.point85.domain.modbus.ModbusMaster;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.modbus.ModbusUtils;
import org.point85.domain.modbus.ModbusVariant;
import org.point85.domain.mqtt.MqttMessageListener;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.opc.da.DaOpcClient;
import org.point85.domain.opc.da.OpcDaSource;
import org.point85.domain.opc.da.OpcDaVariant;
import org.point85.domain.opc.ua.OpcUaSource;
import org.point85.domain.opc.ua.UaOpcClient;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.rmq.RmqClient;
import org.point85.domain.rmq.RmqMessageListener;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

public class TesterController
		implements RmqMessageListener, JmsMessageListener, MqttMessageListener, CronEventListener {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(TesterController.class);

	// RMQ message publisher/subscriber
	private final Map<String, RmqClient> rmqClients = new HashMap<>();

	// JMS message publisher/subscriber
	private final Map<String, JmsClient> jmsClients = new HashMap<>();

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

	// materials
	private final ObservableList<Material> materials = FXCollections.observableList(new ArrayList<>());

	// data sources
	private final ObservableList<CollectorDataSource> collectorDataSources = FXCollections
			.observableList(new ArrayList<>());

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

	@FXML
	private ComboBox<String> cbSourceId;

	@FXML
	private TextField tfValue;

	@FXML
	private TextField tfReason;

	@FXML
	private Button btTest;

	@FXML
	private Button btReset;

	@FXML
	private Button btHttpGetMaterials;

	@FXML
	private Button btHttpGetReasons;

	@FXML
	private Button btHttpGetEntities;

	@FXML
	private RadioButton rbRMQ;

	@FXML
	private RadioButton rbJMS;

	@FXML
	private RadioButton rbMQTT;

	@FXML
	private RadioButton rbHTTP;

	@FXML
	private RadioButton rbModbus;

	@FXML
	private RadioButton rbCron;

	@FXML
	private RadioButton rbDatabase;

	@FXML
	private RadioButton rbFile;

	@FXML
	private RadioButton rbOpcUa;

	@FXML
	private RadioButton rbOpcDa;

	@FXML
	private Label lbNotification;

	void initialize() throws Exception {
		setImages();

		initializeEntityTable();
		initializeMaterialTable();
		initializeReasonTable();

		rbHTTP.setSelected(true);
		onSelectSourceType();
	}

	private void setImages() throws Exception {
		// entity
		btHttpGetEntities.setGraphic(ImageManager.instance().getImageView(Images.EQUIPMENT));
		btHttpGetEntities.setContentDisplay(ContentDisplay.LEFT);

		// materials
		btHttpGetMaterials.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
		btHttpGetMaterials.setContentDisplay(ContentDisplay.LEFT);

		// reasons
		btHttpGetReasons.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		btHttpGetReasons.setContentDisplay(ContentDisplay.LEFT);

		// execute
		btTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btTest.setContentDisplay(ContentDisplay.LEFT);

		// reset
		btReset.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));
		btReset.setContentDisplay(ContentDisplay.LEFT);
	}

	@FXML
	private void onSelectSourceType() {
		try {
			if (rbRMQ.isSelected()) {
				populateDataSources(DataSourceType.RMQ);
			} else if (rbJMS.isSelected()) {
				populateDataSources(DataSourceType.JMS);
			} else if (rbMQTT.isSelected()) {
				populateDataSources(DataSourceType.MQTT);
			} else if (rbHTTP.isSelected()) {
				populateDataSources(DataSourceType.HTTP);

				// save the selected HTTP server
				int idx = cbHost.getSelectionModel().getSelectedIndex();
				if (idx != -1) {
					httpSource = (HttpSource) cbHost.getItems().get(idx);
				} else {
					// use internal server
					httpSource = launchHttpServer();
				}
			} else if (rbDatabase.isSelected()) {
				populateDataSources(DataSourceType.DATABASE);
			} else if (rbFile.isSelected()) {
				populateDataSources(DataSourceType.FILE);
			} else if (rbModbus.isSelected()) {
				populateDataSources(DataSourceType.MODBUS);
			} else if (rbCron.isSelected()) {
				populateDataSources(DataSourceType.CRON);
			} else if (rbOpcUa.isSelected()) {
				populateDataSources(DataSourceType.OPC_UA);
			} else if (rbOpcDa.isSelected()) {
				populateDataSources(DataSourceType.OPC_DA);
			}

			tfValue.clear();
			tfReason.clear();
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
		// httpServer.setDataChangeListener(this);
		httpServer.startup();

		return dataSource;
	}

	@FXML
	private void onSelectSource() {
		try {
			if (rbRMQ.isSelected()) {
				populateSourceIds(DataSourceType.RMQ);
			} else if (rbJMS.isSelected()) {
				populateSourceIds(DataSourceType.JMS);
			} else if (rbMQTT.isSelected()) {
				populateSourceIds(DataSourceType.MQTT);
			} else if (rbHTTP.isSelected()) {
				populateSourceIds(DataSourceType.HTTP);
			} else if (rbDatabase.isSelected()) {
				populateSourceIds(DataSourceType.DATABASE);
			} else if (rbFile.isSelected()) {
				populateSourceIds(DataSourceType.FILE);
			} else if (rbModbus.isSelected()) {
				populateSourceIds(DataSourceType.MODBUS);
			} else if (rbCron.isSelected()) {
				populateSourceIds(DataSourceType.CRON);
			} else if (rbOpcUa.isSelected()) {
				populateSourceIds(DataSourceType.OPC_UA);
			} else if (rbOpcDa.isSelected()) {
				populateSourceIds(DataSourceType.OPC_DA);
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

			// disconnect MQTT brokers
			for (Entry<String, MqttOeeClient> entry : mqttClients.entrySet()) {
				entry.getValue().disconnect();
			}
			mqttClients.clear();
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
		ttcEntityName.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getValue().getName());
		});

		// entity description column
		ttcEntityDescription.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getValue().getDescription());
		});

		// entity level column
		ttcEntityLevel.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getValue().getLevel().toString());
		});

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
		ttcReasonName.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getValue().getName());
		});

		// reason description column
		ttcReasonDescription.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getValue().getDescription());
		});

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
		tcMaterialName.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getName());
		});

		// material description column
		tcMaterialDescription.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getDescription());
		});

		// material category column
		tcMaterialCategory.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getCategory());
		});
	}

	private String buildHttpUrl(String endpoint) throws Exception {
		if (httpSource == null) {
			throw new Exception(TesterLocalizer.instance().getErrorString("no.http.server"));
		}
		return "http://" + httpSource.getHost() + ":" + httpSource.getPort() + '/' + endpoint;
	}

	private String encode(String input) {
		CharSequence space = " ";
		CharSequence encodedSpace = "%20";
		return input.replace(space, encodedSpace);
	}

	private String addQueryParameter(String url, String name, String value) {
		StringBuilder sb = new StringBuilder();

		if (!url.contains("?")) {
			sb.append('?');
		} else {
			sb.append('&');
		}

		// replace spaces
		sb.append(name).append('=').append(encode(value));

		return url + sb.toString();
	}

	private void populateSourceIds(DataSourceType sourceType) throws Exception {
		if (selectedEntity == null) {
			return;
		}

		HttpURLConnection conn = null;
		try {
			// refresh list of source Ids
			String sourceIdUrl = buildHttpUrl(OeeHttpServer.SOURCE_ID_EP);
			String urlString = addQueryParameter(sourceIdUrl, OeeHttpServer.EQUIP_ATTRIB, selectedEntity.getName());
			urlString = addQueryParameter(urlString, OeeHttpServer.DS_TYPE_ATTRIB, sourceType.name());

			URL url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");

			checkResponseCode(conn);

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			StringBuilder sb = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
			}
			in.close();

			String entityString = sb.toString();

			SourceIdResponseDto idDto = gson.fromJson(entityString, SourceIdResponseDto.class);

			List<String> sources = idDto.getSourceIds();

			cbSourceId.getSelectionModel().clearSelection();

			cbSourceId.getItems().clear();
			cbSourceId.getItems().addAll(sources);

			if (sources.size() == 1) {
				cbSourceId.getSelectionModel().select(0);
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private void onSelectEntity(PlantEntity entity) {
		try {
			if (!entity.getLevel().equals(EntityLevel.EQUIPMENT)) {
				return;
			}

			selectedEntity = entity;

			onSelectSource();

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
			if (rbHTTP.isSelected()) {
				onHttpPostEvent();
			} else if (rbJMS.isSelected() || rbRMQ.isSelected() || rbMQTT.isSelected()) {
				onSendEquipmentEventMsg();
			} else if (rbDatabase.isSelected()) {
				onWriteDatabaseEvent();
			} else if (rbFile.isSelected()) {
				onWriteFileEvent();
			} else if (rbModbus.isSelected()) {
				onWriteModbusEvent();
			} else if (rbCron.isSelected()) {
				onTriggerCronEvent();
			} else if (rbOpcUa.isSelected()) {
				onWriteOpcUaEvent();
			} else if (rbOpcDa.isSelected()) {
				onWriteOpcDaEvent();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void onWriteDatabaseEvent() throws Exception {
		DatabaseEventSource source = (DatabaseEventSource) cbHost.getSelectionModel().getSelectedItem();
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

		lbNotification.setText(TesterLocalizer.instance().getLangString("inserted.event", sourceId));
	}

	private void onTriggerCronEvent() throws Exception {
		int idx = cbHost.getSelectionModel().getSelectedIndex();
		CronEventSource source = (CronEventSource) cbHost.getItems().get(idx);

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
		FileEventSource source = (FileEventSource) cbHost.getSelectionModel().getSelectedItem();
		String filePath = source.getHost();

		FileEventClient fileClient = fileClients.get(filePath);

		if (fileClient == null) {
			fileClient = new FileEventClient();
			fileClients.put(filePath, fileClient);
		}

		// value is the file name
		String fileName = tfValue.getText();

		if (fileName == null || fileName.trim().length() == 0) {
			throw new Exception(TesterLocalizer.instance().getErrorString("no.file.name"));
		}

		File file = new File(fileName);

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();

		// move the file to READY folder
		fileClient.moveFile(file, source, sourceId, FileEventClient.READY_FOLDER);

		if (logger.isInfoEnabled()) {
			logger.info("Moved file " + fileName + " to ready folder.");
		}

		lbNotification.setText(TesterLocalizer.instance().getLangString("moved.file", fileName));
	}

	private void onWriteModbusEvent() throws Exception {
		int idx = cbHost.getSelectionModel().getSelectedIndex();
		ModbusSource source = (ModbusSource) cbHost.getItems().get(idx);
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

		lbNotification.setText(TesterLocalizer.instance().getLangString("wrote.register", value, endpoint.toString()));
	}

	private void onWriteOpcDaEvent() throws Exception {
		int idx = cbHost.getSelectionModel().getSelectedIndex();
		OpcDaSource source = (OpcDaSource) cbHost.getItems().get(idx);
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
			writeVariant = new OpcDaVariant(Integer.valueOf(value));
			break;
		case I8:
			writeVariant = new OpcDaVariant(Long.valueOf(value));
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

		lbNotification.setText(TesterLocalizer.instance().getLangString("wrote.opc.da", value, sourceId));
	}

	private void onWriteOpcUaEvent() throws Exception {
		int idx = cbHost.getSelectionModel().getSelectedIndex();
		OpcUaSource source = (OpcUaSource) cbHost.getItems().get(idx);
		String name = source.getName();

		UaOpcClient client = uaClients.get(name);

		if (client == null) {
			client = new UaOpcClient();
			client.connect(source);
			uaClients.put(name, client);
		}

		String sourceId = cbSourceId.getSelectionModel().getSelectedItem();

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

		lbNotification
				.setText(TesterLocalizer.instance().getLangString("wrote.opc.ua", value, nodeId.toParseableString()));
	}

	private void onHttpPostEvent() {
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

			String sourceId = (String) cbSourceId.getSelectionModel().getSelectedItem();
			String input = tfValue.getText();

			// for production, a reason can be attached
			String[] values = AppUtils.parseCsvInput(input);

			EquipmentEventRequestDto dto = new EquipmentEventRequestDto(sourceId, values[0]);
			String timestamp = DomainUtils.offsetDateTimeToString(OffsetDateTime.now(),
					DomainUtils.OFFSET_DATE_TIME_8601);
			dto.setTimestamp(timestamp);
			dto.setReason(values[1]);

			Gson gson = new Gson();
			String payload = gson.toJson(dto);

			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			checkResponseCode(conn);

			lbNotification.setText(TesterLocalizer.instance().getLangString("posted.message", urlString));
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
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

	private void addChildEntityItems(List<PlantEntityDto> childDtos, TreeItem<PlantEntity> parentItem) {

		for (PlantEntityDto childDto : childDtos) {
			EntityLevel level = EntityLevel.valueOf(childDto.getLevel());
			PlantEntity childEntity = new PlantEntity(childDto.getName(), childDto.getDescription(), level);

			TreeItem<PlantEntity> childItem = new TreeItem<>(childEntity);
			parentItem.getChildren().add(childItem);

			addChildEntityItems(childDto.getChildren(), childItem);
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

	@FXML
	private void onHttpGetPlantEntities() {
		HttpURLConnection conn = null;
		try {
			// GET plant entities
			String urlString = buildHttpUrl(OeeHttpServer.ENTITY_EP);

			if (logger.isInfoEnabled()) {
				logger.info("Opening connection to " + urlString);
			}

			URL url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");

			checkResponseCode(conn);

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			StringBuilder sb = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
			}
			in.close();

			String entityString = sb.toString();

			PlantEntityResponseDto listDto = gson.fromJson(entityString, PlantEntityResponseDto.class);
			List<PlantEntityDto> dtoList = listDto.getEntityList();

			ttvEntities.getRoot().getChildren().clear();

			// top-level reasons
			for (PlantEntityDto dto : dtoList) {
				EntityLevel level = EntityLevel.valueOf(dto.getLevel());
				PlantEntity entity = new PlantEntity(dto.getName(), dto.getDescription(), level);

				TreeItem<PlantEntity> entityItem = new TreeItem<>(entity);
				addChildEntityItems(dto.getChildren(), entityItem);

				ttvEntities.getRoot().getChildren().add(entityItem);
			}
			ttvEntities.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	@FXML
	private void onHttpGetReasons() {
		HttpURLConnection conn = null;
		try {
			// GET reason
			String urlString = buildHttpUrl(OeeHttpServer.REASON_EP);
			URL url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");

			checkResponseCode(conn);

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			StringBuilder sb = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
			}
			in.close();

			String entityString = sb.toString();

			ReasonResponseDto listDto = gson.fromJson(entityString, ReasonResponseDto.class);
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

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	@FXML
	private void onHttpGetMaterials() {
		HttpURLConnection conn = null;
		try {
			// GET materials
			String urlString = buildHttpUrl(OeeHttpServer.MATERIAL_EP);
			URL url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");

			checkResponseCode(conn);

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			StringBuilder sb = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				sb.append(inputLine);
			}
			in.close();

			String entityString = sb.toString();

			MaterialResponseDto listDto = gson.fromJson(entityString, MaterialResponseDto.class);
			List<MaterialDto> dtoList = listDto.getMaterialList();

			materials.clear();

			for (MaterialDto dto : dtoList) {
				// avoid a database query
				Material material = new Material(dto.getName(), dto.getDescription());
				material.setCategory(dto.getCategory());
				materials.add(material);
			}
			tvMaterials.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	@Override
	public void onRmqMessage(ApplicationMessage message) {
		logger.warn("Received unhandled RMQ message: " + message.toString());
	}

	@FXML
	private void onSendEquipmentEventMsg() {
		try {
			int selectedIndex = cbHost.getSelectionModel().getSelectedIndex();

			if (selectedIndex == -1) {
				throw new Exception(TesterLocalizer.instance().getErrorString("no.host"));
			}
			CollectorDataSource source = cbHost.getItems().get(selectedIndex);

			if (source == null) {
				throw new Exception(TesterLocalizer.instance().getErrorString("no.source"));
			}

			String sourceId = (String) cbSourceId.getSelectionModel().getSelectedItem();

			String input = tfValue.getText();
			// for production, a reason can be attached
			String[] values = AppUtils.parseCsvInput(input);

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(values[0]);
			msg.setReason(values[1]);

			String hostPort = source.getHost() + ":" + source.getPort();

			String notification = null;

			if (rbRMQ.isSelected()) {

				RmqClient pubsub = rmqClients.get(hostPort);

				if (pubsub == null) {
					pubsub = new RmqClient();
					rmqClients.put(hostPort, pubsub);

					pubsub.connect(source.getHost(), source.getPort(), source.getUserName(), source.getUserPassword());
				}
				pubsub.sendEquipmentEventMessage(msg);
				notification = TesterLocalizer.instance().getLangString("sent.message", source.getHost(),
						source.getPort());
			} else if (rbJMS.isSelected()) {
				JmsClient jmsClient = jmsClients.get(hostPort);

				if (jmsClient == null) {
					jmsClient = new JmsClient();
					jmsClients.put(hostPort, jmsClient);

					jmsClient.connect(source.getHost(), source.getPort(), source.getUserName(),
							source.getUserPassword());
				}
				jmsClient.sendEventMessage(msg);
				notification = TesterLocalizer.instance().getLangString("sent.message", source.getHost(),
						source.getPort());
			} else if (rbMQTT.isSelected()) {
				MqttOeeClient mqttClient = mqttClients.get(hostPort);

				if (mqttClient == null) {
					mqttClient = new MqttOeeClient();
					mqttClients.put(hostPort, mqttClient);

					mqttClient.connect(source.getHost(), source.getPort(), source.getUserName(),
							source.getUserPassword());
				}
				mqttClient.sendEventMessage(msg);
				notification = TesterLocalizer.instance().getLangString("sent.message", source.getHost(),
						source.getPort());
			} else {
				return;
			}
			lbNotification.setText(notification);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources(DataSourceType type) {
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
		logger.info("Received AMQ message: " + message.toString());
	}

	@Override
	public void onMqttMessage(ApplicationMessage message) {
		logger.info("Received MQTT message: " + message.toString());
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
}
