package org.point85.app.db;

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
import org.point85.domain.db.DatabaseEventSource;
import org.point85.domain.exim.Exporter;
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

	@FXML
	private Button btBackup;

	@FXML
	private Button btRefresh;

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

		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT);

		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
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
				onNewDataSource();
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
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(
						DesignerLocalizer.instance().getLangString("object.delete", dataSource.toString()));

				if (type.equals(ButtonType.CANCEL)) {
					return;
				}

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
			this.tfHost.setText(PersistenceService.instance().getJdbcConnection());
			this.tfUserName.setText(PersistenceService.instance().getUserName());
			this.pfPassword.setText(PersistenceService.instance().getUserPassword());
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

	private void populateDataSources() throws Exception {
		// fetch the server ids
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.DATABASE);

		databaseServers.clear();
		for (CollectorDataSource source : sources) {
			databaseServers.add((DatabaseEventSource) source);
		}
		DatabaseEventSource one = databaseServers.size() == 1 ? databaseServers.get(0) : null;
		databaseServers.add(null);
		cbDataSources.setItems(databaseServers);

		if (one != null) {
			cbDataSources.getSelectionModel().select(one);
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
				Exporter.instance().backupDatabaseSources(sources, file);

				AppUtils.showInfoDialog(
						DesignerLocalizer.instance().getLangString("backup.successful", file.getCanonicalPath()));
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onBackup() {
		if (dataSource == null) {
			// confirm all
			ButtonType type = AppUtils.showConfirmationDialog(DesignerLocalizer.instance().getLangString("all.export",
					DatabaseEventSource.class.getSimpleName()));

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			backupToFile(DatabaseEventSource.class);
		} else {
			// one source
			DatabaseEventSource source = getSource();

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
