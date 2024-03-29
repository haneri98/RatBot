package bot.rat.messages;

import bot.rat.CheesePics;
import bot.rat.Jokes;
import bot.rat.entities.UserEntity;
import bot.rat.games.connect4.Connect4;
import bot.rat.services.ReminderService;
import bot.rat.services.SettingsService;
import bot.rat.services.UserService;
import bot.rat.services.WordService;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Map;

@Component
public class MessageHandler {

//    List<String> firstHalfUni = Arrays.asList("U+0078", "U+0058", "U+0425","U+0445","U+04FC", "U+04FD", "U+04FE", "U+04FF");
//    List<String> secondHalfUni = Arrays.asList("U+0044", "U+0064", "U+00D0", "U+00FE", "U+010E", "U+010F",
//            "U+0110", "U+0111", "U+0189", "U+018A", "U+01A2", "U+01F3", "U+01F2", "U+01F1", "U+01F7",
//            "U+024A", "U+024B", "U+0071", "U+1E0A", "U+1E0B", "U+03F7", "U+03F8", "U+044C");
//    String illegalStringSeperators = "[ !,.?]";

    HashSet<String> muteds = new HashSet<>();
    HashSet<String> cheeseList = new HashSet<>();
    Boolean xdIllegal = false;
    Boolean cheese = false;
    Jokes jokes = new Jokes();
    CheesePics cheesePics = new CheesePics();
    Boolean commandsDisabled = false;
    Boolean replyXd = false;
    Commands commands = new Commands();
    Connect4 connect4 = new Connect4(this);
    String selfId;

    String[] settingsToLoadOnStartupArray = new String[]{"replyXd", "cheese", "commandsDisabled", "xdIllegal"};
    Map<String, Boolean> defaultSettingValues = Map.of("replyXd", true,
                                                        "cheese", false,
                                                        "commandsDisabled", false,
                                                            "xdIllegal", false);

    @Autowired
    UserService userService;

    @Autowired
    SettingsService settingsService;

    @Autowired
    ReminderService reminderService;

    @Autowired
    WordService wordService;

    public MessageHandler() throws IOException {
    }

    public boolean isAuthorAdmin(@Nonnull MessageReceivedEvent event) {
        return userService.getUserById(event.getAuthor().getId()).getAdmin();
    }

    public boolean isAuthorMuted(@Nonnull MessageReceivedEvent event) {
        return userService.getUserById(event.getAuthor().getId()).getMuted();
    }

    public void muteUser(UserEntity user) {
        userService.addUserIfMissing(user.getId());
        user.setMuted(true);
        userService.updateUser(user);
    }

    public void unmuteUser(UserEntity user) {
        userService.addUserIfMissing(user.getId());
        user.setMuted(false);
        userService.updateUser(user);
    }

    public void startup() {
        if (settingsService.getAll().size() < settingsToLoadOnStartupArray.length) {
            for (String s : settingsToLoadOnStartupArray) {
                if (settingsService.getSettingById(s) == null) {
                    settingsService.saveSettingBoolean(s, defaultSettingValues.get(s));
                }
            }
        }
        replyXd = settingsService.getSettingById(settingsToLoadOnStartupArray[0]) != null ? settingsService.getSettingById(settingsToLoadOnStartupArray[0]).getBool() : replyXd;
        cheese = settingsService.getSettingById(settingsToLoadOnStartupArray[1]) != null ? settingsService.getSettingById(settingsToLoadOnStartupArray[1]).getBool() : cheese;
        commandsDisabled = settingsService.getSettingById(settingsToLoadOnStartupArray[2]) != null ? settingsService.getSettingById(settingsToLoadOnStartupArray[2]).getBool() : commandsDisabled;
        xdIllegal = settingsService.getSettingById(settingsToLoadOnStartupArray[3]) != null ? settingsService.getSettingById(settingsToLoadOnStartupArray[3]).getBool() : xdIllegal;
    }

