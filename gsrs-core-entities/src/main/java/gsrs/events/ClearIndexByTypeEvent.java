package gsrs.events;

import lombok.AllArgsConstructor;
import lombok.Data;

/*
Flag for clearing specific types from the main index
 */
@AllArgsConstructor
@Data
public class ClearIndexByTypeEvent {
    private Class<?> typeToClear;

}
