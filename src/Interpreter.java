import java.util.*;

public class Interpreter {
    private Map<String, Integer> memory = new HashMap<>();

    public void execute(List<Token> tokens) {
        System.out.println("--- Starting Execution ---");
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).type == TokenType.KEYWORD) {
                String name = tokens.get(i + 1).value;
                int val = Integer.parseInt(tokens.get(i + 3).value);
                memory.put(name, val);
                System.out.println("Set variable: " + name + " to " + val);
                i += 3;
            }
        }
        System.out.println("Memory State: " + memory);
    }
}