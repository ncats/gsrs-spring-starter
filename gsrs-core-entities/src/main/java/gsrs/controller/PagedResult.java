package gsrs.controller;

import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {
    private long total, count, skip, top;
    private Object content;

    public static PagedResult<EntityUtils.Key> ofKeys(Page<?> page) {
        return PagedResult.<EntityUtils.Key>builder()
                .total(page.getTotalElements())
                .count(page.getNumberOfElements())
                .skip(page.getSize() * page.getNumber())
                .top(page.getSize())
                .content(page.toList().stream().map(e -> EntityUtils.EntityWrapper.of(e).getKey()).collect(Collectors.toList()))
                .build();

    }

    public PagedResult(List<T> fullList, long top, long skip, Map<String, String> queryParameters) {
        this.total = fullList.size();
        List<T> subList = fullList.subList((int) Math.min(total, skip), (int) Math.min(total, skip + top));
        this.content = subList.stream().map(e -> GsrsControllerUtil.enhanceWithView(e, queryParameters)).collect(Collectors.toList());
        this.count = subList.size();
        this.top = top;
        this.skip = skip;
    }

    public PagedResult(Page<T> page, Map<String, String> queryParameters) {
        this.total = page.getTotalElements();
        this.count = page.getNumberOfElements();
        this.skip = page.getSize() * page.getNumber();
        this.top = page.getSize();
        content = page.toList().stream().map(e -> GsrsControllerUtil.enhanceWithView(e, queryParameters)).collect(Collectors.toList());


    }
}
