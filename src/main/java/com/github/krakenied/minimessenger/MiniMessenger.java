package com.github.krakenied.minimessenger;

import com.google.common.base.Preconditions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
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
            try (final InputStream inputStream = this.plugin.getResource(this.filename)) {
                this.file.getParentFile().mkdirs();
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

    @SuppressWarnings("DataFlowIssue")
    public @NotNull String getString(final @NotNull String key) {
        return this.config.getString(key);
    }

    public @NotNull List<String> getStringList(final @NotNull String key) {
        return this.config.getStringList(key);
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
