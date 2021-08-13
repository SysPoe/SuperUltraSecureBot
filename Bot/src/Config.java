public class Config {
    public String[] muted;
    public String[] blockedWords;
    public String[] warned;
    public String[] bypass;
    public String[] novoicechat;
    public Config(String[] muted, String[] blockedWords, String[] warned, String[] bypass, String[] novoicechat) {
        this.muted = muted;
        this.blockedWords = blockedWords;
        this.warned = warned;
        this.bypass = bypass;
        this.novoicechat = novoicechat;
    }
}
