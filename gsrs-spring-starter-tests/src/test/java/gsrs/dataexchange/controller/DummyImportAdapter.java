package gsrs.dataexchange.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Return;
import gsrs.imports.ImportAdapter;
import ix.ginas.models.GinasCommonData;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

public class DummyImportAdapter implements ImportAdapter<GinasCommonData> {
    @Override
    public Stream<GinasCommonData> parse(InputStream is, ObjectNode settings) {
        GinasCommonData ginasCommonData = new GinasCommonData();
        ginasCommonData.setUuid(UUID.randomUUID());
        ginasCommonData.setCreated(new Date());
        Stream.Builder<GinasCommonData> builder= Stream.builder();
        builder.add(ginasCommonData);

        GinasCommonData ginasCommonData2 = new GinasCommonData();
        ginasCommonData2.setUuid(UUID.randomUUID());
        ginasCommonData2.setCreated(new Date());
        builder.add(ginasCommonData2);
        return builder.build();
    }

    public static int getExpectedStreamSize(){
        return 2;
    }

}
