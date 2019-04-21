package org.point85.app.messaging;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.jms.JMSClient;
import org.point85.domain.jms.JMSSource;
import org.point85.domain.messaging.MessagingClient;
import org.point85.domain.messaging.MessagingSource;
import org.point85.domain.mqtt.MQTTClient;
import org.point85.domain.mqtt.MQTTSource;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class MqBrokerController extends DesignerDialogController {
	// default ports
	private static final int RMQ_DEFAULT_PORT = 5672;
	private static final int AMQ_DEFAULT_PORT = 61616;
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
	protected void setImages() throws Exception {
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
			if (sourceType.equals(DataSourceType.MESSAGING)) {
				dataSource = new MessagingSource();
			} else if (sourceType.equals(DataSourceType.JMS)) {
				dataSource = new JMSSource();
			} else if (sourceType.equals(DataSourceType.MQTT)) {
				dataSource = new MQTTSource();
			}
		}
		return dataSource;
	}

	protected void setSource(CollectorDataSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onTest() {
		MessagingClient pubsub = null;
		JMSClient jmsClient = null;
		MQTTClient mqttClient = null;

		try {
			if (sourceType.equals(DataSourceType.MESSAGING)) {
				pubsub = new MessagingClient();
				pubsub.connect(getHost(), getPort(), getUserName(), getPassword());
			} else if (sourceType.equals(DataSourceType.JMS)) {
				jmsClient = new JMSClient();
				jmsClient.connect(getHost(), getPort(), getUserName(), getPassword());
			} else if (sourceType.equals(DataSourceType.MQTT)) {
				mqttClient = new MQTTClient();
				mqttClient.connect(getHost(), getPort(), getUserName(), getPassword());
			}

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));

		} catch (Exception e) {
			AppUtils.showErrorDialog(
					DesignerLocalizer.instance().getErrorString("connection.failed", DomainUtils.formatException(e)));
		} finally {
			if (sourceType.equals(DataSourceType.MESSAGING)) {
				if (pubsub != null) {
					try {
						pubsub.shutDown();
					} catch (Exception e) {
					}
				}
			} else if (sourceType.equals(DataSourceType.JMS)) {
				if (jmsClient != null) {
					try {
						jmsClient.shutDown();
					} catch (JMSException e) {
					}
				}
			} else if (sourceType.equals(DataSourceType.MQTT)) {
				try {
					mqttClient.shutDown();
				} catch (Exception e) {
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
				PersistenceService.instance().delete(dataSource);
				brokers.remove(dataSource);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewDataSource() {
		try {
			this.tfHost.clear();
			this.tfUserName.clear();
			this.pfPassword.clear();
			this.tfDescription.clear();

			// default the port
			if (sourceType != null) {
				int port = RMQ_DEFAULT_PORT;

				if (sourceType.equals(DataSourceType.JMS)) {
					port = AMQ_DEFAULT_PORT;
				} else if (sourceType.equals(DataSourceType.MQTT)) {
					port = MQTT_DEFAULT_PORT;
				}
				this.tfPort.setText(String.valueOf(port));
			} else {
				this.tfPort.clear();
			}

			this.cbDataSources.getSelectionModel().clearSelection();

			this.setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			CollectorDataSource dataSource = getSource();

			dataSource.setHost(getHost());
			dataSource.setUserName(getUserName());
			dataSource.setPassword(getPassword());
			dataSource.setPort(getPort());
			dataSource.setDescription(getDescription());

			// name is URL
			String name = getHost() + ":" + getPort();
			dataSource.setName(name);

			// save data source
			CollectorDataSource saved = (CollectorDataSource) PersistenceService.instance().save(dataSource);
			setSource(saved);

			// update list
			if (dataSource.getKey() == null) {
				// new source
				cbDataSources.getItems().add(dataSource);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources() {
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
