import java.util.ArrayList;
import java.util.List;

public class Config {
    public ArrayList<Long> muted;
    public ArrayList<String> blockedWords;
    public ArrayList<Long> bypass;
    public ArrayList<Long> novoicechat;
    public ArrayList<CurrencyUser> currencyUsers;
    public ArrayList<WarnedUser> warnedUsers;
    public long auditLogChannelId;
    public long auditLogServerId;
    public long centralChatServerId;
    public long centralChatChannelId;
    public Config(ArrayList<Long> muted, ArrayList<String> blockedWords, ArrayList<Long> bypass, ArrayList<Long> novoicechat, CurrencyUser[] currencyUsers, long auditLogChannelId, long auditLogServerId, long centralChatChannelId, long centralChatServerId, ArrayList<WarnedUser> warnedUsers) {
        this.muted = muted;
        this.blockedWords = blockedWords;
        this.bypass = bypass;
        this.novoicechat = novoicechat;
        this.currencyUsers = new ArrayList<>(List.of(currencyUsers));
        this.auditLogChannelId = auditLogChannelId;
        this.auditLogServerId = auditLogServerId;
        this.centralChatServerId = centralChatServerId;
        this.centralChatChannelId = centralChatChannelId;
        this.warnedUsers = warnedUsers;
    }
}
