package org.variantsync.boosting.position;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents the location of a file.
 */
public class FilePosition extends Position {
    private final String path;

    /**
     * Constructs a new FilePosition object with the specified file path.
     * 
     * @param path The path of the file
     */
    public FilePosition(final String path) {
        this.path = path;
    }

    /**
     * Compares this FilePosition object with the specified object for equality.
     * 
     * @param o The object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FilePosition))
            return false;
        final FilePosition that = (FilePosition) o;
        return Objects.equals(path, that.path);
    }

    /**
     * Returns a serialized representation of the FilePosition object.
     * 
     * @return An array containing the type "FILE" and the file path
     */
    @Override
    public String[] serializedPosition() {
        return new String[] { "FILE", path };
    }

    /**
     * Returns the file path as a Path object.
     * 
     * @return The file path as a Path object
     */
    @Override
    public Path filePath() {
        return Path.of(path);
    }

    /**
     * Returns the line number associated with the FilePosition object.
     * 
     * @return -1 as line number is not applicable for file positions
     */
    @Override
    public int lineNumber() {
        return -1;
    }

    /**
     * Returns the column number associated with the FilePosition object.
     * 
     * @return -1 as column number is not applicable for file positions
     */
    @Override
    public int columnNumber() {
        return -1;
    }

    /**
     * Returns the hash code value for the FilePosition object.
     * 
     * @return The hash code value for the object based on the file path
     */
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
