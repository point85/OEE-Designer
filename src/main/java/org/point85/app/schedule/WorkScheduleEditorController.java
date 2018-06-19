package org.point85.app.schedule;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.FXMLLoaderFactory;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.schedule.NonWorkingPeriod;
import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.RotationSegment;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.Team;
import org.point85.domain.schedule.WorkSchedule;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WorkScheduleEditorController extends DesignerDialogController {
	private static final String ROOT_SCHEDULE_NAME = "All Schedules";

	// schedule being edited or viewed
	private TreeItem<ScheduleNode> selectedScheduleItem;

	// list of edited schedules
	private final Set<TreeItem<ScheduleNode>> editedScheduleItems = new HashSet<>();

	// current shift being edited
	private Shift currentShift;

	// current team being edited
	private Team currentTeam;

	// current rotation being edited
	private Rotation currentRotation;

	// current rotation segment being edited
	private RotationSegment currentRotationSegment;

	// current non-working period being edited
	private NonWorkingPeriod currentPeriod;

	// list of shifts associated with the work schedule being edited
	private final ObservableList<Shift> shiftList = FXCollections.observableArrayList(new ArrayList<>());

	// list of shift names for the rotation segment starting shift choice
	private final ObservableList<String> shiftNames = FXCollections.observableArrayList(new ArrayList<>());

	// list of teams associated with the work schedule being edited
	private final ObservableList<Team> teamList = FXCollections.observableArrayList(new ArrayList<>());

	// list of rotation names available for team assignment
	private final ObservableList<String> rotationNames = FXCollections.observableArrayList(new ArrayList<>());

	// list of rotations
	private final ObservableList<Rotation> rotationList = FXCollections.observableArrayList(new ArrayList<>());

	// list of rotation segments
	private final ObservableList<RotationSegment> rotationSegmentList = FXCollections
			.observableArrayList(new ArrayList<>());

	// list of non-working periods associated with the work schedule
	private final ObservableList<NonWorkingPeriod> periodList = FXCollections.observableArrayList(new ArrayList<>());

	// controller for template schedules
	private TemplateScheduleDialogController templateController;

	// work schedule
	@FXML
	private TextField tfScheduleName;

	@FXML
	private TextArea taScheduleDescription;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btRefresh;

	@FXML
	private Button btDelete;

	@FXML
	private TreeView<ScheduleNode> tvSchedules;

	@FXML
	private Tab tShifts;

	@FXML
	private Tab tRotations;

	@FXML
	private Tab tTeams;

	@FXML
	private TabPane tpShiftTeams;

	// ******************* shifts *********************************************
	@FXML
	private TextField tfShiftName;

	@FXML
	private TextField tfShiftDescription;

	@FXML
	private TextField tfShiftStart;

	@FXML
	private TextField tfShiftDuration;

	@FXML
	private TableView<Shift> tvShifts;

	@FXML
	private TableColumn<Shift, String> shiftNameColumn;

	@FXML
	private TableColumn<Shift, String> shiftDescriptionColumn;

	@FXML
	private TableColumn<Shift, LocalTime> shiftStartColumn;

	@FXML
	private TableColumn<Shift, String> shiftDurationColumn;

	@FXML
	private Button btNewShift;

	@FXML
	private Button btAddShift;

	@FXML
	private Button btRemoveShift;

	// ******************* teams *********************************************
	@FXML
	private TextField tfTeamName;

	@FXML
	private TextField tfTeamDescription;

	@FXML
	private ComboBox<String> cbTeamRotations;

	@FXML
	private DatePicker dpTeamRotationStart;

	@FXML
	private TableView<Team> tvTeams;

	@FXML
	private TableColumn<Team, String> teamNameColumn;

	@FXML
	private TableColumn<Team, String> teamDescriptionColumn;

	@FXML
	private TableColumn<Team, String> teamRotationColumn;

	@FXML
	private TableColumn<Team, LocalDate> teamRotationStartColumn;

	@FXML
	private TableColumn<Team, String> teamAvgHoursColumn;

	@FXML
	private Button btNewTeam;

	@FXML
	private Button btAddTeam;

	@FXML
	private Button btRemoveTeam;

	// ******************* rotations **************************************
	@FXML
	private TextField tfRotationName;

	@FXML
	private TextField tfRotationDescription;

	@FXML
	private ComboBox<String> cbRotationSegmentShifts;

	@FXML
	private Spinner<Integer> spDaysOn;

	@FXML
	private Spinner<Integer> spDaysOff;

	@FXML
	private TableView<Rotation> tvRotations;

	@FXML
	private TableColumn<Rotation, String> rotationNameColumn;

	@FXML
	private TableColumn<Rotation, String> rotationDescriptionColumn;

	@FXML
	private TableColumn<Rotation, String> rotationDurationColumn;

	@FXML
	private TableView<RotationSegment> tvRotationSegments;

	@FXML
	private TableColumn<RotationSegment, String> rotationSegmentSequenceColumn;

	@FXML
	private TableColumn<RotationSegment, String> rotationSegmentShiftColumn;

	@FXML
	private TableColumn<RotationSegment, String> rotationSegmentDaysOnColumn;

	@FXML
	private TableColumn<RotationSegment, String> rotationSegmentDaysOffColumn;

	@FXML
	private Button btNewRotation;

	@FXML
	private Button btAddRotation;

	@FXML
	private Button btRemoveRotation;

	@FXML
	private Button btNewRotationSegment;

	@FXML
	private Button btAddRotationSegment;

	@FXML
	private Button btRemoveRotationSegment;

	// ***** non-working periods *******************************************
	@FXML
	private TextField tfPeriodName;

	@FXML
	private TextField tfPeriodDescription;

	@FXML
	private DatePicker dpPeriodStartDate;

	@FXML
	private TextField tfPeriodStartTime;

	@FXML
	private TextField tfPeriodDuration;

	@FXML
	private TableView<NonWorkingPeriod> tvNonWorkingPeriods;

	@FXML
	private TableColumn<NonWorkingPeriod, String> periodNameColumn;

	@FXML
	private TableColumn<NonWorkingPeriod, String> periodDescriptionColumn;

	@FXML
	private TableColumn<NonWorkingPeriod, LocalDateTime> periodStartColumn;

	@FXML
	private TableColumn<NonWorkingPeriod, String> periodDurationColumn;

	@FXML
	private TableColumn<NonWorkingPeriod, String> periodLossColumn;

	@FXML
	private Button btNewNonWorkingPeriod;

	@FXML
	private Button btAddNonWorkingPeriod;

	@FXML
	private Button btRemoveNonWorkingPeriod;

	@FXML
	private ComboBox<TimeLoss> cbLosses;

	// context menu
	@FXML
	private MenuItem miSaveAll;

	@FXML
	private MenuItem miRefreshAll;

	// template schedule
	@FXML
	private Button btChooseSchedule;

	private void initializeScheduleList() {
		// list of schedules selection listener
		tvSchedules.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectSchedule(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		tvSchedules.setShowRoot(false);
	}

	private void initializeShiftEditor() {
		// bind to list of shifts
		tvShifts.setItems(shiftList);

		// table view row selection listener
		tvShifts.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue != null) {
				try {
					onSelectShift(newValue);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			}
		});

		// shift table callbacks
		// shift name
		shiftNameColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getName());
		});

		// shift description
		shiftDescriptionColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getDescription());
		});

		// shift start time
		shiftStartColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<LocalTime>(cellDataFeatures.getValue().getStart());
		});

		// shift duration
		shiftDurationColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(AppUtils.stringFromDuration(cellDataFeatures.getValue().getDuration(), false));
		});
	}

	private void initializeRotationEditor() {

		// spinner value factory
		SpinnerValueFactory<Integer> onValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,
				Integer.MAX_VALUE, 1);

		spDaysOn.setValueFactory(onValueFactory);

		// spinner value factory
		SpinnerValueFactory<Integer> offValueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,
				Integer.MAX_VALUE, 1);

		spDaysOff.setValueFactory(offValueFactory);

		// tables
		tvRotations.setItems(rotationList);
		tvRotationSegments.setItems(rotationSegmentList);

		// rotation table view row selection listener
		tvRotations.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue != null) {
				try {
					onSelectRotation(newValue);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			}
		});

		// rotation table callbacks
		// rotation name
		rotationNameColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getName());
		});

		// rotation description
		rotationDescriptionColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getDescription());
		});

		// rotation duration
		rotationDurationColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(AppUtils.stringFromDuration(cellDataFeatures.getValue().getDuration(), false));
		});

		// rotation table view row selection listener
		tvRotationSegments.getSelectionModel().selectedItemProperty()
				.addListener((observableValue, oldValue, newValue) -> {
					if (newValue != null) {
						try {
							updateRotationSegmentEditor(newValue);
						} catch (Exception e) {
							AppUtils.showErrorDialog(e);
						}
					}
				});

		// segment start shift
		rotationSegmentShiftColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getStartingShift().getName());
		});

		// segment days on
		rotationSegmentDaysOnColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(String.valueOf(cellDataFeatures.getValue().getDaysOn()));
		});

		// segment days off
		rotationSegmentDaysOffColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(String.valueOf(cellDataFeatures.getValue().getDaysOff()));
		});

		// sequence
		rotationSegmentSequenceColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(String.valueOf(cellDataFeatures.getValue().getSequence()));
		});

		// starting shift names
		this.cbRotationSegmentShifts.setItems(shiftNames);

	}

	private void initializeTeamEditor() throws Exception {
		// list of rotations to choose from
		this.cbTeamRotations.setItems(rotationNames);

		// bind to list of teams
		tvTeams.setItems(teamList);

		// table view row selection listener
		tvTeams.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue != null) {
				try {
					onSelectTeam(newValue);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			}
		});

		// team table callbacks
		// team name
		teamNameColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getName());
		});

		// team description
		teamDescriptionColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getDescription());
		});

		// team rotation name
		teamRotationColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getRotation().getName());
		});

		// rotation start
		teamRotationStartColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<LocalDate>(cellDataFeatures.getValue().getRotationStart());
		});

		// team average hours worked per week
		teamAvgHoursColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(
					AppUtils.stringFromDuration(cellDataFeatures.getValue().getHoursWorkedPerWeek(), false));
		});
	}

	private void initializeNonWorkingPeriodEditor() throws Exception {
		// bind to list of periods
		this.tvNonWorkingPeriods.setItems(periodList);

		// table view row selection listener
		tvNonWorkingPeriods.getSelectionModel().selectedItemProperty()
				.addListener((observableValue, oldValue, newValue) -> {
					if (newValue != null) {
						try {
							onSelectNonWorkingPeriod(newValue);
						} catch (Exception e) {
							AppUtils.showErrorDialog(e);
						}
					}
				});

		// non-working period table callbacks
		// period name
		periodNameColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getName());
		});

		// period description
		periodDescriptionColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(cellDataFeatures.getValue().getDescription());
		});

		// period start
		periodStartColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleObjectProperty<LocalDateTime>(cellDataFeatures.getValue().getStartDateTime());
		});

		// period duration
		periodDurationColumn.setCellValueFactory(cellDataFeatures -> {
			return new SimpleStringProperty(AppUtils.stringFromDuration(cellDataFeatures.getValue().getDuration(), false));
		});

		// period loss
		periodLossColumn.setCellValueFactory(cellDataFeatures -> {
			TimeLoss loss = cellDataFeatures.getValue().getLossCategory();
			SimpleStringProperty property = new SimpleStringProperty();

			if (loss != null) {
				property = new SimpleStringProperty(loss.toString());
			}
			return property;
		});

		// loss categories
		cbLosses.getItems().clear();
		cbLosses.getItems().addAll(TimeLoss.getNonWorkingLosses());
	}

	@FXML
	public void initialize() throws Exception {
		// images for controls
		setImages();
	}

	// initialize editor
	public void initializeApp(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// all work schedules
		initializeScheduleList();

		// shift editor
		initializeShiftEditor();

		// team editor
		initializeTeamEditor();

		// rotation editor
		initializeRotationEditor();

		// non-working period editor
		initializeNonWorkingPeriodEditor();

		// display all defined work schedules
		displaySchedules();
	}

	// the single root for all schedules (not persistent)
	private TreeItem<ScheduleNode> getRootScheduleItem() throws Exception {
		if (tvSchedules.getRoot() == null) {
			WorkSchedule rootSchedule = new WorkSchedule();
			rootSchedule.setName(ROOT_SCHEDULE_NAME);
			tvSchedules.setRoot(new TreeItem<>(new ScheduleNode(rootSchedule)));
		}
		return tvSchedules.getRoot();
	}

	// update the editor upon selection of a work schedule or refresh
	private void onSelectSchedule(TreeItem<ScheduleNode> oldItem, TreeItem<ScheduleNode> newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		// check for previous edit
		/*
		 * if (oldItem != null) { boolean isChanged = setAttributes(oldItem);
		 * 
		 * if (isChanged) {
		 * oldItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
		 * tvSchedules.refresh(); } }
		 */
		selectedScheduleItem = newItem;

		// new attributes
		WorkSchedule selectedSchedule = getSelectedSchedule();

		if (selectedSchedule == null) {
			return;
		}

		displayAttributes(selectedSchedule);
	}

	// update the shift editing part
	private void onSelectShift(Shift shift) {
		btAddShift.setText("Update");
		this.currentShift = shift;

		// name
		this.tfShiftName.setText(shift.getName());

		// description
		this.tfShiftDescription.setText(shift.getDescription());

		// start time
		LocalTime startTime = shift.getStart();
		this.tfShiftStart.setText(AppUtils.stringFromLocalTime(startTime));

		Duration duration = shift.getDuration();
		this.tfShiftDuration.setText(AppUtils.stringFromDuration(duration, false));
	}

	// called on team selection in table listener
	private void onSelectTeam(Team team) {
		btAddTeam.setText("Update");
		this.currentTeam = team;

		// name
		this.tfTeamName.setText(team.getName());

		// description
		this.tfTeamDescription.setText(team.getDescription());

		// rotation
		this.cbTeamRotations.getSelectionModel().select(team.getRotation().getName());

		// rotation start
		this.dpTeamRotationStart.setValue(team.getRotationStart());
	}

	// called on rotation selection in table listener
	private void onSelectRotation(Rotation rotation) {
		btAddRotation.setText("Update");

		this.currentRotation = rotation;

		// name
		this.tfRotationName.setText(rotation.getName());

		// description
		this.tfRotationDescription.setText(rotation.getDescription());

		// segments
		List<RotationSegment> segments = rotation.getRotationSegments();

		rotationSegmentList.clear();
		for (RotationSegment segment : segments) {
			rotationSegmentList.add(segment);
		}
		Collections.sort(rotationSegmentList);
		tvRotationSegments.refresh();
	}

	// called on rotation segment selection in table listener
	private void updateRotationSegmentEditor(RotationSegment segment) {
		btAddRotationSegment.setText("Update");

		this.currentRotationSegment = segment;

		this.cbRotationSegmentShifts.setValue(segment.getStartingShift().getName());

		this.spDaysOn.getValueFactory().setValue(segment.getDaysOn());

		this.spDaysOff.getValueFactory().setValue(segment.getDaysOff());
	}

	// called on non-working period selection in table listener
	private void onSelectNonWorkingPeriod(NonWorkingPeriod period) {
		btAddNonWorkingPeriod.setText("Update");
		this.currentPeriod = period;

		// name
		this.tfPeriodName.setText(period.getName());

		// description
		this.tfPeriodDescription.setText(period.getDescription());

		// start
		this.dpPeriodStartDate.setValue(period.getStartDateTime().toLocalDate());
		LocalTime startTime = period.getStartDateTime().toLocalTime();
		this.tfPeriodStartTime.setText(AppUtils.stringFromLocalTime(startTime));

		// duration
		this.tfPeriodDuration.setText(AppUtils.stringFromDuration(period.getDuration(), false));

		// loss
		this.cbLosses.getSelectionModel().select(period.getLossCategory());
	}

	private void displaySchedules() throws Exception {
		getRootScheduleItem().getChildren().clear();

		List<WorkSchedule> schedules = PersistenceService.instance().fetchWorkSchedules();
		Collections.sort(schedules);

		for (WorkSchedule schedule : schedules) {
			TreeItem<ScheduleNode> scheduleItem = new TreeItem<>(new ScheduleNode(schedule));
			resetGraphic(scheduleItem);
			getRootScheduleItem().getChildren().add(scheduleItem);
		}
		tvSchedules.refresh();
	}

	@Override
	// images for editor buttons
	protected void setImages() throws Exception {
		super.setImages();

		// new schedule
		btNew.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNew.setContentDisplay(ContentDisplay.RIGHT);

		// save schedule
		btSave.setGraphic(ImageManager.instance().getImageView(Images.SAVE));
		btSave.setContentDisplay(ContentDisplay.RIGHT);

		// refesh schedule
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
		btRefresh.setContentDisplay(ContentDisplay.RIGHT);

		// delete schedule
		btDelete.setGraphic(ImageManager.instance().getImageView(Images.DELETE));
		btDelete.setContentDisplay(ContentDisplay.RIGHT);

		// new shift
		btNewShift.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewShift.setContentDisplay(ContentDisplay.LEFT);

		// add shift
		btAddShift.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddShift.setContentDisplay(ContentDisplay.LEFT);

		// remove shift
		btRemoveShift.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveShift.setContentDisplay(ContentDisplay.LEFT);

		// new team
		btNewTeam.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewTeam.setContentDisplay(ContentDisplay.LEFT);

		// add team
		btAddTeam.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddTeam.setContentDisplay(ContentDisplay.LEFT);

		// remove team
		btRemoveTeam.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveTeam.setContentDisplay(ContentDisplay.LEFT);

		// new rotation
		btNewRotation.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewRotation.setContentDisplay(ContentDisplay.LEFT);

		// add rotation
		btAddRotation.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddRotation.setContentDisplay(ContentDisplay.LEFT);

		// remove rotation
		btRemoveRotation.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveRotation.setContentDisplay(ContentDisplay.LEFT);

		// new rotation segment
		btNewRotationSegment.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewRotationSegment.setContentDisplay(ContentDisplay.LEFT);

		// add rotation segment
		btAddRotationSegment.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddRotationSegment.setContentDisplay(ContentDisplay.LEFT);

		// remove rotation segment
		btRemoveRotationSegment.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveRotationSegment.setContentDisplay(ContentDisplay.LEFT);

		// new non-working period
		btNewNonWorkingPeriod.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewNonWorkingPeriod.setContentDisplay(ContentDisplay.LEFT);

		// add non-working period
		btAddNonWorkingPeriod.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddNonWorkingPeriod.setContentDisplay(ContentDisplay.LEFT);

		// remove non-working period
		btRemoveNonWorkingPeriod.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveNonWorkingPeriod.setContentDisplay(ContentDisplay.LEFT);

		// choose template schedule
		btChooseSchedule.setGraphic(ImageManager.instance().getImageView(Images.CHOOSE));
		btChooseSchedule.setContentDisplay(ContentDisplay.RIGHT);

		// context menu
		miSaveAll.setGraphic(ImageManager.instance().getImageView(Images.SAVE_ALL));
		miRefreshAll.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));
	}

	private void resetEditor() {
		// main attributes
		this.tfScheduleName.clear();
		this.tfScheduleName.requestFocus();
		this.taScheduleDescription.clear();

		// reset each sub-editor
		onNewShift();
		onNewTeam();
		onNewRotation();
		onNewRotationSegment();
		onNewNonWorkingPeriod();

		// clear the tables
		this.shiftList.clear();
		this.shiftNames.clear();
		this.tvShifts.refresh();

		this.teamList.clear();
		this.tvShifts.refresh();

		this.rotationList.clear();
		this.rotationNames.clear();
		this.tvRotations.refresh();

		this.rotationSegmentList.clear();
		this.tvRotationSegments.refresh();

		this.periodList.clear();
		this.tvNonWorkingPeriods.refresh();
	}

	// New button clicked
	@FXML
	private void onNewSchedule() {
		try {
			resetEditor();

			selectedScheduleItem = null;
			tvSchedules.getSelectionModel().clearSelection();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// Delete button clicked
	@FXML
	private void onDeleteSchedule() {
		try {
			if (selectedScheduleItem == null) {
				AppUtils.showErrorDialog("No schedule has been selected for deletion.");
				return;
			}

			// confirm
			WorkSchedule toDelete = getSelectedSchedule();
			String msg = "Do you want to delete schedule " + toDelete.getName() + "?";
			ButtonType type = AppUtils.showConfirmationDialog(msg);

			if (type.equals(ButtonType.CANCEL)) {
				return;
			}

			// delete
			PersistenceService.instance().delete(toDelete);

			// remove this schedule from the tree
			TreeItem<ScheduleNode> selectedScheduleItem = tvSchedules.getSelectionModel().getSelectedItem();
			TreeItem<ScheduleNode> rootNode = tvSchedules.getRoot();
			rootNode.getChildren().remove(selectedScheduleItem);
			tvSchedules.refresh();

			onNewSchedule();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void resetGraphic(TreeItem<ScheduleNode> scheduleItem) throws Exception {
		scheduleItem.setGraphic(ImageManager.instance().getImageView(Images.SCHEDULE));
	}

	@FXML
	private void onRefreshSchedule() {
		try {
			if (getSelectedSchedule() == null) {
				return;
			}

			if (getSelectedSchedule().getKey() != null) {
				// read from database
				WorkSchedule schedule = PersistenceService.instance()
						.fetchScheduleByKey(getSelectedSchedule().getKey());
				selectedScheduleItem.getValue().setWorkSchedule(schedule);
				resetGraphic(selectedScheduleItem);
				displayAttributes(schedule);
			} else {
				// remove from tree
				selectedScheduleItem.getParent().getChildren().remove(selectedScheduleItem);
			}
			tvSchedules.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRefreshAllSchedules() {
		try {
			// all work schedules
			initializeScheduleList();

			// shift editor
			initializeShiftEditor();

			// team editor
			initializeTeamEditor();

			// rotation editor
			initializeRotationEditor();

			// non-working period editor
			initializeNonWorkingPeriodEditor();

			// display all defined work schedules
			displaySchedules();

			resetEditor();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private boolean setAttributes(TreeItem<ScheduleNode> scheduleItem) throws Exception {
		boolean isDirty = false;

		if (scheduleItem == null) {
			return isDirty;
		}
		WorkSchedule schedule = scheduleItem.getValue().getWorkSchedule();

		// name
		String name = this.tfScheduleName.getText().trim();

		if (!name.equals(schedule.getName())) {
			schedule.setName(name);
			isDirty = true;
		}

		// description
		String description = this.taScheduleDescription.getText();

		if (!description.equals(schedule.getDescription())) {
			schedule.setDescription(description);
			isDirty = true;
		}

		if (isDirty) {
			addEditedSchedule(scheduleItem);
		}
		return isDirty;
	}

	private void addEditedSchedule(TreeItem<ScheduleNode> scheduleItem) throws Exception {
		if (!editedScheduleItems.contains(scheduleItem)) {
			editedScheduleItems.add(scheduleItem);
			scheduleItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
		}
	}

	private void createSchedule() {
		try {
			// set attributes
			WorkSchedule newSchedule = new WorkSchedule();
			selectedScheduleItem = new TreeItem<>(new ScheduleNode(newSchedule));
			addEditedSchedule(selectedScheduleItem);

			// set attributes from UI
			setAttributes(selectedScheduleItem);

			// add to tree
			getRootScheduleItem().getChildren().add(selectedScheduleItem);

			tvSchedules.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void removeEditedSchedule(TreeItem<ScheduleNode> scheduleItem) throws Exception {
		resetGraphic(scheduleItem);
		editedScheduleItems.remove(scheduleItem);
	}

	// Save button clicked
	@FXML
	private void onSaveSchedule() {
		try {
			if (selectedScheduleItem == null) {
				// create
				createSchedule();
			} else {
				// update
				setAttributes(selectedScheduleItem);
			}

			// save the created or updated work schedule
			WorkSchedule schedule = getSelectedSchedule();
			WorkSchedule saved = (WorkSchedule) PersistenceService.instance().save(schedule);
			selectedScheduleItem.getValue().setWorkSchedule(saved);
			removeEditedSchedule(selectedScheduleItem);

			tvSchedules.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveAllSchedules() {
		try {
			// current material could have been edited
			setAttributes(selectedScheduleItem);

			// save all modified schedules
			for (TreeItem<ScheduleNode> editedScheduleItem : editedScheduleItems) {
				ScheduleNode node = editedScheduleItem.getValue();
				WorkSchedule saved = (WorkSchedule) PersistenceService.instance().save(node.getWorkSchedule());
				node.setWorkSchedule(saved);
				resetGraphic(editedScheduleItem);
			}
			editedScheduleItems.clear();

			tvSchedules.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// show the schedule attributes
	private void displayAttributes(WorkSchedule schedule) {
		this.resetEditor();

		// main schedule
		this.tfScheduleName.setText(schedule.getName());
		this.taScheduleDescription.setText(schedule.getDescription());

		// shifts
		List<Shift> shifts = schedule.getShifts();

		shiftList.clear();
		shiftNames.clear();

		for (Shift shift : shifts) {
			shiftList.add(shift);
			shiftNames.add(shift.getName());
		}
		Collections.sort(shiftList);
		tvShifts.refresh();

		// teams
		List<Team> teams = schedule.getTeams();

		teamList.clear();
		rotationList.clear();
		rotationSegmentList.clear();
		rotationNames.clear();

		for (Team team : teams) {
			teamList.add(team);

			Rotation rotation = team.getRotation();

			if (rotation.getName() != null && !rotationList.contains(rotation)) {
				// add to table binding
				rotationList.add(rotation);

				// add to combobox of names
				rotationNames.add(rotation.getName());
			}
		}

		Collections.sort(teamList);
		Collections.sort(rotationList);
		Collections.sort(rotationNames);

		tvTeams.refresh();
		tvRotations.refresh();

		// non-working periods
		List<NonWorkingPeriod> periods = schedule.getNonWorkingPeriods();
		periodList.clear();

		for (NonWorkingPeriod period : periods) {
			periodList.add(period);
		}
		Collections.sort(periodList);

		tvNonWorkingPeriods.refresh();

	}

	// new shift button clicked
	@FXML
	private void onNewShift() {
		btAddShift.setText("Add");
		this.currentShift = null;

		// shift editing attributes
		this.tfShiftName.clear();
		this.tfShiftDescription.clear();
		this.tfShiftStart.clear();
		this.tfShiftDuration.clear();

		this.tvShifts.getSelectionModel().clearSelection();
	}

	// new team button clicked
	@FXML
	private void onNewTeam() {
		btAddTeam.setText("Add");
		this.currentTeam = null;

		// team editing attributes
		this.tfTeamName.clear();
		this.tfTeamDescription.clear();
		this.cbTeamRotations.getSelectionModel().clearSelection();
		this.dpTeamRotationStart.setValue(null);

		this.tvTeams.getSelectionModel().clearSelection();
	}

	// add shift button clicked
	@FXML
	private void onAddShift() {
		try {
			// need a work schedule first
			if (getSelectedSchedule() == null) {
				throw new Exception("A work schedule must be created before adding a shift to it.");
			}

			// name
			String name = this.tfShiftName.getText().trim();

			if (name == null || name.length() == 0) {
				throw new Exception("The name of the shift must be specified.");
			}

			// add to comboBox for rotation segments
			if (!shiftNames.contains(name)) {
				shiftNames.add(name);
			}

			// description
			String description = this.tfShiftDescription.getText().trim();

			// start time
			String start = this.tfShiftStart.getText().trim();
			LocalTime startTime = AppUtils.localTimeFromString(start);

			// duration
			String hrsMins = this.tfShiftDuration.getText().trim();

			Duration duration = AppUtils.durationFromString(hrsMins);

			if (currentShift == null) {
				// new shift
				currentShift = getSelectedSchedule().createShift(name, description, startTime, duration);
				shiftList.add(currentShift);
			} else {
				currentShift.setName(name);
				currentShift.setDescription(description);
				currentShift.setStart(startTime);
				currentShift.setDuration(duration);
			}
			Collections.sort(shiftList);

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvShifts.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// remove shift button clicked
	@FXML
	private void onRemoveShift() {
		try {
			Shift shift = this.tvShifts.getSelectionModel().getSelectedItem();

			if (shift == null) {
				return;
			}

			getSelectedSchedule().getShifts().remove(shift);
			shiftList.remove(shift);
			Collections.sort(shiftList);
			shiftNames.remove(shift.getName());
			currentShift = null;
			tvShifts.getSelectionModel().clearSelection();

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvShifts.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// add team button clicked
	@FXML
	private void onAddTeam() {
		try {
			// need a work schedule first
			if (getSelectedSchedule() == null) {
				throw new Exception("A work schedule must be created before adding a team to it.");
			}

			// name
			String name = this.tfTeamName.getText().trim();

			if (name == null || name.length() == 0) {
				throw new Exception("The name of the team must be specified.");
			}

			// description
			String description = this.tfTeamDescription.getText().trim();

			// rotation
			String rotationName = this.cbTeamRotations.getSelectionModel().getSelectedItem();

			Rotation teamRotation = null;

			for (Rotation rotation : rotationList) {
				if (rotation.getName().equals(rotationName)) {
					teamRotation = rotation;
					break;
				}
			}

			// rotation start
			LocalDate rotationStart = this.dpTeamRotationStart.getValue();

			if (currentTeam == null) {
				// new team
				currentTeam = getSelectedSchedule().createTeam(name, description, teamRotation, rotationStart);
				teamList.add(currentTeam);
			} else {
				currentTeam.setName(name);
				currentTeam.setDescription(description);
				currentTeam.setRotation(teamRotation);
				currentTeam.setRotationStart(rotationStart);
			}

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvTeams.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// remove team button clicked
	@FXML
	private void onRemoveTeam() {
		try {
			Team team = this.tvTeams.getSelectionModel().getSelectedItem();

			if (team == null) {
				return;
			}

			getSelectedSchedule().getTeams().remove(team);
			teamList.remove(team);
			Collections.sort(shiftList);
			currentTeam = null;
			tvTeams.getSelectionModel().clearSelection();

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvTeams.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// add rotation button clicked
	@FXML
	private void onAddRotation() {
		try {
			// need a work schedule first
			if (getSelectedSchedule() == null) {
				throw new Exception("A work schedule must be created before adding a rotation to it.");
			}

			// name
			String name = this.tfRotationName.getText().trim();

			// description
			String description = this.tfRotationDescription.getText().trim();

			if (currentRotation == null) {
				// new rotation
				currentRotation = new Rotation(name, description);
				rotationList.add(currentRotation);
			} else {
				currentRotation.setName(name);
				currentRotation.setDescription(description);
			}

			tvRotations.refresh();

			// also set for use by a team
			if (!rotationNames.contains(name)) {
				rotationNames.add(name);
			}

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// new rotation button clicked
	@FXML
	private void onNewRotation() {
		btAddRotation.setText("Add");
		this.currentRotation = null;

		// team editing attributes
		this.tfRotationName.clear();
		this.tfRotationDescription.clear();

		this.tvRotations.getSelectionModel().clearSelection();
	}

	// remove rotation button clicked
	@FXML
	private void onRemoveRotation() {
		try {
			Rotation rotation = this.tvRotations.getSelectionModel().getSelectedItem();

			if (rotation == null) {
				return;
			}

			// check for team reference
			PersistenceService.instance().checkReferences(rotation);

			rotationList.remove(rotation);
			currentRotation = null;
			tvRotations.getSelectionModel().clearSelection();

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvRotations.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// new rotation segment button clicked
	@FXML
	private void onNewRotationSegment() {
		btAddRotationSegment.setText("Add");
		this.currentRotationSegment = null;

		// editing attributes
		this.cbRotationSegmentShifts.getSelectionModel().clearSelection();
		this.spDaysOn.getValueFactory().setValue(1);
		this.spDaysOff.getValueFactory().setValue(1);

		this.tvRotationSegments.getSelectionModel().clearSelection();
	}

	// add rotation segment button clicked
	@FXML
	private void onAddRotationSegment() {
		try {
			// need a work schedule first
			if (getSelectedSchedule() == null) {
				throw new Exception("A work schedule must be created before adding a rotation segment to it.");
			}

			if (currentRotation == null) {
				throw new Exception("A rotation must be selected before adding a segment.");
			}

			// shift
			String shiftName = this.cbRotationSegmentShifts.getSelectionModel().getSelectedItem();
			Shift startingShift = null;

			for (Shift shift : shiftList) {
				if (shift.getName().equals(shiftName)) {
					startingShift = shift;
					break;
				}
			}

			if (startingShift == null) {
				throw new Exception("No shift found with name" + shiftName);
			}

			Integer daysOn = this.spDaysOn.getValue();
			Integer daysOff = this.spDaysOff.getValue();

			if (currentRotationSegment == null) {
				// new
				currentRotationSegment = currentRotation.addSegment(startingShift, daysOn, daysOff);
				rotationSegmentList.add(currentRotationSegment);
				currentRotationSegment.setSequence(rotationSegmentList.size());
			} else {
				// update
				currentRotationSegment.setStartingShift(startingShift);
				currentRotationSegment.setDaysOn(daysOn);
				currentRotationSegment.setDaysOff(daysOff);
			}

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvRotationSegments.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// remove rotation segment button clicked
	@FXML
	private void onRemoveRotationSegment() {
		try {
			RotationSegment segment = this.tvRotationSegments.getSelectionModel().getSelectedItem();

			if (segment == null) {
				return;
			}

			rotationSegmentList.remove(segment);

			// re-order
			for (int i = 0; i < rotationSegmentList.size(); i++) {
				rotationSegmentList.get(i).setSequence(i + 1);
			}

			currentRotationSegment = null;
			tvRotationSegments.getSelectionModel().clearSelection();

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvRotationSegments.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// new NonWorkingPeriod button clicked
	@FXML
	private void onNewNonWorkingPeriod() {
		btAddNonWorkingPeriod.setText("Add");
		this.currentPeriod = null;

		// NonWorkingPeriod editing attributes
		this.tfPeriodName.clear();
		this.tfPeriodDescription.clear();
		this.dpPeriodStartDate.setValue(null);
		this.tfPeriodStartTime.clear();
		this.tfPeriodDuration.clear();
		this.cbLosses.getSelectionModel().clearSelection();
		this.cbLosses.getSelectionModel().select(null);

		this.tvNonWorkingPeriods.getSelectionModel().clearSelection();
	}

	// add NonWorkingPeriod button clicked
	@FXML
	private void onAddNonWorkingPeriod() {
		try {
			// need a work schedule first
			if (getSelectedSchedule() == null) {
				throw new Exception("A work schedule must be created before adding a non-working period to it.");
			}

			// name
			String name = this.tfPeriodName.getText().trim();

			if (name == null || name.length() == 0) {
				throw new Exception("The name of the non-working period must be specified.");
			}

			// description
			String description = this.tfPeriodDescription.getText().trim();

			// start date
			LocalDate startDate = this.dpPeriodStartDate.getValue();

			// start time of day
			String start = this.tfPeriodStartTime.getText().trim();
			LocalTime startTime = AppUtils.localTimeFromString(start);

			LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);

			// duration
			String hrsMins = this.tfPeriodDuration.getText().trim();

			Duration duration = AppUtils.durationFromString(hrsMins);

			// loss category
			TimeLoss loss = this.cbLosses.getSelectionModel().getSelectedItem();

			if (currentPeriod == null) {
				// new non-working period
				currentPeriod = getSelectedSchedule().createNonWorkingPeriod(name, description, startDateTime,
						duration);
				currentPeriod.setLossCategory(loss);
				periodList.add(currentPeriod);
			} else {
				currentPeriod.setName(name);
				currentPeriod.setDescription(description);
				currentPeriod.setStartDateTime(startDateTime);
				currentPeriod.setDuration(duration);
				currentPeriod.setLossCategory(loss);
			}
			Collections.sort(periodList);

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvNonWorkingPeriods.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// remove non-working period button clicked
	@FXML
	private void onRemoveNonWorkingPeriod() {
		try {
			NonWorkingPeriod period = this.tvNonWorkingPeriods.getSelectionModel().getSelectedItem();

			if (period == null) {
				return;
			}

			getSelectedSchedule().getNonWorkingPeriods().remove(period);
			periodList.remove(period);
			Collections.sort(periodList);
			currentPeriod = null;
			tvNonWorkingPeriods.getSelectionModel().clearSelection();

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvNonWorkingPeriods.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// get the selected work schedule
	public WorkSchedule getSelectedSchedule() {
		WorkSchedule schedule = null;

		if (selectedScheduleItem != null) {
			schedule = selectedScheduleItem.getValue().getWorkSchedule();
		}
		return schedule;
	}

	@Override
	@FXML
	protected void onCancel() {
		// close dialog with current material set to null
		selectedScheduleItem = null;
		super.onCancel();
	}

	@FXML
	private void onChooseSchedule() {
		try {
			if (templateController == null) {
				FXMLLoader loader = FXMLLoaderFactory.templateScheduleLoader();
				AnchorPane page = (AnchorPane) loader.getRoot();

				// Create the dialog Stage.
				Stage dialogStage = new Stage(StageStyle.DECORATED);
				dialogStage.setTitle("Template Work Schedule");
				dialogStage.initModality(Modality.NONE);
				Scene scene = new Scene(page);
				dialogStage.setScene(scene);

				// get the controller
				templateController = loader.getController();
				templateController.setDialogStage(dialogStage);
			}

			// Show the dialog and wait until the user closes it
			templateController.getDialogStage().showAndWait();

			WorkSchedule schedule = templateController.getSelectedSchedule();

			if (schedule != null) {
				PersistenceService.instance().save(schedule);

				displaySchedules();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// class for holding a work schedule object in a tree view leaf node
	private class ScheduleNode {
		// schedule
		private WorkSchedule schedule;

		private ScheduleNode(WorkSchedule schedule) {
			this.setWorkSchedule(schedule);
		}

		private WorkSchedule getWorkSchedule() {
			return schedule;
		}

		private void setWorkSchedule(WorkSchedule schedule) {
			this.schedule = schedule;
		}

		@Override
		public String toString() {
			return schedule.getName();
		}
	}
}
