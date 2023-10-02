package anticope.esixtwoone.sources;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Kemono extends Source{

    private static final Random random = new Random();
    @Override
    public void reset() {}

    private static final Pattern pageLength = Pattern.compile("(\\d+)(?!.*\\d)");

    @Override
    protected String randomImage(String filter, Size size) {
        try {
            Document pages_size = Jsoup.connect(String.format("https://kemono.su/%s",filter)).get();
            String text = pages_size.getElementsByTag("small").get(0).text();
            Matcher matcher = pageLength.matcher(text);
            int offset = 0;
            if(matcher.matches()) {
                offset=random.nextInt(0,((Integer.parseInt(matcher.group(1))-1)/50)+1);
            }
            Document page = Jsoup.connect(String.format("https://kemono.su/%s?o=%s",filter,offset)).get();
            Elements post_links = page.select(".post-card.post-card--preview");
            List<Element> withAttachmentPosts = new ArrayList<>();
            for(Element post_container : post_links){
                if(post_container.select(".post-card__image").size()>0){
                    withAttachmentPosts.add(post_container.selectFirst("a"));
                }
            }
            Element post_element = withAttachmentPosts.get(random.nextInt(0,withAttachmentPosts.size()));

            if(size==Size.preview){
                return post_element.selectFirst(".post-card__image").absUrl("src");
            }
            Document post = Jsoup.connect(post_element.absUrl("href")).get();

            Elements image_links = post.select(".fileThumb");
            Element file_element = image_links.get(random.nextInt(0,image_links.size()));
            if(size==Size.sample){
                return file_element.selectFirst("img").absUrl("src");
            }
            return file_element.absUrl("href");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
