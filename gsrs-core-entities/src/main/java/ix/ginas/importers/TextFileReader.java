package ix.ginas.importers;

import gsrs.importer.DefaultPropertyBasedRecordContext;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TextFileReader {

    public Stream<DefaultPropertyBasedRecordContext> readFile(InputStream inputStream, String delim, boolean trimQuotes, List<String> inputFields) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        List<String> fields;
        if(inputFields !=null && inputFields.size() >0){
            fields = new ArrayList<>(inputFields);
        }else {
            fields=Arrays.stream(br.readLine().split(delim)).map(s -> trimQuotes ? removeQuotes(s) : s).collect(Collectors.toList());
        }
        String line;
        Stream.Builder<DefaultPropertyBasedRecordContext> builder=Stream.builder();
        while ((line = br.readLine()) != null) {
            List<String> values = Arrays.stream(line.split(delim)).collect(Collectors.toList());
            DefaultPropertyBasedRecordContext record = new DefaultPropertyBasedRecordContext();
            Map<String, String> lineValues = new HashMap<>();
            for(int i =0; i< fields.size(); i++) {
                try {
                    String currentValue=values.get(i);
                    if( currentValue !=null && trimQuotes ){
                        currentValue=removeQuotes(currentValue);
                    }
                    lineValues.put(fields.get(i), currentValue);
                }
                catch (IndexOutOfBoundsException e) {
                    log.trace("error reading field '{}' from line '{}'", fields.get(i), line);
                }
            }
            record.setAllProperties(lineValues);
            builder.add(record);
        }
        br.close();
        return builder.build();
    }

    public List<String> getFileFields(InputStream inputStream, String delim, boolean trimQuotes) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        return Arrays.stream(br.readLine().split(delim)).map(s->trimQuotes ? removeQuotes(s): s).collect(Collectors.toList());
    }

    public Map<String, InputFieldStatistics> getFileStatistics(InputStream inputStream, String delim, boolean trimQuotes, List<String> fieldNames,
                                                               int maxRecords, int linesToSkip)  {
        log.trace("In getFileStatistics, delim: {}; trimQuotes: {}", delim, trimQuotes);
        Map<String, InputFieldStatistics> retMap = new LinkedHashMap<>();
        Scanner reader = new Scanner(inputStream);

        List<String> fields = new ArrayList<>();
        if(fieldNames != null && fieldNames.size()>0){
            log.trace("getFileStatistics received input fieldNames");
            fields.addAll(fieldNames);
        } else {
            log.trace("getFileStatistics reading fields from file");
            fields = Arrays.stream(reader.nextLine().split(delim)).map(s->trimQuotes ? removeQuotes(s)
                    : s).collect(Collectors.toList());
            log.trace(" got {}", fields.size());
        }

        for(int lineToSkip=0; lineToSkip< linesToSkip; lineToSkip++) {
            reader.nextLine();
        }
        for(int lineToRead=0; (lineToRead<maxRecords && reader.hasNext()); lineToRead++) {
            String currentLine =reader.nextLine();
            if( currentLine == null || currentLine.length()==0) continue;
            List<String> values = Arrays.stream(currentLine.split(delim)).collect(Collectors.toList());
            for(int f=0; f< fields.size(); f++) {
                String fieldName = fields.get(f);
                String value = values.get(f);
                InputFieldStatistics fs = retMap.computeIfAbsent(fieldName, k -> new InputFieldStatistics(k, maxRecords));
                fs.add(value.trim());
            }
        }
        return retMap;
    }

    public Stream<DefaultPropertyBasedRecordContext> readFile2(InputStream inputStream, String delim, boolean trimQuotes,
                                                               List<String> inputFields, int linesToSkip) {
        Scanner reader = new Scanner(inputStream);
        reader.reset();

        List<String> fields;
        if(inputFields !=null && inputFields.size() >0){
            fields = new ArrayList<>(inputFields);
        }else {
            fields=Arrays.stream(reader.nextLine().split(delim)).map(s -> trimQuotes ?  removeQuotes(s) : s).collect(Collectors.toList());
        }
        for( int i =0; i<linesToSkip; i++) reader.nextLine();

        String line;
        Stream.Builder<DefaultPropertyBasedRecordContext> builder=Stream.builder();
        while (reader.hasNext()) {
            line = reader.nextLine();
            List<String> values = Arrays.stream(line.split(delim)).collect(Collectors.toList());
            DefaultPropertyBasedRecordContext record = new DefaultPropertyBasedRecordContext();
            Map<String, String> lineValues = new HashMap<>();
            for(int i =0; i< fields.size(); i++) {
                try {
                    String currentValue=values.get(i);
                    if( currentValue!=null && trimQuotes && currentValue.length()>2) {
                        currentValue = removeQuotes(currentValue);
                    }
                    lineValues.put(fields.get(i), currentValue);
                }
                catch (IndexOutOfBoundsException e) {
                    log.trace("error reading field '{}' from line '{}'", fields.get(i), line);
                }
            }
            record.setAllProperties(lineValues);
            builder.add(record);
        }
        return builder.build();
    }

    public static String removeQuotes(String input) {
        String output = input;
        if(isQuote(output.charAt(0)) ) {
            output = output.substring(1);
        }
        if(isQuote(output.charAt(output.length()-1))) {
            output = output.substring(0, output.length()-1);
        }
        return output;
    }

    public static boolean isQuote(char testChar) {
        List<Character> quoteChars = Arrays.asList('"', '\'');
        return quoteChars.contains(testChar);
    }

}
