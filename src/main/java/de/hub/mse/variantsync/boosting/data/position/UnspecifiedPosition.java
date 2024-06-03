package de.hub.mse.variantsync.boosting.data.position;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents an unspecified position.
 */
public class UnspecifiedPosition extends Position {
    private final String value = "UNSPECIFIED";
    public static final UnspecifiedPosition INSTANCE = new UnspecifiedPosition();

    private UnspecifiedPosition() {
    }

    /**
     * Checks if this UnspecifiedPosition is equal to another object.
     * 
     * @param o The object to compare with this UnspecifiedPosition
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        return o instanceof UnspecifiedPosition;
    }

    /**
     * Generates a hash code for this UnspecifiedPosition.
     * 
     * @return The hash code value for this UnspecifiedPosition
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Serializes the position into an array of strings.
     * 
     * @return An array containing the value of this UnspecifiedPosition
     */
    @Override
    public String[] serializedPosition() {
        return new String[] { value };
    }

    /**
     * Gets the file path associated with this UnspecifiedPosition.
     * 
     * @return null, as an unspecified position does not have a file path
     */
    @Override
    public Path filePath() {
        return null;
    }

    /**
     * Gets the line number associated with this UnspecifiedPosition.
     * 
     * @return -1, as an unspecified position does not have a line number
     */
    @Override
    public int lineNumber() {
        return -1;
    }

    /**
     * Gets the column number associated with this UnspecifiedPosition.
     * 
     * @return -1, as an unspecified position does not have a column number
     */
    @Override
    public int columnNumber() {
        return -1;
    }
}
