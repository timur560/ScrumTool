/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scrumtool;

import javafx.geometry.Insets;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.swing.JOptionPane;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 *
 * @author timur
 */
public class FXMLDocumentController implements Initializable {
    
    @FXML
    private TextField daysTextField, valuesTextField;
    @FXML
    private LineChart burnDownChart;
    
    @FXML
    private void handleButtonAction(ActionEvent event) {
        String[] days = daysTextField.getText().split(",");
        String[] values = valuesTextField.getText().split(",");
        
        if (days.length != values.length) {
            // JOptionPane.showMessageDialog(null, "Error message", "Error", JOptionPane.INFORMATION_MESSAGE);

            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Days and values counts not matches.");
            alert.setContentText("You entered " + days.length + " days and " + values.length + " values.");

            alert.showAndWait();
        } else {
            burnDownChart.getData().clear();
            
            XYChart.Series series = new XYChart.Series();
            series.setName("Sprint Birndown");
            int i = 0;
            for (String day : days) {
                series.getData().add(new XYChart.Data(day, Integer.parseInt(values[i].trim())));
                i++;
            }
            burnDownChart.getData().add(series);

            XYChart.Series series2 = new XYChart.Series();
            series2.setName("Ideal Birndown");
            series2.getData().add(new XYChart.Data(days[0], Integer.parseInt(values[0].trim())));
            series2.getData().add(new XYChart.Data(days[i-1], Integer.parseInt(values[i-1].trim())));

            burnDownChart.getData().add(series2);
        }
        
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    
    
}
