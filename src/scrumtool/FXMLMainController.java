/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scrumtool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

    private Stage stage;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
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
    private void print(ActionEvent event) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();
        
        if (printerJob != null) {
            
            if (printerJob.showPrintDialog(stage.getOwner())) {
                PageLayout pageLayout = printerJob.getJobSettings().getPageLayout();
                double scaleX = pageLayout.getPrintableWidth() / burnDownChart.getBoundsInParent().getWidth();
                double scaleY = pageLayout.getPrintableHeight() / burnDownChart.getBoundsInParent().getHeight();
                burnDownChart.getTransforms().add(new Scale(scaleX, scaleY));
                
                if (printerJob.printPage(burnDownChart)) { // .lookup(".chart")
                    printerJob.endJob();
                }
                
                burnDownChart.getTransforms().clear();
            }
        } else {
            showMessage("No printers", "There are no printers installed in the system.", "Error", Alert.AlertType.ERROR);
        }

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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save sptint");
        fileChooser.setInitialFileName("Untitled.spt");

        file = fileChooser.showSaveDialog(null);
        if (file != null) {
            saveSprintToFile(file);
        }
    }

    @FXML
    private void newSprint(ActionEvent event) {
        if (showConfirmationMessage("Unsaved changes can be lost.", "Dismiss?", "New sprint").get() == ButtonType.OK) {
            startDate.setValue(null);
            endDate.setValue(null);
            totalTextField.setText("");
            
            sprintDays.clear();
            progressTable.setItems(sprintDays);
            
            burnDownChart.getData().clear();
            
            file = null;
            
            stage.setTitle("Scrum Tool");
        }
    }
    
    @FXML
    private void save(ActionEvent event) {
        saveSprintToFile(file);
    }

    @FXML
    private void open(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open sprint file");

        file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            openSprintFile(file);
        }
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
        
        if (progressTable.getItems().size() == 0) {
            return;
        }

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
    
    private static File file = null;
    
    public void saveSprintToFile(File file) {
        try {
            WritableImage snapShot = burnDownChart.snapshot(null, null);

            if (file == null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save sptint");
                fileChooser.setInitialFileName("Untitled.spt");

                file = this.file = fileChooser.showSaveDialog(null);
            }
            
            if (file != null) {
                PrintWriter out = new PrintWriter(file);
                out.println("startDate=" + ((startDate.getValue() == null) ? "" : startDate.getValue().format(DateTimeFormatter.ISO_DATE)));
                out.println("endDate=" + ((endDate.getValue() == null) ? "" : endDate.getValue().format(DateTimeFormatter.ISO_DATE)));
                out.println("total=" + totalTextField.getText());
                List<String> days = new ArrayList<>();
                if (moTB.selectedProperty().getValue()) {days.add("mo");}
                if (tuTB.selectedProperty().getValue()) {days.add("tu");}
                if (weTB.selectedProperty().getValue()) {days.add("we");}
                if (thTB.selectedProperty().getValue()) {days.add("th");}
                if (frTB.selectedProperty().getValue()) {days.add("fr");}
                if (saTB.selectedProperty().getValue()) {days.add("sa");}
                if (suTB.selectedProperty().getValue()) {days.add("su");}

                out.println("days=" + String.join(",", days));
                out.println("tableData:");
                for (SprintDay sd : progressTable.getItems()) {
                    out.println(sd.dayProperty().getValue() + ":" + sd.hrsLeftProperty().getValue());
                }
                out.println("tableData;");
                out.close();
                
                stage.setTitle("Scrum Tool | " + file.getName());
            }
        } catch (IOException ex) {
            Logger.getLogger(FXMLMainController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void openSprintFile(File file) {
        try {
            Scanner in = new Scanner(file);
            String s;
            int i =  0;
            boolean isTableData = false;
            ObservableList<SprintDay> tableData = FXCollections.observableArrayList();
            
            while (in.hasNext() && i++ < 100) {
                s = in.nextLine();
                if (isTableData) {
                    if (s.contains("tableData;")) {
                        progressTable.setItems(tableData);
                        isTableData = false;
                    } else {
                        tableData.add(new SprintDay(s.split(":")[0], Integer.parseInt(s.split(":")[1])));
                    }
                } else if (s.indexOf("startDate=") == 0) {
                    startDate.setValue(LocalDate.parse(s.substring(s.indexOf("=") + 1), DateTimeFormatter.ISO_DATE));
                } else if (s.indexOf("endDate=") == 0) {
                    endDate.setValue(LocalDate.parse(s.substring(s.indexOf("=") + 1), DateTimeFormatter.ISO_DATE));
                } else if (s.indexOf("total=") == 0) {
                    totalTextField.setText(s.substring(s.indexOf("=") + 1));
                } else if (s.indexOf("days=") == 0) {
                    moTB.setSelected(false);
                    tuTB.setSelected(false);
                    weTB.setSelected(false);
                    thTB.setSelected(false);
                    frTB.setSelected(false);
                    saTB.setSelected(false);
                    suTB.setSelected(false);
                    for (String day : s.substring(s.indexOf("=") + 1).split(",")) {
                        switch (day) {
                            case "mo": moTB.setSelected(true); break;
                            case "tu": tuTB.setSelected(true); break;
                            case "we": weTB.setSelected(true); break;
                            case "th": thTB.setSelected(true); break;
                            case "fr": frTB.setSelected(true); break;
                            case "sa": saTB.setSelected(true); break;
                            case "su": suTB.setSelected(true); break;
                        }
                    }
                } else if (s.contains("tableData:")) {
                    isTableData = true;
                }
            }
            
            stage.setTitle("Scrum Tool | " + file.getName());
            
            buildGraph();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FXMLMainController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
