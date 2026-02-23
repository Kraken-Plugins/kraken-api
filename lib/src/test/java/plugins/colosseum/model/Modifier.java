package plugins.colosseum.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;

import java.util.EnumSet;
import java.util.Set;

@RequiredArgsConstructor
public enum Modifier {

    BEES("Bees!", new int[]{5544, 5559, 5574}, 2, 9791),
    BLASPHEMY("Blasphemy", new int[]{5538, 5553, 5568}, 4, 9790),
    DOOM("Doom", new int[]{5543, 5558, 5573}, 8, 10681),
    DYNAMIC_DUO("Dynamic Duo", new int[]{5545}, 9, -1),
    FRAILTY("Frailty", new int[]{5541, 5556, 5571}, 12, 9796),
    MANTIMAYHEM("Mantimayhem", new int[]{5539, 5554, 5569}, 0, 4588),
    MYOPIA("Myopia", new int[]{5547, 5562, 5577}, 11, 9795),
    REENTRY("Reentry", new int[]{5536, 5551, 5566}, 1, 9792),
    RED_FLAG("Red Flag", new int[]{5540}, 13, -1),
    RELENTLESS("Relentless", new int[]{5535, 5550, 5565}, 5, 9798),
    SOLARFLARE("Solarflare", new int[]{5537, 5552, 5567}, 10, 9797),
    QUARTET("Quartet", new int[]{5546}, 6, -1),
    TOTEMIC("Totemic", new int[]{5542}, 7, -1),
    VOLATILITY("Volatility", new int[]{5534, 5549, 5564}, 3, 9799);

    private final String name;
    private final int[] spriteIds;

    @Getter
    private final int id;

    @Getter
    private final int levelVarb;

    public int getLevel(Client client) {
        if (levelVarb == -1) {
            return 1;
        }

        try {
            return client.getVarbitValue(levelVarb);
        } catch (Exception e) {
            return 1;
        }
    }

    public String getName(int level) {
        if (levelVarb == -1) {
            return name;
        }

        return level == 3 ? name + " (III)"
                : level == 2 ? name + " (II)"
                : name;
    }

    public String getName(Client client) {
        return getName(getLevel(client));
    }

    public int getSpriteId(int level) {
        return spriteIds[Math.max(0, level - 1)];
    }

    public int getSpriteId(Client client) {
        return getSpriteId(getLevel(client));
    }

    public static Modifier forId(int id) {
        for (Modifier h : values()) {
            if (h.id == id) {
                return h;
            }
        }

        return null;
    }

    public static Set<Modifier> forBitmask(int bits) {
        Set<Modifier> ret = EnumSet.noneOf(Modifier.class);
        for (Modifier h : values()) {
            if ((bits & (1 << h.id)) != 0) {
                ret.add(h);
            }
        }

        return ret;
    }

}
