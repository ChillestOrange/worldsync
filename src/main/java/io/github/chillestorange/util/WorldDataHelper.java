package io.github.chillestorange.util;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Utilities for working with world data specific to Minecraft
 */
public final class WorldDataHelper {

    private static final String DATA_TAG = "Data";
    private static final String SINGLEPLAYER_UUID_TAG = "singleplayer_uuid";

    private WorldDataHelper() {
    }

    /**
     * Rewrites the singleplayer_uuid field in level.dat.
     *
     * @param levelDatPath the path to the level.dat file
     * @param uuid         UUID to use in singleplayer_uuid
     * @throws NullPointerException  if levelDatPath or uuid is null
     * @throws IOException           if it is impossible to read or write level.dat
     * @throws IllegalStateException if level.dat does not contain the Data compound
     */
    public static void updateSingleplayerUuid(Path levelDatPath, UUID uuid) throws IOException {

        Objects.requireNonNull(levelDatPath, "levelDatPath cannot be null");
        Objects.requireNonNull(uuid, "uuid cannot be null");

        CompoundTag root = NbtIo.readCompressed(levelDatPath, NbtAccounter.unlimitedHeap());
        CompoundTag data = root.getCompound(DATA_TAG)
                .orElseThrow(() -> new IllegalStateException(
                        "Invalid level.dat: missing '" + DATA_TAG
                                + "' compound (" + levelDatPath + ')'
                ));

        data.storeNullable(SINGLEPLAYER_UUID_TAG, UUIDUtil.CODEC, uuid);

        NbtIo.writeCompressed(root, levelDatPath);
    }
}