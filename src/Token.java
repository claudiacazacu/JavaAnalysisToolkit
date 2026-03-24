import java.util.*;

enum TokenType {
    KEYWORD,
    IDENTIFIER,
    NUMBER,
    STRING_LITERAL,
    BOOLEAN_LITERAL,
    BANG,
    ASSIGN,
    EQUAL_EQUAL,
    BANG_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,
    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    SEMICOLON,
    EOF
}

public class Token {
    public final TokenType type;
    public final String value;
    public final int line;
    public final int column;

    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return "[" + type + ": " + value + " @ " + line + ":" + column + "]";
    }
}
