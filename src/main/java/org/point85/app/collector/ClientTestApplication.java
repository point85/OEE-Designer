package org.point85.app.collector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.http.EquipmentEventRequestDto;
import org.point85.domain.http.MaterialDto;
import org.point85.domain.http.MaterialResponseDto;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.http.PlantEntityDto;
import org.point85.domain.http.PlantEntityResponseDto;
import org.point85.domain.http.ReasonDto;
import org.point85.domain.http.ReasonResponseDto;
import org.point85.domain.http.SourceIdResponseDto;
import org.point85.domain.messaging.ApplicationMessage;
import org.point85.domain.messaging.EquipmentEventMessage;
import org.point85.domain.messaging.MessageListener;
import org.point85.domain.messaging.PublisherSubscriber;
import org.point85.domain.messaging.RoutingKey;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ClientTestApplication implements MessageListener {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(ClientTestApplication.class);

	// AMQP message publisher/subscriber
	private Map<String, PublisherSubscriber> pubsubs = new HashMap<>();

	// materials
	private ObservableList<Material> materials = FXCollections.observableList(new ArrayList<>());

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

	@FXML
	private ComboBox<String> cbHttpHostPort;

	@FXML
	private ComboBox<String> cbRmqHostPort;

	@FXML
	private Button btHttpPost;

	@FXML
	private Button btRmqSend;

	@FXML
	private ComboBox<String> cbHttpSourceId;

	@FXML
	private ComboBox<String> cbRmqSourceId;

	@FXML
	private TextField tfHttpValue;

	@FXML
	private TextField tfRmqValue;

	@FXML
	private Button btHttpGetMaterials;

	@FXML
	private Button btHttpGetReasons;

	@FXML
	private Button btHttpGetEntities;

	// JSON parser
	private Gson gson = new Gson();

	public ClientTestApplication() {

	}

	public void start(Stage primaryStage) {
		try {
			primaryStage.setTitle("HTTP and Messaging Test Client");
			primaryStage.getIcons().add(ImageManager.instance().getImage(Images.POINT85));

			AnchorPane mainLayout = (AnchorPane) FXMLLoader.load(getClass().getResource("ClientTestApplication.fxml"));

			Scene scene = new Scene(mainLayout);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
			stop();
		}
	}

	private void setImages() throws Exception {
		// entity
		btHttpGetEntities.setGraphic(ImageManager.instance().getImageView(Images.EQUIPMENT));
		btHttpGetEntities.setContentDisplay(ContentDisplay.RIGHT);

		// materials
		btHttpGetMaterials.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
		btHttpGetMaterials.setContentDisplay(ContentDisplay.RIGHT);

		// reasons
		btHttpGetReasons.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		btHttpGetReasons.setContentDisplay(ContentDisplay.RIGHT);

		// post
		btHttpPost.setGraphic(ImageManager.instance().getImageView(Images.HTTP));
		btHttpPost.setContentDisplay(ContentDisplay.RIGHT);

		// send
		btRmqSend.setGraphic(ImageManager.instance().getImageView(Images.RMQ));
		btRmqSend.setContentDisplay(ContentDisplay.RIGHT);
	}

	// called by Java FX
	public void initialize() throws Exception {
		setImages();
		initializeEntityTable();
		initializeMaterialTable();
		initializeReasonTable();
		populateHttpSourceIds();
		populateRmqSourceIds();
	}

	public void stop() {
		// disconnect RMQ brokers
		try {
			for (Entry<String, PublisherSubscriber> entry : pubsubs.entrySet()) {
				entry.getValue().disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	private void initializeEntityTable() {

		// add the table view listener
		ttvEntities.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null) {
				return;
			}
			try {
				onSelectEntity(newValue.getValue());
			} catch (Exception e) {
				showErrorDialog(e);
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
				onSelectReason(newValue.getValue());
			} catch (Exception e) {
				showErrorDialog(e);
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
				onSelectMaterial(newValue);
			} catch (Exception e) {
				showErrorDialog(e);
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
		String hostPort = cbHttpHostPort.getSelectionModel().getSelectedItem();

		if (hostPort == null) {
			throw new Exception("An HTTP server must be selected");
		}

		String[] tokens = cbHttpHostPort.getSelectionModel().getSelectedItem().split(":");
		return "http://" + tokens[0] + ":" + tokens[1] + '/' + endpoint;
	}

	private String addQueryParameter(String url, String name, String value) {
		StringBuilder sb = new StringBuilder();

		if (!url.contains("?")) {
			sb.append('?');
		} else {
			sb.append('&');
		}
		sb.append(name).append('=').append(value);

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

	private void populateRmqSourceIds(PlantEntity entity) throws Exception {
		HttpURLConnection conn = null;
		try {
			// refresh list of RMQ source Ids
			String urlString = buildHttpUrl(OeeHttpServer.SOURCE_ID_EP);
			urlString = addQueryParameter(urlString, OeeHttpServer.EQUIP_ATTRIB, entity.getName());
			urlString = addQueryParameter(urlString, OeeHttpServer.DS_TYPE_ATTRIB, DataSourceType.MESSAGING.name());

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

			cbRmqSourceId.getSelectionModel().clearSelection();

			cbRmqSourceId.getItems().clear();
			cbRmqSourceId.getItems().addAll(sources);
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
			populateRmqSourceIds(entity);

		} catch (Exception e) {
			showErrorDialog(e);
		}
	}

	private void onSelectMaterial(Material material) {
		tfHttpValue.setText(material.getName());
		tfRmqValue.setText(material.getName());
	}

	private void onSelectReason(Reason reason) {
		tfHttpValue.setText(reason.getName());
		tfRmqValue.setText(reason.getName());
	}

	@FXML
	private void onHttpPostEvent() {
		HttpURLConnection conn = null;
		try {
			// POST event
			String urlString = buildHttpUrl(OeeHttpServer.EVENT_EP);
			URL url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			String sourceId = (String) cbHttpSourceId.getSelectionModel().getSelectedItem();
			String value = tfHttpValue.getText();

			OffsetDateTime odt = OffsetDateTime.now();
			String timestamp = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

			EquipmentEventRequestDto dto = new EquipmentEventRequestDto(sourceId, value, timestamp);
			Gson gson = new Gson();
			String payload = gson.toJson(dto);

			OutputStream os = conn.getOutputStream();
			os.write(payload.getBytes());
			os.flush();

			checkResponseCode(conn);

		} catch (Exception e) {
			showErrorDialog(e);
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
			String msg = "Failed : error code : " + conn.getResponseCode();
			msg += "\nError ...";

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
			showErrorDialog(e);
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
			showErrorDialog(e);
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
			showErrorDialog(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	// display an error dialog
	private void showErrorDialog(Exception e) {
		e.printStackTrace();
		String message = e.getMessage();

		if (message == null) {
			message = e.getClass().getSimpleName();
		}
		showAlert(AlertType.ERROR, "Application Error", "Exception", message);
	}

	// display a general alert
	private ButtonType showAlert(AlertType type, String title, String header, String errorMessage) {
		// Show the error message.
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(errorMessage);
		alert.setResizable(true);

		Optional<ButtonType> result = alert.showAndWait();

		ButtonType buttonType = null;
		try {
			buttonType = result.get();
		} catch (NoSuchElementException e) {

		}
		return buttonType;
	}

	@Override
	public void onMessage(Channel channel, Envelope envelope, ApplicationMessage message) {
		// execute on worker thread
		logger.info("Received message: " + message.toString());
	}

	@FXML
	private void onSendEquipmentEventMsg() {
		try {
			String hostPort = cbRmqHostPort.getSelectionModel().getSelectedItem();

			if (hostPort == null) {
				throw new Exception("A host and port must be specified");
			}

			PublisherSubscriber pubsub = pubsubs.get(hostPort);

			if (pubsub == null) {
				pubsub = new PublisherSubscriber();
				pubsubs.put(hostPort, pubsub);

				String[] tokens = hostPort.split(":");

				pubsub.connect(tokens[0], Integer.valueOf(tokens[1]));
			}

			String sourceId = (String) cbRmqSourceId.getSelectionModel().getSelectedItem();

			String value = tfRmqValue.getText();

			EquipmentEventMessage msg = new EquipmentEventMessage();
			msg.setSourceId(sourceId);
			msg.setValue(value);

			pubsub.publish(msg, RoutingKey.EQUIPMENT_SOURCE_EVENT);
		} catch (Exception e) {
			showErrorDialog(e);
		}
	}

	private void populateHttpSourceIds() {
		try {
			// query db
			List<CollectorDataSource> dataSources = PersistenceService.instance().fetchDataSources(DataSourceType.HTTP);

			cbHttpHostPort.getSelectionModel().clearSelection();
			ObservableList<String> items = cbHttpHostPort.getItems();
			items.clear();

			for (CollectorDataSource dataSource : dataSources) {
				// data sources
				String hostPort = dataSource.getHost() + ":" + dataSource.getPort();

				if (!items.contains(hostPort)) {
					items.add(hostPort);
				}
			}

			cbHttpSourceId.getSelectionModel().clearSelection();
			cbHttpSourceId.getItems().clear();
			tfHttpValue.clear();

			if (items.size() == 1) {
				cbHttpHostPort.getSelectionModel().select(0);
			}
		} catch (Exception e) {
			showErrorDialog(e);
		}
	}

	private void populateRmqSourceIds() {
		try {
			// query db
			List<CollectorDataSource> dataSources = PersistenceService.instance()
					.fetchDataSources(DataSourceType.MESSAGING);

			cbRmqHostPort.getSelectionModel().clearSelection();
			ObservableList<String> items = cbRmqHostPort.getItems();
			items.clear();

			for (CollectorDataSource dataSource : dataSources) {
				// data sources
				String hostPort = dataSource.getHost() + ":" + dataSource.getPort();

				if (!items.contains(hostPort)) {
					items.add(hostPort);
				}
			}

			cbRmqSourceId.getSelectionModel().clearSelection();
			cbRmqSourceId.getItems().clear();
			tfRmqValue.clear();

			if (items.size() == 1) {
				cbRmqHostPort.getSelectionModel().select(0);
			}
		} catch (Exception e) {
			showErrorDialog(e);
		}
	}
}
