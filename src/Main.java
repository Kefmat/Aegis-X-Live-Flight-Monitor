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

        // Shutdown Hook for automatisk generering av misjonsrapport ved avslutning
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SYSTEM] Avslutter... Genererer misjonsrapport.");
            ReportGenerator.generateMarkdownReport("logs/ncr_report.csv", "logs/mission_summary.md");
        }));

        // Starter mottakeren i en egen tråd
        TelemetryReceiver receiver = new TelemetryReceiver(5000, config, logger);
        Thread networkThread = new Thread(receiver);
        networkThread.start();

        // Interaktiv kontroll-loop
        Scanner scanner = new Scanner(System.in);
        boolean active = true;

        while (active) {
            String command = scanner.nextLine().trim().toLowerCase();

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
                case "help":
                    System.out.println("Kommandoer: stop, status, reload, abort");
                    break;
            }
        }
        scanner.close();
    }
}