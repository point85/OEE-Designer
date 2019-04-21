package org.point85.app.db;

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
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class DatabaseServerController extends DesignerDialogController {
	// current source
	private DatabaseEventSource dataSource;

	// list of database server sources
	private final ObservableList<DatabaseEventSource> databaseServers = FXCollections
			.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private ComboBox<DatabaseEventSource> cbDataSources;

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

		// test
		btTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btTest.setContentDisplay(ContentDisplay.LEFT);
	}

	public DatabaseEventSource getSource() {
		if (dataSource == null) {
			dataSource = new DatabaseEventSource();
		}
		return dataSource;
	}

	protected void setSource(DatabaseEventSource source) {
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
			this.tfUserName.setText(dataSource.getUserName());
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
				databaseServers.remove(dataSource);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewDataSource() {
		try {
			this.tfHost.setText(PersistenceService.getJdbcConnection());
			this.tfUserName.setText(PersistenceService.getUserName());
			this.pfPassword.setText(PersistenceService.getUserPassword());
			this.tfDescription.clear();
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
			DatabaseEventSource eventSource = getSource();

			eventSource.setHost(getHost());
			eventSource.setUserName(getUserName());
			eventSource.setPassword(getPassword());
			eventSource.setDescription(getDescription());

			// name is JDBC connection string
			eventSource.setName(getHost());

			// save data source
			DatabaseEventSource saved = (DatabaseEventSource) PersistenceService.instance().save(eventSource);
			setSource(saved);

			// update list
			if (eventSource.getKey() == null) {
				// new source
				cbDataSources.getItems().add(dataSource);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onTest() {
		PersistenceService persistenceService = PersistenceService.create();
		try {
			persistenceService.connectToDatabaseEventServer(getHost(), getUserName(), getPassword());

			AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("connection.successful"));

		} catch (Exception e) {
			AppUtils.showErrorDialog(
					DesignerLocalizer.instance().getErrorString("connection.failed", DomainUtils.formatException(e)));
		} finally {
			if (persistenceService != null) {
				persistenceService.close();
			}
		}
	}

	private void populateDataSources() {
		// fetch the server ids
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.DATABASE);

		databaseServers.clear();
		for (CollectorDataSource source : sources) {
			databaseServers.add((DatabaseEventSource) source);
		}
		cbDataSources.setItems(databaseServers);

		if (databaseServers.size() == 1) {
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

	String getDescription() {
		return this.tfDescription.getText();
	}
}
