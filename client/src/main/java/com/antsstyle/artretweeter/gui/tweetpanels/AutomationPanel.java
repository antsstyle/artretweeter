/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.antsstyle.artretweeter.gui.tweetpanels;

import com.antsstyle.artretweeter.datastructures.Account;
import com.antsstyle.artretweeter.datastructures.AutomationSettingsHolder;
import com.antsstyle.artretweeter.datastructures.OperationResult;
import com.antsstyle.artretweeter.datastructures.TimeZoneComboBoxHolder;
import com.antsstyle.artretweeter.db.AutomationDB;
import com.antsstyle.artretweeter.db.CoreDB;
import com.antsstyle.artretweeter.db.DBResponse;
import com.antsstyle.artretweeter.db.DBTable;
import com.antsstyle.artretweeter.db.ResultSetConversion;
import com.antsstyle.artretweeter.gui.GUI;
import com.antsstyle.artretweeter.gui.GUIHelperMethods;
import static com.antsstyle.artretweeter.gui.tweetpanels.MainTweetsPanel.DB_ERROR_ACCOUNT;
import static com.antsstyle.artretweeter.gui.tweetpanels.MainTweetsPanel.NO_ACCOUNTS;
import com.antsstyle.artretweeter.serverapi.ServerAPI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author antss
 */
