package org.point85.app.schedule;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import org.point85.app.designer.DesignerDialogController;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.schedule.Rotation;
import org.point85.domain.schedule.Shift;
import org.point85.domain.schedule.WorkSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

public class TemplateScheduleDialogController extends DesignerDialogController {
	// logger
	private static final Logger logger = LoggerFactory.getLogger(TemplateScheduleDialogController.class);

	// work schedule
	private WorkSchedule selectedSchedule;

	// list of schedules
	private final ObservableList<WorkSchedule> scheduleList = FXCollections.observableArrayList(new ArrayList<>());

	@FXML
	private TableView<WorkSchedule> tvTemplates;

	@FXML
	private TableColumn<WorkSchedule, String> tcName;

	@FXML
	private TableColumn<WorkSchedule, String> tcDescription;

	@FXML
	private TableColumn<WorkSchedule, Integer> tcShifts;

	@FXML
	private TableColumn<WorkSchedule, Integer> tcTeams;

	@FXML
	private TableColumn<WorkSchedule, Long> tcRotations;

	@FXML
	public void initialize() throws Exception {
		setImages();

		tvTemplates.setItems(scheduleList);

		// name
		tcName.setCellValueFactory(cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getName()));

		// description
		tcDescription.setCellValueFactory(
				cellDataFeatures -> new SimpleStringProperty(cellDataFeatures.getValue().getDescription()));

		tcDescription.setCellFactory(tablecol -> {
			TableCell<WorkSchedule, String> cell = new TableCell<>();
			Text text = new Text();
			cell.setGraphic(text);
			text.wrappingWidthProperty().bind(cell.widthProperty());
			text.textProperty().bind(cell.itemProperty());
			return cell;
		});

		// shifts
		tcShifts.setCellValueFactory(
				cellDataFeatures -> new SimpleObjectProperty<Integer>(cellDataFeatures.getValue().getShifts().size()));

		// teams
		tcTeams.setCellValueFactory(
				cellDataFeatures -> new SimpleObjectProperty<Integer>(cellDataFeatures.getValue().getTeams().size()));

		// rotation days
		tcRotations.setCellValueFactory(cellDataFeatures -> {
			SimpleObjectProperty<Long> days = null;
			try {
				Duration duration = cellDataFeatures.getValue().getTeams().get(0).getRotationDuration();
				days = new SimpleObjectProperty<>(duration.toDays());
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			return days;
		});

		// template schedules
		createWorkSchedules();
	}

	public WorkSchedule getSelectedSchedule() {
		return this.selectedSchedule;
	}

	@FXML
	@Override
	protected void onOK() {
		selectedSchedule = tvTemplates.getSelectionModel().getSelectedItem();
		super.onOK();
	}

	@FXML
	@Override
	protected void onCancel() {
		selectedSchedule = null;
		super.onCancel();
	}

	private void createWorkSchedules() throws Exception {
		scheduleList.add(createNursingICU());
		scheduleList.add(createPostalService());
		scheduleList.add(createSeattleFire());
		scheduleList.add(createKernFire());
		scheduleList.add(createManufacturing());
		scheduleList.add(createGeneric());
		scheduleList.add(createLowNight());
		scheduleList.add(create3TeamFixed24());
		scheduleList.add(create549());
		scheduleList.add(create9to5());
		scheduleList.add(create8Plus12());
		scheduleList.add(createICUInterns());
		scheduleList.add(createDupont());
		scheduleList.add(createDNO());
		scheduleList.add(create21TeamFixed());
		scheduleList.add(createTwoTeam());
		scheduleList.add(createPanama());
	}

