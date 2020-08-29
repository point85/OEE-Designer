package org.point85.app.designer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.domain.plant.EntitySchedule;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.schedule.WorkSchedule;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class EntityWorkScheduleController extends DesignerController {
	// effective work schedules
	private final ObservableList<EntitySchedule> entitySchedules = FXCollections.observableArrayList(new ArrayList<>());

	// entity schedule being edited
	private EntitySchedule currentEntitySchedule;

	// entity work schedule tab
	@FXML
	private Label lbSchedule;

	@FXML
	private Button btWorkSchedule;

	@FXML
	private DatePicker dpPeriodStartDate;

	@FXML
	private TextField tfPeriodStartTime;

	@FXML
	private DatePicker dpPeriodEndDate;

	@FXML
	private TextField tfPeriodEndTime;

	@FXML
	private Button btNewSchedule;

	@FXML
	private Button btAddSchedule;

	@FXML
	private Button btRemoveSchedule;

	@FXML
	private TableView<EntitySchedule> tvSchedules;

	@FXML
	private TableColumn<EntitySchedule, String> scheduleCol;

	@FXML
	private TableColumn<EntitySchedule, LocalDateTime> periodStartColumn;

	@FXML
	private TableColumn<EntitySchedule, LocalDateTime> periodEndColumn;

	void initialize(DesignerApplication app) {
		setApp(app);
		setImages();
		initializeScheduleTable();
	}

	private void initializeScheduleTable() {
		// bind to list of plant entity work schedules
		tvSchedules.setItems(entitySchedules);

		// add the listener for work schedule selection
		tvSchedules.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue != null) {
				try {
					onSelectEntitySchedule(newValue);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			} else {
				clearEditor();
			}
		});

		// schedule name
		scheduleCol.setCellValueFactory(cellDataFeatures -> {
			SimpleStringProperty property = null;
			if (cellDataFeatures.getValue() != null && cellDataFeatures.getValue().getWorkSchedule() != null) {
				property = new SimpleStringProperty(cellDataFeatures.getValue().getWorkSchedule().getName());
			}
			return property;
		});

		// period start
		periodStartColumn.setCellValueFactory(cellDataFeatures -> new SimpleObjectProperty<LocalDateTime>(
				cellDataFeatures.getValue().getStartDateTime()));

		// period end
		periodEndColumn.setCellValueFactory(cellDataFeatures -> new SimpleObjectProperty<LocalDateTime>(
				cellDataFeatures.getValue().getEndDateTime()));
	}

	void showSchedules(PlantEntity entity) {
		Set<EntitySchedule> schedules = entity.getSchedules();

		entitySchedules.clear();
		for (EntitySchedule schedule : schedules) {
			entitySchedules.add(schedule);
		}

		clearEditor();

		tvSchedules.refresh();
	}

	protected void setImages() {
		// new entity schedule
		btNewSchedule.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewSchedule.setContentDisplay(ContentDisplay.RIGHT);

		// add entity schedule
		btAddSchedule.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddSchedule.setContentDisplay(ContentDisplay.RIGHT);

		// remove entity schedule
		btRemoveSchedule.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveSchedule.setContentDisplay(ContentDisplay.RIGHT);

		// work schedule
		btWorkSchedule.setGraphic(ImageManager.instance().getImageView(Images.SCHEDULE));
		btWorkSchedule.setContentDisplay(ContentDisplay.LEFT);
	}

	// find work schedule
	@FXML
	private void onFindWorkSchedule() {
		try {
			// get the work schedule from the dialog
			WorkSchedule schedule = getApp().showScheduleEditor();

			if (schedule == null) {
				return;
			}

			// show schedule
			updateScheduleData(schedule);

			if (currentEntitySchedule == null) {
				currentEntitySchedule = new EntitySchedule();
			}

			currentEntitySchedule.setWorkSchedule(schedule);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void updateScheduleData(WorkSchedule schedule) {
		lbSchedule.setText(schedule.getName());
	}

	void clear() {
		clearEditor();
		entitySchedules.clear();
		currentEntitySchedule = null;
	}

	void clearEditor() {
		this.lbSchedule.setText(null);
		this.tfPeriodStartTime.setText(null);
		this.tfPeriodEndTime.setText(null);
		this.dpPeriodStartDate.setValue(null);
		this.dpPeriodEndDate.setValue(null);
		this.btAddSchedule.setText(DesignerLocalizer.instance().getLangString("add"));
		this.tvSchedules.getSelectionModel().clearSelection();
	}

	@FXML
	private void onNewSchedule() {
		try {
			clearEditor();

			// initial start at Jan. 1
			LocalDate today = LocalDate.now();

			dpPeriodStartDate.setValue(LocalDate.of(today.getYear(), 1, 1));
			tfPeriodStartTime.setText("00:00:00");

			// initial end at Dec. 31 before midnight
			dpPeriodEndDate.setValue(LocalDate.of(today.getYear(), 12, 31));
			tfPeriodEndTime.setText("23:59:59");

			btAddSchedule.setText(DesignerLocalizer.instance().getLangString("add"));

			currentEntitySchedule = null;

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onAddOrUpdateSchedule() {
		try {
			if (currentEntitySchedule == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("choose.schedule"));
			}

			// need a work schedule first
			if (currentEntitySchedule.getWorkSchedule() == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("choose.schedule"));
			}

			// start date
			LocalDate startDate = dpPeriodStartDate.getValue();

			// start time of day
			LocalDateTime startDateTime = null;

			if (tfPeriodStartTime.getText() != null) {
				String start = tfPeriodStartTime.getText().trim();
				LocalTime startTime = AppUtils.localTimeFromString(start);
				startDateTime = LocalDateTime.of(startDate, startTime);
			}

			// end date
			LocalDate endDate = dpPeriodEndDate.getValue();

			// end time of day
			LocalDateTime endDateTime = null;

			if (tfPeriodEndTime.getText() != null) {
				String end = tfPeriodEndTime.getText().trim();
				LocalTime endTime = AppUtils.localTimeFromString(end);
				endDateTime = LocalDateTime.of(endDate, endTime);
			}

			PlantEntity plantEntity = getApp().getPhysicalModelController().getSelectedEntity();
			currentEntitySchedule.setPlantEntity(plantEntity);
			currentEntitySchedule.setStartDateTime(startDateTime);
			currentEntitySchedule.setEndDateTime(endDateTime);

			Collections.sort(entitySchedules);

			if (!entitySchedules.contains(currentEntitySchedule)) {
				entitySchedules.add(currentEntitySchedule);
			}

			Set<EntitySchedule> schedules = new HashSet<>();
			schedules.addAll(entitySchedules);
			plantEntity.setSchedules(schedules);

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			tvSchedules.getSelectionModel().clearSelection();
			currentEntitySchedule = null;

			tvSchedules.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRemoveSchedule() {
		try {
			if (currentEntitySchedule == null) {
				AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.schedule.selected"));
				return;
			}

			PlantEntity entity = getApp().getPhysicalModelController().getSelectedEntity();
			entity.removeEntitySchedule(currentEntitySchedule);

			// mark dirty
			getApp().getPhysicalModelController().markSelectedPlantEntity();

			entitySchedules.remove(currentEntitySchedule);
			currentEntitySchedule = null;
			tvSchedules.getSelectionModel().clearSelection();
			tvSchedules.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void onSelectEntitySchedule(EntitySchedule schedule) {
		currentEntitySchedule = schedule;
		btAddSchedule.setText(DesignerLocalizer.instance().getLangString("update"));

		// name
		lbSchedule.setText(schedule.getWorkSchedule().getName());

		// start
		dpPeriodStartDate.setValue(schedule.getStartDateTime().toLocalDate());
		LocalTime startTime = schedule.getStartDateTime().toLocalTime();
		this.tfPeriodStartTime.setText(AppUtils.stringFromLocalTime(startTime, true));

		// end
		dpPeriodEndDate.setValue(schedule.getEndDateTime().toLocalDate());
		LocalTime endTime = schedule.getEndDateTime().toLocalTime();
		this.tfPeriodEndTime.setText(AppUtils.stringFromLocalTime(endTime, true));
	}
}
