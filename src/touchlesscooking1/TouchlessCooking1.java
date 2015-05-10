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
import java.util.ArrayList;
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
import javafx.scene.Node;
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
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.xml.parsers.ParserConfigurationException;

import leap.LeapManager;
import leap.LeapManager.LEAP_EVENT;
import leap.LeapManager.LEAP_STATE;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.javafx.geom.Point2D;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import recipe.*;
import tts.TTS;

public class TouchlessCooking1 extends Application {

    String newCommand = "", lastCommand = "";
    int sceneWidth = 600, sceneHeight = 500;
    Stage mainStage;
    List<Recipe> recipes;
    List<Text> nodes = new ArrayList<Text>();
    boolean timerShowing = false, tableofcontents = true;
    List<Hyperlink> tofHyperlinks;
    int pageNumber = 0, readIndex = 0, timesRepeated = 0;
    LeapManager manager;
    LeapHandler leapHandler;
    public LEAP_EVENT lastLeapEvent = null;
    Map<String, Integer> timeToInt = new HashMap<String, Integer>();
    Map<String, Integer> recipeIndexMap = new HashMap<String, Integer>();
    Pane timerPane = new Pane();
    Pane superRoot = new Pane();
    Thread listener;
    TTS tts = new TTS();
    Rectangle cursorNode;
    final float cursorOpacity = .3F;
    
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
        manager = new LeapManager();
        manager.setTimerMode(false);
        leapHandler = new LeapHandler(this);
        manager.addListener(leapHandler);
        
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
        
