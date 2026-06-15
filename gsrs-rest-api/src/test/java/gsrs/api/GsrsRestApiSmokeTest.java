package gsrs.api;

import gsrs.api.internal.WithoutContentPagedResult;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GsrsRestApiSmokeTest {

    @Test
    void withoutContentPagedResultMapsMetadataToPagedResult() throws Exception {
        WithoutContentPagedResult source = new WithoutContentPagedResult();
        Date created = new Date();
        URI uri = new URI("https://example.org/api/v1/entities");
        URI next = new URI("https://example.org/api/v1/entities?skip=10");

        source.setId(11L);
        source.setVersion(2);
        source.setCreated(created);
        source.setEtagId("etag-1");
        source.setPath("/api/v1/entities");
        source.setUri(uri);
        source.setNextPageUri(next);
        source.setTotal(200L);
        source.setTop(10L);
        source.setSkip(0L);
        source.setCount(10L);

        GsrsEntityRestTemplate.PagedResult<String> result = source.toPagedResult(Collections.singletonList("item"));

        assertEquals(11L, result.getId());
        assertEquals(2, result.getVersion());
        assertSame(created, result.getCreated());
        assertEquals("etag-1", result.getEtagId());
        assertEquals("/api/v1/entities", result.getPath());
        assertEquals(uri, result.getUri());
        assertEquals(next, result.getNextPageUri());
        assertEquals(200L, result.getTotal());
        assertEquals(10L, result.getTop());
        assertEquals(0L, result.getSkip());
        assertEquals(10L, result.getCount());
        assertEquals(Collections.singletonList("item"), result.getContent());
    }
}
