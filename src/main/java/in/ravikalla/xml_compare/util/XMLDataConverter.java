package in.ravikalla.xml_compare.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import in.ravikalla.xml_compare.dto.SimpleNamespaceContext;
import in.ravikalla.xml_compare.dto.XMLToXMLComparisonResultsHolderDTO;

public class XMLDataConverter {
	private final static Logger logger = Logger.getLogger(XMLDataConverter.class);
	public static DocumentBuilder builder = null;

	public static XMLToXMLComparisonResultsHolderDTO compXPathEleDataWithChildEle(String xmlStr1,
			String xmlStr2, String strIterativeElement, List<String> lstElementsToExclude, String strPrimaryNodeXMLElementName,
			String strTrimElements) throws SAXException, IOException, ParserConfigurationException {
		String strTrimElements_Local = strTrimElements;
		logger.debug("Start : XMLDataConverter.compareXPathElementsDataWithChildElements(...)");
		XMLToXMLComparisonResultsHolderDTO objXMLToXMLComparisonResultsHolderDTO = new XMLToXMLComparisonResultsHolderDTO();

		List<String> lstMissmatchedDataForCSV = new ArrayList<String>();
		List<String> lstMatchedDataForCSV = new ArrayList<String>();
		Map<Integer, Integer> mapMatchedNodePositions = new HashMap<Integer, Integer> ();
		NodeList nodeListRoot1 = null;
		NodeList nodeListRoot2 = null;

		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath1 = xpathFactory.newXPath();
		XPath xpath2 = xpathFactory.newXPath();

		DocumentBuilder builder = XMLDataConverter.getDocumentBuilder();
		Document doc1 = builder.parse(new InputSource(new StringReader(xmlStr1)));
		Document doc2 = builder.parse(new InputSource(new StringReader(xmlStr2)));

		doc1.getDocumentElement().normalize();
		doc2.getDocumentElement().normalize();

		// Compare the data by ignoreCaseSensitive
		Map<String, String> mapCaseSensitiveValues = new HashMap<String, String>();
		String strCaseSensitiveValues = "";
		try {
			
			if (null != strTrimElements_Local) {
				String[] arrTrimElements = strTrimElements_Local.split("&&");
				if (arrTrimElements.length > 1) {
					strTrimElements_Local = arrTrimElements[0];
					strCaseSensitiveValues = arrTrimElements[1];
				}
				if (null != strTrimElements_Local && strTrimElements_Local.equals(""))
					strTrimElements_Local = null;
				mapCaseSensitiveValues = Util_XMLConvert.convertTrimmableElementsToMap(strCaseSensitiveValues, ";", ",");

				logger.debug("Map size for mapCaseSensitiveValues : " + mapCaseSensitiveValues.size());
			}
		} catch(Exception ex) {
			logger.error("62 : XMLDataConverter.compareXPathElementsDataWithChildElements(...) : ex : " + ex);
		}
		// End : Compare the data by ignore case sensitive

		if (null != strTrimElements_Local && !strTrimElements_Local.trim().equals("") && strTrimElements_Local.split(",")[1].equals(";"))
			strTrimElements_Local = null;

		Map<String, String> mapTrimElements = Util_XMLConvert.convertTrimmableElementsToMap(strTrimElements_Local, ";", ",");
		NodeList lstChildNodeList1 = doc1.getChildNodes();
		Util_XMLConvert.trimValueIfApplicable(lstChildNodeList1, mapTrimElements);
		NodeList lstChildNodeList2 = doc2.getChildNodes();
		Util_XMLConvert.trimValueIfApplicable(lstChildNodeList2, mapTrimElements);
		Map<String, String> prefMap1 = getAttributeMap(doc1);
		SimpleNamespaceContext namespaces1 = new SimpleNamespaceContext(prefMap1);
		xpath1.setNamespaceContext(namespaces1);
		Map<String, String> prefMap2 = getAttributeMap(doc2);
		SimpleNamespaceContext namespaces2 = new SimpleNamespaceContext(prefMap2);
		xpath2.setNamespaceContext(namespaces2);

		try {
			// Get repeatable elements count
			XPathExpression expr1 = xpath1.compile("count(" + strIterativeElement + ")");
			int intElementCount1 = ((Double) expr1.evaluate(doc1,  XPathConstants.NUMBER)).intValue();
			XPathExpression expr2 = xpath2.compile("count(" + strIterativeElement + ")");
			int intElementCount2 = ((Double) expr2.evaluate(doc2,  XPathConstants.NUMBER)).intValue();

			logger.debug("92 : " + strIterativeElement + " : " + intElementCount1 + " : " + intElementCount2);

			expr2 = xpath2.compile(strIterativeElement);
			nodeListRoot2 = (NodeList) expr2.evaluate(doc2,  XPathConstants.NODESET);
			logger.debug("96 : " + nodeListRoot2.getLength());

			// Read content from XML1
			getIterativeElementOccurences(strIterativeElement, lstElementsToExclude, mapMatchedNodePositions,
					nodeListRoot2, xpath1, doc1, mapCaseSensitiveValues, intElementCount1);
			expr1 = xpath1.compile(strIterativeElement);
			nodeListRoot1 = (NodeList) expr1.evaluate(doc1,  XPathConstants.NODESET);

			// Find the mismatched node positions in XML1 and XML2 from the matched positions
			getMatchedAndMismatchedDataForCSV(lstElementsToExclude, objXMLToXMLComparisonResultsHolderDTO,
					lstMissmatchedDataForCSV, lstMatchedDataForCSV, mapMatchedNodePositions, nodeListRoot1,
					nodeListRoot2, intElementCount1, intElementCount2);
		} catch (XPathExpressionException e) {
			logger.error("140 : XMLDataConverter.compareXPathElementsDataWithChildElements(...) : XPathExpressionException e : " + e);
		}

		logger.debug("End : XMLDataConverter.compareXPathElementsDataWithChildElements(...)");
		return objXMLToXMLComparisonResultsHolderDTO;
	}

