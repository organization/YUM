package yum;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.Logger;

public class YUM {
	public static Config config;

	public static void init() {
		if (config.get("autoupgrade") == null || !(config.get("autoupgrade") instanceof Boolean))
			config.set("autoupgrade", true);

		if (config.get("repos") == null || !(config.get("repos") instanceof ArrayList))
			config.set("repos", new ArrayList<String>());
	}

	public static void InstallPackage(String name) {
		//
	}

	public static void RemovePackage(String name) {
	}

	@SuppressWarnings("unchecked")
	public static void AddRepository(String url) {
		ArrayList<String> list = (ArrayList<String>) config.get("repos");
		list.add(url);
	}

	@SuppressWarnings("unchecked")
	public static void DeleteRepository(String url) {
		ArrayList<String> list = (ArrayList<String>) config.get("repos");
		boolean success = list.remove(list.indexOf(url)) != null;
	}

	@SuppressWarnings("unchecked")
	public static void ShowRepositoryList() {
		Logger logger = Server.getInstance().getLogger();
		int index = 0;

		logger.info("* SHOW YUM REPOSITORY LIST");
		for (String url : (ArrayList<String>) config.get("repos"))
			logger.info("[" + index++ + "] " + url);
	}

	public static void AutoUpgrade() {
		boolean toggleOn = (Boolean) config.get("autoupgrade");

		if (toggleOn) {
			config.set("autoupgrade", false);
			Server.getInstance().getLogger().info("* AUTO UPGRADE HAS DISABLED");
		} else {
			config.set("autoupgrade", true);
			Server.getInstance().getLogger().info("* AUTO UPGRADE HAS ENABLED");
		}
	}

	public static void Update() {
		//
	}

	public static void Upgrade() {
		//
	}

	public static void UpdateUpgrade() {

	}
}