	private WorkSchedule createPanama() throws Exception {
		String description = "This is a slow rotation plan that uses 4 teams and two 12-hr shifts to provide 24/7 coverage. "
				+ "The working and non-working days follow this pattern: 2 days on, 2 days off, 3 days on, 2 days off, 2 days on, 3 days off. "
				+ "Each team works the same shift (day or night) for 28 days then switches over to the other shift for the next 28 days. "
				+ "After 56 days, the same sequence starts over.";

		WorkSchedule schedule = new WorkSchedule("Panama", description);

		// Day shift, starts at 07:00 for 12 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(7, 0, 0), Duration.ofHours(12));

		// Night shift, starts at 19:00 for 12 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(19, 0, 0), Duration.ofHours(12));

		// rotation
		Rotation rotation = schedule.createRotation("Panama",
				"2 days on, 2 days off, 3 days on, 2 days off, 2 days on, 3 days off");
		// 2 days on, 2 off, 3 on, 2 off, 2 on, 3 off (and repeat)
		rotation.addSegment(day, 2, 2);
		rotation.addSegment(day, 3, 2);
		rotation.addSegment(day, 2, 3);
		rotation.addSegment(day, 2, 2);
		rotation.addSegment(day, 3, 2);
		rotation.addSegment(day, 2, 3);

		// 2 nights on, 2 off, 3 on, 2 off, 2 on, 3 off (and repeat)
		rotation.addSegment(night, 2, 2);
		rotation.addSegment(night, 3, 2);
		rotation.addSegment(night, 2, 3);
		rotation.addSegment(night, 2, 2);
		rotation.addSegment(night, 3, 2);
		rotation.addSegment(night, 2, 3);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		schedule.createTeam("Team 1", "First team", rotation, referenceDate);
		schedule.createTeam("Team 2", "Second team", rotation, referenceDate.minusDays(28));
		schedule.createTeam("Team 3", "Third team", rotation, referenceDate.minusDays(7));
		schedule.createTeam("Team 4", "Fourth team", rotation, referenceDate.minusDays(35));

		return schedule;
	}

	private WorkSchedule createTwoTeam() throws Exception {
		String description = "This is a fixed (no rotation) plan that uses 2 teams and two 12-hr shifts to provide 24/7 coverage. "
				+ "One team will be permanently on the day shift and the other will be on the night shift.";

		WorkSchedule schedule = new WorkSchedule("2 Team Fixed 12 Plan", description);

		// Day shift, starts at 07:00 for 12 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(7, 0, 0), Duration.ofHours(12));

		// Night shift, starts at 19:00 for 12 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(19, 0, 0), Duration.ofHours(12));

		// Team1 rotation
		Rotation team1Rotation = schedule.createRotation("Team1", "Team1");
		team1Rotation.addSegment(day, 1, 0);

		// Team1 rotation
		Rotation team2Rotation = schedule.createRotation("Team2", "Team2");
		team2Rotation.addSegment(night, 1, 0);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		schedule.createTeam("Team 1", "First team", team1Rotation, referenceDate);
		schedule.createTeam("Team 2", "Second team", team2Rotation, referenceDate);

		return schedule;
	}

