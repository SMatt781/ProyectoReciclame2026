package com.example.proyectoreciclame.util;

import java.util.ArrayList;
import java.util.List;

public class
PaginationUtils {

    private PaginationUtils() {
    }

    public static List<Object> buildPageNumbers(int currentPage, int totalPages) {
        List<Object> pages = new ArrayList<>();

        if (totalPages <= 0) {
            return pages;
        }

        if (totalPages <= 7) {
            for (int i = 0; i < totalPages; i++) {
                pages.add(i);
            }
            return pages;
        }

        pages.add(0);

        int start = Math.max(1, currentPage - 1);
        int end = Math.min(totalPages - 2, currentPage + 1);

        if (start > 1) {
            pages.add("...");
        }

        for (int i = start; i <= end; i++) {
            pages.add(i);
        }

        if (end < totalPages - 2) {
            pages.add("...");
        }

        pages.add(totalPages - 1);

        return pages;
    }
}