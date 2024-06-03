package de.hub.mse.variantsync.ecco.data.position;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * Represents a position in a file, line, or root.
 * This abstract class provides methods for creating a Position object from a
 * serialized string,
 * as well as methods for getting the hash code, checking equality, serializing
 * the position,
 * getting the file path, line number, and column number.
 */
public abstract class Position implements Serializable {

    /**
     * Creates a Position object from a serialized string.
     *
     * @param serializedPosition an array of strings representing the serialized
     *                           position
     * @return a Position object based on the serialized string
     * @throws IllegalArgumentException if the type in the serialized string is not
     *                                  recognized
     */
    public static Position fromSerializedPosition(final String[] serializedPosition) {
        final String type = serializedPosition[0];
        switch (type) {
            case "FILE":
                return new FilePosition(serializedPosition[1]);
            case "LINE":
                return new LinePosition(serializedPosition[1], Integer.parseInt(serializedPosition[2]),
                        Integer.parseInt(serializedPosition[3]));
            case "ROOT":
                return RootPosition.INSTANCE;
            case "UNSPECIFIED":
                return UnspecifiedPosition.INSTANCE;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the hash code of the Position object. Must be implemented by
     * subclass.
     *
     * @return the hash code of the Position object
     */
    @Override
    public abstract int hashCode();

    /**
     * Checks if the Position object is equal to another object. Must be implemented
     * by subclass.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * Serializes the Position object into an array of strings.
     *
     * @return an array of strings representing the serialized position
     */
    public abstract String[] serializedPosition();

    /**
     * Returns the file path of the Position object.
     *
     * @return the file path of the Position object
     */
    public abstract Path filePath();

    /**
     * Returns the line number of the Position object.
     *
     * @return the line number of the Position object
     */
    public abstract int lineNumber();

    /**
     * Returns the column number of the Position object.
     *
     * @return the column number of the Position object
     */
    public abstract int columnNumber();
}
