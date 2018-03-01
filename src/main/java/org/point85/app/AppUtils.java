package org.point85.app;

import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TimeZone;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.point85.domain.persistence.PersistencyService;
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
	// date format
	protected static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

	// max and min number of decimal places to show
	private static final int MAX_DIGITS = 9;
	private static final int MIN_DIGITS = 0;

	// no text
	public static final String EMPTY_STRING = "";

	// format a BigDecimal nicely
	public static String formatDouble(double decimal) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
		numberFormat.setGroupingUsed(true);
		numberFormat.setMaximumFractionDigits(MAX_DIGITS);
		numberFormat.setMinimumFractionDigits(MIN_DIGITS);
		return numberFormat.format(decimal);
	}

	public static String formatDate(Date date, TimeZone tz) {
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		String localTimeString = df.format(date);
		StringBuffer sb = new StringBuffer();
		sb.append(localTimeString);

		if (tz != null) {
			// calculate the time zone offset in hours and minutes
			int secsOffset = tz.getOffset(new Date().getTime()) / 1000;
			int hours = Math.abs(secsOffset) / 3600;
			int minutes = (Math.abs(secsOffset) % 3600) / 60;

			String hrFormatted = String.format("%02d", hours);
			String minFormatted = String.format("%02d", minutes);

			// append offset to ISO string
			if (secsOffset < 0) {
				sb.append('-');
			} else {
				sb.append('+');
			}
			sb.append(hrFormatted).append(':').append(minFormatted);
		}

		return sb.toString();
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

	// display an error dialog
	public static void showErrorDialog(Exception e) {
		e.printStackTrace();
		String message = e.getMessage();

		if (message == null) {
			message = e.getClass().getSimpleName();
		}
		showAlert(AlertType.ERROR, "Application Error", "Exception", message);
	}

	// display an ok/cancel dialog
	public static ButtonType showConfirmationDialog(String message) {
		return showAlert(AlertType.CONFIRMATION, "Confirmation", "Confirm Action", message);
	}

	// create a String from the UOM symbol and name
	public static String toDisplayString(String symbol, String name) {
		StringBuffer sb = new StringBuffer();
		sb.append(symbol).append(" (").append(name).append(')');
		return sb.toString();
	}

	// get the display strings for custom symbols defined for the specified UOM
	// type
	public static ObservableList<String> getCustomSymbols(UnitType unitType) {

		List<Object[]> rows = PersistencyService.instance().fetchSymbolsAndNames(unitType);

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

	public static Duration durationFromString(String hrsMins) throws Exception {

		String[] fields = hrsMins.split(":");

		if (fields.length != 2) {
			throw new Exception("Both hours and minutes for the shift duration must be specified.");
		}

		long seconds = Integer.valueOf(fields[0]) * 3600 + Integer.valueOf(fields[1]) * 60;

		Duration duration = Duration.ofSeconds(seconds);

		return duration;
	}

	public static LocalTime localTimeFromString(String hrsMins) throws Exception {
		String[] fields = hrsMins.split(":");

		if (fields.length != 2) {
			throw new Exception("Both hours and minutes for the start time of day must be specified.");
		}

		LocalTime time = LocalTime.of(Integer.valueOf(fields[0]), Integer.valueOf(fields[1]));

		return time;
	}

	public static String stringFromLocalTime(LocalTime time) {
		return String.format("%02d", time.getHour()) + ":" + String.format("%02d", time.getMinute());
	}

	public static String stringFromDuration(Duration duration) {
		long seconds = duration.getSeconds();
		long hours = seconds / 3600;
		long minutes = (seconds - hours * 3600) / 60;

		return String.format("%02d", hours) + ":" + String.format("%02d", minutes);
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
			uom = PersistencyService.instance().fetchUOMBySymbol(symbol);

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
		uom = PersistencyService.instance().fetchUOMBySymbol(symbol);

		if (uom != null) {
			// cache it
			MeasurementSystem.instance().registerUnit(uom);
		}

		if (uom == null) {
			// get from cache next
			uom =MeasurementSystem.instance().getUOM(symbol);
		}

		// bring referenced units into persistence context
		PersistencyService.instance().fetchReferencedUnits(uom);

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

	// removed formatting from decimal string
	public static String removeThousandsSeparator(String formattedString) {
		if (formattedString == null) {
			return null;
		}
		DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();

		StringBuffer sb = new StringBuffer();
		sb.append(decimalFormatSymbols.getGroupingSeparator());
		String separator = sb.toString();

		String[] thousands = formattedString.split(separator);

		sb = new StringBuffer();

		for (String thousand : thousands) {
			sb.append(thousand);
		}
		return sb.toString();
	}
}
