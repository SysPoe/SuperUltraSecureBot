import com.google.gson.Gson;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.*;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.*;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    static DiscordApi api;
    static Config config;
    static final long serverId = 821261855753371649L;
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        api = new DiscordApiBuilder()
                .setToken(Token.token)
                .login().join();

        System.out.println("Loading config...");
        loadConfig();
        System.out.println("Loading listeners...");
        loadListeners();
        loadAuditLogListeners();
        System.out.println("Loaded listeners.");
        // System.out.println("Adding shutdown listener...");
        // addShutdownListener();
        // System.out.println("Added shutdown listener.");
        // System.out.println("Registering slash commands...");
        // registerSlashCommands();
        // System.out.println("Registered slash commands.");
    }

    private static void loadAuditLogListeners() {
        // Messages
        api.addMessageDeleteListener(messageDeleteEvent -> {
           if(messageDeleteEvent.getChannel().asTextChannel().get() != api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get()) messageDeleted(messageDeleteEvent.getMessageAuthor().get().getId(), messageDeleteEvent.getChannel().getId(), messageDeleteEvent.getMessage().get().getContent());
        });
        api.addMessageEditListener(messageEditEvent -> {
            messageEdited(messageEditEvent.getMessageAuthor().get().getId(), messageEditEvent.getChannel().getId(), messageEditEvent.getOldContent().get(), messageEditEvent.getNewContent());
        });

        // Channels
        api.addServerChannelDeleteListener(serverChannelDeleteEvent -> {
            serverChannelCreatedOrDeleted(serverChannelDeleteEvent.getChannel().getName(), "deleted");
        });
        api.addServerChannelCreateListener(serverChannelCreateEvent -> {
            serverChannelCreatedOrDeleted(serverChannelCreateEvent.getChannel().getName(), "creted");
        });
        api.addServerChannelChangeOverwrittenPermissionsListener(serverChannelChangeOverwrittenPermissionsEvent ->   {
            serverOverridePermsChange(serverChannelChangeOverwrittenPermissionsEvent.getChannel().getId(), getChangedPerms(serverChannelChangeOverwrittenPermissionsEvent.getOldPermissions(), serverChannelChangeOverwrittenPermissionsEvent.getNewPermissions()));
        });

        // Text Channels
        api.addServerChannelChangeNameListener(serverChannelChangeNameEvent -> {
            serverChannelValueChange(serverChannelChangeNameEvent.getChannel().getId(), "name", serverChannelChangeNameEvent.getOldName(), serverChannelChangeNameEvent.getNewName());
        });
        api.addServerChannelChangeNsfwFlagListener(serverChannelChangeNsfwFlagEvent -> {
            serverChannelValueChange(serverChannelChangeNsfwFlagEvent.getChannel().getId(), "NSFW Flag", String.valueOf(serverChannelChangeNsfwFlagEvent.getOldNsfwFlag()), String.valueOf(serverChannelChangeNsfwFlagEvent.getNewNsfwFlag()));
        });
        api.addServerTextChannelChangeSlowmodeListener(serverTextChannelChangeSlowmodeEvent -> {
            serverChannelValueChange(serverTextChannelChangeSlowmodeEvent.getChannel().getId(), "slow mode", serverTextChannelChangeSlowmodeEvent.getOldDelayInSeconds() + " seconds", serverTextChannelChangeSlowmodeEvent.getNewDelayInSeconds() + " seconds");
        });
        api.addServerTextChannelChangeTopicListener(serverTextChannelChangeTopicEvent -> {
            serverChannelValueChange(serverTextChannelChangeTopicEvent.getChannel().getId(), "topic", serverTextChannelChangeTopicEvent.getOldTopic(), serverTextChannelChangeTopicEvent.getNewTopic());
        });

        // Voice Channels
        api.addServerVoiceChannelChangeBitrateListener(serverVoiceChannelChangeBitrateEvent -> {
            serverChannelValueChange(serverVoiceChannelChangeBitrateEvent.getChannel().getId(), "bitrate", serverVoiceChannelChangeBitrateEvent.getOldBitrate()+"kbps", serverVoiceChannelChangeBitrateEvent.getNewBitrate()+"kbps");
        });
        api.addServerVoiceChannelChangeUserLimitListener(serverVoiceChannelChangeUserLimitEvent -> {
            serverChannelValueChange(serverVoiceChannelChangeUserLimitEvent.getChannel().getId(), "user limit", serverVoiceChannelChangeUserLimitEvent.getOldUserLimit() + " users", serverVoiceChannelChangeUserLimitEvent.getNewUserLimit() + " users");
        });

        // Invites
        api.addServerChannelInviteCreateListener(serverChannelInviteCreateEvent -> {
            inviteCreated(serverChannelInviteCreateEvent.getChannel().getId(), serverChannelInviteCreateEvent.getInvite().getInviter().get().getId(), serverChannelInviteCreateEvent.getInvite().getUrl().toString());
        });

        // Members
        api.addUserChangeActivityListener(userChangeActivityEvent -> {
            userActivityChanged(userChangeActivityEvent.getUserId(), userChangeActivityEvent.getOldActivities().stream().toList(), userChangeActivityEvent.getNewActivities().stream().toList());
        });
        api.addUserRoleAddListener(userRoleAddEvent -> {
            userRoleAdd(userRoleAddEvent.getUser().getId(), userRoleAddEvent.getRole());
        });
    }

    private static String getChangedPerms(Permissions oldPermissions, Permissions newPermissions) {
        List<PermissionType> oldAllowedPerms = oldPermissions.getAllowedPermission().stream().toList();
        List<PermissionType> newAllowedPerms = newPermissions.getAllowedPermission().stream().toList();
        List<PermissionType> oldUnsetPerms = oldPermissions.getUnsetPermissions().stream().toList();
        List<PermissionType> newUnsetPerms = newPermissions.getUnsetPermissions().stream().toList();
        List<PermissionType> oldDeniedPerms = oldPermissions.getDeniedPermissions().stream().toList();
        List<PermissionType> newDeniedPerms = newPermissions.getDeniedPermissions().stream().toList();

        AtomicReference<String> changes = new AtomicReference<>("");
        oldAllowedPerms.forEach(permissionType -> {
            if(!newAllowedPerms.contains(permissionType)) {
                if(newUnsetPerms.contains(permissionType)) changes.set(changes.get()+":yellow_square:"+permissionType.name()+"\n");
            }
        });
        newAllowedPerms.forEach(permissionType -> {
            if(!oldAllowedPerms.contains(permissionType)) {
                changes.set(changes.get()+":green_square:"+permissionType.name()+"\n");
            }
        });
        oldDeniedPerms.forEach(permissionType -> {
            if(newUnsetPerms.contains(permissionType)) changes.set(changes.get()+":yellow_square:"+permissionType.name()+"\n");
        });
        newDeniedPerms.forEach(permissionType -> {
            if(oldAllowedPerms.contains(permissionType) || oldUnsetPerms.contains(permissionType)) changes.set(changes.get()+":red_square:"+permissionType.name()+"\n");
        });
        return changes.get();
    }
    private static void addShutdownListener() {
        // Runtime.getRuntime().addShutdownHook(new Thread(Main::save));
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
                        List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to mute", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: mute");
        SlashCommand unmute = SlashCommand.with("unmute", "Un-mutes a user",
                        List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to un-mute", true)))
                .createForServer(server)
                .join();

        System.out.println("Registered command: unmute");
        SlashCommand bypass = SlashCommand.with("bypass", "Adds a user to the bypass list, meaning that they cannot be muted, kicked, or banned.",
                        List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to add", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: bypass");
        SlashCommand unbypass = SlashCommand.with("unbypass", "Removes a user to the bypass list, meaning that they will no longer have protection.",
                        List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to remove", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: unbypass");
        SlashCommand kick = SlashCommand.with("kick", "Kicks the specified user.",
                        List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to kick", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: kick");
        SlashCommand ban = SlashCommand.with("ban", "Bans the specified user.",
                        List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to ban", true)))
                .createForServer(server)
                .join();

        System.out.println("Registered command: ban");
        SlashCommand changeNick = SlashCommand.with("nick", "Lets you change the nickname of the specified user",
                        List.of(
                                SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to change the nickname of", true),
                                SlashCommandOption.create(SlashCommandOptionType.STRING, "Nickname", "The name to change it to", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: nick");
        SlashCommand novc = SlashCommand.with("novc", "Blocks the specified user from entering a voice chat.",
                        List.of(
                                SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to block", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: novc");
        SlashCommand yesvc = SlashCommand.with("yesvc", "Allows the specified user to enter a voice chat.",
                        List.of(
                                SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to allow", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: yesvc");
        SlashCommand clear = SlashCommand.with("clear", "Clears the messages in the current text channel.",
                        List.of(
                                SlashCommandOption.create(SlashCommandOptionType.INTEGER, "Messages", "The number of messages to clear", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: clear");
    }
    private static void loadListeners() {
        api.addServerVoiceChannelMemberJoinListener(event -> {
             long id = event.getUser().getId();
             try {
                 if (userIsVCBlocked(id)) event.getServer().kickUserFromVoiceChannel(event.getUser());
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
                case "clear": {
                    try {
                        long id = slashCommandInteraction.getUser().getId();
                            if (userHasAdministrator(id) || userIsBypassing(id)) {
                                slashCommandInteraction.getChannel().get().getMessages(slashCommandInteraction.getFirstOptionIntValue().get()).get().deleteAll().join();
                                slashCommandInteraction.createImmediateResponder().setContent("Cleared " + slashCommandInteraction.getFirstOptionIntValue().get() + " messages.").respond();
                            }
                            else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace(System.err);
                    }
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
                                    slashCommandInteraction.createImmediateResponder().setContent("Set <@"+id+">'s nickname to "+newName).respond();
                                } else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("You cannot do that! <@"+id+"> is on the bypass list!").respond();
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
                                    slashCommandInteraction.createImmediateResponder().setContent("Muted <@"+id+">.").respond();
                                    save();
                                } else slashCommandInteraction.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("<@" + id + "> is already muted!").respond();
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
                                    slashCommandInteraction.createImmediateResponder().setContent("Un-muted <@"+id+">.").respond();
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
                                slashCommandInteraction.createImmediateResponder().setContent("Added <@"+id+"> to the bypass list.").respond();
                                save();
                            } else slashCommandInteraction.createImmediateResponder().setContent("<@" + id + "> is already in the bypass list!").respond();
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
                                    slashCommandInteraction.createImmediateResponder().setContent("Removed <@"+id+"> from the bypass list.").respond();
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
                            String user = api.getUserById(id).get().getName();
                            if (!List.of(config.bypass).contains(user.toLowerCase())) {
                                if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                                    if (cmdName.equals("kick")) {
                                        kick(id, slashCommandInteraction.getServer().get());
                                        slashCommandInteraction.createImmediateResponder().setContent("Kicked " + user).respond();
                                    }
                                    if (cmdName.equals("ban")) {
                                        ban(id, slashCommandInteraction.getServer().get());
                                        slashCommandInteraction.createImmediateResponder().setContent("Banned " + user).respond();
                                    }
                                } else slashCommandInteraction.createImmediateResponder().setContent("You cannot " + cmdName + " <@"+id+">!").respond();
                            } else slashCommandInteraction.createImmediateResponder().setContent("You cannot " + cmdName + " <@"+id+">!").respond();
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
                                slashCommandInteraction.createImmediateResponder().setContent("Added <@"+id+"> to the block list.").respond();
                                save();
                            } else slashCommandInteraction.createImmediateResponder().setContent("<@" + id + "> is already in the block list!").respond();
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
                                    slashCommandInteraction.createImmediateResponder().setContent("Removed <@"+id+"> from the block list.").respond();
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
    private static void kick(long id, Server server) throws ExecutionException, InterruptedException {
        server.kickUser(api.getUserById(id).get());
    }
    private static void ban(long id, Server server) throws ExecutionException, InterruptedException {
        server.banUser( api.getUserById(id).get());
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
                    if(role.getAllowedPermissions().contains(PermissionType.ADMINISTRATOR)) {
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

    private static void messageDeleted(long authorId, long channelId, String content) {
        try {
            TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
            channel.sendMessage(new EmbedBuilder()
                    .setAuthor(api.getUserById(authorId).get())
                    .setColor(Color.RED)
                    .setTitle("Message deleted")
                    .setDescription("**Message by <@" + authorId + "> deleted in <#" + channelId + ">**\n" + content)
            ).join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    private static void messageEdited(long authorId, long channelId, String oldContent, String newContent) {
        try {
            TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
            channel.sendMessage(new EmbedBuilder()
                    .setAuthor(api.getUserById(authorId).get())
                    .setColor(Color.RED)
                    .setTitle("Message edited")
                    .setDescription("**Message by <@"+authorId+"> edited in <#"+channelId+">**\n" +
                            "**Old content:** "+oldContent+"\n" +
                            "**New content:** "+newContent)
            ).join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    private static void serverChannelCreatedOrDeleted(String channelName, String actionType) {
        TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
        channel.sendMessage(new EmbedBuilder()
                .setAuthor(api.getYourself())
                .setColor(Color.RED)
                .setTitle("Channel "+actionType)
                .setDescription("Channel #"+channelName+" "+actionType+".")
        ).join();
    }
    private static void serverChannelValueChange(long channelId, String actionType, String oldValue, String newValue) {
        try {
            TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
            channel.sendMessage(new EmbedBuilder()
                    .setAuthor(api.getYourself())
                    .setColor(Color.RED)
                    .setTitle("Channel "+actionType+" edited")
                    .setDescription("**<#"+channelId+">'s "+actionType+" has been changed!\n" +
                            "**Old value: **"+oldValue+"\n" +
                            "**New value: **"+newValue)
            ).join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    private static void serverOverridePermsChange(long channelId, String changes) {
        TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
        channel.sendMessage(new EmbedBuilder()
                .setAuthor(api.getYourself())
                .setColor(Color.RED)
                .setTitle("<#"+channelId+">'s override permissions have been edited")
                .setDescription("**Changes: **"+changes+"\n")
        ).join();
    }
    private static void inviteCreated(long channelId, long inviterId, String url) {
        try {
            TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
            channel.sendMessage(new EmbedBuilder()
                    .setAuthor(api.getUserById(inviterId).get())
                    .setColor(Color.RED)
                    .setTitle("Invite created")
                    .setDescription("**<@" + inviterId+ "> created invite for <#" + channelId + ">!**\n"+url)
                    .setUrl(url)
            ).join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    private static void userActivityChanged(long userId, List<Activity> oldActivity, List<Activity> newActivity) {
        try {
            TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
            AtomicReference<String> oldActivities = new AtomicReference<>("");
            AtomicReference<String> newActivites = new AtomicReference<>("");
            oldActivity.forEach(activity -> {oldActivities.set(", " + oldActivities.get() + activity);});
            newActivity.forEach(activity -> {newActivites.set(", " + newActivites.get() + activity);});
            oldActivities.set(oldActivities.get().replaceFirst(", ", ""));
            newActivites.set(newActivites.get().replaceFirst(", ", ""));
            channel.sendMessage(new EmbedBuilder()
                    .setAuthor(api.getUserById(userId).get())
                    .setColor(Color.RED)
                    .setTitle("User activity changed")
                    .setDescription("**<@"+userId+">'s activities has been changed.**\nPrevious activities: "+oldActivities.get()+"\nCurrent activities"+newActivites.get())
            ).join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    private static void userRoleAdd(long userId, Role role) {
        System.out.println(userId);
        System.out.println(new Gson().toJson(role));
        try {
            TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
            channel.sendMessage(new EmbedBuilder()
                    .setAuthor(api.getUserById(userId).get())
                    .setColor(Color.RED)
                    .setTitle("<@"+userId+">'s roles have been changed!")
                    .setDescription("**Added role: "+role.getMentionTag()+"**")
            ).join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
