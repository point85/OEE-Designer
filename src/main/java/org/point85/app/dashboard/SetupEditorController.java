package org.point85.app.dashboard;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.app.material.MaterialEditorController;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.KeyedObject;
import org.point85.domain.plant.Material;
import org.point85.domain.script.OeeEventType;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SetupEditorController extends EventEditorController {
	private OeeEvent setupEvent;

	// material editor
	private MaterialEditorController materialController;

	@FXML
	private Button btMaterialEditor;

	@FXML
	private Label lbMaterial;

	@FXML
	private TextField tfJob;

	public void initializeEditor(OeeEvent event) throws Exception {
		setupEvent = event;
		setupEvent.setEventType(OeeEventType.MATL_CHANGE);

		// images for buttons
		setImages();

		getDialogStage().setOnShown(we -> displayAttributes());
	}

	@Override
	protected void setImages() {
		super.setImages();

		btMaterialEditor.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
		btMaterialEditor.setTooltip(new Tooltip(DesignerLocalizer.instance().getLangString("material.tt")));
	}

	@Override
	protected void saveRecord() throws Exception {
		// time period
		setTimePeriod(setupEvent);

		// material
		if (setupEvent.getMaterial() == null) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("material.not.specified"));
		}

		// job
		setupEvent.setJob(tfJob.getText());

		// close off last setup
		List<KeyedObject> records = new ArrayList<>();
		records.add(setupEvent);

		// close off last setup
		OeeEvent lastRecord = PersistenceService.instance().fetchLastEvent(setupEvent.getEquipment(),
				OeeEventType.MATL_CHANGE);

		if (lastRecord != null && !(lastRecord.getKey().equals(setupEvent.getKey()))) {
			OffsetDateTime newStart = setupEvent.getStartTime();
			OffsetDateTime lastStart = lastRecord.getStartTime();

			if (newStart.isBefore(lastStart)) {
				throw new Exception(
						DesignerLocalizer.instance().getErrorString("start.before.end", newStart, lastStart));
			}

			lastRecord.setEndTime(newStart);
			Duration duration = Duration.between(lastRecord.getStartTime(), lastRecord.getEndTime());
			lastRecord.setDuration(duration);

			records.add(lastRecord);
		}

		// save records
		PersistenceService.instance().save(records);
	}

	private void displayMaterial() {
		if (setupEvent.getMaterial() != null) {
			lbMaterial.setText(setupEvent.getMaterial().getDisplayString());
		} else {
			lbMaterial.setText(null);
		}
	}

	@FXML
	private void onShowMaterialEditor() {
		try {
			// display the material editor as a dialog
			if (materialController == null) {
				FXMLLoader loader = FXMLLoaderFactory.materialEditorLoader();
				AnchorPane page = (AnchorPane) loader.getRoot();

				// Create the dialog Stage.
				Stage dialogStage = new Stage(StageStyle.DECORATED);
				dialogStage.setTitle(DesignerLocalizer.instance().getLangString("material.editor"));
				dialogStage.initModality(Modality.APPLICATION_MODAL);
				Scene scene = new Scene(page);
				dialogStage.setScene(scene);

				// get the controller
				materialController = loader.getController();
				materialController.setDialogStage(dialogStage);
				materialController.initialize(null);
			}

			// Show the dialog and wait until the user closes it
			materialController.getDialogStage().showAndWait();

			Material material = materialController.getSelectedMaterial();

			if (material != null) {
				setupEvent.setMaterial(material);
				setupEvent.setInputValue(material.getName());
				displayMaterial();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void displayAttributes() {
		// start date and time
		super.displayAttributes(setupEvent);

		// material
		displayMaterial();

		// job
		tfJob.setText(setupEvent.getJob());
	}
}
