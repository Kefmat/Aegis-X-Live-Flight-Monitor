import java.io.File;
import engine.ConfigLoader;
import engine.LogManager;
import engine.Parser;
import engine.ReportGenerator;
import engine.TelemetryReceiver;
import model.TelemetryData;

/**
 * Hovedklassen for Aegis-X Ground Station.
 * Ansvarlig for oppstart av systemet, lasting av konfigurasjon og initiering av nettverkstjenester.
 */
public class Main {

    /**
     * Applikasjonens startpunkt som koordinerer systemoppstart og datamottak.
     * @param args Kommandolinjeargumenter (ikke i bruk).
     */
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
        
        // Initialiserer ConfigLoader for dynamiske grenseverdier
        ConfigLoader config = new ConfigLoader();
        config.loadConfig("data/system_reqs.xml");
        System.out.println("[CONFIG] Krav-database OK.");

        // Initialiserer NCR-logging (Non-Conformance Reports)
        LogManager logger = new LogManager("logs/ncr_report.csv");
        System.out.println("[LOG] NCR-rapportering aktiv: logs/ncr_report.csv");

        // Legger til Shutdown Hook for automatisk generering av misjonsrapport ved avslutning
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SYSTEM] Avslutter... Genererer misjonsrapport.");
            ReportGenerator.generateMarkdownReport("logs/ncr_report.csv", "logs/mission_summary.md");
        }));

        // Simulering av Live-data 
        // Simulerer en pakke som kommer inn via nettverket (Oppdatert med PHS-tag)
        String mockIncoming = "PHS:PRE_LAUNCH;SPD:1750.5;ALT:15000.0;TEMP:42.5";
        System.out.println("[NETWORK] Mottatt radata: " + mockIncoming);

        // Prosessering og Validering
        TelemetryData currentFlight = Parser.parseRawString(mockIncoming);

        if (currentFlight != null) {
            System.out.println("[SYSTEM] " + currentFlight.toString());
            
            // Hardkodet sjekk mot REQ-NAV-01 (Bruker verdier fra ConfigLoader)
            if (currentFlight.speed < config.minSpeed) {
                System.out.println("----------------------------------------");
                System.out.println("VIOLATION: REQ-NAV-01 (Min Speed)");
                System.out.println("   Malt: " + currentFlight.speed + " km/t");
                System.out.println("   Krav: " + config.minSpeed + " km/t");
                System.out.println("   ACTION: Triggering Flight Termination? NO (Dev Mode)");
                System.out.println("----------------------------------------");
                
                // Logger avviket til NCR-filen
                logger.logViolation("REQ-NAV-01", currentFlight.speed, config.minSpeed);
            } else {
                System.out.println("[STATUS] All systems within nominal parameters.");
            }
        }

        System.out.println("[READY] Ground Station lytter... (Trykk Ctrl+C for a stoppe)");
        
        // Starter den faktiske nettverksmotoren
        // Mottar port og konfigurasjonsobjekt for sanntidsovervaking
        TelemetryReceiver receiver = new TelemetryReceiver(5000, config, logger);
        receiver.start();
    }
}