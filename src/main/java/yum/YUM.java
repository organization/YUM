package yum;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginDescription;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.Utils;
import jline.internal.InputStreamReader;

public class YUM {
	public static Config config;
	public static LinkedHashMap<String, LinkedTreeMap<String, Object>> repos = new LinkedHashMap<>();
	public static LinkedHashMap<String, LinkedTreeMap<String, Object>> fullrepos = new LinkedHashMap<>();

	public static void init() {
		if (config.get("autoupgrade") == null || !(config.get("autoupgrade") instanceof Boolean))
			config.set("autoupgrade", true);

		if (config.get("autocoreupgrade") == null || !(config.get("autocoreupgrade") instanceof Boolean))
			config.set("autocoreupgrade", true);

		if (config.get("repos") == null || !(config.get("repos") instanceof ArrayList)) {
			ArrayList<String> repos = new ArrayList<String>();
			repos.add("https://github.com/organization/YUM/raw/master/YUM.json");
			config.set("repos", repos);
		}

		if (config.get("coreversion") == null)
			config.set("coreversion", (int) 0);

		if (config.get("coreversion") instanceof Double) {
			Double d = (Double) config.get("coreversion");
			config.set("coreversion", d.intValue());
		}

		if (config.get("fullrepos") == null || !(config.get("fullrepos") instanceof ArrayList))
			config.set("fullrepos", new ArrayList<String>());
	}

	public static void InstallPackage(CommandSender sender, String pluginName) {
		if (repos.get(pluginName) == null) {
			sender.sendMessage("* NOT FOUND PACKAGE: " + pluginName);
			return;
		}
		sender.sendMessage("* DOWNLOAD:" + pluginName);
		YUM.UpdatePlugin(sender, pluginName);
	}

	public static void RemovePackage(CommandSender sender, String name) {
		File file = new File(Server.getInstance().getDataPath() + "plugins/" + name + ".jar");

		if (!file.exists()) {
			sender.sendMessage("* NOT INSTALLED PACKAGE: " + name);
			return;
		}

		Plugin plugin = Server.getInstance().getPluginManager().getPlugin(name);
		if (plugin != null)
			Server.getInstance().getPluginManager().disablePlugin(plugin);

		file.delete();
		sender.sendMessage("* SUCCESSFULLY DELETED PACKAGE: " + name);
	}

