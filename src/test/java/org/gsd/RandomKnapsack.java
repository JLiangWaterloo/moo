package org.gsd;

import java.util.Arrays;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.SolverFactory;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;
import org.junit.Test;

/**
 *
 * @author jimmy
 */
public class RandomKnapsack {

    @Test
    public void testKnapsack() {
        for(int z = 0; z < 2; z++){
        int n = 5;
        for (int i = 2; i <= n; i++) {
            for (int k = 0; k < 3;  k++) {
                Solver solver = SolverFactory.makeSolver();
                IntVar[] occurrences = VariableFactory.enumeratedArray("occurences", n, 0, 4, solver);
                IntVar totalWeight = VariableFactory.enumerated("totalWeight", 0, 40, solver);
                IntVar totalEnergy = VariableFactory.enumerated("totalEnergy", 0, 50, solver);
                solver.post(ICF.knapsack(occurrences, totalWeight, totalEnergy, new int[]{1, 2, 3, 4, 5}, new int[]{2, 3, 4, 5, 7}));

                IntVar[] objectives = Arrays.copyOf(occurrences, i);

                long start = System.currentTimeMillis();
                if (k == 0) {
                    System.out.println("gia(" + i + "): " + Moo.gia(solver, objectives));
                } else if (k == 1) {
                    System.out.println("classic(" + i + "): " + Moo.classic(solver, objectives).size());
                } else {
                    System.out.println("oia(" + i + "): " + Moo.oia(solver, objectives).size());
                }
                System.out.println(System.currentTimeMillis() - start);
            }
        }}
    }
}
