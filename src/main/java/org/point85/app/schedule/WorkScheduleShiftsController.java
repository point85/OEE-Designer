package org.point85.app.schedule;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class WorkScheduleShiftsController extends DesignerDialogController {

	// list of shift instances associated with the period of time
	private ObservableList<ShiftInstance> shiftInstanceList = FXCollections.observableArrayList(new ArrayList<>());

	private static final String MIDNIGHT = "00:00";

	// current work schedule
	private WorkSchedule currentSchedule;

	@FXML
	private DatePicker dpPeriodStart;

	@FXML
	private DatePicker dpPeriodEnd;

	@FXML
	private TextField tfStartTime;

	@FXML
	private TextField tfEndTime;

	@FXML
	private TextField tfWorkingTime;

	@FXML
	private TextField tfNonWorkingTime;

	@FXML
	private Button btShowShifts;

	@FXML
	private TableView<ShiftInstance> tvShiftInstances;

	@FXML
	private TableColumn<ShiftInstance, LocalDate> dayColumn;

	@FXML
	private TableColumn<ShiftInstance, String> shiftNameColumn;

	@FXML
	private TableColumn<ShiftInstance, String> teamNameColumn;

	@FXML
	private TableColumn<ShiftInstance, LocalTime> startTimeColumn;

	@FXML
	private TableColumn<ShiftInstance, LocalTime> endTimeColumn;

	@FXML
	private TableColumn<ShiftInstance, String> durationColumn;

	// initialize app
	public void initializeApp(DesignerApplication app) throws Exception {
		setApp(app);

		setImages();

		initializeShiftInstances();
	}

	void setCurrentSchedule(WorkSchedule schedule) {
		this.currentSchedule = schedule;

		clearShiftInstances();
	}

	// images for editor buttons
	@Override
	protected void setImages() throws Exception {
		super.setImages();

		btShowShifts.setGraphic(ImageManager.instance().getImageView(Images.SHIFT));
		btShowShifts.setContentDisplay(ContentDisplay.LEFT);
	}

	private void clearShiftInstances() {

		this.dpPeriodStart.setValue(null);
		this.tfStartTime.setText(MIDNIGHT);
		this.dpPeriodEnd.setValue(null);
		this.tfEndTime.setText(MIDNIGHT);

		this.tfWorkingTime.clear();
		this.tfNonWorkingTime.clear();
		this.shiftInstanceList.clear();
	}

	private void initializeShiftInstances() {
		// bind to list of shifts
		tvShiftInstances.setItems(shiftInstanceList);

		// instance day
		dayColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<LocalDate>(cellDataFeatures.getValue().getStartTime().toLocalDate());
		});

		// team name
		teamNameColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getTeam().getName());
		});

		// shift name
		shiftNameColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getShift().getName());
		});

		// starting time
		startTimeColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<LocalTime>(cellDataFeatures.getValue().getShift().getStart());
		});

		// ending time
		endTimeColumn.setCellValueFactory(cellDataFeatures -> {
			LocalTime end = null;
			try {
				end = cellDataFeatures.getValue().getShift().getEnd();
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
			return new SimpleObjectProperty<LocalTime>(end);
		});

		// duration
		durationColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(
					AppUtils.stringFromDuration(cellDataFeatures.getValue().getShift().getDuration(), false));
		});
	}

	@FXML
	private void onShowShifts() {
		try {
			this.shiftInstanceList.clear();
			this.tfWorkingTime.clear();
			this.tfNonWorkingTime.clear();

			// period start
			LocalDate startDate = this.dpPeriodStart.getValue();

			if (startDate == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("choose.start"));
			}
			String hrsMins = this.tfStartTime.getText().trim();
			LocalTime startTime = AppUtils.localTimeFromString(hrsMins);
			LocalDateTime from = LocalDateTime.of(startDate, startTime);

			// period end. If null then shift instances for the start date/time will be
			// shown
			LocalDate endDate = this.dpPeriodEnd.getValue();
			if (endDate != null) {
				hrsMins = this.tfEndTime.getText().trim();
				LocalTime endTime = AppUtils.localTimeFromString(hrsMins);
				LocalDateTime to = LocalDateTime.of(endDate, endTime);

				// working time
				Duration working = currentSchedule.calculateWorkingTime(from, to);
				this.tfWorkingTime.setText(AppUtils.stringFromDuration(working, false));

				// non working time
				Duration nonWorking = currentSchedule.calculateNonWorkingTime(from, to);
				this.tfNonWorkingTime.setText(AppUtils.stringFromDuration(nonWorking, false));

				// show shift instances
				long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;

				LocalDate day = startDate;

				for (long i = 0; i < days; i++) {
					List<ShiftInstance> instances = currentSchedule.getShiftInstancesForDay(day);

					for (ShiftInstance instance : instances) {
						this.shiftInstanceList.add(instance);
					}
					day = day.plusDays(1);
				}
			} else {
				List<ShiftInstance> instances = currentSchedule.getShiftInstancesForTime(from);
				
				for (ShiftInstance instance : instances) {
					this.shiftInstanceList.add(instance);
				}
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
