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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main extends JavaPlugin implements Listener, TabCompleter {

    private static Main instance;

    public enum MessageType {
        CHAT,
        TITLE,
        SUBTITLE,
        NONE
    }

    public enum AccessLevel {
        OWNER,
        MEMBERS,
        GOVERNINGS,
        ALL
    }

    public enum BindClickType {
        LEFT,
        RIGHT
    }

    public enum TeleportLocation {
        TOP,
        CURRENT
    }

    private static final int DOUBLE_CLICK_DELAY_TICKS = 5;
    private static final int SNEAK_CHECK_DELAY_TICKS = 1;
    private static final int DEFAULT_END_ROD_COUNT = 5;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern SRGB_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    private static Method cachedGetTargetBlockExact = null;
    private static boolean getTargetBlockExactResolved = false;

    private static Method cachedTeleportAsync = null;
    private static boolean teleportAsyncResolved = false;

    private static final Map<String, Material> materialCache = new ConcurrentHashMap<>();
    private static final Map<Material, Short> materialDataCache = new ConcurrentHashMap<>();

    private static boolean folia = false;
    private static Method foliaGetRegionScheduler = null;
    private static Method foliaGetGlobalRegionScheduler = null;
    private static Method foliaGetAsyncScheduler = null;
    private static Method foliaEntityGetScheduler = null;
    private static Method foliaEntitySchedulerExecute = null;
    private static Method foliaRegionSchedulerExecute = null;
    private static Method foliaRegionSchedulerRun = null;
    private static Method foliaRegionSchedulerRunDelayed = null;
    private static Method foliaRegionSchedulerRunAtFixedRate = null;
    private static Method foliaGlobalSchedulerRun = null;
    private static Method foliaGlobalSchedulerRunDelayed = null;
    private static Method foliaGlobalSchedulerRunAtFixedRate = null;

    private static boolean hexSupported = false;
    private static Method sendTitle5 = null;
    private static Method sendTitle2 = null;

    private String prefix;

    private final Set<Material> elevatorBlocks = new HashSet<>();
    private int blockDistance;
    private boolean elevatorEnableParticle;
    private Particle elevatorParticleType;
    private int elevatorParticleCount;
    private String elevatorUsageSound;
    private String elevatorActivateSound;
    private boolean elevatorAllowUnsafe;
    private final Set<String> disabledWorldsElevator = new HashSet<>();

    private boolean teleporterEnableParticle;
    private Particle teleporterParticleType;
    private int teleporterParticleCount;
    private String teleporterUsageSound;
    private String teleporterActivateSound;
    private boolean teleporterAllowUnsafe;
    private final Set<String> disabledWorldsTeleporter = new HashSet<>();

    private boolean teleporterAllowAllBlocks;
    private final Set<Material> teleporterBlockTypes = new HashSet<>();
    private String teleporterBlockTypesPermission;
    private double maxDistance;

    private final Map<Material, BindItem> teleporterBindItems = new LinkedHashMap<>();

    private String cooldownLocale;
    private MessageType cooldownMessageType;

    private int elevatorTitleFadeIn;
    private int elevatorTitleStay;
    private int elevatorTitleFadeOut;
    private int teleporterTitleFadeIn;
    private int teleporterTitleStay;
    private int teleporterTitleFadeOut;

    private String msgIdPrompt;
    private String msgIdInvalid;
    private String msgIdRange;
    private String msgIdUsed;
    private String msgIdSet;
    private MessageType msgIdPromptType;
    private MessageType msgIdInvalidType;
    private MessageType msgIdRangeType;
    private MessageType msgIdUsedType;
    private MessageType msgIdSetType;

    private String msgItemPrompt;
    private String msgItemDisabled;
    private String msgItemSet;
    private MessageType msgItemPromptType;
    private MessageType msgItemDisabledType;
    private MessageType msgItemSetType;

    private String msgNamePrompt;
    private String msgNameSet;
    private MessageType msgNamePromptType;
    private MessageType msgNameSetType;

    private String msgPasswordPrompt;
    private String msgPasswordSet;
    private String msgPasswordDisabled;
    private String msgPasswordRequired;
    private String msgPasswordWrong;
    private String msgPasswordOk;
    private MessageType msgPasswordPromptType;
    private MessageType msgPasswordSetType;
    private MessageType msgPasswordDisabledType;
    private MessageType msgPasswordRequiredType;
    private MessageType msgPasswordWrongType;
    private MessageType msgPasswordOkType;

    private String msgRedirectPrompt;
    private String msgRedirectAdded;
    private String msgRedirectRemoved;
    private String msgRedirectEmpty;
    private MessageType msgRedirectPromptType;
    private MessageType msgRedirectAddedType;
    private MessageType msgRedirectRemovedType;
    private MessageType msgRedirectEmptyType;

    private String msgCancelled;
    private String msgNoPermission;
    private String msgBlockGone;
    private String msgPlayerNotFound;
    private String msgPlayerAdded;
    private MessageType msgCancelledType;
    private MessageType msgNoPermissionType;
    private MessageType msgBlockGoneType;
    private MessageType msgPlayerNotFoundType;
    private MessageType msgPlayerAddedType;

    private String msgNoBlockInSight;
    private String msgNotTeleporter;
    private String msgNoBlocksOwned;
    private String msgYourBlocks;
    private MessageType msgNoBlockInSightType;
    private MessageType msgNotTeleporterType;
    private MessageType msgNoBlocksOwnedType;
    private MessageType msgYourBlocksType;

    private String msgRemoveUsage;
    private String msgRemoveInvalid;
    private String msgRemoveNone;
    private String msgRemoveNotOwner;
    private String msgRemoveSuccess;
    private MessageType msgRemoveUsageType;
    private MessageType msgRemoveInvalidType;
    private MessageType msgRemoveNoneType;
    private MessageType msgRemoveNotOwnerType;
    private MessageType msgRemoveSuccessType;

    private String msgUnknownSub;
    private String msgPlayersOnly;
    private String msgReloaded;
    private String msgListPrompt;
    private String msgHeaderInfo;
    private MessageType msgUnknownSubType;
    private MessageType msgPlayersOnlyType;
    private MessageType msgReloadedType;
    private MessageType msgListPromptType;
    private MessageType msgHeaderInfoType;

    private String msgViewDisabled;
    private String msgViewEnabled;
    private String msgViewUsage;
    private MessageType msgViewDisabledType;
    private MessageType msgViewEnabledType;
    private MessageType msgViewUsageType;

    private String msgTeleportSuccess;
    private String msgElevatorUp;
    private String msgElevatorDown;
    private String msgElevatorDanger;
    private MessageType msgTeleportSuccessType;
    private MessageType msgElevatorUpType;
    private MessageType msgElevatorDownType;
    private MessageType msgElevatorDangerType;

    private String msgNoPair;
    private String msgNoCrossWorld;
    private String msgMissingItem;
    private String msgDistanceTooFar;
    private String msgBlockBound;
    private String msgFeatureDisabled;
    private MessageType msgNoPairType;
    private MessageType msgNoCrossWorldType;
    private MessageType msgMissingItemType;
    private MessageType msgDistanceTooFarType;
    private MessageType msgBlockBoundType;
    private MessageType msgFeatureDisabledType;

    private int viewDurationTicks;
    private int viewUpdateTicks;
    private int viewMaxDistance;
    private int viewParticleCount;
    private Particle viewParticleType;

    private boolean hologramsDefaultEnabled;
    private String hologramsDefaultColor;
    private int hologramsViewDistance;
    private int hologramsUpdateInterval;
    private double hologramsLineHeight;

    private int blockNamingMaxLength;
    private String blockNamingDefaultName;
    private String blockNamingDefaultColor;

    private boolean debug;

    private File blocksFile;
    private FileConfiguration blocksConfig;

    private long messageCooldownMs = 3000L;

    private final Map<UUID, Long> cooldownMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> messageCooldown = new ConcurrentHashMap<>();
    private final Set<String> recentInteractions = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingPearlCancel = ConcurrentHashMap.newKeySet();
    private final Set<UUID> jumpingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, BlockData> blockDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> idIndex = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> globalMembers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> globalGovernings = new ConcurrentHashMap<>();
    private final Map<UUID, ChatInputSession> chatSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> openGuis = new ConcurrentHashMap<>();
    private final Map<UUID, GuiContext> guiContexts = new ConcurrentHashMap<>();
    private final Map<UUID, ViewSession> viewSessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> hologramEntities = new ConcurrentHashMap<>();
    private final Map<String, Integer> redirectIndex = new ConcurrentHashMap<>();
    private final Map<UUID, PendingPassword> pendingPasswords = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, GuiItemMeta>> guiItemMetaMap = new ConcurrentHashMap<>();

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
        public int x;
        public int y;
        public int z;
        public BindClickType clickType = BindClickType.RIGHT;
        public boolean requireSneak = false;
        public boolean requireItem = false;
        public String requiredItemName = "";
        public AccessLevel teleportAccess = AccessLevel.ALL;
        public AccessLevel manageAccess = AccessLevel.OWNER;
        public AccessLevel breakAccess = AccessLevel.ALL;
        public TeleportLocation teleportLocation = TeleportLocation.TOP;
        public String customDestinationWorld = "";
        public double customDestinationX;
        public double customDestinationY;
        public double customDestinationZ;
        public float customDestinationYaw;
        public float customDestinationPitch;
        public String customName = "";
        public boolean hologramEnabled = false;
        public boolean passwordEnabled = false;
        public String password = "";
        public boolean redirectEnabled = false;
        public List<Integer> redirectChain = new ArrayList<>();
        public Set<UUID> members = ConcurrentHashMap.newKeySet();
        public Set<UUID> governings = ConcurrentHashMap.newKeySet();

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
        public Object taskHandle;

        public ViewSession(String mode, long endTime, Object taskHandle) {
            this.mode = mode;
            this.endTime = endTime;
            this.taskHandle = taskHandle;
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

    private static Material resolveMaterial(String name12, String name13plus) {
        String key = name12 + "|" + name13plus;
        Material cached = materialCache.get(key);
        if (cached != null) {
            return cached;
        }
        Material material = null;
        try {
            material = Material.valueOf(name13plus);
        } catch (IllegalArgumentException ignored) {
        }
        if (material == null) {
            try {
                material = Material.valueOf(name12);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (material != null) {
            materialCache.put(key, material);
        }
        return material;
    }

    private static short materialDataValue(Material material) {
        if (material == null) {
            return 0;
        }
        Short cached = materialDataCache.get(material);
        if (cached != null) {
            return cached;
        }
        short data = 0;
        if (material.name().equals("SKULL_ITEM")) {
            data = 3;
        }
        materialDataCache.put(material, data);
        return data;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack createPlayerHead() {
        Material material = resolveMaterial("SKULL_ITEM", "PLAYER_HEAD");
        if (material == null) {
            return new ItemStack(Material.STONE);
        }
        return new ItemStack(material, 1, materialDataValue(material));
    }

    @SuppressWarnings("deprecation")
    private static ItemStack createStack(Material material) {
        if (material == null) {
            return new ItemStack(Material.STONE);
        }
        short data = materialDataValue(material);
        if (data != 0) {
            return new ItemStack(material, 1, data);
        }
        return new ItemStack(material);
    }

    private static Material materialFromConfig(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.equals("PLAYER_HEAD")) {
            return resolveMaterial("SKULL_ITEM", "PLAYER_HEAD");
        }
        String cacheKey = "CFG|" + upper;
        Material cached = materialCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Material material;
        try {
            material = Material.valueOf(upper);
        } catch (IllegalArgumentException e) {
            material = null;
        }
        if (material != null) {
            materialCache.put(cacheKey, material);
        }
        return material;
    }

    private static Particle particleSafe(String name, Particle fallback) {
        if (name == null || name.isEmpty()) {
            return fallback;
        }
        String upper = name.toUpperCase(Locale.ROOT);
        try {
            return Particle.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
        }
        String alt = upper
                .replace("SPELL_WITCH", "WITCH")
                .replace("SPELL_INSTANT", "INSTANT_EFFECT")
                .replace("SPELL_MOB", "ENTITY_EFFECT")
                .replace("SPELL_MOB_AMBIENT", "AMBIENT_ENTITY_EFFECT")
                .replace("WATER_BUBBLE", "BUBBLE")
                .replace("WATER_SPLASH", "SPLASH")
                .replace("WATER_WAKE", "FISHING")
                .replace("REDSTONE", "DUST")
                .replace("SNOWBALL", "ITEM_SNOWBALL")
                .replace("TOTEM", "TOTEM_OF_UNDYING")
                .replace("EXPLOSION_NORMAL", "POOF")
                .replace("EXPLOSION_LARGE", "EXPLOSION")
                .replace("EXPLOSION_HUGE", "EXPLOSION_EMITTER")
                .replace("FIREWORKS_SPARK", "FIREWORK")
                .replace("CRIT_MAGIC", "ENCHANTED_HIT")
                .replace("VILLAGER_ANGRY", "ANGRY_VILLAGER")
                .replace("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        try {
            return Particle.valueOf(alt);
        } catch (IllegalArgumentException ignored) {
        }
        return fallback;
    }

    private static Sound soundSafe(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String upper = name.toUpperCase(Locale.ROOT).replace('.', '_');
        try {
            return Sound.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    private static void initTargetBlockExactReflection() {
        if (getTargetBlockExactResolved) {
            return;
        }
        getTargetBlockExactResolved = true;
        try {
            cachedGetTargetBlockExact = Player.class.getMethod("getTargetBlockExact", int.class);
            cachedGetTargetBlockExact.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
            cachedGetTargetBlockExact = null;
        }
    }

    private static void initTeleportAsyncReflection() {
        if (teleportAsyncResolved) {
            return;
        }
        teleportAsyncResolved = true;
        try {
            cachedTeleportAsync = Entity.class.getMethod("teleportAsync", Location.class);
            cachedTeleportAsync.setAccessible(true);
        } catch (NoSuchMethodException ignored) {
            cachedTeleportAsync = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean tryTeleportAsync(Entity entity, Location location, Consumer<Boolean> callback) {
        initTeleportAsyncReflection();
        if (cachedTeleportAsync == null || entity == null || location == null) {
            return false;
        }
        try {
            Object result = cachedTeleportAsync.invoke(entity, location);
            if (result instanceof CompletableFuture<?>) {
                ((CompletableFuture<Boolean>) result).thenAccept(ok -> {
                    if (callback != null) {
                        callback.accept(ok != null && ok);
                    }
                });
                return true;
            }
            if (callback != null) {
                callback.accept(true);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void initVersionReflection() {
        try {
            String version = Bukkit.getBukkitVersion();
            String[] parts = version.split("-")[0].split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            hexSupported = (major > 1) || (major == 1 && minor >= 16);
        } catch (Throwable ignored) {
            hexSupported = false;
        }
        try {
            sendTitle5 = Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
        } catch (NoSuchMethodException ignored) {
            sendTitle5 = null;
        }
        try {
            sendTitle2 = Player.class.getMethod("sendTitle", String.class, String.class);
        } catch (NoSuchMethodException ignored) {
            sendTitle2 = null;
        }
    }

    private static void initFoliaReflection() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
            return;
        }
        try {
            foliaGetRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
            foliaGetGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
            foliaGetAsyncScheduler = Bukkit.class.getMethod("getAsyncScheduler");
        } catch (NoSuchMethodException e) {
            folia = false;
            return;
        }
        try {
            foliaEntityGetScheduler = Entity.class.getMethod("getScheduler");
            Class<?> entitySchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            foliaEntitySchedulerExecute = entitySchedulerClass.getMethod(
                    "execute",
                    org.bukkit.plugin.Plugin.class,
                    Runnable.class,
                    Runnable.class
            );
        } catch (Throwable ignored) {
        }
        try {
            Class<?> regionSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            foliaRegionSchedulerExecute = regionSchedulerClass.getMethod(
                    "execute",
                    org.bukkit.plugin.Plugin.class,
                    Location.class,
                    Runnable.class
            );
            foliaRegionSchedulerRun = regionSchedulerClass.getMethod(
                    "run",
                    org.bukkit.plugin.Plugin.class,
                    Location.class,
                    Consumer.class
            );
            foliaRegionSchedulerRunDelayed = regionSchedulerClass.getMethod(
                    "runDelayed",
                    org.bukkit.plugin.Plugin.class,
                    Location.class,
                    Consumer.class,
                    long.class
            );
            foliaRegionSchedulerRunAtFixedRate = regionSchedulerClass.getMethod(
                    "runAtFixedRate",
                    org.bukkit.plugin.Plugin.class,
                    Location.class,
                    Consumer.class,
                    long.class,
                    long.class
            );
        } catch (Throwable ignored) {
        }
        try {
            Class<?> globalSchedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            foliaGlobalSchedulerRun = globalSchedulerClass.getMethod(
                    "run",
                    org.bukkit.plugin.Plugin.class,
                    Consumer.class
            );
            foliaGlobalSchedulerRunDelayed = globalSchedulerClass.getMethod(
                    "runDelayed",
                    org.bukkit.plugin.Plugin.class,
                    Consumer.class,
                    long.class
            );
            foliaGlobalSchedulerRunAtFixedRate = globalSchedulerClass.getMethod(
                    "runAtFixedRate",
                    org.bukkit.plugin.Plugin.class,
                    Consumer.class,
                    long.class,
                    long.class
            );
        } catch (Throwable ignored) {
        }
    }

    private final class Sched {

        void entity(Entity entity, Runnable task) {
            if (entity == null) {
                global(task);
                return;
            }
            if (folia) {
                try {
                    foliaEntitySchedulerExecute.invoke(
                            foliaEntityGetScheduler.invoke(entity),
                            Main.this,
                            task,
                            (Runnable) null
                    );
                    return;
                } catch (Throwable ignored) {
                }
            }
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(Main.this, task);
            }
        }

        void at(Location location, Runnable task) {
            if (location == null || location.getWorld() == null) {
                global(task);
                return;
            }
            if (folia) {
                try {
                    Object regionScheduler = foliaGetRegionScheduler.invoke(Bukkit.getServer());
                    foliaRegionSchedulerExecute.invoke(regionScheduler, Main.this, location, task);
                    return;
                } catch (Throwable ignored) {
                }
            }
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(Main.this, task);
            }
        }

        @SuppressWarnings("unchecked")
        void global(Runnable task) {
            if (folia) {
                try {
                    Object globalScheduler = foliaGetGlobalRegionScheduler.invoke(Bukkit.getServer());
                    foliaGlobalSchedulerRun.invoke(globalScheduler, Main.this, (Consumer<Object>) t -> task.run());
                    return;
                } catch (Throwable ignored) {
                }
            }
            if (Bukkit.isPrimaryThread()) {
                task.run();
            } else {
                Bukkit.getScheduler().runTask(Main.this, task);
            }
        }

        @SuppressWarnings("unchecked")
        void entityLater(Entity entity, Runnable task, long delayTicks) {
            if (entity == null) {
                Bukkit.getScheduler().runTaskLater(Main.this, task, delayTicks);
                return;
            }
            if (folia) {
                try {
                    Object globalScheduler = foliaGetGlobalRegionScheduler.invoke(Bukkit.getServer());
                    foliaGlobalSchedulerRunDelayed.invoke(globalScheduler, Main.this, (Consumer<Object>) t -> {
                        if (!entity.isValid()) {
                            return;
                        }
                        try {
                            foliaEntitySchedulerExecute.invoke(
                                    foliaEntityGetScheduler.invoke(entity),
                                    Main.this,
                                    task,
                                    (Runnable) null
                            );
                        } catch (Throwable ignored) {
                            task.run();
                        }
                    }, delayTicks);
                    return;
                } catch (Throwable ignored) {
                }
            }
            Bukkit.getScheduler().runTaskLater(Main.this, task, delayTicks);
        }

        @SuppressWarnings("unchecked")
        void globalTimer(Runnable task, long delayTicks, long periodTicks) {
            if (folia) {
                try {
                    Object globalScheduler = foliaGetGlobalRegionScheduler.invoke(Bukkit.getServer());
                    foliaGlobalSchedulerRunAtFixedRate.invoke(
                            globalScheduler,
                            Main.this,
                            (Consumer<Object>) t -> task.run(),
                            delayTicks,
                            periodTicks
                    );
                    return;
                } catch (Throwable ignored) {
                }
            }
            Bukkit.getScheduler().runTaskTimer(Main.this, task, delayTicks, periodTicks);
        }

        @SuppressWarnings("unchecked")
        Object regionTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
            if (folia) {
                try {
                    Object regionScheduler = foliaGetRegionScheduler.invoke(Bukkit.getServer());
                    return foliaRegionSchedulerRunAtFixedRate.invoke(
                            regionScheduler,
                            Main.this,
                            location,
                            (Consumer<Object>) t -> task.run(),
                            delayTicks,
                            periodTicks
                    );
                } catch (Throwable ignored) {
                }
            }
            return Bukkit.getScheduler().runTaskTimer(Main.this, task, delayTicks, periodTicks).getTaskId();
        }

        void cancel(Object handle) {
            if (handle == null) {
                return;
            }
            if (folia) {
                try {
                    handle.getClass().getMethod("cancel").invoke(handle);
                    return;
                } catch (Throwable ignored) {
                }
            }
            if (handle instanceof Integer) {
                Bukkit.getScheduler().cancelTask((Integer) handle);
            }
        }
    }

    private final Sched sched = new Sched();

    private void teleportPlayer(Player player, Location location, Runnable onSuccess) {
        if (player == null || !player.isOnline()) {
            return;
        }
        boolean started = tryTeleportAsync(player, location, ok -> {
            if (ok && onSuccess != null && player.isOnline()) {
                sched.entity(player, onSuccess);
            }
        });
        if (started) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            player.teleport(location);
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            Bukkit.getScheduler().runTask(this, () -> {
                player.teleport(location);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            });
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        initTargetBlockExactReflection();
        initTeleportAsyncReflection();
        initVersionReflection();
        initFoliaReflection();
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
        getLogger().info("   Folia: " + folia);
        getLogger().info("   Hex colors: " + hexSupported);
        getLogger().info("   Elevator blocks: " + elevatorBlocks.size());
        getLogger().info("   Registered blocks: " + blockDataMap.size());
        getLogger().info("   getTargetBlockExact available: " + (cachedGetTargetBlockExact != null));
        getLogger().info("   teleportAsync available: " + (cachedTeleportAsync != null));
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        for (ViewSession session : viewSessions.values()) {
            sched.cancel(session.taskHandle);
        }
        viewSessions.clear();

        for (UUID id : hologramEntities.values()) {
            try {
                Entity entity = Bukkit.getEntity(id);
                if (entity != null) {
                    entity.remove();
                }
            } catch (Throwable ignored) {
            }
        }
        hologramEntities.clear();

        saveBlocks();
        getLogger().info("Teleporter Disabled!");
    }

    private String color(String message) {
        if (message == null) {
            return "";
        }
        String result = message;

        Matcher hexMatcher = HEX_PATTERN.matcher(result);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            hexMatcher.appendReplacement(
                    hexBuffer,
                    Matcher.quoteReplacement(hexSupported ? hexToMinecraft(hexMatcher.group(1)) : "&f")
            );
        }
        hexMatcher.appendTail(hexBuffer);
        result = hexBuffer.toString();

        Matcher srgbMatcher = SRGB_PATTERN.matcher(result);
        StringBuffer srgbBuffer = new StringBuffer();
        while (srgbMatcher.find()) {
            srgbMatcher.appendReplacement(
                    srgbBuffer,
                    Matcher.quoteReplacement(hexSupported ? hexToMinecraft(srgbMatcher.group(1)) : "&f")
            );
        }
        srgbMatcher.appendTail(srgbBuffer);
        result = srgbBuffer.toString();

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(color(message));
    }

    private void debug(String message) {
        if (debug) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    private MessageType msgType(String path, MessageType fallback) {
        String value = getConfig().getString(path);
        if (value == null) {
            return fallback;
        }
        return getMessageType(value);
    }

    private MessageType getMessageType(String type) {
        if (type == null) {
            return MessageType.NONE;
        }
        String trimmed = type.trim().toUpperCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.equals("NONE") || trimmed.equals("OFF") || trimmed.equals("DISABLED")) {
            return MessageType.NONE;
        }
        try {
            return MessageType.valueOf(trimmed);
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
            Material material = materialFromConfig(blockName);
            if (material != null) {
                elevatorBlocks.add(material);
            } else {
                getLogger().warning("Unknown elevator block: " + blockName);
            }
        }
        blockDistance = getConfig().getInt("Elevator.BlockDistance", 50);
        elevatorEnableParticle = getConfig().getBoolean("Elevator.EnableParticle", true);
        elevatorParticleCount = getConfig().getInt("Elevator.ParticleCount", 20);
        elevatorUsageSound = getConfig().getString("Elevator.UsageSound", "entity.enderman.teleport");
        elevatorActivateSound = getConfig().getString("Elevator.ActivateSound", "entity.player.levelup");
        elevatorAllowUnsafe = getConfig().getBoolean("Elevator.AllowUnsafe", false);
        elevatorParticleType = particleSafe(
                getConfig().getString("Elevator.ParticleType", "SPELL_WITCH"),
                Particle.SPELL_WITCH
        );

        teleporterEnableParticle = getConfig().getBoolean("Teleporter.EnableParticle", true);
        teleporterParticleCount = getConfig().getInt("Teleporter.ParticleCount", 20);
        teleporterUsageSound = getConfig().getString("Teleporter.UsageSound", "entity.enderman.teleport");
        teleporterActivateSound = getConfig().getString("Teleporter.ActivateSound", "entity.player.levelup");
        teleporterAllowUnsafe = getConfig().getBoolean("Teleporter.AllowUnsafe", true);
        teleporterParticleType = particleSafe(
                getConfig().getString("Teleporter.ParticleType", "SPELL_WITCH"),
                Particle.SPELL_WITCH
        );

        teleporterAllowAllBlocks = getConfig().getBoolean("Teleporter.AllowAllBlocks", false);
        teleporterBlockTypesPermission = getConfig().getString("Teleporter.BlockTypesPermission", "");
        maxDistance = getConfig().getDouble("Restrictions.Distance.MaxDistance", 10000.0);

        teleporterBlockTypes.clear();
        List<String> teleporterBlockNames = getConfig().getStringList("Teleporter.BlockTypes");
        if (teleporterBlockNames.isEmpty()) {
            teleporterBlockNames.add("SEA_LANTERN");
        }
        for (String blockName : teleporterBlockNames) {
            Material material = materialFromConfig(blockName);
            if (material != null) {
                teleporterBlockTypes.add(material);
            } else {
                getLogger().warning("Unknown teleporter block: " + blockName);
            }
        }

        teleporterBindItems.clear();
        ConfigurationSection bindItemsSection = getConfig().getConfigurationSection("Teleporter.Bind.Items");
        if (bindItemsSection != null) {
            for (String key : bindItemsSection.getKeys(false)) {
                Material material = materialFromConfig(key);
                if (material == null) {
                    getLogger().warning("Unknown bind item: " + key);
                    continue;
                }
                ConfigurationSection section = bindItemsSection.getConfigurationSection(key);
                boolean consume = section == null || section.getBoolean("Consume", true);
                int amount = section == null ? 1 : section.getInt("ConsumeAmount", 1);
                boolean require = section != null && section.getBoolean("Require", false);
                String permission = section == null ? "" : section.getString("Permission", "");
                teleporterBindItems.put(
                        material,
                        new BindItem(material, consume, amount, require, permission)
                );
            }
        }
        if (teleporterBindItems.isEmpty()) {
            teleporterBindItems.put(
                    Material.ENDER_PEARL,
                    new BindItem(Material.ENDER_PEARL, true, 1, false, "")
            );
        }

        cooldownLocale = getConfig().getString(
                "Restrictions.Cooldown.Locale",
                "&#FF5300Teleporter &7| &fCooldown: %time%s"
        );
        cooldownMessageType = getMessageType(
                getConfig().getString("Restrictions.Cooldown.MessageType", "SUBTITLE")
        );
        messageCooldownMs = getConfig().getLong("Restrictions.MessageCooldown.Milliseconds", 3000L);

        ConfigurationSection elevatorTitles = getConfig().getConfigurationSection("Titles.Elevator");
        if (elevatorTitles != null) {
            elevatorTitleFadeIn = elevatorTitles.getInt("FadeIn", 10);
            elevatorTitleStay = elevatorTitles.getInt("Stay", 40);
            elevatorTitleFadeOut = elevatorTitles.getInt("FadeOut", 10);
        } else {
            elevatorTitleFadeIn = 10;
            elevatorTitleStay = 40;
            elevatorTitleFadeOut = 10;
        }

        ConfigurationSection teleporterTitles = getConfig().getConfigurationSection("Titles.Teleporter");
        if (teleporterTitles != null) {
            teleporterTitleFadeIn = teleporterTitles.getInt("FadeIn", 10);
            teleporterTitleStay = teleporterTitles.getInt("Stay", 60);
            teleporterTitleFadeOut = teleporterTitles.getInt("FadeOut", 10);
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
        viewParticleType = particleSafe(getConfig().getString("View.ParticleType", "END_ROD"), Particle.END_ROD);

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
        if (!usageEnabled(usage)) {
            return false;
        }
        if (!getConfig().getBoolean("Usage." + usage + ".Require", false)) {
            return true;
        }
        String permission = getConfig().getString("Usage." + usage + ".Permission", "");
        return permission.isEmpty() || player.hasPermission(permission);
    }

    private boolean featureEnabled(String feature) {
        return getConfig().getBoolean("Features." + feature + ".Enabled", true);
    }

    private boolean featureAllowed(Player player, String feature) {
        if (!featureEnabled(feature)) {
            return false;
        }
        if (!getConfig().getBoolean("Features." + feature + ".Require", false)) {
            return true;
        }
        String permission = getConfig().getString("Features." + feature + ".Permission", "");
        return permission.isEmpty() || player.hasPermission(permission);
    }

    private AccessLevel featureDefaultAccess(String feature, AccessLevel fallback) {
        String value = getConfig().getString("Features." + feature + ".DefaultAccess");
        if (value == null) {
            return fallback;
        }
        return getAccessLevel(value);
    }

    private boolean commandAllowed(CommandSender sender, String command) {
        if (!getConfig().getBoolean("Commands." + command + ".Enabled", true)) {
            return false;
        }
        if (!getConfig().getBoolean("Commands." + command + ".Require", false)) {
            return true;
        }
        String permission = getConfig().getString("Commands." + command + ".Permission", "");
        return permission.isEmpty() || sender.hasPermission(permission);
    }

    private boolean viewModeAllowed(Player player, String mode) {
        if (!getConfig().getBoolean("Commands.View.Enabled", true)) {
            return false;
        }
        String suffix;
        if (mode.equalsIgnoreCase("all")) {
            suffix = "All";
        } else if (mode.equalsIgnoreCase("owner")) {
            suffix = "Owner";
        } else if (mode.equalsIgnoreCase("member")) {
            suffix = "Member";
        } else {
            return false;
        }
        if (!getConfig().getBoolean("Commands.View." + suffix + ".Require", false)) {
            return true;
        }
        String permission = getConfig().getString("Commands.View." + suffix + ".Permission", "");
        return permission.isEmpty() || player.hasPermission(permission);
    }

    private boolean restrictionEnabled(String restriction) {
        return getConfig().getBoolean("Restrictions." + restriction + ".Enabled", true);
    }

    private boolean restrictionExempt(Player player, String restriction) {
        if (!restrictionEnabled(restriction)) {
            return true;
        }
        if (!getConfig().getBoolean("Restrictions." + restriction + ".Exempt.Require", false)) {
            return false;
        }
        String permission = getConfig().getString("Restrictions." + restriction + ".Exempt.Permission", "");
        return !permission.isEmpty() && player.hasPermission(permission);
    }

    private boolean isBindItem(ItemStack item) {
        return item != null && teleporterBindItems.containsKey(item.getType());
    }

    private BindItem getBindItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        return teleporterBindItems.get(item.getType());
    }

    private void consumeBindItem(Player player, ItemStack hand, BindItem bindItem) {
        if (!bindItem.consume) {
            return;
        }
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
                ConfigurationSection section = blocksSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                BlockData data = new BlockData();
                data.id = section.getInt("Id", 0);
                try {
                    data.owner = UUID.fromString(section.getString("Owner", ""));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                data.world = section.getString("World", "world");
                data.x = section.getInt("X", 0);
                data.y = section.getInt("Y", 0);
                data.z = section.getInt("Z", 0);
                data.clickType = getClickType(section.getString("ClickType", "RIGHT"));
                data.requireSneak = section.getBoolean("RequireSneak", false);
                data.requireItem = section.getBoolean("RequireItem", false);
                data.requiredItemName = section.getString("RequiredItemName", "");
                data.teleportAccess = getAccessLevel(section.getString("TeleportAccess", "ALL"));
                data.manageAccess = getAccessLevel(section.getString("ManageAccess", "OWNER"));
                data.breakAccess = getAccessLevel(section.getString("BreakAccess", "ALL"));
                data.teleportLocation = getTeleportLocation(section.getString("TeleportLocation", "TOP"));
                data.customDestinationWorld = section.getString("CustomDestination.World", "");
                data.customDestinationX = section.getDouble("CustomDestination.X", 0);
                data.customDestinationY = section.getDouble("CustomDestination.Y", 0);
                data.customDestinationZ = section.getDouble("CustomDestination.Z", 0);
                data.customDestinationYaw = (float) section.getDouble("CustomDestination.Yaw", 0);
                data.customDestinationPitch = (float) section.getDouble("CustomDestination.Pitch", 0);
                data.customName = section.getString("CustomName", "");
                data.hologramEnabled = section.getBoolean("HologramEnabled", hologramsDefaultEnabled);
                data.passwordEnabled = section.getBoolean("PasswordEnabled", false);
                data.password = section.getString("Password", "");
                data.redirectEnabled = section.getBoolean("RedirectEnabled", false);
                data.redirectChain = section.getIntegerList("RedirectChain");
                for (String member : section.getStringList("Members")) {
                    try {
                        data.members.add(UUID.fromString(member));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                for (String governing : section.getStringList("Governings")) {
                    try {
                        data.governings.add(UUID.fromString(governing));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                for (String owner : section.getStringList("Owners")) {
                    try {
                        data.governings.add(UUID.fromString(owner));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                blockDataMap.put(data.key(), data);
                idIndex.computeIfAbsent(data.id, k -> ConcurrentHashMap.newKeySet()).add(data.key());
                playerBlocks
                        .computeIfAbsent(data.owner, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(data.key());
            }
        }

        ConfigurationSection playersSection = blocksConfig.getConfigurationSection("Players");
        if (playersSection != null) {
            for (String uuidString : playersSection.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidString);
                if (playerSection == null) {
                    continue;
                }
                Set<UUID> members = ConcurrentHashMap.newKeySet();
                for (String member : playerSection.getStringList("GlobalMembers")) {
                    try {
                        members.add(UUID.fromString(member));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                Set<UUID> governings = ConcurrentHashMap.newKeySet();
                for (String governing : playerSection.getStringList("GlobalGovernings")) {
                    try {
                        governings.add(UUID.fromString(governing));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                for (String owner : playerSection.getStringList("GlobalOwners")) {
                    try {
                        governings.add(UUID.fromString(owner));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                if (!members.isEmpty()) {
                    globalMembers.put(uuid, members);
                }
                if (!governings.isEmpty()) {
                    globalGovernings.put(uuid, governings);
                }
            }
        }
    }

    private void saveBlocks() {
        if (blocksConfig == null || blocksFile == null) {
            return;
        }
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
            List<String> memberList = new ArrayList<>();
            for (UUID member : data.members) {
                memberList.add(member.toString());
            }
            blocksConfig.set(path + ".Members", memberList);
            List<String> governingList = new ArrayList<>();
            for (UUID governing : data.governings) {
                governingList.add(governing.toString());
            }
            blocksConfig.set(path + ".Governings", governingList);
        }

        blocksConfig.set("Players", null);
        Set<UUID> allPlayers = new HashSet<>();
        allPlayers.addAll(globalMembers.keySet());
        allPlayers.addAll(globalGovernings.keySet());
        for (UUID uuid : allPlayers) {
            String path = "Players." + uuid.toString();
            List<String> memberList = new ArrayList<>();
            Set<UUID> members = globalMembers.get(uuid);
            if (members != null) {
                for (UUID member : members) {
                    memberList.add(member.toString());
                }
            }
            blocksConfig.set(path + ".GlobalMembers", memberList);
            List<String> governingList = new ArrayList<>();
            Set<UUID> governings = globalGovernings.get(uuid);
            if (governings != null) {
                for (UUID governing : governings) {
                    governingList.add(governing.toString());
                }
            }
            blocksConfig.set(path + ".GlobalGovernings", governingList);
        }

        try {
            blocksConfig.save(blocksFile);
        } catch (IOException e) {
            getLogger().severe("Could not save blocks.yml: " + e.getMessage());
        }
    }

    private void sendMessage(Player player, String message, MessageType type, int fadeIn, int stay, int fadeOut) {
        if (type == null || type == MessageType.NONE) {
            return;
        }
        if (message == null || message.isEmpty()) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        String formatted = color(message);
        if (formatted.isEmpty()) {
            return;
        }
        sched.entity(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            switch (type) {
                case CHAT:
                    player.sendMessage(formatted);
                    break;
                case TITLE:
                    sendTitleSafe(player, formatted, "", fadeIn, stay, fadeOut);
                    break;
                case SUBTITLE:
                    sendTitleSafe(player, "", formatted, fadeIn, stay, fadeOut);
                    break;
                default:
                    break;
            }
        });
    }

    private void sendTitleSafe(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (sendTitle5 != null) {
            try {
                sendTitle5.invoke(player, title, subtitle, fadeIn, stay, fadeOut);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (sendTitle2 != null) {
            try {
                String combined = title.isEmpty() ? subtitle : title;
                sendTitle2.invoke(player, combined, subtitle);
                return;
            } catch (Throwable ignored) {
            }
        }
        if (!title.isEmpty()) {
            player.sendMessage(title);
        } else if (!subtitle.isEmpty()) {
            player.sendMessage(subtitle);
        }
    }

    private void sendElevatorMessage(Player player, String message, MessageType type) {
        sendMessage(player, message, type, elevatorTitleFadeIn, elevatorTitleStay, elevatorTitleFadeOut);
    }

    private void sendTeleporterMessage(Player player, String message, MessageType type) {
        sendMessage(player, message, type, teleporterTitleFadeIn, teleporterTitleStay, teleporterTitleFadeOut);
    }

    private void sendElevatorPrefixed(Player player, String message, MessageType type) {
        if (message != null && !message.isEmpty()) {
            sendElevatorMessage(player, prefix + message, type);
        }
    }

    private void sendTeleporterPrefixed(Player player, String message, MessageType type) {
        if (message != null && !message.isEmpty()) {
            sendTeleporterMessage(player, prefix + message, type);
        }
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
        if (!canSendMessage(player)) {
            return;
        }
        sendTeleporterPrefixed(player, message, type);
    }

    private void sendElevatorPrefixedThrottled(Player player, String message, MessageType type) {
        if (!canSendMessage(player)) {
            return;
        }
        sendElevatorPrefixed(player, message, type);
    }

    private boolean isInDisabledWorld(Player player, boolean teleporter) {
        String worldName = player.getWorld().getName();
        if (teleporter) {
            return disabledWorldsTeleporter.contains(worldName);
        }
        return disabledWorldsElevator.contains(worldName);
    }

    private long getCooldownRemaining(Player player) {
        if (!restrictionEnabled("Cooldown")) {
            return 0;
        }
        if (restrictionExempt(player, "Cooldown")) {
            return 0;
        }
        Long lastUse = cooldownMap.get(player.getUniqueId());
        if (lastUse == null) {
            return 0;
        }
        int cooldownTime = getConfig().getInt("Restrictions.Cooldown.Time", 30);
        long timeLeft = (lastUse + cooldownTime * 1000L) - System.currentTimeMillis();
        if (timeLeft <= 0) {
            cooldownMap.remove(player.getUniqueId());
            return 0;
        }
        return timeLeft;
    }

    private boolean isOnCooldown(Player player) {
        long timeLeft = getCooldownRemaining(player);
        if (timeLeft > 0) {
            if (canSendMessage(player)) {
                String message = cooldownLocale.replace("%time%", String.valueOf(timeLeft / 1000 + 1));
                sendTeleporterMessage(player, message, cooldownMessageType);
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

    private boolean isSafeLocation(Location location, boolean teleporter) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        boolean unsafeAllowed = teleporter ? teleporterAllowUnsafe : elevatorAllowUnsafe;
        if (unsafeAllowed) {
            return true;
        }
        Material type = location.getBlock().getType();
        return !type.isSolid() && type != Material.LAVA && type != Material.FIRE;
    }

    private void spawnElevatorParticles(Location location) {
        if (!elevatorEnableParticle) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(
                elevatorParticleType,
                location.clone().add(0, 0.5, 0),
                elevatorParticleCount,
                0.3, 0.1, 0.3, 0.1
        );
        world.spawnParticle(
                Particle.END_ROD,
                location.clone().add(0, 0.5, 0),
                DEFAULT_END_ROD_COUNT,
                0.2, 0.2, 0.2, 0.05
        );
    }

    private void spawnTeleporterParticles(Location location) {
        if (!teleporterEnableParticle) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(
                teleporterParticleType,
                location.clone().add(0, 0.5, 0),
                teleporterParticleCount,
                0.3, 0.1, 0.3, 0.1
        );
        world.spawnParticle(
                Particle.END_ROD,
                location.clone().add(0, 0.5, 0),
                DEFAULT_END_ROD_COUNT,
                0.2, 0.2, 0.2, 0.05
        );
    }

    private void playElevatorEffects(Location location, String soundName) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Sound sound = soundSafe(soundName);
        if (sound != null) {
            try {
                location.getWorld().playSound(location, sound, 0.5f, 1.2f);
            } catch (Throwable ignored) {
            }
        }
        spawnElevatorParticles(location);
    }

    private void playTeleporterEffects(Location location, String soundName) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        Sound sound = soundSafe(soundName);
        if (sound != null) {
            try {
                location.getWorld().playSound(location, sound, 0.5f, 1.2f);
            } catch (Throwable ignored) {
            }
        }
        spawnTeleporterParticles(location);
    }

    private void teleportDown(Player player) {
        if (isInDisabledWorld(player, false)) {
            return;
        }
        if (!usageAllowed(player, "Elevator")) {
            return;
        }
        final Location feetLocation = player.getLocation().clone();
        sched.at(feetLocation, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!elevatorBlocks.contains(feetLocation.getBlock().getType())) {
                return;
            }
            for (int i = 1; i <= blockDistance; i++) {
                final Location checkLocation = feetLocation.clone().subtract(0, i, 0);
                if (!elevatorBlocks.contains(checkLocation.getBlock().getType())) {
                    continue;
                }
                final Location targetLocation = checkLocation.clone();
                sched.at(targetLocation, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (!isSafeLocation(targetLocation, false)) {
                        sendElevatorPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
                        return;
                    }
                    teleportPlayer(player, targetLocation, () -> {
                        sched.at(targetLocation, () -> playElevatorEffects(targetLocation, elevatorUsageSound));
                        sendElevatorPrefixedThrottled(player, msgElevatorDown, msgElevatorDownType);
                        setCooldown(player);
                    });
                });
                return;
            }
        });
    }

    private void teleportUp(Player player) {
        if (isInDisabledWorld(player, false)) {
            return;
        }
        if (!usageAllowed(player, "Elevator")) {
            return;
        }
        final Location feetLocation = player.getLocation().clone();
        sched.at(feetLocation, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!elevatorBlocks.contains(feetLocation.getBlock().getType())) {
                return;
            }
            for (int i = 1; i <= blockDistance; i++) {
                final Location checkLocation = feetLocation.clone().add(0, i, 0);
                if (!elevatorBlocks.contains(checkLocation.getBlock().getType())) {
                    continue;
                }
                final Location targetLocation = checkLocation.clone().add(0, 1, 0);
                sched.at(targetLocation, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (!isSafeLocation(targetLocation, false)) {
                        sendElevatorPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
                        return;
                    }
                    player.setVelocity(new Vector(0, 0, 0));
                    player.setFallDistance(0);
                    teleportPlayer(player, targetLocation, () -> {
                        sched.at(targetLocation, () -> playElevatorEffects(targetLocation, elevatorUsageSound));
                        sendElevatorPrefixedThrottled(player, msgElevatorUp, msgElevatorUpType);
                        setCooldown(player);
                    });
                });
                return;
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null) {
            return;
        }
        if (event.getTo().getY() <= event.getFrom().getY()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (jumpingPlayers.contains(uuid)) {
            return;
        }
        if (!usageAllowed(player, "Elevator")) {
            return;
        }
        final Location feetLocation = player.getLocation().clone();
        feetLocation.setY(feetLocation.getY() - 0.1);
        sched.at(feetLocation, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!elevatorBlocks.contains(feetLocation.getBlock().getType())) {
                return;
            }
            for (int i = 1; i <= blockDistance; i++) {
                final Location checkLocation = feetLocation.clone().add(0, i, 0);
                if (!elevatorBlocks.contains(checkLocation.getBlock().getType())) {
                    continue;
                }
                final Location targetLocation = checkLocation.clone().subtract(0, 0.65, 0);
                if (!isSafeLocation(targetLocation, false)) {
                    sendElevatorPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
                    return;
                }
                if (isOnCooldown(player)) {
                    return;
                }
                jumpingPlayers.add(uuid);
                player.setVelocity(new Vector(0, 0, 0));
                player.setFallDistance(0);
                teleportPlayer(player, targetLocation, () -> {
                    sched.at(targetLocation, () -> playElevatorEffects(targetLocation, elevatorUsageSound));
                    sendElevatorPrefixedThrottled(player, msgElevatorUp, msgElevatorUpType);
                    setCooldown(player);
                    sched.entityLater(player, () -> jumpingPlayers.remove(uuid), 10L);
                });
                return;
            }
        });
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!event.isSneaking()) {
            return;
        }
        final Player sneakingPlayer = player;
        sched.entityLater(sneakingPlayer, () -> {
            if (!sneakingPlayer.isOnline() || !sneakingPlayer.isSneaking()) {
                return;
            }
            if (isOnCooldown(sneakingPlayer)) {
                return;
            }
            teleportDown(sneakingPlayer);
        }, SNEAK_CHECK_DELAY_TICKS);
    }

    private boolean canBindBlock(Player player, Block block, Material blockType, ItemStack hand) {
        if (!player.isSneaking()) {
            return false;
        }
        if (isInDisabledWorld(player, true)) {
            return false;
        }
        BindItem bindItem = getBindItem(hand);
        if (bindItem == null) {
            return false;
        }
        if (!bindItemAllowed(player, bindItem)) {
            return false;
        }
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        if (blockDataMap.containsKey(key)) {
            return false;
        }
        if (!teleporterAllowAllBlocks) {
            if (!teleporterBlockTypes.contains(blockType)) {
                boolean allowedByPermission = teleporterBlockTypesPermission != null
                        && !teleporterBlockTypesPermission.isEmpty()
                        && player.hasPermission(teleporterBlockTypesPermission);
                if (!allowedByPermission) {
                    return false;
                }
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
        if (event.getClickedBlock() == null) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isBindItem(hand)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!canBindBlock(player, block, block.getType(), hand)) {
            return;
        }
        event.setCancelled(true);
        try {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        } catch (Throwable ignored) {
        }
        player.setMetadata(
                "teleporter_cancel_pearl",
                new FixedMetadataValue(this, System.currentTimeMillis() + 300L)
        );
        pendingPearlCancel.add(player.getUniqueId());
        sched.entityLater(player, () -> {
            pendingPearlCancel.remove(player.getUniqueId());
            if (player.hasMetadata("teleporter_cancel_pearl")) {
                player.removeMetadata("teleporter_cancel_pearl", Main.this);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        Material blockType = block.getType();
        Action action = event.getAction();
        boolean isRight = action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeft = action == Action.LEFT_CLICK_BLOCK;
        if (!isRight && !isLeft) {
            return;
        }

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

        boolean openGuiWhenSneak = data.requireSneak ? !player.isSneaking() : player.isSneaking();
        if (isRight && openGuiWhenSneak) {
            event.setCancelled(true);
            if (!canManage(player, data)) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            openGui(player, data, "main");
            return;
        }

        if (!usageAllowed(player, "Teleport")) {
            return;
        }
        if (!canTeleport(player, data)) {
            return;
        }

        BindClickType clicked = isRight ? BindClickType.RIGHT : BindClickType.LEFT;
        if (data.clickType != clicked) {
            return;
        }

        if (restrictionEnabled("Sneak") && !restrictionExempt(player, "Sneak")) {
            if (data.requireSneak && !player.isSneaking()) {
                return;
            }
            if (!data.requireSneak && player.isSneaking()) {
                return;
            }
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
        if (recentInteractions.contains(interactionKey)) {
            return;
        }
        recentInteractions.add(interactionKey);
        sched.entityLater(player, () -> recentInteractions.remove(interactionKey), DOUBLE_CLICK_DELAY_TICKS);

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
        if (!(event.getEntity() instanceof EnderPearl)) {
            return;
        }
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity().getShooter();
        if (pendingPearlCancel.contains(player.getUniqueId())) {
            event.setCancelled(true);
            pendingPearlCancel.remove(player.getUniqueId());
            try {
                if (player.hasMetadata("teleporter_cancel_pearl")) {
                    player.removeMetadata("teleporter_cancel_pearl", Main.this);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private boolean itemMatches(ItemStack item, String requiredName) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (requiredName == null || requiredName.isEmpty()) {
            return true;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String display = stripColor(meta.getDisplayName());
        String required = stripColor(requiredName);
        return display.equalsIgnoreCase(required);
    }

    private void tryBindBlock(Player player, Block block, Material blockType) {
        if (!canBindBlock(player, block, blockType, player.getInventory().getItemInMainHand())) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        BindItem bindItem = getBindItem(hand);
        if (bindItem == null) {
            return;
        }
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
        playerBlocks
                .computeIfAbsent(player.getUniqueId(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(key);
        saveBlocks();

        final Location blockLocation = block.getLocation();
        sched.at(blockLocation, () -> playTeleporterEffects(blockLocation, teleporterActivateSound));
        sendTeleporterPrefixed(player, msgBlockBound, msgBlockBoundType);
        openGui(player, data, "main");
    }

    private void performTeleport(Player player, BlockData source) {
        if (!usageAllowed(player, "Teleport")) {
            return;
        }
        long timeLeft = getCooldownRemaining(player);
        if (timeLeft > 0) {
            if (canSendMessage(player)) {
                String message = cooldownLocale.replace("%time%", String.valueOf(timeLeft / 1000 + 1));
                sendTeleporterMessage(player, message, cooldownMessageType);
            }
            return;
        }
        int id = source.id;
        if (id <= 0) {
            return;
        }
        BlockData destination = null;
        if (source.redirectEnabled && !source.redirectChain.isEmpty()) {
            String key = source.key();
            int index = redirectIndex.getOrDefault(key, 0);
            if (index >= source.redirectChain.size()) {
                index = 0;
            }
            int targetId = source.redirectChain.get(index);
            Set<String> candidates = idIndex.get(targetId);
            if (candidates != null && !candidates.isEmpty()) {
                for (String candidate : candidates) {
                    destination = blockDataMap.get(candidate);
                    if (destination != null) {
                        break;
                    }
                }
            }
            redirectIndex.put(key, index + 1);
        } else {
            Set<String> keys = idIndex.get(id);
            if (keys == null || keys.size() < 2) {
                sendTeleporterPrefixedThrottled(player, msgNoPair, msgNoPairType);
                return;
            }
            for (String candidate : keys) {
                if (!candidate.equals(source.key())) {
                    destination = blockDataMap.get(candidate);
                    break;
                }
            }
        }
        if (destination == null) {
            sendTeleporterPrefixedThrottled(player, msgNoPair, msgNoPairType);
            return;
        }
        World destinationWorld = Bukkit.getWorld(destination.world);
        if (destinationWorld == null) {
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
            Location sourceLocation = new Location(player.getWorld(), source.x, source.y, source.z);
            Location destinationLocation = new Location(destinationWorld, destination.x, destination.y, destination.z);
            if (sourceLocation.getWorld() != null && sourceLocation.getWorld().equals(destinationLocation.getWorld())) {
                double distance = sourceLocation.distance(destinationLocation);
                if (distance > maxDistance) {
                    sendTeleporterPrefixedThrottled(player, msgDistanceTooFar, msgDistanceTooFarType);
                    return;
                }
            }
        }
        Location destinationLocation;
        if (destination.teleportLocation == TeleportLocation.CURRENT
                && !destination.customDestinationWorld.isEmpty()) {
            World world = Bukkit.getWorld(destination.customDestinationWorld);
            if (world == null) {
                world = destinationWorld;
            }
            destinationLocation = new Location(
                    world,
                    destination.customDestinationX,
                    destination.customDestinationY,
                    destination.customDestinationZ,
                    destination.customDestinationYaw,
                    destination.customDestinationPitch
            );
        } else {
            destinationLocation = new Location(
                    destinationWorld,
                    destination.x + 0.5,
                    destination.y + 1,
                    destination.z + 0.5,
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch()
            );
        }
        final Location sourceLocation = player.getLocation().clone();
        final Location finalDestination = destinationLocation;
        sched.at(sourceLocation, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!isSafeLocation(finalDestination, true)) {
                sendTeleporterPrefixedThrottled(player, msgElevatorDanger, msgElevatorDangerType);
                return;
            }
            playTeleporterEffects(sourceLocation, teleporterUsageSound);
            sched.at(finalDestination, () -> playTeleporterEffects(finalDestination, teleporterUsageSound));
            teleportPlayer(player, finalDestination, () -> {
                sched.at(finalDestination, () -> playTeleporterEffects(finalDestination, teleporterUsageSound));
                sendTeleporterPrefixedThrottled(player, msgTeleportSuccess, msgTeleportSuccessType);
                setCooldown(player);
            });
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String key = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
        BlockData data = blockDataMap.get(key);
        if (data == null) {
            return;
        }
        if (restrictionEnabled("Access") && !restrictionExempt(player, "Access")) {
            if (!canBreak(player, data)) {
                event.setCancelled(true);
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
            if (keys.isEmpty()) {
                idIndex.remove(data.id);
            }
        }
        List<String> list = playerBlocks.get(data.owner);
        if (list != null) {
            list.remove(key);
            if (list.isEmpty()) {
                playerBlocks.remove(data.owner);
            }
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
        if (restrictionEnabled("Access") && restrictionExempt(player, "Access")) {
            return true;
        }
        AccessLevel level = data.teleportAccess;
        if (level == AccessLevel.ALL) {
            return true;
        }
        if (level == AccessLevel.OWNER) {
            return player.getUniqueId().equals(data.owner);
        }
        if (level == AccessLevel.MEMBERS) {
            if (player.getUniqueId().equals(data.owner)) {
                return true;
            }
            if (data.members.contains(player.getUniqueId())) {
                return true;
            }
            Set<UUID> global = globalMembers.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        if (level == AccessLevel.GOVERNINGS) {
            if (player.getUniqueId().equals(data.owner)) {
                return true;
            }
            if (data.governings.contains(player.getUniqueId())) {
                return true;
            }
            Set<UUID> global = globalGovernings.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        return false;
    }

    private boolean canManage(Player player, BlockData data) {
        if (restrictionEnabled("Access") && restrictionExempt(player, "Access")) {
            return true;
        }
        AccessLevel level = data.manageAccess;
        if (level == AccessLevel.ALL) {
            return true;
        }
        if (level == AccessLevel.OWNER) {
            return player.getUniqueId().equals(data.owner);
        }
        if (level == AccessLevel.MEMBERS) {
            if (player.getUniqueId().equals(data.owner)) {
                return true;
            }
            if (data.members.contains(player.getUniqueId())) {
                return true;
            }
            Set<UUID> global = globalMembers.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        if (level == AccessLevel.GOVERNINGS) {
            if (player.getUniqueId().equals(data.owner)) {
                return true;
            }
            if (data.governings.contains(player.getUniqueId())) {
                return true;
            }
            Set<UUID> global = globalGovernings.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        return false;
    }

    private boolean canBreak(Player player, BlockData data) {
        if (restrictionEnabled("Access") && restrictionExempt(player, "Access")) {
            return true;
        }
        AccessLevel level = data.breakAccess;
        if (level == AccessLevel.ALL) {
            return true;
        }
        if (level == AccessLevel.OWNER) {
            return player.getUniqueId().equals(data.owner);
        }
        if (level == AccessLevel.MEMBERS) {
            if (player.getUniqueId().equals(data.owner)) {
                return true;
            }
            if (data.members.contains(player.getUniqueId())) {
                return true;
            }
            Set<UUID> global = globalMembers.get(data.owner);
            return global != null && global.contains(player.getUniqueId());
        }
        if (level == AccessLevel.GOVERNINGS) {
            if (player.getUniqueId().equals(data.owner)) {
                return true;
            }
            if (data.governings.contains(player.getUniqueId())) {
                return true;
            }
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
            return;
        }
        UUID uuid = player.getUniqueId();
        guiItemMetaMap.remove(uuid);
        Map<Integer, GuiItemMeta> slotMap = new HashMap<>();
        guiItemMetaMap.put(uuid, slotMap);

        String title = menu.getString("title", "&8Menu");
        int size = menu.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }

        String listType = listTypeOverride;
        if (listType == null && guiContexts.containsKey(uuid)) {
            GuiContext previousContext = guiContexts.get(uuid);
            if (previousContext.menuName.equals(menuName)) {
                listType = previousContext.listType;
            }
        }

        int page = 0;
        if (guiContexts.containsKey(uuid)) {
            GuiContext previousContext = guiContexts.get(uuid);
            if (previousContext.menuName.equals(menuName)) {
                page = previousContext.page;
            }
        }

        String inventoryTitle = replacePlaceholders(title, player, data, listType, null, 0, page);
        Inventory inventory = Bukkit.createInventory(null, size, color(inventoryTitle));

        ConfigurationSection items = menu.getConfigurationSection("items");
        if (items != null) {
            for (String itemKey : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(itemKey);
                if (itemSection == null) {
                    continue;
                }
                if (!checkViewRequirement(player, itemSection)) {
                    continue;
                }
                String featureKey = guiItemFeatureKey(itemKey);
                if (featureKey != null && !featureAllowed(player, featureKey)) {
                    continue;
                }
                String materialName = itemSection.getString("material", "STONE");
                Material material = materialFromConfig(materialName);
                if (material == null) {
                    material = Material.STONE;
                }
                int slot = itemSection.getInt("slot", -1);
                if (slot < 0 || slot >= size) {
                    continue;
                }
                String displayName = replacePlaceholders(
                        itemSection.getString("display_name", ""),
                        player,
                        data,
                        listType,
                        null,
                        0,
                        page
                );
                List<String> loreList = itemSection.getStringList("lore");
                List<String> processedLore = new ArrayList<>();
                for (String line : loreList) {
                    processedLore.add(replacePlaceholders(line, player, data, listType, null, 0, page));
                }
                boolean hideAttributes = itemSection.getBoolean("hide_attributes", false);
                ItemStack itemStack = createItem(material, displayName, processedLore, hideAttributes);
                inventory.setItem(slot, itemStack);
                GuiItemMeta meta = new GuiItemMeta();
                meta.itemKey = itemKey;
                meta.menuName = menuName;
                meta.leftCommands = itemSection.getStringList("left_click_commands");
                meta.rightCommands = itemSection.getStringList("right_click_commands");
                meta.shiftLeftCommands = itemSection.getStringList("shift_left_click_commands");
                meta.shiftRightCommands = itemSection.getStringList("shift_right_click_commands");
                slotMap.put(slot, meta);
            }
        }

        ConfigurationSection dynamic = menu.getConfigurationSection("dynamic");
        if (dynamic != null) {
            if (menuName.equals("list") && listType != null) {
                Set<UUID> list = getListByType(data, listType);
                ConfigurationSection playerEntry = dynamic.getConfigurationSection("player_entry");
                if (playerEntry != null) {
                    String materialName = playerEntry.getString("material", "PLAYER_HEAD");
                    Material material = materialFromConfig(materialName);
                    if (material == null) {
                        material = resolveMaterial("SKULL_ITEM", "PLAYER_HEAD");
                    }
                    int startSlot = dynamic.getInt("slots.start", 0);
                    int endSlot = dynamic.getInt("slots.end", size - 10);
                    List<UUID> sorted = new ArrayList<>(list);
                    int perPage = endSlot - startSlot + 1;
                    int totalPages = Math.max(1, (int) Math.ceil(sorted.size() / (double) perPage));
                    if (page >= totalPages) {
                        page = totalPages - 1;
                    }
                    if (page < 0) {
                        page = 0;
                    }
                    int fromIndex = page * perPage;
                    int toIndex = Math.min(fromIndex + perPage, sorted.size());
                    int slotOffset = 0;
                    for (int i = fromIndex; i < toIndex; i++) {
                        UUID target = sorted.get(i);
                        int slot = startSlot + slotOffset;
                        if (slot > endSlot || slot >= size) {
                            break;
                        }
                        String name = Bukkit.getOfflinePlayer(target).getName();
                        if (name == null) {
                            name = target.toString();
                        }
                        String displayName = playerEntry.getString("display_name", "");
                        displayName = displayName.replace("%name%", name).replace("%uuid%", target.toString());
                        List<String> loreList = playerEntry.getStringList("lore");
                        List<String> processedLore = new ArrayList<>();
                        for (String line : loreList) {
                            processedLore.add(replacePlaceholders(line, player, data, listType, name, i, page));
                        }
                        boolean hideAttributes = playerEntry.getBoolean("hide_attributes", false);
                        inventory.setItem(slot, createItem(material, displayName, processedLore, hideAttributes));
                        GuiItemMeta meta = new GuiItemMeta();
                        meta.itemKey = "player_entry";
                        meta.menuName = menuName;
                        meta.leftCommands = new ArrayList<>();
                        for (String command : playerEntry.getStringList("left_click_commands")) {
                            meta.leftCommands.add(command.replace("%uuid%", target.toString()).replace("%name%", name));
                        }
                        slotMap.put(slot, meta);
                        slotOffset++;
                    }
                }
            } else if (menuName.equals("redirect")) {
                ConfigurationSection stepEntry = dynamic.getConfigurationSection("step_entry");
                if (stepEntry != null) {
                    String materialName = stepEntry.getString("material", "PAPER");
                    Material material = materialFromConfig(materialName);
                    if (material == null) {
                        material = Material.PAPER;
                    }
                    int startSlot = dynamic.getInt("slots.start", 9);
                    int endSlot = dynamic.getInt("slots.end", 44);
                    int slotOffset = 0;
                    for (int i = 0; i < data.redirectChain.size(); i++) {
                        int id = data.redirectChain.get(i);
                        int slot = startSlot + slotOffset;
                        if (slot > endSlot || slot >= size) {
                            break;
                        }
                        String displayName = stepEntry.getString("display_name", "")
                                .replace("%index%", String.valueOf(i + 1))
                                .replace("%id%", String.valueOf(id));
                        List<String> loreList = stepEntry.getStringList("lore");
                        List<String> processedLore = new ArrayList<>();
                        for (String line : loreList) {
                            processedLore.add(replacePlaceholders(line, player, data, listType, null, i, page));
                        }
                        boolean hideAttributes = stepEntry.getBoolean("hide_attributes", false);
                        inventory.setItem(slot, createItem(material, displayName, processedLore, hideAttributes));
                        GuiItemMeta meta = new GuiItemMeta();
                        meta.itemKey = "step_entry";
                        meta.menuName = menuName;
                        meta.leftCommands = new ArrayList<>();
                        for (String command : stepEntry.getStringList("left_click_commands")) {
                            meta.leftCommands.add(command.replace("%index%", String.valueOf(i)));
                        }
                        slotMap.put(slot, meta);
                        slotOffset++;
                    }
                }
            }
        }

        GuiContext context = new GuiContext(data.key(), menuName);
        context.listType = listType;
        context.page = page;
        guiContexts.put(uuid, context);
        openGuis.put(uuid, inventory);
        player.openInventory(inventory);
    }

    private String guiItemFeatureKey(String itemKey) {
        switch (itemKey) {
            case "click_type":
                return "ClickType";
            case "sneak":
                return "RequireSneak";
            case "required_item":
                return "Bind";
            case "location":
                return "TeleportLocation";
            case "name":
                return "Naming";
            case "hologram":
                return "Hologram";
            case "redirect":
                return "Redirect";
            case "teleport_access":
                return "TeleportAccess";
            case "manage_access":
                return "ManageAccess";
            case "members":
                return "PeopleUse";
            case "break_access":
                return "BreakAccess";
            case "password":
                return "Password";
            default:
                return null;
        }
    }

    private boolean checkViewRequirement(Player player, ConfigurationSection itemSection) {
        ConfigurationSection viewRequirement = itemSection.getConfigurationSection("view_requirement");
        if (viewRequirement == null) {
            return true;
        }
        String type = viewRequirement.getString("type", "");
        if (type.equalsIgnoreCase("holding_item")) {
            String materialName = viewRequirement.getString("material", "");
            Material material = materialFromConfig(materialName);
            if (material == null) {
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            return hand != null && hand.getType() == material;
        }
        if (type.equalsIgnoreCase("permission")) {
            String permission = viewRequirement.getString("permission", "");
            if (permission.isEmpty()) {
                return true;
            }
            return player.hasPermission(permission);
        }
        return true;
    }

    private String replacePlaceholders(
            String text,
            Player player,
            BlockData data,
            String listType,
            String extraName,
            int index,
            int page
    ) {
        if (text == null) {
            return "";
        }
        String result = text;
        if (data != null) {
            result = result.replace("%id%", String.valueOf(data.id));
            result = result.replace("%blockname%", data.customName.isEmpty() ? "" : data.customName);
            result = result.replace("%click%", data.clickType.name());
            result = result.replace("%sneak%", data.requireSneak ? "Yes" : "No");
            result = result.replace("%item%", data.requireItem ? "Yes" : "No");
            result = result.replace("%value%", data.requiredItemName.isEmpty() ? "-" : data.requiredItemName);
            result = result.replace("%location%", data.teleportLocation.name());
            result = result.replace("%teleport_access%", data.teleportAccess.name());
            result = result.replace("%manage_access%", data.manageAccess.name());
            result = result.replace("%break_access%", data.breakAccess.name());
            result = result.replace("%password%", data.passwordEnabled ? "Yes" : "No");
            result = result.replace("%redirect%", data.redirectEnabled ? "Yes" : "No");
            result = result.replace("%members%", String.valueOf(data.members.size()));
            result = result.replace("%governings%", String.valueOf(data.governings.size()));
            result = result.replace("%count%", String.valueOf(data.members.size()));
            result = result.replace("%list%", listType == null ? "List" : listType);
            result = result.replace("%index%", String.valueOf(index + 1));
            result = result.replace("%page%", String.valueOf(page + 1));
            result = result.replace("%pages%", "1");
            result = result.replace("%hologram%", data.hologramEnabled ? "Yes" : "No");
            result = result.replace("%name%", data.customName.isEmpty() ? "" : data.customName);
        }
        result = result.replace("%player%", player.getName());
        if (extraName != null) {
            result = result.replace("%entry%", extraName);
        }
        return result;
    }

    private Set<UUID> getListByType(BlockData data, String listType) {
        if (listType == null) {
            return new HashSet<>();
        }
        switch (listType) {
            case "LOCAL_MEMBERS":
                return data.members;
            case "LOCAL_GOVERNINGS":
                return data.governings;
            case "GLOBAL_MEMBERS":
                return globalMembers.computeIfAbsent(data.owner, k -> ConcurrentHashMap.newKeySet());
            case "GLOBAL_GOVERNINGS":
                return globalGovernings.computeIfAbsent(data.owner, k -> ConcurrentHashMap.newKeySet());
            default:
                return new HashSet<>();
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, lore, false);
    }

    @SuppressWarnings("deprecation")
    private ItemStack createItem(Material material, String name, List<String> lore, boolean hideAttributes) {
        ItemStack item = createStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null && !lore.isEmpty()) {
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    processedLore.add(color(line));
                }
                meta.setLore(processedLore);
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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        Inventory openedInventory = openGuis.get(uuid);
        if (openedInventory == null) {
            return;
        }
        if (event.getInventory() != openedInventory) {
            return;
        }
        event.setCancelled(true);

        GuiContext context = guiContexts.get(uuid);
        if (context == null) {
            return;
        }
        BlockData data = blockDataMap.get(context.blockKey);
        if (data == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }
        Map<Integer, GuiItemMeta> slotMap = guiItemMetaMap.get(uuid);
        if (slotMap == null) {
            return;
        }
        GuiItemMeta meta = slotMap.get(slot);
        if (meta == null) {
            return;
        }

        List<String> commands;
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
                commands = new ArrayList<>();
                break;
        }
        if (commands == null || commands.isEmpty()) {
            return;
        }
        for (String command : commands) {
            handleGuiCommand(player, data, command, meta, slot);
        }
    }

    private void handleGuiCommand(Player player, BlockData data, String command, GuiItemMeta meta, int slot) {
        if (command == null || command.isEmpty()) {
            return;
        }
        if (command.equals("[close]")) {
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
        String matchedPrefix = null;
        for (String prefixCandidate : prefixes) {
            if (command.startsWith(prefixCandidate)) {
                matchedPrefix = prefixCandidate;
                break;
            }
        }
        if (matchedPrefix == null) {
            return;
        }
        String argument = command.substring(matchedPrefix.length()).trim();

        if (matchedPrefix.equals("[opengui] ")) {
            openGui(player, data, argument);
            return;
        }
        if (matchedPrefix.equals("[opengui_list] ")) {
            openGui(player, data, "list", argument);
            return;
        }
        if (matchedPrefix.equals("[teleporter_page] ")) {
            GuiContext context = guiContexts.get(player.getUniqueId());
            int page = context != null ? context.page : 0;
            if (argument.equals("next")) {
                page++;
            } else if (argument.equals("previous")) {
                page = Math.max(0, page - 1);
            }
            String listType = context != null ? context.listType : null;
            BlockData freshData = blockDataMap.get(data.key());
            if (freshData != null) {
                GuiContext tempContext = new GuiContext(freshData.key(), "list");
                tempContext.listType = listType;
                tempContext.page = page;
                guiContexts.put(player.getUniqueId(), tempContext);
                openGuiWithContext(player, freshData, "list", listType, page);
            }
            return;
        }
        if (matchedPrefix.equals("[teleporter_input] ")) {
            GuiContext context = guiContexts.get(player.getUniqueId());
            String listType = context != null ? context.listType : null;
            player.closeInventory();
            if (argument.equals("SET_ID")) {
                sendTeleporterPrefixed(player, msgIdPrompt, msgIdPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_ID", data.key()));
            } else if (argument.equals("SET_ITEM")) {
                if (!featureAllowed(player, "Bind")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgItemPrompt, msgItemPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_ITEM", data.key()));
            } else if (argument.equals("SET_NAME")) {
                if (!featureAllowed(player, "Naming")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgNamePrompt, msgNamePromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_NAME", data.key()));
            } else if (argument.equals("SET_PASSWORD")) {
                if (!featureAllowed(player, "Password")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgPasswordPrompt, msgPasswordPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("SET_PASSWORD", data.key()));
            } else if (argument.equals("ADD_STEP")) {
                if (!featureAllowed(player, "Redirect")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgRedirectPrompt, msgRedirectPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("ADD_STEP", data.key()));
            } else if (argument.equals("CLEAR_STEPS")) {
                if (!featureAllowed(player, "Redirect")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                data.redirectChain.clear();
                saveBlocks();
                openGui(player, data, "redirect");
            } else if (argument.equals("ADD_PLAYER")) {
                if (!featureAllowed(player, "PeopleUse")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                sendTeleporterPrefixed(player, msgListPrompt, msgListPromptType);
                chatSessions.put(player.getUniqueId(), new ChatInputSession("ADD_PLAYER", data.key(), listType));
            }
            return;
        }
        if (matchedPrefix.equals("[teleporter_toggle] ")) {
            String featureKey = toggleFeatureKey(argument);
            if (featureKey != null && !featureAllowed(player, featureKey)) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            switch (argument) {
                case "click":
                    data.clickType = data.clickType == BindClickType.LEFT ? BindClickType.RIGHT : BindClickType.LEFT;
                    break;
                case "sneak":
                    data.requireSneak = !data.requireSneak;
                    break;
                case "location":
                    data.teleportLocation = data.teleportLocation == TeleportLocation.TOP
                            ? TeleportLocation.CURRENT
                            : TeleportLocation.TOP;
                    if (data.teleportLocation == TeleportLocation.CURRENT) {
                        Location location = player.getLocation();
                        Block block = player.getWorld().getBlockAt(data.x, data.y, data.z);
                        if (location.getWorld() != null
                                && location.getWorld().equals(block.getWorld())
                                && location.distance(block.getLocation()) <= 5.0) {
                            data.customDestinationWorld = location.getWorld().getName();
                            data.customDestinationX = location.getX();
                            data.customDestinationY = location.getY();
                            data.customDestinationZ = location.getZ();
                            data.customDestinationYaw = location.getYaw();
                            data.customDestinationPitch = location.getPitch();
                        } else {
                            data.customDestinationWorld = "";
                        }
                    }
                    break;
                case "hologram":
                    data.hologramEnabled = !data.hologramEnabled;
                    if (data.hologramEnabled) {
                        updateHologram(data);
                    } else {
                        removeHologram(data);
                    }
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
                default:
                    break;
            }
            saveBlocks();
            openGui(player, data, meta.menuName);
            return;
        }
        if (matchedPrefix.equals("[teleporter_list_remove] ")) {
            if (!featureAllowed(player, "PeopleUse")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(argument);
            } catch (IllegalArgumentException e) {
                return;
            }
            GuiContext context = guiContexts.get(player.getUniqueId());
            if (context != null && context.listType != null) {
                Set<UUID> list = getListByType(data, context.listType);
                list.remove(targetUuid);
                saveBlocks();
            }
            String listType = context != null ? context.listType : null;
            int page = context != null ? context.page : 0;
            openGuiWithContext(player, data, "list", listType, page);
            return;
        }
        if (matchedPrefix.equals("[teleporter_redirect_remove] ")) {
            if (!featureAllowed(player, "Redirect")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return;
            }
            int index;
            try {
                index = Integer.parseInt(argument);
            } catch (NumberFormatException e) {
                return;
            }
            if (index >= 0 && index < data.redirectChain.size()) {
                int removedId = data.redirectChain.remove(index);
                saveBlocks();
                player.sendMessage(color(prefix + msgRedirectRemoved.replace("%id%", String.valueOf(removedId))));
            }
            openGui(player, data, "redirect");
            return;
        }
    }

    private String toggleFeatureKey(String argument) {
        switch (argument) {
            case "click":
                return "ClickType";
            case "sneak":
                return "RequireSneak";
            case "location":
                return "TeleportLocation";
            case "hologram":
                return "Hologram";
            case "teleport_access":
                return "TeleportAccess";
            case "manage_access":
                return "ManageAccess";
            case "break_access":
                return "BreakAccess";
            case "redirect":
                return "Redirect";
            default:
                return null;
        }
    }

    private void openGuiWithContext(Player player, BlockData data, String menuName, String listType, int page) {
        GuiContext context = new GuiContext(data.key(), menuName);
        context.listType = listType;
        context.page = page;
        guiContexts.put(player.getUniqueId(), context);
        openGui(player, data, menuName, listType);
    }

    private AccessLevel cycleTeleportAccess(AccessLevel current) {
        AccessLevel[] values = {AccessLevel.OWNER, AccessLevel.MEMBERS, AccessLevel.GOVERNINGS, AccessLevel.ALL};
        return values[(Arrays.asList(values).indexOf(current) + 1) % values.length];
    }

    private AccessLevel cycleManageAccess(AccessLevel current) {
        AccessLevel[] values = {AccessLevel.OWNER, AccessLevel.GOVERNINGS, AccessLevel.ALL};
        return values[(Arrays.asList(values).indexOf(current) + 1) % values.length];
    }

    private AccessLevel cycleBreakAccess(AccessLevel current) {
        AccessLevel[] values = {AccessLevel.OWNER, AccessLevel.MEMBERS, AccessLevel.GOVERNINGS, AccessLevel.ALL};
        return values[(Arrays.asList(values).indexOf(current) + 1) % values.length];
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (openGuis.get(uuid) == event.getInventory()) {
            openGuis.remove(uuid);
            guiContexts.remove(uuid);
            guiItemMetaMap.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
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
            sched.entity(player, () -> {
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
            });
            return;
        }

        ChatInputSession session = chatSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
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
        sched.entity(player, () -> handleChatInput(player, data, session, message));
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
                if (existing != null
                        && !existing.isEmpty()
                        && !(existing.size() == 1 && existing.contains(data.key()))) {
                    if (existing.size() >= 2) {
                        sendTeleporterPrefixedThrottled(player, msgIdUsed, msgIdUsedType);
                        return;
                    }
                }
                Set<String> oldKeys = idIndex.get(data.id);
                if (oldKeys != null) {
                    oldKeys.remove(data.key());
                    if (oldKeys.isEmpty()) {
                        idIndex.remove(data.id);
                    }
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
                String name = message;
                if (name.length() > blockNamingMaxLength) {
                    name = name.substring(0, blockNamingMaxLength);
                }
                if (!name.startsWith("&")
                        && !name.startsWith("&#")
                        && !name.startsWith("<#")
                        && !name.startsWith("§")) {
                    name = blockNamingDefaultColor + name;
                }
                data.customName = name;
                saveBlocks();
                if (data.hologramEnabled) {
                    updateHologram(data);
                }
                sendTeleporterPrefixed(player, msgNameSet.replace("%name%", color(name)), msgNameSetType);
                openGui(player, data, "main");
                return;
            }
            case "SET_PASSWORD": {
                if (!featureAllowed(player, "Password")) {
                    sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                    return;
                }
                if (message.equalsIgnoreCase("no")
                        || message.equalsIgnoreCase("not")
                        || message.equalsIgnoreCase("off")) {
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
                Player targetPlayer = Bukkit.getPlayerExact(message);
                if (targetPlayer == null) {
                    sendTeleporterPrefixedThrottled(player, msgPlayerNotFound, msgPlayerNotFoundType);
                    return;
                }
                if (session.listType == null) {
                    sendTeleporterPrefixedThrottled(player, msgCancelled, msgCancelledType);
                    return;
                }
                Set<UUID> list = getListByType(data, session.listType);
                list.add(targetPlayer.getUniqueId());
                saveBlocks();
                sendTeleporterPrefixed(
                        player,
                        msgPlayerAdded.replace("%name%", targetPlayer.getName()),
                        msgPlayerAddedType
                );
                openGuiWithContext(player, data, "list", session.listType, 0);
                return;
            }
            default:
                break;
        }
    }

    private void startHologramUpdater() {
        sched.globalTimer(() -> {
            for (BlockData data : blockDataMap.values()) {
                if (data.hologramEnabled) {
                    updateHologram(data);
                }
            }
        }, 20L, Math.max(1, hologramsUpdateInterval));
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
        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            return;
        }
        final Location location = new Location(
                world,
                data.x + 0.5,
                data.y + 1.2 + hologramsLineHeight,
                data.z + 0.5
        );
        UUID existingId = hologramEntities.get(data.key());
        ArmorStand stand = null;
        if (existingId != null) {
            Entity entity = Bukkit.getEntity(existingId);
            if (entity instanceof ArmorStand && entity.isValid()) {
                stand = (ArmorStand) entity;
            } else {
                hologramEntities.remove(data.key());
            }
        }
        if (stand == null) {
            final String hologramKey = data.key();
            final String hologramName = color(data.customName);
            sched.at(location, () -> {
                ArmorStand armorStand = world.spawn(location, ArmorStand.class, spawned -> {
                    spawned.setVisible(false);
                    spawned.setGravity(false);
                    spawned.setMarker(true);
                    spawned.setSmall(true);
                    spawned.setCustomNameVisible(true);
                    spawned.setInvulnerable(true);
                    spawned.setBasePlate(false);
                    spawned.setArms(false);
                });
                armorStand.setCustomName(hologramName);
                hologramEntities.put(hologramKey, armorStand.getUniqueId());
            });
            return;
        }
        final ArmorStand finalStand = stand;
        final String hologramName = color(data.customName);
        sched.entity(finalStand, () -> {
            if (!tryTeleportAsync(finalStand, location, ok -> {
            })) {
                try {
                    finalStand.teleport(location);
                } catch (Throwable ignored) {
                }
            }
            finalStand.setCustomName(hologramName);
        });
    }

    private void removeHologram(BlockData data) {
        UUID id = hologramEntities.remove(data.key());
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity == null) {
            return;
        }
        try {
            entity.remove();
        } catch (Throwable ignored) {
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
        final Player viewPlayer = player;
        final String viewMode = mode;
        Runnable task = () -> {
            if (!viewPlayer.isOnline()) {
                stopViewSession(viewPlayer);
                return;
            }
            if (System.currentTimeMillis() >= endTime) {
                stopViewSession(viewPlayer);
                sendTeleporterPrefixedThrottled(viewPlayer, msgViewDisabled, msgViewDisabledType);
                return;
            }
            counter[0]++;
            if (counter[0] % Math.max(1, viewUpdateTicks) != 0) {
                return;
            }
            for (BlockData data : blockDataMap.values()) {
                if (!matchesViewMode(viewPlayer, data, viewMode)) {
                    continue;
                }
                World world = Bukkit.getWorld(data.world);
                if (world == null) {
                    continue;
                }
                if (!world.getName().equals(viewPlayer.getWorld().getName())) {
                    continue;
                }
                Location location = new Location(world, data.x + 0.5, data.y + 1.2, data.z + 0.5);
                if (location.distanceSquared(viewPlayer.getLocation())
                        > (double) viewMaxDistance * viewMaxDistance) {
                    continue;
                }
                try {
                    viewPlayer.spawnParticle(
                            viewParticleType,
                            location,
                            viewParticleCount,
                            0.2, 0.2, 0.2, 0.01
                    );
                    viewPlayer.spawnParticle(
                            teleporterParticleType,
                            location,
                            2,
                            0.3, 0.3, 0.3, 0.01
                    );
                } catch (Exception ignored) {
                }
            }
        };
        Object taskHandle = sched.regionTimer(player.getLocation(), task, 0L, 1L);
        viewSessions.put(player.getUniqueId(), new ViewSession(mode, endTime, taskHandle));
        sendTeleporterPrefixed(player, msgViewEnabled.replace("%mode%", mode), msgViewEnabledType);
    }

    private void stopViewSession(Player player) {
        ViewSession session = viewSessions.remove(player.getUniqueId());
        if (session != null) {
            sched.cancel(session.taskHandle);
        }
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
            if (data.owner.equals(uuid)) {
                return true;
            }
            if (data.members.contains(uuid)) {
                return true;
            }
            if (data.governings.contains(uuid)) {
                return true;
            }
            Set<UUID> globalMemberSet = globalMembers.get(data.owner);
            if (globalMemberSet != null && globalMemberSet.contains(uuid)) {
                return true;
            }
            Set<UUID> globalGoverningSet = globalGovernings.get(data.owner);
            return globalGoverningSet != null && globalGoverningSet.contains(uuid);
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
        jumpingPlayers.remove(uuid);
        stopViewSession(event.getPlayer());
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
            for (String key : keys) {
                BlockData data = blockDataMap.get(key);
                if (data != null) {
                    result.add(data);
                }
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
        if (!command.getName().equalsIgnoreCase("teleporter")) {
            return false;
        }

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

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        if (subCommand.equals("reload")) {
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

        if (subCommand.equals("info")) {
            if (!commandAllowed(player, "Info")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) {
                return true;
            }
            for (BlockData data : list) {
                sendBlockInfo(player, data);
            }
            return true;
        }

        if (subCommand.equals("list")) {
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
            for (String key : keys) {
                BlockData data = blockDataMap.get(key);
                if (data == null) {
                    continue;
                }
                String line = "&#FF5300ID " + data.id + " &7-> &f" + key;
                if (!data.customName.isEmpty()) {
                    line += " &7(" + color(data.customName) + "&7)";
                }
                player.sendMessage(color(line));
            }
            return true;
        }

        if (subCommand.equals("remove")) {
            if (!commandAllowed(player, "Remove")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) {
                return true;
            }
            List<BlockData> toRemove = new ArrayList<>();
            boolean bypassAccess = restrictionEnabled("Access") && restrictionExempt(player, "Access");
            for (BlockData data : list) {
                if (data.owner.equals(player.getUniqueId()) || bypassAccess) {
                    toRemove.add(data);
                }
            }
            if (toRemove.isEmpty()) {
                sendTeleporterPrefixedThrottled(player, msgRemoveNotOwner, msgRemoveNotOwnerType);
                return true;
            }
            int firstId = toRemove.get(0).id;
            for (BlockData data : toRemove) {
                removeBlock(data);
            }
            String message = msgRemoveSuccess
                    .replace("%count%", String.valueOf(toRemove.size()))
                    .replace("%id%", String.valueOf(firstId));
            sendTeleporterPrefixed(player, message, msgRemoveSuccessType);
            return true;
        }

        if (subCommand.equals("view")) {
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

        if (subCommand.equals("menu")) {
            if (!commandAllowed(player, "Menu")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) {
                return true;
            }
            BlockData menuTarget = null;
            for (BlockData data : list) {
                if (canManage(player, data)) {
                    menuTarget = data;
                    break;
                }
            }
            if (menuTarget == null) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            openGui(player, menuTarget, "main");
            return true;
        }

        if (subCommand.equals("tp")) {
            if (!commandAllowed(player, "Tp")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            List<BlockData> list = resolveBlockDataList(player, args, 1);
            if (list.isEmpty()) {
                return true;
            }
            if (!usageAllowed(player, "Teleport")) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            BlockData teleportTarget = null;
            for (BlockData data : list) {
                if (canTeleport(player, data)) {
                    teleportTarget = data;
                    break;
                }
            }
            if (teleportTarget == null) {
                sendTeleporterPrefixedThrottled(player, msgNoPermission, msgNoPermissionType);
                return true;
            }
            World world = Bukkit.getWorld(teleportTarget.world);
            if (world == null) {
                sendTeleporterPrefixedThrottled(player, msgBlockGone, msgBlockGoneType);
                return true;
            }
            final BlockData finalTarget = teleportTarget;
            final World finalWorld = world;
            sched.entity(player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                Location location = new Location(
                        finalWorld,
                        finalTarget.x + 0.5,
                        finalTarget.y + 1,
                        finalTarget.z + 0.5,
                        player.getLocation().getYaw(),
                        player.getLocation().getPitch()
                );
                final Location finalLocation = location;
                teleportPlayer(player, finalLocation, () -> {
                    sched.at(finalLocation, () -> playTeleporterEffects(finalLocation, teleporterUsageSound));
                    sendTeleporterPrefixedThrottled(player, msgTeleportSuccess, msgTeleportSuccessType);
                });
            });
            return true;
        }

        sendTeleporterPrefixedThrottled(player, msgUnknownSub, msgUnknownSubType);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("teleporter")) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String subCommand : Arrays.asList("reload", "info", "list", "remove", "view", "menu", "tp")) {
                if (subCommand.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    result.add(subCommand);
                }
            }
            return result;
        }
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase(Locale.ROOT);
            if (subCommand.equals("view")) {
                for (String mode : Arrays.asList("all", "owner", "member")) {
                    if (mode.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        result.add(mode);
                    }
                }
                return result;
            }
            if ((subCommand.equals("remove")
                    || subCommand.equals("info")
                    || subCommand.equals("menu")
                    || subCommand.equals("tp"))
                    && sender instanceof Player) {
                List<String> keys = playerBlocks.get(((Player) sender).getUniqueId());
                if (keys != null) {
                    Set<Integer> ids = new HashSet<>();
                    for (String key : keys) {
                        BlockData data = blockDataMap.get(key);
                        if (data != null) {
                            ids.add(data.id);
                        }
                    }
                    for (int id : ids) {
                        String idString = String.valueOf(id);
                        if (idString.startsWith(args[1])) {
                            result.add(idString);
                        }
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private static String hexToMinecraft(String hex6) {
        if (hex6 == null || hex6.length() != 6) {
            return "";
        }
        StringBuilder builder = new StringBuilder(14);
        builder.append('\u00A7').append('x');
        for (int i = 0; i < 6; i++) {
            builder.append('\u00A7').append(hex6.charAt(i));
        }
        return builder.toString();
    }

    private Block getTargetBlock(Player player, int distance) {
        initTargetBlockExactReflection();
        if (cachedGetTargetBlockExact != null) {
            try {
                return (Block) cachedGetTargetBlockExact.invoke(player, distance);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        @SuppressWarnings("deprecation")
        Block block = player.getTargetBlock((Set<Material>) null, distance);
        return block;
    }
}
