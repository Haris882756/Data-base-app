package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

enum class ColumnType(val displayName: String, val icon: String, val description: String) {
    SHORT_TEXT("Short Text", "title", "Single-line text up to 255 characters"),
    LONG_TEXT("Long Text", "notes", "Multi-line text, descriptions or notes"),
    INTEGER("Integer", "pin", "Whole numbers (e.g. 42, -100)"),
    DECIMAL("Decimal", "calculate", "Fractional numbers (e.g. 19.99)"),
    BOOLEAN("Boolean", "toggle_on", "True or False toggle switch"),
    DATE("Date", "calendar_today", "Date (YYYY-MM-DD)"),
    TIME("Time", "schedule", "Time (HH:MM or HH:MM:SS)"),
    DATETIME("Date & Time", "event", "Combined Date & Time"),
    EMAIL("Email", "email", "Validated email address"),
    PHONE("Phone Number", "phone", "Phone number with international support"),
    URL("URL / Link", "link", "Web URL (https://...)"),
    SINGLE_CHOICE("Single Choice", "radio_button_checked", "Select one option from a predefined list"),
    MULTI_CHOICE("Multiple Choice", "checklist", "Select multiple options from a predefined list"),
    TAGS("Tags", "sell", "Comma-separated or badge tags"),
    JSON_DOCUMENT("JSON Document", "data_object", "Structured JSON object or array"),
    ATTACHMENT_METADATA("File / Attachment", "attach_file", "Metadata for local files or photos"),
    AUTO_ID("Auto Identifier", "fingerprint", "System-generated sequential or UUID key");

    companion object {
        fun fromString(value: String): ColumnType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SHORT_TEXT
        }
    }
}

object FieldValidator {
    private val EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE_REGEX = Pattern.compile("^[+]?[0-9\\s\\-().]{7,25}$")
    private val URL_REGEX = Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$", Pattern.CASE_INSENSITIVE)

    fun validate(
        value: String,
        type: ColumnType,
        isRequired: Boolean,
        minLength: Int? = null,
        maxLength: Int? = null,
        minValue: Double? = null,
        maxValue: Double? = null,
        options: List<String> = emptyList()
    ): ValidationResult {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            return if (isRequired && type != ColumnType.AUTO_ID) {
                ValidationResult(isValid = false, errorMessage = "This field is required.")
            } else {
                ValidationResult(isValid = true)
            }
        }

        if (minLength != null && minLength > 0 && trimmed.length < minLength) {
            return ValidationResult(isValid = false, errorMessage = "Minimum length is $minLength characters.")
        }
        if (maxLength != null && maxLength > 0 && trimmed.length > maxLength) {
            return ValidationResult(isValid = false, errorMessage = "Maximum length is $maxLength characters.")
        }

        return when (type) {
            ColumnType.SHORT_TEXT -> {
                if (trimmed.length > 255) {
                    ValidationResult(isValid = false, errorMessage = "Short text cannot exceed 255 characters.")
                } else ValidationResult(isValid = true)
            }
            ColumnType.LONG_TEXT -> ValidationResult(isValid = true)
            ColumnType.INTEGER -> {
                try {
                    val num = trimmed.toLong()
                    if (minValue != null && num < minValue) {
                        ValidationResult(isValid = false, errorMessage = "Value must be ≥ ${minValue.toLong()}.")
                    } else if (maxValue != null && num > maxValue) {
                        ValidationResult(isValid = false, errorMessage = "Value must be ≤ ${maxValue.toLong()}.")
                    } else {
                        ValidationResult(isValid = true)
                    }
                } catch (e: NumberFormatException) {
                    ValidationResult(isValid = false, errorMessage = "Must be a valid integer.")
                }
            }
            ColumnType.DECIMAL -> {
                try {
                    val num = trimmed.toDouble()
                    if (minValue != null && num < minValue) {
                        ValidationResult(isValid = false, errorMessage = "Value must be ≥ $minValue.")
                    } else if (maxValue != null && num > maxValue) {
                        ValidationResult(isValid = false, errorMessage = "Value must be ≤ $maxValue.")
                    } else {
                        ValidationResult(isValid = true)
                    }
                } catch (e: NumberFormatException) {
                    ValidationResult(isValid = false, errorMessage = "Must be a valid decimal number.")
                }
            }
            ColumnType.BOOLEAN -> {
                val lower = trimmed.lowercase()
                if (lower == "true" || lower == "false" || lower == "1" || lower == "0" || lower == "yes" || lower == "no") {
                    ValidationResult(isValid = true)
                } else {
                    ValidationResult(isValid = false, errorMessage = "Must be true or false.")
                }
            }
            ColumnType.DATE -> {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                try {
                    format.parse(trimmed)
                    ValidationResult(isValid = true)
                } catch (e: Exception) {
                    ValidationResult(isValid = false, errorMessage = "Date must follow YYYY-MM-DD format.")
                }
            }
            ColumnType.TIME -> {
                if (trimmed.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$"))) {
                    ValidationResult(isValid = true)
                } else {
                    ValidationResult(isValid = false, errorMessage = "Time must follow HH:MM format.")
                }
            }
            ColumnType.DATETIME -> {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
                try {
                    format.parse(trimmed)
                    ValidationResult(isValid = true)
                } catch (e: Exception) {
                    ValidationResult(isValid = false, errorMessage = "Date & Time must follow YYYY-MM-DD HH:MM format.")
                }
            }
            ColumnType.EMAIL -> {
                if (EMAIL_REGEX.matcher(trimmed).matches()) {
                    ValidationResult(isValid = true)
                } else {
                    ValidationResult(isValid = false, errorMessage = "Invalid email address format.")
                }
            }
            ColumnType.PHONE -> {
                if (PHONE_REGEX.matcher(trimmed).matches()) {
                    ValidationResult(isValid = true)
                } else {
                    ValidationResult(isValid = false, errorMessage = "Invalid phone number format.")
                }
            }
            ColumnType.URL -> {
                if (URL_REGEX.matcher(trimmed).matches() || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                    ValidationResult(isValid = true)
                } else {
                    ValidationResult(isValid = false, errorMessage = "Must be a valid URL starting with http:// or https://")
                }
            }
            ColumnType.SINGLE_CHOICE -> {
                if (options.isNotEmpty() && !options.contains(trimmed)) {
                    ValidationResult(isValid = false, errorMessage = "Selected value is not in permitted options.")
                } else {
                    ValidationResult(isValid = true)
                }
            }
            ColumnType.MULTI_CHOICE -> ValidationResult(isValid = true)
            ColumnType.TAGS -> ValidationResult(isValid = true)
            ColumnType.JSON_DOCUMENT -> {
                val isJson = (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                        (trimmed.startsWith("[") && trimmed.endsWith("]"))
                if (isJson) {
                    ValidationResult(isValid = true)
                } else {
                    ValidationResult(isValid = false, errorMessage = "Invalid JSON structure. Must start with { or [.")
                }
            }
            ColumnType.ATTACHMENT_METADATA -> ValidationResult(isValid = true)
            ColumnType.AUTO_ID -> ValidationResult(isValid = true)
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
