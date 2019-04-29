package org.point85.app.tester;

import java.io.BufferedReader;
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

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
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
import org.point85.domain.jms.JMSClient;
import org.point85.domain.jms.JMSEquipmentEventListener;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.MessagingClient;
import org.point85.domain.messaging.RoutingKey;
import org.point85.domain.mqtt.MQTTClient;
import org.point85.domain.mqtt.MQTTEquipmentEventListener;
import org.point85.domain.mqtt.QualityOfService;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.EntityLevel;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

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

public class TesterController implements MessageListener, JMSEquipmentEventListener, MQTTEquipmentEventListener {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(TesterController.class);

	// RMQ message publisher/subscriber
	private final Map<String, MessagingClient> pubsubs = new HashMap<>();

	// JMS message publisher/subscriber
	private final Map<String, JMSClient> jmsClients = new HashMap<>();

	// MQTT message publisher/subscriber
	private final Map<String, MQTTClient> mqttClients = new HashMap<>();

	// materials
	private final ObservableList<Material> materials = FXCollections.observableList(new ArrayList<>());

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

	// HTTP
	@FXML
	private ComboBox<HttpSource> cbHttpHostPort;

	@FXML
	private Button btHttpPost;

	@FXML
	private ComboBox<String> cbHttpSourceId;

	@FXML
	private TextField tfHttpValue;

	// Messaging
	@FXML
	private ComboBox<CollectorDataSource> cbMsgHost;

	@FXML
	private ComboBox<String> cbMsgSourceId;

	@FXML
	private TextField tfMsgValue;

	@FXML
	private Button btMsgSend;

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
	private Label lbNotification;

