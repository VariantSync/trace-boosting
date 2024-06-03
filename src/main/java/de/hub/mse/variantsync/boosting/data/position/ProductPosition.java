package de.hub.mse.variantsync.boosting.data.position;

import de.hub.mse.variantsync.boosting.data.Product;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a mapping between a Product and a Position.
 * 
 * @param product  The Product object associated with this ProductPosition.
 * @param position The Position object associated with this ProductPosition.
 */
public class ProductPosition implements Serializable {
    public final Product product;
    public final Position position;

    /**
     * Constructs a new ProductPosition object with the given Product and Position.
     * 
     * @param product  The Product object to associate with this ProductPosition.
     * @param position The Position object to associate with this ProductPosition.
     */
    public ProductPosition(final Product product, final Position position) {
        this.product = product;
        this.position = position;
    }

    /**
     * Checks if this ProductPosition is equal to another object.
     * 
     * @param o The object to compare with this ProductPosition.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ProductPosition))
            return false;
        final ProductPosition that = (ProductPosition) o;
        return Objects.equals(product, that.product) && Objects.equals(position, that.position);
    }

    /**
     * Generates a hash code for this ProductPosition.
     * 
     * @return The hash code for this ProductPosition.
     */
    @Override
    public int hashCode() {
        return Objects.hash(product, position);
    }

    /**
     * Retrieves the file path associated with the Position of this ProductPosition.
     * 
     * @return The file path of the Position.
     */
    public Path filePath() {
        return position.filePath();
    }

    /**
     * Retrieves the line number associated with the Position of this
     * ProductPosition.
     * 
     * @return The line number of the Position.
     */
    public int lineNumber() {
        return position.lineNumber();
    }

    /**
     * Retrieves the column number associated with the Position of this
     * ProductPosition.
     * 
     * @return The column number of the Position.
     */
    public int columnNumber() {
        return position.columnNumber();
    }
}
