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
import org.point85.domain.mqtt.MqttOeeClient;
import org.point85.domain.mqtt.MqttSource;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class MqttServerController extends DesignerDialogController {
	// default port
	private static final int MQTT_TCP_PORT = 1883;

	// current source
	private MqttSource dataSource;

	// list of brokers and ports
	private final ObservableList<MqttSource> brokers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfPort;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfUserPassword;

	@FXML
	private ComboBox<MqttSource> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private TextField tfKeystore;

	@FXML
	private PasswordField pfKeystorePassword;

	@FXML
	private PasswordField pfKeyPassword;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btTest;

	@FXML
	private Button btBackup;

	@FXML
	private Button btClearAuthentication;

	@FXML
	private Button btClearSSL;

	public void initialize(DesignerApplication app) throws Exception {
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

		// clear authentication settings
		btClearAuthentication.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));

		// clear SSL settings
		btClearSSL.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));
		
		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT); 
	}

	public MqttSource getSource() {
		// set source
		if (dataSource == null) {
			dataSource = new MqttSource();
		}
		return dataSource;
	}

	protected void setSource(MqttSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onTest() {
		MqttOeeClient mqttOeeClient = null;

		try {
			mqttOeeClient = new MqttOeeClient();

			if (tfUserName.getText() != null) {
				mqttOeeClient.setAuthenticationConfiguration(tfUserName.getText(), pfUserPassword.getText());
			}

			if (tfKeystore.getText() != null) {
				mqttOeeClient.setSSLConfiguration(tfKeystore.getText(), pfKeystorePassword.getText(),
						pfKeyPassword.getText());
			}

			mqttOeeClient.connect(getHost(), getPort());

			mqttOeeClient.disconnect();

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));

		} catch (Exception e) {
			AppUtils.showErrorDialog(
					DesignerLocalizer.instance().getErrorString("connection.failed", DomainUtils.formatException(e)));
		} finally {
			if (mqttOeeClient != null) {
				try {
					mqttOeeClient.disconnect();
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
			this.tfPort.setText(String.valueOf(dataSource.getPort()));
			this.tfUserName.setText(dataSource.getUserName());
			this.pfUserPassword.setText(dataSource.getUserPassword());
			this.tfDescription.setText(dataSource.getDescription());
			this.tfKeystore.setText(dataSource.getKeystore());
			this.pfKeystorePassword.setText(dataSource.getKeystorePassword());
			this.pfKeyPassword.setText(dataSource.getKeyPassword());

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

	@FXML
	private void onClearAuthentication() {
		this.tfUserName.clear();
		this.pfUserPassword.clear();
	}

	@FXML
	private void onClearSSL() {
		this.tfKeystore.clear();
		this.pfKeystorePassword.clear();
		this.pfKeyPassword.clear();
	}

	private void clearEditor() {
		this.tfHost.clear();
		this.tfPort.clear();
		this.tfDescription.clear();

		onClearSSL();
		onClearAuthentication();

		this.cbDataSources.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewDataSource() {
		try {
			clearEditor();
			tfPort.setText(String.valueOf(MQTT_TCP_PORT));
			setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			MqttSource eventSource = getSource();

			eventSource.setHost(getHost());
			eventSource.setPort(getPort());
			eventSource.setUserName(getUserName());
			eventSource.setPassword(getPassword());
			eventSource.setDescription(getDescription());
			eventSource.setKeystore(getKeystore());
			eventSource.setKeystorePassword(getKeystorePassword());
			eventSource.setKeyPassword(getKeyPassword());

			// name is URL
			String name = getHost() + ":" + getPort();
			eventSource.setName(name);

			// save data source
			MqttSource saved = (MqttSource) PersistenceService.instance().save(eventSource);
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
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.MQTT);

		brokers.clear();
		for (CollectorDataSource source : sources) {
			brokers.add((MqttSource) source);
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

	Integer getPort() {
		return Integer.valueOf(tfPort.getText());
	}

	String getUserName() {
		return this.tfUserName.getText();
	}

	String getPassword() {
		return this.pfUserPassword.getText();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	String getKeystore() {
		return this.tfKeystore.getText();
	}

	String getKeystorePassword() {
		return this.pfKeystorePassword.getText();
	}

	String getKeyPassword() {
		return this.pfKeyPassword.getText();
	}

	@FXML
	private void onBackup() {
		backupToFile(MqttSource.class);
	}
}
