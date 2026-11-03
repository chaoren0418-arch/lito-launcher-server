package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** Locks the original page-7 callback evidence and the manual runtime policy. */
final class NativeCharacterSelectionUiContractTest {
    private static final Path CLIENT = Path.of(
            "D:/380/天堂(Lineage 3.81C) (1)/LiTo.bin");
    private static final Path DISASSEMBLY = Path.of(
            "../LiTo381-開發/ida-client-oep-v2/full-disassembly.asm");
    private static final Path UNPACKED_CLIENT = Path.of(
            "../LiTo381-開發/runtime/LiTo.unpacked-oep-current.exe");
    private static final Path UI_PROBE = Path.of(
            "../LiTo381-開發/tools/lito_ui_button_probe.py");
    private static final Path RUNTIME_PROBE = Path.of(
            "../LiTo381-開發/tools/lito_runtime_probe.py");

    @Test
    void selectedClientPinsPageSevenRowsAndLeftConfirmToCC9() throws Exception {
        assertEquals("CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292",
                sha256(CLIENT));
        assertEquals("2A3A6E1026EE65288C07EFF00990BDDDBEFA5ACEC9EED551F528A0CC0A7DEA9A",
                sha256(DISASSEMBLY));

        String assembly = read(DISASSEMBLY);
        String producer = between(assembly,
                "sub_4928C0 proc near", "sub_4928C0 endp");
        ordered(producer,
                "mov     edx, 3",
                "call    sub_494120",
                "cmp     al, 0FFh",
                "cmp     byte ptr [ebx+2Eh], 0FFh",
                "mov     al, [ebx+37h]",
                "push    0C9h",
                "push    offset aCs",
                "call    sub_4C6AE0",
                "mov     ecx, 3A980h",
                "call    sub_4D65F0");
        assertFalse(producer.contains("2710h"),
                "the native C_C9 path owns a 240-second timer, not ten seconds");

        String rows = between(assembly, "dword_630DF8 dd", "dword_6311F8 dd");
        assertEquals(4, occurrences(rows, "offset sub_4934A0"),
                "page 7 must retain exactly four selectable character rows");
        assertTrue(rows.contains("dd offset sub_4928C0"),
                "the left confirm control must retain the native C_C9 producer");
        assertTrue(rows.contains("dword_630FA0 dd 1839h"));
        assertTrue(rows.contains("off_630FAC dd offset sub_493150"),
                "the ordinary right control is the leave callback");

        String rowCallback = between(assembly,
                "sub_4934A0 proc near", "sub_4934A0 endp");
        ordered(rowCallback,
                "mov     dword_75F750, edi",
                "call    sub_4934F0",
                "mov     ecx, 8",
                "jmp     sub_4928C0");
    }

    @Test
    void tenSecondCountdownBelongsToLtcharsvrRightControl()
            throws Exception {
        String assembly = read(DISASSEMBLY);
        String specialMode = between(assembly,
                "sub_4966A0 proc near", "sub_4966A0 endp");
        ordered(specialMode,
                "mov     dword_630EE8, ebx",
                "mov     dword_630EEC, 1D2Eh",
                "mov     dword_630FA0, 1D2Fh",
                "mov     off_630FAC, offset dword_4967B0");

        // IDA exported this obfuscated callback as raw dwords. These are the
        // exact little-endian encodings for mov [0x00632458],10 followed by a
        // one-second sub_493F10 timer. They are reached only after the right
        // control's native confirmation callback accepts value 1.
        String callbackBlob = between(assembly,
                "dword_4967B0 dd", "sub_496A00 proc near");
        assertTrue(callbackBlob.contains("63245805h, 0A00h"));
        assertTrue(callbackBlob.contains("10BA0000h, 0B900493Fh, 3E8h"),
                "raw little-endian bytes must encode mov edx,493F10h and mov ecx,1000");
    }

