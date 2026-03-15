import java.nio.file.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = "data/program.txt";

        try {
            String code = Files.readString(Path.of(filePath));
            System.out.println("Reading code from: " + filePath + "\n---");

            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.tokenize();
            System.out.println("Tokens: " + tokens);

            StaticAnalyzer analyzer = new StaticAnalyzer();
            if (analyzer.analyze(tokens)) {
                System.out.println("[STATIC ANALYSIS] Success: No obvious errors found.");

                Interpreter interpreter = new Interpreter();
                interpreter.execute(tokens);
            } else {
                System.out.println("[INTERPRETER] Execution aborted due to static errors.");
            }

        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.err.println("Make sure you have a folder named 'data' with 'program.txt' inside.");
        }
    }
}