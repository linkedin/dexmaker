package com.android.dx.mockito.inline;

import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A map for mock marker object -> {@link InvocationHandlerAdapter} but
 * does not use the mock marker object as the key directly.
 * The problem of not doing so is that the object's real hashCode() and equals() =
 * methods will be invoked during
 * {@link InlineStaticMockMaker#createMock(MockCreationSettings, MockHandler)}. This poses a
 * potential test runtime error depending on the object's hashCode() implementation
 */
class MarkerToHandlerMap implements Map<Object, InvocationHandlerAdapter> {

    private final Map<MockMarkerKey, InvocationHandlerAdapter> markerToHandler = new HashMap<>();

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
        return markerToHandler.containsKey(new MockMarkerKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return markerToHandler.containsValue(value);
    }

    @Override
    public InvocationHandlerAdapter get(Object key) {
        return markerToHandler.get(new MockMarkerKey(key));
    }

    @Override
    public InvocationHandlerAdapter put(Object key, InvocationHandlerAdapter value) {
        return markerToHandler.put(new MockMarkerKey(key), value);
    }

    @Override
    public InvocationHandlerAdapter remove(Object key) {
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
        Set<Object> set = new HashSet<>(entrySet().size());
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
    public Set<Entry<Object, InvocationHandlerAdapter>> entrySet() {
        Set<Entry<Object, InvocationHandlerAdapter>> set = new HashSet<>(entrySet().size());
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
