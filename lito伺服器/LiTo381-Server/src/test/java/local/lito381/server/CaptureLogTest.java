package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CaptureLogTest {
    @TempDir Path temporary;

    @Test void masksEveryNativeAccountCredentialAndProfileRequest() throws Exception {
        Path capturePath;
        try (CaptureLog capture = new CaptureLog(temporary)) {
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_LOGIN).s("Account").s("Secret1").bytes(),
                    20, "login");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_NEW_ACCOUNT).s("Account").s("Secret1").bytes(),
                    20, "registration");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_CHANGE_PASSWORD)
                            .s("Account").s("Secret1").s("Secret2").bytes(),
                    30, "password");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_REGISTER_QUIZ)
                            .s("Account").s("Secret1").s("Secret2")
                            .s("PrivateQuestion").s("PrivateAnswer").bytes(),
                    40, "quiz");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_CHANGE_ACCOUNTINFO)
                            .s("Account").s("PrivateProfileValue").bytes(),
                    30, "profile");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_WISH).s("OpaqueBuilderRequest").bytes(),
                    24, "wish");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_SILENCE).h(3600)
                            .s("PrivateTargetName").bytes(),
                    30, "silence");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_WANTED).c(1)
                            .s("PrivateWatchOperand").bytes(),
                    30, "wanted");
            capture.frame("server_to_client",
                    Protocol.wantedLogin("PrivateAccount", "198.51.100.27"),
                    31, "wanted-login");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_WAREHOUSE_CONTROL).c(0)
                            .d(123456).d(654321).bytes(),
                    12, "warehouse-password");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_SMS).c(0)
                            .s("PrivateSmsContents").s("0912345678").bytes(),
                    40, "sms");
            capture.frame("client_to_server",
                    new Protocol.Writer().c(Protocol.C_BUILDER_CONTROL).c(0x9A)
                            .d(1).d(60_000).c(1).s("PrivateNoticeContents").bytes(),
                    40, "builder-control");
            capture.frame("client_to_server", new byte[] {(byte) Protocol.C_MOVE, 1, 2},
                    5, "ordinary");
            capturePath = capture.path();
        }

        String json = Files.readString(capturePath, StandardCharsets.UTF_8);
        assertTrue(json.contains("MASKED_C_LOGIN"));
        assertTrue(json.contains("MASKED_C_NEW_ACCOUNT"));
        assertTrue(json.contains("MASKED_C_CHANGE_PASSWORD"));
        assertTrue(json.contains("MASKED_C_REGISTER_QUIZ"));
        assertTrue(json.contains("MASKED_C_CHANGE_ACCOUNTINFO"));
        assertTrue(json.contains("MASKED_C_WISH"));
        assertTrue(json.contains("MASKED_C_SILENCE duration=3600 bytes=21"));
        assertTrue(json.contains("MASKED_C_WANTED mode=1 bytes=22"));
        assertTrue(json.contains("MASKED_S_WANTED_LOGIN bytes=30"));
        assertTrue(json.contains("MASKED_C_WAREHOUSE_CONTROL mode=0 bytes=10"));
        assertTrue(json.contains("MASKED_C_SMS mode=0 bytes=32"));
        assertTrue(json.contains("MASKED_C_BUILDER_CONTROL subtype=0x9A bytes=33"));
        assertTrue(json.contains("0A 01 02"));
        assertFalse(json.contains("Account"));
        assertFalse(json.contains("Secret"));
        assertFalse(json.contains("PrivateProfileValue"));
        assertFalse(json.contains("OpaqueBuilderRequest"));
        assertFalse(json.contains("PrivateTargetName"));
        assertFalse(json.contains("PrivateWatchOperand"));
        assertFalse(json.contains("PrivateAccount"));
        assertFalse(json.contains("198.51.100.27"));
        assertFalse(json.contains("40 E2 01 00"));
        assertFalse(json.contains("F1 FB 09 00"));
        assertFalse(json.contains("PrivateSmsContents"));
        assertFalse(json.contains("0912345678"));
        assertFalse(json.contains("PrivateNoticeContents"));
    }
}
