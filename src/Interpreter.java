import java.util.*;

public class Interpreter {
    private final Map<String, RuntimeValue> memory = new LinkedHashMap<>();
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
        if (checkDeclarationKeyword()) {
            ValueType declaredType = ValueType.fromKeyword(advance().value);
            String name = consume(TokenType.IDENTIFIER, "Expected variable name after type keyword.").value;
            consume(TokenType.ASSIGN, "Expected '=' after variable declaration.");
            RuntimeValue value = parseExpression();
            ensureAssignable(declaredType, value, previous());
            consume(TokenType.SEMICOLON, "Expected ';' after declaration.");
            memory.put(name, value);
            System.out.println("Declared variable: " + name + " (" + declaredType.keyword() + ") = " + value.displayValue());
            return;
        }

        if (matchKeyword("print")) {
            RuntimeValue value = parseExpression();
            consume(TokenType.SEMICOLON, "Expected ';' after print statement.");
            System.out.println("Print => " + value.displayValue());
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
            Token identifier = advance();
            String name = identifier.value;
            consume(TokenType.ASSIGN, "Expected '=' after variable name.");
            RuntimeValue value = parseExpression();
            ensureAssignable(requireDeclaredValue(identifier).type(), value, identifier);
            consume(TokenType.SEMICOLON, "Expected ';' after assignment.");
            memory.put(name, value);
            System.out.println("Updated variable: " + name + " = " + value.displayValue());
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
        int nextPos = blockEnd + 1;
        boolean hasElse = isKeywordAt(nextPos, "else");

        if (evaluateCondition(conditionStart, conditionEnd)) {
            executeRange(blockStart + 1, blockEnd);
            pos = hasElse ? findElseBranchEnd(nextPos) : nextPos;
            return;
        }

        if (!hasElse) {
            pos = nextPos;
            return;
        }

        int elseStart = nextPos + 1;
        if (isKeywordAt(elseStart, "if")) {
            pos = elseStart + 1;
            executeIf();
            return;
        }

        int elseBlockEnd = findBlockEnd(elseStart);
        executeRange(elseStart + 1, elseBlockEnd);
        pos = elseBlockEnd + 1;
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

    private int findElseBranchEnd(int elseTokenPos) {
        int elseStart = elseTokenPos + 1;
        if (isKeywordAt(elseStart, "if")) {
            return findIfStatementEnd(elseStart);
        }
        return findBlockEnd(elseStart) + 1;
    }

    private int findIfStatementEnd(int ifTokenPos) {
        if (!isKeywordAt(ifTokenPos, "if")) {
            throw error(tokens.get(ifTokenPos), "Expected 'if' to start an else-if branch.");
        }

        int conditionStart = ifTokenPos + 1;
        if (conditionStart >= tokens.size() || tokens.get(conditionStart).type != TokenType.LPAREN) {
            throw error(tokens.get(ifTokenPos), "Expected '(' after 'if'.");
        }

        int conditionEnd = findMatchingRightParen(conditionStart);
        int blockStart = conditionEnd + 1;
        int blockEnd = findBlockEnd(blockStart);
        int nextPos = blockEnd + 1;
        return isKeywordAt(nextPos, "else") ? findElseBranchEnd(nextPos) : nextPos;
    }

    private int findMatchingRightParen(int leftParenPos) {
        int depth = 0;
        for (int i = leftParenPos; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.LPAREN) {
                depth++;
            } else if (token.type == TokenType.RPAREN) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        throw error(tokens.get(leftParenPos), "Missing ')' to close condition.");
    }

    private boolean evaluateCondition(int startInclusive, int endExclusive) {
        ExpressionCursor cursor = new ExpressionCursor(startInclusive, endExclusive);
        boolean result = evaluateConditionConjunction(cursor);
        while (cursor.match(TokenType.OR_OR)) {
            boolean next = evaluateConditionConjunction(cursor);
            result = result || next;
        }
        if (cursor.hasNext()) {
            throw error(cursor.peek(), "Unexpected token in condition.");
        }
        return result;
    }

    private boolean evaluateConditionConjunction(ExpressionCursor cursor) {
        boolean result = evaluateConditionAtom(cursor);
        while (cursor.match(TokenType.AND_AND)) {
            boolean next = evaluateConditionAtom(cursor);
            result = result && next;
        }
        return result;
    }

    private boolean evaluateConditionAtom(ExpressionCursor cursor) {
        RuntimeValue left = parseExpression(cursor);
        if (cursor.checkComparisonOperator()) {
            Token operator = cursor.advance();
            RuntimeValue right = parseExpression(cursor);
            return compare(left, right, operator);
        }
        if (left.type() == ValueType.BOOL) {
            return left.asBoolean();
        }
        if (left.type() == ValueType.INT) {
            return left.asInt() != 0;
        }
        throw error("String expressions cannot be used as conditions.");
    }

    private boolean compare(RuntimeValue left, RuntimeValue right, Token operator) {
        if (left.type() != right.type()) {
            throw error(operator, "Comparison operands must have the same type.");
        }

        return switch (operator.type) {
            case EQUAL_EQUAL -> left.value().equals(right.value());
            case BANG_EQUAL -> !left.value().equals(right.value());
            case LESS -> compareIntegers(left, right, operator, (a, b) -> a < b);
            case LESS_EQUAL -> compareIntegers(left, right, operator, (a, b) -> a <= b);
            case GREATER -> compareIntegers(left, right, operator, (a, b) -> a > b);
            case GREATER_EQUAL -> compareIntegers(left, right, operator, (a, b) -> a >= b);
            default -> throw error(operator, "Invalid comparison operator '" + operator.value + "'.");
        };
    }

    private RuntimeValue parseExpression() {
        RuntimeValue value = parseTerm();
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            Token operator = previous();
            RuntimeValue right = parseTerm();
            value = applyAdditiveOperator(value, right, operator);
        }
        return value;
    }

