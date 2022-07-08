package org.point85.app.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.email.EmailSource;
import org.point85.domain.file.FileEventSource;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

public class FileShareController extends DesignerDialogController {
	// current source
	private FileEventSource dataSource;

	// list of file share sources
	private final ObservableList<FileEventSource> fileServers = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfHost;

	@FXML
	private ComboBox<FileEventSource> cbDataSources;

	@FXML
	private TextField tfDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btFileChooser;

	@FXML
	private Button btBackup;

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

		// choose file
		btFileChooser.setGraphic(ImageManager.instance().getImageView(Images.CHOOSE_FILE));
		btFileChooser.setContentDisplay(ContentDisplay.LEFT);
		
		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT); 
	}

	public FileEventSource getSource() {
		if (dataSource == null) {
			dataSource = new FileEventSource();
		}
		return dataSource;
	}

	protected void setSource(FileEventSource source) {
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
				fileServers.remove(dataSource);

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
			FileEventSource eventSource = getSource();

			eventSource.setHost(getHost());
			eventSource.setDescription(getDescription());

			// name is file share path
			eventSource.setName(getHost());

			// save data source
			FileEventSource saved = (FileEventSource) PersistenceService.instance().save(eventSource);
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
	private void onChooseFile() {
		try {
			// show file chooser
			DirectoryChooser directoryChooser = new DirectoryChooser();

			// Set title for DirectoryChooser
			directoryChooser.setTitle(DesignerLocalizer.instance().getLangString("select.dir"));

			// Set Initial Directory
			directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));

			File dir = directoryChooser.showDialog(null);

			if (dir == null) {
				return;
			}

			tfHost.setText(dir.getCanonicalPath());

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources() throws Exception {
		// fetch the file share ids
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.FILE);

		fileServers.clear();
		for (CollectorDataSource source : sources) {
			fileServers.add((FileEventSource) source);
		}
		cbDataSources.setItems(fileServers);

		if (fileServers.size() == 1) {
			this.cbDataSources.getSelectionModel().select(0);
			onSelectDataSource();
		}
	}

	String getHost() {
		return this.tfHost.getText();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	@FXML
	private void onBackup() {
		backupToFile(EmailSource.class);
	}
}
