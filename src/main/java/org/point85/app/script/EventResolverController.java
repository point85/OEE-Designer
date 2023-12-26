package org.point85.app.script;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;

import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.modbus.ModbusDataType;
import org.point85.domain.modbus.ModbusEndpoint;
import org.point85.domain.modbus.ModbusSource;
import org.point85.domain.modbus.ModbusUtils;
import org.point85.domain.modbus.ModbusVariant;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Equipment;
import org.point85.domain.plant.Material;
import org.point85.domain.plant.PlantEntity;
import org.point85.domain.plant.Reason;
import org.point85.domain.script.EventResolver;
import org.point85.domain.script.ResolverFunction;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class EventResolverController extends DesignerDialogController {

	// the JavaScript engine
	private ScriptEngine scriptEngine;

	// resolver for the event
	private EventResolver eventResolver;

	// test value
	private Object value;

	@FXML
	private Button btExecute;

	@FXML
	private Button btClearResult;

	@FXML
	private Button btClearScript;

	@FXML
	protected Button btSetValue;

	@FXML
	protected TextField tfValue;

	@FXML
	private TextArea taScript;

	@FXML
	private TextArea taResult;

	@FXML
	private Button btFindReason;

	@FXML
	private Button btFindMaterial;

	@FXML
	private TextField tfReasonCode;

	@FXML
	private Label lbReasonDescription;

	@FXML
	private TextField tfMatlId;

	@FXML
	private Label lbMatlDescription;

	@FXML
	private Button btSetLastValue;

	@FXML
	private TextField tfLastValue;

	@FXML
	private Label lbDataType;

	public void initialize(DesignerApplication app, EventResolver resolver) {
		// script engine
		scriptEngine = new NashornScriptEngineFactory().getScriptEngine();

		// main app
		setApp(app);

		// button images
		setImages();

		// script resolver
		setResolver(resolver);

		// insert 4 spaces instead of a 8 char tab
		taScript.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			final KeyCombination combo = new KeyCodeCombination(KeyCode.TAB);

			@Override
			public void handle(KeyEvent event) {
				// check for only tab key
				if (combo.match(event)) {
					taScript.insertText(taScript.getCaretPosition(), "    ");
					event.consume();
				}
			}
		});
	}

	public EventResolver getResolver() {
		return eventResolver;
	}

	private void setResolver(EventResolver resolver) {
		this.eventResolver = resolver;
		lbDataType.setText(resolver.getDataType());
	}

	// images for buttons
	@Override
	protected void setImages() {
		super.setImages();

		// execute
		btExecute.setGraphic(ImageManager.instance().getImageView(Images.EXECUTE));
		btExecute.setContentDisplay(ContentDisplay.RIGHT);

		// clear script
		btClearScript.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));
		btClearScript.setContentDisplay(ContentDisplay.RIGHT);

		// clear result
		btClearResult.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));
		btClearResult.setContentDisplay(ContentDisplay.RIGHT);

		// set value
		btSetValue.setGraphic(ImageManager.instance().getImageView(Images.APPLY));
		btSetValue.setContentDisplay(ContentDisplay.LEFT);

		// set value
		btSetLastValue.setGraphic(ImageManager.instance().getImageView(Images.APPLY));
		btSetLastValue.setContentDisplay(ContentDisplay.LEFT);

		// find material
		btFindMaterial.setGraphic(ImageManager.instance().getImageView(Images.MATERIAL));
		btFindMaterial.setContentDisplay(ContentDisplay.LEFT);

		// find a reason
		btFindReason.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		btFindReason.setContentDisplay(ContentDisplay.LEFT);
	}

	@FXML
	private void onClearScript() {
		this.taScript.clear();
		this.tfReasonCode.clear();
		this.tfMatlId.clear();
		this.tfValue.clear();
		this.tfLastValue.clear();
		this.lbDataType.setText(null);
		this.lbMatlDescription.setText(null);
		this.lbReasonDescription.setText(null);
	}

	@FXML
	private void onClearResult() {
		taResult.clear();
	}

	private ResolverFunction evaluateFunction(String functionScript) {
		// evaluate function for subsequent execution
		return new ResolverFunction(functionScript);
	}

	protected Object executeScript() throws Exception {
		Object result = null;
		String script = taScript.getText();

		if (script == null || script.length() == 0) {
			return result;
		}

		// create the functions
		String functionScript = ResolverFunction.functionFromBody(script);

		ResolverFunction resolver = evaluateFunction(functionScript);

		// invoke script function
		result = resolver.invoke(scriptEngine, getApp().getAppContext(), value, eventResolver);

		return result;
	}

	protected void setValue(Object value) {
		this.value = value;
	}

	public void showFunctionScript(EventResolver scriptResolver) {
		if (scriptResolver == null) {
			return;
		}
		setResolver(scriptResolver);

		// break out the body
		ResolverFunction resolver = new ResolverFunction(scriptResolver.getScript());
		String body = resolver.getBody();
		this.taScript.setText(body);

		// clear out old executions
		this.taResult.clear();
	}

	@Override
	protected void onOK() {
		String functionScript = ResolverFunction.functionFromBody(taScript.getText());
		this.eventResolver.setScript(functionScript);
		super.onOK();

	}

	@FXML
	private void onExecute() {
		try {
			Object result = executeScript();

			if (result == null) {
				return;
			}

			if (eventResolver.getType() == null) {
				taResult.appendText(result.toString() + '\n');
				return;
			}

			switch (eventResolver.getType()) {
			case AVAILABILITY:
				// must be a reason
				String reasonCode = (String) result;

				// reason must exist
				Reason reason = PersistenceService.instance().fetchReasonByName(reasonCode);

				if (reason == null) {
					throw new Exception(DesignerLocalizer.instance().getErrorString("no.reason.found", reasonCode));
				}
				taResult.appendText(reason.toString() + '\n');
				break;

			case PROD_GOOD:
				taResult.appendText(
						DesignerLocalizer.instance().getLangString("good.production.qty", result.toString()) + '\n');
				break;

			case PROD_REJECT:
				taResult.appendText(
						DesignerLocalizer.instance().getLangString("reject.production.qty", result.toString()) + '\n');
				break;

			case PROD_STARTUP:
				taResult.appendText(
						DesignerLocalizer.instance().getLangString("startup.production.qty", result.toString()) + '\n');
				break;

			case JOB_CHANGE:
				taResult.appendText(DesignerLocalizer.instance().getLangString("job.change", result.toString()) + '\n');
				break;

			case MATL_CHANGE:
				taResult.appendText(
						DesignerLocalizer.instance().getLangString("material.change", result.toString()) + '\n');
				break;

			default:
				taResult.appendText(result.toString() + '\n');
				return;
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// find a reason
	@FXML
	private void onFindReason() {
		try {
			// get the reason from the dialog
			Reason selectedReason = getApp().showReasonEditor();

			if (selectedReason == null) {
				return;
			}

			this.tfReasonCode.setText(selectedReason.getName());
			this.lbReasonDescription.setText(selectedReason.getDescription());

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// find material
	@FXML
	private void onFindMaterial() {
		try {
			// get the material from the dialog
			Material selectedMaterial = getApp().showMaterialEditor();

			if (selectedMaterial == null) {
				return;
			}

			tfMatlId.setText(selectedMaterial.getName());
			lbMatlDescription.setText(selectedMaterial.getDescription());

			// also put in the context
			PlantEntity entity = getApp().getPhysicalModelController().getSelectedEntity();

			if (entity instanceof Equipment) {
				getApp().getAppContext().setMaterial((Equipment) entity, selectedMaterial);
			}

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSetValue() {
		try {
			String valueStr = tfValue.getText();
			if (valueStr == null) {
				return;
			}

			Object objValue = valueStr;

			if (eventResolver.getDataSource() instanceof ModbusSource) {
				ModbusEndpoint source = new ModbusEndpoint(eventResolver.getSourceId());
				ModbusDataType dataType = source.getDataType();
				ModbusVariant variant = ModbusUtils.toVariant(dataType, valueStr);
				List<ModbusVariant> variants = new ArrayList<>(1);
				variants.add(variant);
				objValue = variants;
			}
			setValue(objValue);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSetLastValue() {
		try {
			String valueStr = tfLastValue.getText();

			if (valueStr == null) {
				return;
			}
			this.eventResolver.setLastValue(valueStr);
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}
}
