// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

final class NativeDeProfileKernel {
    private NativeDeProfileKernel() {
    }

    static void initialize(Target target) {
        try {
            int n = target.aiType();
            switch (n) {
                case 1: {
                    if (!target.shop()) {
                        target.createShop();
                    }
                    target.startShop();
                    break;
                }
                case 2: {
                    if (!target.fishing()) {
                        target.createFishing();
                    }
                    target.startAi();
                    break;
                }
                case 0: 
                case 3: 
                case 4: 
                case 6: 
                case 7: {
                    if (!target.combat()) {
                        target.createCombat(switch (n) {
                            case 0 -> Combat.IDLE;
                            case 3 -> Combat.PATROL;
                            case 4 -> Combat.HUNT;
                            case 6 -> Combat.BERSERK;
                            case 7 -> Combat.DEATHMATCH;
                            default -> throw new AssertionError();
                        });
                    }
                    target.startAi();
                    break;
                }
                case 5: {
                    target.startAi();
                    break;
                }
            }
        }
        catch (Exception exception) {
            target.error(exception);
        }
    }

    static interface Target {
        public int aiType();

        public boolean shop();

        public void createShop();

        public void startShop();

        public boolean fishing();

        public void createFishing();

        public boolean combat();

        public void createCombat(Combat var1);

        public void startAi();

        public void error(Exception var1);
    }

    static enum Combat {
        NONE,
        IDLE,
        PATROL,
        HUNT,
        BERSERK,
        DEATHMATCH,
        SIEGE,
        CUSTOM;

    }
}
