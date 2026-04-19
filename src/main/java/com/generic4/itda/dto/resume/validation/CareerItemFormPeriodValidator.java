package com.generic4.itda.dto.resume.validation;

import com.generic4.itda.dto.resume.CareerItemForm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

public class CareerItemFormPeriodValidator implements ConstraintValidator<ValidCareerPeriod, CareerItemForm> {

    @Override
    public boolean isValid(CareerItemForm form, ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }

        Boolean currentlyWorking = form.getCurrentlyWorking();
        String endYearMonth = form.getEndYearMonth();

        // If currentlyWorking is missing, let @NotNull handle it.
        if (currentlyWorking == null) {
            return true;
        }

        if (currentlyWorking) {
            // When currently working, endYearMonth must be empty.
            if (endYearMonth != null && !endYearMonth.isBlank()) {
                return violationOnEndYearMonth(context, "재직중인 경력은 종료 연월을 비워야 합니다.");
            }
            return true;
        }

        // Not currently working: endYearMonth is required.
        if (endYearMonth == null || endYearMonth.isBlank()) {
            return violationOnEndYearMonth(context, "재직중이 아닌 경력은 종료 연월이 필요합니다.");
        }

        // Validate ordering only when both are parseable.
        YearMonth start = parseYearMonth(form.getStartYearMonth());
        YearMonth end = parseYearMonth(endYearMonth);
        if (start == null || end == null) {
            return true;
        }

        if (end.isBefore(start)) {
            return violationOnEndYearMonth(context, "종료 연월은 시작 연월보다 빠를 수 없습니다.");
        }

        return true;
    }

    private boolean violationOnEndYearMonth(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("endYearMonth")
                .addConstraintViolation();
        return false;
    }

    private YearMonth parseYearMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
}

