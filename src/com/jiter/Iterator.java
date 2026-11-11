package com.jiter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

public interface Iterator<T> extends Iterable<T>, IntoIterator<T> {

    Optional<T> next();

    @Override
    default java.util.Iterator<T> iterator() {
        final var peekable = peekable();
        return new java.util.Iterator<>() {
            @Override
            public boolean hasNext() {
                return peekable.peek().isPresent();
            }

            @Override
            public T next() {
                return peekable.next().get();
            }
        };
    }

    @Override
    default Iterator<T> into_iterator() {
        return this;
    }

    default boolean all(Function<T, Boolean> condition) {
        for(final var item : this) if(!condition.apply(item)) return false;
        return true;
    }

    default boolean any(Function<T, Boolean> condition) {
        for(final var item : this) if(condition.apply(item)) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    default Iterator<T[]> array_chunks(int n) {
        assert n > 0;

        return () -> {
            var optional = next();
            if(optional.isEmpty()) return Optional.empty();

            final var chunk = (T[]) Array.newInstance(optional.get().getClass(), n);
            chunk[0] = optional.get();

            for(int i = 1; i < n; i++) {
                if((optional = next()).isEmpty()) return Optional.empty();
                chunk[i] = optional.get();
            }

            return Optional.of(chunk);
        };
    }

    default Iterator<T> chain(Iterator<T> second) {
        return () -> next().or(second::next);
    }

    default Iterator<T> chain(IntoIterator<T> second) {
        return chain(second.into_iterator());
    }

    default Iterator<T> chain(Iterable<T> second) {
        return chain(of(second.iterator()));
    }

    default ArrayList<T> collect() {
        return collectInto(new ArrayList<>());
    }

    default ArrayList<T> collectInto(ArrayList<T> list) {
        forEach((Consumer<T>) list::add);
        return list;
    }

    default int count() {
        int c = 0;
        for(final var item : this) c++;
        return c;
    }

    default Iterator<T> cycle() {
        final var list = collect();

        return list.isEmpty() ? Optional::empty : new Iterator<>() {
            private int i = 0;

            @Override
            public Optional<T> next() {
                return Optional.of(list.get(i++ % list.size()));
            }
        };
    }

    default Zipped<T, Integer> enumerate() {
        return zip(range(0));
    }

    default boolean eq(Iterator<T> iterator) {
        return zip(iterator).all(zip -> zip.a().equals(zip.b()))
                && next().isEmpty() && iterator.next().isEmpty();
    }

    default<K> boolean eqBy(Iterator<K> iterator, BiFunction<T, K, Boolean> predicate) {
        return zip(iterator).all(zip -> predicate.apply(zip.a(), zip.b()))
                && next().isEmpty() && iterator.next().isEmpty();
    }

    default Iterator<T> filter(Function<T, Boolean> filter) {
        return () -> find(filter);
    }

    default<K> Iterator<K> filterMap(Function<T, Optional<K>> filter) {
        return () -> findMap(filter);
    }

    default<K> Iterator<K> flatMap(Function<T, Iterator<K>> function) {
        final var parent = this;
        return new Iterator<>() {
            private Iterator<K> iterator = parent.next().map(function).orElse(null);

            @Override
            public Optional<K> next() {
                if(iterator == null) return Optional.empty();

                Optional<K> optional;
                while((optional = iterator.next()).isEmpty()) {
                    iterator = parent.next().map(function).orElse(null);
                    if(iterator == null) return Optional.empty();
                }

                return optional;
            }
        };
    }

    default Optional<T> find(Function<T, Boolean> predicate) {
        for(Optional<T> optional; (optional = next()).isPresent();) {
            if(predicate.apply(optional.get())) return optional;
        }
        return Optional.empty();
    }

    default<K> Optional<K> findMap(Function<T, Optional<K>> predicate) {
        for(Optional<T> optional; (optional = next()).isPresent();) {
            final var mapped = predicate.apply(optional.get());
            if(mapped.isPresent()) return mapped;
        }
        return Optional.empty();
    }

    default<K> Iterator<K> flatten() {
        final var parent = this;
        return new Iterator<>() {
            private Iterator<K> iterator = empty();

            @Override
            @SuppressWarnings("unchecked")
            public Optional<K> next() {
                var next = iterator.next();
                for (; next.isEmpty(); next = iterator.next()) {
                    final var next_iterator = parent.next();
                    if (next_iterator.isEmpty()) return Optional.empty();

                    if (next_iterator.get() instanceof IntoIterator<?> into) {
                        iterator = into.into_iterator().flatten();
                        continue;
                    }

                    if (next_iterator.get() instanceof Iterable<?> iterable) {
                        iterator = of(iterable.iterator()).flatten();
                        continue;
                    }

                    return (Optional<K>) next_iterator;
                }

                return next;
            }
        };
    }

    default<K> K fold(K startValue, BiFunction<T, K, K> function) {
        for(final var item : this) startValue = function.apply(item, startValue);
        return startValue;
    }

    @Override
    default void forEach(Consumer<? super T> consumer) {
        for(final var item : this) consumer.accept(item);
    }

    default Iterator<T> fuse() {
        final var iterator = this;
        return new Iterator<>() {
            private boolean fused = false;

            @Override
            public Optional<T> next() {
                if(fused) return Optional.empty();

                final var optional = iterator.next();
                fused = optional.isEmpty();

                return optional;
            }
        };
    }

    default Iterator<T> inspect(Consumer<T> consumer) {
        final var list = collect();
        list.forEach(consumer);
        return of(list);
    }

    default Iterator<T> intersperse(T value) {
        return intersperseWith(() -> value);
    }

    default Iterator<T> intersperseWith(Supplier<T> supplier) {
        final var iterator = this;
        return new Iterator<>() {
            private boolean intersperse = true;

            @Override
            public Optional<T> next() {
                intersperse = !intersperse;
                return intersperse ? Optional.of(supplier.get()) : iterator.next();
            }
        };
    }

    default boolean isPartitioned(Function<T, Boolean> predicate) {
        boolean state = true;
        for(final var item : this) {
            final boolean predicate_state = predicate.apply(item);
            if(!state && predicate_state) return false;
            state = predicate_state;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    default boolean isSorted() {
        return isSortedBy((a, b) -> ((Comparable<T>) a).compareTo(b) >= 0);
    }

    default boolean isSortedBy(BiFunction<T, T, Boolean> predicate) {
        var first = next();
        if(first.isEmpty()) return true;

        for(Optional<T> second; (second = next()).isPresent(); first = second) {
            if(!predicate.apply(first.get(), second.get())) return false;
        }

        return true;
    }

    default T nth(int i) throws IndexOutOfBoundsException {
        Optional<T> optional = next();
        if(optional.isEmpty()) throw new IndexOutOfBoundsException();

        while(i-- > 0) {
            if((optional = next()).isEmpty()) throw new IndexOutOfBoundsException();
        }

        return optional.get();
    }

    default<K> Iterator<K> map(Function<T, K> function) {
        return () -> next().map(function);
    }

    default Peekable<T> peekable() {
        return new Peekable<>(this);
    }

    default Iterator<T> reverse() {
        return of(collect()).reverse();
    }

    default<K> Zipped<T, K> zip(Iterator<K> iterator) {
        return new Zipped<>(this, iterator);
    }

    static<T> Iterator<T> of(ArrayList<T> list) {
        return new Iterator<>() {
            private int i = 0;
            private int j = list.size();

            @Override
            public Optional<T> next() {
                return i >= list.size() ? Optional.empty() : Optional.of(list.get(i++));
            }

            @Override
            public Iterator<T> reverse() {
                return () -> j <= i ? Optional.empty() : Optional.of(list.get(--j));
            }

            @Override
            public ArrayList<T> collect() {
                if(i == 0 && j == list.size()) return list;
                return Iterator.super.collect();
            }
        };
    }

    static<T> Iterator<T> of(List<T> list) {
        return of(list.iterator());
    }

    static<T> Iterator<T> of(java.util.Iterator<T> iterator) {
        return () -> iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    static Iterator<Integer> range(int bottom, int top, int step) {
        return new Iterator<>() {
            private int val = bottom;

            @Override
            public Optional<Integer> next() {
                if(val > top) return Optional.empty();
                final int next = val;
                val += step;
                return Optional.of(next);
            }
        };
    }

    static Iterator<Integer> range(int bottom, int top) {
        return range(bottom, top, 1);
    }

    static Iterator<Integer> range(int bottom) {
        return range(bottom, Integer.MAX_VALUE, 1);
    }

    static<K> Iterator<K> empty() {
        return Optional::empty;
    }

}
