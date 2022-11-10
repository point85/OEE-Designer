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
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.schedule.Break;
import org.point85.domain.schedule.ExceptionPeriod;
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
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WorkScheduleEditorController extends DesignerDialogController {
	// not localizable
	private static final String ROOT_SCHEDULE_NAME = "All Schedules";

	// schedule being edited or viewed
	private TreeItem<ScheduleNode> selectedScheduleItem;

	// list of edited schedules
	private final Set<TreeItem<ScheduleNode>> editedScheduleItems = new HashSet<>();

	// current shift being edited
	private Shift currentShift;

	// current break being edited
	private Break currentBreak;

	// current team being edited
	private Team currentTeam;

	// current rotation being edited
	private Rotation currentRotation;

	// current rotation segment being edited
	private RotationSegment currentRotationSegment;

	// current exception period being edited
	private ExceptionPeriod currentPeriod;

	// list of shifts associated with the work schedule being edited
	private final ObservableList<Shift> shiftList = FXCollections.observableArrayList(new ArrayList<>());

	// list of shift names for the rotation segment starting shift choice
	private final ObservableList<String> shiftNames = FXCollections.observableArrayList(new ArrayList<>());

	// list of break periods associated with the shift
	private final ObservableList<Break> breakList = FXCollections.observableArrayList(new ArrayList<>());

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
	private final ObservableList<ExceptionPeriod> periodList = FXCollections.observableArrayList(new ArrayList<>());

	// controller for template schedules
	private TemplateScheduleDialogController templateController;

	// controller for viewing shift instances
	private WorkScheduleShiftsController shiftsController;

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
	private Button btBackup;

	@FXML
	private TreeView<ScheduleNode> tvSchedules;

	@FXML
	private Tab tbShifts;

	@FXML
	private Tab tbRotations;

	@FXML
	private Tab tbTeams;

	@FXML
	private Tab tbPeriods;

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

	// breaks
	@FXML
	private TitledPane tpBreaks;
	
	@FXML
	private TextField tfBreakName;

	@FXML
	private TextField tfBreakDescription;

	@FXML
	private TextField tfBreakStart;

	@FXML
	private TextField tfBreakDuration;

	@FXML
	private TableView<Break> tvBreaks;

	@FXML
	private TableColumn<Break, String> breakNameColumn;

	@FXML
	private TableColumn<Break, String> breakDescriptionColumn;

	@FXML
	private TableColumn<Break, LocalTime> breakStartColumn;

	@FXML
	private TableColumn<Break, String> breakDurationColumn;

	@FXML
	private TableColumn<Break, String> breakLossColumn;

	@FXML
	private Button btNewBreak;

	@FXML
	private Button btAddBreak;

	@FXML
	private Button btRemoveBreak;

	@FXML
	private ComboBox<TimeLoss> cbBreakLosses;

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

	// ***** non-working and overtime periods ***
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
	private TableView<ExceptionPeriod> tvExceptionPeriods;

	@FXML
	private TableColumn<ExceptionPeriod, String> periodNameColumn;

	@FXML
	private TableColumn<ExceptionPeriod, String> periodDescriptionColumn;

	@FXML
	private TableColumn<ExceptionPeriod, LocalDateTime> periodStartColumn;

	@FXML
	private TableColumn<ExceptionPeriod, String> periodDurationColumn;

	@FXML
	private TableColumn<ExceptionPeriod, String> periodCatColumn;

	@FXML
	private Button btNewExceptionPeriod;

	@FXML
	private Button btAddExceptionPeriod;

	@FXML
	private Button btRemoveExceptionPeriod;

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

	// view shift instances
	@FXML
	private Button btViewShifts;

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

		// shift name
		shiftNameColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getName()));

		// shift description
		shiftDescriptionColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDescription()));

		// shift start time
		shiftStartColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleObjectProperty<LocalTime>(cellDataFeatures.getValue().getStart()));

		// shift duration
		shiftDurationColumn.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(
				AppUtils.stringFromDuration(cellDataFeatures.getValue().getDuration(), false)));

		// breaks
		initializeBreakEditor();
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

		// rotation name
		rotationNameColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getName()));

		// rotation description
		rotationDescriptionColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDescription()));

		// rotation duration
		rotationDurationColumn.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(
				AppUtils.stringFromDuration(cellDataFeatures.getValue().getDuration(), false)));

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
		rotationSegmentShiftColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getStartingShift().getName()));

		// segment days on
		rotationSegmentDaysOnColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(String.valueOf(cellDataFeatures.getValue().getDaysOn())));

		// segment days off
		rotationSegmentDaysOffColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(String.valueOf(cellDataFeatures.getValue().getDaysOff())));

		// sequence
		rotationSegmentSequenceColumn.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(
				String.valueOf(cellDataFeatures.getValue().getSequence())));

		// starting shift names
		cbRotationSegmentShifts.setItems(shiftNames);

	}

	private void initializeTeamEditor() {
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

		// team name
		teamNameColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getName()));

		// team description
		teamDescriptionColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDescription()));

		// team rotation name
		teamRotationColumn.setCellValueFactory(cellDataFeatures -> {
			String name = null;
			Rotation rotation = cellDataFeatures.getValue().getRotation();

			if (rotation != null) {
				name = rotation.getName();
			}
			return new SimpleStringProperty(name);
		});

		// rotation start
		teamRotationStartColumn.setCellValueFactory(cellDataFeatures -> new SimpleObjectProperty<LocalDate>(
				cellDataFeatures.getValue().getRotationStart()));

		// team average hours worked per week
		teamAvgHoursColumn.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(
				AppUtils.stringFromDuration(cellDataFeatures.getValue().getHoursWorkedPerWeek(), false)));
	}

	private void initializeExceptionPeriodEditor() {
		// bind to list of periods
		this.tvExceptionPeriods.setItems(periodList);

		// table view row selection listener
		tvExceptionPeriods.getSelectionModel().selectedItemProperty()
				.addListener((observableValue, oldValue, newValue) -> {
					if (newValue != null) {
						try {
							onSelectExceptionPeriod(newValue);
						} catch (Exception e) {
							AppUtils.showErrorDialog(e);
						}
					}
				});

		// exception period table
		// period name
		periodNameColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getName()));

		// period description
		periodDescriptionColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDescription()));

		// period start
		periodStartColumn.setCellValueFactory(cellDataFeatures -> new SimpleObjectProperty<LocalDateTime>(
				cellDataFeatures.getValue().getStartDateTime()));

		// period duration
		periodDurationColumn.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(
				AppUtils.stringFromDuration(cellDataFeatures.getValue().getDuration(), false)));

		// period loss
		periodCatColumn.setCellValueFactory(cellDataFeatures -> {
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
		cbLosses.getItems().add(TimeLoss.NO_LOSS);
	}

	private void initializeBreakEditor() {
		// bind to list of breaks
		this.tvBreaks.setItems(breakList);

		// table view row selection listener
		tvBreaks.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue != null) {
				try {
					onSelectBreak(newValue);
				} catch (Exception e) {
					AppUtils.showErrorDialog(e);
				}
			}
		});

		// break name
		breakNameColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getName()));

		// break description
		breakDescriptionColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDescription()));

		// break start
		breakStartColumn.setCellValueFactory(
				cellDataFeatures -> new SimpleObjectProperty<LocalTime>(cellDataFeatures.getValue().getStart()));

		// break duration
		breakDurationColumn.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(
				AppUtils.stringFromDuration(cellDataFeatures.getValue().getDuration(), false)));

		// break loss
		breakLossColumn.setCellValueFactory(cellDataFeatures -> {
			TimeLoss loss = cellDataFeatures.getValue().getLossCategory();
			SimpleStringProperty property = new SimpleStringProperty();

			if (loss != null) {
				property = new SimpleStringProperty(loss.toString());
			}
			return property;
		});

		// loss categories
		cbBreakLosses.getItems().clear();
		cbBreakLosses.getItems().addAll(TimeLoss.getBreakLosses());
		cbBreakLosses.getItems().add(TimeLoss.NO_LOSS);
	}

	@FXML
	public void initialize() {
		// images for controls
		setImages();
	}

	// initialize editor
	public void initializeApp(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// add the tab pane listener
		tpShiftTeams.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				if (newValue.equals(tbTeams)) {
					tvTeams.refresh();
				}
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});

		// all work schedules
		initializeScheduleList();

		// shift editor
		initializeShiftEditor();

		// team editor
		initializeTeamEditor();

		// rotation editor
		initializeRotationEditor();

		// non-working period editor
		initializeExceptionPeriodEditor();

		// display all defined work schedules
		displaySchedules();
	}

	// the single root for all schedules (not persistent)
	private TreeItem<ScheduleNode> getRootScheduleItem() {
		if (tvSchedules.getRoot() == null) {
			WorkSchedule rootSchedule = new WorkSchedule();
			rootSchedule.setName(ROOT_SCHEDULE_NAME);
			tvSchedules.setRoot(new TreeItem<>(new ScheduleNode(rootSchedule)));
		}
		return tvSchedules.getRoot();
	}

	// update the editor upon selection of a work schedule or refresh
	private void onSelectSchedule(TreeItem<ScheduleNode> oldItem, TreeItem<ScheduleNode> newItem) {
		if (newItem == null) {
			return;
		}

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
		btAddShift.setText(DesignerLocalizer.instance().getLangString("update"));
		this.currentShift = shift;

		// name
		this.tfShiftName.setText(shift.getName());

		// description
		this.tfShiftDescription.setText(shift.getDescription());

		// start time
		LocalTime startTime = shift.getStart();
		this.tfShiftStart.setText(AppUtils.stringFromLocalTime(startTime, false));

		// duration
		Duration duration = shift.getDuration();
		this.tfShiftDuration.setText(AppUtils.stringFromDuration(duration, false));

		// breaks
		initBreakEditor(shift);
	}
	
	private void initBreakEditor(Shift shift) {
		tpBreaks.setDisable(false);
		
		List<Break> breaks = shift.getBreaks();
		breakList.clear();

		for (Break period : breaks) {
			breakList.add(period);
		}
		Collections.sort(breakList);
		tvBreaks.refresh();
		
		// clear editor
		onNewBreak();
	}

	// called on team selection in table listener
	private void onSelectTeam(Team team) {
		btAddTeam.setText(DesignerLocalizer.instance().getLangString("update"));
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
		btAddRotation.setText(DesignerLocalizer.instance().getLangString("update"));

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
		btAddRotationSegment.setText(DesignerLocalizer.instance().getLangString("update"));

		this.currentRotationSegment = segment;

		this.cbRotationSegmentShifts.setValue(segment.getStartingShift().getName());

		this.spDaysOn.getValueFactory().setValue(segment.getDaysOn());

		this.spDaysOff.getValueFactory().setValue(segment.getDaysOff());
	}

	// called on exception period selection in table listener
	private void onSelectExceptionPeriod(ExceptionPeriod period) {
		btAddExceptionPeriod.setText(DesignerLocalizer.instance().getLangString("update"));
		this.currentPeriod = period;

		// name
		this.tfPeriodName.setText(period.getName());

		// description
		this.tfPeriodDescription.setText(period.getDescription());

		// start
		this.dpPeriodStartDate.setValue(period.getStartDateTime().toLocalDate());
		LocalTime startTime = period.getStartDateTime().toLocalTime();
		this.tfPeriodStartTime.setText(AppUtils.stringFromLocalTime(startTime, false));

		// duration
		this.tfPeriodDuration.setText(AppUtils.stringFromDuration(period.getDuration(), false));

		// loss
		this.cbLosses.getSelectionModel().select(period.getLossCategory());
	}

	// called on break period selection in table listener
	private void onSelectBreak(Break period) {
		btAddBreak.setText(DesignerLocalizer.instance().getLangString("update"));
		this.currentBreak = period;

		// name
		this.tfBreakName.setText(period.getName());

		// description
		this.tfBreakDescription.setText(period.getDescription());

		// start
		this.tfBreakStart.setText(AppUtils.stringFromLocalTime(period.getStart(), false));

		// duration
		this.tfBreakDuration.setText(AppUtils.stringFromDuration(period.getDuration(), false));

		// loss
		this.cbBreakLosses.getSelectionModel().select(period.getLossCategory());
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
	protected void setImages() {
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

		// new break
		btNewBreak.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewBreak.setContentDisplay(ContentDisplay.LEFT);

		// add break
		btAddBreak.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddBreak.setContentDisplay(ContentDisplay.LEFT);

		// remove break
		btRemoveBreak.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveBreak.setContentDisplay(ContentDisplay.LEFT);

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
		btNewExceptionPeriod.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNewExceptionPeriod.setContentDisplay(ContentDisplay.LEFT);

		// add non-working period
		btAddExceptionPeriod.setGraphic(ImageManager.instance().getImageView(Images.ADD));
		btAddExceptionPeriod.setContentDisplay(ContentDisplay.LEFT);

		// remove non-working period
		btRemoveExceptionPeriod.setGraphic(ImageManager.instance().getImageView(Images.REMOVE));
		btRemoveExceptionPeriod.setContentDisplay(ContentDisplay.LEFT);

		// choose template schedule
		btChooseSchedule.setGraphic(ImageManager.instance().getImageView(Images.CHOOSE));
		btChooseSchedule.setContentDisplay(ContentDisplay.RIGHT);

		// view shift instances
		btViewShifts.setGraphic(ImageManager.instance().getImageView(Images.SHIFT));
		btViewShifts.setContentDisplay(ContentDisplay.RIGHT);

		// context menu
		miSaveAll.setGraphic(ImageManager.instance().getImageView(Images.SAVE_ALL));
		miRefreshAll.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));

		// backup
		btBackup.setGraphic(ImageManager.instance().getImageView(Images.BACKUP));
		btBackup.setContentDisplay(ContentDisplay.RIGHT);
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
		onNewExceptionPeriod();

		// clear the tables
		this.shiftList.clear();
		this.shiftNames.clear();
		this.tvShifts.refresh();

		this.teamList.clear();
		this.tvTeams.refresh();

		this.rotationList.clear();
		this.rotationNames.clear();
		this.tvRotations.refresh();

		this.rotationSegmentList.clear();
		this.tvRotationSegments.refresh();

		this.periodList.clear();
		this.tvExceptionPeriods.refresh();
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
				AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.schedule.selected"));
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
			TreeItem<ScheduleNode> selectedItem = tvSchedules.getSelectionModel().getSelectedItem();
			TreeItem<ScheduleNode> rootNode = tvSchedules.getRoot();
			rootNode.getChildren().remove(selectedItem);
			tvSchedules.refresh();

			onNewSchedule();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private void resetGraphic(TreeItem<ScheduleNode> scheduleItem) {
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
			initializeExceptionPeriodEditor();

			// display all defined work schedules
			displaySchedules();

			resetEditor();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private boolean setAttributes(TreeItem<ScheduleNode> scheduleItem) {
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

	private void addEditedSchedule(TreeItem<ScheduleNode> scheduleItem) {
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

	private void removeEditedSchedule(TreeItem<ScheduleNode> scheduleItem) {
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

			// re-display attributes
			onRefreshSchedule();

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
		
		// breaks
		breakList.clear();
		tvBreaks.refresh();

		// rotations
		List<Rotation> rotations = schedule.getRotations();
		rotationList.clear();
		rotationSegmentList.clear();
		rotationNames.clear();

		for (Rotation rotation : rotations) {
			rotationList.add(rotation);
			rotationNames.add(rotation.getName());
		}
		Collections.sort(rotationList);
		Collections.sort(rotationNames);
		tvRotations.refresh();

		// teams
		List<Team> teams = schedule.getTeams();

		teamList.clear();

		for (Team team : teams) {
			teamList.add(team);

			Rotation rotation = team.getRotation();

			if (rotation != null && rotation.getName() != null && !rotationList.contains(rotation)) {
				// add to table binding
				rotationList.add(rotation);

				// add to combobox of names
				rotationNames.add(rotation.getName());
			}
		}

		Collections.sort(teamList);
		tvTeams.refresh();

		// exception periods
		List<ExceptionPeriod> periods = schedule.getExceptionPeriods();
		periodList.clear();

		for (ExceptionPeriod period : periods) {
			periodList.add(period);
		}
		Collections.sort(periodList);

		tvExceptionPeriods.refresh();

	}

	// new shift button clicked
	@FXML
	private void onNewShift() {
		btAddShift.setText(DesignerLocalizer.instance().getLangString("add"));
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
		btAddTeam.setText(DesignerLocalizer.instance().getLangString("add"));
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
				throw new Exception(DesignerLocalizer.instance().getErrorString("schedule.before.shift"));
			}

			// name
			String name = this.tfShiftName.getText().trim();

			if (name == null || name.length() == 0) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.shift.name"));
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

			// check for shift reference
			PersistenceService.instance().checkReferences(shift);

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

	// new break clicked
	@FXML
	private void onNewBreak() {
		btAddBreak.setText(DesignerLocalizer.instance().getLangString("add"));
		this.currentBreak = null;

		// break editing attributes
		this.tfBreakName.clear();
		this.tfBreakDescription.clear();
		this.tfBreakStart.clear();
		this.tfBreakDuration.clear();
		this.cbBreakLosses.getSelectionModel().clearSelection();
		this.cbBreakLosses.getSelectionModel().select(null);

		this.tvBreaks.getSelectionModel().clearSelection();
	}

	// add break button clicked
	@FXML
	private void onAddBreak() {
		try {
			// need a shift first
			if (currentShift == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("shift.before.break"));
			}

			// name
			String name = this.tfBreakName.getText().trim();

			if (name == null || name.length() == 0) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.break.name"));
			}

			// description
			String description = this.tfBreakDescription.getText().trim();

			// start time of day
			String start = this.tfBreakStart.getText().trim();
			LocalTime startTime = AppUtils.localTimeFromString(start);

			// duration
			String hrmm = this.tfBreakDuration.getText().trim();
			Duration duration = AppUtils.durationFromString(hrmm);

			// loss category
			TimeLoss loss = this.cbBreakLosses.getSelectionModel().getSelectedItem();

			if (currentBreak == null) {
				// new break
				currentBreak = currentShift.createBreak(name, description, startTime, duration);
				currentBreak.setLossCategory(loss);
				breakList.add(currentBreak);
			} else {
				currentBreak.setName(name);
				currentBreak.setDescription(description);
				currentBreak.setStart(startTime);
				currentBreak.setDuration(duration);
				currentBreak.setLossCategory(loss);
			}
			Collections.sort(breakList);

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvBreaks.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// remove break button clicked
	@FXML
	private void onRemoveBreak() {
		try {
			Break period = this.tvBreaks.getSelectionModel().getSelectedItem();

			if (period == null) {
				return;
			}

			currentShift.getBreaks().remove(period);
			breakList.remove(period);
			Collections.sort(breakList);
			currentBreak = null;
			tvBreaks.getSelectionModel().clearSelection();

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvBreaks.refresh();
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
				throw new Exception(DesignerLocalizer.instance().getErrorString("schedule.before.team"));
			}

			// name
			String name = this.tfTeamName.getText().trim();

			if (name == null || name.length() == 0) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.team.name"));
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

			// check for team reference
			PersistenceService.instance().checkReferences(team);

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
				throw new Exception(DesignerLocalizer.instance().getErrorString("schedule.before.rotation"));
			}

			// name
			String name = this.tfRotationName.getText().trim();

			// description
			String description = this.tfRotationDescription.getText().trim();

			if (currentRotation == null) {
				// new rotation
				currentRotation = getSelectedSchedule().createRotation(name, description);
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
		btAddRotation.setText(DesignerLocalizer.instance().getLangString("add"));
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
			Rotation rotation = tvRotations.getSelectionModel().getSelectedItem();

			if (rotation == null) {
				return;
			}

			// check for team reference
			PersistenceService.instance().checkReferences(rotation);

			// remove from list
			rotationList.remove(rotation);

			// remove from work schedule
			getSelectedSchedule().getRotations().remove(rotation);

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
		btAddRotationSegment.setText(DesignerLocalizer.instance().getLangString("add"));
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
				throw new Exception(DesignerLocalizer.instance().getErrorString("schedule.before.segment"));
			}

			if (currentRotation == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("rotation.before.segment"));
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
				throw new Exception(DesignerLocalizer.instance().getErrorString("shift.not.found", shiftName));
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
			RotationSegment segment = tvRotationSegments.getSelectionModel().getSelectedItem();

			if (segment == null) {
				return;
			}

			rotationSegmentList.remove(segment);

			// remove from rotation
			currentRotation.getRotationSegments().remove(segment);

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

	// new exception button clicked
	@FXML
	private void onNewExceptionPeriod() {
		btAddExceptionPeriod.setText(DesignerLocalizer.instance().getLangString("add"));
		this.currentPeriod = null;

		// exception period editing attributes
		this.tfPeriodName.clear();
		this.tfPeriodDescription.clear();
		this.dpPeriodStartDate.setValue(null);
		this.tfPeriodStartTime.clear();
		this.tfPeriodDuration.clear();
		this.cbLosses.getSelectionModel().clearSelection();
		this.cbLosses.getSelectionModel().select(null);

		this.tvExceptionPeriods.getSelectionModel().clearSelection();
	}

	// add exception button clicked
	@FXML
	private void onAddExceptionPeriod() {
		try {
			// need a work schedule first
			if (getSelectedSchedule() == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("schedule.before.non"));
			}

			// name
			String name = this.tfPeriodName.getText().trim();

			if (name == null || name.length() == 0) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("no.non.name"));
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
				currentPeriod = getSelectedSchedule().createExceptionPeriod(name, description, startDateTime, duration,
						loss);
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

			tvExceptionPeriods.refresh();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// remove exception period button clicked
	@FXML
	private void onRemoveExceptionPeriod() {
		try {
			ExceptionPeriod period = this.tvExceptionPeriods.getSelectionModel().getSelectedItem();

			if (period == null) {
				return;
			}

			getSelectedSchedule().getExceptionPeriods().remove(period);
			periodList.remove(period);
			Collections.sort(periodList);
			currentPeriod = null;
			tvExceptionPeriods.getSelectionModel().clearSelection();

			// add to edited schedules
			addEditedSchedule(selectedScheduleItem);

			tvExceptionPeriods.refresh();
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
	private void onBackup() {
		backupToFile(WorkSchedule.class);
	}

	@FXML
	private void onChooseSchedule() {
		try {
			if (templateController == null) {
				FXMLLoader loader = FXMLLoaderFactory.templateScheduleLoader();
				AnchorPane page = (AnchorPane) loader.getRoot();

				// Create the dialog Stage.
				Stage dialogStage = new Stage(StageStyle.DECORATED);
				dialogStage.setTitle(DesignerLocalizer.instance().getLangString("template.schedule"));
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

	@FXML
	private void onViewShifts() {
		try {
			if (getSelectedSchedule() == null) {
				throw new Exception(DesignerLocalizer.instance().getErrorString("choose.schedule"));
			}

			if (shiftsController == null) {
				FXMLLoader loader = FXMLLoaderFactory.scheduleShiftsLoader();
				AnchorPane page = (AnchorPane) loader.getRoot();

				Stage dialogStage = new Stage(StageStyle.DECORATED);
				dialogStage.setTitle("Work Schedule Shift Instances");
				dialogStage.initModality(Modality.NONE);
				Scene scene = new Scene(page);
				dialogStage.setScene(scene);

				// get the controller
				shiftsController = loader.getController();
				shiftsController.setDialogStage(dialogStage);
				shiftsController.initializeApp(getApp());
			}

			shiftsController.setCurrentSchedule(getSelectedSchedule());

			if (!shiftsController.getDialogStage().isShowing()) {
				shiftsController.getDialogStage().showAndWait();
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
