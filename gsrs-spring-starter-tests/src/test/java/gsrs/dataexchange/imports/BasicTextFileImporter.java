package gsrs.dataexchange.imports;

import gsrs.dataexchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import ix.ginas.models.GinasCommonData;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class BasicTextFileImporter implements ImportAdapter<GinasCommonData> {

    private final String DELIM = ",";

    private List<MappingAction<GinasCommonData, TextRecordContext>> actions;

    public BasicTextFileImporter(List<MappingAction<GinasCommonData, TextRecordContext>> actions) {
        this.actions = actions;
    }

    @Override
    public Stream<GinasCommonData> parse(InputStream is, String encoding) {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(isr);
        if (!(actions.size()==1 && actions.get(0).getClass().getName().contains("BasicMappingAction"))){
            log.trace("using specific actions");
            Stream<GinasCommonData> objects = bufferedReader.lines()
                    .map(line -> {
                        if (line != null && line.length() > 0 && line.split(DELIM).length == 2 && line.split(DELIM)[0] != null
                                && line.split(DELIM)[0].length() > 0 && line.split(DELIM)[1] != null && line.split(DELIM)[0].length() > 1) {
                            final GinasCommonData[] object = {new GinasCommonData()};
                            object[0] = new GinasCommonData();
                            TextRecordContext record = new TextRecordContext(line, DELIM);
                            actions.stream().forEach(a -> {
                                log.trace("running action " + a.getClass().getName());
                                try {
                                    object[0] = a.act(object[0], record);
                                } catch (Exception e) {
                                     log.error("error parsing actions", e);
                                    //todo: handle exception better... rethrow?
                                }
                            });
                            return object[0];
                        }
                        return null;
                    })
                    .filter(o -> o != null);
            return objects;
        } else {
            Stream<GinasCommonData> objects = bufferedReader.lines()
                    .map(line -> {
                        if (line != null && line.length() > 0 && line.split(DELIM).length == 2 && line.split(DELIM)[0] != null
                                && line.split(DELIM)[0].length() > 0 && line.split(DELIM)[1] != null && line.split(DELIM)[0].length() > 1) {
                            GinasCommonData object = new GinasCommonData();
                            object.addMatchContextProperty("Item1", line.split(DELIM)[0]);
                            object.addMatchContextProperty("Item2", line.split(DELIM)[1]);
                            return object;
                        }
                        return null;
                    })
                    .filter(o -> o != null);
            return objects;
        }
    }

}
