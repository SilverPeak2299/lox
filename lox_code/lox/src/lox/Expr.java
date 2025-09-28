package lox;

abstract class Expr {
  interface Visitor<R> {
    R visitVariableExpr(Variable expr);
    R visitBinaryExpr(Binary expr);
    R visitWaterflowExpr(Waterflow expr);
  }

  static class Variable extends Expr {
    final Token name;
    Variable(Token name) { this.name = name; }
    <R> R accept(Visitor<R> v) { return v.visitVariableExpr(this); }
  }

  static class Binary extends Expr {
    final Expr left;
    final Token operator;
    final Expr right;
    Binary(Expr left, Token operator, Expr right) {
      this.left = left; this.operator = operator; this.right = right;
    }
    <R> R accept(Visitor<R> v) { return v.visitBinaryExpr(this); }
  }

  static class Waterflow extends Expr {
    final double area; // only area now
    Waterflow(double area) {
      this.area = area;
    }
    <R> R accept(Visitor<R> v) { return v.visitWaterflowExpr(this); }
  }

  abstract <R> R accept(Visitor<R> visitor);
}