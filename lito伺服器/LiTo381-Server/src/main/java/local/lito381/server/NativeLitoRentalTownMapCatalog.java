package local.lito381.server;

import java.util.List;

/** Selected LiTo.bin's two rental-town S_SHOW_MAP page assignments. */
final class NativeLitoRentalTownMapCatalog {
    static final int LODUN_MAP_ID = 500;
    static final int LODUN_PAGE = 1;
    static final int LENOA_MAP_ID = 560;
    static final int LENOA_PAGE = 2;
    static final int ALPHA_MASK_WIDTH = 311;
    static final int ALPHA_MASK_HEIGHT = 215;
    static final int LODUN_IMAGE_RESOURCE = 0x1A2C;
    static final int LENOA_IMAGE_RESOURCE = 0x1A2D;
    static final String AUTHORITY =
            "selected-LiTo.bin-sub_4AFFB0-sub_48D990-sub_48DE10-LTmapstr";

    record Page(int mapId, int wirePage, int imageResource, String resourceName) {
        Page {
            if (mapId <= 0 || wirePage <= 0 || wirePage > 0xFF
                    || imageResource < 0 || imageResource > 0xFFFF
                    || resourceName == null || resourceName.isBlank()) {
                throw new IllegalArgumentException("invalid rental-town map page");
            }
        }
    }

    private static final List<Page> PAGES = List.of(
            new Page(LODUN_MAP_ID, LODUN_PAGE, LODUN_IMAGE_RESOURCE, "town"),
            new Page(LENOA_MAP_ID, LENOA_PAGE, LENOA_IMAGE_RESOURCE, "town2"));

    private NativeLitoRentalTownMapCatalog() {}

    static List<Page> all() {
        return PAGES;
    }

    static Page findByMap(int mapId) {
        for (Page page : PAGES) {
            if (page.mapId() == mapId) return page;
        }
        return null;
    }

    static int wirePageForMap(int mapId) {
        Page page = findByMap(mapId);
        return page == null ? 0 : page.wirePage();
    }
}
