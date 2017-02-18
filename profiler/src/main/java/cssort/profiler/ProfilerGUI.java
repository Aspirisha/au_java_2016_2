package cssort.profiler;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Created by andy on 2/15/17.
 */
@Slf4j
public class ProfilerGUI implements PropertyChangeListener {
    private JComboBox clientArchitecture;
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
    private JButton showPlotButton;
    private ProgressMonitor progressMonitor;

    private BenchmarkRunner benchmark;

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        // if the operation is finished or has been canceled by
        // the user, take appropriate action
        if (progressMonitor.isCanceled()) {
            benchmark.cancel(true);
        } else if (event.getPropertyName().equals("progress")) {
            // get the % complete from the progress event
            // and set it on the progress monitor
            int progress = (Integer) event.getNewValue();
            progressMonitor.setProgress(progress);
        }
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
        rootPanel.setLayout(new GridLayoutManager(10, 7, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Client Architecture");
        rootPanel.add(label1, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mRadioButton = new JRadioButton();
        mRadioButton.setSelected(true);
        mRadioButton.setText("M");
        rootPanel.add(mRadioButton, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nRadioButton = new JRadioButton();
        nRadioButton.setText("N");
        rootPanel.add(nRadioButton, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaRadioButton = new JRadioButton();
        deltaRadioButton.setText("Delta");
        rootPanel.add(deltaRadioButton, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mValue = new JSpinner();
        rootPanel.add(mValue, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nValue = new JSpinner();
        rootPanel.add(nValue, new GridConstraints(4, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaValue = new JSpinner();
        rootPanel.add(deltaValue, new GridConstraints(5, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mMin = new JSpinner();
        rootPanel.add(mMin, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 28), null, 0, false));
        mMax = new JSpinner();
        rootPanel.add(mMax, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mStep = new JSpinner();
        rootPanel.add(mStep, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Min");
        rootPanel.add(label2, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 18), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Max");
        rootPanel.add(label3, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Step");
        rootPanel.add(label4, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clientArchitecture = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("TCP: Persistent");
        defaultComboBoxModel1.addElement("TCP: Spawning");
        defaultComboBoxModel1.addElement("UDP");
        clientArchitecture.setModel(defaultComboBoxModel1);
        rootPanel.add(clientArchitecture, new GridConstraints(1, 3, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Fixed Value");
        rootPanel.add(label5, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nMin = new JSpinner();
        rootPanel.add(nMin, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 28), null, 0, false));
        nMax = new JSpinner();
        rootPanel.add(nMax, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nStep = new JSpinner();
        rootPanel.add(nStep, new GridConstraints(4, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaStep = new JSpinner();
        rootPanel.add(deltaStep, new GridConstraints(5, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaMax = new JSpinner();
        rootPanel.add(deltaMax, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deltaMin = new JSpinner();
        rootPanel.add(deltaMin, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(89, 28), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Varying argument");
        rootPanel.add(label6, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        xValue = new JSpinner();
        rootPanel.add(xValue, new GridConstraints(6, 3, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("X");
        rootPanel.add(label7, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Statistics Output File");
        rootPanel.add(label8, new GridConstraints(7, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statsFile = new JTextField();
        statsFile.setText("statistics.csv");
        rootPanel.add(statsFile, new GridConstraints(7, 3, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(9, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 20), null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(4, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, new Dimension(20, -1), null, 0, false));
        final Spacer spacer3 = new Spacer();
        rootPanel.add(spacer3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, new Dimension(20, -1), null, 0, false));
        final Spacer spacer4 = new Spacer();
        rootPanel.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 20), null, 0, false));
        runButton = new JButton();
        runButton.setText("Run");
        rootPanel.add(runButton, new GridConstraints(8, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showPlotButton = new JButton();
        showPlotButton.setEnabled(false);
        showPlotButton.setText("Show Plot");
        rootPanel.add(showPlotButton, new GridConstraints(8, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    @AllArgsConstructor
    @Data
    class BenchmarkCaseDescription {
        private int progress;
        private int casesProcessed;
        private int totalCases;

        private int m;
        private int n;
        private int x;
        private int delta;
    }

    @Data
    @AllArgsConstructor
    class ArgDescription {
        final int minValue;
        final int maxValue;
        final int step;

        int getCasesNumber() {
            return (maxValue - minValue + step) / step;
        }
    }

    class BenchmarkRunner extends SwingWorker<Void, BenchmarkCaseDescription> {
        private int progress = 0;
        private final ArgDescription m;
        private final ArgDescription n;
        private final ArgDescription delta;
        private final int x;
        private final int clientArch;

        BenchmarkRunner(ArgDescription m, ArgDescription n, ArgDescription delta, int x, int clientArch) {
            this.n = n;
            this.m = m;
            this.delta = delta;
            this.x = x;
            this.clientArch = clientArch;
        }


        void singleRun(Writer writer, int runNumber, int totalCases, int m, int n, int delta, int x, int clientArch) {
            BenchmarkCaseDescription current = new BenchmarkCaseDescription(progress, runNumber, totalCases, m, n, x, delta);
            progress = (100 * runNumber) / totalCases;
            setProgress(progress);
            publish(current);

            CaseRunner currentRunner = new CaseRunner(m, n, delta, x, clientArch);
            try {
                CaseRunner.CaseResult result = currentRunner.run();
                if (result != null) {
                    writer.write(String.format("%d, %d, %d\n", result.averageProcessTime,
                            result.averageRequestTime, result.averageClientRuntime));
                } else {
                    writer.write(String.format("%s, %s, %s\n", "NaN", "NaN", "NaN"));
                    log.error(String.format("Too many clients failed on th case: " +
                            "m = %d n = %d delta = %d x = %d, clientarch = %d",
                            m, n, x, delta, clientArch));
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground() throws Exception {
            progress = 0;

            // initialize bound property progress (inherited from SwingWorker)
            setProgress(0);
            String statsOutputFile = statsFile.getText();
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(statsOutputFile), "utf-8"))) {
                writer.write(String.format("%s, %s, %s\n", "Sorting Time",
                        "Request Time", "Client Runtime"));

                int totalCases = n.getCasesNumber() * m.getCasesNumber() * delta.getCasesNumber();
                int caseNumber = 0;
                for (int mValue = m.minValue; mValue <= m.maxValue; mValue += m.step) {
                    for (int nValue = n.minValue; nValue <= n.maxValue; nValue += n.step) {
                        for (int deltaValue = delta.minValue; deltaValue <= delta.maxValue; deltaValue += delta.step) {
                            singleRun(writer, caseNumber, totalCases, mValue, nValue, deltaValue, x, clientArch);
                            caseNumber++;
                        }
                    }
                }
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            } catch (FileNotFoundException e1) {
                JOptionPane.showMessageDialog(null, String.format("Coul not write to file %s", statsOutputFile));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }

        @Override
        public void process(List<BenchmarkCaseDescription> v) {
            if (isCancelled() || v.isEmpty()) {
                return;
            }
            BenchmarkCaseDescription update = v.get(0);
            for (BenchmarkCaseDescription d : v) {
                if (d.getCasesProcessed() > update.getCasesProcessed()) {
                    update = d;
                }
            }

            String progressNote = update.getCasesProcessed() + " of "
                    + update.getTotalCases() + " cases run.";
            String caseNote = String.format("Now running case: M = %d; N = %d; Delta = %d",
                    update.getM(), update.getN(), update.getDelta());

            if (update.getProgress() < 100) {
                progressMonitor.setNote(progressNote + " " + caseNote);
            } else {
                progressMonitor.setNote(progressNote);
            }
        }

        @Override
        public void done() {
            try {
                Void result = get();
                log.debug("Benchmark completed.\n");
                showPlotButton.setEnabled(true);
                progressMonitor.close();
            } catch (InterruptedException e) {

            } catch (CancellationException e) {
                log.debug("Benchmark canceled.\n");
            } catch (ExecutionException e) {
                log.error("Exception occurred: " + e.getCause());
            }
            runButton.setEnabled(true);
        }
    }

    private int commitAndGet(JSpinner s) {
        Integer i = (Integer) s.getValue();
        try {
            s.commitEdit();
        } catch (ParseException e) {
            s.setValue(i);
        }

        return i;
    }


    ArgDescription creatVarArgDescription(JSpinner min, JSpinner max, JSpinner step) {
        int minVal = commitAndGet(min);
        int maxVal = commitAndGet(max);
        int stepVal = Math.max(1, commitAndGet(step));
        return new ArgDescription(minVal, maxVal, stepVal);
    }

    ArgDescription createConstArgDescription(JSpinner value) {
        int constVal = commitAndGet(value);
        return new ArgDescription(constVal, constVal, 1);
    }

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
            @Override
            public void actionPerformed(ActionEvent e) {
                progressMonitor = new ProgressMonitor(rootPanel,
                        "Operation in progress...",
                        "", 0, 100);
                progressMonitor.setProgress(0);

                ArgDescription mDesc = mRadioButton.isSelected() ?
                        creatVarArgDescription(mMin, mMax, mStep) : createConstArgDescription(mValue);
                ArgDescription nDesc = nRadioButton.isSelected() ?
                        creatVarArgDescription(nMin, nMax, nStep) : createConstArgDescription(nValue);
                ArgDescription deltaDesc = deltaRadioButton.isSelected() ?
                        creatVarArgDescription(deltaMin, deltaMax, deltaStep) : createConstArgDescription(deltaValue);
                // schedule the copy files operation for execution on a background thread
                int clientArch = clientArchitecture.getSelectedIndex();
                int x = commitAndGet(xValue);
                benchmark = new BenchmarkRunner(mDesc, nDesc, deltaDesc, x, clientArch);
                // add ProgressMonitorExample as a listener on CopyFiles;
                // of specific interest is the bound property progress
                benchmark.addPropertyChangeListener(ProfilerGUI.this);
                runButton.setEnabled(false);
                benchmark.execute();
                showPlotButton.setEnabled(false);
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
