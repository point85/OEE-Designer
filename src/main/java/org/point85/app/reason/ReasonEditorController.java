package org.point85.app.reason;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.ReasonNode;
import org.point85.app.designer.DesignerApplication;
import org.point85.app.designer.DesignerDialogController;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.domain.oee.TimeLoss;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Reason;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;

/**
 * Controller for editing and viewing reasons.
 * 
 * @author Kent Randall
 *
 */
public class ReasonEditorController extends DesignerDialogController {
	// list of edited reasons
	private final Set<TreeItem<ReasonNode>> editedReasonItems = new HashSet<>();

	// Reason being edited or viewed
	private TreeItem<ReasonNode> selectedReasonItem;

	// file of last import
	private File selectedFile;

	// reason hierarchy
	@FXML
	private TreeView<ReasonNode> tvReasons;

	@FXML
	private Button btNew;

	@FXML
	private Button btSave;

	@FXML
	private Button btRefresh;

	@FXML
	private Button btDelete;

	@FXML
	private Button btImport;

	@FXML
	private TextField tfName;

	@FXML
	private TextArea taDescription;

	@FXML
	private ComboBox<TimeLoss> cbLosses;

	@FXML
	private MenuItem miClearSelection;

	@FXML
	private MenuItem miRefreshAll;

	@FXML
	private MenuItem miSaveAll;

	// extract the Reason name from the tree item
	public Reason getSelectedReason() {
		Reason reason = null;

		if (selectedReasonItem != null) {
			reason = selectedReasonItem.getValue().getReason();
		}
		return reason;
	}

