package anticope.esixtwoone.sources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.utils.network.Http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Danbooru extends Source{

    private final String domain;

    public Danbooru(String domain){
        this.domain=domain;
    }

    @Override
    public void reset() {}

    private final List<String> acceptedExtensions = List.of("jpg","png","gif","webm"); //i think it should "just work" with gifs?
    @Override //https://testbooru.donmai.us/posts.json?random=true&limit=1&tags=blue_archive+-rating%3Aexplicit
    protected String randomImage(String filter, Size size) {
        String query = String.format("%s/posts.json?limit=5&tags=%s+random%%3A1", domain, filter);
        System.out.println("we are querying "+query);
        JsonElement result = Http.get(query).sendJson(JsonElement.class);
        System.out.println("result was "+result);
        if (result == null) return null;
        if (result instanceof JsonArray array) {
            List<JsonObject> posts = new ArrayList<>();
            for(int i=0;i<array.size();i++) {
                if (array.get(i) instanceof JsonObject post) {
                    posts.add(post);
                }
            }
            Collections.shuffle(posts);

            for(JsonObject post : posts) {
                if (!acceptedExtensions.contains(post.get("file_ext").getAsString())) {
                    continue;
                }
                JsonElement url = post.get(switch (size) {
                    case preview -> "preview_file_url";
                    case sample -> "large_file_url";
                    case file -> "file_url";
                });
                if (url != null) {
                    return url.getAsString();
                }
            }

        }
        return null;
    }
}
