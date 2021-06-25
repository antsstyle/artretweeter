/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui;

import java.awt.BorderLayout;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

/**
 *
 * @author antss
 */
public class ChooseDatePanel extends JPanel {

    private JDatePickerImpl datePicker;
    private JComboBox hourComboBox;
    private JComboBox minuteComboBox;

    public ChooseDatePanel() {
        super();
        initialise();
    }

    private void initialise() {
        setLayout(new BorderLayout());
        UtilDateModel model = new UtilDateModel();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        model.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        model.setSelected(true);
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanelImpl = new JDatePanelImpl(model, p);
        datePicker = new JDatePickerImpl(datePanelImpl, new DateLabelFormatter());
        JPanel dateDisplayPanel = new JPanel();
        dateDisplayPanel.setLayout(new BoxLayout(dateDisplayPanel, BoxLayout.Y_AXIS));
        JPanel timeDisplayPanel = new JPanel();
        JLabel label = new JLabel("<html>Please choose a date at least 1 hour from now.</html>");
        dateDisplayPanel.add(label);
        dateDisplayPanel.add(datePicker);
        timeDisplayPanel.setLayout(new BoxLayout(timeDisplayPanel, BoxLayout.X_AXIS));
        hourComboBox = new JComboBox();
        for (int i = 0; i < 24; i++) {
            hourComboBox.addItem(String.valueOf(i));
        }
        minuteComboBox = new JComboBox();
        for (int i = 0; i < 60; i += 15) {
            minuteComboBox.addItem(String.valueOf(i));
        }
        Integer hour = cal.get(Calendar.HOUR_OF_DAY);

        Integer minute = cal.get(Calendar.MINUTE);
        if (minute < 15) {
            minuteComboBox.setSelectedItem(String.valueOf(15));
        } else if (minute < 30) {
            minuteComboBox.setSelectedItem(String.valueOf(30));
        } else if (minute < 45) {
            minuteComboBox.setSelectedItem(String.valueOf(45));
        } else {
            minuteComboBox.setSelectedItem(String.valueOf(0));
            hour++;
            if (hour == 24) {
                hour = 0;
            }
        }
        hourComboBox.setSelectedItem(String.valueOf(hour));
        timeDisplayPanel.add(hourComboBox);
        timeDisplayPanel.add(minuteComboBox);
        add(dateDisplayPanel, BorderLayout.PAGE_START);
        add(timeDisplayPanel, BorderLayout.PAGE_END);
    }

    public Timestamp getSelectedTime() {
        Date selectedDate = (Date) datePicker.getModel().getValue();
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt((String) hourComboBox.getSelectedItem()));
        cal.set(Calendar.MINUTE, Integer.parseInt((String) minuteComboBox.getSelectedItem()));
        cal.set(Calendar.SECOND, 0);
        return new Timestamp(cal.getTimeInMillis());
    }
}
