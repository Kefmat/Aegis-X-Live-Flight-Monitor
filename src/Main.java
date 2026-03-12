import java.io.File;
import java.util.Scanner;
import engine.*;

/**
 * Hovedklassen for Aegis-X Ground Station.
 * Ansvarlig for oppstart av systemet, lasting av konfigurasjon og initiering av nettverkstjenester.
 */
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
        
        ConfigLoader config = new ConfigLoader();
        config.loadConfig("data/system_reqs.xml");
        LogManager logger = new LogManager("logs/ncr_report.csv");
        
        // Initialiserer Black Box for rådata-opptak
        BlackBoxProvider blackBox = new BlackBoxProvider("logs/flight_data.jsonl");

        // Shutdown Hook for automatisk generering av misjonsrapport ved avslutning
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SYSTEM] Avslutter... Genererer misjonsrapport.");
            ReportGenerator.generateMarkdownReport("logs/ncr_report.csv", "logs/mission_summary.md");
        }));

        // Starter mottakeren i en egen tråd - nå med Black Box og Remote Control støtte
        TelemetryReceiver receiver = new TelemetryReceiver(5000, config, logger, blackBox);
        Thread networkThread = new Thread(receiver);
        networkThread.start();

        // Interaktiv kontroll-loop
        Scanner scanner = new Scanner(System.in);
        boolean active = true;

        while (active) {
            String input = scanner.nextLine().trim();
            String command = input.toLowerCase();

            // Håndterer kommandoer med argumenter (som set-throttle)
            if (command.startsWith("set-throttle")) {
                try {
                    String[] parts = input.split(" ");
                    if (parts.length > 1) {
                        receiver.sendRemoteCommand("THROTTLE", parts[1]);
                    } else {
                        System.out.println("[USAGE] set-throttle <verdi>");
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Ugyldig throttle-format.");
                }
                continue;
            }

            switch (command) {
                case "stop":
                    receiver.stop();
                    active = false;
                    break;
                case "status":
                    System.out.println("[INFO] Systemet lytter på port 5000.");
                    break;
                case "reload":
                    config.loadConfig("data/system_reqs.xml");
                    System.out.println("[CONFIG] Krav oppdatert.");
                    break;
                case "abort":
                    receiver.triggerManualAbort();
                    break;
                case "analyze":
                    System.out.println("[SYSTEM] Initialiserer Post-Mission Analysis...");
                    FlightAnalyzer.analyze("logs/flight_data.jsonl");
                    break;
                case "help":
                    System.out.println("Kommandoer: stop, status, reload, abort, analyze, set-throttle <verdi>");
                    break;
                case "":
                    break;
                default:
                    System.out.println("[SYSTEM] Ukjent kommando. Skriv 'help' for oversikt.");
                    break;
            }
        }
        scanner.close();
    }
}