package com.generic4.itda.exception;

public class DuplicateEmailException extends IllegalArgumentException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
