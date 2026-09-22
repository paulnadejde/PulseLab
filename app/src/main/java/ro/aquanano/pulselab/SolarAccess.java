package ro.aquanano.pulselab;

import android.content.SharedPreferences;

import ro.aquanano.pulselab.core.SolarAccessLogic;

/** Shared access state used by the SolaRitm screen and notification agent. */
final class SolarAccess {
    static final String PREF_USE_MASTER = "solar_access_use_master";
    static final String PREF_MASTER_VALIDATED = "solar_access_master_validated";
    static final String PREF_CLIENT_VALIDATED = "solar_access_client_validated";
    static final String PREF_CLIENT_REFERENCE = "solar_access_client_reference";

    private SolarAccess() { }

    static boolean isGranted(SharedPreferences preferences) {
        return SolarAccessLogic.isGranted(
            preferences.getBoolean(PREF_USE_MASTER, false),
            preferences.getBoolean(PREF_MASTER_VALIDATED, false),
            preferences.getBoolean(PREF_CLIENT_VALIDATED, false));
    }

    static boolean validateAndRememberMaster(SharedPreferences preferences, String password) {
        if (!preferences.getBoolean(PREF_USE_MASTER, false)
                || !SolarAccessLogic.isMasterPasswordValid(
                    password, BuildConfig.SOLARITM_MASTER_PASSWORD_HASH)) return false;
        preferences.edit().putBoolean(PREF_MASTER_VALIDATED, true).apply();
        return true;
    }

    /** Reserved for a later, independently verified client activation route. */
    static void rememberValidatedClient(SharedPreferences preferences, String reference) {
        preferences.edit()
            .putBoolean(PREF_CLIENT_VALIDATED, true)
            .putString(PREF_CLIENT_REFERENCE, reference == null ? "" : reference)
            .apply();
    }
}
