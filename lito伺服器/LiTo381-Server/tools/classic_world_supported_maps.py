#!/usr/bin/env python3
"""Shared exact map set scheduled by ClassicWorldMapCatalog.supportedMapIds()."""

# Keep this tuple in the same append-only order as the Java catalog. The audit
# scripts deliberately test this set against the runtime scheduler.
SUPPORTED_MAP_IDS = (
    3, 4, 16, 17, 19, 20, 21, 22, 25, 26, 27, 28,
    43, 44, 45, 46, 47, 48, 49, 50, 51, 541, 542, 543,
    30, 31, 32, 33, 35, 36, 37, 67,
    209, 213, 302, 303, 244, 350, 445, 447, 0, 5, 6,
    70, 83, 84, 1, 2, 14, 13, 237,
    7, 8, 9, 10, 11, 12, 53, 54, 55, 56, 52,
    23, 24, 29, 15, 240, 241, 242, 243,
    75, 76, 77, 78, 79, 80, 81, 82, 66,
    101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
    300, 301, 59, 60, 61, 62, 63, 64, 65, 252, 253, 254,
    304, 305, 307, 308, 309, 310, 320, 330, 400, 401, 410,
    2000, 450, 1011, 613, 420, 430, 440, 441, 442, 443, 444,
    480, 481, 482, 483, 484,
    550, 551, 552, 553, 554, 555, 556, 557, 558,
    446,
    16384,
    16896,
    17408,
    17920,
    18432,
    18944,
    19456,
    19968,
    20480,
    20992,
    21504,
    22016,
    22528,
    23040,
    23552,
    24064,
    24576,
    25088,
    # Desire Cave / Abandoned Land component.
    600, 601, 602, 603, 604, 605, 606, 607, 778, 779,
    # Dragon Knight / Illusionist native home towns.
    1000, 1001,
    # Native PK punishment lifecycle.
    666,
)
