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
    private static double iscThreshold = 0.079;
    private static double escThreshold = 0.70;

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
        String[][] currentComp = comp2;
        printSim(currentComp);
        printIntraSim(currentComp);
        printInterSim(currentComp);
        printISC(currentComp);
        printESC(currentComp);
        selectOptimalMicroservices();

        // Second clustering phase with ISC calculations
        secondClusteringPhase(currentComp);
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
                } else {
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
                }
            }
        }
        System.out.println();
    }

    public static double calculateESC(int micNumber, String[][] comp) {
        List<String> micClasses = getClassesInMicroservice(micNumber);
        if (micClasses.isEmpty()) return 0.0;

        // Find which initial microservices were used to create this microservice
        Set<Integer> usedMics = new HashSet<>();
        for (int i = 1; i <= 6; i++) {  // Check all initial microservices
            List<String> initialMicClasses = getClassesInMicroservice(i);
            if (!Collections.disjoint(micClasses, initialMicClasses)) {
                usedMics.add(i);
            }
        }

        // Group external classes by their relationships with our microservice
        Map<String, Map<String, String>> externalRelations = new HashMap<>();

        // For each relationship in comp
        for (String[] relation : comp) {
            String class1 = relation[0];
            String class2 = relation[1];
            String relType = relation[2];

            // Check if class1 is in our microservice and class2 is not
            if (micClasses.contains(class1) && !micClasses.contains(class2)) {
                // Find which initial microservice contains class2
                for (int i = 1; i <= 6; i++) {
                    if (!usedMics.contains(i) && getClassesInMicroservice(i).contains(class2)) {
                        // Only add the relationship if class2 is the source
                        boolean hasOppositeRelation = false;
                        for (String[] r : comp) {
                            if (r[0].equals(class2) && r[1].equals(class1)) {
                                hasOppositeRelation = true;
                                break;
                            }
                        }
                        if (hasOppositeRelation) {
                            externalRelations.computeIfAbsent(class2, k -> new HashMap<>())
                                    .put(class1, relType);
                        }
                        break;
                    }
                }
            }
            // Check if class2 is in our microservice and class1 is not
            else if (micClasses.contains(class2) && !micClasses.contains(class1)) {
                // Find which initial microservice contains class1
                for (int i = 1; i <= 6; i++) {
                    if (!usedMics.contains(i) && getClassesInMicroservice(i).contains(class1)) {
                        // Only add the relationship if class1 is the source
                        boolean hasOppositeRelation = false;
                        for (String[] r : comp) {
                            if (r[0].equals(class1) && r[1].equals(class2)) {
                                hasOppositeRelation = true;
                                break;
                            }
                        }
                        if (hasOppositeRelation) {
                            externalRelations.computeIfAbsent(class1, k -> new HashMap<>())
                                    .put(class2, relType);
                        }
                        break;
                    }
                }
            }
        }

        if (externalRelations.isEmpty()) return 0.0; // No external relations

        System.out.printf("\nCalculating ESC for mic%d:\n", micNumber);
        System.out.printf("Excluding microservices used in formation: %s\n", usedMics);
        System.out.println("External relationships found:");

        double totalInterDistance = 0.0;

        // Calculate inter-distance for each external class
        for (Map.Entry<String, Map<String, String>> entry : externalRelations.entrySet()) {
            String externalClass = entry.getKey();
            Map<String, String> relations = entry.getValue();

            System.out.printf("  %s has relationships with:\n", externalClass);

            // Calculate average relationship for this external class
            double sumInterDistances = 0.0;
            for (Map.Entry<String, String> rel : relations.entrySet()) {
                String micClass = rel.getKey();
                String relType = rel.getValue();

                double sim = Microservices.sim(micClass, externalClass);
                double interDistance = Microservices.interDistance(relType, sim);
                sumInterDistances += interDistance;

                System.out.printf("    - %s (%s): sim=%.6f, interDistance=%.6f\n",
                        micClass, relType, sim, interDistance);
            }

            double avgInterDistance = sumInterDistances / 2;
            totalInterDistance += avgInterDistance;

            System.out.printf("    Average inter-distance = %.6f\n", avgInterDistance);
        }

        // Calculate final ESC
        double avgInterDistance = totalInterDistance / externalRelations.size();
        double esc = BigDecimal.valueOf(1.0 - avgInterDistance)
                .setScale(6, RoundingMode.DOWN)
                .doubleValue();

        System.out.printf("Final ESC calculation:\n");
        System.out.printf("  Total inter-distance = %.6f\n", totalInterDistance);
        System.out.printf("  Number of related classes = %d\n", externalRelations.size());
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
    
    private static int getHighestMicNumber() {
        int maxMicNumber = 0;
        for (Integer micNumber : allMicroservices.keySet()) {
            maxMicNumber = Math.max(maxMicNumber, micNumber);
        }
        return maxMicNumber;
    }
    
    public static void printSecondISC(String[][] comparison) {
        System.out.println("\nSecond Iteration - Internal Service Cohesion (ISC) Calculation");
        System.out.println("--------------------------------------------------------");
        
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
        
        // Calculate ISC for each new microservice
        for (Map.Entry<Integer, List<String>> entry : allMicroservices.entrySet()) {
            int micNumber = entry.getKey();
            List<String> classes = entry.getValue();
            
            System.out.printf("\nmic%d:\n", micNumber);
            for (String className : classes) {
                System.out.printf("\tmic%d.%s\n", micNumber, className);
            }
            
            if (classes.size() == 2) {
                // For 2-class microservices, use regular ISC calculation
                String class1 = classes.get(0);
                String class2 = classes.get(1);
                
                HashMap<String, Double> map1 = intraSimMap.get(class1);
                HashMap<String, Double> map2 = intraSimMap.get(class2);
                
                double intraSim12 = map1 != null ? map1.getOrDefault(class2, 0.0) : 0.0;
                double intraSim21 = map2 != null ? map2.getOrDefault(class1, 0.0) : 0.0;
                
                double isc = (intraSim12 + intraSim21) / 2.0;
                System.out.printf("ISC = %.6f\n", isc);
                
            } else if (classes.size() == 3) {
                // For 3-class microservices, use the special formula
                String class1 = classes.get(0);
                String class2 = classes.get(1);
                String class3 = classes.get(2);
                
                // Get all intra-similarity values
                HashMap<String, Double> map1 = intraSimMap.get(class1);
                HashMap<String, Double> map2 = intraSimMap.get(class2);
                HashMap<String, Double> map3 = intraSimMap.get(class3);
                
                // Calculate pairs
                double intraSim12 = map1 != null ? map1.getOrDefault(class2, 0.0) : 0.0;
                double intraSim21 = map2 != null ? map2.getOrDefault(class1, 0.0) : 0.0;
                
                double intraSim13 = map1 != null ? map1.getOrDefault(class3, 0.0) : 0.0;
                double intraSim31 = map3 != null ? map3.getOrDefault(class1, 0.0) : 0.0;
                
                double intraSim23 = map2 != null ? map2.getOrDefault(class3, 0.0) : 0.0;
                double intraSim32 = map3 != null ? map3.getOrDefault(class2, 0.0) : 0.0;
                
                // Apply the formula: sum of (intraSim(i,j) + intraSim(j,i)) / (n * (n-1))
                double totalSum = (intraSim12 + intraSim21) + (intraSim13 + intraSim31) + (intraSim23 + intraSim32);
                double isc = totalSum / (3.0 * 2.0); // n=3, n*(n-1)=6
                
                System.out.printf("ISC = %.6f\n", isc);
            }
        }
    }
    
    public static void secondClusteringPhase(String[][] comparison) {
        System.out.println("\nClustering Phase");
        System.out.println("----------------");

        // Clear previous microservices
        allMicroservices.clear();

        // Start from the next number after first phase
        int nextMicNumber = lastMicNumberFromFirstPhase + 1;
        System.out.printf("Starting from mic%d (after first phase ended at mic%d)\n\n",
                nextMicNumber, lastMicNumberFromFirstPhase);

        // Get the unique classes from threshold-meeting microservices
        Set<String> uniqueClasses = new HashSet<>();
        for (MicroserviceMetrics metrics : allMetrics) {
            if (metrics.isc > iscThreshold && metrics.esc > escThreshold) {
                uniqueClasses.addAll(metrics.classes);
            }
        }
        List<String> classes = new ArrayList<>(uniqueClasses);
        Collections.sort(classes); // Sort to ensure consistent ordering

        // Create all possible pairs
        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                String class1 = classes.get(i);
                String class2 = classes.get(j);

                // Store the microservice
                List<String> newMicClasses = Arrays.asList(class1, class2);
                allMicroservices.put(nextMicNumber, newMicClasses);

                // Print the microservice
                System.out.printf("mic%d:\n", nextMicNumber);
                System.out.printf("\tmic%d.%s\n", nextMicNumber, class1);
                System.out.printf("\tmic%d.%s\n", nextMicNumber, class2);
                System.out.println();

                nextMicNumber++;
            }
        }

        // Create all possible combinations of 3 classes
        for (int i = 0; i < classes.size(); i++) {
            for (int j = i + 1; j < classes.size(); j++) {
                for (int k = j + 1; k < classes.size(); k++) {
                    String class1 = classes.get(i);
                    String class2 = classes.get(j);
                    String class3 = classes.get(k);

                    // Store the microservice
                    List<String> newMicClasses = Arrays.asList(class1, class2, class3);
                    allMicroservices.put(nextMicNumber, newMicClasses);

                    // Print the microservice
                    System.out.printf("mic%d:\n", nextMicNumber);
                    System.out.printf("\tmic%d.%s\n", nextMicNumber, class1);
                    System.out.printf("\tmic%d.%s\n", nextMicNumber, class2);
                    System.out.printf("\tmic%d.%s\n", nextMicNumber, class3);
                    System.out.println();

                    nextMicNumber++;
                }
            }
        }

        // Calculate ISC for all new microservices
        printSecondISC(comparison);
    }


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
        
        System.out.println("\nUnique classes from threshold-meeting microservices:");
        System.out.println("------------------------------------------------");
        for (String className : uniqueClasses) {
            System.out.println(className);
        }
        
        // Store unique classes for use in second iteration
        uniqueClassesFromFirstIteration.clear();
        uniqueClassesFromFirstIteration.addAll(uniqueClasses);

        // Find microservice that sub-optimizes both objectives
        MicroserviceMetrics bestMic = null;
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
                bestMic = current;
            }
        }

        if (bestMic != null) {
            System.out.printf("\nSelected optimal microservice: mic%d\n", bestMic.micNumber);
            System.out.println("------------------------------------");
            System.out.printf("ISC = %.6f, ESC = %.6f\n", bestMic.isc, bestMic.esc);
            System.out.println("Classes:");
            for (String className : bestMic.classes) {
                System.out.printf("mic%d.model.%s\n", bestMic.micNumber, className);
            }
        }
    }

}