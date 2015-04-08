/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recipe;

import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import touchlesscooking1.XMLTool;

/**
 *
 * @author Dev
 */
public class Recipe {
    String title;
    String description;
    int serves;
    List<Ingredient> ingredients;
    List<Step> steps;
    
    private static Recipe defaultRecipe;
    
    static {
        initializeDefaultRecipe();
    }
    
    public Recipe(String title, String description, int serves, List<Ingredient> ingredients, List<Step> steps) {
        this.title = title;
        this.description = description;
        this.serves = serves;
        this.ingredients = ingredients;
        this.steps = steps;
    }
    
    public void addIngredient(Ingredient i) {
        this.ingredients.add(i);
    }
    
    public void addStep(String text) {
        this.steps.add(new Step(this.steps.size(), text));
    }
    
    public void addStep(Step step) {
        step.number = this.steps.size();
        this.steps.add(step);
    }
    
    public String toString() {
        String output = "";
        output += title + "\n";
        output += description + "\n";
        output += serves + "\n";
        output += ingredients + "\n";
        output += steps + "\n";
        return output;
    }
    
    public static List<Recipe> extractRecipes(NodeList nodes) {
        List<Recipe> recipes = new ArrayList<>();
        for(int i = 0 ; i < nodes.getLength();i++) {

                //get the recipe element
                Element element = (Element)nodes.item(i);
                
                String title = XMLTool.getTextValue(element, "title");
                String description = XMLTool.getTextValue(element, "description").trim();
                int serves = Integer.parseInt(XMLTool.getTextValue(element, "serves"));
                List<Ingredient> ingredients;
                List<Step> steps;
                
                NodeList ingredientList = element.getElementsByTagName("ingredient");
                NodeList stepList = element.getElementsByTagName("step");
                
                //get the Employee object
                ingredients = Ingredient.extractIngredients(ingredientList);
                steps = Step.extractSteps(stepList);
                
                Recipe recipe = new Recipe(title, description, serves, ingredients, steps);
                
                //add it to list
                recipes.add(recipe);
        }
        return recipes;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getServes() {
        return serves;
    }
    
    public List<Ingredient> getIngredients() {
        return ingredients;
    }
    
    public List<Step> getSteps() {
        return steps;
    }
    
    public static void initializeDefaultRecipe() {
        defaultRecipe = new Recipe("Saturday Morning Eggs", "The eggs I make on Saturday mornings.", 2, new ArrayList<Ingredient>(), new ArrayList<Step>());
    }
    
    public static Recipe getDefaultRecipe() {
        return defaultRecipe;
    }
}
