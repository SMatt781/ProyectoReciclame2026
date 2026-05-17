package com.example.proyectoreciclame.util;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateUtil {

    public String formatRelative(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Nunca";
        }
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long days = duration.toDays();
        if (days > 0) {
            return "Hace " + days + " día" + (days > 1 ? "s" : "");
        }
        long hours = duration.toHours();
        if (hours > 0) {
            return "Hace " + hours + " hora" + (hours > 1 ? "s" : "");
        }
        long minutes = duration.toMinutes();
        if (minutes > 0) {
            return "Hace " + minutes + " minuto" + (minutes > 1 ? "s" : "");
        }
        return "Hace un momento";
    }

    public String formatShortDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("d MMM"));
    }
}