	private WorkSchedule create21TeamFixed() throws Exception {
		String description = "This plan is a fixed (no rotation) plan that uses 21 teams and three 8-hr shifts to provide 24/7 coverage. "
				+ "It maximizes the number of consecutive days off while still averaging 40 hours per week. "
				+ "Over a 7 week cycle, each employee has two 3 consecutive days off and is required to work 6 consecutive days on 5 of the 7 weeks. "
				+ "On any given day, 15 teams will be scheduled to work and 6 teams will be off. "
				+ "Each shift will be staffed by 5 teams so the minimum number of employees per shift is five. ";

		WorkSchedule schedule = new WorkSchedule("21 Team Fixed 8 6D Plan", description);

		// Day shift, starts at 07:00 for 8 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(7, 0, 0), Duration.ofHours(8));

		// Swing shift, starts at 15:00 for 8 hours
		Shift swing = schedule.createShift("Swing", "Swing shift", LocalTime.of(15, 0, 0), Duration.ofHours(8));

		// Night shift, starts at 15:00 for 8 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(23, 0, 0), Duration.ofHours(8));

		// day rotation
		Rotation dayRotation = schedule.createRotation("Day", "Day");
		dayRotation.addSegment(day, 6, 3);
		dayRotation.addSegment(day, 5, 3);
		dayRotation.addSegment(day, 6, 2);
		dayRotation.addSegment(day, 6, 2);
		dayRotation.addSegment(day, 6, 2);
		dayRotation.addSegment(day, 6, 2);

		// swing rotation
		Rotation swingRotation = schedule.createRotation("Swing", "Swing");
		swingRotation.addSegment(swing, 6, 3);
		swingRotation.addSegment(swing, 5, 3);
		swingRotation.addSegment(swing, 6, 2);
		swingRotation.addSegment(swing, 6, 2);
		swingRotation.addSegment(swing, 6, 2);
		swingRotation.addSegment(swing, 6, 2);

		// night rotation
		Rotation nightRotation = schedule.createRotation("Night", "Night");
		nightRotation.addSegment(night, 6, 3);
		nightRotation.addSegment(night, 5, 3);
		nightRotation.addSegment(night, 6, 2);
		nightRotation.addSegment(night, 6, 2);
		nightRotation.addSegment(night, 6, 2);
		nightRotation.addSegment(night, 6, 2);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		// day teams
		schedule.createTeam("Team 1", "1st day team", dayRotation, referenceDate);
		schedule.createTeam("Team 2", "2nd day team", dayRotation, referenceDate.plusDays(7));
		schedule.createTeam("Team 3", "3rd day team", dayRotation, referenceDate.plusDays(14));
		schedule.createTeam("Team 4", "4th day team", dayRotation, referenceDate.plusDays(21));
		schedule.createTeam("Team 5", "5th day team", dayRotation, referenceDate.plusDays(28));
		schedule.createTeam("Team 6", "6th day team", dayRotation, referenceDate.plusDays(35));
		schedule.createTeam("Team 7", "7th day team", dayRotation, referenceDate.plusDays(42));

		// swing teams
		schedule.createTeam("Team 8", "1st swing team", swingRotation, referenceDate);
		schedule.createTeam("Team 9", "2nd swing team", swingRotation, referenceDate.plusDays(7));
		schedule.createTeam("Team 10", "3rd swing team", swingRotation, referenceDate.plusDays(14));
		schedule.createTeam("Team 11", "4th swing team", swingRotation, referenceDate.plusDays(21));
		schedule.createTeam("Team 12", "5th swing team", swingRotation, referenceDate.plusDays(28));
		schedule.createTeam("Team 13", "6th swing team", swingRotation, referenceDate.plusDays(35));
		schedule.createTeam("Team 14", "7th swing team", swingRotation, referenceDate.plusDays(42));

		// night teams
		schedule.createTeam("Team 15", "1st night team", nightRotation, referenceDate);
		schedule.createTeam("Team 16", "2nd night team", nightRotation, referenceDate.plusDays(7));
		schedule.createTeam("Team 17", "3rd night team", nightRotation, referenceDate.plusDays(14));
		schedule.createTeam("Team 18", "4th night team", nightRotation, referenceDate.plusDays(21));
		schedule.createTeam("Team 19", "5th night team", nightRotation, referenceDate.plusDays(28));
		schedule.createTeam("Team 20", "6th night team", nightRotation, referenceDate.plusDays(35));
		schedule.createTeam("Team 21", "7th night team", nightRotation, referenceDate.plusDays(42));

		return schedule;
	}

	private WorkSchedule createDNO() throws Exception {
		String description = "This is a fast rotation plan that uses 3 teams and two 12-hr shifts to provide 24/7 coverage. "
				+ "Each team rotates through the following sequence every three days: 1 day shift, 1 night shift, and 1 day off.";

		WorkSchedule schedule = new WorkSchedule("DNO Plan", description);

		// Day shift, starts at 07:00 for 12 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(7, 0, 0), Duration.ofHours(12));

		// Night shift, starts at 19:00 for 12 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(19, 0, 0), Duration.ofHours(12));

		// rotation
		Rotation rotation = schedule.createRotation("DNO", "DNO");
		rotation.addSegment(day, 1, 0);
		rotation.addSegment(night, 1, 1);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		schedule.createTeam("Team 1", "First team", rotation, referenceDate);
		schedule.createTeam("Team 2", "Second team", rotation, referenceDate.minusDays(1));
		schedule.createTeam("Team 3", "Third team", rotation, referenceDate.minusDays(2));

		return schedule;
	}

