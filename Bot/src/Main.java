import com.google.gson.Gson;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.Activity;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
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
    static String commandPrefix = "&";
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        api = new DiscordApiBuilder()
                .setToken(Token.token)
                .login().join();

        System.out.println("Loading config...");
        loadConfig();
        System.out.println("Loading listeners...");
        loadListeners();
        loadAuditLogListeners();
        initCurrency();
        loadOtherCommandListeners();
        System.out.println("Loaded listeners.");
        // System.out.println("Adding shutdown listener...");
        // addShutdownListener();
        // System.out.println("Added shutdown listener.");
        // System.out.println("Registering slash commands...");
        // registerSlashCommands();
        // registerCurrencySlashCommands();
        // System.out.println("Registered slash commands.");
    }

    protected static void save() {
        try {
            FileWriter fileWriter = new FileWriter("G:\\projects\\SuperUltraSecureBot\\out\\artifacts\\SuperUltraSecureBot_jar\\config.json", false);
            fileWriter.write(new Gson().toJson(config, Config.class));
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void initCurrency() {
        api.addInteractionCreateListener(interactionCreateEvent -> {
            SlashCommandInteraction interaction = interactionCreateEvent.getSlashCommandInteraction().get();
            String commandName = interaction.getCommandName().toLowerCase();

            long userID = interaction.getUser().getId();

            switch (commandName) {
                case "withdraw": {
                    CurrencyUser user = CurrencyUser.getCurrencyUser(userID);
                    int index = config.currencyUsers.indexOf(user);
                    int toTransfer = interaction.getFirstOptionIntValue().get();
                    if(user == null) {
                        createNewCurrencyUser(userID, 0, 0);
                        interaction.createImmediateResponder().setContent("Withdrew 0 coins from your bank.").respond();
                    }
                    else {
                        long transferred = user.transferFromBank(toTransfer);
                        config.currencyUsers.set(index, user);
                        if(transferred != 1) interaction.createImmediateResponder().setContent("Withdrew "+transferred+" coins from your bank.\nNew bank balance: "+user.getBank()+"\nNew wallet balance: "+user.getWallet()).respond();
                        else interaction.createImmediateResponder().setContent("Withdrew "+transferred+" coin from your bank.\nNew bank balance: "+user.getBank()+"\nNew wallet balance: "+user.getWallet()).respond();
                    }
                }
                case "deposit": {
                    CurrencyUser user = CurrencyUser.getCurrencyUser(userID);
                    int toTransfer = interaction.getFirstOptionIntValue().get();
                    if(user == null) {
                        createNewCurrencyUser(userID, 0, 0);
                        interaction.createImmediateResponder().addEmbed(new EmbedBuilder()
                                .setAuthor(api.getYourself())
                                .setTitle("Deposited 0 coins into your bank")
                                .setDescription("**Wallet: **"+E.c+" 0\n**Bank: **"+E.c+" 0")).respond();
                    }
                    else {
                        int index = config.currencyUsers.indexOf(user);
                        long transferred = user.transferToBank(toTransfer);
                        config.currencyUsers.set(index, user);
                        if(transferred != 1) {
                            interaction.createImmediateResponder().addEmbed(new EmbedBuilder()
                                    .setAuthor(api.getYourself())
                                    .setTitle("Deposited "+transferred+" coins into your bank.")
                                    .setDescription("**Wallet: **"+user.getWalletAsString()+"\n**Bank: **"+user.getBankAsString())).respond();
                        }
                        else {
                            interaction.createImmediateResponder().addEmbed(new EmbedBuilder()
                                    .setAuthor(api.getYourself())
                                    .setTitle("Deposited "+transferred+" coin into your bank.")
                                    .setDescription("**Wallet: **"+user.getWalletAsString()+"\n**Bank: **"+user.getBankAsString())).respond();
                        }
                    }
                }
            }
        });
    }
    private static void createNewCurrencyUser(long userId, long wallet, long bank) {
        config.currencyUsers.add(new CurrencyUser(userId, wallet, bank));
        save();
    }

    private static void loadAuditLogListeners() {
        // Messages
        api.addMessageDeleteListener(messageDeleteEvent -> {
           if(messageDeleteEvent.getChannel().asTextChannel().get()
                   != api.getServerById(config.auditLogServerId).get()
                   .getTextChannelById(config.auditLogChannelId).get()) {
               messageDeleted(messageDeleteEvent.getMessageAuthor().get().getId(),
                       messageDeleteEvent.getChannel().getId(),
                       messageDeleteEvent.getMessage().get().getContent());
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
        SlashCommand givemerank = SlashCommand.with("givemerank", "Gives you the rank",
                        List.of(
                                SlashCommandOption.create(SlashCommandOptionType.ROLE, "Role", "The role to give you.", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: givemerank");
    }
    private static void registerCurrencySlashCommands() {
        Server server = api.getServerById(serverId).get();
        SlashCommand withdraw = SlashCommand.with("withdraw", "Withdraws the specified amount of currency from your bank.",
                        List.of(
                                SlashCommandOption.create(SlashCommandOptionType.INTEGER, "Amount", "The amount to withdraw", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: withdraw");
        SlashCommand deposit = SlashCommand.with("deposit", "Deposits the specified amount of currency to your bank.",
                        List.of(
                                SlashCommandOption.create(SlashCommandOptionType.INTEGER, "Amount", "The amount to deposit", true)))
                .createForServer(server)
                .join();
        System.out.println("Registered command: deposit");
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
                case "givemerank": {
                    Role r = api.getRoleById(slashCommandInteraction.getFirstOptionStringValue().get()).get();
                    slashCommandInteraction.getUser().addRole(r).join();
                    slashCommandInteraction.createImmediateResponder()
                            .setContent("There ya go!")
                            .respond();
                    break;
                }
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
                                slashCommandInteraction.getChannel().get().getMessages(slashCommandInteraction.getFirstOptionIntValue().get()).get().stream().toList().forEach(Message::delete);
                                slashCommandInteraction.createImmediateResponder().setContent("Clearing " + slashCommandInteraction.getFirstOptionIntValue().get() + " messages. This may take a while!").respond();
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
                            if (config.bypass.contains(user.toLowerCase())) {
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
                            if (!config.muted.contains(id)) {
                                if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                                    config.muted.add(id);
                                    slashCommandInteraction.createImmediateResponder().addEmbed(new EmbedBuilder()
                                            .setAuthor(api.getYourself())
                                            .setColor(Color.RED)
                                            .setTitle("Muted user")
                                            .setDescription(":white_check_mark: <@"+id+"> has been muted.")).respond();
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
                            if (config.muted.contains(id)) {
                                if (!userHasAdministrator(id) && !userIsBypassing(id)) {
                                    config.muted.remove(id);
                                    slashCommandInteraction.createImmediateResponder().addEmbed(new EmbedBuilder()
                                            .setAuthor(api.getYourself())
                                            .setColor(Color.GREEN)
                                            .setTitle("Un-muted user")
                                            .setDescription(":white_check_mark: <@"+id+"> has been un-muted.")).respond();
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
                            if (config.bypass.contains(id)) {
                                config.bypass.add(id);
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
                            if (config.bypass.contains(id)) {
                                if (!userHasAdministrator(id)) {
                                    config.bypass.remove(id);
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
                            if (!config.bypass.contains(user.toLowerCase())) {
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
                            if (!config.novoicechat.contains(id)) {
                                config.novoicechat.add(id);
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
                            if (config.novoicechat.contains(id)) {
                                if (!userHasAdministrator(id)) {
                                    config.novoicechat.remove(id);
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
            if(!event.getMessageAuthor().isBotUser()) {
                boolean doCentralMessages = false;
                // Blocked users
                if (!config.bypass.contains(event.getMessageAuthor().getId())) {
                    boolean canSend = true;
                    for (String blockedWord : config.blockedWords)
                        if (event.getMessage().getContent().toLowerCase().contains(blockedWord.toLowerCase()))
                            canSend = false;
                    if (!canSend) {
                        event.getMessage().delete("Contains a blocked word");
                        doCentralMessages = true;
                    }
                }
                // Muted users
                if (config.muted.contains(event.getMessageAuthor().getId())) {
                    event.getMessage().delete("User muted.");
                    doCentralMessages = true;
                }
                // Central Chat
                if (!doCentralMessages && !event.getMessageContent().startsWith(commandPrefix)) {
                    if(api.getServerChannelById(event.getChannel().getId()).get().getOverwrittenPermissions().get(821266086459736075L).getAllowedPermission().contains(PermissionType.READ_MESSAGES)) {
                        Server server = api.getServerById(config.centralChatServerId).get();
                        TextChannel channel = server.getTextChannelById(config.centralChatChannelId).get();
                        channel.sendMessage("@" + event.getMessageAuthor().getDisplayName() + " in <#" + event.getChannel().getId() + "> says: " + event.getMessageContent()).join();
                    }
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

                        }
                        case "clear": {
                            MessageSet messages = messageCreateEvent.getChannel().getMessagesAfter(19, 0).get();
                            if(messages.size() >= 20) {
                                messageCreateEvent.getMessage().reply(new EmbedBuilder()
                                        .setAuthor(api.getYourself())
                                        .setDescription(":negative_squared_cross_mark: Too many messages! Use /clear instead!")).get();

                            }
                            else {
                                messageCreateEvent.getChannel().getMessagesAsStream().toList().forEach(Message::delete);
                                messageCreateEvent.getMessage().reply(new EmbedBuilder()
                                        .setAuthor(api.getYourself())
                                        .setDescription(":white_check_mark: Cleared channel!")).get();
                            }
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
        return config.bypass.contains(api.getUserById(id).get().getName().toLowerCase(Locale.ROOT));
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
}
