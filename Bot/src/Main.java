import com.google.gson.Gson;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    static DiscordApi api;
    static Config config;
    static final long serverId = 821261855753371649L;
    static String commandPrefix = "security!";
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("Loading config...");
        loadConfig();
        System.out.println("Logging in...");
        api = new DiscordApiBuilder()
                .setToken(config.token)
                .login().join();

        System.out.println("Loading listeners...");
        loadListeners();
        loadAuditLogListeners();
        loadOtherCommandListeners();
        System.out.println("Loaded listeners.");

        api.getServers().forEach(server -> {
            try {
                // Reset Nickname
                api.getYourself().resetNickname(server).get();

                // Get role height
                List<Role> roles = api.getYourself().getRoles(server);
                Role toprole = server.getRoles().get(server.getRoles().size()-1);
                System.out.println("Role position for server "+server.getName()+": " + roles.get(roles.size()-1).getPosition()+" out of "+toprole.getPosition());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // System.out.println("Adding shutdown listener...");
        // addShutdownListener();
        // System.out.println("Added shutdown listener.");

        // System.out.println("Registering slash commands...");
        // clearSlashCommands();
        // registerSlashCommands();
        // System.out.println("Registered slash commands.");
    }

    public static void save() {
        try {
            FileWriter fileWriter = new FileWriter("G:\\projects\\SuperUltraSecureBot\\out\\artifacts\\SuperUltraSecureBot_jar\\config.json", false);
            fileWriter.write(new Gson().toJson(config, Config.class));
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void loadAuditLogListeners() {
        // Messages
        api.addMessageDeleteListener(messageDeleteEvent -> {
            if (messageDeleteEvent.getChannel().asTextChannel().get()
                    != api.getServerById(config.auditLogServerId).get()
                    .getTextChannelById(config.auditLogChannelId).get()) {
                int embedNumber = 0;
                StringBuilder embeds;
                if(messageDeleteEvent.getMessage().get().getEmbeds().size() > 0) {
                    embeds = new StringBuilder("**Embeds: **\n");
                    for (Embed embed : messageDeleteEvent.getMessage().get().getEmbeds()) {
                        embedNumber++;
                        embeds.append("**Embed " + embedNumber + ": \n....Title: **" + embed.getTitle().get() + "\n**....Description: **" + embed.getDescription().get() + "\n");
                    }
                } else {
                    embeds = new StringBuilder("");
                }
                messageDeleted(messageDeleteEvent.getMessageAuthor().get().getId(),
                        messageDeleteEvent.getChannel().getId(),
                        messageDeleteEvent.getMessage().get().getContent(), embeds.toString());
            }
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
            serverOverridePermsChange(serverChannelChangeOverwrittenPermissionsEvent.getChannel().getId(), getChangedPerms(serverChannelChangeOverwrittenPermissionsEvent.getOldPermissions(), serverChannelChangeOverwrittenPermissionsEvent.getNewPermissions()), serverChannelChangeOverwrittenPermissionsEvent.getRole().get().getId());
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
    private static void registerSlashCommands() throws ExecutionException, InterruptedException {
        api.getServers().forEach(server -> {
            SlashCommand ping = SlashCommand.with("ping", "Checks the functionality of this command")
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: ping for server: "+server.getName());

            SlashCommand mute = SlashCommand.with("mute", "Mutes a user",
                            List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to mute", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: mute for server: "+server.getName());
            SlashCommand unmute = SlashCommand.with("unmute", "Un-mutes a user",
                            List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to un-mute", true)))
                    .createForServer(server)
                    .join();

            System.out.println("Registered command: unmute for server: "+server.getName());
            SlashCommand bypass = SlashCommand.with("bypass", "Adds a user to the bypass list, meaning that they cannot be muted, kicked, or banned.",
                            List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to add", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: bypass for server: "+server.getName());
            SlashCommand unbypass = SlashCommand.with("unbypass", "Removes a user to the bypass list, meaning that they will no longer have protection.",
                            List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to remove", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: unbypass for server: "+server.getName());
            SlashCommand kick = SlashCommand.with("kick", "Kicks the specified user.",
                            List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to kick", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: kick for server: "+server.getName());
            SlashCommand ban = SlashCommand.with("ban", "Bans the specified user.",
                            List.of(SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to ban", true)))
                    .createForServer(server)
                    .join();

            System.out.println("Registered command: ban for server: "+server.getName());
            SlashCommand changeNick = SlashCommand.with("nick", "Lets you change the nickname of the specified user",
                            List.of(
                                    SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to change the nickname of", true),
                                    SlashCommandOption.create(SlashCommandOptionType.STRING, "Nickname", "The name to change it to", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: nick for server: "+server.getName());
            SlashCommand novc = SlashCommand.with("novc", "Blocks the specified user from entering a voice chat.",
                            List.of(
                                    SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to block", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: novc for server: "+server.getName());
            SlashCommand yesvc = SlashCommand.with("yesvc", "Allows the specified user to enter a voice chat.",
                            List.of(
                                    SlashCommandOption.create(SlashCommandOptionType.USER, "User", "The user to allow", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: yesvc for server: "+server.getName());
            SlashCommand clear = SlashCommand.with("clear", "Clears the messages in the current text channel.",
                            List.of(
                                    SlashCommandOption.create(SlashCommandOptionType.INTEGER, "Messages", "The number of messages to clear", true),
                                    SlashCommandOption.create(SlashCommandOptionType.BOOLEAN, "Silent", "Whether or not to show the delete message", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: clear for server: "+server.getName());
            SlashCommand givemerank = SlashCommand.with("givemerank", "Gives you the rank",
                            List.of(
                                    SlashCommandOption.create(SlashCommandOptionType.ROLE, "Role", "The role to give you.", true)))
                    .createForServer(server)
                    .join();
            System.out.println("Registered command: givemerank for server: "+server.getName());
        });
    }
    private static void clearSlashCommands() {
        try {api.getGlobalSlashCommands().get().forEach(SlashCommand::deleteGlobal);}
        catch (Exception e) {e.printStackTrace();}
        api.getServers().forEach(server -> {
            try {
                api.getServerSlashCommands(server).get().forEach(slashCommand -> {
                    slashCommand.deleteForServer(server).join();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
                case "givemerank": { giveMeRank(slashCommandInteraction); break; }
                case "ping": { ping(slashCommandInteraction); break;}
                case "clear": { clear(slashCommandInteraction); break; }
                case "nick": { nick(slashCommandInteraction); break; }
                case "mute": { mute(slashCommandInteraction); break; }
                case "unmute": { unmute(slashCommandInteraction); break; }
                case "bypass": { bypass(slashCommandInteraction); break; }
                case "unbypass": { unbypass(slashCommandInteraction); break; }
                case "kick", "ban": { kickorban(slashCommandInteraction, cmdName); break; }
                case "novc": { novc(slashCommandInteraction); break; }
                case "yesvc": { yesvc(slashCommandInteraction); break; }
            }
        });
        api.addMessageCreateListener(event -> {
            if(!event.getMessageAuthor().isBotUser()) {
                // Blocked words
                if (!config.bypass.contains(event.getMessageAuthor().getId())) {
                    boolean canSend = true;
                    for (String blockedWord : config.blockedWords)
                        if (event.getMessage().getContent().toLowerCase().contains(blockedWord.toLowerCase()))
                            canSend = false;
                    if (!canSend) {
                        event.getMessage().delete("Contains a blocked word");
                    }
                }
                // Muted users
                if (MutedUser.userIsMuted(config, event.getMessageAuthor().getId())) {
                    api.getServerById(config.mutedLogServerId).get().getTextChannelById(config.mutedLogChannelId).get().sendMessage(new EmbedBuilder()
                            .setAuthor(event.getMessageAuthor())
                            .setTitle("Message from muted user deleted!")
                            .setDescription("**User: **<@"+event.getMessageAuthor().getId()+">\n**Message: **"+event.getMessageContent())
                            );
                    event.getMessage().delete("User muted.");
                }
            }
        });
    }
    private static void loadOtherCommandListeners() {
        api.addMessageCreateListener(messageCreateEvent -> {
            String message = messageCreateEvent.getMessageContent();
            if(message.startsWith(commandPrefix)) {
                String command = messageCreateEvent.getMessageContent().toLowerCase().replaceFirst(commandPrefix,"").split(" ")[0];
                String author = messageCreateEvent.getMessageAuthor().getName()+"#"+messageCreateEvent.getMessageAuthor().getDiscriminator().get();
                try {
                    switch (command) {
                        case "warn": {
                            String userId = message.toLowerCase().split(" ")[1].replaceFirst("<@!", "").replaceFirst(">", "");
                            String reason;
                            reason = message.replaceFirst("&warn <@!"+userId+">", "").replaceFirst(" ", "");
                            if(reason.equals("")) reason = "unspecified";
                            User u = api.getUserById(userId).get();
                            String description;
                            if(!reason.equals("none")) description = "**You have been warned by "+author+"!\nReason: **"+reason;
                            else description = "**You have been warned by "+author+"!**";
                            u.openPrivateChannel().get().sendMessage(new EmbedBuilder()
                                    .setTitle("You have been warned!")
                                    .setColor(Color.yellow)
                                    .setDescription(description)
                                    .setAuthor(messageCreateEvent.getMessageAuthor()));
                            WarnedUser.increment(messageCreateEvent.getMessageAuthor().asUser().get());
                            messageCreateEvent.getMessage().reply(new EmbedBuilder()
                                    .setAuthor(api.getUserById(userId).get())
                                    .setColor(Color.yellow)
                                    .setDescription(":white_check_mark: Warned <@"+userId+">.\nReason: "+reason)).get();
                            save();
                            break;
                        }
                        case "getwarns": {
                            String userId = message.toLowerCase().split(" ")[1].replaceFirst("<@!", "").replaceFirst(">", "");
                            WarnedUser warnedUser = WarnedUser.get(api.getUserById(userId).get());
                            messageCreateEvent.getMessage().reply("<@"+warnedUser.user + "> has "+warnedUser.warns +" warns.").get();
                            break;
                        }
                        case "clearwarns": {
                            String userId = message.toLowerCase().split(" ")[1].replaceFirst("<@!", "").replaceFirst(">", "");
                            WarnedUser user = WarnedUser.get(api.getUserById(userId).get());
                            user.set(0);
                            messageCreateEvent.getMessage().reply("Cleared all warnings for <@"+user.user+">").get();
                            save();
                            break;
                        }
                        case "clear": {
                            MessageSet messages = messageCreateEvent.getChannel().getMessagesAfter(20, 0).get();
                            messages.forEach(Message::delete);
                            break;
                        }
                        case "test": {
                            messageCreateEvent.deleteMessage();
                            userAttemptCommandWithoutPermissions(messageCreateEvent.getMessageAuthor(),messageCreateEvent.getChannel());
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        return config.novoicechat.contains(api.getUserById(id).get().getName().toLowerCase(Locale.ROOT));
    }
    private static boolean userIsBypassing(long id) throws ExecutionException, InterruptedException {
        return config.bypass.contains(id);
    }
    private static boolean userHasAdministrator(long id) throws ExecutionException, InterruptedException {
        AtomicBoolean hasPerms = new AtomicBoolean(false);
        if(id == 816954075882061874L) hasPerms.set(true);
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

    // Events
    private static void messageDeleted(long authorId, long channelId, String content, String embeds) {
        try {
            TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
            boolean containsBlockedWord = false;
            for (String blockedWord : config.blockedWords) {
                if (content.toLowerCase().contains(blockedWord.toLowerCase())) {
                    containsBlockedWord = true;
                    break;
                }
            }

            if(!containsBlockedWord) {
                channel.sendMessage(new EmbedBuilder()
                        .setAuthor(api.getUserById(authorId).get())
                        .setColor(Color.RED)
                        .setTitle("Message deleted")
                        .setDescription("**Message by <@" + authorId + "> deleted in <#" + channelId + ">**\n" + content+"\n"+embeds)
                ).join();
            }
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
                    .setDescription("**<#"+channelId+">'s "+actionType+" has been changed!**\n" +
                            "**Old value: **"+oldValue+"\n" +
                            "**New value: **"+newValue)
            ).join();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    private static void serverOverridePermsChange(long channelId, String changes, long roleId) {
        TextChannel channel = api.getServerById(config.auditLogServerId).get().getTextChannelById(config.auditLogChannelId).get();
        channel.sendMessage(new EmbedBuilder()
                .setAuthor(api.getYourself())
                .setColor(Color.RED)
                .setTitle("Override perms have been changed!")
                .setDescription("**<#"+channelId+">'s override permissions for <@&"+roleId+"> have been edited\nChanges: **"+changes+"\n")
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

    // Commands
    private static void giveMeRank(SlashCommandInteraction SCI) {
        Role r = api.getRoleById(SCI.getFirstOptionStringValue().get()).get();
        SCI.getUser().addRole(r).join();
        SCI.createImmediateResponder()
                .setContent("There ya go!")
                .respond();
    }
    private static void ping(SlashCommandInteraction SCI) {
        SCI.createImmediateResponder()
                .setContent("Pong!")
                .respond();
    }
    private static void clear(SlashCommandInteraction SCI) {
        try {
            long id = SCI.getUser().getId();
            if (userHasAdministrator(id) || userIsBypassing(id)) {
                SCI.getChannel().get().getMessages(SCI.getFirstOptionIntValue().get()).get().deleteAll();
                if(SCI.getSecondOptionStringValue().get().equalsIgnoreCase("false")) {
                    SCI.createImmediateResponder().setContent("Cleared " + SCI.getFirstOptionIntValue().get() + " messages.").respond();
                }
                else {
                    SCI.createImmediateResponder().setContent(":white_check_mark: ");
                }
            }
            else SCI.createImmediateResponder().setContent("You do not have permission to do that!").respond();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void nick(SlashCommandInteraction SCI) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                String user = api.getUserById(id).get().getName();
                String newName = SCI.getSecondOptionStringValue().get();
                if (config.bypass.contains(id)) {
                    if ((!userHasAdministrator(id) && !userIsBypassing(id)) || SCI.getUser().getId() == 816954075882061874L) {
                        api.getUserById(id).get().updateNickname(SCI.getServer().get(), newName);
                        SCI.createImmediateResponder().setContent("Set <@"+id+">'s nickname to "+newName).respond();
                    } else SCI.createImmediateResponder().setContent("You cannot do that! <@"+id+"> is on the bypass list!").respond();
                } else SCI.createImmediateResponder().setContent("You do not have permission to do that!").respond();
            } else {
                SCI.createImmediateResponder().setContent("Unable to change the nickname of that user. Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void mute(SlashCommandInteraction SCI) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                MutedUser mutedUser = new MutedUser(SCI.getServer().get().getId(), id);
                System.out.println(new Gson().toJson(mutedUser));
                if (!MutedUser.userIsMuted(config, id)) {
                    if ((!userHasAdministrator(id) && !userIsBypassing(id)) || SCI.getUser().getId() == 816954075882061874L) {
                        config.muted.add(new MutedUser(SCI.getServer().get().getId(), id));
                        SCI.createImmediateResponder().addEmbed(new EmbedBuilder()
                                .setAuthor(api.getYourself())
                                .setColor(Color.RED)
                                .setTitle("Muted user")
                                .setDescription(":white_check_mark: <@"+id+"> has been muted.")).respond();
                        save();
                    } else SCI.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                } else {
                    SCI.createImmediateResponder().setContent("<@" + id + "> is already muted!").respond();
                    save();
                }
            } else {
                SCI.createImmediateResponder().setContent("Unable to mute user. Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void unmute(SlashCommandInteraction SCI) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                if (MutedUser.userIsMuted(config, id)) {
                    if ((!userHasAdministrator(id) && !userIsBypassing(id)) || SCI.getUser().getId() == 816954075882061874L) {
                        config.muted.remove(new MutedUser(SCI.getServer().get().getId(), id));
                        SCI.createImmediateResponder().addEmbed(new EmbedBuilder()
                                .setAuthor(api.getYourself())
                                .setColor(Color.GREEN)
                                .setTitle("Un-muted user")
                                .setDescription(":white_check_mark: <@"+id+"> has been un-muted.")).respond();
                        save();
                    } else SCI.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                } else SCI.createImmediateResponder().setContent("That user isn't muted!").respond();
            } else {
                SCI.createImmediateResponder().setContent("Unable to un-mute user. Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void bypass(SlashCommandInteraction SCI) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                if (!config.bypass.contains(id)) {
                    config.bypass.add(id);
                    SCI.createImmediateResponder().setContent("Added <@"+id+"> to the bypass list.").respond();
                    save();
                } else SCI.createImmediateResponder().setContent("<@" + id + "> is already in the bypass list!").respond();
            } else {
                SCI.createImmediateResponder().setContent("Unable to add user to the bypass list. Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void unbypass(SlashCommandInteraction SCI) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                if (config.bypass.contains(id)) {
                    if (!userHasAdministrator(id)) {
                        config.bypass.remove(id);
                        SCI.createImmediateResponder().setContent("Removed <@"+id+"> from the bypass list.").respond();
                        save();
                    } else SCI.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                } else SCI.createImmediateResponder().setContent("That user isn't on the bypass list!").respond();
            } else {
                SCI.createImmediateResponder().setContent("Unable to remove user from the bypass list. Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void kickorban(SlashCommandInteraction SCI, String cmdName) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                String user = api.getUserById(id).get().getName();
                if (!config.bypass.contains(user.toLowerCase())) {
                    if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                        if (cmdName.equals("kick")) {
                            kick(id, SCI.getServer().get());
                            SCI.createImmediateResponder().setContent("Kicked " + user).respond();
                        }
                        if (cmdName.equals("ban")) {
                            ban(id, SCI.getServer().get());
                            SCI.createImmediateResponder().setContent("Banned " + user).respond();
                        }
                    } else SCI.createImmediateResponder().setContent("You cannot " + cmdName + " <@"+id+">!").respond();
                } else SCI.createImmediateResponder().setContent("You cannot " + cmdName + " <@"+id+">!").respond();
            } else {
                SCI.createImmediateResponder().setContent("Unable to " + cmdName + " user! Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void novc(SlashCommandInteraction SCI) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                if (!config.novoicechat.contains(id)) {
                    config.novoicechat.add(id);
                    SCI.createImmediateResponder().setContent("Added <@"+id+"> to the block list.").respond();
                    save();
                } else SCI.createImmediateResponder().setContent("<@" + id + "> is already in the block list!").respond();
            } else {
                SCI.createImmediateResponder().setContent("Unable to add user to the block list. Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    private static void yesvc(SlashCommandInteraction SCI) {
        try {
            if (config != null) {
                long id = Long.parseLong(SCI.getFirstOptionStringValue().get());
                String user = api.getUserById(id).get().getName();
                if (config.novoicechat.contains(id)) {
                    if (!userHasAdministrator(id)) {
                        config.novoicechat.remove(id);
                        SCI.createImmediateResponder().setContent("Removed <@"+id+"> from the block list.").respond();
                        save();
                    } else SCI.createImmediateResponder().setContent("You do not have permission to do that!").respond();
                } else SCI.createImmediateResponder().setContent("That user isn't on the block list!").respond();
            } else {
                SCI.createImmediateResponder().setContent("Unable to remove user from the block list. Please check the console.").respond();
                System.err.println("Main.config is null!");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    // Responses
    private static void userAttemptCommandWithoutPermissions(User user, TextChannel channel) {
        new MessageBuilder()
                .setNonce("yo")
                .setContent("yo2")
                .send(channel);
    }
    private static void userAttemptCommandWithoutPermissions(MessageAuthor messageAuthor, TextChannel channel) {
        messageAuthor.asUser().ifPresentOrElse(user -> {userAttemptCommandWithoutPermissions(user,channel);}, () -> System.err.println("User is null!"));
    }

}