	private static void getMatchedAndMismatchedDataForCSV(List<String> lstElementsToExclude,
			XMLToXMLComparisonResultsHolderDTO objXMLToXMLComparisonResultsHolderDTO,
			List<String> lstMissmatchedDataForCSV, List<String> lstMatchedDataForCSV,
			Map<Integer, Integer> mapMatchedNodePositions, NodeList nodeListRoot1, NodeList nodeListRoot2,
			int intElementCount1, int intElementCount2) {
		List<String> lstMatchedElementPositions_ColonSeparated = findMatchedNodePositionsInXML1AndXML2(mapMatchedNodePositions, intElementCount1);
		List<String> lstMismatchedElementPositions_ColonSeparated = findMismatchedNodePositionsInXML1AndXML2(mapMatchedNodePositions, intElementCount1, intElementCount2);
		List<String> lstNodeInformation_Temp = null;
		for (String strMismatchedPosition_ColumnSeparated : lstMismatchedElementPositions_ColonSeparated) {
			lstNodeInformation_Temp = getNodeInformationForMismatchedData(strMismatchedPosition_ColumnSeparated, nodeListRoot1, nodeListRoot2, lstElementsToExclude);
			if (null != lstNodeInformation_Temp && lstNodeInformation_Temp.size() > 0) {
				lstMissmatchedDataForCSV.add(",,,"); // Adding empty row before showing next set of nodes
				lstMissmatchedDataForCSV.addAll(lstNodeInformation_Temp);
			}
		}
		for (String strMatchedPosition_ColSeparated : lstMatchedElementPositions_ColonSeparated) {
			lstNodeInformation_Temp = getNodeInformationForMatchedData(strMatchedPosition_ColSeparated, nodeListRoot1);
			if (null != lstNodeInformation_Temp && lstNodeInformation_Temp.size() > 0) {
				lstMatchedDataForCSV.add(",,,"); // Adding empty row before showing next set of nodes
				lstMatchedDataForCSV.addAll(lstNodeInformation_Temp);
			}
		}
		objXMLToXMLComparisonResultsHolderDTO.lstMatchedDataForCSV = lstMatchedDataForCSV;
		objXMLToXMLComparisonResultsHolderDTO.lstMismatchedDataForCSV = lstMissmatchedDataForCSV;
	}

