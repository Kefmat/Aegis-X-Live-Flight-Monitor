package engine;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

/**
 * Ansvarlig for å laste systemkrav og grenseverdier fra XML-konfigurasjonsfiler.
 * Dette gjør det mulig å endre operasjonelle parametere uten å rekompilere koden.
 */
public class ConfigLoader {
    public double minSpeed;
    public double maxTemp;
    public double maxAltitude;
    public double maxX;
    public double maxY;

    /**
     * Leser en XML-fil og populerer grenseverdiene for systemet.
     * @param filePath Stien til XML-filen som inneholder kravene.
     */
    public void loadConfig(String filePath) {
        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Parser basert på ITEM-tagger i REQUIREMENT-SET
            NodeList nList = doc.getElementsByTagName("ITEM");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    
                    // Sjekker for de ulike grenseverdiene basert på tag-navn inni hvert ITEM
                    if (eElement.getElementsByTagName("MIN_SPEED").getLength() > 0) {
                        minSpeed = Double.parseDouble(eElement.getElementsByTagName("MIN_SPEED").item(0).getTextContent());
                    }
                    if (eElement.getElementsByTagName("MAX_ALTITUDE").getLength() > 0) {
                        maxAltitude = Double.parseDouble(eElement.getElementsByTagName("MAX_ALTITUDE").item(0).getTextContent());
                    }
                    if (eElement.getElementsByTagName("MAX_TEMP").getLength() > 0) {
                        maxTemp = Double.parseDouble(eElement.getElementsByTagName("MAX_TEMP").item(0).getTextContent());
                    }
                    if (eElement.getElementsByTagName("MAX_X").getLength() > 0) {
                        maxX = Double.parseDouble(eElement.getElementsByTagName("MAX_X").item(0).getTextContent());
                    }
                    if (eElement.getElementsByTagName("MAX_Y").getLength() > 0) {
                        maxY = Double.parseDouble(eElement.getElementsByTagName("MAX_Y").item(0).getTextContent());
                    }
                }
            }
            System.out.println("[CONFIG] Grenseverdier oppdatert fra XML.");
            System.out.println(String.format("         SPD: >%.0f | ALT: <%.0f | TEMP: <%.0f | GEOFENCE: %.0fx%.0f", 
                               minSpeed, maxAltitude, maxTemp, maxX, maxY));
                               
        } catch (Exception e) {
            System.err.println("[CONFIG ERROR] Kunne ikke lese XML: " + e.getMessage());
            // Robuste standardverdier (Fail-safe)
            minSpeed = 1800.0;
            maxAltitude = 25000.0;
            maxTemp = 85.0;
            maxX = 5000.0;
            maxY = 5000.0;
        }
    }
}