	void initialize() throws Exception {
		setImages();

		initializeEntityTable();
		initializeMaterialTable();
		initializeReasonTable();

		populateHttpSourceIds();
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

		// post
		btHttpPost.setGraphic(ImageManager.instance().getImageView(Images.HTTP));
		btHttpPost.setContentDisplay(ContentDisplay.LEFT);

		// send
		btMsgSend.setGraphic(ImageManager.instance().getImageView(Images.RMQ));
		btMsgSend.setContentDisplay(ContentDisplay.LEFT);

		// reset
		btReset.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));
		btReset.setContentDisplay(ContentDisplay.LEFT);
	}

	@FXML
	private void onSelectBrokerType() {
		if (rbRMQ.isSelected()) {
			populateMsgSourceIds(DataSourceType.MESSAGING);
		} else if (rbJMS.isSelected()) {
			populateMsgSourceIds(DataSourceType.JMS);
		} else if (rbMQTT.isSelected()) {
			populateMsgSourceIds(DataSourceType.MQTT);
		}
	}

	@FXML
	private void onSelectHttpSource() {
		tfHttpValue.clear();
	}

	@FXML
	private void onSelectMessagingSource() {
		tfMsgValue.clear();
	}

	public void stop() {
		try {
			// disconnect RMQ brokers
			for (Entry<String, MessagingClient> entry : pubsubs.entrySet()) {
				entry.getValue().disconnect();
			}
			pubsubs.clear();

			// disconnect JMS brokers
			for (Entry<String, JMSClient> entry : jmsClients.entrySet()) {
				entry.getValue().disconnect();
			}
			jmsClients.clear();

			// disconnect MQTT brokers
			for (Entry<String, MQTTClient> entry : mqttClients.entrySet()) {
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
		int idx = cbHttpHostPort.getSelectionModel().getSelectedIndex();
		HttpSource source = cbHttpHostPort.getItems().get(idx);

		if (source == null) {
			throw new Exception(TesterLocalizer.instance().getErrorString("no.http.server"));
		}
		return "http://" + source.getHost() + ":" + source.getPort() + '/' + endpoint;
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

	private void populateHttpSourceIds(PlantEntity entity) throws Exception {
		HttpURLConnection conn = null;
		try {
			// refresh list of HTTP source Ids
			String urlString = buildHttpUrl(OeeHttpServer.SOURCE_ID_EP);
			urlString = addQueryParameter(urlString, OeeHttpServer.EQUIP_ATTRIB, entity.getName());
			urlString = addQueryParameter(urlString, OeeHttpServer.DS_TYPE_ATTRIB, DataSourceType.HTTP.name());
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

			cbHttpSourceId.getSelectionModel().clearSelection();

			cbHttpSourceId.getItems().clear();
			cbHttpSourceId.getItems().addAll(sources);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private void populateMessagingSourceIds(PlantEntity entity) throws Exception {
		HttpURLConnection conn = null;
		try {
			// refresh list of RMQ or AMQ source Ids
			String urlString = buildHttpUrl(OeeHttpServer.SOURCE_ID_EP);
			urlString = addQueryParameter(urlString, OeeHttpServer.EQUIP_ATTRIB, entity.getName());

			DataSourceType sourceType = null;

			if (this.rbRMQ.isSelected()) {
				sourceType = DataSourceType.MESSAGING;
			} else if (rbJMS.isSelected()) {
				sourceType = DataSourceType.JMS;
			} else if (rbMQTT.isSelected()) {
				sourceType = DataSourceType.MQTT;
			} else {
				return;
			}

			String messagingType = sourceType.name();

			urlString = addQueryParameter(urlString, OeeHttpServer.DS_TYPE_ATTRIB, messagingType);

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

			cbMsgSourceId.getSelectionModel().clearSelection();

			cbMsgSourceId.getItems().clear();
			cbMsgSourceId.getItems().addAll(sources);
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

			populateHttpSourceIds(entity);

			populateMessagingSourceIds(entity);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void onSelectMaterial(Material material) {
		if (material != null) {
			tfHttpValue.setText(material.getName());
			tfMsgValue.setText(material.getName());
		}
	}

	private void onSelectReason(Reason reason) {
		if (reason != null) {
			tfHttpValue.setText(reason.getName());
			tfMsgValue.setText(reason.getName());
		}
	}

	@FXML
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

			String sourceId = (String) cbHttpSourceId.getSelectionModel().getSelectedItem();
			String input = tfHttpValue.getText();

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
			
			lbNotification.setText(DesignerLocalizer.instance().getLangString("posted.message", urlString));
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
			String msg = TesterLocalizer.instance().getErrorString("failed.code") + " " + conn.getResponseCode() + "\n";

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
	public void onMessage(Channel channel, Envelope envelope, ApplicationMessage message) {
		logger.warn("Received unhandled RMQ message: " + message.toString());
	}

	@FXML
	private void onSendEquipmentEventMsg() {
		try {
			int selectedIndex = cbMsgHost.getSelectionModel().getSelectedIndex();

			if (selectedIndex == -1) {
				throw new Exception(TesterLocalizer.instance().getErrorString("no.host"));
			}
			CollectorDataSource source = cbMsgHost.getItems().get(selectedIndex);

			if (source == null) {
				throw new Exception(TesterLocalizer.instance().getErrorString("no.source"));
			}

			String sourceId = (String) cbMsgSourceId.getSelectionModel().getSelectedItem();

			String input = tfMsgValue.getText();
			// for production, a reason can be attached
			String[] values = AppUtils.parseCsvInput(input);

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(values[0]);
			msg.setReason(values[1]);

			String hostPort = source.getHost() + ":" + source.getPort();
			
			String notification = null;

			if (rbRMQ.isSelected()) {

				MessagingClient pubsub = pubsubs.get(hostPort);

				if (pubsub == null) {
					pubsub = new MessagingClient();
					pubsubs.put(hostPort, pubsub);

					pubsub.connect(source.getHost(), source.getPort(), source.getUserName(), source.getUserPassword());
				}
				pubsub.publish(msg, RoutingKey.EQUIPMENT_SOURCE_EVENT, 30);
				
				notification = DesignerLocalizer.instance().getLangString("sent.message", source.getHost(), source.getPort());
			} else if (rbJMS.isSelected()) {
				JMSClient jmsClient = jmsClients.get(hostPort);

				if (jmsClient == null) {
					jmsClient = new JMSClient();
					jmsClients.put(hostPort, jmsClient);

					jmsClient.connect(source.getHost(), source.getPort(), source.getUserName(),
							source.getUserPassword());
				}
				jmsClient.sendToQueue(msg, JMSClient.DEFAULT_QUEUE, 30);
				notification = DesignerLocalizer.instance().getLangString("sent.message", source.getHost(), source.getPort());
			} else if (rbMQTT.isSelected()) {
				MQTTClient mqttClient = mqttClients.get(hostPort);

				if (mqttClient == null) {
					mqttClient = new MQTTClient();
					mqttClients.put(hostPort, mqttClient);

					mqttClient.connect(source.getHost(), source.getPort(), source.getUserName(),
							source.getUserPassword());
				}
				mqttClient.publish(msg, QualityOfService.AT_MOST_ONCE);
				notification = DesignerLocalizer.instance().getLangString("sent.message", source.getHost(), source.getPort());
			} else {
				return;
			}
			lbNotification.setText(notification);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateHttpSourceIds() {
		try {
			// query db
			List<CollectorDataSource> dataSources = PersistenceService.instance().fetchDataSources(DataSourceType.HTTP);

			cbHttpHostPort.getSelectionModel().clearSelection();
			ObservableList<HttpSource> items = cbHttpHostPort.getItems();
			items.clear();

			for (CollectorDataSource dataSource : dataSources) {
				items.add((HttpSource) dataSource);
			}

			cbHttpSourceId.getSelectionModel().clearSelection();
			cbHttpSourceId.getItems().clear();
			tfHttpValue.clear();

			if (items.size() == 1) {
				cbHttpHostPort.getSelectionModel().select(0);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateMsgSourceIds(DataSourceType type) {
		try {
			// query db
			List<CollectorDataSource> dataSources = PersistenceService.instance().fetchDataSources(type);

			cbMsgHost.getSelectionModel().clearSelection();
			ObservableList<CollectorDataSource> items = cbMsgHost.getItems();
			items.clear();

			for (CollectorDataSource dataSource : dataSources) {
				// data sources
				items.add(dataSource);
			}

			cbMsgSourceId.getSelectionModel().clearSelection();
			cbMsgSourceId.getItems().clear();
			tfMsgValue.clear();

			if (items.size() == 1) {
				cbMsgHost.getSelectionModel().select(0);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onReset() {
		try {
			populateHttpSourceIds();
			onSelectBrokerType();

			tvMaterials.getSelectionModel().clearSelection();
			ttvEntities.getSelectionModel().clearSelection();
			ttvReasons.getSelectionModel().clearSelection();
			
			lbNotification.setText(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	public void onJMSEquipmentEvent(EquipmentEventMessage message) {
		logger.info("Received AMQ message: " + message.toString());
	}

	@Override
	public void onMQTTEquipmentEvent(EquipmentEventMessage message) {
		logger.info("Received MQTT message: " + message.toString());
	}

}
