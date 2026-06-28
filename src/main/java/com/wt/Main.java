package com.wt;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class Main {

    private static final Color BG        = new Color(0x1A1A2E);
    private static final Color PANEL_BG  = new Color(0x16213E);
    private static final Color ACCENT    = new Color(0x0F3460);
    private static final Color RED_CLR   = new Color(0xC0392B);
    private static final Color ORANGE    = new Color(0xF39C12);
    private static final Color GREEN_CLR = new Color(0x27AE60);
    private static final Color TEXT_CLR  = new Color(0xECF0F1);
    private static final Color HINT_CLR  = new Color(0x95A5A6);
    private static final Color BORDER_CLR= new Color(0x0F3460);

    private JFrame        frame;
    private JSpinner      thresholdSpinner;
    private JTextField    dateField;
    private JTextField    outputField;
    private JTextArea     logArea;
    private JButton       runBtn;
    private JLabel        statusLabel;

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("webdriver.chrome.silentOutput", "true");

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new Main().show());
    }

    private void show() {
        frame = new JFrame("War Thunder - Экспорт клана Jagdflugzeug");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(620, 680);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(0, 0));

        frame.add(buildHeader(),  BorderLayout.NORTH);
        frame.add(buildCenter(),  BorderLayout.CENTER);
        frame.add(buildBottom(),  BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(ACCENT);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("War Thunder - Clan Jagdflugzeug");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(TEXT_CLR);

        JLabel sub = new JLabel("Экспорт игроков в Excel с фильтрацией");
        sub.setFont(new Font("Arial", Font.PLAIN, 12));
        sub.setForeground(HINT_CLR);

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
        texts.setOpaque(false);
        texts.add(title);
        texts.add(sub);
        p.add(texts, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCenter() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(16, 20, 8, 20));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(PANEL_BG);
        grid.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CLR, 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        addSectionLabel(grid, gbc, 0, "Настройки фильтрации");

        addLabel(grid, gbc, 1, 0,
            "Минимум полковых очков",
            "<html>Игроки НИЖЕ этого значения выделяются красным.<br>" +
            "Стандартное значение: 1200</html>");

        thresholdSpinner = new JSpinner(new SpinnerNumberModel(1200, 0, 99999, 100));
        thresholdSpinner.setFont(new Font("Arial", Font.BOLD, 15));
        styleSpinner(thresholdSpinner);
        // Кнопка сброса к 1200
        JButton resetBtn = smallBtn("1200", ACCENT);
        resetBtn.addActionListener(e -> thresholdSpinner.setValue(1200));

        JPanel threshRow = new JPanel(new BorderLayout(8, 0));
        threshRow.setOpaque(false);
        threshRow.add(thresholdSpinner, BorderLayout.CENTER);
        threshRow.add(resetBtn, BorderLayout.EAST);
        addField(grid, gbc, 1, threshRow);

        addLabel(grid, gbc, 2, 0,
            "Дата среза (дд.мм.гггг)",
            "<html>Игроки, вступившие ПОСЛЕ этой даты - новенькие.<br>" +
            "Они НЕ будут красными даже при низком рейтинге.<br>" +
            "По умолчанию: 3 месяца назад</html>");

        String defaultDate = LocalDate.now().format(Player.FMT);
        dateField = styledTextField(defaultDate);

        // Быстрые кнопки: текущий месяц и -1, -2, -3
        JButton btn0m = smallBtn("Этот мес", ACCENT);
        JButton btn1m = smallBtn("−1 мес",   ACCENT);
        JButton btn2m = smallBtn("−2 мес",   ACCENT);
        JButton btn3m = smallBtn("−3 мес",   ACCENT);
        btn0m.addActionListener(e -> dateField.setText(LocalDate.now().format(Player.FMT)));
        btn1m.addActionListener(e -> dateField.setText(LocalDate.now().minusMonths(1).format(Player.FMT)));
        btn2m.addActionListener(e -> dateField.setText(LocalDate.now().minusMonths(2).format(Player.FMT)));
        btn3m.addActionListener(e -> dateField.setText(LocalDate.now().minusMonths(3).format(Player.FMT)));

        JPanel dateButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dateButtons.setOpaque(false);
        dateButtons.add(btn0m); dateButtons.add(btn1m);
        dateButtons.add(btn2m); dateButtons.add(btn3m);

        JPanel dateCol = new JPanel(new BorderLayout(0, 4));
        dateCol.setOpaque(false);
        dateCol.add(dateField, BorderLayout.NORTH);
        dateCol.add(dateButtons, BorderLayout.CENTER);
        addField(grid, gbc, 2, dateCol);

        addSectionLabel(grid, gbc, 3, "Файл результата");

        addLabel(grid, gbc, 4, 0,
            "Путь для сохранения Excel",
            "Куда сохранить .xlsx файл");

        outputField = styledTextField("clan_jagdflugzeug.xlsx");
        JButton browseBtn = smallBtn("Обзор", ACCENT);
        browseBtn.addActionListener(e -> browseOutput());

        JPanel outRow = new JPanel(new BorderLayout(8, 0));
        outRow.setOpaque(false);
        outRow.add(outputField, BorderLayout.CENTER);
        outRow.add(browseBtn, BorderLayout.EAST);
        addField(grid, gbc, 4, outRow);

        addSectionLabel(grid, gbc, 5, "Условные обозначения");

        JPanel legend = new JPanel(new GridLayout(3, 1, 0, 4));
        legend.setOpaque(false);
        legend.add(legendRow(RED_CLR,   "Красный  - мало очков и вступил ДО даты среза"));
        legend.add(legendRow(ORANGE,    "Оранжевый - мало очков, но вступил ПОСЛЕ даты (новенький)"));
        legend.add(legendRow(GREEN_CLR, "Обычный - рейтинг выше минимума"));

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        grid.add(legend, gbc);

        outer.add(grid, BorderLayout.NORTH);

        logArea = new JTextArea(6, 40);
        logArea.setEditable(false);
        logArea.setBackground(new Color(0x0D0D1A));
        logArea.setForeground(new Color(0x00FF88));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        logArea.setText("Готов к работе. Нажмите «Запустить».");

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));
        scroll.getViewport().setBackground(new Color(0x0D0D1A));

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(BG);
        logPanel.setBorder(new EmptyBorder(12, 20, 8, 20));
        logPanel.add(scroll, BorderLayout.CENTER);

        outer.add(logPanel, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(8, 20, 16, 20));

        final Color RUN_GREEN  = new Color(0x27AE60);
        final Color RUN_HOVER  = new Color(0x2ECC71);
        runBtn = new JButton("Запустить экспорт") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? RUN_HOVER : RUN_GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        runBtn.setFont(new Font("Arial", Font.BOLD, 15));
        runBtn.setForeground(Color.WHITE);
        runBtn.setOpaque(false);
        runBtn.setContentAreaFilled(false);
        runBtn.setBorderPainted(false);
        runBtn.setFocusPainted(false);
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runBtn.setPreferredSize(new Dimension(220, 44));
        runBtn.addActionListener(e -> runExport());

        statusLabel = new JLabel("Ожидание запуска...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(HINT_CLR);

        p.add(runBtn, BorderLayout.WEST);
        p.add(statusLabel, BorderLayout.CENTER);
        return p;
    }

    private void runExport() {
        LocalDate since;
        try {
            since = LocalDate.parse(dateField.getText().trim(), Player.FMT);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(frame,
                "Неверный формат даты!\nВведите дату в формате дд.мм.гггг\nПример: 01.01.2024",
                "Ошибка", JOptionPane.ERROR_MESSAGE);
            dateField.requestFocus();
            return;
        }

        String outPath = outputField.getText().trim();
        if (outPath.isEmpty()) outPath = "clan_jagdflugzeug.xlsx";
        if (!outPath.endsWith(".xlsx")) outPath += ".xlsx";
        final String finalOut = outPath;

        int threshold = (int) thresholdSpinner.getValue();

        runBtn.setEnabled(false);
        runBtn.setText("Загрузка...");
        logArea.setText("");

        final LocalDate finalSince = since;
        final int finalThreshold = threshold;

        new Thread(() -> {
            try {
                log("Запуск...");
                log("Порог рейтинга : " + finalThreshold + " очков");
                log("Дата среза     : " + finalSince.format(Player.FMT));
                log("Файл           : " + finalOut);
                log("─────────────────────────────────");

                Scraper scraper = new Scraper();
                List<Player> players = scraper.fetch(msg -> log(msg));

                log("─────────────────────────────────");
                log("Получено игроков : " + players.size());

                players.sort((a, b) -> Integer.compare(a.rating, b.rating));

                long probCnt = players.stream()
                    .filter(p -> p.isProblematic(finalThreshold, finalSince)).count();
                long newbCnt = players.stream()
                    .filter(p -> p.rating < finalThreshold
                              && !p.isProblematic(finalThreshold, finalSince)).count();

                log("Проблемных    : " + probCnt);
                log("Новеньких     : " + newbCnt);
                log("С нормой      : " + (players.size() - probCnt - newbCnt));

                log("Создаю Excel файл...");
                new ExcelWriter().write(players, finalOut, finalThreshold, finalSince);

                log("─────────────────────────────────");
                log("Готово! Файл сохранён: " + finalOut);

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Готово: " + finalOut);
                    statusLabel.setForeground(GREEN_CLR);

                    int choice = JOptionPane.showOptionDialog(frame,
                        "Файл успешно создан!\n" + finalOut +
                        "\n\nИгроков: " + players.size() +
                        "\nПроблемных: " + probCnt +
                        "\nНовеньких: " + newbCnt,
                        "Экспорт завершён",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        new String[]{"Открыть папку", "OK"},
                        "OK"
                    );
                    if (choice == 0) openFolder(finalOut);
                });

            } catch (Exception ex) {
                log("ОШИБКА: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Ошибка");
                    statusLabel.setForeground(RED_CLR);
                    JOptionPane.showMessageDialog(frame,
                        "Ошибка: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    runBtn.setEnabled(true);
                    runBtn.setText("Запустить экспорт");
                    // цвет управляется через paintComponent
                });
            }
        }, "export-thread").start();
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
        System.out.println(msg);
    }

    private void browseOutput() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Сохранить Excel файл");
        fc.setSelectedFile(new File(outputField.getText().trim()));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".xlsx")) path += ".xlsx";
            outputField.setText(path);
        }
    }

    private void openFolder(String filePath) {
        try {
            File f = new File(filePath).getAbsoluteFile().getParentFile();
            Desktop.getDesktop().open(f);
        } catch (Exception ignored) {}
    }

    private void addSectionLabel(JPanel grid, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(row == 0 ? 0 : 14, 4, 2, 4);
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setForeground(new Color(0x5DADE2));
        lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2E86C1)));
        grid.add(lbl, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 4, 6, 4);
    }

    private void addLabel(JPanel grid, GridBagConstraints gbc,
                          int row, int col, String text, String tooltip) {
        gbc.gridx = col; gbc.gridy = row; gbc.weightx = 0.35;
        JLabel lbl = new JLabel(text + ":");
        lbl.setFont(new Font("Arial", Font.PLAIN, 13));
        lbl.setForeground(TEXT_CLR);
        lbl.setToolTipText(tooltip);
        grid.add(lbl, gbc);
    }

    private void addField(JPanel grid, GridBagConstraints gbc, int row, JComponent field) {
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.65;
        grid.add(field, gbc);
    }

    private JTextField styledTextField(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(new Font("Arial", Font.PLAIN, 13));
        tf.setBackground(new Color(0x0D0D1A));
        tf.setForeground(TEXT_CLR);
        tf.setCaretColor(TEXT_CLR);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_CLR, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        return tf;
    }

    private void styleSpinner(JSpinner sp) {
        sp.setBackground(new Color(0x0D0D1A));
        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(new Color(0x0D0D1A));
            tf.setForeground(TEXT_CLR);
            tf.setCaretColor(TEXT_CLR);
            tf.setFont(new Font("Arial", Font.BOLD, 14));
            tf.setHorizontalAlignment(JTextField.CENTER);
        }
    }

    private JButton smallBtn(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.setForeground(TEXT_CLR);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        return btn;
    }

    private JPanel legendRow(Color color, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(16, 16));
        dot.setBackground(color);
        dot.setOpaque(true);
        dot.setBorder(BorderFactory.createLineBorder(color.darker(), 1));
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_CLR);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        row.add(dot); row.add(lbl);
        return row;
    }
}
