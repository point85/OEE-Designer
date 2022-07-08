package org.point85.app;

import java.io.File;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.DomainUtils;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Prefix;
import org.point85.domain.uom.Unit;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

public abstract class AppUtils {
	// no text
	public static final String EMPTY_STRING = "";

	// max and min number of decimal places to show
	private static final int MAX_DIGITS = 9;
	private static final int MIN_DIGITS = 0;

	private AppUtils() {
		throw new IllegalStateException("Utility class");
	}

	// format a Double
	public static String formatDouble(Double decimal) {
		String value = "";

		if (decimal != null) {
			NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
			numberFormat.setGroupingUsed(true);
			numberFormat.setMaximumFractionDigits(MAX_DIGITS);
			numberFormat.setMinimumFractionDigits(MIN_DIGITS);
			value = numberFormat.format(decimal);
		}
		return value;
	}

	// display a general alert
	public static ButtonType showAlert(AlertType type, String title, String header, String message) {
		// Show the message
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(message);
		alert.setResizable(true);

		// modal
		Optional<ButtonType> result = alert.showAndWait();

		ButtonType buttonType = null;
		try {
			if (result.isPresent()) {
				buttonType = result.get();
			}
		} catch (NoSuchElementException e) {
			// ignore
		}

		return buttonType;
	}

	// display an error dialog
	public static void showErrorDialog(String message) {
		showAlert(AlertType.ERROR, DesignerLocalizer.instance().getLangString("app.error"),
				DesignerLocalizer.instance().getLangString("exception"), message);
	}

	// display a warning dialog
	public static void showWarningDialog(String message) {
		showAlert(AlertType.WARNING, DesignerLocalizer.instance().getLangString("app.warning"),
				DesignerLocalizer.instance().getLangString("warning"), message);
	}

	// display a informational dialog
	public static void showInfoDialog(String message) {
		showAlert(AlertType.INFORMATION, DesignerLocalizer.instance().getLangString("app.info"),
				DesignerLocalizer.instance().getLangString("information"), message);
	}

	// display an error dialog
	public static void showErrorDialog(Exception e) {
		String message = DomainUtils.formatException(e);
		showAlert(AlertType.ERROR, DesignerLocalizer.instance().getLangString("app.error"),
				DesignerLocalizer.instance().getLangString("exception"), message);
	}

	// display an ok/cancel dialog
	public static ButtonType showConfirmationDialog(String message) {
		return showAlert(AlertType.CONFIRMATION, DesignerLocalizer.instance().getLangString("app.confirm"),
				DesignerLocalizer.instance().getLangString("confirm"), message);
	}

	// create a String from the UOM symbol and name
	public static String toUomDisplayString(String symbol, String name) {
		StringBuilder sb = new StringBuilder();
		sb.append(symbol).append(" (").append(name).append(')');
		return sb.toString();
	}

	// get the display strings for custom symbols defined for the specified UOM
	// type
	public static ObservableList<String> getCustomSymbols(UnitType unitType) throws Exception {

		List<String[]> rows = PersistenceService.instance().fetchUomSymbolsAndNamesByType(unitType);

		List<String> displayStrings = new ArrayList<>(rows.size());

		for (Object[] row : rows) {
			String symbol = (String) row[0];
			String name = (String) row[1];
			displayStrings.add(AppUtils.toUomDisplayString(symbol, name));
		}

		return FXCollections.observableArrayList(displayStrings);
	}

	// Get display strings for UOMs of the specified type
	public static ObservableList<String> getUnitsOfMeasure(UnitType unitType) throws Exception {
		ObservableList<String> displayStrings = FXCollections.observableArrayList();

		List<UnitOfMeasure> uoms = MeasurementSystem.instance().getUnitsOfMeasure(unitType);

		for (UnitOfMeasure uom : uoms) {
			displayStrings.add(uom.toDisplayString());
		}
		Collections.sort(displayStrings);

		return displayStrings;
	}

	public static Duration durationFromString(String period) throws Exception {

		String[] fields = period.split(":");

		int hours = 0;
		int minutes = 0;
		int seconds = 0;

		if (fields.length > 0) {
			hours = Integer.parseInt(fields[0]);
		}

		if (fields.length > 1) {
			minutes = Integer.parseInt(fields[1]);
		}

		if (fields.length > 2) {
			seconds = Integer.parseInt(fields[2]);
		}

		long totalSeconds = (long) hours * 3600 + minutes * 60 + seconds;

		return Duration.ofSeconds(totalSeconds);
	}

