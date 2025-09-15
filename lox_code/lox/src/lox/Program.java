package lox;

import java.util.List;

class Program {
  final List<Stmt> statements;
  Program(List<Stmt> statements) {
    this.statements = statements;
  }
}
