package org.point85.app.generic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.collector.CollectorDataSource;
import org.point85.domain.collector.DataSourceType;
import org.point85.domain.exim.Exporter;
import org.point85.domain.generic.GenericSource;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.script.EventResolver;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class GenericSourceController extends DesignerDialogController {
	// current source
	private GenericSource dataSource;

	// list of sources
	private final ObservableList<GenericSource> genericSources = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TextField tfName;

	@FXML
	private TextField tfHost;

	@FXML
	private TextField tfPort;

	@FXML
	private TextField tfUserName;

	@FXML
	private PasswordField pfPassword;

	@FXML
	private TextField tfDescription;

	@FXML
	private TextField tfAttribute1;

	@FXML
	private TextField tfAttribute2;

	@FXML
	private TextField tfAttribute3;

	@FXML
	private TextField tfAttribute4;

	@FXML
	private TextField tfAttribute5;

	@FXML
	private ComboBox<GenericSource> cbDataSources;

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

	public GenericSource getSource() {
		// set source
		if (dataSource == null) {
			dataSource = new GenericSource();
		}
		return dataSource;
	}

	protected void setSource(GenericSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onTest() {
		try {
			EventResolver eventResolver = new EventResolver();
			getApp().showScriptEditor(eventResolver);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
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

			this.tfName.setText(dataSource.getName());
			this.tfUserName.setText(dataSource.getUserName());
			if (getPort() != null) {
				this.tfPort.setText(String.valueOf(dataSource.getPort()));
			}
			this.pfPassword.setText(dataSource.getUserPassword());
			this.tfDescription.setText(dataSource.getDescription());
			this.tfAttribute1.setText(dataSource.getAttribute1());
			this.tfAttribute2.setText(dataSource.getAttribute2());
			this.tfAttribute3.setText(dataSource.getAttribute3());
			this.tfAttribute4.setText(dataSource.getAttribute4());
			this.tfAttribute5.setText(dataSource.getAttribute5());
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
				genericSources.remove(dataSource);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void clearEditor() {
		this.tfName.clear();
		this.tfHost.clear();
		this.tfPort.clear();
		this.tfUserName.clear();
		this.pfPassword.clear();
		this.tfDescription.clear();
		this.tfAttribute1.clear();
		this.tfAttribute2.clear();
		this.tfAttribute3.clear();
		this.tfAttribute4.clear();
		this.tfAttribute5.clear();
		this.cbDataSources.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewDataSource() {
		try {
			clearEditor();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			GenericSource eventSource = getSource();

			if (getName() == null || getName().isEmpty()) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.data.source.name"));
			}

			eventSource.setName(getName());
			eventSource.setHost(getHost());
			eventSource.setPort(getPort());
			eventSource.setUserName(getUserName());
			eventSource.setPassword(getPassword());
			eventSource.setDescription(getDescription());
			eventSource.setAttribute1(getAttribute1());
			eventSource.setAttribute2(getAttribute2());
			eventSource.setAttribute3(getAttribute3());
			eventSource.setAttribute4(getAttribute4());
			eventSource.setAttribute5(getAttribute5());

			// save data source
			GenericSource saved = (GenericSource) PersistenceService.instance().save(eventSource);
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
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.GENERIC);
		Collections.sort(sources);

		genericSources.clear();
		for (CollectorDataSource source : sources) {
			genericSources.add((GenericSource) source);
		}
		CollectorDataSource one = genericSources.size() == 1 ? genericSources.get(0) : null;
		genericSources.add(null);
		cbDataSources.setItems(genericSources);

		if (one != null) {
			cbDataSources.getSelectionModel().select((GenericSource) one);
			onSelectDataSource();
		}
	}

	String getHost() {
		return this.tfHost.getText();
	}

	String getName() {
		return this.tfName.getText();
	}

	String getUserName() {
		return this.tfUserName.getText();
	}

	String getPassword() {
		return this.pfPassword.getText();
	}

	Integer getPort() {
		Integer port = null;
		if (tfPort.getText() != null && !tfPort.getText().trim().isEmpty()) {
			port = Integer.valueOf(tfPort.getText());
		}
		return port;
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	String getAttribute1() {
		return this.tfAttribute1.getText();
	}

	String getAttribute2() {
		return this.tfAttribute2.getText();
	}

	String getAttribute3() {
		return this.tfAttribute3.getText();
	}

	String getAttribute4() {
		return this.tfAttribute4.getText();
	}

	String getAttribute5() {
		return this.tfAttribute5.getText();
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
				Exporter.instance().backupGenericSources(sources, file);
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
			ButtonType type = AppUtils.showConfirmationDialog(
					DesignerLocalizer.instance().getLangString("all.export", GenericSource.class.getSimpleName()));

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			backupToFile(GenericSource.class);
		} else {
			// one source
			GenericSource source = getSource();

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
