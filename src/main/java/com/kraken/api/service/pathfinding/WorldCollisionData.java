package com.kraken.api.service.pathfinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.runelite.api.Constants.REGION_SIZE;

/**
 * The walkability of every tile in the game world, read from the collision map bundled with the
 * API.
 *
 * <p>The map is {@code /walker/collision-map.zip}, taken from the shortest-path project. It holds
 * one entry per map region, named {@code <regionX>_<regionY>}, whose bytes are a bit set with four
 * flags per tile. The first two say whether the tile's north edge and east edge can be walked
 * across, and those are the two the planner reads. The other two mark walls and are not used.</p>
 *
 * <p>All regions share one array of words, so the whole map is a handful of arrays. Loading takes
 * a moment, so the bundled map is read once and shared by every search.</p>
 */
final class WorldCollisionData {

    /** Where the collision map is read from on the classpath. */
    static final String RESOURCE_PATH = "/walker/collision-map.zip";

    /** The flag that says a tile's north edge can be walked across. */
    static final int FLAG_NORTH = 0;

    /** The flag that says a tile's east edge can be walked across. */
    static final int FLAG_EAST = 1;

    private static final int FLAG_COUNT = 4;
    private static final int BITS_PER_PLANE = REGION_SIZE * REGION_SIZE * FLAG_COUNT;
    private static final int WORDS_PER_PLANE = BITS_PER_PLANE / Long.SIZE;
    private static final int REGION_MASK = REGION_SIZE - 1;

    /**
     * The bundled map once it has been read. A read that fails leaves this empty, so the next
     * search tries again and fails with the same exception instead of a class that cannot load.
     */
    private static volatile WorldCollisionData bundled;

    private final int minRegionX;
    private final int minRegionY;
    private final int widthInclusive;
    private final byte[] planeCounts;
    private final long[] flags;
    private final int[] regionWordOffset;

    /**
     * Builds collision data from the bit set of each region.
     *
     * @param regions the bytes of each region's bit set, keyed by {@link #regionKey(int, int)}
     */
    WorldCollisionData(Map<Integer, byte[]> regions) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        for (int key : regions.keySet()) {
            minX = Math.min(minX, key & 0xFFFF);
            minY = Math.min(minY, (key >> 16) & 0xFFFF);
            maxX = Math.max(maxX, key & 0xFFFF);
            maxY = Math.max(maxY, (key >> 16) & 0xFFFF);
        }
        if (regions.isEmpty()) {
            minX = 0;
            minY = 0;
        }

        minRegionX = minX;
        minRegionY = minY;
        widthInclusive = maxX - minX + 1;
        int regionCount = widthInclusive * (maxY - minY + 1);
        planeCounts = new byte[regionCount];
        regionWordOffset = new int[regionCount];
        Arrays.fill(regionWordOffset, -1);

        // Each region is decoded and given a slice of the shared array, then copied into it. A bit
        // set drops its trailing zero words, which the zero filled slice already stands for.
        Map<Integer, long[]> regionWords = new HashMap<>(regions.size());
        int totalWords = 0;
        for (Map.Entry<Integer, byte[]> entry : regions.entrySet()) {
            int index = regionIndex(entry.getKey() & 0xFFFF, (entry.getKey() >> 16) & 0xFFFF);
            BitSet bits = BitSet.valueOf(entry.getValue());
            int planeCount = (bits.size() + BITS_PER_PLANE - 1) / BITS_PER_PLANE;
            planeCounts[index] = (byte) planeCount;
            regionWordOffset[index] = totalWords;
            regionWords.put(index, bits.toLongArray());
            totalWords += planeCount * WORDS_PER_PLANE;
        }

        flags = new long[totalWords];
        for (Map.Entry<Integer, long[]> entry : regionWords.entrySet()) {
            long[] words = entry.getValue();
            System.arraycopy(words, 0, flags, regionWordOffset[entry.getKey()], words.length);
        }
    }

    /**
     * Returns the collision map bundled with the API, reading it on first use.
     *
     * @return the shared collision data
     * @throws UncheckedIOException when the bundled map is missing or cannot be read
     */
    static WorldCollisionData bundled() {
        WorldCollisionData data = bundled;
        if (data == null) {
            synchronized (WorldCollisionData.class) {
                data = bundled;
                if (data == null) {
                    InputStream stream = WorldCollisionData.class.getResourceAsStream(RESOURCE_PATH);
                    if (stream == null) {
                        throw new UncheckedIOException(new IOException("Resource not found: " + RESOURCE_PATH));
                    }
                    data = read(stream);
                    bundled = data;
                }
            }
        }
        return data;
    }

    /**
     * Reads collision data from a zip of region bit sets.
     *
     * @param zip the zip to read, which this closes
     * @return the collision data
     * @throws UncheckedIOException when the zip cannot be read
     */
    static WorldCollisionData read(InputStream zip) {
        Map<Integer, byte[]> regions = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(zip)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = in.getNextEntry()) != null) {
                String[] name = entry.getName().split("_");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                int read;
                while ((read = in.read(buffer)) > 0) {
                    bytes.write(buffer, 0, read);
                }
                regions.put(regionKey(Integer.parseInt(name[0]), Integer.parseInt(name[1])), bytes.toByteArray());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new WorldCollisionData(regions);
    }

    /**
     * Makes the key a region's bit set is stored under.
     *
     * @param regionX the region's x, which is a world x divided by 64
     * @param regionY the region's y
     * @return the key
     */
    static int regionKey(int regionX, int regionY) {
        return (regionX & 0xFFFF) | ((regionY & 0xFFFF) << 16);
    }

    /**
     * Reads one flag of one tile.
     *
     * @param x the world x
     * @param y the world y
     * @param plane the plane
     * @param flag {@link #FLAG_NORTH} or {@link #FLAG_EAST}
     * @return true when the flag is set, false when it is not or the map has no such tile
     */
    boolean get(int x, int y, int plane, int flag) {
        int index = regionIndex(x / REGION_SIZE, y / REGION_SIZE);
        if (index < 0 || index >= regionWordOffset.length) {
            return false;
        }

        int wordOffset = regionWordOffset[index];
        if (wordOffset < 0 || plane < 0 || plane >= planeCounts[index]) {
            return false;
        }

        int localBit = (plane * REGION_SIZE * REGION_SIZE
                + (y & REGION_MASK) * REGION_SIZE
                + (x & REGION_MASK)) * FLAG_COUNT + flag;
        return (flags[wordOffset + (localBit >> 6)] >>> (localBit & 63) & 1L) != 0L;
    }

    /**
     * Returns how many planes a region has data for.
     *
     * @param regionIndex the region's index, as {@link #regionIndex(int, int)} gives it
     * @return the number of planes
     */
    byte planeCount(int regionIndex) {
        return planeCounts[regionIndex];
    }

    /**
     * Returns how many regions the map's grid spans, which is one more than its largest index.
     *
     * @return the size of the region grid
     */
    int regionCount() {
        return regionWordOffset.length;
    }

    /**
     * Finds a region's place in the map's grid.
     *
     * @param regionX the region's x
     * @param regionY the region's y
     * @return the index, which is outside 0 to {@link #regionCount()} for a region off the grid
     */
    int regionIndex(int regionX, int regionY) {
        if (regionX < minRegionX || regionX >= minRegionX + widthInclusive) {
            return -1;
        }
        return (regionX - minRegionX) + (regionY - minRegionY) * widthInclusive;
    }

}
