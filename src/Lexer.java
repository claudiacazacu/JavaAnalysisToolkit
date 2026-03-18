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

            if (Character.isDigit(curr)) {
                int startLine = line;
                int startColumn = column;
                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    sb.append(input.charAt(pos));
                    advance();
                }
                tokens.add(new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn));
            } else if (Character.isLetter(curr)) {
                int startLine = line;
                int startColumn = column;
                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && Character.isLetterOrDigit(input.charAt(pos))) {
                    sb.append(input.charAt(pos));
                    advance();
                }
                String val = sb.toString();
                tokens.add(new Token(isKeyword(val) ? TokenType.KEYWORD : TokenType.IDENTIFIER, val, startLine, startColumn));
            } else if (curr == '=' && peekNext() == '=') {
                tokens.add(token(TokenType.EQUAL_EQUAL, "=="));
                advance();
                advance();
            } else if (curr == '!' && peekNext() == '=') {
                tokens.add(token(TokenType.BANG_EQUAL, "!="));
                advance();
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
        return value.equals("int") || value.equals("print") || value.equals("if") || value.equals("while");
    }

    private Token token(TokenType type, String value) {
        return new Token(type, value, line, column);
    }

    private char peekNext() {
        if (pos + 1 >= input.length()) {
            return '\0';
        }
        return input.charAt(pos + 1);
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
