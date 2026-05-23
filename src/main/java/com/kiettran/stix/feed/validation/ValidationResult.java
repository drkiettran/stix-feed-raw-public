package com.kiettran.stix.feed.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {

    private final List<ValidationError> errors = new ArrayList<>();

    public void add(String field, String issue) {
        errors.add(new ValidationError(field, issue));
    }

    public boolean isValid() { return errors.isEmpty(); }

    public List<ValidationError> errors() {
        return Collections.unmodifiableList(errors);
    }
}
