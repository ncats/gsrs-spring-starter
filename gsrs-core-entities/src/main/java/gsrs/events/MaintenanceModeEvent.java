package gsrs.events;

import org.springframework.context.ApplicationEvent;

public class MaintenanceModeEvent extends ApplicationEvent {
    public enum Mode{
        BEGIN(true),END(false);

        private boolean inMaintenanceMode;
        private Mode(boolean b){
            inMaintenanceMode = b;
        }

        public boolean isInMaintenanceMode(){
            return inMaintenanceMode;
        }
    }
    public MaintenanceModeEvent(Mode mode) {
        super(mode);
    }

    @Override
    public Mode getSource() {
        return (Mode) super.getSource();
    }
}
