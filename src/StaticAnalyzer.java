import java.util.*;

public class StaticAnalyzer {
    private final Set<String> declaredVariables = new HashSet<>();
    private List<Token> tokens;
    private int pos;

    public boolean analyze(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        declaredVariables.clear();

        try {
            while (!check(TokenType.EOF)) {
                parseStatement();
            }
            return true;
        } catch (IllegalStateException exception) {
            System.err.println(exception.getMessage());
            return false;
        }
    }

    private void parseStatement() {
        if (matchKeyword("int")) {
            Token variable = consume(TokenType.IDENTIFIER, "Expected variable name after 'int'.");
            if (!declaredVariables.add(variable.value)) {
                throw error(variable, "Variable '" + variable.value + "' is already declared.");
            }
            consume(TokenType.ASSIGN, "Expected '=' after variable declaration.");
            parseExpression();
            consume(TokenType.SEMICOLON, "Missing ';' after declaration.");
            return;
        }

        if (matchKeyword("print")) {
            parseExpression();
            consume(TokenType.SEMICOLON, "Missing ';' after print statement.");
            return;
        }

        if (matchKeyword("if")) {
            consume(TokenType.LPAREN, "Expected '(' after 'if'.");
            parseCondition();
            consume(TokenType.RPAREN, "Expected ')' after if condition.");
            parseBlock();
            return;
        }

        if (matchKeyword("while")) {
            consume(TokenType.LPAREN, "Expected '(' after 'while'.");
            parseCondition();
            consume(TokenType.RPAREN, "Expected ')' after while condition.");
            parseBlock();
            return;
        }

        if (check(TokenType.IDENTIFIER)) {
            Token variable = advance();
            if (!declaredVariables.contains(variable.value)) {
                throw error(variable, "Variable '" + variable.value + "' used before declaration.");
            }
            consume(TokenType.ASSIGN, "Expected '=' after variable name.");
            parseExpression();
            consume(TokenType.SEMICOLON, "Missing ';' after assignment.");
            return;
        }

        throw error(peek(), "Unexpected token '" + peek().value + "'.");
    }

    private void parseBlock() {
        consume(TokenType.LBRACE, "Expected '{' to start a block.");
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            parseStatement();
        }
        consume(TokenType.RBRACE, "Expected '}' to close the block.");
    }

    private void parseCondition() {
        parseExpression();
        if (isComparisonOperator(peek().type)) {
            advance();
            parseExpression();
        }
    }

    private void parseExpression() {
        parseTerm();
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            parseTerm();
        }
    }

    private void parseTerm() {
        parseUnary();
        while (match(TokenType.STAR) || match(TokenType.SLASH)) {
            parseUnary();
        }
    }

    private void parseUnary() {
        if (match(TokenType.MINUS)) {
            parseUnary();
            return;
        }
        parsePrimary();
    }

    private void parsePrimary() {
        if (match(TokenType.NUMBER)) {
            return;
        }

        if (match(TokenType.IDENTIFIER)) {
            Token identifier = previous();
            if (!declaredVariables.contains(identifier.value)) {
                throw error(identifier, "Variable '" + identifier.value + "' used before declaration.");
            }
            return;
        }

        if (match(TokenType.LPAREN)) {
            parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return;
        }

        throw error(peek(), "Expected a number, variable, or parenthesized expression.");
    }

    private boolean isComparisonOperator(TokenType type) {
        return type == TokenType.EQUAL_EQUAL
                || type == TokenType.BANG_EQUAL
                || type == TokenType.LESS
                || type == TokenType.LESS_EQUAL
                || type == TokenType.GREATER
                || type == TokenType.GREATER_EQUAL;
    }

    private boolean matchKeyword(String value) {
        if (check(TokenType.KEYWORD) && peek().value.equals(value)) {
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
        throw error(peek(), message);
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

    private IllegalStateException error(Token token, String message) {
        return new IllegalStateException(
                "[STATIC ANALYSIS ERROR] line " + token.line + ", column " + token.column + ": " + message
        );
    }
}
