package org.gsd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.ErrorRatio;
import org.uma.jmetal.qualityindicator.impl.GeneralizedSpread;
import org.uma.jmetal.qualityindicator.impl.GenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.Hypervolume;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistance;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.solution.DoubleSolution;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.front.Front;
import org.uma.jmetal.util.front.imp.ArrayFront;
import org.uma.jmetal.util.front.util.FrontNormalizer;
import org.uma.jmetal.util.front.util.FrontUtils;
import org.uma.jmetal.util.point.Point;
import org.uma.jmetal.util.point.impl.ArrayPoint;

/**
 *
 * @author jimmy
 */
public class Quality {

    public static void main(String[] args) throws IOException {
        int field = Integer.parseInt(args[0]);

        List<List<Point>> paretoFronts = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            paretoFronts.add(parse(new File(args[i])));
        }
        Front oracle = toFront(merge(paretoFronts));

        List<Double> columns = new ArrayList<>();
        for (List<Point> paretoFront : paretoFronts) {
            Stats s = computeStats(FrontUtils.convertFrontToSolutionList(toFront(paretoFront)), oracle);
            switch (field) {
                case 0:
                    columns.add(s.hypervolumeN);
                    break;
                case 1:
                    columns.add(s.hypervolume);
                    break;
                case 2:
                    columns.add(s.epsilonN);
                    break;
                case 3:
                    columns.add(s.epsilon);
                    break;
                case 4:
                    columns.add(s.gdN);
                    break;
                case 5:
                    columns.add(s.gd);
                    break;
                case 6:
                    columns.add(s.igdN);
                    break;
                case 7:
                    columns.add(s.igd);
                    break;
                case 8:
                    columns.add(s.igdPlusN);
                    break;
                case 9:
                    columns.add(s.igdPlus);
                    break;
                case 10:
                    columns.add(s.spreadN);
                    break;
                case 11:
                    columns.add(s.spread);
                    break;
                case 12:
                    columns.add(s.errorRatio);
                    break;
            }
        }

        Double max = Collections.max(columns);
        Double min = Collections.min(columns);

        Iterator<Double> iter = columns.iterator();
        while (iter.hasNext()) {
            Double value = iter.next();
            if (value.equals(max)) {
                System.out.print("\\color{blue}");
            } else if (value.equals(min)) {
                System.out.print("\\color{red}");
            }
            System.out.printf("%.3G", value);
            if (iter.hasNext()) {
                System.out.print(" & ");
            }else {
                System.out.println(" \\\\");
            }
        }
    }

    private static class Stats {

        private final double hypervolumeN, hypervolume,
                epsilonN, epsilon,
                gdN, gd,
                igdN, igd,
                igdPlusN, igdPlus,
                spreadN, spread,
                errorRatio;

        public Stats(double hypervolumeN, double hypervolume, double epsilonN, double epsilon, double gdN, double gd, double igdN, double igd, double igdPlusN, double igdPlus, double spreadN, double spread, double errorRatio) {
            this.hypervolumeN = hypervolumeN;
            this.hypervolume = hypervolume;
            this.epsilonN = epsilonN;
            this.epsilon = epsilon;
            this.gdN = gdN;
            this.gd = gd;
            this.igdN = igdN;
            this.igd = igd;
            this.igdPlusN = igdPlusN;
            this.igdPlus = igdPlus;
            this.spreadN = spreadN;
            this.spread = spread;
            this.errorRatio = errorRatio;
        }
    }

    /*
     * Adapted from AbstractAlgorithmRunner. Removed R2 since it is throwing index out of range exceptions.
     */
    public static Stats computeStats(List<? extends Solution<?>> population, Front referenceFront)
            throws FileNotFoundException {
        FrontNormalizer frontNormalizer = new FrontNormalizer(referenceFront);

        Front normalizedReferenceFront = frontNormalizer.normalize(referenceFront);
        Front normalizedFront = frontNormalizer.normalize(new ArrayFront(population));
        List<DoubleSolution> normalizedPopulation = FrontUtils
                .convertFrontToSolutionList(normalizedFront);

        return new Stats(
                new Hypervolume<>(normalizedReferenceFront).evaluate(normalizedPopulation),
                new Hypervolume<>(referenceFront).evaluate(population),
                new Epsilon<>(normalizedReferenceFront).evaluate(normalizedPopulation),
                new Epsilon<>(referenceFront).evaluate(population),
                new GenerationalDistance<>(normalizedReferenceFront).evaluate(normalizedPopulation),
                new GenerationalDistance<>(referenceFront).evaluate(population),
                new InvertedGenerationalDistance<>(normalizedReferenceFront).evaluate(normalizedPopulation),
                new InvertedGenerationalDistance<>(referenceFront).evaluate(population),
                new InvertedGenerationalDistancePlus<>(normalizedReferenceFront).evaluate(normalizedPopulation),
                new InvertedGenerationalDistancePlus<>(referenceFront).evaluate(population),
                new GeneralizedSpread<>(normalizedReferenceFront).evaluate(normalizedPopulation),
                new GeneralizedSpread<>(referenceFront).evaluate(population),
                new ErrorRatio<>(referenceFront).evaluate(population));
    }

    private static Front toFront(Collection<Point> paretoFront) {
        ArrayFront front = new ArrayFront(paretoFront.size(), paretoFront.iterator().next().getNumberOfDimensions());
        int i = 0;
        for (Point paretoPoint : paretoFront) {
            front.setPoint(i, paretoPoint);
            i++;
        }
        return front;
    }

    private static List<Point> parse(File file) throws IOException {
        List<Point> paretoFront = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();

                if (line.endsWith(";")) {
                    String[] coordinates = line.split(" ");
                    double[] paretoPoint = new double[coordinates.length - 1];
                    for (int i = 0; i < paretoPoint.length; i++) {
                        paretoPoint[i] = Integer.parseInt(coordinates[i]);
                    }
                    paretoFront.add(new ArrayPoint(paretoPoint));
                }
            }
        }
        return paretoFront;
    }

    private static boolean dominates(Point a, Point b) {
        if (a.getNumberOfDimensions() != b.getNumberOfDimensions()) {
            throw new IllegalArgumentException();
        }
        boolean strict = false;
        for (int i = 0; i < a.getNumberOfDimensions(); i++) {
            if (a.getDimensionValue(i) > b.getDimensionValue(i)) {
                return false;
            } else if (a.getDimensionValue(i) < b.getDimensionValue(i)) {
                strict = true;
            }
        }
        return strict;
    }

    private static Set<Point> merge(List<List<Point>> paretoFronts) {
        Set<Point> merge = new HashSet<>();
        for (List<Point> paretoFront : paretoFronts) {
            merge.addAll(paretoFront);
        }
        for (List<Point> paretoFront : paretoFronts) {
            paretoFront.forEach(paretoPoint -> merge.removeIf(x -> dominates(paretoPoint, x)));
        }
        return merge;
    }
}
