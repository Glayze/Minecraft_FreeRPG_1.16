package mc.carlton.freerpg.serverFileManagement;

import mc.carlton.freerpg.FreeRPG;
import mc.carlton.freerpg.serverInfo.RecentLogouts;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;

public class RecentPlayersFileManager {
    static File recentPlayersDat;

    public void initializeRecentPlayers(){
        RecentLogouts recentLogouts = new RecentLogouts();
        ArrayList<UUID> lastLogoutUUIDs = recentLogouts.getLastLogouts();
        File f = recentPlayersDat;
        String path = f.getPath();
        if (f.exists()) {
            try (BufferedReader fileReader = new BufferedReader(new FileReader(path))) {
                String line = fileReader.readLine();
                while (line != null) {
                    UUID playerUUID = UUID.fromString(line);
                    lastLogoutUUIDs.add(playerUUID);
                    line = fileReader.readLine();
                }
                recentLogouts.setLastLogouts(lastLogoutUUIDs);
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }

    public void writeRecentPlayers() {
        File f = recentPlayersDat;
        RecentLogouts recentLogouts = new RecentLogouts();
        ArrayList<UUID> lastLogouts = (ArrayList<UUID>)recentLogouts.getLastLogouts().clone(); //We clone here to avoid concurrent modification exception
        String path = f.getPath();
        if (f.exists()) {
            try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(path,false))) {
                for (UUID playerUUID : lastLogouts) {
                    fileWriter.write(playerUUID.toString());
                    fileWriter.write("\n");
                }
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }

    public void initializeRecentPlayersFile(){
        Plugin plugin = FreeRPG.getPlugin(FreeRPG.class);
        File serverData = new File(plugin.getDataFolder(), File.separator + "ServerData");
        File f = new File(serverData,"recentPlayers.dat");
        f.setReadable(true,false);
        f.setWritable(true,false);
        recentPlayersDat = f;
        //When  file is created for the first time...
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
        }
        catch (IOException error) {
            error.printStackTrace();
        }

    }

}