	private WorkSchedule createDupont() throws Exception {
		String description = "The DuPont 12-hour rotating shift schedule uses 4 teams (crews) and 2 twelve-hour shifts to provide 24/7 coverage. "
				+ "It consists of a 4-week cycle where each team works 4 consecutive night shifts, "
				+ "followed by 3 days off duty, works 3 consecutive day shifts, followed by 1 day off duty, works 3 consecutive night shifts, "
				+ "followed by 3 days off duty, work 4 consecutive day shift, then have 7 consecutive days off duty. "
				+ "Personnel works an average 42 hours per week.";

		WorkSchedule schedule = new WorkSchedule("DuPont", description);

		// Day shift, starts at 07:00 for 12 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(7, 0, 0), Duration.ofHours(12));

		// Night shift, starts at 19:00 for 12 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(19, 0, 0), Duration.ofHours(12));

		// Team1 rotation
		Rotation rotation = schedule.createRotation("DuPont", "DuPont");
		rotation.addSegment(night, 4, 3);
		rotation.addSegment(day, 3, 1);
		rotation.addSegment(night, 3, 3);
		rotation.addSegment(day, 4, 7);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		schedule.createTeam("Team 1", "First team", rotation, referenceDate);
		schedule.createTeam("Team 2", "Second team", rotation, referenceDate.minusDays(7));
		schedule.createTeam("Team 3", "Third team", rotation, referenceDate.minusDays(14));
		schedule.createTeam("Team 4", "Forth team", rotation, referenceDate.minusDays(21));

		return schedule;
	}

	private WorkSchedule createICUInterns() throws Exception {
		String description = "This plan supports a combination of 14-hr day shift , 15.5-hr cross-cover shift , and a 14-hr night shift for medical interns. "
				+ "The day shift and the cross-cover shift have the same start time (7:00AM). "
				+ "The night shift starts at around 10:00PM and ends at 12:00PM on the next day.";

		WorkSchedule schedule = new WorkSchedule("ICU Interns Plan", description);

		// Day shift #1, starts at 07:00 for 15.5 hours
		Shift crossover = schedule.createShift("Crossover", "Day shift #1 cross-over", LocalTime.of(7, 0, 0),
				Duration.ofHours(15).plusMinutes(30));

		// Day shift #2, starts at 07:00 for 14 hours
		Shift day = schedule.createShift("Day", "Day shift #2", LocalTime.of(7, 0, 0), Duration.ofHours(14));

		// Night shift, starts at 22:00 for 14 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(22, 0, 0), Duration.ofHours(14));

		// Team1 rotation
		Rotation rotation = schedule.createRotation("ICU", "ICU");
		rotation.addSegment(day, 1, 0);
		rotation.addSegment(crossover, 1, 0);
		rotation.addSegment(night, 1, 1);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		schedule.createTeam("Team 1", "First team", rotation, referenceDate);
		schedule.createTeam("Team 2", "Second team", rotation, referenceDate.minusDays(3));
		schedule.createTeam("Team 3", "Third team", rotation, referenceDate.minusDays(2));
		schedule.createTeam("Team 4", "Forth team", rotation, referenceDate.minusDays(1));

		return schedule;
	}

