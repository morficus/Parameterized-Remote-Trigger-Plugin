package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import junit.framework.TestCase;

import java.util.Iterator;

public class SearchPatternTest extends TestCase {

    public void testSearchPattern() {
        SearchPattern sp = new SearchPattern(5, 2);
        // Test iterator() twice
        for (int x = 0; x < 2; x++) {
            Iterator<Integer> it = sp.iterator();
            assertEquals(5, it.next().intValue());
            assertEquals(4, it.next().intValue());
            assertEquals(6, it.next().intValue());
            assertEquals(3, it.next().intValue());
            assertEquals(7, it.next().intValue());
            assertEquals(false, it.hasNext());
        }
    }

}
