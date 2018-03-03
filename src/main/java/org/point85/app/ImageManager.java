package org.point85.app;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageManager {
	private static final String path = "/org/point85/images/";
	private static final int DIM = 16;

	private ConcurrentMap<ImageEnum, Image> imageCache;

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

	public ImageView getImageView(ImageEnum id) throws Exception {
		return new ImageView(getImage(id));
	}

	public Image getImage(ImageEnum id) throws Exception {
		Image image = imageCache.get(id);

		if (image == null) {
			switch (id) {
			case ADD:
				image = new Image(path + "Add.png", DIM, DIM, true, true);
				break;
			case APPLY:
				image = new Image(path + "Apply.png", DIM, DIM, true, true);
				break;
			case AREA:
				image = new Image(path + "Area.png", DIM, DIM, true, true);
				break;
			case CANCEL:
				image = new Image(path + "Cancel.png", DIM, DIM, true, true);
				break;
			case CATEGORY:
				image = new Image(path + "Category.png", DIM, DIM, true, true);
				break;
			case CELL:
				image = new Image(path + "WorkCell.png", DIM, DIM, true, true);
				break;
			case CHANGED:
				image = new Image(path + "Changed.png", DIM, DIM, true, true);
				break;
			case CHOOSE:
				image = new Image(path + "Choose.png", DIM, DIM, true, true);
				break;
			case CLEAR:
				image = new Image(path + "Clear.png", DIM, DIM, true, true);
				break;
			case COLLECTOR:
				image = new Image(path + "Collect.png", DIM, DIM, true, true);
				break;
			case CONNECT:
				image = new Image(path + "Connect.png", DIM, DIM, true, true);
				break;
			case CONVERT:
				image = new Image(path + "Convert.png", DIM, DIM, true, true);
				break;
			case DASHBOARD:
				image = new Image(path + "DashBoard.png", DIM, DIM, true, true);
				break;
			case DELETE:
				image = new Image(path + "Delete.png", DIM, DIM, true, true);
				break;
			case DISCONNECT:
				image = new Image(path + "Disconnect.png", DIM, DIM, true, true);
				break;
			case ENTERPRISE:
				image = new Image(path + "Enterprise.png", DIM, DIM, true, true);
				break;
			case EQUIPMENT:
				image = new Image(path + "Equipment.png", DIM, DIM, true, true);
				break;
			case EXECUTE:
				image = new Image(path + "Execute.png", DIM, DIM, true, true);
				break;
			case FOLDER:
				image = new Image(path + "Folder.png", DIM, DIM, true, true);
				break;
			case HTTP:
				image = new Image(path + "HttpSource.png", DIM, DIM, true, true);
				break;
			case IMPORT:
				image = new Image(path + "Import.png", DIM, DIM, true, true);
				break;
			case LINE:
				image = new Image(path + "ProductionLine.png", DIM, DIM, true, true);
				break;
			case MATERIAL:
				image = new Image(path + "Material.png", DIM, DIM, true, true);
				break;
			case NEW:
				image = new Image(path + "New.png", DIM, DIM, true, true);
				break;
			case OK:
				image = new Image(path + "OK.png", DIM, DIM, true, true);
				break;
			case OPC_DA:
				image = new Image(path + "OpcDaSource.png", DIM, DIM, true, true);
				break;
			case OPC_UA:
				image = new Image(path + "OpcUaSource.png", DIM, DIM, true, true);
				break;
			case POINT85:
				image = new Image(path + "Point85.png", DIM, DIM, true, true);
				break;
			case REASON:
				image = new Image(path + "Reason.png", DIM, DIM, true, true);
				break;
			case REFRESH:
				image = new Image(path + "Refresh.png", DIM, DIM, true, true);
				break;
			case REFRESH_ALL:
				image = new Image(path + "RefreshAll.png", DIM, DIM, true, true);
				break;
			case REMOVE:
				image = new Image(path + "Remove.png", DIM, DIM, true, true);
				break;
			case RMQ:
				image = new Image(path + "RmqSource.png", DIM, DIM, true, true);
				break;
			case SAVE:
				image = new Image(path + "Save.png", DIM, DIM, true, true);
				break;
			case SAVE_ALL:
				image = new Image(path + "SaveAll.png", DIM, DIM, true, true);
				break;
			case SCHEDULE:
				image = new Image(path + "WorkSchedule.png", DIM, DIM, true, true);
				break;
			case SCRIPT:
				image = new Image(path + "Script.png", DIM, DIM, true, true);
				break;
			case SITE:
				image = new Image(path + "Site.png", DIM, DIM, true, true);
				break;
			case SOURCE:
				image = new Image(path + "DataSource.png", DIM, DIM, true, true);
				break;
			case START:
				image = new Image(path + "Start.png", DIM, DIM, true, true);
				break;
			case STOP:
				image = new Image(path + "Stop.png", DIM, DIM, true, true);
				break;
			case TAG:
				image = new Image(path + "Tag.png", DIM, DIM, true, true);
				break;
			case UOM:
				image = new Image(path + "UOMs.png", DIM, DIM, true, true);
				break;
			case UPDATE:
				image = new Image(path + "Update.png", DIM, DIM, true, true);
				break;
			case VALUE:
				image = new Image(path + "Value.png", DIM, DIM, true, true);
				break;
			case WEB:
				image = new Image(path + "WebSource.png", DIM, DIM, true, true);
				break;
			default:
				break;

			}
			imageCache.put(id, image);
		}
		return image;
	}
}
