import java.util.*;

public class StaticAnalyzer {
    public boolean analyze(List<Token> tokens) {
        Set<String> declaredVariables = new HashSet<>();
        boolean hasError = false;
        int i = 0;

        while (i < tokens.size() && tokens.get(i).type != TokenType.EOF) {
            Token current = tokens.get(i);

            if (isKeyword(current, "int")) {
                if (!isToken(tokens, i + 1, TokenType.IDENTIFIER)) {
                    hasError = reportError("Expected variable name after 'int'.");
                    break;
                }

                String variableName = tokens.get(i + 1).value;
                if (declaredVariables.contains(variableName)) {
                    hasError = reportError("Variable '" + variableName + "' is already declared.");
                } else {
                    declaredVariables.add(variableName);
                }

                i += 2;
                if (!isToken(tokens, i, TokenType.ASSIGN)) {
                    hasError = reportError("Expected '=' after variable declaration of '" + variableName + "'.");
                    break;
                }
                i++;
                i = consumeExpression(tokens, i, declaredVariables);
                if (i == -1) return false;
                if (!isToken(tokens, i, TokenType.SEMICOLON)) {
                    return reportError("Missing ';' after declaration of '" + variableName + "'.");
                }
                i++;
                continue;
            }

            if (isKeyword(current, "print")) {
                i++;
                i = consumeExpression(tokens, i, declaredVariables);
                if (i == -1) return false;
                if (!isToken(tokens, i, TokenType.SEMICOLON)) {
                    return reportError("Missing ';' after print statement.");
                }
                i++;
                continue;
            }

            if (current.type == TokenType.IDENTIFIER) {
                if (!declaredVariables.contains(current.value)) {
                    hasError = reportError("Variable '" + current.value + "' used without being declared with 'int'.");
                }
                if (!isToken(tokens, i + 1, TokenType.ASSIGN)) {
                    hasError = reportError("Expected '=' after variable '" + current.value + "'.");
                    break;
                }
                i += 2;
                i = consumeExpression(tokens, i, declaredVariables);
                if (i == -1) return false;
                if (!isToken(tokens, i, TokenType.SEMICOLON)) {
                    return reportError("Missing ';' after assignment to '" + current.value + "'.");
                }
                i++;
                continue;
            }

            return reportError("Unexpected token '" + current.value + "'.");
        }

        return !hasError;
    }

    private int consumeExpression(List<Token> tokens, int index, Set<String> declaredVariables) {
        int balance = 0;
        boolean expectsOperand = true;

        while (index < tokens.size()) {
            Token token = tokens.get(index);
            if (token.type == TokenType.SEMICOLON && balance == 0) {
                return expectsOperand ? fail("Incomplete expression before ';'.") : index;
            }

            if (token.type == TokenType.LPAREN) {
                if (!expectsOperand) {
                    return fail("Missing operator before '('.");
                }
                balance++;
                index++;
                continue;
            }

            if (token.type == TokenType.RPAREN) {
                if (balance == 0) {
                    return fail("Closing ')' without matching '('.");
                }
                balance--;
                index++;
                expectsOperand = false;
                continue;
            }

            if (token.type == TokenType.IDENTIFIER) {
                if (!expectsOperand) {
                    return fail("Missing operator before variable '" + token.value + "'.");
                }
                if (!declaredVariables.contains(token.value)) {
                    return fail("Variable '" + token.value + "' used before declaration.");
                }
                index++;
                expectsOperand = false;
                continue;
            }

            if (token.type == TokenType.NUMBER) {
                if (!expectsOperand) {
                    return fail("Missing operator before number '" + token.value + "'.");
                }
                index++;
                expectsOperand = false;
                continue;
            }

            if (isOperator(token.type)) {
                if (expectsOperand && token.type != TokenType.MINUS) {
                    return fail("Operator '" + token.value + "' cannot appear here.");
                }
                if (!expectsOperand && token.type == TokenType.MINUS) {
                    index++;
                    expectsOperand = true;
                    continue;
                }
                if (expectsOperand) {
                    index++;
                    continue;
                }
                index++;
                expectsOperand = true;
                continue;
            }

            return fail("Invalid token '" + token.value + "' inside expression.");
        }

        return fail("Unexpected end of tokens while reading expression.");
    }

    private boolean isOperator(TokenType type) {
        return type == TokenType.PLUS || type == TokenType.MINUS || type == TokenType.STAR || type == TokenType.SLASH;
    }

    private boolean isKeyword(Token token, String value) {
        return token.type == TokenType.KEYWORD && token.value.equals(value);
    }

    private boolean isToken(List<Token> tokens, int index, TokenType type) {
        return index < tokens.size() && tokens.get(index).type == type;
    }

    private boolean reportError(String message) {
        System.err.println("[STATIC ANALYSIS ERROR] " + message);
        return true;
    }

    private int fail(String message) {
        reportError(message);
        return -1;
    }
}
