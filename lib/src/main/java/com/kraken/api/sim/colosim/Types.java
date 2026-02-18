package com.kraken.api.sim.colosim;

public class Types {
    public static class Coordinates {
        public int x;
        public int y;

        public Coordinates(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class Mob {
        public int x;
        public int y;
        public int type;
        public int spawnX;
        public int spawnY;
        public int cooldown;
        public String extra;
        public String originalExtra;

        public Mob(int x, int y, int type, int spawnX, int spawnY, int cooldown, String extra, String originalExtra) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.cooldown = cooldown;
            this.extra = extra;
            this.originalExtra = originalExtra;
        }
    }

    public static class MobSpec {
        public int spawnX;
        public int spawnY;
        public int type;
        public String extra;

        public MobSpec(int spawnX, int spawnY, int type, String extra) {
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.type = type;
            this.extra = extra;
        }
    }
}
