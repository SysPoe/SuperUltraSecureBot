import java.util.ArrayList;

public class Config {
    public ArrayList<MutedUser> muted;
    public ArrayList<String> blockedWords;
    public ArrayList<Long> bypass;
    public ArrayList<Long> novoicechat;
    public ArrayList<WarnedUser> warnedUsers;
    public long auditLogChannelId;
    public long auditLogServerId;
    public long mutedLogChannelId;
    public long mutedLogServerId;
    public String token;
}
