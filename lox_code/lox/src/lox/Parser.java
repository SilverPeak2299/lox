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
    return comparison();
  }

  private Expr comparison() {
    Expr expr = addition();
    while (match(TokenType.GREATER)) {
      Token op = previous();
      Expr right = addition();
      expr = new Expr.Binary(expr, op, right);
    }
    return expr;
  }

  private Expr addition() {
    Expr expr = primary();
    while (match(TokenType.PLUS)) {
      Token op = previous();
      Expr right = primary();
      expr = new Expr.Binary(expr, op, right);
    }
    return expr;
  }

  private Expr primary() {
    if (match(TokenType.FALSE)) return new Expr.Literal(false);
    if (match(TokenType.TRUE)) return new Expr.Literal(true);
    if (match(TokenType.NIL)) return new Expr.Literal(null);

    if (match(TokenType.NUMBER)) {
      return new Expr.Literal(previous().literal);
    }
    if (match(TokenType.STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(TokenType.DAM)) return damExpr();

    if (isWaterflowStart()) return waterflow();

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expr();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return expr;
    }

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    error(peek(), "Expect expression.");
    throw new ParseError();
  }

  private Expr damExpr() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'dam'.");
    Expr init = expr();
    consume(TokenType.COMMA, "Expect ',' after dam init expression.");
    Expr cap = expr();
    consume(TokenType.COMMA, "Expect ',' after dam cap expression.");
    Expr.DamRules rules = damRules();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after dam expression.");
    return new Expr.Dam(init, cap, rules);
  }

  private Expr.DamRules damRules() {
    return damIfChain();
  }

  private Expr.DamRules damIfChain() {
    if (match(TokenType.IF)) {
      Expr condition = expr();
      consume(TokenType.FLOW, "Expect 'flow' after dam if condition.");
      Expr value = expr();
      consume(TokenType.ELSE, "Expect 'else' after dam flow value.");
      Expr.DamRules elseBranch = damIfChain();
      return new Expr.DamRules.IfFlow(condition, value, elseBranch);
    }

    consume(TokenType.FLOW, "Expect 'flow' in dam rules.");
    Expr value = expr();
    return new Expr.DamRules.Flow(value);
  }

  // waterflow -> '(' NUMBER ')'
  private Expr waterflow() {
    consume(TokenType.LEFT_PAREN, "Expect '('.");
    Token areaTok = consume(TokenType.NUMBER, "Expect catchment area.");
    consume(TokenType.RIGHT_PAREN, "Expect ')' after area.");
    return new Expr.Waterflow((double)areaTok.literal);
  }

  private boolean isWaterflowStart() {
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