import java.io.File;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   AEGIS-X: LIVE FLIGHT MONITOR       ");
        System.out.println("   Status: Oppstart fase 2...          ");
        System.out.println("========================================");

        File reqFile = new File("data/system_reqs.xml");
        if (reqFile.exists()) {
            System.out.println("[CONFIG] Lastet systemkrav fra: " + reqFile.getName());
        } else {
            System.err.println("[ERROR] Fant ikke system_reqs.xml! Avslutter...");
            return;
        }

        System.out.println("[SYSTEM] Initialiserer TelemetryReceiver på port 5000...");
        
        System.out.println("[READY] Ground Station er aktiv. Venter på missildata...");
    }
}