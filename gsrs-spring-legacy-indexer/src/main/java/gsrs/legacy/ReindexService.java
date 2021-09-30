package gsrs.legacy;

import gsrs.util.TaskListener;

import java.io.IOException;

public interface ReindexService<T> {

    default void executeAsync(Object id, TaskListener l) throws IOException{
        executeAsync(id, l, false);
    }
    default void execute(Object id, TaskListener l) throws IOException{
        execute(id, l, false);
    }

    void executeAsync(Object id, TaskListener l, boolean wipeIndexFirst) throws IOException;
    void execute(Object id, TaskListener l, boolean wipeIndexFirst) throws IOException;
}
