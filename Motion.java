import josx.platform.rcx.Motor;
import josx.platform.rcx.Sound;

public class Motion {
    private static final int MIN_POWER = 0;
    private static final int MAX_POWER = 7;

    public static void moveSteps(float steps) {
        if (steps > 0) {
            Motor.A.setPower(1);
            Motor.A.forward();
            Motor.C.setPower(1);
            Motor.A.forward();

            try {
                Thread.sleep(Math.round(100 * steps));
            } catch (Exception ex) {
                Sound.buzz();
            }
        }
        Motor.A.flt();
        Motor.B.flt();
        Motor.C.flt();
    }

    public static void turnRight(float degrees) {
        if (degrees > 0) {
            Motor.A.setPower(MAX_POWER);
            Motor.A.forward();
            Motor.C.setPower(MAX_POWER);
            Motor.A.backward();

            try {
                Thread.sleep(Math.round(100 * degrees));
            } catch (Exception ex) {
                Sound.buzz();
            }
        }
        Motor.A.flt();
        Motor.B.flt();
        Motor.C.flt();
    }

    public static void turnLeft(float degrees) {
        if (degrees > 0) {
            Motor.A.setPower(MAX_POWER);
            Motor.A.backward();
            Motor.C.setPower(MAX_POWER);
            Motor.A.forward();

            try {
                Thread.sleep(Math.round(100 * degrees));
            } catch (Exception ex) {
                Sound.buzz();
            }
        }
        Motor.A.flt();
        Motor.B.flt();
        Motor.C.flt();
    }
}
