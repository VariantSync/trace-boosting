package org.variantsync.boosting.position;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a position in a file with line and column numbers.
 */
public class LinePosition extends Position {
    private final String filePosition;
    private final int lineNumber;
    private final int columnNumber;

    /**
     * Constructs a LinePosition object with the specified file position, line
     * number, and column number.
     * 
     * @param filePosition the file position
     * @param lineNumber   the line number
     * @param columnNumber the column number
     */
    public LinePosition(final String filePosition, final int lineNumber, final int columnNumber) {
        this.filePosition = filePosition;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Compares this LinePosition object to the specified object for equality.
     * 
     * @param o the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LinePosition))
            return false;
        final LinePosition that = (LinePosition) o;
        return lineNumber == that.lineNumber && columnNumber == that.columnNumber
                && Objects.equals(filePosition, that.filePosition);
    }

    /**
     * Returns a hash code value for the LinePosition object.
     * 
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(filePosition, lineNumber, columnNumber);
    }

    /**
     * Returns a serialized representation of the LinePosition object.
     * 
     * @return an array of strings representing the serialized position
     */
    @Override
    public String[] serializedPosition() {
        return new String[] { "LINE", filePosition, String.valueOf(lineNumber), String.valueOf(columnNumber) };
    }

    /**
     * Returns the file path of the file that contains the line.
     * 
     * @return the file path
     */
    @Override
    public Path filePath() {
        return Path.of(filePosition);
    }

    /**
     * Returns the line number of the line.
     * 
     * @return the line number
     */
    @Override
    public int lineNumber() {
        return lineNumber;
    }

    /**
     * Returns the column number of the line.
     * 
     * @return the column number
     */
    @Override
    public int columnNumber() {
        return columnNumber;
    }
}
