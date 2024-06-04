package org.variantsync.boosting.datastructure;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a feature with a name.
 * 
 * This class implements the Comparable interface to allow for comparison of
 * Feature objects based on their names.
 */
public class Feature implements Comparable<Feature>, Serializable {

    private final String name;

    /**
     * Constructs a new Feature object with the given name.
     * 
     * @param name the name of the feature
     */
    public Feature(final String name) {
        this.name = name;
    }

    /**
     * Returns the name of the feature.
     * 
     * @return the name of the feature
     */
    public String getName() {
        return name;
    }

    /**
     * Compares this Feature object with the specified object for equality.
     * 
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final Feature feature = (Feature) o;
        return name.equals(feature.name);
    }

    /**
     * Returns a hash code value for the Feature object.
     * 
     * @return the hash code value for the object
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * Compares this Feature object with the specified Feature object for order.
     * 
     * @param other the Feature object to compare with
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object
     */
    public int compareTo(final Feature other) {
        return name.compareTo(other.name);
    }
}
