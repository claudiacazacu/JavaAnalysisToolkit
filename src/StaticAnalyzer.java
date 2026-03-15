import java.util.*;

public class StaticAnalyzer {
    public boolean analyze(List<Token> tokens) {
        Set<String> declaredVariables = new HashSet<>();
        boolean hasError = false;

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            if (t.type == TokenType.IDENTIFIER) {
                if (i > 0 && tokens.get(i - 1).type == TokenType.KEYWORD) {
                    declaredVariables.add(t.value);
                } else if (!declaredVariables.contains(t.value)) {
                    System.err.println("[STATIC ANALYSIS ERROR] Variable '" + t.value + "' used without being declared with 'int'!");
                    hasError = true;
                }
            }
        }
        return !hasError;
    }
}