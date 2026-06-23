package io.github.chillestorange.service.sync;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Replaces main.exe + NBT_explorer.hpp (reading) and Comparator.exe (comparing)
 * in one place — these three pieces (the data shape, reading it, and comparing
 * two readings) are only ever used together in sequence, so there's no benefit
 * to splitting them across files.
 * <p>
 * NbtReader is nested here as a private helper rather than a standalone file —
 * it has exactly one caller (read() below), and keeping a general-purpose binary
 * parser in its own file implied reusability it doesn't actually have. If you'd
 * rather use Minecraft's own NbtIo since it's already on the classpath, swap
 * NbtReader.readGzipCompound's body for NbtIo.readCompressed(...) adjusted for
 * your exact MC version's mapping.
 */
public final class LevelSync {

    public record Summary(String levelName, long time, long lastPlayed, int dataVersion) {}

    private LevelSync() {}

    @SuppressWarnings("unchecked")
    public static Summary read(Path levelDatPath) throws IOException {
        Map<String, Object> root = NbtReader.readGzipCompound(levelDatPath);

        Object dataObj = root.get("Data");
        if (!(dataObj instanceof Map)) {
            throw new IOException("level.dat has no 'Data' compound: " + levelDatPath);
        }
        Map<String, Object> data = (Map<String, Object>) dataObj;

        String levelName = (String) data.get("LevelName");
        long time = ((Number) data.get("Time")).longValue();
        long lastPlayed = ((Number) data.get("LastPlayed")).longValue();
        int dataVersion = ((Number) data.get("DataVersion")).intValue();

        return new Summary(levelName, time, lastPlayed, dataVersion);
    }

    public static SyncDirection compare(Summary local, Summary remote) {
        if (!local.levelName().equals(remote.levelName())) {
            // The original C++ bailed to "-1" here too, with no separate "wrong
            // world" signal. Worth revisiting later if you want a distinct
            // warning instead of a silent no-op.
            return SyncDirection.NO_OP;
        }

        if (local.time() == remote.time()
                && local.lastPlayed() == remote.lastPlayed()
                && local.dataVersion() == remote.dataVersion()) {
            return SyncDirection.NO_OP;
        }

        if (local.time() > remote.time()) return SyncDirection.UPLOAD;
        if (local.time() < remote.time()) return SyncDirection.DOWNLOAD;

        // Ticks equal but lastPlayed/dataVersion differ — the original C++ has
        // no explicit branch for this either (falls through main() without
        // printing anything), matching that here.
        return SyncDirection.NO_OP;
    }

    // ── NBT reader ────────────────────────────────────────────────────────────

    /**
     * Minimal, dependency-free NBT binary parser. Handles all 12 tag types,
     * gzip-wrapped, big-endian — exactly the format Minecraft's level.dat uses.
     * Nested here because it has one caller; keeping it standalone implied a
     * reusability that wasn't real.
     */
    private static final class NbtReader {

        private static final int TAG_END = 0, TAG_BYTE = 1, TAG_SHORT = 2, TAG_INT = 3,
                TAG_LONG = 4, TAG_FLOAT = 5, TAG_DOUBLE = 6, TAG_BYTE_ARRAY = 7,
                TAG_STRING = 8, TAG_LIST = 9, TAG_COMPOUND = 10, TAG_INT_ARRAY = 11, TAG_LONG_ARRAY = 12;

        private NbtReader() {}

        @SuppressWarnings("unchecked")
        static Map<String, Object> readGzipCompound(Path path) throws IOException {
            try (InputStream in = Files.newInputStream(path);
                 DataInputStream dis = new DataInputStream(new GZIPInputStream(in))) {

                int type = dis.readUnsignedByte();
                if (type != TAG_COMPOUND) {
                    throw new IOException("Expected root TAG_Compound, got tag " + type + " in " + path);
                }
                readString(dis); // root tag name, normally empty — discarded
                return (Map<String, Object>) readPayload(dis, TAG_COMPOUND);
            }
        }

        private static Object readPayload(DataInputStream dis, int type) throws IOException {
            switch (type) {
                case TAG_BYTE: return dis.readByte();
                case TAG_SHORT: return dis.readShort();
                case TAG_INT: return dis.readInt();
                case TAG_LONG: return dis.readLong();
                case TAG_FLOAT: return dis.readFloat();
                case TAG_DOUBLE: return dis.readDouble();
                case TAG_BYTE_ARRAY: {
                    int len = dis.readInt();
                    byte[] arr = new byte[len];
                    dis.readFully(arr);
                    return arr;
                }
                case TAG_STRING:
                    return readString(dis);
                case TAG_LIST: {
                    int elementType = dis.readUnsignedByte();
                    int len = dis.readInt();
                    Object[] list = new Object[len];
                    for (int i = 0; i < len; i++) {
                        list[i] = elementType == TAG_END ? null : readPayload(dis, elementType);
                    }
                    return list;
                }
                case TAG_COMPOUND: {
                    Map<String, Object> map = new LinkedHashMap<>();
                    while (true) {
                        int childType = dis.readUnsignedByte();
                        if (childType == TAG_END) break;
                        String name = readString(dis);
                        map.put(name, readPayload(dis, childType));
                    }
                    return map;
                }
                case TAG_INT_ARRAY: {
                    int len = dis.readInt();
                    int[] arr = new int[len];
                    for (int i = 0; i < len; i++) arr[i] = dis.readInt();
                    return arr;
                }
                case TAG_LONG_ARRAY: {
                    int len = dis.readInt();
                    long[] arr = new long[len];
                    for (int i = 0; i < len; i++) arr[i] = dis.readLong();
                    return arr;
                }
                default:
                    throw new IOException("Unsupported NBT tag type: " + type);
            }
        }

        private static String readString(DataInputStream dis) throws IOException {
            int len = dis.readUnsignedShort();
            byte[] bytes = new byte[len];
            dis.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}