	private static void getIterativeElementOccurences(String strIterativeElement, List<String> lstElementsToExclude,
			Map<Integer, Integer> mapMatchedNodePositions, NodeList nodeListRoot2, XPath xpath1, Document doc1,
			Map<String, String> mapCaseSensitiveValues, int intElementCount1) throws XPathExpressionException {
		int intTempNode2Position;
		NodeList nodeListRoot1;
		Node node1;
		XPathExpression expr1;
		for (int intElementCtr1 = 0; intElementCtr1 < intElementCount1; intElementCtr1++) {

			// Idetify element from first XML
			expr1 = xpath1.compile(strIterativeElement + "[" + (intElementCtr1 + 1) + "]");
			nodeListRoot1 = (NodeList) expr1.evaluate(doc1,  XPathConstants.NODESET);
			node1 = null;
			if (nodeListRoot1.getLength() > 0)
				node1 = nodeListRoot1.item(0);
			intTempNode2Position = -1;
			if (null != node1) {
				if (eligibleNodeForValidation(node1, lstElementsToExclude)) {
					intTempNode2Position = getPositionOfMatchingNodeFromList(node1, nodeListRoot2, mapMatchedNodePositions, lstElementsToExclude, mapCaseSensitiveValues);
				}
				if (-1 != intTempNode2Position) {
					mapMatchedNodePositions.put(new Integer(intElementCtr1), new Integer(intTempNode2Position));
				}
			}
		}
	}

	private static List<String> getNodeInformationForMatchedData(String strMatchedPosition_ColSeparated,
			NodeList nodeListRoot1) {
		List<String> lstResult = new ArrayList<String>();
		List<String> lstNode1Data = new ArrayList<String>();
		int intElePos1 = -1;

//		Find data for XML1
		if (!strMatchedPosition_ColSeparated.trim().equals("")) {
			intElePos1 = Integer.parseInt(strMatchedPosition_ColSeparated.trim());
			lstNode1Data.addAll(getListOfCSVDataRows(nodeListRoot1.item(intElePos1)));
		}
//		Merge data rows into CSV format
		lstResult = mergeMatchedListsInCSVFormat(lstNode1Data);
		return lstResult;
	}

	private static List<String> getNodeInformationForMismatchedData(String strMismatchedPosition_ColumnSeparated,
			NodeList nodeListRoot1, NodeList nodeListRoot2, List<String> lstElementsToExclude) {
		List<String> lstResult = new ArrayList<String>();
		List<String> lstNode1Data = new ArrayList<String>();
		List<String> lstNode2Data = new ArrayList<String>();
		String[] arrMismatchedPositions = strMismatchedPosition_ColumnSeparated.split(":");
		int intElePos1 = -1;
		int intElePos2 = -1;

//		Find data for XML1
		if (!arrMismatchedPositions[0].trim().equals("")) {
			intElePos1 = Integer.parseInt(arrMismatchedPositions[0].trim());
			if (eligibleNodeForValidation(nodeListRoot1.item(intElePos1), lstElementsToExclude))
				lstNode1Data.addAll(getListOfCSVDataRows(nodeListRoot1.item(intElePos1)));
		}
//		Find data for XML2
		if (!arrMismatchedPositions[1].trim().equals("")) {
			intElePos2 = Integer.parseInt(arrMismatchedPositions[1].trim());
			if (eligibleNodeForValidation(nodeListRoot2.item(intElePos2), lstElementsToExclude))
				lstNode2Data.addAll(getListOfCSVDataRows(nodeListRoot2.item(intElePos2)));
		}
//		Merge data rows into CSV format
		lstResult = mergeMismatchedListsInCSVFormat(lstNode1Data, lstNode2Data);
		return lstResult;
	}

	private static List<String> mergeMismatchedListsInCSVFormat(List<String> lstNode1Data, List<String> lstNode2Data) {
		List<String> lstResults = new ArrayList<String>();
		int intLst1Size = lstNode1Data.size();
		int intLst2Size = lstNode2Data.size();
		for (int i = 0; i < intLst1Size; i++)
			lstResults.add(lstNode1Data.get(i) + ", , ");
		for (int i = 0; i < intLst2Size; i++)
			lstResults.add(" , ," + lstNode2Data.get(i));
		return lstResults;
	}
	private static List<String> mergeMatchedListsInCSVFormat(List<String> lstNode1Data) {
		List<String> lstResults = new ArrayList<String>();
		int intLst1Size = lstNode1Data.size();
		String str1 = null;
//		String str2 = null;
//		str2 = " , ";
		for (int i = 0; i < intLst1Size; i++) {
			str1 = lstNode1Data.get(i);
			String[] strArray = str1.split(",");
			if (strArray.length > 1)
				lstResults.add(str1 + "," + strArray[1]);
			else
				lstResults.add(str1 + "," + "");
		}
		return lstResults;
	}

