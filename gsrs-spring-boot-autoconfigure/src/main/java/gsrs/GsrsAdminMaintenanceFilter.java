package gsrs;

import gsrs.events.MaintenanceModeEvent;
import gsrs.security.GsrsSecurityUtils;
import gsrs.services.PrivilegeService;
import ix.core.models.Role;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class GsrsAdminMaintenanceFilter implements Filter {

    //users who have any of these privs will be allowed on the system even when it's undergoing maintenance
    private final List<String> PRIVS_ALLOWED_ON_ALWAYS = Arrays.asList("Configure System", "Index Data");
    private boolean maintenanceMode =false;
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(maintenanceMode && PRIVS_ALLOWED_ON_ALWAYS.stream().noneMatch(p->PrivilegeService.instance().canDo(p) )){
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is undergoing maintenance. Please try again later.");
        }
    }

    @EventListener
    public void onMaintenanceMode(MaintenanceModeEvent event){
        maintenanceMode = event.getSource().isInMaintenanceMode();
    }
}
