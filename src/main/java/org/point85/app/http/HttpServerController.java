package org.point85.app.http;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.http.HttpSource;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class HttpServerController extends DialogController {
	// current source
	private HttpSource dataSource;

	// list of servers and ports
	private final ObservableList<HttpSource> servers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private TextField tfPort;

	@FXML
	private ComboBox<HttpSource> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	public void initializeServer() throws Exception {

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
			this.tfUserName.clear();
			this.pfPassword.clear();
			this.tfDescription.clear();
			this.tfPort.clear();
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
			HttpSource dataSource = getSource();

			dataSource.setHost(getHost());
			dataSource.setUserName(getUserName());
			dataSource.setPassword(getPassword());
			dataSource.setPort(getPort());
			dataSource.setDescription(getDescription());

			// name is URL
			String name = getHost() + ":" + getPort();
			dataSource.setName(name);

			// save data source
			PersistenceService.instance().save(dataSource);

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
