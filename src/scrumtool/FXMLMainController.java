/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scrumtool;

import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.NumberStringConverter;

/**
 * FXML Controller class
 *
 * @author timur
 */
public class FXMLMainController implements Initializable {
    @FXML
    private DatePicker startDate;
    @FXML
    private DatePicker endDate;
    @FXML
    private TextField totalTextField;
    @FXML
    private TableView<SprintDay> progressTable;
    @FXML
    private TableColumn<SprintDay, String> dayTableCol;
    @FXML
    private TableColumn<SprintDay, Number> hrsLeftTableCol;
    @FXML
    private LineChart burnDownChart;

    ObservableList<SprintDay> sprintDays;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        
        // Handle TextField text changes.
        totalTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            dateIntervalChanged();
        });
    }    

    private void dateIntervalChanged() {
        LocalDate sDate = startDate.getValue();
        LocalDate eDate = endDate.getValue();
        
        try {
            if (sDate == null || eDate == null || Integer.parseInt(totalTextField.getText()) <= 0) {
                return;
            }
        } catch (NumberFormatException e) {
            return;
        }
        
        if (sDate.compareTo(eDate) >= 0) {
            showMessage("Start date should be greater", "", "Error", Alert.AlertType.ERROR);
            return;
        }
        
        sprintDays = getSprintDays(sDate, eDate);
        
        dayTableCol.setCellValueFactory(new PropertyValueFactory<>("day"));
        hrsLeftTableCol.setCellValueFactory(new PropertyValueFactory<>("hrsLeft"));

        hrsLeftTableCol.setCellFactory(TextFieldTableCell.<SprintDay, Number>forTableColumn(new NumberStringConverter()));
        hrsLeftTableCol.setOnEditCommit(
            new EventHandler<CellEditEvent<SprintDay, Number>>() {
                @Override
                public void handle(CellEditEvent<SprintDay, Number> t) {
                    ((SprintDay) t.getTableView().getItems().get(
                        t.getTablePosition().getRow())
                        ).setHrsLeft(t.getNewValue().intValue());
                }
            }
        );        
        progressTable.setItems(sprintDays);
    }
        
    @FXML
    private void dateIntervalChanged(ActionEvent event) {
        dateIntervalChanged();
    }

    @FXML
    private void buildButtonClick(ActionEvent event) {
        buildGraph();
    }

    private ObservableList<SprintDay> getSprintDays(LocalDate sDate, LocalDate eDate) {
        ObservableList<SprintDay> result = FXCollections.observableArrayList();

        Integer firstDayHrs = Integer.parseInt(totalTextField.getText());
        
        while (sDate.compareTo(eDate) < 0) {
            result.add(new SprintDay(sDate.getDayOfMonth() + "." + sDate.getMonthValue() + "." + sDate.getYear(), firstDayHrs));
            firstDayHrs = 0;
            sDate = sDate.plusDays(1);
        }

        return result;
    }
    
    private void buildGraph() {
        burnDownChart.getData().clear();

        XYChart.Series series = new XYChart.Series();
        series.setName("Sprint Birndown");

        progressTable.getItems().get(0);
        
        for (SprintDay sd : progressTable.getItems()) {
            if (sd.hrsLeftProperty().longValue() == 0) {
                break;
            }
            series.getData().add(new XYChart.Data(sd.dayProperty().getValue(), sd.hrsLeftProperty().longValue()));
        }

        burnDownChart.getData().add(series);

        XYChart.Series series2 = new XYChart.Series();
        series2.setName("Ideal Line");

        series2.getData().add(new XYChart.Data(progressTable.getItems().get(0).dayProperty().getValue(), 
                progressTable.getItems().get(0).hrsLeftProperty().longValue()));
        series2.getData().add(new XYChart.Data(progressTable.getItems().get(progressTable.getItems().size() - 1).dayProperty().getValue(), 0));

        burnDownChart.getData().add(series2);
    }
    
    private void updateGraph() {
        
    }
    
    public void showMessage(String headerText, String contentText, String title, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }
}
