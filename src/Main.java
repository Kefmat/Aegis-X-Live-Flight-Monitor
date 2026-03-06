import java.io.File;
import engine.Parser;
import engine.TelemetryReceiver;
import model.TelemetryData;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   AEGIS-X: LIVE FLIGHT MONITOR         ");
        System.out.println("   [STATUS]: OPERATIONAL                ");
        System.out.println("========================================");

        // Verifiser konfigurasjon
        File reqFile = new File("data/system_reqs.xml");
        if (!reqFile.exists()) {
            System.err.println("[ERROR] Fant ikke system_reqs.xml!");
            return;
        }
        System.out.println("[CONFIG] Krav-database OK.");

        // Simulering av Live-data 
        // Simulerer en pakke som kommer inn via nettverket
        String mockIncoming = "SPD:1750.5;ALT:15000.0;TEMP:42.5";
        System.out.println("[NETWORK] Mottatt radata: " + mockIncoming);

        // Prosessering og Validering
        TelemetryData currentFlight = Parser.parseRawString(mockIncoming);

        if (currentFlight != null) {
            System.out.println("[SYSTEM] " + currentFlight.toString());
            
            // Hardkodet sjekk mot REQ-NAV-01
            if (currentFlight.speed < 1800) {
                System.out.println("----------------------------------------");
                System.out.println("VIOLATION: REQ-NAV-01 (Min Speed)");
                System.out.println("   Malt: " + currentFlight.speed + " km/t");
                System.out.println("   Krav: 1800.0 km/t");
                System.out.println("   ACTION: Triggering Flight Termination? NO (Dev Mode)");
                System.out.println("----------------------------------------");
            } else {
                System.out.println("[STATUS] All systems within nominal parameters.");
            }
        }

        System.out.println("[READY] Ground Station lytter... (Trykk Ctrl+C for a stoppe)");
        
        // Starter den faktiske nettverksmotoren
        TelemetryReceiver receiver = new TelemetryReceiver(5000);
        receiver.start();
    }
}