package org.point85.app.dashboard;

import java.time.Duration;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.reason.ReasonEditorController;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.Reason;
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

public class AvailabilityEditorController extends EventEditorController {

	private OeeEvent availabilityEvent;

	// reason editor controller
	private ReasonEditorController reasonController;

	@FXML
	private Button btReasonEditor;

	@FXML
	private Label lbReason;

	@FXML
	private TextField tfDuration;

	public void initializeEditor(OeeEvent event) throws Exception {
		availabilityEvent = event;

		reasonController = null;

		lbReason.setText(null);
		tfDuration.clear();
		tfDuration.setDisable(true);

		// images for buttons
		setImages();

		getDialogStage().setOnShown((we) -> {
			displayAttributes();
		});
	}

	@Override
	protected void setImages() throws Exception {
		super.setImages();

		btReasonEditor.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		btReasonEditor.setTooltip(new Tooltip("Find reason."));
	}

	@Override
	protected void saveRecord() throws Exception {
		// duration
		if (tfDuration.getText() != null && tfDuration.getText().length() > 0) {
			Duration duration = AppUtils.durationFromString(tfDuration.getText());
			availabilityEvent.setDuration(duration);
		}

		// time period
		setTimePeriod(availabilityEvent);

		// reason
		if (availabilityEvent.getReason() == null) {
			throw new Exception("A reason must be specified.");
		}

		// material
		Equipment equipment = availabilityEvent.getEquipment();
		OeeEvent lastSetup = PersistenceService.instance().fetchLastEvent(equipment, OeeEventType.MATL_CHANGE);

		Material material = null;
		if (lastSetup == null) {
			material = equipment.getDefaultEquipmentMaterial().getMaterial();
		} else {
			material = lastSetup.getMaterial();
		}

		if (material == null) {
			throw new Exception("No material found for equipment " + equipment.getName());
		}

		availabilityEvent.setMaterial(material);

		PersistenceService.instance().save(availabilityEvent);
	}

	private void displayReason() {
		if (availabilityEvent.getReason() != null) {
			lbReason.setText(availabilityEvent.getReason().getDisplayString());
			tfDuration.setDisable(false);
		} else {
			lbReason.setText(null);
			tfDuration.setDisable(true);
		}
	}

	@FXML
	private void onShowReasonEditor() {
		try {
			// display the reason editor as a dialog
			if (reasonController == null) {
				FXMLLoader loader = FXMLLoaderFactory.reasonEditorLoader();
				AnchorPane page = (AnchorPane) loader.getRoot();

				// Create the dialog Stage.
				Stage dialogStage = new Stage(StageStyle.DECORATED);
				dialogStage.setTitle("Availability Reason");
				dialogStage.initModality(Modality.APPLICATION_MODAL);
				Scene scene = new Scene(page);
				dialogStage.setScene(scene);

				// get the controller
				reasonController = loader.getController();
				reasonController.setDialogStage(dialogStage);
				reasonController.initialize(null);
			}

			// Show the dialog and wait until the user closes it
			reasonController.getDialogStage().showAndWait();

			Reason reason = reasonController.getSelectedReason();
			availabilityEvent.setReason(reason);
			availabilityEvent.setInputValue(reason.getName());
			displayReason();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void displayAttributes() {
		// start date and time
		super.displayAttributes(availabilityEvent);

		// reason
		displayReason();

		// duration
		if (availabilityEvent.getDuration() != null) {
			tfDuration.setText(AppUtils.stringFromDuration(availabilityEvent.getDuration(), true));
		}
	}
}