    public void onGuildMessageReceived(@Nonnull MessageReceivedEvent event) {
        String msg = event.getMessage().getContentRaw();
        try {
            userService.addUserIfMissing(event.getAuthor().getId());
            if (!event.getAuthor().isBot()) {
                if (muteHandler(event)) {
                    return;
                }
                if (msg.length() > 4 && msg.substring(0, 5).equals("!rat ")) {
                    commandHandler(msg.substring(5), event);
                } else {
                    wordService.recordWords(event, event.getMessage().getContentRaw());
                    if (msg.toLowerCase().contains("right, ratbot?")) {
                        String word = wordService.getRightResponse();
                        if (word.equals("UHM")) {
                            String uhmFormatted = event.getGuild().getEmojisByName("uhm", true).get(0).getFormatted();
                            event.getChannel().sendMessage(uhmFormatted).queue();
                        } else {
                            event.getChannel().sendMessage(word).queue();
                        }
                    }
                    if (cheese) {
                        cheeseHandler(event);
                    }
                    if (!manageIllegalMessage(event)){
                        userService.giveUserPoints(event);
                    }
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString().substring(0, 800);
            event.getMessage().getChannel().sendMessage("Uh oh! RatBot did a fucky wucky. UwU\n" +
                    "Message was: " + event.getMessage().getContentRaw() + "\n" +
                    stackTrace).queue();
        }
    }

    private boolean manageIllegalMessage(@Nonnull MessageReceivedEvent event) {
//        char[] chars = event.getMessage().getContentRaw().replaceAll(illegalStringSeperators, "").toCharArray();
//        boolean xFound = false;
//        for (char c : chars) {
//            if (xFound) {
//                if (secondHalfUni.contains(String.format("U+%04X", (int) c))) {
//                    if (xdIllegal && !isAuthorAdmin(event)) {
//                        event.getMessage().delete().queue();
//                    } else {
//                        if (replyXd) {
//                            event.getMessage().getChannel().sendMessage("xd").queue();
//                        }
//                    }
//                    return;
//                }
//            }
//            if (firstHalfUni.contains(String.format("U+%04X", (int) c))) {
//                xFound = true;
//            } else {
//                xFound = false;
//            }
//        }
        if (event.getMessage().getContentRaw().toLowerCase().contains("xd")) {
            if (xdIllegal && !isAuthorAdmin(event)) {
                event.getMessage().delete().queue();
                return true;
            } else if (replyXd) {
                event.getMessage().getChannel().sendMessage("xd").queue();
            }
        }
        return false;
    }

    private void commandHandler(String message, @Nonnull MessageReceivedEvent event) {
        if (!commandsDisabled) {
            if (isAuthorAdmin(event)) {
                adminCommandHandler(message, event);
            }
            userCommandHandler(message, event);
        } else {
            commandEnableHandler(message, event);
        }
    }

    private void adminCommandHandler(String message, @Nonnull MessageReceivedEvent event) {
        commands.adminCommandHandler(message, event, this);
    }

    private void userCommandHandler(String message, @Nonnull MessageReceivedEvent event) {
        commands.userCommandHandler(message, event, this);
    }

    private void commandEnableHandler(String message, @Nonnull MessageReceivedEvent event) {
        if (message.equals("ec") || message.equals("enable commands")) {
            commandsDisabled = commands.enableCommands(event, this);
        }
    }

    private boolean muteHandler(@Nonnull MessageReceivedEvent event) throws RateLimitedException {
        if (isAuthorMuted(event)) {
            event.getMessage().delete().complete(true);
            return true;
        }
        return false;
    }

    private void cheeseHandler(@Nonnull MessageReceivedEvent event) {
        // TODO: Fix whatever this shit used to be
//        event.getMessage().addReaction("U+1F9C0").queue();
//        if (event.getMessage().getContentRaw().toLowerCase().contains("cheese")) {
//            String id = event.getAuthor().getId();
//            if (cheeseList.add(id)) {
//                event.getMessage().getChannel().sendMessage(
//                        "<@" + id + "> has been added to the cheese list.").queue();
//            }
//        }
    }

    protected UserEntity getUser(String id) {
        return userService.getUserById(id);
    }

    /**
     *  Auto generated generic code below
     */

    public Boolean getXdIllegal() {
        return xdIllegal;
    }

    public void setXdIllegal(Boolean xdIllegal) {
        this.xdIllegal = xdIllegal;
    }

    public Boolean getCheese() {
        return cheese;
    }

    public void setCheese(Boolean cheese) {
        this.cheese = cheese;
    }

    public Boolean getCommandsDisabled() {
        return commandsDisabled;
    }

    public void setCommandsDisabled(Boolean commandsDisabled) {
        this.commandsDisabled = commandsDisabled;
    }

    public Boolean getReplyXd() {
        return replyXd;
    }

    public void setReplyXd(Boolean replyXd) {
        this.replyXd = replyXd;
    }

    public HashSet<String> getMuteds() {
        return muteds;
    }

    public Jokes getJokes() {
        return jokes;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public UserService getUserService() {
        return userService;
    }

    public CheesePics getCheesePics() {
        return cheesePics;
    }

    public Connect4 getConnect4() {
        return connect4;
    }

    public ReminderService getReminderService() {
        return reminderService;
    }

    public WordService getWordService() {
        return wordService;
    }

    public void setWordService(WordService wordService) {
        this.wordService = wordService;
    }
}
