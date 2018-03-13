package org.point85.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
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
	protected void setImages() throws Exception {
		// OK
		btOK.setGraphic(ImageManager.instance().getImageView(Images.OK));
		btOK.setContentDisplay(ContentDisplay.LEFT);

		// Cancel
		if (btCancel != null) {
			btCancel.setGraphic(ImageManager.instance().getImageView(Images.CANCEL));
			btCancel.setContentDisplay(ContentDisplay.LEFT);
		} else {
			// just one button
			btOK.setText("Done");
		}
	}
}
