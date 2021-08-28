import java.util.ArrayList;

public class Config {
    public ArrayList<Long> muted;
    public ArrayList<String> blockedWords;
    public ArrayList<Long> bypass;
    public ArrayList<Long> novoicechat;
    public ArrayList<WarnedUser> warnedUsers;
    public long auditLogChannelId;
    public long auditLogServerId;
    public String token;
    public Config(ArrayList<Long> muted, ArrayList<String> blockedWords, ArrayList<Long> bypass, ArrayList<Long> novoicechat, long auditLogChannelId, long auditLogServerId, ArrayList<WarnedUser> warnedUsers, String token) {
        this.muted = muted;
        this.blockedWords = blockedWords;
        this.bypass = bypass;
        this.novoicechat = novoicechat;
        this.auditLogChannelId = auditLogChannelId;
        this.auditLogServerId = auditLogServerId;
        this.warnedUsers = warnedUsers;
        this.token = token;
    }
}
