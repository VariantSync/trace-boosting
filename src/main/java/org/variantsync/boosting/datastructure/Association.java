package org.variantsync.boosting.datastructure;

import org.logicng.formulas.Formula;

import java.util.*;

/**
 * Represents an association between modules and AST nodes.
 * An association consists of the following components:
 * - A set of AST nodes (astNodes) representing the code elements involved in
 * the association
 * - Three sets of modules (min, all, max) representing the minimum, all, and
 * maximum products in which the association appears
 * - A set of modules (not) representing the products in which the association
 * does not appear
 * - A boolean flag (isBasic) indicating whether the association appears in all
 * products
 * - A formula (mapping) representing the mapping between the modules and AST
 * nodes
 * 
 * This class provides methods for accessing and modifying the components of the
 * association, such as:
 * - Getting and setting the AST nodes, modules, and mapping
 * - Removing specific AST nodes from the association
 * - Getting the smallest modules in the association based on their size
 * - Checking if the association is basic
 * 
 * The equals and hashCode methods are overridden to ensure proper comparison of
 * association objects based on their components.
 */
public class Association {
    private CustomHashSet<ASTNode> astNodes;
    private CustomHashSet<Module> min;
    private CustomHashSet<Module> all;
    private CustomHashSet<Module> max;
    private CustomHashSet<Module> not;
    private Formula mapping;
    // isBasic tells whether the code of the association appears in all products
    private boolean isBasic;

    /**
     * Constructs a new Association with the specified sets of modules and AST
     * nodes.
     * 
     * @param min      the minimum set of modules
     * @param all      the set of all modules
     * @param max      the maximum set of modules
     * @param not      the set of modules that are not in the association
     * @param astNodes the set of AST nodes associated with the modules
     */
    public Association(final CustomHashSet<Module> min, final CustomHashSet<Module> all, final CustomHashSet<Module> max,
                       final CustomHashSet<Module> not, final CustomHashSet<ASTNode> astNodes) {
        this.min = new CustomHashSet<>(min);
        this.all = new CustomHashSet<>(all);
        this.max = new CustomHashSet<>(max);
        this.not = new CustomHashSet<>(not);
        this.astNodes = astNodes == null ? new CustomHashSet<>() : astNodes;
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

    /**
     * Returns the set of AST nodes contained in this EccoSet.
     *
     * @return a set of ASTNode objects representing the AST
     *         nodes
     */
    public CustomHashSet<ASTNode> getAstNodes() {
        return astNodes;
    }

    /**
     * Removes the specified nodes from the current set of AST nodes.
     * 
     * @param toRemove The set of nodes to be removed from the current set of AST
     *                 nodes.
     * @throws NullPointerException if the specified set of nodes to be removed is
     *                              null.
     */
    public void removeNodes(final CustomHashSet<ASTNode> toRemove) {
        if (toRemove == null) {
            throw new NullPointerException("The specified set of nodes to be removed cannot be null.");
        }

        astNodes = astNodes.without(toRemove);
    }

    /**
     * Returns the feature mapping formula associated with this object.
     * 
     * @return the mapping formula
     */
    public Formula getMapping() {
        return mapping;
    }

    /**
     * Sets the feature mapping for the association.
     * 
     * @param mapping the formula to set as the mapping
     */
    public void setMapping(final Formula mapping) {
        this.mapping = mapping;
    }

    /**
     * Returns the set of min modules.
     * 
     * @return the min modules
     */
    public CustomHashSet<Module> getMin() {
        return min;
    }

    /**
     * Returns the set off all modules.
     * 
     * @return the all modules
     */
    public CustomHashSet<Module> getAll() {
        return all;
    }

    /**
     * Returns the set of max module.
     * 
     * @return the max modules
     */
    public CustomHashSet<Module> getMax() {
        return max;
    }

    /**
     * Returns the set of not modules.
     * 
     * @return the not modules
     */
    public CustomHashSet<Module> getNot() {
        return not;
    }

    /**
     * Returns a list of the min modules with the least literals.
     * 
     * @return The list of smallest min modules
     */
    public List<Module> getSmallestMinModules() {
        return getSmallestModules(this.min);
    }

    /**
     * Returns a list of the max modules with the least literals.
     * 
     * @return The list of smallest max modules
     */
    public List<Module> getSmallestMaxModules() {
        return getSmallestModules(this.max);
    }

    /**
     * Returns a list of the modules with the least literals in the given set of modules.
     *
     * @param modules the EccoSet of modules to search for the smallest ones
     * @return a list of the smallest modules from the given set
     */
    private List<Module> getSmallestModules(final CustomHashSet<Module> modules) {
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

    /**
     * Sets the min modules.
     * 
     * @param min a set of min modules
     */
    public void setMin(final CustomHashSet<Module> min) {
        this.min = min;
    }

    /**
     * Sets the all modules.
     * 
     * @param all a set of all modules
     */
    public void setAll(final CustomHashSet<Module> all) {
        this.all = all;
    }

    /**
     * Sets the max modules.
     * 
     * @param max a set of max modules
     */
    public void setMax(final CustomHashSet<Module> max) {
        this.max = max;
    }

    /**
     * Sets the not modules.
     * 
     * @param not a set of not modules
     */
    public void setNot(final CustomHashSet<Module> not) {
        this.not = not;
    }

    /**
     * Define this association as tracking base code.
     */
    public void setBasic(final Boolean basicCode) {
        this.isBasic = basicCode;
    }

    /**
     * Returns whether the association tracks base code.
     * 
     */
    public boolean isBasic() {
        return isBasic;
    }

}
