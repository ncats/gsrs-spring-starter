package gsrs.legacy;

import lombok.Data;

import java.util.List;

public interface LegacySearchResult<T> {

    List<T> getContent();

}
