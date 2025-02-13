package mainCalculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class MicDecomp {
    private static final Map<String, Set<String>> Llist = new HashMap<>();

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
    private static double escThreshold = 0.7;

    private static class MicroservicePair {
        Set<String> classes;
        double isc;
        double esc;

        MicroservicePair(Set<String> classes, double isc, double esc) {
            this.classes = classes;
            this.isc = isc;
            this.esc = esc;
        }

        double getCombinedScore() {
            return isc + esc;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (String cls : classes) {
                sb.append("[").append(cls).append("] ");
            }
            return String.format("%sISC = %.6f ESC = %.6f Combined = %.6f", 
                sb.toString(), isc, esc, getCombinedScore());
        }
    }

    public static void selectOptimalMicroservice() {
        List<MicroservicePair> pairs = new ArrayList<>();
        List<Map.Entry<String, Set<String>>> microservices = new ArrayList<>(Llist.entrySet());

        // Calculate metrics for each pair of microservices
        for (int i = 0; i < microservices.size(); i++) {
            for (int j = i + 1; j < microservices.size(); j++) {
                Set<String> combinedClasses = new HashSet<>();
                combinedClasses.addAll(microservices.get(i).getValue());
                combinedClasses.addAll(microservices.get(j).getValue());
                List<String> classes = new ArrayList<>(combinedClasses);

                // Calculate ISC
                double sumIntraSim = 0.0;
                for (int m = 0; m < classes.size(); m++) {
                    for (int n = 0; n < classes.size(); n++) {
                        if (m != n) {
                            String class1 = classes.get(m);
                            String class2 = classes.get(n);
                            for (String[] rel : relationships) {
                                if (rel[0].equals(class1) && rel[1].equals(class2)) {
                                    double similarity = sim(class1, class2);
                                    double intraSim = intraSim(rel[2], similarity);
                                    sumIntraSim += intraSim;
                                    break;
                                }
                            }
                        }
                    }
                }
                int numClasses = classes.size();
                double isc = 0.0;
                if (numClasses > 1) {
                    isc = sumIntraSim / (numClasses * (numClasses - 1));
                }

                // Calculate ESC using the corrected method
                // Get all other classes as potential optimal classes
                Set<String> otherClasses = new HashSet<>();
                for (Map.Entry<String, Set<String>> entry : Llist.entrySet()) {
                    if (!entry.getKey().equals(microservices.get(i).getKey()) && 
                        !entry.getKey().equals(microservices.get(j).getKey())) {
                        otherClasses.addAll(entry.getValue());
                    }
                }
                
                double esc = calculateESC(combinedClasses, otherClasses);

                // Create pair and add to list
                pairs.add(new MicroservicePair(combinedClasses, isc, esc));

                // Print details for this combination
                System.out.printf("\nAnalyzing combination of %s and %s:\n", 
                    microservices.get(i).getKey(), microservices.get(j).getKey());
                System.out.printf("Classes: %s\n", String.join(", ", combinedClasses));
                System.out.printf("ISC = %.6f\n", isc);
                System.out.printf("ESC = %.6f\n", esc);
            }
        }

        // Filter pairs that meet both thresholds
        List<MicroservicePair> validPairs = pairs.stream()
                .filter(p -> p.isc >= iscThreshold && p.esc >= escThreshold)
                .sorted((p1, p2) -> {
                    // First compare by ISC
                    int iscCompare = Double.compare(p2.isc, p1.isc);
                    if (iscCompare != 0) {
                        return iscCompare;
                    }
                    // If ISC is equal, compare by ESC
                    return Double.compare(p2.esc, p1.esc);
                })
                .collect(Collectors.toList());

        System.out.println("\n=== Optimal Microservice Analysis ===");
        System.out.printf("ISC Threshold: %.6f%n", iscThreshold);
        System.out.printf("ESC Threshold: %.6f%n", escThreshold);
        System.out.println("\nValid Combinations (sorted by ISC, then ESC):");

        if (validPairs.isEmpty()) {
            System.out.println("No combinations found that meet both thresholds");
        } else {
            for (MicroservicePair pair : validPairs) {
                System.out.println(pair);
            }
            System.out.println("\nOptimal combination:");
            System.out.println(validPairs.get(0));
        }
    }

    public static void main(String[] args) {
        Llist.clear();

        // Define relationships

        String[][] data = {
            {"Student", "DbStorage", "com"},
            {"Student", "FileStorage", "com"},
            {"DataParser", "DbStorage", "com"},
            {"Controller", "DbStorage", "dep"},
            {"DataParser", "FileStorage", "com"},
            {"Controller", "FileStorage", "dep"},
            {"Controller", "StorageType", "ass"}
        };

        String[][] data2 = {
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


        relationships = data;
        /**
        addMicroservice("mic1", "StorageType");
        addMicroservice("mic2", "FileStorage");
        addMicroservice("mic3", "Student");
        addMicroservice("mic4", "Controller");
        addMicroservice("mic5", "DbStorage");
        addMicroservice("mic6", "DataParser");
        **/

        addMicroservice("mic1", "StorageType");
        addMicroservice("mic2", "FileStorage");
        addMicroservice("mic3", "Student");
        addMicroservice("mic4", "Controller");
        addMicroservice("mic5", "DbStorage", "DataParser");

        /**
        addMicroservice("mic1", "StorageType");
        addMicroservice("mic2", "FileStorage","Student");
        addMicroservice("mic3", "Controller");
        addMicroservice("mic4", "DbStorage", "DataParser");
         **/

        //addMicroservice("mic1", "Controller");
        //addMicroservice("mic2", "Role");
        //addMicroservice("mic3", "Project", "Architect", "Architects");
        //addMicroservice("mic5", "Engineer","ProjectNumber", "Engineers");




        System.out.println("\n=== Internal Structural Cohesion (ISC) Analysis ===");
        calculateAllPairISC();

        System.out.println("\n=== External Structural Cohesion (ESC) Analysis ===");
        calculateAllESC();

        System.out.println("\n=== Analyzing combination Using (ESC and (ISC) ===");
        System.out.println();
        selectOptimalMicroservice();
    }

    public static void addMicroservice(String micName, String... classes) {
        Set<String> classSet = new HashSet<>();
        for (String cls : classes) {
            classSet.add(cls);
        }
        Llist.put(micName, classSet);
    }

    private static void collectClassesFromArray(String[][] data) {
        Set<String> uniqueClasses = new HashSet<>();
        for (String[] relation : data) {
            uniqueClasses.add(relation[0]);
            uniqueClasses.add(relation[1]);
        }

        relationships = data;

        int micCount = 1;
        for (String className : uniqueClasses) {
            Set<String> classes = new HashSet<>();
            classes.add(className);
            Llist.put("mic" + micCount++, classes);
        }
    }

    private static String[][] relationships;

    public static void calculateAllPairISC() {
        System.out.println("Stage 1:");

        // Get all microservices and their classes
        List<Map.Entry<String, Set<String>>> microservices = new ArrayList<>(Llist.entrySet());

        // Calculate ISC for combinations of microservices
        for (int i = 0; i < microservices.size(); i++) {
            for (int j = i + 1; j < microservices.size(); j++) {
                // Get classes from both microservices
                Set<String> combinedClasses = new HashSet<>();
                combinedClasses.addAll(microservices.get(i).getValue());
                combinedClasses.addAll(microservices.get(j).getValue());

                List<String> classes = new ArrayList<>(combinedClasses);

                // Calculate ISC for the combined classes
                double sumIntraSim = 0.0;

                // Calculate intraSim for each directed pair
                for (int m = 0; m < classes.size(); m++) {
                    for (int n = 0; n < classes.size(); n++) {
                        if (m != n) {
                            String class1 = classes.get(m);
                            String class2 = classes.get(n);

                            // Find relationship and calculate intraSim if it exists
                            for (String[] rel : relationships) {
                                if (rel[0].equals(class1) && rel[1].equals(class2)) {
                                    double similarity = sim(class1, class2);
                                    double intraSim = intraSim(rel[2], similarity);
                                    sumIntraSim += intraSim;
                                    break;
                                }
                            }
                        }
                    }
                }

                // Calculate ISC
                int numClasses = classes.size();
                double denominator = numClasses * (numClasses - 1);
                double isc = sumIntraSim / denominator;

                for (int k = 0; k < classes.size(); k++) {
                    if (k > 0) System.out.print(", ");
                    System.out.printf("[%s] ", classes.get(k));
                }

                // Show ISC calculation
                System.out.printf("ISC = %.6f%n", isc);
            }
        }
    }

    public static double sim(String class1, String class2) {
        HashMap<Character, Integer> freq1 = new HashMap<>();
        HashMap<Character, Integer> freq2 = new HashMap<>();

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

        for (char c : allChars) {
            int count1 = freq1.getOrDefault(c, 0);
            int count2 = freq2.getOrDefault(c, 0);

            commonCount += Math.min(count1, count2);
            distinctCount += Math.max(count1, count2);
        }
        double result = (double) commonCount / distinctCount;
        return BigDecimal.valueOf(result).setScale(6, RoundingMode.DOWN).doubleValue();
    }

    public static double intraSim(String rel, double sim) {
        double result = intraWeight.get(rel) * sim;
        return BigDecimal.valueOf(result).setScale(6, RoundingMode.DOWN).doubleValue();
    }

    public static double interDistance(String rel, double sim) {
        double result = interWeight.get(rel) * (1 - sim);
        return BigDecimal.valueOf(result).setScale(6, RoundingMode.DOWN).doubleValue();
    }

    private static double calculateESC(Set<String> targetClasses, Set<String> optimalClasses) {
        // First, identify all microservices that have relationships with our target classes
        Map<String, Set<String>> clientMicroservices = new HashMap<>(); // micName -> classes
        
        // Find all client classes and their microservices
        for (String[] rel : relationships) {
            if (targetClasses.contains(rel[1]) && !targetClasses.contains(rel[0])) {
                // Find which microservice this client belongs to
                for (Map.Entry<String, Set<String>> entry : Llist.entrySet()) {
                    if (entry.getValue().contains(rel[0])) {
                        clientMicroservices.put(entry.getKey(), entry.getValue());
                        break;
                    }
                }
            }
        }

        if (clientMicroservices.isEmpty()) {
            System.out.printf("No incoming relationships found, ESC = 0.0\n");
            return 0.0;
        }

        int M = targetClasses.size();
        int N2 = clientMicroservices.size(); // Number of client microservices

        System.out.printf("M (classes in microservice) = %d\n", M);
        System.out.printf("N2 (external microservices with relationships) = %d\n", N2);

        // Calculate sum for each class in our microservice
        double totalSum = 0.0;

        // For each class in our microservice (M)
        for (String targetClass : targetClasses) {
            System.out.printf("  %s has relationships with:\n", targetClass);
            double sumMicroserviceAvgs = 0.0;

            // For each client microservice
            for (Map.Entry<String, Set<String>> clientMic : clientMicroservices.entrySet()) {
                String micName = clientMic.getKey();
                Set<String> clientClasses = clientMic.getValue();
                double micSum = 0.0;
                int micRelCount = 0;

                System.out.printf("    Microservice %s:\n", micName);
                // Calculate average inter-distance with all classes in this microservice
                for (String clientClass : clientClasses) {
                    for (String[] rel : relationships) {
                        if (rel[0].equals(clientClass) && rel[1].equals(targetClass)) {
                            double sim = sim(clientClass, targetClass);
                            double interDist = interDistance(rel[2], sim);
                            micSum += interDist;
                            micRelCount++;
                            System.out.printf("      - %s (%s): sim=%.6f, interDist=%.6f\n",
                                clientClass, rel[2], sim, interDist);
                        }
                    }
                }

                // Calculate average for this microservice
                double micAvg = micRelCount > 0 ? micSum / clientClasses.size() : 0.0;
                System.out.printf("      Average for microservice %s = %.6f\n", micName, micAvg);
                sumMicroserviceAvgs += micAvg;
            }

            // Calculate average for this target class (divide by N2)
            double classAvg = N2 > 0 ? sumMicroserviceAvgs / N2 : 0.0;
            System.out.printf("    Average for %s = %.6f\n", targetClass, classAvg);
            totalSum += classAvg;
        }

        // Calculate final ESC (divide by M and subtract from 1)
        double avgInterDistance = M > 0 ? totalSum / M : 0.0;
        double esc = 1.0 - avgInterDistance;

        System.out.printf("Final ESC calculation:\n");
        System.out.printf("  Total sum = %.6f\n", totalSum);
        System.out.printf("  Average inter-distance = %.6f\n", avgInterDistance);
        System.out.printf("  ESC = 1 - %.6f = %.6f\n", avgInterDistance, esc);

        return BigDecimal.valueOf(esc)
            .setScale(6, RoundingMode.DOWN)
            .doubleValue();
    }

    public static void calculateAllESC() {
        System.out.println("\nCalculating ESC for all microservice combinations:");

        List<Map.Entry<String, Set<String>>> microservices = new ArrayList<>(Llist.entrySet());

        // Calculate ESC for each pair of microservices
        for (int i = 0; i < microservices.size(); i++) {
            for (int j = i + 1; j < microservices.size(); j++) {
                Map.Entry<String, Set<String>> mic1 = microservices.get(i);
                Map.Entry<String, Set<String>> mic2 = microservices.get(j);
                
                Set<String> combinedClasses = new HashSet<>();
                combinedClasses.addAll(mic1.getValue());
                combinedClasses.addAll(mic2.getValue());

                System.out.printf("\nCalculating ESC for combined %s and %s:\n", 
                    mic1.getKey(), mic2.getKey());
                System.out.printf("Considering relationships with other microservices\n");

                // Get N2 classes
                Set<String> n2Classes = new HashSet<>();
                for (String[] rel : relationships) {
                    if (combinedClasses.contains(rel[1]) && !combinedClasses.contains(rel[0])) {
                        n2Classes.add(rel[0]);
                    }
                }

                if (!n2Classes.isEmpty()) {
                    System.out.printf("M (classes in microservice) = %d\n", combinedClasses.size());
                    System.out.printf("N2 (external classes with incoming relationships) = %d\n", n2Classes.size());
                    
                    // Get all other classes as potential optimal classes
                    Set<String> otherClasses = new HashSet<>();
                    for (Map.Entry<String, Set<String>> entry : Llist.entrySet()) {
                        if (!entry.getKey().equals(mic1.getKey()) && !entry.getKey().equals(mic2.getKey())) {
                            otherClasses.addAll(entry.getValue());
                        }
                    }
                    
                    double esc = calculateESC(combinedClasses, otherClasses);
                    System.out.printf("ESC(%s+%s) = %.6f\n", 
                        mic1.getKey(), mic2.getKey(), esc);
                } else {
                    System.out.println("No incoming relationships found, ESC = 0.000000");
                }
            }
        }
    }

    // Print to view
    public static void printSim(String[][] comparison) {
        System.out.println("\nSimilarity Calculation");
        System.out.println("----------------------");
        for (String[] pair : comparison) {
            System.out.printf("[%s], [%s] = %.6f\n", pair[0], pair[1], sim(pair[0], pair[1]));
        }
        System.out.println();
    }

    public static void printIntraSim(String[][] comparison) {
        System.out.println("IntraSimilarity Calculation");
        System.out.println("---------------------------");
        for (String[] pair : comparison) {
            System.out.printf("[%s], [%s] = %.6f\n", pair[0], pair[1], intraSim(pair[2], sim(pair[0], pair[1])));
        }
        System.out.println();
    }

    public static void printInterDistance(String[][] comparison) {
        System.out.println("InterSimilarity Calculation");
        System.out.println("---------------------------");
        for (String[] pair : comparison) {
            System.out.printf("[%s], [%s] = %.6f\n", pair[0], pair[1], interDistance(pair[2], sim(pair[0], pair[1])));
        }
        System.out.println();
    }

}
