
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Wolfgang Höfer 
 * @version 1.0
 */
public class Ultraschall
{

    public double messen(GpioPinDigitalInput echo, GpioPinDigitalOutput trigger)throws InterruptedException{
        double start = 0d;
        double stopp = 0d;
        double div = 0d;
        double distanz = 0d;
        double luft = 343.0f;
        while(echo.isHigh());
        trigger.high();
        Thread.sleep(20);
        trigger.low();
        while(echo.isLow());
        start = System.nanoTime();
        while(echo.isHigh());
        stopp = System.nanoTime();
        div = stopp - start;
        distanz = div * luft * 0.5E-7;

        return distanz;
    }

    public static void main(String[] args) throws InterruptedException, IOException{
    	int count = 0;
    	DecimalFormat df2 = new DecimalFormat(".##");
    	final GpioController gpio = GpioFactory.getInstance();
        final GpioPinDigitalOutput trigger = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04);
        trigger.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF);
        final GpioPinDigitalInput echo = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05,PinPullResistance.PULL_DOWN);
        Ultraschall ultraschall = new Ultraschall();
        Thread.sleep(2000);
        
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
	        while(true){
	        	try {
                    Socket socket = serverSocket.accept();
                            
                    count++;
                    System.out.println("#"
                    + count + " from "
                            + socket.getInetAddress() + ":" 
                            + socket.getPort());
                    
                    Thread.sleep(2000);
                    
                    double distance = ultraschall.messen(echo, trigger);
                    
                    System.out.printf("%.2f %n", distance);
                    schreibeNachricht(socket, Double.parseDouble(df2.format(distance)));
                    
                    HostThread myHostThread = new HostThread(socket, count);
                    myHostThread.start();
                    
                } catch (IOException ex) {
                    System.out.println(ex.toString());
                }
	        }
        }catch (IOException ex) {
            System.out.println(ex.toString());
        }

    }
    
	private static void schreibeNachricht(Socket client, double distance) throws IOException{
    	//PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
    	//printWriter.print(color);
    	//printWriter.flush();
    	DataOutputStream dos = new DataOutputStream(client.getOutputStream());
    	dos.writeDouble(distance);
    }
    
    private static double leseNachricht(Socket client) throws IOException{
    	//BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
    	//int color = Integer.parseInt(bufferedReader.readLine());
    	DataInputStream dis = new DataInputStream(client.getInputStream());
    	double distance = dis.readDouble();
    	return distance;
    }
    
    private static class HostThread extends Thread{
        
        private Socket hostThreadSocket;
        int cnt;
        
        HostThread(Socket socket, int c){
            hostThreadSocket = socket;
            cnt = c;
        }

        @Override
        public void run() {

            OutputStream outputStream;
            try {
                outputStream = hostThreadSocket.getOutputStream();
                
                try (PrintStream printStream = new PrintStream(outputStream)) {
                        printStream.print(" :Hello from Raspberry Pi in background thread, you are #" + cnt);
                }
            } catch (IOException ex) {
                Logger.getLogger(Ultraschall.class.getName()).log(Level.SEVERE, null, ex);
            }finally{
                try {
                    hostThreadSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Ultraschall.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
