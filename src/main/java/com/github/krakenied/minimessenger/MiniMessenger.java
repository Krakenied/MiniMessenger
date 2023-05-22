package com.github.krakenied.minimessenger;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@SuppressWarnings("unused")
public final class MiniMessenger {

    private final JavaPlugin plugin;
    private final String filename;
    private final String prefixPath;
    private final String messagesSectionPath;

    private final File file;
    private final YamlConfiguration config;

    private Component prefix;
    private ConfigurationSection messagesSection;

    public MiniMessenger(final @NotNull JavaPlugin plugin, final @NotNull String filename, final @NotNull String prefixPath, final @NotNull String messagesSectionPath) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(filename);
        Preconditions.checkNotNull(prefixPath);
        Preconditions.checkNotNull(messagesSectionPath);

        this.plugin = plugin;
        this.filename = filename;
        this.prefixPath = prefixPath;
        this.messagesSectionPath = messagesSectionPath;

        this.file = new File(plugin.getDataFolder(), filename);
        this.config = new YamlConfiguration();

        this.reload();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean reload() {
        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            try (final InputStream inputStream = this.plugin.getResource(this.filename)) {
                Preconditions.checkArgument(inputStream != null, "resource cannot be null");
                Files.copy(inputStream, this.file.toPath());
            } catch (final IllegalArgumentException | IOException | SecurityException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Could not copy default '" + this.filename + "' config file", e);
                return false;
            }
        }

        try {
            this.config.load(this.file);
        } catch (final InvalidConfigurationException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not load '" + this.filename + "' config file", e);
            final File renamedFile = new File(this.plugin.getDataFolder(), this.filename + System.nanoTime());
            this.file.renameTo(renamedFile);
            return this.reload();
        } catch (final IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not load '" + this.filename + "' config file", e);
            return false;
        }

        try {
            final String prefixString = this.config.getString(this.prefixPath);
            Preconditions.checkArgument(prefixString != null, "Could not load '" + this.prefixPath + "' prefix");
            this.prefix = MiniMessage.miniMessage().deserialize(prefixString);

            final ConfigurationSection messagesSection = this.config.getConfigurationSection(this.messagesSectionPath);
            Preconditions.checkArgument(messagesSection != null, "Could not load '" + this.messagesSectionPath + "' messages section");
            this.messagesSection = messagesSection;
        } catch (final IllegalArgumentException e) {
            this.plugin.getLogger().log(Level.SEVERE, e.getMessage(), e.getStackTrace());
            return false;
        }

