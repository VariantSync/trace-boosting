package de.hub.mse.variantsync.ecco.debugging;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.transformations.simplification.AdvancedSimplifier;
import org.logicng.transformations.simplification.BackboneSimplifier;
import org.logicng.transformations.simplification.DefaultRatingFunction;

public class LogicNG {
    public static void main(final String... args) throws ParserException {
        final FormulaFactory f = new FormulaFactory();
        final Variable a = f.variable("A");
        final Variable b = f.variable("B");
        final Variable c = f.variable("C");
        final Formula formula1 = f.or(f.and(a, f.and(f.and(b,c))), f.and(a, b));
        final Formula formula2 = new PropositionalParser(new FormulaFactory()).parse("A & B & (A | B | C) & (~B | D)");
        System.out.println();

        System.out.println("Formula 1: " + formula1);

        var minimizedFormula1 = formula1.transform(new AdvancedSimplifier(new DefaultRatingFunction()));
        System.out.println("Minimized Formula 1 (AdvancedSimplifier): " + minimizedFormula1);

        var minimizedFormula2 = formula1.transform(new BackboneSimplifier());
        System.out.println("Minimized Formula 1 (BackboneSimplifier): " + minimizedFormula2);

        System.out.println();
        System.out.println("+++++++++++++");
        System.out.println();

        System.out.println("Formula 2: " + formula2);

        minimizedFormula1 = formula2.transform(new AdvancedSimplifier(new DefaultRatingFunction()));
        System.out.println("Minimized Formula 2 (AdvancedSimplifier): " + minimizedFormula1);

        minimizedFormula2 = formula2.transform(new BackboneSimplifier());
        System.out.println("Minimized Formula 2 (BackboneSimplifier): " + minimizedFormula2);
    }
}
