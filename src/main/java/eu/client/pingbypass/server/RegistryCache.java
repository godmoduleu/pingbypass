package eu.client.pingbypass.server;

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
            ResourcePackManager packManager = VanillaDataPackProvider.createManager(
                    java.nio.file.Files.createTempDirectory("pb-datapacks"),
                    new net.minecraft.util.path.SymlinkFinder(path -> true));

            DataConfiguration dataConfig = MinecraftServer.loadDataPacks(
                    packManager, DataConfiguration.SAFE_MODE, true, false);
            List<ResourcePack> packs = packManager.createResourcePacks();
            LifecycledResourceManager resourceManager = new LifecycledResourceManagerImpl(
                    ResourceType.SERVER_DATA, packs);

            CombinedDynamicRegistries<ServerDynamicRegistryType> combined =
                    ServerDynamicRegistryType.createCombinedDynamicRegistries();

            List<Registry.PendingTagLoad<?>> pendingTags = TagGroupLoader.startReload(
                    resourceManager, combined.get(ServerDynamicRegistryType.STATIC));

            DynamicRegistryManager.Immutable preceding =
                    combined.getPrecedingRegistryManagers(ServerDynamicRegistryType.WORLDGEN);
            List<RegistryWrapper.Impl<?>> taggedWrappers =
                    TagGroupLoader.collectRegistries(preceding, pendingTags);

            DynamicRegistryManager.Immutable worldgenRegistries = RegistryLoader.loadFromResource(
                    resourceManager, taggedWrappers, RegistryLoader.DYNAMIC_REGISTRIES);

            List<RegistryWrapper.Impl<?>> allWrappers =
                    Stream.concat(taggedWrappers.stream(), worldgenRegistries.stream()).toList();

            DynamicRegistryManager.Immutable dimensionRegistries = RegistryLoader.loadFromResource(
                    resourceManager, allWrappers, RegistryLoader.DIMENSION_REGISTRIES);

            combinedRegistries = combined.with(
                    ServerDynamicRegistryType.WORLDGEN, worldgenRegistries, dimensionRegistries);

            for (Registry.PendingTagLoad<?> pending : pendingTags) {
                pending.apply();
            }

            registryManager = combinedRegistries.getCombinedRegistryManager();

            knownPacks.addAll(
                    resourceManager.streamResourcePacks()
                            .flatMap(pack -> pack.getInfo().knownPackInfo().stream())
                            .toList());

            com.mojang.serialization.DynamicOps<NbtElement> ops = registryManager.getOps(NbtOps.INSTANCE);
            SerializableRegistries.forEachSyncedRegistry(ops,
                    combinedRegistries.getSucceedingRegistryManagers(ServerDynamicRegistryType.WORLDGEN),
                    Set.of(),
                    (key, entries) -> registryPackets.add(new DynamicRegistriesS2CPacket(key, entries)));

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
