package org.variantsync.boosting.datastructure;

import org.logicng.formulas.Literal;

import java.util.Objects;

/**
 * A class representing a module in ECCO.
 * 
 * This class encapsulates a set of literals and provides methods for comparing
 * modules and retrieving information about the literals it contains.
 */
public class Module {
    private final CustomHashSet<Literal> literals;

    /**
     * Constructs a new Module with the given set of literals.
     * 
     * @param literals The set of literals to be contained in the module
     */
    public Module(final CustomHashSet<Literal> literals) {
        this.literals = literals;
    }

    /**
     * Compares this Module with the specified object for equality.
     * 
     * @param o The object to compare this Module with
     * @return true if the specified object is equal to this Module, false otherwise
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Module))
            return false;
        final Module module = (Module) o;
        return Objects.equals(literals, module.literals);
    }

    /**
     * Returns a hash code value for the Module.
     * 
     * @return A hash code value for this Module
     */
    @Override
    public int hashCode() {
        return Objects.hash(literals);
    }

    /**
     * Returns the set of literals contained in this Module.
     * 
     * @return The set of literals contained in this Module
     */
    public CustomHashSet<Literal> getLiterals() {
        return literals;
    }

    /**
     * Returns the number of literals in this Module.
     * 
     * @return The number of literals in this Module
     */
    public int size() {
        return literals.size();
    }
}
