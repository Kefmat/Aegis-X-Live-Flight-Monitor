package sim;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import model.FlightPhase;

/**
 * Simulerer et missil i flukt som sender telemetri og lytter etter FTS-kommandoer.
 */
public class MissileSimulator {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5000;
        
        double speed = 0.0;
        double altitude = 0.0;
        double temp = 15.0;
        FlightPhase currentPhase;

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(50); // Lytter uten å blokkere flyvningen
            InetAddress address = InetAddress.getByName(host);
            boolean flying = true;

            System.out.println("[SIMULATOR] Starter sekvens...");

            for (int i = 0; i < 200 && flying; i++) {
                // Simuleringslogikk
                if (i < 10) currentPhase = FlightPhase.PRE_LAUNCH;
                else if (i < 40) {
                    currentPhase = FlightPhase.BOOST;
                    speed += 115.5; altitude += 450.0; temp += 2.2;
                } else {
                    currentPhase = FlightPhase.SUSTAIN;
                    speed = 1850.0 + (Math.random() * 15);
                    altitude += 25.0; temp = 48.0 + (Math.random() * 1.5);
                }

                String message = String.format("PHS:%s;SPD:%.2f;ALT:%.2f;TEMP:%.2f", 
                                                currentPhase, speed, altitude, temp);
                
                byte[] buf = message.getBytes();
                socket.send(new DatagramPacket(buf, buf.length, address, port));

                // Sjekk etter kommandoer i retur
                try {
                    byte[] rBuf = new byte[1024];
                    DatagramPacket rPacket = new DatagramPacket(rBuf, rBuf.length);
                    socket.receive(rPacket);
                    if (new String(rPacket.getData(), 0, rPacket.getLength()).equals("CMD:TERMINATE")) {
                        System.out.println("\n[!!!] TERMINATION RECEIVED! SHUTTING DOWN.");
                        flying = false;
                    }
                } catch (SocketTimeoutException e) { /* Fortsett flyvning */ }

                Thread.sleep(500);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}