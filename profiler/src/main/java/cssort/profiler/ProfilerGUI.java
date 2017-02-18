package cssort.profiler;

import javax.swing.*;
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
