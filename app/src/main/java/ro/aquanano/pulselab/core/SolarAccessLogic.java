package ro.aquanano.pulselab.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Pure validation rules for access to the SolaRitm glyph display. */
public final class SolarAccessLogic {
    private SolarAccessLogic() { }

    public static boolean isMasterPasswordValid(String password, String expectedHashHex) {
        if (password == null || expectedHashHex == null || expectedHashHex.length() != 64)
            return false;
        try {
            byte[] candidate = MessageDigest.getInstance("SHA-256")
                .digest(password.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(hex(expectedHashHex), candidate);
        } catch (Exception unavailable) {
            return false;
        }
    }

    public static boolean isGranted(boolean useMasterPassword,
                                    boolean masterPasswordValidated,
                                    boolean clientPasswordValidated) {
        return clientPasswordValidated
            || (useMasterPassword && masterPasswordValidated);
    }

    private static byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) throw new IllegalArgumentException("hash");
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }
}
