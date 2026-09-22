package ro.aquanano.pulselab.core;

public final class SolarAccessLogicTests {
    public static void main(String[] args) {
        String testHash =
            "50e36703f8a3672f3ba30790879e4ff511babde6f48f06c2d3d9efff76183705";
        assert SolarAccessLogic.isMasterPasswordValid("unit_test_password", testHash);
        assert !SolarAccessLogic.isMasterPasswordValid("wrong_password", testHash);
        assert !SolarAccessLogic.isMasterPasswordValid("unit_test_password", "");
        assert !SolarAccessLogic.isGranted(false, false, false);
        assert !SolarAccessLogic.isGranted(false, true, false);
        assert SolarAccessLogic.isGranted(true, true, false);
        assert SolarAccessLogic.isGranted(false, false, true);
        System.out.println("Solar access tests passed");
    }
}
