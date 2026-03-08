package engine;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ansvarlig for permanent lagring av avvik
 * Skriver hendelser til en CSV-fil for senere analyse.
 */
public class LogManager {
    private String logFile;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(String fileName) {
        this.logFile = fileName;
        initializeLog();
    }

    private void initializeLog() {
        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            // Skriver headere hvis filen er tom
            out.println("Timestamp,Requirement_ID,Measured_Value,Limit,Status");
        } catch (IOException e) {
            System.err.println("[LOG ERROR] Kunne ikke initialisere loggfil: " + e.getMessage());
        }
    }

    /**
     * Logger et avvik til filen.
     * @param reqId ID-en til kravet som ble brutt.
     * @param value Den målte verdien.
     * @param limit Grenseverdien som ble overskredet.
     */
    public void logViolation(String reqId, double value, double limit) {
        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = dtf.format(LocalDateTime.now());
            out.printf("%s,%s,%.2f,%.2f,VIOLATION\n", timestamp, reqId, value, limit);
        } catch (IOException e) {
            System.err.println("[LOG ERROR] Kunne ikke skrive til logg: " + e.getMessage());
        }
    }
}