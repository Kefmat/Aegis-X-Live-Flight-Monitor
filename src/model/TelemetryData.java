package model;

public class TelemetryData {
    public double speed;
    public double altitude;
    public double temperature;

    public TelemetryData(double speed, double altitude, double temperature) {
        this.speed = speed;
        this.altitude = altitude;
        this.temperature = temperature;
    }

    @Override
    public String toString() {
        return String.format("Telemetri -> Fart: %.1f km/t, Høyde: %.1f m, Temp: %.1f°C", 
                              speed, altitude, temperature);
    }
}