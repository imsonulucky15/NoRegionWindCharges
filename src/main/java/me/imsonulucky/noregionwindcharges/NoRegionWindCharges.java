package me.imsonulucky.noregionwindcharges;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class NoRegionWindCharges extends JavaPlugin implements Listener {

    private static final String DISABLED_MSG = "§cWind Charges are disabled in this region!";
    private List<String> disabledRegionNames;
    private Map<String, ProtectedRegion> disabledRegionsCache = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDisabledRegions();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("NoRegionWindCharges enabled.");
    }

    private void loadDisabledRegions() {
        disabledRegionNames = getConfig().getStringList("disabled-regions");
        disabledRegionsCache.clear();

        // Load all disabled regions into cache by searching all loaded worlds
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer()
                    .get(BukkitAdapter.adapt(world));

            if (manager == null) continue;

            for (String regionName : disabledRegionNames) {
                ProtectedRegion region = manager.getRegion(regionName);
                if (region != null) {
                    // Cache using a key format to support multiple worlds with same region name
                    String key = world.getName() + ":" + regionName;
                    disabledRegionsCache.put(key, region);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();

        Location loc = player.getLocation();
        BlockVector3 blockPos = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // Check if player is inside any disabled region in their current world
        for (String regionName : disabledRegionNames) {
            String key = worldName + ":" + regionName;
            ProtectedRegion region = disabledRegionsCache.get(key);
            if (region != null && region.contains(blockPos)) {
                event.setCancelled(true);
                player.sendMessage(DISABLED_MSG);
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("nrw") && args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("nrw.reload")) {
                sender.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            reloadConfig();
            loadDisabledRegions();
            sender.sendMessage("§aNoRegionWindCharges configuration reloaded.");
            getLogger().info("Configuration reloaded by " + sender.getName());
            return true;
        }
        return false;
    }
}