	public static LocalTime localTimeFromString(String timeOfDay) throws Exception {
		String[] fields = timeOfDay.split(":");

		if (fields.length == 1) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("both.hours.and.minutes"));
		}

		int seconds = 0;
		if (fields.length == 3) {
			seconds = Integer.parseInt(fields[2]);
		}
		return LocalTime.of(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), seconds);
	}

	public static String stringFromLocalTime(LocalTime time, boolean withSeconds) {
		String dayTime = String.format("%02d", time.getHour()) + ":" + String.format("%02d", time.getMinute());

		if (withSeconds) {
			dayTime += ":" + String.format("%02d", time.getSecond());
		}

		return dayTime;
	}

	public static String stringFromDuration(Duration duration, boolean withSeconds) {
		long totalSeconds = duration.getSeconds();
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds - hours * 3600) / 60;
		long seconds = totalSeconds - (hours * 3600) - (minutes * 60);

		String value = String.format("%02d", hours) + ":" + String.format("%02d", minutes);

		if (withSeconds) {
			value += ":" + String.format("%02d", seconds);
		}
		return value;
	}

	// get the UOM from cache first, then from the database if not found
	public static UnitOfMeasure getUOMForConversion(Prefix prefix, String symbol) throws Exception {
		UnitOfMeasure uom = null;

		if (symbol == null || symbol.length() == 0) {
			return uom;
		}

		// look in cache first
		uom = MeasurementSystem.instance().getUOM(symbol);

		if (uom == null) {
			// database next
			uom = PersistenceService.instance().fetchUomBySymbol(symbol);

			if (uom != null) {
				// cache it
				MeasurementSystem.instance().registerUnit(uom);
			}
		}

		if (uom != null && prefix != null) {
			uom = MeasurementSystem.instance().getUOM(prefix, uom);
		}

		return uom;
	}

	// get the UOM from the database first, then from cache if not found
	public static UnitOfMeasure getUOMForEditing(String symbol) throws Exception {
		UnitOfMeasure uom = null;

		if (symbol == null || symbol.length() == 0) {
			return uom;
		}

		// look in database first
		uom = PersistenceService.instance().fetchUomBySymbol(symbol);

		if (uom != null) {
			// cache it
			MeasurementSystem.instance().registerUnit(uom);
		}

		if (uom == null) {
			// get from cache next
			uom = MeasurementSystem.instance().getUOM(symbol);
		}

		// bring referenced units into persistence context
		PersistenceService.instance().fetchReferencedUnits(uom);

		return uom;
	}

	// parse the UOM symbol out of the display string
	public static String parseSymbol(String displayString) {
		String symbol = null;

		if (displayString != null) {
			int idx = displayString.indexOf('(');
			symbol = displayString.substring(0, idx - 1);
		}
		return symbol;
	}

	public static Double stringToDouble(String number) throws Exception {
		try {
			return Double.valueOf(number);
		} catch (NumberFormatException e) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("not.number", number));
		}
	}

	public static Long stringToLong(String number) throws Exception {
		try {
			return Long.valueOf(number);
		} catch (NumberFormatException e) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("not.number", number));
		}
	}

	public static String[] parseCsvInput(String csv) throws Exception {
		String[] values = csv.split(",");
		String reason = null;

		if (values.length == 0) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.values"));
		} else if (values.length == 2) {
			reason = values[1].trim();
		}

		String[] outputs = new String[2];
		outputs[0] = values[0].trim();
		outputs[1] = reason;

		return outputs;
	}

	public static List<UnitType> sortUnitTypes() {
		List<UnitType> sorted = Arrays.asList(UnitType.values());

		Collections.sort(sorted, new Comparator<UnitType>() {
			@Override
			public int compare(UnitType o1, UnitType o2) {
				return o1.name().compareTo(o2.name());
			}
		});

		return sorted;
	}

	public static List<Unit> sortUnits() {
		List<Unit> sorted = Arrays.asList(Unit.values());

		Collections.sort(sorted, new Comparator<Unit>() {
			@Override
			public int compare(Unit o1, Unit o2) {
				return o1.name().compareTo(o2.name());
			}
		});

		return sorted;
	}

	public static File showFileSaveDialog(File file) {
		// show file chooser
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(DesignerLocalizer.instance().getLangString("filechooser.backup"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup files (*.p85x)", "*.p85x"));

		if (file != null) {
			fileChooser.setInitialDirectory(file);
		}

		// name a file
		return fileChooser.showSaveDialog(null);
	}
}
