package anticope.esixtwoone.sources;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import anticope.esixtwoone.E621Hud;

public abstract class Source {
    public enum Size {
        preview,
        sample,
        file
    }

    public enum SourceType {
        e621,
        gelbooru,
        rule34,
        nekoslife,
        danbooru,
        mwm,
        kemono,
        safebooru,

        none
    }

    protected final Random random = new Random();

    public abstract void reset();

    protected abstract String randomImage(String filter, Size size);

    public String getRandomImage(String filter, Size size) {
        try {
            return randomImage(URLEncoder.encode(filter, StandardCharsets.UTF_8), size);
        } catch (Exception ex) {
            E621Hud.LOG.error("Failed to fetch an image.", ex);
        }
        return null;
    }

    public static Source getSource(SourceType type) {
        return switch (type) {
            case e621 -> new ESixTwoOne();
            case gelbooru -> new GelBooru("https://gelbooru.com/", 42);
            case rule34 -> new GelBooru("https://api.rule34.xxx/", 42);
            case nekoslife -> new NekosLife("https://nekos.life");
            case danbooru -> new Danbooru("https://danbooru.donmai.us");
            case mwm -> new Mwm("https://t.mwm.moe", List.of("ycy","moez","ai","ysz","pc","moe","fj","bd","ys","mp","moemp","ysmp","tx","lai","xhl"));
            case kemono -> new Kemono();
            case safebooru -> new GelBooru("https://safebooru.org/",40);
            default -> null;
        };
    }
}
