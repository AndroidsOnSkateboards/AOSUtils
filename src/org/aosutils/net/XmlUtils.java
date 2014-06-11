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
import javax.xml.transform.TransformerException;

import org.aosutils.IoUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import android.backported.xml.DOMSource;
import android.backported.xml.StreamResult;
import android.backported.xml.Transformer;
import android.backported.xml.TransformerFactory;

public class XmlUtils {
	public static Document newDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.newDocument();
	}
	public static void addTextElement(String name, String value, Element parent) {
		Element element = parent.getOwnerDocument().createElement(name);
		element.appendChild(parent.getOwnerDocument().createTextNode(value));
		parent.appendChild(element);
	}
	public static String getTextElementValue(String elementName, Element parentElement, boolean showEmptyAsNull) {
		ArrayList<Node> childNodesWithTagName = getChildNodesWithTagName(elementName, parentElement);
		
		if (childNodesWithTagName.size() > 0) {
			Node childNode = childNodesWithTagName.get(0);
			String text = childNode.getFirstChild().getNodeValue().trim();
			
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
		return parseXml(IoUtils.getString(inputStream));
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
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	    
	    StringWriter stringWriter = new StringWriter();
	    transformer.transform(new DOMSource(xml), new StreamResult(stringWriter));
	    stringWriter.flush();
	    return stringWriter.toString();
	}
	
	public static String myToString(Document xml) {
		String output = "";
		
		ArrayList<String> lines = parseChild(xml.getDocumentElement());
		for (int i=0; i<lines.size(); i++) {
			output += lines.get(i);
			if (i+1<lines.size()) {
				output += "\n";
			}
		}
		
		return output;
	}
	
	private static ArrayList<String> parseChild(Element element) {
		ArrayList<String> attributeLines = processAttributes(element);
		NodeList childNodes = element.getChildNodes();
		
		ArrayList<String> output = new ArrayList<String>();
		
		
		if (attributeLines.size() == 0) {
			output.add("<" + element.getTagName() + (childNodes.getLength() == 0 ? "/>" : ">"));
		}
		else {
			output.add("<" + element.getTagName());
			
			for (String attributeLine : attributeLines) {
				output.add("\t" + attributeLine);
			}
			
			output.add(childNodes.getLength() == 0 ? "/>" : "\t>");
		}
		
		if (childNodes.getLength() > 0) {
			for (int i=0; i<childNodes.getLength(); i++) {
				Element child = (Element) childNodes.item(i);
				for (String childNodeLine : parseChild(child)) {
					output.add("\t" + childNodeLine);
				}
			}
			output.add("</" + element.getTagName() + ">");
		}
		
		return output;
	}
	
	private static ArrayList<String> processAttributes(Element element) {
		NamedNodeMap attributes = element.getAttributes();
		
		ArrayList<String> output = new ArrayList<String>();
		for (int i=0; i<attributes.getLength(); i++) {
			Node attribute = attributes.item(i);
			output.add(String.format("%s=\"%s\"",attribute.getNodeName(), attribute.getNodeValue()));
		}
		return output;
	}
}