	@SuppressWarnings("unchecked")
	public static void AddRepository(CommandSender sender, String url, boolean isFullPack) {
		if (!isFullPack) {
			ArrayList<String> list = (ArrayList<String>) config.get("repos");
			list.add(url);
		} else {
			ArrayList<String> list = (ArrayList<String>) config.get("fullrepos");
			list.add(url);
		}
		YUM.DownloadRepoData(url, sender.getName(), isFullPack);
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

	public static void AutoCoreUpgrade(CommandSender sender) {
		boolean toggleOn = (Boolean) config.get("autocoreupgrade");

		if (toggleOn) {
			config.set("autocoreupgrade", false);
			sender.sendMessage("* AUTO CORE UPGRADE HAS DISABLED");
		} else {
			config.set("autocoreupgrade", true);
			sender.sendMessage("* AUTO CORE UPGRADE HAS ENABLED");
		}
	}

	@SuppressWarnings("unchecked")
	public static void Update(CommandSender sender) {
		ArrayList<String> repos = (ArrayList<String>) config.get("repos");
		ArrayList<String> fullrepos = (ArrayList<String>) config.get("fullrepos");

		if (sender == null) {
			YUMPlugin.getInstance().getLogger().info("* REPOSITORY DATA IS EMPTY");
		} else {
			sender.sendMessage("* REPOSITORY DATA IS EMPTY");
		}

		String senderName = (sender == null) ? null : sender.getName();

		for (String urlString : repos)
			YUM.DownloadRepoData(urlString, senderName, false);

		for (String urlString : fullrepos)
			YUM.DownloadRepoData(urlString, senderName, true);
	}

	public static void DownloadRepoData(String urlString, String senderName, boolean isFullPack) {
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

			@SuppressWarnings("unchecked")
			public void onCompletion(Server server) {
				if (this.map == null) {
					Player player = server.getPlayer(this.senderName);

					if (player != null) {
						player.sendMessage("* DEADLINK: " + url);
					} else {
						YUMPlugin.getInstance().getLogger().info("* DEADLINK: " + url);
					}
					return;
				}

				for (Entry<String, Object> entry : this.map.entrySet()) {
					if (!isFullPack) {
						YUM.repos.put(entry.getKey(), (LinkedTreeMap<String, Object>) entry.getValue());
					} else {
						YUM.fullrepos.put(entry.getKey(), (LinkedTreeMap<String, Object>) entry.getValue());
					}
				}

				Player player = (this.senderName != null) ? server.getPlayer(this.senderName) : null;

				if (player != null) {
					player.sendMessage("* UPDATED: " + url);
				} else {
					YUMPlugin.getInstance().getLogger().info("* UPDATED: " + url);
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
		Server server = Server.getInstance();

		for (Plugin plugin : server.getPluginManager().getPlugins().values())
			YUM.UpdatePlugin(sender, plugin.getName());

		for (Entry<String, LinkedTreeMap<String, Object>> entry : YUM.fullrepos.entrySet()) {
			String pluginName = entry.getKey();
			String downloadLink = (String) entry.getValue().get("link");
			String repoVersion = (String) entry.getValue().get("version");
			YUM.UpdateJAR(pluginName, downloadLink, server.getDataPath() + "plugins/" + pluginName + ".jar",
					repoVersion);
		}

		String senderName = (sender != null) ? sender.getName() : null;
		if (senderName == null) {
			YUMPlugin.getInstance().getLogger().info("* AUTOMATIC UPGRADE CHECK FINISHED");
		} else {
			Player player = Server.getInstance().getPlayer(senderName);
			if (player == null) {
				YUMPlugin.getInstance().getLogger().info("* AUTOMATIC UPGRADE CHECK FINISHED");
				return;
			}
			player.sendMessage("* AUTOMATIC UPGRADE CHECK FINISHED");
		}
	}

	public static void UpdatePlugin(CommandSender sender, String pluginName) {
		if (!YUM.repos.containsKey(pluginName))
			return;

		Server server = Server.getInstance();
		Plugin plugin = server.getPluginManager().getPlugin(pluginName);

		String repoVersion = (String) YUM.repos.get(pluginName).get("version");
		String downloadLink = (String) YUM.repos.get(pluginName).get("link");

		if (repoVersion == null || downloadLink == null)
			return;

		if (plugin != null) {
			if (repoVersion.equals(plugin.getDescription().getVersion())) {
				if (sender != null) {
					sender.sendMessage("* ALREADY UPDATED:" + pluginName);
				} else {
					YUMPlugin.getInstance().getLogger().info("* ALREADY UPDATED:" + pluginName);
				}
				return;
			}
		}

		if (sender != null) {
			sender.sendMessage("* UPDATE: " + pluginName + " [" + repoVersion + "]");
		} else {
			YUMPlugin.getInstance().getLogger().info("* UPDATE: " + pluginName + " [" + repoVersion + "]");
		}

		YUM.UpdateJAR(pluginName, downloadLink, server.getDataPath() + "plugins/", repoVersion);
	}

	public static void UpdateJAR(String pluginName, String downloadLink, String path, String repoVersion) {
		Server.getInstance().getScheduler().scheduleAsyncTask(new AsyncTask() {
			private String link, pluginName, path, repoVersion;
			private boolean success = false;

			public AsyncTask setData(String pluginName, String link, String path, String repoVersion) {
				this.pluginName = pluginName;
				this.link = link;
				this.path = path;
				this.repoVersion = repoVersion;
				return this;
			}

			@Override
			public void onRun() {
				File files = new File(this.path);

				File matchFile = null;
				if (files.isDirectory()) {
					for (File file : files.listFiles()) {
						if (!file.getName().toLowerCase().contains(".jar"))
							continue;

						PluginDescription description = null;
						try {
							JarFile jar = new JarFile(file);
							JarEntry entry = jar.getJarEntry("plugin.yml");
							if (entry == null)
								continue;
							InputStream stream = jar.getInputStream(entry);
							description = new PluginDescription(Utils.readFile(stream));
						} catch (IOException e) {
						}

						if (description == null)
							continue;

						if (description.getName().toLowerCase().equals(pluginName.toLowerCase())) {
							matchFile = file;
							break;
						}
					}
				}else{
					matchFile = files;
				}

				if (matchFile.exists())
					matchFile.delete();

				URL website;
				try {
					website = new URL(this.link);
					try (InputStream in = website.openStream()) {
						Files.copy(in, matchFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						this.success = true;
					}
				} catch (Exception e) {
				}
			}

			public void onCompletion(Server server) {
				if (!success) {
					YUMPlugin.getInstance().getLogger().info("* PASS: " + this.pluginName);
					return;
				}
				YUMPlugin.getInstance().getLogger()
						.info("* UPDATED: " + this.pluginName + " [" + this.repoVersion + "]");
			}
		}.setData(pluginName, downloadLink, path, repoVersion));
	}

	public static void UpdateCore(CommandSender sender) {
		String senderName = (sender != null) ? sender.getName() : null;

		if (sender == null || senderName == null) {
			YUMPlugin.getInstance().getLogger().info("* AUTOMATIC CORE UPGRADE CHECK START..");
		} else {
			sender.sendMessage("* AUTOMATIC CORE UPGRADE CHECK START..");
		}

		Server.getInstance().getScheduler().scheduleAsyncTask(new AsyncTask() {
			private String senderName;
			private String serverPath;
			private int serverCoreVersion;
			private int nukkitRepoVersion;
			private boolean isSucces = false;

			private String message = null;

			public AsyncTask setData(String senderName, int serverCoreVersion) {
				this.senderName = senderName;
				this.serverPath = Server.getInstance().getDataPath();
				this.serverCoreVersion = serverCoreVersion;
				return this;
			}

			@Override
			public void onRun() {
				File files = new File(this.serverPath);

				if (!files.isDirectory())
					return;

				File matchFile = null;
				for (File checkFile : files.listFiles()) {
					if (!checkFile.getName().contains(".jar") || !checkFile.getName().contains("nukkit"))
						continue;

					matchFile = checkFile;
					break;
				}

				if (matchFile == null)
					return;

				// CHECK IS NEED TO UPGRADE
				URL nukkitLastBuildSite;
				try {
					nukkitLastBuildSite = new URL(
							"http://ci.mengcraft.com:8080/job/nukkit/lastSuccessfulBuild/artifact/");
					URLConnection urlConnection = nukkitLastBuildSite.openConnection();
					urlConnection.setConnectTimeout(1000);
					urlConnection.setReadTimeout(1000);

					@SuppressWarnings("resource")
					BufferedReader breader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
					StringBuilder stringBuilder = new StringBuilder();

					String line;
					while ((line = breader.readLine()) != null)
						stringBuilder.append(line);

					String nukkitLastBuildSiteSource = stringBuilder.toString();

					nukkitLastBuildSiteSource = (nukkitLastBuildSiteSource
							.split("<title>Artifacts of Nukkit #").length > 1)
									? nukkitLastBuildSiteSource.split("<title>Artifacts of Nukkit #")[1] : null;
					if (nukkitLastBuildSiteSource == null)
						return;

					nukkitLastBuildSiteSource = (nukkitLastBuildSiteSource.split(" ").length > 1)
							? nukkitLastBuildSiteSource.split(" ")[0] : null;
					if (nukkitLastBuildSiteSource == null)
						return;

					nukkitRepoVersion = Integer.valueOf(nukkitLastBuildSiteSource);
					if (nukkitRepoVersion <= serverCoreVersion) {
						message = "* ALREADY INSTALLED HIGHEST NUKKIT VERSION";
						return;
					}

					if (matchFile.exists())
						matchFile.delete();

					YUM.UpdateJAR("NUKKIT CORE",
							"http://ci.mengcraft.com:8080/job/nukkit/lastSuccessfulBuild/artifact/target/nukkit-1.0-SNAPSHOT.jar",
							matchFile.getAbsolutePath(), "#" + String.valueOf(nukkitRepoVersion));

					this.isSucces = true;
				} catch (Exception e) {
				}
			}

			public void onCompletion(Server server) {
				Player player = (senderName != null) ? server.getPlayer(senderName) : null;

				if (senderName == null || player == null) {
					YUMPlugin.getInstance().getLogger().info("* AUTOMATIC CORE UPGRADE CHECK FINISHED");
					if (this.message != null)
						YUMPlugin.getInstance().getLogger().info(this.message);
				} else {
					player.sendMessage("* AUTOMATIC CORE UPGRADE CHECK FINISHED");
					if (this.message != null)
						player.sendMessage(this.message);
				}

				if (!this.isSucces)
					return;

				YUM.config.set("coreversion", (int) nukkitRepoVersion);
			}
		}.setData(senderName, (int) config.get("coreversion")));
	}

	public static void UpdateUpgrade() {
		Server server = Server.getInstance();

		YUMPlugin plugin = YUMPlugin.getInstance();
		plugin.getLogger().info("* AUTOMATIC UPDATE START..");
		plugin.getLogger().info("* UPGRADE WILL BE START 10 SECONDS LATER..");

		YUM.Update(null);

		server.getScheduler().scheduleDelayedTask(new Task() {
			@Override
			public void onRun(int currentTick) {
				plugin.getLogger().info("* AUTOMATIC UPGRADE CHECK START..");
				YUM.Upgrade(null);
			}
		}, 200);
	}
}