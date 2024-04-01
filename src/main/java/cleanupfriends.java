import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.reflect.Type;


@ExtensionInfo(Title = "Clean up Friends", Description = "Remove all inactive friends before a specific date", Version = "1.0", Author = "Slogga")
public class cleanupfriends extends Extension {

    private final AtomicInteger progress = new AtomicInteger(0);
    private final AtomicInteger friendsRemovedCount = new AtomicInteger(0);
    private List<String> usernamesToRemove;

    public static void main(String[] args) {
        new cleanupfriends(args).run();
    }

    public cleanupfriends(String[] args) {
        super(args);
    }

    @Override
    protected void initExtension() {
        registerHandlers();
    }

    private void registerHandlers() {
        intercept(HMessage.Direction.TOSERVER, "Chat", this::onChat);
        intercept(HMessage.Direction.TOCLIENT, "ExtendedProfile", this::onExtendedProfile);
    }

    private void onChat(HMessage message) {
        String chatMessage = message.getPacket().readString();
        if (chatMessage.equals(":generatelist")) {
            new Thread(this::generateAndSaveFriendList).start();
        } else if (chatMessage.equals(":removefriends")) {
            new Thread(this::removeFriends).start();
        }
    }

    private void generateAndSaveFriendList() {
        String username = getUsernameFromConfig();
        String uniqueId = getUniqueId(username);
        getFriendsInfoAndSave(uniqueId);
        whisper("Operazione completata: Amici inattivi trovati: " + usernamesToRemove.size() + ". Ok puoi procedere.");
    }

    private void removeFriends() {
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>() {}.getType();
            try (Reader reader = new FileReader("usernames.json")) {
                usernamesToRemove = gson.fromJson(reader, type);
            }
            AtomicInteger index = new AtomicInteger(0);
            usernamesToRemove.forEach(username -> {
                HPacket packet = new HPacket("GetExtendedProfileByName", HMessage.Direction.TOSERVER, username);
                sendToServer(packet);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                int currentIndex = index.incrementAndGet();
                whisper(currentIndex + "/" + usernamesToRemove.size() + " amici eliminati.");
            });
            whisper("100% hai rimosso tutti gli amici inattivi!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void whisper(String text) {
        HPacket packet = new HPacket("Whisper", HMessage.Direction.TOCLIENT, -1, text, 0, 30, 0, -1);
        sendToClient(packet);
    }

    private void onExtendedProfile(HMessage message) {
        HPacket packet = message.getPacket();
        int userId = packet.readInteger();
        removeFriend(userId);
    }

    private void removeFriend(int userId) {
        HPacket packet = new HPacket("RemoveFriend", HMessage.Direction.TOSERVER, 1, userId);
        sendToServer(packet);
        friendsRemovedCount.incrementAndGet();
    }

    private String getUsernameFromConfig() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.ini")) {
            prop.load(input);
            return prop.getProperty("username");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String getBeforeDateFromConfig() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.ini")) {
            prop.load(input);
            return prop.getProperty("BeforeDate");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    private String getUniqueId(String username) {
        try {
            URL url = new URL("https://www.habbo.it/api/public/users?name=" + username);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                Gson gson = new Gson();
                Map<?, ?> map = gson.fromJson(content.toString(), Map.class);
                return (String) map.get("uniqueId");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getFriendsInfoAndSave(String uniqueId) {
        if (uniqueId == null) {
            whisper("Errore: Unique ID non trovato.");
            return;
        }
        String beforeDateString = getBeforeDateFromConfig();
        final List<String> usernamesBeforeDate = new ArrayList<>();
        final List<String> profilePrivate = new ArrayList<>();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date beforeDate = dateFormat.parse(beforeDateString);
            URL friendsUrl = new URL("https://www.habbo.it/api/public/users/" + uniqueId + "/friends");
            HttpURLConnection conn = (HttpURLConnection) friendsUrl.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> friendsList = gson.fromJson(responseBuilder.toString(), listType);
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < friendsList.size(); i++) {
                Map<String, Object> friend = friendsList.get(i);
                String friendName = (String) friend.get("name");
                URL userInfoUrl = new URL("https://www.habbo.it/api/public/users?name=" + friendName);
                HttpURLConnection userInfoConn = (HttpURLConnection) userInfoUrl.openConnection();
                userInfoConn.setRequestMethod("GET");
                BufferedReader userInfoReader = new BufferedReader(new InputStreamReader(userInfoConn.getInputStream()));
                StringBuilder userInfoResponseBuilder = new StringBuilder();
                while ((line = userInfoReader.readLine()) != null) {
                    userInfoResponseBuilder.append(line);
                }
                userInfoReader.close();
                Map<String, Object> userInfo = gson.fromJson(userInfoResponseBuilder.toString(), Map.class);
                String lastAccessTimeStr = (String) userInfo.get("lastAccessTime");
                if (lastAccessTimeStr != null) {
                    Date lastAccessTime = dateFormat.parse(lastAccessTimeStr);
                    if (lastAccessTime.before(beforeDate)) {
                        usernamesBeforeDate.add(friendName);
                    }
                } else {
                    profilePrivate.add(friendName);
                }
                if (System.currentTimeMillis() - startTime >= 5000) {
                    int percentageComplete = (int) (((double) (i + 1) / friendsList.size()) * 100);
                    whisper("Elaborazione completata al " + percentageComplete + "%");
                    startTime = System.currentTimeMillis();
                }
            }
            saveToFile(usernamesBeforeDate, "usernames.json");
            saveToFile(profilePrivate, "profileprivate.json");
            whisper("Operazione completata: Amici inattivi trovati: " + usernamesBeforeDate.size() + ". Ok puoi procedere.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveToFile(List<String> data, String fileName) {
        try (Writer writer = new FileWriter(fileName)) {
            new Gson().toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
