package org.variantsync.boosting.datastructure;

import java.util.Collection;
import java.util.HashSet;

/**
 * A custom set implementation to handle sets similar to how they are defined by
 * ECCO.
 */
public class EccoSet<E> extends HashSet<E> {

    /**
     * Constructs a new EccoSet with no elements.
     */
    public EccoSet() {
        super();
    }

    /**
     * Constructs a new EccoSet with the elements from the specified collection.
     *
     * @param elements the collection of elements to initialize the set with
     * @throws NullPointerException if the specified collection is null
     */
    public EccoSet(final Collection<E> elements) {
        super(elements);
    }

    /**
     * Replaces an equivalent element in the set with a new version of it
     */
    public void overwrite(final E element) {
        this.remove(element);
        this.add(element);
    }

    /**
     * Returns a new set that is the union of this set and the given set
     *
     * @param toUnite The set to form the union with
     * @return A new set that contains the union of elements from this set and the
     *         given set
     */
    public EccoSet<E> unite(final EccoSet<E> toUnite) {
        final EccoSet<E> result = new EccoSet<>(this);
        result.addAll(toUnite);
        return result;
    }

    /**
     * Returns a new set that is the union of this set and the given set
     *
     * @param toUnite The set to form the union with
     * @return A new set that contains the union of elements from this set and the
     *         given set
     */
    public EccoSet<E> uniteElement(final E toUnite) {
        final EccoSet<E> result = new EccoSet<>(this);
        result.add(toUnite);
        return result;
    }

    /**
     * Returns a new set that contains all elements in this set after removing the
     * elements in the given set
     *
     * @param without The elements that should not be in the new set
     * @return New set with elements that are only in this set but not in the given
     *         set
     */
    public EccoSet<E> without(final EccoSet<E> without) {
        final EccoSet<E> result = new EccoSet<>(this);
        result.removeAll(without);
        return result;
    }

    /**
     * Returns a new set that contains all elements in this set after removing the
     * elements in the given set
     *
     * @param without The elements that should not be in the new set
     * @return New set with elements that are only in this set but not in the given
     *         set
     */
    public EccoSet<E> withoutElement(final E without) {
        final EccoSet<E> result = new EccoSet<>(this);
        result.remove(without);
        return result;
    }

    /**
     * Returns a new set that is an intersection between this set and the given set
     *
     * @param toIntersect set to intersect with
     * @return a new set with the elements that represent the intersection of both
     *         sets
     */
    public EccoSet<E> intersect(final EccoSet<E> toIntersect) {
        final EccoSet<E> result = new EccoSet<>(this);
        result.retainAll(toIntersect);
        return result;
    }

    /**
     * Returns a new set that is an intersection between this set and the given set
     *
     * @param toIntersect set to intersect with
     * @return a new set with the elements that represent the intersection of both
     *         sets
     */
    public EccoSet<E> intersectElement(final E toIntersect) {
        final EccoSet<E> result = new EccoSet<>(this);
        if (this.contains(toIntersect)) {
            result.add(toIntersect);
        }
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    /**
     * Returns the power set of this set.
     */
    public EccoSet<EccoSet<E>> powerSet() {
        return powerSet(this);
    }

    private EccoSet<EccoSet<E>> powerSet(final EccoSet<E> input) {
        final EccoSet<EccoSet<E>> result = new EccoSet<>();
        result.add(input);
        for (final E element : input) {
            result.addAll(powerSet(input.withoutElement(element)));
        }
        return result;
    }
}
