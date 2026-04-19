package com.generic4.itda.dto.resume.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CareerItemFormPeriodValidator.class)
@Documented
public @interface ValidCareerPeriod {

    String message() default "Invalid career period";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

