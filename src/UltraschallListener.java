
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;
import java.text.ParseException;

/**
 * Write a description of class Ultraschall here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class UltraschallListener
{

    public  static double start;
    public  static double stopp;
    public  static double wert;
    public  static double div;
    public  static double distanz;
    public  static final double luft = 343.0f;
    public  static boolean ok;

    public static class EchoListener implements GpioPinListenerDigital {
        public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
            wert = System.nanoTime();
            PinState state = event.getState();
            if(state.isHigh()){
                start = wert;
            }else{
                stopp = wert;
                div = stopp - start;
                distanz = div * luft * 0.5E-7;
                start = 0d;
                stopp = 0d;
                ok= true;
            }
        }
    }    

    public static void main(String[] args) throws InterruptedException{
        final GpioController gpio = GpioFactory.getInstance();
        final GpioPinDigitalOutput trigger = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04);
        trigger.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        final GpioPinDigitalInput echo = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05,PinPullResistance.OFF);
        echo.addListener(new EchoListener());
        System.out.println("Listener");
        Thread.sleep(2000);
        while(true){
            ok = false;
            Thread.sleep(1000);
            while(echo.isHigh());
            trigger.high();
            // 40 µsec Pause
            Thread.sleep(0, 40000);
            trigger.low();
            Thread.sleep(30);
            if(ok){
                System.out.println("Distanz; "+distanz);
            }
        }

    }
}
