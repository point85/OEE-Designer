package org.point85.app.cron;

import java.io.File;
import java.time.LocalDateTime;
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
import org.point85.domain.cron.CronEventClient;
import org.point85.domain.cron.CronEventSource;
import org.point85.domain.exim.Exporter;
import org.point85.domain.persistence.PersistenceService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;

public class CronEditorController extends DesignerDialogController {
	// current source
	private CronEventSource dataSource;

	// list of cron sources
	private final ObservableList<CronEventSource> cronSources = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private ComboBox<CronEventSource> cbCronSources;

	@FXML
	private TextField tfJobName;

	@FXML
	private TextField tfDescription;

	@FXML
	private TextField tfExpression;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btDelete;

	@FXML
	private Button btCronHelp;

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

		// help
		btCronHelp.setGraphic(ImageManager.instance().getImageView(Images.HELP));
		btCronHelp.setContentDisplay(ContentDisplay.LEFT);

		// test
		btTest.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btTest.setContentDisplay(ContentDisplay.LEFT);

		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.LEFT);

		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
	}

	public CronEventSource getSource() {
		if (dataSource == null) {
			dataSource = new CronEventSource();
		}
		return dataSource;
	}

	protected void setSource(CronEventSource source) {
		this.dataSource = source;
	}

	@FXML
	private void onSelectDataSource() {
		try {
			dataSource = cbCronSources.getSelectionModel().getSelectedItem();

			if (dataSource == null) {
				onNewDataSource();
				return;
			}

			this.tfJobName.setText(dataSource.getName());
			this.tfDescription.setText(dataSource.getDescription());
			this.tfExpression.setText(dataSource.getCronExpression());
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
				cronSources.remove(dataSource);

				onNewDataSource();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onNewDataSource() {
		try {
			this.tfJobName.clear();
			this.tfDescription.clear();
			this.tfExpression.clear();
			this.cbCronSources.getSelectionModel().clearSelection();

			this.setSource(null);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveDataSource() {
		// set attributes
		try {
			CronEventSource eventSource = getSource();

			eventSource.setName(getJobName());
			eventSource.setDescription(getDescription());
			eventSource.setCronExpression(getExpression());

			// save data source
			CronEventSource saved = (CronEventSource) PersistenceService.instance().save(eventSource);
			setSource(saved);

			// update list
			if (eventSource.getKey() == null) {
				// new source
				cbCronSources.getItems().add(dataSource);
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onShowHelp() {
		try {
			this.getApp().showCronHelp();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onTest() {
		try {
			LocalDateTime ldt = CronEventClient.getFirstFireTime(tfExpression.getText());

			AppUtils.showConfirmationDialog(
					DesignerLocalizer.instance().getLangString("cron.first.firetime", ldt.toString()));
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void populateDataSources() throws Exception {
		// fetch the cron ids
		List<CollectorDataSource> sources = PersistenceService.instance().fetchDataSources(DataSourceType.CRON);

		cronSources.clear();
		for (CollectorDataSource source : sources) {
			cronSources.add((CronEventSource) source);
		}
		CronEventSource one = cronSources.size() == 1 ? cronSources.get(0) : null;
		cronSources.add(null);
		cbCronSources.setItems(cronSources);

		if (one != null) {
			cbCronSources.getSelectionModel().select(one);
			onSelectDataSource();
		}
	}

	String getJobName() {
		return this.tfJobName.getText();
	}

	String getDescription() {
		return this.tfDescription.getText();
	}

	String getExpression() {
		return this.tfExpression.getText();
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
				Exporter.instance().backupCronSources(sources, file);

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
					DesignerLocalizer.instance().getLangString("all.export", CronEventSource.class.getSimpleName()));

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			backupToFile(CronEventSource.class);
		} else {
			// one source
			CronEventSource source = getSource();

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
