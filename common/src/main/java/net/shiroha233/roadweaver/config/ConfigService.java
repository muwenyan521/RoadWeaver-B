package net.shiroha233.roadweaver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BASE_DIR = "roadweaver";
    private static final String FILE_NAME = "roadweaver.json";

    private static ModConfig INSTANCE = new ModConfig();

    private ConfigService() {}

    public static synchronized void load() {
        Path cfgRoot = Platform.getConfigFolder();
        Path baseDir = cfgRoot.resolve(BASE_DIR);
        Path file = baseDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create config directory: {}", baseDir, e);
        }
        if (Files.exists(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to read config, using defaults. File: {}", file, e);
            }
        } else {
            save();
        }
        // ensure defaults and new fields are populated
        try {
            INSTANCE.sanitize();
        } catch (Throwable t) {
            LOGGER.warn("Failed to sanitize config; continuing with raw values.", t);
        }
        LOGGER.info("Configuration loaded (radiusChunks={}, enabled={})",
                INSTANCE.predictRadiusChunks(), INSTANCE.villagePredictionEnabled());
    }

    public static synchronized void save() {
        Path cfgRoot = Platform.getConfigFolder();
        Path baseDir = cfgRoot.resolve(BASE_DIR);
        Path file = baseDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            LOGGER.warn("Failed to create config directory: {}", baseDir, e);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            LOGGER.warn("Failed to write config file: {}", file, e);
        }
    }

    public static synchronized ModConfig get() {
        return INSTANCE;
    }
}
