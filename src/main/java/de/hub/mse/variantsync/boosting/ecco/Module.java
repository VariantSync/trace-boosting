package de.hub.mse.variantsync.boosting.ecco;

import org.logicng.formulas.Literal;

import java.util.Objects;

public class Module {
    private final EccoSet<Literal> literals;

    public Module(final EccoSet<Literal> literals) {
        this.literals = literals;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Module))
            return false;
        final Module module = (Module) o;
        return Objects.equals(literals, module.literals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(literals);
    }

    public EccoSet<Literal> getLiterals() {
        return literals;
    }

    public int size() {
        return literals.size();
    }
}
