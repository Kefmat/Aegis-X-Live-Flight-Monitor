# Aegis-X: Live Flight Monitor 

Aegis-X er et prosjekt i grensesnittet mellom Systems Engineering og DevOps. Målet er å transformere statiske systemkrav til aktive overvåkingsregler for missilsystemer under flukt, støttet av robust datalogging og fjernstyring.

## Systemarkitektur (Rough Sketch)
**Systemet opererer nå som en fullverdig Command & Control (C2) stasjon med toveis kommunikasjon.**

- Missile Simulator (UDP Node): Simulerer telemetri og reagerer på innkommende kontroll-instrukser (Throttle/Terminate).

- Ground Station (Java Engine): Kjerne-systemet som validerer data, håndterer FTS (Flight Termination System) og visualiserer trender.

- Black Box (JSONL): Kontinuerlig rådata-opptak med nanosekund-presisjon for havariundersøkelser.

- PMA Tool (Post-Mission Analysis): Statistisk tolkning av flydata etter endt oppdrag.

<img width="1104" height="587" alt="excalidraw-skisse" src="https://github.com/user-attachments/assets/b0a11d41-d521-4674-867b-22cbd900303f" />

### Komponenter
* **Telemetry Receiver** Sanntidslytter med integrert ASCII Trend Graph for visuell overvåking.
* **Black Box Provider:** Sikrer data-persistens i et robust JSON-Lines format.
* **Requirement Engine (XML):** Sannhetskilden (Golden Rules) for operasjonelle parametere.
* **Flight Analyzer:** Verktøy for å beregne maks fart, høyde og gjennomsnittlig termisk belastning.

## DevOps & Prosjektfokus
* **V-Modell Automasjon:** Lukker gapet mellom design (krav) og operasjon (data).
* **Digital Engineering:** Bruk av digitale modeller for å sikre flyvesikkerhet.
* **NCR Reporting:** Automatisk generering av avviksrapporter og Markdown-oppsummeringer.

## Teknisk Oppsett

**Installasjon og kompilering**
For å bygge hele systemet inkludert analyse- og simulator-verktøy:
```
javac -d bin \
src/model/*.java \
src/engine/*.java \
src/sim/MissileSimulator.java \
src/Main.java
```

### Kjøring
**Start bakkestasjonen for å begynne overvåking:**
```
java -cp bin Main
```

**Start simulatoren:**
```
java -cp bin sim.MissileSimulator
```

**Nettverksprotokoll**
**Uplink (Port 5000):** Telemetri fra missil (Format: PHS:val;SPD:val;ALT:val;TEMP:val)

**Downlink (Retur):** Kommandoer til missil (Format: CMD:TERMINATE eller CMD:THROTTLE:val)

## Dokumentasjon
**Prosjektet bruker Javadoc for teknisk dokumentasjon. For å generere API-oversikten lokalt:**
```
javadoc -d docs/api -sourcepath src -subpackages engine:model:sim src/Main.java
```

## Kvalitetssikring

- **Git Hooks:** Lokal kompilering og validering av Black Box-stier før hver commit.

- **CI Pipeline:** GitHub Actions verifiserer bygg, Javadoc-generering og kildekode-integritet ved hver push.

- **Post-Mission Analysis:** Verifisering av flyvningen via kommandoen analyze i terminalen.