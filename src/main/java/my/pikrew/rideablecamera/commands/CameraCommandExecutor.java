package my.pikrew.rideablecamera.commands;

import my.pikrew.rideablecamera.RideableCameraPlugin;
import my.pikrew.rideablecamera.camera.CameraManager;
import my.pikrew.rideablecamera.config.ConfigManager;
import my.pikrew.rideablecamera.models.CameraSession;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all camera commands
 */
public class CameraCommandExecutor implements CommandExecutor, TabCompleter {

    private final RideableCameraPlugin plugin;
    private final CameraManager cameraManager;
    private final ConfigManager configManager;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "on", "off", "toggle", "info", "reload", "help"
    );

    public CameraCommandExecutor(RideableCameraPlugin plugin) {
        this.plugin = plugin;
        this.cameraManager = plugin.getCameraManager();
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // No args = toggle
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getMessage("player-only"));
                return true;
            }

            return handleToggle((Player) sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "on":
            case "enable":
                return handleEnable(sender);

            case "off":
            case "disable":
                return handleDisable(sender);

            case "toggle":
                return handleToggle(sender);

            case "info":
            case "status":
                return handleInfo(sender);

            case "reload":
                return handleReload(sender);

            case "help":
                return handleHelp(sender);

            default:
                // Try to toggle for another player
                return handleToggleOther(sender, args[0]);
        }
    }

    private boolean handleEnable(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (cameraManager.isActive(player)) {
            player.sendMessage(configManager.getMessage("already-enabled"));
            return true;
        }

        if (cameraManager.enableCamera(player)) {
            player.sendMessage(configManager.getMessage("enabled"));
            return true;
        } else {
            player.sendMessage(configManager.getMessage("error"));
            return true;
        }
    }

    private boolean handleDisable(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!cameraManager.isActive(player)) {
            player.sendMessage(configManager.getMessage("not-enabled"));
            return true;
        }

        if (cameraManager.disableCamera(player)) {
            player.sendMessage(configManager.getMessage("disabled"));
            return true;
        } else {
            player.sendMessage(configManager.getMessage("error"));
            return true;
        }
    }

    private boolean handleToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;
        boolean enabled = cameraManager.toggleCamera(player);

        if (enabled) {
            player.sendMessage(configManager.getMessage("enabled"));
        } else {
            player.sendMessage(configManager.getMessage("disabled"));
        }

        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!cameraManager.isActive(player)) {
            player.sendMessage(configManager.getMessage("not-enabled"));
            return true;
        }

        CameraSession session = cameraManager.getSession(player);

        player.sendMessage("§6=== Camera Info ===");
        player.sendMessage("§eStatus: §aActive");
        player.sendMessage("§eDistance: §f" + session.getSettings().getDistance());
        player.sendMessage("§eHeight: §f" + session.getSettings().getHeight());
        player.sendMessage("§eSide Offset: §f" + session.getSettings().getSideOffset());
        player.sendMessage("§eDuration: §f" + (session.getDuration() / 1000) + "s");

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("rideablecamera.reload")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        configManager.reloadConfiguration();
        sender.sendMessage(configManager.getMessage("reloaded"));

        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage("§6=== RideableCamera Help ===");
        sender.sendMessage("§e/ridecam §f- Toggle camera");
        sender.sendMessage("§e/ridecam on §f- Enable camera");
        sender.sendMessage("§e/ridecam off §f- Disable camera");
        sender.sendMessage("§e/ridecam info §f- View camera info");
        sender.sendMessage("§e/ridecam reload §f- Reload config");
        sender.sendMessage("§e/ridecam help §f- Show this help");

        if (sender.hasPermission("rideablecamera.others")) {
            sender.sendMessage("§e/ridecam <player> §f- Toggle for other player");
        }

        sender.sendMessage("");
        sender.sendMessage("§7Aliases: §f/rcam, /ridecamera, /npcam");

        return true;
    }

    private boolean handleToggleOther(CommandSender sender, String targetName) {
        if (!sender.hasPermission("rideablecamera.others")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(configManager.getMessage("player-not-found"));
            return true;
        }

        boolean enabled = cameraManager.toggleCamera(target);

        if (enabled) {
            sender.sendMessage("§aCamera enabled for " + target.getName());
            target.sendMessage(configManager.getMessage("enabled-by-other")
                    .replace("%player%", sender.getName()));
        } else {
            sender.sendMessage("§cCamera disabled for " + target.getName());
            target.sendMessage(configManager.getMessage("disabled-by-other")
                    .replace("%player%", sender.getName()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest subcommands
            completions.addAll(SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));

            // Suggest online players if has permission
            if (sender.hasPermission("rideablecamera.others")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}