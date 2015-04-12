/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package touchlesscooking1;

/**
 *
 * @author Dev
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import recipe.*;

public class TouchlessCooking1 extends Application {

    String newCommand = "";
    int sceneWidth = 600, sceneHeight = 500;
    Stage mainStage;
    List<Recipe> recipes;
    int pageNumber = 0;
    Map<String, Integer> timeToInt = new HashMap<String, Integer>();
    Pane timerPane = new Pane();
    Pane superRoot = new Pane();
    Thread listener;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) throws ParserConfigurationException {
        initializeMap();
        mainStage = primaryStage;
        
        Recipe defaultRecipe;
        
        Document dom = XMLTool.parseXmlFile("recipes\\apple_crisp.xml");
        
        Element rootElement = dom.getDocumentElement();
        NodeList nodes = rootElement.getElementsByTagName("recipe");
        
        recipes = Recipe.extractRecipes(nodes);
        if(recipes.size() > 0)
            defaultRecipe = recipes.get(0);
        else
            defaultRecipe = Recipe.getDefaultRecipe();
        
        VBox root = new VBox(15);
        
        renderRecipe(root, defaultRecipe);
        
        superRoot.getChildren().add(root);
        displayTimer(superRoot, 0);
        Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
        scene.getStylesheets().add("/css/stylesheet.css");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        listener = new ListeningThread(this);
        listener.start();
        
        Timeline timer = new Timeline(new KeyFrame(
            Duration.millis(100), f -> react()
        ));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }
    
    public void displayTimer(Pane root, int knobTurn) {
        RadialGradient gradient = new RadialGradient(0,
        .1,sceneWidth/2, sceneHeight/2, Math.min(sceneWidth, sceneHeight)/8, false, CycleMethod.REFLECT,
            new Stop(0, Color.WHITESMOKE),
            new Stop(1, Color.GAINSBORO) );
        
        Circle timerCircle = new Circle(sceneWidth/2, sceneHeight/2, Math.min(sceneWidth, sceneHeight)/4);
        timerCircle.getStyleClass().add("timer");
        timerCircle.setFill(gradient);
        
        Circle knobCircle = new Circle(sceneWidth/2, sceneHeight/2, Math.min(sceneWidth, sceneHeight)/8);
        knobCircle.getStyleClass().add("timer");
        knobCircle.setFill(Color.WHITESMOKE);
        
        Rectangle knobHandle = new Rectangle(sceneWidth/2 - Math.min(sceneWidth, sceneHeight)/10, sceneHeight/2 - Math.min(sceneWidth, sceneHeight)/40, Math.min(sceneWidth, sceneHeight)/5, Math.min(sceneWidth, sceneHeight)/20);
        knobHandle.setFill(Color.CORNFLOWERBLUE);
        knobHandle.setRotate(-knobTurn * 6 + 90);
        
        double radius = Math.min(sceneWidth, sceneHeight)/4;
        
        Text time = new Text("" + knobTurn);
        time.setStyle("-fx-font: 30px serif");
        Bounds bounds = time.getLayoutBounds();
        Rectangle timeBackground = new Rectangle(sceneWidth/2 - bounds.getWidth() + bounds.getMinX(), sceneHeight/2 - radius + bounds.getMinY(), bounds.getMaxX(), bounds.getMaxY());
        timeBackground.setFill(Color.RED);
        
        timerPane.getChildren().add(timeBackground);
        System.out.println(timeBackground);
        
        time.setLayoutX(sceneWidth/2 - bounds.getWidth());
        time.setLayoutY(sceneHeight/2 - radius);
        timerPane.getChildren().add(time);
        timerPane.getChildren().add(timerCircle);
        for(int i=0; i < 60; i++){
            Line tick;
            if(i % 5 == 0)
                tick = new Line(sceneWidth/2 + (Math.cos(-Math.PI/2 + Math.PI*i/30) * radius * 4 / 5), sceneHeight/2 + (Math.sin(-Math.PI/2 + Math.PI*i/30) * radius * 4 / 5), sceneWidth/2 + (Math.cos(-Math.PI/2 + Math.PI*i/30) * radius), sceneHeight/2 + (Math.sin(-Math.PI/2 + Math.PI*i/30) * radius));
            else
                tick = new Line(sceneWidth/2 + (Math.cos(-Math.PI/2 + Math.PI*i/30) * radius * 9 / 10), sceneHeight/2 + (Math.sin(-Math.PI/2 + Math.PI*i/30) * radius * 9 / 10), sceneWidth/2 + (Math.cos(-Math.PI/2 + Math.PI*i/30) * radius), sceneHeight/2 + (Math.sin(-Math.PI/2 + Math.PI*i/30) * radius));
            timerPane.getChildren().add(tick);
        }
        timerPane.getChildren().add(knobCircle);
        timerPane.getChildren().add(knobHandle);
        radius = Math.min(sceneWidth, sceneHeight)/8;
        for(int i=0; i < 24; i++){
            Line tick = new Line(sceneWidth/2 + (Math.cos(knobTurn * 6 / 180.0 * Math.PI + Math.PI*i/12) * radius * 4 / 5), sceneHeight/2 + (Math.sin(knobTurn * 6 / 180.0 * Math.PI + Math.PI*i/12) * radius * 4 / 5), sceneWidth/2 + (Math.cos(knobTurn * 6 / 180.0 * Math.PI + Math.PI*i/12) * radius), sceneHeight/2 + (Math.sin(knobTurn * 6 / 180.0 * Math.PI + Math.PI*i/12) * radius));
            timerPane.getChildren().add(tick);
        }
        root.getChildren().add(timerPane);
    }
    
    public void handle(String command){
        System.out.printf("Command: %s received\n", command);
        newCommand = command;
    }
    
    public void react() {
        if(newCommand.equals("next page")) {
            if(pageNumber < recipes.size() - 1) {
                Pane superRoot = new Pane();
                VBox root = new VBox(15);            
                renderRecipe(root, recipes.get(++pageNumber));
                superRoot.getChildren().add(root);
                Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
                scene.getStylesheets().add("/css/stylesheet.css");
                mainStage.setScene(scene);
            }
            superRoot.getChildren().remove(timerPane);
        } else if(newCommand.equals("previous page")) {
            if(pageNumber > 0) {
                Pane superRoot = new Pane();
                VBox root = new VBox(15);
                renderRecipe(root, recipes.get(--pageNumber));
                superRoot.getChildren().add(root);
                Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
                scene.getStylesheets().add("/css/stylesheet.css");
                mainStage.setScene(scene);
            }
            superRoot.getChildren().remove(timerPane);
        } else if(newCommand.startsWith("set timer")) {
            String[] words = newCommand.split(" ");
            String time = words[3];
            time += (words.length == 6) ? " " + words[4] : "";
            System.out.println(time);
            displayTimer(superRoot, timeToInt.get(time));
            if(superRoot.getChildren().contains(timerPane)) {
                superRoot.getChildren().add(timerPane);
            }
        }else if(newCommand.equals("close timer")) {
            superRoot.getChildren().remove(timerPane);
        } else if(newCommand.startsWith("go to")) {
            String linkPointedTo = newCommand.substring(5);
            System.out.println(linkPointedTo);
            superRoot.getChildren().remove(timerPane);
        }
        newCommand  = "";
    }
    
    public void renderRecipe(VBox root, Recipe recipe) {
        Text title = new Text(recipe.getTitle());
        title.getStyleClass().add("title");
        HBox top = new HBox();
        VBox topLeft = new VBox(10);
        topLeft.getChildren().add(title);
        Text desc = new Text(recipe.getDescription());
        desc.setWrappingWidth(300);
        topLeft.getChildren().add(desc);
        topLeft.getChildren().add(new Text("Serves " + recipe.getServes()));
        topLeft.getStyleClass().add("topPane");
        
        top.getChildren().add(topLeft);
        //File images = new File("images/eggs.jpg");
        Image image = new Image("images/eggs.jpg");
        ImageView finalImage = new ImageView(image);
        finalImage.getStyleClass().add("topPane");
        top.getChildren().add(finalImage);
        
        Text ingredientHeader = new Text("Ingredients");
        ingredientHeader.getStyleClass().add("sectionHeader");
        VBox ingredientsPane = new VBox();
        List<Ingredient> recipeIngredients = recipe.getIngredients();
        recipeIngredients.stream().forEach((i) -> {
            ingredientsPane.getChildren().add(new Text(i.toString()));
        });
        
        Text stepHeader = new Text("Steps");
        stepHeader.getStyleClass().add("sectionHeader");
        VBox stepsPane = new VBox();
        List<Step> recipeSteps = recipe.getSteps();
        recipeSteps.stream().forEach((s) -> {
            stepsPane.getChildren().add(new Text(s.toString()));
        });
        
        root.getChildren().add(top);
        root.getChildren().addAll(ingredientHeader, ingredientsPane);
        root.getChildren().addAll(stepHeader, stepsPane);
    }
    
    public void initializeMap() {
        timeToInt.put("ten", 10);
        timeToInt.put("fifteen", 15);
        timeToInt.put("twenty", 20);
        timeToInt.put("thirty", 30);
        timeToInt.put("forty", 40);
        timeToInt.put("forty five", 45);
        timeToInt.put("fifty", 50);
        timeToInt.put("sixty", 60);
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        //listener.interrupt();
    }
}