	// initialize
	public void initialize(DesignerApplication app) throws Exception {
		// main app
		setApp(app);

		// images for buttons
		setImages();

		// add the tree view listener for reason selection
		tvReasons.getSelectionModel().selectedItemProperty().addListener((observableValue, oldValue, newValue) -> {
			try {
				onSelectReason(oldValue, newValue);
			} catch (Exception e) {
				AppUtils.showErrorDialog(e);
			}
		});
		tvReasons.setShowRoot(false);

		// fill in the top-level reason nodes
		populateTopReasonNodes();

		// loss categories
		List<TimeLoss> losses = new ArrayList<>();

		for (TimeLoss loss : TimeLoss.values()) {
			losses.add(loss);
		}

		Collections.sort(losses, new Comparator<TimeLoss>() {
			@Override
			public int compare(TimeLoss o1, TimeLoss o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});

		cbLosses.getItems().addAll(losses);
	}

	private void addEditedReason(TreeItem<ReasonNode> item) {
		if (!editedReasonItems.contains(item)) {
			// check for parent
			if (item.getParent() != null && editedReasonItems.contains(item.getParent())) {
				return;
			}
			editedReasonItems.add(item);
		}
	}

	// reason selected in the hierarchy
	private void onSelectReason(TreeItem<ReasonNode> oldItem, TreeItem<ReasonNode> newItem) throws Exception {
		if (newItem == null) {
			return;
		}

		// new attributes
		selectedReasonItem = newItem;
		Reason selectedReason = newItem.getValue().getReason();
		displayAttributes(selectedReason);

		// show the children too
		Set<Reason> children = selectedReason.getChildren();
		List<Reason> sortedChildren = new ArrayList<>(children);
		Collections.sort(sortedChildren);

		boolean hasTreeChildren = newItem.getChildren().size() > 0 ? true : false;

		// check to see if the node's children have been previously shown
		if (!hasTreeChildren) {
			newItem.getChildren().clear();
			for (Reason child : children) {
				TreeItem<ReasonNode> entityItem = new TreeItem<>(new ReasonNode(child));
				newItem.getChildren().add(entityItem);
				entityItem.setGraphic(ImageManager.instance().getImageView(Images.REASON));
			}
		}
		newItem.setExpanded(true);
	}

	// images for editor buttons
	@Override
	protected void setImages() throws Exception {
		super.setImages();

		// new
		btNew.setGraphic(ImageManager.instance().getImageView(Images.NEW));
		btNew.setContentDisplay(ContentDisplay.RIGHT);

		// save
		btSave.setGraphic(ImageManager.instance().getImageView(Images.SAVE));
		btSave.setContentDisplay(ContentDisplay.RIGHT);

		// refresh
		btRefresh.setGraphic(ImageManager.instance().getImageView(Images.REFRESH));
		btRefresh.setContentDisplay(ContentDisplay.RIGHT);

		// delete
		btDelete.setGraphic(ImageManager.instance().getImageView(Images.DELETE));
		btDelete.setContentDisplay(ContentDisplay.RIGHT);

		// import
		btImport.setGraphic(ImageManager.instance().getImageView(Images.IMPORT));
		btImport.setContentDisplay(ContentDisplay.RIGHT);

		// context menu
		miSaveAll.setGraphic(ImageManager.instance().getImageView(Images.SAVE_ALL));
		miRefreshAll.setGraphic(ImageManager.instance().getImageView(Images.REFRESH_ALL));
		miClearSelection.setGraphic(ImageManager.instance().getImageView(Images.CLEAR));
	}

	// show the Reason attributes
	private void displayAttributes(Reason reason) {
		if (reason == null) {
			return;
		}

		// name
		tfName.setText(reason.getName());

		// description
		taDescription.setText(reason.getDescription());

		// loss category
		cbLosses.getSelectionModel().select(reason.getLossCategory());
	}

	// populate the top-level tree view reasons
	private void populateTopReasonNodes() throws Exception {
		tvReasons.getSelectionModel().clearSelection();

		// fetch the reasons
		List<Reason> reasons = PersistenceService.instance().fetchTopReasons();
		Collections.sort(reasons);

		// add them to the root reason
		ObservableList<TreeItem<ReasonNode>> children = getRootReasonItem().getChildren();
		children.clear();

		for (Reason reason : reasons) {
			TreeItem<ReasonNode> reasonItem = new TreeItem<>(new ReasonNode(reason));
			children.add(reasonItem);
			reasonItem.setGraphic(ImageManager.instance().getImageView(Images.REASON));
		}

		// refresh tree view
		getRootReasonItem().setExpanded(true);
		tvReasons.refresh();
	}

	// the single root for all reasons
	private TreeItem<ReasonNode> getRootReasonItem() throws Exception {
		if (tvReasons.getRoot() == null) {
			Reason rootReason = new Reason();
			rootReason.setName(Reason.ROOT_REASON_NAME);
			tvReasons.setRoot(new TreeItem<>(new ReasonNode(rootReason)));
		}
		return tvReasons.getRoot();
	}

	@FXML
	private void onNewReason() {
		try {
			// main attributes
			this.tfName.clear();
			this.tfName.requestFocus();
			this.taDescription.clear();
			this.cbLosses.getSelectionModel().clearSelection();

			this.selectedReasonItem = null;
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	// set the Reason attributes from the UI
	private boolean setAttributes(TreeItem<ReasonNode> reasonItem) throws Exception {
		boolean isDirty = false;

		if (reasonItem == null) {
			return isDirty;
		}
		Reason reason = reasonItem.getValue().getReason();

		// name
		String name = this.tfName.getText().trim();

		if (name.length() == 0) {
			throw new Exception(DesignerLocalizer.instance().getErrorString("no.name"));
		}

		if (!name.equals(reason.getName())) {
			reason.setName(name);
			isDirty = true;
		}

		// description
		String description = this.taDescription.getText();

		if (!description.equals(reason.getDescription())) {
			reason.setDescription(description);
			isDirty = true;
		}

		// loss
		TimeLoss loss = cbLosses.getSelectionModel().getSelectedItem();

		if (loss != null && !loss.equals(reason.getLossCategory())) {
			reason.setLossCategory(loss);
			isDirty = true;
		}

		if (isDirty) {
			reasonItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
			addEditedReason(reasonItem);
		}

		return isDirty;
	}

	private boolean createReason() {
		try {
			// parent reason
			TreeItem<ReasonNode> parentItem = this.tvReasons.getSelectionModel().getSelectedItem();

			if (parentItem == null) {
				// confirm
				ButtonType type = AppUtils
						.showConfirmationDialog(DesignerLocalizer.instance().getLangString("add.reason"));

				if (type.equals(ButtonType.CANCEL)) {
					return false;
				}

				// add to all reasons
				parentItem = tvReasons.getRoot();
			} else {
				// confirm
				ButtonType type = AppUtils.showConfirmationDialog(DesignerLocalizer.instance()
						.getLangString("add.child.reason", parentItem.getValue().getReason().getName()));

				if (type.equals(ButtonType.CANCEL)) {
					return false;
				}
			}
			Reason parentReason = parentItem.getValue().getReason();

			// new child
			Reason newReason = new Reason();
			selectedReasonItem = new TreeItem<>(new ReasonNode(newReason));
			setAttributes(selectedReasonItem);

			// add new child reason if not a top level
			if (!parentReason.getName().equals(Reason.ROOT_REASON_NAME)) {
				parentReason.addChild(newReason);
				parentItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
			}

			// add to tree view
			parentItem.getChildren().add(selectedReasonItem);
			selectedReasonItem.setGraphic(ImageManager.instance().getImageView(Images.CHANGED));
			addEditedReason(selectedReasonItem);

			parentItem.setExpanded(true);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
		return true;
	}

	private void resetGraphic(TreeItem<ReasonNode> parentItem) throws Exception {
		parentItem.setGraphic(ImageManager.instance().getImageView(Images.REASON));

		for (TreeItem<ReasonNode> reasonItem : parentItem.getChildren()) {
			resetGraphic(reasonItem);
		}
	}

	@FXML
	private void onSaveReason() {
		try {
			if (selectedReasonItem == null) {
				// create
				if (!createReason()) {
					return;
				}
			} else {
				// update
				setAttributes(selectedReasonItem);
			}

			// save the reason
			Reason reason = getSelectedReason();
			Reason saved = (Reason) PersistenceService.instance().save(reason);

			selectedReasonItem.getValue().setReason(saved);
			resetGraphic(selectedReasonItem.getParent());

			editedReasonItems.remove(selectedReasonItem);

			tvReasons.refresh();

		} catch (Exception e) {
			// remove from persistence unit
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onSaveAllReasons() {
		try {
			// current reason could have been edited
			setAttributes(selectedReasonItem);

			// save all modified reasons
			for (TreeItem<ReasonNode> editedReasonItem : editedReasonItems) {
				ReasonNode node = editedReasonItem.getValue();
				Reason saved = (Reason) PersistenceService.instance().save(node.getReason());
				node.setReason(saved);
				editedReasonItem.setGraphic(ImageManager.instance().getImageView(Images.REASON));
			}

			editedReasonItems.clear();
			tvReasons.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onDeleteReason() {
		Reason selectedReason = getSelectedReason();
		if (selectedReason == null) {
			AppUtils.showErrorDialog(DesignerLocalizer.instance().getErrorString("no.reason.selected"));
			return;
		}

		// confirm
		ButtonType type = AppUtils.showConfirmationDialog(
				DesignerLocalizer.instance().getLangString("delete.reason", selectedReason.getName()));

		if (type.equals(ButtonType.CANCEL)) {
			return;
		}

		try {
			Reason parentReason = selectedReason.getParent();
			if (parentReason != null) {
				// remove from parent with orphan removal
				parentReason.removeChild(selectedReason);
				PersistenceService.instance().save(parentReason);
			} else {
				// cascade delete
				PersistenceService.instance().delete(selectedReason);
			}

			// remove this reason from the tree
			TreeItem<ReasonNode> selectedReasonItem = tvReasons.getSelectionModel().getSelectedItem();
			TreeItem<ReasonNode> parentNode = selectedReasonItem.getParent();
			parentNode.getChildren().remove(selectedReasonItem);

			// clear fields
			onNewReason();
			tvReasons.getSelectionModel().clearSelection();

			tvReasons.refresh();
			parentNode.setExpanded(true);

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onClearSelection() {
		this.tvReasons.getSelectionModel().clearSelection();
	}

	@FXML
	private void onRefreshReason() {
		try {
			if (getSelectedReason() == null) {
				return;
			}

			if (getSelectedReason().getKey() != null) {
				// read from database
				Reason reason = PersistenceService.instance().fetchReasonByKey(getSelectedReason().getKey());
				selectedReasonItem.getValue().setReason(reason);
				resetGraphic(selectedReasonItem.getParent());
				displayAttributes(reason);
			} else {
				// remove from tree
				selectedReasonItem.getParent().getChildren().remove(selectedReasonItem);
			}
			tvReasons.refresh();

		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onRefreshAllReasons() {
		try {
			populateTopReasonNodes();
			onNewReason();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	@FXML
	protected void onCancel() {
		// close dialog with current reason set to null
		super.onCancel();
	}

	@FXML
	private void onImportReasons() {
		try {
			// show file chooser
			FileChooser fileChooser = new FileChooser();

			if (selectedFile != null) {
				fileChooser.setInitialDirectory(selectedFile.getParentFile());
			}
			selectedFile = fileChooser.showOpenDialog(null);

			if (selectedFile == null) {
				return;
			}

			// first pass to create the reasons
			Map<Reason, String> parentReasons = new HashMap<>();

			// read each line
			BufferedReader br = new BufferedReader(new FileReader(selectedFile));
			String line = null;

			try {
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");

					if (values.length > 0 && values[0] == null || values[0].trim().length() == 0) {
						throw new Exception(DesignerLocalizer.instance().getErrorString("no.name"));
					}

					// name
					String name = values[0].trim();

					// description
					String description = null;
					if (values.length > 1 && values[1] != null && values[1].trim().length() > 0) {
						description = values[1].trim();
					}

					// loss
					String lossName = null;
					if (values.length > 2 && values[2] != null && values[2].trim().length() > 0) {
						lossName = values[2].trim();
					}

					TimeLoss loss = null;

					if (lossName != null && lossName.length() > 0) {
						loss = TimeLoss.valueOf(lossName);
					}

					// parent
					String parentName = null;

					if (values.length > 3 && values[2].trim().length() > 0) {
						parentName = values[3].trim();
					}

					Reason reason = PersistenceService.instance().fetchReasonByName(name);

					if (reason != null) {
						// update
						reason.setName(name);
						reason.setDescription(description);
						reason.setLossCategory(loss);
					} else {
						// new reason
						reason = new Reason(name, description);
						reason.setLossCategory(loss);
					}

					Reason savedReason = (Reason) PersistenceService.instance().save(reason);

					if (parentName != null) {
						parentReasons.put(savedReason, parentName);
					}
				}
			} catch (Exception e) {
				throw e;
			} finally {
				br.close();
			}

			// second pass
			for (Entry<Reason, String> entry : parentReasons.entrySet()) {
				Reason parentReason = PersistenceService.instance().fetchReasonByName(entry.getValue());

				if (parentReason == null) {
					throw new Exception(
							DesignerLocalizer.instance().getErrorString("no.parent.reason", entry.getValue()));
				}

				Reason childReason = entry.getKey();
				parentReason.addChild(childReason);

				PersistenceService.instance().save(childReason);
			}

			// fill in the top-level reason nodes
			populateTopReasonNodes();
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

}
