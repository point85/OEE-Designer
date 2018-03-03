package org.point85.app.designer;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorState;
import org.point85.domain.collector.DataCollector;
import org.point85.domain.persistence.PersistencyService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class DataCollectorController extends DialogController {
	private DataCollector collectorConfiguration;

	// list of collector names
	private ObservableList<String> collectors = FXCollections.observableArrayList(new ArrayList<>());

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
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private ComboBox<String> cbCollectors;

	@FXML
	private ComboBox<CollectorState> cbCollectorStates;

	public void initialize(DesignerApplication app) throws Exception {
		// main app
		//setApp(app);

		// button images
		setImages();

		// possible states
		for (CollectorState state : CollectorState.values()) {
			cbCollectorStates.getItems().add(state);
		}

		// retrieve the defined collectors
		populateCollectors();
	}

	// images for buttons
	@Override
	protected void setImages() throws Exception {
		super.setImages();

		// new
		btNew.setGraphic(new ImageView(Images.newImage));
		btNew.setContentDisplay(ContentDisplay.LEFT);

		// save
		btSave.setGraphic(new ImageView(Images.saveImage));
		btSave.setContentDisplay(ContentDisplay.LEFT);

		// delete
		btDelete.setGraphic(new ImageView(Images.deleteImage));
		btDelete.setContentDisplay(ContentDisplay.LEFT);
	}

	public DataCollector getCollectorDefinition() {
		if (collectorConfiguration == null) {
			collectorConfiguration = new DataCollector();
		}
		return collectorConfiguration;
	}

	protected void setCollectorDefinition(DataCollector definition) {
		this.collectorConfiguration = definition;
	}

	@FXML
	private void onSelectCollector() {
		String collector = cbCollectors.getSelectionModel().getSelectedItem();

		// retrieve collector by name
		DataCollector configuration = null;
		try {
			configuration = (DataCollector) PersistencyService.instance().fetchByName(DataCollector.COLLECT_BY_NAME,
					collector);
			setCollectorDefinition(configuration);
		} catch (Exception e) {
			// not saved yet
			return;
		}

		if (configuration != null) {
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

			this.cbCollectorStates.getSelectionModel().select(configuration.getCollectorState());
		}
	}

	@FXML
	private void onDeleteCollector() {
		try {
			// delete
			DataCollector definition = getCollectorDefinition();

			if (definition != null) {
				PersistencyService.instance().delete(definition);
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
			this.tfBrokerPort.clear();

			this.cbCollectors.getSelectionModel().clearSelection();
			this.cbCollectorStates.getSelectionModel().select(CollectorState.DEV);

			this.setCollectorDefinition(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveCollector() {
		// set attributes
		try {
			DataCollector configuration = getCollectorDefinition();

			configuration.setHost(getHost());
			configuration.setName(getName());
			configuration.setDescription(getDescription());
			configuration.setBrokerHost(getBrokerHost());
			configuration.setBrokerPort(getBrokerPort());
			configuration.setCollectorState(getCollectorState());

			// save collector
			DataCollector saved = (DataCollector) PersistencyService.instance().save(configuration);
			setCollectorDefinition(saved);

			// update list
			populateCollectors();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateCollectors() {
		// fetch the collectors
		List<DataCollector> definitions = PersistencyService.instance().fetchAllDataCollectors();

		collectors.clear();
		for (DataCollector definition : definitions) {
			collectors.add(definition.getName());
		}
		cbCollectors.setItems(collectors);

		if (collectors.size() == 1) {
			this.cbCollectors.getSelectionModel().select(0);
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

		Integer port = null;
		if (portText != null && portText.trim().length() > 0) {
			port = Integer.valueOf(portText.trim());
		}
		return port;
	}

	CollectorState getCollectorState() {
		return this.cbCollectorStates.getSelectionModel().getSelectedItem();
	}

}
