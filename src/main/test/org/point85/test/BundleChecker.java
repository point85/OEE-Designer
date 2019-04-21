package org.point85.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.point85.app.AppUtils;
import org.point85.app.collector.CollectorLocalizer;
import org.point85.app.designer.DesignerLocalizer;
import org.point85.app.monitor.MonitorLocalizer;
import org.point85.app.operator.OperatorLocalizer;
import org.point85.app.tester.TesterLocalizer;
import org.point85.ops.WebOperatorLocalizer;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class BundleChecker extends Application {

	@FXML
	private Button btCheckKeys;

	@FXML
	private Button btMissingErrors;

	@FXML
	private Button btReverseStrings;

	@FXML
	private Button btFxmlText;

	@FXML
	private void onFxmlText() {
		String api = "text=\"%";
		// String api = "getErrorString(\"";

		try {
			String path = "C:\\tmp";
			File selectedFile = selectFile(path);

			if (selectedFile == null) {
				return;
			}

			// read each line
			BufferedReader br = new BufferedReader(new FileReader(selectedFile));
			String line = null;

			int count = 0;

			try {
				while ((line = br.readLine()) != null) {
					int idx = line.indexOf(api);

					if (idx != -1) {
						String end = line.substring(idx + api.length());
						int idx2 = end.indexOf("\"");
						String key = end.substring(0, idx2);

						String value = DesignerLocalizer.instance().getLangString(key);
						// DesignerLocalizer.instance().getErrorString(key);
						// DomainLocalizer.instance().getErrorString(key);

						if (value.contains("!")) {
							value = MonitorLocalizer.instance().getLangString(key);

							if (value.contains("!")) {
								value = OperatorLocalizer.instance().getLangString(key);

								if (value.contains("!")) {
									value = CollectorLocalizer.instance().getLangString(key);
								}

								if (value.contains("!")) {
									value = TesterLocalizer.instance().getLangString(key);
								}

								if (value.contains("!")) {
									throw new Exception("Missing key: " + key);
								}
							}
						}
						count++;
						System.out.println("[" + count + "] Key: " + key + ", value: " + value);
					}
				}
			} catch (Exception e) {
				throw e;
			} finally {
				br.close();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onReverseStrings() {
		try {
			String inPath = "C:\\dev\\OEE-Operations\\src\\main\\resources\\i18n";
			// "C:\\dev\\OEE-Domain\\src\\main\\resources\\i18n";
			// "C:\\dev\\OEE-Designer\\src\\main\\resources\\org\\point85\\i18n";
			String outPath = "C:\\tmp";

			File inFile = selectFile(inPath);

			if (inFile == null) {
				return;
			}

			// read each line
			FileReader fr = new FileReader(inFile);
			BufferedReader br = new BufferedReader(fr);
			String line = null;

			int count = 0;

			int idx = inFile.getName().indexOf('.');
			String outFile = outPath + "\\" + inFile.getName().substring(0, idx) + "_en.properties";
			FileWriter writer = new FileWriter(outFile);
			BufferedWriter bw = new BufferedWriter(writer);

			try {
				while ((line = br.readLine()) != null) {
					String[] tokens = line.split("=");

					if (tokens.length != 2) {
						continue;
					}

					StringBuilder sb = new StringBuilder();
					String reversed = sb.append(tokens[1]).reverse().toString();

					String r2 = reversed.replace("}0{", "{0}");
					String r3 = r2.replace("}1{", "{1}");
					String r4 = r3.replace("}2{", "{2}");

					String kv = tokens[0] + " = " + r4 + "\n";
					count++;
					System.out.print("[" + count + "] " + kv);

					bw.write(kv);
				}
			} catch (Exception e) {
				throw e;
			} finally {
				br.close();
				bw.close();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@FXML
	private void onCheckMissingErrors() {
		String api = "getLangString(\"";
		// String api = "getErrorString(\"";

		try {
			String path = "C:\\tmp";
			File selectedFile = selectFile(path);

			if (selectedFile == null) {
				return;
			}

			// read each line
			BufferedReader br = new BufferedReader(new FileReader(selectedFile));
			String line = null;

			int count = 0;

			try {
				while ((line = br.readLine()) != null) {
					int idx = line.indexOf(api);

					if (idx != -1) {
						String end = line.substring(idx);
						int idx2 = end.indexOf("\"");
						String next = end.substring(idx2 + 1);
						int idx3 = next.indexOf("\"");
						String key = next.substring(0, idx3);

						String value = WebOperatorLocalizer.instance().getLangString(key);
						// DesignerLocalizer.instance().getErrorString(key);
						// DomainLocalizer.instance().getErrorString(key);

						if (value.contains("!")) {
							throw new Exception("Missing key: " + key);
						}

						if (value.contains("!")) {
							value = MonitorLocalizer.instance().getErrorString(key);

							if (value.contains("!")) {
								value = OperatorLocalizer.instance().getErrorString(key);

								if (value.contains("!")) {
									value = CollectorLocalizer.instance().getErrorString(key);
								}

								if (value.contains("!")) {
									value = TesterLocalizer.instance().getErrorString(key);
								}

								if (value.contains("!")) {
									throw new Exception("Missing key: " + key);
								}
							}
						}
						count++;
						System.out.println("[" + count + "] Key: " + key + ", value: " + value);
					}
				}
			} catch (Exception e) {
				throw e;
			} finally {
				br.close();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	private File selectFile(String path) {
		FileChooser fileChooser = new FileChooser();

		fileChooser.setInitialDirectory(new File(path));

		File selectedFile = fileChooser.showOpenDialog(null);
		return selectedFile;
	}

	@FXML
	private void onCheckKeys() {
		try {
			// show file chooser
			String path = "C:\\dev\\OEE-Operations\\src\\main\\resources\\i18n";
			File selectedFile = selectFile(path);

			if (selectedFile == null) {
				return;
			}

			// read each line
			BufferedReader br = new BufferedReader(new FileReader(selectedFile));
			String line = null;

			TreeMap<String, String> bundleMap = new TreeMap<>();

			try {
				while ((line = br.readLine()) != null) {
					String[] values = line.split("=");

					if (values.length != 2) {
						continue;
					}
					String key = values[0].trim();
					String value = values[1].trim();

					if (!bundleMap.containsKey(key)) {
						bundleMap.put(key, value);
					} else {
						throw new Exception("Duplicate key: " + key + ", previous value: " + value);
					}
				}

				// sort
				for (Entry<String, String> entry : bundleMap.entrySet()) {
					System.out.println(entry.getKey() + " = " + entry.getValue());
				}
			} catch (Exception e) {
				throw e;
			} finally {
				br.close();
			}
		} catch (Exception e) {
			AppUtils.showErrorDialog(e);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(BundleChecker.class.getResource("BundleChecker.fxml"));
			fxmlLoader.load();
			AnchorPane mainLayout = fxmlLoader.getRoot();

			Scene scene = new Scene(mainLayout);

			primaryStage.setTitle("Bundle Checker");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
		}
	}
}
