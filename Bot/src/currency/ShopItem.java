package currency;

import java.util.Objects;

public enum ShopItem {
    ANTI_ROB,
    PHALLIC_OBJECT,
    CELL_PHONE,
    PADLOCK,
    LAPTOP,
    BOX_OF_SAND,
    APPLE,
    LANDMINE,
    FAKE_ID,
    ALCOHOL,
    LUCKY_HORSESHOE,
    FIDGET_SPINNER,
    SHOVEL,
    FISHING_POLE,
    HUNTING_RIFLE,
    COIN_BOMB,
    LIFE_SAVER,
    TIDEPOD,
    ROBBERS_WHITELIST,
    SHREDDED_CHEESE,
    RARE_PEPE,
    PIZZA_SLICE,
    PEPE_COIN,
    PEPE_MEDAL,
    PEPE_TROPHY,
    PEPE_CROWN;

    public static long getPrice(ShopItem item) {
        long value;
        switch (item) {
            case ANTI_ROB: value = 1000L;
            case PHALLIC_OBJECT: value = 10L;
            case CELL_PHONE: value = 3500L;
            case PADLOCK: value = 4000L;
            case LAPTOP: value = 5000L;
            case BOX_OF_SAND: value = 2500L;
            case APPLE: value = 8000L;
            case LANDMINE: value = 9500L;
            case FAKE_ID: value = 10000L;
            case ALCOHOL: value = 12000L;
            case LUCKY_HORSESHOE:
            case SHOVEL:
            case FIDGET_SPINNER:
            case FISHING_POLE:
            case HUNTING_RIFLE:
                value = 25000L;
            case COIN_BOMB:
            case LIFE_SAVER:
            case TIDEPOD:
                value = 30000L;
            case ROBBERS_WHITELIST: value = 50000L;
            case SHREDDED_CHEESE: value = 70000L;
            case RARE_PEPE: value = 75000L;
            case PIZZA_SLICE: value = 250000L;
            case PEPE_COIN: value = 625000L;
            case PEPE_MEDAL: value = 10000000L;
            case PEPE_TROPHY: value = 50000000L;
            case PEPE_CROWN: value = 250000000L;
        }
        value = 0L;
        return value;
    }
    public static String getPrice(ShopItem item, boolean withCommas) {
        long value = 0L;
        switch (item) {
            case ANTI_ROB: value = 1000L; break;
            case PHALLIC_OBJECT: value = 10L; break;
            case CELL_PHONE: value = 3500L; break;
            case PADLOCK: value = 4000L; break;
            case LAPTOP: value = 5000L; break;
            case BOX_OF_SAND: value = 2500L; break;
            case APPLE: value = 8000L; break;
            case LANDMINE: value = 9500L; break;
            case FAKE_ID: value = 10000L; break;
            case ALCOHOL: value = 12000L; break;
            case LUCKY_HORSESHOE:
            case SHOVEL:
            case FIDGET_SPINNER:
            case FISHING_POLE:
            case HUNTING_RIFLE:
                value = 25000L; break;
            case COIN_BOMB:
            case LIFE_SAVER:
            case TIDEPOD:
                value = 30000L; break;
            case ROBBERS_WHITELIST: value = 50000L; break;
            case SHREDDED_CHEESE: value = 70000L; break;
            case RARE_PEPE: value = 75000L; break;
            case PIZZA_SLICE: value = 250000L; break;
            case PEPE_COIN: value = 625000L; break;
            case PEPE_MEDAL: value = 10000000L; break;
            case PEPE_TROPHY: value = 50000000L; break;
            case PEPE_CROWN: value = 250000000L; break;
        }
        if(!withCommas) return String.valueOf(value);
        String[] valueString = String.valueOf(value).split("");
        String result = "";
        for (int i = valueString.length - 1; i > -1; i--) {
            if((valueString.length - i) % 3 == 1 && i != valueString.length - 1) result = valueString[i] + "," + result;
            else result = valueString[i] + result;
        }
        return result;
    }
    public static String getProperName(ShopItem item) {
        String[] name = item.name().replace("_"," ").split("");
        String result = "";
        boolean isCapital = true;
        for (String s : name) {
            if(isCapital) result+=s.toUpperCase();
            else result+= s.toLowerCase();

            if(Objects.equals(s, " ")) isCapital = true;
            if(!Objects.equals(s, " ")) isCapital = false;
        }
        return result;
    }
}
