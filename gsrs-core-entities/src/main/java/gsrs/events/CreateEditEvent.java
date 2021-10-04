package gsrs.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateEditEvent {
    private Class<?> kind;
    private Object id;
    private String comments;
    
    private String oldJson;
    private String version;

}
