package local.lito381.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ProtocolAccountTest {
    @Test void parsesExactNativeRegistrationAndPasswordChangeFrames() {
        Protocol.Writer registration = new Protocol.Writer()
                .c(Protocol.C_NEW_ACCOUNT)
                .s("Native01").s("Passcode1")
                .s("mail@example.invalid").s("profile-id")
                .c(1);
        List<String> tail = new ArrayList<>();
        for (int index = 0; index < 13; index++) {
            String value = "field" + index;
            tail.add(value);
            registration.s(value);
        }
        Protocol.NewAccountRequest created = Protocol.newAccountRequest(registration.bytes());
        assertEquals("Native01", created.account());
        assertEquals("Passcode1", created.password());
        assertEquals(1, created.type());
        assertEquals(List.of("mail@example.invalid", "profile-id"),
                created.profileFields().subList(0, 2));
        assertEquals(tail, created.profileFields().subList(2, 15));

        assertEquals(new Protocol.PasswordChangeRequest("Native01", "test", "Changed1"),
                Protocol.passwordChangeRequest(new Protocol.Writer()
                        .c(Protocol.C_CHANGE_PASSWORD)
                        .s("Native01").s("test").s("Changed1").bytes()));

        assertEquals(new Protocol.QuizRegistrationRequest(
                        "Native01", "test", "Changed1",
                        List.of("first-question", "answer-one", "second-question", "answer-two")),
                Protocol.quizRegistrationRequest(new Protocol.Writer()
                        .c(Protocol.C_REGISTER_QUIZ)
                        .s("Native01").s("test").s("Changed1")
                        .s("first-question").s("answer-one")
                        .s("second-question").s("answer-two").bytes()));

        Protocol.Writer accountInfo = new Protocol.Writer()
                .c(Protocol.C_CHANGE_ACCOUNTINFO).s("Native01")
                .s("field0").s("field1").s("field2").s("field3")
                .c(1).s("field4").s("field5").s("field6");
        assertEquals(new Protocol.AccountInfoChangeRequest("Native01", 1,
                        List.of("field0", "field1", "field2", "field3",
                                "field4", "field5", "field6")),
                Protocol.accountInfoChangeRequest(accountInfo.bytes()));
    }

    @Test void writesCompactNativeAccountCheckFrames() {
        assertArrayEquals(new byte[] {0x02, 0x04, 0, 0, 0, 0},
                Protocol.newAccountCheck(
                        Protocol.NewAccountCheckResult.RETURN_TO_LOGIN, 0));
        assertArrayEquals(new byte[] {0x02, 0x07, 0, 0, 0, 0},
                Protocol.newAccountCheck(
                        Protocol.NewAccountCheckResult.ACCOUNT_ALREADY_EXISTS, 0));
        assertArrayEquals(new byte[] {0x02, 0x01, 0, 0, 0, 0},
                Protocol.newAccountCheck(
                        Protocol.NewAccountCheckResult.SYSTEM_ERROR_NO_TRANSITION, 0));
        assertArrayEquals(new byte[] {0x08, 0x0A, 0, 0, 0, 0},
                Protocol.changePasswordCheck(
                        Protocol.ChangePasswordCheckResult.WRONG_PASSWORD, 0));
        assertArrayEquals(new byte[] {0x08, 0x35, 0, 0, 0, 0},
                Protocol.changePasswordCheck(
                        Protocol.ChangePasswordCheckResult.PASSWORD_CHANGE_UNAVAILABLE, 0));
        assertArrayEquals(new byte[] {0x67, 0x04, 0, 0, 0, 0},
                Protocol.changeAccountInfoCheck(
                        Protocol.ChangeAccountInfoCheckResult.RETURN_TO_LOGIN, 0));
    }

    @Test void rejectsNonNativeAccountShapes() {
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.passwordChangeRequest(new Protocol.Writer()
                        .c(Protocol.C_CHANGE_PASSWORD)
                        .s("1bad").s("test").s("Changed1").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.passwordChangeRequest(new Protocol.Writer()
                        .c(Protocol.C_CHANGE_PASSWORD)
                        .s("Native01").s("test").s("lettersOnly").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.quizRegistrationRequest(new Protocol.Writer()
                        .c(Protocol.C_REGISTER_QUIZ)
                        .s("Native01").s("Changed1").s("Changed1")
                        .s("first-question").s("answer-one")
                        .s("second-question").s("answer-two").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.quizRegistrationRequest(new Protocol.Writer()
                        .c(Protocol.C_REGISTER_QUIZ)
                        .s("Native01").s("test").s("Changed1")
                        .s("short").s("answer-one")
                        .s("second-question").s("answer-two").bytes()));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.quizRegistrationRequest(new Protocol.Writer()
                        .c(Protocol.C_REGISTER_QUIZ)
                        .s("Native01").s("test").s("Changed1")
                        .s("first-question").s("answer-one")
                        .s("second-question").s("answer-two").c(0).bytes()));
        assertThrows(NullPointerException.class,
                () -> Protocol.newAccountCheck(null, 0));
        assertThrows(NullPointerException.class,
                () -> Protocol.changePasswordCheck(null, 0));
        assertThrows(NullPointerException.class,
                () -> Protocol.changeAccountInfoCheck(null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.newAccountCheck(
                        Protocol.NewAccountCheckResult.RETURN_TO_LOGIN, 0x1_0000_0000L));
        assertThrows(IllegalArgumentException.class,
                () -> Protocol.accountInfoChangeRequest(new Protocol.Writer()
                        .c(Protocol.C_CHANGE_ACCOUNTINFO).s("Native01")
                        .s("a").s("b").s("c").s("d").c(2)
                        .s("e").s("f").s("g").bytes()));
    }
}
