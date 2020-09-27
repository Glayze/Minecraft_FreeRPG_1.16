package mc.carlton.freerpg.playerInfo;

import mc.carlton.freerpg.FreeRPG;
import mc.carlton.freerpg.globalVariables.StringsAndOtherData;
import mc.carlton.freerpg.serverInfo.ConfigLoad;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Leaderboards {
    static Map<String,ArrayList<PlayerLeaderboardStat>> leaderboards = new ConcurrentHashMap<>();
    static Map<UUID,Map<String, PlayerLeaderboardStat>> playerUUID_to_personalSkillLeaderboards = new ConcurrentHashMap<>();
    static Map<String,Map<String, PlayerLeaderboardStat>> playerName_to_personalSkillLeaderboards = new ConcurrentHashMap<>();
    static boolean leaderboardsLoaded;
    static List<String> leaderboardNames =  new ArrayList<>();
    static boolean leaderboardUpdating;

    public boolean isLeaderboardsLoaded() {
        return leaderboardsLoaded;
    }

    public void setLeaderboardsLoaded(boolean isLeaderboardsLoaded) {
        Leaderboards.leaderboardsLoaded = isLeaderboardsLoaded;
    }

    public List<String> getLeaderboardNames(){
        return leaderboardNames;
    }

    public void initializeLeaderboards(){
        leaderboardUpdating = false;
        StringsAndOtherData stringsAndOtherData = new StringsAndOtherData();
        ArrayList<String> skillNames = stringsAndOtherData.getSkillNames();
        leaderboardNames.add("playTime");
        leaderboardNames.add("global");
        for (String skillName : skillNames) {
            leaderboardNames.add(skillName);
        }
        for (String leaderboardName : leaderboardNames) {
            leaderboards.put(leaderboardName,new ArrayList<>());
        }
        asyncUpdateLeaderBoards();
    }

    public void initializeNewPlayer(Player p) {
        UUID playerUUID = p.getUniqueId();
        if (playerUUID_to_personalSkillLeaderboards.containsKey(playerUUID)) { //Player is already loaded in (i.e not new)
            return;
        }
        if (leaderboardUpdating) {
            Plugin plugin = FreeRPG.getPlugin(FreeRPG.class);
            new BukkitRunnable() {
                @Override
                public void run() {
                    initializeNewPlayer(p);
                }
            }.runTaskLater(plugin,20);
            return;
        }
        PlayerStats playerStats = new PlayerStats(p);
        for (String leaderBoardName : leaderboardNames) {
            PlayerLeaderboardStat player;
            if (leaderBoardName.equalsIgnoreCase("global")) {
                int level = (int) playerStats.getPlayerData().get(leaderBoardName).get(0);
                int totalExperience = (int) playerStats.getPlayerData().get(leaderBoardName).get(29);
                player = new PlayerLeaderboardStat(playerUUID,p.getName(),level,totalExperience);

            }
            else if (leaderBoardName.equalsIgnoreCase("playTime")) {
                long playTime = playerStats.getPlayerPlayTime();
                player = new PlayerLeaderboardStat(playerUUID,p.getName(),playTime);
            }
            else {
                int exp = (int) playerStats.getPlayerData().get(leaderBoardName).get(1);
                player = new PlayerLeaderboardStat(playerUUID,p.getName(),exp);
            }
            ArrayList<PlayerLeaderboardStat> leaderboard = leaderboards.get(leaderBoardName);
            leaderboard.add(player);
            leaderboards.put(leaderBoardName,leaderboard);
            addPlayerLeaderboardProfilePointer(playerUUID,p.getName(),leaderBoardName,player);
        }
    }

    public void addPlayerSkillStat(UUID playerUUID, String playerName, int experience, String skillName) {
        PlayerLeaderboardStat player = new PlayerLeaderboardStat(playerUUID,playerName,experience);
        ArrayList<PlayerLeaderboardStat> leaderboard = leaderboards.get(skillName);
        leaderboard.add(player);
        leaderboards.put(skillName,leaderboard);
        addPlayerLeaderboardProfilePointer(playerUUID,playerName,skillName,player);

    }
    public void addPlayerGlobalStat(UUID playerUUID, String playerName, int totalLevel, int totalExperience) {
        PlayerLeaderboardStat player = new PlayerLeaderboardStat(playerUUID,playerName,totalLevel,totalExperience);
        ArrayList<PlayerLeaderboardStat> leaderboard = leaderboards.get("global");
        leaderboard.add(player);
        leaderboards.put("global",leaderboard);
        addPlayerLeaderboardProfilePointer(playerUUID,playerName,"global",player);
    }
    public void addPlayerTimeStat(UUID playerUUID, String playerName, long timePlayed) {
        PlayerLeaderboardStat player = new PlayerLeaderboardStat(playerUUID,playerName,timePlayed);
        ArrayList<PlayerLeaderboardStat> leaderboard = leaderboards.get("playTime");
        leaderboard.add(player);
        leaderboards.put("playTime",leaderboard);
        addPlayerLeaderboardProfilePointer(playerUUID,playerName,"playTime",player);
    }

    public void sortAllLeaderBoards(){
        ConfigLoad configLoad = new ConfigLoad();
        if (!configLoad.isLeaderboardDyanmicUpdate()) {
            return;
        }
        for (String leaderBoardName : leaderboardNames) {
            sortLeaderBoard(leaderBoardName);
        }
    }
    public void sortAllLeaderBoards(boolean forceSort){
        if (!forceSort) {
            sortAllLeaderBoards();
        }
        for (String leaderBoardName : leaderboardNames) {
            sortLeaderBoard(leaderBoardName,forceSort);
        }
    }

    public void sortLeaderBoard(String skillName) {
        ConfigLoad configLoad = new ConfigLoad();
        if (!configLoad.isLeaderboardDyanmicUpdate()) {
            return;
        }
        ArrayList<PlayerLeaderboardStat> leaderboard = leaderboards.get(skillName);
        leaderboard.sort(new Comparator<PlayerLeaderboardStat>() {
            @Override
            public int compare(PlayerLeaderboardStat o1, PlayerLeaderboardStat o2) {
                if (o1.get_sortedStat().doubleValue() < o2.get_sortedStat().doubleValue()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        leaderboards.put(skillName,leaderboard);
    }
    public void sortLeaderBoard(String skillName, boolean forceSort) {
        if (!forceSort) {
            sortLeaderBoard(skillName);
        }
        ArrayList<PlayerLeaderboardStat> leaderboard = leaderboards.get(skillName);
        leaderboard.sort(new Comparator<PlayerLeaderboardStat>() {
            @Override
            public int compare(PlayerLeaderboardStat o1, PlayerLeaderboardStat o2) {
                if ( o1.get_sortedStat().doubleValue() < o2.get_sortedStat().doubleValue()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
    }

    public void updateAllPlayerStats() {
        for (UUID playerUUID : playerUUID_to_personalSkillLeaderboards.keySet()) {
            if (Bukkit.getPlayer(playerUUID) != null) {
                if (Bukkit.getPlayer(playerUUID).isOnline()) {
                    PlayerStats playerStats = new PlayerStats(playerUUID);
                    Map<String,ArrayList<Number>> pStats = playerStats.getPlayerData();
                    for (String leaderboardName : leaderboardNames) {
                        if (leaderboardName.equalsIgnoreCase("global")) {
                            playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(leaderboardName).set_sortedStat(pStats.get(leaderboardName).get(0));
                            playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(leaderboardName).set_stat2(pStats.get(leaderboardName).get(29));
                        }
                        else if (leaderboardName.equalsIgnoreCase("playTime")) {
                            long playTime = playerStats.getNewPlayTime();
                            playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(leaderboardName).set_sortedStat(playTime);
                        }
                        else {
                            playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(leaderboardName).set_stat2(pStats.get(leaderboardName).get(0));
                            playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(leaderboardName).set_sortedStat(pStats.get(leaderboardName).get(1));
                        }
                    }
                }
            }
        }
    }

    public void updatePlayerPlayTimes(){
        ConfigLoad configLoad = new ConfigLoad();
        if (!configLoad.isLeaderboardDyanmicUpdate()) {
            return;
        }
        for (UUID playerUUID : playerUUID_to_personalSkillLeaderboards.keySet()) {
            if (Bukkit.getPlayer(playerUUID) != null) {
                if (Bukkit.getPlayer(playerUUID).isOnline()) {
                    PlayerStats playerStats = new PlayerStats(playerUUID);
                    long playTime = playerStats.getNewPlayTime();
                    playerUUID_to_personalSkillLeaderboards.get(playerUUID).get("playTime").set_sortedStat(playTime);
                }
            }
        }
    }

    public void asyncUpdateLeaderBoards() {
        ConfigLoad configLoad = new ConfigLoad();
        if (configLoad.isLeaderboardDyanmicUpdate()) {
            return;
        }
        Plugin plugin = FreeRPG.getPlugin(FreeRPG.class);
        int ticksWait = 20*configLoad.getLeaderboardUpdateTimer();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (leaderboardsLoaded) {
                    leaderboardUpdating = true;
                    updateAllPlayerStats();
                    sortAllLeaderBoards(true);
                    leaderboardUpdating = false;
                }
            }
        }.runTaskTimerAsynchronously(plugin,ticksWait,ticksWait);
    }

    public ArrayList<PlayerLeaderboardStat> getLeaderboard(String skillName) {
        return leaderboards.get(skillName);
    }

    public Map<String, ArrayList<PlayerLeaderboardStat>> getLeaderboards() {
        return leaderboards;
    }

    public void addPlayerLeaderboardProfilePointer(UUID playerUUID,String playerName, String skillName, PlayerLeaderboardStat player) {
        if (!playerUUID_to_personalSkillLeaderboards.containsKey(playerUUID)) {
            Map<String, PlayerLeaderboardStat> initialMap = new ConcurrentHashMap<>();
            initialMap.put(skillName,player);
            playerUUID_to_personalSkillLeaderboards.put(playerUUID,initialMap);
        }
        else {
            Map<String, PlayerLeaderboardStat> map = playerUUID_to_personalSkillLeaderboards.get(playerUUID);
            map.put(skillName,player);
            playerUUID_to_personalSkillLeaderboards.put(playerUUID,map);
        }
        if (!playerName_to_personalSkillLeaderboards.containsKey(playerName)) {
            Map<String, PlayerLeaderboardStat> initialMap = new ConcurrentHashMap<>();
            initialMap.put(skillName,player);
            playerName_to_personalSkillLeaderboards.put(playerName,initialMap);
        }
        else {
            Map<String, PlayerLeaderboardStat> map = playerName_to_personalSkillLeaderboards.get(playerName);
            map.put(skillName,player);
            playerName_to_personalSkillLeaderboards.put(playerName,map);
        }
    }

    public void removeForbiddenUUIDs(HashSet<UUID> allPlayerUUIDs) {
        for (String skillName : leaderboards.keySet()) {
            ArrayList<PlayerLeaderboardStat> forbiddenLeaderBoardStats = new ArrayList<>();
            ArrayList<PlayerLeaderboardStat> players = leaderboards.get(skillName);
            for (PlayerLeaderboardStat player : players) {
                if (!allPlayerUUIDs.contains(player.get_playerUUID())) {
                    forbiddenLeaderBoardStats.add(player);
                }
            }
            players.removeAll(forbiddenLeaderBoardStats);
            leaderboards.put(skillName,players);
        }
    }

    public void updatePlayerStats(Player p, String skillName, Number sortedStat, Object stat2) {
        UUID playerUUID = p.getUniqueId();
        updatePlayerStats(playerUUID,skillName,sortedStat,stat2);
    }

    public void updatePlayerStats(UUID playerUUID, String skillName, Number sortedStat, Object stat2) {
        PlayerLeaderboardStat player = playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(skillName);
        player.updateStats(sortedStat,stat2);
    }

    public PlayerLeaderboardStat getPlayerStatAtLeaderBoardPosition(String leaderboardName, int position) {
        if (!leaderboards.containsKey(leaderboardName)) {
            return null;
        }
        sortLeaderBoard(leaderboardName);
        ArrayList<PlayerLeaderboardStat> leaderboard = leaderboards.get(leaderboardName);

        if (position < 1) {
            position = 1;
        }
        else if (position > leaderboard.size()) {
            position = leaderboard.size();
        }
        return leaderboard.get(position-1);
    }

    public int getLeaderboardPosition(Player p,String leaderboardName) {
        UUID playerUUID = p.getUniqueId();
        return getLeaderboardPosition(playerUUID,leaderboardName);
    }

    public int getLeaderboardPosition(UUID playerUUID,String leaderboardName) {
        sortLeaderBoard(leaderboardName);
        if (!leaderboards.containsKey(leaderboardName)) {
            return -1;
        }
        PlayerLeaderboardStat playerStat = playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(leaderboardName);
        int position = leaderboards.get(leaderboardName).indexOf(playerStat) + 1;
        return position;
    }
    public int getLeaderboardPosition(String playerName,String leaderboardName) {
        sortLeaderBoard(leaderboardName);
        if (!leaderboards.containsKey(leaderboardName)) {
            return -1;
        }
        PlayerLeaderboardStat playerStat = playerName_to_personalSkillLeaderboards.get(playerName).get(leaderboardName);
        int position = leaderboards.get(leaderboardName).indexOf(playerStat) + 1;
        return position;
    }

    public int getLeaderboardSize(String leaderboardName) {
        if (!leaderboards.containsKey(leaderboardName)) {
            return -1;
        }
        return leaderboards.get(leaderboardName).size();
    }

    public PlayerLeaderboardStat getPlayerStat(String playerName,String leaderboardName) {
        if (playerName_to_personalSkillLeaderboards.containsKey(playerName)) {
            return playerName_to_personalSkillLeaderboards.get(playerName).get(leaderboardName);
        }
        else {
            return null;
        }
    }

    public PlayerLeaderboardStat getPlayerStat(UUID playerUUID,String leaderboardName) {
        if (playerUUID_to_personalSkillLeaderboards.containsKey(playerUUID)) {
            return playerUUID_to_personalSkillLeaderboards.get(playerUUID).get(leaderboardName);
        }
        else {
            return null;
        }
    }

    public boolean isPlayerOnLeaderboards(String playerName) {
        if (playerName_to_personalSkillLeaderboards.containsKey(playerName)) {
            return true;
        }
        else {
            return false;
        }
    }



}
