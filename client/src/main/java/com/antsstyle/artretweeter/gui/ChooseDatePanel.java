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
        add(datePicker, BorderLayout.PAGE_START);
        JPanel dateDisplayPanel = new JPanel();
        JPanel timeDisplayPanel = new JPanel();
        timeDisplayPanel.setLayout(new BoxLayout(timeDisplayPanel, BoxLayout.X_AXIS));
        hourComboBox = new JComboBox();
        for (int i = 0; i < 24; i++) {
            hourComboBox.addItem(String.valueOf(i));
        }
        minuteComboBox = new JComboBox();
        for (int i = 0; i < 60; i+=15) {
            minuteComboBox.addItem(String.valueOf(i));
        }
        hourComboBox.setSelectedItem(String.valueOf((int) cal.get(Calendar.HOUR_OF_DAY)));
        minuteComboBox.setSelectedItem(String.valueOf((int) cal.get(Calendar.MINUTE)));
        timeDisplayPanel.add(hourComboBox);
        timeDisplayPanel.add(minuteComboBox);
        add(dateDisplayPanel, BorderLayout.PAGE_START);
        add(timeDisplayPanel, BorderLayout.PAGE_END);
    }

    public Timestamp getSelectedTime() {
        Date selectedDate = (Date) datePicker.getModel().getValue();
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, (int) hourComboBox.getSelectedItem());
        cal.set(Calendar.MINUTE, (int) minuteComboBox.getSelectedItem());
        cal.set(Calendar.SECOND, 0);
        return new Timestamp(cal.getTimeInMillis());
    }
}
