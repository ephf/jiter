# `jiter` - Java Iterators

Implements Rust iterators into Java

```java
import com.jiter.Iterator;

void main() {
    final var numbers = List.of(1, 2, 3, 4);
    
    Iterator.of(numbers)
            .zip(Iterator.of(numbers).reverse())
            .map((a, b) -> a * b)
            .chain(numbers)
            .enumerate()
            .forEach((number, i) -> IO.println(String.format("[%d] = %d", i, number)));
}
```

*Sample Output*

```
[0] = 4
[1] = 6
[2] = 6
[3] = 4
[4] = 1
[5] = 2
[6] = 3
[7] = 4
```