package org.gsd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.BitSet;
import org.uma.jmetal.problem.ConstrainedProblem;
import org.uma.jmetal.problem.impl.AbstractBinaryProblem;
import org.uma.jmetal.solution.BinarySolution;
import org.uma.jmetal.util.solutionattribute.impl.NumberOfViolatedConstraints;
import org.uma.jmetal.util.solutionattribute.impl.OverallConstraintViolation;

/**
 *
 * @author jimmy
 */
public class KnapsackProblem extends AbstractBinaryProblem implements ConstrainedProblem<BinarySolution> {

    private static int[] parse1dArray(Reader in) throws IOException {
        int c;
        while (Character.isWhitespace(c = in.read())) {
        }
        if (c != '[') {
            throw new IOException();
        }
        int[] array = new int[4];
        int size = 0;

        StringBuilder item = new StringBuilder();
        while (true) {
            c = in.read();
            if (c == ',' || c == ']') {
                if (size == array.length) {
                    array = Arrays.copyOf(array, array.length * 2);
                }
                array[size++] = Integer.parseInt(item.toString());
                if (c == ']') {
                    return Arrays.copyOf(array, size);
                }
                item.setLength(0);
            } else if (!Character.isWhitespace(c)) {
                item.append((char) c);
            }
        }
    }

    private static int[][] parse2dArray(Reader in) throws IOException {
        int c;
        while (Character.isWhitespace(c = in.read())) {
        }
        if (c != '[') {
            throw new IOException();
        }
        int[][] array = new int[4][];
        int size = 0;

        array[size++] = parse1dArray(in);
        while ((c = in.read()) != ']') {
            if (c == ',') {
                if (size == array.length) {
                    array = Arrays.copyOf(array, array.length * 2);
                }
                array[size++] = parse1dArray(in);
            } else if (!Character.isWhitespace(c)) {
                throw new Error("Unknown character: " + (char) c);
            }
        }
        return Arrays.copyOf(array, size);
    }

    public static KnapsackProblem parseKnapsack(BufferedReader in) throws IOException {
        int objectives = Integer.parseUnsignedInt(in.readLine());
        int objects = Integer.parseUnsignedInt(in.readLine());
        int capacity = Integer.parseUnsignedInt(in.readLine());

        int[][] energies = parse2dArray(in);
        if (energies.length != objectives) {
            throw new IOException();
        }
        for (int[] energy : energies) {
            if (energy.length != objects) {
                throw new IOException();
            }
        }

        int[] weights = parse1dArray(in);
        if (weights.length != objects) {
            throw new IOException();
        }

        return new KnapsackProblem(capacity, weights, energies);
    }

    private final OverallConstraintViolation<BinarySolution> overallConstraintViolationDegree
            = new OverallConstraintViolation<>();
    private final NumberOfViolatedConstraints<BinarySolution> numberOfViolatedConstraints
            = new NumberOfViolatedConstraints<>();

    private final int capacity;
    private final int[] weights;
    private final int[][] energies;

    public KnapsackProblem(int capacity, int[] weights, int[][] energies) {
        this.capacity = capacity;
        this.weights = weights;
        this.energies = energies;
        for (int[] energy : energies) {
            if (weights.length != energy.length) {
                throw new IllegalArgumentException();
            }
        }

        setNumberOfVariables(1);
        setNumberOfObjectives(energies.length);
        setName("Knapsack");
    }

    public int getCapacity() {
        return capacity;
    }

    public int getNumberOfObjects() {
        return weights.length;
    }

    public int[] getWeights() {
        return weights;
    }

    public int[][] getEnergies() {
        return energies;
    }

    @Override
    protected int getBitsPerVariable(int index) {
        return weights.length;
    }

    @Override
    public void evaluate(BinarySolution solution) {
        BitSet items = solution.getVariableValue(0);
        for (int objective = 0; objective < energies.length; objective++) {
            int totalEnergy = 0;
            for (int i = 0; i < energies[objective].length; i++) {
                if (items.get(i)) {
                    totalEnergy += energies[objective][i];
                }
            }
            solution.setObjective(objective, -totalEnergy);
        }
    }

    @Override
    public void evaluateConstraints(BinarySolution solution) {
        BitSet items = solution.getVariableValue(0);
        int totalWeight = 0;
        for (int i = 0; i < weights.length; i++) {
            if (items.get(i)) {
                totalWeight += weights[i];
            }
        }
        if (totalWeight > capacity) {
            overallConstraintViolationDegree.setAttribute(solution, (double) capacity - totalWeight);
            numberOfViolatedConstraints.setAttribute(solution, 1);
        } else {
            overallConstraintViolationDegree.setAttribute(solution, 0.0);
            numberOfViolatedConstraints.setAttribute(solution, 0);
        }
    }
}
