package me.zort.commandlib.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContextualCollection<T> implements Iterable<T> {

    private final String namespace;
    private final List<ContextualCollection<T>> children;
    private final List<T> values;

    public ContextualCollection() {
        this("");
    }

    private ContextualCollection(String namespace) {
        this.namespace = namespace;
        this.children = new CopyOnWriteArrayList<>();
        this.values = new CopyOnWriteArrayList<>();
    }

    // Save object based on provided context.
    // Example: Context "test " will be applicable for all requests for get
    // starting with "test ".
    public boolean save(String context, T object) {
        if(!isInputApplicable(context))
            return false;

        context = context.replaceFirst(namespace, "");
        if(context.isEmpty()) {
            values.add(object);
        } else {
            for (ContextualCollection<T> child : children) {
                if(child.save(context, object)) {
                    return true;
                }
            }

            ContextualCollection<T> newOne = new ContextualCollection<>(namespace + context);
            if(!newOne.save(context, object)) {
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

    public boolean isInputApplicable(String input) {
        return input.startsWith(namespace);
    }

}
