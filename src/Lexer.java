import java.util.*;

public class Lexer {
    private final String input;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char curr = input.charAt(pos);
            if (Character.isWhitespace(curr)) {
                advanceWhitespace(curr);
                continue;
            }

            if (curr == '/' && peekNext() == '/') {
                skipLineComment();
                continue;
            }

            if (curr == '/' && peekNext() == '*') {
                skipBlockComment();
                continue;
            }

            if (Character.isDigit(curr)) {
                int startLine = line;
                int startColumn = column;
                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    sb.append(input.charAt(pos));
                    advance();
                }
                tokens.add(new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn));
            } else if (curr == '"') {
                tokens.add(readStringLiteral());
            } else if (Character.isLetter(curr)) {
                int startLine = line;
                int startColumn = column;
                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
                    sb.append(input.charAt(pos));
                    advance();
                }
                String val = sb.toString();
                TokenType type = isBooleanLiteral(val)
                        ? TokenType.BOOLEAN_LITERAL
                        : (isKeyword(val) ? TokenType.KEYWORD : TokenType.IDENTIFIER);
                tokens.add(new Token(type, val, startLine, startColumn));
            } else if (curr == '=' && peekNext() == '=') {
                tokens.add(token(TokenType.EQUAL_EQUAL, "=="));
                advance();
                advance();
            } else if (curr == '!' && peekNext() == '=') {
                tokens.add(token(TokenType.BANG_EQUAL, "!="));
                advance();
                advance();
            } else if (curr == '&' && peekNext() == '&') {
                tokens.add(token(TokenType.AND_AND, "&&"));
                advance();
                advance();
            } else if (curr == '!') {
                tokens.add(token(TokenType.BANG, "!"));
                advance();
            } else if (curr == '<' && peekNext() == '=') {
                tokens.add(token(TokenType.LESS_EQUAL, "<="));
                advance();
                advance();
            } else if (curr == '>' && peekNext() == '=') {
                tokens.add(token(TokenType.GREATER_EQUAL, ">="));
                advance();
                advance();
            } else if (curr == '=') {
                tokens.add(token(TokenType.ASSIGN, "="));
                advance();
            } else if (curr == '<') {
                tokens.add(token(TokenType.LESS, "<"));
                advance();
            } else if (curr == '>') {
                tokens.add(token(TokenType.GREATER, ">"));
                advance();
            } else if (curr == '+') {
                tokens.add(token(TokenType.PLUS, "+"));
                advance();
            } else if (curr == '-') {
                tokens.add(token(TokenType.MINUS, "-"));
                advance();
            } else if (curr == '*') {
                tokens.add(token(TokenType.STAR, "*"));
                advance();
            } else if (curr == '/') {
                tokens.add(token(TokenType.SLASH, "/"));
                advance();
            } else if (curr == '%') {
                tokens.add(token(TokenType.PERCENT, "%"));
                advance();
            } else if (curr == '(') {
                tokens.add(token(TokenType.LPAREN, "("));
                advance();
            } else if (curr == ')') {
                tokens.add(token(TokenType.RPAREN, ")"));
                advance();
            } else if (curr == '{') {
                tokens.add(token(TokenType.LBRACE, "{"));
                advance();
            } else if (curr == '}') {
                tokens.add(token(TokenType.RBRACE, "}"));
                advance();
            } else if (curr == ';') {
                tokens.add(token(TokenType.SEMICOLON, ";"));
                advance();
            } else {
                throw new IllegalArgumentException(
                        "Unexpected character '" + curr + "' at line " + line + ", column " + column
                );
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private boolean isKeyword(String value) {
        return value.equals("int")
                || value.equals("bool")
                || value.equals("string")
                || value.equals("print")
                || value.equals("if")
                || value.equals("else")
                || value.equals("while");
    }

    private boolean isBooleanLiteral(String value) {
        return value.equals("true") || value.equals("false");
    }

    private Token token(TokenType type, String value) {
        return new Token(type, value, line, column);
    }

    private Token readStringLiteral() {
        int startLine = line;
        int startColumn = column;
        advance();

        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char curr = input.charAt(pos);
            if (curr == '"') {
                advance();
                return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startColumn);
            }

            if (curr == '\\') {
                advance();
                if (pos >= input.length()) {
                    break;
                }

                char escaped = input.charAt(pos);
                sb.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '"' -> '"';
                    case '\\' -> '\\';
                    default -> throw new IllegalArgumentException(
                            "Unsupported escape sequence '\\" + escaped + "' at line " + line + ", column " + column
                    );
                });
                advance();
                continue;
            }

            if (curr == '\n' || curr == '\r') {
                throw new IllegalArgumentException(
                        "Unterminated string literal at line " + startLine + ", column " + startColumn
                );
            }

            sb.append(curr);
            advance();
        }

        throw new IllegalArgumentException(
                "Unterminated string literal at line " + startLine + ", column " + startColumn
        );
    }

    private char peekNext() {
        if (pos + 1 >= input.length()) {
            return '\0';
        }
        return input.charAt(pos + 1);
    }

    private void skipLineComment() {
        advance();
        advance();

        while (pos < input.length()) {
            char curr = input.charAt(pos);
            if (curr == '\n' || curr == '\r') {
                return;
            }
            advance();
        }
    }

    private void skipBlockComment() {
        int startLine = line;
        int startColumn = column;
        advance();
        advance();

        while (pos < input.length()) {
            char curr = input.charAt(pos);
            if (curr == '*' && peekNext() == '/') {
                advance();
                advance();
                return;
            }

            if (curr == '\n' || curr == '\r') {
                advanceWhitespace(curr);
                continue;
            }

            advance();
        }

        throw new IllegalArgumentException(
                "Unterminated block comment at line " + startLine + ", column " + startColumn
        );
    }

    private void advanceWhitespace(char curr) {
        if (curr == '\n') {
            pos++;
            line++;
            column = 1;
            return;
        }

        if (curr == '\r') {
            pos++;
            if (pos < input.length() && input.charAt(pos) == '\n') {
                pos++;
            }
            line++;
            column = 1;
            return;
        }

        advance();
    }

    private void advance() {
        pos++;
        column++;
    }
}
