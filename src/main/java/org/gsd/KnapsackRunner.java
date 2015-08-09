package org.gsd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.SolverFactory;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.search.loop.monitors.IMonitorInitialize;
import org.chocosolver.solver.search.loop.monitors.IMonitorOpenNode;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.multiobjective.mocell.MOCell;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAII;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIMeasures;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.SteadyStateNSGAII;
import org.uma.jmetal.algorithm.multiobjective.pesa2.PESA2;
import org.uma.jmetal.algorithm.multiobjective.spea2.SPEA2;
import org.uma.jmetal.operator.CrossoverOperator;
import org.uma.jmetal.operator.MutationOperator;
import org.uma.jmetal.operator.SelectionOperator;
import org.uma.jmetal.operator.impl.crossover.SinglePointCrossover;
import org.uma.jmetal.operator.impl.mutation.BitFlipMutation;
import org.uma.jmetal.operator.impl.selection.BinaryTournamentSelection;
import org.uma.jmetal.runner.AbstractAlgorithmRunner;
import org.uma.jmetal.solution.BinarySolution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;
import org.uma.jmetal.util.neighborhood.Neighborhood;
import org.uma.jmetal.util.neighborhood.impl.C9;
import org.uma.jmetal.util.solutionattribute.impl.NumberOfViolatedConstraints;

/**
 *
 * @author jimmy
 */
public class KnapsackRunner extends AbstractAlgorithmRunner {

    public static void main(String[] args) throws IOException {
        run(Integer.parseInt(args[0]), new File(args[1]));
    }

    public static void run(int algorithm, File file) throws IOException {
        int populationSize = 100;
        long timeout = 5 * 60 * 1000;
        KnapsackProblem problem = KnapsackProblem.parseKnapsack(new BufferedReader(new FileReader(file)));

        long start = System.currentTimeMillis();

        String name;
        List<int[]> paretoFront;

        switch (algorithm) {
            case 0:
                Solver solver = SolverFactory.makeSolver();
                IntVar[] objectives = choco(problem, solver, timeout);
                name = "oia";
                paretoFront = Moo.oia(solver, objectives);
                for (int[] paretoPoint : paretoFront) {
                    for (int i = 0; i < paretoPoint.length; i++) {
                        paretoPoint[i] = -paretoPoint[i];
                    }
                }
                break;
            case 1:
                name = "spea";
                paretoFront = map(KnapsackRunner::convert, runJMetal(spea(problem, populationSize, timeout)));
                break;
            case 2:
                name = "pesa";
                paretoFront = map(KnapsackRunner::convert, runJMetal(pesa(problem, populationSize, timeout)));
                break;
            case 3:
                name = "mocell";
                paretoFront = map(KnapsackRunner::convert, runJMetal(mocell(problem, populationSize, timeout)));
                break;
            case 4:
                name = "nsgaii";
                paretoFront = map(KnapsackRunner::convert, runJMetal(nsgaii(problem, populationSize, timeout)));
                break;
            case 5:
                name = "steadyStateNsgaii";
                paretoFront = map(KnapsackRunner::convert, runJMetal(steadyStateNsgaii(problem, populationSize, timeout)));
                break;
            case 6:
                name = "nsgaiiMeasures";
                paretoFront = map(KnapsackRunner::convert, runJMetal(nsgaiiMeasures(problem, populationSize, timeout)));
                break;
            case 7:
                name = "random";
                paretoFront = map(KnapsackRunner::convert, runJMetal(random(problem, timeout)));
                break;
            default:
                throw new IllegalArgumentException();
        }

        long time = System.currentTimeMillis() - start;
        System.out.println(name + " " + problem.getNumberOfObjectives() + " " + problem.getNumberOfObjects() + " " + paretoFront.size() + " " + time);
        for (int[] paretoPoint : paretoFront) {
            for (int p : paretoPoint) {
                System.out.print(p + " ");
            }
            System.out.println(";");
        }

        // No constraint: ibea, smsemoa
    }

    private static <T> T runJMetal(Algorithm<T> algorithm) {
        algorithm.run();
        return algorithm.getResult();
    }

    private static int[] convert(BinarySolution solutions) {
        int[] paretoPoint = new int[solutions.getNumberOfObjectives()];
        for (int i = 0; i < paretoPoint.length; i++) {
            paretoPoint[i] = (int) solutions.getObjective(i);
        }
        return paretoPoint;
    }

    private static <T, R> List<R> map(Function<T, R> function, List<T> list) {
        return list.stream().map(function).collect(Collectors.toList());
    }

