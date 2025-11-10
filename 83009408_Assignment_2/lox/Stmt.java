package lox;

abstract class Stmt {

  interface Visitor<R> {
    R visitAssignStmt(Assign stmt);

  }

  static class Assign extends Stmt {
    final Token name;
    final Expr value;

    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }
    
    <R> R accept(Visitor<R> v) { return v.visitAssignStmt(this); }
  }

  abstract <R> R accept(Visitor<R> visitor);
}