	private WorkSchedule create8Plus12() throws Exception {
		// work schedule
		WorkSchedule schedule = new WorkSchedule("8 Plus 12 Plan",
				"This is a fast rotation plan that uses 4 teams and a combination of three 8-hr shifts on weekdays "
						+ "and two 12-hr shifts on weekends to provide 24/7 coverage.");

		// Day shift #1, starts at 07:00 for 12 hours
		Shift day1 = schedule.createShift("Day1", "Day shift #1", LocalTime.of(7, 0, 0), Duration.ofHours(12));

		// Day shift #2, starts at 07:00 for 8 hours
		Shift day2 = schedule.createShift("Day2", "Day shift #2", LocalTime.of(7, 0, 0), Duration.ofHours(8));

		// Swing shift, starts at 15:00 for 8 hours
		Shift swing = schedule.createShift("Swing", "Swing shift", LocalTime.of(15, 0, 0), Duration.ofHours(8));

		// Night shift #1, starts at 19:00 for 12 hours
		Shift night1 = schedule.createShift("Night1", "Night shift #1", LocalTime.of(19, 0, 0), Duration.ofHours(12));

		// Night shift #2, starts at 23:00 for 8 hours
		Shift night2 = schedule.createShift("Night2", "Night shift #2", LocalTime.of(23, 0, 0), Duration.ofHours(8));

		// shift rotation (28 days)
		Rotation rotation = schedule.createRotation("8 Plus 12", "8 Plus 12");
		rotation.addSegment(day2, 5, 0);
		rotation.addSegment(day1, 2, 3);
		rotation.addSegment(night2, 2, 0);
		rotation.addSegment(night1, 2, 0);
		rotation.addSegment(night2, 3, 4);
		rotation.addSegment(swing, 5, 2);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		// 4 teams, rotating through 5 shifts
		schedule.createTeam("Team 1", "First team", rotation, referenceDate);
		schedule.createTeam("Team 2", "Second team", rotation, referenceDate.minusDays(7));
		schedule.createTeam("Team 3", "Third team", rotation, referenceDate.minusDays(14));
		schedule.createTeam("Team 4", "Fourth team", rotation, referenceDate.minusDays(21));

		return schedule;
	}

	private WorkSchedule create9to5() throws Exception {
		WorkSchedule schedule = new WorkSchedule("9 To 5 Plan",
				"This is the basic 9 to 5 schedule plan for office employees. Every employee works 8 hrs a day from Monday to Friday.");

		// Shift starts at 09:00 for 8 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(9, 0, 0), Duration.ofHours(8));

		// Team1 rotation (5 days)
		Rotation rotation = schedule.createRotation("9 To 5 ", "9 To 5 ");
		rotation.addSegment(day, 5, 2);

		// 1 team, 1 shift
		schedule.createTeam("Team", "One team", rotation, LocalDate.of(2016, 10, 31));

		return schedule;
	}

	private WorkSchedule create549() throws Exception {
		WorkSchedule schedule = new WorkSchedule("5/4/9 Plan", "Compressed work schedule.");

		// Shift 1 starts at 07:00 for 9 hours
		Shift day1 = schedule.createShift("Day1", "Day shift #1", LocalTime.of(7, 0, 0), Duration.ofHours(9));

		// Shift 2 starts at 07:00 for 8 hours
		Shift day2 = schedule.createShift("Day2", "Day shift #2", LocalTime.of(7, 0, 0), Duration.ofHours(8));

		// Team rotation (28 days)
		Rotation rotation = schedule.createRotation("5/4/9 ", "5/4/9 ");
		rotation.addSegment(day1, 4, 0);
		rotation.addSegment(day2, 1, 3);
		rotation.addSegment(day1, 4, 3);
		rotation.addSegment(day1, 4, 2);
		rotation.addSegment(day1, 4, 0);
		rotation.addSegment(day2, 1, 2);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		// 2 teams
		schedule.createTeam("Team1", "First team", rotation, referenceDate);
		schedule.createTeam("Team2", "Second team", rotation, referenceDate.minusDays(14));

		return schedule;
	}

