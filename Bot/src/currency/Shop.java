package currency;

import org.javacord.api.entity.message.embed.EmbedBuilder;

public class Shop {
    public static EmbedBuilder getShop() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("**Shop Items**");
        StringBuilder items = new StringBuilder();
        for (ShopItem value : ShopItem.values()) {
            items.append("**").append(ShopItem.getProperName(value)).append("** - ").append(E.c).append(" ").append(ShopItem.getPrice(value, true)).append("\n");
        }
        embedBuilder.setDescription(items.toString());
        return embedBuilder;
    }
}
