package part5;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Microservices {
    private static final HashMap<String, Double> intraWeight = new HashMap<>();
    static {
        intraWeight.put("com", 0.8);
        intraWeight.put("agg", 0.6);
        intraWeight.put("ass", 0.4);
        intraWeight.put("dep", 0.2);
    }
    private static final HashMap<String, Double> interWeight = new HashMap<>();
    static {
        interWeight.put("com", 0.2);
        interWeight.put("agg", 0.4);
        interWeight.put("ass", 0.8);
        interWeight.put("dep", 1.0);
    }
    private static double iscThreshold = 0.016;
    private static double escThreshold = 0.80;

    public static void main(String[] args) {
        String[][] comp = {
                {"Controller", "Role", "ass"},
                {"Controller", "Architects", "dep"},
                {"Controller", "Engineers", "dep"},
                {"Architects", "Architect", "agg"},
                {"Architect", "Project", "ass"},
                {"Architect", "ProjectNumber", "ass"},
                {"Engineers", "Engineer", "agg"},
                {"Engineer", "Project", "ass"},
                {"Engineer", "ProjectNumber", "ass"},
                {"Project", "ProjectNumber", "ass"}
        };

        String[][] comp3 = {
                {"Controller", "Role", "ass"},
                {"Controller", "Architects", "dep"},
                {"Controller", "Engineers", "dep"},
                {"Architect", "Architects", "com"},
                {"Engineer", "Engineers", "com"},
                {"Project", "Architect", "com"},
                {"Project", "Engineer", "com"},
                {"ProjectNumber", "Engineer", "com"},
                {"ProjectNumber", "Project", "com"},
                {"ProjectNumber", "Architect", "com"}
        };

        String[][] comp2 = {
                {"Student", "DbStorage", "com"},
                {"Student", "FileStorage", "com"},
                {"DataParser", "DbStorage", "com"},
                {"Controller", "DbStorage", "dep"},
                {"DataParser", "FileStorage", "com"},
                {"Controller", "FileStorage", "dep"},
                {"Controller", "StorageType", "ass"}
        };

        // First iteration
        System.out.println("First Iteration Clustering");
        System.out.println("------------------------");
        String[][] currentComp = comp3;

        // Initialize microservices
        generateInitialMicroservices(currentComp);

        // Generate clustered microservices for first phase
        generateClusteredMicroservices(currentComp);

        // Calculate metrics for first phase and select optimal microservice
        printSim(currentComp);
        printIntraSim(currentComp);
        printInterSim(currentComp);
        printISC(currentComp);
        printESC(currentComp);  // This will also call selectOptimalMicroservices()

        // Second clustering phase
        System.out.println("\nSecond Iteration Clustering");
        System.out.println("-------------------------");
        generateSecondPhaseClusteredMicroservices(currentComp);
    }

    protected static Map<Integer, List<String>> allMicroservices = new HashMap<>();

    public static void generateInitialMicroservices(String[][] comp) {
        HashSet<String> uniqueClasses = new HashSet<>();
        for (String[] relation : comp) {
            uniqueClasses.add(relation[0]);
            uniqueClasses.add(relation[1]);
        }

        System.out.println("\nInitializing Phase");
        System.out.println("------------------");

        // Store singleton microservices (mic1 through mic6)
        int micCounter = 1;
        for (String className : uniqueClasses) {
            List<String> classes = new ArrayList<>();
            classes.add(className);
            allMicroservices.put(micCounter, classes);
            System.out.printf("mic%d:\n\tmic%d.%s\n", micCounter, micCounter, className);
            micCounter++;
        }
    }

    public static void generateClusteredMicroservices(String[][] comp) {
        // First ensure we have the initial microservices
        if (allMicroservices.isEmpty()) {
            generateInitialMicroservices(comp);
        }

        // Find the number of initial microservices
        int numInitialMics = 0;
        while (allMicroservices.containsKey(numInitialMics + 1)) {
            numInitialMics++;
        }

        // Start clustering phase from the next number after initial phase
        int micCounter = numInitialMics + 1;

        System.out.println("\nClustering Phase");
        System.out.println("----------------");

        // Generate all possible pairs of singleton microservices
        for (int i = 1; i <= numInitialMics; i++) {
            for (int j = i + 1; j <= numInitialMics; j++) {
                List<String> mic1Classes = allMicroservices.get(i);
                List<String> mic2Classes = allMicroservices.get(j);

                // Create new microservice containing all classes from both microservices
                List<String> newMicClasses = new ArrayList<>();
                newMicClasses.addAll(mic1Classes);
                newMicClasses.addAll(mic2Classes);

                // Store the new microservice
                allMicroservices.put(micCounter, newMicClasses);

                // Print the new microservice
                System.out.printf("mic%d:\n", micCounter);
                for (String className : newMicClasses) {
                    System.out.printf("\tmic%d.%s\n", micCounter, className);
                }

                micCounter++;
            }
        }
    }

    public static List<String> getClassesInMicroservice(int micNumber) {
        return allMicroservices.getOrDefault(micNumber, new ArrayList<>());
    }

    public static double sim(String class1, String class2) {
        HashMap<Character, Integer> freq1 = new HashMap<>();
        HashMap<Character, Integer> freq2 = new HashMap<>();

        /*
        example: SSS
        freq1 will put S first but since in freq1 there's none of S yet, it will be freq1.put(S, 0+1);
        fre1 will put S again but since there's one, it will be freq1.put(S, 2); so on...
        */
        for (int i = 0; i < class1.length(); i++) {
            freq1.put(class1.charAt(i), freq1.getOrDefault(class1.charAt(i), 0) + 1);
        }

        for (int i = 0; i < class2.length(); i++) {
            freq2.put(class2.charAt(i), freq2.getOrDefault(class2.charAt(i), 0) + 1);
        }

        // Add all the keys of frequencies of both classes characters
        HashSet<Character> allChars = new HashSet<>();
        allChars.addAll(freq1.keySet());
        allChars.addAll(freq2.keySet());

        int commonCount = 0;
        int distinctCount = 0;

        /*
        example: SaSa and Saa
        freq1 will give me S = 2 and freq2 will give me S = 1
        freq1 will give me a = 2 and freq2 will give me a = 2
        so that's why commonCount = min(2, 1) + min(2, 2) = 1 + 2 = 3
        distinctCount = max(2, 1) + max(2, 2) = 2 + 2 = 4
        */

        for (char c : allChars) {
            int count1 = freq1.getOrDefault(c, 0);
            int count2 = freq2.getOrDefault(c, 0);

            commonCount += Math.min(count1, count2);
            distinctCount += Math.max(count1, count2);
        }
        return (double) commonCount / distinctCount;
    }

    public static void printSim(String[][] comparison) {
        System.out.println("Similarity Calculation");
        System.out.println("----------------------");
        for (String[] pair : comparison) {
            System.out.printf("Similarity of %s, %s = %.6f\n", pair[0], pair[1], sim(pair[0], pair[1]));
        }
        System.out.println();
    }

    public static double intraSim(String rel, double sim) {
        return intraWeight.get(rel) * sim;
    }

    public static void printIntraSim(String[][] comparison) {
        System.out.println("IntraSimilarity Calculation");
        System.out.println("---------------------------");
        for (String[] pair : comparison) {
            System.out.printf("Intrasimilarity of %s, %s = %.6f\n", pair[0], pair[1], intraSim(pair[2], sim(pair[0], pair[1])));
        }
        System.out.println();
    }

    public static double interDistance(String rel, double sim) {
        double result = interWeight.get(rel) * (1 - sim);
        return BigDecimal.valueOf(result)
                .setScale(6, RoundingMode.DOWN)
                .doubleValue();
    }

    public static void printInterSim(String[][] comparison) {
        System.out.println("InterSimilarity Calculation");
        System.out.println("---------------------------");
        for (String[] pair : comparison) {
            System.out.printf("Intersimilarity of %s, %s = %.6f\n", pair[0], pair[1], interDistance(pair[2], sim(pair[0], pair[1])));
        }
        System.out.println();
    }

    public static void printISC(String[][] comparison) {
        // First generate the microservices if not already done
        if (allMicroservices.isEmpty()) {
            generateInitialMicroservices(comparison);
            generateClusteredMicroservices(comparison);
        }

        // Build the intra-similarity map
        HashMap<String, HashMap<String, Double>> intraSimMap = new HashMap<>();
        for (String[] pair : comparison) {
            String class1 = pair[0];
            String class2 = pair[1];
            String relation = pair[2];
            double sim = sim(class1, class2);
            double intraSim = intraSim(relation, sim);

            intraSimMap.putIfAbsent(class1, new HashMap<>());
            intraSimMap.get(class1).put(class2, intraSim);
        }

        System.out.println("\nInternal Cohesion (ISC) Calculation");
        System.out.println("-----------------------------------");

        // Find the maximum microservice number
        int maxMicNumber = 0;
        for (Integer micNumber : allMicroservices.keySet()) {
            maxMicNumber = Math.max(maxMicNumber, micNumber);
        }

        // Calculate ISC for each microservice
        for (int micNumber = 1; micNumber <= maxMicNumber; micNumber++) {
            List<String> classes = getClassesInMicroservice(micNumber);
            if (!classes.isEmpty()) {
                System.out.printf("\nmic%d:\n", micNumber);

                if (classes.size() < 2) {
                    System.out.printf("Single class microservice: %s\n", classes.get(0));
                    System.out.printf("ISC(mic%d) = 0.000000\n", micNumber);
                } else if (classes.size() == 2) {
                    System.out.println("Class comparisons:");
                    double totalIntraSim = 0.0;
                    int pairs = 0;

                    // Show all class comparisons
                    for (int i = 0; i < classes.size(); i++) {
                        for (int j = i + 1; j < classes.size(); j++) {
                            String class1 = classes.get(i);
                            String class2 = classes.get(j);

                            HashMap<String, Double> map1 = intraSimMap.get(class1);
                            HashMap<String, Double> map2 = intraSimMap.get(class2);

                            double intraSim12 = map1 != null ? map1.getOrDefault(class2, 0.0) : 0.0;
                            double intraSim21 = map2 != null ? map2.getOrDefault(class1, 0.0) : 0.0;

                            double avgIntraSim = (intraSim12 + intraSim21) / 2.0;
                            System.out.printf("  %s <-> %s\n", class1, class2);

                            if (intraSim12 != 0.0 || intraSim21 != 0.0) {
                                totalIntraSim += avgIntraSim;
                                pairs++;
                            }
                        }
                    }

                    double isc = pairs > 0 ?
                            BigDecimal.valueOf(totalIntraSim / pairs)
                                    .setScale(6, RoundingMode.DOWN)
                                    .doubleValue() : 0.0;

                    System.out.printf("ISC(mic%d) = %.6f\n", micNumber, isc);
                } else if (classes.size() == 3) {
                    System.out.println("Class comparisons:");
                    double totalIntraSim = 0.0;
                    int n = classes.size();
                    int denominator = n * (n - 1); // 3 * (3-1) = 6

                    // Show all class comparisons
                    for (int i = 0; i < classes.size(); i++) {
                        for (int j = i + 1; j < classes.size(); j++) {
                            String class1 = classes.get(i);
                            String class2 = classes.get(j);

                            HashMap<String, Double> map1 = intraSimMap.get(class1);
                            HashMap<String, Double> map2 = intraSimMap.get(class2);

                            double intraSim12 = map1 != null ? map1.getOrDefault(class2, 0.0) : 0.0;
                            double intraSim21 = map2 != null ? map2.getOrDefault(class1, 0.0) : 0.0;

                            System.out.printf("  %s <-> %s\n", class1, class2);
                            
                            // Add both directions to total
                            totalIntraSim += (intraSim12 + intraSim21);
                        }
                    }

                    // Calculate ISC: sum of all pairs divided by n(n-1)
                    double isc = BigDecimal.valueOf(totalIntraSim / denominator)
                            .setScale(6, RoundingMode.DOWN)
                            .doubleValue();

                    System.out.printf("ISC(mic%d) = %.6f\n", micNumber, isc);
                }
            }
        }
        System.out.println();
    }

    public static double calculateESC(int micNumber, String[][] comp) {
        List<String> micClasses = getClassesInMicroservice(micNumber);
        if (micClasses.isEmpty()) return 0.0;

        System.out.printf("\nCalculating ESC for mic%d:\n", micNumber);

        // Get all unique external classes that have incoming relationships with our microservice
        Set<String> n2Classes = new HashSet<>();
        for (String[] relation : comp) {
            if (micClasses.contains(relation[1]) && !micClasses.contains(relation[0])) {
                n2Classes.add(relation[0]);
            }
        }

        // If no incoming relationships, return 0.0
        if (n2Classes.isEmpty()) {
            System.out.printf("No incoming relationships found, ESC = 0.0\n");
            return 0.0;
        }

        double totalInterDistance = 0.0;
        int M = micClasses.size();
        int N2 = n2Classes.size();

        System.out.printf("M (classes in microservice) = %d\n", M);
        System.out.printf("N2 (external classes with incoming relationships) = %d\n", N2);

        // For each class in our microservice
        for (String micClass : micClasses) {
            System.out.printf("  %s has relationships with:\n", micClass);
            double sumInterDistances = 0.0;

            // Find all relationships for this class
            for (String[] relation : comp) {
                // Only consider relationships where this class is the target (incoming)
                if (relation[1].equals(micClass)) {
                    String otherClass = relation[0];
                    String relType = relation[2];

                    // Skip if otherClass is in our microservice
                    if (micClasses.contains(otherClass)) continue;

                    double sim = Microservices.sim(micClass, otherClass);
                    double interDistance = Microservices.interDistance(relType, sim);

                    System.out.printf("    - %s (%s, incoming): sim=%.6f, interDistance=%.6f\n",
                            otherClass, relType, sim, interDistance);

                    sumInterDistances += interDistance;
                }
            }

            // Average inter-distance for this class (divide by N2)
            double avgInterDistance = sumInterDistances / N2;
            totalInterDistance += avgInterDistance;
            System.out.printf("    Average inter-distance = %.6f\n", avgInterDistance);
        }

        // Calculate final ESC (divide by M)
        double avgInterDistance = totalInterDistance / M;
        double esc = BigDecimal.valueOf(1.0 - avgInterDistance)
                .setScale(6, RoundingMode.DOWN)
                .doubleValue();

        System.out.printf("Final ESC calculation:\n");
        System.out.printf("  Total inter-distance = %.6f\n", totalInterDistance);
        System.out.printf("  Average inter-distance = %.6f\n", avgInterDistance);
        System.out.printf("  ESC = 1 - %.6f = %.6f\n", avgInterDistance, esc);

        return esc;
    }

    public static void printESC(String[][] comparison) {
        // First ensure we have the microservices
        if (allMicroservices.isEmpty()) {
            generateInitialMicroservices(comparison);
            generateClusteredMicroservices(comparison);
        }

        System.out.println("\nExternal Service Cohesion (ESC) Calculation");
        System.out.println("----------------------------------------");

        // Find max microservice number
        int maxMicNumber = 0;
        for (Integer micNumber : allMicroservices.keySet()) {
            maxMicNumber = Math.max(maxMicNumber, micNumber);
        }

        // Create and build the intra-similarity map for ISC calculation
        HashMap<String, HashMap<String, Double>> intraSimMap = new HashMap<>();
        for (String[] pair : comparison) {
            String class1 = pair[0];
            String class2 = pair[1];
            String relation = pair[2];
            double sim = sim(class1, class2);
            double intraSim = intraSim(relation, sim);

            intraSimMap.putIfAbsent(class1, new HashMap<>());
            intraSimMap.get(class1).put(class2, intraSim);
        }

        // Process each microservice
        for (int micNumber = 1; micNumber <= maxMicNumber; micNumber++) {
            List<String> micClasses = getClassesInMicroservice(micNumber);
            if (!micClasses.isEmpty()) {
                // Get N2 classes (external classes with incoming relationships)
                Set<String> n2Classes = new HashSet<>();
                for (String[] relation : comparison) {
                    if (micClasses.contains(relation[1]) && !micClasses.contains(relation[0])) {
                        n2Classes.add(relation[0]);
                    }
                }

                double esc = calculateESC(micNumber, comparison);
                if (!n2Classes.isEmpty()) {
                    System.out.printf("ESC(mic%d) = %.6f (M=%d, N2=%d)\n", 
                        micNumber, esc, micClasses.size(), n2Classes.size());
                } else {
                    System.out.printf("ESC(mic%d) = %.6f (no incoming relationships)\n", 
                        micNumber, esc);
                }
            }
        }

        // Store the highest microservice number from first phase
        lastMicNumberFromFirstPhase = maxMicNumber;
        
        // Calculate ESC for each combined microservice (skip single class ones)
        allMetrics.clear(); // Clear previous metrics
        for (int micNumber = 1; micNumber <= maxMicNumber; micNumber++) {
            List<String> classes = getClassesInMicroservice(micNumber);
            if (!classes.isEmpty() && classes.size() > 1) {
                // Print microservice details
                System.out.printf("\nmic%d (Combined): %s\n", micNumber, String.join(" + ", classes));

                // Calculate ESC
                double esc = calculateESC(micNumber, comparison);
                System.out.printf("ESC(mic%d) = %.6f\n", micNumber, esc);

                // Calculate ISC using the same method as printISC
                double totalIntraSim = 0.0;
                int pairs = 0;
                for (int i = 0; i < classes.size(); i++) {
                    for (int j = i + 1; j < classes.size(); j++) {
                        String class1 = classes.get(i);
                        String class2 = classes.get(j);

                        HashMap<String, Double> map1 = intraSimMap.get(class1);
                        HashMap<String, Double> map2 = intraSimMap.get(class2);

                        double intraSim12 = map1 != null ? map1.getOrDefault(class2, 0.0) : 0.0;
                        double intraSim21 = map2 != null ? map2.getOrDefault(class1, 0.0) : 0.0;

                        if (intraSim12 != 0.0 || intraSim21 != 0.0) {
                            totalIntraSim += (intraSim12 + intraSim21) / 2.0;
                            pairs++;
                        }
                    }
                }

                double isc = pairs == 0 ? 0.0 : BigDecimal.valueOf(totalIntraSim / pairs)
                        .setScale(6, RoundingMode.DOWN)
                        .doubleValue();

                // Store metrics
                allMetrics.add(new MicroserviceMetrics(micNumber, isc, esc, new ArrayList<>(classes)));
            }
        }

        // Store the highest microservice number from first phase
        for (Integer micNumber : allMicroservices.keySet()) {
            lastMicNumberFromFirstPhase = Math.max(lastMicNumberFromFirstPhase, micNumber);
        }

        // Apply selection algorithm
        selectOptimalMicroservices();
    }

    private static class MicroserviceMetrics {
        int micNumber;
        double isc;
        double esc;
        List<String> classes;

        MicroserviceMetrics(int micNumber, double isc, double esc, List<String> classes) {
            this.micNumber = micNumber;
            this.isc = isc;
            this.esc = esc;
            this.classes = classes;
        }
    }

    private static List<MicroserviceMetrics> allMetrics = new ArrayList<>();
    private static Set<String> uniqueClassesFromFirstIteration = new HashSet<>();
    private static int lastMicNumberFromFirstPhase = 0;
    private static MicroserviceMetrics optimalMicroservice = null;
    private static Set<String> excludedClasses = new HashSet<>();


    private static void selectOptimalMicroservices() {
        // Filter microservices that meet thresholds
        List<MicroserviceMetrics> ltmp = new ArrayList<>();
        Set<String> uniqueClasses = new HashSet<>();  // Track unique classes from threshold-meeting microservices
        
        System.out.println("\nMicroservices meeting threshold criteria (ISC > " + iscThreshold + ", ESC > " + escThreshold + ")");
        System.out.println("-----------------------------------------------------------------");
        
        for (MicroserviceMetrics metrics : allMetrics) {
            if (metrics.isc > iscThreshold && metrics.esc > escThreshold) {
                ltmp.add(metrics);
                System.out.printf("\nmic%d (ISC = %.6f, ESC = %.6f):\n",
                        metrics.micNumber, metrics.isc, metrics.esc);
                for (String className : metrics.classes) {
                    System.out.printf("mic%d.model.%s\n", metrics.micNumber, className);
                    uniqueClasses.add(className);  // Add to set of unique classes
                }
            }
        }

        // Find microservice that sub-optimizes both objectives
        optimalMicroservice = null;
        int maxOptimizationCount = -1;

        for (int i = 0; i < ltmp.size(); i++) {
            MicroserviceMetrics current = ltmp.get(i);
            int optimizationCount = 0;

            for (int j = 0; j < ltmp.size(); j++) {
                if (i != j) {
                    MicroserviceMetrics other = ltmp.get(j);
                    if (current.isc > other.isc && current.esc < other.esc) {
                        optimizationCount++;
                    }
                }
            }

            if (optimizationCount > maxOptimizationCount) {
                maxOptimizationCount = optimizationCount;
                optimalMicroservice = current;
            }
        }

        if (optimalMicroservice != null) {
            System.out.printf("\nSelected optimal microservice: mic%d\n", optimalMicroservice.micNumber);
            System.out.println("------------------------------------");
            System.out.printf("ISC = %.6f, ESC = %.6f\n", optimalMicroservice.isc, optimalMicroservice.esc);
            System.out.println("Classes:");
            for (String className : optimalMicroservice.classes) {
                System.out.printf("mic%d.model.%s\n", optimalMicroservice.micNumber, className);
                excludedClasses.add(className); // Store classes to exclude in second phase
            }
            // Store the last microservice number
            lastMicNumberFromFirstPhase = allMicroservices.size();
            // Clear previous metrics for second phase
            allMetrics.clear();
            allMicroservices.clear();
        }
    }

    public static void generateSecondPhaseClusteredMicroservices(String[][] comp) {
        // Get all unique classes from the current composition
        Set<String> availableClasses = new HashSet<>();
        for (String[] relation : comp) {
            availableClasses.add(relation[0]);
            availableClasses.add(relation[1]);
        }

        // Get classes from optimal microservice
        List<String> optimalClasses = new ArrayList<>(excludedClasses);

        // Get remaining classes (excluding optimal classes)
        Set<String> remainingClasses = new HashSet<>(availableClasses);
        remainingClasses.removeAll(excludedClasses);
        List<String> remainingClassesList = new ArrayList<>(remainingClasses);
        Collections.sort(remainingClassesList); // Sort for consistent ordering

        // Start clustering from the next number after first phase
        int micCounter = lastMicNumberFromFirstPhase + 1;

        // First, generate all possible pairs of remaining classes
        for (int i = 0; i < remainingClassesList.size(); i++) {
            for (int j = i + 1; j < remainingClassesList.size(); j++) {
                String class1 = remainingClassesList.get(i);
                String class2 = remainingClassesList.get(j);

                // Create new microservice containing both classes
                List<String> newMicClasses = new ArrayList<>();
                newMicClasses.add(class1);
                newMicClasses.add(class2);

                // Store the new microservice
                allMicroservices.put(micCounter, newMicClasses);

                // Print the new microservice
                System.out.printf("mic%d:\n", micCounter);
                for (String className : newMicClasses) {
                    System.out.printf("\tmic%d.%s\n", micCounter, className);
                }

                micCounter++;
            }
        }

        // Then, generate combinations of remaining classes with optimal classes
        for (String remainingClass : remainingClassesList) {
            // Create new microservice containing all optimal classes plus the remaining class
            List<String> newMicClasses = new ArrayList<>(optimalClasses);
            newMicClasses.add(remainingClass);

            // Store the new microservice
            allMicroservices.put(micCounter, newMicClasses);

            // Print the new microservice
            System.out.printf("mic%d:\n", micCounter);
            for (String className : newMicClasses) {
                System.out.printf("\tmic%d.%s\n", micCounter, className);
            }

            micCounter++;
        }

        // Calculate metrics for second phase
        printISC(comp);
        printSecondPhaseESC(comp);
    }

    public static double calculateSecondPhaseESC(int micNumber, String[][] comp) {
        List<String> micClasses = getClassesInMicroservice(micNumber);
        if (micClasses.isEmpty()) return 0.0;

        System.out.printf("\nCalculating ESC for mic%d:\n", micNumber);
        System.out.printf("Considering relationships with optimal microservice mic%d and unique classes\n", 
                optimalMicroservice.micNumber);

        // Get all unique external classes that have incoming relationships with our microservice
        Set<String> n2Classes = new HashSet<>();
        for (String[] relation : comp) {
            if (micClasses.contains(relation[1]) && !micClasses.contains(relation[0])) {
                n2Classes.add(relation[0]);
            }
        }

        // If no incoming relationships, return 0.0
        if (n2Classes.isEmpty()) {
            System.out.printf("No incoming relationships found, ESC = 0.0\n");
            return 0.0;
        }

        double totalInterDistance = 0.0;
        int M = micClasses.size();
        int N2 = n2Classes.size();

        System.out.printf("M (classes in microservice) = %d\n", M);
        System.out.printf("N2 (external classes with incoming relationships) = %d\n", N2);

        // For each class in our microservice
        for (String micClass : micClasses) {
            System.out.printf("  %s has relationships with:\n", micClass);
            double sumInterDistances = 0.0;

            // Find all relationships for this class
            for (String[] relation : comp) {
                // Only consider relationships where this class is the target (incoming)
                if (relation[1].equals(micClass)) {
                    String otherClass = relation[0];
                    String relType = relation[2];

                    // Skip if otherClass is in our microservice
                    if (micClasses.contains(otherClass)) continue;

                    double weight = 1.0;
                    // If otherClass is in optimal microservice, divide by 2
                    if (optimalMicroservice.classes.contains(otherClass)) {
                        weight = 2.0;
                    }

                    double sim = Microservices.sim(micClass, otherClass);
                    double interDistance = Microservices.interDistance(relType, sim);
                    double weightedInterDistance = interDistance / weight;

                    System.out.printf("    - %s (%s, incoming): sim=%.6f, interDistance=%.6f, weighted=%.6f\n",
                            otherClass, relType, sim, interDistance, weightedInterDistance);

                    sumInterDistances += weightedInterDistance;
                }
            }

            // Average inter-distance for this class (divide by N2)
            double avgInterDistance = sumInterDistances / N2;
            totalInterDistance += avgInterDistance;
            System.out.printf("    Average inter-distance = %.6f\n", avgInterDistance);
        }

        // Calculate final ESC (divide by M)
        double avgInterDistance = totalInterDistance / M;
        double esc = BigDecimal.valueOf(1.0 - avgInterDistance)
                .setScale(6, RoundingMode.DOWN)
                .doubleValue();

        System.out.printf("Final ESC calculation:\n");
        System.out.printf("  Total inter-distance = %.6f\n", totalInterDistance);
        System.out.printf("  Average inter-distance = %.6f\n", avgInterDistance);
        System.out.printf("  ESC = 1 - %.6f = %.6f\n", avgInterDistance, esc);

        return esc;
    }

    public static void printSecondPhaseESC(String[][] comparison) {
        System.out.println("\nExternal Service Cohesion (ESC) Calculation - Second Phase");
        System.out.println("----------------------------------------------------");

        // Find max microservice number
        int maxMicNumber = 0;
        for (Integer micNumber : allMicroservices.keySet()) {
            maxMicNumber = Math.max(maxMicNumber, micNumber);
        }

        // Build the intra-similarity map for ISC calculation
        HashMap<String, HashMap<String, Double>> intraSimMap = new HashMap<>();
        for (String[] pair : comparison) {
            String class1 = pair[0];
            String class2 = pair[1];
            String relation = pair[2];
            double sim = sim(class1, class2);
            double intraSim = intraSim(relation, sim);

            intraSimMap.putIfAbsent(class1, new HashMap<>());
            intraSimMap.get(class1).put(class2, intraSim);
        }
        
        // Calculate ESC for each combined microservice (skip single class ones)
        allMetrics.clear(); // Clear previous metrics
        for (int micNumber = lastMicNumberFromFirstPhase + 1; micNumber <= maxMicNumber; micNumber++) {
            List<String> classes = getClassesInMicroservice(micNumber);
            if (!classes.isEmpty() && classes.size() > 1) {
                // Print microservice details
                System.out.printf("\nmic%d (Combined): %s\n", micNumber, String.join(" + ", classes));

                // Calculate ESC
                double esc = calculateSecondPhaseESC(micNumber, comparison);
                System.out.printf("ESC(mic%d) = %.6f\n", micNumber, esc);

                // Calculate ISC using the same method as printISC
                double totalIntraSim = 0.0;
                int pairs = 0;
                for (int i = 0; i < classes.size(); i++) {
                    for (int j = i + 1; j < classes.size(); j++) {
                        String class1 = classes.get(i);
                        String class2 = classes.get(j);

                        HashMap<String, Double> map1 = intraSimMap.get(class1);
                        HashMap<String, Double> map2 = intraSimMap.get(class2);

                        double intraSim12 = map1 != null ? map1.getOrDefault(class2, 0.0) : 0.0;
                        double intraSim21 = map2 != null ? map2.getOrDefault(class1, 0.0) : 0.0;

                        if (intraSim12 != 0.0 || intraSim21 != 0.0) {
                            totalIntraSim += (intraSim12 + intraSim21) / 2.0;
                            pairs++;
                        }
                    }
                }

                double isc = pairs == 0 ? 0.0 : BigDecimal.valueOf(totalIntraSim / pairs)
                        .setScale(6, RoundingMode.DOWN)
                        .doubleValue();

                // Store metrics
                allMetrics.add(new MicroserviceMetrics(micNumber, isc, esc, new ArrayList<>(classes)));
            }
        }
    }

}