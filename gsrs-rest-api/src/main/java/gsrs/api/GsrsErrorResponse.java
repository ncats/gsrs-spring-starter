package gsrs.api;

import lombok.Data;

@Data
public class GsrsErrorResponse {
    private int status;
    private String message;
}
