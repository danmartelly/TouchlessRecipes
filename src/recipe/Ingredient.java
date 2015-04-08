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
public class Ingredient {
    String amount;
    String item;
    String details;
    
    public Ingredient(String amount, String item, String details) {
        this.amount = amount;
        this.item = item;
        this.details = details;
    }
    
    @Override
    public String toString() {
        return amount + " " + item + " " + details;
    }
    
    public static List<Ingredient> extractIngredients(NodeList nodes) {
        List<Ingredient> ingredients = new ArrayList<>();
        //System.out.println(nodes.getLength());
        for(int i = 0; i < nodes.getLength(); ++i) {
            
            Element element = (Element)nodes.item(i);
            
            String amount = XMLTool.getTextValue(element, "amount");
            String item = XMLTool.getTextValue(element, "item");
            String details = XMLTool.getTextValue(element, "details");
            
            Ingredient ingredient = new Ingredient(amount, item, details);
            
            ingredients.add(ingredient);
        }
        
        return ingredients;
    }
}
