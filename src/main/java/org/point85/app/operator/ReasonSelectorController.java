package org.point85.app.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.point85.app.AppUtils;
import org.point85.app.DialogController;
import org.point85.app.ImageManager;
import org.point85.app.Images;
import org.point85.app.ReasonNode;
import org.point85.domain.persistence.PersistenceService;
import org.point85.domain.plant.Reason;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class ReasonSelectorController extends DialogController {
	// reason hierarchy
	@FXML
	private TreeView<ReasonNode> tvReasons;

	// selected reason
	private Reason selectedReason;

	// initialize
	public void initialize() throws Exception {
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
	private TreeItem<ReasonNode> getRootReasonItem() {
		if (tvReasons.getRoot() == null) {
			Reason rootReason = new Reason();
			rootReason.setName(Reason.ROOT_REASON_NAME);
			tvReasons.setRoot(new TreeItem<>(new ReasonNode(rootReason)));
		}
		return tvReasons.getRoot();
	}

	// reason selected in the hierarchy
	private void onSelectReason(TreeItem<ReasonNode> oldItem, TreeItem<ReasonNode> newItem) {
		if (newItem == null) {
			return;
		}

		// new attributes
		selectedReason = newItem.getValue().getReason();

		// show the children too
		Set<Reason> children = selectedReason.getChildren();
		List<Reason> sortedChildren = new ArrayList<>(children);
		Collections.sort(sortedChildren);

		boolean hasTreeChildren = !newItem.getChildren().isEmpty();

		// check to see if the node's children have been previously shown
		if (!hasTreeChildren) {
			newItem.getChildren().clear();
			for (Reason child : sortedChildren) {
				TreeItem<ReasonNode> entityItem = new TreeItem<>(new ReasonNode(child));
				newItem.getChildren().add(entityItem);
				entityItem.setGraphic(ImageManager.instance().getImageView(Images.REASON));
			}
		}
		newItem.setExpanded(true);
	}

	public Reason getSelectedReason() {
		return selectedReason;
	}
}
