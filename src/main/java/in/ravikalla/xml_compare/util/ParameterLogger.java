package in.ravikalla.xml_compare.util;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParameterLogger {
    private static final Logger logger = LogManager.getLogger(ParameterLogger.class);

    private ParameterLogger() {
        // Utility class - private constructor
    }

    public static void logComparisonParameters(String resultFile, String xml1, String xml2, 
                                             List<String> iterativeElements, List<String> excludeElements,
                                             String primaryNodeElementName, String trimElements) {
        logger.debug("Start : ParameterLogger.logComparisonParameters(...)");
        
        String iterativeElementsStr = iterativeElements != null ? 
                iterativeElements.stream().collect(Collectors.joining(",")) : "";
        String excludeElementsStr = excludeElements != null ? 
                excludeElements.stream().collect(Collectors.joining(",")) : "";
        
        StringBuilder content = new StringBuilder();
        content.append(resultFile).append("\n")
               .append("@xmlStr1 : ").append(xml1).append("\n")
               .append("@xmlStr2 : ").append(xml2).append("\n")
               .append("@strIterativeElement : ").append(iterativeElementsStr).append("\n")
               .append("@lstElementsToExclude : ").append(excludeElementsStr).append("\n")
               .append("@strPrimaryNodeXMLElementName : ").append(primaryNodeElementName).append("\n")
               .append("@strTrimElements : ").append(trimElements);
        
        String paramFileName = resultFile + "_Params";
        FileUtil.writeParametersToFile(paramFileName, content.toString());
        
        logger.debug("End : ParameterLogger.logComparisonParameters(...)");
    }
}