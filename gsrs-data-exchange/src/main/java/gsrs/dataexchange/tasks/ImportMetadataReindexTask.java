package gsrs.dataexchange.tasks;

import gsrs.dataexchange.services.ImportMetadataReindexer;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

public class ImportMetadataReindexTask extends ScheduledTaskInitializer {

    @Autowired
    ImportMetadataReindexer reindexer;

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        UUID uuid = UUID.randomUUID();
        try {
            reindexer.execute(uuid, l);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return "Reindex data and metadata in the Staging Area for fast and accurate searches";
    }
}
