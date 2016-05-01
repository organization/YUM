package yum;

import java.io.BufferedReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.Config;
import jline.internal.InputStreamReader;

public class YUM {
	public static Config config;
	public static LinkedHashMap<String, LinkedHashMap<String, Object>> linkMap = new LinkedHashMap<>();

	public static void init() {
		if (config.get("autoupgrade") == null || !(config.get("autoupgrade") instanceof Boolean))
			config.set("autoupgrade", true);

		if (config.get("repos") == null || !(config.get("repos") instanceof ArrayList))
			config.set("repos", new ArrayList<String>());

		if (config.get("installed") == null || !(config.get("installed") instanceof ArrayList))
			config.set("installed", new ArrayList<String>());
	}

	public static void InstallPackage(CommandSender sender, String name) {
		if (linkMap.get("name") == null) {
			sender.sendMessage("* NOT FOUND PACKAGE: " + name);
			return;
		}
	}

	public static void RemovePackage(CommandSender sender, String name) {
		@SuppressWarnings("unchecked")
		ArrayList<String> installedList = (ArrayList<String>) config.get("installed");

		if (!installedList.contains(name)) {
			sender.sendMessage("* NOT INSTALLED PACKAGE: " + name);
			return;
		}
	}

	@SuppressWarnings("unchecked")
	public static void AddRepository(CommandSender sender, String url) {
		ArrayList<String> list = (ArrayList<String>) config.get("repos");
		list.add(url);
		YUM.DownloadRepoData(url, sender.getName());
	}

	@SuppressWarnings("unchecked")
	public static void DeleteRepository(CommandSender sender, String url) {
		ArrayList<String> list = (ArrayList<String>) config.get("repos");
		boolean success = list.remove(list.indexOf(url)) != null;
		sender.sendMessage(success ? "* SUCCESSFULLY DELETED REPO LINK" : "* NOT FOUND REPO LINK");
	}

	@SuppressWarnings("unchecked")
	public static void ShowRepositoryList(CommandSender sender) {
		int index = 0;

		sender.sendMessage("* SHOW YUM REPOSITORY LIST");
		for (String url : (ArrayList<String>) config.get("repos"))
			sender.sendMessage("[" + index++ + "] " + url);
	}

	public static void AutoUpgrade(CommandSender sender) {
		boolean toggleOn = (Boolean) config.get("autoupgrade");

		if (toggleOn) {
			config.set("autoupgrade", false);
			sender.sendMessage("* AUTO UPGRADE HAS DISABLED");
		} else {
			config.set("autoupgrade", true);
			sender.sendMessage("* AUTO UPGRADE HAS ENABLED");
		}
	}

	@SuppressWarnings("unchecked")
	public static void Update(CommandSender sender) {
		for (String urlString : (ArrayList<String>) config.get("repos"))
			YUM.DownloadRepoData(urlString, sender.getName());
	}

	public static void DownloadRepoData(String urlString, String senderName) {
		Server.getInstance().getScheduler().scheduleAsyncTask(new AsyncTask() {
			private String url, senderName;
			private LinkedHashMap<String, Object> map = null;

			public AsyncTask setData(String url, String senderName) {
				this.url = url;
				this.senderName = senderName;
				return this;
			}

			@Override
			public void onRun() {
				this.map = YUM.DownloadYUMJSON(url);
			}

			public void onCompletion(Server server) {
				if (this.map != null)
					YUM.linkMap.put(url, this.map);

				Player player = server.getPlayer(this.senderName);

				if (player != null) {
					player.sendMessage("* UPDATED " + url);
				} else {
					YUMPlugin.getInstance().getLogger().info("* UPDATED " + url);
				}
			}
		}.setData(urlString, senderName));
	}

	public static LinkedHashMap<String, Object> DownloadYUMJSON(String urlString) {
		try {
			URL url = new URL(urlString);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(1000);
			urlConnection.setReadTimeout(1000);

			@SuppressWarnings("resource")
			BufferedReader breader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();

			String line;
			while ((line = breader.readLine()) != null)
				stringBuilder.append(line);

			return new GsonBuilder().create().fromJson(stringBuilder.toString(),
					new TypeToken<LinkedHashMap<String, Object>>() {
					}.getType());
		} catch (Exception e) {
		}
		return null;
	}

	public static void Upgrade(CommandSender sender) {
		//
	}

	public static void UpdateUpgrade() {

	}
}