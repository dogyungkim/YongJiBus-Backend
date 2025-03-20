package com.yongjibus.global.infra.email;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MjuEmailValidator.class)
@Documented
public @interface MjuEmail {
    String message() default "잘못된 이메일 형식입니다. @mju.ac.kr만 사용 가능합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}