package edu.cmu.sphinx.common.collections;

import static com.google.common.base.Functions.forMap;
import static com.google.common.collect.Maps.newHashMap;
import static edu.cmu.sphinx.common.collections.PriorityQueue.newPriorityQueue;
import static java.util.Arrays.sort;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.Random;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class PriorityQueueTest {

    private Map<Integer, Integer> priorityMap;
    private PriorityQueue<Integer, Integer> queue;

    @BeforeMethod
    public void setUp() {
        priorityMap = newHashMap();
        queue = newPriorityQueue(forMap(priorityMap, Integer.MAX_VALUE));
    }

    @Test
    public void emptyQueue() {
        assertThat(queue.isEmpty(), is(true));
    }

    @Test
    public void defaultValue() {
        queue.insert(42);
        assertThat(queue.extractMin(), equalTo(42));
        assertThat(queue.isEmpty(), is(true));
    }

    @Test
    public void insertInAscendingOrder() {
        for (int i = 0; i < 10; ++i) {
            priorityMap.put(i, i);
            queue.insert(i);
        }

        for (int i = 0; i < 10; ++i)
            assertThat(queue.extractMin(), equalTo(i));
        assertThat(queue.isEmpty(), is(true));
    }

    @Test
    public void insertInDescendingOrder() {
        for (int i = 9; i >= 0; --i) {
            priorityMap.put(i, i);
            queue.insert(i);
        }

        for (int i = 0; i < 10; ++i)
            assertThat(queue.extractMin(), equalTo(i));
        assertThat(queue.isEmpty(), is(true));
    }

    @Test
    public void insertRandom() {
        int[] a = new int[1000];
        Random rng = new Random(42);
        for (int i = 0; i < a.length; ++i) {
            a[i] = rng.nextInt();
            priorityMap.put(a[i], a[i]);
            queue.insert(a[i]);
        }

        sort(a);
        for (int i = 0; i < a.length; ++i)
            assertThat(queue.extractMin(), equalTo(a[i]));
    }
}
