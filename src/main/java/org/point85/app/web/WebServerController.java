package org.point85.app.web;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.domain.persistence.PersistencyService;
import org.point85.domain.web.WebSource;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class WebServerController extends DialogController {
	// current source
	private WebSource dataSource;

	// list of servers
	private ObservableList<WebSource> servers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private TextField tfPort;

	@FXML
	private ComboBox<WebSource> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	public void initialize(DesignerApplication app) throws Exception {
		// main app
		// setApp(app);

		// button images
		setImages();

		// retrieve the defined data sources
		populateDataSources();
	}

	private void populateDataSources() {
		// fetch the server ids
		List<WebSource> sources = PersistencyService.instance().fetchWebSources();

		setDataSources(sources);
	}

	// images for buttons
	@Override
	protected void setImages() {
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

	public WebSource getSource() {
		return dataSource;
	}

	@FXML
	private void onDeleteDataSource() {
		try {
			// delete
			if (dataSource != null) {
				PersistencyService.instance().delete(dataSource);
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

			this.dataSource = null;
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			if (dataSource == null) {
				dataSource = new WebSource();
			}

			dataSource.setHost(getHost());
			dataSource.setUserName(getUserName());
			dataSource.setPassword(getPassword());
			dataSource.setPort(getPort());
			dataSource.setDescription(getDescription());

			// name is URL
			String name = getHost() + ":" + getPort();
			dataSource.setName(name);

			// save data source
			PersistencyService.instance().save(dataSource);

			// update list
			if (dataSource.getKey() == null) {
				// new source
				cbDataSources.getItems().add(dataSource);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
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

	/*
	 * private String getDataSourceId() { WebSource selectedSource =
	 * cbDataSources.getSelectionModel().getSelectedItem();
	 * 
	 * String id = null; if (selectedSource != null) { id = selectedSource.getId();
	 * } return id; }
	 */

	String getDescription() {
		return this.tfDescription.getText();
	}

	private void setDataSources(List<WebSource> sources) {

		servers.clear();
		for (WebSource source : sources) {
			servers.add(source);
		}
		cbDataSources.setItems(servers);

		if (servers.size() == 1) {
			this.cbDataSources.getSelectionModel().select(0);
			onSelectDataSource();
		}
	}

	@FXML
	private void onSelectDataSource() {
		/*
		 * String sourceId = getDataSourceId(); if (sourceId == null ||
		 * sourceId.length() == 0) { return; }
		 * 
		 * // retrieve data source by name try { dataSource = (WebSource)
		 * PersistencyService.getInstance().fetchByName(WebSource.WEB_SRC_BY_NAME,
		 * sourceId); } catch (Exception e) { // not saved yet return; }
		 */
		dataSource = this.cbDataSources.getSelectionModel().getSelectedItem();

		this.tfHost.setText(dataSource.getHost());
		this.tfUserName.setText(dataSource.getUserName());
		this.tfPort.setText(String.valueOf(dataSource.getPort()));
		this.pfPassword.setText(dataSource.getPassword());
		this.tfDescription.setText(dataSource.getDescription());
	}
}