	private WorkSchedule create3TeamFixed24() throws Exception {
		WorkSchedule schedule = new WorkSchedule("3 Team Fixed 24 Plan", "Fire departments");

		// Shift starts at 00:00 for 24 hours
		Shift shift = schedule.createShift("24 Hour", "24 hour shift", LocalTime.of(0, 0, 0), Duration.ofHours(24));

		// Team rotation
		Rotation rotation = schedule.createRotation("3 Team Fixed 24 Plan", "3 Team Fixed 24 Plan");
		rotation.addSegment(shift, 1, 1);
		rotation.addSegment(shift, 1, 1);
		rotation.addSegment(shift, 1, 4);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		// 3 teams
		schedule.createTeam("Team1", "First team", rotation, referenceDate);
		schedule.createTeam("Team2", "Second team", rotation, referenceDate.minusDays(3));
		schedule.createTeam("Team3", "Third team", rotation, referenceDate.minusDays(6));

		return schedule;
	}

	private WorkSchedule createLowNight() throws Exception {
		WorkSchedule schedule = new WorkSchedule("Low Night Demand Plan", "Low night demand");

		// 3 shifts
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(7, 0, 0), Duration.ofHours(8));
		Shift swing = schedule.createShift("Swing", "Swing shift", LocalTime.of(15, 0, 0), Duration.ofHours(8));
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(23, 0, 0), Duration.ofHours(8));

		// Team rotation
		Rotation rotation = schedule.createRotation("Low night demand", "Low night demand");
		rotation.addSegment(day, 3, 0);
		rotation.addSegment(swing, 4, 3);
		rotation.addSegment(day, 4, 0);
		rotation.addSegment(swing, 3, 4);
		rotation.addSegment(day, 3, 0);
		rotation.addSegment(night, 4, 3);
		rotation.addSegment(day, 4, 0);
		rotation.addSegment(night, 3, 4);

		// reference date for start of shift rotations
		LocalDate referenceDate = LocalDate.of(2016, 10, 31);

		// 6 teams
		schedule.createTeam("Team1", "First team", rotation, referenceDate);
		schedule.createTeam("Team2", "Second team", rotation, referenceDate.minusDays(21));
		schedule.createTeam("Team3", "Third team", rotation, referenceDate.minusDays(7));
		schedule.createTeam("Team4", "Fourth team", rotation, referenceDate.minusDays(28));
		schedule.createTeam("Team5", "Fifth team", rotation, referenceDate.minusDays(14));
		schedule.createTeam("Team6", "Sixth team", rotation, referenceDate.minusDays(35));
		return schedule;
	}

	private WorkSchedule createGeneric() throws Exception {
		// regular work week with holidays and breaks
		WorkSchedule schedule = new WorkSchedule("Generic", "Regular 40 hour work week, two teams.");

		// holidays
		schedule.createExceptionPeriod("MEMORIAL DAY", "Memorial day", LocalDateTime.of(2016, 5, 30, 0, 0, 0),
				Duration.ofHours(24), TimeLoss.NOT_SCHEDULED);
		schedule.createExceptionPeriod("INDEPENDENCE DAY", "Independence day", LocalDateTime.of(2016, 7, 4, 0, 0, 0),
				Duration.ofHours(24), TimeLoss.NOT_SCHEDULED);
		schedule.createExceptionPeriod("LABOR DAY", "Labor day", LocalDateTime.of(2016, 9, 5, 0, 0, 0),
				Duration.ofHours(24), TimeLoss.NOT_SCHEDULED);
		schedule.createExceptionPeriod("THANKSGIVING", "Thanksgiving day and day after",
				LocalDateTime.of(2016, 11, 24, 0, 0, 0), Duration.ofHours(48), TimeLoss.NOT_SCHEDULED);
		schedule.createExceptionPeriod("CHRISTMAS SHUTDOWN", "Christmas week scheduled maintenance",
				LocalDateTime.of(2016, 12, 25, 0, 30, 0), Duration.ofHours(168), TimeLoss.NOT_SCHEDULED);

		// each shift duration
		Duration shiftDuration = Duration.ofHours(8);
		LocalTime shift1Start = LocalTime.of(7, 0, 0);
		LocalTime shift2Start = LocalTime.of(15, 0, 0);

		// shift 1
		Shift shift1 = schedule.createShift("Shift1", "Shift #1", shift1Start, shiftDuration);

		// breaks
		shift1.createBreak("10AM", "10 am break", LocalTime.of(10, 0, 0), Duration.ofMinutes(15));
		shift1.createBreak("LUNCH", "lunch", LocalTime.of(12, 0, 0), Duration.ofHours(1));
		shift1.createBreak("2PM", "2 pm break", LocalTime.of(14, 0, 0), Duration.ofMinutes(15));

		// shift 2
		Shift shift2 = schedule.createShift("Shift2", "Shift #2", shift2Start, shiftDuration);

		// shift 1, 5 days ON, 2 OFF
		Rotation rotation1 = schedule.createRotation("Shift1", "Shift1");
		rotation1.addSegment(shift1, 5, 2);

		// shift 2, 5 days ON, 2 OFF
		Rotation rotation2 = schedule.createRotation("Shift2", "Shift2");
		rotation2.addSegment(shift2, 5, 2);

		LocalDate startRotation = LocalDate.of(2016, 1, 1);
		schedule.createTeam("Team1", "Team #1", rotation1, startRotation);
		schedule.createTeam("Team2", "Team #2", rotation2, startRotation);

		return schedule;
	}

	private WorkSchedule createManufacturing() throws Exception {
		// manufacturing company
		WorkSchedule schedule = new WorkSchedule("Manufacturing Company", "Four 12 hour alternating day/night shifts");

		// day shift, start at 07:00 for 12 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(7, 0, 0), Duration.ofHours(12));

		// night shift, start at 19:00 for 12 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(19, 0, 0), Duration.ofHours(12));

		// 7 days ON, 7 OFF
		Rotation dayRotation = schedule.createRotation("Day", "Day");
		dayRotation.addSegment(day, 7, 7);

		// 7 nights ON, 7 OFF
		Rotation nightRotation = schedule.createRotation("Night", "Night");
		nightRotation.addSegment(night, 7, 7);

		schedule.createTeam("A", "A day shift", dayRotation, LocalDate.of(2014, 1, 2));
		schedule.createTeam("B", "B night shift", nightRotation, LocalDate.of(2014, 1, 2));
		schedule.createTeam("C", "C day shift", dayRotation, LocalDate.of(2014, 1, 9));
		schedule.createTeam("D", "D night shift", nightRotation, LocalDate.of(2014, 1, 9));

		return schedule;
	}

	private WorkSchedule createKernFire() throws Exception {
		// Kern Co, CA
		WorkSchedule schedule = new WorkSchedule("Kern Co.", "Three 24 hour alternating shifts");

		// shift, start 07:00 for 24 hours
		Shift shift = schedule.createShift("24 Hour", "24 hour shift", LocalTime.of(7, 0, 0), Duration.ofHours(24));

		// 2 days ON, 2 OFF, 2 ON, 2 OFF, 2 ON, 8 OFF
		Rotation rotation = schedule.createRotation("24 Hour", "2 days ON, 2 OFF, 2 ON, 2 OFF, 2 ON, 8 OFF");
		rotation.addSegment(shift, 2, 2);
		rotation.addSegment(shift, 2, 2);
		rotation.addSegment(shift, 2, 8);

		schedule.createTeam("Red", "A Shift", rotation, LocalDate.of(2017, 1, 8));
		schedule.createTeam("Black", "B Shift", rotation, LocalDate.of(2017, 2, 1));
		schedule.createTeam("Green", "C Shift", rotation, LocalDate.of(2017, 1, 2));

		return schedule;
	}

	private WorkSchedule createNursingICU() throws Exception {
		// ER nursing schedule
		WorkSchedule schedule = new WorkSchedule("Nursing ICU",
				"Two 12 hr back-to-back shifts, rotating every 14 days");

		// day shift, starts at 06:00 for 12 hours
		Shift day = schedule.createShift("Day", "Day shift", LocalTime.of(6, 0, 0), Duration.ofHours(12));

		// night shift, starts at 18:00 for 12 hours
		Shift night = schedule.createShift("Night", "Night shift", LocalTime.of(18, 0, 0), Duration.ofHours(12));

		// day rotation
		Rotation dayRotation = schedule.createRotation("Day", "Day");
		dayRotation.addSegment(day, 3, 4);
		dayRotation.addSegment(day, 4, 3);

		// inverse day rotation (day + 3 days)
		Rotation inverseDayRotation = schedule.createRotation("Inverse Day", "Inverse Day");
		inverseDayRotation.addSegment(day, 0, 3);
		inverseDayRotation.addSegment(day, 4, 4);
		inverseDayRotation.addSegment(day, 3, 0);

		// night rotation
		Rotation nightRotation = schedule.createRotation("Night", "Night");
		nightRotation.addSegment(night, 4, 3);
		nightRotation.addSegment(night, 3, 4);

		// inverse night rotation
		Rotation inverseNightRotation = schedule.createRotation("Inverse Night", "Inverse Night");
		inverseNightRotation.addSegment(night, 0, 4);
		inverseNightRotation.addSegment(night, 3, 3);
		inverseNightRotation.addSegment(night, 4, 0);

		LocalDate rotationStart = LocalDate.of(2014, 1, 6);

		schedule.createTeam("A", "Day shift", dayRotation, rotationStart);
		schedule.createTeam("B", "Day inverse shift", inverseDayRotation, rotationStart);
		schedule.createTeam("C", "Night shift", nightRotation, rotationStart);
		schedule.createTeam("D", "Night inverse shift", inverseNightRotation, rotationStart);

		return schedule;
	}

	private WorkSchedule createPostalService() throws Exception {
		// United States Postal Service
		WorkSchedule schedule = new WorkSchedule("USPS", "Six 9 hr shifts, rotating every 42 days");

		// shift, start at 08:00 for 9 hours
		Shift day = schedule.createShift("Day", "day shift", LocalTime.of(8, 0, 0), Duration.ofHours(9));

		Rotation rotation = schedule.createRotation("Day", "Day");
		rotation.addSegment(day, 3, 7);
		rotation.addSegment(day, 1, 7);
		rotation.addSegment(day, 1, 7);
		rotation.addSegment(day, 1, 7);
		rotation.addSegment(day, 1, 7);

		LocalDate rotationStart = LocalDate.of(2017, 1, 27);

		// day teams
		schedule.createTeam("Team A", "A team", rotation, rotationStart);
		schedule.createTeam("Team B", "B team", rotation, rotationStart.minusDays(7));
		schedule.createTeam("Team C", "C team", rotation, rotationStart.minusDays(14));
		schedule.createTeam("Team D", "D team", rotation, rotationStart.minusDays(21));
		schedule.createTeam("Team E", "E team", rotation, rotationStart.minusDays(28));
		schedule.createTeam("Team F", "F team", rotation, rotationStart.minusDays(35));

		return schedule;
	}

	private WorkSchedule createSeattleFire() throws Exception {
		// Seattle, WA fire shifts
		WorkSchedule schedule = new WorkSchedule("Seattle", "Four 24 hour alternating shifts");

		// shift, start at 07:00 for 24 hours
		Shift shift = schedule.createShift("24 Hours", "24 hour shift", LocalTime.of(7, 0, 0), Duration.ofHours(24));

		// 1 day ON, 4 OFF, 1 ON, 2 OFF
		Rotation rotation = schedule.createRotation("24 Hours", "24 Hours");
		rotation.addSegment(shift, 1, 4);
		rotation.addSegment(shift, 1, 2);

		schedule.createTeam("A", "Platoon1", rotation, LocalDate.of(2014, 2, 2));
		schedule.createTeam("B", "Platoon2", rotation, LocalDate.of(2014, 2, 4));
		schedule.createTeam("C", "Platoon3", rotation, LocalDate.of(2014, 1, 31));
		schedule.createTeam("D", "Platoon4", rotation, LocalDate.of(2014, 1, 29));

		return schedule;
	}
}
