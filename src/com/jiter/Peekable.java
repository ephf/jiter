package com.jiter;

import java.util.Optional;

public class Peekable<T> implements Iterator<T> {

    private final Iterator<T> iterator;
    private Optional<T> current;

    public Peekable(Iterator<T> iterator) {
        this.iterator = iterator;
        this.current = iterator.next();
    }

    @Override
    public Optional<T> next() {
        final var next = current;
        if(next.isEmpty()) return Optional.empty();

        current = iterator.next();
        return next;
    }

    public Optional<T> peek() {
        return current;
    }

}
