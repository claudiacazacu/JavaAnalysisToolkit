import java.nio.file.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filePath = args.length > 0 ? args[0] : "data/program.txt";

        try {
            String code = Files.readString(Path.of(filePath));
            System.out.println("Reading code from: " + filePath + "\n---");
            System.out.println(code);
            System.out.println("---");

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
            System.err.println("Execution failed: " + e.getMessage());
            System.err.println("Make sure the selected input file exists and contains valid code.");
        }
    }
}
