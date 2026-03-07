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
 * Dette gjor det mulig å endre operasjonelle parametere uten å rekompilere koden.
 */
public class ConfigLoader {
    public double minSpeed;
    public double maxTemp;

    /**
     * Leser en XML-fil og populerer grenseverdiene for systemet.
     * * @param filePath Stien til XML-filen som inneholder kravene.
     */
    public void loadConfig(String filePath) {
        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("requirement");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String id = eElement.getAttribute("id");
                    double value = Double.parseDouble(eElement.getElementsByTagName("value").item(0).getTextContent());

                    if (id.equals("REQ-NAV-01")) minSpeed = value;
                    if (id.equals("REQ-THERM-01")) maxTemp = value;
                }
            }
            System.out.println("[CONFIG] Grenseverdier lastet: Min Speed=" + minSpeed + ", Max Temp=" + maxTemp);
        } catch (Exception e) {
            System.err.println("[CONFIG ERROR] Kunne ikke lese XML: " + e.getMessage());
            minSpeed = 1800.0;
            maxTemp = 85.0;
        }
    }
}