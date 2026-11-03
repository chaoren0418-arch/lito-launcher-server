// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

record SourceSummonTemplate(int npcId, String name, String nameId, int gfx, int level, int maxHp, int maxMp, int armorClass, int strength, int dexterity, int moveIntervalMillis, int attackIntervalMillis) {
    SourceSummonTemplate {
        if (!SourceSummonTemplate.supportedNpcId(npcId) || name == null || name.isBlank() || nameId == null || nameId.isBlank() || gfx <= 0 || gfx > 65535 || level <= 0 || level > 255 || maxHp <= 0 || maxMp < 0 || strength <= 0 || dexterity <= 0 || moveIntervalMillis <= 0 || attackIntervalMillis <= 0) {
            throw new IllegalArgumentException("invalid source summon template");
        }
    }

    static SourceSummonTemplate memoryFixture(int npcId) {
        return switch (npcId) {
            case 45303 -> SourceSummonTemplate.source(npcId, "\u706b\u4e4b\u7cbe\u9748", "$2004", 2276, 30, 400, 0, -11, 16, 18, 640, 1000);
            case 45304 -> SourceSummonTemplate.source(npcId, "\u6c34\u4e4b\u7cbe\u9748", "$2005", 2274, 30, 400, 0, -13, 16, 18, 640, 1000);
            case 45305 -> SourceSummonTemplate.source(npcId, "\u98a8\u4e4b\u7cbe\u9748", "$2006", 2275, 30, 400, 0, -13, 16, 18, 640, 1000);
            case 45306 -> SourceSummonTemplate.source(npcId, "\u571f\u4e4b\u7cbe\u9748", "$2007", 2273, 30, 400, 0, -15, 16, 18, 640, 1000);
            case 81050 -> SourceSummonTemplate.source(npcId, "\u5f37\u529b\u706b\u4e4b\u7cbe\u9748", "$2008", 2266, 45, 600, 60, -18, 20, 18, 640, 980);
            case 81051 -> SourceSummonTemplate.source(npcId, "\u5f37\u529b\u6c34\u4e4b\u7cbe\u9748", "$2009", 2268, 45, 600, 60, -18, 18, 18, 640, 980);
            case 81052 -> SourceSummonTemplate.source(npcId, "\u5f37\u529b\u98a8\u4e4b\u7cbe\u9748", "$2010", 2282, 45, 720, 60, -18, 17, 18, 640, 980);
            case 81053 -> SourceSummonTemplate.source(npcId, "\u5f37\u529b\u571f\u4e4b\u7cbe\u9748", "$2011", 2267, 45, 650, 60, -18, 19, 18, 640, 980);
            case 81210 -> SourceSummonTemplate.source(npcId, "\u54c8\u67cf\u54e5\u5e03\u6797", "$1111", 1206, 20, 300, 20, 1, 13, 13, 640, 960);
            case 81211 -> SourceSummonTemplate.source(npcId, "\u827e\u591a\u502b", "$1043", 1150, 20, 180, 20, 10, 11, 11, 320, 960);
            case 81212 -> SourceSummonTemplate.source(npcId, "\u5b89\u666e", "$1044", 1152, 20, 120, 60, 6, 9, 14, 480, 1520);
            case 81213 -> SourceSummonTemplate.source(npcId, "\u7518\u5730\u5996\u9b54", "$496", 784, 24, 350, 30, 0, 10, 10, 640, 1040);
            case 81214 -> SourceSummonTemplate.source(npcId, "\u90fd\u9054\u746a\u62c9\u5996\u9b54", "$498", 788, 24, 210, 30, -5, 12, 14, 640, 1160);
            case 81215 -> SourceSummonTemplate.source(npcId, "\u5996\u9b54\u5de1\u5b88", "$511", 790, 24, 140, 70, -3, 14, 14, 640, 1520);
            case 81216 -> SourceSummonTemplate.source(npcId, "\u72c2\u91ce\u6bd2\u7259", "$3379", 4207, 28, 400, 40, -6, 17, 13, 640, 920);
            case 81217 -> SourceSummonTemplate.source(npcId, "\u72c2\u91ce\u4e4b\u6bd2", "$3378", 4205, 28, 240, 80, 0, 16, 14, 640, 680);
            case 81218 -> SourceSummonTemplate.source(npcId, "\u72c2\u91ce\u4e4b\u9b54", "$3380", 4203, 28, 160, 80, -10, 18, 12, 640, 880);
            case 81219 -> SourceSummonTemplate.source(npcId, "\u98df\u4eba\u5996\u7cbe", "$325", 152, 32, 450, 50, -6, 19, 12, 640, 840);
            case 81220 -> SourceSummonTemplate.source(npcId, "\u98df\u4eba\u5996\u7cbe\u738b", "$937", 894, 32, 270, 50, -8, 21, 12, 640, 720);
            case 81221 -> SourceSummonTemplate.source(npcId, "\u51b0\u4eba", "$1791", 2158, 32, 180, 90, -20, 17, 17, 960, 1600);
            case 81222 -> SourceSummonTemplate.source(npcId, "\u72c2\u66b4\u8725\u8734\u4eba", "$3381", 4149, 36, 500, 50, -4, 14, 16, 640, 960);
            case 81223 -> SourceSummonTemplate.source(npcId, "\u91cd\u88dd\u8725\u8734\u4eba", "$3383", 4151, 36, 300, 50, -15, 14, 18, 640, 1200);
            case 81224 -> SourceSummonTemplate.source(npcId, "\u9ad8\u7b49\u8725\u8734\u4eba", "$3382", 4192, 36, 200, 100, -10, 10, 16, 640, 1000);
            case 81225 -> SourceSummonTemplate.source(npcId, "\u706b\u8725\u8734", "$1505", 1666, 40, 550, 60, -23, 17, 15, 640, 1080);
            case 81226 -> SourceSummonTemplate.source(npcId, "\u706b\u7130\u6230\u58eb", "$1566", 1770, 40, 330, 60, -11, 15, 11, 960, 1520);
            case 81227 -> SourceSummonTemplate.source(npcId, "\u706b\u7130\u5f13\u7bad\u624b", "$1572", 1832, 40, 220, 110, -5, 13, 16, 960, 2000);
            case 81228 -> SourceSummonTemplate.source(npcId, "\u9b54\u718a", "$2893", 3357, 44, 600, 40, -25, 14, 12, 426, 840);
            case 81229 -> SourceSummonTemplate.source(npcId, "\u9b54\u72fc", "$2105", 2392, 48, 360, 70, -30, 22, 18, 480, 1440);
            case 81230 -> SourceSummonTemplate.source(npcId, "\u9b54\u8759\u8760", "$2892", 3415, 48, 240, 120, -6, 19, 12, 853, 1560);
            case 81231 -> SourceSummonTemplate.source(npcId, "\u5de8\u5927\u5b88\u8b77\u879e\u87fb", "$5285", 6290, 48, 650, 60, -48, 20, 20, 480, 880);
            case 81232 -> SourceSummonTemplate.source(npcId, "\u5f37\u5316\u767d\u879e\u87fb\u7fa4", "$5282", 6161, 48, 390, 60, -12, 20, 20, 640, 880);
            case 81233 -> SourceSummonTemplate.source(npcId, "\u5de8\u5927\u5f37\u5316\u767d\u879e\u87fb", "$5284", 6302, 48, 260, 130, -24, 15, 13, 640, 880);
            case 81234 -> SourceSummonTemplate.source(npcId, "\u5730\u7344\u5974\u96b8", "$3812", 4622, 52, 700, 60, -41, 16, 15, 960, 960);
            case 81235 -> SourceSummonTemplate.source(npcId, "\u95c7\u7cbe\u9748\u738b", "$2896", 3365, 52, 420, 60, -40, 17, 16, 960, 1400);
            case 81236 -> SourceSummonTemplate.source(npcId, "\u98df\u8150\u7378", "$4306", 5442, 52, 280, 140, -50, 15, 20, 960, 1120);
            case 81237 -> SourceSummonTemplate.source(npcId, "\u5730\u7344\u675f\u7e1b\u72ac", "$3811", 4643, 50, 657, 278, -40, 16, 15, 640, 1040);
            case 81238 -> SourceSummonTemplate.source(npcId, "\u8b8a\u5f62\u602a", "$1554", 2389, 58, 1000, 100, -30, 20, 18, 1040, 1440);
            case 81239 -> SourceSummonTemplate.source(npcId, "\u5de8\u5927\u725b\u4eba", "$2106", 2402, 58, 1000, 100, -65, 30, 12, 960, 1520);
            case 81240 -> SourceSummonTemplate.source(npcId, "\u9ed1\u8c79", "$2587", 3049, 80, 4000, 400, -80, 85, 63, 480, 440);
            default -> throw new IllegalArgumentException("unknown memory summon NPC id: " + npcId);
        };
    }

    static boolean supportedNpcId(int npcId) {
        return npcId >= 45303 && npcId <= 45306 || npcId >= 81050 && npcId <= 81053 || npcId >= 81210 && npcId <= 81240;
    }

    private static SourceSummonTemplate source(int npcId, String name, String nameId, int gfx, int level, int maxHp, int maxMp, int armorClass, int strength, int dexterity, int moveIntervalMillis, int attackIntervalMillis) {
        return new SourceSummonTemplate(npcId, name, nameId, gfx, level, maxHp, maxMp, armorClass, strength, dexterity, moveIntervalMillis, attackIntervalMillis);
    }
}
