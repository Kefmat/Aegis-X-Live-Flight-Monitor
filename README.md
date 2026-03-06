# Aegis-X: Live Flight Monitor 

Aegis-X er et prosjekt i grensesnittet mellom Systems Engineering og DevOps. Målet er å transformere statiske systemkrav til aktive overvåkingsregler for missilsystemer under flukt.

## Systemarkitektur (Rough Sketch)
Dette konseptet er basert på sanntids verifikasjon av telemetri mot definerte terskelverdier i XML.

<img width="1104" height="587" alt="excalidraw-skisse" src="https://github.com/user-attachments/assets/b0a11d41-d521-4674-867b-22cbd900303f" />

### Komponenter
* **Missile Simulator (UDP Sender):** Simulerer telemetri-pakker (SPD, ALT, FUEL).
* **Ground Station (Java Backend):** Lytter på port 5000 og parser innkommende data.
* **Requirement Engine (XML):** Sannhetskilden (Golden Rules) for hva som er akseptabelt.
* **Live Dashboard:** Visualisering av Mission Status (Grønt/Rødt lys).

## DevOps & Prosjektfokus
* **V-Modell Automasjon:** Lukker gapet mellom design (krav) og operasjon (data).
* **Digital Engineering:** Bruk av digitale modeller for å sikre flyvesikkerhet.
* **NCR Reporting:** Automatisk generering av avviksrapporter ved kravbrudd.
