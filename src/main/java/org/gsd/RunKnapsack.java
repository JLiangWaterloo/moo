package org.gsd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.SolverFactory;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.search.loop.monitors.IMonitorOpenNode;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;

/**
 *
 * @author jimmy
 */
public class RunKnapsack {

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

    private static int sum(int... array) {
        int sum = 0;
        for (int i : array) {
            sum += i;
        }
        return sum;
    }

    private static class Pair {

        IntVar[] totalEnergies;
        int objects;

        Pair(IntVar[] totalEnergies, int objects) {
            this.totalEnergies = totalEnergies;
            this.objects = objects;
        }
    }

    private static Pair parseKnapsack(BufferedReader in, Solver solver) throws IOException {
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

        BoolVar[] occurences = VF.boolArray("occurences", objects, solver);
        IntVar totalWeight = VF.enumerated("totalWeight", 0, capacity, solver);
        IntVar[] totalEnergies = new IntVar[energies.length];
        for (int i = 0; i < energies.length; i++) {
            totalEnergies[i] = VF.enumerated("totalEnergy[" + i + "]", 0, sum(energies[i]), solver);
            solver.post(ICF.knapsack(occurences, totalWeight, totalEnergies[i], weights, energies[i]));
        }
        return new Pair(totalEnergies, objects);
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        Path path = Paths.get(args[0]);
        Solver solver = SolverFactory.makeSolver();

        Pair pair = parseKnapsack(Files.newBufferedReader(path), solver);
        IntVar[] objectives = pair.totalEnergies;

        long timeout = 10 * 60 * 1000;
        long start = System.currentTimeMillis();
        solver.plugMonitor(new IMonitorOpenNode() {

            @Override
            public void beforeOpenNode() {
                if (System.currentTimeMillis() - start > timeout) {
                    solver.getSearchLoop().interrupt("Times up", false);
                }
            }

            @Override
            public void afterOpenNode() {
            }
        });

        List<int[]> paretoFront = Moo.oia(solver, objectives);
        long time = System.currentTimeMillis() - start;
        String out = "oia " + objectives.length + " " + pair.objects + " " + paretoFront.size() + " " + solver.getMeasures().getSolutionCount();
        System.out.println(out + " " + time + " " + (time > timeout));
        for (int[] paretoPoint : paretoFront) {
            for (int p : paretoPoint) {
                System.out.print(p + " ");
            }
            System.out.println(";");
        }
    }
}
