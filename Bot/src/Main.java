import com.google.gson.Gson;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberJoinEvent;
import org.javacord.api.interaction.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    static DiscordApi api;
    static Config config;
    static final long serverId = 875516599417831505L;
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        api = new DiscordApiBuilder()
                .setToken("ODc1NTAwNjE4ODU1MzgzMDUx.YRWbfQ.5MXflvGJ8SMN8rc6Ruz5mZgoyEQ")
                .login().join();

        System.out.println("Loading config...");
        loadConfig();
        System.out.println("Loading listeners...");
        loadListeners();
        System.out.println("Adding shutdown listener...");
        addShutdownListener();
        System.out.println("Registering slash commands...");
        registerSlashCommands();
    }
    private static void addShutdownListener() {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::save));
    }
    private static void save() {
        try {
            FileWriter fileWriter = new FileWriter("G:\\projects\\SuperUltraSecureBot\\out\\artifacts\\SuperUltraSecureBot_jar\\config.json", false);
            fileWriter.write(new Gson().toJson(config, Config.class));
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    private static void registerSlashCommands() throws ExecutionException, InterruptedException {
        api.getGlobalSlashCommands().get().forEach(SlashCommand::deleteGlobal);

        Server server = api.getServerById(serverId).get();
        SlashCommand ping = SlashCommand.with("ping", "Checks the functionality of this command")
                .createForServer(server)
                .join();
        System.out.println("Registered command: ping");

        SlashCommand mute = SlashCommand.with("mute", "Mutes a user",
                        List.of(SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to mute")))
                .createForServer(server)
                .join();
        System.out.println("Registered command: mute");
        SlashCommand unmute = SlashCommand.with("unmute", "Un-mutes a user",
                        List.of(SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to un-mute")))
                .createForServer(server)
                .join();

        System.out.println("Registered command: unmute");
        SlashCommand bypass = SlashCommand.with("bypass", "Adds a user to the bypass list, meaning that they cannot be muted, kicked, or banned.",
                        List.of(SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to add")))
                .createForServer(server)
                .join();
        System.out.println("Registered command: bypass");
        SlashCommand unbypass = SlashCommand.with("unbypass", "Removes a user to the bypass list, meaning that they will no longer have protection.",
                        List.of(SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to remove")))
                .createForServer(server)
                .join();
        System.out.println("Registered command: unbypass");
        SlashCommand kick = SlashCommand.with("kick", "Kicks the specified user.",
                        List.of(SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to kick")))
                .createForServer(server)
                .join();
        System.out.println("Registered command: kick");
        SlashCommand ban = SlashCommand.with("ban", "Bans the specified user.",
                        List.of(SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to ban")))
                .createForServer(server)
                .join();

        System.out.println("Registered command: ban");
        SlashCommand changeNick = SlashCommand.with("nick", "Lets you change the nickname of the specified user",
                        List.of(
                                SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to change the nickname of"),
                                SlashCommandOption.createWithOptions(SlashCommandOptionType.STRING, "Nickname", "The name to change it to")))
                .createForServer(server)
                .join();
        System.out.println("Registered command: nick");
        SlashCommand novc = SlashCommand.with("novc", "Blocks the specified user from entering a voice chat.",
                        List.of(
                                SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to block")))
                .createForServer(server)
                .join();
        System.out.println("Registered command: novc");

        SlashCommand yesvc = SlashCommand.with("yesvc", "Allows the specified user to enter a voice chat.",
                        List.of(
                                SlashCommandOption.createWithOptions(SlashCommandOptionType.USER, "User", "The user to allow")))
                .createForServer(server)
                .join();
        System.out.println("Registered command: yesvc");
    }
    private static void loadListeners() {
        api.addServerVoiceChannelMemberJoinListener(event -> {
             long id = event.getUser().getId();
             try {
                 if (userIsVCBlocked(id)) event.getUser().;
             } catch (Exception e){
                 System.err.println(e.getMessage());
                 e.printStackTrace();
             }
        });
        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
            String cmdName = slashCommandInteraction.getCommandName().toLowerCase(Locale.ROOT);
            long userId = slashCommandInteraction.getUser().getId();
            try {
                if(!userHasAdministrator(userId) && !userIsBypassing(userId)) {
                    slashCommandInteraction.createImmediateResponder().setContent("You are not allowed to do that!").respond();
                    return;
                }
            }
            catch (Exception e) { System.err.println(e.getMessage()); e.printStackTrace(); }
            switch (cmdName) {
                case "ping": {
                    slashCommandInteraction.createImmediateResponder()
                            .setContent("Pong!")
                            .respond();
                    break;
                }
                case "nick": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(id).get().getName();
                            String newName = slashCommandInteraction.getSecondOptionStringValue().get();
                            if (!List.of(config.bypass).contains(user.toLowerCase())) {
                                if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                                    api.getUserById(id).get().updateNickname(slashCommandInteraction.getServer().get(), newName);
                                    slashCommandInteraction.createImmediateResponder().setContent("Set "+user+"'s nickname to "+newName).respond();
                                } else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("You cannot do that! " + user + " is on the bypass list!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to change the nickname of that user. Please check the console.").respond();
                            System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
                case "mute": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(id).get().getName();
                            if (!List.of(config.muted).contains(user.toLowerCase())) {
                                if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                                    ArrayList<String> muted = new ArrayList<>(List.of(config.muted));
                                    muted.add(user.toLowerCase());
                                    config.muted = oA2sA(muted.toArray());
                                    slashCommandInteraction.createImmediateResponder().setContent("Muted " + user + ".").respond();
                                    save();
                                } else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent(user + " is already muted!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to mute user. Please check the console.").respond();
                            System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
                case "unmute": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(id).get().getName();
                            if (List.of(config.muted).contains(user.toLowerCase())) {
                                if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                                    ArrayList<String> muted = new ArrayList<>(List.of(config.muted));
                                    muted.remove(user.toLowerCase());
                                    config.muted = oA2sA(muted.toArray());
                                    slashCommandInteraction.createImmediateResponder().setContent("Un-muted " + user + ".").respond();
                                    save();
                                } else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("That user isn't muted!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to un-mute user. Please check the console.").respond();
                        System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
                case "bypass": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(id).get().getName();
                            if (!List.of(config.bypass).contains(user.toLowerCase())) {
                                ArrayList<String> bypass = new ArrayList<>(List.of(config.bypass));
                                bypass.add(user.toLowerCase());
                                config.bypass = oA2sA(bypass.toArray());
                                slashCommandInteraction.createImmediateResponder().setContent("Added " + user + " to the bypass list.").respond();
                                save();
                            } else slashCommandInteraction.createImmediateResponder().setContent(user + " is already in the bypass list!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to add user to the bypass list. Please check the console.").respond();
                            System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
                case "unbypass": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(id).get().getName();
                            if (List.of(config.bypass).contains(user.toLowerCase())) {
                                if (!userHasAdministrator(id)) {
                                    ArrayList<String> bypass = new ArrayList<>(List.of(config.bypass));
                                    bypass.remove(user.toLowerCase());
                                    config.bypass = oA2sA(bypass.toArray());
                                    slashCommandInteraction.createImmediateResponder().setContent("Removed " + user + " from the bypass list.").respond();
                                    save();
                                } else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("That user isn't on the bypass list!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to remove user from the bypass list. Please check the console.").respond();
                            System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
                case "kick", "ban": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get())).get().getName();
                            if (!List.of(config.bypass).contains(user.toLowerCase())) {
                                if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                                    if (cmdName.equals("kick")) kick(id);
                                    if (cmdName.equals("ban")) ban(id);
                                } else slashCommandInteraction.createImmediateResponder().setContent("You cannot " + cmdName + " " + user + "!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("You cannot " + cmdName + " " + user + "!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to " + cmdName + " user! Please check the console.").respond();
                            System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
                case "novc": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(id).get().getName();
                            if (!List.of(config.novoicechat).contains(user.toLowerCase())) {
                                ArrayList<String> novoicechat = new ArrayList<>(List.of(config.novoicechat));
                                novoicechat.add(user.toLowerCase());
                                config.novoicechat = oA2sA(novoicechat.toArray());
                                slashCommandInteraction.createImmediateResponder().setContent("Added " + user + " to the block list.").respond();
                                save();
                            } else slashCommandInteraction.createImmediateResponder().setContent(user + " is already in the block list!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to add user to the block list. Please check the console.").respond();
                            System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
                case "yesvc": {
                    try {
                        if (config != null) {
                            long id = Long.parseLong(slashCommandInteraction.getFirstOptionStringValue().get());
                            String user = api.getUserById(id).get().getName();
                            if (List.of(config.novoicechat).contains(user.toLowerCase())) {
                                if (!userHasAdministrator(id)) {
                                    ArrayList<String> novoicechat = new ArrayList<>(List.of(config.novoicechat));
                                    novoicechat.remove(user.toLowerCase());
                                    config.novoicechat = oA2sA(novoicechat.toArray());
                                    slashCommandInteraction.createImmediateResponder().setContent("Removed " + user + " from the block list.").respond();
                                    save();
                                } else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("That user isn't on the block list!").respond();
                        } else {
                            slashCommandInteraction.createImmediateResponder().setContent("Unable to remove user from the block list. Please check the console.").respond();
                            System.err.println("Main.config is null!");
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
                    break;
                }
            }
        });
        api.addMessageCreateListener(event -> {
            if(!List.of(config.bypass).contains(event.getMessageAuthor().getName())) {
                boolean canSend = true;
                for (String blockedWord : config.blockedWords) if(event.getMessage().getContent().toLowerCase().contains(blockedWord.toLowerCase())) canSend = false;
                if(!canSend) event.getMessage().delete("Contains a blocked word");
            }
            if(List.of(config.muted).contains(event.getMessageAuthor().getName().toLowerCase())) {
                event.getMessage().delete("User muted.");
            }
        });
    }
    private static void kick(long id) throws ExecutionException, InterruptedException {
        api.getServerById(id).get().kickUser(api.getUserById(id).get());
    }
    private static void ban(long id) throws ExecutionException, InterruptedException {
        api.getServerById(id).get().banUser( api.getUserById(id).get());
    }
    private static boolean userIsVCBlocked(long id) throws ExecutionException, InterruptedException {
        return List.of(config.novoicechat).contains(api.getUserById(id).get().getName().toLowerCase(Locale.ROOT));
    }
    private static boolean userIsBypassing(long id) throws ExecutionException, InterruptedException {
        return List.of(config.bypass).contains(api.getUserById(id).get().getName().toLowerCase(Locale.ROOT));
    }
    private static boolean userHasAdministrator(long id) throws ExecutionException, InterruptedException {
        AtomicBoolean hasPerms = new AtomicBoolean(false);
        api.getUserById(id)
                .get()
                .getRoles(api.getServerById(serverId)
                        .get())
                .forEach(role ->
                {
                    if(role.getPermissions().getAllowedPermission().contains(PermissionType.ADMINISTRATOR)) {
                        hasPerms.set(true);
                    }
                });
        return hasPerms.get();
    }
    private static String[] oA2sA(Object[] array) {
        String[] result = new String[array.length];
        for(int i = 0; i < array.length; i++) { result[i] = String.valueOf(array[i]); }
        return result;
    }
    private static void loadConfig() {
        try {
            StringBuilder cfg = new StringBuilder();
            File file = new File("G:\\projects\\SuperUltraSecureBot\\out\\artifacts\\SuperUltraSecureBot_jar\\config.json");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                System.out.println(data);
                cfg.append(data);
            }
            scanner.close();
            config = new Gson().fromJson(cfg.toString(), Config.class);
            System.out.println("Config loaded.");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
