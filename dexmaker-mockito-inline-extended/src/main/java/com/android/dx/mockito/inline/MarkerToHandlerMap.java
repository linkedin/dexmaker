package com.android.dx.mockito.inline;

import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A map for mock marker object -> {@link InvocationHandlerAdapter} but
 * does not use the mock marker object as the key directly.
 * The problem of not doing so is that the object's real hashCode() and equals() =
 * methods will be invoked during
 * {@link InlineStaticMockMaker#createMock(MockCreationSettings, MockHandler)}. This poses a
 * potential test runtime error depending on the object's hashCode() implementation
 */
class MarkerToHandlerMap implements Map<Object, InvocationHandlerAdapter> {

    // NOTE: Must be ConcurrentHashMap to ensure thread safety when background threads
    // perform mock dispatch while test threads register or remove handlers. Note that
    // ConcurrentHashMap does not accept {@code null} keys or values, which is why null checks
    // are explicitly handled in this map.
    private final Map<MockMarkerKey, InvocationHandlerAdapter> markerToHandler = new ConcurrentHashMap<>();

    @Override
    public int size() {
        return markerToHandler.size();
    }

    @Override
    public boolean isEmpty() {
        return markerToHandler.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        return markerToHandler.containsKey(new MockMarkerKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        return markerToHandler.containsValue(value);
    }

    @Override
    public InvocationHandlerAdapter get(Object key) {
        if (key == null) {
            return null;
        }
        return markerToHandler.get(new MockMarkerKey(key));
    }

    @Override
    public InvocationHandlerAdapter put(Object key, InvocationHandlerAdapter value) {
        if (key == null || value == null) {
            return null;
        }
        return markerToHandler.put(new MockMarkerKey(key), value);
    }

    @Override
    public InvocationHandlerAdapter remove(Object key) {
        if (key == null) {
            return null;
        }
        return markerToHandler.remove(new MockMarkerKey(key));
    }

    @Override
    public void putAll(Map<?, ? extends InvocationHandlerAdapter> m) {
        for (Entry<?, ? extends InvocationHandlerAdapter> entry : m.entrySet()) {
            put(new MockMarkerKey(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public void clear() {
        markerToHandler.clear();
    }

    @Override
    public Set<Object> keySet() {
        Set<Object> set = new HashSet<>(markerToHandler.size());
        for (MockMarkerKey key : markerToHandler.keySet()) {
            set.add(key.mockMarker);
        }
        return set;
    }

    @Override
    public Collection<InvocationHandlerAdapter> values() {
        return markerToHandler.values();
    }

    @Override
    @SuppressWarnings("InfiniteRecursion")
    public Set<Entry<Object, InvocationHandlerAdapter>> entrySet() {
        Set<Entry<Object, InvocationHandlerAdapter>> set = new HashSet<>(markerToHandler.size());
        for (Entry<MockMarkerKey, InvocationHandlerAdapter> entry : markerToHandler.entrySet()) {
            set.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey().mockMarker, entry.getValue()));
        }
        return set;
    }

    private static class MockMarkerKey {

        private final Object mockMarker;

        public MockMarkerKey(Object mockMarker) {
            this.mockMarker = mockMarker;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MockMarkerKey mockMarkerKey = (MockMarkerKey) o;

            return mockMarker == mockMarkerKey.mockMarker;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(mockMarker);
        }
    }
}
