package local.lito381.server;

// ported from candidate-50635f7e.jar onto src/main; T029 targets formal 0.6.71.
// Incomplete semantic verification: see docs/reports/issue-70.md before B-land.



import java.io.ByteArrayOutputStream;

import java.io.EOFException;

import java.io.IOException;

import java.io.InputStream;

import java.io.OutputStream;

import java.nio.charset.Charset;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;

import java.util.Arrays;

import java.util.List;

import java.util.Objects;

import java.util.Set;



final class Protocol {

    static final long KEY_HIGH = 0x87546CA1L;

    static final Charset LEGACY_TEXT = Charset.forName("MS950");

    static final Charset WIDE_TEXT = StandardCharsets.UTF_16LE;

    /** LiTo-exclusive LTicon/LTdesc records begin after the ordinary 0..19999 namespace. */

    static final int LITO_RESOURCE_WIRE_BASE = 20_000;

    /** LTdesc/LTicon logical record 5 is the native rental-village Light Gem. */

    static final int LITO_LIGHT_GEM_RESOURCE_INDEX = 5;

    static final int LITO_LIGHT_GEM_ICON =

            LITO_RESOURCE_WIRE_BASE + LITO_LIGHT_GEM_RESOURCE_INDEX;

    /** LTdesc/LTicon logical record 128 is the native Boundless Arena town map. */

    static final int LITO_TOWN_MAP_RESOURCE_INDEX = 128;

    static final int LITO_TOWN_MAP_ICON =

            LITO_RESOURCE_WIRE_BASE + LITO_TOWN_MAP_RESOURCE_INDEX;

    /**

     * Safe current-server policy: zero selects LiTo.bin's generic cds producer.

     * The original rental server's S_16/S_7C use-type byte is not yet captured.

     */

    static final int LITO_TOWN_MAP_USE_TYPE_POLICY = 0;

    static final boolean LITO_TOWN_MAP_HISTORICAL_USE_TYPE_EXACT = false;

    static final boolean LITO_TOWN_MAP_HISTORICAL_CLICK_RESPONSE_AVAILABLE = false;

    static final boolean LITO_TOWN_MAP_SELECTED_CLIENT_RESPONSE_EXACT = true;

    /** LTdesc/LTicon logical record 155 is the native transfer stone. */

    static final int LITO_TRANSFER_STONE_RESOURCE_INDEX = 155;

    static final int LITO_TRANSFER_STONE_ICON =

            LITO_RESOURCE_WIRE_BASE + LITO_TRANSFER_STONE_RESOURCE_INDEX;



    static final int C_VERSION = 0x00;

    static final int C_LOGIN = 0x01;

    static final int C_LOGOUT = 0x02;

    static final int C_NEW_ACCOUNT = 0x03;

    static final int C_ENTER_WORLD = 0x05;

    static final int C_CHANGE_PASSWORD = 0x06;

    static final int C_DELETE_CHARACTER = 0x07;

    static final int C_GOTO_PORTAL = 0x08;

    static final int C_QUIT = 0x0F;

    static final int C_RESTART = 0x10;

    static final int C_GIVE = 0x11;

    static final int C_SAY = 0x13;

    static final int C_TELL = 0x1B;

    static final int C_SERVER_SELECT = 0x35;

    static final int C_CHANGE_DIRECTION = 0x09;

    static final int C_MOVE = 0x0A;

    static final int C_BLINK = 0x5A;

    static final int C_GET = 0x0B;

    static final int C_DROP = 0x0C;

    static final int C_ONOFF = 0x0D;

    static final int C_OPEN = 0x0E;

    static final int C_USE_SPELL = 0x14;

    static final int C_ATTACK = 0x17;

    static final int C_FAR_ATTACK = 0x18;

    static final int C_WHO = 0x1A;

    static final int C_USE_ITEM = 0x1C;

    static final int C_SAVE = 0x1D;

    static final int C_WISH = 0x1E;

    static final int C_ADDR = 0x2A;

    static final int C_ACTION = 0x1F;

    static final int C_ALIVE = 0x20;

    static final int C_CREATE_PLEDGE = 0x21;

    static final int C_JOIN_PLEDGE = 0x22;

    static final int C_ANSWER = 0x23;

    static final int C_BAN = 0x24;

    static final int C_EXCLUDE = 0x25;

    static final int C_KICK = 0x26;

    static final int C_HACTION = 0x27;

    static final int C_BUY_SELL = 0x28;

    static final int C_DIALOG = 0x29;

    static final int C_ASK_XCHG = 0x34;

    static final int C_CANCEL_XCHG = 0x36;

    static final int C_ACCEPT_XCHG = 0x37;

    static final int C_ADD_XCHG = 0x38;

    static final int C_SUMMON = 0x39;

    static final int C_BUILDER_CONTROL = 0x3B;

    static final int C_HYPERTEXT_INPUT_RESULT = 0x51;

    static final int C_REGISTER_QUIZ = 0x58;

    static final int C_CHANGE_ACCOUNTINFO = 0x5C;

    static final int C_RETURN_SUMMON = 0x4E;

    static final int C_TELEPORT_USER = 0x50;

    static final int C_TITLE = 0x2B;

    static final int C_BOOKMARK = 0x2C;

    static final int C_DELETE_BOOKMARK = 0x2D;

    static final int C_TELEPORT = 0x2E;

    static final int C_EMBLEM = 0x30;

    static final int C_WAR = 0x31;

    static final int C_READ_NEWS = 0x33;

    static final int C_CONTROL_WEATHER = 0x3A;

    static final int C_MERCENARYEMPLOY = 0x3C;

    static final int C_MERCENARYSELECT = 0x3E;

    static final int C_MERCENARYARRANGE = 0x3F;

    static final int C_TAX = 0x41;

    static final int C_WITHDRAW = 0x42;

    static final int C_REQUEST_ROLL = 0x43;

    static final int C_FIX = 0x44;

    static final int C_FIXABLE_ITEM = 0x45;

    static final int C_READ_NOTICE = 0x46;

    static final int C_DEPOSIT = 0x47;

    static final int C_SELECTABLE_TIME = 0x4C;

    static final int C_SELECT_TIME = 0x4D;

    static final int C_WHO_PLEDGE = 0x48;

    static final int C_BUYABLE_SPELL = 0x49;

    static final int C_BUY_SPELL = 0x4A;

    static final int C_SILENCE = 0x4B;

    static final int C_EXCHANGEABLE_SPELL = 0x52;

    static final int C_EXCHANGE_SPELL = 0x53;

    static final int C_GOTO_MAP = 0x59;

    static final int C_ENTER_SHIP = 0x66;

    static final int C_WANTED = 0x68;

    static final int C_ENTER_PORTAL = 0x73;

    static final int C_SLAVE_CONTROL = 0x4F;

    static final int C_BOARD_WRITE = 0x54;

    static final int C_BOARD_LIST = 0x55;

    static final int C_BOARD_READ = 0x56;

    static final int C_BOARD_DELETE = 0x57;

    static final int C_INVITE_PARTY = 0x5D;

    static final int C_BANISH_PARTY = 0x5E;

    static final int C_LEAVE_PARTY = 0x5F;

    static final int C_WHO_PARTY = 0x60;

    static final int C_LOGIN_RESULT = 0x61;

    static final int C_EXIT_GHOST = 0x6A;

    static final int C_CHECK_PK = 0x6C;

    static final int C_QUERY_BUDDY = 0x6D;

    static final int C_ADD_BUDDY = 0x6E;

    static final int C_REMOVE_BUDDY = 0x6F;

    static final int C_CREATE_CUSTOM_CHARACTER = 0x70;

    static final int C_MONITOR_CONTROL = 0x71;

    static final int C_SMS = 0x72;

    static final int C_MARRIAGE = 0x75;

    static final int C_CHECK_INVENTORY = 0x63;

    static final int C_QUERY_CASTLE_SECURITY = 0x64;

    static final int C_CHANGE_CASTLE_SECURITY = 0x65;

    static final int C_INCLUDE = 0x62;

    static final int C_DESTROY_ITEM = 0x76;

    static final int C_CHAT = 0x77;

    static final int C_RANK_CONTROL = 0x7C;

    static final int C_WAREHOUSE_CONTROL = 0x7E;

    static final int C_NPC_ITEM_CONTROL = 0x7F;

    static final int C_ALT_ATTACK = 0x7A;

    static final int C_PERSONAL_SHOP = 0x80;

    static final int C_QUERY_PERSONAL_SHOP = 0x81;

    static final int C_LITO_LIN_CHARACTER_INFO = 0xC8;

    static final int C_LITO_SELECT_CHAR = 0xC9;

    static final int C_LITO_ENTER_CHANNEL = 0xCA;

    static final int C_LITO_LEAVE_CHANNEL = 0xCB;

    static final int C_LITO_CREATE_ROOM = 0xCC;

    static final int C_LITO_ROOM_USER = 0xCD;

    static final int C_LITO_ROOM_LIST = 0xCE;

    static final int C_LITO_JOIN = 0xCF;

    static final int C_LITO_EXIT_ROOM = 0xD0;

    static final int C_LITO_GAME_INFO = 0xD1;

    static final int C_LITO_READY_GAME = 0xD2;

    static final int C_LITO_SET_FORCE = 0xD3;

    static final int C_LITO_KICK_USER = 0xD4;

    static final int C_LITO_INVITE_USER = 0xD5;

    static final int C_LITO_INVITE_REPLY = 0xD6;

    static final int C_LITO_EDIT_ROOM = 0xDD;

    static final int C_LITO_RANKING = 0xD7;

    static final int C_LITO_CHANGE_OPTION = 0xD8;

    static final int C_LITO_QUIT_FIGHT = 0xD9;

    static final int C_LITO_BUY_LIST = 0xDA;

    static final int C_LITO_BUY_SELL = 0xDB;

    static final int C_LITO_FIND_USER = 0xDC;

    static final int C_LITO_SELL_LIST = 0xDE;

    static final int C_LITO_CONFIRM_DEATH = 0xE0;

    static final int C_LITO_SET_OBSERVER = 0xE1;

    static final int C_LITO_READ_NEWS = 0xE2;

    static final int C_LITO_ASK_MATCH_MAKING = 0xE3;

    static final int C_LITO_QUIT_MATCH_MAKING = 0xE4;

    static final int C_LITO_QUERY_BUDDY_GROUP = 0xE5;

    static final int C_LITO_CREATE_BUDDY_GROUP = 0xE6;

    static final int C_LITO_REMOVE_BUDDY_GROUP = 0xE7;

    static final int C_LITO_RENAME_BUDDY_GROUP = 0xE8;

    static final int C_LITO_CHANGE_BUDDY_GROUP = 0xE9;

    static final int C_LITO_BUILDER_CONTROL = 0xDF;

    static final int C_LITO_EXROOM = 0xEA;

    static final int C_LITO_RECALL = 0xEB;

    static final int C_LITO_RETURN = 0xEC;

    static final int C_LITO_MOVE = 0xED;

    static final int C_LITO_SILENCE = 0xEE;

    static final int C_LITO_CLAN_CREATE = 0xEF;

    static final int C_LITO_CLAN_DESTROY = 0xF0;

    static final int C_LITO_CLAN_UNDO_DESTROY = 0xF1;

    static final int C_LITO_CLAN_ADD = 0xF5;

    static final int C_LITO_CLAN_ADMIT = 0xF6;

    static final int C_LITO_CLAN_DELETE = 0xF7;

    static final int C_LITO_CLAN_INFO = 0xF8;

    static final int C_LITO_SERVER_LIST = 0xF9;

    static final int C_LITO_LOGIN_NO_OTP = 0xFA;

    static final int C_LITO_ROOM_WHO = 0xFB;



    static final int S_LOGIN_CHECK = 0x00;

    static final int S_KEY = 0x01;

    static final int S_BLIND = 0x0A;

    static final int S_NEW_ACCOUNT_CHECK = 0x02;

    static final int S_CHANGE_PASSWORD_CHECK = 0x08;

    static final int S_PUT_OBJECT = 0x0B;

    static final int S_STATUS = 0x0C;

    static final int S_HIT_POINT = 0x0D;

    static final int S_CHANGE_ITEM_BLESS = 0x0E;

    static final int S_MESSAGE = 0x0F;

    static final int S_IDENTIFY_CODE = 0x3F;

    static final int S_MESSAGE_CODE = 0x10;

    static final int S_RESURRECT = 0x11;

    static final int S_SAY = 0x13;

    static final int S_TELL = 0x14;

    static final int S_SAY_CODE = 0x2D;

    static final int S_REMOVE_OBJECT = 0x15;

    static final int S_ADD_INVENTORY = 0x16;

    static final int S_REMOVE_INVENTORY = 0x17;

    static final int S_CHANGE_ITEM_DESC = 0x18;

    static final int S_CHANGE_ITEM_USE = 0x19;

    static final int S_CHANGE_COUNT = 0x1A;

    static final int S_CHANGE_LIGHT = 0x1B;

    static final int S_CHANGE_DIRECTION = 0x1C;

    static final int S_WIELD = 0x1D;

    static final int S_ADD_SPELL = 0x1E;

    static final int S_REMOVE_SPELL = 0x1F;

    static final int S_ACTION = 0x20;

    static final int S_TIME = 0x21;

    static final int S_CHANGE_ATTR = 0x22;

    static final int S_ASK = 0x24;

    static final int S_PARALYSE = 0x25;

    static final int S_CHANGE_ABILITY = 0x26;

    static final int S_POLYMORPH = 0x27;

    static final int S_SPEED = 0x29;

    static final int S_HYPERTEXT = 0x2A;

    static final int S_BUY_LIST = 0x2B;

    static final int S_SELL_LIST = 0x2C;

    static final int S_RETRIEVE_LIST = 0x31;

    static final int S_SLAVE_CONTROL = 0x57;

    static final int S_REQUEST_SUMMON = 0x55;

    static final int S_CHANGE_DESC = 0x58;

    static final int S_CHANGE_ALIGNMENT = 0x59;

    static final int S_HYPERTEXT_INPUT = 0x5B;

    static final int S_READ_MAIL = 0x5E;

    static final int S_EFFECT = 0x37;

    static final int S_PACKET_BOX = 0xFA;

    static final int S_NEWS = 0x38;

    static final int S_EFFECT_LOC = 0x53;

    static final int S_WORLD = 0x28;

    static final int S_POISON = 0x32;

    static final int S_INVISIBLE = 0x34;

    static final int S_EMBLEM = 0x35;

    static final int S_WAR = 0x36;

    static final int S_NUM_CHARACTER = 0x03;

    static final int S_CHARACTER_INFO = 0x04;

    static final int S_NEW_CHAR_INFO = 0x05;

    static final int S_CREATE_CHARACTER_CHECK = 0x06;

    static final int S_ENTER_WORLD_CHECK = 0x07;

    static final int S_DELETE_CHARACTER_CHECK = 0x09;

    static final int S_VERSION_CHECK = 0x3A;

    static final int S_SERVER_LIST = 0x3B;

    static final int S_XCHG_START = 0x3C;

    static final int S_ADD_XCHG = 0x3D;

    static final int S_XCHG_RESULT = 0x3E;

    static final int S_MERCENARYEMPLOY = 0x40;

    static final int S_MERCENARYSELECT = 0x42;

    static final int S_MERCENARYARRANGE = 0x43;

    static final int S_ARCHERARRANGE = 0x44;

    static final int S_MOVE_OBJECT = 0x12;

    static final int S_ATTACK = 0x23;

    static final int S_ATTACK_ALL = 0x2E;

    static final int S_ATTACK_MANY = 0x39;

    static final int S_PORTAL = 0x5A;

    static final int S_BLINK = 0x63;

    static final int S_RESTART = 0x65;

    static final int S_KICK = 0x66;

    static final int S_HIT_RATIO = 0x68;

    static final int S_PLEDGE = 0x69;

    static final int S_CRIMINAL = 0x6A;

    static final int S_TITLE = 0x2F;

    static final int S_ADD_BOOKMARK = 0x30;

    static final int S_WEATHER = 0x33;

    static final int S_ROLL_RESULT = 0x48;

    static final int S_FIXABLE_ITEM_LIST = 0x49;

    static final int S_BUYABLE_SPELL_LIST = 0x4E;

    static final int S_EXCHANGEABLE_SPELL_LIST = 0x5C;

    static final int S_NOT_ENOUGH_FOR_SPELL = 0x5D;

    static final int S_SOUND_EFFECT = 0x4A;

    static final int S_NOTICE = 0x4B;

    static final int S_TAX = 0x45;

    static final int S_WITHDRAW = 0x46;

    static final int S_CASTLE_OWNER = 0x47;

    static final int S_DEPOSIT = 0x4C;

    static final int S_MANA_POINT = 0x4D;

    static final int S_MASTER = 0x4F;

    static final int S_SELECTABLE_TIME_LIST = 0x54;

    static final int S_MAGIC_STATUS = 0x56;

    static final int S_MAGE_STRENGTH = 0x6B;

    static final int S_MAGE_DEXTERITY = 0x6C;

    static final int S_MAGE_SHIELD = 0x6D;

    static final int S_ABILITY_SCORES = 0x50;

    static final int S_EXP = 0x51;

    static final int S_AC = 0x52;

    static final int S_CHANGE_ITEM_DESC_EX = 0x6F;

    static final int S_SHOW_MAP = 0x6E;

    static final int S_DRUNKEN = 0x70;

    static final int S_WANTED_LOGIN = 0x71;

    static final int S_WARNING_CODE = 0x72;

    /** LTstring[110]: the current battle type requires at least this many players. */

    static final int LITO_BATTLE_MINIMUM_PARTICIPANTS_WARNING = 2110;

    /** LTstring[186]: the current battle type cannot proceed at or above this count. */

    static final int LITO_BATTLE_UPPER_BOUND_WARNING = 2186;

    /** LTstring[285]: each force requires at least the supplied participant count. */

    static final int LITO_FORCE_MINIMUM_PARTICIPANTS_WARNING = 2285;

    /** LTstring[286]: each force cannot proceed at or above the supplied count. */

    static final int LITO_FORCE_UPPER_BOUND_WARNING = 2286;

    static final int S_AGIT_LIST = 0x73;

    static final int S_AGIT_MAP = 0x74;

    static final int S_BREATH = 0x77;

    static final int S_BOARD_LIST = 0x5F;

    static final int S_BOARD_READ = 0x60;

    // Historical registry label. The exact receiver is the dch brave-speed

    // state used by classic S_SkillBrave, not an actor emote animation.

    static final int S_EMOTION = 0x62;

    static final int S_PING = 0x78;

    static final int S_CLONE = 0x79;

    static final int S_BOOK_LIST = 0x7E;

    static final int S_CHANGE_ACCOUNTINFO_CHECK = 0x67;

    static final int S_COMMAND_TARGET = 0x7A;

    static final int S_EVENT = 0x7B;

    static final int S_ADD_INVENTORY_BATCH = 0x7C;

    static final int S_PERSONAL_SHOP_LIST = 0x7F;

    static final int S_LITO_SELECT_CHAR_RESULT = 0xC8;

    static final int S_LITO_ENTER_CHANNEL_RESULT = 0xC9;

    static final int S_LITO_CHANNEL_INFO = 0xCA;

    static final int S_LITO_CHARACTER_INFO = 0xCB;

    static final int S_LITO_ROOM_INFO = 0xCC;

    static final int S_LITO_ROOM_USER = 0xCD;

    static final int S_LITO_JOIN_RESULT = 0xCE;

    static final int S_LITO_DELETE_ROOM = 0xCF;

    static final int S_LITO_EXIT_ROOM = 0xD0;

    static final int S_LITO_READY_GAME = 0xD1;

    static final int S_LITO_START_GAME = 0xD2;

    static final int S_LITO_END_GAME = 0xD3;

    static final int S_LITO_SET_FORCE = 0xD4;

    static final int S_LITO_GAME_INFO = 0xD5;

    static final int S_LITO_KICK_USER = 0xD6;

    static final int S_LITO_INVITE_USER = 0xD7;

    static final int S_LITO_INVITE_REPLY = 0xD8;

    static final int S_LITO_RANKING = 0xD9;

    static final int S_LITO_RESET = 0xDA;

    static final int S_LITO_POINT = 0xDB;

    static final int S_LITO_BUY_LIST = 0xDF;

    static final int S_LITO_SELL_LIST = 0xE0;

    static final int S_LITO_COUNTDOWN = 0xE1;

    static final int S_LITO_OBSERVER_BOX = 0xE2;

    static final int S_LITO_CONFIRM_DEATH = 0xE3;

    static final int S_LITO_SET_GHOST = 0xE4;

    static final int S_LITO_SET_OBSERVER_RESULT = 0xE5;

    static final int S_LITO_NEWS = 0xE6;

    static final int S_LITO_MINIMAP = 0xE8;

    static final int S_LITO_BUDDY_LIST = 0xEA;

    static final int S_LITO_LOGIN_OK = 0xE7;

    static final int S_LITO_TIMELIMIT = 0xE9;

    static final int S_LITO_ASK_MATCH_MAKING_RESULT = 0xEB;

    static final int S_LITO_QUIT_MATCH_MAKING_RESULT = 0xEC;

    /** Exact result values consumed by native {@code sub_4AAE70}. */

    enum LitoMatchMakingResult {

        NO_TRANSITION(0x00),

        ACCEPTED(0x01),

        ITEMS_REQUIRED(0x10),

        OBSERVER_REJECTED(0x11);



        private final int wireValue;



        LitoMatchMakingResult(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    /** Results emitted by this server for native {@code sub_4AAF60}. */

    enum LitoQuitMatchMakingResult {

        ACCEPTED(0x00),

        NO_TRANSITION(0x01);



        private final int wireValue;



        LitoQuitMatchMakingResult(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    /**

     * Exact observer-state byte shared by both native C_E1 producers and the

     * S_E5 receiver.  The client computes each request with {@code setz}, so

     * values outside this closed 0/1 domain have no native producer.

     */

    enum LitoObserverState {

        DISABLED(0),

        ENABLED(1);



        private final int wireValue;



        LitoObserverState(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        boolean enabled() {

            return this == ENABLED;

        }



        static LitoObserverState fromEnabled(boolean enabled) {

            return enabled ? ENABLED : DISABLED;

        }



        static LitoObserverState fromWire(int value) {

            return switch (value) {

                case 0 -> DISABLED;

                case 1 -> ENABLED;

                default -> throw new IllegalArgumentException(

                        "unknown native LiTo observer state " + value);

            };

        }

    }



    /**

     * Exact selectable LiTo room forces. Native C_D3 producers install only

     * callback operands 0 and 1, and S_D4 receiver {@code sub_4A9BD0} has

     * state-changing branches only for those two values.

     */

    enum LitoForce {

        FORCE_0(0),

        FORCE_1(1);



        private final int wireValue;



        LitoForce(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static LitoForce fromWire(int value) {

            return switch (value) {

                case 0 -> FORCE_0;

                case 1 -> FORCE_1;

                default -> throw new IllegalArgumentException(

                        "unknown native LiTo force " + value);

            };

        }

    }



    /**

     * Native participant states shared by C_D2, S_D1 and the initial S_CD

     * roster row. Value 2 is a room shop/list state, not ready=true.

     */

    enum LitoParticipantState {

        NOT_READY(0),

        READY(1),

        SHOP_LIST(2);



        private final int wireValue;



        LitoParticipantState(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        boolean ready() {

            return this == READY;

        }



        static LitoParticipantState fromWire(int value) {

            return switch (value) {

                case 0 -> NOT_READY;

                case 1 -> READY;

                case 2 -> SHOP_LIST;

                default -> throw new IllegalArgumentException(

                        "unknown native LiTo participant state " + value);

            };

        }

    }



    /** Exact C_D6/S_D8 invitation reply domain produced by the native client. */

    enum LitoInvitationReply {

        NOT_ACCEPTED(0),

        ACCEPTED(1);



        private final int wireValue;



        LitoInvitationReply(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        boolean accepted() {

            return this == ACCEPTED;

        }



        static LitoInvitationReply fromWire(int value) {

            return switch (value) {

                case 0 -> NOT_ACCEPTED;

                case 1 -> ACCEPTED;

                default -> throw new IllegalArgumentException(

                        "unknown native LiTo invitation reply " + value);

            };

        }

    }



    /** Exact S_F2 buddy-add error domain consumed by native sub_4AB680. */

    enum LitoBuddyError {

        ALREADY_REGISTERED(0x12),

        LIST_FULL(0x13);



        private final int wireValue;



        LitoBuddyError(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static LitoBuddyError fromWire(int value) {

            return switch (value) {

                case 0x12 -> ALREADY_REGISTERED;

                case 0x13 -> LIST_FULL;

                default -> throw new IllegalArgumentException(

                        "unknown native LiTo buddy error " + value);

            };

        }

    }



    /**

     * Canonical S_EF presence values consumed by native {@code sub_4AB580}.

     * The receiver treats every nonzero byte as the disconnected branch, but

     * the production writer exposes only the canonical 0/1 pair.

     */

    enum LitoBuddyPresence {

        ONLINE(0),

        OFFLINE(1);



        private final int wireValue;



        LitoBuddyPresence(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        boolean online() {

            return this == ONLINE;

        }



        static LitoBuddyPresence fromWire(int value) {

            return switch (value) {

                case 0 -> ONLINE;

                case 1 -> OFFLINE;

                default -> throw new IllegalArgumentException(

                        "noncanonical native LiTo buddy presence " + value);

            };

        }

    }



    /**

     * Exact S_F1/S_EA buddy activity domain rendered by native

     * {@code sub_4642B0}. Values 0/1/2 select LTsurf 7107/7108/7109;

     * LTstring-c names the live states "waiting" and "fighting", while

     * {@code sub_456AF0} sorts the gray value-2 rows after all live rows.

     */

    enum LitoBuddyActivity {

        WAITING(0),

        FIGHTING(1),

        OFFLINE(2);



        private final int wireValue;



        LitoBuddyActivity(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        boolean online() {

            return this != OFFLINE;

        }



        static LitoBuddyActivity fromWire(int value) {

            return switch (value) {

                case 0 -> WAITING;

                case 1 -> FIGHTING;

                case 2 -> OFFLINE;

                default -> throw new IllegalArgumentException(

                        "noncanonical native LiTo buddy activity " + value);

            };

        }

    }



    /**

     * Exact native S_EA {@code shccsc} row in receiver field order.

     *

     * <p>{@code sub_4A2A50}'s string arguments are {@code capacity, destination}.

     * Following those varargs proves that the second byte is copied to record

     * {@code +0x106}/row {@code +0x6c} (palette), while the final byte is copied

     * to record {@code +0x105}/row {@code +0x75} (activity).</p>

     */

    record LitoBuddyRow(String name, int groupId, int relationshipStyle,

                        int paletteOffset, String origin, LitoBuddyActivity activity) {

        LitoBuddyRow {

            if (name == null || name.isBlank()) {

                throw new IllegalArgumentException("S_LITO_BUDDY_LIST requires a name");

            }

            if (groupId < 0 || groupId > 0xFFFF) {

                throw new IllegalArgumentException("S_LITO_BUDDY_LIST invalid group " + groupId);

            }

            if (relationshipStyle < 0 || relationshipStyle > 4) {

                throw new IllegalArgumentException(

                        "S_LITO_BUDDY_LIST invalid relationship style " + relationshipStyle);

            }

            if (paletteOffset < 0 || paletteOffset > 0xC2) {

                throw new IllegalArgumentException(

                        "S_LITO_BUDDY_LIST invalid palette offset " + paletteOffset);

            }

            origin = origin == null ? "" : origin;

            Objects.requireNonNull(activity, "buddy activity");

        }

    }



    /** Exact role word stored by native S_F6/S_F7 clan member records. */

    enum LitoClanRole {

        ROYAL(1), MEMBER(2), CANDIDATE(3), OWNER(4);



        private final int wireValue;



        LitoClanRole(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static LitoClanRole fromWire(int value) {

            return switch (value) {

                case 1 -> ROYAL;

                case 2 -> MEMBER;

                case 3 -> CANDIDATE;

                case 4 -> OWNER;

                default -> throw new IllegalArgumentException(

                        "unknown native LiTo clan role " + value);

            };

        }

    }



    /**

     * S_F6/S_F7 final-byte activity domain. The clan builder uses the same

     * row+0x75 sorter and LTsurf 7107/7108/7109 renderer as S_EA.

     */

    enum LitoClanActivity {

        WAITING(0), FIGHTING(1), OFFLINE(2);



        private final int wireValue;



        LitoClanActivity(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static LitoClanActivity fromWire(int value) {

            return switch (value) {

                case 0 -> WAITING;

                case 1 -> FIGHTING;

                case 2 -> OFFLINE;

                default -> throw new IllegalArgumentException(

                        "noncanonical native LiTo clan activity " + value);

            };

        }



        static LitoClanActivity fromBuddy(LitoBuddyActivity activity) {

            Objects.requireNonNull(activity, "buddy activity");

            return fromWire(activity.wireValue());

        }

    }



    /** Production-safe S_D3 outcomes consumed by native {@code sub_4A9A60}. */

    enum LitoBattleResult {

        NO_WINNER(-1),

        FORCE_0_WON(0),

        FORCE_1_WON(1);



        private final int wireValue;



        LitoBattleResult(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static LitoBattleResult fromWinningForce(int value) {

            return switch (value) {

                case -1 -> NO_WINNER;

                case 0 -> FORCE_0_WON;

                case 1 -> FORCE_1_WON;

                default -> throw new IllegalArgumentException(

                        "unknown native LiTo winning force " + value);

            };

        }

    }



    /**

     * Operational S_FC values verified against this server's encrypted room

     * lifecycle. The selected client stores the dword but has no static

     * consumer that can prove historical original-server result semantics.

     */

    enum LitoKickStatusResult {

        ACCEPTED(0),

        REJECTED(1);



        private final int wireValue;



        LitoKickStatusResult(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }

    static final int S_LITO_BUDDY_GROUP_LIST = 0xED;

    static final int S_LITO_CHANGE_BUDDY_GROUP_RESULT = 0xEE;

    static final int S_LITO_BUDDY_LOGIN = 0xEF;

    static final int S_LITO_QUIT_FIGHT = 0xF0;

    static final int S_LITO_BUDDY_CHANGE_STATUS = 0xF1;

    static final int S_LITO_BUDDY_ERROR = 0xF2;

    static final int S_LITO_VS_SERVER_STATUS = 0xF3;

    static final int S_LITO_CLAN_INFO = 0xF6;

    static final int S_LITO_CLAN_ADD = 0xF7;

    static final int S_LITO_CLAN_DELETE = 0xF9;

    static final int S_LITO_CLAN = 0xFA;

    static final int S_LITO_VS_SERVER_IP = 0xF4;

    static final int S_LITO_VS_SERVER_ALLOW_JOIN = 0xF5;

    static final int S_LITO_SERVER_LIST = 0xFB;

    static final int S_LITO_KICK_STATUS = 0xFC;

    static final int S_LITO_RACE_INFO = 0xFD;



    /**

     * Production S_LOGIN_CHECK values with a proved server-side predicate.

     *

     * <p>The selected client's sub_4903D0 has an exhaustive 0..255 result

     * domain, but receiver reachability does not establish the proprietary

     * server meaning of every branch.  Keeping the production writer typed

     * prevents unassigned native selectors from being emitted accidentally.</p>

     */

    enum LoginCheckResult {

        ACCEPTED(0x00),

        SYSTEM_ERROR(0x05),

        ACCESS_FAILED(0x08),

        ACCOUNT_BANNED(0x1A),

        ACCOUNT_IN_USE(0x16);



        private final int wireValue;



        LoginCheckResult(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    private interface NativeCheckResult {

        int wireValue();

    }



    /** Typed S_02 selectors owned by the new-account mutation. */

    enum NewAccountCheckResult implements NativeCheckResult {

        SYSTEM_ERROR_NO_TRANSITION(0x01),

        RETURN_TO_LOGIN(0x04),

        ACCOUNT_ALREADY_EXISTS(0x07);



        private final int wireValue;



        NewAccountCheckResult(int wireValue) {

            this.wireValue = wireValue;

        }



        public int wireValue() {

            return wireValue;

        }

    }



    /** Typed S_08 selectors owned by password/quiz mutations. */

    enum ChangePasswordCheckResult implements NativeCheckResult {

        RETURN_TO_LOGIN(0x04),

        WRONG_PASSWORD(0x0A),

        PASSWORD_CHANGE_UNAVAILABLE(0x35);



        private final int wireValue;



        ChangePasswordCheckResult(int wireValue) {

            this.wireValue = wireValue;

        }



        public int wireValue() {

            return wireValue;

        }

    }



    /** Typed S_67 selectors owned by account-information mutation. */

    enum ChangeAccountInfoCheckResult implements NativeCheckResult {

        SYSTEM_ERROR_NO_TRANSITION(0x01),

        RETURN_TO_LOGIN(0x04);



        private final int wireValue;



        ChangeAccountInfoCheckResult(int wireValue) {

            this.wireValue = wireValue;

        }



        public int wireValue() {

            return wireValue;

        }

    }



    // Exact sub_480560 C_VERSION values for this LiTo 3.81C image.  fromCD

    // is launch metadata (registry value, or 2 in connector mode) and is not

    // part of the version allow-list.

    static final int VERSION_MAGIC = 0x8033;

    static final long VERSION_PRODUCT = 0x0000_03B6L;

    static final int VERSION_BUILD = 0x14F8;

    static final int VERSION_ACCEPTED = 0;

    static final int VERSION_REJECTED = 1;



    /** Typed S_06 failure selectors; creation success uses S_03/S_05/S_04. */

    enum CreateCharacterFailure implements NativeCheckResult {

        ALREADY_EXISTS(0x06),

        INVALID_NAME(0x09),

        WRONG_AMOUNT(0x15);



        private final int wireValue;



        CreateCharacterFailure(int wireValue) {

            this.wireValue = wireValue;

        }



        public int wireValue() {

            return wireValue;

        }

    }



    /** Typed S_07 actions. Result zero is the native no-transition reject. */

    enum EnterWorldCheckResult implements NativeCheckResult {

        NO_TRANSITION(0x00),

        ACCEPTED(0x03);



        private final int wireValue;



        EnterWorldCheckResult(int wireValue) {

            this.wireValue = wireValue;

        }



        public int wireValue() {

            return wireValue;

        }

    }



    /** Typed S_09 actions. Result zero is the native no-transition reject. */

    enum DeleteCharacterCheckResult implements NativeCheckResult {

        NO_TRANSITION(0x00),

        DELETE_NOW(0x05);



        private final int wireValue;



        DeleteCharacterCheckResult(int wireValue) {

            this.wireValue = wireValue;

        }



        public int wireValue() {

            return wireValue;

        }

    }



    /** Native crest is 16 by 12 pixels in the client's 16-bit surface format. */

    static final int EMBLEM_BYTES = 16 * 12 * 2;

    static final long BOARD_INITIAL_CURSOR = 0x7FFF_FFFFL;

    static final int BOARD_PAGE_SIZE = 8;

    static final int BOARD_WRITE_FEE = 300;



    private Protocol() {}



    static final class CipherState {

        private long value;



        CipherState(int seed) {

            value = (KEY_HIGH << 32) | Integer.toUnsignedLong(seed);

        }



        byte[] encrypt(byte[] plain) {

            if (plain.length == 0) return plain;

            byte[] key = keyBytes();

            byte[] out = new byte[plain.length];

            out[0] = (byte) ((plain[0] & 0xFF) ^ (key[0] & 0xFF));

            for (int i = 1; i < plain.length; i++) {

                out[i] = (byte) ((plain[i] & 0xFF) ^ (out[i - 1] & 0xFF) ^ (key[i & 7] & 0xFF));

            }

            value += plain.length;

            return out;

        }



        byte[] decrypt(byte[] cipher) {

            if (cipher.length == 0) return cipher;

            byte[] key = keyBytes();

            byte[] out = new byte[cipher.length];

            int previous = cipher[0] & 0xFF;

            out[0] = (byte) (previous ^ (key[0] & 0xFF));

            for (int i = 1; i < cipher.length; i++) {

                int current = cipher[i] & 0xFF;

                out[i] = (byte) (current ^ previous ^ (key[i & 7] & 0xFF));

                previous = current;

            }

            value += cipher.length;

            return out;

        }



        long value() {

            return value;

        }



        private byte[] keyBytes() {

            byte[] bytes = new byte[8];

            long current = value;

            for (int i = 0; i < 8; i++) {

                bytes[i] = (byte) current;

                current >>>= 8;

            }

            return bytes;

        }

    }



    static final class Reader {

        private final byte[] data;

        private int offset;



        Reader(byte[] data) {

            this.data = data;

        }



        int c() { require(1); return data[offset++] & 0xFF; }

        int h() { require(2); int v = c() | (c() << 8); return v; }

        long d() { require(4); return Integer.toUnsignedLong(c() | (c() << 8) | (c() << 16) | (c() << 24)); }



        String s() {

            return new String(zBytes(), LEGACY_TEXT);

        }



        String S() {

            int start = offset;

            while (offset + 1 < data.length

                    && (data[offset] != 0 || data[offset + 1] != 0)) {

                offset += 2;

            }

            if (offset + 1 >= data.length) {

                throw new IllegalArgumentException("unterminated UTF-16LE string");

            }

            String value = new String(data, start, offset - start, WIDE_TEXT);

            offset += 2;

            if (!wellFormedWideString(value)) {

                throw new IllegalArgumentException("malformed UTF-16LE string");

            }

            return value;

        }



        byte[] zBytes() {

            int start = offset;

            while (offset < data.length && data[offset] != 0) offset++;

            if (offset >= data.length) throw new IllegalArgumentException("unterminated string");

            byte[] value = Arrays.copyOfRange(data, start, offset);

            offset++;

            return value;

        }



        int remaining() { return data.length - offset; }



        private void require(int count) {

            if (count < 0 || offset + count > data.length) throw new IllegalArgumentException("packet underflow");

        }

    }



    static final class Writer {

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        private int wideVitalFieldsRemaining;



        Writer c(int value) {

            out.write(value & 0xFF);

            if (out.size() == 1 && NumericBreakthrough.enabled()

                    && (value == S_HIT_POINT || value == S_MANA_POINT)) {

                wideVitalFieldsRemaining = 2;

            }

            return this;

        }

        Writer h(int value) {

            if (wideVitalFieldsRemaining > 0) {

                wideVitalFieldsRemaining--;

                return d(Integer.toUnsignedLong(value));

            }

            c(value); c(value >>> 8); return this;

        }

        Writer d(long value) { c((int) value); c((int) (value >>> 8)); c((int) (value >>> 16)); c((int) (value >>> 24)); return this; }

        Writer s(String value) {

            byte[] encoded = (value == null ? "" : value).getBytes(LEGACY_TEXT);

            out.writeBytes(encoded);

            out.write(0);

            return this;

        }

        Writer S(String value) {

            String text = value == null ? "" : value;

            if (!wellFormedWideString(text)) {

                throw new IllegalArgumentException("malformed UTF-16LE string");

            }

            out.writeBytes(text.getBytes(WIDE_TEXT));

            out.write(0);

            out.write(0);

            return this;

        }

        Writer raw(byte[] value) { out.writeBytes(value); return this; }

        byte[] bytes() { return out.toByteArray(); }

    }



    static byte[] readFrame(InputStream input, int maxFrame) throws IOException {

        int lo = input.read();

        if (lo < 0) return null;

        int hi = input.read();

        if (hi < 0) throw new EOFException("truncated frame header");

        int total = lo | (hi << 8);

        if (total < 2 || total > maxFrame) throw new IOException("invalid LiTo frame length: " + total);

        byte[] payload = input.readNBytes(total - 2);

        if (payload.length != total - 2) throw new EOFException("truncated LiTo frame");

        return payload;

    }



    static void writeFrame(OutputStream output, byte[] payload) throws IOException {

        int total = payload.length + 2;

        if (total > 0xFFFF) throw new IOException("LiTo frame too large");

        output.write(total & 0xFF);

        output.write((total >>> 8) & 0xFF);

        output.write(payload);

        output.flush();

    }



    static String hex(byte[] bytes) {

        StringBuilder result = new StringBuilder(bytes.length * 3);

        for (byte value : bytes) {

            if (!result.isEmpty()) result.append(' ');

            result.append(String.format("%02X", value & 0xFF));

        }

        return result.toString();

    }



    /**

     * Native Tournament battle-scene bootstrap order.

     *

     * <p>S_WORLD must precede S_LITO_START_GAME.  LiTo's 0x28 handler stores

     * the native arena id and resolves dword_75A898; the following 0xD2

     * handler immediately indexes the arena table through that value while it

     * replaces the room callbacks.  Reversing these two frames makes the

     * protected client build the scene from stale room state and return from

     * its main loop.</p>

     */

    static BattleBootstrap battleBootstrap(int mapId, int timeLimitSeconds) {

        return new BattleBootstrap(

                new Writer().c(S_WORLD).h(mapId).c(0).bytes(),

                new Writer().c(S_LITO_START_GAME).bytes(),

                new Writer().c(S_LITO_TIMELIMIT).d(timeLimitSeconds).bytes());

    }



    static byte[] litoTimeLimit(int timeLimitSeconds) {

        if (timeLimitSeconds <= 0) {

            throw new IllegalArgumentException("invalid LiTo time limit");

        }

        return new Writer().c(S_LITO_TIMELIMIT).d(timeLimitSeconds).bytes();

    }



    /**

     * Native Tournament battle result.

     *

     * <p>LiTo's sub_4A9A60 parses this frame with format {@code d}: the value

     * is the winning force, not a per-client win/loss byte.  {@code -1}

     * represents a draw.  Sending only one byte leaves the two result

     * controllers without a complete value, so phase 5 opens without the

     * winner/loser presentation or usable result controls.</p>

     */

    static byte[] battleResult(LitoBattleResult result) {

        if (result == null) throw new IllegalArgumentException("null LiTo battle result");

        return new Writer().c(S_LITO_END_GAME).d(result.wireValue()).bytes();

    }



    static byte[] litoForceChanged(String playerName, LitoForce force) {

        requireBoundedLegacyString(playerName, 1, 27,

                "S_LITO_SET_FORCE participant name");

        if (force == null) throw new IllegalArgumentException("null LiTo force");

        return new Writer().c(S_LITO_SET_FORCE).s(playerName)

                .c(force.wireValue()).bytes();

    }



    static byte[] joinRejected() {

        // sub_4A8BA0 recognizes result 1 and routes directly to the common

        // cleanup path without consuming a room-record tail or selecting an

        // unverified localized error string.

        return joinRejected(LitoJoinFailure.SILENT_CLEANUP);

    }



    static byte[] joinRejected(LitoJoinFailure failure) {

        return new Writer().c(S_LITO_JOIN_RESULT).c(failure.wireValue()).bytes();

    }



    /**

     * Exact selected-build S_CD room-participant record.

     *

     * <p>{@code sub_4A7F60} parses the selected protocol branch as

     * {@code dscccdddcscsdccdc}, followed by twelve legacy status bytes,

     * five newer status bytes and one dword.  The three score dwords are

     * copied as low words to record offsets {@code +0x3c}, {@code +0x3e}

     * and {@code +0x40}: total decided matches, wins and losses.  Native

     * {@code sub_4A7F60} derives the byte at {@code +0x46} as

     * {@code wins / total * 100}; {@code sub_459AE0} renders the latter two

     * values with LTstring[16] ({@code record: %d wins %d losses}) and the

     * derived percentage with LTstring[18].  The first bounded string after

     * those score fields is copied to record offset {@code +0x20}.

     * Primary field 11 is parsed in every retained protocol-version branch,

     * but {@code sub_4A7F60} never copies it into the 0x8c-byte participant

     * record; it is therefore a reserved compatibility byte and is emitted as

     * zero.  Primary field 13 is copied as a low word to record offset

     * {@code +0x42}.  Together with offsets {@code +0x3c/+0x3e/+0x40}, this

     * forms the exact same ordered four-word statistic tuple that

     * {@code sub_4A7550/sub_4928C0/sub_4934F0} consume from S_CB as decided

     * matches, wins, losses and disconnects.  Field 13 therefore carries the

     * persisted disconnect count; it is not a source-server selector.

     * Primary field 14 is copied to record {@code +0x35}; renderer

     * {@code sub_459AE0} maps nonnegative values through LTstring[5..9] and

     * wraps the selected label with LTstring[21] ({@code channel: %s}).  The

     * signed-byte sentinel {@code -1} renders an empty channel label.

     * Primary fields 9 and 15 are copied into the two list-item paths at

     * object offset {@code +0x6C}.  The renderer treats zero as the ordinary

     * text colour and any native room state (1=ready, 2=shop/list-entry) as

     * the highlighted colour.  Later {@code S_D1} updates the same room

     * record byte at {@code +0x44}, proving that these are the initial ready

     * states rather than unused booleans.

     * The penultimate dword in the primary format is the current channel

     * rank.  {@code sub_4A7F60} converts rank 1 to badge {@code 0x22}, ranks

     * 2..10 to {@code 0x21}, and ranks 11..1610 to 32 descending 50-player

     * bands beginning at {@code 0x20}; zero or an out-of-range rank remains

     * the unranked badge.  Primary field 17 is packed into the high word of

     * the participant list icon selector.  The selected renderer indexes

     * {@code LTlist[0xC2-selector]}; selectors 0..14 therefore select the

     * native Onyx-through-Emperor Tournament-grade resources 194..180.  It

     * is not the character's classic source-server id.

     * The twelve bytes following the primary format are record offsets

     * {@code +0x47..+0x52}.  {@code sub_459AE0} splits each nonzero byte into

     * a high-nibble award generation and low-nibble material tag:

     * 1=Mithril through LTstring[188], 2=Gold through LTstring[189], and

     * 4=Silver through LTstring[190].  The next five bytes at

     * {@code +0x64..+0x68} are positive signed-byte edition numbers for the

     * Death Match, Honor Match, Quest Battle, Time Attack and Siege War

     * victory badges rendered by LTstring[210..214].  The terminal dword at

     * {@code +0x6c} has one proved consumer: bit zero renders the fixed first

     * LiTo Master badge through LTstring[215].  These are externally awarded

     * honours, not values derived from ordinary wins.

     * {@code lteventsvr} sets {@code byte_64AF9D=1}, causing

     * {@code sub_459AE0} to render that field with LTstring[17]

     * ({@code team name}); ordinary builds render the same affiliation slot

     * with LTstring[19] ({@code clan}).  It is therefore not the character's

     * source-server name.</p>

     */

    static byte[] litoRoomParticipant(long roomId, String name, LitoForce force,

                                      int characterType, int sex,

                                      GameStateStore.TournamentStatistics statistics,

                                      String affiliationName, int channelSelector,

                                      int channelRank, LitoParticipantState readyState,

                                      int tournamentGradeSelector,

                                      GameStateStore.TournamentAwards awards) {

        requireUnsignedDword(roomId, "LiTo room id");

        if (roomId == 0) throw new IllegalArgumentException("LiTo room id is zero");

        requireBoundedLegacyString(name, 1, 27, "LiTo room participant name");

        requireBoundedLegacyString(affiliationName, 0, 16,

                "LiTo room participant affiliation");

        if (force == null) {

            throw new IllegalArgumentException("null LiTo room participant force");

        }

        if (characterType < 0 || characterType > 6 || sex < 0 || sex > 1) {

            throw new IllegalArgumentException(

                    "invalid LiTo room participant class or sex");

        }

        if (channelRank < 0) {

            throw new IllegalArgumentException("negative LiTo room participant channel rank");

        }

        if (channelSelector < -1 || channelSelector > 4) {

            throw new IllegalArgumentException(

                    "invalid LiTo room participant channel selector");

        }

        if (readyState == null) {

            throw new IllegalArgumentException("null LiTo room participant state");

        }

        if (tournamentGradeSelector < 0 || tournamentGradeSelector > 14) {

            throw new IllegalArgumentException(

                    "invalid LiTo room participant Tournament grade selector");

        }

        if (statistics == null) {

            throw new IllegalArgumentException("null LiTo room participant statistics");

        }

        if (awards == null) {

            throw new IllegalArgumentException("null LiTo room participant awards");

        }

        requireUnsignedDword(statistics.winRateDenominator(),

                "LiTo room participant decided matches");

        requireUnsignedDword(statistics.wins(), "LiTo room participant wins");

        requireUnsignedDword(statistics.losses(), "LiTo room participant losses");

        requireUnsignedDword(statistics.disconnects(),

                "LiTo room participant disconnects");

        Writer packet = new Writer()

                .c(S_LITO_ROOM_USER)

                .d(roomId).s(name)

                .c(force.wireValue()).c(characterType).c(sex)

                .d(statistics.winRateDenominator())

                .d(statistics.wins()).d(statistics.losses())

                .c(readyState.wireValue()).s(affiliationName).c(0).s("Ready")

                .d(statistics.disconnects())

                .c(channelSelector).c(readyState.wireValue()).d(channelRank)

                .c(tournamentGradeSelector);

        for (int code : awards.legacyCertificateCodes()) packet.c(code);

        for (int edition : awards.victoryBadgeEditions()) packet.c(edition);

        return packet.d(awards.litoMaster() ? 1 : 0).bytes();

    }



    /**

     * Exact native C_E3 matchmaking selector state.

     *

     * <p>The wire order differs from the UI object's field order: the client

     * emits {@code [object+0x0D]}, {@code [object+0x0C]}, then

     * {@code [object+0x0E]}.  The selected LTstring tables identify those

     * fields as channel restriction, match method and participant

     * restriction respectively.  LTstring[70] and [158..161] label the final

     * field as the lower/middle/upper/high per-force participant interval,

     * not a win-rate selector.</p>

     */

    record MatchMakingRequest(int fixedSelector, int channelRestriction,

                              int matchMethod, int participantRestriction) {

        static final int NO_CHANNEL_RESTRICTION = 0x7F;



        boolean hasChannelRestriction() {

            return channelRestriction != NO_CHANNEL_RESTRICTION;

        }



        /**

         * Maps the matchmaking dropdown value to the exact native room mode

         * id.  The selected client's LTmapstr parser and room renderer agree

         * on all six numeric identities, including Quest=2, Siege=3 and

         * Honor=5.

         */

        BattleRules.Mode requestedMode() {

            return switch (matchMethod) {

                case 0 -> BattleRules.Mode.PET_RACE;

                case 1 -> BattleRules.Mode.DEATHMATCH;

                case 2 -> BattleRules.Mode.QUEST_BATTLE;

                case 3 -> BattleRules.Mode.SIEGE_WAR;

                case 4 -> BattleRules.Mode.TIME_ATTACK;

                case 5 -> BattleRules.Mode.HONOR_MATCH;

                default -> throw new IllegalArgumentException(

                        "unknown native C_E3 match method " + matchMethod);

            };

        }

    }



    record ShopLine(long itemId, long quantity) {}



    record ClassicShopTransaction(long npcObjectId, int mode,

                                  List<ShopLine> lines) {}

    record ClassicBuyListRow(long orderId, int icon, long unitPrice,

                             String name, byte[] detail) {

        ClassicBuyListRow {

            detail = detail == null ? new byte[0] : detail.clone();

        }



        @Override public byte[] detail() { return detail.clone(); }

    }

    record ClassicSellListRow(long inventoryObjectId, long unitPrice) {}

    record RetrieveListRow(long storageObjectId, int useType, int icon,

                           int blessing, long quantity, int identified,

                           String name) {}

    record HouseAuctionRow(long houseId, String name, int area,

                           int deadlineMonth, int deadlineDay, long price) {}



    record GroundPickup(int x, int y, long worldObjectId, long quantity, int auxiliary) {}

    record InventoryDrop(int x, int y, long inventoryObjectId, long quantity) {}

    record OpenRequest(int x, int y, long worldObjectId) {}

    record GiveRequest(long targetObjectId, int targetX, int targetY,

                       long inventoryObjectId, long quantity) {}

    record InventoryUse(long inventoryObjectId, String argument) {}

    record LightInventoryUse(long inventoryObjectId, long clientTick) {}

    record HomeTeleportInventoryUse(long inventoryObjectId, int currentMapId,

                                    int targetX, int targetY) {}

    record AppraisalInventoryUse(long inventoryObjectId, long targetObjectId) {}

    record ChoiceInventoryUse(long inventoryObjectId, long targetObjectId) {}

    record TargetLocationInventoryUse(long inventoryObjectId, long targetObjectId,

                                      int x, int y) {}

    record ShipTicketInventoryUse(long inventoryObjectId, int marker) {}

    record LetterComposeInventoryUse(long inventoryObjectId, int letterCode,

                                     String receiver, String subject, String content) {}

    record GotoMapRequest(boolean cancelled, int destinationMap, int x, int y) {}

    record EnterShipRequest(int selector) {}

    record EnterPortalRequest(int x, int y) {}

    record AddressProof(long challengeId, long checksum, long protectedState) {}

    record VersionRequest(int magic, long product, int fromCd, int build) {}

    enum LoginLayout { DIRECT, CONNECTOR }

    record LoginRequest(String account, String password, long clientAddress,

                        String connector, LoginLayout layout) {

        boolean hasConnector() { return layout == LoginLayout.CONNECTOR; }

    }

    record NewAccountRequest(String account, String password, int type,

                             List<String> profileFields) {}

    record PasswordChangeRequest(String account, String oldPassword, String newPassword) {}

    record QuizRegistrationRequest(String account, String oldPassword, String newPassword,

                                   List<String> quizFields) {}

    record AccountInfoChangeRequest(String account, int type, List<String> profileFields) {}

    record OnOffRequest(int selector, int value) {}

    record ChatRequest(int type, String message) {}

    record TellRequest(String target, String message) {}

    record SilenceRequest(int durationSeconds, String target) {}

    record WantedRequest(int mode, String operand) {}

    record WarehouseControlRequest(int mode, long first, long second) {}

    record NpcItemControlRequest(int selector, long npcObjectId, int listIndex) {}

    record SmsRequest(int mode, String message, String destination,

                      byte[] nativeProducerArtifact) {

        SmsRequest {

            nativeProducerArtifact = nativeProducerArtifact == null

                    ? new byte[0] : nativeProducerArtifact.clone();

        }



        @Override public byte[] nativeProducerArtifact() {

            return nativeProducerArtifact.clone();

        }

    }

    record ClanRankControlRequest(int command, int role, String characterName) {}

    record MoveRequest(int sourceX, int sourceY, int heading, int x, int y) {}

    record AttackRequest(long targetObjectId, int x, int y) {}

    enum HypertextActionKind { DISMISS, SELECT, ATTACK_TARGET, POSTURE, COLLECT, RENAME, GENERIC }

    record HypertextActionRequest(long worldObjectId, String action, String parameter,

                                  HypertextActionKind kind) {}

    record HypertextInputResult(long worldObjectId, long value, int mode, String callback) {}

    record CastleTaxRequest(long playerObjectId, int taxRate) {}

    record CastleAmountRequest(long playerObjectId, long amount) {}

    record CastleSecurityQuery(long stewardObjectId) {}

    record CastleSecurityChange(long stewardObjectId, boolean enabled,

                                long nativeSelector) {}

    record MercenaryEmployRequest(long stewardObjectId, int selectedRowCount,

                                  int entryId, int quantity, int submittedTotalPrice) {}

    record MercenarySelectRequest(long stewardObjectId, int identifier) {}

    record MercenaryArrangeRequest(long stewardObjectId, int command,

                                   int selector, long callbackToken, int quantity) {}

    record MercenaryEmployRow(int entryId, String name, int nativeValue) {}

    record MercenarySelectRow(int identifier, String name) {}

    record ArcherArrangeRow(int entryId, String name, int arranged) {}

    record SelectableTime(int identifier, long encodedTime) {}

    record AskAnswer(int questionId, int answer, String text) {}

    record ClanWarRequest(ClanWarRequestType type, String targetClan) {

        ClanWarRequest {

            Objects.requireNonNull(type, "clan-war request type");

        }

    }

    record ExchangeRequest(long targetObjectId) {}

    record ExchangeItemRequest(long inventoryObjectId, long quantity) {}

    /**

     * Server-owned terminal values for the native player-exchange window.

     *

     * <p>The selected client receiver ({@code sub_4AE240}) accepts the full

     * unsigned-byte domain: value {@code 1} selects its cancel/failure text,

     * while every other value selects its completed text.  The same-version

     * server family, however, only assigns producer semantics to {@code 0}

     * and {@code 1}; keeping those producers typed prevents the other 254

     * receiver-compatible values from acquiring invented server meanings.</p>

     */

    enum ExchangeTerminalResult {

        COMPLETED(0x00),

        CANCELLED_OR_FAILED(0x01);



        private final int wireValue;



        ExchangeTerminalResult(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    /** Exact request selectors emitted by the four recovered native C_WAR producers. */

    enum ClanWarRequestType {

        DECLARE(0x00),

        SURRENDER(0x02),

        CEASE(0x03),

        ALLIANCE(0x05);



        private final int wireValue;



        ClanWarRequestType(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static ClanWarRequestType fromWire(int wireValue) {

            return switch (wireValue) {

                case 0x00 -> DECLARE;

                case 0x02 -> SURRENDER;

                case 0x03 -> CEASE;

                case 0x05 -> ALLIANCE;

                default -> throw new IllegalArgumentException(

                        "unsupported native C_WAR request type");

            };

        }

    }



    /**

     * Complete selector domain consumed by native S_WAR receiver

     * {@code sub_4AD000}; names follow the same-version S_War/L1War producers.

     */

    enum ClanWarNoticeType {

        DECLARED(0x01),

        SURRENDERED(0x02),

        ENDED(0x03),

        WON(0x04),

        ALLIED(0x06),

        ALLIANCE_BROKEN(0x07),

        CURRENT_WAR(0x08);



        private final int wireValue;



        ClanWarNoticeType(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    /**

     * Exact {@code S_EVENT (0x7B)} subtype domain currently produced by this

     * server.  The selected LiTo.bin receiver {@code sub_4B07C0} has a wider

     * 0..40 switch; receiver-only cases are deliberately not assigned server

     * semantics here until an authoritative same-version producer is wired.

     */

    enum ServerEventType {

        CASTLE_WAR_BEGIN(0x00),

        CASTLE_WAR_END(0x01),

        CASTLE_WAR_GOING(0x02),

        CASTLE_WAR_INITIATIVE(0x03),

        CASTLE_WAR_OCCUPIED(0x04),

        SMS_RESULT(0x06),

        MARRIED(0x09),

        CLIENT_INTEGRITY_CHALLENGE(0x21);



        private final int wireValue;



        ServerEventType(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    /**

     * Exact paired modes emitted by the same-version {@code S_Paralysis}

     * producer.  Native receiver {@code sub_4A6A80} accepts the wider signed

     * byte domain {@code 0..25}; values outside these seven producer pairs are

     * deliberately not exposed to production writers.

     */

    enum ParalysisMode {

        PARALYSIS_APPLY(0x02),

        PARALYSIS_RELEASE(0x03),

        SECOND_PARALYSIS_APPLY(0x04),

        SECOND_PARALYSIS_RELEASE(0x05),

        TELEPORT_LOCK(0x06),

        TELEPORT_UNLOCK(0x07),

        SLEEP_APPLY(0x0A),

        SLEEP_RELEASE(0x0B),

        FREEZE_APPLY(0x0C),

        FREEZE_RELEASE(0x0D),

        STUN_APPLY(0x16),

        STUN_RELEASE(0x17),

        BIND_APPLY(0x18),

        BIND_RELEASE(0x19);



        private final int wireValue;



        ParalysisMode(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    /**

     * Exact movement-speed modes shared by native receiver {@code sub_4A7240}

     * and the same-version {@code S_SkillHaste} producer.  Keeping the

     * producer domain typed prevents an arbitrary actor-state byte from being

     * reused as an {@code S_SPEED} mode.

     */

    enum SpeedMode {

        NORMAL(0),

        HASTE(1),

        SLOW(2);



        private final int wireValue;



        SpeedMode(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static SpeedMode fromWire(int value) {

            return switch (value) {

                case BattleTimedStatus.NORMAL -> NORMAL;

                case BattleTimedStatus.HASTE -> HASTE;

                case BattleTimedStatus.SLOW -> SLOW;

                default -> throw new IllegalArgumentException(

                        "unknown native movement-speed mode " + value);

            };

        }

    }



    /**

     * Producer-backed {@code S_DRUNKEN} modes retained by the same-family

     * server. Native receiver {@code sub_4A7310} treats any nonzero byte as an

     * active visual state, but that compatibility behavior is not authority to

     * emit the other 252 values.

     */

    enum DrunkenMode {

        CLEAR(0),

        ORDINARY_LIQUOR(1),

        CONFUSION(2),

        CAKE_OR_NPC(8);



        private final int wireValue;



        DrunkenMode(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }



    /**

     * Authoritative brave-speed modes emitted by same-version server paths.

     * Native receiver {@code sub_4AF9F0} also contains a compatibility branch

     * for value 2, but no authenticated producer predicate assigns that value

     * to the current server flow, so it is deliberately not exposed here.

     */

    enum BraveMode {

        NORMAL(0),

        FIRST_STAGE(1),

        ELVEN_WAFER(3),

        MOVEMENT_BUFF(4),

        SECOND_STAGE(5),

        BLOODLUST(6);



        private final int wireValue;



        BraveMode(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static BraveMode fromWire(int value) {

            return switch (value) {

                case BattleTimedStatus.BRAVE_NORMAL -> NORMAL;

                case BattleTimedStatus.BRAVE_FIRST -> FIRST_STAGE;

                case BattleTimedStatus.BRAVE_ELF -> ELVEN_WAFER;

                case BattleTimedStatus.BRAVE_HOLY_WALK -> MOVEMENT_BUFF;

                case BattleTimedStatus.BRAVE_SECOND -> SECOND_STAGE;

                case BattleTimedStatus.BRAVE_BLOODLUST -> BLOODLUST;

                default -> throw new IllegalArgumentException(

                        "unknown native brave-speed producer mode " + value);

            };

        }

    }

    record SummonRequest(String argument, int mode, Long firstValue, Long secondValue) {}

    /** One native {@code S_SERVER_LIST} record: {@code s d h}. */

    record ClassicServerEndpoint(String name, long ipv4, int port) {}

    /**

     * First native {@code S_LITO_SERVER_LIST} group: a joinable room row.

     * The two trailing dwords occupy record offsets {@code +0x10C/+0x110}.

     */

    record LitoRoomDirectoryEntry(

            String name, long ipv4, int port, long roomId, long users) {}

    /**

     * Second native {@code S_LITO_SERVER_LIST} group: a selectable VS server.

     * Its two trailing dwords occupy record offsets {@code +0x110/+0x114}

     * and are rendered as current users/capacity.

     */

    record LitoVsServerDirectoryEntry(

            String name, long ipv4, int port, long users, long capacity) {}

    /**

     * The selected LiTo.bin allocates exactly ten dwords at

     * {@code dword_64AD2C} for S_F3.  S_F4 indexes that table directly.

     */

    static final int MAX_LITO_VS_SERVERS = 10;

    /** Native S_F3 states distinguished by the sole S_F4 consumer. */

    enum LitoVsServerAvailability {

        AVAILABLE(0),

        UNAVAILABLE(0xFFFF_FFFFL);



        private final long wireValue;



        LitoVsServerAvailability(long wireValue) {

            this.wireValue = wireValue;

        }



        long wireValue() {

            return wireValue;

        }

    }

    /**

     * Native {@code sub_4A76D0} S_C8 result branches.  Result 7 displays

     * LTstring[93], result 8 displays LTstring[92], and other nonzero values

     * have no dedicated message branch in the selected client.

     */

    enum LitoCharacterSelectResult {

        ACCEPTED(0),

        SILENT_REJECTED(1),

        PARTICIPATION_RESTRICTED(7),

        INVALID_CHARACTER_NAME(8);



        private final int wireValue;



        LitoCharacterSelectResult(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }

    /**

     * Production-safe native {@code sub_4A8BA0} S_CE rejection branches.

     *

     * <p>Only values whose receiver behavior and server predicate are backed

     * by native/client or encrypted integration evidence are represented.

     * Results 2, 6, 8 and 15..19 only reach the shared cleanup path; result 9

     * shares result 4's LTstring[29] branch, but its original discriminator is

     * unknown.  They deliberately have no enum constant and therefore cannot

     * be emitted through the production writer.</p>

     */

    enum LitoJoinFailure {

        /** No dedicated localized message; native common cleanup only. */

        SILENT_CLEANUP(1),

        /** LTstring[28]: the battle room has no remaining participant seat. */

        ROOM_FULL(3),

        /** LTstring[29]: the battle has already started. */

        BATTLE_STARTED(4),

        /** LTstring[30]: the battle room was closed. */

        ROOM_CLOSED(5),

        /** LTstring[85]: this participant was removed and cannot re-enter. */

        KICKED(7),

        /** LTstring[31]: an upper-tier player cannot enter this room. */

        UPPER_TIER_RESTRICTED(10),

        /** LTstring[32]: the supplied room password is incorrect. */

        BAD_PASSWORD(11),

        /** LTstring[33]: the battle ended while its result UI remains active. */

        BATTLE_ENDED(12),

        /** LTstring[34]: an assigned event team may enter its named room only. */

        TEAM_ROOM_ONLY(13),

        /** LTstring[96]: this room does not allow observers. */

        OBSERVER_DISABLED(14),

        /** LTstring[294]: the selected event class is not allowed. */

        CLASS_RESTRICTED(20),

        /** LTstring[295]: the selected source server is not allowed. */

        SERVER_RESTRICTED(21);



        private final int wireValue;



        LitoJoinFailure(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }

    record LitoCharacterSourceRequest(String sourceName) {}

    /** Exact C_D7 selector mapping recovered from sub_45AAA0 and 7831c..7849c. */

    enum LitoRankingScope {

        OPEN(-1), MONTHLY(0), OVERALL(1), CHANNEL(2), CLASSIC_SERVER(3), RACE(4);



        private final int wireValue;



        LitoRankingScope(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static LitoRankingScope fromWire(int value) {

            for (LitoRankingScope scope : values()) {

                if (scope.wireValue == value) return scope;

            }

            throw new IllegalArgumentException("invalid native C_D7 ranking selector");

        }

    }

    /** Five current-rank fields and their signed native movement companions. */

    record LitoCharacterRankings(

            int overall, int classicServer, int channel, int level, int race,

            int overallMovement, int classicServerMovement, int channelMovement,

            int levelMovement, int raceMovement) {

        LitoCharacterRankings {

            if (overall < 0 || classicServer < 0 || channel < 0

                    || level < 0 || race < 0) {

                throw new IllegalArgumentException("native LiTo ranks cannot be negative");

            }

        }



        static LitoCharacterRankings unknown() {

            return new LitoCharacterRankings(0, 0, 0, 0, 0,

                    0, 0, 0, 0, 0);

        }

    }



    /**

     * Exact battle-statistic consumers in the selected-character UI.

     *

     * <p>{@code winRateDenominator} is the divisor used by native

     * {@code sub_4928C0} for {@code wins / denominator * 100}.  Disconnects

     * are a subset of losses in the server's decided-result model, so a

     * disconnected result is counted once in the denominator.</p>

     */

    record LitoCharacterBattleStatistics(

            int disconnects, int winRateDenominator, int wins, int losses,

            int rankingSelector) {

        LitoCharacterBattleStatistics {

            if (disconnects < 0 || winRateDenominator < 0 || wins < 0 || losses < 0

                    || rankingSelector < 0 || rankingSelector > 4

                    || disconnects > losses

                    || (long) wins + losses != winRateDenominator) {

                throw new IllegalArgumentException("invalid native LiTo battle statistics");

            }

        }



        static LitoCharacterBattleStatistics empty() {

            return new LitoCharacterBattleStatistics(0, 0, 0, 0, 0);

        }

    }



    /**

     * One exact LiTo character-directory row consumed by {@code sub_4A7550}.

     *

     * <p>The native client stores four 0xB4-byte rows; the launcher pages up to eight. The first

     * fields retain the classic 3.81 character-list meanings; the LiTo-only

     * extension carries five ranking pairs and the selected-character battle

     * statistics in their recovered native order.</p>

     */

    record LitoCharacterDirectoryRow(

            String name, String clanName, int type, int sex, int lawful,

            int hp, int mp, int armorClass, int level,

            int strength, int dexterity, int constitution, int wisdom,

            int charisma, int intelligence, long nativeTime,

            LitoCharacterRankings rankings,

            LitoCharacterBattleStatistics battleStatistics) {

        LitoCharacterDirectoryRow(

                String name, String clanName, int type, int sex, int lawful,

                int hp, int mp, int armorClass, int level,

                int strength, int dexterity, int constitution, int wisdom,

                int charisma, int intelligence, long nativeTime) {

            this(name, clanName, type, sex, lawful, hp, mp, armorClass, level,

                    strength, dexterity, constitution, wisdom, charisma,

                    intelligence, nativeTime, LitoCharacterRankings.unknown(),

                    LitoCharacterBattleStatistics.empty());

        }

    }

    enum LitoChannelTarget { CHANNEL, ARGO, CHANNEL_SELECTION }



    /**

     * Complete C_CA producer domain in the selected LiTo.bin.

     *

     * <p>The four native selector callbacks own values 0..3, the /argo

     * callback owns 99 and the return/reset callbacks own 100. No producer

     * in the complete eight-site census can serialize any other byte.</p>

     */

    enum LitoChannelSelection {

        CHANNEL_0(0, LitoChannelTarget.CHANNEL),

        CHANNEL_1(1, LitoChannelTarget.CHANNEL),

        CHANNEL_2(2, LitoChannelTarget.CHANNEL),

        CHANNEL_3(3, LitoChannelTarget.CHANNEL),

        ARGO(99, LitoChannelTarget.ARGO),

        CHANNEL_SELECTION(100, LitoChannelTarget.CHANNEL_SELECTION);



        private final int wireValue;

        private final LitoChannelTarget target;



        LitoChannelSelection(int wireValue, LitoChannelTarget target) {

            this.wireValue = wireValue;

            this.target = target;

        }



        int wireValue() {

            return wireValue;

        }



        LitoChannelTarget target() {

            return target;

        }



        static LitoChannelSelection fromWire(int value) {

            return switch (value) {

                case 0 -> CHANNEL_0;

                case 1 -> CHANNEL_1;

                case 2 -> CHANNEL_2;

                case 3 -> CHANNEL_3;

                case 99 -> ARGO;

                case 100 -> CHANNEL_SELECTION;

                default -> throw new IllegalArgumentException(

                        "invalid native C_CA channel selection=" + value);

            };

        }



        static LitoChannelSelection activeChannel(int value) {

            LitoChannelSelection selection = fromWire(value);

            if (selection.target == LitoChannelTarget.CHANNEL_SELECTION) {

                throw new IllegalArgumentException(

                        "native channel selector sentinel is not an active channel");

            }

            return selection;

        }

    }

    /**

     * Native {@code sub_4A7850} S_C9 warning branches.

     *

     * <p>Result 1 displays LTstring[94] (the selected channel is not in

     * service). Result 10 displays LTstring[95] (the selected character's

     * level cannot enter a channel). Results 2..9 have no dedicated branch in

     * this client image and must not be used as generic server errors.</p>

     */

    enum LitoEnterChannelFailure {

        CHANNEL_NOT_IN_SERVICE(1),

        CHARACTER_LEVEL_RESTRICTED(10);



        private final int wireValue;



        LitoEnterChannelFailure(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }

    }

    enum LitoRoomSettingsLayout { COMPACT, OPTIONS }

    record LitoRoomSettingsRequest(String name, int attributes, String password,

                                   int capacity, LitoRoomSettingsLayout layout,

                                   int option1, int option2, int option3, int option4,

                                   List<Integer> selections) {

        LitoRoomSettingsRequest {

            selections = List.copyOf(selections);

        }



        boolean hasOptions() { return layout == LitoRoomSettingsLayout.OPTIONS; }

        int eventFlags() { return option1; }

        int eventType() { return option2; }

        int winnerCount() { return option3; }

        int allowedClassMask() { return option4; }

        List<Integer> allowedServerIds() { return selections; }

    }

    enum LitoModeStepDirection {

        NEXT(1),

        PREVIOUS(2);



        private final int wireValue;



        LitoModeStepDirection(int wireValue) {

            this.wireValue = wireValue;

        }



        int wireValue() {

            return wireValue;

        }



        static LitoModeStepDirection fromWire(int value) {

            return switch (value) {

                case 1 -> NEXT;

                case 2 -> PREVIOUS;

                default -> throw new IllegalArgumentException(

                        "invalid native C_D1 mode-step direction=" + value);

            };

        }

    }



    sealed interface LitoGameInfoRequest

            permits LitoModeStepRequest, LitoMapSelectionRequest {}



    record LitoModeStepRequest(LitoModeStepDirection direction)

            implements LitoGameInfoRequest {

        LitoModeStepRequest {

            Objects.requireNonNull(direction, "LiTo mode-step direction");

        }

    }



    record LitoMapSelectionRequest(int mapId) implements LitoGameInfoRequest {

        LitoMapSelectionRequest {

            if (mapId < 0 || mapId > 0xFFFF) {

                throw new IllegalArgumentException(

                        "native C_D1 map id outside unsigned word=" + mapId);

            }

        }

    }

    enum LitoReadyOperation { OWNER_START, PARTICIPANT_STATE }

    record LitoReadyRequest(LitoReadyOperation operation,

                            LitoParticipantState participantState) {

        LitoReadyRequest {

            if (operation == null) {

                throw new IllegalArgumentException("null LiTo ready operation");

            }

            if (operation == LitoReadyOperation.OWNER_START

                    && participantState != null) {

                throw new IllegalArgumentException(

                        "owner-start envelope has no participant state");

            }

            if (operation == LitoReadyOperation.PARTICIPANT_STATE

                    && participantState == null) {

                throw new IllegalArgumentException(

                        "participant-state envelope requires a state");

            }

        }



        boolean ownerStart() {

            return operation == LitoReadyOperation.OWNER_START;

        }

    }

    record LitoInviteReplyRequest(String inviterName, LitoInvitationReply reply) {

        LitoInviteReplyRequest {

            requireBoundedLegacyString(inviterName, 1, 27,

                    "C_LITO_INVITE_REPLY inviter name");

            if (reply == null) {

                throw new IllegalArgumentException("null LiTo invitation reply");

            }

        }

    }

    record LitoFindUserRequest(String query) {}

    enum LitoBuilderAdminOperation {

        FORCE_WIN,

        PREPARE,

        LOAD,

        ITEM,

        KICK,

        ARENA_WHO,

        TELEPORT

    }

    record LitoBuilderAdminRequest(int subtype, LitoBuilderAdminOperation operation,

                                   int firstWord, int secondWord, int thirdWord,

                                   String targetName) {}

    enum BuilderControlLayout {

        SIGNAL,

        BYTE,

        DWORD,

        TWO_DWORDS,

        TWO_BYTES,

        TWO_BYTES_TEXT,

        TEXT,

        DWORD_TEXT,

        NOTICE

    }

    record BuilderControlRequest(int subtype, BuilderControlLayout layout,

                                 Long firstDword, Long secondDword,

                                 Integer firstByte, Integer secondByte,

                                 String text) {}

    record SlaveControlRequest(long controllerObjectId, int mode, long targetObjectId) {}

    record SpellSelector(int row, int column) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record SelfSpellRequest(int row, int column, long clientTick) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record ObjectSpellRequest(int row, int column, long targetObjectId) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record TeleportSpellRequest(int row, int column, int mapId, int x, int y) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record MassTeleportSpellRequest(int row, int column, int mapId, int x, int y) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record CallClanSpellRequest(int row, int column, String targetName) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record RunClanSpellRequest(int row, int column, String targetName) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record SummonSpellRequest(int row, int column, long selection) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record AttackSpellRequest(int row, int column, long targetObjectId, int x, int y) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record TrueTargetRequest(int row, int column, long targetObjectId,

                             int x, int y, String text) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record AttackAllTarget(long objectId, boolean hit) {}

    record BookListEntry(String name, long timestamp) {}

    record AttackManyTarget(long objectId, boolean hit) {}

    record GroundSpellRequest(int row, int column, int x, int y) {

        int skillId() { return (row << 3) + column + 1; }

    }

    record TeleportRequest(boolean includesMap, int mapId, int x, int y) {}

    record PortalDestination(int mapId, int x, int y, boolean accepted) {}

    record TitleRequest(String characterName, String title) {}

    record CustomCharacterRequest(String name, int sex, int characterClass,

                                  List<Integer> attributes, int value1, int value2) {}

    record ClassicCharacterRequest(String name, boolean hasNativeDeleteValue,

                                   long nativeDeleteValue) {}



    record InventoryWireItem(long objectId, int useType, int chargeCount, int icon,

                             int blessing, long count, int identified,

                             String name, byte[] detail) {}



    /**

     * Native LiTo transfer stone inventory contract.

     *

     * <p>{@code sub_442A30/sub_49D5E0} subtract 20000 before indexing the

     * LTicon/LTdesc tables.  LT record 155 therefore travels on S_16 as icon

     * 20155, not 155.  Its use type is zero so {@code sub_445C10} reaches the

     * default {@code sub_445BD0} C_USE_ITEM producer ({@code cds}).</p>

     */

    static InventoryWireItem litoTransferStone(long objectId) {

        requireDwordId(objectId, "LiTo transfer-stone object id");

        return new InventoryWireItem(

                objectId, 0, 0, LITO_TRANSFER_STONE_ICON,

                1, 1, 1, "傳送石", new byte[0]);

    }



    /**

     * Native rental-village Light Gem presentation record.

     *

     * <p>{@code LitoHelp7/LitoHelp12} prove the initial grant and currency

     * role; {@code LTdesc[5]} and {@code LTicon/5.lbt} prove the displayed

     * resource.  Quantity and merchant exchange values remain server-policy

     * fields until an original-server inventory capture is recovered.</p>

     */

    static InventoryWireItem litoRentalLightGem(long objectId, long count) {

        requireDwordId(objectId, "LiTo Light-Gem object id");

        if (count <= 0 || count > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("LiTo Light-Gem count out of range: " + count);

        }

        return new InventoryWireItem(

                objectId, 0, 0, LITO_LIGHT_GEM_ICON,

                1, count, 1, "光之寶石", new byte[0]);

    }



    /**

     * Native rental-village map presentation with an explicit safe use policy.

     *

     * <p>The selected client names LT record 128 {@code 無界擂台地圖}.  In

     * {@code sub_442860}, the S_16/S_7C use-type byte is stored at item+0x6C

     * while icon 20128 is stored independently at item+0x6E.  The double-click

     * dispatcher {@code sub_445C10} selects behavior from item+0x6C, not the

     * icon.  Policy value zero reaches default {@code sub_445BD0} and emits the

     * exact generic {@code C_USE_ITEM cds} request.  Neither the historical

     * use-type byte nor its original-server response has been recovered, so a

     * click remains fail-closed instead of fabricating a map UI.</p>

     */

    static InventoryWireItem litoRentalTownMap(long objectId) {

        requireDwordId(objectId, "LiTo town-map object id");

        return new InventoryWireItem(

                objectId, LITO_TOWN_MAP_USE_TYPE_POLICY, 0, LITO_TOWN_MAP_ICON,

                1, 1, 1, "無界擂台地圖", new byte[0]);

    }



    /** Exact LTdesc/LTicon presentation for the four Lodun citizenship emblems. */

    static InventoryWireItem litoRentalTownEmblem(

            long objectId, int resourceIndex, String name) {

        requireDwordId(objectId, "LiTo citizenship-emblem object id");

        requireNativeString(name, "LiTo citizenship-emblem name");

        if (resourceIndex < NativeLitoRentalTownNpcCatalog.AUTHORITY_EMBLEM_INDEX

                || resourceIndex > NativeLitoRentalTownNpcCatalog.PEACE_EMBLEM_INDEX

                || name.isBlank()) {

            throw new IllegalArgumentException("invalid LiTo citizenship emblem");

        }

        return new InventoryWireItem(

                objectId, 0, 0, LITO_RESOURCE_WIRE_BASE + resourceIndex,

                1, 1, 1, name, new byte[0]);

    }



    record PersonalShopOffer(long objectId, long unitPrice, long quantity) {}

    record PersonalShopRequest(int type, List<PersonalShopOffer> sellOffers,

                               List<PersonalShopOffer> buyOffers, byte[] announcement) {

        PersonalShopRequest {

            sellOffers = sellOffers == null ? List.of() : List.copyOf(sellOffers);

            buyOffers = buyOffers == null ? List.of() : List.copyOf(buyOffers);

            announcement = announcement == null ? new byte[0] : announcement.clone();

        }



        @Override public byte[] announcement() { return announcement.clone(); }

    }

    record PersonalShopQuery(int type, long targetObjectId) {}

    record PersonalShopSellRow(int index, int blessing, int icon,

                               long remainingQuantity, long unitPrice,

                               String name, byte[] detail) {

        PersonalShopSellRow {

            detail = detail == null ? new byte[0] : detail.clone();

        }



        @Override public byte[] detail() { return detail.clone(); }

    }

    record PersonalShopBuyRow(int index, long inventoryObjectId,

                              long remainingQuantity, long unitPrice) {}



    record ShopTransaction(boolean hasContext, long contextId, int operation,

                           List<ShopLine> lines, byte[] nativeTrailer) {

        ShopTransaction {

            lines = lines == null ? List.of() : List.copyOf(lines);

            nativeTrailer = nativeTrailer == null ? new byte[0] : nativeTrailer.clone();

        }



        ShopTransaction(boolean hasContext, long contextId, int operation,

                        List<ShopLine> lines) {

            this(hasContext, contextId, operation, lines, new byte[0]);

        }



        @Override public byte[] nativeTrailer() { return nativeTrailer.clone(); }

        boolean exactNativeTrailer() { return nativeTrailer.length == 8; }

    }



    /** Native C_GET producers sub_4062E0/sub_406620/sub_4066F0: {@code chhddc}. */

    static GroundPickup groundPickup(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_GET) throw new IllegalArgumentException("not a native C_GET packet");

        GroundPickup request = new GroundPickup(in.h(), in.h(), in.d(), in.d(), in.c());

        if (request.worldObjectId() == 0 || request.quantity() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_GET layout");

        }

        return request;

    }



    /** Native C_GET stores {@code (worldX-0x8000)>>1 + 0x8000} in its x word. */

    static int groundPickupWireX(int worldX) {

        if (worldX < 0 || worldX > 0xFFFF) {

            throw new IllegalArgumentException("invalid native ground world x");

        }

        return ((worldX - 0x8000) >> 1) + 0x8000;

    }



    /**

     * Native door/open-object producer {@code sub_4062E0}: {@code chhd}.

     *

     * <p>The two words observed from the protected LiTo.bin are the selected

     * object's world X/Y coordinates, followed by its world-object id.  The

     * producer performs an isometric conversion on its internal click-space

     * field before serializing; that internal field is not the world X value

     * itself.  Applying the C_GET transform a second time on the server is

     * therefore incorrect.</p>

     */

    static OpenRequest openRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_OPEN) throw new IllegalArgumentException("not a native C_OPEN packet");

        OpenRequest request = new OpenRequest(in.h(), in.h(), in.d());

        if (request.worldObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_OPEN layout");

        }

        return request;

    }



    /**

     * Native builder fallback producers {@code sub_4068A0/sub_406B40}:

     * {@code cs}.  This recovers the exact envelope only; the server must not

     * infer an item/catalogue mutation until a matching authoritative handler

     * is established.

     */

    static String wishRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_WISH) throw new IllegalArgumentException("not a native C_WISH packet");

        String wish = in.s();

        int bytes = wish.getBytes(LEGACY_TEXT).length;

        if (bytes == 0 || bytes > 0xFF || in.remaining() != 0

                || !LEGACY_TEXT.newEncoder().canEncode(wish)

                || !wish.equals(wish.trim()) || containsControl(wish)) {

            throw new IllegalArgumentException("invalid native C_WISH layout");

        }

        return wish;

    }



    /** Native C_DROP producer sub_443FF0: {@code chhdd}. */

    static InventoryDrop inventoryDrop(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_DROP) throw new IllegalArgumentException("not a native C_DROP packet");

        InventoryDrop request = new InventoryDrop(in.h(), in.h(), in.d(), in.d());

        if (request.inventoryObjectId() == 0 || request.quantity() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_DROP layout");

        }

        return request;

    }



    /** Native C_GIVE producers sub_4440F0/sub_444B40: {@code cdhhdd}. */

    static GiveRequest giveRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_GIVE) throw new IllegalArgumentException("not a native C_GIVE packet");

        GiveRequest request = new GiveRequest(in.d(), in.h(), in.h(), in.d(), in.d());

        if (request.targetObjectId() == 0 || request.inventoryObjectId() == 0

                || request.quantity() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_GIVE layout");

        }

        return request;

    }



    /** Native default/use-type-1 C_USE_ITEM producer sub_445BD0: {@code cds}. */

    static InventoryUse defaultInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        InventoryUse request = new InventoryUse(in.d(), in.s());

        if (request.inventoryObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native default C_USE_ITEM layout");

        }

        return request;

    }



    /**

     * Native blank-letter producer {@code sub_479680}: {@code cdhsSS}.

     * The protected client stores the recipient in 0x1c bytes, the subject in

     * 0x2a bytes and the content in 0x202 bytes. Upper-case {@code S} is a

     * terminated UTF-16LE string, not an MS950 string.

     */

    static LetterComposeInventoryUse letterComposeInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) {

            throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        }

        LetterComposeInventoryUse request = new LetterComposeInventoryUse(

                in.d(), in.h(), in.s(), in.S(), in.S());

        int receiverBytes = request.receiver().getBytes(LEGACY_TEXT).length;

        if (request.inventoryObjectId() == 0 || request.receiver().isEmpty()

                || !request.receiver().equals(request.receiver().trim())

                || receiverBytes > 27 || !LEGACY_TEXT.newEncoder().canEncode(request.receiver())

                || containsControl(request.receiver()) || request.receiver().indexOf(';') >= 0

                || request.subject().length() > 20 || request.content().length() > 256

                || containsUnsafeLetterControl(request.subject())

                || containsUnsafeLetterControl(request.content())

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native blank-letter C_USE_ITEM layout");

        }

        return request;

    }



    /**

     * Reads only the common opcode/object-id prefix so the server can select

     * the exact native C_USE_ITEM tail from authoritative item metadata.

     */

    static long inventoryUseObjectId(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        long objectId = in.d();

        if (objectId == 0) throw new IllegalArgumentException("invalid native C_USE_ITEM object id");

        return objectId;

    }



    /** Native use-type-10 C_USE_ITEM producer in sub_445C10: {@code cdd}. */

    static LightInventoryUse lightInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        LightInventoryUse request = new LightInventoryUse(in.d(), in.d());

        if (request.inventoryObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native light C_USE_ITEM layout");

        }

        return request;

    }



    /**

     * Native use-type-9 producer {@code sub_407570}: {@code cdhhh}.

     * The selected client sends its current map followed by two zero words;

     * the server never trusts those words as a destination.

     */

    static HomeTeleportInventoryUse homeTeleportInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) {

            throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        }

        HomeTeleportInventoryUse request = new HomeTeleportInventoryUse(

                in.d(), in.h(), in.h(), in.h());

        if (request.inventoryObjectId() == 0 || request.currentMapId() <= 0

                || request.targetX() != 0 || request.targetY() != 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "invalid native home-teleport C_USE_ITEM layout");

        }

        return request;

    }



    /** Native use-type-7 callback sub_407760: {@code cdd(scroll,target)}. */

    static AppraisalInventoryUse appraisalInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        AppraisalInventoryUse request = new AppraisalInventoryUse(in.d(), in.d());

        if (request.inventoryObjectId() == 0 || request.targetObjectId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native appraisal C_USE_ITEM layout");

        }

        return request;

    }



    /** Native use-type-14 callback sub_407890: {@code cdd(item,target)}. */

    static ChoiceInventoryUse choiceInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) {

            throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        }

        ChoiceInventoryUse request = new ChoiceInventoryUse(in.d(), in.d());

        if (request.inventoryObjectId() == 0 || request.targetObjectId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native choice C_USE_ITEM layout");

        }

        return request;

    }



    /** Native use-type-5 callback {@code sub_406D80}: {@code cddhh}. */

    static TargetLocationInventoryUse targetLocationInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) {

            throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        }

        TargetLocationInventoryUse request = new TargetLocationInventoryUse(

                in.d(), in.d(), in.h(), in.h());

        if (request.inventoryObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "invalid native target-location C_USE_ITEM layout");

        }

        return request;

    }



    /** Native ship-ticket helper sub_447B50 emits {@code cdc}, marker 1. */

    static ShipTicketInventoryUse shipTicketInventoryUse(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_ITEM) throw new IllegalArgumentException("not a native C_USE_ITEM packet");

        ShipTicketInventoryUse request = new ShipTicketInventoryUse(in.d(), in.c());

        if (request.inventoryObjectId() == 0 || request.marker() != 1 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native ship-ticket C_USE_ITEM layout");

        }

        return request;

    }



    /**

     * Native C_GOTO_MAP is either {@code ch(0xFFFF)} cancellation or

     * {@code chhh(destinationMap,x,y)} from the ship animation producers.

     */

    static GotoMapRequest gotoMapRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_GOTO_MAP) throw new IllegalArgumentException("not a native C_GOTO_MAP packet");

        if (in.remaining() == 2) {

            if (in.h() != 0xFFFF || in.remaining() != 0) {

                throw new IllegalArgumentException("invalid native C_GOTO_MAP cancellation");

            }

            return new GotoMapRequest(true, -1, -1, -1);

        }

        if (in.remaining() != 6) {

            throw new IllegalArgumentException("invalid native C_GOTO_MAP layout");

        }

        GotoMapRequest request = new GotoMapRequest(false, in.h(), in.h(), in.h());

        if (request.destinationMap() == 0xFFFF || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_GOTO_MAP destination");

        }

        return request;

    }



    /** Exact LiTo 3.81C entership callback sub_407C70: {@code cc(0|1)}. */

    static EnterShipRequest enterShipRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_ENTER_SHIP) {

            throw new IllegalArgumentException("not a native C_ENTER_SHIP packet");

        }

        int selector = in.c();

        if ((selector != 0 && selector != 1) || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_ENTER_SHIP layout");

        }

        return new EnterShipRequest(selector);

    }



    /** Exact LiTo 3.81C ground-portal producer sub_48BFA0: {@code chh(x,y)}. */

    static EnterPortalRequest enterPortalRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_ENTER_PORTAL || in.remaining() != 4) {

            throw new IllegalArgumentException("invalid native C_ENTER_PORTAL layout");

        }

        EnterPortalRequest request = new EnterPortalRequest(in.h(), in.h());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing native C_ENTER_PORTAL bytes");

        }

        return request;

    }



    /**

     * Native C_ADDR producer in S_EVENT handler {@code sub_4B07C0}, case 33:

     * {@code cddd}.  Despite the historical name, these fields are an opaque

     * challenge id, the seeded CRC32 result and a protected client-state bit;

     * no network address is present on the wire.

     */

    static AddressProof addressProof(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_ADDR || in.remaining() != 12) {

            throw new IllegalArgumentException("invalid native C_ADDR length");

        }

        AddressProof proof = new AddressProof(in.d(), in.d(), in.d());

        if (in.remaining() != 0 || proof.challengeId() == 0 || proof.protectedState() > 1) {

            throw new IllegalArgumentException("invalid native C_ADDR proof");

        }

        return proof;

    }



    /**

     * Exact login callback {@code sub_492090}: direct launch emits

     * {@code cssd}; command-line connector mode ({@code byte_64BBF8 != 0})

     * emits {@code cssds}. The optional final string comes from the native

     * 32-byte {@code dword_64B97C} command-line buffer. It is compatibility

     * metadata only and must never participate in authentication or endpoint

     * authorization.

     */

    static LoginRequest loginRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LOGIN) {

            throw new IllegalArgumentException("not a native C_LOGIN packet");

        }

        String account = in.s();

        String password = in.s();

        long clientAddress = in.d();

        String connector = "";

        LoginLayout layout = LoginLayout.DIRECT;

        byte[] connectorWire = new byte[0];

        if (in.remaining() != 0) {

            connectorWire = in.zBytes();

            connector = new String(connectorWire, LEGACY_TEXT);

            layout = LoginLayout.CONNECTOR;

        }

        byte[] connectorBytes = connector.getBytes(LEGACY_TEXT);

        if (in.remaining() != 0 || connectorBytes.length > 31

                || !Arrays.equals(connectorWire, connectorBytes)

                || containsControl(connector)) {

            throw new IllegalArgumentException("invalid native C_LOGIN layout");

        }

        return new LoginRequest(account, password, clientAddress, connector, layout);

    }



    /** Exact {@code sub_480560} client version envelope: {@code chdch}. */

    static VersionRequest versionRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_VERSION) {

            throw new IllegalArgumentException("not a native C_VERSION packet");

        }

        VersionRequest request = new VersionRequest(in.h(), in.d(), in.c(), in.h());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing native C_VERSION bytes");

        }

        return request;

    }



    static boolean supportedVersion(VersionRequest request) {

        return request.magic() == VERSION_MAGIC

                && request.product() == VERSION_PRODUCT

                && request.build() == VERSION_BUILD;

    }



    /**

     * Exact successful {@code sub_4A33E0} receiver union.

     *

     * <p>The broken IDA function boundary hides the native continuation.  It

     * consumes {@code c(result=0)}, then {@code cdddddcc}, one country byte,

     * and—because serverVersion is at least {@code 0x9E38}—one server-type

     * dword.  Unlike the related 3.80 packet, this image does not consume the

     * later uptime/short/byte tail.</p>

     */

    static byte[] versionAccepted() {

        return new Writer()

                .c(S_VERSION_CHECK)

                .c(VERSION_ACCEPTED)

                .c(0x0D)

                .d(0x0734FD33L)

                .d(0x0734FD30L)

                .d(0x77CF6EBAL)

                .d(0x0734FD31L)

                .d(0)

                .c(0).c(0)

                .c(3)

                .d(0x00077D82L)

                .bytes();

    }



    /** Result 1 is the only short rejection branch proved by sub_4A33E0. */

    static byte[] versionRejected() {

        return new Writer().c(S_VERSION_CHECK).c(VERSION_REJECTED).bytes();

    }



    /**

     * Exact LiTo 3.81C {@code S_LOGIN_CHECK} receiver envelope: {@code ccdsch}.

     *

     * <p>{@code sub_4903D0} receives the packet at its opcode.  Opcode zero

     * always selects the long {@code cdsch} body parser, regardless of the

     * result selector.  The compact {@code cd} body belongs only to the

     * nonzero S_02/S_08/S_67 opcodes that share that native function.</p>

     */

    static byte[] loginCheck(LoginCheckResult result, long value, String serverName,

                             int flag, int nativeWord) {

        Objects.requireNonNull(result, "login result");

        requireUnsignedDword(value, "login value");

        requireByte(flag, "login flag");

        requireUnsignedWord(nativeWord, "login native word");

        String name = serverName == null ? "" : serverName;

        byte[] encodedName = name.getBytes(LEGACY_TEXT);

        // sub_4903D0 passes 0x29 to the native string parser for var_2C:

        // at most 40 encoded bytes plus the terminating NUL.

        if (encodedName.length == 0 || encodedName.length > 0x28

                || containsControl(name)

                || !Arrays.equals(encodedName, new String(encodedName, LEGACY_TEXT)

                        .getBytes(LEGACY_TEXT))) {

            throw new IllegalArgumentException("invalid native login server name");

        }

        return new Writer()

                .c(S_LOGIN_CHECK)

                .c(result.wireValue())

                .d(value)

                .s(name)

                .c(flag)

                .h(nativeWord)

                .bytes();

    }



    /**

     * Native {@code S_SERVER_LIST} receiver {@code sub_48FAA0} consumes

     * {@code c(count) + count * sdh}.  Each native record has a 0x40-byte

     * name field including NUL and a 0x48-byte total stride.  The receiver

     * sign-extends the count before allocating that array, so values 128..255

     * are not safe members of this exact client contract.

     */

    static byte[] classicServerList(List<ClassicServerEndpoint> endpoints) {

        if (endpoints == null || endpoints.isEmpty() || endpoints.size() > 0x7F) {

            throw new IllegalArgumentException("native S_SERVER_LIST count must be 1..127");

        }

        Writer packet = new Writer().c(S_SERVER_LIST).c(endpoints.size());

        for (ClassicServerEndpoint endpoint : endpoints) {

            if (endpoint == null) {

                throw new IllegalArgumentException("native S_SERVER_LIST endpoint is required");

            }

            String name = endpoint.name() == null ? "" : endpoint.name();

            byte[] encoded = name.getBytes(LEGACY_TEXT);

            if (encoded.length == 0 || encoded.length > 0x3F

                    || containsControl(name)

                    || !Arrays.equals(encoded, new String(encoded, LEGACY_TEXT)

                            .getBytes(LEGACY_TEXT))) {

                throw new IllegalArgumentException("invalid native S_SERVER_LIST source name");

            }

            requireUnsignedDword(endpoint.ipv4(), "classic-server IPv4");

            if (endpoint.port() < 1 || endpoint.port() > 0xFFFF) {

                throw new IllegalArgumentException("invalid native S_SERVER_LIST port");

            }

            packet.s(name).d(endpoint.ipv4()).h(endpoint.port());

        }

        return packet.bytes();

    }



    /** Both native {@code C_SERVER_SELECT} producers emit opcode-only 0x35. */

    static void requireClassicServerSelect(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_SERVER_SELECT || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_SERVER_SELECT layout");

        }

    }



    /** Native {@code S_LITO_VS_SERVER_IP} receiver {@code sub_4AC570}: {@code cdh}. */

    static byte[] litoVsServerIp(int serverIndex, long ipv4, int port) {

        // sub_4AC570 uses movsx and directly indexes the ten-dword S_F3 table;

        // it performs no bounds check of its own.

        if (serverIndex < 0 || serverIndex >= MAX_LITO_VS_SERVERS) {

            throw new IllegalArgumentException("invalid native VS-server index");

        }

        requireUnsignedDword(ipv4, "VS-server IPv4");

        if (port < 1 || port > 0xFFFF) {

            throw new IllegalArgumentException("invalid native VS-server port");

        }

        return new Writer().c(S_LITO_VS_SERVER_IP).c(serverIndex)

                .d(ipv4).h(port).bytes();

    }



    /** Native C_CHECK_INVENTORY/C_DESTROY_ITEM {@code cd} object-id envelope. */

    static long inventoryObjectRequest(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_CHECK_INVENTORY && expectedOpcode != C_DESTROY_ITEM) {

            throw new IllegalArgumentException("unsupported inventory object request opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode) throw new IllegalArgumentException("wrong inventory request opcode");

        long objectId = in.d();

        if (objectId == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native inventory object request");

        }

        return objectId;

    }



    /** Native registration form sub_494160: {@code csssscsssssssssssss}. */

    static NewAccountRequest newAccountRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_NEW_ACCOUNT) {

            throw new IllegalArgumentException("not a native C_NEW_ACCOUNT packet");

        }

        String account = in.s();

        String password = in.s();

        List<String> profile = new ArrayList<>(15);

        profile.add(in.s());

        profile.add(in.s());

        int type = in.c();

        for (int index = 0; index < 13; index++) profile.add(in.s());

        if (in.remaining() != 0 || !nativeAccount(account)

                || !nativeNewPassword(password) || (type != 0 && type != 1)

                || profile.stream().anyMatch(value -> value.getBytes(LEGACY_TEXT).length > 0x3FF)) {

            throw new IllegalArgumentException("invalid native C_NEW_ACCOUNT layout");

        }

        return new NewAccountRequest(account, password, type, List.copyOf(profile));

    }



    /** Native password form sub_48FCF0: {@code csss}. */

    static PasswordChangeRequest passwordChangeRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_CHANGE_PASSWORD) {

            throw new IllegalArgumentException("not a native C_CHANGE_PASSWORD packet");

        }

        PasswordChangeRequest request = new PasswordChangeRequest(in.s(), in.s(), in.s());

        int oldBytes = request.oldPassword().getBytes(LEGACY_TEXT).length;

        if (in.remaining() != 0 || !nativeAccount(request.account())

                || oldBytes < 4 || oldBytes > 13 || !nativeNewPassword(request.newPassword())) {

            throw new IllegalArgumentException("invalid native C_CHANGE_PASSWORD layout");

        }

        return request;

    }



    /**

     * Native security-question form {@code sub_4900B0}: {@code csssssss}.

     * The fourth UI password field is confirmation-only and is not sent.  The

     * wire order is account, old password, new password, question/answer pairs.

     */

    static QuizRegistrationRequest quizRegistrationRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_REGISTER_QUIZ) {

            throw new IllegalArgumentException("not a native C_REGISTER_QUIZ packet");

        }

        String account = in.s();

        String oldPassword = in.s();

        String newPassword = in.s();

        List<String> quizFields = List.of(in.s(), in.s(), in.s(), in.s());

        int oldBytes = oldPassword.getBytes(LEGACY_TEXT).length;

        int question1Bytes = quizFields.get(0).getBytes(LEGACY_TEXT).length;

        int answer1Bytes = quizFields.get(1).getBytes(LEGACY_TEXT).length;

        int question2Bytes = quizFields.get(2).getBytes(LEGACY_TEXT).length;

        int answer2Bytes = quizFields.get(3).getBytes(LEGACY_TEXT).length;

        if (in.remaining() != 0 || !nativeAccount(account)

                || oldBytes < 4 || oldBytes > 13 || !nativeNewPassword(newPassword)

                || oldPassword.equals(newPassword)

                || question1Bytes < 10 || question1Bytes > 0x1FF

                || question2Bytes < 10 || question2Bytes > 0x1FF

                || answer1Bytes < 4 || answer1Bytes > 0x3F

                || answer2Bytes < 4 || answer2Bytes > 0x3F) {

            throw new IllegalArgumentException("invalid native C_REGISTER_QUIZ layout");

        }

        return new QuizRegistrationRequest(

                account, oldPassword, newPassword, List.copyOf(quizFields));

    }



    /** Native account-information form sub_494770: {@code cssssscsss}. */

    static AccountInfoChangeRequest accountInfoChangeRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_CHANGE_ACCOUNTINFO) {

            throw new IllegalArgumentException("not a native C_CHANGE_ACCOUNTINFO packet");

        }

        String account = in.s();

        List<String> profile = new ArrayList<>(7);

        for (int index = 0; index < 4; index++) profile.add(in.s());

        int type = in.c();

        for (int index = 0; index < 3; index++) profile.add(in.s());

        if (in.remaining() != 0 || !nativeAccount(account)

                || (type != 0 && type != 1)

                || profile.stream().anyMatch(value -> value.getBytes(LEGACY_TEXT).length > 0x7F)) {

            throw new IllegalArgumentException("invalid native C_CHANGE_ACCOUNTINFO layout");

        }

        return new AccountInfoChangeRequest(account, type, List.copyOf(profile));

    }



    /** Native nonzero-opcode branch of sub_4903D0: {@code c c(result) d(value)}. */

    static byte[] newAccountCheck(NewAccountCheckResult result, long value) {

        return nativeCheck(S_NEW_ACCOUNT_CHECK, result, value);

    }



    /** Native nonzero-opcode branch of sub_4903D0: {@code c c(result) d(value)}. */

    static byte[] changePasswordCheck(ChangePasswordCheckResult result, long value) {

        return nativeCheck(S_CHANGE_PASSWORD_CHECK, result, value);

    }



    /** Native nonzero-opcode branch of sub_4903D0: {@code c c(result) d(value)}. */

    static byte[] changeAccountInfoCheck(ChangeAccountInfoCheckResult result, long value) {

        return nativeCheck(S_CHANGE_ACCOUNTINFO_CHECK, result, value);

    }



    private static byte[] nativeCheck(int opcode, NativeCheckResult result, long value) {

        Objects.requireNonNull(result, "native check result");

        requireUnsignedDword(value, "native check value");

        return new Writer().c(opcode).c(result.wireValue()).d(value).bytes();

    }



    /**

     * Exact recovered {@code S_NOTICE} receiver contract in {@code sub_48F600}:

     * the opcode is followed by one little-endian signed 16-bit Text index.

     * Negative indices cannot select a native message and are rejected here.

     */

    static byte[] notice(int messageId) {

        if (messageId < 0 || messageId > 0x7FFF) {

            throw new IllegalArgumentException("S_NOTICE message id is outside signed 16-bit range");

        }

        return new Writer().c(S_NOTICE).h(messageId).bytes();

    }



    /**

     * Native login announcement handled by {@code sub_48F3C0}.  The handler

     * passes the opcode tail directly to the common UI text interpreter;

     * same-family {@code S_CommonNews} fixes that tail as one NUL string.

     */

    static byte[] commonNews(String message) {

        requireNativeString(message, "common-news message");

        if (message.isEmpty()) {

            throw new IllegalArgumentException("common-news message is empty");

        }

        return new Writer().c(S_NEWS).s(message).bytes();

    }



    /** Recovered callback at 0x00494D50 emits opcode-only C_LOGOUT. */

    static void logout(byte[] payload) {

        requireNoticeOpcodeOnly(payload, C_LOGOUT, "C_LOGOUT");

    }



    /** Recovered S_NEWS callback at 0x0048F490 emits opcode-only C_READ_NEWS. */

    static void readNews(byte[] payload) {

        requireNoticeOpcodeOnly(payload, C_READ_NEWS, "C_READ_NEWS");

    }



    /** Exact callback packet produced by {@code sub_48F9B0}: opcode only. */

    static void readNotice(byte[] payload) {

        requireNoticeOpcodeOnly(payload, C_READ_NOTICE, "C_READ_NOTICE");

    }



    /** Exact message-438 callback packet produced by {@code sub_48F9F0}: opcode only. */

    static void loginResult(byte[] payload) {

        requireNoticeOpcodeOnly(payload, C_LOGIN_RESULT, "C_LOGIN_RESULT");

    }



    private static void requireNoticeOpcodeOnly(byte[] payload, int expectedOpcode, String name) {

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native " + name + " opcode-only layout");

        }

    }



    private static boolean nativeAccount(String value) {

        int bytes = value.getBytes(LEGACY_TEXT).length;

        if (bytes < 2 || bytes > 27 || !asciiLetter(value.charAt(0))) return false;

        for (int index = 1; index < value.length(); index++) {

            char ch = value.charAt(index);

            if (!asciiLetter(ch) && (ch < '0' || ch > '9')) return false;

        }

        return true;

    }



    private static boolean nativeNewPassword(String value) {

        int bytes = value.getBytes(LEGACY_TEXT).length;

        if (bytes < 8 || bytes > 13) return false;

        return value.chars().anyMatch(ch -> !asciiLetter((char) ch));

    }



    private static boolean asciiLetter(char value) {

        return (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z');

    }



    /**

     * Native option/channel producers emit {@code ccc}.  The 3.81 server

     * consumes selectors 0/2/6/255 as chat preferences; selectors 3/4/5/7/8

     * are emitted by the client UI but are client-only in the same-family

     * server handler.

     */

    static OnOffRequest onOffRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_ONOFF) throw new IllegalArgumentException("not a native C_ONOFF packet");

        OnOffRequest request = new OnOffRequest(in.c(), in.c());

        boolean clientBooleanSelector = switch (request.selector()) {

            case 0, 2, 3, 4, 5, 6, 7, 8 -> true;

            default -> false;

        };

        if (in.remaining() != 0

                || (!clientBooleanSelector && request.selector() != 0xFF)

                || (clientBooleanSelector && request.value() != 0 && request.value() != 1)) {

            throw new IllegalArgumentException("invalid native C_ONOFF layout");

        }

        return request;

    }



    /** Native C_SAY/C_CHAT producers: {@code ccs}; C_CHAT has fixed type 12. */

    static ChatRequest chatRequest(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_SAY && expectedOpcode != C_CHAT) {

            throw new IllegalArgumentException("unsupported native chat opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode) throw new IllegalArgumentException("wrong native chat opcode");

        ChatRequest request = new ChatRequest(in.c(), in.s());

        int messageBytes = request.message().getBytes(LEGACY_TEXT).length;

        if (messageBytes == 0 || messageBytes > 0xFF || in.remaining() != 0

                || (expectedOpcode == C_CHAT && request.type() != 12)) {

            throw new IllegalArgumentException("invalid native chat layout");

        }

        return request;

    }



    /**

     * Exact native LiTo clan-list callbacks:

     * {@code sub_40A490 -> cc(command=0)} and

     * {@code sub_40A2D0/sub_4B7C10 -> cccs(command=1, role=1..3, name)}.

     */

    static ClanRankControlRequest clanRankControlRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_RANK_CONTROL) {

            throw new IllegalArgumentException("wrong clan-rank opcode");

        }

        int command = in.c();

        if (command == 0) {

            if (in.remaining() != 0) {

                throw new IllegalArgumentException("invalid native clan-rank close layout");

            }

            return new ClanRankControlRequest(0, 0, "");

        }

        if (command != 1) {

            throw new IllegalArgumentException("unsupported native clan-rank command");

        }

        int role = in.c();

        String name = in.s();

        int nameBytes = name.getBytes(LEGACY_TEXT).length;

        if (role < 1 || role > 3 || name.isBlank() || !name.equals(name.trim())

                || nameBytes > 0x1B || containsControl(name) || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native clan-rank assignment layout");

        }

        return new ClanRankControlRequest(command, role, name);

    }



    /** Native C_TELL producers sub_405F90/sub_4189F0/sub_4510D0: {@code css}. */

    static TellRequest tellRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_TELL) throw new IllegalArgumentException("not a native C_TELL packet");

        TellRequest request = new TellRequest(in.s().trim(), in.s());

        int targetBytes = request.target().getBytes(LEGACY_TEXT).length;

        int messageBytes = request.message().getBytes(LEGACY_TEXT).length;

        if (targetBytes == 0 || targetBytes > 27 || messageBytes == 0

                || messageBytes > 0xFF || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_TELL layout");

        }

        return request;

    }



    /**

     * Exact LiTo producers use {@code cs} for C_BAN, C_EXCLUDE, C_KICK and

     * C_INCLUDE.  The client-side edit buffers reserve 28 bytes including the

     * terminator.  C_EXCLUDE alone permits an empty string: the exclusion UI

     * sends it before replaying its current entries, which is the native

     * server-list reset operation.

     */

    static String namedCommandRequest(byte[] payload, int expectedOpcode,

                                      boolean allowEmpty) {

        if (expectedOpcode != C_BAN && expectedOpcode != C_EXCLUDE

                && expectedOpcode != C_KICK && expectedOpcode != C_INCLUDE) {

            throw new IllegalArgumentException("unsupported native named command opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode) {

            throw new IllegalArgumentException("unexpected native named command opcode");

        }

        String name = in.s().trim();

        int nameBytes = name.getBytes(LEGACY_TEXT).length;

        if ((!allowEmpty && nameBytes == 0) || nameBytes > 0x1B

                || containsControl(name) || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native named command layout");

        }

        return name;

    }



    /** Native C_WHO producers sub_402740/sub_40AA80/sub_451280: {@code cs}. */

    static String whoRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_WHO) throw new IllegalArgumentException("not a native C_WHO packet");

        String target = in.s().trim();

        if (target.getBytes(LEGACY_TEXT).length > 27 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_WHO layout");

        }

        return target;

    }



    /**

     * Every recovered LiTo 3.81C C_HACTION producer calls the direct writer

     * with {@code cdss}: world object id, action name and parameter.  Static

     * pet/summon actions use an empty parameter; both native {@code select}

     * producers format an unsigned decimal value through {@code "%u"}.

     * Dynamic HTML actions remain legal because the server originally owns

     * their action strings through S_HYPERTEXT resources.

     */

    static HypertextActionRequest hypertextActionRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_HACTION) {

            throw new IllegalArgumentException("not a native C_HACTION packet");

        }

        long objectId = in.d();

        String action = in.s();

        String parameter = in.s();

        int actionBytes = action.getBytes(LEGACY_TEXT).length;

        int parameterBytes = parameter.getBytes(LEGACY_TEXT).length;

        if (objectId == 0 || actionBytes == 0 || actionBytes > 0xFF

                || parameterBytes > 0xFF || in.remaining() != 0

                || !action.equals(action.trim()) || containsControl(action)

                || containsControl(parameter)) {

            throw new IllegalArgumentException("invalid native C_HACTION layout");

        }



        HypertextActionKind kind = switch (action.toLowerCase(java.util.Locale.ROOT)) {

            case "dismiss" -> HypertextActionKind.DISMISS;

            case "select" -> HypertextActionKind.SELECT;

            case "attackchr" -> HypertextActionKind.ATTACK_TARGET;

            case "aggressive", "defensive", "stay", "extend", "alert" ->

                    HypertextActionKind.POSTURE;

            case "getitem" -> HypertextActionKind.COLLECT;

            case "changename" -> HypertextActionKind.RENAME;

            default -> HypertextActionKind.GENERIC;

        };

        if (kind == HypertextActionKind.SELECT) {

            if (!unsignedDecimalDword(parameter)) {

                throw new IllegalArgumentException("invalid native C_HACTION select parameter");

            }

        } else if (kind != HypertextActionKind.GENERIC && !parameter.isEmpty()) {

            throw new IllegalArgumentException("unexpected native C_HACTION action parameter");

        }

        return new HypertextActionRequest(objectId, action, parameter, kind);

    }



    /** Native sub_4062E0 emits C_DIALOG through the direct writer as {@code cd}. */

    static long dialogObjectId(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_DIALOG) {

            throw new IllegalArgumentException("not a native C_DIALOG packet");

        }

        long objectId = in.d();

        if (objectId == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_DIALOG layout");

        }

        return objectId;

    }



    /**

     * Exact LiTo 3.81C S_HYPERTEXT receiver sub_4A5220 layout:

     * {@code dssh}, followed by the unsigned-word count of repeated

     * NUL-terminated strings.  The second string is retained separately by

     * the client and is round-tripped as the C_HACTION parameter by the

     * generic HTML callback; the built-in anicom/moncom controllers use an

     * empty value and their static action names.

     */

    static byte[] hypertext(long objectId, String htmlId, String parameter,

                            List<String> arguments) {

        requireDwordId(objectId, "hypertext object id");

        requireNativeString(htmlId, "hypertext html id");

        requireNativeString(parameter, "hypertext parameter");

        if (htmlId.isEmpty() || arguments == null || arguments.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_HYPERTEXT values");

        }

        Writer packet = new Writer().c(S_HYPERTEXT).d(objectId)

                .s(htmlId).s(parameter).h(arguments.size());

        for (String argument : arguments) {

            requireNativeString(argument, "hypertext argument");

            packet.s(argument);

        }

        return packet.bytes();

    }



    /**

     * Native empty-html close path of {@code sub_4A5220}.  The receiver still

     * parses its complete {@code dssh} prefix before testing whether the first

     * string is empty, so this is not the shorter legacy {@code d+s} packet.

     */

    static byte[] closeHypertext(long objectId) {

        requireDwordId(objectId, "hypertext close object id");

        return new Writer().c(S_HYPERTEXT).d(objectId).s("").s("").h(0).bytes();

    }



    /** Exact LiTo producer sub_43D3A0: C_TAX is {@code cdh}. */

    static CastleTaxRequest castleTaxRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_TAX) throw new IllegalArgumentException("not a native C_TAX packet");

        CastleTaxRequest request = new CastleTaxRequest(in.d(), in.h());

        if (request.playerObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_TAX layout");

        }

        return request;

    }



    /** Exact LiTo producer sub_43D3A0: C_WITHDRAW/C_DEPOSIT are {@code cdd}. */

    static CastleAmountRequest castleAmountRequest(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_WITHDRAW && expectedOpcode != C_DEPOSIT) {

            throw new IllegalArgumentException("unsupported native castle amount opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode) {

            throw new IllegalArgumentException("unexpected native castle amount opcode");

        }

        CastleAmountRequest request = new CastleAmountRequest(in.d(), in.d());

        if (request.playerObjectId() == 0 || request.amount() == 0

                || request.amount() > Integer.MAX_VALUE || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native castle amount layout");

        }

        return request;

    }



    /** Exact LiTo S_TAX receiver sub_4AE7C0: {@code dccc}. */

    static byte[] castleTax(long playerObjectId, int current, int minimum, int maximum) {

        requireDwordId(playerObjectId, "castle-tax player object id");

        requireByte(current, "castle-tax current");

        requireByte(minimum, "castle-tax minimum");

        requireByte(maximum, "castle-tax maximum");

        if (minimum > current || current > maximum) {

            throw new IllegalArgumentException("castle-tax values are outside native bounds");

        }

        return new Writer().c(S_TAX).d(playerObjectId)

                .c(current).c(minimum).c(maximum).bytes();

    }



    /** Exact LiTo S_WITHDRAW receiver sub_4AE860: {@code dd}. */

    static byte[] castleWithdrawal(long playerObjectId, long publicMoney) {

        requireDwordId(playerObjectId, "castle-withdraw player object id");

        requireSignedNonNegativeDword(publicMoney, "castle-withdraw public money");

        return new Writer().c(S_WITHDRAW).d(playerObjectId).d(publicMoney).bytes();

    }



    /** Exact LiTo S_DEPOSIT receiver sub_4AE970: {@code d}. */

    static byte[] castleDeposit(long playerObjectId) {

        requireDwordId(playerObjectId, "castle-deposit player object id");

        return new Writer().c(S_DEPOSIT).d(playerObjectId).bytes();

    }



    /** Exact LiTo receiver {@code sub_4AEDD0}: {@code cd(crownSlot, ownerObjectId)}. */

    static byte[] castleOwner(int crownSlot, long ownerObjectId) {

        if (crownSlot < 0 || crownSlot > 8) {

            throw new IllegalArgumentException("castle crown slot is outside native 0..8 range");

        }

        requireDwordId(ownerObjectId, "castle owner object id");

        return new Writer().c(S_CASTLE_OWNER).c(crownSlot).d(ownerObjectId).bytes();

    }



    /** Exact LiTo producer {@code sub_407C70}: C64 is {@code cd}. */

    static CastleSecurityQuery castleSecurityQuery(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_QUERY_CASTLE_SECURITY) {

            throw new IllegalArgumentException("not a native C_QUERY_CASTLE_SECURITY packet");

        }

        CastleSecurityQuery request = new CastleSecurityQuery(in.d());

        if (request.stewardObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_QUERY_CASTLE_SECURITY layout");

        }

        return request;

    }



    /**

     * Exact LiTo producers {@code enableclear}/{@code disableclear} in

     * {@code sub_407C70}: C65 is {@code cdcd}.  The mode byte is 1/0 and the

     * trailing dword is the native constant 1 in both recovered branches.

     */

    static CastleSecurityChange castleSecurityChange(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_CHANGE_CASTLE_SECURITY) {

            throw new IllegalArgumentException("not a native C_CHANGE_CASTLE_SECURITY packet");

        }

        long stewardObjectId = in.d();

        int mode = in.c();

        CastleSecurityChange request = new CastleSecurityChange(

                stewardObjectId, mode == 1, in.d());

        if (request.stewardObjectId() == 0 || mode > 1

                || request.nativeSelector() != 1 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_CHANGE_CASTLE_SECURITY layout");

        }

        return request;

    }



    /**

     * Exact LiTo producer {@code sub_43D3A0}, controller mode 4:

     * {@code cdhhhh}.  The preserved 3.80 {@code C_SoldierBuy} reader proves

     * that the first word is the selected-row count.  LiTo's controller emits

     * exactly one row, followed by entry id, quantity and submitted total.

     */

    static MercenaryEmployRequest mercenaryEmployRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_MERCENARYEMPLOY) {

            throw new IllegalArgumentException("not a native C_MERCENARYEMPLOY packet");

        }

        MercenaryEmployRequest request = new MercenaryEmployRequest(

                in.d(), in.h(), in.h(), in.h(), in.h());

        if (request.stewardObjectId() == 0 || request.selectedRowCount() != 1

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_MERCENARYEMPLOY layout");

        }

        return request;

    }



    /** Exact LiTo {@code mercenarySelect0..5} producer: {@code cdh}. */

    static MercenarySelectRequest mercenarySelectRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_MERCENARYSELECT) {

            throw new IllegalArgumentException("not a native C_MERCENARYSELECT packet");

        }

        MercenarySelectRequest request = new MercenarySelectRequest(in.d(), in.h());

        if (request.stewardObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_MERCENARYSELECT layout");

        }

        return request;

    }



    /**

     * Exact LiTo producer {@code sub_43D3A0}, controller mode 5:

     * {@code cdhhdh}.  The first word is the native constant 1; selector and

     * callback token are echoed from S43 and quantity is bounded by its UI.

     */

    static MercenaryArrangeRequest mercenaryArrangeRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_MERCENARYARRANGE) {

            throw new IllegalArgumentException("not a native C_MERCENARYARRANGE packet");

        }

        MercenaryArrangeRequest request = new MercenaryArrangeRequest(

                in.d(), in.h(), in.h(), in.d(), in.h());

        if (request.stewardObjectId() == 0 || request.command() != 1

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_MERCENARYARRANGE layout");

        }

        return request;

    }



    /**

     * Exact LiTo S40 receiver {@code sub_4AE320}:

     * {@code ddh + count*[hsh]}.  The second dword is retained by the native

     * employment controller.  The preserved server lineage identifies the

     * row's final word as the unit price.

     */

    static byte[] mercenaryEmploy(long stewardObjectId, long nativeBudget,

                                  List<MercenaryEmployRow> rows) {

        requireDwordId(stewardObjectId, "mercenary-employ steward object id");

        requireUnsignedDword(nativeBudget, "mercenary-employ native budget");

        if (rows == null || rows.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_MERCENARYEMPLOY row count");

        }

        Writer packet = new Writer().c(S_MERCENARYEMPLOY)

                .d(stewardObjectId).d(nativeBudget).h(rows.size());

        for (MercenaryEmployRow row : rows) {

            if (row == null) throw new IllegalArgumentException("null native mercenary-employ row");

            requireUnsignedWord(row.entryId(), "mercenary-employ entry id");

            requireUnsignedWord(row.nativeValue(), "mercenary-employ row value");

            requireNativeString(row.name(), "mercenary-employ row name");

            if (row.name().isEmpty() || containsControl(row.name())) {

                throw new IllegalArgumentException("invalid native mercenary-employ row name");

            }

            packet.h(row.entryId()).s(row.name()).h(row.nativeValue());

        }

        return packet.bytes();

    }



    /**

     * Exact LiTo S42 receiver {@code sub_4AE420}:

     * {@code dh + count*[hs]}.  The client caps the list at six and maps its

     * identifiers to {@code mercenarySelect0..5}.

     */

    static byte[] mercenarySelect(long stewardObjectId,

                                  List<MercenarySelectRow> rows) {

        requireDwordId(stewardObjectId, "mercenary-select steward object id");

        if (rows == null || rows.size() > 6) {

            throw new IllegalArgumentException("invalid native S_MERCENARYSELECT row count");

        }

        Set<Integer> identifiers = new java.util.HashSet<>();

        Writer packet = new Writer().c(S_MERCENARYSELECT)

                .d(stewardObjectId).h(rows.size());

        for (MercenarySelectRow row : rows) {

            if (row == null) throw new IllegalArgumentException("null native mercenary-select row");

            requireUnsignedWord(row.identifier(), "mercenary-select identifier");

            requireNativeString(row.name(), "mercenary-select row name");

            if (row.name().isEmpty() || containsControl(row.name())

                    || !identifiers.add(row.identifier())) {

                throw new IllegalArgumentException("invalid native mercenary-select row");

            }

            packet.h(row.identifier()).s(row.name());

        }

        return packet.bytes();

    }



    /**

     * Exact LiTo S43 receiver {@code sub_4AE530}:

     * {@code dhhh + sdh}.  The receiver always consumes the one trailing row

     * and opens {@code npcdeloy}.  Preserved S_SoldierGive writes selected

     * type 0..3 into the third word; LiTo clamps its local copy to 1 but does

     * not use that copy when its mode-5 controller emits C3F.

     */

    static byte[] mercenaryArrange(long stewardObjectId, int selector,

                                   int displayTotal, int nativeType,

                                   String assigneeName, long callbackToken,

                                   int maximum) {

        requireDwordId(stewardObjectId, "mercenary-arrange steward object id");

        requireUnsignedWord(selector, "mercenary-arrange selector");

        requireUnsignedWord(displayTotal, "mercenary-arrange display total");

        requireUnsignedWord(nativeType, "mercenary-arrange native type");

        requireNativeString(assigneeName, "mercenary-arrange assignee name");

        requireUnsignedDword(callbackToken, "mercenary-arrange callback token");

        requireUnsignedWord(maximum, "mercenary-arrange maximum");

        if (assigneeName.isEmpty() || containsControl(assigneeName)) {

            throw new IllegalArgumentException("invalid native mercenary-arrange assignee name");

        }

        return new Writer().c(S_MERCENARYARRANGE).d(stewardObjectId)

                .h(selector).h(displayTotal).h(nativeType)

                .s(assigneeName).d(callbackToken).h(maximum).bytes();

    }



    /** Exact LiTo S44 receiver {@code sub_4AE6A0}: {@code dhhh + count*[hsh]}. */

    static byte[] archerArrange(long stewardObjectId, int category, int limit,

                                List<ArcherArrangeRow> rows) {

        requireDwordId(stewardObjectId, "archer-arrange steward object id");

        requireUnsignedWord(category, "archer-arrange category");

        requireUnsignedWord(limit, "archer-arrange limit");

        if (rows == null || rows.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_ARCHERARRANGE row count");

        }

        Writer packet = new Writer().c(S_ARCHERARRANGE).d(stewardObjectId)

                .h(category).h(limit).h(rows.size());

        for (ArcherArrangeRow row : rows) {

            if (row == null) throw new IllegalArgumentException("null native archer-arrange row");

            requireUnsignedWord(row.entryId(), "archer-arrange entry id");

            requireNativeString(row.name(), "archer-arrange row name");

            requireUnsignedWord(row.arranged(), "archer-arrange arranged value");

            if (row.name().isEmpty() || containsControl(row.name())) {

                throw new IllegalArgumentException("invalid native archer-arrange row name");

            }

            packet.h(row.entryId()).s(row.name()).h(row.arranged());

        }

        return packet.bytes();

    }



    /** Exact LiTo producer {@code sub_407C70}: C_SELECTABLE_TIME is opcode-only. */

    static void requireSelectableTime(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_SELECTABLE_TIME || in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "invalid native C_SELECTABLE_TIME opcode-only layout");

        }

    }



    /**

     * Exact LiTo producer {@code sub_407C70}: C_SELECT_TIME is {@code cd}.

     *

     * <p>The S_SELECTABLE_TIME_LIST receiver stores its one-byte identifier

     * with {@code movsx}; identifiers 0x80..0xFF therefore return as the

     * sign-extended dwords 0xFFFFFF80..0xFFFFFFFF, not as 0x00000080..FF.</p>

     */

    static int selectedTimeIdentifier(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_SELECT_TIME) {

            throw new IllegalArgumentException("not a native C_SELECT_TIME packet");

        }

        long wire = in.d();

        int identifier = (int) (wire & 0xFF);

        long signExtended = (long) (byte) identifier & 0xFFFF_FFFFL;

        if (wire != signExtended || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_SELECT_TIME layout");

        }

        return identifier;

    }



    /**

     * Exact LiTo receiver {@code sub_4AF130}: {@code h(count), s(timezone)},

     * followed by at most six {@code c(identifier), d(encodedTime)} rows.

     * The client keeps the identifiers for the later C_SELECT_TIME producer.

     */

    static byte[] selectableTimeList(String timezone, List<SelectableTime> entries) {

        requireNativeString(timezone, "selectable-time timezone");

        if (timezone.isEmpty() || timezone.getBytes(LEGACY_TEXT).length > 7

                || entries == null || entries.size() > 6) {

            throw new IllegalArgumentException("invalid native selectable-time header");

        }

        Set<Integer> identifiers = new java.util.HashSet<>();

        Writer packet = new Writer().c(S_SELECTABLE_TIME_LIST)

                .h(entries.size()).s(timezone);

        for (SelectableTime entry : entries) {

            if (entry == null) {

                throw new IllegalArgumentException("null native selectable-time row");

            }

            requireByte(entry.identifier(), "selectable-time identifier");

            requireUnsignedDword(entry.encodedTime(), "selectable-time encoded time");

            if (!identifiers.add(entry.identifier())) {

                throw new IllegalArgumentException("duplicate native selectable-time identifier");

            }

            packet.c(entry.identifier()).d(entry.encodedTime());

        }

        return packet.bytes();

    }



    /**

     * Exact LiTo 3.81C C_HYPERTEXT_INPUT_RESULT producer

     * {@code sub_43D3A0}: {@code cddcs}.  The numeric value is the input

     * widget's current dword after its native signed min/max checks.  The

     * final string is copied from S_HYPERTEXT_INPUT's third string; it is a

     * server-owned callback token, not user-entered free text.

     */

    static HypertextInputResult hypertextInputResult(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_HYPERTEXT_INPUT_RESULT) {

            throw new IllegalArgumentException("not a native C_HYPERTEXT_INPUT_RESULT packet");

        }

        HypertextInputResult result = new HypertextInputResult(

                in.d(), in.d(), in.c(), in.s());

        int callbackBytes = result.callback().getBytes(LEGACY_TEXT).length;

        if (result.worldObjectId() == 0 || callbackBytes > 0xFF

                || containsControl(result.callback()) || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_HYPERTEXT_INPUT_RESULT layout");

        }

        return result;

    }



    /**

     * Exact LiTo 3.81C S_HYPERTEXT_INPUT receiver {@code sub_4AF6B0}:

     * {@code dddddcsssh}, followed by {@code count} strings.  The five

     * dwords are object, auxiliary, current, minimum and maximum.  The three

     * strings are prompt, HTML id and callback in that wire order; the

     * same-family writers express the leading empty prompt as {@code H(0)}

     * (mode byte plus the first string terminator).  The client compares

     * current/minimum/maximum as signed 32-bit values and returns

     * object/current/mode/callback through C_51.

     */

    static byte[] hypertextInput(long objectId, long auxiliary, long current,

                                 long minimum, long maximum, int mode,

                                 String htmlId, String prompt, String callback,

                                 List<String> arguments) {

        requireDwordId(objectId, "hypertext-input object id");

        requireUnsignedDword(auxiliary, "hypertext-input auxiliary");

        requireSignedNonNegativeDword(current, "hypertext-input current");

        requireSignedNonNegativeDword(minimum, "hypertext-input minimum");

        requireSignedNonNegativeDword(maximum, "hypertext-input maximum");

        requireByte(mode, "hypertext-input mode");

        requireNativeString(htmlId, "hypertext-input html id");

        requireNativeString(prompt, "hypertext-input prompt");

        requireNativeString(callback, "hypertext-input callback");

        if (htmlId.isEmpty() || htmlId.getBytes(LEGACY_TEXT).length > 0x1F

                || current < minimum || current > maximum || arguments == null

                || arguments.size() > 0x7FFF) {

            throw new IllegalArgumentException("invalid native S_HYPERTEXT_INPUT values");

        }

        Writer packet = new Writer().c(S_HYPERTEXT_INPUT)

                .d(objectId).d(auxiliary).d(current).d(minimum).d(maximum)

                .c(mode).s(prompt).s(htmlId).s(callback).h(arguments.size());

        for (String argument : arguments) {

            requireNativeString(argument, "hypertext-input argument");

            packet.s(argument);

        }

        return packet.bytes();

    }



    /**

     * Exact LiTo 3.81C house-auction list receiver {@code sub_4B0090}:

     * {@code d(board) h(count)}, followed by

     * {@code d(house) s(name) h(area) c(month) c(day) d(price)} rows.

     */

    static byte[] agitList(long boardObjectId, List<HouseAuctionRow> rows) {

        requireDwordId(boardObjectId, "house-auction board object id");

        if (rows == null || rows.size() > 0xFF) {

            throw new IllegalArgumentException("invalid native house-auction row count");

        }

        Writer packet = new Writer().c(S_AGIT_LIST).d(boardObjectId).h(rows.size());

        for (HouseAuctionRow row : rows) {

            if (row == null || !supportedAgitHouseId(row.houseId())

                    || row.area() < 0 || row.area() > 0xFFFF

                    || row.deadlineMonth() < 1 || row.deadlineMonth() > 12

                    || row.deadlineDay() < 1 || row.deadlineDay() > 31

                    || row.price() <= 0 || row.price() > 2_000_000_000L) {

                throw new IllegalArgumentException("invalid native house-auction row");

            }

            requireNativeString(row.name(), "house-auction name");

            if (row.name().isEmpty() || row.name().getBytes(LEGACY_TEXT).length > 0x7F) {

                throw new IllegalArgumentException("invalid native house-auction name");

            }

            packet.d(row.houseId()).s(row.name()).h(row.area())

                    .c(row.deadlineMonth()).c(row.deadlineDay()).d(row.price());

        }

        return packet.bytes();

    }



    /**

     * Exact LiTo 3.81C house-map receiver {@code sub_4B0140}: {@code dd}.

     * The client accepts only house domains whose high word is 4..7.

     */

    static byte[] agitMap(long boardObjectId, long houseId) {

        requireDwordId(boardObjectId, "house-auction board object id");

        if (!supportedAgitHouseId(houseId)) {

            throw new IllegalArgumentException("house id is outside the native agit-map domain");

        }

        return new Writer().c(S_AGIT_MAP).d(boardObjectId).d(houseId).bytes();

    }



    private static boolean supportedAgitHouseId(long houseId) {

        if (houseId <= 0 || houseId > 0xFFFF_FFFFL) return false;

        long domain = (houseId >>> 16) & 0xFFFFL;

        return domain >= 4 && domain < 8;

    }



    /**

     * Exact pet-rename prompt in {@code sub_4A6890}: S_ASK parses the leading

     * question word and question 325 has no additional packet fields.

     */

    static byte[] petRenameAsk() {

        return new Writer().c(S_ASK).h(325).bytes();

    }



    /** Native {@code sub_40A1D0} producer: {@code c s(clanName)}. */

    static String createPledgeRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_CREATE_PLEDGE) {

            throw new IllegalArgumentException("not a native C_CREATE_PLEDGE packet");

        }

        String clanName = in.s();

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_CREATE_PLEDGE layout");

        }

        return clanName;

    }



    /** Both recovered C_JOIN_PLEDGE producers are opcode-only. */

    static void requireJoinPledge(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_JOIN_PLEDGE || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_JOIN_PLEDGE opcode-only layout");

        }

    }



    /** Question 97 uses the generic {@code h + s(requesterName)} S_ASK branch. */

    static byte[] clanJoinAsk(String requesterName) {

        requireNativeString(requesterName, "clan applicant name");

        int length = requesterName.getBytes(LEGACY_TEXT).length;

        if (length == 0 || length > 0x3F) {

            throw new IllegalArgumentException("invalid native clan applicant name");

        }

        return new Writer().c(S_ASK).h(97).s(requesterName).bytes();

    }



    /**

     * Every recovered native C_WAR producer writes {@code ccs}: opcode,

     * request type and target clan.  A single ASCII space is the retained

     * unowned-castle sentinel resolved through the player's NPC temp id.

     * The client emits type 0 (declare),

     * 2 (surrender), 3 (cease) and 5 (alliance); type 5 is parsed here but

     * remains a separately gated server feature until its answer transition

     * is recovered.

     */

    static ClanWarRequest clanWarRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_WAR) {

            throw new IllegalArgumentException("not a native C_WAR packet");

        }

        ClanWarRequestType type = ClanWarRequestType.fromWire(in.c());

        String targetClan = in.s();

        ClanWarRequest result = new ClanWarRequest(type, targetClan);

        int encodedLength = result.targetClan().getBytes(LEGACY_TEXT).length;

        boolean unownedCastle = " ".equals(result.targetClan());

        if ((!unownedCastle && (encodedLength < 2 || encodedLength > 0x10

                    || !result.targetClan().equals(result.targetClan().trim())))

                || containsControl(result.targetClan()) || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_WAR layout");

        }

        return result;

    }



    /**

     * Native S_ASK question branches used by clan war proposals.  Question

     * 217 is {@code hss}; questions 221 and 222 are the generic {@code hs}

     * branch.  The opcode byte is included by the writer.

     */

    static byte[] clanWarAsk(ClanWarRequestType requestType, String requesterClan,

                             String requesterName) {

        Objects.requireNonNull(requestType, "clan-war request type");

        requireClanWarName(requesterClan, "clan-war requester clan");

        return switch (requestType) {

            case DECLARE -> {

                requireNativeString(requesterName, "clan-war requester name");

                if (requesterName.isBlank() || containsControl(requesterName)) {

                    throw new IllegalArgumentException("invalid clan-war requester name");

                }

                yield new Writer().c(S_ASK).h(217)

                        .s(requesterClan).s(requesterName).bytes();

            }

            case SURRENDER -> new Writer().c(S_ASK).h(221).s(requesterClan).bytes();

            case CEASE -> new Writer().c(S_ASK).h(222).s(requesterClan).bytes();

            default -> throw new IllegalArgumentException(

                    "unsupported native clan-war question type");

        };

    }



    /** Exact native S_WAR receiver layout: {@code ccss}. */

    static byte[] clanWar(ClanWarNoticeType type, String clanOne, String clanTwo) {

        Objects.requireNonNull(type, "clan-war notice type");

        requireClanWarName(clanOne, "clan-war first clan");

        requireClanWarName(clanTwo, "clan-war second clan");

        return new Writer().c(S_WAR).c(type.wireValue()).s(clanOne).s(clanTwo).bytes();

    }



    private static void requireClanWarName(String value, String label) {

        requireNativeString(value, label);

        int encodedLength = value.getBytes(LEGACY_TEXT).length;

        if (encodedLength == 0 || encodedLength > 0x10 || containsControl(value)) {

            throw new IllegalArgumentException("invalid " + label);

        }

    }



    /**

     * Exact slash-command producers in {@code sub_407C70} emit opcode-only

     * C_INVITE_PARTY, C_LEAVE_PARTY and C_WHO_PARTY packets.

     */

    static void requireClassicPartyOpcodeOnly(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_INVITE_PARTY && expectedOpcode != C_LEAVE_PARTY

                && expectedOpcode != C_WHO_PARTY) {

            throw new IllegalArgumentException("unsupported classic party opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid classic party opcode-only layout");

        }

    }



    /** Native {@code sub_407C70} /checkpk producer is opcode-only. */

    static void requireCheckPk(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_CHECK_PK || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_CHECK_PK opcode-only layout");

        }

    }



    /**

     * Exact hidden-command producer in {@code sub_407C70}: opcode-only

     * C_EXIT_GHOST. The producer is gated by the native builder role byte;

     * this method fixes only the wire envelope, not a classic-world ghost

     * lifecycle that Tournament sessions do not possess.

     */

    static void requireExitGhost(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_EXIT_GHOST || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_EXIT_GHOST opcode-only layout");

        }

    }



    /** Native /banish callbacks {@code sub_40A3E0/sub_40A450}: {@code cs}. */

    static String partyBanishRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BANISH_PARTY) {

            throw new IllegalArgumentException("not a native C_BANISH_PARTY packet");

        }

        String name = in.s().trim();

        int encodedLength = name.getBytes(LEGACY_TEXT).length;

        if (encodedLength == 0 || encodedLength > 0x1B || containsControl(name)

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_BANISH_PARTY layout");

        }

        return name;

    }



    /** Native C_EMBLEM producer sub_424130: {@code c d(emblemId)}. */

    static long emblemRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_EMBLEM) {

            throw new IllegalArgumentException("unexpected emblem opcode");

        }

        long emblemId = in.d();

        if (emblemId == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid C_EMBLEM payload");

        }

        return emblemId;

    }



    /** Native S_EMBLEM receiver sub_4ACFD0: {@code c d + 384 raw bytes}. */

    static byte[] emblem(long emblemId, byte[] pixels) {

        if (emblemId <= 0 || emblemId > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("emblem id is outside native dword range");

        }

        if (pixels == null || pixels.length != EMBLEM_BYTES) {

            throw new IllegalArgumentException("native emblem must contain exactly "

                    + EMBLEM_BYTES + " bytes");

        }

        return new Writer().c(S_EMBLEM).d(emblemId).raw(pixels).bytes();

    }



    record BoardWriteRequest(long boardObjectId, String title, String content) {}

    record BoardPageRequest(long boardObjectId, long cursor) {}

    record BoardReadRequest(long boardObjectId, long topicId) {}

    record BoardDeleteRequest(long boardObjectId, long topicId) {}

    record RepairRequest(long inventoryObjectId, long npcObjectId) {}

    record RepairableItem(long inventoryObjectId, int damage) {}

    record SpellShopRequest(long npcObjectId) {}

    record SpellPurchaseRequest(List<Integer> wireSkillIds) {}

    record SpellMaterialShopRequest(long npcObjectId) {}

    record SpellMaterialPurchaseRequest(List<Integer> wireSkillIds) {}



    /** Native board editor callback sub_411C50: {@code c d s(title) s(content)}. */

    static BoardWriteRequest boardWriteRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BOARD_WRITE) {

            throw new IllegalArgumentException("not a native C_BOARD_WRITE packet");

        }

        BoardWriteRequest request = new BoardWriteRequest(in.d(), in.s(), in.s());

        int titleBytes = request.title().getBytes(LEGACY_TEXT).length;

        int contentBytes = request.content().getBytes(LEGACY_TEXT).length;

        if (request.boardObjectId() == 0 || titleBytes == 0 || titleBytes > 40

                || contentBytes > 1024 || containsControl(request.title())

                || containsUnsafeBoardControl(request.content()) || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_BOARD_WRITE layout");

        }

        return request;

    }



    /** Both recovered list producers use {@code cdd}: object id plus page cursor. */

    static BoardPageRequest boardPageRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BOARD_LIST) {

            throw new IllegalArgumentException("not a native C_BOARD_LIST packet");

        }

        BoardPageRequest request = new BoardPageRequest(in.d(), in.d());

        if (request.boardObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_BOARD_LIST layout");

        }

        return request;

    }



    /** Native topic-open callbacks use {@code cdd}: object id plus topic id. */

    static BoardReadRequest boardReadRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BOARD_READ) {

            throw new IllegalArgumentException("not a native C_BOARD_READ packet");

        }

        BoardReadRequest request = new BoardReadRequest(in.d(), in.d());

        if (request.boardObjectId() == 0 || request.topicId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_BOARD_READ layout");

        }

        return request;

    }



    /** Recovered callback at 0x00412740 emits exact {@code cdd}. */

    static BoardDeleteRequest boardDeleteRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BOARD_DELETE) {

            throw new IllegalArgumentException("not a native C_BOARD_DELETE packet");

        }

        BoardDeleteRequest request = new BoardDeleteRequest(in.d(), in.d());

        if (request.boardObjectId() == 0 || request.topicId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_BOARD_DELETE layout");

        }

        return request;

    }



    /**

     * Exact S_BOARD_LIST receiver sub_4128E0: {@code ddhsh}, then at most

     * eight {@code dsss} topic summaries.  The cursor is echoed so the client

     * can append a page only to the list state that requested it; 0x7fffffff

     * is the native reset/newest-page sentinel.

     */

    static byte[] boardList(long boardObjectId, long cursor, String boardName,

                            int writeFee, List<GameStateStore.BoardTopic> topics) {

        requireDwordId(boardObjectId, "board object id");

        requireUnsignedDword(cursor, "board cursor");

        requireNativeString(boardName, "board name");

        if (boardName.isEmpty() || boardName.getBytes(LEGACY_TEXT).length > 20

                || containsControl(boardName) || writeFee < 0 || writeFee > 0xFFFF

                || topics == null || topics.size() > BOARD_PAGE_SIZE) {

            throw new IllegalArgumentException("invalid native S_BOARD_LIST header");

        }

        Writer packet = new Writer().c(S_BOARD_LIST).d(boardObjectId).d(cursor)

                .h(topics.size()).s(boardName).h(writeFee);

        for (GameStateStore.BoardTopic topic : topics) {

            requireBoardTopic(topic, false);

            packet.d(topic.id()).s(topic.author()).s(topic.date()).s(topic.title());

        }

        return packet.bytes();

    }



    /**

     * Exact S_BOARD_READ receiver sub_4115A0: {@code dssss}.  Its native

     * destination buffers prove the order author, title, date, content; this

     * intentionally corrects older same-family server copies that swapped

     * title and date for this LiTo client revision.

     */

    static byte[] boardRead(GameStateStore.BoardTopic topic) {

        requireBoardTopic(topic, true);

        return new Writer().c(S_BOARD_READ).d(topic.id()).s(topic.author())

                .s(topic.title()).s(topic.date()).s(topic.content()).bytes();

    }



    /**

     * Exact native producer {@code sub_407C70} emits opcode-only

     * {@code C_FIXABLE_ITEM 0x45} after the repair hypertext action has saved

     * the selected NPC object id in client state.

     */

    static void requireFixableItemRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_FIXABLE_ITEM || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_FIXABLE_ITEM opcode-only layout");

        }

    }



    /** Native repair-list callback {@code sub_4D9B80}: {@code cdd(item,npc)}. */

    static RepairRequest repairRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_FIX) {

            throw new IllegalArgumentException("not a native C_FIX packet");

        }

        RepairRequest request = new RepairRequest(in.d(), in.d());

        if (request.inventoryObjectId() == 0 || request.npcObjectId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_FIX layout");

        }

        return request;

    }



    /**

     * Exact receiver {@code sub_4AEB80}: {@code dh + dc[]}.  The receiver

     * sign-extends each damage byte before multiplying it by the dword unit

     * price, so values above 127 are rejected instead of becoming negative.

     */

    static byte[] fixableItemList(long unitPrice, List<RepairableItem> items) {

        requireUnsignedDword(unitPrice, "repair unit price");

        if (unitPrice == 0 || items == null || items.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_FIXABLE_ITEM_LIST header");

        }

        Writer packet = new Writer().c(S_FIXABLE_ITEM_LIST).d(unitPrice).h(items.size());

        for (RepairableItem item : items) {

            if (item == null || item.inventoryObjectId() <= 0

                    || item.inventoryObjectId() > 0xFFFF_FFFFL

                    || item.damage() <= 0 || item.damage() > 0x7F) {

                throw new IllegalArgumentException("invalid native repair-list item");

            }

            packet.d(item.inventoryObjectId()).c(item.damage());

        }

        return packet.bytes();

    }



    /** Exact native buyspell producer {@code sub_407C70}: {@code cd(npc)}. */

    static SpellShopRequest spellShopRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BUYABLE_SPELL) {

            throw new IllegalArgumentException("not a native C_BUYABLE_SPELL packet");

        }

        SpellShopRequest request = new SpellShopRequest(in.d());

        if (request.npcObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_BUYABLE_SPELL layout");

        }

        return request;

    }



    /**

     * Mode-7 list callback {@code sub_4D9BD0}: {@code ch + d[]}.

     * The local construction buffer includes the two-byte frame length, so

     * selected rows begin at buffer+5 while the encrypted plaintext begins

     * with opcode+word count.

     */

    static SpellPurchaseRequest spellPurchaseRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BUY_SPELL) {

            throw new IllegalArgumentException("not a native C_BUY_SPELL packet");

        }

        int count = in.h();

        if (count <= 0 || count > SpellShopCatalog.MAX_NATIVE_SKILL_ID

                || in.remaining() != count * 4) {

            throw new IllegalArgumentException("invalid native C_BUY_SPELL layout");

        }

        List<Integer> wireSkillIds = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {

            long value = in.d();

            if (value >= SpellShopCatalog.MAX_NATIVE_SKILL_ID) {

                throw new IllegalArgumentException("invalid native spell-shop row");

            }

            wireSkillIds.add((int) value);

        }

        return new SpellPurchaseRequest(List.copyOf(wireSkillIds));

    }



    /** Exact receiver {@code sub_4AEC60}: {@code dh + d[]} or empty {@code dhd}. */

    static byte[] buyableSpellList(long priceMultiplier, List<Integer> wireSkillIds,

                                   long npcObjectId) {

        requireUnsignedDword(priceMultiplier, "spell price multiplier");

        requireDwordId(npcObjectId, "spell-shop NPC object id");

        if (priceMultiplier == 0 || wireSkillIds == null || wireSkillIds.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_BUYABLE_SPELL_LIST header");

        }

        Writer packet = new Writer().c(S_BUYABLE_SPELL_LIST)

                .d(priceMultiplier).h(wireSkillIds.size());

        if (wireSkillIds.isEmpty()) return packet.d(npcObjectId).bytes();

        for (Integer wireSkillId : wireSkillIds) {

            if (wireSkillId == null || wireSkillId < 0

                    || wireSkillId >= SpellShopCatalog.MAX_NATIVE_SKILL_ID) {

                throw new IllegalArgumentException("invalid native buyable spell row");

            }

            packet.d(wireSkillId);

        }

        return packet.bytes();

    }



    /** Exact native xchgspell producer {@code sub_407C70}: {@code cd(npc)}. */

    static SpellMaterialShopRequest spellMaterialShopRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_EXCHANGEABLE_SPELL) {

            throw new IllegalArgumentException("not a native C_EXCHANGEABLE_SPELL packet");

        }

        SpellMaterialShopRequest request = new SpellMaterialShopRequest(in.d());

        if (request.npcObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_EXCHANGEABLE_SPELL layout");

        }

        return request;

    }



    /**

     * Mode-7 material-learning callback {@code sub_4D9BD0}: {@code ch + d[]}.

     * The row ids use the same zero-based spell numbering as C_BUY_SPELL.

     */

    static SpellMaterialPurchaseRequest spellMaterialPurchaseRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_EXCHANGE_SPELL) {

            throw new IllegalArgumentException("not a native C_EXCHANGE_SPELL packet");

        }

        int count = in.h();

        if (count <= 0 || count > SpellShopCatalog.MAX_NATIVE_SKILL_ID

                || in.remaining() != count * 4) {

            throw new IllegalArgumentException("invalid native C_EXCHANGE_SPELL layout");

        }

        List<Integer> wireSkillIds = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {

            long value = in.d();

            if (value >= SpellShopCatalog.MAX_NATIVE_SKILL_ID) {

                throw new IllegalArgumentException("invalid native material-spell row");

            }

            wireSkillIds.add((int) value);

        }

        return new SpellMaterialPurchaseRequest(List.copyOf(wireSkillIds));

    }



    /** Exact receiver {@code sub_4AF7E0}: {@code ch + d[]} (empty is only {@code ch}). */

    static byte[] exchangeableSpellList(List<Integer> wireSkillIds) {

        if (wireSkillIds == null || wireSkillIds.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_EXCHANGEABLE_SPELL_LIST header");

        }

        Writer packet = new Writer().c(S_EXCHANGEABLE_SPELL_LIST).h(wireSkillIds.size());

        for (Integer wireSkillId : wireSkillIds) {

            if (wireSkillId == null || wireSkillId < 0

                    || wireSkillId >= SpellShopCatalog.MAX_NATIVE_SKILL_ID) {

                throw new IllegalArgumentException("invalid native exchangeable spell row");

            }

            packet.d(wireSkillId);

        }

        return packet.bytes();

    }



    /** Exact receiver {@code sub_4AF8E0}: {@code cd(zero-based skill id)}. */

    static byte[] notEnoughForSpell(int wireSkillId) {

        if (wireSkillId < 0 || wireSkillId >= SpellShopCatalog.MAX_NATIVE_SKILL_ID) {

            throw new IllegalArgumentException("invalid native material shortage spell id");

        }

        return new Writer().c(S_NOT_ENOUGH_FOR_SPELL).d(wireSkillId).bytes();

    }



    private static void requireBoardTopic(GameStateStore.BoardTopic topic, boolean includeContent) {

        if (topic == null || topic.id() <= 0 || topic.id() >= BOARD_INITIAL_CURSOR) {

            throw new IllegalArgumentException("invalid native board topic id");

        }

        int authorBytes = topic.author().getBytes(LEGACY_TEXT).length;

        int dateBytes = topic.date().getBytes(LEGACY_TEXT).length;

        int titleBytes = topic.title().getBytes(LEGACY_TEXT).length;

        int contentBytes = topic.content().getBytes(LEGACY_TEXT).length;

        if (authorBytes == 0 || authorBytes > 28 || dateBytes == 0 || dateBytes > 8

                || titleBytes == 0 || titleBytes > 40 || containsControl(topic.author())

                || containsControl(topic.date()) || containsControl(topic.title())

                || (includeContent && (contentBytes > 1024

                || containsUnsafeBoardControl(topic.content())))) {

            throw new IllegalArgumentException("board topic exceeds exact native receiver storage");

        }

    }



    /** Question 953 is the standard gameplay-party invitation prompt. */

    static byte[] partyInviteAsk(String inviterName) {

        requireNativeString(inviterName, "party inviter name");

        int length = inviterName.getBytes(LEGACY_TEXT).length;

        if (length == 0 || length > 0x3F || containsControl(inviterName)) {

            throw new IllegalArgumentException("invalid native party inviter name");

        }

        return new Writer().c(S_ASK).h(953).s(inviterName).bytes();

    }



    /**

     * The native /party UI is the resource-driven {@code party} hypertext.

     * Its two substitutions are the leader name and the space-delimited

     * member list used by the same-family 3.81 S_Party implementation.

     */

    static byte[] partyList(long objectId, String leader, List<String> members) {

        requireNativeString(leader, "party leader");

        if (leader.isBlank() || members == null || members.size() < 2 || members.size() > 8

                || members.stream().anyMatch(name -> name == null || name.isBlank()

                        || containsControl(name)

                        || name.getBytes(LEGACY_TEXT).length > 0x1B)

                || members.stream().noneMatch(name -> name.equalsIgnoreCase(leader))) {

            throw new IllegalArgumentException("invalid native party list");

        }

        return hypertext(objectId, "party", "",

                List.of(leader, String.join(" ", members) + " "));

    }



    /** Exact C_ANSWER producers use {@code chcs}: question, answer and text. */

    static AskAnswer askAnswer(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_ANSWER) {

            throw new IllegalArgumentException("not a native C_ANSWER packet");

        }

        AskAnswer result = new AskAnswer(in.h(), in.c(), in.s());

        if (result.answer() > 1 || in.remaining() != 0

                || result.text().getBytes(LEGACY_TEXT).length > 0xFF

                || containsControl(result.text())) {

            throw new IllegalArgumentException("invalid native C_ANSWER layout");

        }

        return result;

    }



    /** Exact selected-client question 748 receiver consumes one empty string. */

    static byte[] massTeleportAsk() {

        return new Writer().c(S_ASK).h(748).s("").bytes();

    }



    /** Exact generic S_ASK branch for Call Clan question 729: {@code chs}. */

    static byte[] callClanAsk() {

        return new Writer().c(S_ASK).h(729).s("").bytes();

    }



    /**

     * The only native C_MARRIAGE producer in {@code sub_407C70} writes

     * {@code cc}: {@code propose} selects mode 0 and {@code divorce} mode 1.

     */

    static int marriageRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_MARRIAGE) {

            throw new IllegalArgumentException("not a native C_MARRIAGE packet");

        }

        int mode = in.c();

        if (mode > 1 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_MARRIAGE layout");

        }

        return mode;

    }



    /**

     * The two native C_WAREHOUSE_CONTROL producers in {@code sub_4EAD50}

     * both write {@code ccdd}.  Mode 0 carries old/new decimal passwords;

     * modes 1 and 2 carry password/NPC object id for ordinary/elven and

     * pledge retrieval respectively.  Unlike related 3.81 server forks,

     * this exact LiTo image does not serialize six encoded digit dwords.

     */

    static WarehouseControlRequest warehouseControlRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_WAREHOUSE_CONTROL) {

            throw new IllegalArgumentException("not a native C_WAREHOUSE_CONTROL packet");

        }

        WarehouseControlRequest request = new WarehouseControlRequest(

                in.c(), in.d(), in.d());

        if (in.remaining() != 0 || request.mode() > 2

                || request.first() > 999_999 || request.second() > 999_999

                && request.mode() == 0

                || request.mode() != 0 && request.second() == 0) {

            throw new IllegalArgumentException("invalid native C_WAREHOUSE_CONTROL layout");

        }

        return request;

    }



    /**

     * Both exact C_NPC_ITEM_CONTROL producers write {@code ccdc}: selector 0,

     * owned pet object id and the zero-based pet-inventory row.  The first is

     * the general item-use dispatcher ({@code sub_445C10}); the second is the

     * pet-list double-click callback ({@code sub_4B6B40}).

     */

    static NpcItemControlRequest npcItemControlRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_NPC_ITEM_CONTROL) {

            throw new IllegalArgumentException("not a native C_NPC_ITEM_CONTROL packet");

        }

        NpcItemControlRequest request = new NpcItemControlRequest(

                in.c(), in.d(), in.c());

        if (request.selector() != 0 || request.npcObjectId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_NPC_ITEM_CONTROL layout");

        }

        return request;

    }



    /** Generic S_ASK branch: question 654 consumes one proposer-name string. */

    static byte[] marriageProposalAsk(String proposerName) {

        requireNativeString(proposerName, "marriage proposer name");

        int length = proposerName.getBytes(LEGACY_TEXT).length;

        if (length == 0 || length > 0x3F) {

            throw new IllegalArgumentException("invalid native marriage proposer name");

        }

        return new Writer().c(S_ASK).h(654).s(proposerName).bytes();

    }



    /** Generic S_ASK branch still consumes an empty string for question 653. */

    static byte[] marriageDivorceAsk() {

        return new Writer().c(S_ASK).h(653).s("").bytes();

    }



    /** Exact S_EVENT case 9 has no body after its subtype. */

    static byte[] marriedEvent() {

        return new Writer().c(S_EVENT)

                .c(ServerEventType.MARRIED.wireValue()).bytes();

    }



    /**

     * Exact {@code sub_4B07C0} cases 0..2 castle-war envelope.  The recovered

     * receiver consumes one dword after the subtype; same-version

     * {@code S_PacketBoxWar} writes that value as {@code c(castleId)+h(0)}.

     */

    static byte[] castleWarEvent(ServerEventType type, int castleId) {

        Objects.requireNonNull(type, "castle-war event type");

        if ((type != ServerEventType.CASTLE_WAR_BEGIN

                && type != ServerEventType.CASTLE_WAR_END

                && type != ServerEventType.CASTLE_WAR_GOING)

                || castleId < 1 || castleId > 8) {

            throw new IllegalArgumentException("invalid native castle-war event");

        }

        return new Writer().c(S_EVENT).c(type.wireValue()).d(castleId).bytes();

    }



    /**

     * Exact selected LiTo {@code sub_4B07C0} cases 3/4.  Both branches read

     * no body: case 3 renders message 642 (initiative), case 4 renders 643

     * (occupied).

     */

    static byte[] castleWarOwnershipEvent(ServerEventType type) {

        Objects.requireNonNull(type, "castle-war ownership event type");

        if (type != ServerEventType.CASTLE_WAR_INITIATIVE

                && type != ServerEventType.CASTLE_WAR_OCCUPIED) {

            throw new IllegalArgumentException(

                    "invalid native castle-war ownership event");

        }

        return new Writer().c(S_EVENT).c(type.wireValue()).bytes();

    }



    /**

     * Native Taiwan name validator {@code sub_401680} with maximum 0x1B:

     * 2..27 encoded bytes, ASCII letters/digits/space, or Big5 byte pairs in

     * the inclusive recovered-image range B0A1..C8FE.

     */

    static boolean validPetName(String value) {

        return validTaiwanName(value, 0x1B);

    }



    /** The C_CREATE_PLEDGE callback calls the same validator with maximum 0x10. */

    static boolean validClanName(String value) {

        return validTaiwanName(value, 0x10);

    }



    private static boolean validTaiwanName(String value, int maximumBytes) {

        if (value == null || containsControl(value)

                || !LEGACY_TEXT.newEncoder().canEncode(value)) return false;

        byte[] bytes = value.getBytes(LEGACY_TEXT);

        if (bytes.length < 2 || bytes.length > maximumBytes) return false;

        for (int index = 0; index < bytes.length;) {

            int first = bytes[index] & 0xFF;

            if ((first >= 'a' && first <= 'z')

                    || (first >= 'A' && first <= 'Z')

                    || (first >= '0' && first <= '9') || first == ' ') {

                index++;

                continue;

            }

            if (index + 1 >= bytes.length) return false;

            int pair = (first << 8) | (bytes[index + 1] & 0xFF);

            if (pair < 0xB0A1 || pair > 0xC8FE) return false;

            index += 2;

        }

        return true;

    }



    /** Exact S_CHANGE_DESC receiver {@code sub_4AF410}: {@code d(object),s(name)}. */

    static byte[] changeDescription(long objectId, String name) {

        requireDwordId(objectId, "change-description object id");

        requireNativeString(name, "change-description name");

        if (name.isEmpty() || containsControl(name)) {

            throw new IllegalArgumentException("invalid native change-description name");

        }

        return new Writer().c(S_CHANGE_DESC).d(objectId).s(name).bytes();

    }



    /**

     * Exact gameplay-active S_CHANGE_ALIGNMENT receiver {@code sub_4AF470}:

     * {@code d(objectId)+h(lawful)}. The receiver resolves the world object and

     * calls {@code sub_4E7790}, which writes the signed lawful word to actor

     * offset {@code +0x16}, mirrors it to the local-player lawful global when

     * applicable, and stores the {@code sub_4DFB50} name-colour result at actor

     * offset {@code +0x28}. This is a mutation packet; initial actor state

     * belongs in the lawful word already carried by S_PUT_OBJECT. Tournament

     * results do not mutate lawful, so no battle/result emitter is valid.

     */

    static byte[] changeAlignment(long objectId, int lawful) {

        requireDwordId(objectId, "alignment world object id");

        requireSignedWord(lawful, "alignment lawful");

        return new Writer().c(S_CHANGE_ALIGNMENT).d(objectId).h(lawful).bytes();

    }



    /**

     * Exact S_CRIMINAL receiver {@code sub_4AFE50}: {@code d(objectId)+c(time)}.

     * Same-family {@code S_PinkName} uses 180 to start and 0 to clear the

     * temporary criminal/pink-name state. Tournament room combat is a formal

     * consensual fight, matching the classic fight-id exemption, so this

     * mutation has no automatic battle emission.

     */

    static byte[] criminalStatus(long objectId, int time) {

        requireDwordId(objectId, "criminal world object id");

        requireByte(time, "criminal time");

        return new Writer().c(S_CRIMINAL).d(objectId).c(time).bytes();

    }



    /**

     * Exact gameplay-active S_CLONE receiver {@code sub_4B0490}:

     * {@code ddhhcs(objectId,clanId,gfx,lawful,weaponAction,title)}.

     *

     * <p>The receiver updates an existing object's clan id, title, gfx and

     * lawful/action state, then reuses the ordinary S_POLYMORPH update

     * helpers. The formerly cited same-family

     * {@code S_ChangeShape(L1DeInstance)} {@code ddhhcs} overload is only a

     * wire analogue: that source writes {@code S_POLYMORPH=62}, while it

     * declares {@code S_CLONE=92} separately and never writes it. Public

     * fake-player/robot sources likewise use ordinary S_ChangeShape. This

     * receiver-shaped compatibility writer therefore has no production

     * caller until an original S_CLONE producer is proved.</p>

     */

    static byte[] cloneAppearance(long objectId, long clanId, int gfx, int lawful,

                                  int weaponAction, String title) {

        requireDwordId(objectId, "clone world object id");

        requireUnsignedDword(clanId, "clone clan id");

        // Native base sprite 0 is the valid male-prince appearance. Rejecting

        // zero made the isolated S_CLONE restore fail after a source-backed

        // type-0/sex-0 character had already been drawn with S_PUT_OBJECT.

        if (gfx < 0 || gfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid native clone gfx");

        }

        requireSignedWord(lawful, "clone lawful");

        requireByte(weaponAction, "clone weapon action");

        requireNativeString(title, "clone title");

        if (containsControl(title)) {

            throw new IllegalArgumentException("invalid native clone title");

        }

        return new Writer().c(S_CLONE).d(objectId).d(clanId).h(gfx).h(lawful)

                .c(weaponAction).s(title).bytes();

    }



    /**

     * Exact gameplay-active S_BOOK_LIST receiver {@code sub_4B2780}:

     * {@code c(legacyTag)+c(count)+repeated s(name),d(timestamp)}.

     *

     * <p>The receiver skips {@code legacyTag}, parses each name into a

     * 28-byte local buffer and calls 32-bit {@code localtime} for each

     * timestamp without retaining the result. This builder therefore exposes

     * the ignored byte instead of guessing a canonical value. It is not an

     * initial bookmark-list packet. A recovered-PE audit covers all 447

     * direct writer calls, four buffered-builder calls and the two formerly

     * ambiguous sites: {@code C_BOOK 0x7D} has no producer in this exact

     * LiTo image. The paired receiver is therefore retained as a

     * receiver-only compatibility contract and has no production caller.</p>

     */

    static byte[] bookList(int legacyTag, List<BookListEntry> entries) {

        requireByte(legacyTag, "book-list legacy tag");

        if (entries == null || entries.size() > 0xFF) {

            throw new IllegalArgumentException("invalid native book-list entry count");

        }

        Writer packet = new Writer().c(S_BOOK_LIST).c(legacyTag).c(entries.size());

        for (BookListEntry entry : entries) {

            if (entry == null) {

                throw new IllegalArgumentException("null native book-list entry");

            }

            requireNativeString(entry.name(), "book-list name");

            int nameLength = entry.name().getBytes(LEGACY_TEXT).length;

            if (nameLength > 0x1B || containsControl(entry.name())) {

                throw new IllegalArgumentException("invalid native book-list name");

            }

            requireUnsignedDword(entry.timestamp(), "book-list timestamp");

            packet.s(entry.name()).d(entry.timestamp());

        }

        return packet.bytes();

    }



    /** Exact native player-trade request producer: {@code cd(targetObjectId)}. */

    static ExchangeRequest exchangeRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_ASK_XCHG) {

            throw new IllegalArgumentException("not a native C_ASK_XCHG packet");

        }

        ExchangeRequest request = new ExchangeRequest(in.d());

        if (request.targetObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_ASK_XCHG layout");

        }

        return request;

    }



    /** Exact native trade-window item producer: {@code cdd(objectId,count)}. */

    static ExchangeItemRequest exchangeItemRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_ADD_XCHG) {

            throw new IllegalArgumentException("not a native C_ADD_XCHG packet");

        }

        ExchangeItemRequest request = new ExchangeItemRequest(in.d(), in.d());

        if (request.inventoryObjectId() == 0 || request.quantity() == 0

                || request.quantity() > Integer.MAX_VALUE || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_ADD_XCHG layout");

        }

        return request;

    }



    /** Both C_CANCEL_XCHG and C_ACCEPT_XCHG are opcode-only producers. */

    static void requireExchangeOpcodeOnly(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_CANCEL_XCHG && expectedOpcode != C_ACCEPT_XCHG) {

            throw new IllegalArgumentException("unsupported exchange opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native exchange opcode-only layout");

        }

    }



    /** Native C_WHO_PLEDGE producer {@code sub_40A490} is opcode-only. */

    static void requireWhoPledge(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_WHO_PLEDGE || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_WHO_PLEDGE opcode-only layout");

        }

    }



    /** Question 252 is parsed by S_ASK as {@code h + s(requesterName)}. */

    static byte[] exchangeAsk(String requesterName) {

        requireNativeString(requesterName, "exchange requester name");

        int length = requesterName.getBytes(LEGACY_TEXT).length;

        if (length == 0 || length > 0x3F) {

            throw new IllegalArgumentException("invalid native exchange requester name");

        }

        return new Writer().c(S_ASK).h(252).s(requesterName).bytes();

    }



    /** Exact S_XCHG_START receiver: {@code s(partnerName)} in a 28-byte buffer. */

    static byte[] exchangeStart(String partnerName) {

        requireNativeString(partnerName, "exchange partner name");

        int length = partnerName.getBytes(LEGACY_TEXT).length;

        if (length == 0 || length > 0x1B) {

            throw new IllegalArgumentException("invalid native exchange partner name");

        }

        return new Writer().c(S_XCHG_START).s(partnerName).bytes();

    }



    /**

     * Exact S_ADD_XCHG receiver contract {@code chscc}: side, inventory gfx,

     * display text, blessing and optional raw-name byte count.  Tournament

     * items use no appended raw-name bytes, so the final count is zero.

     */

    static byte[] exchangeAddItem(int side, int inventoryGfx, String displayName, int blessing) {

        requireNativeString(displayName, "exchange item display name");

        int length = displayName.getBytes(LEGACY_TEXT).length;

        if ((side != 0 && side != 1) || inventoryGfx < 0 || inventoryGfx > 0xFFFF

                || length == 0 || length > 0xFF || blessing < 0 || blessing > 3) {

            throw new IllegalArgumentException("invalid native S_ADD_XCHG values");

        }

        return new Writer().c(S_ADD_XCHG).c(side).h(inventoryGfx)

                .s(displayName).c(blessing).c(0).bytes();

    }



    /** Strict producer subset for the one-byte S_XCHG_RESULT receiver. */

    static byte[] exchangeResult(ExchangeTerminalResult result) {

        Objects.requireNonNull(result, "exchange terminal result");

        return new Writer().c(S_XCHG_RESULT).c(result.wireValue()).bytes();

    }



    /**

     * Exact LiTo 3.81C C_SLAVE_CONTROL target callback. Both native producers

     * ({@code sub_40AFE0} and {@code sub_4AF3A0}) write

     * {@code cdcd(controllerObjectId,mode,targetObjectId)}. The mode byte is

     * copied from the preceding S_SLAVE_CONTROL prompt.

     */

    static SlaveControlRequest slaveControlRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_SLAVE_CONTROL) {

            throw new IllegalArgumentException("not a native C_SLAVE_CONTROL packet");

        }

        SlaveControlRequest request = new SlaveControlRequest(in.d(), in.c(), in.d());

        if (request.controllerObjectId() == 0 || request.targetObjectId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_SLAVE_CONTROL layout");

        }

        return request;

    }



    /**

     * Exact gameplay-active S_SLAVE_CONTROL receiver {@code sub_4AF340}:

     * {@code dc(controllerObjectId,mode)}. It installs the native world-click

     * callback {@code sub_4AF3A0}; the selected target is returned through

     * C_SLAVE_CONTROL, not C_HACTION and not S_COMMAND_TARGET.

     */

    static byte[] slaveControl(long controllerObjectId, int mode) {

        requireDwordId(controllerObjectId, "slave-control object id");

        requireByte(mode, "slave-control mode");

        return new Writer().c(S_SLAVE_CONTROL).d(controllerObjectId).c(mode).bytes();

    }



    /**

     * Exact gameplay-active S_COMMAND_TARGET receiver {@code sub_4B0540}:

     * {@code dhhs}.  It creates a timed world marker for an already supplied

     * object/coordinate and text.  It is not the cross-version S_SelectTarget

     * prompt and must not be used to solicit a client target choice.

     */

    static byte[] commandTarget(long objectId, int x, int y, String text) {

        requireUnsignedDword(objectId, "command-target object id");

        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF) {

            throw new IllegalArgumentException("command-target coordinate is outside word range");

        }

        requireNativeString(text, "command-target text");

        if (text.getBytes(LEGACY_TEXT).length > 0x34 || containsControl(text)) {

            throw new IllegalArgumentException("command-target text exceeds exact receiver storage");

        }

        return new Writer().c(S_COMMAND_TARGET).d(objectId).h(x).h(y).s(text).bytes();

    }



    private static boolean containsControl(String value) {

        return value.chars().anyMatch(Character::isISOControl);

    }



    private static boolean containsUnsafeBoardControl(String value) {

        return value.chars().anyMatch(ch -> Character.isISOControl(ch)

                && ch != '\r' && ch != '\n' && ch != '\t');

    }



    private static boolean containsUnsafeLetterControl(String value) {

        return value.chars().anyMatch(ch -> Character.isISOControl(ch)

                && ch != '\r' && ch != '\n' && ch != '\t');

    }



    private static boolean wellFormedWideString(String value) {

        if (value == null) return false;

        for (int index = 0; index < value.length(); index++) {

            char current = value.charAt(index);

            if (current == 0 || Character.isLowSurrogate(current)) return false;

            if (Character.isHighSurrogate(current)) {

                if (++index >= value.length()

                        || !Character.isLowSurrogate(value.charAt(index))) {

                    return false;

                }

            }

        }

        return true;

    }



    private static boolean unsignedDecimalDword(String value) {

        if (value.isEmpty() || value.length() > 10) return false;

        long result = 0;

        for (int index = 0; index < value.length(); index++) {

            char digit = value.charAt(index);

            if (digit < '0' || digit > '9') return false;

            result = result * 10 + digit - '0';

            if (result > 0xFFFF_FFFFL) return false;

        }

        return true;

    }



    /** Native sub_4E09D0 emits C_ATTACK/C_FAR_ATTACK/C_ALT_ATTACK as {@code cdhh}. */

    static AttackRequest attackRequest(byte[] payload, int expectedOpcode) {

        return EmptyMelee.parse(payload, expectedOpcode);

    }



    /**

     * The selected LiTo 3.81C producer emits C_MOVE as {@code chhc}: the two

     * shorts are the current/source tile and the final byte is the native

     * eight-way heading.  The destination is the adjacent tile implied by

     * that heading, not the two shorts carried on the wire.

     */

    static MoveRequest moveRequest(byte[] payload) {

        final int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};

        final int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};

        Reader in = new Reader(payload);

        if (in.c() != C_MOVE) throw new IllegalArgumentException("wrong native move opcode");

        int sourceX = in.h();

        int sourceY = in.h();

        int heading = in.c();

        if (heading < 0 || heading >= dx.length || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native move layout");

        }

        return new MoveRequest(sourceX, sourceY, heading,

                sourceX + dx[heading], sourceY + dy[heading]);

    }



    /**

     * Every recovered native C_USE_SPELL producer starts with

     * {@code c(opcode), c(row), c(column)}. The client derives the classic

     * skill id as {@code (row << 3) + column + 1}; the remaining fields vary

     * with the selected skill.

     */

    static SpellSelector spellSelector(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_USE_SPELL) {

            throw new IllegalArgumentException("not a native C_USE_SPELL packet");

        }

        SpellSelector selector = new SpellSelector(in.c(), in.c());

        if (selector.column() > 7 || selector.skillId() > 220) {

            throw new IllegalArgumentException("invalid native C_USE_SPELL selector");

        }

        return selector;

    }



    /**

     * LiTo skill-index 2 (Shield) takes the {@code loc_4CBBD4} producer and

     * writes exactly {@code ccc}: opcode, row and column, with no tail.

     */

    static SpellSelector selectorOnlySpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid selector-only C_USE_SPELL layout");

        }

        return selector;

    }



    /**

     * LiTo 3.81C's dedicated skill-index 59 branch in {@code sub_4CB5F0}

     * writes {@code cccd}: opcode, row 7, column 3 and the dword returned by

     * {@code sub_4D69F0}.  That final value is a client tick, not an object id;

     * treating this envelope as an object-target spell would make native

     * Invisibility (skill 60) target a fabricated world object.

     */

    static SelfSpellRequest selfSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        SelfSpellRequest request = new SelfSpellRequest(

                selector.row(), selector.column(), in.d());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native self C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Native ground-target producer {@code sub_4C9270} writes

     * {@code ccchh}: opcode, skill row, skill column, x and y. In this client

     * image that envelope is selected for fire wall (58) and life stream

     * (63); other skills use different C_USE_SPELL tails.

     */

    static GroundSpellRequest groundSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        GroundSpellRequest request = new GroundSpellRequest(

                selector.row(), selector.column(), in.h(), in.h());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native ground C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Native object-target producer family (including {@code sub_4C9430})

     * writes {@code cccd}: opcode, skill row, skill column and target object

     * id. The matching 3.80 {@code C_UseSkill} parser confirms that basic

     * healing skills 1 and 19 consume this exact target-id tail.

     */

    static ObjectSpellRequest objectSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        ObjectSpellRequest request = new ObjectSpellRequest(

                selector.row(), selector.column(), in.d());

        if (request.targetObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native object C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * LiTo skill-index 4 (server skill 5) writes {@code ccchhh}: row 0,

     * column 4, map id, x and y.  A random teleport is represented by x/y

     * zero; the selected executable sets its native action/cursor lock before

     * transmitting this packet, so every server terminal branch must release

     * that lock with S_PARALYSE mode 7.

     */

    static TeleportSpellRequest teleportSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        TeleportSpellRequest request = new TeleportSpellRequest(

                selector.row(), selector.column(), in.h(), in.h(), in.h());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "invalid native teleport C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * LiTo skill-index 68 producer and bookmark callback write {@code ccchhh}:

     * selector followed by map id, x and y.  The last four bytes are two

     * coordinate words, not the dword bookmark id assumed by older servers.

     */

    static MassTeleportSpellRequest massTeleportSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        MassTeleportSpellRequest request = new MassTeleportSpellRequest(

                selector.row(), selector.column(), in.h(), in.h(), in.h());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "invalid native mass-teleport C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Exact selected LiTo skill-index 115 callback {@code sub_4CAFF0} writes

     * {@code cccs}: row 14, column 3 and one CP950 character name.  The

     * shared Taiwan name edit domain reserves at most 27 encoded bytes.

     */

    static CallClanSpellRequest callClanSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        String targetName = in.s();

        int encodedBytes = targetName.getBytes(LEGACY_TEXT).length;

        CallClanSpellRequest request = new CallClanSpellRequest(

                selector.row(), selector.column(), targetName);

        if (encodedBytes == 0 || encodedBytes > 0x1B

                || !LEGACY_TEXT.newEncoder().canEncode(targetName)

                || !targetName.equals(targetName.trim())

                || containsControl(targetName) || in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "invalid native Call Clan C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Same-version official case 117 writes {@code cccs}: row 14, column 5

     * and one CP950 character name.  The selected LiTo case is present but

     * its callback accidentally hard-codes column 3; the launcher compatibility

     * proxy repairs only that in-memory immediate and therefore produces this

     * original Run Clan envelope without modifying {@code LiTo.bin} on disk.

     */

    static RunClanSpellRequest runClanSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        String targetName = in.s();

        int encodedBytes = targetName.getBytes(LEGACY_TEXT).length;

        RunClanSpellRequest request = new RunClanSpellRequest(

                selector.row(), selector.column(), targetName);

        if (request.skillId() != BattleSpellCatalog.RUN_CLAN

                || encodedBytes == 0 || encodedBytes > 0x1B

                || !LEGACY_TEXT.newEncoder().canEncode(targetName)

                || !targetName.equals(targetName.trim())

                || containsControl(targetName) || in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "invalid native Run Clan C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Exact LiTo 3.81C skill-index 50 producer at {@code loc_4CBB0C} and the

     * summonlist callback at {@code loc_40A08B}: {@code cccd}, where the last

     * dword is a summon grade/choice, never a target object id.

     */

    static SummonSpellRequest summonSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        SummonSpellRequest request = new SummonSpellRequest(

                selector.row(), selector.column(), in.d());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native summon C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Native attack-spell producers {@code sub_4C9E40/sub_4CA0B0/sub_4CA6E0}

     * write {@code cccdhh}: selector, target object id and target x/y.

     */

    static AttackSpellRequest attackSpellRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        AttackSpellRequest request = new AttackSpellRequest(

                selector.row(), selector.column(), in.d(), in.h(), in.h());

        if (request.targetObjectId() == 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native attack C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Exact LiTo 3.81C True Target producer {@code sub_4CADF0} writes

     * {@code cccdhhs}: fixed selector row 14/column 0 (skill 113), target

     * object id, the client's target-coordinate snapshot and an annotation.

     * The server resolves the target id again and never trusts those two

     * coordinates when constructing S_COMMAND_TARGET.

     */

    static TrueTargetRequest trueTargetRequest(byte[] payload) {

        SpellSelector selector = spellSelector(payload);

        Reader in = new Reader(payload);

        in.c();

        in.c();

        in.c();

        TrueTargetRequest request = new TrueTargetRequest(

                selector.row(), selector.column(), in.d(), in.h(), in.h(), in.s());

        int textBytes = request.text().getBytes(LEGACY_TEXT).length;

        if (request.skillId() != 113 || request.targetObjectId() == 0

                || textBytes > 0x34 || in.remaining() != 0

                || !LEGACY_TEXT.newEncoder().canEncode(request.text())

                || containsControl(request.text())) {

            throw new IllegalArgumentException("invalid native True Target C_USE_SPELL layout");

        }

        return request;

    }



    /**

     * Exact LiTo 3.81C S_ADD_SPELL receiver contract: count followed by that

     * many eight-skill bitmap bytes. Skill 1 is bit 0 of byte 0.

     */

    static byte[] addSpells(Set<Integer> skillIds) {

        return spellBitmap(S_ADD_SPELL, skillIds);

    }



    /** Exact LiTo 3.81C S_REMOVE_SPELL bitmap contract. */

    static byte[] removeSpells(Set<Integer> skillIds) {

        return spellBitmap(S_REMOVE_SPELL, skillIds);

    }



    private static byte[] spellBitmap(int opcode, Set<Integer> skillIds) {

        if (opcode != S_ADD_SPELL && opcode != S_REMOVE_SPELL) {

            throw new IllegalArgumentException("unsupported native spell bitmap opcode");

        }

        if (skillIds == null) throw new IllegalArgumentException("native spell ids are null");

        int maximum = 0;

        for (int skillId : skillIds) {

            if (skillId < 1 || skillId > 220) {

                throw new IllegalArgumentException("native spell id outside 1..220");

            }

            maximum = Math.max(maximum, skillId);

        }

        byte[] bitmap = new byte[(maximum + 7) / 8];

        for (int skillId : skillIds) {

            int zeroBased = skillId - 1;

            bitmap[zeroBased >>> 3] |= (byte) (1 << (zeroBased & 7));

        }

        return new Writer().c(opcode).c(bitmap.length).raw(bitmap).bytes();

    }



    /**

     * Native S_ATTACK physical branch. A zero sequence terminates the fixed

     * {@code cddccd} body in {@code sub_4A5ED0}; the hit marker is 10 for an

     * accepted damaging hit and 0 for the normal miss animation.

     */

    static byte[] physicalAttack(int actionId, long attackerObjectId,

                                 long targetObjectId, boolean hit, int heading) {

        requireByte(actionId, "physical-attack action");

        requireDwordId(attackerObjectId, "physical-attack attacker");

        requireDwordId(targetObjectId, "physical-attack target");

        if (heading < 0 || heading > 7) {

            throw new IllegalArgumentException("invalid physical-attack heading");

        }

        return new Writer().c(S_ATTACK)

                .c(actionId).d(attackerObjectId).d(targetObjectId)

                .c(hit ? 10 : 0).c(heading).d(0).bytes();

    }



    /**

     * Shock Stun uses the native close-attack branch, but the retained server

     * places the actual (byte-clamped) magic damage in its hit marker instead

     * of the ordinary physical helper's fixed {@code 10} marker.

     */

    static byte[] shockStunAttack(int actionId, long attackerObjectId,

                                  long targetObjectId, int damage, int heading) {

        return physicalDamageAttack(actionId, attackerObjectId,

                targetObjectId, damage, heading);

    }



    /** Native close S_ATTACK with the actual byte-clamped damage marker. */

    static byte[] physicalDamageAttack(int actionId, long attackerObjectId,

                                       long targetObjectId, int damage, int heading) {

        requireByte(actionId, "physical damage attack action");

        requireDwordId(attackerObjectId, "physical damage attack attacker");

        requireDwordId(targetObjectId, "physical damage attack target");

        if (damage < 0 || heading < 0 || heading > 7) {

            throw new IllegalArgumentException("invalid native physical damage attack");

        }

        return new Writer().c(S_ATTACK)

                .c(actionId).d(attackerObjectId).d(targetObjectId)

                .c(Math.min(0xFF, damage)).c(heading).d(0).bytes();

    }



    /**

     * Exact LiTo 3.81C S_ATTACK_ALL receiver {@code sub_4A63F0}:

     * {@code c(action)+d(attacker)+h(count)+repeated dc(target,hit)}.

     *

     * <p>This writer is deliberately not selected by the Tournament battle

     * engine. The same-family mob-skill implementation loops ordinary

     * S_ATTACK actions for physical area damage and never references its

     * S_ATTACK_ALL registry constant, so an authoritative trigger is not yet

     * proven.</p>

     */

    static byte[] attackAll(int actionId, long attackerObjectId,

                            List<AttackAllTarget> targets) {

        requireByte(actionId, "attack-all action");

        requireDwordId(attackerObjectId, "attack-all attacker");

        if (targets == null || targets.isEmpty() || targets.size() > Short.MAX_VALUE) {

            throw new IllegalArgumentException("invalid native attack-all target count");

        }

        Writer packet = new Writer().c(S_ATTACK_ALL)

                .c(actionId).d(attackerObjectId).h(targets.size());

        for (AttackAllTarget target : targets) {

            if (target == null) {

                throw new IllegalArgumentException("null native attack-all target");

            }

            requireDwordId(target.objectId(), "attack-all target");

            packet.d(target.objectId()).c(target.hit() ? 0x20 : 0);

        }

        return packet.bytes();

    }



    /**

     * Native S_ATTACK extended branch. A nonzero sequence makes

     * {@code sub_4A5ED0} consume {@code hchhhhcs} after the fixed

     * {@code cddccd} header and instantiate the spell projectile/effect.

     */

    static byte[] attackSpell(int actionId, long casterObjectId, long targetObjectId,

                              int hitMarker, int heading, long sequence, int effectGfx,

                              int casterX, int casterY, int targetX, int targetY) {

        requireByte(actionId, "attack-spell action");

        requireDwordId(casterObjectId, "attack-spell caster");

        // Retained S_UseAttackSkill writes object zero for a PC target rejected

        // by the safety/Non-PvP gate, while preserving the clicked coordinates

        // and projectile/effect tail. A real hit marker may never accompany it.

        requireUnsignedDword(targetObjectId, "attack-spell target");

        requireByte(hitMarker, "attack-spell hit marker");

        if ((targetObjectId == 0 && hitMarker != 0)

                || heading < 0 || heading > 7

                || sequence <= 0 || sequence > 0xFFFF_FFFFL

                || effectGfx <= 0 || effectGfx > 0xFFFF

                || casterX < 0 || casterX > 0xFFFF || casterY < 0 || casterY > 0xFFFF

                || targetX < 0 || targetX > 0xFFFF || targetY < 0 || targetY > 0xFFFF) {

            throw new IllegalArgumentException("invalid native attack-spell fields");

        }

        // Source spell data uses newer sprites absent from LiTo's embedded table.
        // Keep combat values/timing, but render the corresponding native spell.
        int nativeGfx = switch (effectGfx) {
            case 11736 -> 10;   // call lightning
            case 11738 -> 1809; // cone of cold projectile, native impact 1810
            case 11746, 11748 -> 1815; // disintegrate (critical keeps server damage)
            default -> effectGfx;
        };
        return new Writer().c(S_ATTACK)

                .c(actionId).d(casterObjectId).d(targetObjectId)

                .c(hitMarker).c(heading).d(sequence)

                .h(SharedClientResources.effect(nativeGfx, effectGfx)).c(0)

                .h(casterX).h(casterY).h(targetX).h(targetY)

                .c(0).s("").bytes();

    }



    /**

     * Native physical projectile branch of the same LiTo 3.81C S_ATTACK

     * receiver.  The proved same-family arrow producer selects flight type

     * {@code 0x7f}.  The player-arrow producer uses target object zero on a

     * miss while preserving the clicked coordinates; the retained NPC-arrow

     * producer preserves its target object id and writes zero damage instead.

     * The caller therefore supplies the source-family target marker explicitly.

     */

    static byte[] rangedPhysicalAttack(

            int actionId, long attackerObjectId, long targetObjectId,

            int damageMarker, int heading, long sequence, int projectileGfx,

            int attackerX, int attackerY, int targetX, int targetY) {

        requireByte(actionId, "ranged-physical action");

        requireDwordId(attackerObjectId, "ranged-physical attacker");

        requireUnsignedDword(targetObjectId, "ranged-physical target");

        requireByte(damageMarker, "ranged-physical damage marker");

        if ((targetObjectId == 0 && damageMarker != 0)

                || heading < 0 || heading > 7

                || sequence <= 0 || sequence > 0xFFFF_FFFFL

                || projectileGfx <= 0 || projectileGfx > 0xFFFF

                || attackerX < 0 || attackerX > 0xFFFF

                || attackerY < 0 || attackerY > 0xFFFF

                || targetX < 0 || targetX > 0xFFFF

                || targetY < 0 || targetY > 0xFFFF) {

            throw new IllegalArgumentException("invalid native ranged-physical fields");

        }

        return new Writer().c(S_ATTACK)

                .c(actionId).d(attackerObjectId).d(targetObjectId)

                .c(damageMarker).c(heading).d(sequence)

                .h(projectileGfx).c(0x7f)

                .h(attackerX).h(attackerY).h(targetX).h(targetY)

                .c(0).s("").bytes();

    }



    /**

     * Selected-LiTo producer for retained {@code L1BowInstance} apertures.

     * Unlike a player-arrow miss, the original aperture always writes its

     * fixed damage marker even when no character intersects the lane and the

     * target object id is zero.  Keep that exceptional contract isolated from

     * {@link #rangedPhysicalAttack} so ordinary ranged misses remain strict.

     */

    static byte[] classicWorldBowAttack(

            long bowObjectId, long targetObjectId, int heading,

            int bowX, int bowY, int targetX, int targetY) {

        requireDwordId(bowObjectId, "classic-world bow attacker");

        requireUnsignedDword(targetObjectId, "classic-world bow target");

        if ((heading & 1) != 0 || heading < 0 || heading > 7

                || bowX < 0 || bowX > 0xFFFF

                || bowY < 0 || bowY > 0xFFFF

                || targetX < 0 || targetX > 0xFFFF

                || targetY < 0 || targetY > 0xFFFF) {

            throw new IllegalArgumentException("invalid classic-world bow fields");

        }

        return new Writer().c(S_ATTACK)

                .c(ClassicWorldBowCatalog.ATTACK_ACTION)

                .d(bowObjectId).d(targetObjectId)

                // LiTo.bin sub_4A5ED0 reads the fixed section as cddccd;

                // this selected client therefore consumes one damage byte.

                .c(ClassicWorldBowCatalog.DAMAGE).c(heading)

                .d(ClassicWorldBowCatalog.NATIVE_PROJECTILE_SEQUENCE)

                .h(ClassicWorldBowCatalog.PROJECTILE_GFX).c(0x7f)

                .h(bowX).h(bowY).h(targetX).h(targetY)

                .c(0).s("").bytes();

    }



    /**

     * Exact LiTo 3.81C {@code S_ATTACK_MANY} receiver {@code sub_4A6510}:

     * {@code cdhhcdhccs}, then {@code h(count)} and repeated {@code dc}.

     * The canonical range-magic producer uses directional type 8, zero for

     * the otherwise unused extra byte, an empty text field, and 0x20 as the

     * native per-target accepted-hit marker.

     */

    static byte[] attackMany(int actionId, long casterObjectId, int casterX, int casterY,

                             int heading, long sequence, int effectGfx,

                             List<AttackManyTarget> targets) {

        return attackMany(actionId, casterObjectId, casterX, casterY,

                heading, sequence, effectGfx, 8, targets);

    }



    /**

     * The native range-skill receiver uses type 0 for caster-centred spells

     * and type 8 for directional/target-centred spells.  Keep that byte

     * explicit so a selector-only no-direction spell cannot be serialized as

     * one of the already-verified line/area producers by accident.

     */

    static byte[] attackMany(int actionId, long casterObjectId, int casterX, int casterY,

                             int heading, long sequence, int effectGfx, int type,

                             List<AttackManyTarget> targets) {

        requireByte(actionId, "attack-many action");

        requireDwordId(casterObjectId, "attack-many caster");

        if (casterX < 0 || casterX > 0xFFFF || casterY < 0 || casterY > 0xFFFF

                || heading < 0 || heading > 7 || sequence <= 0 || sequence > 0xFFFF_FFFFL

                || effectGfx <= 0 || effectGfx > 0xFFFF

                || (type != 0 && type != 8)

                || targets == null || targets.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native attack-many fields");

        }

        Writer packet = new Writer().c(S_ATTACK_MANY)

                .c(actionId).d(casterObjectId).h(casterX).h(casterY)

                .c(heading).d(sequence).h(effectGfx)

                .c(type).c(0).s("").h(targets.size());

        for (AttackManyTarget target : targets) {

            if (target == null) {

                throw new IllegalArgumentException("null native attack-many target");

            }

            requireDwordId(target.objectId(), "attack-many target");

            packet.d(target.objectId()).c(target.hit() ? 0x20 : 0);

        }

        return packet.bytes();

    }



    /**

     * Native C_TELEPORT producers use either {@code chh} for the builder

     * slash-command path or {@code chhh} for click/list destinations.

     * The short form omits the map and therefore inherits the authoritative

     * current map on the server.

     */

    static TeleportRequest teleportRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_TELEPORT) {

            throw new IllegalArgumentException("not a native C_TELEPORT packet");

        }

        TeleportRequest request;

        if (in.remaining() == 4) {

            request = new TeleportRequest(false, -1, in.h(), in.h());

        } else if (in.remaining() == 6) {

            request = new TeleportRequest(true, in.h(), in.h(), in.h());

        } else {

            throw new IllegalArgumentException("invalid native C_TELEPORT layout");

        }

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing native C_TELEPORT bytes");

        }

        return request;

    }



    /**

     * Native builder command {@code teleport <character>} reaches

     * {@code sub_404DC0}, whose three C50 exits all use {@code cs}.  The

     * command splitter owns a 0x80-byte destination buffer including NUL.

     */

    static String teleportUserRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_TELEPORT_USER) {

            throw new IllegalArgumentException("not a native C_TELEPORT_USER packet");

        }

        String target = in.s();

        int encodedBytes = target.getBytes(LEGACY_TEXT).length;

        if (encodedBytes == 0 || encodedBytes > 0x7F

                || !target.equals(target.trim()) || containsControl(target)

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_TELEPORT_USER layout");

        }

        return target;

    }



    /**

     * Both exact {@code silence} producers ({@code sub_405650} and

     * {@code sub_452A60}) emit {@code chs}: an unsigned word followed by the

     * normalized target name.  The client parses the optional numeric input,

     * multiplies it by 60, caps ordinary positive values at 0x7D00, and uses

     * 0x0E10 when the field is omitted.  The wire parser preserves the full

     * unsigned word because negative/overflowing atoi input can still reach

     * the native writer; the session capability rejects values above the

     * intended 0x7D00 ceiling before changing state.

     */

    static SilenceRequest silenceRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_SILENCE) {

            throw new IllegalArgumentException("not a native C_SILENCE packet");

        }

        int durationSeconds = in.h();

        String target = in.s();

        int encodedBytes = target.getBytes(LEGACY_TEXT).length;

        if (encodedBytes == 0 || encodedBytes > 0x7F

                || !LEGACY_TEXT.newEncoder().canEncode(target)

                || !target.equals(target.trim()) || containsControl(target)

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_SILENCE layout");

        }

        return new SilenceRequest(durationSeconds, target);

    }



    /**

     * Exact native {@code wanted/notwanted/getwanted} producers all use

     * {@code ccs}: mode 1 adds an operand, mode 2 removes one, and mode 0

     * carries the static empty string.  The matching S_WANTED_LOGIN receiver

     * labels its first string as an account, so the server binds this operand

     * to an account watch only after the administrator and 0x3F-byte receiver

     * bounds have been checked.

     */

    static WantedRequest wantedRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_WANTED) {

            throw new IllegalArgumentException("not a native C_WANTED packet");

        }

        int mode = in.c();

        String operand = in.s();

        int encodedBytes = operand.getBytes(LEGACY_TEXT).length;

        boolean validShape = mode == 0 ? operand.isEmpty()

                : (mode == 1 || mode == 2) && encodedBytes > 0 && encodedBytes <= 0x7F;

        if (!validShape || !LEGACY_TEXT.newEncoder().canEncode(operand)

                || !operand.equals(operand.trim()) || containsControl(operand)

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_WANTED layout");

        }

        return new WantedRequest(mode, operand);

    }



    /**

     * Exact native C_SMS producers use two semantic layouts.  Mode 0 is

     * {@code ccss}: message first, destination second.  The command splitter

     * provides 0xC8/0x1C-byte buffers including NUL, while the equivalent UI

     * fields use still smaller limits.  Mode 1's reliable command producer is

     * {@code ccs}: one message whose destination is implicit in unavailable

     * original service state.

     *

     * <p>The mode-1 UI callback {@code sub_47B4E0} names {@code ccss} but

     * pushes only one string argument.  The variadic writer consequently

     * reads the saved UI object pointer as a second string; the fixed-base

     * object's vtable pointer terminates after three bytes.  That exact

     * three-byte producer artifact is accepted and kept opaque so the native

     * UI can receive a failure result without interpreting or logging it.</p>

     */

    static SmsRequest smsRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_SMS) {

            throw new IllegalArgumentException("not a native C_SMS packet");

        }

        int mode = in.c();

        String message = in.s();

        String destination = "";

        byte[] producerArtifact = new byte[0];

        if (mode == 0) {

            destination = in.s();

        } else if (mode == 1 && in.remaining() != 0) {

            producerArtifact = in.zBytes();

        }



        int messageBytes = message.getBytes(LEGACY_TEXT).length;

        int destinationBytes = destination.getBytes(LEGACY_TEXT).length;

        boolean validShape = switch (mode) {

            case 0 -> destinationBytes > 0 && destinationBytes <= 0x1B

                    && producerArtifact.length == 0;

            case 1 -> destination.isEmpty()

                    && (producerArtifact.length == 0 || Arrays.equals(producerArtifact,

                    new byte[] {(byte) 0x9C, (byte) 0x93, 0x4F}));

            default -> false;

        };

        if (!validShape || messageBytes == 0 || messageBytes > 0xC7

                || !LEGACY_TEXT.newEncoder().canEncode(message)

                || !LEGACY_TEXT.newEncoder().canEncode(destination)

                || !destination.equals(destination.trim())

                || containsControl(message) || containsControl(destination)

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_SMS layout");

        }

        return new SmsRequest(mode, message, destination, producerArtifact);

    }



    /**

     * Exact native C_SUMMON producers use four wire layouts:

     * {@code csc} for ordinary {@code summon/summonf} (modes 0/1) and the

     * protected management modes 3..7, {@code cscd} for mode 2, and

     * {@code cscdd} for mode 8.  The native command buffers establish three

     * distinct string ceilings: 0x7f bytes for modes 0/1, 0xff for modes

     * 4/6/7, and 0x0f for mode 8.  Modes 2/3/5 always emit the shared empty

     * string.  This fixes only the client envelope; it does not infer the

     * original server's NPC catalogue or bot-management mutation.

     */

    static SummonRequest summonRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_SUMMON) {

            throw new IllegalArgumentException("not a native C_SUMMON packet");

        }

        String argument = in.s();

        int mode = in.c();

        Long firstValue = null;

        Long secondValue = null;

        switch (mode) {

            case 0, 1, 3, 4, 5, 6, 7 -> { }

            case 2 -> firstValue = in.d();

            case 8 -> {

                firstValue = in.d();

                secondValue = in.d();

            }

            default -> throw new IllegalArgumentException("unsupported native C_SUMMON mode");

        }



        int encodedBytes = argument.getBytes(LEGACY_TEXT).length;

        boolean invalidArgument = !LEGACY_TEXT.newEncoder().canEncode(argument);

        if (mode == 0 || mode == 1) {

            invalidArgument |= encodedBytes == 0 || encodedBytes > 0x7F;

        } else if (mode == 2 || mode == 3 || mode == 5) {

            invalidArgument |= !argument.isEmpty();

        } else if (mode == 8) {

            invalidArgument |= encodedBytes > 0x0F;

        } else {

            invalidArgument |= encodedBytes > 0xFF;

        }

        if (invalidArgument || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_SUMMON layout");

        }

        return new SummonRequest(argument, mode, firstValue, secondValue);

    }



    /**

     * Exact native C_BUILDER_CONTROL producers form a classic builder/admin

     * multiplexer.  {@code control <number>} (and the fixed {@code quiet}

     * alias) can emit {@code cc} for any low-byte subtype, so an opcode plus

     * subtype is a complete native packet even when that subtype also has a

     * named producer with a body.  The remaining named forms use the layouts

     * below; no original-server mutation or reply is inferred here.

     */

    static BuilderControlRequest builderControlRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BUILDER_CONTROL) {

            throw new IllegalArgumentException("not a native C_BUILDER_CONTROL packet");

        }

        int subtype = in.c();

        if (in.remaining() == 0) {

            return new BuilderControlRequest(subtype, BuilderControlLayout.SIGNAL,

                    null, null, null, null, null);

        }



        Long firstDword = null;

        Long secondDword = null;

        Integer firstByte = null;

        Integer secondByte = null;

        String text = null;

        BuilderControlLayout layout;

        switch (subtype) {

            case 0x80, 0x86 -> {

                layout = BuilderControlLayout.TEXT;

                text = in.s();

                requireBuilderText(text, 1, 0xFF, "query text");

            }

            case 0x87, 0x91, 0x98, 0x9B -> {

                layout = BuilderControlLayout.DWORD;

                firstDword = in.d();

                if (subtype == 0x9B && (firstDword < 1 || firstDword > 8)) {

                    throw new IllegalArgumentException("invalid native castle-war id");

                }

            }

            case 0x8A -> {

                layout = BuilderControlLayout.TWO_BYTES_TEXT;

                firstByte = in.c();

                secondByte = in.c();

                text = in.s();

                boolean dog = firstByte >= 1 && firstByte <= 49 && secondByte == 1;

                boolean monster = firstByte == 1 && secondByte >= 1 && secondByte <= 16;

                requireBuilderText(text, 1, 0x7F, "wish target");

                if (!dog && !monster) {

                    throw new IllegalArgumentException("invalid native wishdog/wishmon controls");

                }

            }

            case 0x8C, 0x8E -> {

                layout = BuilderControlLayout.BYTE;

                firstByte = in.c();

                int maximum = subtype == 0x8C ? 99 : 8;

                if (firstByte < 1 || firstByte > maximum) {

                    throw new IllegalArgumentException("invalid native builder byte value");

                }

            }

            case 0x93, 0x94, 0x95, 0x96 -> {

                layout = BuilderControlLayout.TWO_DWORDS;

                firstDword = in.d();

                secondDword = in.d();

                if (subtype == 0x93 && firstDword > 2) {

                    throw new IllegalArgumentException("invalid native flag operation");

                }

                if (subtype == 0x94 && (firstDword < 1 || firstDword > 8)) {

                    throw new IllegalArgumentException("invalid native castle id");

                }

            }

            case 0x97 -> {

                layout = BuilderControlLayout.TWO_BYTES;

                firstByte = in.c();

                secondByte = in.c();

                if (firstByte > 5 || secondByte < 3 || secondByte > 25) {

                    throw new IllegalArgumentException("invalid native setstat values");

                }

            }

            case 0x99 -> {

                layout = BuilderControlLayout.DWORD_TEXT;

                firstDword = in.d();

                text = in.s();

                requireBuilderText(text, 1, 0x0F, "suspend target");

            }

            case 0x9A -> {

                layout = BuilderControlLayout.NOTICE;

                firstDword = in.d();

                secondDword = in.d();

                firstByte = in.c();

                text = in.s();

                if (firstDword < 1 || firstDword > 0x19BF_CC00L) {

                    throw new IllegalArgumentException("invalid native notice start time");

                }

                requireBuilderText(text, 1, 0xC7, "notice contents");

            }

            default -> throw new IllegalArgumentException(

                    "unsupported native C_BUILDER_CONTROL body subtype");

        }

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing native C_BUILDER_CONTROL bytes");

        }

        return new BuilderControlRequest(subtype, layout, firstDword, secondDword,

                firstByte, secondByte, text);

    }



    private static void requireBuilderText(String value, int minimumBytes,

                                           int maximumBytes, String field) {

        int encodedBytes = value.getBytes(LEGACY_TEXT).length;

        if (!LEGACY_TEXT.newEncoder().canEncode(value)

                || encodedBytes < minimumBytes || encodedBytes > maximumBytes

                || containsControl(value)) {

            throw new IllegalArgumentException("invalid native builder " + field);

        }

    }



    /**

     * Native {@code sub_4AF280} and the deferred teleport path in

     * {@code sub_4E2150} both emit {@code cc}: C_RETURN_SUMMON followed by

     * the client's current summon-state byte.  The exact client does not

     * constrain that byte, so it is parsed but never trusted as authority.

     */

    static int returnSummon(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_RETURN_SUMMON) {

            throw new IllegalArgumentException("not a native C_RETURN_SUMMON packet");

        }

        int summonState = in.c();

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing native C_RETURN_SUMMON bytes");

        }

        return summonState;

    }



    /**

     * S_REQUEST_SUMMON itself is body-independent in exact receiver

     * {@code sub_4AF280}.  The two control bytes plus player object id retain

     * the established 3.81 server-family S_Teleport envelope.

     */

    static byte[] requestSummon(long playerObjectId) {

        requireDwordId(playerObjectId, "request-summon player object id");

        return new Writer().c(S_REQUEST_SUMMON).c(0).c(0x40)

                .d(playerObjectId).bytes();

    }



    /** Native S_PORTAL receiver sub_4AF4B0 parses {@code hhh}: map, x, y. */

    static byte[] portal(int mapId, int x, int y) {

        if (mapId < 0 || mapId > 0xFFFF || x < 0 || x > 0xFFFF

                || y < 0 || y > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_PORTAL destination");

        }

        return new Writer().c(S_PORTAL).h(mapId).h(x).h(y).bytes();

    }



    /** Native sub_48BC80 emits C_GOTO_PORTAL immediately before clearing the world. */

    static void gotoPortal(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_GOTO_PORTAL || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_GOTO_PORTAL layout");

        }

    }



    /**

     * Both exact-client {@code /ping} producers emit {@code cc}: opcode 0x71

     * and monitor target 0 (the game server). Other C_MONITOR_CONTROL

     * subtypes have separate native layouts and are deliberately not folded

     * into this round trip.

     */

    static int monitorPingRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_MONITOR_CONTROL) {

            throw new IllegalArgumentException("not a native C_MONITOR_CONTROL packet");

        }

        int target = in.c();

        if (target != 0 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_MONITOR_CONTROL ping layout");

        }

        return target;

    }



    /** Native S_PING receivers parse {@code c}: the echoed monitor target. */

    static byte[] ping(int target) {

        if (target < 0 || target > 4) {

            throw new IllegalArgumentException("invalid native S_PING target");

        }

        return new Writer().c(S_PING).c(target).bytes();

    }



    /** Native sub_4AEFB0 reads current/max MP directly as two little-endian words. */

    static byte[] manaPoint(int current, int maximum) {

        int maximumAllowed = NumericBreakthrough.enabled()

                ? NumericBreakthrough.MAX_VITAL : 0x7FFF;

        if (current < 0 || maximum <= 0 || current > maximum || maximum > maximumAllowed) {

            throw new IllegalArgumentException("invalid native S_MANA_POINT values");

        }

        return new Writer().c(S_MANA_POINT).h(current).h(maximum).bytes();

    }



    /** Exact LiTo 3.81C sub_4A36F0 {@code dcdcccccchhhhcdcchcccc}. */

    static byte[] characterStatus(long objectId, int level, long experience,

                                  int strength, int constitution, int dexterity,

                                  int charisma, int intelligence, int wisdom,

                                  int currentHp, int currentMp,

                                  int maximumHp, int maximumMp,

                                  int armorClass, int food225, int weight240,

                                  int lawful, int fireResistance,

                                  int waterResistance, int windResistance,

                                  int earthResistance) {

        requireDwordId(objectId, "status object id");

        if (level <= 0 || level > (NumericBreakthrough.levelAbilityEnabled()

                ? NumericBreakthrough.MAX_LEVEL : 0xFF)

                || experience < 0 || experience > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("invalid native character status progress");

        }

        if (NumericBreakthrough.levelAbilityEnabled()) {

            requireBreakthroughAbility(strength, "status strength");

            requireBreakthroughAbility(constitution, "status constitution");

            requireBreakthroughAbility(dexterity, "status dexterity");

            requireBreakthroughAbility(charisma, "status charisma");

            requireBreakthroughAbility(intelligence, "status intelligence");

            requireBreakthroughAbility(wisdom, "status wisdom");

        } else {

            requireByte(strength, "status strength");

            requireByte(constitution, "status constitution");

            requireByte(dexterity, "status dexterity");

            requireByte(charisma, "status charisma");

            requireByte(intelligence, "status intelligence");

            requireByte(wisdom, "status wisdom");

        }

        if (NumericBreakthrough.enabled()) {

            requireBreakthroughVital(currentHp, maximumHp, "status HP");

            requireBreakthroughVital(currentMp, maximumMp, "status MP");

            if (armorClass < NumericBreakthrough.MIN_ARMOR_CLASS

                    || armorClass > NumericBreakthrough.MAX_ARMOR_CLASS) {

                throw new IllegalArgumentException("status armor class exceeds breakthrough range");

            }

        } else {

            requireUnsignedWord(currentHp, "status current HP");

            requireUnsignedWord(currentMp, "status current MP");

            requireUnsignedWord(maximumHp, "status maximum HP");

            requireUnsignedWord(maximumMp, "status maximum MP");

            requireSignedByte(armorClass, "status armor class");

        }

        requireSignedWord(lawful, "status lawful");

        if (NumericBreakthrough.elementalResistanceEnabled()) {

            requireBreakthroughElementalResistance(fireResistance,

                    "status fire resistance");

            requireBreakthroughElementalResistance(waterResistance,

                    "status water resistance");

            requireBreakthroughElementalResistance(windResistance,

                    "status wind resistance");

            requireBreakthroughElementalResistance(earthResistance,

                    "status earth resistance");

        } else {

            requireSignedByte(fireResistance, "status fire resistance");

            requireSignedByte(waterResistance, "status water resistance");

            requireSignedByte(windResistance, "status wind resistance");

            requireSignedByte(earthResistance, "status earth resistance");

        }

        Writer packet = new Writer().c(S_STATUS)

                // S_STATUS keeps a saturated byte level as the native shadow.

                // Schema 2 publishes the full dword level through S_EXP.

                .d(objectId).c(Math.min(level, 0xFF)).d(experience);

        if (NumericBreakthrough.levelAbilityEnabled()) {

            packet.d(strength).d(intelligence).d(wisdom)

                    .d(dexterity).d(constitution).d(charisma);

        } else {

            packet

                // sub_4A36F0 destinations 0x8348B0..B5 are the retained

                // S_OwnCharStatus order: STR, INT, WIS, DEX, CON, CHA.

                // Keep the public method arguments semantic so callers cannot

                // accidentally encode the selected client's storage order.

                .c(strength).c(intelligence).c(wisdom)

                .c(dexterity).c(constitution).c(charisma);

        }

        if (NumericBreakthrough.enabled()) {

            packet.d(currentHp).d(maximumHp).d(currentMp).d(maximumMp)

                    .d(armorClass);

        } else {

            // The unpatched selected receiver uses the same destination order

            // as retained S_OwnCharStatus: current HP, max HP, current MP,

            // max MP.  Schema 1 widens these same four positions only.

            packet.h(currentHp).h(maximumHp).h(currentMp).h(maximumMp)

                    .c(armorClass);

        }

        // This Tournament client renders both gauges as value * 100 / 29

        // (sub_4CFF40/sub_4CFDE0).  The retained database domains are food225

        // and weight240, so sending either raw value overflows the native bars

        // and can make a perfectly valid character appear corrupt. Saturating

        // projection also keeps an out-of-range legacy row from disconnecting

        // the client during S_STATUS construction.

        packet.d(0).c(nativeFoodGauge(food225))

                .c(nativeWeightGauge(weight240)).h(lawful);

        if (NumericBreakthrough.elementalResistanceEnabled()) {

            packet.d(Integer.toUnsignedLong(fireResistance))

                    .d(Integer.toUnsignedLong(waterResistance))

                    .d(Integer.toUnsignedLong(windResistance))

                    .d(Integer.toUnsignedLong(earthResistance));

        } else {

            packet.c(fireResistance).c(waterResistance)

                    .c(windResistance).c(earthResistance);

        }

        return packet.bytes();

    }



    /**

     * Exact LiTo 3.81C S_MASTER receiver {@code sub_4AEFD0}: {@code ds}.

     * The receiver resolves an existing world object and stores the supplied

     * owner/master name on that actor, so this packet must follow S_PUT_OBJECT.

     */

    static byte[] master(long objectId, String masterName) {

        requireDwordId(objectId, "master world object id");

        requireNativeString(masterName, "master name");

        if (masterName.getBytes(LEGACY_TEXT).length > 0x3F

                || containsControl(masterName)) {

            throw new IllegalArgumentException("invalid native S_MASTER name");

        }

        return new Writer().c(S_MASTER).d(objectId).s(masterName).bytes();

    }



    /**

     * Exact LiTo 3.81C S_MAGIC_STATUS receiver {@code sub_4AF320}: {@code cc}.

     * Same-family 3.81 S_SPMR identifies the bytes as current SP and MR

     * modifiers; an unmodified tournament character therefore bootstraps 0/0.

     * Numeric schema 3 replaces that exact receiver with a paired dword decoder.

     */

    static byte[] magicStatus(int spellPowerModifier, int magicResistanceModifier) {

        Writer packet = new Writer().c(S_MAGIC_STATUS);

        if (NumericBreakthrough.magicModifierEnabled()) {

            requireBreakthroughModifier(spellPowerModifier, "spell-power modifier");

            requireBreakthroughModifier(magicResistanceModifier,

                    "magic-resistance modifier");

            return packet.d(Integer.toUnsignedLong(spellPowerModifier))

                    .d(Integer.toUnsignedLong(magicResistanceModifier)).bytes();

        }

        requireSignedByte(spellPowerModifier, "spell-power modifier");

        requireSignedByte(magicResistanceModifier, "magic-resistance modifier");

        return packet.c(spellPowerModifier).c(magicResistanceModifier).bytes();

    }



    /** Exact LiTo receiver {@code sub_4AFE90}: {@code hccc}. */

    static byte[] mageStrength(int durationSeconds, int currentStrength,

                               int weight, int type) {

        requireUnsignedWord(durationSeconds, "strength-buff duration");

        if (NumericBreakthrough.levelAbilityEnabled()) {

            requireBreakthroughAbility(currentStrength, "current strength");

        } else {

            requireByte(currentStrength, "current strength");

        }

        requireByte(weight, "weight");

        requireByte(type, "strength-buff type");

        return new Writer().c(S_MAGE_STRENGTH).h(durationSeconds)

                .c(Math.min(currentStrength, NativeCharacterStats.MAX_RUNTIME_ABILITY))

                .c(weight).c(type).bytes();

    }



    /** Exact LiTo receiver {@code sub_4AFEE0}: {@code hcc}. */

    static byte[] mageDexterity(int durationSeconds, int currentDexterity, int type) {

        requireUnsignedWord(durationSeconds, "dexterity-buff duration");

        if (NumericBreakthrough.levelAbilityEnabled()) {

            requireBreakthroughAbility(currentDexterity, "current dexterity");

        } else {

            requireByte(currentDexterity, "current dexterity");

        }

        requireByte(type, "dexterity-buff type");

        return new Writer().c(S_MAGE_DEXTERITY).h(durationSeconds)

                .c(Math.min(currentDexterity, NativeCharacterStats.MAX_RUNTIME_ABILITY))

                .c(type).bytes();

    }



    /** Exact LiTo receiver {@code sub_4AFF30}: {@code hc}. */

    static byte[] mageShield(int durationSeconds, int type) {

        requireUnsignedWord(durationSeconds, "shield-buff duration");

        requireByte(type, "shield-buff type");

        return new Writer().c(S_MAGE_SHIELD).h(durationSeconds).c(type).bytes();

    }



    /**

     * Native {@code sub_4AF0A0} copies STR, INT, WIS, DEX, CON, CHA and the

     * inventory weight byte into {@code 0x8348B0..B5/CB}.  This exact LiTo

     * build renders that byte on a 0..29 gauge, so the retained weight240

     * domain is projected at the wire boundary; it is not an unspent-stat

     * counter.

     */

    static byte[] abilityScores(int strength, int constitution, int dexterity,

                                 int charisma, int intelligence, int wisdom,

                                 int weight240) {

        if (NumericBreakthrough.levelAbilityEnabled()) {

            requireBreakthroughAbility(strength, "strength");

            requireBreakthroughAbility(constitution, "constitution");

            requireBreakthroughAbility(dexterity, "dexterity");

            requireBreakthroughAbility(charisma, "charisma");

            requireBreakthroughAbility(intelligence, "intelligence");

            requireBreakthroughAbility(wisdom, "wisdom");

        } else {

            requireByte(strength, "strength");

            requireByte(constitution, "constitution");

            requireByte(dexterity, "dexterity");

            requireByte(charisma, "charisma");

            requireByte(intelligence, "intelligence");

            requireByte(wisdom, "wisdom");

        }

        Writer packet = new Writer().c(S_ABILITY_SCORES);

        if (NumericBreakthrough.levelAbilityEnabled()) {

            packet.d(strength).d(intelligence).d(wisdom)

                    .d(dexterity).d(constitution).d(charisma);

        } else {

            packet.c(strength).c(intelligence).c(wisdom)

                    .c(dexterity).c(constitution).c(charisma);

        }

        return packet.c(nativeWeightGauge(weight240)).bytes();

    }



    /** Exact 0..29 LiTo food gauge projected from the retained 0..225 domain. */

    static int nativeFoodGauge(int food225) {

        return nativeGauge29(food225, 225);

    }



    /** Exact 0..29 LiTo weight gauge projected from retained getWeight240(). */

    static int nativeWeightGauge(int weight240) {

        return nativeGauge29(weight240, 240);

    }



    private static int nativeGauge29(int retainedValue, int retainedMaximum) {

        int saturated = Math.max(0, Math.min(retainedMaximum, retainedValue));

        return (saturated * 29 + retainedMaximum / 2) / retainedMaximum;

    }



    /** Native sub_4AF0E0 parses {@code cd}: level followed by total EXP. */

    static byte[] experience(int level, long totalExperience) {

        if (level <= 0 || level > (NumericBreakthrough.levelAbilityEnabled()

                ? NumericBreakthrough.MAX_LEVEL : 0xFF)

                || totalExperience < 0 || totalExperience > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("invalid native S_EXP values");

        }

        Writer packet = new Writer().c(S_EXP);

        if (NumericBreakthrough.levelAbilityEnabled()) packet.d(level);

        else packet.c(level);

        return packet.d(totalExperience).bytes();

    }



    /** Native sub_4AF100 parses AC plus fire, water, wind and earth resistance. */

    static byte[] armorClass(int base, int fireResistance, int waterResistance,

                             int windResistance, int earthResistance) {

        if (NumericBreakthrough.enabled()) {

            if (base < NumericBreakthrough.MIN_ARMOR_CLASS

                    || base > NumericBreakthrough.MAX_ARMOR_CLASS) {

                throw new IllegalArgumentException("base armor class exceeds breakthrough range");

            }

        } else {

            requireSignedByte(base, "base armor class");

        }

        if (NumericBreakthrough.elementalResistanceEnabled()) {

            requireBreakthroughElementalResistance(fireResistance,

                    "fire resistance");

            requireBreakthroughElementalResistance(waterResistance,

                    "water resistance");

            requireBreakthroughElementalResistance(windResistance,

                    "wind resistance");

            requireBreakthroughElementalResistance(earthResistance,

                    "earth resistance");

        } else {

            requireSignedByte(fireResistance, "fire resistance");

            requireSignedByte(waterResistance, "water resistance");

            requireSignedByte(windResistance, "wind resistance");

            requireSignedByte(earthResistance, "earth resistance");

        }

        Writer packet = new Writer().c(S_AC);

        if (NumericBreakthrough.enabled()) packet.d(base);

        else packet.c(base);

        if (NumericBreakthrough.elementalResistanceEnabled()) {

            packet.d(Integer.toUnsignedLong(fireResistance))

                    .d(Integer.toUnsignedLong(waterResistance))

                    .d(Integer.toUnsignedLong(windResistance))

                    .d(Integer.toUnsignedLong(earthResistance));

        } else {

            packet.c(fireResistance).c(waterResistance)

                    .c(windResistance).c(earthResistance);

        }

        return packet.bytes();

    }



    private static void requireBreakthroughElementalResistance(int value,

                                                                String name) {

        if (value < NumericBreakthrough.MIN_ELEMENTAL_RESISTANCE

                || value > NumericBreakthrough.MAX_ELEMENTAL_RESISTANCE) {

            throw new IllegalArgumentException(name + " exceeds breakthrough range");

        }

    }



    private static void requireBreakthroughVital(int current, int maximum, String name) {

        if (current < 0 || maximum <= 0 || current > maximum

                || maximum > NumericBreakthrough.MAX_VITAL) {

            throw new IllegalArgumentException(name + " exceeds breakthrough range");

        }

    }



    private static void requireBreakthroughAbility(int value, String name) {

        if (value < 0 || value > NumericBreakthrough.MAX_ABILITY) {

            throw new IllegalArgumentException(name + " exceeds breakthrough range");

        }

    }



    private static void requireBreakthroughModifier(int value, String name) {

        if (value < NumericBreakthrough.MIN_MAGIC_MODIFIER

                || value > NumericBreakthrough.MAX_MAGIC_MODIFIER) {

            throw new IllegalArgumentException(name + " is outside schema-3 range");

        }

    }



    /** Native S_SAY receiver sub_4A2E30: {@code cdshh}. */

    static byte[] say(int type, long objectId, String message, int x, int y) {

        requireByte(type, "chat type");

        requireDwordId(objectId, "speaker object id");

        requireNativeString(message, "chat message");

        if (message.getBytes(LEGACY_TEXT).length > 0x1FF

                || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_SAY fields");

        }

        return new Writer().c(S_SAY).c(type).d(objectId).s(message).h(x).h(y).bytes();

    }



    /** Native S_SAY_CODE receiver sub_4A3140: {@code cdshh}. */

    static byte[] sayCode(int type, long objectId, String message, int x, int y) {

        if (type < 0 || type > 3) {

            throw new IllegalArgumentException("NPC speech type is outside native 0..3 range");

        }

        requireDwordId(objectId, "NPC speaker object id");

        if (message == null || message.isEmpty() || message.indexOf('\0') >= 0

                || message.getBytes(LEGACY_TEXT).length > 0x1FF) {

            throw new IllegalArgumentException("NPC speech exceeds native 511-byte storage");

        }

        requireUnsignedWord(x, "NPC speech x");

        requireUnsignedWord(y, "NPC speech y");

        return new Writer().c(S_SAY_CODE).c(type).d(objectId).s(message).h(x).h(y).bytes();

    }



    /** Native {@code S_CHANGE_DIRECTION}: object dword followed by heading byte. */

    static byte[] changeDirection(long objectId, int heading) {

        requireDwordId(objectId, "heading object id");

        if (heading < 0 || heading > 7) {

            throw new IllegalArgumentException("heading outside native 0..7 range");

        }

        return new Writer().c(S_CHANGE_DIRECTION).d(objectId).c(heading).bytes();

    }



    /**

     * LiTo mapping of retained {@code S_HelpMessage}: NPC channel type 0,

     * synthetic object id 0, and the exact native {@code cdshh} receiver.

     * The retained TW opcode 0xA1 is not registered by the selected LiTo.bin;

     * recovered {@code sub_4A3140} explicitly accepts a failed id-0 lookup.

     */

    static byte[] questHelpMessage(String message, int x, int y) {

        if (message == null || message.isEmpty() || message.indexOf('\0') >= 0

                || message.getBytes(LEGACY_TEXT).length > 0x1FF) {

            throw new IllegalArgumentException(

                    "quest help message exceeds native 511-byte storage");

        }

        requireUnsignedWord(x, "quest help message x");

        requireUnsignedWord(y, "quest help message y");

        return new Writer().c(S_SAY_CODE).c(0).d(0).s(message)

                .h(x).h(y).bytes();

    }



    /** Native S_TELL receiver sub_4A3310: {@code ss}. */

    static byte[] tell(String sender, String message) {

        requireNativeString(sender, "tell sender");

        requireNativeString(message, "tell message");

        if (sender.getBytes(LEGACY_TEXT).length > 27

                || message.getBytes(LEGACY_TEXT).length > 0xFF) {

            throw new IllegalArgumentException("invalid native S_TELL fields");

        }

        return new Writer().c(S_TELL).s(sender).s(message).bytes();

    }



    /** Native C_TITLE producer sub_4044A0: {@code css}. */

    static TitleRequest titleRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_TITLE) throw new IllegalArgumentException("not a native C_TITLE packet");

        TitleRequest request = new TitleRequest(in.s().trim(), in.s().trim());

        int nameBytes = request.characterName().getBytes(LEGACY_TEXT).length;

        int titleBytes = request.title().getBytes(LEGACY_TEXT).length;

        if (nameBytes == 0 || nameBytes > 27 || titleBytes == 0 || titleBytes > 16

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_TITLE layout");

        }

        return request;

    }



    /** Native C_REQUEST_ROLL producer sub_4949C0: {@code cc}. */

    static int characterRollClass(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_REQUEST_ROLL) {

            throw new IllegalArgumentException("not a native C_REQUEST_ROLL packet");

        }

        int characterClass = in.c();

        if (!CharacterRollCatalog.supports(characterClass) || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native character-roll class");

        }

        return characterClass;

    }



    /** Native S_TITLE receiver sub_4A73F0: {@code ds}. */

    static byte[] title(long worldObjectId, String title) {

        requireDwordId(worldObjectId, "world object id");

        requireNativeString(title, "character title");

        if (title.getBytes(LEGACY_TEXT).length > 16) {

            throw new IllegalArgumentException("character title exceeds native 16-byte field");

        }

        return new Writer().c(S_TITLE).d(worldObjectId).s(title).bytes();

    }



    /**

     * Narrows retained SQL text to the exact native title buffer without

     * splitting an MS950 character or allowing an unencodable/control value

     * to abort world entry.

     */

    static String nativeTitleValue(String retainedTitle) {

        if (retainedTitle == null || retainedTitle.isEmpty()) return "";

        StringBuilder accepted = new StringBuilder();

        for (int offset = 0; offset < retainedTitle.length();) {

            int codePoint = retainedTitle.codePointAt(offset);

            offset += Character.charCount(codePoint);

            if (Character.isISOControl(codePoint)) continue;

            String character = new String(Character.toChars(codePoint));

            if (!LEGACY_TEXT.newEncoder().canEncode(character)) continue;

            String candidate = accepted + character;

            if (candidate.getBytes(LEGACY_TEXT).length > 16) break;

            accepted.append(character);

        }

        return accepted.toString();

    }



    /** Native S_ROLL_RESULT receiver sub_4AEA80: six one-byte attributes. */

    static byte[] characterRollResult(int[] attributes) {

        if (attributes == null || attributes.length != CharacterRollCatalog.ATTRIBUTE_COUNT) {

            throw new IllegalArgumentException("native character roll requires six attributes");

        }

        Writer packet = new Writer().c(S_ROLL_RESULT);

        for (int attribute : attributes) {

            requireByte(attribute, "character-roll attribute");

            packet.c(attribute);

        }

        return packet.bytes();

    }



    /** Native C_CREATE_CUSTOM_CHARACTER producer sub_492580: {@code cscccccccccc}. */

    static CustomCharacterRequest customCharacter(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_CREATE_CUSTOM_CHARACTER) {

            throw new IllegalArgumentException("not a native C_CREATE_CUSTOM_CHARACTER packet");

        }

        String name = in.s().trim();

        int sex = in.c();

        int characterClass = in.c();

        List<Integer> attributes = new ArrayList<>(CharacterRollCatalog.ATTRIBUTE_COUNT);

        for (int index = 0; index < CharacterRollCatalog.ATTRIBUTE_COUNT; index++) {

            attributes.add(in.c());

        }

        int value1 = in.c();

        int value2 = in.c();

        int[] attributeArray = attributes.stream().mapToInt(Integer::intValue).toArray();

        int nameBytes = name.getBytes(LEGACY_TEXT).length;

        // sub_4A7550 stores the directory name in a 0x1C-byte field including

        // NUL.  A longer created name could never be selected safely by C_C9.

        if (nameBytes == 0 || nameBytes > 27 || name.chars().anyMatch(Character::isISOControl)

                || sex > 1 || !CharacterRollCatalog.contains(characterClass, attributeArray)

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native custom-character request");

        }

        return new CustomCharacterRequest(name, sex, characterClass,

                List.copyOf(attributes), value1, value2);

    }



    /** Native success signal; sub_4A74F0 consumes no body and emits C_C8. */

    static byte[] newCharacterInfo() {

        return new Writer().c(S_NEW_CHAR_INFO).bytes();

    }



    /**

     * Exact opcode-only S_04 compatibility event for the native

     * {@code /ltcharsvr} and {@code /lteventcsvr} launch modes.

     *

     * <p>This is not a character row or a normal directory terminator.  The

     * selected client dispatches it to {@code sub_4A74F0} only before active

     * gameplay; the callback automatically emits C_C8 only when the special

     * character-server flag is set.  Active gameplay treats the same opcode

     * as a fatal protocol mismatch and sends C_QUIT.  Keep this exact writer

     * outside normal production; its only permitted call site is the dedicated

     * loopback/read-only-memory {@code /ltcharsvr} runtime fixture.</p>

     */

    static byte[] specialCharacterServerEvent() {

        return new Writer().c(S_CHARACTER_INFO).bytes();

    }



    /** Native S_03 receiver {@code sub_4A7450}: {@code c(count)}, max 8 rows. */

    static byte[] litoCharacterDirectoryCount(int count) {

        if (count < 0 || count > 8) {

            throw new IllegalArgumentException("LiTo character count is outside native 8-row storage");

        }

        return new Writer().c(S_NUM_CHARACTER).c(count).bytes();

    }



    /**

     * Native S_CB receiver {@code sub_4A7550}:

     * {@code sscchhhcccccccccddddddddddcdddddc}.

     */

    static byte[] litoCharacterDirectoryRow(LitoCharacterDirectoryRow row) {

        if (row == null) throw new IllegalArgumentException("missing LiTo character row");

        if (row.rankings() == null || row.battleStatistics() == null) {

            throw new IllegalArgumentException("missing LiTo character extension");

        }

        requireBoundedLegacyString(row.name(), 1, 27, "LiTo character name");

        requireBoundedLegacyString(row.clanName(), 0, 16, "LiTo clan name");

        // sub_4A7550 stores the class byte without a 0..4 guard. The later

        // sub_4928C0 selector has dedicated 0/1/2/4 branches and routes 3/5/6

        // through its common class UI, so 3.81 dragon knights and illusionists

        // are valid directory rows as well.

        if (row.type() < 0 || row.type() > 6 || row.sex() < 0 || row.sex() > 1) {

            throw new IllegalArgumentException("invalid LiTo character type or sex");

        }

        requireSignedWord(row.lawful(), "LiTo lawful");

        requireSignedNonNegativeWord(row.hp(), "LiTo hp");

        requireSignedNonNegativeWord(row.mp(), "LiTo mp");

        requireSignedByte(row.armorClass(), "LiTo armor class");

        requirePositiveSignedByte(row.level(), "LiTo level");

        requireNonNegativeSignedByte(row.strength(), "LiTo strength");

        requireNonNegativeSignedByte(row.dexterity(), "LiTo dexterity");

        requireNonNegativeSignedByte(row.constitution(), "LiTo constitution");

        requireNonNegativeSignedByte(row.wisdom(), "LiTo wisdom");

        requireNonNegativeSignedByte(row.charisma(), "LiTo charisma");

        requireNonNegativeSignedByte(row.intelligence(), "LiTo intelligence");

        requireUnsignedDword(row.nativeTime(), "LiTo native character time");



        int checkCode = row.level() ^ row.strength() ^ row.dexterity()

                ^ row.constitution() ^ row.wisdom() ^ row.charisma()

                ^ row.intelligence();

        LitoCharacterRankings ranks = row.rankings();

        LitoCharacterBattleStatistics battle = row.battleStatistics();

        return new Writer().c(S_LITO_CHARACTER_INFO)

                .s(row.name()).s(row.clanName())

                .c(row.type()).c(row.sex())

                .h(row.lawful()).h(row.hp()).h(row.mp())

                .c(row.armorClass()).c(row.level())

                .c(row.strength()).c(row.dexterity()).c(row.constitution())

                .c(row.wisdom()).c(row.charisma()).c(row.intelligence())

                .c(0)

                .d(row.nativeTime())

                // Wire order follows the destination pointers pushed by

                // sub_4A7550, not the visual ranking-row order.

                .d(ranks.overall()).d(ranks.level()).d(ranks.classicServer())

                .d(ranks.channel()).d(ranks.race())

                .d(battle.disconnects()).d(battle.winRateDenominator())

                .d(battle.wins()).d(battle.losses())

                .c(battle.rankingSelector())

                .d(ranks.overallMovement()).d(ranks.levelMovement())

                .d(ranks.classicServerMovement()).d(ranks.channelMovement())

                .d(ranks.raceMovement())

                .c(checkCode)

                .bytes();

    }



    /** Native S_06 branch in sub_4903D0 consumes {@code c(status)d(message)}. */

    static byte[] createCharacterFailure(CreateCharacterFailure failure, long messageCode) {

        return nativeCheck(S_CREATE_CHARACTER_CHECK, failure, messageCode);

    }



    /**

     * Recovered native character callbacks.

     *

     * <p>C_ENTER_WORLD keeps the same-family {@code cs} envelope.  The exact

     * C_DELETE_CHARACTER callback has two branches: {@code cs} and

     * {@code csd}.  The extra dword comes from a native UI control, but it is

     * not trusted as authorization; account ownership remains authoritative.</p>

     */

    static ClassicCharacterRequest classicCharacterRequest(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_ENTER_WORLD && expectedOpcode != C_DELETE_CHARACTER) {

            throw new IllegalArgumentException("unsupported classic character request opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode) {

            throw new IllegalArgumentException("wrong classic character request opcode");

        }

        String name = in.s().trim();

        int encodedLength = name.getBytes(LEGACY_TEXT).length;

        boolean hasDeleteValue = expectedOpcode == C_DELETE_CHARACTER && in.remaining() == 4;

        long deleteValue = hasDeleteValue ? in.d() : 0;

        if (encodedLength == 0 || encodedLength > 27 || in.remaining() != 0

                || name.chars().anyMatch(Character::isISOControl)

                || (expectedOpcode == C_ENTER_WORLD && hasDeleteValue)) {

            throw new IllegalArgumentException("invalid classic character request");

        }

        return new ClassicCharacterRequest(name, hasDeleteValue, deleteValue);

    }



    /** LiTo sub_4903D0 reads {@code c(status)d(argument)} for S_07. */

    static byte[] enterWorldCheck(EnterWorldCheckResult result, long argument) {

        return nativeCheck(S_ENTER_WORLD_CHECK, result, argument);

    }



    /** LiTo sub_4903D0 reads {@code c(status)d(argument)} for S_09. */

    static byte[] deleteCharacterCheck(DeleteCharacterCheckResult result, long argument) {

        return nativeCheck(S_DELETE_CHARACTER_CHECK, result, argument);

    }



    /** Native C_BOOKMARK/C_DELETE_BOOKMARK producers: {@code cs}, name <= 27 bytes. */

    static String bookmarkName(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_BOOKMARK && expectedOpcode != C_DELETE_BOOKMARK) {

            throw new IllegalArgumentException("unsupported native bookmark opcode");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode) throw new IllegalArgumentException("wrong bookmark opcode");

        String name = in.s().trim();

        int nameBytes = name.getBytes(LEGACY_TEXT).length;

        if (nameBytes == 0 || nameBytes > 27 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native bookmark name");

        }

        return name;

    }



    /** Native S_ADD_BOOKMARK receiver sub_4AC920: {@code shhh}. */

    static byte[] addBookmark(String name, int x, int y, int mapId) {

        requireNativeString(name, "bookmark name");

        if (name.isBlank() || name.getBytes(LEGACY_TEXT).length > 27

                || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF

                || mapId < 0 || mapId > 0xFFFF) {

            throw new IllegalArgumentException("invalid native bookmark record");

        }

        return new Writer().c(S_ADD_BOOKMARK).s(name).h(x).h(y).h(mapId).bytes();

    }



    /** Native C_CONTROL_WEATHER producer sub_404630: {@code cc}. */

    static int weatherControl(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_CONTROL_WEATHER) {

            throw new IllegalArgumentException("not a native C_CONTROL_WEATHER packet");

        }

        int encoded = in.c();

        int target = encoded & 0xF0;

        int weather = encoded & 0x0F;

        if ((target != 0 && target != 0x10 && target != 0x20)

                || weather > 3 || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native weather control value");

        }

        return encoded;

    }



    /** Native S_WEATHER receiver sub_4ACD70: one encoded target/weather byte. */

    static byte[] weather(int encoded) {

        // Use the same validation as the native C producer envelope.

        weatherControl(new Writer().c(C_CONTROL_WEATHER).c(encoded).bytes());

        return new Writer().c(S_WEATHER).c(encoded).bytes();

    }



    /** Native S_TIME receiver sub_4A5E00 passes this dword to 32-bit localtime. */

    static byte[] worldTime(long gameSeconds) {

        if (gameSeconds < 0 || gameSeconds > 0x7FFF_FFFFL) {

            throw new IllegalArgumentException("world time exceeds native 32-bit time_t range");

        }

        return new Writer().c(S_TIME).d(gameSeconds).bytes();

    }



    /** Native S_ADD_INVENTORY receiver sub_4A4D90: {@code dcchcdcsc + raw(detail)}. */

    static byte[] addInventory(InventoryWireItem item) {

        Writer packet = new Writer().c(S_ADD_INVENTORY);

        appendInventoryRecord(packet, item);

        return packet.bytes();

    }



    /** Native S_ADD_INVENTORY_BATCH receiver sub_4B2650: count + repeated S_16 records. */

    static byte[] inventoryBatch(List<InventoryWireItem> items) {

        if (items == null || items.size() > 0xFF) {

            throw new IllegalArgumentException("native inventory batch exceeds one-byte count");

        }

        Writer packet = new Writer().c(S_ADD_INVENTORY_BATCH).c(items.size());

        for (InventoryWireItem item : items) appendInventoryRecord(packet, item);

        return packet.bytes();

    }



    static byte[] removeInventory(long objectId) {

        requireDwordId(objectId, "inventory object id");

        return new Writer().c(S_REMOVE_INVENTORY).d(objectId).bytes();

    }



    static byte[] changeInventoryDescription(long objectId, String description) {

        requireDwordId(objectId, "inventory object id");

        requireNativeString(description, "inventory description");

        return new Writer().c(S_CHANGE_ITEM_DESC).d(objectId).s(description).bytes();

    }



    static byte[] changeInventoryCharge(long objectId, int chargeCount) {

        requireDwordId(objectId, "inventory object id");

        requireByte(chargeCount, "inventory charge count");

        return new Writer().c(S_CHANGE_ITEM_USE).d(objectId).c(chargeCount).bytes();

    }



    /** Exact LiTo receiver {@code sub_4A6F00}: {@code c(type)+c(enabled)}. */

    static byte[] changeAbility(int abilityType, boolean enabled) {

        if (abilityType < 0 || abilityType >= 6) {

            throw new IllegalArgumentException("native ability type is outside 0..5");

        }

        return new Writer().c(S_CHANGE_ABILITY).c(abilityType).c(enabled ? 1 : 0).bytes();

    }



    /** Native S_CHANGE_LIGHT receiver sub_4A4110: {@code dc}. */

    static byte[] changeLight(long objectId, int lightRange) {

        requireDwordId(objectId, "world object id");

        requireByte(lightRange, "actor light range");

        return new Writer().c(S_CHANGE_LIGHT).d(objectId).c(lightRange).bytes();

    }



    /**

     * Native S_POISON receiver sub_4ACCB0: {@code dcc}. Type 0 restores the

     * ordinary palette, type 1 enables green poison and type 2 enables the

     * gray paralysis palette.

     */

    static byte[] poison(long objectId, int type) {

        requireDwordId(objectId, "world object id");

        if (type < 0 || type > 2) {

            throw new IllegalArgumentException("native poison type must be 0..2");

        }

        int green = type == 1 ? 1 : 0;

        int gray = type == 2 ? 1 : 0;

        return new Writer().c(S_POISON).d(objectId).c(green).c(gray).bytes();

    }



    /**

     * Native {@code S_PARALYSE (0x25)}: {@code c c(signed mode)}.  The writer

     * accepts only modes retained by the same-version server producer rather

     * than the receiver's broader {@code 0..25} implementation domain.

     */

    static byte[] paralysis(ParalysisMode mode) {

        Objects.requireNonNull(mode, "paralysis mode");

        return new Writer().c(S_PARALYSE).c(mode.wireValue()).bytes();

    }



    /**

     * Exact LiTo 3.81C S_POLYMORPH receiver sub_4A70B0: {@code dhc}.

     * The final byte is the actor's current weapon/walk action family; it is

     * not the hard-coded cross-version word value 29.

     */

    static byte[] polymorph(long objectId, int gfx, int walkAction) {

        return polymorph(objectId, gfx, walkAction, true);

    }



    /**

     * PET_RACE forms are movement-only actors.  In particular the native cow

     * form (gfx 945) has a recovered walk action but no combat action in the

     * retained 3.80 spr_action table, so it must not weaken the combat-facing

     * {@link #polymorph(long, int, int)} validation.

     */

    static byte[] petRacePolymorph(long objectId, int gfx, int walkAction) {

        return polymorph(objectId, gfx, walkAction, false);

    }



    /**

     * Native {@code S_ChangeShape(L1NpcInstance)} carrier for a static NPC.

     * Static quest actors use status/action zero and need no combat-animation

     * family; the selected receiver is still the exact {@code dhc} layout.

     */

    static byte[] classicStaticNpcShape(long objectId, int gfx, int status) {

        requireDwordId(objectId, "static NPC shape object id");

        if (gfx <= 0 || gfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid static NPC shape gfx");

        }

        requireByte(status, "static NPC shape status");

        return new Writer().c(S_POLYMORPH).d(objectId).h(gfx).c(status).bytes();

    }



    private static byte[] polymorph(long objectId, int gfx, int walkAction,

                                    boolean combatCapable) {

        requireDwordId(objectId, "polymorph world object id");

        // Native character gfx 0 is the male royal base sprite.  It is a

        // valid unsigned-word value and must be publishable when a royal

        // cancels polymorph or is resurrected.

        if (gfx < 0 || gfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid native polymorph gfx");

        }

        requireByte(walkAction, "polymorph walk action");

        if (combatCapable) {

            BattleAnimationCatalog.get().requireBattleCapableSprite(gfx, walkAction);

        } else {

            BattleAnimationCatalog.get().requireWalkAction(gfx, walkAction);

        }

        return new Writer().c(S_POLYMORPH).d(objectId).h(gfx).c(walkAction).bytes();

    }



    /** Native S_INVISIBLE receiver {@code sub_4ACE30}: {@code dc}. */

    static byte[] invisible(long objectId, int mode) {

        requireDwordId(objectId, "invisible world object id");

        if (mode != 0 && mode != 1) {

            throw new IllegalArgumentException("native invisible mode must be 0 or 1");

        }

        return new Writer().c(S_INVISIBLE).d(objectId).c(mode).bytes();

    }



    /** Native S_REMOVE_OBJECT receiver consumes one world-object dword. */

    static byte[] removeObject(long objectId) {

        requireDwordId(objectId, "removed world object id");

        return new Writer().c(S_REMOVE_OBJECT).d(objectId).bytes();

    }



    static byte[] changeInventoryCount(long objectId, long count) {

        requireDwordId(objectId, "inventory object id");

        if (count < 0 || count > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("inventory count is outside native dword range");

        }

        return new Writer().c(S_CHANGE_COUNT).d(objectId).d(count).bytes();

    }



    /**

     * Exact LiTo 3.81C S_CHANGE_ITEM_BLESS receiver {@code sub_4A59F0}: dc.

     * Same-family item colour states are 0..3, with 128 added for the four

     * sealed variants.

     */

    static byte[] changeInventoryBlessing(long objectId, int blessing) {

        requireDwordId(objectId, "inventory object id");

        if (!((blessing >= 0 && blessing <= 3)

                || (blessing >= 128 && blessing <= 131))) {

            throw new IllegalArgumentException(

                    "native inventory blessing must be 0..3 or sealed 128..131");

        }

        return new Writer().c(S_CHANGE_ITEM_BLESS).d(objectId).c(blessing).bytes();

    }



    /**

     * Exact LiTo 3.81C S_IDENTIFY_CODE receiver sub_4A4CB0:

     * {@code chhc + s[argumentCount]}.

     */

    static byte[] identifyCode(int itemDescriptionId, int messageId,

                               List<String> arguments) {

        if (itemDescriptionId < 0 || itemDescriptionId > 0xFFFF

                || messageId < 0 || messageId > 0xFFFF

                || arguments == null || arguments.isEmpty() || arguments.size() > 0xFF) {

            throw new IllegalArgumentException("invalid native identify description");

        }

        Writer packet = new Writer().c(S_IDENTIFY_CODE)

                .h(itemDescriptionId).h(messageId).c(arguments.size());

        for (String argument : arguments) {

            requireNativeString(argument, "identify argument");

            packet.s(argument);

        }

        return packet.bytes();

    }



    /**

     * Exact LiTo 3.81C S_KICK receiver {@code sub_4AFC40}: one reason byte.

     * Reason 0 follows the native default path (message 64), clears client

     * state and terminates the process.  The server still closes the socket

     * after flushing this packet so headless clients cannot remain attached.

     */

    static byte[] kick(int reason) {

        requireByte(reason, "native kick reason");

        return new Writer().c(S_KICK).c(reason).bytes();

    }



    /** Native S_CHANGE_ITEM_DESC_EX receiver sub_4A58B0: {@code dsdc + raw(detail)}. */

    static byte[] changeInventoryDescriptionExtended(long objectId, String description,

                                                     long count, byte[] detail) {

        requireDwordId(objectId, "inventory object id");

        requireNativeString(description, "extended inventory description");

        if (count <= 0 || count > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("extended inventory count is outside native dword range");

        }

        byte[] raw = detail == null ? new byte[0] : detail.clone();

        if (raw.length > 0xFF) throw new IllegalArgumentException("extended inventory detail is too long");

        return new Writer().c(S_CHANGE_ITEM_DESC_EX).d(objectId).s(description)

                .d(count).c(raw.length).raw(raw).bytes();

    }



    /** Exact LiTo 3.81C S_SHOW_MAP receiver sub_4AFFB0: {@code dc}. */

    static byte[] showMap(long inventoryObjectId, int page) {

        requireDwordId(inventoryObjectId, "map inventory object id");

        if (page < 1 || page > 18) {

            throw new IllegalArgumentException("native map page must be 1..18");

        }

        return new Writer().c(S_SHOW_MAP).d(inventoryObjectId).c(page).bytes();

    }



    /**

     * Exact gameplay-active {@code S_READ_MAIL} receiver {@code sub_4AF940}:

     * {@code dhhssSScs}. The two upper-case fields are terminated UTF-16LE;

     * the remaining strings are MS950. The first word is the open inventory

     * gfx (466 for the normal closed-letter 465 transition), not a catalog id.

     */

    static byte[] readMail(long inventoryObjectId, int openGfx, int letterCode,

                           String sender, String receiver, String subject,

                           String content, int templateId, String date) {

        requireDwordId(inventoryObjectId, "letter inventory object id");

        requireUnsignedWord(openGfx, "letter open inventory gfx");

        requireUnsignedWord(letterCode, "letter code");

        requireByte(templateId, "letter template id");

        requireNativeString(sender, "letter sender");

        requireNativeString(receiver, "letter receiver");

        int senderBytes = sender.getBytes(LEGACY_TEXT).length;

        int receiverBytes = receiver.getBytes(LEGACY_TEXT).length;

        if (sender.isEmpty() || receiver.isEmpty() || senderBytes > 27 || receiverBytes > 27

                || !LEGACY_TEXT.newEncoder().canEncode(sender)

                || !LEGACY_TEXT.newEncoder().canEncode(receiver)

                || containsControl(sender) || containsControl(receiver)

                || !wellFormedWideString(subject) || !wellFormedWideString(content)

                || subject.length() > 20 || content.length() > 256

                || containsUnsafeLetterControl(subject)

                || containsUnsafeLetterControl(content)

                || date == null || !date.matches("\\d{2}/\\d{2}/\\d{2}")) {

            throw new IllegalArgumentException("invalid native S_READ_MAIL fields");

        }

        return new Writer().c(S_READ_MAIL).d(inventoryObjectId)

                .h(openGfx).h(letterCode).s(sender).s(receiver)

                .S(subject).S(content).c(templateId).s(date).bytes();

    }



    /**

     * Exact LiTo 3.81C S_BREATH receiver sub_4B02D0: {@code dh}.  The retained

     * Doll_Water producer writes -1 as {@code 0xFFFF} for an unlimited status.

     */

    static byte[] underwaterBreath(long objectId, int durationSeconds) {

        requireDwordId(objectId, "underwater-breath object id");

        if (durationSeconds < -1 || durationSeconds > 0xFFFF) {

            throw new IllegalArgumentException("invalid native underwater-breath duration");

        }

        return new Writer().c(S_BREATH).d(objectId).h(durationSeconds).bytes();

    }



    /** Native S_WIELD receiver sub_4A5A30: {@code dc}. */

    static byte[] wield(long objectId, int walkAction) {

        requireDwordId(objectId, "world object id");

        requireByte(walkAction, "weapon walk action");

        return new Writer().c(S_WIELD).d(objectId).c(walkAction).bytes();

    }



    /**

     * Native S_SPEED receiver sub_4A7240: {@code dch(objectId,type,durationSeconds)}.

     * Type 0 is normal, 1 is haste and 2 is slow. Only the local actor uses

     * the duration to create its client timer; observers receive duration 0.

     */

    static byte[] speed(long objectId, SpeedMode mode, int durationSeconds) {

        requireDwordId(objectId, "speed-status object id");

        Objects.requireNonNull(mode, "movement-speed mode");

        if (durationSeconds < 0 || durationSeconds > 0xFFFF

                || (mode == SpeedMode.NORMAL && durationSeconds != 0)) {

            throw new IllegalArgumentException("invalid native speed status");

        }

        return new Writer().c(S_SPEED).d(objectId)

                .c(mode.wireValue()).h(durationSeconds).bytes();

    }



    /** Native S_BLIND receiver sub_4A6F40 parses one c(mode): 0 off, 1/2 on. */

    static byte[] blind(int mode) {

        if (mode < 0 || mode > 2) {

            throw new IllegalArgumentException("invalid native blindness mode");

        }

        return new Writer().c(S_BLIND).c(mode).bytes();

    }



    /** Native S_SOUND_EFFECT receiver sub_4AEE10: {@code ch(repeat,soundId)}. */

    static byte[] soundEffect(int repeat, int soundId) {

        if (repeat < 0 || repeat > 2 || soundId < 0 || soundId > 0x7FFF) {

            throw new IllegalArgumentException("invalid native sound effect");

        }

        return new Writer().c(S_SOUND_EFFECT).c(repeat).h(soundId).bytes();

    }



    /** Native S_DRUNKEN receiver sub_4A7310: {@code dc(objectId,mode)}. */

    static byte[] drunken(long objectId, DrunkenMode mode) {

        requireDwordId(objectId, "drunken-status object id");

        Objects.requireNonNull(mode, "drunken-status mode");

        return new Writer().c(S_DRUNKEN).d(objectId).c(mode.wireValue()).bytes();

    }



    /**

     * Exact S_WANTED_LOGIN receiver sub_4B0020 reads {@code ss} into two

     * 0x40-byte buffers and renders an account/source login notification.

     * The server supplies the source from the authenticated socket peer and

     * sends it only to gameplay-active administrator sessions.

     */

    static byte[] wantedLogin(String account, String source) {

        requireNativeString(account, "wanted-login account");

        requireNativeString(source, "wanted-login source");

        int accountBytes = account.getBytes(LEGACY_TEXT).length;

        int sourceBytes = source.getBytes(LEGACY_TEXT).length;

        if (accountBytes == 0 || accountBytes > 0x3F || sourceBytes == 0

                || sourceBytes > 0x3F || containsControl(account) || containsControl(source)) {

            throw new IllegalArgumentException("invalid native S_WANTED_LOGIN strings");

        }

        return new Writer().c(S_WANTED_LOGIN).s(account).s(source).bytes();

    }



    /**

     * Exact S_EVENT receiver {@code sub_4B07C0}, case 6, consumes one count

     * byte.  Zero selects native failure message 0x288; a positive count

     * formats the native success message with that count.

     */

    static byte[] smsResult(int count) {

        requireByte(count, "SMS result count");

        return new Writer().c(S_EVENT)

                .c(ServerEventType.SMS_RESULT.wireValue()).c(count).bytes();

    }



    /**

     * Historical {@code S_EMOTION}; exact receiver sub_4AF9F0 is the classic

     * {@code dch(objectId,braveMode,durationSeconds)} speed-state packet.

     */

    static byte[] braveSpeed(long objectId, BraveMode mode, int durationSeconds) {

        requireDwordId(objectId, "brave-speed object id");

        Objects.requireNonNull(mode, "brave-speed mode");

        if (durationSeconds < 0 || durationSeconds > 0xFFFF

                || (mode == BraveMode.NORMAL && durationSeconds != 0)) {

            throw new IllegalArgumentException("invalid native brave-speed status");

        }

        return new Writer().c(S_EMOTION).d(objectId)

                .c(mode.wireValue()).h(durationSeconds).bytes();

    }



    private static void appendInventoryRecord(Writer packet, InventoryWireItem item) {

        if (item == null) throw new IllegalArgumentException("native inventory item is required");

        requireDwordId(item.objectId(), "inventory object id");

        requireByte(item.useType(), "inventory use type");

        requireByte(item.chargeCount(), "inventory charge count");

        if (item.icon() < 0 || item.icon() > 0xFFFF || item.blessing() < 0 || item.blessing() >= 4

                || item.count() <= 0 || item.count() > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("invalid native inventory record fields");

        }

        requireByte(item.identified(), "inventory identified state");

        requireNativeString(item.name(), "inventory item name");

        byte[] detail = item.detail() == null ? new byte[0] : item.detail().clone();

        if (detail.length > 0xFF) throw new IllegalArgumentException("inventory item detail is too long");

        packet.d(item.objectId()).c(item.useType()).c(item.chargeCount()).h(item.icon())

                .c(item.blessing()).d(item.count()).c(item.identified()).s(item.name())

                .c(detail.length).raw(detail);

    }



    /**

     * Native C_BUY_SELL 0x28 result envelope.

     *

     * <p>The zero-row producer {@code sub_4DA270} writes {@code cdch}; the

     * same 3.81 family result handler identifies the two bytes written by the

     * final {@code h(0)} as an unsigned row count followed by a reserved zero

     * byte.  Non-empty rows are {@code dd}: list/order or storage object id,

     * then quantity.  Native list modes are bounded by the client's

     * {@code sub_4EBCB0} 0..16 UI dispatch table.</p>

     */

    static ClassicShopTransaction classicShopTransaction(byte[] payload) {

        if (payload == null || payload.length < 8) {

            throw new IllegalArgumentException("truncated native C_BUY_SELL packet");

        }

        Reader in = new Reader(payload);

        if (in.c() != C_BUY_SELL) {

            throw new IllegalArgumentException("not a native C_BUY_SELL packet");

        }

        long npcObjectId = in.d();

        int mode = in.c();

        int count = in.c();

        int reserved = in.c();

        if (npcObjectId == 0 || mode > 16 || reserved != 0

                || in.remaining() != count * 8) {

            throw new IllegalArgumentException("invalid native C_BUY_SELL layout");

        }

        List<ShopLine> lines = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {

            long itemId = in.d();

            long quantity = in.d();

            // Native buy-list order ids are zero-based, so row id 0 is valid.

            // Object-backed sell/retrieve rows are checked as non-zero by the

            // server-owned list context in ClientSession.

            if (quantity == 0 || quantity > Integer.MAX_VALUE) {

                throw new IllegalArgumentException("invalid native C_BUY_SELL row");

            }

            lines.add(new ShopLine(itemId, quantity));

        }

        return new ClassicShopTransaction(npcObjectId, mode, List.copyOf(lines));

    }



    /** Native S_BUY_LIST 0x2B receiver: {@code dh + (dhdsc+raw)*n}. */

    static byte[] classicBuyList(long npcObjectId, List<ClassicBuyListRow> rows) {

        return nativeBuyList(npcObjectId, rows, true);

    }



    /**

     * Preserved LiTo Light-Gem merchant list. The retained 71286/71287

     * producer deliberately omitted the classic trailing currency id 7.

     */

    static byte[] litoRentalBuyList(long npcObjectId, List<ClassicBuyListRow> rows) {

        return nativeBuyList(npcObjectId, rows, false);

    }



    private static byte[] nativeBuyList(

            long npcObjectId, List<ClassicBuyListRow> rows,

            boolean appendClassicCurrencyTail) {

        requireDwordId(npcObjectId, "classic merchant NPC object id");

        if (rows == null || rows.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_BUY_LIST row count");

        }

        Writer packet = new Writer().c(S_BUY_LIST).d(npcObjectId).h(rows.size());

        for (ClassicBuyListRow row : rows) {

            if (row == null) throw new IllegalArgumentException("null native S_BUY_LIST row");

            requireUnsignedDword(row.orderId(), "classic buy-list order id");

            requireUnsignedDword(row.unitPrice(), "classic buy-list unit price");

            requireNativeString(row.name(), "classic buy-list name");

            byte[] detail = row.detail();

            if (row.icon() < 0 || row.icon() > 0xFFFF || row.unitPrice() == 0

                    || detail.length > 0xFF || containsControl(row.name())) {

                throw new IllegalArgumentException("invalid native S_BUY_LIST row");

            }

            packet.d(row.orderId()).h(row.icon()).d(row.unitPrice())

                    .s(row.name()).c(detail.length).raw(detail);

        }

        // The matching classic server family appends 7 for Adena.  The

        // 3.81C handler deliberately ignores this compatibility tail after

        // consuming all rows, exactly as the original client did.

        if (appendClassicCurrencyTail) packet.h(7);

        return packet.bytes();

    }



    /** Native S_SELL_LIST 0x2C receiver: {@code dh + (dd)*n}. */

    static byte[] classicSellList(long npcObjectId, List<ClassicSellListRow> rows) {

        requireDwordId(npcObjectId, "classic merchant NPC object id");

        if (rows == null || rows.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_SELL_LIST row count");

        }

        Writer packet = new Writer().c(S_SELL_LIST).d(npcObjectId).h(rows.size());

        for (ClassicSellListRow row : rows) {

            if (row == null) throw new IllegalArgumentException("null native S_SELL_LIST row");

            requireDwordId(row.inventoryObjectId(), "classic sell-list inventory object id");

            requireUnsignedDword(row.unitPrice(), "classic sell-list unit price");

            if (row.unitPrice() == 0) {

                throw new IllegalArgumentException("invalid native S_SELL_LIST price");

            }

            packet.d(row.inventoryObjectId()).d(row.unitPrice());

        }

        return packet.bytes();

    }



    /**

     * Native S_RETRIEVE_LIST 0x31 receiver:

     * {@code dhc + (dchcdcs)*n}.  Modes 3, 5 and 9 are the personal, pledge

     * and elven retrieval controllers selected by the native close callback.

     */

    static byte[] retrieveList(long npcObjectId, int mode, List<RetrieveListRow> rows) {

        requireDwordId(npcObjectId, "warehouse NPC object id");

        if ((mode != 3 && mode != 5 && mode != 9) || rows == null

                || rows.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_RETRIEVE_LIST envelope");

        }

        Writer packet = new Writer().c(S_RETRIEVE_LIST).d(npcObjectId)

                .h(rows.size()).c(mode);

        for (RetrieveListRow row : rows) {

            if (row == null) throw new IllegalArgumentException("null native S_RETRIEVE_LIST row");

            requireDwordId(row.storageObjectId(), "warehouse storage object id");

            requireUnsignedDword(row.quantity(), "warehouse item quantity");

            requireNativeString(row.name(), "warehouse item name");

            if (row.useType() < 0 || row.useType() > 0xFF

                    || row.icon() < 0 || row.icon() > 0xFFFF

                    || row.blessing() < 0 || row.blessing() > 3

                    || row.quantity() == 0 || row.quantity() > Integer.MAX_VALUE

                    || (row.identified() != 0 && row.identified() != 1)

                    || containsControl(row.name())) {

                throw new IllegalArgumentException("invalid native S_RETRIEVE_LIST row");

            }

            packet.d(row.storageObjectId()).c(row.useType()).h(row.icon())

                    .c(row.blessing()).d(row.quantity()).c(row.identified()).s(row.name());

        }

        // Same-family 3.81 servers append the per-row Adena fee to personal

        // and pledge lists. sub_4AC9B0 stops after the declared rows, so this

        // compatibility dword is ignored safely by LiTo; elven storage uses

        // two pure mithril and has no dword tail.

        if (mode == 3 || mode == 5) packet.d(30);

        return packet.bytes();

    }



    private static void requireDwordId(long value, String label) {

        if (value <= 0 || value > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException(label + " is outside native dword range");

        }

    }



    private static void requireUnsignedDword(long value, String label) {

        if (value < 0 || value > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException(label + " is outside unsigned dword range");

        }

    }



    private static void requireSignedNonNegativeDword(long value, String label) {

        if (value < 0 || value > Integer.MAX_VALUE) {

            throw new IllegalArgumentException(label + " is outside non-negative signed dword range");

        }

    }



    private static void requireByte(int value, String label) {

        if (value < 0 || value > 0xFF) throw new IllegalArgumentException(label + " is outside byte range");

    }



    private static void requireUnsignedWord(int value, String label) {

        if (value < 0 || value > 0xFFFF) {

            throw new IllegalArgumentException(label + " is outside unsigned word range");

        }

    }



    private static void requireSignedWord(int value, String label) {

        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {

            throw new IllegalArgumentException(label + " is outside signed word range");

        }

    }



    private static void requireSignedNonNegativeWord(int value, String label) {

        if (value < 0 || value > Short.MAX_VALUE) {

            throw new IllegalArgumentException(label + " is outside non-negative signed word range");

        }

    }



    private static void requireSignedByte(int value, String label) {

        if (value < -128 || value > 127) {

            throw new IllegalArgumentException(label + " is outside signed byte range");

        }

    }



    private static void requireNonNegativeSignedByte(int value, String label) {

        if (value < 0 || value > 127) {

            throw new IllegalArgumentException(label + " is outside non-negative signed byte range");

        }

    }



    private static void requirePositiveSignedByte(int value, String label) {

        if (value <= 0 || value > 127) {

            throw new IllegalArgumentException(label + " is outside positive signed byte range");

        }

    }



    private static void requireBoundedLegacyString(String value, int minimumBytes,

                                                   int maximumBytes, String label) {

        if (value == null || value.chars().anyMatch(Character::isISOControl)) {

            throw new IllegalArgumentException(label + " is missing or contains control characters");

        }

        int encodedLength = value.getBytes(LEGACY_TEXT).length;

        if (encodedLength < minimumBytes || encodedLength > maximumBytes) {

            throw new IllegalArgumentException(label + " exceeds native storage");

        }

    }



    private static void requireNativeString(String value, String label) {

        if (value == null || value.getBytes(LEGACY_TEXT).length > 0xFF) {

            throw new IllegalArgumentException(label + " exceeds native 255-byte storage");

        }

    }



    /**

     * Native C_80 open/close producer.  {@code sub_42BC20} manually assembles

     * the type-0 frame instead of using the client's formatted packet writer.

     */

    static PersonalShopRequest personalShopRequest(byte[] payload) {

        if (payload == null || payload.length < 2) {

            throw new IllegalArgumentException("truncated native C_80 personal-shop request");

        }

        Reader in = new Reader(payload);

        if (in.c() != C_PERSONAL_SHOP) {

            throw new IllegalArgumentException("not a native C_80 personal-shop request");

        }

        int type = in.c();

        if (type == 1) {

            if (in.remaining() != 0) {

                throw new IllegalArgumentException("invalid native C_80 close layout");

            }

            return new PersonalShopRequest(1, List.of(), List.of(), new byte[0]);

        }

        if (type != 0 || in.remaining() < 4) {

            throw new IllegalArgumentException("unsupported native C_80 personal-shop type");

        }



        int sellCount = in.h();

        if (sellCount > 8 || in.remaining() < sellCount * 12 + 2) {

            throw new IllegalArgumentException("invalid native C_80 sell count");

        }

        List<PersonalShopOffer> sellOffers = new ArrayList<>(sellCount);

        for (int index = 0; index < sellCount; index++) {

            sellOffers.add(personalShopOffer(in));

        }



        int buyCount = in.h();

        if (buyCount > 8 || in.remaining() < buyCount * 12) {

            throw new IllegalArgumentException("invalid native C_80 buy count");

        }

        List<PersonalShopOffer> buyOffers = new ArrayList<>(buyCount);

        for (int index = 0; index < buyCount; index++) {

            buyOffers.add(personalShopOffer(in));

        }

        if (in.remaining() > 80) {

            throw new IllegalArgumentException("native C_80 announcement exceeds 80 bytes");

        }

        byte[] announcement = Arrays.copyOfRange(payload,

                payload.length - in.remaining(), payload.length);

        return new PersonalShopRequest(0, sellOffers, buyOffers, announcement);

    }



    private static PersonalShopOffer personalShopOffer(Reader in) {

        PersonalShopOffer offer = new PersonalShopOffer(in.d(), in.d(), in.d());

        if (offer.objectId() == 0 || offer.unitPrice() == 0 || offer.quantity() == 0

                || offer.unitPrice() > Integer.MAX_VALUE

                || offer.quantity() > Integer.MAX_VALUE) {

            throw new IllegalArgumentException("invalid native C_80 personal-shop row");

        }

        return offer;

    }



    /** Native C_81 producer in {@code sub_407C70}: {@code c c(type) d(target)}. */

    static PersonalShopQuery personalShopQuery(byte[] payload) {

        if (payload == null || payload.length != 6) {

            throw new IllegalArgumentException("invalid native C_81 personal-shop query size");

        }

        Reader in = new Reader(payload);

        if (in.c() != C_QUERY_PERSONAL_SHOP) {

            throw new IllegalArgumentException("not a native C_81 personal-shop query");

        }

        PersonalShopQuery query = new PersonalShopQuery(in.c(), in.d());

        if ((query.type() != 0 && query.type() != 1) || query.targetObjectId() == 0

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_81 personal-shop query");

        }

        return query;

    }



    /** Native S_7F type-0 list parsed by {@code sub_4B27E0}: {@code dh + cchddsc*}. */

    static byte[] personalShopSellList(long targetObjectId,

                                       List<PersonalShopSellRow> rows) {

        requireDwordId(targetObjectId, "personal-shop target object id");

        if (rows == null || rows.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_7F sell-row count");

        }

        Writer packet = new Writer().c(S_PERSONAL_SHOP_LIST).c(0)

                .d(targetObjectId).h(rows.size());

        for (PersonalShopSellRow row : rows) {

            if (row == null || row.index() < 0 || row.index() > 0xFF

                    || row.blessing() < 0 || row.blessing() > 0xFF

                    || row.icon() < 0 || row.icon() > 0xFFFF

                    || row.remainingQuantity() <= 0

                    || row.remainingQuantity() > Integer.MAX_VALUE

                    || row.unitPrice() <= 0 || row.unitPrice() > Integer.MAX_VALUE

                    || row.name() == null || row.name().getBytes(LEGACY_TEXT).length > 0xFF

                    || row.detail().length > 0xFF) {

                throw new IllegalArgumentException("invalid native S_7F sell row");

            }

            byte[] detail = row.detail();

            packet.c(row.index()).c(row.blessing()).h(row.icon())

                    .d(row.remainingQuantity()).d(row.unitPrice()).s(row.name())

                    .c(detail.length).raw(detail);

        }

        return packet.bytes();

    }



    /** Native S_7F type-1 list parsed by {@code sub_4B27E0}: {@code dh + cddd*}. */

    static byte[] personalShopBuyList(long targetObjectId,

                                      List<PersonalShopBuyRow> rows) {

        requireDwordId(targetObjectId, "personal-shop target object id");

        if (rows == null || rows.size() > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_7F buy-row count");

        }

        Writer packet = new Writer().c(S_PERSONAL_SHOP_LIST).c(1)

                .d(targetObjectId).h(rows.size());

        for (PersonalShopBuyRow row : rows) {

            if (row == null || row.index() < 0 || row.index() > 0xFF

                    || row.inventoryObjectId() <= 0

                    || row.inventoryObjectId() > 0xFFFF_FFFFL

                    || row.remainingQuantity() <= 0

                    || row.remainingQuantity() > Integer.MAX_VALUE

                    || row.unitPrice() <= 0 || row.unitPrice() > Integer.MAX_VALUE) {

                throw new IllegalArgumentException("invalid native S_7F buy row");

            }

            packet.c(row.index()).d(row.inventoryObjectId())

                    .d(row.remainingQuantity()).d(row.unitPrice());

        }

        return packet.bytes();

    }



    /**

     * Native C_DB producer {@code sub_4D9A20} and row serializer

     * {@code sub_4D9930}.

     *

     * <p>Ordinary Tournament UI phases use

     * {@code c c(operation) h(count) + count * [d(item_id) d(quantity)]}.

     * Native phase 4 inserts one context dword before the operation byte.

     * Both exact envelopes append eight opaque stack bytes.  Historical test

     * clients omitted this tail, so the parser accepts either layout while

     * exposing whether the exact native trailer was present.  Personal-shop

     * modes 13/14 use the contextual form with operations 0/1.</p>

     */

    static ShopTransaction shopTransaction(byte[] payload) {

        if (payload == null || payload.length < 4) {

            throw new IllegalArgumentException("truncated native C_DB shop transaction");

        }

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_BUY_SELL) {

            throw new IllegalArgumentException("not a native C_DB shop transaction");

        }



        boolean hasContext;

        if ((payload.length & 7) == 4) {

            hasContext = false;

        } else if ((payload.length & 7) == 0 && payload.length >= 8) {

            hasContext = true;

        } else {

            throw new IllegalArgumentException("invalid native C_DB packet size");

        }



        long contextId = hasContext ? in.d() : 0;

        int operation = in.c();

        if (operation != 0 && operation != 1) {

            throw new IllegalArgumentException("unsupported native C_DB shop operation");

        }

        int count = in.h();

        int rowBytes = count * 8;

        if (count > 510 || (in.remaining() != rowBytes && in.remaining() != rowBytes + 8)) {

            throw new IllegalArgumentException("invalid native C_DB row count");

        }



        List<ShopLine> lines = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {

            long itemId = in.d();

            long quantity = in.d();

            // Contextual personal-shop sell rows are byte indices widened to

            // dword by sub_4D9930, so index 0 is valid only for operation 0.

            if (quantity == 0 || (itemId == 0 && !(hasContext && operation == 0))) {

                throw new IllegalArgumentException("invalid native C_DB shop row");

            }

            lines.add(new ShopLine(itemId, quantity));

        }

        byte[] nativeTrailer = new byte[in.remaining()];

        for (int index = 0; index < nativeTrailer.length; index++) nativeTrailer[index] = (byte) in.c();

        if (hasContext && nativeTrailer.length == 8 && count > 509) {

            throw new IllegalArgumentException("native phase-4 C_DB exceeds its 0x1000-byte buffer");

        }

        return new ShopTransaction(hasContext, contextId, operation,

                List.copyOf(lines), nativeTrailer);

    }



    /**

     * Native C_E3 producer at {@code 0x00454530}: {@code ccccc}.

     *

     * <p>The first body byte is the literal 8.  The remaining three bytes are

     * copied from the native matchmaking UI object at offsets 0x0D, 0x0C and

     * 0x0E respectively.  {@code sub_453B00/sub_454670} prove the structural

     * domains and labels: channel restriction is one of the five native

     * character-source tiers 0..4 or the 0x7F "none" sentinel; match method is

     * 0..5; and participant restriction is 0..2 except that value 3 is exposed

     * when match method is 3.  Multilingual LTstring labels also prove that

     * this selector's order is racing, deathmatch, quest battle, siege war,

     * time attack and honor match, which differs from the independent room

     * wire-mode order.  Korean LTstring[285/286] proves that the last field

     * selects a per-force participant-count interval.  Its numeric lower and

     * upper cutoffs are supplied by the original server through warning codes

     * 2285/2286 and are not stored in this client, so they remain unavailable

     * and must not be invented.  Original server handling of the channel

     * restriction is likewise evidence-gated.</p>

     */

    static MatchMakingRequest matchMakingRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_ASK_MATCH_MAKING) {

            throw new IllegalArgumentException("not a native C_E3 matchmaking request");

        }

        MatchMakingRequest request = new MatchMakingRequest(in.c(), in.c(), in.c(), in.c());

        int channelRestriction = request.channelRestriction();

        if (request.fixedSelector() != 8

                || !(channelRestriction <= 4

                     || channelRestriction == MatchMakingRequest.NO_CHANNEL_RESTRICTION)

                || request.matchMethod() > 5

                || request.participantRestriction() > (request.matchMethod() == 3 ? 3 : 2)

                || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_E3 matchmaking layout");

        }

        return request;

    }



    /**

     * Native C_D7 has two exact producers. {@code sub_46AD10} emits only the

     * opcode when the ranking window is opened; {@code sub_45A830} emits

     * {@code cc} after selecting one of its five UI rows (0..4).

     *

     * <p>The selected Traditional-Chinese LTsurf resources pair the five

     * producers with monthly, overall, channel, classic-server and race

     * rankings.  The producer order is not the visual resource-number order:

     * selectors 2 and 3 are channel and classic-server respectively.</p>

     */

    static LitoRankingScope litoRankingRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_RANKING || in.remaining() > 1) {

            throw new IllegalArgumentException("invalid native C_D7 ranking layout");

        }

        int selector = in.remaining() == 0 ? -1 : in.c();

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing native C_D7 ranking bytes");

        }

        return LitoRankingScope.fromWire(selector);

    }



    /**

     * Exact {@code sub_46A690} producer: {@code cccs}. The two bytes are read

     * from zero-initialized {@code byte_64AFC1/byte_64AFC2}; each symbol has

     * only this one immediate reference in the recovered image and no writer.

     * The UI text is copied into a 0x20-byte stack buffer, including NUL.

     */

    static LitoFindUserRequest litoFindUserRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_FIND_USER) {

            throw new IllegalArgumentException("not a native C_DC find-user request");

        }

        int reserved1 = in.c();

        int reserved2 = in.c();

        byte[] encoded = in.zBytes();

        if (reserved1 != 0 || reserved2 != 0 || encoded.length > 0x1F) {

            throw new IllegalArgumentException("invalid native C_DC reserved/query boundary");

        }

        String query = new String(encoded, LEGACY_TEXT);

        if (!Arrays.equals(encoded, query.getBytes(LEGACY_TEXT))) {

            throw new IllegalArgumentException("native C_DC query is not valid MS950");

        }

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing C_DC find-user bytes=" + in.remaining());

        }

        return new LitoFindUserRequest(query);

    }



    /** Both native C_E1 producers serialize exactly {@code cc} with value 0 or 1. */

    static LitoObserverState litoObserverRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_SET_OBSERVER || in.remaining() != 1) {

            throw new IllegalArgumentException("invalid native C_E1 observer layout");

        }

        return LitoObserverState.fromWire(in.c());

    }



    /** Native death acknowledgment from sub_47C430/sub_4AACC0: opcode only. */

    static void litoConfirmDeathRequest(byte[] payload) {

        requireLitoOpcodeOnly(payload, C_LITO_CONFIRM_DEATH, "C_E0 death confirmation");

    }



    /** Immediate/deferred acknowledgements from the opcode-only S_E6 receiver. */

    static void litoNewsRequest(byte[] payload) {

        requireLitoOpcodeOnly(payload, C_LITO_READ_NEWS, "C_E2 news acknowledgement");

    }



    /** Native matchmaking cancel callback sub_4544F0 emits only C_E4. */

    static void litoQuitMatchMakingRequest(byte[] payload) {

        requireLitoOpcodeOnly(payload, C_LITO_QUIT_MATCH_MAKING,

                "C_E4 matchmaking cancellation");

    }



    /** Native {@code sub_4AAE70} consumes exactly one result byte after S_EB. */

    static byte[] litoAskMatchMakingResult(LitoMatchMakingResult result) {

        if (result == null) throw new IllegalArgumentException("matchmaking result is required");

        return new Writer().c(S_LITO_ASK_MATCH_MAKING_RESULT).c(result.wireValue()).bytes();

    }



    /** Native {@code sub_4AAF60} consumes exactly one result byte after S_EC. */

    static byte[] litoQuitMatchMakingResult(LitoQuitMatchMakingResult result) {

        if (result == null) throw new IllegalArgumentException("quit matchmaking result is required");

        return new Writer().c(S_LITO_QUIT_MATCH_MAKING_RESULT).c(result.wireValue()).bytes();

    }



    /** Native {@code sub_46EAE0} emits opcode-only C_E5 before C_QUERY_BUDDY. */

    static void litoBuddyGroupQuery(byte[] payload) {

        requireLitoOpcodeOnly(payload, C_LITO_QUERY_BUDDY_GROUP,

                "C_E5 buddy-group query");

    }



    /**

     * Native {@code sub_4AA380} ranking receiver.  The current version

     * handshake selects its {@code scddd} record branch (server version at

     * least {@code 0x7726}); the leading row count is a dword and is capped by

     * the client at 256 entries.  The byte selects a ranking/class icon from

     * {@code LTlist.ico}; the final signed dword is the row's rank movement.

     * Native code derives the displayed current rank solely from list order

     * and uses the movement to choose its neutral/up/down magnitude indicator.

     */

    static byte[] litoRanking(List<GameStateStore.Ranking> ranking) {

        if (ranking == null || ranking.size() > 0x100) {

            throw new IllegalArgumentException("native S_D9 ranking exceeds 256 entries");

        }

        Writer packet = new Writer().c(S_LITO_RANKING).d(ranking.size());

        for (int index = 0; index < ranking.size(); index++) {

            GameStateStore.Ranking row = ranking.get(index);

            if (row == null) throw new IllegalArgumentException("null native ranking row");

            requireNativeString(row.name(), "ranking name");

            requireUnsignedDword(row.wins(), "ranking wins");

            requireUnsignedDword(row.losses(), "ranking losses");

            if (row.classIcon() < 0 || row.classIcon() > 6) {

                throw new IllegalArgumentException("ranking class icon outside native class range");

            }

            if (row.name().isEmpty() || containsControl(row.name())) {

                throw new IllegalArgumentException("invalid native ranking name");

            }

            packet.s(row.name()).c(row.classIcon())

                    .d(row.wins()).d(row.losses()).d(row.rankMovement());

        }

        return packet.bytes();

    }



    /** Native {@code sub_4AAAB0} consumes one explicit observer-state byte. */

    static byte[] litoObserverResult(LitoObserverState state) {

        if (state == null) throw new IllegalArgumentException("observer state is required");

        return new Writer().c(S_LITO_SET_OBSERVER_RESULT).c(state.wireValue()).bytes();

    }



    /**

     * S_E6 is an opcode-only readiness trigger.  Its native receiver

     * {@code sub_48F500} never parses a body and answers with opcode-only C_E2

     * immediately, or from {@code loc_48F550} after local UI cleanup.

     */

    static byte[] litoNewsTrigger() {

        return new Writer().c(S_LITO_NEWS).bytes();

    }



    /** Native {@code sub_4AACF0} consumes no body before entering login UI. */

    static byte[] litoLoginOk() {

        return new Writer().c(S_LITO_LOGIN_OK).bytes();

    }



    /** Native {@code sub_4AC280} stores one dword kick result. */

    static byte[] litoKickStatus(LitoKickStatusResult result) {

        if (result == null) throw new IllegalArgumentException("kick status result is required");

        return new Writer().c(S_LITO_KICK_STATUS).d(result.wireValue()).bytes();

    }



    private static void requireLitoOpcodeOnly(byte[] payload, int opcode, String label) {

        Reader in = new Reader(payload);

        if (in.c() != opcode || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native " + label + " layout");

        }

    }



    /**

     * Both reachable C_C8 producers use {@code cs}. {@code sub_4AC4C0}

     * serializes the selected 0x48-byte S_SERVER_LIST record name after S_F5;

     * {@code sub_48FAA0} serializes the cached classic-source suffix. The

     * record loader stores the displayed source in a 0x40-byte field including

     * NUL, so the largest native producer value is 63 encoded bytes.

     */

    static LitoCharacterSourceRequest litoCharacterSourceRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_LIN_CHARACTER_INFO) {

            throw new IllegalArgumentException("not a native C_C8 character-source request");

        }

        byte[] encoded = in.zBytes();

        if (encoded.length > 0x3F) {

            throw new IllegalArgumentException("native C_C8 source exceeds 63 encoded bytes");

        }

        String sourceName = new String(encoded, LEGACY_TEXT);

        if (!Arrays.equals(encoded, sourceName.getBytes(LEGACY_TEXT))) {

            throw new IllegalArgumentException("native C_C8 source is not valid MS950");

        }

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing C_C8 character-source bytes=" + in.remaining());

        }

        return new LitoCharacterSourceRequest(sourceName);

    }



    /**

     * Exact LiTo 3.81C C_CA channel-selector union.

     *

     * <p>The recovered UI table at 0x00630B20 contains four channel controls

     * whose callback arguments are exactly 0..3. The same callback serializes

     * {@code cc}. The slash-command producer {@code sub_452C20} additionally

     * toggles value 99, while the native return/reset paths serialize value

     * 100 after C_CB. The parser rejects every other byte because the complete

     * producer census proves no such selection exists in this client image.</p>

     */

    static LitoChannelSelection litoEnterChannelRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_ENTER_CHANNEL || in.remaining() != 1) {

            throw new IllegalArgumentException("invalid native C_CA enter-channel layout");

        }

        return LitoChannelSelection.fromWire(in.c());

    }



    /** Native S_C9 result 0 consumes the complete {@code cccdh d} tail. */

    static byte[] litoEnterChannelSuccess(LitoChannelSelection selection) {

        if (selection == null

                || selection.target() == LitoChannelTarget.CHANNEL_SELECTION) {

            throw new IllegalArgumentException("success requires an active native channel");

        }

        return new Writer().c(S_LITO_ENTER_CHANNEL_RESULT)

                .c(0).c(selection.wireValue()).d(0).h(0).d(0).bytes();

    }



    /** Native S_C9 failure branches consume only result and value (3 bytes). */

    static byte[] litoEnterChannelFailure(

            LitoEnterChannelFailure result, LitoChannelSelection selection) {

        if (result == null || selection == null

                || selection.target() == LitoChannelTarget.CHANNEL_SELECTION

                || (result == LitoEnterChannelFailure.CHARACTER_LEVEL_RESTRICTED

                    && selection.target() != LitoChannelTarget.CHANNEL)) {

            throw new IllegalArgumentException("unsupported native S_C9 failure");

        }

        return new Writer().c(S_LITO_ENTER_CHANNEL_RESULT)

                .c(result.wireValue()).c(selection.wireValue()).bytes();

    }



    /** Canonical fail-closed reply when no valid C_CA selection can be parsed. */

    static byte[] litoMalformedEnterChannelFailure() {

        return new Writer().c(S_LITO_ENTER_CHANNEL_RESULT)

                .c(LitoEnterChannelFailure.CHANNEL_NOT_IN_SERVICE.wireValue())

                .c(LitoChannelSelection.CHANNEL_0.wireValue()).bytes();

    }



    /** Exact native S_CA {@code chh} channel/counter update. */

    static byte[] litoChannelInfo(

            LitoChannelSelection selection, int users, int capacity) {

        if (selection == null

                || selection.target() == LitoChannelTarget.CHANNEL_SELECTION

                || (users & ~0xFFFF) != 0

                || (capacity & ~0xFFFF) != 0) {

            throw new IllegalArgumentException("native S_CA field is out of range");

        }

        return new Writer().c(S_LITO_CHANNEL_INFO)

                .c(selection.wireValue()).h(users).h(capacity).bytes();

    }



    /**

     * Exact shared LiTo 3.81C C_CC/C_DD room-settings envelope.

     *

     * <p>{@code sub_461060} emits either {@code cscsh} or the legacy

     * {@code cscshccccc} form whose four options and count are all zero.

     * {@code sub_462620} emits {@code cscshcccccb}: event flags, event type,

     * winner count, a five-class mask, a count byte and exactly that many

     * non-zero original-server id bytes. Both

     * producer functions select C_CC versus C_DD before entering the same

     * writer tail, so the two opcodes have identical three wire shapes.</p>

     *

     * <p>The native dialogs reject an empty name and capacities outside

     * 2..200. The extended producer domains are flags 0..7, event type 0..2,

     * winners 1..127, class mask 0..31, and at most eight server ids. Attribute

     * bits are exactly 0x04/0x08/0x10/0x20, with 0x04 derived from whether

     * the password is non-empty.</p>

     */

    static LitoRoomSettingsRequest litoRoomSettingsRequest(byte[] payload, int expectedOpcode) {

        if (expectedOpcode != C_LITO_CREATE_ROOM && expectedOpcode != C_LITO_EDIT_ROOM) {

            throw new IllegalArgumentException("room settings opcode must be C_CC or C_DD");

        }

        Reader in = new Reader(payload);

        if (in.c() != expectedOpcode) {

            throw new IllegalArgumentException("unexpected native room settings opcode");

        }



        byte[] encodedName = in.zBytes();

        if (encodedName.length == 0 || encodedName.length > 0x59) {

            throw new IllegalArgumentException("native room name exceeds producer/receiver storage");

        }

        String name = new String(encodedName, LEGACY_TEXT);

        if (!Arrays.equals(encodedName, name.getBytes(LEGACY_TEXT)) || containsControl(name)) {

            throw new IllegalArgumentException("native room name is not valid MS950 text");

        }



        int attributes = in.c();

        if ((attributes & ~0x3C) != 0) {

            throw new IllegalArgumentException("native room attributes contain unknown bits");

        }



        byte[] encodedPassword = in.zBytes();

        if (encodedPassword.length > 4) {

            throw new IllegalArgumentException("native room password exceeds four encoded bytes");

        }

        String password = new String(encodedPassword, LEGACY_TEXT);

        if (!Arrays.equals(encodedPassword, password.getBytes(LEGACY_TEXT))

                || containsControl(password)) {

            throw new IllegalArgumentException("native room password is not valid MS950 text");

        }

        boolean passwordPresent = (attributes & GameHub.RoomSettings.PASSWORD_PRESENT) != 0;

        if (passwordPresent != !password.isEmpty()) {

            throw new IllegalArgumentException("native room password flag does not match its string");

        }



        int capacity = in.h();

        if (capacity < 2 || capacity > 0xC8) {

            throw new IllegalArgumentException("native room capacity is outside 2..200");

        }

        if (in.remaining() == 0) {

            return new LitoRoomSettingsRequest(name, attributes, password, capacity,

                    LitoRoomSettingsLayout.COMPACT, 0, 0, 0, 0, List.of());

        }

        if (in.remaining() < 5) {

            throw new IllegalArgumentException("partial native room options tail");

        }



        int option1 = in.c();

        int option2 = in.c();

        int option3 = in.c();

        int option4 = in.c();

        int count = in.c();

        if (count > 8 || in.remaining() != count) {

            throw new IllegalArgumentException("native room selection count does not match payload");

        }

        boolean legacyZeroTail = option1 == 0 && option2 == 0 && option3 == 0

                && option4 == 0 && count == 0;

        if (!legacyZeroTail && (option1 > 7 || option2 > 2

                || option3 < 1 || option3 > 0x7F || option4 > 0x1F)) {

            throw new IllegalArgumentException("native extended room option is outside producer domain");

        }

        List<Integer> selections = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {

            int selection = in.c();

            if (selection == 0) {

                throw new IllegalArgumentException("native room selection list cannot contain zero");

            }

            selections.add(selection);

        }

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("trailing native room settings bytes");

        }

        return new LitoRoomSettingsRequest(name, attributes, password, capacity,

                LitoRoomSettingsLayout.OPTIONS, option1, option2, option3, option4,

                selections);

    }



    /**

     * Exact LiTo 3.81C C_D1 room-control union.

     *

     * <p>The complete native producer census contains only two writers:

     * {@code sub_46B7D0} emits {@code ccc} with subtype 2 and direction 1/2,

     * while {@code sub_464AD0} emits {@code cch} with subtype 3 and a raw

     * unsigned map word. There is no opcode-only snapshot producer and no

     * other subtype in this client image.</p>

     */

    static LitoGameInfoRequest litoGameInfoRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_GAME_INFO) {

            throw new IllegalArgumentException("not a native C_D1 game-info request");

        }

        int subtype = in.c();

        if (subtype == 2) {

            if (in.remaining() != 1) {

                throw new IllegalArgumentException("invalid native C_D1 mode-step length");

            }

            return new LitoModeStepRequest(LitoModeStepDirection.fromWire(in.c()));

        }

        if (subtype == 3) {

            if (in.remaining() != 2) {

                throw new IllegalArgumentException("invalid native C_D1 map length");

            }

            return new LitoMapSelectionRequest(in.h());

        }

        throw new IllegalArgumentException("unsupported native C_D1 subtype=" + subtype);

    }



    /**

     * Both native C_C9 producers use {@code cs}: the character-list click in

     * {@code sub_4928C0} sends the selected row name, while

     * {@code sub_4A7550} resends the cached selected name during a native

     * transition.  Even an empty string therefore carries its NUL terminator;

     * opcode-only or trailing data is not an exact 3.81C envelope.

     */

    static String litoCharacterSelectRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_SELECT_CHAR) {

            throw new IllegalArgumentException("not a native C_C9 character-select request");

        }

        byte[] encoded = in.zBytes();

        if (encoded.length > 27) {

            throw new IllegalArgumentException(

                    "native C_C9 character name exceeds 27 encoded bytes");

        }

        String name = new String(encoded, LEGACY_TEXT);

        if (!Arrays.equals(encoded, name.getBytes(LEGACY_TEXT)) || containsControl(name)) {

            throw new IllegalArgumentException("native C_C9 character name is not valid MS950 text");

        }

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_C9 character-select layout");

        }

        return name;

    }



    /** Native {@code sub_4A76D0} consumes exactly {@code c(result)}. */

    static byte[] litoCharacterSelectResult(LitoCharacterSelectResult result) {

        if (result == null) {

            throw new IllegalArgumentException("missing native S_C8 character-select result");

        }

        return new Writer().c(S_LITO_SELECT_CHAR_RESULT).c(result.wireValue()).bytes();

    }



    /**

     * Native opcode-only world/object/UI reset handled by

     * {@code sub_4AA8B0 -> sub_46A260} in both client receive states.

     *

     * <p>This packet deliberately does not change the native UI phase.  A

     * caller must pair it with the proven phase-transition packet required by

     * its flow; the privileged {@code /exroom} path uses S_F0 immediately

     * afterward.</p>

     */

    static byte[] litoReset() {

        return new Writer().c(S_LITO_RESET).bytes();

    }



    /**

     * Native S_EVENT case 33 integrity request: {@code ccddd}.  The client

     * clamps the requested range against its {@code sub_4ED840} image anchor,

     * computes the seeded CRC in {@code sub_4B0680}, then answers with C_ADDR.

     */

    static byte[] addressChallenge(long challengeId, long startAddress, long length) {

        if (challengeId == 0 || startAddress == 0 || length == 0

                || challengeId > 0xFFFF_FFFFL || startAddress > 0xFFFF_FFFFL

                || length > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("invalid native address challenge");

        }

        return new Writer().c(S_EVENT)

                .c(ServerEventType.CLIENT_INTEGRITY_CHALLENGE.wireValue())

                .d(challengeId).d(startAddress).d(length).bytes();

    }



    /**

     * Native movement/teleport synchronization challenge handled by

     * {@code sub_4AFBA0}.  The receiver consumes no body and replies with the

     * opcode-only C_BLINK producer immediately, or after the local movement

     * action has completed.

     */

    static byte[] movementLock() {

        return new Writer().c(S_BLINK).bytes();

    }

    /** S_BLINK locks native action bit 8; mode 17 releases it without curing status effects. */
    static byte[] movementLockRelease() {
        return new Writer().c(S_PARALYSE).c(17).bytes();
    }




    /** Native C_BLINK producers sub_4AFBA0/sub_4E2150 are opcode-only. */

    static void movementUnlock(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_BLINK || in.remaining() != 0) {

            throw new IllegalArgumentException("invalid native C_BLINK layout");

        }

    }



    /**

     * Exact LiTo 3.81C S_MESSAGE receiver {@code sub_4A4190}: {@code cs}.

     *

     * <p>Type 9 is the same-family {@code S_SystemMessage} channel used by

     * {@code Game_Fight} for the per-viewer battle result line.  The native

     * receiver has a larger stack buffer, but server-originated text remains

     * inside the shared 255-byte legacy-string boundary.</p>

     */

    static byte[] systemMessage(int type, String message) {

        requireByte(type, "system-message type");

        requireNativeString(message, "system message");

        if (message.isEmpty() || containsControl(message)) {

            throw new IllegalArgumentException("invalid native system message");

        }

        return new Writer().c(S_MESSAGE).c(type).s(message).bytes();

    }



    /** Native S_MESSAGE_CODE receiver {@code sub_4A4670}: {@code hc+s*count}. */

    static byte[] messageCode(int code, String... arguments) {

        if (code < 0 || code > 0xFFFF || arguments == null || arguments.length > 0xFF) {

            throw new IllegalArgumentException("invalid native message-code envelope");

        }

        Writer packet = new Writer().c(S_MESSAGE_CODE).h(code).c(arguments.length);

        for (String argument : arguments) packet.s(argument == null ? "" : argument);

        return packet.bytes();

    }



    /**

     * Exact gameplay-active S_WARNING_CODE receiver {@code sub_4B01A0}:

     * {@code hc+s*count}.

     *

     * <p>The receiver sign-extends both the message-code word and argument

     * count, and its local argument-pointer envelope holds ten strings.  Keep

     * those native limits instead of accepting the wider wire primitives.</p>

     */

    static byte[] warningCode(int code, String... arguments) {

        if (code < 0 || code > Short.MAX_VALUE || arguments == null

                || arguments.length > 10) {

            throw new IllegalArgumentException("invalid native warning-code envelope");

        }

        Writer packet = new Writer().c(S_WARNING_CODE).h(code).c(arguments.length);

        for (String argument : arguments) {

            requireNativeString(argument, "warning-code argument");

            if (containsControl(argument)) {

                throw new IllegalArgumentException("warning-code argument contains control data");

            }

            packet.s(argument);

        }

        return packet.bytes();

    }



    /**

     * Native whole-battle lower-bound warning. {@code sub_4B01A0} subtracts

     * 2000 from message code 2110 and formats LTstring[110] with this count.

     */

    static byte[] litoBattleMinimumWarning(int minimumPlayers) {

        if (minimumPlayers <= 0) {

            throw new IllegalArgumentException("minimum battle players must be positive");

        }

        return warningCode(LITO_BATTLE_MINIMUM_PARTICIPANTS_WARNING,

                Integer.toString(minimumPlayers));

    }



    /**

     * Native whole-battle upper-bound warning. LTstring[186] identifies the

     * supplied value as the first disallowed participant count.

     */

    static byte[] litoBattleUpperBoundWarning(int firstDisallowedPlayers) {

        if (firstDisallowedPlayers <= 0) {

            throw new IllegalArgumentException(

                    "first disallowed battle player count must be positive");

        }

        return warningCode(LITO_BATTLE_UPPER_BOUND_WARNING,

                Integer.toString(firstDisallowedPlayers));

    }



    /**

     * Native LiTo participant-band lower-bound warning.  {@code sub_4B01A0}

     * subtracts 2000 from message code 2285, selects LTstring[285], and uses

     * this sole string argument for its {@code %d} placeholder.

     */

    static byte[] litoForceMinimumWarning(int minimumPlayersPerForce) {

        if (minimumPlayersPerForce <= 0) {

            throw new IllegalArgumentException("minimum players per force must be positive");

        }

        return warningCode(LITO_FORCE_MINIMUM_PARTICIPANTS_WARNING,

                Integer.toString(minimumPlayersPerForce));

    }



    /**

     * Native LiTo participant-band upper-bound warning.  LTstring[286] says a

     * force cannot proceed with the supplied count or more, so the argument is

     * the first disallowed count rather than the largest allowed count.

     */

    static byte[] litoForceUpperBoundWarning(int firstDisallowedPlayersPerForce) {

        if (firstDisallowedPlayersPerForce <= 0) {

            throw new IllegalArgumentException(

                    "first disallowed players per force must be positive");

        }

        return warningCode(LITO_FORCE_UPPER_BOUND_WARNING,

                Integer.toString(firstDisallowedPlayersPerForce));

    }



    /**

     * Native battle/observer movement envelope.

     *

     * <p>{@code sub_4AC5E0} parses four unsigned shorts and appends them as a

     * 16-byte rectangle. {@code sub_48B640} consumes that record as

     * {@code minX, minY, maxXExclusive, maxYExclusive} while admitting native

     * movement coordinates.</p>

     */

    static byte[] observerBox(int minX, int minY, int maxXExclusive, int maxYExclusive) {

        if (minX < 0 || minY < 0 || maxXExclusive > 0xFFFF || maxYExclusive > 0xFFFF

                || minX >= maxXExclusive || minY >= maxYExclusive) {

            throw new IllegalArgumentException("invalid native observer box");

        }

        return new Writer().c(S_LITO_OBSERVER_BOX)

                .h(minX).h(minY).h(maxXExclusive).h(maxYExclusive).bytes();

    }



    /**

     * Actor-shaped native S_PUT_OBJECT used by the Tournament battle world.

     *

     * <p>The exact receiver {@code sub_4A3990} parses

     * {@code hhdhccccdhsscdssccccshc}.  In particular, the byte after the

     * title is the classic actor status mask ({@code 4 = PC}, {@code 0 =

     * NPC/monster}); it is not a room participant type.  The four bytes after

     * the two clan strings are clan rank, HP ratio, third-speed/cake state

     * and level. Ordinary brave mode 1 is rebuilt by the separate exact

     * {@link #braveSpeed(long, int, int)} receiver.</p>

     */

    static byte[] battleActor(long objectId, int x, int y, int gfx, int walkAction,

                              int lightRange, int moveSpeed,

                              String name, String title, int status, int hpRatio,

                              int level) {

        return battleActor(objectId, x, y, gfx, walkAction, lightRange, moveSpeed,

                name, title, status, 0, "", hpRatio, level);

    }



    static byte[] battleActor(long objectId, int x, int y, int gfx, int walkAction,

                              int lightRange, int moveSpeed,

                              String name, String title, int status, long emblemId,

                              String clanName, int hpRatio, int level) {

        return battleActor(objectId, x, y, gfx, walkAction, lightRange, moveSpeed,

                name, title, status, emblemId, clanName, 0, hpRatio, level);

    }



    static byte[] battleActor(long objectId, int x, int y, int gfx, int walkAction,

                              int lightRange, int moveSpeed,

                              String name, String title, int status, long emblemId,

                              String clanName, int lawful, int hpRatio, int level) {

        return battleActorWithHeading(objectId, x, y, gfx, walkAction,

                0, lightRange, moveSpeed, name, title, status, emblemId,

                clanName, lawful, hpRatio, level);

    }



    /**

     * Exact actor body with the native heading byte kept explicit.

     *

     * <p>The common battle bootstrap historically faced every actor north,

     * so {@link #battleActor(long, int, int, int, int, int, int, String,

     * String, int, long, String, int, int, int)} retains heading zero.  World

     * screen-entry and rental-town peer rebuilds need the actor's committed

     * heading in the second byte after gfx; dropping it makes a newly visible

     * player face north until their next S_CHANGE_DIRECTION or movement.</p>

     */

    static byte[] battleActorWithHeading(

            long objectId, int x, int y, int gfx, int walkAction, int heading,

            int lightRange, int moveSpeed, String name, String title, int status,

            long emblemId, String clanName, int lawful, int hpRatio, int level) {

        requireDwordId(objectId, "world object id");

        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF

                || gfx < 0 || gfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid native battle actor coordinates or gfx");

        }

        requireByte(walkAction, "weapon walk action");

        if (heading < 0 || heading > 7) {

            throw new IllegalArgumentException("invalid native actor heading");

        }

        requireByte(lightRange, "actor light range");

        if (moveSpeed < BattleTimedStatus.NORMAL || moveSpeed > BattleTimedStatus.SLOW) {

            throw new IllegalArgumentException("invalid native actor move speed");

        }

        requireByte(status, "classic actor status");

        requireSignedWord(lawful, "actor lawful");

        requireByte(hpRatio, "actor HP ratio");

        requireByte(level, "actor level");

        requireNativeString(name, "actor name");

        requireNativeString(title, "actor title");

        requireNativeString(clanName, "actor clan name");

        if (emblemId < 0 || emblemId > 0xFFFF_FFFFL

                || (emblemId != 0 && clanName.isEmpty())) {

            throw new IllegalArgumentException("actor emblem id and clan name disagree");

        }

        if (name.isEmpty()) throw new IllegalArgumentException("native actor name is empty");

        return NativeActorTail.complete(new Writer().c(S_PUT_OBJECT)

                .h(x).h(y).d(objectId).h(gfx)

                .c(walkAction).c(heading).c(lightRange).c(moveSpeed)

                .d(0).h(lawful)

                .s(name).s(title)

                .c(status).d(emblemId).s(clanName).s("")

                .c(0).c(hpRatio).c(0).c(level)

                .s("").h(0).c(0).bytes(), status);

    }



    /** Exact retained S_NPCPack_Pet body, including EXP/lawful and owner-only HP. */

    static byte[] classicPet(BattleCompanion.Snapshot pet,

                             String masterName, boolean ownerView) {

        if (pet == null || pet.kind() != BattleCompanion.Kind.PET || !pet.active()

                || pet.collarObjectId() == 0 || pet.experience() < 0

                || pet.experience() > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("invalid classic pet actor");

        }

        requireDwordId(pet.objectId(), "classic pet object id");

        requireNativeString(pet.name(), "classic pet name");

        requireNativeString(masterName, "classic pet master name");

        requireSignedWord(pet.lawful(), "classic pet lawful");

        int hpRatio = ownerView ? healthRatio(pet.hp(), pet.maxHp()) : 0xFF;

        return new Writer().c(S_PUT_OBJECT)

                .h(pet.x()).h(pet.y()).d(pet.objectId()).h(pet.gfx())

                .c(BattleActionCodes.WALK).c(pet.heading()).c(0).c(pet.moveSpeed())

                .d(pet.experience()).h(pet.lawful())

                .s(pet.name()).s("")

                .c(0).d(0).s("").s(masterName)

                .c(0).c(hpRatio).c(0).c(0).c(0).c(0xFF).c(0xFF)

                .bytes();

    }



    /** Exact retained S_NPCPack_Doll actor body. */

    static byte[] magicDoll(NativeMagicDoll doll, String masterName) {

        if (doll == null) throw new IllegalArgumentException("magic doll is required");

        requireNativeString(masterName, "magic doll master name");

        if (masterName.isEmpty()) {

            throw new IllegalArgumentException("magic doll master name is empty");

        }

        SourceMagicDollType type = doll.type();

        return new Writer().c(S_PUT_OBJECT)

                .h(doll.x()).h(doll.y()).d(doll.objectId()).h(type.gfxId())

                .c(0).c(doll.heading()).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0).s(type.nameId()).s("")

                .c(0).d(0).s("").s(masterName)

                .c(0).c(0xFF).c(0).c(0).c(0).c(0xFF).c(0xFF)

                .bytes();

    }



    static byte[] magicDollIcon(int n) {
        if (n < 0 || n > 65535) {
            throw new IllegalArgumentException("invalid magic doll icon duration");
        }
        return new Writer().c(123).c(56).h(n).h(0).bytes();
    }



    static byte[] magicDollCustomIcon(int n, int n2, boolean bl) {
        if (n <= 0 || n > 65535 || n2 < 0 || n2 > 65535) {
            throw new IllegalArgumentException("invalid magic doll custom icon");
        }
        return new Writer().c(123).c(154).h(n2).h(n).h(0).h(bl ? 1 : 0).bytes();
    }



    /**

     * Exact fixed-merchant form of the classic S_NPCPack_M actor body.

     *

     * <p>The retained writer supplies template status 0, spawn heading,

     * template light, normal movement speed, EXP/lawful and the localized

     * {@code nameid}.  Merchants keep the hidden HP sentinel and level zero.

     * The final zero completes the selected LiTo receiver's trailing

     * {@code shc} parse after the retained writer's empty-string/FFFF tail.</p>

     */

    static byte[] classicFixedMerchant(

            ClassicWorldNpcCatalog.StaticNpc npc, int currentHeading) {

        if (npc == null || !npc.fixedMerchant()

                || currentHeading < 0 || currentHeading > 7) {

            throw new IllegalArgumentException("invalid fixed classic merchant");

        }

        return classicStaticNpc(npc, currentHeading);

    }



    /** Exact retained static-NPC actor body used by merchants and teleporters. */

    static byte[] classicStaticNpc(

            ClassicWorldNpcCatalog.StaticNpc npc, int currentHeading) {

        return classicStaticNpc(npc, currentHeading, npc == null ? 0 : npc.gfxId());

    }



    /** Static-NPC actor body with a retained runtime GFX mutation. */

    static byte[] classicStaticNpc(

            ClassicWorldNpcCatalog.StaticNpc npc, int currentHeading, int currentGfx) {

        if (npc == null || currentHeading < 0 || currentHeading > 7) {

            throw new IllegalArgumentException("invalid static classic NPC");

        }

        if (currentGfx <= 0 || currentGfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid static classic NPC gfx");

        }

        requireDwordId(npc.objectId(), "classic static NPC object id");

        requireNativeString(npc.nameId(), "classic static NPC name id");

        return new Writer().c(S_PUT_OBJECT)

                .h(npc.x()).h(npc.y()).d(npc.objectId()).h(currentGfx)

                .c(0).c(currentHeading).c(npc.lightSize()).c(BattleTimedStatus.NORMAL)

                .d(npc.experience()).h(npc.lawful())

                .s(npc.nameId()).s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0xFFFF).c(0).bytes();

    }



    /**

     * Native rental-town actor body using the LiTo-exclusive GFX namespace.

     * The body matches the retained static-NPC S_PUT_OBJECT layout; the

     * selected client dispatches GFX values at or above 20000 through its LT

     * resource array in {@code sub_442A30}.

     */

    static byte[] litoRentalTownNpc(NativeLitoRentalTownNpcCatalog.Npc npc) {

        if (npc == null) {

            throw new IllegalArgumentException("LiTo rental-town NPC is required");

        }

        requireDwordId(npc.objectId(), "LiTo rental-town NPC object id");

        requireNativeString(npc.name(), "LiTo rental-town NPC name");

        return new Writer().c(S_PUT_OBJECT)

                .h(npc.x()).h(npc.y()).d(npc.objectId()).h(npc.gfxId())

                .c(0).c(npc.heading()).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s(npc.name()).s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0xFFFF).c(0).bytes();

    }



    /** Exact retained {@code S_FollowerPack} actor body for Roi NPC 70957. */

    static byte[] classicWorldRoiFollower(

            ClassicWorldRoiFollowers.Snapshot follower) {

        if (follower == null) {

            throw new IllegalArgumentException("Roi follower is required");

        }

        requireDwordId(follower.objectId(), "Roi follower object id");

        requireNativeString(ClassicWorldRoiFollowers.NAME_ID, "Roi follower name id");

        return new Writer().c(S_PUT_OBJECT)

                .h(follower.x()).h(follower.y()).d(follower.objectId())

                .h(ClassicWorldRoiFollowers.GFX_ID)

                .c(0).c(follower.heading()).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s(ClassicWorldRoiFollowers.NAME_ID).s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0).c(0).c(0xFF).c(0xFF).bytes();

    }



    /** Exact retained {@code S_FollowerPack} body for Searcher and Endia. */

    static byte[] classicWorldRescueFollower(

            ClassicWorldRescueFollowers.Snapshot follower) {

        if (follower == null) {

            throw new IllegalArgumentException("rescue follower is required");

        }

        ClassicWorldRescueFollowers.Definition definition = follower.definition();

        requireDwordId(follower.objectId(), "rescue follower object id");

        requireNativeString(definition.nameId(), "rescue follower name id");

        return new Writer().c(S_PUT_OBJECT)

                .h(follower.x()).h(follower.y()).d(follower.objectId())

                .h(definition.gfxId())

                .c(0).c(follower.heading()).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s(definition.nameId()).s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0).c(0).c(0xFF).c(0xFF).bytes();

    }



    /** Exact retained {@code S_FollowerPack} body for Dspy NPC 80012. */

    static byte[] classicWorldDspyFollower(

            ClassicWorldDspyEscorts.Snapshot follower) {

        if (follower == null) {

            throw new IllegalArgumentException("Dspy follower is required");

        }

        requireDwordId(follower.objectId(), "Dspy follower object id");

        requireNativeString(

                ClassicWorldDspyCatalog.DSPY_NAME_ID, "Dspy follower name id");

        return new Writer().c(S_PUT_OBJECT)

                .h(follower.x()).h(follower.y()).d(follower.objectId())

                .h(ClassicWorldDspyCatalog.DSPY_GFX_ID)

                .c(0).c(follower.heading()).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s(ClassicWorldDspyCatalog.DSPY_NAME_ID).s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0).c(0).c(0xFF).c(0xFF).bytes();

    }



    /** Exact ordinary-monster form of retained {@code S_NPCPack}. */

    static byte[] classicWorldMonster(

            ClassicWorldMonsterPopulation.Snapshot monster) {

        return classicWorldMonster(monster, BattleTimedStatus.NORMAL);

    }



    static byte[] classicWorldMonster(

            ClassicWorldMonsterPopulation.Snapshot monster, int moveSpeed) {

        if (monster == null || !monster.present()) {

            throw new IllegalArgumentException("invalid classic monster actor");

        }

        ClassicWorldMonsterCatalog.MonsterTemplate template = monster.template();

        if (moveSpeed < BattleTimedStatus.NORMAL

                || moveSpeed > BattleTimedStatus.SLOW) {

            throw new IllegalArgumentException("invalid classic monster speed");

        }

        requireDwordId(monster.objectId(), "classic monster object id");

        requireNativeString(monster.nameId(), "classic monster name id");

        requireNativeString(monster.title(), "classic monster title");

        int actorStatus = template.doppel() && monster.gfxId() != 31

                ? 0x04 : monster.actorStatus();

        int objectStatus = template.doppel() && monster.npcId() != 81069

                ? 0x04 : 0;

        return new Writer().c(S_PUT_OBJECT)

                .h(monster.x()).h(monster.y()).d(monster.objectId())

                .h(monster.gfxId()).c(actorStatus).c(monster.heading())

                .c(template.lightSize()).c(moveSpeed)

                .d(template.experience()).h(monster.lawful())

                .s(monster.nameId()).s(monster.title())

                .c(objectStatus).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(template.level())

                .s("").h(0xFFFF).c(0).bytes();

    }



    /** Retained S_NPCPack_F/S_NPCPack actor for a live castle-war object. */

    static byte[] classicCastleWarObject(

            ClassicWorldCastleWarField.WarObject object) {

        if (object == null) {

            throw new IllegalArgumentException("castle-war object is required");

        }

        requireDwordId(object.objectId(), "castle-war object id");

        requireNativeString(object.nameId(), "castle-war object name id");

        return new Writer().c(S_PUT_OBJECT)

                .h(object.x()).h(object.y()).d(object.objectId())

                .h(object.gfxId()).c(0).c(object.heading())

                .c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s(object.nameId()).s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(object.level())

                .s("").h(0xFFFF).c(0).bytes();

    }



    /**

     * NPC-shaped S_PUT_OBJECT for classic cooking effect NPC 81170. The

     * retained NPC template has an empty name-id, GFX 5943 and no actor

     * action; HP remains the classic non-player sentinel 0xFF.

     */

    static byte[] cookingFire(BattleCookingFires.Fire fire) {

        if (fire == null) throw new IllegalArgumentException("cooking fire is null");

        return new Writer().c(S_PUT_OBJECT)

                .h(fire.x()).h(fire.y()).d(fire.objectId()).h(BattleCookingFires.GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /**

     * NPC-shaped S_PUT_OBJECT for retained effect NPC 80149.  Unlike the two

     * skill effects attached to nearby actors, GFX6706 is a real temporary

     * world object and must later be paired with S_REMOVE_OBJECT.

     */

    static byte[] cubeIgnition(BattleCubeIgnitionEffects.Cube cube) {

        byte[] nativePacket = NativeSourceCubeTemplate.packet(cube);

        if (nativePacket != null) return nativePacket;


        if (cube == null) throw new IllegalArgumentException(

                "Cube: Ignition object is null");

        return new Writer().c(S_PUT_OBJECT)

                .h(cube.x()).h(cube.y()).d(cube.objectId())

                .h(BattleCubeIgnitionEffects.CUBE_GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /** NPC-shaped S_PUT_OBJECT for retained effect NPC 80150 / GFX6712. */

    static byte[] cubeQuake(BattleCubeQuakeEffects.Cube cube) {

        byte[] nativePacket = NativeSourceCubeTemplate.packet(cube);

        if (nativePacket != null) return nativePacket;



        if (cube == null) throw new IllegalArgumentException(

                "Cube: Quake object is null");

        return new Writer().c(S_PUT_OBJECT)

                .h(cube.x()).h(cube.y()).d(cube.objectId())

                .h(BattleCubeQuakeEffects.CUBE_GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0).s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /** NPC-shaped S_PUT_OBJECT for retained effect NPC 80151 / GFX6718. */

    static byte[] cubeShock(BattleCubeShockEffects.Cube cube) {

        byte[] nativePacket = NativeSourceCubeTemplate.packet(cube);

        if (nativePacket != null) return nativePacket;



        if (cube == null) throw new IllegalArgumentException(

                "Cube: Shock object is null");

        return new Writer().c(S_PUT_OBJECT)

                .h(cube.x()).h(cube.y()).d(cube.objectId())

                .h(BattleCubeShockEffects.CUBE_GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0).s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /** NPC-shaped S_PUT_OBJECT for retained effect NPC 80152 / GFX6724. */

    static byte[] cubeHarmony(BattleCubeHarmonyEffects.Cube cube) {

        byte[] nativePacket = NativeSourceCubeTemplate.packet(cube);

        if (nativePacket != null) return nativePacket;



        if (cube == null) throw new IllegalArgumentException(

                "Cube: Harmony object is null");

        return new Writer().c(S_PUT_OBJECT)

                .h(cube.x()).h(cube.y()).d(cube.objectId())

                .h(BattleCubeHarmonyEffects.CUBE_GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0).s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /**

     * NPC-shaped S_PUT_OBJECT for the retained Shock Stun effect NPC 81162.

     * Its selected template is nameless GFX 4183 and exists only for the

     * 2-4 second stun lease; S_REMOVE_OBJECT removes it at expiry/refresh.

     */

    static byte[] shockStunEffect(BattleShockStunEffects.Effect effect) {

        if (effect == null) throw new IllegalArgumentException("Shock Stun effect is null");

        return new Writer().c(S_PUT_OBJECT)

                .h(effect.x()).h(effect.y()).d(effect.objectId())

                .h(BattleShockStunEffects.GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /** NPC-shaped S_PUT_OBJECT for skill208 effect NPC86123/GFX6552. */

    static byte[] boneBreakEffect(BattleBoneBreakEffects.Effect effect) {

        if (effect == null) {

            throw new IllegalArgumentException("Bone Break effect is null");

        }

        return new Writer().c(S_PUT_OBJECT)

                .h(effect.x()).h(effect.y()).d(effect.objectId())

                .h(BattleBoneBreakEffects.GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /** NPC-shaped S_PUT_OBJECT for retained freeze effect NPC 81168/GFX 176. */

    static byte[] freezeEffect(BattleFreezeEffects.Effect effect) {

        if (effect == null) throw new IllegalArgumentException("freeze effect is null");

        return new Writer().c(S_PUT_OBJECT)

                .h(effect.x()).h(effect.y()).d(effect.objectId())

                .h(BattleFreezeEffects.GFX)

                .c(0).c(0).c(0).c(BattleTimedStatus.NORMAL)

                .d(0).h(0)

                .s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0).c(0).bytes();

    }



    /**

     * Guardian-tower specialization of the exact native S_PUT_OBJECT actor

     * body.  The byte normally carrying an actor's walk/weapon stance carries

     * the tower's persistent healthy/crack/death action, matching the classic

     * NPC pack contract.  Subsequent transitions use ordinary S_ACTION.

     */

    static byte[] guardianTower(BattleGuardianTower.Snapshot tower) {

        if (tower == null || tower.objectId() == 0 || tower.force() < 0

                || tower.force() > 1 || tower.destroyed() != (tower.hp() == 0)

                || tower.maxHp() != BattleGuardianTower.MAX_HP

                || tower.objectiveKind() == null

                || !BattleGuardianTower.isStateAction(tower.action())) {

            throw new IllegalArgumentException("invalid native guardian tower");

        }

        return battleActor(tower.objectId(), tower.x(), tower.y(), tower.gfx(),

                tower.action(), 0, BattleTimedStatus.NORMAL,

                tower.name(), "", 0, 0, "", 0,

                healthRatio(tower.hp(), tower.maxHp()), 1);

    }



    /** Native common S_PUT_OBJECT body for one authoritative Quest monster. */

    static byte[] questMonster(BattleQuestEncounter.Snapshot monster) {

        if (monster == null || monster.objectId() == 0

                || monster.force() < 0 || monster.force() > 1

                || monster.roomNumber() < 1 || monster.roomNumber() > 4

                || !monster.alive() || monster.hp() <= 0

                || monster.hp() > monster.maxHp()) {

            throw new IllegalArgumentException("invalid native Quest monster");

        }

        BattleAnimationCatalog.get().requireBattleCapableSprite(

                monster.gfx(), BattleActionCodes.WALK);

        return battleActor(monster.objectId(), monster.x(), monster.y(),

                monster.gfx(), BattleActionCodes.WALK, 0,

                BattleTimedStatus.NORMAL, monster.name(), "", 0,

                healthRatio(monster.hp(), monster.maxHp()), monster.level());

    }



    /** Native common S_PUT_OBJECT body for one cooperative Time Attack actor. */

    static byte[] timeAttackActor(BattleTimeAttackEncounter.Snapshot actor) {

        if (actor == null || actor.objectId() == 0 || !actor.alive()

                || actor.hp() <= 0 || actor.hp() > actor.maxHp()) {

            throw new IllegalArgumentException("invalid native Time Attack actor");

        }

        if (actor.finalBoss()) {

            BattleAnimationCatalog.get().requireBattleCapableSprite(

                    actor.gfx(), BattleActionCodes.WALK);

        } else {

            // The documented Sea Dancer supports walk plus actions 18/19,

            // but not ordinary melee action 1. It remains a passive fragment.

            BattleAnimationCatalog.get().requireWalkAction(

                    actor.gfx(), BattleActionCodes.WALK);

        }

        return battleActor(actor.objectId(), actor.x(), actor.y(),

                actor.gfx(), BattleActionCodes.WALK, 0,

                BattleTimedStatus.NORMAL, actor.name(), "", 0,

                healthRatio(actor.hp(), actor.maxHp()), actor.level());

    }



    /**

     * Door-shaped native S_PUT_OBJECT for the authoritative battle door.

     *

     * <p>Classic {@code S_DoorPack} supplies gfx 2083 with action 29 while

     * closed and action 28 while open. The exact LiTo receiver still consumes

     * the common {@code hhdhccccdhsscdssccccshc} body; an empty name is

     * intentional for this non-character sprite and the hidden HP sentinel is

     * {@code 0xFF}.</p>

     */

    static byte[] battleDoor(long objectId, int x, int y, int action) {

        return classicCastleDoor(objectId, x, y, BattleDoor.GFX, action);

    }



    /** Native retained {@code S_DoorPack} with the database-selected door gfx. */

    static byte[] classicCastleDoor(

            long objectId, int x, int y, int gfxId, int action) {

        requireDwordId(objectId, "door world object id");

        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF

                || gfxId <= 0 || gfxId > 0xFFFF

                || (action != BattleDoor.ACTION_OPEN && action != BattleDoor.ACTION_CLOSED)) {

            throw new IllegalArgumentException("invalid native castle door fields");

        }

        return new Writer().c(S_PUT_OBJECT)

                .h(x).h(y).d(objectId).h(gfxId)

                .c(action).c(0).c(0).c(0)

                .d(1).h(0)

                .s("").s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0xFFFF).c(0).bytes();

    }



    /**

     * Native {@code S_CHANGE_ATTR} receiver {@code sub_4A5E70}: {@code hhcc}.

     * The matching 3.80/3.81 {@code S_Door} writer sends the entrance tile,

     * door direction and passability immediately after the door action.

     */

    static byte[] doorAttribute(int x, int y, int direction, int passable) {

        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF

                || (direction != 0 && direction != 1)

                || (passable != BattleDoor.PASSABLE

                    && passable != BattleDoor.NOT_PASSABLE)) {

            throw new IllegalArgumentException("invalid native door attribute fields");

        }

        return new Writer().c(S_CHANGE_ATTR).h(x).h(y).c(direction).c(passable).bytes();

    }



    /** Exact gameplay action receiver envelope: {@code dc}. */

    static byte[] objectAction(long objectId, int action) {

        requireDwordId(objectId, "action world object id");

        requireByte(action, "object action");

        return new Writer().c(S_ACTION).d(objectId).c(action).bytes();

    }



    /**

     * Native shop actor action consumed by {@code sub_4A5A90} and

     * {@code sub_4E9140}.  Action 70 leaves the remaining raw, NUL/0xFF-delimited

     * announcement in the packet; the receiver copies at most 80 bytes.

     */

    static byte[] personalShopAction(long objectId, byte[] announcement) {

        requireDwordId(objectId, "personal-shop actor object id");

        byte[] raw = announcement == null ? new byte[0] : announcement.clone();

        if (raw.length > 80) {

            throw new IllegalArgumentException("personal-shop announcement exceeds 80 bytes");

        }

        return new Writer().c(S_ACTION).d(objectId).c(70).raw(raw).bytes();

    }



    /** Native S_PLEDGE receiver {@code sub_4AFDC0}: {@code dssd}. */

    static byte[] pledgeMetadata(long objectId, String clanName, String title, long emblemId) {

        requireDwordId(objectId, "pledge object id");

        requireNativeString(clanName, "pledge clan name");

        requireNativeString(title, "pledge title");

        if (emblemId < 0 || emblemId > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("pledge emblem id is outside native dword range");

        }

        return new Writer().c(S_PLEDGE).d(objectId).s(clanName).s(title).d(emblemId).bytes();

    }



    /**

     * Item-shaped native S_PUT_OBJECT used by the battle ground table.

     *

     * <p>The exact client still enters {@code sub_4A3990} and parses the same

     * {@code hhdhccccdhsscdssccccshc} body as an actor.  A ground gfx whose

     * sprite metadata has class 9 makes {@code sub_4E89F0} expose the object

     * to the three native C_GET producers.  The classic S_NPCPack_Item source

     * supplies quantity in the first dword, an empty title/clan domain and

     * the {@code 0xFF} hidden HP sentinel.</p>

     */

    static byte[] groundItem(long objectId, int x, int y, int groundGfx,

                             long quantity, String displayName) {

        requireDwordId(objectId, "ground world object id");

        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF

                || groundGfx <= 0 || groundGfx > 0xFFFF

                || quantity <= 0 || quantity > 2_000_000_000L) {

            throw new IllegalArgumentException("invalid native ground item fields");

        }

        requireNativeString(displayName, "ground item display name");

        if (displayName.isEmpty() || displayName.getBytes(LEGACY_TEXT).length > 0x7F) {

            throw new IllegalArgumentException("invalid native ground item name");

        }

        return new Writer().c(S_PUT_OBJECT)

                .h(x).h(y).d(objectId).h(groundGfx)

                .c(0).c(0).c(0).c(0)

                .d(quantity).h(0)

                .s(displayName).s("")

                .c(0).d(0).s("").s("")

                .c(0).c(0xFF).c(0).c(0)

                .s("").h(0xFFFF).c(0).bytes();

    }



    /**

     * Native overhead HP meter consumed by {@code sub_4A3870} as

     * {@code d(objectId), c(ratio)}.

     *

     * <p>The exact receiver clamps values above 100 and stores the result in

     * world-object byte {@code +0x51}. Use only 0..100 on the wire; the

     * {@code 0xFF} hidden sentinel belongs to the S_PUT_OBJECT actor tail and

     * cannot be sent through this receiver because it would become 100.</p>

     */

    static byte[] hitRatio(long objectId, int currentHp, int maximumHp) {

        requireDwordId(objectId, "HP-meter object id");

        return new Writer().c(S_HIT_RATIO).d(objectId)

                .c(healthRatio(currentHp, maximumHp)).bytes();

    }



    static int healthRatio(int currentHp, int maximumHp) {

        if (maximumHp <= 0 || currentHp < 0 || currentHp > maximumHp) {

            throw new IllegalArgumentException("invalid native HP-meter values");

        }

        return (int) ((100L * currentHp) / maximumHp);

    }



    /**

     * Native location effect consumed by {@code sub_4ADF60} as

     * {@code h(x), h(y), h(gfx)}. IDA mistakes the four format bytes

     * {@code 68 68 68 00} for a pointer named {@code off_6344E8}; those bytes

     * are the literal parser format string {@code "hhh"}.

     */

    static byte[] effectLocation(int x, int y, int gfx) {

        if (x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF

                || gfx <= 0 || gfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid native location effect");

        }

        return new Writer().c(S_EFFECT_LOC).h(x).h(y).h(gfx).bytes();

    }



    /**

     * Native object-bound effect consumed by {@code sub_4ADDA0} as

     * {@code d(objectId), h(gfx)}. The receiver resolves the world object

     * before attaching the effect; it is therefore not interchangeable with

     * {@link #effectLocation(int, int, int)}.

     */

    static byte[] effectObject(long objectId, int gfx) {

        requireDwordId(objectId, "effect target object id");

        if (gfx <= 0 || gfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid native object effect gfx");

        }

        // LiTo embeds 5,894 base sprites (+285 LT sprites). Modern server heal

        // IDs 11722..11725 index past that native table in sub_44EDE0.

        // Embedded list.spr entries 829..831 alias sprite 744 with distinct sounds.

        int nativeGfx = switch (gfx) {

            case 11722 -> 744; // heal

            case 11723 -> 829; // extra heal

            case 11724 -> 830; // greater heal

            case 11725 -> 831; // full heal

            default -> gfx;

        };

        return new Writer().c(S_EFFECT).d(objectId).h(nativeGfx).bytes();

    }



    /**

     * Native resurrection transition consumed by {@code sub_4A6F70} as

     * {@code d(objectId), c(action), d(sourceObjectId), h(gfx)}.

     *

     * <p>The receiver clears the corpse/death state, resets the local death

     * controller when the target is the local player, and restores the

     * supplied action/gfx. The source id stays on the exact wire even though

     * this client receiver does not otherwise use it.</p>

     */

    static byte[] resurrect(long objectId, int action, long sourceObjectId, int gfx) {

        requireDwordId(objectId, "resurrection target object id");

        requireByte(action, "resurrection action");

        requireDwordId(sourceObjectId, "resurrection source object id");

        if (gfx < 0 || gfx > 0xFFFF) {

            throw new IllegalArgumentException("invalid native resurrection gfx");

        }

        return new Writer().c(S_RESURRECT).d(objectId).c(action)

                .d(sourceObjectId).h(gfx).bytes();

    }



    /**

     * Exact player-resurrection confirmation consumed by native

     * {@code sub_4A6890}. Question 321 is one of the compact paths: the packet

     * ends after the leading question word and has no retained-server tail.

     */

    static byte[] resurrectionAsk() {

        return new Writer().c(S_ASK).h(321).bytes();

    }



    /**

     * Exact Greater Resurrection confirmation consumed by native

     * {@code sub_4A6890}. Unlike special-case question 321, question 322 falls

     * through the generic {@code hs} branch, so an empty string terminator is

     * a required fourth byte rather than optional padding.

     */

    static byte[] greaterResurrectionAsk() {

        return new Writer().c(S_ASK).h(322).s("").bytes();

    }



    /**

     * Native Tournament minimap player marker.

     *

     * <p>{@code sub_4AAD10} parses {@code dhhhcc} and stores a record keyed by

     * object id. The original local C_MOVE producer writes reserved=0,

     * category=1 and relation=0. {@code sub_470390} proves the second and third

     * shorts are x/y, category 1 is the ordinary player path, and non-zero

     * relation selects the allied colour; the local object id is independently

     * overridden to green.</p>

     */

    static byte[] minimapPlayer(long objectId, int x, int y, boolean allied) {

        if (objectId == 0 || x < 0 || x > 0xFFFF || y < 0 || y > 0xFFFF) {

            throw new IllegalArgumentException("invalid native minimap player marker");

        }

        return new Writer().c(S_LITO_MINIMAP).d(objectId)

                .h(0).h(x).h(y).c(1).c(allied ? 1 : 0).bytes();

    }



    /**

     * Native VS-server availability table consumed by {@code sub_4AC520}.

     *

     * <p>The receiver reads one byte of count followed by that many dwords

     * into the fixed ten-dword {@code dword_64AD2C[]} table.  Its sole

     * consumer treats {@code 0xFFFFFFFF} as unavailable and every other value

     * identically as available.  Production therefore exposes only those two

     * proven semantic classes instead of arbitrary dwords.</p>

     */

    static byte[] vsServerStatus(LitoVsServerAvailability... statuses) {

        if (statuses == null || statuses.length > MAX_LITO_VS_SERVERS) {

            throw new IllegalArgumentException("S_LITO_VS_SERVER_STATUS count exceeds native table");

        }

        Writer packet = new Writer().c(S_LITO_VS_SERVER_STATUS).c(statuses.length);

        for (LitoVsServerAvailability status : statuses) {

            if (status == null) {

                throw new IllegalArgumentException("null native VS-server status");

            }

            packet.d(status.wireValue());

        }

        return packet.bytes();

    }



    /**

     * Native {@code S_LITO_SERVER_LIST} receiver {@code sub_4AC7D0}.

     *

     * <p>The wire begins with two dword counts and then uses {@code sdhdd}

     * records for both groups.  They are not interchangeable: the first loop

     * passes records to {@code sub_4756E0}, whose click path reads room id at

     * {@code +0x10C}; the second passes records to {@code sub_475960}, whose

     * click path reads user count/capacity at {@code +0x110/+0x114} and may

     * reconnect to the advertised endpoint.</p>

     */

    static byte[] litoServerDirectory(

            List<LitoRoomDirectoryEntry> rooms,

            List<LitoVsServerDirectoryEntry> servers) {

        if (rooms == null || servers == null || rooms.size() > 0x7F

                || servers.size() > MAX_LITO_VS_SERVERS) {

            throw new IllegalArgumentException("invalid native S_LITO_SERVER_LIST counts");

        }

        Writer packet = new Writer().c(S_LITO_SERVER_LIST)

                .d(rooms.size()).d(servers.size());

        for (LitoRoomDirectoryEntry room : rooms) {

            requireLitoDirectoryEndpoint(room == null ? null : room.name(),

                    room == null ? -1 : room.ipv4(), room == null ? 0 : room.port());

            requireUnsignedDword(room.roomId(), "LiTo directory room id");

            requireUnsignedDword(room.users(), "LiTo directory room users");

            if (room.roomId() == 0) {

                throw new IllegalArgumentException("LiTo directory room id must be nonzero");

            }

            packet.s(room.name()).d(room.ipv4()).h(room.port())

                    .d(room.roomId()).d(room.users());

        }

        for (LitoVsServerDirectoryEntry server : servers) {

            requireLitoDirectoryEndpoint(server == null ? null : server.name(),

                    server == null ? -1 : server.ipv4(), server == null ? 0 : server.port());

            requireUnsignedDword(server.users(), "LiTo VS-server users");

            requireUnsignedDword(server.capacity(), "LiTo VS-server capacity");

            if (server.capacity() == 0) {

                throw new IllegalArgumentException("LiTo VS-server capacity must be nonzero");

            }

            packet.s(server.name()).d(server.ipv4()).h(server.port())

                    .d(server.users()).d(server.capacity());

        }

        return packet.bytes();

    }



    private static void requireLitoDirectoryEndpoint(String name, long ipv4, int port) {

        String value = name == null ? "" : name;

        byte[] encoded = value.getBytes(LEGACY_TEXT);

        if (encoded.length == 0 || encoded.length > 0x100 || containsControl(value)

                || !Arrays.equals(encoded,

                        new String(encoded, LEGACY_TEXT).getBytes(LEGACY_TEXT))) {

            throw new IllegalArgumentException("invalid native S_LITO_SERVER_LIST name");

        }

        requireUnsignedDword(ipv4, "LiTo directory IPv4");

        if (port < 1 || port > 0xFFFF) {

            throw new IllegalArgumentException("invalid native S_LITO_SERVER_LIST port");

        }

    }



    /** Native S_RESTART receiver {@code sub_4C20D0} consumes no body. */

    static byte[] restart() {

        return new Writer().c(S_RESTART).bytes();

    }



    /** Native S_F5 receiver {@code sub_4AC4C0} consumes no body. */

    static byte[] allowPostTransition() {

        return new Writer().c(S_LITO_VS_SERVER_ALLOW_JOIN).bytes();

    }



    /**

     * Direct rental-village S_CE result reached from an S_FB group-1 row.

     *

     * <p>This deliberately bypasses ordinary S_CC room rendering: exact

     * {@code sub_4A7EF0} only owns wire modes 0..5, while the complete S_CE

     * receiver stores mode 6 and either retained town map directly.</p>

     */

    static byte[] litoRentalTownJoinResult(long roomId, String name,

                                           int mapId, String owner) {

        return litoRentalTownJoinResult(roomId, name, mapId, owner, 1);

    }



    static byte[] litoRentalTownJoinResult(long roomId, String name,

                                           int mapId, String owner, int users) {

        requireUnsignedDword(roomId, "LiTo rental-town room id");

        if (roomId == 0 || NativeLitoTownCatalog.forMap(mapId) == null

                || users < 1 || users > 30) {

            throw new IllegalArgumentException("invalid native rental-town assignment");

        }

        String roomName = name == null ? "" : name;

        String ownerName = owner == null ? "" : owner;

        if (roomName.isBlank() || ownerName.isBlank()

                || containsControl(roomName) || containsControl(ownerName)) {

            throw new IllegalArgumentException("rental-town names are required");

        }

        return new Writer().c(S_LITO_JOIN_RESULT)

                .c(0).d(roomId).s(roomName)

                .c(0).s("").h(users).h(30)

                .c(BattleRules.Mode.TOWN.wireId()).h(mapId)

                .c(0).c(0).c(0).c(0).c(0)

                .s(ownerName)

                .c(0).c(0).c(0).c(0)

                .c(0).bytes();

    }



    /** Native S_FD subtype 1 roster: {@code h(count)h(localIndex)+cs records}. */

    static byte[] raceRoster(int localIndex, List<RaceEntry> entries) {

        if (entries == null || entries.size() > 0x7FFF) {

            throw new IllegalArgumentException("S_LITO_RACE_INFO roster exceeds signed native count");

        }

        if (localIndex < -1 || localIndex >= entries.size()) {

            throw new IllegalArgumentException("S_LITO_RACE_INFO local roster index is invalid");

        }

        Writer packet = new Writer().c(S_LITO_RACE_INFO).c(1)

                .h(entries.size()).h(localIndex < 0 ? 0xFFFF : localIndex);

        for (RaceEntry entry : entries) {

            if (entry == null || entry.name() == null || entry.name().isBlank()) {

                throw new IllegalArgumentException("S_LITO_RACE_INFO roster requires a name");

            }

            packet.c(entry.value()).s(entry.name());

        }

        return packet.bytes();

    }



    /** Native S_FD subtype 2 pair displayed by the race HUD. */

    static byte[] raceValues(int value1, int value2) {

        return new Writer().c(S_LITO_RACE_INFO).c(2).h(value1).h(value2).bytes();

    }



    /** Native S_FD subtype 0 starts the client's one-second HUD timer. */

    static byte[] raceTimerStart() {

        return new Writer().c(S_LITO_RACE_INFO).c(0).bytes();

    }



    /** Native S_FD subtype 3 updates the elapsed time on one named row. */

    static byte[] raceTime(String name, long milliseconds) {

        if (name == null || name.isBlank() || milliseconds < 0 || milliseconds > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("invalid S_LITO_RACE_INFO named time");

        }

        return new Writer().c(S_LITO_RACE_INFO).c(3).s(name).d(milliseconds).bytes();

    }



    record BattleBootstrap(byte[] world, byte[] start, byte[] timeLimit) {}

    record RaceEntry(int value, String name) {}



    static byte[] readyState(String playerName, boolean ready) {

        return readyState(playerName,

                ready ? LitoParticipantState.READY : LitoParticipantState.NOT_READY);

    }



    static byte[] readyState(String playerName, LitoParticipantState state) {

        if (playerName == null || playerName.isBlank()) {

            throw new IllegalArgumentException("S_LITO_READY_GAME requires a room participant name");

        }

        if (state == null) throw new IllegalArgumentException("null LiTo participant state");

        return new Writer().c(S_LITO_READY_GAME).s(playerName)

                .c(state.wireValue()).bytes();

    }



    /**

     * Native C_D2 is a phase-local union.  The participant controls recovered

     * from the image emit exact cc values 0, 1 and 2; value 2 precedes the

     * native litobuy/litosell list request and is not ready=true.  A physical

     * click on the selected LiTo.bin room-owner "Start" control independently

     * emits exact opcode-only c, so that envelope remains distinguishable and

     * can be authorized against current room ownership by ClientSession.

     */

    static LitoReadyRequest litoReadyRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_READY_GAME) {

            throw new IllegalArgumentException("invalid C_LITO_READY_GAME payload");

        }

        if (in.remaining() == 0) {

            return new LitoReadyRequest(LitoReadyOperation.OWNER_START, null);

        }

        if (in.remaining() != 1) {

            throw new IllegalArgumentException("invalid C_LITO_READY_GAME payload");

        }

        return new LitoReadyRequest(LitoReadyOperation.PARTICIPANT_STATE,

                LitoParticipantState.fromWire(in.c()));

    }



    /** Native C_D3 is exactly cc and S_D4 only implements force branches 0 and 1. */

    static LitoForce litoForceRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_SET_FORCE || in.remaining() != 1) {

            throw new IllegalArgumentException("invalid C_LITO_SET_FORCE payload");

        }

        return LitoForce.fromWire(in.c());

    }



    /**

     * Exact C_DF administrator multiplexer produced by {@code sub_452CA0} and

     * {@code sub_452BE0}. All numeric operands are preserved as raw unsigned

     * wire words; no gameplay meaning is assigned to the opaque prepare/load,

     * item or teleport operations without original receive-side evidence.

     */

    static LitoBuilderAdminRequest litoBuilderAdminRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_BUILDER_CONTROL) {

            throw new IllegalArgumentException("invalid C_LITO_BUILDER_CONTROL opcode");

        }

        int subtype = in.h();

        LitoBuilderAdminRequest request = switch (subtype) {

            case 0 -> {

                int force = in.h();

                if (force > 1) {

                    throw new IllegalArgumentException("unsupported forcewin side=" + force);

                }

                yield new LitoBuilderAdminRequest(subtype,

                        LitoBuilderAdminOperation.FORCE_WIN, force, -1, -1, "");

            }

            case 1, 2 -> new LitoBuilderAdminRequest(subtype,

                    subtype == 1 ? LitoBuilderAdminOperation.PREPARE

                            : LitoBuilderAdminOperation.LOAD,

                    in.h(), in.h(), in.h(), "");

            case 3 -> new LitoBuilderAdminRequest(subtype,

                    LitoBuilderAdminOperation.ITEM, in.h(), -1, -1, "");

            case 4 -> {

                byte[] encoded = in.zBytes();

                if (encoded.length > 0x7F) {

                    throw new IllegalArgumentException("native /lito kick buffer exceeds 127 bytes");

                }

                String target = new String(encoded, LEGACY_TEXT);

                if (!Arrays.equals(encoded, target.getBytes(LEGACY_TEXT))) {

                    throw new IllegalArgumentException("native /lito kick target is not valid MS950");

                }

                yield new LitoBuilderAdminRequest(subtype,

                        LitoBuilderAdminOperation.KICK, -1, -1, -1, target);

            }

            case 5 -> new LitoBuilderAdminRequest(subtype,

                    LitoBuilderAdminOperation.ARENA_WHO, -1, -1, -1, "");

            case 10 -> new LitoBuilderAdminRequest(subtype,

                    LitoBuilderAdminOperation.TELEPORT, -1, -1, -1, "");

            default -> throw new IllegalArgumentException(

                    "unsupported C_LITO_BUILDER_CONTROL subtype=" + subtype);

        };

        if (in.remaining() != 0) {

            throw new IllegalArgumentException(

                    "trailing C_LITO_BUILDER_CONTROL bytes=" + in.remaining());

        }

        return request;

    }



    /** Native C_D4 is exactly cs, including the terminating zero byte. */

    static String litoKickRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_KICK_USER) {

            throw new IllegalArgumentException("invalid C_LITO_KICK_USER opcode");

        }

        String target = in.s();

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid C_LITO_KICK_USER payload");

        }

        return target;

    }



    /**

     * Native administrator command {@code roomwho} has two recovered C_FB

     * producers, both exactly {@code cd}.  A zero room id is deliberately

     * representable and means the sender's current room on the server.

     */

    static long litoRoomWhoRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_ROOM_WHO || in.remaining() != 4) {

            throw new IllegalArgumentException("invalid C_LITO_ROOM_WHO payload");

        }

        return in.d();

    }



    /**

     * Native room invitation consumed by {@code sub_4AA2C0} with format

     * {@code sds}. The protected client stores this room id and password, then

     * emits C_CF itself after the user accepts the invitation.

     */

    static byte[] roomInvitation(String inviterName, long roomId, String password) {

        if (inviterName == null || inviterName.isBlank()) {

            throw new IllegalArgumentException("S_LITO_INVITE_USER requires an inviter name");

        }

        return new Writer().c(S_LITO_INVITE_USER)

                .s(inviterName).d(roomId).s(password).bytes();

    }



    /** Native invite response consumed by {@code sub_4AA310} with format {@code sc}. */

    static byte[] roomInvitationReply(String inviteeName, LitoInvitationReply reply) {

        if (inviteeName == null || inviteeName.isBlank()) {

            throw new IllegalArgumentException("S_LITO_INVITE_REPLY requires an invitee name");

        }

        if (reply == null) {

            throw new IllegalArgumentException("null LiTo invitation reply");

        }

        return new Writer().c(S_LITO_INVITE_REPLY)

                .s(inviteeName).c(reply.wireValue()).bytes();

    }



    /**

     * Native C_D6 has exactly two {@code csc} producers.  The accept callback

     * emits literal 1 and immediately follows with C_CF; the alternate

     * callback normalizes its one-byte UI flag to 0/1.  Values 2..255 are not

     * native producer values and must not be collapsed to acceptance.

     */

    static LitoInviteReplyRequest litoInviteReplyRequest(byte[] payload) {

        Reader in = new Reader(payload);

        if (in.c() != C_LITO_INVITE_REPLY) {

            throw new IllegalArgumentException("invalid C_LITO_INVITE_REPLY payload");

        }

        String inviterName = in.s();

        if (in.remaining() != 1) {

            throw new IllegalArgumentException("invalid C_LITO_INVITE_REPLY payload");

        }

        LitoInvitationReply reply = LitoInvitationReply.fromWire(in.c());

        if (in.remaining() != 0) {

            throw new IllegalArgumentException("invalid C_LITO_INVITE_REPLY payload");

        }

        return new LitoInviteReplyRequest(inviterName, reply);

    }



    /**

     * Native S_D5 subtype 0 consumed by {@code sub_4A9E50} as

     * {@code c,d,s,c,h,c,s,c,c,c,c}.  The five summary bytes are separate

     * from the create/edit-room option bytes carried by subtype 1.

     */

    static byte[] litoRoomSummary(long roomId, GameHub.RoomSettings settings,

                                  int mapId, String ownerName) {

        requireNativeRoomIdentity(roomId, settings, mapId, ownerName);

        return new Writer().c(S_LITO_GAME_INFO).c(0).d(roomId)

                .s(settings.name()).c(settings.option0()).h(mapId)

                .c(0).s(ownerName).c(0).c(0).c(0).c(0).bytes();

    }



    /**

     * Native S_D5 subtype 1 consumed by {@code sub_4A9E50} as

     * {@code c,d,s,s,c,c,c,c,c,c,c + c[count]}.  It is the room-edit state:

     * name, password, attribute flags, one-byte capacity, event flags/type,

     * winner count, five-class mask and at most eight original-server ids.

     */

    static byte[] litoRoomConfiguration(long roomId, GameHub.RoomSettings settings) {

        if (settings == null) throw new IllegalArgumentException("room settings are required");

        requireRoomId(roomId);

        int nameBytes = settings.name().getBytes(LEGACY_TEXT).length;

        int passwordBytes = settings.password().getBytes(LEGACY_TEXT).length;

        if (nameBytes == 0 || nameBytes > 0x59 || passwordBytes > 4) {

            throw new IllegalArgumentException("room strings exceed native S_D5 buffers");

        }

        requireByte(settings.attributes(), "room attributes");

        if (settings.capacity() < 2 || settings.capacity() > 0xC8) {

            throw new IllegalArgumentException("room capacity is outside native range");

        }

        boolean legacyZeroTail = settings.eventFlags() == 0 && settings.eventType() == 0

                && settings.winnerCount() == 0 && settings.allowedClassMask() == 0

                && settings.allowedServerIds().isEmpty();

        if (!legacyZeroTail && (settings.eventFlags() < 0 || settings.eventFlags() > 7

                || settings.eventType() < 0 || settings.eventType() > 2

                || settings.winnerCount() < 1 || settings.winnerCount() > 0x7F

                || settings.allowedClassMask() < 0

                || settings.allowedClassMask() > 0x1F)) {

            throw new IllegalArgumentException(

                    "native room event setting is outside producer domain");

        }

        if (settings.selections().size() > 8) {

            throw new IllegalArgumentException("native room selection count exceeds eight");

        }

        Writer packet = new Writer().c(S_LITO_GAME_INFO).c(1).d(roomId)

                .s(settings.name()).s(settings.password()).c(settings.attributes())

                .c(settings.capacity()).c(settings.option1()).c(settings.option2())

                .c(settings.option3()).c(settings.option4()).c(settings.selections().size());

        for (int selection : settings.selections()) {

            requireByte(selection, "room selection");

            if (selection == 0) {

                throw new IllegalArgumentException(

                        "native room original-server id cannot be zero");

            }

            packet.c(selection);

        }

        return packet.bytes();

    }



    /**

     * Native S_D5 subtype 2 changes the room mode stored in word_64ACCA.

     * LTmapstr category parsing and the room renderer agree on the typed

     * {@link BattleRules.Mode} wire domain 0..6.

     */

    static byte[] litoRoomMode(long roomId, BattleRules.Mode mode) {

        requireRoomId(roomId);

        Objects.requireNonNull(mode, "room mode");

        return new Writer().c(S_LITO_GAME_INFO).c(2).d(roomId)

                .c(mode.wireId()).bytes();

    }



    /**

     * Native S_D5 subtype 3 supplies a raw map word. The receiver preserves

     * the {@code 0xFFFF}/{@code 0xFF9C} sentinel words and otherwise derives

     * its display-map byte by adding twelve to the low byte.

     */

    static byte[] litoRoomMap(long roomId, int mapId) {

        requireRoomId(roomId);

        if (mapId < 0 || mapId > 0xFFFF) {

            throw new IllegalArgumentException("native room map is invalid");

        }

        return new Writer().c(S_LITO_GAME_INFO).c(3).d(roomId).h(mapId).bytes();

    }



    /**

     * Exact receiver-shaped S_D5 scalar summary delta. Summary indexes zero

     * through four map to subtypes 4, 6, 7, 8 and 9 respectively. In this

     * client image these five destinations have no read xrefs, so this helper

     * deliberately has no production caller until an original trigger and an

     * observable effect are recovered.

     */

    static byte[] litoRoomSummaryScalar(long roomId, int summaryIndex, int value) {

        requireRoomId(roomId);

        requireByte(value, "room summary scalar");

        int subtype = switch (summaryIndex) {

            case 0 -> 4;

            case 1 -> 6;

            case 2 -> 7;

            case 3 -> 8;

            case 4 -> 9;

            default -> throw new IllegalArgumentException(

                    "native room summary index is outside zero through four");

        };

        return new Writer().c(S_LITO_GAME_INFO).c(subtype).d(roomId).c(value).bytes();

    }



    /** Native S_D5 subtype 5 changes the owner name stored in word_64ABCA. */

    static byte[] litoRoomOwner(long roomId, String ownerName) {

        requireRoomId(roomId);

        int ownerBytes = ownerName == null ? 0 : ownerName.getBytes(LEGACY_TEXT).length;

        if (ownerBytes == 0 || ownerBytes > 0x1B) {

            throw new IllegalArgumentException("native room owner name is invalid");

        }

        return new Writer().c(S_LITO_GAME_INFO).c(5).d(roomId).s(ownerName).bytes();

    }



    private static void requireNativeRoomIdentity(long roomId, GameHub.RoomSettings settings,

                                                  int mapId, String ownerName) {

        if (settings == null) throw new IllegalArgumentException("room settings are required");

        requireRoomId(roomId);

        int nameBytes = settings.name().getBytes(LEGACY_TEXT).length;

        int ownerBytes = ownerName == null ? 0 : ownerName.getBytes(LEGACY_TEXT).length;

        if (nameBytes == 0 || nameBytes > 0x59 || ownerBytes == 0 || ownerBytes > 0x1B) {

            throw new IllegalArgumentException("native room identity is invalid");

        }

        requireByte(settings.option0(), "room mode");

        if (mapId < 0 || mapId > 0xFFFF) {

            throw new IllegalArgumentException("native room map is invalid");

        }

    }



    private static void requireRoomId(long roomId) {

        if (roomId <= 0 || roomId > 0xFFFF_FFFFL) {

            throw new IllegalArgumentException("native room id is invalid");

        }

    }



    /** Native S_EE receiver {@code sub_4AB4E0} parses {@code sh}. */

    static byte[] buddyGroupChanged(String characterName, int groupId) {

        if (characterName == null || characterName.isBlank()) {

            throw new IllegalArgumentException("S_LITO_CHANGE_BUDDY_GROUP_RESULT requires a name");

        }

        return new Writer().c(S_LITO_CHANGE_BUDDY_GROUP_RESULT)

                .s(characterName).h(groupId).bytes();

    }



    /** Native S_EF receiver {@code sub_4AB580} parses typed {@code sc}. */

    static byte[] buddyLogin(String characterName, LitoBuddyPresence presence) {

        if (characterName == null || characterName.isBlank()) {

            throw new IllegalArgumentException("S_LITO_BUDDY_LOGIN requires a name");

        }

        Objects.requireNonNull(presence, "buddy presence");

        return new Writer().c(S_LITO_BUDDY_LOGIN)

                .s(characterName).c(presence.wireValue()).bytes();

    }



    /** Native S_EA receiver {@code sub_4AAFD0} parses {@code c + shccsc[]}. */

    static byte[] buddyList(List<LitoBuddyRow> rows) {

        Objects.requireNonNull(rows, "buddy rows");

        if (rows.size() > 0xFF) {

            throw new IllegalArgumentException("S_LITO_BUDDY_LIST exceeds one-byte count");

        }

        Writer writer = new Writer().c(S_LITO_BUDDY_LIST).c(rows.size());

        for (LitoBuddyRow row : rows) {

            Objects.requireNonNull(row, "buddy row");

            writer.s(row.name()).h(row.groupId()).c(row.relationshipStyle())

                    .c(row.paletteOffset()).s(row.origin()).c(row.activity().wireValue());

        }

        return writer.bytes();

    }



    /** Native S_F1 receiver {@code sub_4AB630} updates S_EA's final activity byte. */

    static byte[] buddyStatus(String characterName, LitoBuddyActivity activity) {

        if (characterName == null || characterName.isBlank()) {

            throw new IllegalArgumentException("S_LITO_BUDDY_CHANGE_STATUS requires a name");

        }

        Objects.requireNonNull(activity, "buddy activity");

        return new Writer().c(S_LITO_BUDDY_CHANGE_STATUS)

                .s(characterName).c(activity.wireValue()).bytes();

    }



    /** Native S_F2 receiver {@code sub_4AB680}: exact typed {@code c(error)}. */

    static byte[] buddyError(LitoBuddyError error) {

        Objects.requireNonNull(error, "buddy error");

        return new Writer().c(S_LITO_BUDDY_ERROR).c(error.wireValue()).bytes();

    }



    /** Native S_F6 receiver {@code sub_4AB780}: {@code sc + shccsc records}. */

    static byte[] clanInfo(GameHub.ClanData data) {

        if (data == null || data.name().isBlank()) {

            return new Writer().c(S_LITO_CLAN_INFO).s("").c(0).bytes();

        }

        if (data.members().size() > 0xFF) {

            throw new IllegalArgumentException("S_LITO_CLAN_INFO member count exceeds one byte");

        }

        Writer packet = new Writer().c(S_LITO_CLAN_INFO).s(data.name()).c(data.members().size());

        for (GameHub.ClanMemberSnapshot member : data.members()) writeClanMember(packet, member);

        return packet.bytes();

    }



    /**

     * Native LiTo clan creation has no dedicated result packet: S_F6 is only

     * the {@code sc + shccsc*} roster snapshot.  The selected client already

     * receives the retained clan-state messages through exact S_MESSAGE_CODE,

     * and the matching 3.81 server source maps these two reachable failures

     * to 86 (owner already belongs to a clan) and 99 (duplicate clan name).

     */

    static byte[] litoClanCreateFailure(GameHub.ClassicClanCreateStatus status) {

        int code = switch (status) {

            case OWNER_ALREADY_MEMBER -> 86;

            case DUPLICATE_NAME -> 99;

            default -> throw new IllegalArgumentException(

                    "native LiTo clan-create status has no failure message: " + status);

        };

        return messageCode(code);

    }



    /** Native S_F7 receiver {@code sub_4ABBB0}: one {@code shccsc} record. */

    static byte[] clanMemberAdded(GameHub.ClanMemberSnapshot member) {

        Writer packet = new Writer().c(S_LITO_CLAN_ADD);

        writeClanMember(packet, member);

        return packet.bytes();

    }



    /** Native S_F9 receiver {@code sub_4ABF40}: {@code s(characterName)}. */

    static byte[] clanMemberDeleted(String characterName) {

        if (characterName == null || characterName.isBlank()) {

            throw new IllegalArgumentException("S_LITO_CLAN_DELETE requires a name");

        }

        return new Writer().c(S_LITO_CLAN_DELETE).s(characterName).bytes();

    }



    /** Native S_FA receiver {@code sub_4AC1C0}: {@code ddsd}. */

    static byte[] battleClanMetadata(long objectId, long value1, String clanName, boolean active) {

        if (objectId == 0) throw new IllegalArgumentException("S_LITO_CLAN requires an object id");

        return new Writer().c(S_LITO_CLAN).d(objectId).d(value1)

                .s(clanName == null ? "" : clanName).d(active ? 1 : 0).bytes();

    }



    private static void writeClanMember(Writer packet, GameHub.ClanMemberSnapshot member) {

        if (member == null || member.name() == null || member.name().isBlank()) {

            throw new IllegalArgumentException("native clan record requires a character name");

        }

        Objects.requireNonNull(member.role(), "native clan role");

        Objects.requireNonNull(member.activity(), "native clan activity");

        packet.s(member.name()).h(member.role().wireValue())

                .c(member.relationshipStyle()).c(member.paletteOffset())

                .s(member.title() == null ? "" : member.title())

                .c(member.activity().wireValue());

    }



    static byte[] copyOf(byte[] data) { return Arrays.copyOf(data, data.length); }

}

