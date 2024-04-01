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

    private String getDomainFromConfig() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.ini")) {
            prop.load(input);
            return prop.getProperty("domain", "habbo.com");
        } catch (IOException ex) {
            ex.printStackTrace();
            return "habbo.com";
        }
    }

    private void onChat(HMessage message) {
        String chatMessage = message.getPacket().readString();
        String domain = getDomainFromConfig();
        String username = getUsernameFromConfig();
        String beforeDate = getBeforeDateFromConfig();

        // Messaggi per :generatelist
        String generatelistMessage = "";
        // Messaggi per :removefriends
        String removefriendsMessage = "";

        switch (domain) {
            case "habbo.com.br":
                generatelistMessage = String.format("Ei %s, começando a procurar todos os amigos inativos no habbo.com.br antes de %s, isso pode levar alguns minutos µ", username, beforeDate);
                removefriendsMessage = "Começando a remover todos os amigos inativos ‡";
                break;
            case "habbo.com":
                generatelistMessage = String.format("Hey %s, starting to search for all inactive friends on habbo.com before %s, this might take a few minutes µ", username, beforeDate);
                removefriendsMessage = "Starting to remove all inactive friends ‡";
                break;
            case "habbo.de":
                generatelistMessage = String.format("Hallo %s, ich beginne damit, alle inaktiven Freunde auf habbo.de vor dem %s zu suchen, das könnte einige Minuten dauern µ", username, beforeDate);
                removefriendsMessage = "Beginne damit, alle inaktiven Freunde zu entfernen ‡";
                break;
            case "habbo.es":
                generatelistMessage = String.format("Hola %s, empezando a buscar todos los amigos inactivos en habbo.es antes del %s, esto puede tardar unos minutos µ", username, beforeDate);
                removefriendsMessage = "Empezando a eliminar todos los amigos inactivos ‡";
                break;
            case "habbo.fi":
                generatelistMessage = String.format("Hei %s, aloitan etsimään kaikkia passiivisia ystäviä habbo.fi ennen %s, tämä voi kestää muutaman minuutin µ", username, beforeDate);
                removefriendsMessage = "Aloitetaan kaikkien passiivisten ystävien poistaminen ‡";
                break;
            case "habbo.fr":
                generatelistMessage = String.format("Salut %s, je commence à chercher tous les amis inactifs sur habbo.fr avant le %s, cela pourrait prendre quelques minutes µ", username, beforeDate);
                removefriendsMessage = "Commencer à supprimer tous les amis inactifs ‡";
                break;
            case "habbo.it":
                generatelistMessage = String.format("Ciao %s, inizio a cercare tutti gli amici inattivi su habbo.it prima del %s, potrebbe volerci qualche minuto µ", username, beforeDate);
                removefriendsMessage = "Inizio ad eliminare tutti gli amici inattivi ‡";
                break;
            case "habbo.nl":
                generatelistMessage = String.format("Hey %s, ik begin met het zoeken naar alle inactieve vrienden op habbo.nl voor %s, dit kan een paar minuten duren µ", username, beforeDate);
                removefriendsMessage = "Begin met het verwijderen van alle inactieve vrienden ‡";
                break;
            case "habbo.com.tr":
                generatelistMessage = String.format("Merhaba %s, tüm inaktif arkadaşları habbo.com.tr üzerinde %s öncesinde aramaya başlıyorum, bu birkaç dakika sürebilir µ", username, beforeDate);
                removefriendsMessage = "Tüm inaktif arkadaşların kaldırılmasına başlanıyor ‡";
                break;
            default:
                generatelistMessage = "Domain not found for messages µ";
                removefriendsMessage = "Domain not found for messages ‡";
                break;
        }

        if (chatMessage.equals(":generatelist")) {
            message.setBlocked(true);
            whisper(generatelistMessage);
            new Thread(this::generateAndSaveFriendList).start();
        } else if (chatMessage.equals(":removefriends")) {
            message.setBlocked(true);
            whisper(removefriendsMessage);
            new Thread(this::removeFriends).start();
        }
    }



    private void generateAndSaveFriendList() {
        String username = getUsernameFromConfig();
        String uniqueId = getUniqueId(username);
        getFriendsInfoAndSave(uniqueId);
        whisper("¶ Operazione completata: Amici inattivi trovati: " + usernamesToRemove.size() + ". ª Per procedere con l'eliminazione scrivi :removefriends ª");
    }

    private void removeFriends() {
        String domain = getDomainFromConfig();
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
                switch (domain) {
                    case "habbo.com.br":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " amigos eliminados. ª");
                        break;
                    case "habbo.de":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " Freunde gelöscht. ª");
                        break;
                    case "habbo.es":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " amigos eliminados. ª");
                        break;
                    case "habbo.fi":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " ystävää poistettu. ª");
                        break;
                    case "habbo.fr":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " amis supprimés. ª");
                        break;
                    case "habbo.it":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " amici eliminati. ª");
                        break;
                    case "habbo.nl":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " vrienden verwijderd. ª");
                        break;
                    case "habbo.com.tr":
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " arkadaş silindi. ª");
                        break;
                    default:
                        whisper(currentIndex + "/" + usernamesToRemove.size() + " friends removed. ª");
                        break;
                }
            });
            switch (domain) {
                case "habbo.com.br":
                    whisper("100% dos amigos inativos foram removidos!");
                    break;
                case "habbo.de":
                    whisper("100% der inaktiven Freunde wurden entfernt!");
                    break;
                case "habbo.es":
                    whisper("¡100% de los amigos inactivos han sido eliminados!");
                    break;
                case "habbo.fi":
                    whisper("100% passiivisista ystävistä poistettu!");
                    break;
                case "habbo.fr":
                    whisper("100% des amis inactifs ont été supprimés !");
                    break;
                case "habbo.it":
                    whisper("100% hai rimosso tutti gli amici inattivi!");
                    break;
                case "habbo.nl":
                    whisper("100% van de inactieve vrienden zijn verwijderd!");
                    break;
                case "habbo.com.tr":
                    whisper("%100 inaktif arkadaş kaldırıldı!");
                    break;
                default:
                    whisper("100% of inactive friends have been removed!");
                    break;
            }
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
        message.setBlocked(true);
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
        String domain = getDomainFromConfig();
        try {
            URL url = new URL("https://www." + domain + "/api/public/users?name=" + username);
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
        String domain = getDomainFromConfig();
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
            URL friendsUrl = new URL("https://www." + domain + "/api/public/users/" + uniqueId + "/friends");
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
                URL userInfoUrl = new URL("https://www." + domain + "/api/public/users?name=" + friendName);
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
                    switch (domain) {
                        case "habbo.com.br":
                            whisper("º Pesquisa concluída em " + percentageComplete + "%");
                            break;
                        case "habbo.de":
                            whisper("º Suche abgeschlossen bei " + percentageComplete + "%");
                            break;
                        case "habbo.es":
                            whisper("º Búsqueda completada en " + percentageComplete + "%");
                            break;
                        case "habbo.fi":
                            whisper("º Haku valmis " + percentageComplete + "%");
                            break;
                        case "habbo.fr":
                            whisper("º Recherche terminée à " + percentageComplete + "%");
                            break;
                        case "habbo.it":
                            whisper("º Ricerca completata al " + percentageComplete + "%");
                            break;
                        case "habbo.nl":
                            whisper("º Zoeken voltooid op " + percentageComplete + "%");
                            break;
                        case "habbo.com.tr":
                            whisper("º Arama tamamlandı " + percentageComplete + "%");
                            break;
                        default:
                            whisper("º Search completed at " + percentageComplete + "%");
                            break;
                    }
                    startTime = System.currentTimeMillis();
                }
            }
            saveToFile(usernamesBeforeDate, "usernames.json");
            saveToFile(profilePrivate, "profileprivate.json");
            switch (domain) {
                case "habbo.com.br":
                    whisper("¶ Operação completada: Amigos inativos encontrados: " + usernamesBeforeDate.size() + ". Para prosseguir com a remoção digite :removefriends");
                    break;
                case "habbo.de":
                    whisper("¶ Vorgang abgeschlossen: Inaktive Freunde gefunden: " + usernamesBeforeDate.size() + ". Zum Fortfahren mit dem Löschen tippe :removefriends");
                    break;
                case "habbo.es":
                    whisper("¶ Operación completada: Amigos inactivos encontrados: " + usernamesBeforeDate.size() + ". Para proceder con la eliminación escribe :removefriends");
                    break;
                case "habbo.fi":
                    whisper("¶ Toiminto valmis: Passiiviset ystävät löydetty: " + usernamesBeforeDate.size() + ". Jatkaaksesi poistamista kirjoita :removefriends");
                    break;
                case "habbo.fr":
                    whisper("¶ Opération terminée: Amis inactifs trouvés: " + usernamesBeforeDate.size() + ". Pour procéder à la suppression, tapez :removefriends");
                    break;
                case "habbo.it":
                    whisper("¶ Operazione completata: Amici inattivi trovati: " + usernamesBeforeDate.size() + ". Per procedere con l'eliminazione scrivi :removefriends");
                    break;
                case "habbo.nl":
                    whisper("¶ Bewerking voltooid: Inactieve vrienden gevonden: " + usernamesBeforeDate.size() + ". Om door te gaan met verwijderen typ :removefriends");
                    break;
                case "habbo.com.tr":
                    whisper("¶ İşlem tamamlandı: Etkin olmayan arkadaşlar bulundu: " + usernamesBeforeDate.size() + ". Silme işlemine devam etmek için :removefriends yazın");
                    break;
                default:
                    whisper("¶ Operation completed: Inactive friends found: " + usernamesBeforeDate.size() + ". To proceed with the deletion type :removefriends");
                    break;
            }
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