    private static int sum(int... array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    /*
     * Adapting SPEA2BinaryRunner to knapsack.
     */
    public static Algorithm<List<BinarySolution>> spea(KnapsackProblem problem, int populationSize, long timeout) {
        double crossoverProbability = 0.9;
        CrossoverOperator<BinarySolution> crossover = new SinglePointCrossover(crossoverProbability);

        double mutationProbability = 1.0 / problem.getTotalNumberOfBits();
        MutationOperator<BinarySolution> mutation = new BitFlipMutation(mutationProbability);

        SelectionOperator<List<BinarySolution>, BinarySolution> selection
                = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

        return new SPEA2(problem, 0, populationSize, crossover, mutation, selection, new SequentialSolutionListEvaluator<>()) {
            long start = 0;

            @Override
            protected void initProgress() {
                super.initProgress();
                start = System.currentTimeMillis();
            }

            @Override
            protected boolean isStoppingConditionReached() {
                return System.currentTimeMillis() - start > timeout;
            }
        };
    }

    /*
     * Adapting PESA2Runner to knapsack.
     */
    public static Algorithm<List<BinarySolution>> pesa(KnapsackProblem problem, int populationSize, long timeout) {
        double crossoverProbability = 0.9;
        CrossoverOperator<BinarySolution> crossover = new SinglePointCrossover(crossoverProbability);

        double mutationProbability = 1.0 / problem.getTotalNumberOfBits();
        MutationOperator<BinarySolution> mutation = new BitFlipMutation(mutationProbability);

        return new PESA2(problem, 0, populationSize, 100, 5, crossover, mutation, new SequentialSolutionListEvaluator<>()) {
            long start = 0;

            @Override
            protected void initProgress() {
                super.initProgress();
                start = System.currentTimeMillis();
            }

            @Override
            protected boolean isStoppingConditionReached() {
                return System.currentTimeMillis() - start > timeout;
            }
        };
    }

    /*
     * Adapting MOCell to knapsack.
     */
    public static Algorithm<List<BinarySolution>> mocell(KnapsackProblem problem, int populationSize, long timeout) {
        double crossoverProbability = 0.9;
        CrossoverOperator<BinarySolution> crossover = new SinglePointCrossover(crossoverProbability);

        double mutationProbability = 1.0 / problem.getTotalNumberOfBits();
        MutationOperator<BinarySolution> mutation = new BitFlipMutation(mutationProbability);

        SelectionOperator<List<BinarySolution>, BinarySolution> selection
                = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

        Neighborhood<BinarySolution> neighborhood = new C9<>((int) Math.sqrt(populationSize), (int) Math.sqrt(populationSize));

        return new MOCell(problem, 0, populationSize, 100, neighborhood, crossover, mutation, selection, new SequentialSolutionListEvaluator<>()) {
            long start = 0;

            @Override
            protected void initProgress() {
                super.initProgress();
                start = System.currentTimeMillis();
            }

            @Override
            protected boolean isStoppingConditionReached() {
                return System.currentTimeMillis() - start > timeout;
            }
        };
    }

    /*
     * Adapting NSGAIIBinaryRunner to knapsack.
     */
    public static Algorithm<List<BinarySolution>> nsgaii(KnapsackProblem problem, int populationSize, long timeout) {

        double crossoverProbability = 0.9;
        CrossoverOperator<BinarySolution> crossover = new SinglePointCrossover(crossoverProbability);

        double mutationProbability = 1.0 / problem.getTotalNumberOfBits();
        MutationOperator<BinarySolution> mutation = new BitFlipMutation(mutationProbability);

        SelectionOperator<List<BinarySolution>, BinarySolution> selection
                = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

        return new NSGAII(problem, 0, populationSize, crossover, mutation, selection, new SequentialSolutionListEvaluator<>()) {
            long start = 0;

            @Override
            protected void initProgress() {
                super.initProgress();
                start = System.currentTimeMillis();
            }

            @Override
            protected boolean isStoppingConditionReached() {
                return System.currentTimeMillis() - start > timeout;
            }
        };
    }

    /*
     * Adapting NSGAIIBinaryRunner to knapsack.
     */
    public static Algorithm<List<BinarySolution>> steadyStateNsgaii(KnapsackProblem problem, int populationSize, long timeout) {

        double crossoverProbability = 0.9;
        CrossoverOperator<BinarySolution> crossover = new SinglePointCrossover(crossoverProbability);

        double mutationProbability = 1.0 / problem.getTotalNumberOfBits();
        MutationOperator<BinarySolution> mutation = new BitFlipMutation(mutationProbability);

        SelectionOperator<List<BinarySolution>, BinarySolution> selection
                = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

        return new SteadyStateNSGAII(problem, 0, populationSize, crossover, mutation, selection, new SequentialSolutionListEvaluator<>()) {
            long start = 0;

            @Override
            protected void initProgress() {
                super.initProgress();
                start = System.currentTimeMillis();
            }

            @Override
            protected boolean isStoppingConditionReached() {
                return System.currentTimeMillis() - start > timeout;
            }
        };
    }

    /*
     * Adapting NSGAIIBinaryRunner to knapsack.
     */
    public static Algorithm<List<BinarySolution>> nsgaiiMeasures(KnapsackProblem problem, int populationSize, long timeout) {

        double crossoverProbability = 0.9;
        CrossoverOperator<BinarySolution> crossover = new SinglePointCrossover(crossoverProbability);

        double mutationProbability = 1.0 / problem.getTotalNumberOfBits();
        MutationOperator<BinarySolution> mutation = new BitFlipMutation(mutationProbability);

        SelectionOperator<List<BinarySolution>, BinarySolution> selection
                = new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>());

        return new NSGAIIMeasures(problem, 0, populationSize, crossover, mutation, selection, new SequentialSolutionListEvaluator<>()) {
            long start = 0;

            @Override
            protected void initProgress() {
                super.initProgress();
                start = System.currentTimeMillis();
            }

            @Override
            protected boolean isStoppingConditionReached() {
                return System.currentTimeMillis() - start > timeout;
            }
        };
    }

