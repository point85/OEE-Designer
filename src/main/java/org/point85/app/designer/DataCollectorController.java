package org.point85.app.designer;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.jms.JmsClient;
import org.point85.domain.kafka.KafkaOeeClient;
import org.point85.domain.kafka.KafkaSource;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.rmq.RmqClient;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;

public class DataCollectorController extends DialogController {
	// the collector
	private DataCollector dataCollector;

	// list of collector names
	private final ObservableList<String> collectors = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfName;

	@FXML
	private TextField tfDescription;

	@FXML
	private TextField tfHost;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btTest;
	
	@FXML
	private Button btClearMessagingServer;

	@FXML
	private ComboBox<String> cbCollectors;

	@FXML
	private ComboBox<CollectorState> cbCollectorStates;

	@FXML
	private ComboBox<CollectorDataSource> cbNotificationServers;

	public void initialize(DesignerApplication app) throws Exception {
		// button images
		setImages();

		// possible states
		for (CollectorState state : CollectorState.values()) {
			cbCollectorStates.getItems().add(state);
		}

		cbCollectors.setItems(collectors);

		// messaging notification servers
		List<DataSourceType> messagingTypes = new ArrayList<>();
		messagingTypes.add(DataSourceType.RMQ);
		messagingTypes.add(DataSourceType.JMS);
		messagingTypes.add(DataSourceType.MQTT);
		messagingTypes.add(DataSourceType.KAFKA);

		List<CollectorDataSource> messagingServers = PersistenceService.instance().fetchDataSources(messagingTypes);

		for (CollectorDataSource server : messagingServers) {
			cbNotificationServers.getItems().add(server);
		}

		// retrieve the defined collectors
		populateCollectors();

		if (collectors.size() == 1) {
			cbCollectors.getSelectionModel().select(0);
			onSelectCollector();
		}
	}

	// images for buttons
	@Override
	protected void setImages() {
		super.setImages();

		// new
		btNew.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNew.setContentDisplay(ContentDisplay.LEFT);

		// save
		btSave.setGraphic(ImageManager.instance().getImageView(Images.SAVE));
		btSave.setContentDisplay(ContentDisplay.LEFT);

		// delete
		btDelete.setGraphic(ImageManager.instance().getImageView(Images.DELETE));
		btDelete.setContentDisplay(ContentDisplay.LEFT);

		// test
		btTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btTest.setContentDisplay(ContentDisplay.LEFT);
		
		// clear
		btClearMessagingServer.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));
	}

	public DataCollector getCollector() {
		if (dataCollector == null) {
			dataCollector = new DataCollector();
		}
		return dataCollector;
	}

	protected void setCollector(DataCollector definition) {
		this.dataCollector = definition;
	}

	@FXML
	private void onSelectCollector() {
		try {
			String collectorName = cbCollectors.getSelectionModel().getSelectedItem();

			// JFX fires this event with a null selected item!
			if (collectorName == null) {
				onNewCollector();
				return;
			}

			// retrieve collector by name
			DataCollector collector = PersistenceService.instance().fetchCollectorByName(collectorName);
			setCollector(collector);

			this.tfHost.setText(collector.getHost());
			this.tfName.setText(collector.getName());
			this.tfDescription.setText(collector.getDescription());

			// notification servers
			CollectorDataSource messagingSource = collector.getNotificationServer();

			if (messagingSource != null) {
				cbNotificationServers.getSelectionModel().select(messagingSource);
			}

			// collector state
			cbCollectorStates.getSelectionModel().select(collector.getCollectorState());
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDeleteCollector() {
		try {
			// delete
			DataCollector definition = getCollector();

			if (definition != null) {
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(
						DesignerLocalizer.instance().getLangString("object.delete", definition.toString()));

				if (type.equals(ButtonType.CANCEL)) {
					return;
				}

				PersistenceService.instance().delete(definition);
				collectors.remove(definition.getName());

				onNewCollector();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewCollector() {
		try {
			this.tfHost.clear();
			this.tfName.clear();
			this.tfDescription.clear();
			this.cbNotificationServers.getSelectionModel().select(null);

			this.cbCollectors.getSelectionModel().clearSelection();
			this.cbCollectorStates.getSelectionModel().select(null);
			this.cbNotificationServers.getSelectionModel().clearSelection();

			this.setCollector(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private String validateHost(String hostId) {
		// allow "localhost"
		return hostId;
	}

	@FXML
	private void onSaveCollector() {
		// set attributes
		try {
			DataCollector collector = getCollector();

			CollectorDataSource notificationServer = cbNotificationServers.getSelectionModel().getSelectedItem();
			collector.setNotificationServer(notificationServer);

			collector.setHost(validateHost(getHost()));
			collector.setName(getName());
			collector.setDescription(getDescription());
			collector.setCollectorState(getCollectorState());

			// save collector
			DataCollector saved = (DataCollector) PersistenceService.instance().save(collector);
			setCollector(saved);

			// update list
			populateCollectors();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateCollectors() throws Exception {
		// fetch the collectors
		List<DataCollector> definitions = PersistenceService.instance().fetchAllDataCollectors();

		collectors.clear();
		for (DataCollector definition : definitions) {
			collectors.add(definition.getName());
		}
	}

	String getHost() {
		return this.tfHost.getText();
	}

	String getName() {
		return this.tfName.getText();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	CollectorState getCollectorState() {
		return this.cbCollectorStates.getSelectionModel().getSelectedItem();
	}

	@FXML
	private void onTest() {		
		String content = "This is a test.";

		CollectorDataSource notificationServer = cbNotificationServers.getSelectionModel().getSelectedItem();

		if (notificationServer == null) {
			return;
		}

		try {
			if (notificationServer.getDataSourceType().equals(DataSourceType.RMQ)) {
				RmqClient rmqClient = new RmqClient();
				rmqClient.connect(notificationServer.getHost(), notificationServer.getPort(),
						notificationServer.getUserName(), notificationServer.getUserPassword());
				rmqClient.sendNotification(content, NotificationSeverity.INFO);
				
			} else if (notificationServer.getDataSourceType().equals(DataSourceType.JMS)) {
				JmsClient jmsClient = new JmsClient();
				jmsClient.connect(notificationServer.getHost(), notificationServer.getPort(),
						notificationServer.getUserName(), notificationServer.getUserPassword());
				jmsClient.sendNotification(content, NotificationSeverity.INFO);
				
			} else if (notificationServer.getDataSourceType().equals(DataSourceType.MQTT)) {
				MqttOeeClient mqttClient = new MqttOeeClient();
				mqttClient.connect(notificationServer.getHost(), notificationServer.getPort(),
						notificationServer.getUserName(), notificationServer.getUserPassword());
				mqttClient.sendNotification(content, NotificationSeverity.INFO);
				
			} else if (notificationServer.getDataSourceType().equals(DataSourceType.KAFKA)) {
				KafkaSource server = (KafkaSource) notificationServer;
				KafkaOeeClient kafkaClient = new KafkaOeeClient();
				kafkaClient.createProducer(server, KafkaOeeClient.NOTIFICATION_TOPIC);
				kafkaClient.sendNotification(content, NotificationSeverity.INFO);
				
			}
			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
	
	@FXML
	private void onClearMessagingServer() {
		cbNotificationServers.getSelectionModel().select(null);
		cbNotificationServers.getSelectionModel().clearSelection();
	}
}