        //renderRecipe(root, pageNumber);
        tofHyperlinks = new ArrayList<Hyperlink>();
        Text title = new Text("Table of Contents");
        title.getStyleClass().add("title");
        root.getChildren().add(title);
        title.setTextAlignment(TextAlignment.CENTER);
        for(int i = 0; i < recipes.size(); i++) {
            Hyperlink link = new Hyperlink(recipes.get(i).getTitle());
            link.setFont(new Font(36));
            link.setStyle("-fx-text-decoration: none");
            link.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    tableofcontents = false;
                    setRecipe(link.getText());
                }
            });
            tofHyperlinks.add(link);
            root.getChildren().add(link);
        }
        //cursor
        cursorNode = new Rectangle(0,0,10,10);
        cursorNode.setFill(Color.DODGERBLUE);
        
        superRoot.getChildren().add(root);
        superRoot.getChildren().add(cursorNode);
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
        Rectangle timeBackground = new Rectangle(sceneWidth/2 - radius - bounds.getWidth(), sceneHeight/2 - radius + bounds.getMinY() - bounds.getHeight(), bounds.getWidth() * 3 + radius * 2, bounds.getHeight() + radius * 2 - bounds.getMinY());
        timeBackground.setFill(Color.WHITE);
        timerPane.getChildren().add(timeBackground);
        
        time.setLayoutX(sceneWidth/2 - bounds.getWidth());
        time.setLayoutY(sceneHeight/2 - radius - 2);
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
    }
    
    public void handle(String command){
        System.out.printf("Command: %s received\n", command);
        newCommand = command;
        if(newCommand.equals(lastCommand) && newCommand.equals("touchless repeat")) {
            timesRepeated++;
            if(timesRepeated > 3) {
                newCommand = "";
            }
        } else {
            timesRepeated = 0;
        }
        lastCommand = command;
    }
    
    public void updateLeapEvent(LEAP_EVENT event) {
    	lastLeapEvent = event;
    }
    
    public void react() {
        String command = newCommand.substring(0);
        newCommand  = "";
        if(command.equals("touch less next page")) {
            goToNextPage();
        } else if(command.equals("touch less previous page")) {
            goToPrevPage();
        } else if (command.equals("touch less go to link")) {
            if (tableofcontents) {
                System.out.println("checking hyperlinks");
                // check intersection between cursor and hyperlinks
                Bounds cursorLocalBounds = cursorNode.getBoundsInLocal();
                Bounds cursorScreenBounds = cursorNode.localToScreen(cursorLocalBounds);
                for (int i = 0; i < 1;i++) {//tofHyperlinks.size(); i++) {
                        Hyperlink link = tofHyperlinks.get(i);
                        Bounds linkLocalBounds = link.getBoundsInLocal();
                        Bounds linkScreenBounds = link.localToScreen(linkLocalBounds);
                        if (linkScreenBounds.intersects(cursorScreenBounds)) {
                                link.fire();
                        }
                }
            }
        } else if(command.startsWith("set timer")) {
//                String[] words = command.split(" ");
//                String time = words[3];
//                time += (words.length == 6) ? " " + words[4] : "";
//                System.out.println(time);
//                displayTimer(superRoot, timeToInt.get(time));
//                if(!superRoot.getChildren().contains(timerPane)) {
//                    superRoot.getChildren().add(timerPane);
//                    timerShowing = true;
//                }
        } else if(command.equals("close timer")) {
//                superRoot.getChildren().remove(timerPane);
//                timerShowing = false;
        } else if(command.equals("touch less repeat")) {
            if(!tableofcontents) {
                Recipe currentRecipe = recipes.get(pageNumber / 3);
                switch(pageNumber % 3) {
                    case 1:
                        tts.say(currentRecipe.getIngredients().get(readIndex).toString());
                        break;
                    case 2:
                        tts.say(currentRecipe.getSteps().get(readIndex).toString());
                        break;
                }
            }
        } else if(command.startsWith("touch less go to")){
            if(tableofcontents) {
                
            }
        } else if(command.startsWith("touch less read")) {
            if(!tableofcontents) {
                String whatToRead = command.split(" ")[2];
                Recipe currentRecipe = recipes.get(pageNumber / 3);
                if(whatToRead.equals("next")) {
                    switch(pageNumber % 3) {
                        case 1:
                            if(readIndex < currentRecipe.getIngredients().size() - 1) {
                                readIndex++;
                                Pane superRoot = new Pane();
                                VBox root = new VBox(15);            
                                renderRecipe(root, pageNumber);
                                superRoot.getChildren().add(root);
                                Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
                                scene.getStylesheets().add("/css/stylesheet.css");
                                mainStage.setScene(scene);
                            }
                            tts.say(currentRecipe.getIngredients().get(readIndex).toString());
                            break;
                        case 2:
                            if(readIndex < currentRecipe.getSteps().size() - 1) {
                                readIndex++;
                                Pane superRoot = new Pane();
                                VBox root = new VBox(15);            
                                renderRecipe(root, pageNumber);
                                superRoot.getChildren().add(root);
                                Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
                                scene.getStylesheets().add("/css/stylesheet.css");
                                mainStage.setScene(scene);
                            }
                            tts.say(currentRecipe.getSteps().get(readIndex).toString());
                            break;
                    }    
                } else if(whatToRead.equals("previous")) {
                    switch(pageNumber % 3) {
                        case 1:
                            if(readIndex > 0) {
                                readIndex--;
                                Pane superRoot = new Pane();
                                VBox root = new VBox(15);            
                                renderRecipe(root, pageNumber);
                                superRoot.getChildren().add(root);
                                Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
                                scene.getStylesheets().add("/css/stylesheet.css");
                                mainStage.setScene(scene);
                            }
                            tts.say(currentRecipe.getIngredients().get(readIndex).toString());
                            break;
                        case 2:
                            if(readIndex > 0) {
                                readIndex--;
                                Pane superRoot = new Pane();
                                VBox root = new VBox(15);            
                                renderRecipe(root, pageNumber);
                                superRoot.getChildren().add(root);
                                Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
                                scene.getStylesheets().add("/css/stylesheet.css");
                                mainStage.setScene(scene);
                            }
                            tts.say(currentRecipe.getSteps().get(readIndex).toString());
                            break;
                    }
                } else {
                    switch(pageNumber % 3) {
                        case 1:
                            List<Ingredient> ingredients = currentRecipe.getIngredients();
                            String ingredientsToRead = "";
                            for(int i = 0; i < ingredients.size(); i++) {
                                ingredientsToRead += ingredients.get(i).toString() + " ";
                            }
                            tts.say(ingredientsToRead);
                            break;
                        case 2:
                            List<Step> steps = currentRecipe.getSteps();
                            String stepsToRead = "";
                            for(int i = 0; i < steps.size(); i++) {
                                stepsToRead += steps.get(i).toString() + " ";
                            }
                            tts.say(stepsToRead);
                            tts.say(currentRecipe.getSteps().get(readIndex).toString());
                            break;
                    }
                }
                superRoot.getChildren().remove(timerPane);
                timerShowing = false;
            }
        } else if(command.equals("table of contents")) {
            tableofcontents = true;
            Pane superRoot = new Pane();
            VBox root = new VBox(15);

            for(int i = 0; i < recipes.size(); i++) {
                Hyperlink link = new Hyperlink(recipes.get(i).getTitle());
                link.setFont(new Font(36));
                link.setStyle("-fx-text-decoration: none");
                link.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        tableofcontents = false;
                        setRecipe(link.getText());
                    }
                });
                root.getChildren().add(link);
            }
            //cursor
            cursorNode = new Rectangle(0,0,10,10);
            cursorNode.setFill(Color.DODGERBLUE);

            superRoot.getChildren().add(root);
            superRoot.getChildren().add(cursorNode);
            
            Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
            scene.getStylesheets().add("/css/stylesheet.css");
            mainStage.setScene(scene);
        }
        // Leap stuff
        if (manager.getCurrentState() == LEAP_STATE.IS_ZOOMING) {
            float zoomMultiplier = manager.getZoomMultiplier();
            for(Text node: nodes) {
                int size = (int)node.getFont().getSize();
                node.setFont(new Font((int)(size * zoomMultiplier)));
            }
        } else if (manager.getCurrentState() == LEAP_STATE.IS_ROTATING) {
        	float rotationDelta = manager.getRotation();
                if(timerShowing) {
                    //double modulusAngle = rotationDelta % Math.PI;
                    displayTimer(superRoot, (int)(rotationDelta / Math.PI * 180));
                }
        } else if (lastLeapEvent != null) {
        	if (lastLeapEvent == LEAP_EVENT.END_NEXT_PAGE)
                    goToNextPage();
        	else if (lastLeapEvent == LEAP_EVENT.END_NEXT_PAGE)
                    goToPrevPage();
        	// more leap events possible
        	lastLeapEvent = null;
        }
        
        // visibility of cursor
        Point2D cursorPos = manager.getCursorPosition();
        if (cursorPos != null) {
        	cursorNode.setOpacity(cursorOpacity);
        	cursorNode.setX(cursorPos.x);
        	cursorNode.setY(cursorPos.y);
        } else {
        	cursorNode.setOpacity(0);
        }
    }
    
    protected void goToNextPage() {
        readIndex = 0;
        if(tableofcontents) {
            tableofcontents = false;
            pageNumber = 0;
            Pane superRoot = new Pane();
            VBox root = new VBox(15);            
            renderRecipe(root, pageNumber);
            superRoot.getChildren().add(root);
            Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
            scene.getStylesheets().add("/css/stylesheet.css");
            mainStage.setScene(scene);
        } else if(pageNumber < recipes.size()*3 - 1) {
            pageNumber++;
            Recipe currentRecipe = recipes.get(pageNumber / 3);
            switch(pageNumber % 3) {
                case 1:
                    tts.say(currentRecipe.getIngredients().get(readIndex).toString());
                    break;
                case 2:
                    tts.say(currentRecipe.getSteps().get(readIndex).toString());
                    break;
            }
            Pane superRoot = new Pane();
            VBox root = new VBox(15);            
            renderRecipe(root, pageNumber);
            superRoot.getChildren().add(root);
            Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
            scene.getStylesheets().add("/css/stylesheet.css");
            mainStage.setScene(scene);
        }
        superRoot.getChildren().remove(timerPane);
        timerShowing = false;
    }
    
    protected void goToPrevPage() {
    	readIndex = 0;
        if(pageNumber > 0 && !tableofcontents) {
            pageNumber--;
            Recipe currentRecipe = recipes.get(pageNumber / 3);
            switch(pageNumber % 3) {
                case 1:
                    tts.say(currentRecipe.getIngredients().get(readIndex).toString());
                    break;
                case 2:
                    tts.say(currentRecipe.getSteps().get(readIndex).toString());
                    break;
            }
            Pane superRoot = new Pane();
            VBox root = new VBox(15);
            renderRecipe(root, pageNumber);
            superRoot.getChildren().add(root);
            Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
            scene.getStylesheets().add("/css/stylesheet.css");
            mainStage.setScene(scene);
        }
        superRoot.getChildren().remove(timerPane);
        timerShowing = false;
    }
    
    public void renderRecipe(VBox root, int pageNumber) {
        System.out.println("Page rendering");
        Recipe recipe = recipes.get(pageNumber / 3);
        root.setMaxWidth(sceneWidth);
        switch(pageNumber % 3) {
            case 0:
                Text title = new Text(recipe.getTitle());
                nodes.add(title);
                title.getStyleClass().add("title");
                root.getChildren().add(title);
                title.setTextAlignment(TextAlignment.CENTER);
                HBox top = new HBox();
                VBox topLeft = new VBox(10);
                //topLeft.getChildren().add(title);
                Text desc = new Text(recipe.getDescription());
                nodes.add(desc);
                desc.setWrappingWidth(300);
                topLeft.getChildren().add(desc);
                Text serves = new Text("Serves " + recipe.getServes());
                nodes.add(serves);
                topLeft.getChildren().add(serves);
                topLeft.getStyleClass().add("topPane");

                top.getChildren().add(topLeft);
                Image image = new Image("images/eggs.jpg");
                switch(pageNumber / 3) {
                    case 0:
                        image = new Image("images/eggs.jpg");
                        break;
                    case 1:
                        image = new Image("images/applecrisp.jpg");
                        break;
                    case 2:
                        image = new Image("images/cake.jpg");
                        break;
                }
                ImageView finalImage = new ImageView(image);
                finalImage.getStyleClass().add("topPane");
                top.getChildren().add(finalImage);
                root.getChildren().add(top);
                break;
            case 1:
                System.out.println("Ingredients");
                Text ingredientHeader = new Text("Ingredients");
                nodes.add(ingredientHeader);
                ingredientHeader.getStyleClass().add ("sectionHeader");
                //ingredientHeader.wrappingWidthProperty().bind(root.widthProperty().multiply(0.9));
                ingredientHeader.setTextAlignment(TextAlignment.CENTER);
                VBox ingredientsPane = new VBox();
                List<Ingredient> recipeIngredients = recipe.getIngredients();
                for(int i = 0; i < recipeIngredients.size(); i++) {
                    Text ingredient = new Text(recipeIngredients.get(i).toString());
                    nodes.add(ingredient);
                    ingredient.wrappingWidthProperty().bind(ingredientsPane.widthProperty());
                    ingredientsPane.getChildren().add(ingredient);
                    if(i == readIndex) {
                        ingredient.getStyleClass().add("focused");
                        ingredient.setFont(Font.font("Times New Roman", FontWeight.EXTRA_BOLD, 30));
                    } else {
                        ingredient.setFont(Font.font("Times New Roman", FontWeight.NORMAL, 24));
                    }
                };
                root.getChildren().addAll(ingredientHeader, ingredientsPane);
                break;
            case 2:
                System.out.println("Steps");
                Text stepHeader = new Text("Steps");
                nodes.add(stepHeader);
                stepHeader.getStyleClass().add("sectionHeader");
                //stepHeader.wrappingWidthProperty().bind(root.widthProperty().multiply(0.9));
                //stepHeader.setTextAlignment(TextAlignment.CENTER);
                VBox stepsPane = new VBox();
                List<Step> recipeSteps = recipe.getSteps();
                for(int i = 0; i < recipeSteps.size(); i++) {
                    Text step = new Text(recipeSteps.get(i).toString());
                    nodes.add(step);
                    step.setWrappingWidth(600);
                    stepsPane.getChildren().add(step);
                    if(i == readIndex) {
                        step.getStyleClass().add("focused");
                        step.setFont(Font.font("Times New Roman", FontWeight.EXTRA_BOLD, 30));
                    } else {
                        step.setFont(Font.font("Times New Roman", FontWeight.NORMAL, 24));
                    }
                };
                root.getChildren().addAll(stepHeader, stepsPane);
                break;
        }
        //cursor
        cursorNode = new Rectangle(0,0,10,10);
        cursorNode.setFill(Color.DODGERBLUE);

        root.getChildren().add(cursorNode);
    }
    
    public void initializeMap() {
        timeToInt.put("one", 1);
        timeToInt.put("two", 2);
        timeToInt.put("three", 3);
        timeToInt.put("four", 4);
        timeToInt.put("five", 5);
        timeToInt.put("six", 6);
        timeToInt.put("seven", 7);
        timeToInt.put("eight", 8);
        timeToInt.put("nine", 9);
        timeToInt.put("ten", 10);
        timeToInt.put("fifteen", 15);
        timeToInt.put("twenty", 20);
        timeToInt.put("thirty", 30);
        timeToInt.put("half an", 30);
        timeToInt.put("forty", 40);
        timeToInt.put("forty five", 45);
        timeToInt.put("fifty", 50);
        timeToInt.put("sixty", 60);
        timeToInt.put("an hour", 60);
        
        recipeIndexMap.put("Eggs", 0);
        recipeIndexMap.put("Apple Crisp (x2)", 3);
        recipeIndexMap.put("Cake", 6);
    }
    
    private void setRecipe(String text) {
        pageNumber = recipeIndexMap.get(text);
        Recipe currentRecipe = recipes.get(pageNumber / 3);
            switch(pageNumber % 3) {
                case 1:
                    tts.say(currentRecipe.getIngredients().get(readIndex).toString());
                    break;
                case 2:
                    tts.say(currentRecipe.getSteps().get(readIndex).toString());
                    break;
            }
            Pane superRoot = new Pane();
            VBox root = new VBox(15);            
            renderRecipe(root, pageNumber);
            superRoot.getChildren().add(root);
            Scene scene = new Scene(superRoot, sceneWidth, sceneHeight);
            scene.getStylesheets().add("/css/stylesheet.css");
            mainStage.setScene(scene);
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        listener.stop();
    }
}
