package engine;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Genererer en formell oppsummeringsrapport i Markdown-format
 * basert på registrerte avvik i CSV-loggen.
 */
public class ReportGenerator {

    public static void generateMarkdownReport(String csvPath, String reportPath) {
        List<String> violations = new ArrayList<>();
        int count = 0;

        try {
            if (!Files.exists(Paths.get(csvPath))) {
                System.out.println("[REPORT] Ingen loggfil funnet. Hopper over rapport.");
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(csvPath));
            String line;
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                violations.add(line);
                count++;
            }
            reader.close();

            PrintWriter writer = new PrintWriter(new FileWriter(reportPath));
            writer.println("# AEGIS-X MISSION CONFORMANCE REPORT");
            writer.println("## Status: " + (count > 0 ? "FAILED" : "SUCCESS"));
            writer.println("\n**Dato:** " + new Date());
            writer.println("**Totalt antall avvik:** " + count);
            writer.println("\n### Detaljerte avvik");
            
            if (count == 0) {
                writer.println("Ingen kritiske avvik registrert under flyvningen.");
            } else {
                writer.println("| Tidspunkt | Krav-ID | Målt Verdi | Grense |");
                writer.println("|-----------|----------|------------|--------|");
                for (String v : violations) {
                    String[] p = v.split(",");
                    writer.printf("| %s | %s | %s | %s |\n", p[0], p[1], p[2], p[3]);
                }
            }

            writer.println("\n---\n*Generert automatisk av Aegis-X Ground Station*");
            writer.close();
            System.out.println("[REPORT] Rapport generert: " + reportPath);

        } catch (IOException e) {
            System.err.println("[REPORT ERROR] Kunne ikke generere rapport: " + e.getMessage());
        }
    }
}