package lox;

import java.util.List;
import java.util.ArrayList;

class Parser {
  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) { this.tokens = tokens; }

  Program parse() {
    List<Double> rainfall = parseRainfallHeader();
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(statement());
    }
    return new Program(rainfall, statements);
  }

  private List<Double> parseRainfallHeader() {
    List<Double> values = new ArrayList<>();
    consume(TokenType.LEFT_BRACKET, "Program must start with rainfall series '['.");
    values.add(readRainNumber());
    while (match(TokenType.COMMA)) {
      values.add(readRainNumber());
      if (values.size() > 10) {
        error(previous(), "Rainfall series may not exceed 10 numbers.");
        throw new ParseError();
      }
    }
    consume(TokenType.RIGHT_BRACKET, "Expect ']' after rainfall series.");
    return values;
  }

  private double readRainNumber() {
    Token num = consume(TokenType.NUMBER, "Expect number in rainfall series.");
    return (double) num.literal;
  }

  private Stmt statement() {
    Token name = consume(TokenType.IDENTIFIER, "Expect identifier.");
    consume(TokenType.EQUAL, "Expect '=' after identifier.");
    Expr value = expr();
    consume(TokenType.SEMICOLON, "Expect ';' after statement.");
    return new Stmt.Assign(name, value);
  }

  private Expr expr() {
    if (isSingleCatchmentStart()) return waterflow();
    Expr left = entity();
    while (match(TokenType.PLUS)) {
      Token op = previous();
      Expr right = entity();
      left = new Expr.Binary(left, op, right);
    }
    return left;
  }

  private Expr entity() {
    Token name = consume(TokenType.IDENTIFIER, "Expect identifier.");
    return new Expr.Variable(name);
  }

  // waterflow -> '(' NUMBER ')'
  private Expr waterflow() {
    consume(TokenType.LEFT_PAREN, "Expect '('.");
    Token areaTok = consume(TokenType.NUMBER, "Expect catchment area.");
    consume(TokenType.RIGHT_PAREN, "Expect ')' after area.");
    return new Expr.Waterflow((double)areaTok.literal);
  }

  private boolean isSingleCatchmentStart() {
    if (!check(TokenType.LEFT_PAREN)) return false;
    return peekType(1) == TokenType.NUMBER && peekType(2) == TokenType.RIGHT_PAREN;
  }

  // Utility methods
  private Token consume(TokenType type, String msg) {
    if (check(type)) return advance();
    error(peek(), msg); throw new ParseError();
  }
  private boolean match(TokenType... types) {
    for (TokenType t : types) if (check(t)) { advance(); return true; }
    return false;
  }
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }
  private boolean isAtEnd() { return peek().type == TokenType.EOF; }
  private Token peek() { return tokens.get(current); }
  private Token previous() { return tokens.get(current - 1); }
  private TokenType peekType(int ahead) {
    int idx = current + ahead;
    if (idx >= tokens.size()) return TokenType.EOF;
    return tokens.get(idx).type;
  }
  private void error(Token token, String message) { Lox.error(token, message); }
  private static class ParseError extends RuntimeException {}
}