/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scrumtool;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author timur
 */
public class SprintDay {
    private StringProperty day;
    private IntegerProperty hrsLeft;
    
    public SprintDay(String day, Integer hrsLeft) {
        this.day = new SimpleStringProperty(day);
        this.hrsLeft = new SimpleIntegerProperty(hrsLeft);
    }

    
    public void setDay(String value) {
        dayProperty().set(value);
    }
    
    public StringProperty dayProperty() { 
        if (day == null) day = new SimpleStringProperty(this, "day");
        return day; 
    }

    
    public void setHrsLeft(Integer value) {
        hrsLeftProperty().set(value);
    }
    
    public IntegerProperty hrsLeftProperty() { 
        if (hrsLeft == null) hrsLeft = new SimpleIntegerProperty(this, "hrsLeft");
        return hrsLeft; 
    }
}
