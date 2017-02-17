package cssort.profiler;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.ParseException;

import static sun.misc.PostVMInitHook.run;

/**
 * Created by andy on 2/15/17.
 */
public class ProfilerGUI {
    private JComboBox serverArchitecture;
    private JComboBox clientArchitecture;
    private JLabel serverArchLabel;
    private JLabel clientArchLabel;
    private JRadioButton mRadioButton;
    private JRadioButton nRadioButton;
    private JRadioButton deltaRadioButton;
    private JSpinner mValue;
    private JSpinner nValue;
    private JSpinner deltaValue;
    private JSpinner mMin;
    private JSpinner mMax;
    private JSpinner mStep;
    private JButton runButton;
    private JSpinner nMin;
    private JSpinner nMax;
    private JSpinner nStep;
    private JSpinner deltaStep;
    private JSpinner deltaMax;
    private JSpinner deltaMin;
    private JPanel rootPanel;
    private JSpinner xValue;
    private JTextField statsFile;


    public ProfilerGUI() {
        VariableGroupListener l = new VariableGroupListener();
        mRadioButton.addActionListener(l);
        nRadioButton.addActionListener(l);
        deltaRadioButton.addActionListener(l);
        mMin.setValue(5);
        mMax.setValue(100);
        mStep.setValue(5);
        mValue.setValue(10);
        nValue.setValue(1000);
        xValue.setValue(10);
        nMin.setValue(5);
        nMax.setValue(10000);
        nStep.setValue(100);
        deltaMax.setValue(3000);
        deltaMin.setValue(0);
        deltaStep.setValue(200);
        deltaValue.setValue(200);

        l.actionPerformed(new ActionEvent(mRadioButton, 0, ""));
        runButton.addActionListener(new ActionListener() {
            int commitAndGet(JSpinner s) {
                Integer i = (Integer) s.getValue();
                try {
                    s.commitEdit();
                } catch (ParseException e) {
                    s.setValue(i);
                }

                return i;
            }

            void singleRun(Writer writer, int m, int n, int delta, int x, int clientArch, int serverArch) {
                CaseRunner r = new CaseRunner(m, n, delta, x, clientArch, serverArch);
                try {
                    CaseRunner.CaseResult result = r.run();
                    writer.write(String.format("%d, %d, %d\n", result.averageProcessTime,
                            result.averageRequestTime, result.averageClientRuntime));

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                String statsOutputFile = statsFile.getText();
                try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(statsOutputFile), "utf-8"))) {
                    writer.write(String.format("%s, %s, %s\n", "Average Process Time",
                            "Average Request Time", "Average Client Runtime"));
                    int clientArch = clientArchitecture.getSelectedIndex();
                    int serverArch = serverArchitecture.getSelectedIndex();
                    int n = commitAndGet(nValue);
                    int delta = commitAndGet(deltaValue);
                    int x = commitAndGet(xValue);
                    int m = commitAndGet(mValue);
                    if (mRadioButton.isSelected()) {
                        int minM = commitAndGet(mMin);
                        int maxM = commitAndGet(mMax);
                        int stepM = commitAndGet(mStep);
                        stepM = Math.max(stepM, 1);
                        for (m = minM; m < maxM; m += stepM) {
                            singleRun(writer, m, n, delta, x, clientArch, serverArch);
                        }
                    } else if (nRadioButton.isSelected()) {
                        int minN = commitAndGet(nMin);
                        int maxN = commitAndGet(nMax);
                        int stepN = commitAndGet(nStep);
                        stepN = Math.max(stepN, 1);
                        for (n = minN; n < maxN; n += stepN) {
                            singleRun(writer, m, n, delta, x, clientArch, serverArch);
                        }
                    } else {
                        int minDelta = commitAndGet(deltaMin);
                        int maxDelta = commitAndGet(deltaMax);
                        int stepDelta = commitAndGet(deltaStep);
                        stepDelta = Math.max(stepDelta, 1);
                        for (delta = minDelta; n < maxDelta; n += stepDelta) {
                            singleRun(writer, m, n, delta, x, clientArch, serverArch);
                        }
                    }
                } catch (UnsupportedEncodingException e1) {
                    e1.printStackTrace();
                } catch (FileNotFoundException e1) {
                    JOptionPane.showMessageDialog(null, String.format("Coul not write to file %s", statsOutputFile));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(9, 5, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Server Architecture");
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Client Architecture");
        rootPanel.add(label2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverArchitecture = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("TCP: Thread per Client");
        defaultComboBoxModel1.addElement("TCP: Caching Thread Pool");
        defaultComboBoxModel1.addElement("TCP: NonBlocking Fixed Thread Pool");
        defaultComboBoxModel1.addElement("TCP: Serial");
        defaultComboBoxModel1.addElement("TCP: Async");
        defaultComboBoxModel1.addElement("UDP: Thread per Request");
        defaultComboBoxModel1.addElement("UDP: Fixed Thread Pool");
        serverArchitecture.setModel(defaultComboBoxModel1);
        rootPanel.add(serverArchitecture, new GridConstraints(0, 1, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mRadioButton = new JRadioButton();
        mRadioButton.setSelected(true);
        mRadioButton.setText("M");
        rootPanel.add(mRadioButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nRadioButton = new JRadioButton();
        nRadioButton.setText("N");
        rootPanel.add(nRadioButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaRadioButton = new JRadioButton();
        deltaRadioButton.setText("Delta");
        rootPanel.add(deltaRadioButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mValue = new JSpinner();
        rootPanel.add(mValue, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nValue = new JSpinner();
        rootPanel.add(nValue, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaValue = new JSpinner();
        rootPanel.add(deltaValue, new GridConstraints(5, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mMin = new JSpinner();
        rootPanel.add(mMin, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 28), null, 0, false));
        mMax = new JSpinner();
        rootPanel.add(mMax, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mStep = new JSpinner();
        rootPanel.add(mStep, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Min");
        rootPanel.add(label3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 18), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Max");
        rootPanel.add(label4, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Step");
        rootPanel.add(label5, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        runButton = new JButton();
        runButton.setText("Run");
        rootPanel.add(runButton, new GridConstraints(8, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clientArchitecture = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("TCP: Persistent");
        defaultComboBoxModel2.addElement("TCP: Spawning");
        defaultComboBoxModel2.addElement("UDP");
        clientArchitecture.setModel(defaultComboBoxModel2);
        rootPanel.add(clientArchitecture, new GridConstraints(1, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Fixed Value");
        rootPanel.add(label6, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nMin = new JSpinner();
        rootPanel.add(nMin, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 28), null, 0, false));
        nMax = new JSpinner();
        rootPanel.add(nMax, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nStep = new JSpinner();
        rootPanel.add(nStep, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaStep = new JSpinner();
        rootPanel.add(deltaStep, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaMax = new JSpinner();
        rootPanel.add(deltaMax, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaMin = new JSpinner();
        rootPanel.add(deltaMin, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 28), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Varying argument");
        rootPanel.add(label7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        xValue = new JSpinner();
        rootPanel.add(xValue, new GridConstraints(6, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("X");
        rootPanel.add(label8, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Statistics Output File");
        rootPanel.add(label9, new GridConstraints(7, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statsFile = new JTextField();
        statsFile.setText("statistics.csv");
        rootPanel.add(statsFile, new GridConstraints(7, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(mRadioButton);
        buttonGroup.add(nRadioButton);
        buttonGroup.add(deltaRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    class VariableGroupListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean isMVariable = e.getSource() == mRadioButton;
            boolean isNVariable = e.getSource() == nRadioButton;
            boolean isDeltaVariable = e.getSource() == deltaRadioButton;

            mMax.setEnabled(isMVariable);
            mMin.setEnabled(isMVariable);
            mStep.setEnabled(isMVariable);
            mValue.setEnabled(!isMVariable);
            nMax.setEnabled(isNVariable);
            nMin.setEnabled(isNVariable);
            nStep.setEnabled(isNVariable);
            nValue.setEnabled(!isNVariable);
            deltaMax.setEnabled(isDeltaVariable);
            deltaMin.setEnabled(isDeltaVariable);
            deltaStep.setEnabled(isDeltaVariable);
            deltaValue.setEnabled(!isDeltaVariable);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("ProfilerGUI");
        frame.setContentPane(new ProfilerGUI().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }
}
