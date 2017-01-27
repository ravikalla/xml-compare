package in.ravikalla.xml_compare;

import java.io.IOException;

import org.apache.log4j.Logger;

import in.ravikalla.xml_compare.util.CommonUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase {
    private final static Logger logger = Logger.getLogger(AppTest.class);
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
    	boolean blnExceptionExists = false;
    	try {
			CommonUtil.readDataFromFile(null);
		} catch (IOException e) {
			logger.debug("IOException e : " + e.getMessage());
			blnExceptionExists = true;
		}
    	catch (NullPointerException e) {
			logger.debug("NullPointerException e : " + e.getMessage());
			blnExceptionExists = true;
		}
        assertTrue( blnExceptionExists );
    }
}
