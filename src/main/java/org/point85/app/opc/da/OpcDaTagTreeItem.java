/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.point85.app.opc.da;

import java.util.Collection;

import org.point85.domain.opc.da.OpcDaTagTreeBranch;
import org.point85.domain.opc.da.OpcDaTreeBrowser;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 *
 * @author krandall
 */
public class OpcDaTagTreeItem extends TreeItem<OpcDaTagTreeBranch> {
	// true if TagNode is a leaf (it has OPC items)

	private boolean isLeaf = false;
	// We do the children and leaf testing only once, and then set these
	// booleans to false so that we do not check again during this
	// run.
	private boolean isFirstTimeChildren = true;
	private boolean isFirstTimeLeaf = true;
	private OpcDaTreeBrowser treeBrowser;

	public OpcDaTagTreeItem(OpcDaTagTreeBranch tagNode, OpcDaTreeBrowser browser) {
		this.setValue(tagNode);
		this.treeBrowser = browser;
	}

	public OpcDaTagTreeItem(OpcDaTagTreeBranch tagNode, OpcDaTreeBrowser browser, ImageView imageView) {
		this.setValue(tagNode);
		this.treeBrowser = browser;
		this.setGraphic(imageView);
	}

	public OpcDaTreeBrowser getTreeBrowser() {
		return this.treeBrowser;
	}

	@Override
	public ObservableList<TreeItem<OpcDaTagTreeBranch>> getChildren() {
		if (isFirstTimeChildren) {
			isFirstTimeChildren = false;

			try {
				super.getChildren().setAll(buildChildren(this));
			} catch (Exception e) {
			}
		}
		return super.getChildren();
	}

	@Override
	public boolean isLeaf() {
		if (isFirstTimeLeaf) {
			isFirstTimeLeaf = false;

			try {
				OpcDaTagTreeBranch tagNode = (OpcDaTagTreeBranch) getValue();
				isLeaf = treeBrowser.isLastNode(tagNode);
			} catch (Exception ex) {
				isLeaf = false;
			}
		}

		return isLeaf;
	}

	private ObservableList<OpcDaTagTreeItem> buildChildren(OpcDaTagTreeItem treeItem) throws Exception {
		OpcDaTagTreeBranch tagBranch = (OpcDaTagTreeBranch) treeItem.getValue();
		ObservableList<OpcDaTagTreeItem> children = FXCollections.observableArrayList();

		if (treeBrowser != null) {
			Collection<OpcDaTagTreeBranch> branches = treeBrowser.getBranches(tagBranch);
			
			Image folder = ((ImageView)getGraphic()).getImage();

			for (OpcDaTagTreeBranch childBranch : branches) {
				children.add(new OpcDaTagTreeItem(childBranch, treeBrowser, new ImageView(folder)));
			}
		}

		return children;
	}
}
