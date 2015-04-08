/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package touchlesscooking1;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Dev
 */
public class XMLTool {
    /**
    * I take a xml element and the tag name, look for the tag and get
    * the text content
    */
    public static String getTextValue(Element ele, String tagName) {
            String textVal = null;
            NodeList nl = ele.getElementsByTagName(tagName);
            if(nl != null && nl.getLength() > 0) {
                    Element el = (Element)nl.item(0);
                    if(el.getFirstChild() != null)
                        textVal = el.getFirstChild().getNodeValue();
                    else
                        textVal = "";
            } else {
                textVal = "0";
            }

            return textVal;
    }
    
    public static Document parseXmlFile(String recipeFile) throws ParserConfigurationException {
        //get the factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //Using factory get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        try {

            //parse using builder to get DOM representation of the XML file
            Document dom = db.parse(recipeFile);
            return dom;
            
        } catch (org.xml.sax.SAXException | IOException ex) {
            Logger.getLogger(XMLTool.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return db.newDocument();
    }
    
    
}
