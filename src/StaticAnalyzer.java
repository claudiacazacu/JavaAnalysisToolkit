import java.util.*;

public class StaticAnalyzer {
    private final Map<String, ValueType> declaredVariables = new HashMap<>();
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
        if (checkDeclarationKeyword()) {
            ValueType declaredType = ValueType.fromKeyword(advance().value);
            Token variable = consume(TokenType.IDENTIFIER, "Expected variable name after type keyword.");
            if (declaredVariables.containsKey(variable.value)) {
                throw error(variable, "Variable '" + variable.value + "' is already declared.");
            }
            consume(TokenType.ASSIGN, "Expected '=' after variable declaration.");
            ValueType expressionType = parseExpression();
            ensureAssignable(declaredType, expressionType, variable);
            consume(TokenType.SEMICOLON, "Missing ';' after declaration.");
            declaredVariables.put(variable.value, declaredType);
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
            if (matchKeyword("else")) {
                parseBlock();
            }
            return;
        }

        if (matchKeyword("while")) {
            consume(TokenType.LPAREN, "Expected '(' after 'while'.");
            parseCondition();
            consume(TokenType.RPAREN, "Expected ')' after while condition.");
            parseBlock();
            return;
        }

        if (matchKeyword("else")) {
            throw error(previous(), "Unexpected 'else' without a matching 'if'.");
        }

        if (check(TokenType.IDENTIFIER)) {
            Token variable = advance();
            ValueType declaredType = requireDeclaredVariable(variable);
            consume(TokenType.ASSIGN, "Expected '=' after variable name.");
            ValueType expressionType = parseExpression();
            ensureAssignable(declaredType, expressionType, variable);
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
        parseConjunction();
        while (match(TokenType.OR_OR)) {
            parseConjunction();
        }
    }

    private void parseConjunction() {
        parseConditionAtom();
        while (match(TokenType.AND_AND)) {
            parseConditionAtom();
        }
    }

    private void parseConditionAtom() {
        Token conditionToken = peek();
        ValueType leftType = parseExpression();
        if (isComparisonOperator(peek().type)) {
            Token operator = advance();
            ValueType rightType = parseExpression();
            validateComparison(operator, leftType, rightType);
            return;
        }

        if (leftType == ValueType.STRING) {
            throw error(conditionToken, "String expressions cannot be used as conditions.");
        }
    }

    private ValueType parseExpression() {
        ValueType type = parseTerm();
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            Token operator = previous();
            ValueType rightType = parseTerm();
            type = mergeAdditiveTypes(operator, type, rightType);
        }
        return type;
    }

    private ValueType parseTerm() {
        ValueType type = parseUnary();
        while (match(TokenType.STAR) || match(TokenType.SLASH) || match(TokenType.PERCENT)) {
            Token operator = previous();
            ValueType rightType = parseUnary();
            if (type != ValueType.INT || rightType != ValueType.INT) {
                throw error(operator, "Operators '*', '/', and '%' only support 'int' operands.");
            }
            type = ValueType.INT;
        }
        return type;
    }

    private ValueType parseUnary() {
        if (match(TokenType.BANG)) {
            Token operator = previous();
            ValueType operandType = parseUnary();
            if (operandType != ValueType.BOOL) {
                throw error(operator, "Unary '!' only supports 'bool' operands.");
            }
            return ValueType.BOOL;
        }

        if (match(TokenType.MINUS)) {
            Token operator = previous();
            ValueType operandType = parseUnary();
            if (operandType != ValueType.INT) {
                throw error(operator, "Unary '-' only supports 'int' operands.");
            }
            return ValueType.INT;
        }
        return parsePrimary();
    }

    private ValueType parsePrimary() {
        if (match(TokenType.NUMBER)) {
            return ValueType.INT;
        }

        if (match(TokenType.STRING_LITERAL)) {
            return ValueType.STRING;
        }

        if (match(TokenType.BOOLEAN_LITERAL)) {
            return ValueType.BOOL;
        }

        if (match(TokenType.IDENTIFIER)) {
            Token identifier = previous();
            return requireDeclaredVariable(identifier);
        }

        if (match(TokenType.LPAREN)) {
            ValueType type = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return type;
        }

        throw error(peek(), "Expected a literal, variable, or parenthesized expression.");
    }

    private ValueType mergeAdditiveTypes(Token operator, ValueType leftType, ValueType rightType) {
        if (operator.type == TokenType.PLUS && leftType == ValueType.STRING && rightType == ValueType.STRING) {
            return ValueType.STRING;
        }

        if (leftType == ValueType.INT && rightType == ValueType.INT) {
            return ValueType.INT;
        }

        throw error(
                operator,
                "Operator '" + operator.value + "' does not support operands of type '" + leftType.keyword()
                        + "' and '" + rightType.keyword() + "'."
        );
    }

    private void validateComparison(Token operator, ValueType leftType, ValueType rightType) {
        if (operator.type == TokenType.EQUAL_EQUAL || operator.type == TokenType.BANG_EQUAL) {
            if (leftType != rightType) {
                throw error(operator, "Equality comparisons require both operands to have the same type.");
            }
            return;
        }

        if (leftType != ValueType.INT || rightType != ValueType.INT) {
            throw error(operator, "Relational comparisons only support 'int' operands.");
        }
    }

    private void ensureAssignable(ValueType targetType, ValueType expressionType, Token token) {
        if (targetType != expressionType) {
            throw error(
                    token,
                    "Cannot assign expression of type '" + expressionType.keyword()
                            + "' to variable of type '" + targetType.keyword() + "'."
            );
        }
    }

    private ValueType requireDeclaredVariable(Token variable) {
        ValueType variableType = declaredVariables.get(variable.value);
        if (variableType == null) {
            throw error(variable, "Variable '" + variable.value + "' used before declaration.");
        }
        return variableType;
    }

    private boolean checkDeclarationKeyword() {
        return check(TokenType.KEYWORD)
                && (peek().value.equals("int") || peek().value.equals("bool") || peek().value.equals("string"));
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
