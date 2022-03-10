package gsrs.controller;

import gsrs.dataExchange.model.ImportFieldHandling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.util.Collection;

public abstract class AbstractImportSupportingGsrsEntityController<C extends AbstractImportSupportingGsrsEntityController, T, I >
        extends AbstractGsrsEntityController<C, T, I> {

    @Autowired
    private PlatformTransactionManager transactionManager;

    /*
    Parse a file to create Entities, persist those Entities and return the associated IDs
     */
    abstract public Collection<String> extractAndPersistDataFromFile(File dataFile, Collection<ImportFieldHandling> rules,
                                                   ImportFieldHandling handling);

    /*
    Return the set of fields for the file
    (for example, those fields inside field name delimiters for an SD file or the first line of a delimited text file
     */
    abstract public Collection<String> getFields(File dataFile, ImportFieldHandling handling);


    /*
    Save the file in a temporary area and return its ID
     */
    abstract public String persistDataFile(File dataFile);
}
