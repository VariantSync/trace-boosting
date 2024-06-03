package de.hub.mse.variantsync.boosting.data;

import org.logicng.formulas.Formula;

import java.util.*;

public class Association {
    private EccoSet<EccoNode> astNodes;
    private EccoSet<Module> min;
    private EccoSet<Module> all;
    private EccoSet<Module> max;
    private EccoSet<Module> not;
    private Formula mapping;
    // isBasic tells whether the code of the association appears in all products
    private boolean isBasic;

    public Association(final EccoSet<Module> min, final EccoSet<Module> all, final EccoSet<Module> max,
            final EccoSet<Module> not, final EccoSet<EccoNode> astNodes) {
        this.min = new EccoSet<>(min);
        this.all = new EccoSet<>(all);
        this.max = new EccoSet<>(max);
        this.not = new EccoSet<>(not);
        this.astNodes = astNodes == null ? new EccoSet<>() : astNodes;
        this.isBasic = true;
        this.mapping = null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final Association that = (Association) o;
        return Objects.equals(min, that.min) &&
                Objects.equals(all, that.all) &&
                Objects.equals(max, that.max) &&
                Objects.equals(not, that.not) &&
                Objects.equals(astNodes, that.astNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(astNodes, min, all, max, not);
    }

    public EccoSet<EccoNode> getAstNodes() {
        return astNodes;
    }

    public void removeNodes(final EccoSet<EccoNode> toRemove) {
        astNodes = astNodes.without(toRemove);
    }

    public Formula getMapping() {
        return mapping;
    }

    public void setMapping(final Formula mapping) {
        this.mapping = mapping;
    }

    public EccoSet<Module> getMin() {
        return min;
    }

    public EccoSet<Module> getAll() {
        return all;
    }

    public EccoSet<Module> getMax() {
        return max;
    }

    public EccoSet<Module> getNot() {
        return not;
    }

    public List<Module> getSmallestMinModules() {
        return getSmallestModules(this.min);
    }

    public List<Module> getSmallestMaxModules() {
        return getSmallestModules(this.max);
    }

    private List<Module> getSmallestModules(final EccoSet<Module> modules) {
        final List<Module> result = new LinkedList<>();
        int size = Integer.MAX_VALUE;
        for (final Module module : modules) {
            if (module.size() < size) {
                // Reset the result list
                result.clear();
                result.add(module);
                size = module.size();
            } else if (module.size() == size) {
                result.add(module);
                size = module.size();
            }
        }
        return result;
    }

    public void setMin(final EccoSet<Module> min) {
        this.min = min;
    }

    public void setAll(final EccoSet<Module> all) {
        this.all = all;
    }

    public void setMax(final EccoSet<Module> max) {
        this.max = max;
    }

    public void setNot(final EccoSet<Module> not) {
        this.not = not;
    }

    public void setBasic(final Boolean basicCode) {
        this.isBasic = basicCode;
    }

    public boolean isBasic() {
        return isBasic;
    }
}
