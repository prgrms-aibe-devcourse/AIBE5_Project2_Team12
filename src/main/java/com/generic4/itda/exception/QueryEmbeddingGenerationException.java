package com.generic4.itda.exception;

public class QueryEmbeddingGenerationException extends RuntimeException {

    public QueryEmbeddingGenerationException(String message) {
        super(message);
    }

    public QueryEmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