    private void parseCondition() {
        parseConditionConjunction();
        while (match(TokenType.OR_OR)) {
            parseConditionConjunction();
        }
    }

    private void parseConditionConjunction() {
        parseConditionAtom();
        while (match(TokenType.AND_AND)) {
            parseConditionAtom();
        }
    }

    private void parseConditionAtom() {
        parseExpression();
        if (isComparisonOperator(peek().type)) {
            advance();
            parseExpression();
        }
    }

    private RuntimeValue parseTerm() {
        RuntimeValue value = parseFactor();
        while (match(TokenType.STAR) || match(TokenType.SLASH) || match(TokenType.PERCENT)) {
            Token operator = previous();
            RuntimeValue right = parseFactor();
            value = applyMultiplicativeOperator(value, right, operator);
        }
        return value;
    }

    private RuntimeValue parseFactor() {
        if (match(TokenType.BANG)) {
            Token operator = previous();
            RuntimeValue value = parseFactor();
            if (value.type() != ValueType.BOOL) {
                throw error(operator, "Unary '!' only supports 'bool' operands.");
            }
            return new RuntimeValue(ValueType.BOOL, !value.asBoolean());
        }

        if (match(TokenType.MINUS)) {
            Token operator = previous();
            RuntimeValue value = parseFactor();
            if (value.type() != ValueType.INT) {
                throw error(operator, "Unary '-' only supports 'int' operands.");
            }
            return new RuntimeValue(ValueType.INT, -value.asInt());
        }
        if (match(TokenType.NUMBER)) {
            return new RuntimeValue(ValueType.INT, Integer.parseInt(previous().value));
        }
        if (match(TokenType.STRING_LITERAL)) {
            return new RuntimeValue(ValueType.STRING, previous().value);
        }
        if (match(TokenType.BOOLEAN_LITERAL)) {
            return new RuntimeValue(ValueType.BOOL, Boolean.parseBoolean(previous().value));
        }
        if (isBuiltinLenCall()) {
            Token lenToken = advance();
            consume(TokenType.LPAREN, "Expected '(' after 'len'.");
            RuntimeValue argument = parseExpression();
            if (argument.type() != ValueType.STRING) {
                throw error(lenToken, "Builtin 'len' only accepts 'string' expressions.");
            }
            consume(TokenType.RPAREN, "Expected ')' after len argument.");
            return new RuntimeValue(ValueType.INT, argument.asString().length());
        }
        if (match(TokenType.IDENTIFIER)) {
            return requireDeclaredValue(previous());
        }
        if (match(TokenType.LPAREN)) {
            RuntimeValue value = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return value;
        }
        throw error("Expected a literal, variable, or parenthesized expression.");
    }

    private RuntimeValue parseExpression(ExpressionCursor cursor) {
        RuntimeValue value = parseTerm(cursor);
        while (cursor.match(TokenType.PLUS) || cursor.match(TokenType.MINUS)) {
            Token operator = cursor.previous();
            RuntimeValue right = parseTerm(cursor);
            value = applyAdditiveOperator(value, right, operator);
        }
        return value;
    }

    private RuntimeValue parseTerm(ExpressionCursor cursor) {
        RuntimeValue value = parseFactor(cursor);
        while (cursor.match(TokenType.STAR) || cursor.match(TokenType.SLASH) || cursor.match(TokenType.PERCENT)) {
            Token operator = cursor.previous();
            RuntimeValue right = parseFactor(cursor);
            value = applyMultiplicativeOperator(value, right, operator);
        }
        return value;
    }

