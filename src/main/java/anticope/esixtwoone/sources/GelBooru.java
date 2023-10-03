package anticope.esixtwoone.sources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import meteordevelopment.meteorclient.utils.network.Http;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;

public class GelBooru extends Source {

    private final String domain;
    private final int pageSize;

    public GelBooru(String domain, int pageSize) {
        this.domain = domain;
        this.pageSize = pageSize;
    }

    @Override
    public void reset() {}

    @Override
    public String randomImage(String filter, Size size) {
        int lastPID = 1;
        try {
            Document pages_size = Jsoup.connect(String.format("%s/index.php?page=post&s=list&tags=%s", domain, filter)).get();
            Elements elements = pages_size.getElementsByAttributeValue("alt","last page");
            if(elements.size()>0){
                lastPID=Integer.parseInt(elements.get(0).attr("href").split("&")[elements.get(0).attr("href").split("&").length-1].split("=")[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String query = String.format("%s/index.php?page=dapi&s=post&q=index&tags=%s&pid=%d&json=1&limit=1", domain, filter, random.nextInt(0, lastPID));
        JsonElement result = Http.get(query).sendJson(JsonElement.class);
        if (result == null) return null;
        if (result instanceof JsonArray array) {
            if (array.size()>0&&array.get(0) instanceof JsonObject post) {
                if(post.get(size.toString()+"_url")!=null) {
                    var url = post.get(size.toString() + "_url").getAsString();
                    return url;
                }else{
                    return String.format("%s//images/%s/%s",domain,post.get("directory").getAsString(),post.get("image").getAsString());
                }
            }
        } else if (result instanceof JsonObject object) {
            if (object.get("post") instanceof JsonArray array) {
                if (array.size()>0&&array.get(0) instanceof JsonObject post) {
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
