package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Search around a specified {@link startingValue} by a magnitude of {@link maxDrift}.
 */
public class SearchPattern implements Iterable<Integer> {
    private final int startingValue;
    private final int maxDrift;

    public SearchPattern(int startingValue, int maxDrift) {
        this.startingValue = startingValue;
        this.maxDrift = maxDrift;
    }

    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int drift = 0;

            public boolean hasNext() {
                return (drift != -maxDrift - 1);
            }

            public Integer next() {
                if (! hasNext()) throw new NoSuchElementException();
                int ret = startingValue + drift;
                if (drift < 0) {
                    drift = -drift;
                } else {
                    drift = -drift - 1;
                }
                return ret;
            }

            public void remove() {
                next();
            }
        };
    }
}