    private RuntimeValue parseFactor(ExpressionCursor cursor) {
        if (cursor.match(TokenType.BANG)) {
            Token operator = cursor.previous();
            RuntimeValue value = parseFactor(cursor);
            if (value.type() != ValueType.BOOL) {
                throw error(operator, "Unary '!' only supports 'bool' operands.");
            }
            return new RuntimeValue(ValueType.BOOL, !value.asBoolean());
        }

        if (cursor.match(TokenType.MINUS)) {
            Token operator = cursor.previous();
            RuntimeValue value = parseFactor(cursor);
            if (value.type() != ValueType.INT) {
                throw error(operator, "Unary '-' only supports 'int' operands.");
            }
            return new RuntimeValue(ValueType.INT, -value.asInt());
        }
        if (cursor.match(TokenType.NUMBER)) {
            return new RuntimeValue(ValueType.INT, Integer.parseInt(cursor.previous().value));
        }
        if (cursor.match(TokenType.STRING_LITERAL)) {
            return new RuntimeValue(ValueType.STRING, cursor.previous().value);
        }
        if (cursor.match(TokenType.BOOLEAN_LITERAL)) {
            return new RuntimeValue(ValueType.BOOL, Boolean.parseBoolean(cursor.previous().value));
        }
        if (cursor.isBuiltinLenCall()) {
            Token lenToken = cursor.advance();
            cursor.consume(TokenType.LPAREN, "Expected '(' after 'len'.");
            RuntimeValue argument = parseExpression(cursor);
            if (argument.type() != ValueType.STRING) {
                throw error(lenToken, "Builtin 'len' only accepts 'string' expressions.");
            }
            cursor.consume(TokenType.RPAREN, "Expected ')' after len argument.");
            return new RuntimeValue(ValueType.INT, argument.asString().length());
        }
        if (cursor.match(TokenType.IDENTIFIER)) {
            return requireDeclaredValue(cursor.previous());
        }
        if (cursor.match(TokenType.LPAREN)) {
            RuntimeValue value = parseExpression(cursor);
            cursor.consume(TokenType.RPAREN, "Expected ')' after expression.");
            return value;
        }
        throw error(cursor.peek(), "Expected a literal, variable, or parenthesized expression.");
    }

    private RuntimeValue applyAdditiveOperator(RuntimeValue left, RuntimeValue right, Token operator) {
        if (operator.type == TokenType.PLUS && left.type() == ValueType.STRING && right.type() == ValueType.STRING) {
            return new RuntimeValue(ValueType.STRING, left.asString() + right.asString());
        }

        if (left.type() == ValueType.INT && right.type() == ValueType.INT) {
            int result = operator.type == TokenType.PLUS ? left.asInt() + right.asInt() : left.asInt() - right.asInt();
            return new RuntimeValue(ValueType.INT, result);
        }

        throw error(
                operator,
                "Operator '" + operator.value + "' does not support operands of type '" + left.type().keyword()
                        + "' and '" + right.type().keyword() + "'."
        );
    }

    private RuntimeValue applyMultiplicativeOperator(RuntimeValue left, RuntimeValue right, Token operator) {
        if (left.type() != ValueType.INT || right.type() != ValueType.INT) {
            throw error(operator, "Operators '*', '/', and '%' only support 'int' operands.");
        }

        if ((operator.type == TokenType.SLASH || operator.type == TokenType.PERCENT) && right.asInt() == 0) {
            throw error(operator, "Division or modulo by zero is not allowed.");
        }

        int result = switch (operator.type) {
            case STAR -> left.asInt() * right.asInt();
            case SLASH -> left.asInt() / right.asInt();
            case PERCENT -> left.asInt() % right.asInt();
            default -> throw error(operator, "Invalid multiplicative operator '" + operator.value + "'.");
        };
        return new RuntimeValue(ValueType.INT, result);
    }

    private boolean compareIntegers(RuntimeValue left, RuntimeValue right, Token operator, IntComparator comparator) {
        if (left.type() != ValueType.INT || right.type() != ValueType.INT) {
            throw error(operator, "Relational comparisons only support 'int' operands.");
        }
        return comparator.compare(left.asInt(), right.asInt());
    }

    private void ensureAssignable(ValueType targetType, RuntimeValue value, Token token) {
        if (targetType != value.type()) {
            throw error(
                    token,
                    "Cannot assign value of type '" + value.type().keyword()
                            + "' to variable of type '" + targetType.keyword() + "'."
            );
        }
    }

    private RuntimeValue requireDeclaredValue(Token token) {
        RuntimeValue value = memory.get(token.value);
        if (value == null) {
            throw error(token, "Variable '" + token.value + "' does not have a runtime value.");
        }
        return value;
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

    private boolean isKeywordAt(int index, String keyword) {
        return index < tokens.size()
                && tokens.get(index).type == TokenType.KEYWORD
                && tokens.get(index).value.equals(keyword);
    }

    private boolean isBuiltinLenCall() {
        return check(TokenType.IDENTIFIER)
                && peek().value.equals("len")
                && checkNext(TokenType.LPAREN);
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

    private boolean checkNext(TokenType type) {
        return pos + 1 < tokens.size() && tokens.get(pos + 1).type == type;
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

    @FunctionalInterface
    private interface IntComparator {
        boolean compare(int left, int right);
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

        private boolean checkComparisonOperator() {
            return hasNext() && isComparisonOperator(peek().type);
        }

        private boolean isBuiltinLenCall() {
            return check(TokenType.IDENTIFIER)
                    && peek().value.equals("len")
                    && checkNext(TokenType.LPAREN);
        }

        private boolean check(TokenType type) {
            return hasNext() && peek().type == type;
        }

        private boolean checkNext(TokenType type) {
            return cursorPos + 1 < endExclusive && tokens.get(cursorPos + 1).type == type;
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
