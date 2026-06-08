package eu.client.pingbypass.server;

import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.s2c.config.DynamicRegistriesS2CPacket;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagPacketSerializer;
import net.minecraft.resource.*;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

/**
 * Loads vanilla registries and tags at proxy startup, caches serialized packets.
 * Does the same work as SaveLoading.load() but without DataPackContents to avoid
 * Fabric mixin interference.
 */
public class RegistryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryCache.class);

    private final List<Packet<?>> registryPackets = new ArrayList<>();
    private final List<VersionedIdentifier> knownPacks = new ArrayList<>();
    private CombinedDynamicRegistries<ServerDynamicRegistryType> combinedRegistries;
    private DynamicRegistryManager.Immutable registryManager;
    private boolean loaded;
    private volatile boolean loadAttempted;

    public void load() {
        LOGGER.info("Loading vanilla registries for proxy...");
        long start = System.currentTimeMillis();

        try {
            // Create resource pack manager with vanilla data packs
            ResourcePackManager packManager = VanillaDataPackProvider.createManager(
                    java.nio.file.Files.createTempDirectory("pb-datapacks"),
                    new net.minecraft.util.path.SymlinkFinder(path -> true));

            // Load data packs
            DataConfiguration dataConfig = MinecraftServer.loadDataPacks(
                    packManager, DataConfiguration.SAFE_MODE, true, false);
            List<ResourcePack> packs = packManager.createResourcePacks();
            LifecycledResourceManager resourceManager = new LifecycledResourceManagerImpl(
                    ResourceType.SERVER_DATA, packs);

            // Build registries (same as SaveLoading.load)
            CombinedDynamicRegistries<ServerDynamicRegistryType> combined =
                    ServerDynamicRegistryType.createCombinedDynamicRegistries();

            // Load tags for static registries
            List<Registry.PendingTagLoad<?>> pendingTags = TagGroupLoader.startReload(
                    resourceManager, combined.get(ServerDynamicRegistryType.STATIC));

            // Create wrappers with tags applied (doesn't modify original registries)
            DynamicRegistryManager.Immutable preceding =
                    combined.getPrecedingRegistryManagers(ServerDynamicRegistryType.WORLDGEN);
            List<RegistryWrapper.Impl<?>> taggedWrappers =
                    TagGroupLoader.collectRegistries(preceding, pendingTags);

            // Load dynamic registries (biome, dimension_type, enchantment, etc.)
            DynamicRegistryManager.Immutable worldgenRegistries = RegistryLoader.loadFromResource(
                    resourceManager, taggedWrappers, RegistryLoader.DYNAMIC_REGISTRIES);

            List<RegistryWrapper.Impl<?>> allWrappers =
                    Stream.concat(taggedWrappers.stream(), worldgenRegistries.stream()).toList();

            // Load dimension registries
            DynamicRegistryManager.Immutable dimensionRegistries = RegistryLoader.loadFromResource(
                    resourceManager, allWrappers, RegistryLoader.DIMENSION_REGISTRIES);

            // Combine all registries
            combinedRegistries = combined.with(
                    ServerDynamicRegistryType.WORLDGEN, worldgenRegistries, dimensionRegistries);

            // Apply pending tag loads to get tags bound to registries
            for (Registry.PendingTagLoad<?> pending : pendingTags) {
                pending.apply();
            }

            registryManager = combinedRegistries.getCombinedRegistryManager();

            // Collect known packs
            knownPacks.addAll(
                    resourceManager.streamResourcePacks()
                            .flatMap(pack -> pack.getInfo().knownPackInfo().stream())
                            .toList());

            // Serialize registry packets
            com.mojang.serialization.DynamicOps<NbtElement> ops = registryManager.getOps(NbtOps.INSTANCE);
            SerializableRegistries.forEachSyncedRegistry(ops,
                    combinedRegistries.getSucceedingRegistryManagers(ServerDynamicRegistryType.WORLDGEN),
                    Set.of(),
                    (key, entries) -> registryPackets.add(new DynamicRegistriesS2CPacket(key, entries)));

            // Serialize tags
            registryPackets.add(new SynchronizeTagsS2CPacket(
                    TagPacketSerializer.serializeTags(combinedRegistries)));

            loaded = true;
            resourceManager.close();

            long elapsed = System.currentTimeMillis() - start;
            LOGGER.info("Loaded {} registry packets in {}ms", registryPackets.size(), elapsed);
        } catch (Exception e) {
            LOGGER.error("Failed to load vanilla registries", e);
        }
    }

    public boolean isLoaded() {
        if (!loaded && !loadAttempted) {
            loadAttempted = true;
            load();
        }
        return loaded;
    }

    public List<Packet<?>> getRegistryPackets() { return registryPackets; }
    public List<VersionedIdentifier> getKnownPacks() { return knownPacks; }
    public CombinedDynamicRegistries<ServerDynamicRegistryType> getCombinedRegistries() { return combinedRegistries; }
    public DynamicRegistryManager.Immutable getRegistryManager() { return registryManager; }
}
