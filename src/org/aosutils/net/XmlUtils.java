package org.aosutils.net;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlUtils {
	public static Document newDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.newDocument();
	}
	public static void addTextElement(String name, String value, Element parent) {
		Element element = parent.getOwnerDocument().createElement(name);
		element.setTextContent(value);
		parent.appendChild(element);
	}
	public static String getTextElementValue(String elementName, Element parentElement, boolean showEmptyAsNull) {
		ArrayList<Node> childNodesWithTagName = getChildNodesWithTagName(elementName, parentElement);
		
		if (childNodesWithTagName.size() > 0) {
			Node childNode = childNodesWithTagName.get(0);
			String text = childNode.getTextContent().trim();
			
			return text.equals("") && showEmptyAsNull ? null : text;
		}
		
		return null;
	}
	public static ArrayList<Node> getChildNodesWithTagName(String tagName, Element parentElement) {
		NodeList childNodes = parentElement.getChildNodes();
		
		ArrayList<Node> childNodesWithTagName = new ArrayList<Node>();
		
		for (int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE && childNode.getNodeName().equals(tagName)) {
				childNodesWithTagName.add(childNode);
			}
		}
		
		return childNodesWithTagName;
	}
	
	public static String httpPost(String uri, Map<String, String> headers, Document xml, Integer httpTimeout) throws FileNotFoundException, MalformedURLException, IOException, TransformerException {
		xml.getDocumentElement().normalize();
		return HttpUtils.post(uri, headers, toString(xml), httpTimeout);
	}
	
	public static Document parseXml(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
		return parseXml(HttpUtils.getString(inputStream));
	}
	public static Document parseXml(String input) throws ParserConfigurationException, SAXException, IOException {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			try {
				dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			}
			 catch (ParserConfigurationException e) {
				 
			 }
			
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(new ByteArrayInputStream(input.getBytes()));
			doc.getDocumentElement().normalize();
			
			return doc;
		} catch (SAXParseException e) {
			throw new SAXParseException("ERROR parsing XML: " + input, null);
		}
	}
	
	public static String toString(Document xml) throws IOException, TransformerException {
	    Transformer transformer = TransformerFactory.newInstance().newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
	    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    // Indent
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	    
	    StringWriter stringWriter = new StringWriter();
	    transformer.transform(new DOMSource(xml), new StreamResult(stringWriter));
	    stringWriter.flush();
	    return stringWriter.toString();
	}
}