	private static Collection<? extends String> getListOfCSVDataRows(Node objNode) {
		List<String> lstDataRows = new ArrayList<String>();
		if (isSimpleElement(objNode))
			lstDataRows.add(getCompletePathAndDataInCSVFormat(objNode));
		else { // If it is a complex element
			NodeList lstChildNodes = objNode.getChildNodes();
			for (int i=0; i < lstChildNodes.getLength(); i++) {
				if (lstChildNodes.item(i).getNodeType() != Node.TEXT_NODE) { // Eliminating blank spaces between child elements of a node
					lstDataRows.addAll(getListOfCSVDataRows(lstChildNodes.item(i)));
				}
			}
		}
		return lstDataRows;
	}

	private static String getCompletePathAndDataInCSVFormat(Node objNode) {
		String strCompletepathAndDataInCSVFormat = null;
		String strData = objNode.getTextContent();
		String strCompletePath = getCompletePathForANode(objNode);
		strCompletepathAndDataInCSVFormat = strCompletePath + "," + strData;
		return strCompletepathAndDataInCSVFormat;
	}

	private static boolean isSimpleElement(Node objNode) {
		NodeList lstChildNodes = objNode.getChildNodes();
		boolean isSimpleElement = true;
		for (int i=0; i < lstChildNodes.getLength(); i++) {
			if (lstChildNodes.item(i).getNodeType() != Node.TEXT_NODE) { // Eliminating blank spaces between child elements of a node
				isSimpleElement = false;
			}
		}
		return isSimpleElement;
	}

	private static List<String> findMismatchedNodePositionsInXML1AndXML2(Map<Integer, Integer> mapMatchedNodePositions,
			int intElementCount1, int intElementCount2) {
		List<String> lstMismatchedNodePositionsInXML1AndXML2 = new ArrayList<String>();
		List<String> lstMismatchedPositionsInXML1 = new ArrayList<String>();
		List<String> lstMismatchedPositionsInXML2 = new ArrayList<String>();
		String strPosition1 = null;
		String strPosition2 = null;

		for (int intTempPos = 0; intTempPos < intElementCount1 || intTempPos < intElementCount2; intTempPos++) {
			boolean blnElementFoundInXML1 = false;
			boolean blnElementFoundInXML2 = false;
			for (Map.Entry<Integer, Integer> objEntry : mapMatchedNodePositions.entrySet()) {
				if (objEntry.getKey().intValue() == intTempPos)
					blnElementFoundInXML1 = true;
				if (objEntry.getValue().intValue() == intTempPos)
					blnElementFoundInXML2 = true;
			}
			if ((!blnElementFoundInXML1) && (intTempPos < intElementCount1))
					lstMismatchedPositionsInXML1.add((new Integer(intTempPos)).toString());
			if ((!blnElementFoundInXML2) && (intTempPos < intElementCount2))
					lstMismatchedPositionsInXML2.add((new Integer(intTempPos)).toString());
		}
		for (int intTempPos = 0; intTempPos < lstMismatchedPositionsInXML1.size(); intTempPos++) {
			strPosition1 = lstMismatchedPositionsInXML1.get(intTempPos);
			lstMismatchedNodePositionsInXML1AndXML2.add(strPosition1 + ": ");
		}
		for (int intTempPos = 0; intTempPos < lstMismatchedPositionsInXML2.size(); intTempPos++) {
			strPosition2 = lstMismatchedPositionsInXML2.get(intTempPos);
			lstMismatchedNodePositionsInXML1AndXML2.add(" :" + strPosition2);
		}
		return lstMismatchedNodePositionsInXML1AndXML2;
	}

