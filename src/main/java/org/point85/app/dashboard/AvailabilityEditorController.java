package org.point85.app.dashboard;

import java.time.Duration;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.LoaderFactory;
import org.point85.app.reason.ReasonEditorController;
import org.point85.domain.collector.AvailabilityRecord;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Reason;

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

	private AvailabilityRecord availabilityEvent;

	// reason editor controller
	private ReasonEditorController reasonController;

	@FXML
	private Button btReasonEditor;

	@FXML
	private Label lbReason;

	@FXML
	private TextField tfDuration;

	public void initializeEditor(AvailabilityRecord event) throws Exception {
		availabilityEvent = event;

		// images for buttons
		setImages();

		getDialogStage().setOnShown((we) -> {
			setAttributes();
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
		// time period
		setTimePeriod(availabilityEvent);

		// reason
		if (availabilityEvent.getReason() == null) {
			throw new Exception("A reason must be specified.");
		}

		// duration
		Duration duration = AppUtils.durationFromString(tfDuration.getText());
		availabilityEvent.setDuration(duration);

		PersistenceService.instance().save(availabilityEvent);
	}

	private void showReason() {
		if (availabilityEvent.getReason() != null) {
			lbReason.setText(availabilityEvent.getReason().getDisplayString());
		} else {
			lbReason.setText(null);
		}
	}

	@FXML
	private void onShowReasonEditor() {
		try {
			// display the reason editor as a dialog
			if (reasonController == null) {
				FXMLLoader loader = LoaderFactory.reasonEditorLoader();
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
			showReason();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void setAttributes() {
		// reason
		showReason();

		// start date and time
		super.setAttributes(availabilityEvent);

		// duration
		if (availabilityEvent.getDuration() != null) {
			tfDuration.setText(AppUtils.stringFromDuration(availabilityEvent.getDuration()));
		}
	}
}
