package anticope.esixtwoone.sources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import meteordevelopment.meteorclient.utils.network.Http;

public class GelBooru extends Source {

    private final String domain;
    private final int lastPage;

    public GelBooru(String domain, int lastPage) {
        this.domain = domain;
        this.lastPage = lastPage;
    }

    @Override
    public void reset() {}

    @Override
    public String randomImage(String filter, Size size) {
        String query = String.format("%s/index.php?page=dapi&s=post&q=index&tags=%s&pid=%d&json=1&limit=10", domain, filter, random.nextInt(0, lastPage));
        JsonElement result = Http.get(query).sendJson(JsonElement.class);
        if (result == null) return null;
        if (result instanceof JsonArray array) {
            if (array.get(random.nextInt(0, Math.min(11, array.size()))) instanceof JsonObject post) {
                if(post.get(size.toString()+"_url")!=null) {
                    var url = post.get(size.toString() + "_url").getAsString();
                    return url;
                }else{
                    return String.format("%s//images/%s/%s",domain,post.get("directory").getAsString(),post.get("image").getAsString());
                }
            }
        } else if (result instanceof JsonObject object) {
            if (object.get("post") instanceof JsonArray array) {
                if (array.get(random.nextInt(0, Math.min(11, array.size()))) instanceof JsonObject post) {
                    if(post.get(size.toString()+"_url")!=null) {
                        var url = post.get(size.toString()+"_url").getAsString();
                        return url;
                    }else{
                        return String.format("%s//images/%s/%s",domain,post.get("directory").getAsString(),post.get("image").getAsString());
                    }
                }
            }
        }

        return null;
    }
}