	private static List<String> findMatchedNodePositionsInXML1AndXML2(Map<Integer, Integer> mapMatchedNodePositions,
			int intElementCount1) {
		List<String> lstMatchedNodePositionsInXML1 = new ArrayList<String>();
		List<String> lstMatchedPositionsInXML1 = new ArrayList<String>();
		String strPosition1 = null;
		for (int intTempPos = 0; intTempPos < intElementCount1; intTempPos++) {
			boolean blnElementFoundInXML1 = false;
			for (Map.Entry<Integer, Integer> objEntry : mapMatchedNodePositions.entrySet()) {
				if (objEntry.getKey().intValue() == intTempPos)
					blnElementFoundInXML1 = true;
			}
			if ((blnElementFoundInXML1) && (intTempPos < intElementCount1))
					lstMatchedPositionsInXML1.add((new Integer(intTempPos)).toString());
		}
		for (int intTempPos = 0; intTempPos < lstMatchedPositionsInXML1.size(); intTempPos++) {
			if(intTempPos >= lstMatchedPositionsInXML1.size())
				strPosition1 = " ";
			else
				strPosition1 = lstMatchedPositionsInXML1.get(intTempPos);
			lstMatchedNodePositionsInXML1.add(strPosition1);
		}
		return lstMatchedNodePositionsInXML1;
	}

	private static int getPositionOfMatchingNodeFromList(Node node1, NodeList nodeListRoot2,
			Map<Integer, Integer> mapMatchedNodePositions, List<String> lstElementsToExclude,
			Map<String, String> mapIgnoreCaseSensitiveValues) {
		int intPositionOfMatchingNodeFromList = -1;
		for (int intNodePositionInLst2 = 0; intNodePositionInLst2 < nodeListRoot2.getLength(); intNodePositionInLst2++) {
			if ((!isNumberPresentInMapValue(intNodePositionInLst2, mapMatchedNodePositions))
				&& (eligibleNodeForValidation(nodeListRoot2.item(intNodePositionInLst2), lstElementsToExclude))
					&& (equal(node1, nodeListRoot2.item(intNodePositionInLst2), lstElementsToExclude, mapIgnoreCaseSensitiveValues))) {
						intPositionOfMatchingNodeFromList = intNodePositionInLst2;
						break;
			}
		}
		return intPositionOfMatchingNodeFromList;
	}

	private static boolean equal(Node node1, Node node2, List<String> lstElementsToExclude,
			Map<String, String> mapIgnoreCaseSensitive) {
		boolean isEqual = true;
		List<Integer> lstMatchedPositionsInSecondList = new ArrayList<Integer> ();

		if ((node1.getNodeType() == node2.getNodeType()) && (node1.getNodeName().equals(node2.getNodeName()))) {
			if (node1.getNodeType() == Node.TEXT_NODE || node1.getNodeType() == Node.ATTRIBUTE_NODE) {
				isEqual = equalTextOrAttributeNodes(node1, node2, mapIgnoreCaseSensitive, isEqual);
			}
			else { // if the node is not text or attribute node
				isEqual = equalNonTextOrAttNodes(node1, node2, lstElementsToExclude, mapIgnoreCaseSensitive,
						isEqual, lstMatchedPositionsInSecondList);
			}
		}
		else
			isEqual = false;
		return isEqual;
	}