    /*
     * Adapting RandomSearchRunner to knapsack.
     */
    public static Algorithm<List<BinarySolution>> random(KnapsackProblem problem, long timeout) {
        /*
         * Adapting RandomSearch to account for constraints.
         */
        return new Algorithm<List<BinarySolution>>() {

            NonDominatedSolutionListArchive<BinarySolution> nonDominatedArchive = new NonDominatedSolutionListArchive<>();
            NumberOfViolatedConstraints<BinarySolution> numberOfViolatedConstraints = new NumberOfViolatedConstraints<>();

            @Override
            public void run() {
                Integer zero = 0;
                long start = System.currentTimeMillis();
                BinarySolution newSolution;
                while (System.currentTimeMillis() - start <= timeout) {
                    newSolution = problem.createSolution();
                    problem.evaluate(newSolution);
                    problem.evaluateConstraints(newSolution);
                    if (zero.equals(numberOfViolatedConstraints.getAttribute(newSolution))) {
                        nonDominatedArchive.add(newSolution);
                    }
                }
            }

            @Override
            public List<BinarySolution> getResult() {
                return nonDominatedArchive.getSolutionList();
            }
        };
    }

    public static IntVar[] choco(KnapsackProblem problem, Solver solver, long timeout) {
        BoolVar[] occurences = VF.boolArray("occurences", problem.getNumberOfObjects(), solver);
        IntVar totalWeight = VF.enumerated("totalWeight", 0, problem.getCapacity(), solver);
        IntVar[] totalEnergies = new IntVar[problem.getNumberOfObjectives()];
        for (int i = 0; i < totalEnergies.length; i++) {
            totalEnergies[i] = VF.enumerated("totalEnergy[" + i + "]", 0, sum(problem.getEnergies()[i]), solver);
            solver.post(ICF.knapsack(occurences, totalWeight, totalEnergies[i], problem.getWeights(), problem.getEnergies()[i]));
        }
        solver.set(ISF.domOverWDeg(occurences, System.currentTimeMillis(), ISF.max_value_selector()));
        solver.plugMonitor(new ChocoTimeout(solver, timeout));
        return totalEnergies;
    }

    private static class ChocoTimeout implements IMonitorInitialize, IMonitorOpenNode {

        private final Solver solver;
        private final long timeout;
        private long start = 0;

        public ChocoTimeout(Solver solver, long timeout) {
            this.solver = solver;
            this.timeout = timeout;
        }

        @Override
        public void beforeInitialize() {
            start = System.currentTimeMillis();
        }

        @Override
        public void afterInitialize() {
        }

        @Override
        public void beforeOpenNode() {
            if (System.currentTimeMillis() - start > timeout) {
                solver.getSearchLoop().interrupt("Times up", false);
            }
        }

        @Override
        public void afterOpenNode() {
        }
    }
}
