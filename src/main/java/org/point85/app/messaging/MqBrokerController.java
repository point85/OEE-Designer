package org.point85.app.messaging;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.jms.JmsClient;
import org.point85.domain.jms.JmsSource;
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.rmq.RmqClient;
import org.point85.domain.rmq.RmqSource;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class MqBrokerController extends DesignerDialogController {
	// default ports
	private static final int RMQ_DEFAULT_PORT = 5672;
	private static final int JMS_DEFAULT_PORT = 61616;
	private static final int MQTT_DEFAULT_PORT = 1883;

	// current source
	private CollectorDataSource dataSource;

	// source type
	private DataSourceType sourceType;

	// list of brokers and ports
	private final ObservableList<CollectorDataSource> brokers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private TextField tfPort;

	@FXML
	private ComboBox<CollectorDataSource> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btTest;

	public void initialize(DesignerApplication app, DataSourceType type) throws Exception {
		// set source
		this.sourceType = type;

		// main app
		setApp(app);

		// button images
		setImages();

		// retrieve the defined data sources
		populateDataSources();
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
	}

	public CollectorDataSource getSource() {
		// set source
		if (dataSource == null) {
			if (sourceType.equals(DataSourceType.RMQ)) {
				dataSource = new RmqSource();
			} else if (sourceType.equals(DataSourceType.JMS)) {
				dataSource = new JmsSource();
			} else if (sourceType.equals(DataSourceType.MQTT)) {
				dataSource = new MqttSource();
			}
		}
		return dataSource;
	}

	protected void setSource(CollectorDataSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onTest() {
		RmqClient pubsub = null;
		JmsClient jmsClient = null;
		MqttOeeClient mqttClient = null;

		try {
			if (sourceType.equals(DataSourceType.RMQ)) {
				pubsub = new RmqClient();
				pubsub.connect(getHost(), getPort(), getUserName(), getPassword());
			} else if (sourceType.equals(DataSourceType.JMS)) {
				jmsClient = new JmsClient();
				jmsClient.connect(getHost(), getPort(), getUserName(), getPassword());
			} else if (sourceType.equals(DataSourceType.MQTT)) {
				mqttClient = new MqttOeeClient();
				mqttClient.connect(getHost(), getPort(), getUserName(), getPassword());
			}

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));

		} catch (Exception e) {
			AppUtils.showErrorDialog(
					DesignerLocalizer.instance().getErrorString("connection.failed", DomainUtils.formatException(e)));
		} finally {
			if (sourceType.equals(DataSourceType.RMQ)) {
				if (pubsub != null) {
					try {
						pubsub.disconnect();
					} catch (Exception e) {
						// ignore
					}
				}
			} else if (sourceType.equals(DataSourceType.JMS)) {
				if (jmsClient != null) {
					try {
						jmsClient.disconnect();
					} catch (Exception e) {
						// ignore
					}
				}
			} else if (sourceType.equals(DataSourceType.MQTT)) {
				try {
					mqttClient.disconnect();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}

	@FXML
	private void onSelectDataSource() {
		try {
			dataSource = cbDataSources.getSelectionModel().getSelectedItem();

			if (dataSource == null) {
				return;
			}

			this.tfHost.setText(dataSource.getHost());
			this.tfUserName.setText(dataSource.getUserName());
			this.tfPort.setText(String.valueOf(dataSource.getPort()));
			this.pfPassword.setText(dataSource.getUserPassword());
			this.tfDescription.setText(dataSource.getDescription());
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDeleteDataSource() {
		try {
			// delete
			if (dataSource != null) {
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(
						DesignerLocalizer.instance().getLangString("object.delete", dataSource.toString()));

				if (type.equals(ButtonType.CANCEL)) {
					return;
				}

				PersistenceService.instance().delete(dataSource);
				brokers.remove(dataSource);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void clearEditor() {
		this.tfHost.clear();
		this.tfUserName.clear();
		this.pfPassword.clear();
		this.tfDescription.clear();
		this.tfPort.clear();

		this.cbDataSources.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewDataSource() {
		try {
			clearEditor();

			// default the port
			if (sourceType != null) {
				int port = RMQ_DEFAULT_PORT;

				if (sourceType.equals(DataSourceType.JMS)) {
					port = JMS_DEFAULT_PORT;
				} else if (sourceType.equals(DataSourceType.MQTT)) {
					port = MQTT_DEFAULT_PORT;
				}
				this.tfPort.setText(String.valueOf(port));
			}

			setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private String validateHost(String hostId) throws Exception {
		// "localhost" allowed
		return hostId;
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			CollectorDataSource eventSource = getSource();

			eventSource.setHost(validateHost(getHost()));
			eventSource.setUserName(getUserName());
			eventSource.setPassword(getPassword());
			eventSource.setPort(getPort());
			eventSource.setDescription(getDescription());

			// name is URL
			String name = getHost() + ":" + getPort();
			eventSource.setName(name);

			// save data source
			CollectorDataSource saved = (CollectorDataSource) PersistenceService.instance().save(eventSource);
			setSource(saved);

			// update list
			if (eventSource.getKey() != null) {
				// updated
				cbDataSources.getItems().remove(eventSource);
			}
			cbDataSources.getItems().add(saved);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources() throws Exception {
		// fetch the server ids
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(sourceType);

		brokers.clear();
		for (CollectorDataSource source : sources) {
			brokers.add(source);
		}
		cbDataSources.setItems(brokers);

		if (brokers.size() == 1) {
			this.cbDataSources.getSelectionModel().select(0);
			onSelectDataSource();
		}
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

	Integer getPort() {
		return Integer.valueOf(tfPort.getText());
	}

	String getDescription() {
		return this.tfDescription.getText();
	}
}
