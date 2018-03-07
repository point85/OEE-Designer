package org.point85.app.messaging;

import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.messaging.MessagingSource;
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
	// current source
	private MessagingSource dataSource;

	// list of brokers and ports
	private ObservableList<MessagingSource> brokers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private TextField tfPort;

	@FXML
	private ComboBox<MessagingSource> cbDataSources;

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
	}

	public MessagingSource getSource() {
		if (dataSource == null) {
			dataSource = new MessagingSource();
		}
		return dataSource;
	}

	protected void setSource(MessagingSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onSelectDataSource() {
		/*
		 * String sourceId = getDataSourceId(); if (sourceId == null ||
		 * sourceId.length() == 0) { return; }
		 * 
		 * // retrieve data source by name MessagingSource source = null; try { source =
		 * (MessagingSource)
		 * PersistencyService.getInstance().fetchByName(MessagingSource.MSG_SRC_BY_NAME,
		 * sourceId); setSource(source); } catch (Exception e) { // not saved yet
		 * return; }
		 */

		dataSource = cbDataSources.getSelectionModel().getSelectedItem();

		this.tfHost.setText(dataSource.getHost());
		this.tfUserName.setText(dataSource.getUserName());
		this.tfPort.setText(String.valueOf(dataSource.getPort()));
		this.pfPassword.setText(dataSource.getPassword());
		this.tfDescription.setText(dataSource.getDescription());
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
			MessagingSource dataSource = getSource();

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
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.MESSAGING);

		brokers.clear();
		for (CollectorDataSource source : sources) {
			brokers.add((MessagingSource)source);
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
