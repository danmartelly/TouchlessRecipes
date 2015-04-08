/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recipe;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import touchlesscooking1.XMLTool;

/**
 *
 * @author Dev
 */
public class Step {
    int number;
    String text;
    
    public Step(int number, String text) {
        this.number = number;
        this.text = text;
    }
    
    @Override
    public String toString() {
        return number + " " + text;
    }
    
    public static List<Step> extractSteps(NodeList nodes) {
        List<Step> steps = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); ++i) {
            
            Element element = (Element)nodes.item(i);
            
            int number = Integer.parseInt(XMLTool.getTextValue(element, "number"));
            String text = XMLTool.getTextValue(element, "text");
            Step step = new Step(i+1, text);
            
            steps.add(step);
        }
        return steps;
    }
}
