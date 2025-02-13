package mainCalculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
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
    private static double iscThreshold = 0.79;
    private static double escThreshold = 0.7;

    public static void main(String[] args) {
        // Clear the Llist before starting
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
        relationships = data;

        // Define microservices
        addMicroservice("mic1", "StorageType");
        addMicroservice("mic2", "Student");
        addMicroservice("mic3", "FileStorage");
        addMicroservice("mic4", "Controller");
        addMicroservice("mic5", "DbStorage");
        addMicroservice("mic6", "DataParser");

        // Calculate and print ISC for all possible pairs
        System.out.println("\n=== Internal Structural Cohesion (ISC) Analysis ===");
        calculateAllPairISC();

        // Calculate and print ESC for all possible pairs
        System.out.println("\n=== External Structural Cohesion (ESC) Analysis ===");
        calculateAllESC();

        // Set thresholds
        iscThreshold = 0.079;
        escThreshold = 0.7;
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
    
    private static List<String> getIncomingClasses(Set<String> targetClasses) {
        List<String> incomingClasses = new ArrayList<>();
        for (String[] rel : relationships) {
            if (targetClasses.contains(rel[1]) && !targetClasses.contains(rel[0])) {
                if (!incomingClasses.contains(rel[0])) {
                    incomingClasses.add(rel[0]);
                }
            }
        }
        return incomingClasses;
    }
    
    private static double calculateSumInterDistance(String incomingClass, Set<String> targetClasses) {
        double sumInterDistance = 0.0;
        
        for (String targetClass : targetClasses) {
            // Find relationship if it exists
            String relationType = null;
            for (String[] rel : relationships) {
                if (rel[0].equals(incomingClass) && rel[1].equals(targetClass)) {
                    relationType = rel[2];
                    break;
                }
            }
            
            if (relationType != null) {
                double similarity = sim(incomingClass, targetClass);
                sumInterDistance += interDistance(relationType, similarity);
            } else {
                sumInterDistance += 0.0; // No relationship means zero contribution
            }
        }
        
        return sumInterDistance;
    }
    
    public static void calculateAllESC() {
        System.out.println("\nCalculating ESC for all microservice combinations:");

        // Get all microservices
        List<Map.Entry<String, Set<String>>> microservices = new ArrayList<>(Llist.entrySet());

        // Calculate ESC for each pair of microservices
        for (int i = 0; i < microservices.size(); i++) {
            for (int j = i + 1; j < microservices.size(); j++) {
                Set<String> combinedClasses = new HashSet<>();
                combinedClasses.addAll(microservices.get(i).getValue());
                combinedClasses.addAll(microservices.get(j).getValue());

                // Get incoming classes (excluding classes in the combined microservice)
                List<String> incomingClasses = getIncomingClasses(combinedClasses);

                if (!incomingClasses.isEmpty()) {
                    double totalSum = 0.0;

                    // For each incoming class, calculate sum of its interDistances
                    for (String incomingClass : incomingClasses) {
                        double sumForClass = calculateSumInterDistance(incomingClass, combinedClasses);
                        totalSum += sumForClass;
                    }

                    // Calculate ESC according to the formula:
                    // esc = 1 - (sum of all interDistances / number of target classes) / number of incoming classes
                    double esc = 1.0 - (totalSum / combinedClasses.size()) / incomingClasses.size();
                    esc = BigDecimal.valueOf(esc).setScale(6, RoundingMode.DOWN).doubleValue();

                    // Print results in the requested format
                    StringBuilder output = new StringBuilder();
                    for (String cls : combinedClasses) {
                        output.append("[").append(cls).append("] ");
                    }
                    output.append("ESC = %.6f%n");
                    System.out.printf(output.toString(), esc);
                    
                    System.out.print("Incoming classes: ");
                    for (String cls : incomingClasses) {
                        System.out.printf("[%s] ", cls);
                    }
                    System.out.println();
                    System.out.println();
                } else {
                    // For cases with no incoming relationships
                    StringBuilder output = new StringBuilder();
                    for (String cls : combinedClasses) {
                        output.append("[").append(cls).append("] ");
                    }
                    output.append("ESC = 0.000000");
                    System.out.println(output.toString());
                    System.out.println("No incoming relationships found");
                    System.out.println();
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
