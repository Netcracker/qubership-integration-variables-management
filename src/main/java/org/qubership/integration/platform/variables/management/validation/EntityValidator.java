/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.integration.platform.variables.management.validation;

import jakarta.validation.*;
import org.qubership.integration.platform.variables.management.validation.constraint.NotStartOrEndWithSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EntityValidator {

    private final Validator validator;

    @Autowired
    public EntityValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(final Object entity) {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(entity);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    public static class NotStartOrEndWithSpaceValidator implements ConstraintValidator<NotStartOrEndWithSpace, String> {

        private boolean optional;

        @Override
        public void initialize(NotStartOrEndWithSpace constraintAnnotation) {
            this.optional = constraintAnnotation.optional();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.isEmpty()) {
                return optional;
            }

            return value.length() == value.trim().length();
        }
    }
}