        return true;
    }

    public @NotNull String getPath(final @NotNull String key) {
        return String.join(".", this.config.getCurrentPath(), key);
    }

    public boolean getBoolean(final @NotNull String key) throws IllegalStateException {
        final Object value = this.config.get(key);
        if (!(value instanceof final Boolean valueBoolean)) throw new IllegalStateException(key + " is null or not instanceof Boolean");
        return valueBoolean;
    }

    public int getInt(final @NotNull String key) throws IllegalStateException {
        final Object value = this.config.get(key);
        if (!(value instanceof final Number valueNumber)) throw new IllegalStateException(key + " is null or not instanceof Number");
        return valueNumber.intValue();
    }

    public long getLong(final @NotNull String key) throws IllegalStateException {
        final Object value = this.config.get(key);
        if (!(value instanceof final Number valueNumber)) throw new IllegalStateException(key + " is null or not instanceof Number");
        return valueNumber.longValue();
    }

    public float getFloat(final @NotNull String key) throws IllegalStateException {
        final Object value = this.config.get(key);
        if (!(value instanceof final Number valueNumber)) throw new IllegalStateException(key + " is null or not instanceof Number");
        return valueNumber.floatValue();
    }

    public double getDouble(final @NotNull String key) throws IllegalStateException {
        final Object value = this.config.get(key);
        if (!(value instanceof final Number valueNumber)) throw new IllegalStateException(key + " is null or not instanceof Number");
        return valueNumber.doubleValue();
    }

    public @NotNull String getString(final @NotNull String key) throws IllegalStateException {
        final Object value = this.config.get(key);
        if (!(value instanceof final String valueString)) throw new IllegalStateException(key + " is null or not instanceof String");
        return valueString;
    }

    public @NotNull List<String> getStringList(final @NotNull String key) throws IllegalStateException {
        final Object value = this.config.get(key);
        if (!(value instanceof List)) throw new IllegalStateException(key + " is null or not instanceof List");
        return this.config.getStringList(key);
    }

    public @NotNull Material getMaterial(final @NotNull String key) throws IllegalStateException {
        final String materialString = this.getString(key);
        final Material material;
        try {
            material = Material.valueOf(materialString);
        } catch (final IllegalArgumentException e) {
            throw new IllegalStateException(materialString + " is not a valid material", e);
        }
        return material;
    }

    public @NotNull List<Material> getMaterialList(final @NotNull String key) throws IllegalStateException {
        final List<String> materialStringList = this.getStringList(key);
        final ReferenceList<Material> materialList = new ReferenceArrayList<>();
        for (final String materialString : materialStringList) {
            final Material material;
            try {
                material = Material.valueOf(materialString);
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException(materialString + " is not a valid material", e);
            }
            materialList.add(material);
        }
        return materialList;
    }

    public @NotNull String getMessagePath(final @NotNull String key) {
        return String.join(".", this.messagesSection.getCurrentPath(), key);
    }

    public @NotNull String getMessageString(final @NotNull String key) {
        return this.messagesSection.getString(key, this.getMessagePath(key));
    }

    public @NotNull List<String> getMessageStringList(final @NotNull String key) {
        return this.messagesSection.getStringList(key);
    }

    public @NotNull Component getComponent(final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        return MiniMessage.miniMessage().deserialize(this.getMessageString(key), tagResolvers);
    }

    public @NotNull List<Component> getComponentList(final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        final List<String> messageStringList = this.getMessageStringList(key);
        final List<Component> componentList = new ArrayList<>();
        for (final String messageString : messageStringList) {
            final Component component = MiniMessage.miniMessage().deserialize(messageString, tagResolvers);
            componentList.add(component);
        }
        return componentList;
    }

    public @NotNull Component getComponentPrefixed(final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        return Component.textOfChildren(this.prefix, this.getComponent(key, tagResolvers));
    }

    public void sendMessage(final @NotNull Audience audience, final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        audience.sendMessage(this.getComponent(key, tagResolvers));
    }

    public void sendMessagePrefixed(final @NotNull Audience audience, final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        audience.sendMessage(this.getComponentPrefixed(key, tagResolvers));
    }

    public void broadcast(final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        this.plugin.getServer().broadcast(this.getComponent(key, tagResolvers));
    }

    public void broadcast(final @NotNull String key, final @NotNull String permission, final @NotNull TagResolver... tagResolvers) {
        this.plugin.getServer().broadcast(this.getComponent(key, tagResolvers), permission);
    }

    public void broadcastPrefixed(final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        this.plugin.getServer().broadcast(this.getComponentPrefixed(key, tagResolvers));
    }

    public void broadcastPrefixed(final @NotNull String key, final @NotNull String permission, final @NotNull TagResolver... tagResolvers) {
        this.plugin.getServer().broadcast(this.getComponentPrefixed(key, tagResolvers), permission);
    }

    public void sendActionBar(final @NotNull Audience audience, final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        audience.sendActionBar(this.getComponent(key, tagResolvers));
    }

    public void sendActionBarPrefixed(final @NotNull Audience audience, final @NotNull String key, final @NotNull TagResolver... tagResolvers) {
        audience.sendActionBar(this.getComponentPrefixed(key, tagResolvers));
    }
}
