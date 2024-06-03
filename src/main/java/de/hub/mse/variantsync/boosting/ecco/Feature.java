package de.hub.mse.variantsync.boosting.ecco;

import java.io.Serializable;
import java.util.Objects;

public class Feature implements Comparable<Feature>, Serializable {

    private final String name;

    public Feature(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final Feature feature = (Feature) o;
        return name.equals(feature.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public int compareTo(final Feature other) {
        return name.compareTo(other.name);
    }
}
