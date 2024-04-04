package gsrs.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class UserProfileAuthenticationResult {
    private boolean matchesRepository;

    public boolean matchesRepository() {
        return matchesRepository;
    }

    public void setMatchesRepository(boolean matchesRepository) {
        this.matchesRepository = matchesRepository;
    }

    public boolean needsSave() {
        return needsSave;
    }

    public void setNeedsSave(boolean needsSave) {
        this.needsSave = needsSave;
    }

    private boolean needsSave;
}
