package org.point85.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public abstract class DialogController {
	@FXML
	protected Button btOK;

	@FXML
	protected Button btCancel;

	// stage for the dialog
	@FXML
	private Stage dialogStage;

	public Stage getDialogStage() {
		return this.dialogStage;
	}

	// reference to the main app stage
	public void setDialogStage(Stage dialogStage) {
		this.dialogStage = dialogStage;
	}

	@FXML
	protected void onOK() {
		// close dialog
		this.dialogStage.close();
	}

	@FXML
	protected void onCancel() {
		// close dialog
		this.dialogStage.close();
	}

	// images for controls
	protected void setImages() {
		// OK
		btOK.setGraphic(new ImageView(Images.okImage));
		btOK.setContentDisplay(ContentDisplay.LEFT);

		// Cancel
		if (btCancel != null) {
			btCancel.setGraphic(new ImageView(Images.cancelImage));
			btCancel.setContentDisplay(ContentDisplay.LEFT);
		}
	}
}