	private static boolean equalNonTextOrAttNodes(Node node1, Node node2, List<String> lstElementsToExclude,
			Map<String, String> mapIgnoreCaseSensitive, boolean isEqual,
			List<Integer> lstMatchedPositionsInSecondList) {
		boolean isEqual_Local = isEqual;
		int node1Ctr;
		int node2Ctr;
		List<Node> lst1 = getChildrenWithoutTextNodesIfComplex(node1, lstElementsToExclude);
		List<Node> lst2 = getChildrenWithoutTextNodesIfComplex(node2, lstElementsToExclude);
		logger.debug("246 : XMLDataConverter.equal(...) : " + lst1.size() + " : " + lst2.size());
		if (lst1.size() == lst2.size() && lst1.isEmpty() && lst2.isEmpty()) {
			NodeList childNodes1 = node1.getChildNodes();
			NodeList childNodes2 = node2.getChildNodes();
			if (null != childNodes1 && childNodes1.getLength() > 0 && null != childNodes2 && childNodes2.getLength() > 0) {
				logger.debug("Child Nodes Length : " + childNodes1.getLength() + " : " + childNodes2.getLength());
				if (childNodes1.getLength() != childNodes2.getLength())
					isEqual_Local = false;
			}
			else {
				String strYesNo = "No";
				if (null != mapIgnoreCaseSensitive) {
					String strPath = Util_XMLConvert.getCompletePathForANode(node1).substring(0,  Util_XMLConvert.getCompletePathForANode(node1).lastIndexOf("/"));
					strYesNo = mapIgnoreCaseSensitive.get(strPath);
				}

				if (null != strYesNo && strYesNo.equalsIgnoreCase("Yes")) {
					if (node1.getTextContent().trim().equalsIgnoreCase(node2.getTextContent().trim()))
						isEqual_Local = true;
					else
						isEqual_Local = false;
				} else if (null != strYesNo && (strYesNo.startsWith("<")
						|| strYesNo.startsWith(">")
						|| strYesNo.startsWith("Value")
						|| strYesNo.startsWith("Between"))) {
					String equalityCond[] = strYesNo.split(",");
					if (node1.getTextContent() != null && !node1.getTextContent().trim().equals("")) {
						// TODO : Check this logic
						switch (equalityCond[0]) {
						case "<":
							Double nodeVal = Double.parseDouble(node2.getTextContent().trim());
							Double inputVal = Double.parseDouble(equalityCond[1].trim());
							if (nodeVal < inputVal)
								isEqual_Local = true;
							else
								isEqual_Local = false;
							break;
						case ">":
							Double nodeVal1 = Double.parseDouble(node2.getTextContent().trim());
							Double inputVal1 = Double.parseDouble(equalityCond[1].trim());
							if (nodeVal1 > inputVal1)
								isEqual_Local = true;
							else
								isEqual_Local = false;
							break;
						case "Value":
							String nodeVal2 = node2.getTextContent().trim();
							String inputVal2 = equalityCond[1].trim();
							if (nodeVal2.equals(inputVal2))
								isEqual_Local = true;
							else
								isEqual_Local = false;
							break;
						case "Between":
							Double nodeVal3 = Double.parseDouble(node2.getTextContent().trim());
							Double inputVal3 = Double.parseDouble(equalityCond[1].trim());
							Double inputVal4 = Double.parseDouble(equalityCond[2].trim());
							if (nodeVal3 > inputVal3 && nodeVal3 < inputVal4)
								isEqual_Local = true;
							else
								isEqual_Local = false;
							break;
						default:
							break;
						}
					}
				} else {
					if (node1.getTextContent().trim().equals(node2.getTextContent().trim()))
						isEqual_Local = true;
					else
						isEqual_Local = false;
				}
			}
		}
		if (lst1.size() != lst2.size())
			isEqual_Local = false;
		else {
			Node node1_child = null;
			Node node2_child = null;
			int intDoNotCompareElementCnt = 0;
			for (node1Ctr = 0; node1Ctr < lst1.size(); node1Ctr++) {
				node1_child = lst1.get(node1Ctr);
				if (eligibleNodeForValidation(node1_child, lstElementsToExclude)) {
					boolean blnNodeMatchFound = false;
					for (node2Ctr = 0; node2Ctr < lst2.size(); node2Ctr++) {
						node2_child = lst2.get(node2Ctr);
						if (eligibleNodeForValidation(node2_child, lstElementsToExclude)) {
							if (isAlreadyMatchedNode(lstMatchedPositionsInSecondList, node2Ctr))
								continue;
							else
								if (equal(node1_child, node2_child, lstElementsToExclude, mapIgnoreCaseSensitive)) {
									lstMatchedPositionsInSecondList.add(new Integer(node2Ctr));
									blnNodeMatchFound = true;
									break;
								}
						}
					}

//							If any element from first list is not present in the second list, then there is a mismatch
					if (!blnNodeMatchFound) {
						isEqual_Local = false;
						break;
					}
				}
				else
					intDoNotCompareElementCnt++;
			}
//					If the matched positions count is not same as the number of element in the first child list, then there is a mismatch
			if ((lstMatchedPositionsInSecondList.size() + intDoNotCompareElementCnt) != lst1.size())
				isEqual_Local = false;
		}
		return isEqual_Local;
	}

