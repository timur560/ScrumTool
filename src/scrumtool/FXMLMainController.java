/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scrumtool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.PerspectiveCamera;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import javax.imageio.ImageIO;

/**
 * FXML Controller class
 *
 * @author timur
 */
public class FXMLMainController implements Initializable {
    @FXML
    private DatePicker startDate, endDate;
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

    @FXML
    private ToggleButton moTB, tuTB, weTB, thTB, frTB, saTB, suTB;
    
            
            
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

    @FXML
    private void quitApplication(ActionEvent event) {
        if (showConfirmationMessage("Quit application", "Are you sure?", "Confirm").get() == ButtonType.OK) {
            System.exit(0);
        }
    }

    @FXML
    private void showAbout(ActionEvent event) {
        showMessage("Scrum Tool", "by timur560\n(c)2014", "About", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void exportToImageFile(ActionEvent event) {
        exportGraph();
    }

    @FXML
    private void saveAs(ActionEvent event) {
        saveSprintToFile();
    }
    
    @FXML
    private void save(ActionEvent event) {
        // saveSprintToFile();
    }

    @FXML
    private void openSprintFile(ActionEvent event) {
        openSprintFile();
    }
    
    private ObservableList<SprintDay> getSprintDays(LocalDate sDate, LocalDate eDate) {
        ObservableList<SprintDay> result = FXCollections.observableArrayList();

        Integer firstDayHrs = Integer.parseInt(totalTextField.getText());
        
        while (sDate.compareTo(eDate) < 0) {
            if (sDate.getDayOfWeek() == DayOfWeek.MONDAY && !moTB.selectedProperty().getValue()) {sDate = sDate.plusDays(1); continue;}
            else if (sDate.getDayOfWeek() == DayOfWeek.TUESDAY && !tuTB.selectedProperty().getValue()) {sDate = sDate.plusDays(1); continue;}
            else if (sDate.getDayOfWeek() == DayOfWeek.WEDNESDAY && !weTB.selectedProperty().getValue()) {sDate = sDate.plusDays(1); continue;}
            else if (sDate.getDayOfWeek() == DayOfWeek.THURSDAY && !thTB.selectedProperty().getValue()) {sDate = sDate.plusDays(1); continue;}
            else if (sDate.getDayOfWeek() == DayOfWeek.FRIDAY && !frTB.selectedProperty().getValue()) {sDate = sDate.plusDays(1); continue;}
            else if (sDate.getDayOfWeek() == DayOfWeek.SATURDAY && !saTB.selectedProperty().getValue()) {sDate = sDate.plusDays(1); continue;}
            else if (sDate.getDayOfWeek() == DayOfWeek.SUNDAY && !suTB.selectedProperty().getValue()) {sDate = sDate.plusDays(1); continue;}

            result.add(new SprintDay(sDate.getDayOfMonth() + "." + sDate.getMonthValue() + "." + sDate.getYear(), firstDayHrs));
            firstDayHrs = 0;
            sDate = sDate.plusDays(1);
        }

        return result;
    }
    
    private void buildGraph() {
        burnDownChart.getData().clear();

        List<String> categories = new ArrayList<>();

        XYChart.Series series = new XYChart.Series();
        series.setName("Sprint Birndown");

        for (SprintDay sd : progressTable.getItems()) {
            categories.add(sd.dayProperty().getValue());
        }
        
        burnDownChart.getXAxis().setAutoRanging(false); 
        ((CategoryAxis)burnDownChart.getXAxis()).setCategories(FXCollections.<String>observableArrayList(categories)); 
        burnDownChart.getXAxis().invalidateRange(categories);
        
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

        // 3D
//        final Region chartContent = (Region) burnDownChart.lookup(".chart-content");
//        chartContent.setRotationAxis(Rotate.Y_AXIS);
//        chartContent.setRotate(10 - new Random().nextInt(20));

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
    
    public Optional<ButtonType> showConfirmationMessage(String headerText, String contentText, String title) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        return alert.showAndWait();
    }
    
    public void exportGraph() {
        try {
            WritableImage snapShot = burnDownChart.snapshot(null, null);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export to PNG");
            fileChooser.setInitialFileName("Untitled.png");
            File file = fileChooser.showSaveDialog(null);
            
            if (file != null) {
                ImageIO.write(SwingFXUtils.fromFXImage(snapShot, null), "png", file);
            }
        } catch (IOException ex) {
            Logger.getLogger(FXMLMainController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void saveSprintToFile() {
        try {
            WritableImage snapShot = burnDownChart.snapshot(null, null);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save sptint");
            fileChooser.setInitialFileName("Untitled.spt");
            File file = fileChooser.showSaveDialog(null);
            
            if (file != null) {
                PrintWriter in = new PrintWriter(file);
                in.println("sDate=" + startDate.getValue().format(DateTimeFormatter.ISO_DATE));
                in.println("eDate=" + endDate.getValue().format(DateTimeFormatter.ISO_DATE));
                in.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(FXMLMainController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void openSprintFile() {
        // TODO:
    }
}
