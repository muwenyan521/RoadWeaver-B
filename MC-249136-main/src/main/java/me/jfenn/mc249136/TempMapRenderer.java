package me.jfenn.mc249136;

import net.minecraft.item.map.MapState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

public class TempMapRenderer {

    private static final int MAP_SIZE = 128;
    private static final byte MAP_COLOR = 29*4;

    private static final ImageReference IMAGE_LOADING = new ImageReference("map_loading.png");
    private static final ImageReference IMAGE_NOT_FOUND = new ImageReference("map_not_found.png");

    public static void fillLoadingState(MapState state) {
        BufferedImage image = IMAGE_LOADING.getImage();
        if (image != null) fillImage(state, image);
    }

    public static void fillErrorState(MapState state) {
        BufferedImage image = IMAGE_NOT_FOUND.getImage();
        if (image != null) fillImage(state, image);
    }

    private static void fillImage(MapState state, BufferedImage image) {
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                int rgb = image.getRGB(x, y);
                state.colors[x + y * MAP_SIZE] = rgb != 0 ? MAP_COLOR : 0;
            }
        }
    }

}
