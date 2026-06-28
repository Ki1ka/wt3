package com.wt;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ExcelWriter {

    private static final byte[] HDR_BG    = rgb("1F4E79");
    private static final byte[] HDR_FG    = rgb("FFFFFF");
    private static final byte[] PROB_BG   = rgb("C0392B");
    private static final byte[] PROB_FG   = rgb("FFFFFF");
    private static final byte[] NEW_BG    = rgb("F39C12");
    private static final byte[] NEW_FG    = rgb("FFFFFF");
    private static final byte[] EVEN_BG   = rgb("DCE6F1");
    private static final byte[] ODD_BG    = rgb("FFFFFF");
    private static final byte[] BLACK     = rgb("000000");
    private static final byte[] DARK      = rgb("17375E");
    private static final byte[] BLUE      = rgb("2E75B6");
    private static final byte[] GREY      = rgb("CCCCCC");
    private static final byte[] GREEN_BG  = rgb("1E8449");
    private static final byte[] GREEN_FG  = rgb("FFFFFF");

    public void write(List<Player> sorted, String path,
                      int threshold, LocalDate newcomerSince) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            St s = new St(wb);
            buildAllSheet(wb, s, sorted, threshold, newcomerSince);
            buildProbSheet(wb, s, sorted, threshold, newcomerSince);
            buildNewSheet(wb, s, sorted, threshold, newcomerSince);

            try (FileOutputStream fos = new FileOutputStream(path)) {
                wb.write(fos);
            }
        }
    }

    private void buildAllSheet(XSSFWorkbook wb, St s, List<Player> players,
                               int threshold, LocalDate newcomerSince) {
        XSSFSheet sh = wb.createSheet("Все игроки");
        sh.createFreezePane(0, 2);

        title(sh, s.title,
            "Клан [LUFT] Jagdflugzeug  -  сортировка по полковым очкам (возрастание)", 6);
        header(sh, s.hdr, 1,
            "#", "Никнейм", "Звание", "Полковые очки", "Дата вступления", "Статус");

        for (int i = 0; i < players.size(); i++) {
            Player p  = players.get(i);
            boolean prob = p.isProblematic(threshold, newcomerSince);
            boolean newb = (p.rating < threshold) && !prob; // низкий рейтинг но новенький
            boolean even = i % 2 == 0;

            XSSFCellStyle tx, nm;
            if (prob) { tx = s.probTx; nm = s.probNm; }
            else if (newb) { tx = s.newbTx; nm = s.newbNm; }
            else { tx = even ? s.evenTx : s.oddTx; nm = even ? s.evenNm : s.oddNm; }

            XSSFRow row = sh.createRow(i + 2);
            row.setHeightInPoints(18);
            c(row, 0, i + 1,          nm);
            c(row, 1, p.nick,          tx);
            c(row, 2, p.rank,          tx);
            c(row, 3, p.rating,        nm);
            c(row, 4, p.joinDateStr(), tx);
            String status = prob ? "Мало очков"
                          : newb ? "Новенький"
                          : "✓ OK";
            c(row, 5, status, tx);
        }

        long probCnt = players.stream().filter(p -> p.isProblematic(threshold, newcomerSince)).count();
        long newbCnt = players.stream().filter(p -> p.rating < threshold && !p.isProblematic(threshold, newcomerSince)).count();
        int  stat    = players.size() + 3;
        addStat(sh, s, stat,     "Всего игроков:",            players.size(), BLACK);
        addStat(sh, s, stat + 1, "Проблемных (< " + threshold + " очков):", (int) probCnt, PROB_BG);
        addStat(sh, s, stat + 2, "Новеньких (не учитываются):", (int) newbCnt, NEW_BG);

        addLegend(wb, sh, stat + 4, s.probTx, "  🔴 Красный - мало очков, вступил ДО " + newcomerSince.format(Player.FMT));
        addLegend(wb, sh, stat + 5, s.newbTx, "  🟠 Оранжевый - мало очков, но вступил ПОСЛЕ " + newcomerSince.format(Player.FMT) + " (новенький)");

        sh.setColumnWidth(0,  1_500);
        sh.setColumnWidth(1, 10_500);
        sh.setColumnWidth(2,  5_500);
        sh.setColumnWidth(3,  5_500);
        sh.setColumnWidth(4,  5_500);
        sh.setColumnWidth(5,  5_000);
    }

    private void buildProbSheet(XSSFWorkbook wb, St s, List<Player> players,
                                int threshold, LocalDate newcomerSince) {
        XSSFSheet sh = wb.createSheet("Проблемные");
        sh.createFreezePane(0, 2);

        List<Player> prob = players.stream()
            .filter(p -> p.isProblematic(threshold, newcomerSince))
            .collect(Collectors.toList());

        title(sh, s.title,
            "Мало очков И вступили ДО " + newcomerSince.format(Player.FMT)
            + "  (порог: " + threshold + ")", 5);
        header(sh, s.hdr, 1, "#", "Никнейм", "Звание", "Полковые очки", "Дата вступления");

        for (int i = 0; i < prob.size(); i++) {
            Player p = prob.get(i);
            XSSFRow row = sh.createRow(i + 2);
            row.setHeightInPoints(18);
            c(row, 0, i + 1,          s.probNm);
            c(row, 1, p.nick,          s.probTx);
            c(row, 2, p.rank,          s.probTx);
            c(row, 3, p.rating,        s.probNm);
            c(row, 4, p.joinDateStr(), s.probTx);
        }

        sh.setColumnWidth(0,  1_500);
        sh.setColumnWidth(1, 10_500);
        sh.setColumnWidth(2,  5_500);
        sh.setColumnWidth(3,  5_500);
        sh.setColumnWidth(4,  5_500);
    }

    private void buildNewSheet(XSSFWorkbook wb, St s, List<Player> players,
                               int threshold, LocalDate newcomerSince) {
        XSSFSheet sh = wb.createSheet("Новенькие");
        sh.createFreezePane(0, 2);

        List<Player> newbs = players.stream()
            .filter(p -> p.rating < threshold && !p.isProblematic(threshold, newcomerSince))
            .collect(Collectors.toList());

        title(sh, s.title,
            "Низкий рейтинг, но вступили ПОСЛЕ " + newcomerSince.format(Player.FMT)
            + "  (порог: " + threshold + ")", 5);
        header(sh, s.hdr, 1, "#", "Никнейм", "Звание", "Полковые очки", "Дата вступления");

        for (int i = 0; i < newbs.size(); i++) {
            Player p = newbs.get(i);
            XSSFRow row = sh.createRow(i + 2);
            row.setHeightInPoints(18);
            c(row, 0, i + 1,          s.newbNm);
            c(row, 1, p.nick,          s.newbTx);
            c(row, 2, p.rank,          s.newbTx);
            c(row, 3, p.rating,        s.newbNm);
            c(row, 4, p.joinDateStr(), s.newbTx);
        }

        sh.setColumnWidth(0,  1_500);
        sh.setColumnWidth(1, 10_500);
        sh.setColumnWidth(2,  5_500);
        sh.setColumnWidth(3,  5_500);
        sh.setColumnWidth(4,  5_500);
    }

    private void title(XSSFSheet sh, XSSFCellStyle st, String text, int span) {
        XSSFRow r = sh.createRow(0); r.setHeightInPoints(26);
        XSSFCell cell = r.createCell(0);
        cell.setCellValue(text); cell.setCellStyle(st);
        sh.addMergedRegion(new CellRangeAddress(0, 0, 0, span - 1));
    }

    private void header(XSSFSheet sh, XSSFCellStyle st, int ri, String... labels) {
        XSSFRow r = sh.createRow(ri); r.setHeightInPoints(20);
        for (int i = 0; i < labels.length; i++) {
            XSSFCell cell = r.createCell(i);
            cell.setCellValue(labels[i]); cell.setCellStyle(st);
        }
    }

    private void addStat(XSSFSheet sh, St s, int ri, String lbl, int val, byte[] color) {
        XSSFRow r = sh.createRow(ri);
        XSSFCell lc = r.createCell(4); lc.setCellValue(lbl); lc.setCellStyle(s.mkStatLbl(color));
        XSSFCell vc = r.createCell(5); vc.setCellValue(val);  vc.setCellStyle(s.mkStatVal(color));
    }

    private void addLegend(XSSFWorkbook wb, XSSFSheet sh, int ri,
                           XSSFCellStyle bg, String text) {
        XSSFRow r = sh.createRow(ri);
        XSSFCell c = r.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(bg);
        sh.addMergedRegion(new CellRangeAddress(ri, ri, 0, 5));
    }

    private void c(XSSFRow row, int col, String val, XSSFCellStyle st) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(val == null ? "" : val);
        cell.setCellStyle(st);
    }

    private void c(XSSFRow row, int col, int val, XSSFCellStyle st) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(val);
        cell.setCellStyle(st);
    }

    static class St {
        final XSSFWorkbook wb;
        final XSSFCellStyle title, hdr;
        final XSSFCellStyle probTx, probNm;
        final XSSFCellStyle newbTx, newbNm;
        final XSSFCellStyle evenTx, evenNm, oddTx, oddNm;

        St(XSSFWorkbook wb) {
            this.wb = wb;
            title  = mkTitle(wb);
            hdr    = mkHdr(wb);
            probTx = mk(wb, PROB_BG, PROB_FG, true, false);
            probNm = mk(wb, PROB_BG, PROB_FG, true, true);
            newbTx = mk(wb, NEW_BG,  NEW_FG,  false, false);
            newbNm = mk(wb, NEW_BG,  NEW_FG,  false, true);
            evenTx = mk(wb, EVEN_BG, BLACK,   false, false);
            evenNm = mk(wb, EVEN_BG, BLACK,   false, true);
            oddTx  = mk(wb, ODD_BG,  BLACK,   false, false);
            oddNm  = mk(wb, ODD_BG,  BLACK,   false, true);
        }

        XSSFCellStyle mkStatLbl(byte[] color) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setAlignment(HorizontalAlignment.RIGHT);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont f = wb.createFont(); f.setBold(true);
            f.setColor(new XSSFColor(color, null)); f.setFontName("Arial");
            s.setFont(f); return s;
        }

        XSSFCellStyle mkStatVal(byte[] color) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont f = wb.createFont(); f.setBold(true);
            f.setColor(new XSSFColor(color, null)); f.setFontName("Arial");
            s.setFont(f); return s;
        }

        private static XSSFCellStyle mkTitle(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(new XSSFColor(DARK, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont f = wb.createFont(); f.setBold(true);
            f.setFontHeightInPoints((short) 12);
            f.setColor(new XSSFColor(HDR_FG, null)); f.setFontName("Arial");
            s.setFont(f); return s;
        }

        private static XSSFCellStyle mkHdr(XSSFWorkbook wb) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(new XSSFColor(HDR_BG, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont f = wb.createFont(); f.setBold(true);
            f.setColor(new XSSFColor(HDR_FG, null)); f.setFontName("Arial");
            s.setFont(f); border(s); return s;
        }

        private static XSSFCellStyle mk(XSSFWorkbook wb, byte[] bg, byte[] fg,
                                        boolean bold, boolean center) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFillForegroundColor(new XSSFColor(bg, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            s.setAlignment(center ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            XSSFFont f = wb.createFont(); f.setBold(bold);
            f.setColor(new XSSFColor(fg, null)); f.setFontName("Arial");
            s.setFont(f); border(s); return s;
        }

        private static void border(XSSFCellStyle s) {
            XSSFColor g = new XSSFColor(GREY, null);
            s.setBorderBottom(BorderStyle.THIN); s.setBottomBorderColor(g);
            s.setBorderTop(BorderStyle.THIN);    s.setTopBorderColor(g);
            s.setBorderLeft(BorderStyle.THIN);   s.setLeftBorderColor(g);
            s.setBorderRight(BorderStyle.THIN);  s.setRightBorderColor(g);
        }
    }

    private static byte[] rgb(String hex) {
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}
