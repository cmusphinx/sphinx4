package edu.cmu.sphinx.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;


public class PriorityQueue<E, P extends Comparable<? super P>> {

    public static <E, P extends Comparable<? super P>> PriorityQueue<E, P>
            newPriorityQueue(Function<E, P> priority)
    {
        return new PriorityQueue<E, P>(priority);
    }

    private final List<E> elements;
    private final Map<E, Integer> indices;
    private final Function<E, P> priority;

    private PriorityQueue(Function<E, P> priority) {
        checkNotNull(priority);
        this.priority = priority;
        elements = newArrayList();
        indices = newHashMap();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     *
     * @param elem
     * @param value
     * @return
     */
    public void insert(E elem) {
        elements.add(elem);
        indices.put(elem, elements.size() - 1);
        int p = elements.size() - 1;
        while (p > 0 && less(p, (p - 1) / 2))
            p = swap(p, (p - 1) / 2);
    }

    /**
     *
     * @return
     */
    public E getMin() {
        if (isEmpty())
            return null;
        return elements.get(0);
    }

    /**
     *
     * @return
     */
    public E extractMin() {
        if (isEmpty())
            return null;

        int p = swap(elements.size() - 1, 0);
        E elem = elements.remove(elements.size() - 1);
        while (p < elements.size() / 2) {
            int min = p;
            int left = 2 * p + 1;
            if (less(left, min))
                min = left;
            int right = 2 * p + 2;
            if (right < elements.size() && less(right, min))
                min = right;
            if (p != min)
                p = swap(p, min);
            else
                break;
        }
        return elem;
    }

    private int swap(int p, int q) {
        E elem = elements.get(p);
        elements.set(p, elements.get(q));
        elements.set(q, elem);
        return q;
    }

    private boolean less(int p, int q) {
        return priority.apply(elements.get(p))
                .compareTo(priority.apply(elements.get(q))) < 0;
    }
}
