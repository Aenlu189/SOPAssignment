package part5;

import java.util.HashMap;
import java.util.HashSet;

public class MicroserviceDecomposition {
    private static final HashMap<String, Double> intraWeight = new HashMap<>();
    static {
        intraWeight.put("com", 0.8);
        intraWeight.put("agg", 0.6);
        intraWeight.put("ass", 0.4);
        intraWeight.put("dep", 0.2);
    }
    public static final HashMap<String, Double> interWeight = new HashMap<>();
    static {
        interWeight.put("com", 0.2);
        interWeight.put("agg", 0.4);
        interWeight.put("ass", 0.8);
        interWeight.put("dep", 1.0);
    }

    public static double sim(String class1, String class2) {
        HashMap<Character, Integer> freq1 = new HashMap<>();
        HashMap<Character, Integer> freq2 = new HashMap<>();

        /*
        example: SSS
        freq1 will put S first but since in freq1 there's none of S yet, it will be freq1.put(S, 0+1);
        fre1 will put S again but since there's one, it will be freq1.put(S, 2); so on...
         */
        for (int i=0; i<class1.length(); i++) {
            freq1.put(class1.charAt(i), freq1.getOrDefault(class1.charAt(i), 0) + 1);
        }

        for (int i=0; i<class2.length(); i++) {
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
        for (char c: allChars){
            int count1 = freq1.getOrDefault(c, 0);
            int count2 = freq2.getOrDefault(c, 0);

            commonCount += Math.min(count1, count2);
            distinctCount += Math.max(count1, count2);
        }
        return (double) commonCount / distinctCount;
    }

    public static double intraSim(String rel, double sim) {
        return (double)  intraWeight.get(rel) * sim;
    }

    public static double interDistance(String rel, double sim) {
        return (double) (interWeight.get(rel) * (1-sim));
    }

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
                {"DbStorage", "Student", "com"},
                {"FileStorage", "Student", "com"},
                {"DataParser", "DbStorage", "com"},
                {"Controller", "DbStorage", "dep"},
                {"DataParser", "FileStorage", "com"},
                {"Controller", "FileStorage", "dep"},
                {"StorageType", "Controller", "ass"}
        };

        printSim(comp2);
        printIntraSim(comp2);
        printInterSim(comp2);
    }

    public static void printSim(String[][] comparison) {
        System.out.println("Similarity Calculation");
        System.out.println("----------------------");
        for (String[] pair : comparison) {
            System.out.printf("Similarity of %s, %s = %.6f\n", pair[0], pair[1], sim(pair[0], pair[1]));
        }
        System.out.println();
    }

    public static void printIntraSim(String[][] comparison) {
        System.out.println("IntraSimilarity Calculation");
        System.out.println("---------------------------");
        for (String[] pair : comparison) {
            System.out.printf("Intrasimilarity of %s, %s = %.6f\n", pair[0], pair[1], intraSim(pair[2], sim(pair[0], pair[1])));
        }
        System.out.println();
    }

    public static void printInterSim(String[][] comparison) {
        System.out.println("InterSimilarity Calculation");
        System.out.println("---------------------------");
        for (String[] pair : comparison) {
            System.out.printf("Intersimilarity of %s, %s = %.6f\n", pair[0], pair[1], interDistance(pair[2], sim(pair[0], pair[1])));
        }
        System.out.println();
    }
}