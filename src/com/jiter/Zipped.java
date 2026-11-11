package com.jiter;

import java.util.Optional;
import java.util.function.*;

public class Zipped<A, B> implements Iterator<Zipped.Zip<A, B>> {

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    @FunctionalInterface
    public interface QuadFunction<T, U, V, W, R> {
        R apply(T t, U u, V v, W w);
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    public record Zip<A, B>(A a, B b) {}

    private final Iterator<A> iterator_a;
    private final Iterator<B> iterator_b;

    public Zipped(Iterator<A> iterator_a, Iterator<B> iterator_b) {
        this.iterator_a = iterator_a;
        this.iterator_b = iterator_b;
    }

    @Override
    public Optional<Zip<A, B>> next() {
        final var a = iterator_a.next();
        final var b = iterator_b.next();

        return a.isPresent() && b.isPresent()
                ? Optional.of(new Zip<>(a.get(), b.get()))
                : Optional.empty();
    }

    private<R> Function<Zip<A, B>, R> unzip(BiFunction<A, B, R> bi) {
        return zip -> bi.apply(zip.a, zip.b);
    }

    private<C, R> BiFunction<Zip<A, B>, C, R> unzip(TriFunction<A, B, C, R> tri) {
        return (zip, c) -> tri.apply(zip.a, zip.b, c);
    }

    private<R> BiFunction<Zip<A, B>, Zip<A, B>, R> unzip(QuadFunction<A, B, A, B, R> quad) {
        return (za, zb) -> quad.apply(za.a, za.b, zb.a, zb.b);
    }

    private Consumer<Zip<A, B>> unzip(BiConsumer<A, B> bi) {
        return zip -> bi.accept(zip.a, zip.b);
    }

    private<C> BiConsumer<Zip<A, B>, C> unzip(TriConsumer<A, B, C> tri) {
        return (zip, c) -> tri.accept(zip.a, zip.b, c);
    }

    public boolean all(BiFunction<A, B, Boolean> condition) {
        return all(unzip(condition));
    }

    public boolean any(BiFunction<A, B, Boolean> condition) {
        return any(unzip(condition));
    }

    public<K> boolean eqBy(Iterator<K> iterator, TriFunction<A, B, K, Boolean> predicate) {
        return eqBy(iterator, unzip(predicate));
    }

    public Iterator<Zip<A, B>> filter(BiFunction<A, B, Boolean> filter) {
        return filter(unzip(filter));
    }

    public<K> Iterator<K> filterMap(BiFunction<A, B, Optional<K>> filter) {
        return filterMap(unzip(filter));
    }

    public<K> Iterator<K> flatMap(BiFunction<A, B, Iterator<K>> function) {
        return flatMap(unzip(function));
    }

    public Optional<Zip<A, B>> find(BiFunction<A, B, Boolean> predicate) {
        return find(unzip(predicate));
    }

    public<K> Optional<K> findMap(BiFunction<A, B, Optional<K>> predicate) {
        return findMap(unzip(predicate));
    }

    public<K> K fold(K startValue, TriFunction<A, B, K, K> function) {
        return fold(startValue, unzip(function));
    }

    public void forEach(BiConsumer<A, B> consumer) {
        forEach(unzip(consumer));
    }

    public Iterator<Zip<A, B>> inspect(BiConsumer<A, B> consumer) {
        return inspect(unzip(consumer));
    }

    public boolean isPartitioned(BiFunction<A, B, Boolean> predicate) {
        return isPartitioned(unzip(predicate));
    }

    public boolean isSortedBy(QuadFunction<A, B, A, B, Boolean> predicate) {
        return isSortedBy(unzip(predicate));
    }

    public<K> Iterator<K> map(BiFunction<A, B, K> function) {
        return map(unzip(function));
    }

}
