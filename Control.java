import josx.platform.rcx.Motor;

public class Control {
    public static void stop(String option) {
        Motor.A.stop();
        Motor.B.stop();
        Motor.C.stop();
    }
}
