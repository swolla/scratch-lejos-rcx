import josx.platform.rcx.Button;
import josx.platform.rcx.Sensor;
import josx.platform.rcx.SensorConstants;
import josx.platform.rcx.Sound;
import josx.platform.rcx.remotecontrol.RemoteControlSensor;
import josx.util.Timer;

public class Scratch implements SensorConstants {
    public static void main(String[] args) {
        new Scratch().run(new ScratchProject());
    }

    public void run(ScratchProject scratchProject) {
        Timer timer = new Timer(100, scratchProject);
        timer.start();
        Sensor.S1.setTypeAndMode(SENSOR_TYPE_TOUCH, SENSOR_MODE_BOOL);
        Sensor.S1.activate();
        Sensor.S1.addSensorListener(scratchProject);
        Sensor.S2.setTypeAndMode(SENSOR_TYPE_LIGHT, SENSOR_MODE_PCT);
        Sensor.S2.activate();
        Sensor.S2.addSensorListener(scratchProject);
        Sensor.S3.setTypeAndMode(SENSOR_TYPE_TOUCH, SENSOR_MODE_BOOL);
        Sensor.S3.activate();
        Sensor.S3.addSensorListener(scratchProject);
        RemoteControlSensor remoteControlSensor = new RemoteControlSensor();
        remoteControlSensor.addRemoteControlListener(scratchProject);
        Button.RUN.addButtonListener(scratchProject);
        Button.VIEW.addButtonListener(scratchProject);
        Button.PRGM.addButtonListener(scratchProject);

        try {
            Button.RUN.waitForPressAndRelease();
        } catch (Exception ex) {
            Sound.buzz();
        }
    }
}
