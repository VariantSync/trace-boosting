package de.hub.mse.variantsync.ecco.data.position;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents the root position in a tree.
 */
public class RootPosition extends Position {
    private final String value = "ROOT";
    public static final RootPosition INSTANCE = new RootPosition();

    private RootPosition() {
    }

    /**
     * Checks if the given object is equal to this RootPosition instance.
     * 
     * @param o The object to compare with this RootPosition instance.
     * @return true if the given object is an instance of RootPosition, false
     *         otherwise.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        return o instanceof RootPosition;
    }

    /**
     * Returns the serialized representation of the root position.
     * 
     * @return An array containing the value of the root position.
     */
    @Override
    public String[] serializedPosition() {
        return new String[] { value };
    }

    /**
     * Returns the file path associated with the root position.
     * 
     * @return null, as the root position does not have a file path.
     */
    @Override
    public Path filePath() {
        return null;
    }

    /**
     * Returns the line number associated with the root position.
     * 
     * @return -1, as the root position does not have a line number.
     */
    @Override
    public int lineNumber() {
        return -1;
    }

    /**
     * Returns the column number associated with the root position.
     * 
     * @return -1, as the root position does not have a column number.
     */
    @Override
    public int columnNumber() {
        return -1;
    }

    /**
     * Returns the hash code value for the root position.
     * 
     * @return The hash code value based on the value of the root position.
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
