package currency;
// Emojis that are used.

import java.util.ArrayList;

public class CurrencyUser {
    private long wallet;
    private long bank;
    private final long userId;

    public CurrencyUser(long userId, long wallet, long bank) {
        this.wallet = wallet;
        this.bank = bank;
        this.userId = userId;
    }

    public long getBank() {return bank;}
    public long getWallet() {return wallet;}
    public long getUserId() {return userId;}

    public String getBankAsString() {return E.c + " " + bank;}
    public String getWalletAsString() {return E.c + " " + wallet;}


    public void setBank(long newValue) {this.bank = newValue;}
    public void setWallet(long newValue) {this.wallet = newValue;}
    public void incrementBank(long value) {this.bank+=value;}
    public void incrementWallet(long value) {this.wallet+=value;}

    public long transferToBank(long value) {
        long toTransfer = Math.min(this.wallet, value);
        this.incrementWallet(toTransfer*-1);
        this.incrementBank(toTransfer);
        return toTransfer;
    }
    public long transferFromBank(long value) {
        long toTransfer = Math.min(this.bank, value);
        this.incrementBank(toTransfer*-1);
        this.incrementWallet(toTransfer);
        return toTransfer;
    }

    public static CurrencyUser getCurrencyUser(long userId, ArrayList<CurrencyUser> currencyUsers) {
        CurrencyUser user = null;
        for (CurrencyUser currencyUser : currencyUsers) {
            if(currencyUser.getUserId() == userId) user = currencyUser;
        }
        return user;
    }
}
