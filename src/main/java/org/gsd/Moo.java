package org.gsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.variables.IntVar;

/**
 *
 * @author jimmy
 */
public class Moo {

    private static boolean dominates(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] < b[i]) {
                return false;
            }
        }
        return true;
    }

    public static List<int[]> oia(Solver solver, IntVar... objectives) {
        List<int[]> paretoPoints = new ArrayList<>();
        if (solver.findSolution()) {
            do {
                Constraint[] better = new Constraint[objectives.length];
                for (int i = 0; i < objectives.length; i++) {
                    better[i] = ICF.arithm(objectives[i], ">", objectives[i].getValue());
                }
                solver.post(LCF.or(better));

                int[] point = new int[objectives.length];
                for (int i = 0; i < point.length; i++) {
                    point[i] = objectives[i].getValue();
                }
                paretoPoints.removeIf(x -> dominates(point, x));
                paretoPoints.add(point);
            } while (solver.nextSolution());
        }
        return paretoPoints;
    }

    public static int gia(Solver solver, IntVar... objectives) {
        int count = 0;
        while (solver.findSolution()) {
            List<Constraint> stack = new ArrayList<>();
            do {
                Constraint[] better = new Constraint[objectives.length];
                for (int i = 0; i < better.length; i++) {
                    better[i] = ICF.arithm(objectives[i], ">", objectives[i].getValue());
                    Constraint guide = ICF.arithm(objectives[i], ">=", objectives[i].getValue());
                    stack.add(guide);
                    solver.post(guide);
                }
                solver.post(LCF.or(better));
            } while (solver.nextSolution());
            count++;
            solver.getSearchLoop().reset();
            solver.getEngine().flush();
            stack.forEach(solver::unpost);
        }
        return count;
    }

    private static int[] cons(int head, int[] tail) {
        int[] cons = new int[tail.length + 1];
        cons[0] = head;
        System.arraycopy(tail, 0, cons, 1, tail.length);
        return cons;
    }

    public static List<int[]> classic(Solver solver, IntVar... objectives) {
        if (solver.findSolution()) {
            List<Constraint> stack = new ArrayList<>();
            int optimal0;
            do {
                optimal0 = objectives[0].getValue();
                Constraint better = ICF.arithm(objectives[0], ">", objectives[0].getValue());
                stack.add(better);
                solver.post(better);
            } while (solver.nextSolution());
            solver.getSearchLoop().reset();
            solver.getEngine().flush();
            stack.forEach(solver::unpost);
            if (objectives.length == 1) {
                return Collections.singletonList(new int[]{optimal0});
            } else {
                stack.clear();
                List<int[]> paretoPoints = new ArrayList<>();
                Constraint same0 = ICF.arithm(objectives[0], "=", optimal0);
                solver.post(same0);
                for (int[] same0ParetoPoints : classic(solver, Arrays.copyOfRange(objectives, 1, objectives.length))) {
                    paretoPoints.add(cons(optimal0, same0ParetoPoints));
                    Constraint[] better = new Constraint[objectives.length - 1];
                    for (int i = 0; i < better.length; i++) {
                        better[i] = ICF.arithm(objectives[i + 1], ">", same0ParetoPoints[i]);
                    }
                    Constraint improve = LCF.or(better);
                    stack.add(improve);
                    solver.post(improve);
                }
                solver.unpost(same0);
                paretoPoints.addAll(classic(solver, objectives));
                stack.forEach(solver::unpost);
                return paretoPoints;
            }
        }
        solver.getSearchLoop().reset();
        solver.getEngine().flush();
        return Collections.emptyList();
    }
}
