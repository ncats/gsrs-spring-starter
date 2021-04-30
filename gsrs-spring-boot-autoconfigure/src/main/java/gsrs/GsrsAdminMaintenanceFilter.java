package gsrs;

import gsrs.events.MaintenanceModeEvent;
import gsrs.security.GsrsSecurityUtils;
import ix.core.models.Role;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class GsrsAdminMaintenanceFilter implements Filter {

    private boolean maintenanceMode =false;
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(maintenanceMode && !GsrsSecurityUtils.hasAnyRoles(Role.Admin)){
            ((HttpServletResponse) servletResponse).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is undergoing maintenance. Please try again later.");
        }
    }

    @EventListener
    public void onMaintenanceMode(MaintenanceModeEvent event){
        maintenanceMode = event.getSource().isInMaintenanceMode();
    }
}
