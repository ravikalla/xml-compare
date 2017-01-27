package in.ravikalla.xml_compare.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ConvertXMLToFullPathInCSV {
	private final static Logger logger = Logger.getLogger(ConvertXMLToFullPathInCSV.class);
	public static List<String> getFirstLevelOfReapeatingElements(String strXML1, String strXML2) {
		List<Node> lstResultNodes = null;
		List<String> lstResultNodeNames_Temp = new ArrayList<String>();
		List<String> lstResultNodeNames = new ArrayList<String>();
		try {
			// Create document
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			Document document;
			InputStream is = null;

			is = new ByteArrayInputStream(strXML1.getBytes());
			document = domFactory.newDocumentBuilder().parse(is);

			// List of all nodes containing a value
			document.getDocumentElement().normalize();
			lstResultNodes = getFirstRepeatingAndLeafNodes(document);
			// Get list of names from list of nodes
			for (Node objNode : lstResultNodes) {
				StringBuilder strNodePath = new StringBuilder(objNode.getNodeName());
				// Traverse all parents and prepend their names to path
				objNode = objNode.getParentNode();
				while (objNode.getNodeType() != Node.DOCUMENT_NODE) {
					strNodePath.insert(0,  objNode.getNodeName() + '/');
					objNode = objNode.getParentNode();
				}
				strNodePath.insert(0, '/');

				lstResultNodeNames_Temp = addStringToListIfNotAdded(strNodePath.toString(), lstResultNodeNames_Temp);
			}
			is = new ByteArrayInputStream(strXML2.getBytes());
			document= domFactory.newDocumentBuilder().parse(is);

			// List of all nodes containing a value
			document.getDocumentElement().normalize();
			lstResultNodes = getFirstRepeatingAndLeafNodes(document);

			// Get list of names from list of Nodes
			for (Node objNode : lstResultNodes) {
				StringBuilder strNodePath = new StringBuilder(objNode.getNodeName());
				// Traverse all parents and prepend their names to path
				objNode = objNode.getParentNode();
				while(objNode.getNodeType() != Node.DOCUMENT_NODE) {
					strNodePath.insert(0,  objNode.getNodeName() + '/');
					objNode = objNode.getParentNode();
				}
				strNodePath.insert(0,  '/');

				lstResultNodeNames_Temp = addStringToListIfNotAdded(strNodePath.toString(), lstResultNodeNames_Temp);
			}
			// Convert list to a string with a separator
			for (int i=0; i < lstResultNodeNames_Temp.size(); i++) {
				lstResultNodeNames.add(lstResultNodeNames_Temp.get(i));
			}
		} catch (SAXException e) {
			logger.error("72 : ConvertXMLToFullPathInCSV.getFirstLevelOfReapeatingElements(...)"
					+ " : SAXException e : " + e);
		} catch (IOException e) {
			logger.error("75 : ConvertXMLToFullPathInCSV.getFirstLevelOfReapeatingElements(...)"
					+ " : IOException e : " + e);
		} catch (ParserConfigurationException e) {
			logger.error("80 : ConvertXMLToFullPathInCSV.getFirstLevelOfReapeatingElements(...)"
					+ " : ParserConfigurationException e : " + e);
		}
		return lstResultNodeNames;
	}

	private static List<String> addStringToListIfNotAdded(String strToCheck, List<String> lst) {
		int intElementPosToReplace = -1;
		boolean blnExists = false;
		for (int i=0; i < lst.size(); i++) {
			if (lst.get(0).indexOf(strToCheck) == 0) {
				blnExists = true;
				if (lst.get(i).length() > strToCheck.length()) {
					intElementPosToReplace = i;
				}
				break;
			}
		}
		if (-1 != intElementPosToReplace) {
			lst.remove(intElementPosToReplace);
			if (!blnExists)
				lst.add(strToCheck);
		}
		else if (!blnExists)
			lst.add(strToCheck);
		return lst;
	}

	private static List<Node> getFirstRepeatingAndLeafNodes(Node node) {
		NodeList lstChildren = node.getChildNodes();
		List<Node> lstRepeatingNodes = getRepeatedNodes(lstChildren);
		List<Node> lstNonRepeatingNodes = getNonRepeatedNodes(lstChildren, lstRepeatingNodes);
		List<Node> lstNonRepeatingLeafNodes = getNonRepeatingLeafNodes(lstNonRepeatingNodes);
		List<Node> lstRepeatingAndLeafNodes = new ArrayList<Node>();
		lstRepeatingAndLeafNodes.addAll(lstRepeatingNodes);
		lstRepeatingAndLeafNodes.addAll(lstNonRepeatingLeafNodes);
		List<Node> lstNonRepeatingWithoutLeafNodes = getNonRepeatingWithoutLeafNodes(lstNonRepeatingNodes, lstNonRepeatingLeafNodes);

		for (Node objNonRepeatingNonLeafNode : lstNonRepeatingWithoutLeafNodes) {
			lstRepeatingAndLeafNodes.addAll(getFirstRepeatingAndLeafNodes(objNonRepeatingNonLeafNode));
		}
		return lstRepeatingAndLeafNodes;
	}

	private static List<Node> getNonRepeatingWithoutLeafNodes(List<Node> lstNonRepeatingNodes,
			List<Node> lstNonRepeatingLeafNodes) {
		List<Node> lstNonRepeatingWithoutLeafNodes = new ArrayList<Node>();
		boolean blnNodeIsLeaf;
		for (Node objNonRepeatingNode : lstNonRepeatingNodes) {
			blnNodeIsLeaf = false;
			for (Node objNonRepeatingLeafNode : lstNonRepeatingLeafNodes) {
				if (objNonRepeatingNode.getNodeName().equals(objNonRepeatingLeafNode.getNodeName())) {
					blnNodeIsLeaf = true;
					break;
				}
			}
			if (!blnNodeIsLeaf)
				lstNonRepeatingWithoutLeafNodes.add(objNonRepeatingNode);
		}
		return lstNonRepeatingWithoutLeafNodes;
	}

	private static List<Node> getNonRepeatingLeafNodes(List<Node> lstNonRepeatingNodes) {
		List<Node> lstNonRepeatingLeafNode = new ArrayList<Node> ();
		for (Node objNonRepeatingNode : lstNonRepeatingNodes) {
			if (isLeafNode(objNonRepeatingNode)) {
				lstNonRepeatingLeafNode.add(objNonRepeatingNode);
			}
		}
		return lstNonRepeatingLeafNode;
	}

	private static boolean isLeafNode(Node objNonRepeatingNode) {
		NodeList lst = objNonRepeatingNode.getChildNodes();
		return ((lst.getLength() == 1) && (lst.item(0).getNodeType() == Node.TEXT_NODE));
	}

	private static List<Node> getNonRepeatedNodes(NodeList objNodeList, List<Node> lstRepeatingNodes) {
		List<Node> lstNonRepeatedNodes = new ArrayList<Node>();
		Node objNodeFromEntireList= null;
		boolean blnNodeExistInRepeatingList;
		for (int intNodeList_Ctr = 0; intNodeList_Ctr < objNodeList.getLength(); intNodeList_Ctr++) {
			objNodeFromEntireList = objNodeList.item(intNodeList_Ctr);
			if (objNodeFromEntireList.getNodeType() == Node.ELEMENT_NODE) {
				blnNodeExistInRepeatingList = false;
				for (Node objNodeFromRepeatingList : lstRepeatingNodes) {
					if (objNodeFromEntireList.getNodeName().equals(objNodeFromRepeatingList.getNodeName())) {
						blnNodeExistInRepeatingList = true;
						break;
					}
				}
				if (!blnNodeExistInRepeatingList)
					lstNonRepeatedNodes.add(objNodeFromEntireList);
			}
		}
		return lstNonRepeatedNodes;
	}

	private static List<Node> getRepeatedNodes(NodeList lstChildren) {
		List<Node> lstUnionOfNodes = new ArrayList<Node>();
		List<Node> lstRepeatingNodes = new ArrayList<Node>();
		Node objNode;
		String strNodeName_Temp;
		for (int i=0; i < lstChildren.getLength(); i++) {
			objNode = lstChildren.item(i);
			if (objNode.getNodeType() == Node.ELEMENT_NODE) {
				strNodeName_Temp = objNode.getNodeName();
				if (nodeExistsInList(strNodeName_Temp, lstUnionOfNodes))
					lstRepeatingNodes.add(objNode);
				else
					lstUnionOfNodes.add(objNode);
			}
		}
		return lstRepeatingNodes;
	}

	private static boolean nodeExistsInList(String strNodeName_Temp, List<Node> lstUnionOfNodes) {
		boolean nodeExistsInList = false;
		for (Node objNode_Temp : lstUnionOfNodes) {
			if (objNode_Temp.getNodeName().equals(strNodeName_Temp)) {
				nodeExistsInList = true;
				break;
			}
		}
		return nodeExistsInList;
	}
}
