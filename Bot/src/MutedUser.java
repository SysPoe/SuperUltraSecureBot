import java.util.concurrent.atomic.AtomicBoolean;

public class MutedUser {
    public long serverId;
    public long userId;

    public MutedUser(long serverId, long userId) {
        this.serverId = serverId;
        this.userId = userId;
    }

    public static boolean userIsMuted(Config config, long userId) {
        AtomicBoolean isMuted = new AtomicBoolean(false);
        config.muted.forEach(mutedUser -> {
            if(mutedUser.userId == userId) isMuted.set(true);
        });
        return isMuted.get();
    }
}
