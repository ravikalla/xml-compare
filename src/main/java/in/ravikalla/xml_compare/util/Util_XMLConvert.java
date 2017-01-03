package in.ravikalla.xml_compare.util;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Util_XMLConvert {

	public static Map<String, String> convertTrimmableElementsToMap(String strTrimElements, String strElementsSeparator, String strValueSeparator) {
		Map<String, String> mapTrimmableElements = new HashMap<String, String>();
		if (null != strTrimElements && strTrimElements.trim().length() > 0) {
			String[] arrElements = strTrimElements.split(strElementsSeparator);
			if (null != arrElements && arrElements.length > 0) {
				for (String strElementValuePairs : arrElements) {
					if (null != strElementValuePairs && strElementValuePairs.length() > 0) {
						String[] arrElementValuePairs = strElementValuePairs.split(strValueSeparator);
						if (null != arrElementValuePairs && arrElementValuePairs.length > 0) {
							if (null != arrElementValuePairs[1] && arrElementValuePairs[1].equalsIgnoreCase("Between")) {
								mapTrimmableElements.put(arrElementValuePairs[0], arrElementValuePairs[1] + "," + arrElementValuePairs[2] + "," + arrElementValuePairs[3]);
							}
							else if (null != arrElementValuePairs[1]
									&& (arrElementValuePairs[1].equals("<") || arrElementValuePairs[1].equals(">")
											|| arrElementValuePairs[1].equals("Value"))) {
								mapTrimmableElements.put(arrElementValuePairs[0], arrElementValuePairs[1] + "," + arrElementValuePairs[2]);
							}
							else {
								mapTrimmableElements.put(arrElementValuePairs[0], arrElementValuePairs[1]);
							}
						}
					}
				}
			}
		}
		return mapTrimmableElements;
	}

	public static void trimValueIfApplicable(NodeList lstChildNodeList, Map<String, String> mapTrimElements) {
		if (null != lstChildNodeList && lstChildNodeList.getLength() > 0) {
			int intChildrenLength = lstChildNodeList.getLength();
			Node objChildNode = null;
			for (int intChildCtr = 0; intChildCtr < intChildrenLength; intChildCtr++) {
				objChildNode = lstChildNodeList.item(intChildCtr);
				String strCompletePath = Util_XMLConvert.getCompletePathForANode(objChildNode);
				if (null != strCompletePath && strCompletePath.length() > 0) {
					String strValue = mapTrimElements.get(strCompletePath);
					if (null != strValue && strValue.length() > 0) {
						int intValueLength = Integer.parseInt(strValue);
						String strTextContent = objChildNode.getTextContent();

						if (strTextContent.length() >= intValueLength) {
							strTextContent = strTextContent.substring(0, intValueLength);
							objChildNode.setTextContent(strTextContent);
						}
					}
				}
				trimValueIfApplicable(objChildNode.getChildNodes(), mapTrimElements);
			}
		}
	}

	public static String getCompletePathForANode(Node objNode) {
		String strCompletePathForNode = objNode.getNodeName();
		Node tempNode = objNode;
		while (tempNode.getParentNode() != null) {
			tempNode = tempNode.getParentNode();
			if (tempNode.getNodeName().equals("#document"))
				strCompletePathForNode = "/" + strCompletePathForNode;
			else
				strCompletePathForNode = tempNode.getNodeName() + "/" + strCompletePathForNode;
		}
		return strCompletePathForNode;
	}
}
