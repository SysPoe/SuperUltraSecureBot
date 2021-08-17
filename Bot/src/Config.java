public class Config {
    public String[] muted;
    public String[] blockedWords;
    public String[] bypass;
    public String[] novoicechat;
    public long auditLogChannelId;
    public long auditLogServerId;
    public Config(String[] muted, String[] blockedWords, String[] bypass, String[] novoicechat, long auditLogChannelId, long auditLogServerId) {
        this.muted = muted;
        this.blockedWords = blockedWords;
        this.bypass = bypass;
        this.novoicechat = novoicechat;
        this.auditLogChannelId = auditLogChannelId;
        this.auditLogServerId = auditLogServerId;
    }
}
