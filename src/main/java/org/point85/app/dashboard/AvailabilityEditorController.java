package org.point85.app.dashboard;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.LoaderFactory;
import org.point85.app.reason.ReasonEditorController;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.AvailabilityRecord;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Reason;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;
import org.point85.domain.script.ResolvedEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AvailabilityEditorController extends DialogController {

	private ResolvedEvent resolvedEvent;

	// reason editor controller
	private ReasonEditorController reasonController;

	@FXML
	private Button btReasonEditor;

	@FXML
	private Label lbReason;

	@FXML
	private DatePicker dpStartDate;

	@FXML
	private TextField tfStartTime;

	@FXML
	private DatePicker dpEndDate;

	@FXML
	private TextField tfEndTime;

	@FXML
	private TextField tfDuration;

	// @FXML
	public void initializeEditor(ResolvedEvent event) throws Exception {
		resolvedEvent = event;

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

	@FXML
	protected void onOK() {
		try {
			// save data
			saveRecord();
			super.onOK();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void saveRecord() throws Exception {
		// time period
		LocalDate startDate = dpStartDate.getValue();
		Duration startSeconds = AppUtils.durationFromString(tfStartTime.getText());
		LocalTime startTime = LocalTime.ofSecondOfDay(startSeconds.getSeconds());
		LocalDateTime ldtStart = LocalDateTime.of(startDate, startTime);

		LocalDate endDate = dpEndDate.getValue();
		Duration endSeconds = AppUtils.durationFromString(tfEndTime.getText());
		LocalTime endTime = LocalTime.ofSecondOfDay(endSeconds.getSeconds());
		LocalDateTime ldtEnd = LocalDateTime.of(endDate, endTime);

		if (ldtEnd.isBefore(ldtStart)) {
			throw new Exception("The starting time " + ldtStart + " must be before the ending time " + ldtEnd);
		}

		OffsetDateTime odtStart = DomainUtils.fromLocalDateTime(ldtStart);
		OffsetDateTime odtEnd = DomainUtils.fromLocalDateTime(ldtEnd);
		resolvedEvent.setStartTime(odtStart);
		resolvedEvent.setEndTime(odtEnd);

		// set shift
		Shift shift = null;
		WorkSchedule schedule = resolvedEvent.getEquipment().findWorkSchedule();

		if (schedule != null) {
			List<ShiftInstance> shiftInstances = schedule.getShiftInstancesForTime(ldtStart);

			if (shiftInstances.size() > 0) {
				// pick first one
				shift = shiftInstances.get(0).getShift();
			}
		}
		resolvedEvent.setShift(shift);

		// reason
		if (resolvedEvent.getReason() == null) {
			throw new Exception("A reason must be specified.");
		}

		// duration
		Duration duration = AppUtils.durationFromString(tfDuration.getText());
		resolvedEvent.setDuration(duration);

		AvailabilityRecord record = new AvailabilityRecord(resolvedEvent);

		PersistenceService.instance().save(record);

	}

	private void showReason() {
		if (resolvedEvent.getReason() != null) {
			lbReason.setText(resolvedEvent.getReason().getDisplayString());
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
			resolvedEvent.setReason(reason);
			showReason();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	void setAttributes() {
		// reason
		showReason();

		// start date and time
		if (resolvedEvent.getStartTime() != null) {
			dpStartDate.setValue(resolvedEvent.getStartTime().toLocalDate());
			int seconds = resolvedEvent.getStartTime().toLocalTime().toSecondOfDay();
			tfStartTime.setText(AppUtils.stringFromDuration(Duration.ofSeconds(seconds)));
		}

		// end date and time
		if (resolvedEvent.getEndTime() != null) {
			dpEndDate.setValue(resolvedEvent.getEndTime().toLocalDate());
			int seconds = resolvedEvent.getEndTime().toLocalTime().toSecondOfDay();
			tfEndTime.setText(AppUtils.stringFromDuration(Duration.ofSeconds(seconds)));
		}

		// duration
		if (resolvedEvent.getDuration() != null) {
			tfDuration.setText(AppUtils.stringFromDuration(resolvedEvent.getDuration()));
		}
	}

	public ResolvedEvent getResolvedEvent() {
		return resolvedEvent;
	}

	public void setResolvedEvent(ResolvedEvent resolvedEvent) {
		this.resolvedEvent = resolvedEvent;
	}

}
