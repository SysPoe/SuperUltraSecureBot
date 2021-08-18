import java.util.ArrayList;
import java.util.List;

public class Config {
    public String[] muted;
    public String[] blockedWords;
    public String[] bypass;
    public String[] novoicechat;
    public ArrayList<CurrencyUser> currencyUsers;
    public long auditLogChannelId;
    public long auditLogServerId;
    public Config(String[] muted, String[] blockedWords, String[] bypass, String[] novoicechat, CurrencyUser[] currencyUsers, long auditLogChannelId, long auditLogServerId) {
        this.muted = muted;
        this.blockedWords = blockedWords;
        this.bypass = bypass;
        this.novoicechat = novoicechat;
        this.currencyUsers = new ArrayList<>(List.of(currencyUsers));
        this.auditLogChannelId = auditLogChannelId;
        this.auditLogServerId = auditLogServerId;
    }
}
