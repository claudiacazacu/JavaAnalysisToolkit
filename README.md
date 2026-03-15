# JavaAnalysisToolkit

JavaAnalysisToolkit is a simple Java project for experimenting with lexical analysis, static analysis, and interpretation of a small custom language.

## What the project has

- A lexer that converts source code into tokens
- A static analyzer that checks for basic semantic errors
- An interpreter that executes valid programs
- Support for integer variable declarations
- Support for variable reassignment
- Support for arithmetic expressions with `+`, `-`, `*`, `/`
- Support for parentheses in expressions
- Support for `print` statements

## Example language features

```txt
int x = 10;
int y = 20;
int sum = x + y * 2;
print sum;
sum = sum - 5;
print (sum + x) / 5;
```

## Project structure

- `src/Main.java` - entry point
- `src/Lexer.java` - tokenizes the input code
- `src/StaticAnalyzer.java` - checks declarations and expression validity
- `src/Interpreter.java` - runs the program
- `src/Token.java` - token definitions
- `data/program.txt` - sample input program

## What will be added

- Conditional statements like `if`
- Loops like `while`
- More data types such as `string` or `bool`
- Better error reporting with line numbers
- A parser layer for a cleaner architecture

The program reads code from `data/program.txt`.
