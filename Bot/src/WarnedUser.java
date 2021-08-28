import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WarnedUser {
    public long user;
    public int warns = 0;

    public WarnedUser(User user) {
        this.user = user.getId();
    }

    public static @NotNull WarnedUser get(User u) {
        AtomicReference<WarnedUser> returnValue = new AtomicReference<>(null);
        Main.config.warnedUsers.forEach(warnedUser -> {
            if (Objects.equals(u.getId(), warnedUser.user)) returnValue.set(warnedUser);
        });
        if (returnValue.get() == null) {
            returnValue.set(new WarnedUser(u));
            Main.config.warnedUsers.add(returnValue.get());
        }
        return returnValue.get();
    }

    public static void increment(User u) {
        AtomicBoolean done = new AtomicBoolean(false);
        Main.config.warnedUsers.forEach(warnedUser -> {
            if (Objects.equals(warnedUser.user, u.getId()) && !done.get()) {
                int index = Main.config.warnedUsers.indexOf(warnedUser);
                Main.config.warnedUsers.set(index, warnedUser.add());
                done.set(true);
            }
        });
        if(!done.get()) {
            Main.config.warnedUsers.add(new WarnedUser(u).add());
        }
    }

    public WarnedUser add() {
        this.warns++;
        return this;
    }

    public WarnedUser set(int i) {
        this.warns = i;
        return this;
    }

    public static void set(User u, int i) {
        AtomicBoolean done = new AtomicBoolean(false);
        Main.config.warnedUsers.forEach(warnedUser -> {
            if (Objects.equals(warnedUser.user, u.getId()) && !done.get()) {
                int index = Main.config.warnedUsers.indexOf(warnedUser);
                Main.config.warnedUsers.set(index, warnedUser.set(i));
                done.set(true);
            }
        });
        if(!done.get()) {
            Main.config.warnedUsers.add(new WarnedUser(u).set(i));
        }
    }
}
