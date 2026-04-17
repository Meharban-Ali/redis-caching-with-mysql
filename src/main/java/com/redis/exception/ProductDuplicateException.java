
package com.redis.exception;

public class ProductDuplicateException extends RuntimeException {

    public ProductDuplicateException(String name) {
        super("Product already exists with name: " + name);
    }
}