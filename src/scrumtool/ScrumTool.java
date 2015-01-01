/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scrumtool;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 *
 * @author timur
 */
public class ScrumTool extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Scrum Tool");
        

        FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLMain.fxml"));
        
        Parent root = loader.load(); // FXMLLoader.load(getClass().getResource("FXMLMain.fxml"));

        FXMLMainController c = loader.getController();
        c.setStage(stage);

        
        Scene scene = new Scene(root);
        scene.setCamera(new PerspectiveCamera());
     
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
