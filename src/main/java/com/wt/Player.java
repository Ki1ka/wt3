package com.wt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Player {
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public final String    nick;
    public final int       rating;
    public final String    rank;
    public final LocalDate joinDate;

    public Player(String nick, int rating, String rank, LocalDate joinDate) {
        this.nick     = nick;
        this.rating   = rating;
        this.rank     = rank;
        this.joinDate = joinDate;
    }

    /**
     * Красный (проблемный) = рейтинг ниже порога И вступил ДО даты среза.
     * Если вступил ПОСЛЕ даты среза — новенький, ещё не успел, не красный.
     */
    public boolean isProblematic(int threshold, LocalDate newcomerSince) {
        if (rating >= threshold) return false;                        // рейтинг OK
        if (joinDate != null && !joinDate.isBefore(newcomerSince)) return false; // новенький
        return true;
    }

    public String joinDateStr() {
        return joinDate != null ? joinDate.format(FMT) : "";
    }
}
