package org.point85.app.http;

import java.net.HttpURLConnection;
import java.net.URL;
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
import org.point85.domain.http.HttpSource;
import org.point85.domain.http.OeeHttpServer;
import org.point85.domain.persistence.PersistenceService;

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

public class HttpServerController extends DesignerDialogController {
	// current source
	private HttpSource dataSource;

	// list of servers and ports
	private final ObservableList<HttpSource> servers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfPort;

	@FXML
	private TextField tfHttpsPort;

	@FXML
	private CheckBox ckStandalone;

	@FXML
	private ComboBox<HttpSource> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private TextField tfOAuthClientId;

	@FXML
	private TextField tfOAuthClientSecret;

	@FXML
	private TextField tfOAuthUserName;

	@FXML
	private PasswordField pfOAuthPassword;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btHttpTest;

	@FXML
	private Button btBackup;

	public void initializeServer(DesignerApplication app) throws Exception {
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
		btHttpTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btHttpTest.setContentDisplay(ContentDisplay.LEFT);

		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT);
	}

	public HttpSource getSource() {
		if (dataSource == null) {
			dataSource = new HttpSource();
		}
		return dataSource;
	}

	protected void setSource(HttpSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onSelectDataSource() {
		try {
			dataSource = cbDataSources.getSelectionModel().getSelectedItem();

			if (dataSource == null) {
				return;
			}

			this.tfHost.setText(dataSource.getHost());

			String port = dataSource.getPort() != null ? String.valueOf(dataSource.getPort()) : "";
			this.tfPort.setText(String.valueOf(port));

			String httpsPort = dataSource.getHttpsPort() != null ? String.valueOf(dataSource.getHttpsPort()) : "";
			this.tfHttpsPort.setText(httpsPort);
			this.tfDescription.setText(dataSource.getDescription());
			this.ckStandalone.setSelected(dataSource.isStandalone());

			this.tfOAuthClientId.setText(dataSource.getClientId());
			this.tfOAuthClientSecret.setText(dataSource.getClientSecret());
			this.tfOAuthUserName.setText(dataSource.getUserName());
			this.pfOAuthPassword.setText(dataSource.getUserPassword());

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
	private void onNewDataSource() {
		try {
			this.tfHost.clear();
			this.tfDescription.clear();
			this.tfPort.setText(String.valueOf(OeeHttpServer.DEFAULT_PORT));
			this.tfHttpsPort.clear();
			this.cbDataSources.getSelectionModel().clearSelection();
			this.tfOAuthClientId.clear();
			this.tfOAuthClientSecret.clear();
			this.tfOAuthUserName.clear();
			this.pfOAuthPassword.clear();
			this.ckStandalone.setSelected(false);

			this.setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onTestUrl() {
		HttpURLConnection conn = null;
		try {
			String urlString = "http://" + tfHost.getText() + ":" + tfPort.getText() + '/' + OeeHttpServer.EVENT_EP;
			URL url = new URL(urlString);

			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setConnectTimeout(2000);
			conn.getOutputStream();

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));

		} catch (Exception e) {
			AppUtils.showErrorDialog(
					DesignerLocalizer.instance().getErrorString("connection.failed", DomainUtils.formatException(e)));
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			HttpSource eventSource = getSource();

			eventSource.setHost(getHost());
			eventSource.setPort(getPort());
			eventSource.setHttpsPort(getHttpsPort());
			eventSource.setDescription(getDescription());
			eventSource.setStandalone(isStandalone());

			// OAuth
			eventSource.setClientId(getOAuthClientId());
			eventSource.setClientSecret(getOAuthClientSecret());
			eventSource.setUserName(getOAuthUserName());
			eventSource.setPassword(getOAuthPassword());

			// name is URL
			String name = getHost() + ":" + getPort();
			eventSource.setName(name);

			// save data source
			HttpSource saved = (HttpSource) PersistenceService.instance().save(eventSource);
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
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.HTTP);

		servers.clear();
		for (CollectorDataSource source : sources) {
			servers.add((HttpSource) source);
		}
		cbDataSources.setItems(servers);

		if (servers.size() == 1) {
			this.cbDataSources.getSelectionModel().select(0);
			onSelectDataSource();
		}
	}

	private String getHost() {
		return this.tfHost.getText();
	}

	private Integer getPort() {
		String text = tfPort.getText();
		return (text != null && text.length() > 0) ? Integer.parseInt(text) : null;
	}

	private Integer getHttpsPort() {
		String text = tfHttpsPort.getText();
		return (text != null && text.length() > 0) ? Integer.parseInt(text) : null;
	}

	private boolean isStandalone() {
		return ckStandalone.isSelected();
	}

	private String getDescription() {
		return this.tfDescription.getText();
	}

	private String getOAuthClientId() {
		return this.tfOAuthClientId.getText();
	}

	private String getOAuthClientSecret() {
		return this.tfOAuthClientSecret.getText();
	}

	private String getOAuthUserName() {
		return this.tfOAuthUserName.getText();
	}

	private String getOAuthPassword() {
		return this.pfOAuthPassword.getText();
	}

	@FXML
	private void onBackup() {
		backupToFile(HttpSource.class);
	}
}
