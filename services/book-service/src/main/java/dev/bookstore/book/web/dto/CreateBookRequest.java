package dev.bookstore.book.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Payload of {@code POST /books}. Wrapper types ({@code Integer}) are used where "missing"
 * must be distinguishable from zero, so {@code @NotNull} can reject an absent field.
 */
public record CreateBookRequest(
        @NotBlank @Pattern(regexp = "\\d{13}", message = "must be exactly 13 digits") String isbn,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @PositiveOrZero Integer stock) {
}