    @Test
    void physicalProbeWaitContinuesIntoRowAndConfirmActions() throws Exception {
        String probe = read(UI_PROBE);
        assertTrue(probe.contains(
                "Action(\"native_character_row_1\", 30, 76"));
        assertTrue(probe.contains(
                "Action(\"native_source_1\", 118, 78"));
        assertTrue(probe.contains(
                "Action(\"native_character_confirm\", 260, 208"));
        String sourceSelection = between(probe,
                "if args.select_native_source_1:",
                "if args.wait_native_character_page > 0:");
        assertTrue(sourceSelection.contains("wait_for_native_source_overlay("),
                "physical input must wait until the original 0x1813 row exists");
        assertTrue(sourceSelection.contains(
                "time.sleep(max(0.0, args.native_source_delay))"));
        assertEquals(1, occurrences(sourceSelection,
                "real_click(hwnd, source.x, source.y)"),
                "the native source row sends C_C8 from one button-up callback");
        assertTrue(sourceSelection.contains("\"click_count\": 1"));
        assertTrue(sourceSelection.contains("sub_4999B0(index=0)"));

        int submit = probe.indexOf("submit_native_login(hwnd, account, password)");
        int source = probe.indexOf("if args.select_native_source_1:");
        int wait = probe.indexOf("if args.wait_native_character_page > 0:");
        int actions = probe.indexOf("for index, name in enumerate(args.actions, 1):");
        assertTrue(submit >= 0 && submit < source && source < wait,
                "login and retained-source selection must precede page-7 readiness");
        assertTrue(wait < actions,
                "page-7 readiness must be established before row/confirm actions");

        String waitBranch = probe.substring(wait,
                probe.indexOf("if args.confirm_close:", wait));
        assertFalse(waitBranch.contains("dialog"),
                "page readiness must not enter the unrelated dialog-dismiss path");
        assertFalse(waitBranch.contains("return 0"),
                "page readiness must continue into the requested row/confirm actions");
    }

    @Test
    void nativeSourceOverlayUsesOneClickCallbackToProduceCC8() throws Exception {
        String assembly = read(DISASSEMBLY);
        byte[] unpacked = Files.readAllBytes(UNPACKED_CLIENT);
        // IDA ended sub_498F80 early at 0x49918C and exported this valid tail
        // as raw dwords. Pin the executable bytes that create source index 0:
        // push esi(index twice), push sub_4999B0, x/y, image 0x1813, then call
        // sub_457FE0. The section is mapped flat, so RVA equals file offset.
        assertBytesAt(unpacked, 0x000998D3,
                "565668B0994900535768131800008BC8E8F8E6FBFF");
        assertBytesAt(unpacked, 0x00099978,
                "C705BCC96200908A4900C705B8C96200409A4900"
                + "C705C0C96200609A4900");

        String rowCallback = between(assembly,
                "sub_4999B0 proc near", "sub_4999B0 endp");
        ordered(rowCallback,
                "mov     dword_75F6A0, ebx",
                "mov     ecx, 1",
                "jmp     dword_75FF78");

        String sourceProducer = between(assembly,
                "sub_48F580 proc near", "sub_48F580 endp");
        ordered(sourceProducer,
                "cmp     esi, 1",
                "mov     eax, dword_75F6A0",
                "push    0C8h",
                "push    offset aCs",
                "call    sub_4C6AE0");
    }

