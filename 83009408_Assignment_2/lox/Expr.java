package lox;

abstract class Expr {
  interface Visitor<R> {
    R visitVariableExpr(Variable expr);
    R visitBinaryExpr(Binary expr);
    R visitWaterflowExpr(Waterflow expr);
    R visitLiteralExpr(Literal expr);
    R visitDamExpr(Dam expr);
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

  static class Literal extends Expr {
    final Object value;
    Literal(Object value) { this.value = value; }
    <R> R accept(Visitor<R> v) { return v.visitLiteralExpr(this); }
  }

  static class Waterflow extends Expr {
    final double area; // only area now
    Waterflow(double area) {
      this.area = area;
    }
    <R> R accept(Visitor<R> v) { return v.visitWaterflowExpr(this); }
  }

  static class Dam extends Expr {
    final Expr init;
    final Expr cap;
    final DamRules rules;
    Dam(Expr init, Expr cap, DamRules rules) {
      this.init = init;
      this.cap = cap;
      this.rules = rules;
    }
    <R> R accept(Visitor<R> v) { return v.visitDamExpr(this); }
  }

  abstract static class DamRules {
    static class Flow extends DamRules {
      final Expr value;
      Flow(Expr value) { this.value = value; }
    }

    static class IfFlow extends DamRules {
      final Expr condition;
      final Expr value;
      final DamRules elseBranch;
      IfFlow(Expr condition, Expr value, DamRules elseBranch) {
        this.condition = condition;
        this.value = value;
        this.elseBranch = elseBranch;
      }
    }
  }

  abstract <R> R accept(Visitor<R> visitor);
}