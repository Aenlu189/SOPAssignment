package part5;

//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MicroserviceDecomposition {

    // To represent a singleton microservice for each class.
    static class Microservice{
        String mic;
        String fullClassPath;

        public Microservice(String mic, String fullClassPath){
            this.mic = mic;
            this.fullClassPath = fullClassPath;
        }

        @Override
        public String toString(){
            return mic + ":\n\t" + mic + "." + fullClassPath;
        }
    }

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

    public static double intraSim(String rel, double sim) {
        return intraWeight.get(rel) * sim;
    }

    public static double interDistance(String rel, double sim) {
        return (interWeight.get(rel) * (1 - sim));
    }

    public static List<MicroserviceDecomposition.Microservice> initPhase(String[] singlaton) {
        List<MicroserviceDecomposition.Microservice> microservices = new ArrayList<>();
        Set<String> uniqueClasses = new LinkedHashSet<>(Arrays.asList(singlaton)); // Convert array to Set

        int micCounter = 1;
        for (String className : uniqueClasses) {
            String packageName = getPackageForClass(className);
            String fullClassPath = packageName + "." + className;
            MicroserviceDecomposition.Microservice microservice =
                    new MicroserviceDecomposition.Microservice("mic" + micCounter, fullClassPath);

            microservices.add(microservice);
            micCounter++;
        }

        return microservices;
    }

    private static String getPackageForClass(String className) {

        switch(className){
            case "Controller":
            case "Role":
                return "part1";
            case "Architect":
            case "Architects":
                return "part2";
            case "Engineer":
            case "Engineers":
                return "part3";
            case "Project":
            case "ProjectNumber":
                return "part4";
            default:
                return "unknownPackage.";
        }
    }

    public static double isc(String class1, String class2, HashMap<String, HashMap<String, Double>> intraSimMap) {
        HashMap<String, Double> map1 = intraSimMap.get(class1);
        HashMap<String, Double> map2 = intraSimMap.get(class2);

        double intraSim12 = map1 != null ? map1.getOrDefault(class2, 0.0) : 0.0;
        double intraSim21 = map2 != null ? map2.getOrDefault(class1, 0.0) : 0.0;

        if (intraSim12 == 0.0 && intraSim21 == 0.0) {
            return 0.0;
        }
        BigDecimal result = BigDecimal.valueOf(intraSim12 + intraSim21).divide(BigDecimal.valueOf(2), 6, RoundingMode.DOWN);

        return result.doubleValue();
    }

    public static double esc(String class1, String class2, HashMap<String, HashMap<String, Double>> interSimMap, int M) {
        double interSim12 = interSimMap.getOrDefault(class1, new HashMap<>()).getOrDefault(class2, 0.0);
        double interSim21 = interSimMap.getOrDefault(class2, new HashMap<>()).getOrDefault(class1, 0.0);

        if (interSim12 == 0.0 && interSim21 == 0.0) {
            return 0.0;
        }

        double sumInterSim = interSim12 + interSim21;
        double escValue = 1 - (sumInterSim / (M - 1));

        return BigDecimal.valueOf(escValue)
                .setScale(6, RoundingMode.DOWN)
                .doubleValue();
    }

    public static void main(String[] args) {

        String[] compFiles = {"Controller", "Role", "Architect", "Architects","Engineer", "Engineers","Project", "ProjectNumber"};

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
        printIniti(compFiles);
        printISC(comp2);
        //printESC(comp2);

    }

    public static void printIniti(String[] singlaton){

        List<MicroserviceDecomposition.Microservice> microservicesComp = initPhase(singlaton);

        System.out.println("Initialized Microservices:");
        System.out.println("--------------------------");
        for (MicroserviceDecomposition.Microservice m : microservicesComp) {
            System.out.println(m);
        }
        System.out.println();
    }

    public static void printISC(String[][] comparison){

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

        HashSet<String> uniqueEntities = new HashSet<>();

        for (String[] pair: comparison) {
            uniqueEntities.add(pair[0]);
            uniqueEntities.add(pair[1]);
        }

        System.out.println("Internal Cohesion Calculation");
        System.out.println("-----------------------------");
        ArrayList<String> entityList = new ArrayList<>(uniqueEntities);
        for (int i=0; i<entityList.size(); i++){
            for (int j=i+1; j<entityList.size(); j++) {
                String class1 = entityList.get(i);
                String class2 = entityList.get(j);
                double isc = isc(class1, class2, intraSimMap);
                System.out.printf("Internal cohesion of %s and %s = %.6f\n", class1, class2, isc);
            }
        }
        System.out.println();
    }

    public static void printESC(String[][] comparison) {
        HashMap<String, HashMap<String, Double>> interSimMap = new HashMap<>();

        for (String[] pair : comparison) {
            String class1 = pair[0];
            String class2 = pair[1];
            String relation = pair[2];
            double sim = sim(class1, class2);
            double interSim = interDistance(relation, sim);

            interSimMap.putIfAbsent(class1, new HashMap<>());
            interSimMap.putIfAbsent(class2, new HashMap<>());
            interSimMap.get(class1).put(class2, interSim);
            interSimMap.get(class2).put(class1, interSim);
        }

        HashSet<String> uniqueEntities = new HashSet<>();
        for (String[] pair : comparison) {
            uniqueEntities.add(pair[0]);
            uniqueEntities.add(pair[1]);
        }

        System.out.println("External Cohesion Calculation");
        System.out.println("-----------------------------");
        ArrayList<String> entityList = new ArrayList<>(uniqueEntities);
        int M = entityList.size();
        for (int i = 0; i < entityList.size(); i++) {
            for (int j = i + 1; j < entityList.size(); j++) {
                String class1 = entityList.get(i);
                String class2 = entityList.get(j);
                double esc = esc(class1, class2, interSimMap, M);
                System.out.printf("External cohesion of %s and %s = %.6f\n", class1, class2, esc);
            }
        }
        System.out.println();
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