package gsrs.sequence;


import ix.core.models.SequenceEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

/**
 * An interface that the given entity
 * can have attached Sequence format files.
 */
public interface SequenceFileSupport {
    /**
     * An Object containing information about an attached Sequence File
     * and a mechanism to get that data.
     */
    interface SequenceFileData{
        /**
         * The types of supported sequence files.
         */
        enum SequenceFileType{
            /**
             * A FASTA file of either DNA, RNA or protein sequences.
             */
            FASTA,
            //TODO add more formats if needed (fastq, etc)
            ;
        }

        /**
         * Get the {@link ix.core.models.SequenceEntity.SequenceType}.
         * @return
         */
        SequenceEntity.SequenceType getSequenceType();

        /**
         * Get the sequence file type which is needed to know
         * how to interpret the data returned by {@link #createInputStream()}.
         * @return
         */
        SequenceFileType getSequenceFileType();

        /**
         * Get the file data of this sequence file as an InputStream.
         * @return a new InputStream.
         * @throws IOException if there is a problem opening the file.
         */
        InputStream createInputStream() throws IOException;

        /**
         * Get the file name of this sequence file.
         * @return the name of the file.
         */
        String getName();
    }

    /**
     * Does this entity have any attached sequence files.
     * From an API perspective this should usually be the same but more efficient
     * than checking if {@link #getSequenceFileData()} returns a non-empty Stream.
     *
     * @return {@code true} if there are attached sequence files;
     * {@code false} otherwise.  This method does not actually check
     * that the contents of these files, so while unlikely, it is possible that
     * there are attached sequence files that contain no sequence data.
     */
    boolean hasSequenceFiles();

    /**
     * Get the SequenceFileData for the attached sequence files.
     * @return
     */
    Stream<SequenceFileData> getSequenceFileData();
}
