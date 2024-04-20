import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Log4JScanner {
    private static final List<String> vulnerableVersions = Arrays.asList(
            "2.0-beta9", "2.10.0", "2.14.1"
    );

    private static final Pattern LOG4J_JAR_PATTERN = Pattern.compile("log4j-core-(\\d+\\.\\d+\\.\\d+)(?:-[^.]+)?\\.jar");

    public List<String> findVulnerableLog4jJars(File directory) {
        List<String> foundVulnerableJars = new ArrayList<>();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        foundVulnerableJars.addAll(findVulnerableLog4jJars(file));
                    } else {
                        Matcher matcher = LOG4J_JAR_PATTERN.matcher(file.getName());
                        if (matcher.find()) {
                            String version = matcher.group(1);
                            if (vulnerableVersions.contains(version)) {
                                foundVulnerableJars.add(file.getAbsolutePath() + " contains vulnerable version: " + version);
                            }
                        }
                    }
                }
            }
        }
        return foundVulnerableJars;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java EnhancedStaticCodeAnalyzer <path_to_scan>");
            System.exit(1);
        }
        File directoryToScan = new File(args[0]);
        Log4JScanner analyzer = new Log4JScanner();
        List<String> vulnerableFiles = analyzer.findVulnerableLog4jJars(directoryToScan);
        if (vulnerableFiles.isEmpty()) {
            System.out.println("No vulnerable Log4j versions found.");
        } else {
            System.out.println("Vulnerable Log4j files found:");
            vulnerableFiles.forEach(System.out::println);
        }
    }
}
