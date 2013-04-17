package dk.aamj.itu.plugin.view.option3;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class dummy {

	public static void main(String[] argv) {
		String send2printer = "send2printer";
		readXml(send2printer);
	}

	public static String readXml(String expectedStr) {
		try {

			File fXmlFile = new File("intent/intents.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("intent");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					String intentNames = eElement.getAttribute("name");

					if (intentNames.equals(expectedStr)) {
						String dataField = eElement
								.getElementsByTagName("data").item(0)
								.getTextContent();
						String intentName = "Intent i = new Intent(\""
								+ eElement.getAttribute("name") + "\")";

						intentName += "\n i.putExtra(\"TODO\", extraValue)";

						if (!dataField.isEmpty()) {
							intentName += "\n i.setData(" + dataField + ")";
						}
						return intentNames;
					}

				}
			}
			return null;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