    @Test
    void physicalBridgeReturnsAfterBuiltPageSevenWithoutOpeningAnotherSourceModal()
            throws Exception {
        String assembly = read(DISASSEMBLY);
        String tail = between(assembly, "loc_496FB4:", "sub_496EB0 endp");
        ordered(tail,
                "mov     al, byte_64AFA0",
                "loc_496FC6:",
                "call    sub_41B720",
                "call    sub_41B6C0",
                "call    sub_41B720",
                "call    sub_41B6E0",
                "mov     ecx, 3",
                "jmp     sub_498F80");

        String runtimeProbe = read(RUNTIME_PROBE);
        String bridge = between(runtimeProbe,
                "def install_physical_character_page_bridge(",
                "def invoke_manual_news_ready_bridge(");
        assertTrue(bridge.contains(
                "CLIENT_CHARACTER_PAGE_NETWORK_TAIL, b\"\\xC3\\x90\""),
                "page 7 is already built, so the mode-1 tail must return before "
                        + "the duplicate C_C8 and the mode-3 source-modal path");
        assertFalse(bridge.contains(
                "CLIENT_CHARACTER_PAGE_NETWORK_TAIL, b\"\\xEB\\x10\""),
                "EB 10 enters loc_496FC6 and opens a second retained-source modal");
        assertTrue(bridge.contains("\"replacement\": \"C3 90\""));
        assertTrue(bridge.contains("has already constructed page 7"));
        assertTrue(bridge.contains(
                "CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH,"));
        assertTrue(bridge.contains(
                "CLIENT_CHARACTER_ROW_LOCAL_ONLY_BRANCH_BYTES"),
                "row clicks must retain local selection/preview and leave C_C9 to Confirm");
        ordered(bridge,
                "if expected_character_server_mode == 1:",
                "CLIENT_CHARACTER_SERVER_MODE) + b\"\\x00\"",
                "CLIENT_CHARACTER_PAGE_BUILDER - (remote + call_offset + 5)",
                "CLIENT_CHARACTER_SERVER_MODE) + b\"\\x01\"");
        assertTrue(bridge.contains("ten_second_automatic_page_disabled"),
                "the physical page report must expose the disabled legacy auto page");
        assertTrue(bridge.contains("CLIENT_CHARACTER_COUNT_HANDLER_ENTRY"));
        assertTrue(bridge.contains("CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL_BYTES"));
        assertTrue(bridge.contains("CLIENT_CHARACTER_DIRECTORY_LOCAL_UI_REFRESH"));
        assertTrue(bridge.contains("CLIENT_CHARACTER_SELECTED_INDEX"));
        assertTrue(bridge.contains("character_directory_final_refresh"));
        assertTrue(bridge.contains("CLIENT_CHARACTER_PAGE_TIMER_CALLBACK"));
        assertTrue(bridge.contains("CLIENT_LOGIN_RECONNECT_TIMER_CALLBACK"));
        assertTrue(bridge.contains("CLIENT_GAME_RECONNECT_TIMER_CALLBACK"));
        assertTrue(bridge.contains("ten_second_reconnect_loop_disabled"),
                "final S_CB must restore page 7's native timer and stop alternating reconnects");
        assertTrue(bridge.contains("expected_character_count_address"));
        assertTrue(bridge.contains("character_directory_completion_policy"),
                "the bridge must retain S_03 count until all announced S_CB rows arrive");
        assertTrue(bridge.contains(
                "CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH"));
        assertTrue(bridge.contains(
                "CLIENT_CHARACTER_PAGE_STABLE_RETURN_BRANCH_BYTES"));
        assertTrue(bridge.contains("CLIENT_SERVER_LIST_HANDLER_ENTRY"));
        assertTrue(bridge.contains("stale_server_list_suppression"));
        assertTrue(bridge.contains("C35 then blank C8 00"));
        assertTrue(bridge.contains("\"page_seven_loading_loop_disabled\": True"));
        assertTrue(bridge.contains(
                "\"page-7 return before it can emit blank C8 00 and Loading\""));
    }

    @Test
    void physicalNativeLoginRequiresOriginalLtcharsvrMode() throws Exception {
        String runtimeProbe = read(RUNTIME_PROBE);
        String validation = between(runtimeProbe,
                "if args.physical_native_login:",
                "if args.invoke_transfer_stone_producer:");
        assertTrue(validation.contains(
                "argument.casefold() == \"/ltcharsvr\" for argument in args.arguments"));
        assertTrue(validation.contains(
                "physical native login requires the original /ltcharsvr argument"));
    }