	private static boolean equalTextOrAttributeNodes(Node node1, Node node2, Map<String, String> mapIgnoreCaseSensitive,
			boolean isEqual) {
		boolean isEqual_Local = isEqual;
		String strYesNo = "No";
		if (null != mapIgnoreCaseSensitive) {
			String strpath = Util_XMLConvert.getCompletePathForANode(node1).substring(0, Util_XMLConvert.getCompletePathForANode(node1).lastIndexOf("/"));
			strYesNo = mapIgnoreCaseSensitive.get(strpath);
		}

		if (null != strYesNo && strYesNo.equalsIgnoreCase("Yes")) {
			if (node1.getTextContent().trim().equalsIgnoreCase(node2.getTextContent().trim())) {
				isEqual_Local = true;
			}
			else {
				isEqual_Local = false;
			}
		} else if (null != strYesNo && (strYesNo.startsWith("<")
			|| strYesNo.startsWith(">")
			|| strYesNo.startsWith("Value")
			|| strYesNo.startsWith("Between"))) {
//					If Node2(Actual XML content) matches the criteria move to matched tab in excel
			String equalityCond[] = strYesNo.split(",");
			if (node2.getTextContent() != null && !node2.getTextContent().trim().equals("")) {
				switch (equalityCond[0]) {
				case "<":
					Double nodeVal = Double.parseDouble(node2.getTextContent().trim());
					Double inputVal = Double.parseDouble(equalityCond[1].trim());
					if (nodeVal < inputVal)
						isEqual_Local = true;
					else
						isEqual_Local = false;
					break;
				case ">":
					Double nodeVal1 = Double.parseDouble(node2.getTextContent().trim());
					Double inputVal1 = Double.parseDouble(equalityCond[1].trim());
					if (nodeVal1 > inputVal1)
						isEqual_Local = true;
					else
						isEqual_Local = false;
					break;
				case "Value":
					String nodeVal2 = node2.getTextContent().trim();
					String inputVal2 = equalityCond[1].trim();
					if (nodeVal2.equals(inputVal2))
						isEqual_Local = true;
					else
						isEqual_Local = false;
					break;
				case "Between":
					Double nodeVal3 = Double.parseDouble(node2.getTextContent().trim());
					Double inputVal3 = Double.parseDouble(equalityCond[1].trim());
					Double inputVal4 = Double.parseDouble(equalityCond[2].trim());
					if (nodeVal3 > inputVal3 && nodeVal3 < inputVal4)
						isEqual_Local = true;
					else
						isEqual_Local = false;
					break;
				default:
					break;
				}
			}
		} else {
			if (node1.getTextContent().trim().equals(node2.getTextContent().trim()))
				isEqual_Local = true;
			else
				isEqual_Local = false;
		}
		return isEqual_Local;
	}

	private static boolean isAlreadyMatchedNode(List<Integer> lstMatchedPositions, int nodeCtr) {
		boolean isAlreadyMatchedNode = false;
		for (Integer intTempPos : lstMatchedPositions)
			if (intTempPos.intValue() == nodeCtr) {
				isAlreadyMatchedNode = true;
				break;
			}
		return isAlreadyMatchedNode;
	}

	private static List<Node> getChildrenWithoutTextNodesIfComplex(Node objNodes, List<String> lstElementsToExclude) {
		NodeList childNodes = objNodes.getChildNodes();
		int intNodeListLen = childNodes.getLength();
		List<Node> lstChildrenWithoutTextNodes = new ArrayList<Node> ();
		boolean isComplex = false;
		for (int i=0; i < intNodeListLen; i++) {
			if (childNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				isComplex = true;
				break;
			}
		}
		for (int i=0; i<intNodeListLen; i++) {
			if ((isComplex)
					&& (childNodes.item(i).getNodeType() != Node.TEXT_NODE)) {
				String str = childNodes.item(i).getTextContent();
				if ((str != null && !str.equals(""))
					&& (eligibleNodeForValidation(childNodes.item(i), lstElementsToExclude))) {
						lstChildrenWithoutTextNodes.add(childNodes.item(i));
				}
			}
			else if (eligibleNodeForValidation(childNodes.item(i), lstElementsToExclude))
					lstChildrenWithoutTextNodes.add(childNodes.item(i));
		}
		return lstChildrenWithoutTextNodes;
	}

	private static boolean isNumberPresentInMapValue(int intToMatch,
			Map<Integer, Integer> mapMatchedNodePositions) {
		for (Map.Entry<Integer, Integer> entry : mapMatchedNodePositions.entrySet()) {
			if (entry.getValue().intValue() == intToMatch) {
				return true;
			}
		}
		return false;
	}

