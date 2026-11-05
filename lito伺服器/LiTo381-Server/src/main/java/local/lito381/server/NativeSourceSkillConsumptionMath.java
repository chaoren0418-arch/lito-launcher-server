// DECOMPILED from base-0.6.71.jar, TODO review
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

final class NativeSourceSkillConsumptionMath {
    static int mpCost(int n, int n2, int n3, int n4, boolean bl) {
        int n5 = n2;
        if (n3 > 12 && n > 8 && n <= 80) {
            --n5;
        }
        if (n3 > 13 && n > 16 && n <= 80) {
            --n5;
        }
        if (n3 > 14 && n > 23 && n <= 80) {
            --n5;
        }
        if (n3 > 15 && n > 32 && n <= 80) {
            --n5;
        }
        if (n3 > 16 && n > 40 && n <= 80) {
            --n5;
        }
        if (n3 > 17 && n > 48 && n <= 80) {
            --n5;
        }
        if (n3 > 18 && n > 56 && n <= 80) {
            --n5;
        }
        if (n3 > 12 && n >= 87 && n <= 91) {
            n5 -= n3 - 12;
        }
        switch (n) {
            case 26:
                if (bl) n5 >>= 1;
                break;
            case 43:
                if (bl) { n5 >>= 1; break; }
                if (bl) n5 >>= 1;
                break;
            case 1:
                if (bl) n5 >>= 1;
                break;
            case 19:
                if (bl) n5 >>= 1;
                break;
            case 12:
                if (bl) n5 >>= 1;
                break;
            case 13:
                if (bl) n5 >>= 1;
                break;
            case 42:
                if (bl) n5 >>= 1;
                break;
            case 54:
                if (bl) n5 >>= 1;
        }
        if (0 < n2) {
            n5 = Math.max(n5, 1);
        }
        if (n4 > 0) {
            n5 -= n4;
        }
        return n5;
    }

    static int originalReduction(int n, int n2) {
        int n3 = n2;
        int n4 = 0;
        if (n == 0) {
            if (n3 == 11 || n3 == 12) n4 = 1; else if (n3 >= 13) n4 = 2; else n4 = 0;
        } else if (n == 1) {
            if (n3 == 9 || n3 == 10) n4 = 1; else if (n3 >= 11) n4 = 2; else n4 = 0;
        } else if (n == 2) {
            n4 = 0;
        } else if (n == 4) {
            if (n3 == 13 || n3 == 14) n4 = 1; else if (n3 >= 15) n4 = 2; else n4 = 0;
        } else if (n == 3) {
            n4 = 0;
        } else if (n == 5) {
            n4 = 0;
        } else if (n == 6) {
            if (n3 == 14) n4 = 1; else if (n3 >= 15) n4 = 2; else n4 = 0;
        }
        return n4;
    }

    private NativeSourceSkillConsumptionMath() {
    }
}
