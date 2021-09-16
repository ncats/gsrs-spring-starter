package ix.core.initializers;

import org.springframework.boot.ApplicationArguments;

public abstract class Initializer {
    
    public abstract void onStart(ApplicationArguments args);
}