	private static boolean eligibleNodeForValidation(Node objNodeToCheck, List<String> lstElementsToExclude) {
		boolean isEligibleForValidation = true;
		if (null != lstElementsToExclude && lstElementsToExclude.size() > 0) {
			String strElementPath = getCompletePathForANode(objNodeToCheck);
			for (String strTempElementToExclude : lstElementsToExclude) {
				if (strTempElementToExclude.equals(strElementPath)) {
					isEligibleForValidation = false;
					break;
				}
			}
		}
		return isEligibleForValidation;
	}

	private static String getCompletePathForANode(Node objNode) {
		String strCompletePathForNode = objNode.getNodeName();
		Node tempNode = objNode;
		while (null != tempNode.getParentNode()) {
			tempNode = tempNode.getParentNode();
			if (tempNode.getNodeName().equals("#document"))
				strCompletePathForNode = "/" + strCompletePathForNode;
			else
				strCompletePathForNode = tempNode.getNodeName() + "/" + strCompletePathForNode;
		}
		return strCompletePathForNode;
	}

	private static Map<String, String> getAttributeMap(Document doc) {
		Map<String, String> mapAttributes = new HashMap<String, String>();
		NodeList nodeList = doc.getElementsByTagName("*");
		for (int i=0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				NamedNodeMap mapNamedNode = node.getAttributes();
				int intNamedNodeSize = mapNamedNode.getLength();
				for (int intCtr = 0; intCtr < intNamedNodeSize; intCtr++) {
					String strNodeName = mapNamedNode.item(intCtr).getNodeName();
					String[] arrNameSpace = strNodeName.toLowerCase().split(":");
					if (arrNameSpace.length > 1
							&& arrNameSpace[0].trim().equalsIgnoreCase("xmlns")) {
						String strNameSpaceKey = null;
						if (arrNameSpace.length > 1)
							strNameSpaceKey = arrNameSpace[1];
						else
							strNameSpaceKey = "";
						String strNameSpaceValue = mapNamedNode.item(intCtr).getNodeValue();
						mapAttributes.put(strNameSpaceKey, strNameSpaceValue);
					}
				}
			}
		}
		return mapAttributes;
	}

	private static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
		if (null == builder) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			builder = factory.newDocumentBuilder();
		}
		return builder;
	}

	public static void printResultsToFile(String strComparisonResultsFile, List<String> lstPassedCSVData, List<String> lstFailedCSVData) {
		logger.debug("Start : XMLDataConverter.printResultsToFile(...)");
		HSSFWorkbook workbook = new HSSFWorkbook();
		try {
			HSSFSheet sheet = workbook.createSheet("MatchedData");
			if (null != lstPassedCSVData) {
				short intRowCnt = 0;
				for (String strPassedCSVROWData : lstPassedCSVData) {
					String[] arrpassedCSVRowData = strPassedCSVROWData.split(",");
					HSSFRow rowhead = sheet.createRow(intRowCnt++);
					for (int intColCnt = 0; intColCnt < arrpassedCSVRowData.length; intColCnt++) {
						rowhead.createCell(intColCnt).setCellValue(arrpassedCSVRowData[intColCnt]);
					}
				}
			}
			sheet = workbook.createSheet("MismatchedData");
			if(null != lstFailedCSVData) {
				short intRowCnt = 0;
				for (String strFailedCSVROWData : lstFailedCSVData) {
					String[] arrFailedCSVRowData = strFailedCSVROWData.split(",");
					HSSFRow rowhead = sheet.createRow(intRowCnt++);
					for (int intColCnt = 0; intColCnt < arrFailedCSVRowData.length; intColCnt++) {
						rowhead.createCell(intColCnt).setCellValue(arrFailedCSVRowData[intColCnt]);
					}
				}
			}
			FileOutputStream fileOut = new FileOutputStream(strComparisonResultsFile);
			workbook.write(fileOut);
			fileOut.close();
		} catch (Exception e) {
			logger.error("706 : XMLDataConverter.printResultsToFile(...) : " + e);
		}
		finally {
			if (null != workbook) {
				try {
					workbook.close();
				} catch (IOException e) {
					logger.error("713 : XMLDataConverter.printResultsToFile(...) : IOException e : " + e);
				}
			}
		}
		logger.debug("End : XMLDataConverter.printResultsToFile(...)");
	}
}
