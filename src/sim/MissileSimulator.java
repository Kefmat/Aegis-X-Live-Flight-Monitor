package sim;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import model.FlightPhase;

/**
 * Simulerer et missil i flukt som sender telemetri og lytter etter kommandoer (FTS og Remote Control).
 */
public class MissileSimulator {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5000;
        
        double speed = 0.0;
        double altitude = 0.0;
        double temp = 15.0;
        double throttle = 1.0; // Standard kraft (100%)
        FlightPhase currentPhase;

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(100); 
            InetAddress address = InetAddress.getByName(host);
            boolean flying = true;

            System.out.println("[SIMULATOR] Systemer online. Venter på utskytning...");

            for (int i = 0; i < 500 && flying; i++) {
                // Bestemmer fase
                if (i < 10) currentPhase = FlightPhase.PRE_LAUNCH;
                else if (i < 60) currentPhase = FlightPhase.BOOST;
                else currentPhase = FlightPhase.SUSTAIN;

                // Simuleringslogikk påvirket av throttle
                if (currentPhase == FlightPhase.BOOST) {
                    speed += (115.5 * throttle);
                    altitude += (450.0 * throttle);
                    temp += (2.2 * throttle);
                } else if (currentPhase == FlightPhase.SUSTAIN) {
                    // Ved sustain prøver missilet å holde en stabil fart, men throttle endrer målet
                    double targetSpeed = 1850.0 * throttle;
                    speed += (targetSpeed - speed) * 0.1; 
                    altitude += 25.0;
                    temp = (48.0 * throttle) + (Math.random() * 1.5);
                }

                // Send telemetri
                String message = String.format("PHS:%s;SPD:%.2f;ALT:%.2f;TEMP:%.2f", 
                                                currentPhase, speed, altitude, temp);
                byte[] buf = message.getBytes();
                socket.send(new DatagramPacket(buf, buf.length, address, port));

                // LYTT etter retur-kommandoer (FTS eller Remote Config)
                try {
                    byte[] rBuf = new byte[1024];
                    DatagramPacket rPacket = new DatagramPacket(rBuf, rBuf.length);
                    socket.receive(rPacket);
                    
                    String receivedCmd = new String(rPacket.getData(), 0, rPacket.getLength());

                    if (receivedCmd.equals("CMD:TERMINATE")) {
                        System.out.println("\n[!!!] FTS AKTIVERT: Terminerer flyvning umiddelbart.");
                        flying = false;
                    } 
                    else if (receivedCmd.startsWith("CMD:THROTTLE:")) {
                        try {
                            throttle = Double.parseDouble(receivedCmd.split(":")[2]);
                            System.out.println("\n[REMOTE] Mottok ny throttle-verdi: " + throttle);
                        } catch (Exception e) {
                            System.err.println("[SIM ERROR] Ugyldig throttle-verdi mottatt.");
                        }
                    }
                } catch (SocketTimeoutException e) {
                }

                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("[SIMULATOR] Program avsluttet.");
    }
}