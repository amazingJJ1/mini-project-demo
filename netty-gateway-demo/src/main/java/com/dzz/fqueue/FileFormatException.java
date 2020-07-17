package com.dzz.fqueue;

/**
 * @author zoufeng
 * @date 2020-7-7
 */
public class FileFormatException extends Exception {

    private static final long serialVersionUID = 6950322066714479555L;

    public FileFormatException() {
        super();
    }

    public FileFormatException(String message) {
        super(message);
    }

    public FileFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileFormatException(Throwable cause) {
        super(cause);
    }
}