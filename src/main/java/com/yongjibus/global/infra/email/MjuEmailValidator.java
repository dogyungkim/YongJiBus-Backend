package com.yongjibus.global.infra.email;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MjuEmailValidator implements ConstraintValidator<MjuEmail, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.endsWith("@mju.ac.kr") && value.length() > "@mju.ac.kr".length();
    }
}