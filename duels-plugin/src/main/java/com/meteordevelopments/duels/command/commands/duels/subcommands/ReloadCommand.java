package com.meteordevelopments.duels.command.commands.duels.subcommands;

import com.meteordevelopments.duels.DuelsPlugin;
import com.meteordevelopments.duels.command.BaseCommand;
import com.meteordevelopments.duels.util.Loadable;
import com.meteordevelopments.duels.util.Reloadable;
import com.meteordevelopments.duels.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class ReloadCommand extends BaseCommand {

    public ReloadCommand(final DuelsPlugin plugin) {
        super(plugin, "reload", null, null, 1, false, "rl");
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        try {
            if (args.length > getLength()) {
                final String targetName = args[1];
                final Loadable target = plugin.find(targetName);

                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Module '" + targetName + "' not found. The following modules are available for a reload: " + StringUtil.join(plugin.getReloadables(), ", "));
                    return;
                }

                if (!(target instanceof Reloadable)) {
                    sender.sendMessage(ChatColor.RED + "Module '" + targetName + "' is not reloadable. The following modules are available for a reload: " + StringUtil.join(plugin.getReloadables(), ", "));
                    return;
                }

                final String name = target.getClass().getSimpleName();

                try {
                    if (plugin.reload(target)) {
                        sender.sendMessage(ChatColor.GREEN + "[" + plugin.getDescription().getFullName() + "] Successfully reloaded " + name + ".");
                    } else {
                        sender.sendMessage(ChatColor.RED + "An error occurred while reloading " + name + "! Please check the console for more information.");
                    }
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "An error occurred while reloading " + name + "! Please check the console for more information.");
                    e.printStackTrace();
                }

                return;
            }

            try {
                if (plugin.reload()) {
                    sender.sendMessage(ChatColor.GREEN + "[" + plugin.getDescription().getFullName() + "] Reload complete.");
                } else {
                    sender.sendMessage(ChatColor.RED + "An error occurred while reloading the plugin! Please check the console for more information.");
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "An error occurred while reloading the plugin! Please check the console for more information.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An unexpected error occurred during reload! Please check the console for more information.");
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        try {
            if (args.length == 2) {
                final String input = args[1].toLowerCase();
                return plugin.getReloadables().stream()
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}