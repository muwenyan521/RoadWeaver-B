package net.shiroha233.roadweaver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 预设加载服务：读取 config/roadweaver_presets.json
 */
public final class PresetService {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String BASE_DIR = "roadweaver";
    private static final String PRESET_DIR = "presets";
    private static final String FILE_NAME = "road_surface.json"; 

    private static final AtomicReference<PresetConfig> PRESET = new AtomicReference<>();

    private PresetService() {}

    public static synchronized PresetConfig get() {
        PresetConfig cur = PRESET.get();
        if (cur != null) return cur;
        load();
        return PRESET.get();
    }

    public static synchronized void load() {
        Path cfgRoot = Platform.getConfigFolder();
        Path baseDir = cfgRoot.resolve(BASE_DIR);
        Path presetDir = baseDir.resolve(PRESET_DIR);
        Path file = presetDir.resolve(FILE_NAME);
        PresetConfig preset = null;
        try {
            try {
                java.nio.file.Files.createDirectories(presetDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to create preset directory: {}", presetDir, e);
            }
            if (Files.exists(file)) {
                try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    preset = GSON.fromJson(br, PresetConfig.class);
                }
            } else {
                preset = PresetConfig.defaults();
                save(preset);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read presets, using defaults: {}", file, e);
            preset = PresetConfig.defaults();
        }
        if (preset == null) preset = PresetConfig.defaults();
        try { preset.sanitize(); } catch (Throwable ignored) {}
        PRESET.set(preset);
        LOGGER.info("Presets loaded from {}/{}: materials={} entries",
                BASE_DIR, PRESET_DIR, preset.materials.size());
    }

    public static synchronized void save() {
        PresetConfig p = PRESET.get();
        if (p == null) p = PresetConfig.defaults();
        save(p);
    }

    private static void save(PresetConfig p) {
        Path cfgRoot = Platform.getConfigFolder();
        Path baseDir = cfgRoot.resolve(BASE_DIR);
        Path presetDir = baseDir.resolve(PRESET_DIR);
        Path file = presetDir.resolve(FILE_NAME);
        try {
            java.nio.file.Files.createDirectories(presetDir);
        } catch (Exception e) {
            LOGGER.warn("Failed to create preset directory: {}", presetDir, e);
        }
        try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(p, bw);
        } catch (Exception e) {
            LOGGER.warn("Failed to write presets: {}", file, e);
        }
    }

    public static List<List<String>> getMaterialCombos() {
        PresetConfig p = get();
        return p.materials == null ? List.of() : p.materials;
    }
}
