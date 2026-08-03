package com.example.comptaperso

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Utilitaire pour la gestion des dates dans l'application.
 * Garantit l'utilisation du format ISO 8601 (yyyy-MM-dd) et évite les problèmes de locale.
 */
object DateUtils {

    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withLocale(Locale.US)

    /**
     * Retourne la date actuelle au format ISO (yyyy-MM-dd).
     */
    fun getTodayIso(): String {
        return LocalDate.now().format(isoFormatter)
    }

    /**
     * Formate une LocalDate en chaîne ISO.
     */
    fun formatIso(date: LocalDate): String {
        return date.format(isoFormatter)
    }

    /**
     * Parse une chaîne ISO en LocalDate.
     * Retourne null si le parsing échoue.
     */
    fun parseIso(isoDate: String): LocalDate? {
        return try {
            LocalDate.parse(isoDate, isoFormatter)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /**
     * Retourne un label relatif (aujourd'hui, hier, etc.) pour une date ISO.
     */
    fun getRelativeDateLabel(isoDate: String): String {
        val date = parseIso(isoDate) ?: return isoDate
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(date, today)

        return when {
            days == 0L -> "aujourd'hui"
            days == 1L -> "hier"
            days < 7L -> "il y a $days jours"
            days == 7L -> "il y a une semaine"
            days < 14L -> "il y a plus d'une semaine"
            else -> "il y a ${days / 7} semaines"
        }
    }

    /**
     * Vérifie si une date ISO correspond à aujourd'hui.
     */
    fun isToday(isoDate: String): Boolean {
        return isoDate == getTodayIso()
    }
}
