package org.point85.app;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class SplashController {

	static final int SPLASH_WIDTH = 650;
	static final int SPLASH_HEIGHT = 225;

	@FXML
	private ImageView ivPoint85;

	@FXML
	private Label lbSplash;

	@FXML
	private ImageView ivSplash;

	void initialize() throws Exception {
		// Point85
		ivPoint85.setImage(ImageManager.instance().getImage(Images.POINT85));

		// main image
		ivSplash.setImage(ImageManager.instance().getImage(Images.SPLASH));
	}

	void setSplashText(String text) {
		lbSplash.setText(text);
	}

}
