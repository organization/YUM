package yum;

import java.io.File;
import java.util.LinkedHashMap;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.SimpleCommandMap;
import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

public class YUMPlugin extends PluginBase {
	private static YUMPlugin instance;

	@Override
	public void onLoad() {
		instance = this;

		this.getDataFolder().mkdirs();
		YUM.config = new Config(new File(this.getDataFolder(), "repos.json"), Config.JSON);
		YUM.init();
	}

	@Override
	public void onEnable() {
		this.registerPermission("yum.commands.run", true, "JAR Package Online Install&Update Permission");
		this.registerCommand("yum", "yum.commands.run", "JAR Package Online Install&Update", "/yum");

		if ((boolean) YUM.config.get("autoupgrade"))
			YUM.UpdateUpgrade();

		if ((boolean) YUM.config.get("autocoreupgrade"))
			YUM.UpdateCore(null);
	}

	@Override
	public void onDisable() {
		YUM.config.save();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().toLowerCase().equals("yum")) {
			if (args.length == 0) {
				this.sendHelpMessage(sender);
				return true;
			}

			switch (args[0].toLowerCase()) {
			case "install":
				if (args.length < 2) {
					sender.sendMessage("/YUM INSTALL <NAME>");
					break;
				}
				YUM.InstallPackage(sender, args[1]);
				break;
			case "remove":
				if (args.length < 2) {
					sender.sendMessage("/YUM REMOVE <NAME>");
					break;
				}
				YUM.RemovePackage(sender, args[1]);
				break;
			case "add":
				if (args.length < 2) {
					sender.sendMessage("/YUM ADD <URL>");
					break;
				}
				boolean isFullPack = (args.length > 2 && args[2].toLowerCase().equals("full")) ? true : false;
				YUM.AddRepository(sender, args[1], isFullPack);
				break;
			case "del":
				if (args.length < 2) {
					sender.sendMessage("/YUM DEL <URL>");
					break;
				}
				YUM.DeleteRepository(sender, args[1]);
				break;
			case "list":
				YUM.ShowRepositoryList(sender);
				break;
			case "autoupgrade":
				YUM.AutoUpgrade(sender);
				break;
			case "update":
				YUM.Update(sender);
				break;
			case "upgrade":
				YUM.Upgrade(sender);
				break;
			case "autocoreupgrade":

				break;
			case "coreupgrade":
				break;
			default:
				this.sendHelpMessage(sender);
				break;
			}
			return true;
		}
		return false;
	}

	public void sendHelpMessage(CommandSender sender) {
		sender.sendMessage("/YUM INSTALL <NAME> - INSTALL JAR PACKAGE");
		sender.sendMessage("/YUM REMOVE <NAME> - REMOVE JAR PACKAGE");
		sender.sendMessage("/YUM ADD <URL> - ADD JAR PACKAGE REPO URL <+FULL>");
		sender.sendMessage("/YUM DEL <URL> - REMOVE JAR PACKAGE REPO URL");
		sender.sendMessage("/YUM LIST - SHOW JAR PACKAGE REPO URL LIST");
		sender.sendMessage("/YUM AUTOUPGRADE - TURN AUTOMATIC UPDATING ON OR OFF");
		sender.sendMessage("/YUM UPDATE - UPDATE DOWNLOAD LINK FROM THE REPO");
		sender.sendMessage("/YUM UPGRADE - UPGRADE THE PLUG-INS.");
		sender.sendMessage("/YUM AUTOCOREUPGRADE - TURN AUTO CORE-UPDATING ON OR OFF");
		sender.sendMessage("/YUM COREUPGRADE - UPGRADE THE NUKKIT CORE JAR FILE");
	}

	protected boolean registerCommand(String commandName, String permissionName, String commandDescription,
			String commandUsage) {
		SimpleCommandMap commandMap = this.getServer().getCommandMap();
		PluginCommand<Plugin> command = new PluginCommand<>(commandName, this);
		command.setDescription(commandDescription);
		command.setPermission(permissionName);
		command.setUsage(commandUsage);
		return commandMap.register(commandName, command);
	}

	protected boolean registerPermission(String permissionName, boolean isOp, String description) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("description", description);

		String DEFAULT = (isOp) ? Permission.DEFAULT_OP : Permission.DEFAULT_TRUE;
		Permission permission = Permission.loadPermission(permissionName, data, DEFAULT);
		return this.getServer().getPluginManager().addPermission(permission);
	}

	public static YUMPlugin getInstance() {
		return YUMPlugin.instance;
	}
}