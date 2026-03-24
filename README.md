# JavaAnalysisToolkit

JavaAnalysisToolkit is a small Java project that explores the core stages of a simple programming language implementation: lexical analysis, static validation, and runtime interpretation.

At the moment, the project can read a source file, tokenize it, validate the program structure and variable usage, then execute it through a lightweight interpreter.

## Current Features

- Lexical analysis for a small custom language
- Token metadata with line and column tracking
- Static analysis for:
  - variable declaration checks
  - duplicate declaration detection
  - assignment validation
  - basic expression validation
  - block validation for control flow
- Runtime interpreter with typed memory storage
- Integer variable declarations with `int`
- Boolean variable declarations with `bool`
- String variable declarations with `string`
- Variable reassignment
- Arithmetic expressions with:
  - `+`
  - `-`
  - `*`
  - `/`
  - `%`
- String concatenation with `+`
- Boolean negation with unary `!`
- Unary minus support
- Parenthesized expressions
- Boolean literals: `true`, `false`
- String literals with basic escape support
- Source comments:
  - `// line comments`
  - `/* block comments */`
- `print` statements
- `if` blocks
- `if` / `else` blocks
- `while` loops
- Boolean condition chaining with `&&`
- Boolean condition chaining with `||`
- Comparison operators:
  - `==`
  - `!=`
  - `<`
  - `<=`
  - `>`
  - `>=`

## Example Program

```txt
// Even / odd demo with modulo, &&, and ||
int x = 5;
int evenCount = 0;
int oddCount = 0;

while (x > 0) {
    if (x == 5 || x % 2 == 0 && x > 1) {
        evenCount = evenCount + 1;
        print "even";
    } else {
        oddCount = oddCount + 1;
        print "odd";
    }

    x = x - 1;
}

print evenCount;
print oddCount;
```

## Project Structure

- `src/Main.java` - entry point that reads the source file, runs analysis, and executes the program
- `src/Lexer.java` - converts source code into tokens
- `src/Token.java` - token model and token type definitions
- `src/StaticAnalyzer.java` - validates declarations, assignments, expressions, and blocks
- `src/Interpreter.java` - executes valid programs
- `src/ValueType.java` - shared language type definitions
- `src/RuntimeValue.java` - typed runtime value container used by the interpreter
- `data/program.txt` - sample source program

## How It Works

1. The application reads source code from `data/program.txt`.
2. The lexer transforms the source into tokens.
3. The static analyzer checks the token stream for common semantic and structural errors.
4. If validation succeeds, the interpreter executes the program.

## Running the Project

Compile:

```bash
javac -d out src\*.java
```

Run:

```bash
java -cp out Main
```

Run a specific test file:

```bash
java -cp out Main data\test_everything.txt
```

Available sample programs in `data\` include:

- `program.txt` - current default sample
- `test_arithmetic.txt` - arithmetic, unary minus, modulo
- `test_strings.txt` - string concatenation and escapes
- `test_booleans.txt` - boolean literals and `!`
- `test_comparisons.txt` - comparisons and `&&`
- `test_if_while.txt` - `if` / `else` plus `while`
- `test_comments.txt` - line and block comments
- `test_static_error_undeclared.txt` - expected static analysis failure
- `test_runtime_error_div_zero.txt` - expected runtime failure
- `test_everything.txt` - larger integration example

## Current Scope

This project is currently a compact educational interpreter rather than a full compiler or production-grade language toolchain. It is designed to demonstrate language-processing fundamentals in a simple and readable codebase.

## Planned Development

The project will continue to be developed. In practice, that means improving both language design and internal architecture.

Planned next steps include:

- separating parsing into a dedicated parser layer instead of relying directly on token-driven execution
- building an AST (Abstract Syntax Tree) for cleaner semantic analysis and interpretation
- improving error reporting further with more precise diagnostics
- adding automated tests for lexer, analyzer, and interpreter behavior
- exploring block scoping and more advanced language rules

## Why This Project Matters

This project is useful for learning and demonstrating:

- how a lexer works
- how static validation differs from runtime execution
- how interpreters evaluate expressions and statements
- how simple control flow can be implemented in a custom language
- how language tooling evolves from a minimal prototype toward a cleaner architecture
