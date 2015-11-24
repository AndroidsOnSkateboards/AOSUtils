package org.aosutils.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.aosutils.AOSConstants;
import org.aosutils.IoUtils;
import org.aosutils.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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
		Element element = createTextElement(name, value, parent.getOwnerDocument());
		parent.appendChild(element);
	}
	private static Element createTextElement(String name, String value, Document document) {
		Element element = document.createElement(name);
		element.appendChild(document.createTextNode(value));
		return element;
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
	
	public static Document toXmlDocument(String rootElementName, Object object) throws ParserConfigurationException, SAXException {
		Document document = newDocument();
		Element rootElement = toXmlElement(rootElementName, object, document);
		document.appendChild(rootElement);
		document.normalize();
		
		return document;
	}
	private static Element toXmlElement(String elementName, Object object, Document document) throws SAXException {
		if (object instanceof Node) {
			throw new SAXException("Trying to build XML Node out of existing XML Node");
		}
		else if (elementName == null) {
			throw new SAXException("ElementName cannot be null");
		}
		else if (object instanceof Collection) {
			String elementNameSingular = elementName.endsWith("s") ? elementName.substring(0, elementName.length()-1) : elementName;
			String elementNamePlural = elementNameSingular + "s";
			
			Element element = document.createElement(elementNamePlural);
			for (Object item : (Collection<?>) object) {
				element.appendChild(toXmlElement(elementNameSingular, item, document));
			}
			return element;
		}
		else if (object instanceof Map) {
			Element element = document.createElement(elementName);
			for (Object key : ((Map<?, ?>) object).keySet()) {
				if (!(key instanceof String)) {
					throw new SAXException("All Maps must have Strings as keys.");
				}
				else {
					String name = (String) key;
					Object value = ((Map<?, ?>) object).get(key);
					Element child = toXmlElement(name, value, document);
					
						element.appendChild(child);
					
				}
			}
			return element;
		}
		else return createTextElement(elementName, object == null ? "" : object.toString(), document);
	}
	
	public static String toString(Document xml, boolean indent) {
		String indentStr = indent ? "\t" : null;
		
		String output = "<?xml version=\"1.0\" encoding=\"" + AOSConstants.CHARACTER_ENCODING + "\" standalone=\"yes\"?>" + "\n";
		
		ArrayList<String> lines = parseChild(xml.getDocumentElement(), indentStr);
		for (int i=0; i<lines.size(); i++) {
			output += lines.get(i);
			if (i+1<lines.size()) {
				output += "\n";
			}
		}
		
		return output;
	}
	
	private static ArrayList<String> parseChild(Element element, String indent) {
		ArrayList<String> attributeLines = processAttributes(element);
		NodeList childNodes = element.getChildNodes();
		
		ArrayList<String> output = new ArrayList<String>();
		
		if (attributeLines.size() == 0) {
			if (childNodes.getLength() == 1 && !(childNodes.item(0) instanceof Element)) {
				output.add("<" + xmlEscape(element.getTagName()) + ">" + xmlEscape(childNodes.item(0).getNodeValue()) + "</" + xmlEscape(element.getTagName()) + ">");
			}
			else {
				output.add("<" + xmlEscape(element.getTagName()) + (childNodes.getLength() == 0 ? "/>" : ">"));
			}
		}
		else {
			// Process Attributes
			output.add("<" + xmlEscape(element.getTagName()));
			
			for (String attributeLine : attributeLines) {
				output.add(indent + attributeLine);
			}
			
			output.add(childNodes.getLength() == 0 ? "/>" : "\t>\n");
		}
		
		// Process child nodes
		if (childNodes.getLength() >= 2 || (childNodes.getLength() == 1 && (childNodes.item(0) instanceof Element || attributeLines.size() > 0))) {
			for (int i=0; i<childNodes.getLength(); i++) {
				Element child = (Element) childNodes.item(i);
				for (String childNodeLine : parseChild((Element) child, indent)) {
					output.add(indent + childNodeLine);
				}
			}
			output.add("</" + xmlEscape(element.getTagName()) + ">");
		
		}
		
		if (indent == null) {
			// Join all together in one line
			String joined = StringUtils.join(output, "");
			output.clear();
			output.add(joined);
		}
		
		return output;
	}
	
	private static ArrayList<String> processAttributes(Element element) {
		NamedNodeMap attributes = element.getAttributes();
		
		ArrayList<String> output = new ArrayList<String>();
		for (int i=0; i<attributes.getLength(); i++) {
			Node attribute = attributes.item(i);
			output.add(String.format("%s=\"%s\"", xmlEscape(attribute.getNodeName()), xmlEscape(attribute.getNodeValue())));
		}
		return output;
	}
	
	private static String xmlEscape(String string) {
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<string.length(); i++) {
			char c = string.charAt(i);
			builder.append(
				c == '"' ? "&quot;" :
				c == '\'' ? "&apos;" :
				c == '<' ? "&lt;" :
				c == '>' ? "&gt;" :
				c == '&' ? "&amp;" :
				c);
		}
		return builder.toString();
	}
}