public class AutomationPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = LogManager.getLogger(AutomationPanel.class);

    private Account currentlySelectedAccount;
    private final DefaultComboBoxModel selectAccountBoxModel = new DefaultComboBoxModel();

    private final JCheckBox[] dayCheckBoxes;
    private final JCheckBox[] hourCheckBoxes;
    private final JCheckBox[] minuteCheckBoxes;
    private final DefaultComboBoxModel timeZoneBoxModel = new DefaultComboBoxModel();
    private TimeZoneComboBoxHolder currentlySelectedTimeZone = null;

    /**
     * Creates new form AutomationPanel
     */
    public AutomationPanel() {
        initComponents();
        dayCheckBoxes = new JCheckBox[]{mondayCheckBox, tuesdayCheckBox, wednesdayCheckBox, thursdayCheckBox,
            fridayCheckBox, saturdayCheckBox, sundayCheckBox};
        hourCheckBoxes = new JCheckBox[]{time0001CheckBox, time0102CheckBox, time0203CheckBox, time0304CheckBox, time0405CheckBox,
            time0506CheckBox, time0607CheckBox, time0708CheckBox, time0809CheckBox, time0910CheckBox, time1011CheckBox,
            time1112CheckBox, time1213CheckBox, time1314CheckBox, time1415CheckBox, time1516CheckBox, time1617CheckBox,
            time1718CheckBox, time1819CheckBox, time1920CheckBox, time2021CheckBox, time2122CheckBox, time2223CheckBox,
            time2300CheckBox};
        minuteCheckBoxes = new JCheckBox[]{min0CheckBox, min15CheckBox, min30CheckBox, min45CheckBox};
    }

    public void initialise() {
        refreshAccountBoxModel(true);
        populateTimeZones();
        setDaylightSavingsComponents();
        dateWrapperPanel.setJDatePickerSize();
    }

    private void populateTimeZones() {
        timeZoneBoxModel.removeAllElements();
        String[] ids = TimeZone.getAvailableIDs();
        ArrayList<Double> totalOffsets = new ArrayList<>();
        TreeSet<TimeZoneComboBoxHolder> timeZoneList = new TreeSet<>();
        TimeZone currentTimeZone = TimeZone.getDefault();
        Integer currentTimeZoneHours = (int) TimeUnit.MILLISECONDS.toHours(currentTimeZone.getRawOffset());
        Integer currentTimeZoneMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(currentTimeZone.getRawOffset())
                - (int) TimeUnit.HOURS.toMinutes(currentTimeZoneHours);
        Date d = new Date();
        if (currentTimeZone.inDaylightTime(d)) {
            currentTimeZoneHours++;
        }
        String timeZoneAsString = "<html>Your system timezone is UTC";
        if (currentTimeZoneHours >= 0) {
            timeZoneAsString = timeZoneAsString.concat("+").concat(String.valueOf(currentTimeZoneHours))
                    .concat(":").concat(String.format("%02d", currentTimeZoneMinutes)).concat(".");
        } else {
            timeZoneAsString = timeZoneAsString.concat("-").concat(String.valueOf(currentTimeZoneHours))
                    .concat(":").concat(String.format("%02d", currentTimeZoneMinutes)).concat(".");
        }
        if (currentTimeZone.inDaylightTime(d)) {
            timeZoneAsString = timeZoneAsString.concat(" (Daylight savings time is active.)");
        }
        timeZoneAsString = timeZoneAsString.concat("</html>");
        systemTimeZoneLabel.setText(timeZoneAsString);
        TimeZoneComboBoxHolder timeZoneToSelect = null;
        DBResponse resp = CoreDB.selectFromTable(DBTable.USERAUTOMATIONSETTINGS,
                new String[]{"USERTWITTERID"},
                new Object[]{currentlySelectedAccount.getTwitterID()});
        if (resp.wasSuccessful()) {
            ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
            if (!rows.isEmpty()) {
                AutomationSettingsHolder holder = ResultSetConversion.getAutomationSettingsHolder(rows.get(0));
                currentTimeZoneHours = holder.getTimeZoneHourOffset();
                currentTimeZoneMinutes = holder.getTimeZoneMinuteOffset();
            }
        } else {
            LOGGER.error("Failed to get user timezone automation settings from DB!");
        }
        for (String id : ids) {
            TimeZone timeZone = TimeZone.getTimeZone(id);
            Integer hours = (int) TimeUnit.MILLISECONDS.toHours(timeZone.getRawOffset());
            Integer minutes = (int) TimeUnit.MILLISECONDS.toMinutes(timeZone.getRawOffset())
                    - (int) TimeUnit.HOURS.toMinutes(hours);
            Integer absMinutes = Math.abs(minutes);
            Double totalOffset;
            if (hours >= 0) {
                totalOffset = hours + (absMinutes / 60.0);
            } else {
                totalOffset = hours - (absMinutes / 60.0);
            }
            if (totalOffsets.contains(totalOffset)) {
                continue;
            }
            totalOffsets.add(totalOffset);
            String formattedString;
            if (hours >= 0) {
                formattedString = String.format("(UTC+%d:%02d) %s", hours, absMinutes, timeZone.getID());
            } else {
                formattedString = String.format("(UTC%d:%02d) %s", hours, absMinutes, timeZone.getID());
            }

            TimeZoneComboBoxHolder holder = new TimeZoneComboBoxHolder()
                    .setTimeZone(timeZone)
                    .setFormattedString(formattedString)
                    .setHourOffset(hours)
                    .setMinuteOffset(minutes);
            timeZoneList.add(holder);
            if (hours.equals(currentTimeZoneHours) && minutes.equals(currentTimeZoneMinutes)) {
                timeZoneToSelect = holder;
            }
        }
        for (TimeZoneComboBoxHolder holder : timeZoneList) {
            timeZoneBoxModel.addElement(holder);
        }
        if (timeZoneToSelect != null) {
            timeZoneComboBox.setSelectedItem(timeZoneToSelect);
        }
    }

    private void refreshAutomationGUIConfig() {
        if (currentlySelectedAccount.equals(MainTweetsPanel.NO_ACCOUNTS) || currentlySelectedAccount.equals(MainTweetsPanel.DB_ERROR_ACCOUNT)) {
            return;
        }
        DBResponse resp = CoreDB.selectFromTable(DBTable.USERAUTOMATIONSETTINGS,
                new String[]{"usertwitterid"},
                new Object[]{currentlySelectedAccount.getTwitterID()});
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to retrieve user automation settings from DB!");
            return;
        }
        if (resp.getReturnedRows().isEmpty()) {
            return;
        }
        AutomationSettingsHolder holder = ResultSetConversion.getAutomationSettingsHolder(resp.getReturnedRows().get(0));
        String dayFlags = holder.getDayFlags();
        for (int i = 0; i < dayFlags.length(); i++) {
            String flag = String.valueOf(dayFlags.charAt(i));
            dayCheckBoxes[i].setSelected(flag.equals("Y"));
        }
        String hourFlags = holder.getHourFlags();
        for (int i = 0; i < hourFlags.length(); i++) {
            String flag = String.valueOf(hourFlags.charAt(i));
            hourCheckBoxes[i].setSelected(flag.equals("Y"));
        }
        String minuteFlags = holder.getMinuteFlags();
        for (int i = 0; i < minuteFlags.length(); i++) {
            String flag = String.valueOf(minuteFlags.charAt(i));
            minuteCheckBoxes[i].setSelected(flag.equals("Y"));
        }
        enableAutomatedRetweetingCheckBox.setSelected(holder.getAutomationEnabled().equals("Y"));
        ignoreOldTweetsCheckBox.setSelected(holder.getOldTweetCutoffDateEnabled().equals("Y"));
        excludeTweetTextCheckBox.setSelected(holder.getExcludedTextEnabled().equals("Y"));
        includeTweetTextCheckBox.setSelected(holder.getIncludedTextEnabled().equals("Y"));
        excludeTweetTextField.setText(holder.getExcludedText() == null ? "" : holder.getExcludedText());
        includeTweetTextField.setText(holder.getIncludedText() == null ? "" : holder.getIncludedText());
        retweetPercentTextField.setText(String.valueOf(holder.getRetweetPercent()));
        includeTextConditionComboBox.setSelectedItem(holder.getIncludeTextCondition());
        excludeTextConditionComboBox.setSelectedItem(holder.getExcludeTextCondition());
        Calendar cal = Calendar.getInstance();
        if (holder.getOldTweetCutoffDate() != null) {
            cal.setTimeInMillis(holder.getOldTweetCutoffDate().getTime());
        }
        dateWrapperPanel.getDatePicker().getModel().setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        populateTimeZones();
    }


    protected boolean noAccountsInBoxModel() {
        if (selectAccountBoxModel.getSize() == 0) {
            return true;
        } else if (selectAccountBoxModel.getSize() == 1) {
            Account account = (Account) selectAccountBoxModel.getSelectedItem();
            return (account.equals(NO_ACCOUNTS) || account.equals(DB_ERROR_ACCOUNT));
        }
        return false;
    }

    public void refreshAccountBoxModel(boolean initialRefresh) {
        selectAccountComboBox.setEnabled(false);
        boolean noElementsBefore = noAccountsInBoxModel();
        selectAccountBoxModel.removeAllElements();
        DBResponse resp = CoreDB.selectFromTable(DBTable.ACCOUNTS);
        if (!resp.wasSuccessful()) {
            LOGGER.error("Failed to get collections data to refresh combo box model!");
            currentlySelectedAccount = DB_ERROR_ACCOUNT;
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
            selectAccountComboBox.setEnabled(true);
            selectAccountBoxModel.removeAllElements();
            return;
        }
        ArrayList<HashMap<String, Object>> rows = resp.getReturnedRows();
        for (HashMap<String, Object> row : rows) {
            Account account = ResultSetConversion.getAccount(row);
            selectAccountBoxModel.addElement(account);
        }
        if (selectAccountBoxModel.getSize() != 0) {
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
            currentlySelectedAccount = (Account) selectAccountBoxModel.getElementAt(0);
            if (initialRefresh || noElementsBefore) {
                refreshAutomationGUIConfig();
            }
        } else {
            selectAccountBoxModel.addElement(NO_ACCOUNTS);
            currentlySelectedAccount = NO_ACCOUNTS;
            selectAccountBoxModel.setSelectedItem(selectAccountBoxModel.getElementAt(0));
        }
        selectAccountComboBox.setEnabled(true);
    }

    private boolean checkIfNoDaysSelected() {
        for (JCheckBox checkBox : dayCheckBoxes) {
            if (checkBox.isSelected()) {
                return false;
            }
        }
        return true;
    }

    private boolean checkIfNoTimesSelected() {
        for (JCheckBox checkBox : hourCheckBoxes) {
            if (checkBox.isSelected()) {
                return false;
            }
        }
        return true;
    }

    private boolean checkIfNoMinutesSelected() {
        for (JCheckBox checkBox : minuteCheckBoxes) {
            if (checkBox.isSelected()) {
                return false;
            }
        }
        return true;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        selectAccountComboBox = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        mainAutomationSettingsPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        excludeTweetTextCheckBox = new javax.swing.JCheckBox();
        dateWrapperPanel = new com.antsstyle.artretweeter.gui.DateWrapperPanel();
        ignoreOldTweetsCheckBox = new javax.swing.JCheckBox();
        includeTweetTextField = new javax.swing.JTextField();
        includeTweetTextCheckBox = new javax.swing.JCheckBox();
        excludeTweetTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        retweetPercentTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        mainSettingsExplanationLabel = new javax.swing.JLabel();
        includeTextConditionComboBox = new javax.swing.JComboBox<>();
        excludeTextConditionComboBox = new javax.swing.JComboBox<>();
        jPanel5 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        time0001CheckBox = new javax.swing.JCheckBox();
        time0102CheckBox = new javax.swing.JCheckBox();
        time0203CheckBox = new javax.swing.JCheckBox();
        time0304CheckBox = new javax.swing.JCheckBox();
        time0405CheckBox = new javax.swing.JCheckBox();
        time0506CheckBox = new javax.swing.JCheckBox();
        time0607CheckBox = new javax.swing.JCheckBox();
        time0708CheckBox = new javax.swing.JCheckBox();
        time0809CheckBox = new javax.swing.JCheckBox();
        time0910CheckBox = new javax.swing.JCheckBox();
        time1011CheckBox = new javax.swing.JCheckBox();
        time1112CheckBox = new javax.swing.JCheckBox();
        time1213CheckBox = new javax.swing.JCheckBox();
        time1314CheckBox = new javax.swing.JCheckBox();
        time1415CheckBox = new javax.swing.JCheckBox();
        time1516CheckBox = new javax.swing.JCheckBox();
        time1617CheckBox = new javax.swing.JCheckBox();
        time1718CheckBox = new javax.swing.JCheckBox();
        time1819CheckBox = new javax.swing.JCheckBox();
        time1920CheckBox = new javax.swing.JCheckBox();
        time2021CheckBox = new javax.swing.JCheckBox();
        time2122CheckBox = new javax.swing.JCheckBox();
        time2223CheckBox = new javax.swing.JCheckBox();
        time2300CheckBox = new javax.swing.JCheckBox();
        selectAllTimesButton = new javax.swing.JButton();
        selectNoneTimesButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        min0CheckBox = new javax.swing.JCheckBox();
        min45CheckBox = new javax.swing.JCheckBox();
        min30CheckBox = new javax.swing.JCheckBox();
        min15CheckBox = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        fridayCheckBox = new javax.swing.JCheckBox();
        saturdayCheckBox = new javax.swing.JCheckBox();
        wednesdayCheckBox = new javax.swing.JCheckBox();
        mondayCheckBox = new javax.swing.JCheckBox();
        tuesdayCheckBox = new javax.swing.JCheckBox();
        thursdayCheckBox = new javax.swing.JCheckBox();
        sundayCheckBox = new javax.swing.JCheckBox();
        selectAllDaysButton = new javax.swing.JButton();
        selectNoneDaysButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        dateAndTimeExplanationLabel = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        timeZoneComboBox = new javax.swing.JComboBox<>();
        timeZoneMessageLabel = new javax.swing.JLabel();
        systemTimeZoneLabel = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        enableAutomatedRetweetingCheckBox = new javax.swing.JCheckBox();
        saveChangesButton = new javax.swing.JButton();

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setText("Automation");

        selectAccountComboBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        selectAccountComboBox.setModel(selectAccountBoxModel);
        selectAccountComboBox.setMaximumSize(new java.awt.Dimension(250, 26));
        selectAccountComboBox.setMinimumSize(new java.awt.Dimension(250, 26));
        selectAccountComboBox.setPreferredSize(new java.awt.Dimension(250, 26));
        selectAccountComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAccountComboBoxActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel4.setText("Select account: ");

        jPanel3.setBorder(javax.swing.BorderFactory.createCompoundBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true), new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        jPanel3.setMaximumSize(new java.awt.Dimension(867, 239));
        jPanel3.setMinimumSize(new java.awt.Dimension(867, 239));

        excludeTweetTextCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        excludeTweetTextCheckBox.setText("Exclude all tweets that contain this text:");
        excludeTweetTextCheckBox.setMaximumSize(new java.awt.Dimension(450, 32));
        excludeTweetTextCheckBox.setMinimumSize(new java.awt.Dimension(450, 32));
        excludeTweetTextCheckBox.setPreferredSize(new java.awt.Dimension(450, 32));

        dateWrapperPanel.setMaximumSize(new java.awt.Dimension(318, 32));
        dateWrapperPanel.setMinimumSize(new java.awt.Dimension(318, 32));
        dateWrapperPanel.setPreferredSize(new java.awt.Dimension(318, 32));

        ignoreOldTweetsCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        ignoreOldTweetsCheckBox.setText("Ignore tweets posted before this date:");
        ignoreOldTweetsCheckBox.setMaximumSize(new java.awt.Dimension(450, 32));
        ignoreOldTweetsCheckBox.setMinimumSize(new java.awt.Dimension(450, 32));
        ignoreOldTweetsCheckBox.setPreferredSize(new java.awt.Dimension(450, 32));

        includeTweetTextCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        includeTweetTextCheckBox.setText("Include only tweets that contain this text (hashtags not allowed):");
        includeTweetTextCheckBox.setMaximumSize(new java.awt.Dimension(450, 32));
        includeTweetTextCheckBox.setMinimumSize(new java.awt.Dimension(450, 32));
        includeTweetTextCheckBox.setPreferredSize(new java.awt.Dimension(450, 32));

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel3.setText("  Exclude tweets in the bottom % of your tweet engagement (20-75%):");
        jLabel3.setMaximumSize(new java.awt.Dimension(450, 32));
        jLabel3.setMinimumSize(new java.awt.Dimension(450, 32));
        jLabel3.setPreferredSize(new java.awt.Dimension(450, 32));

        jLabel5.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Search Settings");

        mainSettingsExplanationLabel.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        mainSettingsExplanationLabel.setText("<html>These settings allow you to enable or disable automated retweeting, and set filters on which tweets will be scheduled for retweeting.</html>");
        mainSettingsExplanationLabel.setMaximumSize(new java.awt.Dimension(847, 39));
        mainSettingsExplanationLabel.setMinimumSize(new java.awt.Dimension(847, 39));
        mainSettingsExplanationLabel.setPreferredSize(new java.awt.Dimension(847, 39));

        includeTextConditionComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Any of these words", "All of these words", "This exact phrase" }));
        includeTextConditionComboBox.setMinimumSize(new java.awt.Dimension(163, 32));
        includeTextConditionComboBox.setName("[163, 32]"); // NOI18N
        includeTextConditionComboBox.setPreferredSize(new java.awt.Dimension(163, 32));

        excludeTextConditionComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Any of these words", "All of these words", "This exact phrase" }));
        excludeTextConditionComboBox.setMinimumSize(new java.awt.Dimension(163, 32));
        excludeTextConditionComboBox.setName("[163, 32]"); // NOI18N
        excludeTextConditionComboBox.setPreferredSize(new java.awt.Dimension(163, 32));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                .addComponent(ignoreOldTweetsCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(dateWrapperPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(includeTweetTextCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(excludeTweetTextCheckBox, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(includeTweetTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(retweetPercentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(excludeTweetTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(includeTextConditionComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(excludeTextConditionComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(mainSettingsExplanationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainSettingsExplanationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(dateWrapperPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ignoreOldTweetsCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(excludeTweetTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(excludeTweetTextCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(includeTextConditionComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(excludeTextConditionComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(includeTweetTextCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(includeTweetTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(retweetPercentTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createCompoundBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true), new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        jPanel5.setMaximumSize(new java.awt.Dimension(867, 412));
        jPanel5.setMinimumSize(new java.awt.Dimension(867, 412));
        jPanel5.setPreferredSize(new java.awt.Dimension(867, 412));

        time0001CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0001CheckBox.setText("00:00 - 01:00");
        time0001CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0001CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0001CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0102CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0102CheckBox.setText("01:00 - 02:00");
        time0102CheckBox.setToolTipText("");
        time0102CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0102CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0102CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0203CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0203CheckBox.setText("02:00 - 03:00");
        time0203CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0203CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0203CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0304CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0304CheckBox.setText("03:00 - 04:00");
        time0304CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0304CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0304CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0405CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0405CheckBox.setText("04:00 - 05:00");
        time0405CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0405CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0405CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0506CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0506CheckBox.setText("05:00 - 06:00");
        time0506CheckBox.setToolTipText("");
        time0506CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0506CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0506CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0607CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0607CheckBox.setText("06:00 - 07:00");
        time0607CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0607CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0607CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0708CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0708CheckBox.setText("07:00 - 08:00");
        time0708CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0708CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0708CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0809CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0809CheckBox.setText("08:00 - 09:00");
        time0809CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0809CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0809CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time0910CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time0910CheckBox.setText("09:00 - 10:00");
        time0910CheckBox.setToolTipText("");
        time0910CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time0910CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time0910CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1011CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1011CheckBox.setText("10:00 - 11:00");
        time1011CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1011CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1011CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1112CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1112CheckBox.setText("11:00 - 12:00");
        time1112CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1112CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1112CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1213CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1213CheckBox.setText("12:00 - 13:00");
        time1213CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1213CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1213CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1314CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1314CheckBox.setText("13:00 - 14:00");
        time1314CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1314CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1314CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1415CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1415CheckBox.setText("14:00 - 15:00");
        time1415CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1415CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1415CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1516CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1516CheckBox.setText("15:00 - 16:00");
        time1516CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1516CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1516CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1617CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1617CheckBox.setText("16:00 - 17:00");
        time1617CheckBox.setToolTipText("");
        time1617CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1617CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1617CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1718CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1718CheckBox.setText("17:00 - 18:00");
        time1718CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1718CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1718CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1819CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1819CheckBox.setText("18:00 - 19:00");
        time1819CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1819CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1819CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time1920CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time1920CheckBox.setText("19:00 - 20:00");
        time1920CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time1920CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time1920CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time2021CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time2021CheckBox.setText("20:00 - 21:00");
        time2021CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time2021CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time2021CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time2122CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time2122CheckBox.setText("21:00 - 22:00");
        time2122CheckBox.setToolTipText("");
        time2122CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time2122CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time2122CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time2223CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time2223CheckBox.setText("22:00 - 23:00");
        time2223CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time2223CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time2223CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        time2300CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        time2300CheckBox.setText("23:00 - 00:00");
        time2300CheckBox.setMaximumSize(new java.awt.Dimension(135, 24));
        time2300CheckBox.setMinimumSize(new java.awt.Dimension(135, 24));
        time2300CheckBox.setPreferredSize(new java.awt.Dimension(135, 24));

        selectAllTimesButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        selectAllTimesButton.setText("Select all");
        selectAllTimesButton.setMaximumSize(new java.awt.Dimension(102, 33));
        selectAllTimesButton.setMinimumSize(new java.awt.Dimension(102, 33));
        selectAllTimesButton.setPreferredSize(new java.awt.Dimension(102, 33));
        selectAllTimesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllTimesButtonActionPerformed(evt);
            }
        });

        selectNoneTimesButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        selectNoneTimesButton.setText("Select none");
        selectNoneTimesButton.setMaximumSize(new java.awt.Dimension(126, 33));
        selectNoneTimesButton.setMinimumSize(new java.awt.Dimension(126, 33));
        selectNoneTimesButton.setPreferredSize(new java.awt.Dimension(126, 33));
        selectNoneTimesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectNoneTimesButtonActionPerformed(evt);
            }
        });

        min0CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        min0CheckBox.setText("0 past hour");
        min0CheckBox.setMaximumSize(new java.awt.Dimension(119, 24));
        min0CheckBox.setMinimumSize(new java.awt.Dimension(119, 24));
        min0CheckBox.setPreferredSize(new java.awt.Dimension(119, 24));

        min45CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        min45CheckBox.setText("45 past hour");
        min45CheckBox.setMaximumSize(new java.awt.Dimension(119, 24));
        min45CheckBox.setMinimumSize(new java.awt.Dimension(119, 24));
        min45CheckBox.setPreferredSize(new java.awt.Dimension(119, 24));

        min30CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        min30CheckBox.setText("30 past hour");
        min30CheckBox.setMaximumSize(new java.awt.Dimension(119, 24));
        min30CheckBox.setMinimumSize(new java.awt.Dimension(119, 24));
        min30CheckBox.setPreferredSize(new java.awt.Dimension(119, 24));

        min15CheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        min15CheckBox.setText("15 past hour");
        min15CheckBox.setMaximumSize(new java.awt.Dimension(119, 24));
        min15CheckBox.setMinimumSize(new java.awt.Dimension(119, 24));
        min15CheckBox.setPreferredSize(new java.awt.Dimension(119, 24));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(min0CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(min45CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(min30CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(min15CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(min0CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(min15CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(min30CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(min45CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(time0102CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0203CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0405CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0506CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0607CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0708CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0001CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0304CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(41, 41, 41)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(time0809CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time0910CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time1011CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time1213CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time1314CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time1415CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time1516CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(time1112CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(41, 41, 41)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(time2300CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(time1718CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(time1819CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(time1920CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(time2021CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(time2122CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(time2223CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(time1617CheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(28, 28, 28)
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(135, 135, 135)
                        .addComponent(selectAllTimesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(selectNoneTimesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(time1617CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1718CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1819CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1920CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time2021CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time2122CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time2223CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(48, 48, 48))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(time0809CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0910CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1011CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1112CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1213CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1314CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time1415CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(time1516CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(time2300CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(time0001CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0102CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0203CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0304CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0405CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0506CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0607CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(time0708CheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, Short.MAX_VALUE)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(selectAllTimesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(selectNoneTimesButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        fridayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        fridayCheckBox.setText("Friday");

        saturdayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        saturdayCheckBox.setText("Saturday");

        wednesdayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        wednesdayCheckBox.setText("Wednesday");

        mondayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        mondayCheckBox.setText("Monday");
        mondayCheckBox.setMaximumSize(new java.awt.Dimension(120, 24));

        tuesdayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        tuesdayCheckBox.setText("Tuesday");

        thursdayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        thursdayCheckBox.setText("Thursday");

        sundayCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        sundayCheckBox.setText("Sunday");

        selectAllDaysButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        selectAllDaysButton.setText("Select all");
        selectAllDaysButton.setMaximumSize(new java.awt.Dimension(120, 33));
        selectAllDaysButton.setMinimumSize(new java.awt.Dimension(120, 33));
        selectAllDaysButton.setPreferredSize(new java.awt.Dimension(120, 33));
        selectAllDaysButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectAllDaysButtonActionPerformed(evt);
            }
        });

        selectNoneDaysButton.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        selectNoneDaysButton.setText("Select none");
        selectNoneDaysButton.setMaximumSize(new java.awt.Dimension(120, 33));
        selectNoneDaysButton.setMinimumSize(new java.awt.Dimension(120, 33));
        selectNoneDaysButton.setPreferredSize(new java.awt.Dimension(120, 33));
        selectNoneDaysButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectNoneDaysButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(selectAllDaysButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mondayCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(tuesdayCheckBox)
                            .addComponent(thursdayCheckBox)
                            .addComponent(fridayCheckBox)
                            .addComponent(saturdayCheckBox)
                            .addComponent(sundayCheckBox)
                            .addComponent(selectNoneDaysButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(wednesdayCheckBox))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(mondayCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tuesdayCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wednesdayCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(thursdayCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fridayCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saturdayCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sundayCheckBox)
                .addGap(18, 18, 18)
                .addComponent(selectAllDaysButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectNoneDaysButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Date & Time Settings");
        jLabel2.setMaximumSize(new java.awt.Dimension(841, 22));
        jLabel2.setMinimumSize(new java.awt.Dimension(841, 22));
        jLabel2.setPreferredSize(new java.awt.Dimension(841, 22));

        dateAndTimeExplanationLabel.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        dateAndTimeExplanationLabel.setText("<html>These settings control which hours and minutes of the day ArtRetweeter will schedule your automated retweets for. Only the select hours and times past the hour will be used (you must pick at least one hour and at least one minute setting).</html>");
        dateAndTimeExplanationLabel.setMaximumSize(new java.awt.Dimension(841, 67));
        dateAndTimeExplanationLabel.setMinimumSize(new java.awt.Dimension(841, 67));
        dateAndTimeExplanationLabel.setPreferredSize(new java.awt.Dimension(841, 67));

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("Hour Intervals");

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Minutes");

        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Days");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dateAndTimeExplanationLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 847, Short.MAX_VALUE)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel5Layout.createSequentialGroup()
                                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel5Layout.createSequentialGroup()
                                    .addGap(6, 6, 6)
                                    .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(26, 26, 26)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 466, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(28, 28, 28)
                                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(0, 24, Short.MAX_VALUE)))
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dateAndTimeExplanationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createCompoundBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true), new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED)));
        jPanel6.setMaximumSize(new java.awt.Dimension(278, 395));
        jPanel6.setMinimumSize(new java.awt.Dimension(278, 395));
        jPanel6.setPreferredSize(new java.awt.Dimension(278, 395));

        jLabel6.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Timezone Settings");
        jLabel6.setMaximumSize(new java.awt.Dimension(273, 30));
        jLabel6.setMinimumSize(new java.awt.Dimension(273, 30));
        jLabel6.setPreferredSize(new java.awt.Dimension(273, 30));

        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel7.setText("<html>Select the timezone you want here (the hours you have selected will be in relation to your selected timezone).</html>");
        jLabel7.setMaximumSize(new java.awt.Dimension(266, 63));
        jLabel7.setMinimumSize(new java.awt.Dimension(266, 63));
        jLabel7.setPreferredSize(new java.awt.Dimension(266, 63));

        timeZoneComboBox.setModel(timeZoneBoxModel);
        timeZoneComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeZoneComboBoxActionPerformed(evt);
            }
        });

        timeZoneMessageLabel.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        timeZoneMessageLabel.setText("<html>Note that ArtRetweeter ignores daylight savings time; if you want to use your normal timezone without DST, choose a timezone 1 hour behind.</html>");
        timeZoneMessageLabel.setMaximumSize(new java.awt.Dimension(266, 246));
        timeZoneMessageLabel.setMinimumSize(new java.awt.Dimension(266, 246));
        timeZoneMessageLabel.setPreferredSize(new java.awt.Dimension(266, 246));

        systemTimeZoneLabel.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        systemTimeZoneLabel.setText("Your system timezone is");
        systemTimeZoneLabel.setMaximumSize(new java.awt.Dimension(273, 55));
        systemTimeZoneLabel.setMinimumSize(new java.awt.Dimension(273, 55));
        systemTimeZoneLabel.setPreferredSize(new java.awt.Dimension(273, 55));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(timeZoneComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(timeZoneMessageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(systemTimeZoneLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(timeZoneComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(systemTimeZoneLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(timeZoneMessageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(65, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout mainAutomationSettingsPanelLayout = new javax.swing.GroupLayout(mainAutomationSettingsPanel);
        mainAutomationSettingsPanel.setLayout(mainAutomationSettingsPanelLayout);
        mainAutomationSettingsPanelLayout.setHorizontalGroup(
            mainAutomationSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainAutomationSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainAutomationSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(mainAutomationSettingsPanelLayout.createSequentialGroup()
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 293, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        mainAutomationSettingsPanelLayout.setVerticalGroup(
            mainAutomationSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainAutomationSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainAutomationSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel8.setText("<html>ArtRetweeter can automatically schedule some of your tweets to be retweeted (it will only use tweets that have images in them). You can enable this in the settings below. Note that ArtRetweeter schedules retweets once a day, and will schedule retweets for two days in advance; it will not begin immediately.\n\n<br/><br/>\n\nWhen automation is enabled, you do not need to keep this application open for it to work. The ArtRetweeter server will schedule and retweet with your given settings until you change them here.\n\n</html>");
        jLabel8.setMaximumSize(new java.awt.Dimension(506, 60));
        jLabel8.setMinimumSize(new java.awt.Dimension(506, 60));
        jLabel8.setPreferredSize(new java.awt.Dimension(506, 60));

        enableAutomatedRetweetingCheckBox.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        enableAutomatedRetweetingCheckBox.setText("Enable automated retweeting");
        enableAutomatedRetweetingCheckBox.setMaximumSize(new java.awt.Dimension(225, 24));
        enableAutomatedRetweetingCheckBox.setMinimumSize(new java.awt.Dimension(225, 24));
        enableAutomatedRetweetingCheckBox.setPreferredSize(new java.awt.Dimension(225, 24));

        saveChangesButton.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        saveChangesButton.setText("Save all changes");
        saveChangesButton.setMaximumSize(new java.awt.Dimension(154, 91));
        saveChangesButton.setMinimumSize(new java.awt.Dimension(154, 91));
        saveChangesButton.setPreferredSize(new java.awt.Dimension(154, 91));
        saveChangesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveChangesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(jLabel1)
                                    .addGap(47, 47, 47)
                                    .addComponent(jLabel4)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                    .addGap(6, 6, 6)
                                    .addComponent(enableAutomatedRetweetingCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 972, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveChangesButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(mainAutomationSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(selectAccountComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveChangesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(enableAutomatedRetweetingCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainAutomationSettingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void selectAccountComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAccountComboBoxActionPerformed
        if (selectAccountComboBox.isEnabled()) {
            Account acc = (Account) selectAccountComboBox.getSelectedItem();
            if (!acc.equals(currentlySelectedAccount)) {
                currentlySelectedAccount = acc;
                refreshAutomationGUIConfig();
            }
        }
    }//GEN-LAST:event_selectAccountComboBoxActionPerformed

    private void selectAllDaysButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllDaysButtonActionPerformed
        for (JCheckBox checkBox : dayCheckBoxes) {
            checkBox.setSelected(true);
        }
    }//GEN-LAST:event_selectAllDaysButtonActionPerformed

    private void selectNoneDaysButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectNoneDaysButtonActionPerformed
        for (JCheckBox checkBox : dayCheckBoxes) {
            checkBox.setSelected(false);
        }
    }//GEN-LAST:event_selectNoneDaysButtonActionPerformed

    private void saveChangesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveChangesButtonActionPerformed
        if (currentlySelectedAccount.equals(MainTweetsPanel.NO_ACCOUNTS) || currentlySelectedAccount.equals(MainTweetsPanel.DB_ERROR_ACCOUNT)) {
            return;
        }
        String retweetPercentString = retweetPercentTextField.getText().trim();
        Integer retweetPercent;
        try {
            retweetPercent = Integer.parseInt(retweetPercentString);
        } catch (Exception e) {
            String msg = "You must enter a valid number for the retweet percentage (whole numbers only, between 10-50).";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (retweetPercent < 20 || retweetPercent > 75) {
            String msg = "You must enter a valid number for the retweet percentage (whole numbers only, between 20-75).";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String includeText = includeTweetTextField.getText().trim();
        String excludeText = excludeTweetTextField.getText().trim();
        String[] includeWords = StringUtils.split(includeText, " ");
        String[] excludeWords = StringUtils.split(excludeText, " ");
        if (checkIfNoDaysSelected()) {
            String msg = "You must select at least one day.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (checkIfNoTimesSelected()) {
            String msg = "You must select at least one time interval.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (checkIfNoMinutesSelected()) {
            String msg = "You must select at least one minute interval.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (includeText.contains("#")) {
            String msg = "Included words may not have hash symbols or hashtags.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (includeWords.length > 5 || includeText.length() > 100) {
            String msg = "You can only include a maximum of 5 words, max 100 characters.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (excludeWords.length > 5 || excludeText.length() > 100) {
            String msg = "You can only exclude a maximum of 5 words or hashtags, max 100 characters.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (includeText.contains(";")) {
            String msg = "No semicolons can be present in the words to include.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (excludeText.contains(";")) {
            String msg = "No semicolons can be present in the words to exclude.";
            JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!enableAutomatedRetweetingCheckBox.isSelected()) {
            DBResponse selectExistingResp = CoreDB.selectFromTable(DBTable.USERAUTOMATIONSETTINGS,
                    new String[]{"usertwitterid"},
                    new Object[]{currentlySelectedAccount.getTwitterID()});
            if (!selectExistingResp.wasSuccessful()) {
                String msg = "Failed to retrieve existing automation settings for this user from DB - check log output.";
                JOptionPane.showMessageDialog(GUI.getInstance(), msg, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!selectExistingResp.getReturnedRows().isEmpty()) {
                ArrayList<HashMap<String, Object>> existingRows = selectExistingResp.getReturnedRows();
                AutomationSettingsHolder currentSettings = ResultSetConversion.getAutomationSettingsHolder(existingRows.get(0));
                if (currentSettings.getAutomationEnabled().equals("Y")) {
                    String msg = "Automation is currently enabled. If you disable it, currently scheduled retweets will be unqueued."
                            + "<br/><br/> If this is fine, press OK to continue.";
                    int promptResult = JOptionPane.showConfirmDialog(dateWrapperPanel, msg, "Confirmation",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (promptResult != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
            }
        }
        String includedTextCondition = (String) includeTextConditionComboBox.getSelectedItem();
        String excludedTextCondition = (String) excludeTextConditionComboBox.getSelectedItem();
        StringBuilder sb = new StringBuilder();
        for (JCheckBox checkBox : dayCheckBoxes) {
            sb.append(checkBox.isSelected() ? "Y" : "N");
        }
        String dayFlags = sb.toString();
        sb.setLength(0);
        for (JCheckBox checkBox : hourCheckBoxes) {
            sb.append(checkBox.isSelected() ? "Y" : "N");
        }
        String hourFlags = sb.toString();
        sb.setLength(0);
        for (JCheckBox checkBox : minuteCheckBoxes) {
            sb.append(checkBox.isSelected() ? "Y" : "N");
        }
        String minuteFlags = sb.toString();
        Date selectedDate = (Date) dateWrapperPanel.getDatePicker().getModel().getValue();
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        TimeZoneComboBoxHolder timeZoneHolder = (TimeZoneComboBoxHolder) timeZoneComboBox.getSelectedItem();
        Integer timeZoneHourOffset = timeZoneHolder.getHourOffset();
        String includeTextConditionEncoded = StringUtils.replace(includedTextCondition, " ", "%20");
        String excludeTextConditionEncoded = StringUtils.replace(excludedTextCondition, " ", "%20");
        AutomationSettingsHolder holder = new AutomationSettingsHolder()
                .setUserTwitterID(currentlySelectedAccount.getTwitterID())
                .setAutomationEnabled(enableAutomatedRetweetingCheckBox.isSelected() ? "Y" : "N")
                .setExcludedTextEnabled(excludeTweetTextCheckBox.isSelected() ? "Y" : "N")
                .setIncludedTextEnabled(includeTweetTextCheckBox.isSelected() ? "Y" : "N")
                .setOldTweetCutoffDateEnabled(ignoreOldTweetsCheckBox.isSelected() ? "Y" : "N")
                .setDayFlags(dayFlags)
                .setHourFlags(hourFlags)
                .setMinuteFlags(minuteFlags)
                .setTimeZoneHourOffset(timeZoneHourOffset)
                .setTimeZoneMinuteOffset(timeZoneHolder.getMinuteOffset())
                .setIncludeTextCondition(includeTextConditionEncoded)
                .setExcludeTextCondition(excludeTextConditionEncoded)
                .setRetweetPercent(retweetPercent);
        if (excludeTweetTextCheckBox.isSelected()) {
            holder.setExcludedText(excludeText.equals("") ? null : StringUtils.replace(excludeText, " ", "%20"));
        }
        if (includeTweetTextCheckBox.isSelected()) {
            holder.setIncludedText(includeText.equals("") ? null : StringUtils.replace(includeText, " ", "%20"));
        }
        if (ignoreOldTweetsCheckBox.isSelected()) {
            holder.setOldTweetCutoffDate(new Timestamp(cal.getTimeInMillis()));
        }
        OperationResult result = ServerAPI.commitAutomationSettings(currentlySelectedAccount, holder);
        if (result.wasSuccessful()) {
            if (excludeTweetTextCheckBox.isSelected()) {
                holder.setExcludedText(excludeText.equals("") ? null : StringUtils.replace(excludeText, "%20", " "));
            }
            if (includeTweetTextCheckBox.isSelected()) {
                holder.setIncludedText(includeText.equals("") ? null : StringUtils.replace(includeText, "%20", " "));
            }
            holder.setIncludeTextCondition(includedTextCondition);
            holder.setIncludeTextCondition(excludedTextCondition);
            if (AutomationDB.updateAutomationSettings(holder)) {
                JOptionPane.showMessageDialog(GUI.getInstance(), "Automation settings saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                GUIHelperMethods.showErrors(result, LOGGER, "Queued successfully on server, but an error occurred updating local DB:");
            }
        } else {
            GUIHelperMethods.showErrors(result, LOGGER, "Error committing automation settings:");
        }
    }//GEN-LAST:event_saveChangesButtonActionPerformed

    private void selectAllTimesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllTimesButtonActionPerformed
        for (JCheckBox checkBox : hourCheckBoxes) {
            checkBox.setSelected(true);
        }
    }//GEN-LAST:event_selectAllTimesButtonActionPerformed

    private void selectNoneTimesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectNoneTimesButtonActionPerformed
        for (JCheckBox checkBox : hourCheckBoxes) {
            checkBox.setSelected(false);
        }
    }//GEN-LAST:event_selectNoneTimesButtonActionPerformed

    private void timeZoneComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timeZoneComboBoxActionPerformed
        if (currentlySelectedTimeZone == null) {
            currentlySelectedTimeZone = (TimeZoneComboBoxHolder) timeZoneComboBox.getSelectedItem();
        } else {
            TimeZoneComboBoxHolder holder = (TimeZoneComboBoxHolder) timeZoneComboBox.getSelectedItem();
            if (!currentlySelectedTimeZone.equals(holder)) {
                currentlySelectedTimeZone = holder;
            }
        }
    }//GEN-LAST:event_timeZoneComboBoxActionPerformed

    private void setDaylightSavingsComponents() {
        TimeZone timeZone = TimeZone.getDefault();
        timeZoneMessageLabel.setVisible(timeZone.inDaylightTime(new Date()));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel dateAndTimeExplanationLabel;
    private com.antsstyle.artretweeter.gui.DateWrapperPanel dateWrapperPanel;
    private javax.swing.JCheckBox enableAutomatedRetweetingCheckBox;
    private javax.swing.JComboBox<String> excludeTextConditionComboBox;
    private javax.swing.JCheckBox excludeTweetTextCheckBox;
    private javax.swing.JTextField excludeTweetTextField;
    private javax.swing.JCheckBox fridayCheckBox;
    private javax.swing.JCheckBox ignoreOldTweetsCheckBox;
    private javax.swing.JComboBox<String> includeTextConditionComboBox;
    private javax.swing.JCheckBox includeTweetTextCheckBox;
    private javax.swing.JTextField includeTweetTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel mainAutomationSettingsPanel;
    private javax.swing.JLabel mainSettingsExplanationLabel;
    private javax.swing.JCheckBox min0CheckBox;
    private javax.swing.JCheckBox min15CheckBox;
    private javax.swing.JCheckBox min30CheckBox;
    private javax.swing.JCheckBox min45CheckBox;
    private javax.swing.JCheckBox mondayCheckBox;
    private javax.swing.JTextField retweetPercentTextField;
    private javax.swing.JCheckBox saturdayCheckBox;
    private javax.swing.JButton saveChangesButton;
    private javax.swing.JComboBox<String> selectAccountComboBox;
    private javax.swing.JButton selectAllDaysButton;
    private javax.swing.JButton selectAllTimesButton;
    private javax.swing.JButton selectNoneDaysButton;
    private javax.swing.JButton selectNoneTimesButton;
    private javax.swing.JCheckBox sundayCheckBox;
    private javax.swing.JLabel systemTimeZoneLabel;
    private javax.swing.JCheckBox thursdayCheckBox;
    private javax.swing.JCheckBox time0001CheckBox;
    private javax.swing.JCheckBox time0102CheckBox;
    private javax.swing.JCheckBox time0203CheckBox;
    private javax.swing.JCheckBox time0304CheckBox;
    private javax.swing.JCheckBox time0405CheckBox;
    private javax.swing.JCheckBox time0506CheckBox;
    private javax.swing.JCheckBox time0607CheckBox;
    private javax.swing.JCheckBox time0708CheckBox;
    private javax.swing.JCheckBox time0809CheckBox;
    private javax.swing.JCheckBox time0910CheckBox;
    private javax.swing.JCheckBox time1011CheckBox;
    private javax.swing.JCheckBox time1112CheckBox;
    private javax.swing.JCheckBox time1213CheckBox;
    private javax.swing.JCheckBox time1314CheckBox;
    private javax.swing.JCheckBox time1415CheckBox;
    private javax.swing.JCheckBox time1516CheckBox;
    private javax.swing.JCheckBox time1617CheckBox;
    private javax.swing.JCheckBox time1718CheckBox;
    private javax.swing.JCheckBox time1819CheckBox;
    private javax.swing.JCheckBox time1920CheckBox;
    private javax.swing.JCheckBox time2021CheckBox;
    private javax.swing.JCheckBox time2122CheckBox;
    private javax.swing.JCheckBox time2223CheckBox;
    private javax.swing.JCheckBox time2300CheckBox;
    private javax.swing.JComboBox<String> timeZoneComboBox;
    private javax.swing.JLabel timeZoneMessageLabel;
    private javax.swing.JCheckBox tuesdayCheckBox;
    private javax.swing.JCheckBox wednesdayCheckBox;
    // End of variables declaration//GEN-END:variables

}