    @Test
    void channelSelectorSourceFlagIsArmedOnlyByNativeCharacterConfirm()
            throws Exception {
        String assembly = read(DISASSEMBLY);
        String resultHandler = between(assembly,
                "sub_4A76D0 proc near", "sub_4A76D0 endp");
        ordered(resultHandler,
                "cmp     byte_64AFBC, bl",
                "mov     dword_64AFB8, 2",
                "mov     byte_64AFBC, bl",
                "call    sub_496A00",
                "call    sub_497020");

        String selectorBuilder = between(assembly,
                "sub_497020 proc near", "sub_497020 endp");
        ordered(selectorBuilder,
                "mov     eax, dword_64AFB8",
                "test    eax, eax",
                "push    64h ; 'd'",
                "push    0CAh",
                "loc_49703E:");

        String runtimeProbe = read(RUNTIME_PROBE);
        assertTrue(runtimeProbe.contains(
                "\"post_directory_channel_selector_handoff\": {"));
        assertTrue(runtimeProbe.contains(
                "\"armed_only_by_native_character_confirm\""));
        String confirmHook = between(runtimeProbe,
                "confirm_stub = bytearray()",
                "page = bytearray(b\"\\x90\" * 0x400)");
        ordered(confirmHook,
                "CLIENT_UI_ROOT",
                "b\"\\x83\\x78\\x0C\\x07\"",
                "CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A",
                "CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX_BYTES");
        assertTrue(runtimeProbe.contains(
                "confirm_hook_address - (CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX + 5)"));
        assertTrue(runtimeProbe.contains(
                "keep both source bytes clear while S_CB parses"));
        assertTrue(runtimeProbe.contains(
                "automatic blank C8 reconnect loop"));
        assertTrue(runtimeProbe.contains(
                "def native_character_directory_batch_complete("));
        assertTrue(runtimeProbe.contains(
                "slot_markers[slot * 2 : slot * 2 + 2]"));
        assertTrue(runtimeProbe.contains(
                "populated != (slot < expected_count)"));

        String initialActivation = between(runtimeProbe,
                "initial_activation = physical_page_bridge.get(",
                "if (\n                    args.manual_native_selection");
        ordered(initialActivation,
                "directory_batch_complete",
                "delay_after_complete_seconds",
                "CLIENT_CHARACTER_SELECTED_INDEX",
                "post_native_character_row_selection(");
        assertTrue(runtimeProbe.contains(
                "\"row_click_network_disabled\": True"));
    }

    @Test
    void physicalLoginStagesTheSameDisplayIdentityUsedByReadyAndExitPackets()
            throws Exception {
        String runtimeProbe = read(RUNTIME_PROBE);
        String handoff = between(runtimeProbe,
                "# A physical native login keeps the account only in",
                "elif phase_value not in (1, 2, 3):");
        ordered(handoff,
                "connect_redirect[\"credential_valid_address\"]",
                "connect_redirect[\"hook_address\"]",
                "connect_bridge + 0x820",
                "CLIENT_ACCOUNT_CELL_COUNT",
                "build_lito_display_identity(",
                "CLIENT_TOURNAMENT_CHARACTER_NAME",
                "CLIENT_ROOM_LOCAL_NAME");
        assertTrue(handoff.contains(
                "\"process_private_native_login_capture\""));
        assertTrue(handoff.contains("\"account_logged\": False"));
        assertFalse(handoff.contains("credential_password_address"),
                "room identity decoration must never read the captured password");
    }

    private static void ordered(String text, String... fragments) {
        int position = -1;
        for (String fragment : fragments) {
            int next = text.indexOf(fragment, position + 1);
            assertTrue(next > position, "missing or out-of-order native fragment: " + fragment);
            position = next;
        }
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        for (int at = 0; (at = text.indexOf(needle, at)) >= 0;
             at += needle.length()) count++;
        return count;
    }

    private static String between(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        int end = text.indexOf(endMarker, start + Math.max(1, startMarker.length()));
        assertTrue(start >= 0, "missing start marker: " + startMarker);
        assertTrue(end > start, "missing end marker: " + endMarker);
        return text.substring(start, end);
    }

    private static String read(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), "missing source evidence: " + path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) {
                if (count > 0) digest.update(buffer, 0, count);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(digest.digest());
    }

    private static void assertBytesAt(byte[] bytes, int offset, String expectedHex) {
        byte[] expected = HexFormat.of().parseHex(expectedHex);
        assertTrue(offset >= 0 && offset + expected.length <= bytes.length,
                "native byte range is outside the unpacked image");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], bytes[offset + i],
                    "native byte mismatch at file offset 0x"
                            + Integer.toHexString(offset + i));
        }
    }
}
