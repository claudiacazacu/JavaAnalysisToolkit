import java.util.*;

public class Interpreter {
    private final Map<String, Integer> memory = new LinkedHashMap<>();
    private List<Token> tokens;
    private int pos;

    public void execute(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        System.out.println("--- Starting Execution ---");

        while (!check(TokenType.EOF)) {
            if (matchKeyword("int")) {
                String name = consume(TokenType.IDENTIFIER, "Expected variable name after 'int'.").value;
                consume(TokenType.ASSIGN, "Expected '=' after variable declaration.");
                int value = parseExpression();
                consume(TokenType.SEMICOLON, "Expected ';' after declaration.");
                memory.put(name, value);
                System.out.println("Declared variable: " + name + " = " + value);
            } else if (matchKeyword("print")) {
                int value = parseExpression();
                consume(TokenType.SEMICOLON, "Expected ';' after print statement.");
                System.out.println("Print => " + value);
            } else if (check(TokenType.IDENTIFIER)) {
                String name = advance().value;
                consume(TokenType.ASSIGN, "Expected '=' after variable name.");
                int value = parseExpression();
                consume(TokenType.SEMICOLON, "Expected ';' after assignment.");
                memory.put(name, value);
                System.out.println("Updated variable: " + name + " = " + value);
            } else {
                throw error("Unexpected token '" + peek().value + "'.");
            }
        }

        System.out.println("Memory State: " + memory);
    }

    private int parseExpression() {
        int value = parseTerm();
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            Token operator = previous();
            int right = parseTerm();
            value = operator.type == TokenType.PLUS ? value + right : value - right;
        }
        return value;
    }

    private int parseTerm() {
        int value = parseFactor();
        while (match(TokenType.STAR) || match(TokenType.SLASH)) {
            Token operator = previous();
            int right = parseFactor();
            if (operator.type == TokenType.STAR) {
                value *= right;
            } else {
                if (right == 0) {
                    throw error("Division by zero is not allowed.");
                }
                value /= right;
            }
        }
        return value;
    }

    private int parseFactor() {
        if (match(TokenType.MINUS)) {
            return -parseFactor();
        }
        if (match(TokenType.NUMBER)) {
            return Integer.parseInt(previous().value);
        }
        if (match(TokenType.IDENTIFIER)) {
            String name = previous().value;
            if (!memory.containsKey(name)) {
                throw error("Variable '" + name + "' does not have a runtime value.");
            }
            return memory.get(name);
        }
        if (match(TokenType.LPAREN)) {
            int value = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return value;
        }
        throw error("Expected a number, variable, or parenthesized expression.");
    }

    private boolean matchKeyword(String keyword) {
        if (check(TokenType.KEYWORD) && peek().value.equals(keyword)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(message);
    }

    private boolean check(TokenType type) {
        return peek().type == type;
    }

    private Token advance() {
        if (!check(TokenType.EOF)) {
            pos++;
        }
        return previous();
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private IllegalStateException error(String message) {
        return new IllegalStateException("[INTERPRETER ERROR] " + message);
    }
}
