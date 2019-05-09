package org.point85.app.designer;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.messaging.MessagingClient;
import org.point85.domain.messaging.NotificationSeverity;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class DataCollectorController extends DialogController {
	// default ports
	private static final int RMQ_DEFAULT_PORT = 5672;

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
	private TextField tfBrokerHost;

	@FXML
	private TextField tfBrokerPort;

	@FXML
	private TextField tfBrokerUserName;

	@FXML
	private PasswordField pfBrokerUserPassword;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btTest;

	@FXML
	private ComboBox<String> cbCollectors;

	@FXML
	private ComboBox<CollectorState> cbCollectorStates;

	public void initialize(DesignerApplication app) throws Exception {
		// button images
		setImages();

		// possible states
		for (CollectorState state : CollectorState.values()) {
			cbCollectorStates.getItems().add(state);
		}

		cbCollectors.setItems(collectors);

		// retrieve the defined collectors
		populateCollectors();
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
			String collector = cbCollectors.getSelectionModel().getSelectedItem();

			// JFX fires this event with a null selected item!
			if (collector == null) {
				onNewCollector();
				return;
			}

			// retrieve collector by name
			DataCollector configuration = PersistenceService.instance().fetchCollectorByName(collector);
			setCollector(configuration);

			this.tfHost.setText(configuration.getHost());
			this.tfName.setText(configuration.getName());
			this.tfDescription.setText(configuration.getDescription());

			if (configuration.getBrokerHost() != null) {
				this.tfBrokerHost.setText(configuration.getBrokerHost());
			} else {
				this.tfBrokerHost.clear();
			}

			if (configuration.getBrokerPort() != null) {
				this.tfBrokerPort.setText(String.valueOf(configuration.getBrokerPort()));
			} else {
				this.tfBrokerPort.clear();
			}

			if (configuration.getBrokerUserName() != null) {
				this.tfBrokerUserName.setText(configuration.getBrokerUserName());
			} else {
				this.tfBrokerUserName.clear();
			}

			if (configuration.getBrokerUserPassword() != null) {
				this.pfBrokerUserPassword.setText(configuration.getBrokerUserPassword());
			} else {
				this.pfBrokerUserPassword.clear();
			}

			this.cbCollectorStates.getSelectionModel().select(configuration.getCollectorState());
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
			this.tfBrokerHost.clear();
			this.tfBrokerPort.setText(String.valueOf(RMQ_DEFAULT_PORT));
			this.tfBrokerUserName.clear();
			this.pfBrokerUserPassword.clear();

			this.cbCollectors.getSelectionModel().clearSelection();
			this.cbCollectorStates.getSelectionModel().select(null);

			this.setCollector(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveCollector() {
		// set attributes
		try {
			DataCollector configuration = getCollector();

			configuration.setHost(getHost());
			configuration.setName(getName());
			configuration.setDescription(getDescription());
			configuration.setBrokerHost(getBrokerHost());
			configuration.setBrokerPort(getBrokerPort());
			configuration.setBrokerUserName(getBrokerUserName());
			configuration.setBrokerUserPassword(getBrokerUserPassword());
			configuration.setCollectorState(getCollectorState());

			// save collector
			DataCollector saved = (DataCollector) PersistenceService.instance().save(configuration);
			setCollector(saved);

			// update list
			populateCollectors();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateCollectors() {
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

	String getBrokerHost() {
		return this.tfBrokerHost.getText();
	}

	Integer getBrokerPort() {
		String portText = tfBrokerPort.getText();

		Integer port = 0;
		if (portText != null && portText.trim().length() > 0) {
			port = Integer.valueOf(portText.trim());
		}
		return port;
	}

	String getBrokerUserName() {
		return this.tfBrokerUserName.getText();
	}

	String getBrokerUserPassword() {
		return this.pfBrokerUserPassword.getText();
	}

	CollectorState getCollectorState() {
		return this.cbCollectorStates.getSelectionModel().getSelectedItem();
	}

	@FXML
	private void onTest() {
		try {
			MessagingClient pubsub = new MessagingClient();
			pubsub.connect(getBrokerHost(), getBrokerPort(), getBrokerUserName(), getBrokerUserPassword());
			pubsub.sendNotification("This is a test.", NotificationSeverity.INFO);

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

}
