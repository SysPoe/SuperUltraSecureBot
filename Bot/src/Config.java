public class Config {
    public String[] muted;
    public String[] blockedWords;
    public String[] bypass;
    public String[] novoicechat;
    public Config(String[] muted, String[] blockedWords, String[] bypass, String[] novoicechat) {
        this.muted = muted;
        this.blockedWords = blockedWords;
        this.bypass = bypass;
        this.novoicechat = novoicechat;
    }
}
