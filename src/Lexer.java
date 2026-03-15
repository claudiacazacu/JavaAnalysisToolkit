import java.util.*;

public class Lexer {
    private final String input;
    private int pos = 0;

    public Lexer(String input) { this.input = input; }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char curr = input.charAt(pos);
            if (Character.isWhitespace(curr)) { pos++; continue; }

            if (Character.isDigit(curr)) {
                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) sb.append(input.charAt(pos++));
                tokens.add(new Token(TokenType.NUMBER, sb.toString()));
            } else if (Character.isLetter(curr)) {
                StringBuilder sb = new StringBuilder();
                while (pos < input.length() && Character.isLetterOrDigit(input.charAt(pos))) sb.append(input.charAt(pos++));
                String val = sb.toString();
                tokens.add(new Token(isKeyword(val) ? TokenType.KEYWORD : TokenType.IDENTIFIER, val));
            } else if (curr == '=') {
                tokens.add(new Token(TokenType.ASSIGN, "="));
                pos++;
            } else if (curr == '+') {
                tokens.add(new Token(TokenType.PLUS, "+"));
                pos++;
            } else if (curr == '-') {
                tokens.add(new Token(TokenType.MINUS, "-"));
                pos++;
            } else if (curr == '*') {
                tokens.add(new Token(TokenType.STAR, "*"));
                pos++;
            } else if (curr == '/') {
                tokens.add(new Token(TokenType.SLASH, "/"));
                pos++;
            } else if (curr == '(') {
                tokens.add(new Token(TokenType.LPAREN, "("));
                pos++;
            } else if (curr == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")"));
                pos++;
            } else if (curr == ';') {
                tokens.add(new Token(TokenType.SEMICOLON, ";"));
                pos++;
            } else {
                throw new IllegalArgumentException("Unexpected character '" + curr + "' at position " + pos);
            }
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private boolean isKeyword(String value) {
        return value.equals("int") || value.equals("print");
    }
}
