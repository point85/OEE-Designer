package org.point85.app.dashboard;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.domain.DomainUtils;
import org.point85.domain.collector.OeeEvent;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.ShiftInstance;
import org.point85.domain.schedule.WorkSchedule;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

abstract class EventEditorController extends DialogController {
	@FXML
	private DatePicker dpStartDate;

	@FXML
	private TextField tfStartTime;

	@FXML
	private DatePicker dpEndDate;

	@FXML
	private TextField tfEndTime;

	protected abstract void saveRecord() throws Exception;

	protected void displayAttributes(OeeEvent event) {

		// start date and time
		if (event.getStartTime() != null) {
			dpStartDate.setValue(event.getStartTime().toLocalDate());
			int seconds = event.getStartTime().toLocalTime().toSecondOfDay();
			tfStartTime.setText(AppUtils.stringFromDuration(Duration.ofSeconds(seconds)));
		}

		// end date and time
		if (event.getEndTime() != null) {
			dpEndDate.setValue(event.getEndTime().toLocalDate());
			int seconds = event.getEndTime().toLocalTime().toSecondOfDay();
			tfEndTime.setText(AppUtils.stringFromDuration(Duration.ofSeconds(seconds)));
		}
	}

	protected void setTimePeriod(OeeEvent event) throws Exception {
		// time period
		LocalDate startDate = dpStartDate.getValue();

		if (startDate == null) {
			throw new Exception("The starting date must be specified");
		}

		Duration startSeconds = null;
		if (tfStartTime.getText() != null && tfStartTime.getText().trim().length() > 0) {
			startSeconds = AppUtils.durationFromString(tfStartTime.getText().trim());
		} else {
			startSeconds = Duration.ZERO;
		}
		LocalTime startTime = LocalTime.ofSecondOfDay(startSeconds.getSeconds());
		LocalDateTime ldtStart = LocalDateTime.of(startDate, startTime);

		LocalDateTime ldtEnd = null;

		if (dpEndDate.getValue() != null) {
			LocalDate endDate = dpEndDate.getValue();
			Duration endSeconds = null;
			if (tfEndTime.getText() != null && tfEndTime.getText().trim().length() > 0) {
				endSeconds = AppUtils.durationFromString(tfEndTime.getText().trim());
			} else {
				endSeconds = Duration.ofSeconds(86400);
			}
			LocalTime endTime = LocalTime.ofSecondOfDay(endSeconds.getSeconds());
			ldtEnd = LocalDateTime.of(endDate, endTime);
		}

		if (ldtEnd != null && ldtEnd.isBefore(ldtStart)) {
			throw new Exception("The starting time " + ldtStart + " must be before the ending time " + ldtEnd);
		}

		LocalDateTime now = LocalDateTime.now();
		if (ldtStart.isAfter(now)) {
			throw new Exception("The starting time " + ldtStart + " cannot be later than the current time ");
		}

		if (ldtEnd != null && ldtEnd.isAfter(now)) {
			throw new Exception("The ending time " + ldtEnd + " cannot be later than the current time ");
		}

		OffsetDateTime odtStart = DomainUtils.fromLocalDateTime(ldtStart);
		OffsetDateTime odtEnd = DomainUtils.fromLocalDateTime(ldtEnd);

		event.setStartTime(odtStart);
		event.setEndTime(odtEnd);

		// set shift
		Shift shift = null;
		WorkSchedule schedule = event.getEquipment().findWorkSchedule();

		if (schedule != null) {
			List<ShiftInstance> shiftInstances = schedule.getShiftInstancesForTime(ldtStart);

			if (shiftInstances.size() > 0) {
				// pick first one
				shift = shiftInstances.get(0).getShift();
			}
		}
		event.setShift(shift);
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
}
