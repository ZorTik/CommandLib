package me.zort.commandlib.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NamespacedCollection<T> implements Iterable<T> {

    private final String namespace;
    private final List<NamespacedCollection<T>> children;
    private final List<T> values;

    public NamespacedCollection() {
        this("");
    }

    private NamespacedCollection(String namespace) {
        this.namespace = namespace;
        this.children = new CopyOnWriteArrayList<>();
        this.values = new CopyOnWriteArrayList<>();
    }

    // Save object based on provided context.
    // Example: Context "test " will be applicable for all requests for get
    // starting with "test ".
    public boolean save(String prefix, T object) {
        if(!isInputApplicable(prefix))
            return false;

        prefix = prefix.replaceFirst(namespace, "");
        if(prefix.isEmpty()) {
            values.add(object);
        } else {
            for (NamespacedCollection<T> child : children) {
                if(child.save(prefix, object)) {
                    return true;
                }
            }

            NamespacedCollection<T> newOne = new NamespacedCollection<>(namespace + prefix);
            if(!newOne.save(prefix, object)) {
                // WTF??
                throw new RuntimeException("Something went wrong while saving object to context.");
            }
            children.add(newOne);
        }
        return true;
    }

    // Objects that are in context that is smaller or equal to input
    // and also matches all symbols in correct order to input.

    // Example: input = "a.b.c", context = "a.b" will match.
    //
    // Input: "test", will match all contexts from "t" to "test".
    public List<T> getAllInContext(String input) {
        if(input == null || !isInputApplicable(input))
            return Collections.emptyList();

        List<T> result = new ArrayList<>(values);
        children.forEach(c -> values.addAll(c.getAllInContext(input)));
        return result;
    }

    public List<T> getAll() {
        ArrayList<T> values = new ArrayList<>(this.values);
        children.forEach(c -> values.addAll(c.getAll()));
        return values;
    }

    @Override
    public Iterator<T> iterator() {
        return getAll().iterator();
    }

    public boolean isInputApplicable(String prefix) {
        return prefix.startsWith(namespace);
    }

}
