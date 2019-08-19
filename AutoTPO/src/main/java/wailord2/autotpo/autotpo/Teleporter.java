package wailord2.autotpo.autotpo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class Teleporter implements CommandExecutor {

    private Plugin plugin;
    private Player player;
    private Map<Player, Timer> playerTimers = new HashMap<>();
    private Map<Timer, Integer> atPlayerCounter = new HashMap<>();

    public Teleporter(Plugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try{

            this.player = (Player)sender;
            if(!player.hasPermission("autotpo.use")){
                player.sendMessage(ChatColor.RED + "You do not have permission for this command.");
                return true;
            }
        }catch (Exception e){
            plugin.getLogger().severe(e.getMessage());
            return true;
        }

        if(command.getName().equalsIgnoreCase("tpostart")){

            if(playerTimers.get(player)!= null){
                player.sendMessage(ChatColor.GOLD + "You are already running a TPO check. Please stop the current check using /tpostop.");
                return true;
            }
            if(args.length == 1){
                try{
                    Timer timer = new Timer();
                    playerTimers.put(player, timer);
                    startFromPlayer(player, timer, Integer.valueOf(args[0]), 1);
                    return true;
                }
                catch (Exception e){
                    return true;
                }
            }
            else if(args.length == 2){
                try{
                    Timer timer = new Timer();
                    playerTimers.put(player, timer);
                    startFromPlayer(player, timer, Integer.valueOf(args[0]), Integer.valueOf(args[1]));
                }
                catch (Exception e){
                    plugin.getLogger().severe(e.getMessage());
                    return true;
                }
            }
            return true;
        }
        else if(command.getName().equalsIgnoreCase("tpostop")){
            if(args.length == 0) {
                try {
                    if (playerTimers.get(player) != null) {
                        player.sendMessage(ChatColor.GOLD + "Stopping automated tpo check. Stopped at player number " + atPlayerCounter.get(playerTimers.get(player)));
                        Timer timerToStop = playerTimers.get(player);
                        timerToStop.cancel();
                        timerToStop.purge();
                        playerTimers.remove(player);
                        return true;
                    }
                    else{
                        player.sendMessage(ChatColor.GOLD + "You do not have an automated tpo check running at the moment.");
                        return true;
                    }
                }
                catch (Exception e){
                    plugin.getLogger().severe(e.getMessage());
                    return true;
                }
            }
            else{
                return false;
            }

        }
        else if(command.getName().equalsIgnoreCase("tpostopall")){
            if(player.hasPermission("autotpo.stopall")){
                Iterator<Map.Entry<Player,Timer>> iter = playerTimers.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Player,Timer> entry = iter.next();
                    entry.getKey().sendMessage(ChatColor.DARK_RED + "Somebody stopped your tpo check.");

                    entry.getKey().sendMessage(ChatColor.GOLD + "Stopping automated tpo check. Stopped at player number " + atPlayerCounter.get(playerTimers.get(entry.getKey())));
                    Timer timerToStop = playerTimers.get(entry.getKey());
                    timerToStop.cancel();
                    timerToStop.purge();
                }
                playerTimers.clear();
                return true;
            }
            else{
                player.sendMessage(ChatColor.RED + "You do not have permission for this command.");
                return true;
            }
        }
        return true;
    }

    public void startFromPlayer(Player teleporter, Timer timer, int interval, int firstPlayer){
        Collection players = plugin.getServer().getOnlinePlayers();

        Iterator iterator = players.iterator();

        if(firstPlayer > players.size()){
            teleporter.sendMessage(ChatColor.GOLD + "That number is too high.");
            playerTimers.remove(teleporter);
            return;
        }

        atPlayerCounter.put(timer, 0);

        for(int i = 1; i < firstPlayer; i++){
            int currentPlayer = atPlayerCounter.get(timer);
            iterator.next();
            currentPlayer++;
            atPlayerCounter.replace(timer, currentPlayer);
        }

        teleporter.sendMessage(ChatColor.GOLD + "Starting automated tpo check.");

        if(iterator.hasNext()){

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (iterator.hasNext()) {
                            Player possibleNext = (Player) iterator.next();
                            if (possibleNext.getName().equalsIgnoreCase(teleporter.getName())) {
                                teleporter.sendMessage(ChatColor.GOLD + "Skipping yourself");
                            } else {
                                teleporter.sendMessage(ChatColor.GOLD + "Teleporting to next player.");

                                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                        try {
                                            int currentPlayer = atPlayerCounter.get(timer);
                                            currentPlayer++;
                                            atPlayerCounter.replace(timer, currentPlayer);
                                            Bukkit.dispatchCommand(teleporter, "tpo " + possibleNext.getName());
                                        }
                                        catch (Exception e){
                                            teleporter.sendMessage(ChatColor.GOLD + "Can't find player, moving to next.");
                                        }
                                    });
                            }
                        } else {
                            teleporter.sendMessage(ChatColor.GOLD + "Finished automated tpo check. Stopped at player number " + atPlayerCounter.get(timer));
                            Timer timerToStop = playerTimers.get(teleporter);
                            timerToStop.cancel();
                            timerToStop.purge();
                            playerTimers.remove(teleporter);
                        }
                    }
                    catch (Exception e){
                        plugin.getLogger().severe("This is an expected error.");
                    }
                }
            } , 0, interval*1000);
        }
        else{
            teleporter.sendMessage(ChatColor.GOLD + "No players found to check.");
        }
    }

    public void stopAll(){

    }
}
