package com.wt;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Использует Microsoft Playwright — встроенный Chromium.
 * Chrome на ПК не нужен. При первом запуске скачивает браузер (~120MB).
 */
public class Scraper {

    private static final String URL =
            "https://warthunder.ru/ru/community/claninfo/Jagdflugzeug";

    private static final Pattern PLAYER_BLOCK = Pattern.compile(
            "(?m)^(\\d{1,3})\\n"
                    + "(.+?)\\n"
                    + "(\\d+)\\n"
                    + "(\\d+)\\n"
                    + "(Командир|Офицер|Сержант|Рядовой|clan/sergeant|clan/officer|clan/regular|clan/commander)\\n"
                    + "(\\d{2}\\.\\d{2}\\.\\d{4})"
    );

    public interface ProgressListener {
        void onStatus(String message);
    }

    public List<Player> fetch(ProgressListener listener) {
        log(listener, "Подготовка браузера...");

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.List.of(
                            "--no-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-blink-features=AutomationControlled",
                            "--lang=ru-RU"
                    ));

            try (Browser browser = playwright.chromium().launch(opts)) {
                BrowserContext ctx = browser.newContext(
                        new Browser.NewContextOptions()
                                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                        "Chrome/124.0.0.0 Safari/537.36")
                                .setLocale("ru-RU")
                );

                Page page = ctx.newPage();

                log(listener, "Открываю страницу клана...");
                page.navigate(URL, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                log(listener, "Жду загрузки списка игроков...");
                try {
                    page.waitForSelector("a[href*='userinfo']",
                            new Page.WaitForSelectorOptions().setTimeout(15000));
                } catch (Exception e) {
                    log(listener, "Таймаут — продолжаю...");
                }

                Thread.sleep(800);

                log(listener, "Прокручиваю страницу...");
                int prevCount = 0;
                for (int attempt = 0; attempt < 15; attempt++) {
                    page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                    Thread.sleep(800);
                    int count = (Integer) page.evaluate(
                            "document.querySelectorAll(\"a[href*='userinfo']\").length");
                    log(listener, "Найдено игроков: " + count);
                    if (count == prevCount) break;
                    prevCount = count;
                }
                page.evaluate("window.scrollTo(0, 0)");
                Thread.sleep(400);

                String pageText = (String) page.evaluate("document.body.innerText");

                try (FileWriter fw = new FileWriter("clan_debug.txt")) {
                    fw.write(pageText);
                    log(listener, "Дамп сохранён в clan_debug.txt");
                } catch (IOException e) {
                    log(listener, "Не удалось сохранить дамп: " + e.getMessage());
                }

                log(listener, "Парсю данные...");
                List<Player> result = parsePageText(pageText);
                log(listener, "Найдено регуляркой: " + result.size() + " из " + prevCount);

                if (result.isEmpty()) {
                    result = fallback(page);
                }

                if (result.isEmpty())
                    throw new RuntimeException("Игроки не найдены!");

                return result;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Прервано", e);
        }
    }

    private List<Player> parsePageText(String text) {
        List<Player> result = new ArrayList<>();
        Matcher m = PLAYER_BLOCK.matcher(text);
        while (m.find()) {
            result.add(new Player(
                    m.group(2).trim(),
                    Integer.parseInt(m.group(3)),
                    normalizeRank(m.group(5)),
                    parseDate(m.group(6))
            ));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Player> fallback(Page page) {
        List<Player> result = new ArrayList<>();
        try {
            List<String> blocks = (List<String>) page.evaluate(
                    "() => {" +
                            "  var links = document.querySelectorAll('a[href*=\"userinfo\"]');" +
                            "  var results = [];" +
                            "  links.forEach(a => {" +
                            "    var el = a;" +
                            "    for (var i = 0; i < 5; i++) {" +
                            "      el = el.parentElement; if (!el) break;" +
                            "      var t = el.innerText || '';" +
                            "      if (t.match(/(Командир|Офицер|Сержант|Рядовой|clan/sergeant|clan/officer|clan/commander)/)) { results.push(t.trim()); break; }" +
                            "    }" +
                            "  });" +
                            "  return results;" +
                            "}"
            );

            if (blocks == null) return result;
            Pattern rankPat = Pattern.compile("Командир|Офицер|Сержант|Рядовой|clan/sergeant|clan/officer|clan/commander");
            Pattern datePat = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})");
            Pattern numPat  = Pattern.compile("\\b(\\d+)\\b");

            for (String block : blocks) {
                String[] lines = block.split("\\n");
                String nick = "";
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.matches("\\d+") ||
                            datePat.matcher(line).matches() || rankPat.matcher(line).find()) continue;
                    nick = line; break;
                }
                if (nick.isEmpty()) continue;

                String rank = "Рядовой";
                Matcher rm = rankPat.matcher(block);
                if (rm.find()) rank = rm.group();

                LocalDate date = null;
                Matcher dm = datePat.matcher(block);
                if (dm.find()) date = parseDate(dm.group(1));

                String clean = datePat.matcher(block).replaceAll(" ");
                List<Integer> nums = new ArrayList<>();
                Matcher nm = numPat.matcher(clean);
                while (nm.find()) nums.add(Integer.parseInt(nm.group(1)));

                int rating = 0;
                if (nums.size() >= 2) rating = nums.get(1);
                else if (nums.size() == 1) rating = nums.get(0);
                if (rating > 99999) rating = 0;

                result.add(new Player(nick, rating, rank, date));
            }
        } catch (Exception e) {
            System.err.println("Fallback error: " + e.getMessage());
        }
        return result;
    }

    private static String normalizeRank(String rank) {
        if (rank == null) return "Рядовой";
        switch (rank) {
            case "clan/sergeant":  return "Сержант";
            case "clan/officer":   return "Офицер";
            case "clan/commander": return "Командир";
            case "clan/regular":  return "Рядовой";
            default: return rank;
        }
    }

    private static LocalDate parseDate(String s) {
        try { return LocalDate.parse(s, Player.FMT); } catch (Exception e) { return null; }
    }

    private static void log(ProgressListener l, String msg) {
        System.out.println(msg);
        if (l != null) l.onStatus(msg);
    }
}
