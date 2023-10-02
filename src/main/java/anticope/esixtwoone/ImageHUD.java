package anticope.esixtwoone;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import anticope.esixtwoone.sources.Source;
import anticope.esixtwoone.sources.Source.Size;
import anticope.esixtwoone.sources.Source.SourceType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.WHITE;

public class ImageHUD extends HudElement {
    public static final HudElementInfo<ImageHUD> INFO = new HudElementInfo<>(Hud.GROUP, "e621-image", "sex", ImageHUD::create);

    private boolean locked = false;
    private boolean empty = true;
    private int ticks = 0;
    private Source source;

    private Identifier TEXID = new Identifier("e621", UUID.randomUUID().toString().toLowerCase()); //uhoh

    private static final Map<String,BufferedImage> loadedImages = new HashMap<>(); //this WILL leak memory if you construct this multiple times but i dont care

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> maxWidth = sgGeneral.add(new DoubleSetting.Builder()
        .name("width")
        .description("The max width of the image.")
        .defaultValue(128)
        .sliderRange(64, 2048)
        .onChanged(o -> updateSize())
        .build()
    );

    private final Setting<Double> maxHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("The max height of the image.")
        .defaultValue(128)
        .sliderRange(64, 2048)
        .onChanged(o -> updateSize())
        .build()
    );

    private final Setting<String> tags = sgGeneral.add(new StringSetting.Builder()
        .name("tags")
        .description("Tags")
        .defaultValue("blue_archive")
        .onChanged((v) -> updateSource())
        .build()
    );

    private final Setting<Size> size = sgGeneral.add(new EnumSetting.Builder<Size>()
        .name("size")
        .description("Size mode.")
        .defaultValue(Size.preview)
        .onChanged((v) -> updateSource())
        .build()
    );

    private final Setting<SourceType> sourceType = sgGeneral.add(new EnumSetting.Builder<SourceType>()
        .name("source")
        .description("Source Type. None disables loading sources for this instance.\nSyntax for kemono filter: patreon/user/3161935\n Syntax for mwm: [ycy|moez|ysz|pc|moe|fj|bd|ys|mp|moemp|ysmp|tx|lai|xhl]")
        .defaultValue(SourceType.safebooru)
        .onChanged(v -> updateSource())
        .build()
    );

    private final Setting<Integer> refreshRate = sgGeneral.add(new IntSetting.Builder()
        .name("refresh-rate")
        .description("How often to change (ticks).")
        .defaultValue(1200)
        .sliderRange(20, 3000)
        .build()
    );
    private final Setting<String> writeImageIdentifier = sgGeneral.add(new StringSetting.Builder()
        .name("write-identifier")
        .description("The id of the downloaded image")
        .defaultValue("image1")
        .onChanged(v -> updateImageIdentifiers())
        .build()
    );
    private final Setting<String> readImageIdentifier = sgGeneral.add(new StringSetting.Builder()
        .name("read-identifier")
        .description("The id of the displayed image")
        .defaultValue("image1")
        .onChanged(v -> updateImageIdentifiers())
        .build()
    );

    public ImageHUD() {
        super(INFO);
        updateSource();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void remove() {
        super.remove();
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    private static ImageHUD create() {
        return new ImageHUD();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ticks ++;
        if (ticks >= refreshRate.get()) {
            ticks = 0;
            loadImage();
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        if (empty) {
            loadImage();
            return;
        }

        GL.bindTexture(TEXID);
        Renderer2D.TEXTURE.begin();

        Renderer2D.TEXTURE.texQuad(
            switch(box.xAnchor){
                case Left -> x;
                case Center -> x+(maxWidth.get()/2)-(native_width/2);
                case Right -> x+maxWidth.get()-(native_width);
            },
            switch(box.yAnchor){
                case Top -> y;
                case Center -> y+(maxHeight.get()/2)-(native_height/2);
                case Bottom -> y+maxHeight.get()-(native_height);
            },
            (int)native_width,
            (int)native_height,
            WHITE);
        Renderer2D.TEXTURE.render(null);
    }

    private void updateSize() {
        setSize(maxWidth.get(),maxHeight.get());
    }

    private void updateSource() {
        source = Source.getSource(sourceType.get());
        if(source!=null) {
            source.reset();
        }
        empty = true;
    }

    private void updateImageIdentifiers(){
        uploadAndResizeImage(loadedImages.get(readImageIdentifier.get()));
    }
    private double native_width=64;
    private double native_height=64;

    private void loadImage() {
        if(sourceType.get()==SourceType.none){
            uploadAndResizeImage(loadedImages.get(readImageIdentifier.get()));
            empty = false;
            return;
        }
        if (locked || source == null)
            return;
        new Thread(() -> {
            try {
                locked = true;
                String url = source.getRandomImage(tags.get(), size.get());
                if (url == null) {
                    locked = false;
                    return;
                }
                E621Hud.LOG.info(url);
                BufferedImage bufferedImage = ImageIO.read(Http.get(url).sendInputStream());
                loadedImages.put(writeImageIdentifier.get(), bufferedImage);
                uploadAndResizeImage(bufferedImage);
                empty = false;
            } catch (Exception ex) {
                E621Hud.LOG.error("Failed to render the image.", ex);
            }
            try {
                Thread.sleep(2000); //dont spam
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            locked = false;
        }).start();
        updateSize();
    }

    private void uploadAndResizeImage(BufferedImage bufferedImage){
        if(bufferedImage==null) return;
        try {
            BufferedImage resizedImage = new BufferedImage(
                (int) Math.min(maxWidth.get(),bufferedImage.getWidth()*(maxHeight.get()/bufferedImage.getHeight())),
                (int) Math.min(maxHeight.get(),bufferedImage.getHeight()*(maxWidth.get()/bufferedImage.getWidth())),
                ((bufferedImage.getType() == 0) ? BufferedImage.TYPE_INT_ARGB : bufferedImage.getType())
            );
            native_width=resizedImage.getWidth();
            native_height=resizedImage.getHeight();
            Graphics2D g2d = resizedImage.createGraphics(); //TODO figure out how to downsize the image without making it look bad
            g2d.setComposite(AlphaComposite.Src);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(bufferedImage, 0, 0, resizedImage.getWidth(), resizedImage.getHeight(), null);
            g2d.dispose();



            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resizedImage,"png", os); //i think this works?
            mc.getTextureManager().registerTexture(TEXID, new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(os.toByteArray()))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
