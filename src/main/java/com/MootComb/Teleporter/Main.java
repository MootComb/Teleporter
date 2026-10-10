package com.MootComb.Teleporter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main extends JavaPlugin implements Listener, TabCompleter {

    private static Main instance;

    public enum MessageType { CHAT, TITLE, SUBTITLE, NONE }
    public enum AccessLevel { OWNER, MEMBERS, GOVERNINGS, ALL }
    public enum BindClickType { LEFT, RIGHT }
    public enum TeleportLocation { TOP, CURRENT }

    private static final int DOUBLE_CLICK_DELAY_TICKS = 5;
    private static final int SNEAK_CHECK_DELAY_TICKS = 1;
    private static final int DEFAULT_END_ROD_COUNT = 5;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern SRGB_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    private String prefix;

    private Set<Material> elevatorBlocks = new HashSet<>();
    private int blockDistance;
    private boolean elevatorEnableParticle;
    private Particle elevatorParticleType;
    private int elevatorParticleCount;
    private String elevatorUsageSound;
    private String elevatorActivateSound;
    private boolean elevatorAllowUnsafe;
    private Set<String> disabledWorldsElevator = new HashSet<>();

    private boolean teleporterEnableParticle;
    private Particle teleporterParticleType;
    private int teleporterParticleCount;
    private String teleporterUsageSound;
    private String teleporterActivateSound;
    private boolean teleporterAllowUnsafe;
    private Set<String> disabledWorldsTeleporter = new HashSet<>();

    private boolean teleporterAllowAllBlocks;
    private Set<Material> teleporterBlockTypes = new HashSet<>();
    private String teleporterBlockTypesPermission;
    private double maxDistance;

    private final Map<Material, BindItem> teleporterBindItems = new LinkedHashMap<>();

    private String cooldownLocale;
    private MessageType cooldownMessageType;

    private int elevatorTitleFadeIn, elevatorTitleStay, elevatorTitleFadeOut;
    private int teleporterTitleFadeIn, teleporterTitleStay, teleporterTitleFadeOut;

    private String msgIdPrompt, msgIdInvalid, msgIdRange, msgIdUsed, msgIdSet;
    private MessageType msgIdPromptType, msgIdInvalidType, msgIdRangeType, msgIdUsedType, msgIdSetType;

    private String msgItemPrompt, msgItemDisabled, msgItemSet;
    private MessageType msgItemPromptType, msgItemDisabledType, msgItemSetType;

    private String msgNamePrompt, msgNameSet;
    private MessageType msgNamePromptType, msgNameSetType;

    private String msgPasswordPrompt, msgPasswordSet, msgPasswordDisabled, msgPasswordRequired, msgPasswordWrong, msgPasswordOk;
    private MessageType msgPasswordPromptType, msgPasswordSetType, msgPasswordDisabledType, msgPasswordRequiredType, msgPasswordWrongType, msgPasswordOkType;

    private String msgRedirectPrompt, msgRedirectAdded, msgRedirectRemoved, msgRedirectEmpty;
    private MessageType msgRedirectPromptType, msgRedirectAddedType, msgRedirectRemovedType, msgRedirectEmptyType;

    private String msgCancelled, msgNoPermission, msgBlockGone, msgPlayerNotFound, msgPlayerAdded;
    private MessageType msgCancelledType, msgNoPermissionType, msgBlockGoneType, msgPlayerNotFoundType, msgPlayerAddedType;

    private String msgNoBlockInSight, msgNotTeleporter, msgNoBlocksOwned, msgYourBlocks;
    private MessageType msgNoBlockInSightType, msgNotTeleporterType, msgNoBlocksOwnedType, msgYourBlocksType;

    private String msgRemoveUsage, msgRemoveInvalid, msgRemoveNone, msgRemoveNotOwner, msgRemoveSuccess;
    private MessageType msgRemoveUsageType, msgRemoveInvalidType, msgRemoveNoneType, msgRemoveNotOwnerType, msgRemoveSuccessType;

    private String msgUnknownSub, msgPlayersOnly, msgReloaded, msgListPrompt, msgHeaderInfo;
    private MessageType msgUnknownSubType, msgPlayersOnlyType, msgReloadedType, msgListPromptType, msgHeaderInfoType;

    private String msgViewDisabled, msgViewEnabled, msgViewUsage;
    private MessageType msgViewDisabledType, msgViewEnabledType, msgViewUsageType;

    private String msgTeleportSuccess, msgElevatorUp, msgElevatorDown, msgElevatorDanger;
    private MessageType msgTeleportSuccessType, msgElevatorUpType, msgElevatorDownType, msgElevatorDangerType;

    private String msgNoPair, msgNoCrossWorld, msgMissingItem, msgDistanceTooFar, msgBlockBound, msgFeatureDisabled;
    private MessageType msgNoPairType, msgNoCrossWorldType, msgMissingItemType, msgDistanceTooFarType, msgBlockBoundType, msgFeatureDisabledType;

    private int viewDurationTicks, viewUpdateTicks, viewMaxDistance, viewParticleCount;
    private Particle viewParticleType;

    private boolean hologramsDefaultEnabled;
    private String hologramsDefaultColor;
    private int hologramsViewDistance, hologramsUpdateInterval;
    private double hologramsLineHeight;

    private int blockNamingMaxLength;
    private String blockNamingDefaultName, blockNamingDefaultColor;

    private boolean debug;

    private File blocksFile;
    private FileConfiguration blocksConfig;

    private long messageCooldownMs = 3000L;

    private final Map<UUID, Long> cooldownMap = new HashMap<>();
    private final Map<UUID, Long> messageCooldown = new ConcurrentHashMap<>();
    private final Set<String> recentInteractions = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingPearlCancel = ConcurrentHashMap.newKeySet();
    private final Map<String, BlockData> blockDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> idIndex = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> globalMembers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> globalGovernings = new ConcurrentHashMap<>();
    private final Map<UUID, ChatInputSession> chatSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> openGuis = new HashMap<>();
    private final Map<UUID, GuiContext> guiContexts = new HashMap<>();
    private final Map<UUID, ViewSession> viewSessions = new HashMap<>();
    private final Map<String, UUID> hologramEntities = new ConcurrentHashMap<>();
    private final Map<String, Integer> redirectIndex = new ConcurrentHashMap<>();
    private final Map<UUID, PendingPassword> pendingPasswords = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, GuiItemMeta>> guiItemMetaMap = new HashMap<>();

    public static class BindItem {
        public Material material;
        public boolean consume;
        public int consumeAmount;
        public boolean require;
        public String permission;

        public BindItem(Material material, boolean consume, int consumeAmount, boolean require, String permission) {
            this.material = material;
            this.consume = consume;
            this.consumeAmount = Math.max(1, consumeAmount);
            this.require = require;
            this.permission = permission == null ? "" : permission;
        }
    }

    public static class BlockData {
        public int id;
        public UUID owner;
        public String world;
        public int x, y, z;
        public BindClickType clickType = BindClickType.RIGHT;
        public boolean requireSneak = false;
        public boolean requireItem = false;
        public String requiredItemName = "";
        public AccessLevel teleportAccess = AccessLevel.ALL;
        public AccessLevel manageAccess = AccessLevel.OWNER;
        public AccessLevel breakAccess = AccessLevel.ALL;
        public TeleportLocation teleportLocation = TeleportLocation.TOP;
        public String customDestinationWorld = "";
        public double customDestinationX, customDestinationY, customDestinationZ;
        public float customDestinationYaw, customDestinationPitch;
        public String customName = "";
        public boolean hologramEnabled = false;
        public boolean passwordEnabled = false;
        public String password = "";
        public boolean redirectEnabled = false;
        public List<Integer> redirectChain = new ArrayList<>();
        public Set<UUID> members = new HashSet<>();
        public Set<UUID> governings = new HashSet<>();

        public String key() {
            return world + ":" + x + ":" + y + ":" + z;
        }
    }

    public static class ChatInputSession {
        public String type;
        public String blockKey;
        public String listType;
        public long expireTime;

        public ChatInputSession(String type, String blockKey) {
            this.type = type;
            this.blockKey = blockKey;
            this.expireTime = System.currentTimeMillis() + 60000L;
        }

        public ChatInputSession(String type, String blockKey, String listType) {
            this.type = type;
            this.blockKey = blockKey;
            this.listType = listType;
            this.expireTime = System.currentTimeMillis() + 60000L;
        }
    }

    public static class GuiContext {
        public String blockKey;
        public String menuName;
        public String listType;
        public int page = 0;

        public GuiContext(String blockKey, String menuName) {
            this.blockKey = blockKey;
            this.menuName = menuName;
        }
    }

    public static class ViewSession {
        public String mode;
        public long endTime;
        public int taskId;

        public ViewSession(String mode, long endTime, int taskId) {
            this.mode = mode;
            this.endTime = endTime;
            this.taskId = taskId;
        }
    }

    public static class PendingPassword {
        public String blockKey;
        public long expireTime;

        public PendingPassword(String blockKey) {
            this.blockKey = blockKey;
            this.expireTime = System.currentTimeMillis() + 60000L;
        }
    }

    public static class GuiItemMeta {
        public String itemKey;
        public String menuName;
        public List<String> leftCommands = new ArrayList<>();
        public List<String> rightCommands = new ArrayList<>();
        public List<String> shiftLeftCommands = new ArrayList<>();
        public List<String> shiftRightCommands = new ArrayList<>();
    }

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        loadBlocks();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("teleporter") != null) {
            getCommand("teleporter").setExecutor(this);
            getCommand("teleporter").setTabCompleter(this);
        }
        startHologramUpdater();
        getLogger().info("==================================================");
        getLogger().info("   Teleporter Plugin Enabled!");
        getLogger().info("   Elevator blocks: " + elevatorBlocks.size());
        getLogger().info("   Registered blocks: " + blockDataMap.size());
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        for (ViewSession session : viewSessions.values()) {
            Bukkit.getScheduler().cancelTask(session.taskId);
        }
        viewSessions.clear();
        for (BlockData data : blockDataMap.values()) {
            removeHologram(data);
        }
        hologramEntities.clear();
        saveBlocks();
        getLogger().info("Teleporter Disabled!");
    }

    private String color(String message) {
        if (message == null) return "";
        String msg = message;
        Matcher matcher = HEX_PATTERN.matcher(msg);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);
        msg = buffer.toString();
        Matcher srgb = SRGB_PATTERN.matcher(msg);
        StringBuffer buffer2 = new StringBuffer();
        while (srgb.find()) {
            srgb.appendReplacement(buffer2, net.md_5.bungee.api.ChatColor.of("#" + srgb.group(1)).toString());
        }
        srgb.appendTail(buffer2);
        msg = buffer2.toString();
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(color(message));
    }

    private void debug(String message) {
        if (debug) getLogger().info("[DEBUG] " + message);
    }

    private MessageType msgType(String path, MessageType fallback) {
        String val = getConfig().getString(path);
        if (val == null) return fallback;
        return getMessageType(val);
    }

    private MessageType getMessageType(String type) {
        if (type == null) return MessageType.NONE;
        String t = type.trim().toUpperCase(Locale.ROOT);
        if (t.isEmpty() || t.equals("NONE") || t.equals("OFF") || t.equals("DISABLED")) return MessageType.NONE;
        try {
            return MessageType.valueOf(t);
        } catch (IllegalArgumentException e) {
            return MessageType.CHAT;
        }
    }

    private AccessLevel getAccessLevel(String level) {
        try {
            return AccessLevel.valueOf(level.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return AccessLevel.ALL;
        }
    }

    private BindClickType getClickType(String type) {
        try {
            return BindClickType.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BindClickType.RIGHT;
        }
    }

    private TeleportLocation getTeleportLocation(String type) {
        try {
            return TeleportLocation.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TeleportLocation.TOP;
        }
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        prefix = getConfig().getString("Messages.Prefix", "&#FF5300Teleporter &7| &f");

        elevatorBlocks.clear();
        for (String blockName : getConfig().getStringList("Elevator.BlockTypes")) {
            Material mat = Material.getMaterial(blockName);
            if (mat != null) elevatorBlocks.add(mat);
            else getLogger().warning("Unknown elevator block: " + blockName);
        }
        blockDistance = getConfig().getInt("Elevator.BlockDistance", 50);
        elevatorEnableParticle = getConfig().getBoolean("Elevator.EnableParticle", true);
        elevatorParticleCount = getConfig().getInt("Elevator.ParticleCount", 20);
        elevatorUsageSound = getConfig().getString("Elevator.UsageSound", "entity.enderman.teleport");
        elevatorActivateSound = getConfig().getString("Elevator.ActivateSound", "entity.player.levelup");
        elevatorAllowUnsafe = getConfig().getBoolean("Elevator.AllowUnsafe", false);
        try {
            elevatorParticleType = Particle.valueOf(getConfig().getString("Elevator.ParticleType", "SPELL_WITCH"));
        } catch (IllegalArgumentException e) {
            elevatorParticleType = Particle.SPELL_WITCH;
        }

        teleporterEnableParticle = getConfig().getBoolean("Teleporter.EnableParticle", true);
        teleporterParticleCount = getConfig().getInt("Teleporter.ParticleCount", 20);
        teleporterUsageSound = getConfig().getString("Teleporter.UsageSound", "entity.enderman.teleport");
        teleporterActivateSound = getConfig().getString("Teleporter.ActivateSound", "entity.player.levelup");
        teleporterAllowUnsafe = getConfig().getBoolean("Teleporter.AllowUnsafe", true);
        try {
            teleporterParticleType = Particle.valueOf(getConfig().getString("Teleporter.ParticleType", "SPELL_WITCH"));
        } catch (IllegalArgumentException e) {
            teleporterParticleType = Particle.SPELL_WITCH;
        }

        teleporterAllowAllBlocks = getConfig().getBoolean("Teleporter.AllowAllBlocks", false);
        teleporterBlockTypesPermission = getConfig().getString("Teleporter.BlockTypesPermission", "");
        maxDistance = getConfig().getDouble("Restrictions.Distance.MaxDistance", 10000.0);

        teleporterBlockTypes.clear();
        List<String> teleporterBlockNames = getConfig().getStringList("Teleporter.BlockTypes");
        if (teleporterBlockNames.isEmpty()) teleporterBlockNames.add("SEA_LANTERN");
        for (String blockName : teleporterBlockNames) {
            Material mat = Material.getMaterial(blockName);
            if (mat != null) teleporterBlockTypes.add(mat);
            else getLogger().warning("Unknown teleporter block: " + blockName);
        }

        teleporterBindItems.clear();
        ConfigurationSection bindItems = getConfig().getConfigurationSection("Teleporter.Bind.Items");
        if (bindItems != null) {
            for (String key : bindItems.getKeys(false)) {
                Material mat = Material.getMaterial(key);
                if (mat == null) {
                    getLogger().warning("Unknown bind item: " + key);
                    continue;
                }
                ConfigurationSection sec = bindItems.getConfigurationSection(key);
                boolean consume = sec == null || sec.getBoolean("Consume", true);
                int amount = sec == null ? 1 : sec.getInt("ConsumeAmount", 1);
                boolean require = sec != null && sec.getBoolean("Require", false);
                String perm = sec == null ? "" : sec.getString("Permission", "");
                teleporterBindItems.put(mat, new BindItem(mat, consume, amount, require, perm));
            }
        }
        if (teleporterBindItems.isEmpty()) {
            teleporterBindItems.put(Material.ENDER_PEARL, new BindItem(Material.ENDER_PEARL, true, 1, false, ""));
        }

        cooldownLocale = getConfig().getString("Restrictions.Cooldown.Locale", "&#FF5300Teleporter &7| &fCooldown: %time%s");
        cooldownMessageType = getMessageType(getConfig().getString("Restrictions.Cooldown.MessageType", "SUBTITLE"));
        messageCooldownMs = getConfig().getLong("Restrictions.MessageCooldown.Milliseconds", 3000L);

        ConfigurationSection et = getConfig().getConfigurationSection("Titles.Elevator");
        if (et != null) {
            elevatorTitleFadeIn = et.getInt("FadeIn", 10);
            elevatorTitleStay = et.getInt("Stay", 40);
            elevatorTitleFadeOut = et.getInt("FadeOut", 10);
        } else {
            elevatorTitleFadeIn = 10;
            elevatorTitleStay = 40;
            elevatorTitleFadeOut = 10;
        }
        ConfigurationSection tt = getConfig().getConfigurationSection("Titles.Teleporter");
        if (tt != null) {
            teleporterTitleFadeIn = tt.getInt("FadeIn", 10);
            teleporterTitleStay = tt.getInt("Stay", 60);
            teleporterTitleFadeOut = tt.getInt("FadeOut", 10);
        } else {
            teleporterTitleFadeIn = 10;
            teleporterTitleStay = 60;
            teleporterTitleFadeOut = 10;
        }

        msgIdPrompt = getConfig().getString("Messages.IdPrompt", "");
        msgIdPromptType = msgType("Messages.IdPromptType", MessageType.CHAT);
        msgIdInvalid = getConfig().getString("Messages.IdInvalid", "");
        msgIdInvalidType = msgType("Messages.IdInvalidType", MessageType.CHAT);
        msgIdRange = getConfig().getString("Messages.IdRange", "");
        msgIdRangeType = msgType("Messages.IdRangeType", MessageType.CHAT);
        msgIdUsed = getConfig().getString("Messages.IdUsed", "");
        msgIdUsedType = msgType("Messages.IdUsedType", MessageType.CHAT);
        msgIdSet = getConfig().getString("Messages.IdSet", "");
        msgIdSetType = msgType("Messages.IdSetType", MessageType.CHAT);

        msgItemPrompt = getConfig().getString("Messages.ItemPrompt", "");
        msgItemPromptType = msgType("Messages.ItemPromptType", MessageType.CHAT);
        msgItemDisabled = getConfig().getString("Messages.ItemDisabled", "");
        msgItemDisabledType = msgType("Messages.ItemDisabledType", MessageType.CHAT);
        msgItemSet = getConfig().getString("Messages.ItemSet", "");
        msgItemSetType = msgType("Messages.ItemSetType", MessageType.CHAT);

        msgNamePrompt = getConfig().getString("Messages.NamePrompt", "");
        msgNamePromptType = msgType("Messages.NamePromptType", MessageType.CHAT);
        msgNameSet = getConfig().getString("Messages.NameSet", "");
        msgNameSetType = msgType("Messages.NameSetType", MessageType.CHAT);

        msgPasswordPrompt = getConfig().getString("Messages.PasswordPrompt", "");
        msgPasswordPromptType = msgType("Messages.PasswordPromptType", MessageType.CHAT);
        msgPasswordSet = getConfig().getString("Messages.PasswordSet", "");
        msgPasswordSetType = msgType("Messages.PasswordSetType", MessageType.CHAT);
        msgPasswordDisabled = getConfig().getString("Messages.PasswordDisabled", "");
        msgPasswordDisabledType = msgType("Messages.PasswordDisabledType", MessageType.CHAT);
        msgPasswordRequired = getConfig().getString("Messages.PasswordRequired", "");
        msgPasswordRequiredType = msgType("Messages.PasswordRequiredType", MessageType.CHAT);
        msgPasswordWrong = getConfig().getString("Messages.PasswordWrong", "");
        msgPasswordWrongType = msgType("Messages.PasswordWrongType", MessageType.CHAT);
        msgPasswordOk = getConfig().getString("Messages.PasswordOk", "");
        msgPasswordOkType = msgType("Messages.PasswordOkType", MessageType.CHAT);

        msgRedirectPrompt = getConfig().getString("Messages.RedirectPrompt", "");
        msgRedirectPromptType = msgType("Messages.RedirectPromptType", MessageType.CHAT);
        msgRedirectAdded = getConfig().getString("Messages.RedirectAdded", "");
        msgRedirectAddedType = msgType("Messages.RedirectAddedType", MessageType.CHAT);
        msgRedirectRemoved = getConfig().getString("Messages.RedirectRemoved", "");
        msgRedirectRemovedType = msgType("Messages.RedirectRemovedType", MessageType.CHAT);
        msgRedirectEmpty = getConfig().getString("Messages.RedirectEmpty", "");
        msgRedirectEmptyType = msgType("Messages.RedirectEmptyType", MessageType.CHAT);

        msgCancelled = getConfig().getString("Messages.Cancelled", "");
        msgCancelledType = msgType("Messages.CancelledType", MessageType.CHAT);
        msgNoPermission = getConfig().getString("Messages.NoPermission", "");
        msgNoPermissionType = msgType("Messages.NoPermissionType", MessageType.CHAT);
        msgBlockGone = getConfig().getString("Messages.BlockGone", "");
        msgBlockGoneType = msgType("Messages.BlockGoneType", MessageType.CHAT);
        msgPlayerNotFound = getConfig().getString("Messages.PlayerNotFound", "");
        msgPlayerNotFoundType = msgType("Messages.PlayerNotFoundType", MessageType.CHAT);
        msgPlayerAdded = getConfig().getString("Messages.PlayerAdded", "");
        msgPlayerAddedType = msgType("Messages.PlayerAddedType", MessageType.CHAT);

        msgNoBlockInSight = getConfig().getString("Messages.NoBlockInSight", "");
        msgNoBlockInSightType = msgType("Messages.NoBlockInSightType", MessageType.CHAT);
        msgNotTeleporter = getConfig().getString("Messages.NotTeleporter", "");
        msgNotTeleporterType = msgType("Messages.NotTeleporterType", MessageType.CHAT);
        msgNoBlocksOwned = getConfig().getString("Messages.NoBlocksOwned", "");
        msgNoBlocksOwnedType = msgType("Messages.NoBlocksOwnedType", MessageType.CHAT);
        msgYourBlocks = getConfig().getString("Messages.YourBlocks", "");
        msgYourBlocksType = msgType("Messages.YourBlocksType", MessageType.CHAT);

        msgRemoveUsage = getConfig().getString("Messages.RemoveUsage", "");
        msgRemoveUsageType = msgType("Messages.RemoveUsageType", MessageType.CHAT);
        msgRemoveInvalid = getConfig().getString("Messages.RemoveInvalid", "");
        msgRemoveInvalidType = msgType("Messages.RemoveInvalidType", MessageType.CHAT);
        msgRemoveNone = getConfig().getString("Messages.RemoveNone", "");
        msgRemoveNoneType = msgType("Messages.RemoveNoneType", MessageType.CHAT);
        msgRemoveNotOwner = getConfig().getString("Messages.RemoveNotOwner", "");
        msgRemoveNotOwnerType = msgType("Messages.RemoveNotOwnerType", MessageType.CHAT);
        msgRemoveSuccess = getConfig().getString("Messages.RemoveSuccess", "");
        msgRemoveSuccessType = msgType("Messages.RemoveSuccessType", MessageType.CHAT);

        msgUnknownSub = getConfig().getString("Messages.UnknownSub", "");
        msgUnknownSubType = msgType("Messages.UnknownSubType", MessageType.CHAT);
        msgPlayersOnly = getConfig().getString("Messages.PlayersOnly", "");
        msgPlayersOnlyType = msgType("Messages.PlayersOnlyType", MessageType.CHAT);
        msgReloaded = getConfig().getString("Messages.Reloaded", "");
        msgReloadedType = msgType("Messages.ReloadedType", MessageType.CHAT);
        msgListPrompt = getConfig().getString("Messages.ListPrompt", "");
        msgListPromptType = msgType("Messages.ListPromptType", MessageType.CHAT);
        msgHeaderInfo = getConfig().getString("Messages.HeaderInfo", "");
        msgHeaderInfoType = msgType("Messages.HeaderInfoType", MessageType.CHAT);

        msgViewDisabled = getConfig().getString("Messages.ViewDisabled", "");
        msgViewDisabledType = msgType("Messages.ViewDisabledType", MessageType.CHAT);
        msgViewEnabled = getConfig().getString("Messages.ViewEnabled", "");
        msgViewEnabledType = msgType("Messages.ViewEnabledType", MessageType.CHAT);
        msgViewUsage = getConfig().getString("Messages.ViewUsage", "");
        msgViewUsageType = msgType("Messages.ViewUsageType", MessageType.CHAT);

        msgTeleportSuccess = getConfig().getString("Messages.TeleportSuccess", "");
        msgTeleportSuccessType = msgType("Messages.TeleportSuccessType", MessageType.SUBTITLE);
        msgElevatorUp = getConfig().getString("Messages.ElevatorUp", "");
        msgElevatorUpType = msgType("Messages.ElevatorUpType", MessageType.SUBTITLE);
        msgElevatorDown = getConfig().getString("Messages.ElevatorDown", "");
        msgElevatorDownType = msgType("Messages.ElevatorDownType", MessageType.SUBTITLE);
        msgElevatorDanger = getConfig().getString("Messages.ElevatorDanger", "");
        msgElevatorDangerType = msgType("Messages.ElevatorDangerType", MessageType.SUBTITLE);

        msgNoPair = getConfig().getString("Messages.NoPair", "");
        msgNoPairType = msgType("Messages.NoPairType", MessageType.CHAT);
        msgNoCrossWorld = getConfig().getString("Messages.NoCrossWorld", "");
        msgNoCrossWorldType = msgType("Messages.NoCrossWorldType", MessageType.CHAT);
        msgMissingItem = getConfig().getString("Messages.MissingItem", "");
        msgMissingItemType = msgType("Messages.MissingItemType", MessageType.CHAT);
        msgDistanceTooFar = getConfig().getString("Messages.DistanceTooFar", "");
        msgDistanceTooFarType = msgType("Messages.DistanceTooFarType", MessageType.CHAT);
        msgBlockBound = getConfig().getString("Messages.BlockBound", "");
        msgBlockBoundType = msgType("Messages.BlockBoundType", MessageType.CHAT);
        msgFeatureDisabled = getConfig().getString("Messages.FeatureDisabled", "");
        msgFeatureDisabledType = msgType("Messages.FeatureDisabledType", MessageType.CHAT);

        viewDurationTicks = getConfig().getInt("View.DurationTicks", 200);
        viewUpdateTicks = getConfig().getInt("View.UpdateEveryTicks", 5);
        viewMaxDistance = getConfig().getInt("View.MaxDistance", 64);
        viewParticleCount = getConfig().getInt("View.ParticleCount", 3);
        try {
            viewParticleType = Particle.valueOf(getConfig().getString("View.ParticleType", "END_ROD"));
        } catch (IllegalArgumentException e) {
            viewParticleType = Particle.END_ROD;
        }

        hologramsDefaultEnabled = getConfig().getBoolean("Holograms.DefaultEnabled", false);
        hologramsDefaultColor = getConfig().getString("Holograms.DefaultColor", "&f");
        hologramsViewDistance = getConfig().getInt("Holograms.ViewDistance", 16);
        hologramsUpdateInterval = getConfig().getInt("Holograms.UpdateIntervalTicks", 20);
        hologramsLineHeight = getConfig().getDouble("Holograms.LineHeight", 0.3);

        blockNamingMaxLength = getConfig().getInt("BlockNaming.MaxLength", 64);
        blockNamingDefaultName = getConfig().getString("BlockNaming.DefaultName", "");
        blockNamingDefaultColor = getConfig().getString("BlockNaming.DefaultColor", "&f");

        disabledWorldsElevator.clear();
        disabledWorldsElevator.addAll(getConfig().getStringList("DisabledWorlds.Elevator"));
        disabledWorldsTeleporter.clear();
        disabledWorldsTeleporter.addAll(getConfig().getStringList("DisabledWorlds.Teleporter"));

        debug = getConfig().getBoolean("Debug", false);
    }

    private boolean usageEnabled(String usage) {
        return getConfig().getBoolean("Usage." + usage + ".Enabled", true);
    }

    private boolean usageAllowed(Player player, String usage) {
        if (!usageEnabled(usage)) return false;
        if (!getConfig().getBoolean("Usage." + usage + ".Require", false)) return true;
        String perm = getConfig().getString("Usage." + usage + ".Permission", "");
        return perm.isEmpty() || player.hasPermission(perm);
    }

    private boolean featureEnabled(String feature) {
        return getConfig().getBoolean("Features." + feature + ".Enabled", true);
    }

    private boolean featureAllowed(Player player, String feature) {
        if (!featureEnabled(feature)) return false;
        if (!getConfig().getBoolean("Features." + feature + ".Require", false)) return true;
        String perm = getConfig().getString("Features." + feature + ".Permission", "");
        return perm.isEmpty() || player.hasPermission(perm);
    }

    private AccessLevel featureDefaultAccess(String feature, AccessLevel fallback) {
        String val = getConfig().getString("Features." + feature + ".DefaultAccess");
        if (val == null) return fallback;
        return getAccessLevel(val);
    }

    private boolean commandAllowed(CommandSender sender, String command) {
        if (!getConfig().getBoolean("Commands." + command + ".Enabled", true)) return false;
        if (!getConfig().getBoolean("Commands." + command + ".Require", false)) return true;
        String perm = getConfig().getString("Commands." + command + ".Permission", "");
        return perm.isEmpty() || sender.hasPermission(perm);
    }

    private boolean viewModeAllowed(Player player, String mode) {
        if (!getConfig().getBoolean("Commands.View.Enabled", true)) return false;
        String suffix;
        if (mode.equalsIgnoreCase("all")) suffix = "All";
        else if (mode.equalsIgnoreCase("owner")) suffix = "Owner";
        else if (mode.equalsIgnoreCase("member")) suffix = "Member";
        else return false;
        if (!getConfig().getBoolean("Commands.View." + suffix + ".Require", false)) return true;
        String perm = getConfig().getString("Commands.View." + suffix + ".Permission", "");
        return perm.isEmpty() || player.hasPermission(perm);
    }

    private boolean restrictionEnabled(String restriction) {
        return getConfig().getBoolean("Restrictions." + restriction + ".Enabled", true);
    }

    private boolean restrictionExempt(Player player, String restriction) {
        if (!restrictionEnabled(restriction)) return true;
        if (!getConfig().getBoolean("Restrictions." + restriction + ".Exempt.Require", false)) return false;
        String perm = getConfig().getString("Restrictions." + restriction + ".Exempt.Permission", "");
        return !perm.isEmpty() && player.hasPermission(perm);
    }

    private boolean isBindItem(ItemStack item) {
        return item != null && teleporterBindItems.containsKey(item.getType());
    }

    private BindItem getBindItem(ItemStack item) {
        if (item == null) return null;
        return teleporterBindItems.get(item.getType());
    }

    private void consumeBindItem(Player player, ItemStack hand, BindItem bindItem) {
        if (!bindItem.consume) return;
        int amount = hand.getAmount();
        int toConsume = bindItem.consumeAmount;
        if (amount > toConsume) {
            hand.setAmount(amount - toConsume);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }

    private boolean bindItemAllowed(Player player, BindItem bindItem) {
        if (bindItem.require && !bindItem.permission.isEmpty()) {
            return player.hasPermission(bindItem.permission);
        }
        return true;
    }

    private void loadBlocks() {
        blockDataMap.clear();
        idIndex.clear();
        playerBlocks.clear();
        globalMembers.clear();
        globalGovernings.clear();
        redirectIndex.clear();

        blocksFile = new File(getDataFolder(), "blocks.yml");
        if (!blocksFile.exists()) {
            try {
                getDataFolder().mkdirs();
                blocksFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create blocks.yml: " + e.getMessage());
                return;
            }
        }
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);

        ConfigurationSection blocksSection = blocksConfig.getConfigurationSection("Blocks");
        if (blocksSection != null) {
            for (String key : blocksSection.getKeys(false)) {
                ConfigurationSection sec = blocksSection.getConfigurationSection(key);
                if (sec == null) continue;
                BlockData data = new BlockData();
                data.id = sec.getInt("Id", 0);
                try {
                    data.owner = UUID.fromString(sec.getString("Owner", ""));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                data.world = sec.getString("World", "world");
                data.x = sec.getInt("X", 0);
                data.y = sec.getInt("Y", 0);
                data.z = sec.getInt("Z", 0);
                data.clickType = getClickType(sec.getString("ClickType", "RIGHT"));
                data.requireSneak = sec.getBoolean("RequireSneak", false);
                data.requireItem = sec.getBoolean("RequireItem", false);
                data.requiredItemName = sec.getString("RequiredItemName", "");
                data.teleportAccess = getAccessLevel(sec.getString("TeleportAccess", "ALL"));
                data.manageAccess = getAccessLevel(sec.getString("ManageAccess", "OWNER"));
                data.breakAccess = getAccessLevel(sec.getString("BreakAccess", "ALL"));
                data.teleportLocation = getTeleportLocation(sec.getString("TeleportLocation", "TOP"));
                data.customDestinationWorld = sec.getString("CustomDestination.World", "");
                data.customDestinationX = sec.getDouble("CustomDestination.X", 0);
                data.customDestinationY = sec.getDouble("CustomDestination.Y", 0);
                data.customDestinationZ = sec.getDouble("CustomDestination.Z", 0);
                data.customDestinationYaw = (float) sec.getDouble("CustomDestination.Yaw", 0);
                data.customDestinationPitch = (float) sec.getDouble("CustomDestination.Pitch", 0);
                data.customName = sec.getString("CustomName", "");
                data.hologramEnabled = sec.getBoolean("HologramEnabled", hologramsDefaultEnabled);
                data.passwordEnabled = sec.getBoolean("PasswordEnabled", false);
                data.password = sec.getString("Password", "");
                data.redirectEnabled = sec.getBoolean("RedirectEnabled", false);
                data.redirectChain = sec.getIntegerList("RedirectChain");
                for (String member : sec.getStringList("Members")) {
                    try { data.members.add(UUID.fromString(member)); } catch (IllegalArgumentException ignored) {}
                }
                for (String gov : sec.getStringList("Governings")) {
                    try { data.governings.add(UUID.fromString(gov)); } catch (IllegalArgumentException ignored) {}
                }
                for (String gov : sec.getStringList("Owners")) {
                    try { data.governings.add(UUID.fromString(gov)); } catch (IllegalArgumentException ignored) {}
                }
                blockDataMap.put(data.key(), data);
                idIndex.computeIfAbsent(data.id, k -> ConcurrentHashMap.newKeySet()).add(data.key());
                playerBlocks.computeIfAbsent(data.owner, k -> new ArrayList<>()).add(data.key());
            }
        }

        ConfigurationSection playersSection = blocksConfig.getConfigurationSection("Players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection pSec = playersSection.getConfigurationSection(uuidStr);
                if (pSec == null) continue;
                Set<UUID> members = ConcurrentHashMap.newKeySet();
                for (String m : pSec.getStringList("GlobalMembers")) {
                    try { members.add(UUID.fromString(m)); } catch (IllegalArgumentException ignored) {}
                }
                Set<UUID> governings = ConcurrentHashMap.newKeySet();
                for (String o : pSec.getStringList("GlobalGovernings")) {
                    try { governings.add(UUID.fromString(o)); } catch (IllegalArgumentException ignored) {}
                }
                for (String o : pSec.getStringList("GlobalOwners")) {
                    try { governings.add(UUID.fromString(o)); } catch (IllegalArgumentException ignored) {}
                }
                if (!members.isEmpty()) globalMembers.put(uuid, members);
                if (!governings.isEmpty()) globalGovernings.put(uuid, governings);
            }
        }
    }

    private void saveBlocks() {
        if (blocksConfig == null || blocksFile == null) return;
        blocksConfig.set("Blocks", null);
        for (Map.Entry<String, BlockData> entry : blockDataMap.entrySet()) {
            BlockData data = entry.getValue();
            String path = "Blocks." + entry.getKey().replace(".", "_").replace(":", "_");
            blocksConfig.set(path + ".Id", data.id);
            blocksConfig.set(path + ".Owner", data.owner.toString());
            blocksConfig.set(path + ".World", data.world);
            blocksConfig.set(path + ".X", data.x);
            blocksConfig.set(path + ".Y", data.y);
            blocksConfig.set(path + ".Z", data.z);
            blocksConfig.set(path + ".ClickType", data.clickType.name());
            blocksConfig.set(path + ".RequireSneak", data.requireSneak);
            blocksConfig.set(path + ".RequireItem", data.requireItem);
            blocksConfig.set(path + ".RequiredItemName", data.requiredItemName);
            blocksConfig.set(path + ".TeleportAccess", data.teleportAccess.name());
            blocksConfig.set(path + ".ManageAccess", data.manageAccess.name());
            blocksConfig.set(path + ".BreakAccess", data.breakAccess.name());
            blocksConfig.set(path + ".TeleportLocation", data.teleportLocation.name());
            blocksConfig.set(path + ".CustomDestination.World", data.customDestinationWorld);
            blocksConfig.set(path + ".CustomDestination.X", data.customDestinationX);
            blocksConfig.set(path + ".CustomDestination.Y", data.customDestinationY);
            blocksConfig.set(path + ".CustomDestination.Z", data.customDestinationZ);
            blocksConfig.set(path + ".CustomDestination.Yaw", data.customDestinationYaw);
            blocksConfig.set(path + ".CustomDestination.Pitch", data.customDestinationPitch);
            blocksConfig.set(path + ".CustomName", data.customName);
            blocksConfig.set(path + ".HologramEnabled", data.hologramEnabled);
            blocksConfig.set(path + ".PasswordEnabled", data.passwordEnabled);
            blocksConfig.set(path + ".Password", data.password);
            blocksConfig.set(path + ".RedirectEnabled", data.redirectEnabled);
            blocksConfig.set(path + ".RedirectChain", data.redirectChain);
            List<String> members = new ArrayList<>();
            for (UUID u : data.members) members.add(u.toString());
            blocksConfig.set(path + ".Members", members);
            List<String> governings = new ArrayList<>();
            for (UUID u : data.governings) governings.add(u.toString());
            blocksConfig.set(path + ".Governings", governings);
        }

        blocksConfig.set("Players", null);
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(globalMembers.keySet());
        allPlayers.addAll(globalGovernings.keySet());
        for (UUID uuid : allPlayers) {
            String path = "Players." + uuid.toString();
            List<String> members = new ArrayList<>();
            Set<UUID> m = globalMembers.get(uuid);
            if (m != null) for (UUID u : m) members.add(u.toString());
            blocksConfig.set(path + ".GlobalMembers", members);
            List<String> governings = new ArrayList<>();
            Set<UUID> o = globalGovernings.get(uuid);
            if (o != null) for (UUID u : o) governings.add(u.toString());
            blocksConfig.set(path + ".GlobalGovernings", governings);
        }

        try {
            blocksConfig.save(blocksFile);
        } catch (IOException e) {
            getLogger().severe("Could not save blocks.yml: " + e.getMessage());
        }
    }

    private void sendMessage(Player player, String message, MessageType type, int fadeIn, int stay, int fadeOut) {
        if (type == null || type == MessageType.NONE) return;
        if (message == null || message.isEmpty()) return;
        String formatted = color(message);
        if (formatted.isEmpty()) return;
        switch (type) {
            case CHAT:
                player.sendMessage(formatted);
                break;
            case TITLE:
                player.sendTitle(formatted, "", fadeIn, stay, fadeOut);
                break;
            case SUBTITLE:
                player.sendTitle("", formatted, fadeIn, stay, fadeOut);
                break;
        }
    }

    private void sendElevatorMessage(Player player, String message, MessageType type) {
        sendMessage(player, message, type, elevatorTitleFadeIn, elevatorTitleStay, elevatorTitleFadeOut);
    }

    private void sendTeleporterMessage(Player player, String message, MessageType type) {
        sendMessage(player, message, type, teleporterTitleFadeIn, teleporterTitleStay, teleporterTitleFadeOut);
    }

    private void sendElevatorPrefixed(Player player, String message, MessageType type) {
        if (message != null && !message.isEmpty()) sendElevatorMessage(player, prefix + message, type);
    }

    private void sendTeleporterPrefixed(Player player, String message, MessageType type) {
        if (message != null && !message.isEmpty()) sendTeleporterMessage(player, prefix + message, type);
    }

    private boolean canSendMessage(Player player) {
        UUID uuid = player.getUniqueId();
        Long last = messageCooldown.get(uuid);
        long now = System.currentTimeMillis();
        if (last != null && now - last < messageCooldownMs) {
            return false;
        }
        messageCooldown.put(uuid, now);
        return true;
    }

    private void sendTeleporterPrefixedThrottled(Player player, String message, MessageType type) {
        if (!canSendMessage(player)) return;
        sendTeleporterPrefixed(player, message, type);
    }

    private void sendElevatorPrefixedThrottled(Player player, String message, MessageType type) {
        if (!canSendMessage(player)) return;
        sendElevatorPrefixed(player, message, type);
    }

    private boolean isInDisabledWorld(Player player, boolean teleporter) {
        String world = player.getWorld().getName();
        return teleporter ? disabledWorldsTeleporter.contains(world) : disabledWorldsElevator.contains(world);
    }

    private long getCooldownRemaining(Player player) {
        if (!restrictionEnabled("Cooldown")) return 0;
        if (restrictionExempt(player, "Cooldown")) return 0;
        Long lastUse = cooldownMap.get(player.getUniqueId());
        if (lastUse == null) return 0;
        int cooldownTime = getConfig().getInt("Restrictions.Cooldown.Time", 30);
        long timeLeft = (lastUse + cooldownTime * 1000L) - System.currentTimeMillis();
        if (timeLeft <= 0) {
            cooldownMap.remove(player.getUniqueId());
            return 0;
        }
        return timeLeft;
    }

    private boolean isOnCooldown(Player player) {
        long left = getCooldownRemaining(player);
        if (left > 0) {
            if (canSendMessage(player)) {
                String msg = cooldownLocale.replace("%time%", String.valueOf(left / 1000 + 1));
                sendTeleporterMessage(player, msg, cooldownMessageType);
            }
            return true;
        }
        return false;
    }

    private void setCooldown(Player player) {
        if (restrictionEnabled("Cooldown") && !restrictionExempt(player, "Cooldown")) {
            cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    private boolean isSafeLocation(Location loc, boolean teleporter) {
        boolean unsafeAllowed = teleporter ? teleporterAllowUnsafe : elevatorAllowUnsafe;
        if (unsafeAllowed) return true;
        Material type = loc.getBlock().getType();
        return !type.isSolid() && type != Material.LAVA && type != Material.FIRE;
    }

    private void spawnElevatorParticles(Location loc) {
        if (!elevatorEnableParticle) return;
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(elevatorParticleType, loc.clone().add(0, 0.5, 0), elevatorParticleCount, 0.3, 0.1, 0.3, 0.1);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), DEFAULT_END_ROD_COUNT, 0.2, 0.2, 0.2, 0.05);
    }

    private void spawnTeleporterParticles(Location loc) {
        if (!teleporterEnableParticle) return;
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(teleporterParticleType, loc.clone().add(0, 0.5, 0), teleporterParticleCount, 0.3, 0.1, 0.3, 0.1);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), DEFAULT_END_ROD_COUNT, 0.2, 0.2, 0.2, 0.05);
    }

    private void playElevatorEffects(Location loc, String soundName) {
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                World world = loc.getWorld();
                if (world != null) world.playSound(loc, sound, 0.5f, 1.2f);
            } catch (IllegalArgumentException ignored) {}
        }
        spawnElevatorParticles(loc);
    }

    private void playTeleporterEffects(Location loc, String soundName) {
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                World world = loc.getWorld();
                if (world != null) world.playSound(loc, sound, 0.5f, 1.2f);
            } catch (IllegalArgumentException ignored) {}
        }
        spawnTeleporterParticles(loc);
    }

    private void teleportDown(Player player) {
        if (isInDisabledWorld(player, false)) return;
        if (!usageAllowed(player, "Elevator")) return;

        Location feetLocation = player.getLocation().clone();
        if (!elevatorBlocks.contains(feetLocation.getBlock().getType())) return;

        for (int i = 1; i <= blockDistance; i++) {
            Location checkLoc = feetLocation.clone().subtract(0, i, 0);
            if (elevatorBlocks.contains(checkLoc.getBlock().getType())) {
                Location targetLoc = checkLoc.clone();
                if (!isSafeLocation(targetLoc, false)) {
                    sendElevatorPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
                    return;
                }
                player.teleport(targetLoc);
                playElevatorEffects(targetLoc, elevatorUsageSound);
                sendElevatorPrefixedThrottled(player, msgElevatorDown, msgElevatorDownType);
                setCooldown(player);
                return;
            }
        }
    }

    private void teleportUp(Player player) {
        if (isInDisabledWorld(player, false)) return;
        if (!usageAllowed(player, "Elevator")) return;

        Location feetLocation = player.getLocation().clone();
        if (!elevatorBlocks.contains(feetLocation.getBlock().getType())) return;

        for (int i = 1; i <= blockDistance; i++) {
            Location checkLoc = feetLocation.clone().add(0, i, 0);
            if (elevatorBlocks.contains(checkLoc.getBlock().getType())) {
                Location targetLoc = checkLoc.clone();
                if (!isSafeLocation(targetLoc, false)) {
                    sendElevatorPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
                    return;
                }
                player.setVelocity(player.getVelocity().setY(0));
                final Location finalLoc = targetLoc;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        player.teleport(finalLoc);
                        playElevatorEffects(finalLoc, elevatorUsageSound);
                        sendElevatorPrefixedThrottled(player, msgElevatorUp, msgElevatorUpType);
                        setCooldown(player);
                    }
                }.runTask(this);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (event.getTo().getY() <= event.getFrom().getY()) return;
        if (event.getFrom().getBlock().getY() == event.getTo().getBlock().getY()) return;

        Location feetLocation = player.getLocation().clone();
        feetLocation.setY(feetLocation.getY() - 0.1);

        if (!usageAllowed(player, "Elevator")) return;
        if (!elevatorBlocks.contains(feetLocation.getBlock().getType())) return;

        for (int i = 1; i <= blockDistance; i++) {
            Location checkLoc = feetLocation.clone().add(0, i, 0);
            if (!elevatorBlocks.contains(checkLoc.getBlock().getType())) continue;

            Location targetLoc = checkLoc.clone().subtract(0, 0.65, 0);
            if (!isSafeLocation(targetLoc, false)) {
                sendElevatorPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
                return;
            }

            if (isOnCooldown(player)) return;

            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            player.setFallDistance(0);
            player.teleport(targetLoc);
            playElevatorEffects(targetLoc, elevatorUsageSound);
            sendElevatorPrefixedThrottled(player, msgElevatorUp, msgElevatorUpType);
            setCooldown(player);

            return;
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) return;

        final Player p = player;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !p.isSneaking()) return;
                if (isOnCooldown(p)) return;
                teleportDown(p);
            }
        }.runTaskLater(this, SNEAK_CHECK_DELAY_TICKS);
    }

    private boolean canBindBlock(Player player, Block block, Material blockType, ItemStack hand) {
        if (!player.isSneaking()) return false;
        if (isInDisabledWorld(player, true)) return false;
        BindItem bindItem = getBindItem(hand);
        if (bindItem == null) return false;
        if (!bindItemAllowed(player, bindItem)) return false;
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        if (blockDataMap.containsKey(key)) return false;
        if (!teleporterAllowAllBlocks) {
            if (!teleporterBlockTypes.contains(blockType)) {
                boolean allowedByPerm = teleporterBlockTypesPermission != null
                        && !teleporterBlockTypesPermission.isEmpty()
                        && player.hasPermission(teleporterBlockTypesPermission);
                if (!allowedByPerm) return false;
            }
        } else {
            if (teleporterBlockTypesPermission != null
                    && !teleporterBlockTypesPermission.isEmpty()
                    && !player.hasPermission(teleporterBlockTypesPermission)) {
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteractLowest(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!player.isSneaking()) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isBindItem(hand)) return;
        Block block = event.getClickedBlock();
        Material blockType = block.getType();
        if (!canBindBlock(player, block, blockType, hand)) return;
        event.setCancelled(true);
        try {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        } catch (Throwable ignored) {}
        player.setMetadata("teleporter_cancel_pearl", new FixedMetadataValue(this, System.currentTimeMillis() + 300L));
        pendingPearlCancel.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                pendingPearlCancel.remove(player.getUniqueId());
                if (player.hasMetadata("teleporter_cancel_pearl")) player.removeMetadata("teleporter_cancel_pearl", Main.this);
            }
        }.runTaskLater(this, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        Material blockType = block.getType();
        Action action = event.getAction();
        boolean isRight = action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeft = action == Action.LEFT_CLICK_BLOCK;
        if (!isRight && !isLeft) return;

        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        BlockData data = blockDataMap.get(key);
        ItemStack handItem = player.getInventory().getItemInMainHand();
        boolean holdingBindItem = isBindItem(handItem);

        if (data == null) {
            if (isRight && player.isSneaking() && holdingBindItem) {
                if (canBindBlock(player, block, blockType, handItem)) {
                    event.setCancelled(true);
                    tryBindBlock(player, block, blockType);
                }
            }
            return;
        }

        boolean guiSneak = data.requireSneak ? !player.isSneaking() : player.isSneaking();

        if (isRight && guiSneak) {
            event.setCancelled(true);
            if (!canManage(player, data)) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            openGui(player, data, "main");
            return;
        }

        if (!usageAllowed(player, "Teleport")) {
            debug("Usage Teleport disabled or no permission");
            return;
        }
        if (!canTeleport(player, data)) {
            debug("No teleport rights for " + player.getName());
            return;
        }

        BindClickType clicked = isRight ? BindClickType.RIGHT : BindClickType.LEFT;
        if (data.clickType != clicked) return;

        if (restrictionEnabled("Sneak") && !restrictionExempt(player, "Sneak")) {
            if (data.requireSneak && !player.isSneaking()) return;
            if (!data.requireSneak && player.isSneaking()) return;
        }

        if (restrictionEnabled("Item") && !restrictionExempt(player, "Item")) {
            if (data.requireItem) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (!itemMatches(item, data.requiredItemName)) {
                    sendTeleporterPrefixedThrottled(player, msgMissingItem, msgMissingItemType);
                    return;
                }
            }
        }

        String interactionKey = player.getUniqueId() + ":" + key;
        if (recentInteractions.contains(interactionKey)) return;
        recentInteractions.add(interactionKey);
        new BukkitRunnable() {
            @Override
            public void run() {
                recentInteractions.remove(interactionKey);
            }
        }.runTaskLater(this, DOUBLE_CLICK_DELAY_TICKS);

        event.setCancelled(true);

        if (data.passwordEnabled && !data.password.isEmpty()) {
            pendingPasswords.put(player.getUniqueId(), new PendingPassword(data.key()));
            sendTeleporterPrefixedThrottled(player, msgPasswordRequired, msgPasswordRequiredType);
            return;
        }

        performTeleport(player, data);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player player = (Player) event.getEntity().getShooter();
        if (pendingPearlCancel.contains(player.getUniqueId())) {
            event.setCancelled(true);
            pendingPearlCancel.remove(player.getUniqueId());
            try {
                if (player.hasMetadata("teleporter_cancel_pearl")) player.removeMetadata("teleporter_cancel_pearl", Main.this);
            } catch (Throwable ignored) {}
        }
    }

    private boolean itemMatches(ItemStack item, String requiredName) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (requiredName == null || requiredName.isEmpty()) return true;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String display = stripColor(meta.getDisplayName());
        String required = stripColor(requiredName);
        return display.equalsIgnoreCase(required);
    }

    private void tryBindBlock(Player player, Block block, Material blockType) {
        if (!canBindBlock(player, block, blockType, player.getInventory().getItemInMainHand())) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        BindItem bindItem = getBindItem(hand);
        if (bindItem == null) return;
        consumeBindItem(player, hand, bindItem);

        BlockData data = new BlockData();
        data.owner = player.getUniqueId();
        data.world = block.getWorld().getName();
        data.x = block.getX();
        data.y = block.getY();
        data.z = block.getZ();
        data.clickType = BindClickType.RIGHT;
        data.teleportAccess = featureDefaultAccess("TeleportAccess", AccessLevel.ALL);
        data.manageAccess = featureDefaultAccess("ManageAccess", AccessLevel.OWNER);
        data.breakAccess = featureDefaultAccess("BreakAccess", AccessLevel.ALL);
        data.teleportLocation = TeleportLocation.TOP;
        data.customName = blockNamingDefaultName;
        data.hologramEnabled = hologramsDefaultEnabled;
        data.id = 0;

        String key = data.key();
        blockDataMap.put(key, data);
        playerBlocks.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(key);
        saveBlocks();

        playTeleporterEffects(block.getLocation(), teleporterActivateSound);
        sendTeleporterPrefixed(player, msgBlockBound, msgBlockBoundType);
        openGui(player, data, "main");
    }

    private void performTeleport(Player player, BlockData source) {
        if (!usageAllowed(player, "Teleport")) return;

        long left = getCooldownRemaining(player);
        if (left > 0) {
            if (canSendMessage(player)) {
                String msg = cooldownLocale.replace("%time%", String.valueOf(left / 1000 + 1));
                sendTeleporterMessage(player, msg, cooldownMessageType);
            }
            return;
        }

        int id = source.id;
        if (id <= 0) {
            debug("No ID");
            return;
        }
        BlockData destination = null;
        if (source.redirectEnabled && !source.redirectChain.isEmpty()) {
            String key = source.key();
            int idx = redirectIndex.getOrDefault(key, 0);
            if (idx >= source.redirectChain.size()) idx = 0;
            int targetId = source.redirectChain.get(idx);
            Set<String> candidates = idIndex.get(targetId);
            if (candidates != null && !candidates.isEmpty()) {
                for (String k : candidates) {
                    destination = blockDataMap.get(k);
                    if (destination != null) break;
                }
            }
            redirectIndex.put(key, idx + 1);
        } else {
            Set<String> keys = idIndex.get(id);
            if (keys == null || keys.size() < 2) {
                sendTeleporterPrefixedThrottled(player, msgNoPair, msgNoPairType);
                return;
            }
            for (String k : keys) {
                if (!k.equals(source.key())) {
                    destination = blockDataMap.get(k);
                    break;
                }
            }
        }
        if (destination == null) {
            sendTeleporterPrefixedThrottled(player, msgNoPair, msgNoPairType);
            return;
        }
        World destWorld = Bukkit.getWorld(destination.world);
        if (destWorld == null) {
            debug("Dest world missing");
            return;
        }
        if (restrictionEnabled("CrossWorld") && !restrictionExempt(player, "CrossWorld")) {
            boolean allow = getConfig().getBoolean("Restrictions.CrossWorld.Allow", true);
            if (!allow && !player.getWorld().getName().equals(destination.world)) {
                sendTeleporterPrefixedThrottled(player, msgNoCrossWorld, msgNoCrossWorldType);
                return;
            }
        }
        if (restrictionEnabled("Distance") && !restrictionExempt(player, "Distance")) {
            Location a = new Location(player.getWorld(), source.x, source.y, source.z);
            Location b = new Location(destWorld, destination.x, destination.y, destination.z);
            if (a.getWorld() != null && a.getWorld().equals(b.getWorld())) {
                double dist = a.distance(b);
                if (dist > maxDistance) {
                    sendTeleporterPrefixedThrottled(player, msgDistanceTooFar, msgDistanceTooFarType);
                    return;
                }
            }
        }
        Location destLoc;
        if (destination.teleportLocation == TeleportLocation.CURRENT && !destination.customDestinationWorld.isEmpty()) {
            World w = Bukkit.getWorld(destination.customDestinationWorld);
            if (w == null) w = destWorld;
            destLoc = new Location(w, destination.customDestinationX, destination.customDestinationY, destination.customDestinationZ, destination.customDestinationYaw, destination.customDestinationPitch);
        } else {
            destLoc = new Location(destWorld, destination.x + 0.5, destination.y + 1, destination.z + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch());
        }
        if (!isSafeLocation(destLoc, true)) {
            sendTeleporterPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
            return;
        }
        Location sourceLoc = player.getLocation().clone();
        playTeleporterEffects(sourceLoc, teleporterUsageSound);
        playTeleporterEffects(destLoc, teleporterUsageSound);
        player.teleport(destLoc);
        playTeleporterEffects(destLoc, teleporterUsageSound);
        sendTeleporterPrefixedThrottled(player, msgTeleportSuccess, msgTeleportSuccessType);
        setCooldown(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        BlockData data = blockDataMap.get(key);
        if (data == null) return;
        if (restrictionEnabled("Access") && !restrictionExempt(player, "Access")) {
            if (!canBreak(player, data)) {
                event.setCancelled(true);
                debug("No break rights");
                return;
            }
        }
        removeBlock(data);
    }

    private void removeBlock(BlockData data) {
        String key = data.key();
        removeHologram(data);
        blockDataMap.remove(key);
        Set<String> keys = idIndex.get(data.id);
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) idIndex.remove(data.id);
        }
        List<String> list = playerBlocks.get(data.owner);
        if (list != null) {
            list.remove(key);
            if (list.isEmpty()) playerBlocks.remove(data.owner);
        }
        redirectIndex.remove(key);
        for (BlockData other : blockDataMap.values()) {
            if (other.redirectChain != null && other.redirectChain.contains(data.id)) {
                other.redirectChain.remove(Integer.valueOf(data.id));
            }
        }
        saveBlocks();
    }

    private boolean canTeleport(Player player, BlockData data) {
        if (restrictionEnabled("Access") && restrictionExempt(player, "Access")) return true;
        AccessLevel level = data.teleportAccess;
        if (level == AccessLevel.ALL) return true;
        if (level == AccessLevel.OWNER) return player.getUniqueId().equals(data.owner);
        if (level == AccessLevel.MEMBERS) {
            if (player.getUniqueId().equals(data.owner)) return true;
            if (data.members.contains(player.getUniqueId())) return true;
            Set<UUID> global = globalMembers.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        if (level == AccessLevel.GOVERNINGS) {
            if (player.getUniqueId().equals(data.owner)) return true;
            if (data.governings.contains(player.getUniqueId())) return true;
            Set<UUID> global = globalGovernings.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        return false;
    }

    private boolean canManage(Player player, BlockData data) {
        if (restrictionEnabled("Access") && restrictionExempt(player, "Access")) return true;
        AccessLevel level = data.manageAccess;
        if (level == AccessLevel.ALL) return true;
        if (level == AccessLevel.OWNER) return player.getUniqueId().equals(data.owner);
        if (level == AccessLevel.MEMBERS) {
            if (player.getUniqueId().equals(data.owner)) return true;
            if (data.members.contains(player.getUniqueId())) return true;
            Set<UUID> global = globalMembers.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        if (level == AccessLevel.GOVERNINGS) {
            if (player.getUniqueId().equals(data.owner)) return true;
            if (data.governings.contains(player.getUniqueId())) return true;
            Set<UUID> global = globalGovernings.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        return false;
    }

    private boolean canBreak(Player player, BlockData data) {
        if (restrictionEnabled("Access") && restrictionExempt(player, "Access")) return true;
        AccessLevel level = data.breakAccess;
        if (level == AccessLevel.ALL) return true;
        if (level == AccessLevel.OWNER) return player.getUniqueId().equals(data.owner);
        if (level == AccessLevel.MEMBERS) {
            if (player.getUniqueId().equals(data.owner)) return true;
            if (data.members.contains(player.getUniqueId())) return true;
            Set<UUID> global = globalMembers.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        if (level == AccessLevel.GOVERNINGS) {
            if (player.getUniqueId().equals(data.owner)) return true;
            if (data.governings.contains(player.getUniqueId())) return true;
            Set<UUID> global = globalGovernings.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        return false;
    }

    private void openGui(Player player, BlockData data, String menuName) {
        openGui(player, data, menuName, null);
    }

    private void openGui(Player player, BlockData data, String menuName, String listTypeOverride) {
        ConfigurationSection menu = getConfig().getConfigurationSection("GUI.Menus." + menuName);
        if (menu == null) {
            debug("Menu not found: " + menuName);
            return;
        }

        UUID uuid = player.getUniqueId();

        guiItemMetaMap.remove(uuid);
        Map<Integer, GuiItemMeta> slotMap = new HashMap<>();
        guiItemMetaMap.put(uuid, slotMap);

        String title = menu.getString("title", "&8Menu");
        int size = menu.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) size = 54;

        String listType = listTypeOverride;
        if (listType == null && guiContexts.containsKey(uuid)) {
            GuiContext old = guiContexts.get(uuid);
            if (old.menuName.equals(menuName)) listType = old.listType;
        }

        int page = 0;
        if (guiContexts.containsKey(uuid)) {
            GuiContext old = guiContexts.get(uuid);
            if (old.menuName.equals(menuName)) page = old.page;
        }

        Inventory inv = Bukkit.createInventory(null, size, color(replacePlaceholders(title, player, data, listType, null, 0, page)));

        ConfigurationSection items = menu.getConfigurationSection("items");
        if (items != null) {
            for (String itemKey : items.getKeys(false)) {
                ConfigurationSection itemSec = items.getConfigurationSection(itemKey);
                if (itemSec == null) continue;
                if (!checkViewRequirement(player, itemSec)) continue;

                String featureKey = guiItemFeatureKey(itemKey);
                if (featureKey != null && !featureAllowed(player, featureKey)) continue;

                String materialName = itemSec.getString("material", "STONE");
                Material mat;
                try {
                    mat = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    mat = Material.STONE;
                }
                int slot = itemSec.getInt("slot", -1);
                if (slot < 0 || slot >= size) continue;
                String display = replacePlaceholders(itemSec.getString("display_name", ""), player, data, listType, null, 0, page);
                List<String> loreList = itemSec.getStringList("lore");
                List<String> loreOut = new ArrayList<>();
                for (String l : loreList) loreOut.add(replacePlaceholders(l, player, data, listType, null, 0, page));
                boolean hideAttr = itemSec.getBoolean("hide_attributes", false);
                ItemStack stack = createItem(mat, display, loreOut, hideAttr);
                inv.setItem(slot, stack);
                GuiItemMeta meta = new GuiItemMeta();
                meta.itemKey = itemKey;
                meta.menuName = menuName;
                meta.leftCommands = itemSec.getStringList("left_click_commands");
                meta.rightCommands = itemSec.getStringList("right_click_commands");
                meta.shiftLeftCommands = itemSec.getStringList("shift_left_click_commands");
                meta.shiftRightCommands = itemSec.getStringList("shift_right_click_commands");
                slotMap.put(slot, meta);
            }
        }

        ConfigurationSection dyn = menu.getConfigurationSection("dynamic");
        if (dyn != null) {
            if (menuName.equals("list") && listType != null) {
                Set<UUID> list = getListByType(data, listType);
                ConfigurationSection entry = dyn.getConfigurationSection("player_entry");
                if (entry != null) {
                    String materialName = entry.getString("material", "PLAYER_HEAD");
                    Material mat;
                    try {
                        mat = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        mat = Material.PLAYER_HEAD;
                    }
                    int startSlot = dyn.getInt("slots.start", 0);
                    int endSlot = dyn.getInt("slots.end", size - 10);
                    List<UUID> sorted = new ArrayList<>(list);
                    int perPage = endSlot - startSlot + 1;
                    int totalPages = Math.max(1, (int) Math.ceil(sorted.size() / (double) perPage));
                    if (page >= totalPages) page = totalPages - 1;
                    if (page < 0) page = 0;
                    int from = page * perPage;
                    int to = Math.min(from + perPage, sorted.size());
                    int idx = 0;
                    for (int i = from; i < to; i++) {
                        UUID target = sorted.get(i);
                        int s = startSlot + idx;
                        if (s > endSlot || s >= size) break;
                        String name = Bukkit.getOfflinePlayer(target).getName();
                        if (name == null) name = target.toString();
                        String dn = entry.getString("display_name", "");
                        dn = dn.replace("%name%", name).replace("%uuid%", target.toString());
                        List<String> loreList = entry.getStringList("lore");
                        List<String> loreOut = new ArrayList<>();
                        for (String l : loreList) loreOut.add(replacePlaceholders(l, player, data, listType, name, i, page));
                        boolean hideAttrEntry = entry.getBoolean("hide_attributes", false);
                        inv.setItem(s, createItem(mat, dn, loreOut, hideAttrEntry));
                        GuiItemMeta meta = new GuiItemMeta();
                        meta.itemKey = "player_entry";
                        meta.menuName = menuName;
                        meta.leftCommands = new ArrayList<>();
                        for (String cmd : entry.getStringList("left_click_commands")) {
                            meta.leftCommands.add(cmd.replace("%uuid%", target.toString()).replace("%name%", name));
                        }
                        meta.rightCommands = new ArrayList<>();
                        slotMap.put(s, meta);
                        idx++;
                    }
                }
            } else if (menuName.equals("redirect")) {
                ConfigurationSection entry = dyn.getConfigurationSection("step_entry");
                if (entry != null) {
                    String materialName = entry.getString("material", "PAPER");
                    Material mat;
                    try {
                        mat = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        mat = Material.PAPER;
                    }
                    int startSlot = dyn.getInt("slots.start", 9);
                    int endSlot = dyn.getInt("slots.end", 44);
                    int idx = 0;
                    for (int i = 0; i < data.redirectChain.size(); i++) {
                        int id = data.redirectChain.get(i);
                        int s = startSlot + idx;
                        if (s > endSlot || s >= size) break;
                        String dn = entry.getString("display_name", "")
                                .replace("%index%", String.valueOf(i + 1))
                                .replace("%id%", String.valueOf(id));
                        List<String> loreList = entry.getStringList("lore");
                        List<String> loreOut = new ArrayList<>();
                        for (String l : loreList) loreOut.add(replacePlaceholders(l, player, data, listType, null, i, page));
                        boolean hideAttrStep = entry.getBoolean("hide_attributes", false);
                        inv.setItem(s, createItem(mat, dn, loreOut, hideAttrStep));
                        GuiItemMeta meta = new GuiItemMeta();
                        meta.itemKey = "step_entry";
                        meta.menuName = menuName;
                        meta.leftCommands = new ArrayList<>();
                        for (String cmd : entry.getStringList("left_click_commands")) {
                            meta.leftCommands.add(cmd.replace("%index%", String.valueOf(i)));
                        }
                        meta.rightCommands = new ArrayList<>();
                        slotMap.put(s, meta);
                        idx++;
                    }
                }
            }
        }

        GuiContext ctx = new GuiContext(data.key(), menuName);
        ctx.listType = listType;
        ctx.page = page;
        guiContexts.put(uuid, ctx);
        openGuis.put(uuid, inv);
        player.openInventory(inv);
    }

    private String guiItemFeatureKey(String itemKey) {
        switch (itemKey) {
            case "click_type": return "ClickType";
            case "sneak": return "RequireSneak";
            case "required_item": return "Bind";
            case "location": return "TeleportLocation";
            case "name": return "Naming";
            case "hologram": return "Hologram";
            case "redirect": return "Redirect";
            case "teleport_access": return "TeleportAccess";
            case "manage_access": return "ManageAccess";
            case "members": return "PeopleUse";
            case "break_access": return "BreakAccess";
            case "password": return "Password";
            default: return null;
        }
    }

    private boolean checkViewRequirement(Player player, ConfigurationSection itemSec) {
        ConfigurationSection vr = itemSec.getConfigurationSection("view_requirement");
        if (vr == null) return true;
        String type = vr.getString("type", "");
        if (type.equalsIgnoreCase("holding_item")) {
            String matName = vr.getString("material", "");
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            return hand != null && hand.getType() == mat;
        }
        if (type.equalsIgnoreCase("permission")) {
            String perm = vr.getString("permission", "");
            if (perm.isEmpty()) return true;
            return player.hasPermission(perm);
        }
        return true;
    }

    private String replacePlaceholders(String text, Player player, BlockData data, String listType, String extraName, int index, int page) {
        if (text == null) return "";
        String out = text;
        if (data != null) {
            out = out.replace("%id%", String.valueOf(data.id));
            out = out.replace("%blockname%", data.customName.isEmpty() ? "" : data.customName);
            out = out.replace("%click%", data.clickType.name());
            out = out.replace("%sneak%", data.requireSneak ? "Yes" : "No");
            out = out.replace("%item%", data.requireItem ? "Yes" : "No");
            out = out.replace("%value%", data.requiredItemName.isEmpty() ? "-" : data.requiredItemName);
            out = out.replace("%location%", data.teleportLocation.name());
            out = out.replace("%teleport_access%", data.teleportAccess.name());
            out = out.replace("%manage_access%", data.manageAccess.name());
            out = out.replace("%break_access%", data.breakAccess.name());
            out = out.replace("%password%", data.passwordEnabled ? "Yes" : "No");
            out = out.replace("%redirect%", data.redirectEnabled ? "Yes" : "No");
            out = out.replace("%members%", String.valueOf(data.members.size()));
            out = out.replace("%governings%", String.valueOf(data.governings.size()));
            out = out.replace("%count%", String.valueOf(data.members.size()));
            out = out.replace("%list%", listType == null ? "List" : listType);
            out = out.replace("%index%", String.valueOf(index + 1));
            out = out.replace("%page%", String.valueOf(page + 1));
            out = out.replace("%pages%", "1");
            out = out.replace("%hologram%", data.hologramEnabled ? "Yes" : "No");
            out = out.replace("%name%", data.customName.isEmpty() ? "" : data.customName);
        }
        out = out.replace("%player%", player.getName());
        if (extraName != null) out = out.replace("%entry%", extraName);
        return out;
    }

    private Set<UUID> getListByType(BlockData data, String listType) {
        if (listType == null) return new HashSet<>();
        switch (listType) {
            case "LOCAL_MEMBERS": return data.members;
            case "LOCAL_GOVERNINGS": return data.governings;
            case "GLOBAL_MEMBERS": return globalMembers.computeIfAbsent(data.owner, k -> ConcurrentHashMap.newKeySet());
            case "GLOBAL_GOVERNINGS": return globalGovernings.computeIfAbsent(data.owner, k -> ConcurrentHashMap.newKeySet());
        }
        return new HashSet<>();
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        return createItem(mat, name, lore, false);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore, boolean hideAttributes) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> loreList = new ArrayList<>();
                for (String l : lore) loreList.add(color(l));
                meta.setLore(loreList);
            }
            if (hideAttributes) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        Inventory opened = openGuis.get(uuid);
        if (opened == null) return;
        if (event.getInventory() != opened) return;

        event.setCancelled(true);

        GuiContext ctx = guiContexts.get(uuid);
        if (ctx == null) return;

        BlockData data = blockDataMap.get(ctx.blockKey);
        if (data == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        Map<Integer, GuiItemMeta> slotMap = guiItemMetaMap.get(uuid);
        if (slotMap == null) return;

        GuiItemMeta meta = slotMap.get(slot);
        if (meta == null) return;

        List<String> commands = new ArrayList<>();
        org.bukkit.event.inventory.ClickType click = event.getClick();
        switch (click) {
            case LEFT:
                commands = meta.leftCommands;
                break;
            case RIGHT:
                commands = meta.rightCommands;
                break;
            case SHIFT_LEFT:
                commands = !meta.shiftLeftCommands.isEmpty() ? meta.shiftLeftCommands : meta.leftCommands;
                break;
            case SHIFT_RIGHT:
                commands = !meta.shiftRightCommands.isEmpty() ? meta.shiftRightCommands : meta.rightCommands;
                break;
            default:
                break;
        }

        if (commands == null || commands.isEmpty()) return;
        for (String cmd : commands) {
            handleGuiCommand(player, data, cmd, meta, slot);
        }
    }

    private void handleGuiCommand(Player player, BlockData data, String cmd, GuiItemMeta meta, int slot) {
        if (cmd == null || cmd.isEmpty()) return;
        
        if (cmd.equals("[close]")) {
            player.closeInventory();
            return;
        }
        
        String[] prefixes = {
                "[opengui] ",
                "[opengui_list] ",
                "[teleporter_page] ",
                "[teleporter_input] ",
                "[teleporter_toggle] ",
                "[teleporter_list_remove] ",
                "[teleporter_redirect_remove] "
        };
        
        String matched = null;
        for (String p : prefixes) {
            if (cmd.startsWith(p)) {
                matched = p;
                break;
            }
        }
        
        if (matched == null) return;
        String arg = cmd.substring(matched.length()).trim();

        if (matched.equals("[opengui] ")) {
            openGui(player, data, arg);
            return;
        }
        
        if (matched.equals("[opengui_list] ")) {
            openGui(player, data, "list", arg);
            return;
        }
        
        if (matched.equals("[teleporter_page] ")) {
            GuiContext ctx = guiContexts.get(player.getUniqueId());
            int page = ctx != null ? ctx.page : 0;
            if (arg.equals("next")) page++;
            else if (arg.equals("previous")) page = Math.max(0, page - 1);
            String listType = ctx != null ? ctx.listType : null;
            BlockData fresh = blockDataMap.get(data.key());
            if (fresh != null) {
                GuiContext tmp = new GuiContext(fresh.key(), "list");
                tmp.listType = listType;
                tmp.page = page;
                guiContexts.put(player.getUniqueId(), tmp);
                openGuiWithContext(player, fresh, "list", listType, page);
            }
            return;
        }
        
        if (matched.equals("[teleporter_input] ")) {
            GuiContext ctx = guiContexts.get(player.getUniqueId());
            String listType = ctx != null ? ctx.listType : null;
            player.closeInventory();
            if (arg.equals("SET_ID")) {
                sendTeleporterPrefixed(player, msgIdPrompt, msgIdPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_ID", data.key()));
            } else if (arg.equals("SET_ITEM")) {
                if (!featureAllowed(player, "Bind")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgItemPrompt, msgItemPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_ITEM", data.key()));
            } else if (arg.equals("SET_NAME")) {
                if (!featureAllowed(player, "Naming")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgNamePrompt, msgNamePromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_NAME", data.key()));
            } else if (arg.equals("SET_PASSWORD")) {
                if (!featureAllowed(player, "Password")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgPasswordPrompt, msgPasswordPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_PASSWORD", data.key()));
            } else if (arg.equals("ADD_STEP")) {
                if (!featureAllowed(player, "Redirect")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgRedirectPrompt, msgRedirectPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("ADD_STEP", data.key()));
            } else if (arg.equals("CLEAR_STEPS")) {
                if (!featureAllowed(player, "Redirect")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                data.redirectChain.clear();
                saveBlocks();
                openGui(player, data, "redirect");
            } else if (arg.equals("ADD_PLAYER")) {
                if (!featureAllowed(player, "PeopleUse")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgListPrompt, msgListPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("ADD_PLAYER", data.key(), listType));
            }
            return;
        }
        
        if (matched.equals("[teleporter_toggle] ")) {
            String featureKey = toggleFeatureKey(arg);
            if (featureKey != null && !featureAllowed(player, featureKey)) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            switch (arg) {
                case "click":
                    data.clickType = data.clickType == BindClickType.LEFT ? BindClickType.RIGHT : BindClickType.LEFT;
                    break;
                case "sneak":
                    data.requireSneak = !data.requireSneak;
                    break;
                case "location":
                    data.teleportLocation = data.teleportLocation == TeleportLocation.TOP ? TeleportLocation.CURRENT : TeleportLocation.TOP;
                    if (data.teleportLocation == TeleportLocation.CURRENT) {
                        Location loc = player.getLocation();
                        Block block = player.getWorld().getBlockAt(data.x, data.y, data.z);
                        if (loc.getWorld() != null && loc.getWorld().equals(block.getWorld()) && loc.distance(block.getLocation()) <= 5.0) {
                            data.customDestinationWorld = loc.getWorld().getName();
                            data.customDestinationX = loc.getX();
                            data.customDestinationY = loc.getY();
                            data.customDestinationZ = loc.getZ();
                            data.customDestinationYaw = loc.getYaw();
                            data.customDestinationPitch = loc.getPitch();
                        } else {
                            data.customDestinationWorld = "";
                        }
                    }
                    break;
                case "hologram":
                    data.hologramEnabled = !data.hologramEnabled;
                    if (data.hologramEnabled) updateHologram(data);
                    else removeHologram(data);
                    break;
                case "teleport_access":
                    data.teleportAccess = cycleTeleportAccess(data.teleportAccess);
                    break;
                case "manage_access":
                    data.manageAccess = cycleManageAccess(data.manageAccess);
                    break;
                case "break_access":
                    data.breakAccess = cycleBreakAccess(data.breakAccess);
                    break;
                case "redirect":
                    data.redirectEnabled = !data.redirectEnabled;
                    break;
            }
            saveBlocks();
            openGui(player, data, meta.menuName);
            return;
        }
        
        if (matched.equals("[teleporter_list_remove] ")) {
            if (!featureAllowed(player, "PeopleUse")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            UUID target;
            try {
                target = UUID.fromString(arg);
            } catch (IllegalArgumentException e) {
                return;
            }
            GuiContext ctx = guiContexts.get(player.getUniqueId());
            if (ctx != null && ctx.listType != null) {
                Set<UUID> list = getListByType(data, ctx.listType);
                list.remove(target);
                saveBlocks();
            }
            String listType = ctx != null ? ctx.listType : null;
            int page = ctx != null ? ctx.page : 0;
            openGuiWithContext(player, data, "list", listType, page);
            return;
        }
        if (matched.equals("[teleporter_redirect_remove] ")) {
            if (!featureAllowed(player, "Redirect")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            int idx;
            try {
                idx = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                return;
            }
            if (idx >= 0 && idx < data.redirectChain.size()) {
                int removed = data.redirectChain.remove(idx);
                saveBlocks();
                player.sendMessage(color(prefix + msgRedirectRemoved.replace("%id%", String.valueOf(removed))));
            }
            openGui(player, data, "redirect");
            return;
        }
    }

    private String toggleFeatureKey(String arg) {
        switch (arg) {
            case "click": return "ClickType";
            case "sneak": return "RequireSneak";
            case "location": return "TeleportLocation";
            case "hologram": return "Hologram";
            case "teleport_access": return "TeleportAccess";
            case "manage_access": return "ManageAccess";
            case "break_access": return "BreakAccess";
            case "redirect": return "Redirect";
            default: return null;
        }
    }

    private void openGuiWithContext(Player player, BlockData data, String menuName, String listType, int page) {
        GuiContext ctx = new GuiContext(data.key(), menuName);
        ctx.listType = listType;
        ctx.page = page;
        guiContexts.put(player.getUniqueId(), ctx);
        openGui(player, data, menuName, listType);
    }

    private AccessLevel cycleTeleportAccess(AccessLevel c) {
        AccessLevel[] v = {AccessLevel.OWNER, AccessLevel.MEMBERS, AccessLevel.GOVERNINGS, AccessLevel.ALL};
        return v[(Arrays.asList(v).indexOf(c) + 1) % v.length];
    }

    private AccessLevel cycleManageAccess(AccessLevel c) {
        AccessLevel[] v = {AccessLevel.OWNER, AccessLevel.GOVERNINGS, AccessLevel.ALL};
        return v[(Arrays.asList(v).indexOf(c) + 1) % v.length];
    }

    private AccessLevel cycleBreakAccess(AccessLevel c) {
        AccessLevel[] v = {AccessLevel.OWNER, AccessLevel.MEMBERS, AccessLevel.GOVERNINGS, AccessLevel.ALL};
        return v[(Arrays.asList(v).indexOf(c) + 1) % v.length];
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (openGuis.get(uuid) == event.getInventory()) {
            openGuis.remove(uuid);
            guiContexts.remove(uuid);
            guiItemMetaMap.remove(uuid);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingPassword pending = pendingPasswords.get(player.getUniqueId());
        if (pending != null) {
            if (System.currentTimeMillis() > pending.expireTime) {
                pendingPasswords.remove(player.getUniqueId());
                return;
            }
            event.setCancelled(true);
            String message = event.getMessage().trim();
            pendingPasswords.remove(player.getUniqueId());
            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(color(prefix + msgCancelled));
                return;
            }
            BlockData data = blockDataMap.get(pending.blockKey);
            if (data == null) {
                player.sendMessage(color(prefix + msgBlockGone));
                return;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!data.passwordEnabled) {
                        performTeleport(player, data);
                        return;
                    }
                    if (data.password.equalsIgnoreCase(message)) {
                        sendTeleporterPrefixedThrottled(player, msgPasswordOk, msgPasswordOkType);
                        performTeleport(player, data);
                    } else {
                        sendTeleporterPrefixedThrottled(player, msgPasswordWrong, msgPasswordWrongType);
                    }
                }
            }.runTask(this);
            return;
        }
        ChatInputSession session = chatSessions.get(player.getUniqueId());
        if (session == null) return;
        if (System.currentTimeMillis() > session.expireTime) {
            chatSessions.remove(player.getUniqueId());
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        chatSessions.remove(player.getUniqueId());
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(color(prefix + msgCancelled));
            return;
        }
        BlockData data = blockDataMap.get(session.blockKey);
        if (data == null) {
            player.sendMessage(color(prefix + msgBlockGone));
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                handleChatInput(player, data, session, message);
            }
        }.runTask(this);
    }

    private void handleChatInput(Player player, BlockData data, ChatInputSession session, String message) {
        switch (session.type) {
            case "SET_ID": {
                int newId;
                try {
                    newId = Integer.parseInt(message);
                } catch (NumberFormatException e) {
                    sendTeleporterPrefixedThrottled(player, msgIdInvalid, msgIdInvalidType);
                    return;
                }
                if (newId <= 0 || newId > 99999) {
                    sendTeleporterPrefixedThrottled(player, msgIdRange, msgIdRangeType);
                    return;
                }
                Set<String> existing = idIndex.get(newId);
                if (existing != null && !existing.isEmpty() && !(existing.size() == 1 && existing.contains(data.key()))) {
                    if (existing.size() >= 2) {
                        sendTeleporterPrefixedThrottled(player, msgIdUsed, msgIdUsedType);
                        return;
                    }
                }
                Set<String> oldKeys = idIndex.get(data.id);
                if (oldKeys != null) {
                    oldKeys.remove(data.key());
                    if (oldKeys.isEmpty()) idIndex.remove(data.id);
                }
                data.id = newId;
                idIndex.computeIfAbsent(newId, k -> ConcurrentHashMap.newKeySet()).add(data.key());
                saveBlocks();
                sendTeleporterPrefixed(player, msgIdSet.replace("%id%", String.valueOf(newId)), msgIdSetType);
                openGui(player, data, "main");
                return;
            }
            case "SET_ITEM": {
                if (!featureAllowed(player, "Bind")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                if (message.equalsIgnoreCase("no") || message.equalsIgnoreCase("not")) {
                    data.requireItem = false;
                    data.requiredItemName = "";
                    saveBlocks();
                    sendTeleporterPrefixed(player, msgItemDisabled, msgItemDisabledType);
                    openGui(player, data, "main");
                    return;
                }
                data.requireItem = true;
                data.requiredItemName = message;
                saveBlocks();
                sendTeleporterPrefixed(player, msgItemSet.replace("%name%", message), msgItemSetType);
                openGui(player, data, "main");
                return;
            }
            case "SET_NAME": {
                if (!featureAllowed(player, "Naming")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                if (message.equalsIgnoreCase("no") || message.equalsIgnoreCase("not")) {
                    data.customName = "";
                    saveBlocks();
                    removeHologram(data);
                    sendTeleporterPrefixed(player, msgNameSet.replace("%name%", "-"), msgNameSetType);
                    openGui(player, data, "main");
                    return;
                }
                String name = message.length() > blockNamingMaxLength ? message.substring(0, blockNamingMaxLength) : message;
                if (!name.startsWith("&") && !name.startsWith("&#") && !name.startsWith("<#") && !name.startsWith("§")) {
                    name = blockNamingDefaultColor + name;
                }
                data.customName = name;
                saveBlocks();
                if (data.hologramEnabled) updateHologram(data);
                sendTeleporterPrefixed(player, msgNameSet.replace("%name%", color(name)), msgNameSetType);
                openGui(player, data, "main");
                return;
            }
            case "SET_PASSWORD": {
                if (!featureAllowed(player, "Password")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                if (message.equalsIgnoreCase("no") || message.equalsIgnoreCase("not") || message.equalsIgnoreCase("off")) {
                    data.passwordEnabled = false;
                    data.password = "";
                    saveBlocks();
                    sendTeleporterPrefixed(player, msgPasswordDisabled, msgPasswordDisabledType);
                    openGui(player, data, "main");
                    return;
                }
                data.passwordEnabled = true;
                data.password = message;
                saveBlocks();
                sendTeleporterPrefixed(player, msgPasswordSet, msgPasswordSetType);
                openGui(player, data, "main");
                return;
            }
            case "ADD_STEP": {
                if (!featureAllowed(player, "Redirect")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                int newId;
                try {
                    newId = Integer.parseInt(message);
                } catch (NumberFormatException e) {
                    sendTeleporterPrefixedThrottled(player, msgIdInvalid, msgIdInvalidType);
                    return;
                }
                if (newId <= 0 || newId > 99999) {
                    sendTeleporterPrefixedThrottled(player, msgIdRange, msgIdRangeType);
                    return;
                }
                data.redirectChain.add(newId);
                saveBlocks();
                sendTeleporterPrefixed(player, msgRedirectAdded.replace("%id%", String.valueOf(newId)), msgRedirectAddedType);
                openGui(player, data, "redirect");
                return;
            }
            case "ADD_PLAYER": {
                if (!featureAllowed(player, "PeopleUse")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                Player target = Bukkit.getPlayerExact(message);
                if (target == null) {
                    sendTeleporterPrefixedThrottled(player, msgPlayerNotFound, msgPlayerNotFoundType);
                    return;
                }
                if (session.listType == null) {
                    sendTeleporterPrefixedThrottled(player, msgCancelled, msgCancelledType);
                    return;
                }
                Set<UUID> list = getListByType(data, session.listType);
                list.add(target.getUniqueId());
                saveBlocks();
                sendTeleporterPrefixed(player, msgPlayerAdded.replace("%name%", target.getName()), msgPlayerAddedType);
                openGuiWithContext(player, data, "list", session.listType, 0);
                return;
            }
            default:
        }
    }

    private void startHologramUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (BlockData data : blockDataMap.values()) {
                    if (data.hologramEnabled) updateHologram(data);
                }
            }
        }.runTaskTimer(this, 20L, Math.max(1, hologramsUpdateInterval));
    }

    private void updateHologram(BlockData data) {
        if (!data.hologramEnabled) {
            removeHologram(data);
            return;
        }
        if (data.customName == null || data.customName.isEmpty()) {
            removeHologram(data);
            return;
        }
        World w = Bukkit.getWorld(data.world);
        if (w == null) return;
        Location loc = new Location(w, data.x + 0.5, data.y + 1.2 + hologramsLineHeight, data.z + 0.5);
        UUID existing = hologramEntities.get(data.key());
        ArmorStand stand = null;
        if (existing != null) {
            Entity e = Bukkit.getEntity(existing);
            if (e instanceof ArmorStand && e.isValid()) stand = (ArmorStand) e;
            else hologramEntities.remove(data.key());
        }
        if (stand == null) {
            stand = w.spawn(loc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.setSmall(true);
                as.setCustomNameVisible(true);
                as.setInvulnerable(true);
                as.setBasePlate(false);
                as.setArms(false);
            });
            hologramEntities.put(data.key(), stand.getUniqueId());
        } else {
            stand.teleport(loc);
        }
        stand.setCustomName(color(data.customName));
    }

    private void removeHologram(BlockData data) {
        UUID id = hologramEntities.remove(data.key());
        if (id != null) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
    }

    private void startViewSession(Player player, String mode) {
        if (!commandAllowed(player, "View")) {
            sendTeleporterPrefixedThrottled(player, msgViewDisabled, msgViewDisabledType);
            return;
        }
        if (!viewModeAllowed(player, mode)) {
            sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
            return;
        }
        stopViewSession(player);
        long endTime = System.currentTimeMillis() + (viewDurationTicks * 50L);
        final int[] counter = {0};
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    viewSessions.remove(player.getUniqueId());
                    return;
                }
                if (System.currentTimeMillis() >= endTime) {
                    cancel();
                    viewSessions.remove(player.getUniqueId());
                    sendTeleporterPrefixedThrottled(player, msgViewDisabled, msgViewDisabledType);
                    return;
                }
                counter[0]++;
                if (counter[0] % Math.max(1, viewUpdateTicks) != 0) return;
                for (BlockData data : blockDataMap.values()) {
                    if (!matchesViewMode(player, data, mode)) continue;
                    World w = Bukkit.getWorld(data.world);
                    if (w == null) continue;
                    if (!w.getName().equals(player.getWorld().getName())) continue;
                    Location loc = new Location(w, data.x + 0.5, data.y + 1.2, data.z + 0.5);
                    if (loc.distanceSquared(player.getLocation()) > (double) viewMaxDistance * viewMaxDistance) continue;
                    try {
                        player.spawnParticle(viewParticleType, loc, viewParticleCount, 0.2, 0.2, 0.2, 0.01);
                        player.spawnParticle(teleporterParticleType, loc, 2, 0.3, 0.3, 0.3, 0.01);
                    } catch (Exception ignored) {}
                }
            }
        };
        int taskId = runnable.runTaskTimer(this, 0L, 1L).getTaskId();
        viewSessions.put(player.getUniqueId(), new ViewSession(mode, endTime, taskId));
        sendTeleporterPrefixed(player, msgViewEnabled.replace("%mode%", mode), msgViewEnabledType);
    }

    private void stopViewSession(Player player) {
        ViewSession session = viewSessions.remove(player.getUniqueId());
        if (session != null) Bukkit.getScheduler().cancelTask(session.taskId);
    }

    private boolean matchesViewMode(Player player, BlockData data, String mode) {
        UUID uuid = player.getUniqueId();
        if (mode.equalsIgnoreCase("all")) {
            return true;
        }
        if (mode.equalsIgnoreCase("owner")) {
            return data.owner.equals(uuid);
        }
        if (mode.equalsIgnoreCase("member")) {
            if (data.owner.equals(uuid)) return true;
            if (data.members.contains(uuid)) return true;
            if (data.governings.contains(uuid)) return true;
            Set<UUID> gm = globalMembers.get(data.owner);
            if (gm != null && gm.contains(uuid)) return true;
            Set<UUID> go = globalGovernings.get(data.owner);
            if (go != null && go.contains(uuid)) return true;
            return false;
        }
        return false;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        chatSessions.remove(uuid);
        openGuis.remove(uuid);
        guiContexts.remove(uuid);
        guiItemMetaMap.remove(uuid);
        pendingPearlCancel.remove(uuid);
        pendingPasswords.remove(uuid);
        messageCooldown.remove(uuid);
        stopViewSession(event.getPlayer());
    }

    private Block getTargetBlock(Player player, int distance) {
        try {
            return player.getTargetBlockExact(distance);
        } catch (NoSuchMethodError e) {
            @SuppressWarnings("deprecation")
            Block b = player.getTargetBlock((Set<Material>) null, distance);
            return b;
        }
    }

    private List<BlockData> resolveBlockDataList(Player player, String[] args, int argIndex) {
        List<BlockData> result = new ArrayList<>();
        if (args.length > argIndex) {
            int id;
            try {
                id = Integer.parseInt(args[argIndex]);
            } catch (NumberFormatException e) {
                sendTeleporterPrefixedThrottled(player, msgIdInvalid, msgIdInvalidType);
                return result;
            }
            Set<String> keys = idIndex.get(id);
            if (keys == null || keys.isEmpty()) {
                sendTeleporterPrefixedThrottled(player, msgRemoveNone, msgRemoveNoneType);
                return result;
            }
            for (String k : keys) {
                BlockData d = blockDataMap.get(k);
                if (d != null) result.add(d);
            }
            return result;
        }
        Block block = getTargetBlock(player, 10);
        if (block == null) {
            sendTeleporterPrefixedThrottled(player, msgNoBlockInSight, msgNoBlockInSightType);
            return result;
        }
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        BlockData data = blockDataMap.get(key);
        if (data == null) {
            sendTeleporterPrefixedThrottled(player, msgNotTeleporter, msgNotTeleporterType);
            return result;
        }
        result.add(data);
        return result;
    }

    private void sendBlockInfo(Player player, BlockData data) {
        player.sendMessage(color(prefix + msgHeaderInfo));
        player.sendMessage(color("&#FF5300ID: &f" + data.id));
        player.sendMessage(color("&#FF5300Name: &f" + (data.customName.isEmpty() ? "-" : color(data.customName))));
        player.sendMessage(color("&#FF5300Owner: &f" + Bukkit.getOfflinePlayer(data.owner).getName()));
        player.sendMessage(color("&#FF5300Click: &f" + data.clickType.name()));
        player.sendMessage(color("&#FF5300Require sneak: &f" + data.requireSneak));
        player.sendMessage(color("&#FF5300Require item: &f" + data.requireItem + " (" + data.requiredItemName + ")"));
        player.sendMessage(color("&#FF5300Location: &f" + data.teleportLocation.name()));
        player.sendMessage(color("&#FF5300Teleport access: &f" + data.teleportAccess.name()));
        player.sendMessage(color("&#FF5300Manage access: &f" + data.manageAccess.name()));
        player.sendMessage(color("&#FF5300Break access: &f" + data.breakAccess.name()));
        player.sendMessage(color("&#FF5300Hologram: &f" + (data.hologramEnabled ? "Yes" : "No")));
        player.sendMessage(color("&#FF5300Password: &f" + (data.passwordEnabled ? "Yes" : "No")));
        player.sendMessage(color("&#FF5300Redirect: &f" + (data.redirectEnabled ? "Yes" : "No")
                + " (" + data.redirectChain.size() + " steps)"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("teleporter")) return false;
        if (args.length == 0) {
            sender.sendMessage(color("&#FF5300Teleporter &7| &fCommands:"));
            sender.sendMessage(color("&#FF5300/teleporter reload"));
            sender.sendMessage(color("&#FF5300/teleporter info [id]"));
            sender.sendMessage(color("&#FF5300/teleporter list"));
            sender.sendMessage(color("&#FF5300/teleporter remove [id]"));
            sender.sendMessage(color("&#FF5300/teleporter view <all|owner|member>"));
            sender.sendMessage(color("&#FF5300/teleporter menu [id]"));
            sender.sendMessage(color("&#FF5300/teleporter tp [id]"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("reload")) {
            if (!commandAllowed(sender, "Reload")) {
                sender.sendMessage(color(prefix + msgNoPermission));
                return true;
            }
            loadConfig();
            loadBlocks();
            sender.sendMessage(color(prefix + msgReloaded));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(color(prefix + msgPlayersOnly));
            return true;
        }
        Player player = (Player) sender;

        if (sub.equals("info")) {
            if (!commandAllowed(player, "Info")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) return true;
            for (BlockData data : list) {
                sendBlockInfo(player, data);
            }
            return true;
        }

        if (sub.equals("list")) {
            if (!commandAllowed(player, "List")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<String> keys = playerBlocks.get(player.getUniqueId());
            if (keys == null || keys.isEmpty()) {
                sendTeleporterPrefixedThrottled(player, msgNoBlocksOwned, msgNoBlocksOwnedType);
                return true;
            }
            sendTeleporterPrefixed(player, msgYourBlocks, msgYourBlocksType);
            for (String k : keys) {
                BlockData data = blockDataMap.get(k);
                if (data == null) continue;
                player.sendMessage(color("&#FF5300ID " + data.id + " &7-> &f" + k
                        + (data.customName.isEmpty() ? "" : " &7(" + color(data.customName) + "&7)")));
            }
            return true;
        }

        if (sub.equals("remove")) {
            if (!commandAllowed(player, "Remove")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) return true;
            List<BlockData> toRemove = new ArrayList<>();
            boolean bypass = restrictionEnabled("Access") && restrictionExempt(player, "Access");
            for (BlockData data : list) {
                if (data.owner.equals(player.getUniqueId()) || bypass) {
                    toRemove.add(data);
                }
            }
            if (toRemove.isEmpty()) {
                sendTeleporterPrefixedThrottled(player, msgRemoveNotOwner, msgRemoveNotOwnerType);
                return true;
            }
            int id = toRemove.get(0).id;
            for (BlockData data : toRemove) removeBlock(data);
            sendTeleporterPrefixed(player, msgRemoveSuccess
                    .replace("%count%", String.valueOf(toRemove.size()))
                    .replace("%id%", String.valueOf(id)), msgRemoveSuccessType);
            return true;
        }

        if (sub.equals("view")) {
            if (args.length < 2) {
                stopViewSession(player);
                sendTeleporterPrefixedThrottled(player, msgViewDisabled, msgViewDisabledType);
                return true;
            }
            String mode = args[1].toLowerCase(Locale.ROOT);
            if (!mode.equals("all") && !mode.equals("owner") && !mode.equals("member")) {
                sendTeleporterPrefixedThrottled(player, msgViewUsage, msgViewUsageType);
                return true;
            }
            startViewSession(player, mode);
            return true;
        }

        if (sub.equals("menu")) {
            if (!commandAllowed(player, "Menu")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) return true;
            BlockData target = null;
            for (BlockData d : list) {
                if (canManage(player, d)) { target = d; break; }
            }
            if (target == null) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            openGui(player, target, "main");
            return true;
        }

        if (sub.equals("tp")) {
            if (!commandAllowed(player, "Tp")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) return true;
            if (!usageAllowed(player, "Teleport")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            BlockData target = null;
            for (BlockData d : list) {
                if (canTeleport(player, d)) { target = d; break; }
            }
            if (target == null) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            World w = Bukkit.getWorld(target.world);
            if (w == null) {
                sendTeleporterPrefixedThrottled(player, msgBlockGone, msgBlockGoneType);
                return true;
            }
            Location loc = new Location(w,
                    target.x + 0.5,
                    target.y + 1,
                    target.z + 0.5,
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch());
            player.teleport(loc);
            playTeleporterEffects(loc, teleporterUsageSound);
            sendTeleporterPrefixedThrottled(player, msgTeleportSuccess, msgTeleportSuccessType);
            return true;
        }

        sendTeleporterPrefixedThrottled(player, msgUnknownSub, msgUnknownSubType);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("teleporter")) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("reload", "info", "list", "remove", "view", "menu", "tp")) {
                if (s.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    result.add(s);
                }
            }
            return result;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("view")) {
                for (String s : Arrays.asList("all", "owner", "member")) {
                    if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) result.add(s);
                }
            } else if ((sub.equals("remove") || sub.equals("info") || sub.equals("menu") || sub.equals("tp")) && sender instanceof Player) {
                List<String> keys = playerBlocks.get(((Player) sender).getUniqueId());
                if (keys != null) {
                    Set<Integer> ids = new HashSet<>();
                    for (String k : keys) {
                        BlockData d = blockDataMap.get(k);
                        if (d != null) ids.add(d.id);
                    }
                    for (int id : ids) {
                        String s = String.valueOf(id);
                        if (s.startsWith(args[1])) result.add(s);
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
