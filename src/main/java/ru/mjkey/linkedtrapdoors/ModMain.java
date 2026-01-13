package ru.mjkey.linkedtrapdoors;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class ModMain extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ModMain INSTANCE;

    public ModMain(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("LinkedTrapdoors v1.0.0 загружается...");
        this.getEntityStoreRegistry().registerSystem(new TrapdoorEventSystem());
        LOGGER.atInfo().log("LinkedTrapdoors загружен!");
    }

    public static ModMain getInstance() {
        return INSTANCE;
    }
}
