package org.point85.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class ImageManager {
	private static final String IMAGE_PATH = "/images/";

	// standard image size LxW
	private static final int DIM = 16;

	private final ConcurrentMap<Images, Image> imageCache;

	private static ImageManager imageManager;

	private ImageManager() {
		imageCache = new ConcurrentHashMap<>();
	}

	public static synchronized ImageManager instance() {
		if (imageManager == null) {
			imageManager = new ImageManager();
		}
		return imageManager;
	}

	public ImageView getImageView(Images id) {
		return new ImageView(getImage(id));
	}

	public Image getImage(Images id) {
		Image image = imageCache.get(id);

		if (image != null) {
			return image;
		}

		// create it
		switch (id) {
		case ABOUT:
			image = new Image(IMAGE_PATH + "About.png", DIM, DIM, true, true);
			break;
		case ADD:
			image = new Image(IMAGE_PATH + "Add.png", DIM, DIM, true, true);
			break;
		case APPLY:
			image = new Image(IMAGE_PATH + "Apply.png", DIM, DIM, true, true);
			break;
		case AREA:
			image = new Image(IMAGE_PATH + "Area.png", DIM, DIM, true, true);
			break;
		case CANCEL:
			image = new Image(IMAGE_PATH + "Cancel.png", DIM, DIM, true, true);
			break;
		case CATEGORY:
			image = new Image(IMAGE_PATH + "Category.png", DIM, DIM, true, true);
			break;
		case CELL:
			image = new Image(IMAGE_PATH + "WorkCell.png", DIM, DIM, true, true);
			break;
		case CHANGED:
			image = new Image(IMAGE_PATH + "Changed.png", DIM, DIM, true, true);
			break;
		case CHARTXY:
			image = new Image(IMAGE_PATH + "ChartXY.png", DIM, DIM, true, true);
			break;
		case CHOOSE:
			image = new Image(IMAGE_PATH + "Choose.png", DIM, DIM, true, true);
			break;
		case CLEAR:
			image = new Image(IMAGE_PATH + "Clear.png", DIM, DIM, true, true);
			break;
		case COLLECTOR:
			image = new Image(IMAGE_PATH + "Collect.png", DIM, DIM, true, true);
			break;
		case CONNECT:
			image = new Image(IMAGE_PATH + "Connect.png", DIM, DIM, true, true);
			break;
		case CONVERT:
			image = new Image(IMAGE_PATH + "Convert.png", DIM, DIM, true, true);
			break;
		case DASHBOARD:
			image = new Image(IMAGE_PATH + "Dashboard.png", DIM, DIM, true, true);
			break;
		case DELETE:
			image = new Image(IMAGE_PATH + "Delete.png", DIM, DIM, true, true);
			break;
		case DISCONNECT:
			image = new Image(IMAGE_PATH + "Disconnect.png", DIM, DIM, true, true);
			break;
		case ENTERPRISE:
			image = new Image(IMAGE_PATH + "Enterprise.png", DIM, DIM, true, true);
			break;
		case EQUIPMENT:
			image = new Image(IMAGE_PATH + "Equipment.png", DIM, DIM, true, true);
			break;
		case EXECUTE:
			image = new Image(IMAGE_PATH + "Execute.png", DIM, DIM, true, true);
			break;
		case FOLDER:
			image = new Image(IMAGE_PATH + "Folder.png", DIM, DIM, true, true);
			break;
		case HTTP:
			image = new Image(IMAGE_PATH + "HttpSource.png", DIM, DIM, true, true);
			break;
		case IMPORT:
			image = new Image(IMAGE_PATH + "Import.png", DIM, DIM, true, true);
			break;
		case LINE:
			image = new Image(IMAGE_PATH + "ProductionLine.png", DIM, DIM, true, true);
			break;
		case MATERIAL:
			image = new Image(IMAGE_PATH + "Material.png", DIM, DIM, true, true);
			break;
		case NEW:
			image = new Image(IMAGE_PATH + "New.png", DIM, DIM, true, true);
			break;
		case OK:
			image = new Image(IMAGE_PATH + "OK.png", DIM, DIM, true, true);
			break;
		case OPC_DA:
			image = new Image(IMAGE_PATH + "OpcDaSource.png", DIM, DIM, true, true);
			break;
		case OPC_UA:
			image = new Image(IMAGE_PATH + "OpcUaSource.png", DIM, DIM, true, true);
			break;
		case POINT85:
			image = new Image(IMAGE_PATH + "Point85.png", 64, 64, true, true);
			break;
		case READ:
			image = new Image(IMAGE_PATH + "Read.png", DIM, DIM, true, true);
			break;
		case REASON:
			image = new Image(IMAGE_PATH + "Reason.png", DIM, DIM, true, true);
			break;
		case REFRESH:
			image = new Image(IMAGE_PATH + "Refresh.png", DIM, DIM, true, true);
			break;
		case REFRESH_ALL:
			image = new Image(IMAGE_PATH + "RefreshAll.png", DIM, DIM, true, true);
			break;
		case REMOVE:
			image = new Image(IMAGE_PATH + "Remove.png", DIM, DIM, true, true);
			break;
		case RMQ:
			image = new Image(IMAGE_PATH + "RMQSource.png", DIM, DIM, true, true);
			break;
		case JMS:
			image = new Image(IMAGE_PATH + "JMSSource.png", DIM, DIM, true, true);
			break;	
		case KAFKA:
			image = new Image(IMAGE_PATH + "KafkaSource.png", DIM, DIM, true, true);
			break;
		case EMAIL:
			image = new Image(IMAGE_PATH + "EmailSource.png", DIM, DIM, true, true);
			break;
		case MQTT:
			image = new Image(IMAGE_PATH + "MQTTSource.png", DIM, DIM, true, true);
			break;
		case MODBUS:
			image = new Image(IMAGE_PATH + "ModbusSource.png", DIM, DIM, true, true);
			break;
		case DB:
			image = new Image(IMAGE_PATH + "DatabaseSource.png", DIM, DIM, true, true);
			break;
		case FILE:
			image = new Image(IMAGE_PATH + "FileSource.png", DIM, DIM, true, true);
			break;
		case SAVE:
			image = new Image(IMAGE_PATH + "Save.png", DIM, DIM, true, true);
			break;
		case SAVE_ALL:
			image = new Image(IMAGE_PATH + "SaveAll.png", DIM, DIM, true, true);
			break;
		case SCHEDULE:
			image = new Image(IMAGE_PATH + "WorkSchedule.png", DIM, DIM, true, true);
			break;
		case SHIFT:
			image = new Image(IMAGE_PATH + "Shifts.png", DIM, DIM, true, true);
			break;
		case SCRIPT:
			image = new Image(IMAGE_PATH + "Script.png", DIM, DIM, true, true);
			break;
		case SITE:
			image = new Image(IMAGE_PATH + "Site.png", DIM, DIM, true, true);
			break;
		case SOURCE:
			image = new Image(IMAGE_PATH + "DataSource.png", DIM, DIM, true, true);
			break;
		case SPLASH:
			// large splash image
			image = new Image(IMAGE_PATH + "FactoryEquipment.jpg", 848, 477, true, true);
			break;
		case START:
			image = new Image(IMAGE_PATH + "Start.png", DIM, DIM, true, true);
			break;
		case STARTUP:
			image = new Image(IMAGE_PATH + "Startup.png", DIM, DIM, true, true);
			break;
		case STOP:
			image = new Image(IMAGE_PATH + "Stop.png", DIM, DIM, true, true);
			break;
		case SHUTDOWN:
			image = new Image(IMAGE_PATH + "Shutdown.png", DIM, DIM, true, true);
			break;
		case TAG:
			image = new Image(IMAGE_PATH + "Tag.png", DIM, DIM, true, true);
			break;
		case UOM:
			image = new Image(IMAGE_PATH + "UOM.png", DIM, DIM, true, true);
			break;
		case UPDATE:
			image = new Image(IMAGE_PATH + "Update.png", DIM, DIM, true, true);
			break;
		case VALUE:
			image = new Image(IMAGE_PATH + "Value.png", DIM, DIM, true, true);
			break;
		case WATCH:
			image = new Image(IMAGE_PATH + "Watch.png", DIM, DIM, true, true);
			break;
		case WEB:
			image = new Image(IMAGE_PATH + "WebSource.png", DIM, DIM, true, true);
			break;
		case WRITE:
			image = new Image(IMAGE_PATH + "Write.png", DIM, DIM, true, true);
			break;
		case CHOOSE_FILE:
			image = new Image(IMAGE_PATH + "ChooseFile.png", DIM, DIM, true, true);
			break;
		case PRODUCT:
			image = new Image(IMAGE_PATH + "Product.png", DIM, DIM, true, true);
			break;
		case SETUP:
			image = new Image(IMAGE_PATH + "Setup.png", DIM, DIM, true, true);
			break;
		case AVAILABILITY:
			image = new Image(IMAGE_PATH + "Availability.png", DIM, DIM, true, true);
			break;
		case HELP:
			image = new Image(IMAGE_PATH + "Info.png", DIM, DIM, true, true);
			break;
		case CRON:
			image = new Image(IMAGE_PATH + "Clock.png", DIM, DIM, true, true);
			break;
		case PROFICY:
			image = new Image(IMAGE_PATH + "Proficy.png", DIM, DIM, true, true);
			break;
		case FILTER:
			image = new Image(IMAGE_PATH + "Filter.png", DIM, DIM, true, true);
			break;
		default:
			image = new Image(IMAGE_PATH + "Folder.png", DIM, DIM, true, true);
			break;
		}
		imageCache.put(id, image);
		return image;
	}
}
