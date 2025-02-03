package org.point85.app.socket;

import java.io.File;
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
import org.point85.domain.exim.Exporter;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.socket.WebSocketOeeClient;
import org.point85.domain.socket.WebSocketSource;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class WebSocketServerController extends DesignerDialogController {

	// default port
	private static final int WS_TCP_PORT = 8887;

	// current source
	private WebSocketSource dataSource;

	// list of brokers and ports
	private final ObservableList<WebSocketSource> servers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfPort;

	@FXML
	private CheckBox ckClientAuth;

	@FXML
	private ComboBox<WebSocketSource> cbDataSources;

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
	private Button btRefresh;

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

		// clear SSL settings
		btClearSSL.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));

		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT);

		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
	}

	public WebSocketSource getSource() {
		// set source
		if (dataSource == null) {
			dataSource = new WebSocketSource();
		}
		return dataSource;
	}

	protected void setSource(WebSocketSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onTest() {
		WebSocketOeeClient wsOeeClient = null;

		try {
			wsOeeClient = new WebSocketOeeClient(dataSource);

			if (wsOeeClient.openConnection()) {
				wsOeeClient.closeConnection();

				AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));

			} else {
				throw new Exception(
						DesignerLocalizer.instance().getErrorString("ws.cannot.connect", getHost(), getPort()));
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(
					DesignerLocalizer.instance().getErrorString("connection.failed", DomainUtils.formatException(e)));
		} finally {
			if (wsOeeClient != null) {
				try {
					wsOeeClient.close();
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
				onNewDataSource();
				return;
			}

			this.tfHost.setText(dataSource.getHost());
			this.tfPort.setText(String.valueOf(dataSource.getPort()));
			this.tfDescription.setText(dataSource.getDescription());
			this.tfKeystore.setText(dataSource.getKeystore());
			this.pfKeystorePassword.setText(dataSource.getKeystorePassword());
			this.pfKeyPassword.setText(dataSource.getKeyPassword());
			this.ckClientAuth.setSelected(dataSource.isClientAuthorization());

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
				servers.remove(dataSource);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
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

		this.ckClientAuth.setSelected(false);

		this.cbDataSources.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewDataSource() {
		try {
			clearEditor();
			tfPort.setText(String.valueOf(WS_TCP_PORT));
			setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			WebSocketSource eventSource = getSource();

			eventSource.setHost(getHost());
			eventSource.setPort(getPort());
			eventSource.setDescription(getDescription());
			eventSource.setKeystore(getKeystore());
			eventSource.setKeystorePassword(getKeystorePassword());
			eventSource.setKeyPassword(getKeyPassword());
			eventSource.setClientAuthorization(getClientAuthorization());

			// name is URL
			String name = getHost() + ":" + getPort();
			eventSource.setName(name);

			// save data source
			WebSocketSource saved = (WebSocketSource) PersistenceService.instance().save(eventSource);
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
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.WEB_SOCKET);

		servers.clear();
		for (CollectorDataSource source : sources) {
			servers.add((WebSocketSource) source);
		}
		WebSocketSource one = servers.size() == 1 ? servers.get(0) : null;
		servers.add(null);
		cbDataSources.setItems(servers);

		if (one != null) {
			cbDataSources.getSelectionModel().select(one);
			onSelectDataSource();
		}
	}

	String getHost() {
		return this.tfHost.getText();
	}

	Integer getPort() {
		return Integer.valueOf(tfPort.getText());
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

	boolean getClientAuthorization() {
		return ckClientAuth.isSelected();
	}

	@FXML
	private void onRefresh() throws Exception {
		populateDataSources();
	}

	private void backupSources(List<CollectorDataSource> sources) {
		try {
			// show file chooser
			File file = getBackupFile();

			if (file != null) {
				// backup
				Exporter.instance().backupWebSocketSources(sources, file);

				AppUtils.showInfoDialog(
						DesignerLocalizer.instance().getLangString("backup.successful", file.getCanonicalPath()));
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onBackup() {
		if (getSource() == null || getSource().getName() == null) {
			// confirm all
			ButtonType type = AppUtils.showConfirmationDialog(
					DesignerLocalizer.instance().getLangString("all.export", WebSocketSource.class.getSimpleName()));

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			backupToFile(WebSocketSource.class);
		} else {
			// one source
			WebSocketSource source = getSource();

			List<CollectorDataSource> sources = new ArrayList<>();
			sources.add(source);

			// confirm
			ButtonType type = AppUtils.showConfirmationDialog(
					DesignerLocalizer.instance().getLangString("object.export", source.getName()));

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			backupSources(sources);
		}
	}
}
