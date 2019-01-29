package org.point85.app;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.point85.domain.DomainUtils;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.uom.MeasurementSystem;
import org.point85.domain.uom.Prefix;
import org.point85.domain.uom.UnitOfMeasure;
import org.point85.domain.uom.UnitType;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public abstract class AppUtils {

	// max and min number of decimal places to show
	private static final int MAX_DIGITS = 9;
	private static final int MIN_DIGITS = 0;

	// no text
	public static final String EMPTY_STRING = "";

	// format a BigDecimal
	public static String formatDouble(double decimal) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
		numberFormat.setGroupingUsed(true);
		numberFormat.setMaximumFractionDigits(MAX_DIGITS);
		numberFormat.setMinimumFractionDigits(MIN_DIGITS);
		return numberFormat.format(decimal);
	}

	// display a general alert
	public static ButtonType showAlert(AlertType type, String title, String header, String errorMessage) {
		// Show the error message.
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(errorMessage);
		alert.setResizable(true);

		Optional<ButtonType> result = alert.showAndWait();

		ButtonType buttonType = null;
		try {
			buttonType = result.get();
		} catch (NoSuchElementException e) {

		}
		return buttonType;
	}

	// display an error dialog
	public static void showErrorDialog(String message) {
		showAlert(AlertType.ERROR, "Application Error", "Exception", message);
	}

	// display a warning dialog
	public static void showWarningDialog(String message) {
		showAlert(AlertType.WARNING, "Application Warning", "Warning", message);
	}

	// display an error dialog
	public static void showErrorDialog(Exception e) {
		String message = DomainUtils.formatException(e);
		showAlert(AlertType.ERROR, "Application Error", "Exception", message);
	}

	// display an ok/cancel dialog
	public static ButtonType showConfirmationDialog(String message) {
		return showAlert(AlertType.CONFIRMATION, "Confirmation", "Confirm Action", message);
	}

	// create a String from the UOM symbol and name
	public static String toDisplayString(String symbol, String name) {
		StringBuilder sb = new StringBuilder();
		sb.append(symbol).append(" (").append(name).append(')');
		return sb.toString();
	}

	// get the display strings for custom symbols defined for the specified UOM
	// type
	public static ObservableList<String> getCustomSymbols(UnitType unitType) {

		List<String[]> rows = PersistenceService.instance().fetchUomSymbolsAndNamesByType(unitType);

		List<String> displayStrings = new ArrayList<>(rows.size());

		for (Object[] row : rows) {
			String symbol = (String) row[0];
			String name = (String) row[1];
			displayStrings.add(AppUtils.toDisplayString(symbol, name));
		}

		return FXCollections.observableArrayList(displayStrings);
	}

	// Get display strings for UOMs of the specified type
	public static ObservableList<String> getUnitsOfMeasure(String type) throws Exception {
		ObservableList<String> displayStrings = FXCollections.observableArrayList();

		// UnitType
		UnitType unitType = UnitType.valueOf(type);

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
			hours = Integer.valueOf(fields[0]);
		}

		if (fields.length > 1) {
			minutes = Integer.valueOf(fields[1]);
		}

		if (fields.length > 2) {
			seconds = Integer.valueOf(fields[2]);
		}

		long totalSeconds = hours * 3600 + minutes * 60 + seconds;

		return Duration.ofSeconds(totalSeconds);
	}

	public static LocalTime localTimeFromString(String hrsMins) throws Exception {
		String[] fields = hrsMins.split(":");

		if (fields.length != 2) {
			throw new Exception("Both hours and minutes for the start time of day must be specified.");
		}

		return LocalTime.of(Integer.valueOf(fields[0]), Integer.valueOf(fields[1]));
	}

	public static String stringFromLocalTime(LocalTime time) {
		return String.format("%02d", time.getHour()) + ":" + String.format("%02d", time.getMinute());
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
			throw new Exception(number + " is not a number.");
		}
	}

	public static Long stringToLong(String number) throws Exception {
		try {
			return Long.valueOf(number);
		} catch (NumberFormatException e) {
			throw new Exception(number + " is not a number.");
		}
	}
}
