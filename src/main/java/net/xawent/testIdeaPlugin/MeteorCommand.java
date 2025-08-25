package net.xawent.testIdeaPlugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.entity.Player;

public class MeteorCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        World world;
        Location target;

        if (sender instanceof Player) {
            Player player = (Player) sender;
            world = player.getWorld();

            if (args.length == 3) {
                try {
                    double x = Double.parseDouble(args[0]);
                    double y = Double.parseDouble(args[1]);
                    double z = Double.parseDouble(args[2]);
                    target = new Location(world, x, y, z);
                } catch (Exception e) {
                    return false;
                }
            } else if (args.length == 0) {
                target = player.getLocation();
            } else {
                return false;
            }
        } else if (sender instanceof BlockCommandSender) {
            BlockCommandSender commandBlock = (BlockCommandSender) sender;
            world = commandBlock.getBlock().getWorld();

            if (args.length == 3) {
                try {
                    double x = Double.parseDouble(args[0]);
                    double y = Double.parseDouble(args[1]);
                    double z = Double.parseDouble(args[2]);
                    target = new Location(world, x, y, z);
                } catch (Exception e) {
                    return false;
                }
            } else {
                target = commandBlock.getBlock().getLocation();
            }
        } else {
            return true;
        }
        new Meteor(target);
        return true;
    }
}