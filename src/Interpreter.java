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
            executeStatement();
        }

        System.out.println("Memory State: " + memory);
    }

    private void executeStatement() {
        if (matchKeyword("int")) {
            String name = consume(TokenType.IDENTIFIER, "Expected variable name after 'int'.").value;
            consume(TokenType.ASSIGN, "Expected '=' after variable declaration.");
            int value = parseExpression();
            consume(TokenType.SEMICOLON, "Expected ';' after declaration.");
            memory.put(name, value);
            System.out.println("Declared variable: " + name + " = " + value);
            return;
        }

        if (matchKeyword("print")) {
            int value = parseExpression();
            consume(TokenType.SEMICOLON, "Expected ';' after print statement.");
            System.out.println("Print => " + value);
            return;
        }

        if (matchKeyword("if")) {
            executeIf();
            return;
        }

        if (matchKeyword("while")) {
            executeWhile();
            return;
        }

        if (check(TokenType.IDENTIFIER)) {
            String name = advance().value;
            consume(TokenType.ASSIGN, "Expected '=' after variable name.");
            int value = parseExpression();
            consume(TokenType.SEMICOLON, "Expected ';' after assignment.");
            memory.put(name, value);
            System.out.println("Updated variable: " + name + " = " + value);
            return;
        }

        throw error(peek(), "Unexpected token '" + peek().value + "'.");
    }

    private void executeIf() {
        consume(TokenType.LPAREN, "Expected '(' after 'if'.");
        int conditionStart = pos;
        parseCondition();
        int conditionEnd = pos;
        consume(TokenType.RPAREN, "Expected ')' after if condition.");

        int blockStart = pos;
        int blockEnd = findBlockEnd(blockStart);
        if (evaluateCondition(conditionStart, conditionEnd)) {
            executeRange(blockStart + 1, blockEnd);
        }
        pos = blockEnd + 1;
    }

    private void executeWhile() {
        consume(TokenType.LPAREN, "Expected '(' after 'while'.");
        int conditionStart = pos;
        parseCondition();
        int conditionEnd = pos;
        consume(TokenType.RPAREN, "Expected ')' after while condition.");

        int blockStart = pos;
        int blockEnd = findBlockEnd(blockStart);
        while (evaluateCondition(conditionStart, conditionEnd)) {
            executeRange(blockStart + 1, blockEnd);
        }
        pos = blockEnd + 1;
    }

    private void executeRange(int startInclusive, int endExclusive) {
        int savedPos = pos;
        pos = startInclusive;
        while (pos < endExclusive) {
            executeStatement();
        }
        pos = savedPos;
    }

    private int findBlockEnd(int blockStart) {
        if (tokens.get(blockStart).type != TokenType.LBRACE) {
            throw error(tokens.get(blockStart), "Expected '{' to start a block.");
        }

        int depth = 0;
        for (int i = blockStart; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.LBRACE) {
                depth++;
            } else if (token.type == TokenType.RBRACE) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        throw error(tokens.get(blockStart), "Missing '}' to close the block.");
    }

    private boolean evaluateCondition(int startInclusive, int endExclusive) {
        ExpressionCursor cursor = new ExpressionCursor(startInclusive, endExclusive);
        int left = parseExpression(cursor);
        if (cursor.hasNext()) {
            Token operator = cursor.advance();
            int right = parseExpression(cursor);
            if (cursor.hasNext()) {
                throw error(cursor.peek(), "Unexpected token in condition.");
            }
            return compare(left, right, operator);
        }
        return left != 0;
    }

    private boolean compare(int left, int right, Token operator) {
        return switch (operator.type) {
            case EQUAL_EQUAL -> left == right;
            case BANG_EQUAL -> left != right;
            case LESS -> left < right;
            case LESS_EQUAL -> left <= right;
            case GREATER -> left > right;
            case GREATER_EQUAL -> left >= right;
            default -> throw error(operator, "Invalid comparison operator '" + operator.value + "'.");
        };
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

    private void parseCondition() {
        parseExpression();
        if (isComparisonOperator(peek().type)) {
            advance();
            parseExpression();
        }
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

    private int parseExpression(ExpressionCursor cursor) {
        int value = parseTerm(cursor);
        while (cursor.match(TokenType.PLUS) || cursor.match(TokenType.MINUS)) {
            Token operator = cursor.previous();
            int right = parseTerm(cursor);
            value = operator.type == TokenType.PLUS ? value + right : value - right;
        }
        return value;
    }

    private int parseTerm(ExpressionCursor cursor) {
        int value = parseFactor(cursor);
        while (cursor.match(TokenType.STAR) || cursor.match(TokenType.SLASH)) {
            Token operator = cursor.previous();
            int right = parseFactor(cursor);
            if (operator.type == TokenType.STAR) {
                value *= right;
            } else {
                if (right == 0) {
                    throw error(operator, "Division by zero is not allowed.");
                }
                value /= right;
            }
        }
        return value;
    }

    private int parseFactor(ExpressionCursor cursor) {
        if (cursor.match(TokenType.MINUS)) {
            return -parseFactor(cursor);
        }
        if (cursor.match(TokenType.NUMBER)) {
            return Integer.parseInt(cursor.previous().value);
        }
        if (cursor.match(TokenType.IDENTIFIER)) {
            String name = cursor.previous().value;
            if (!memory.containsKey(name)) {
                throw error(cursor.previous(), "Variable '" + name + "' does not have a runtime value.");
            }
            return memory.get(name);
        }
        if (cursor.match(TokenType.LPAREN)) {
            int value = parseExpression(cursor);
            cursor.consume(TokenType.RPAREN, "Expected ')' after expression.");
            return value;
        }
        throw error(cursor.peek(), "Expected a number, variable, or parenthesized expression.");
    }

    private boolean isComparisonOperator(TokenType type) {
        return type == TokenType.EQUAL_EQUAL
                || type == TokenType.BANG_EQUAL
                || type == TokenType.LESS
                || type == TokenType.LESS_EQUAL
                || type == TokenType.GREATER
                || type == TokenType.GREATER_EQUAL;
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
        return error(peek(), message);
    }

    private IllegalStateException error(Token token, String message) {
        return new IllegalStateException(
                "[INTERPRETER ERROR] line " + token.line + ", column " + token.column + ": " + message
        );
    }

    private final class ExpressionCursor {
        private final int endExclusive;
        private int cursorPos;

        private ExpressionCursor(int startInclusive, int endExclusive) {
            this.cursorPos = startInclusive;
            this.endExclusive = endExclusive;
        }

        private boolean hasNext() {
            return cursorPos < endExclusive;
        }

        private boolean match(TokenType type) {
            if (check(type)) {
                cursorPos++;
                return true;
            }
            return false;
        }

        private Token consume(TokenType type, String message) {
            if (check(type)) {
                return advance();
            }
            throw error(peek(), message);
        }

        private boolean check(TokenType type) {
            return hasNext() && peek().type == type;
        }

        private Token advance() {
            cursorPos++;
            return previous();
        }

        private Token peek() {
            if (!hasNext()) {
                return tokens.get(endExclusive - 1);
            }
            return tokens.get(cursorPos);
        }

        private Token previous() {
            return tokens.get(cursorPos - 1);
        }
    }
}
