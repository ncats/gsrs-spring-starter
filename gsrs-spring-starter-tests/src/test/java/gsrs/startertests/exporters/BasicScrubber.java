package gsrs.startertests.exporters;

import ix.ginas.exporters.RecordScrubber;
import ix.ginas.models.GinasCommonData;

import java.util.Optional;
import java.util.UUID;

public class BasicScrubber implements RecordScrubber<GinasCommonData> {
    @Override
    public Optional<GinasCommonData> scrub(GinasCommonData object) {
        GinasCommonData newObject = new GinasCommonData();
        newObject.setUuid(UUID.randomUUID());
        newObject.setAccess(object.getAccess());
        newObject.setCreated(object.getCreated());
        newObject.setLastEdited(object.getLastEdited());
        newObject.setRecordAccess(object.getRecordAccess());

        newObject.setIsAllDirty();
        return Optional.of(newObject);
    }
}
