package com.ethan.janus.core.exception;

/**
 * 判断条件表达式不合法
 */
public class JanusException extends RuntimeException {

    public JanusException(String message) {
        super(message);
    }

    public JanusException(String message, Throwable e) {
        super(message, e);
    }
}
