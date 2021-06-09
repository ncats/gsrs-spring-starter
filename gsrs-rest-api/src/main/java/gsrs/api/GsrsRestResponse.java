package gsrs.api;

import lombok.Data;

import java.util.Optional;

@Data
public class GsrsRestResponse {

    private int statusCode;

    private Optional<GsrsErrorResponse> errorResponse;


}
