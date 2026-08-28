package colosim;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SpriteStore {
    private final Map<Integer, BufferedImage> byNpcType = new HashMap<Integer, BufferedImage>();

    public SpriteStore() {
        register(NpcType.PLAYER, "player.png");
        register(NpcType.SERPENT_SHAMAN, "serpent_shaman.png");
        register(NpcType.REINFORCEMENT_SHAMAN, "serpent_shaman.png");
        register(NpcType.JAVELIN_COLOSSUS, "javelin_colossus.png");
        register(NpcType.JAGUAR_WARRIOR, "jaguar_warrior.png");
        register(NpcType.MANTICORE, "manticore.png");
        register(NpcType.MINOTAUR, "minotaur.png");
        register(NpcType.SHOCKWAVE_COLOSSUS, "shockwave_colossus.png");
    }

    public BufferedImage get(int npcType) {
        return byNpcType.get(npcType);
    }

    private void register(NpcType type, String fileName) {
        BufferedImage image = load(fileName);
        if (image != null) {
            byNpcType.put(type.typeId, image);
        }
    }

    private BufferedImage load(String fileName) {
        String[] classpathCandidates = {
                "/sim/" + fileName,
                "/resources/sim/" + fileName
        };
        for (String path : classpathCandidates) {
            try (InputStream input = SpriteStore.class.getResourceAsStream(path)) {
                if (input != null) {
                    return ImageIO.read(input);
                }
            } catch (IOException ignored) {
            }
        }

        String[] fileCandidates = {
                "src/main/resources/sim/" + fileName,
                "resources/sim/" + fileName,
                "public/" + fileName
        };
        for (String path : fileCandidates) {
            try {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    return ImageIO.read(file);
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public Image scale(BufferedImage image, int width, int height) {
        if (image == null) {
            return null;
        }
        return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
}
