package me.jfenn.mc249136;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageReference {

    private final Logger log = LoggerFactory.getLogger(ImageReference.class);

    private final String imagePath;

    public ImageReference(String imagePath) {
        this.imagePath = imagePath;
        getImage();
    }

    private WeakReference<BufferedImage> imageReference = null;

    public BufferedImage getImage() {
        BufferedImage image;

        // If the WeakReference is stored, return it!
        if (imageReference != null) {
            image = imageReference.get();
            if (image != null)
                return image;
        }

        // Otherwise, re-fetch (and store) the BufferedImage
        image = readImage();
        imageReference = new WeakReference<>(image);
        return image;
    }

    private BufferedImage readImage() {
        Path configPath = getConfigPath();
        File configFile = configPath.toFile();

        if (Files.exists(configPath)) {
            try {
                return ImageIO.read(configFile);
            } catch (Exception e) {
                log.error("Unable to read image path: " + imagePath, e);
            }
        }

        BufferedImage image;
        try (InputStream inputStream = ImageReference.class.getResourceAsStream(getResourcePath())) {
            image = ImageIO.read(inputStream);
        } catch (Exception e) {
            log.error("Unable to read image resource: " + imagePath, e);
            return null;
        }

        try {
            Files.createDirectories(configPath.getParent());
            Files.createFile(configPath);
        } catch (Exception e) {
            log.error("Error creating file: " + imagePath, e);
            return image;
        }

        try  (
            InputStream inputStream = ImageReference.class.getResourceAsStream(getResourcePath());
            FileOutputStream outputStream = new FileOutputStream(configFile)
        ) {
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            log.error("Error writing to file: " + imagePath, e);
        }

        return image;
    }

    private Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("mc249136")
                .resolve(imagePath);
    }

    private String getResourcePath() {
        return "/assets/mc249136/" + imagePath;
    }

}
