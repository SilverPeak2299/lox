package lox;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  String printProgram(Program program) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < program.rainfallSeries.size(); i++) {
      if (i > 0) sb.append(',');
      double v = program.rainfallSeries.get(i);
      if (v == Math.rint(v)) sb.append((long) v); else sb.append(v);
    }
    sb.append("]\n");
    for (Stmt s : program.statements) {
      sb.append(s.accept(this)).append("\n");
    }
    return sb.toString().trim();
  }

  String printStmt(Stmt stmt) { return stmt.accept(this); }

  public String visitAssignStmt(Stmt.Assign stmt) {
    return stmt.name.lexeme + " = " + stmt.value.accept(this);
  }

  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  public String visitBinaryExpr(Expr.Binary expr) {
    return "(+ " + expr.left.accept(this) + " " + expr.right.accept(this) + ")";
  }

  public String visitWaterflowExpr(Expr.Waterflow expr) {
    return "(waterflow " + expr.area + ")";
  }

  public String visitLiteralExpr(Expr.Literal expr) {
    return formatLiteral(expr.value);
  }

  public String visitDamExpr(Expr.Dam expr) {
    return "Dam(" + printDamExprValue(expr.init) + ", " +
        printDamExprValue(expr.cap) + ", " + printDamRules(expr.rules) + ")";
  }

  private String printDamRules(Expr.DamRules rules) {
    if (rules instanceof Expr.DamRules.Flow) {
      Expr.DamRules.Flow flow = (Expr.DamRules.Flow) rules;
      return "Flow(" + printDamExprValue(flow.value) + ")";
    }
    if (rules instanceof Expr.DamRules.IfFlow) {
      Expr.DamRules.IfFlow ifFlow = (Expr.DamRules.IfFlow) rules;
      return "IfFlow(cond:" + printDamExprValue(ifFlow.condition) +
          ", value:" + printDamExprValue(ifFlow.value) +
          ", else:" + printDamRules(ifFlow.elseBranch) + ")";
    }
    throw new IllegalArgumentException("Unknown dam rule type");
  }

  private String printDamExprValue(Expr expr) {
    if (expr instanceof Expr.Binary) {
      Expr.Binary binary = (Expr.Binary) expr;
      return "(" + printDamExprValue(binary.left) +
          binary.operator.lexeme +
          printDamExprValue(binary.right) + ")";
    }
    if (expr instanceof Expr.Variable) {
      return visitVariableExpr((Expr.Variable) expr);
    }
    if (expr instanceof Expr.Literal) {
      return visitLiteralExpr((Expr.Literal) expr);
    }
    if (expr instanceof Expr.Waterflow) {
      return visitWaterflowExpr((Expr.Waterflow) expr);
    }
    if (expr instanceof Expr.Dam) {
      return visitDamExpr((Expr.Dam) expr);
    }
    return expr.accept(this);
  }

  private String formatLiteral(Object value) {
    if (value == null) return "nil";
    if (value instanceof Double) {
      double d = (Double) value;
      if (d == Math.rint(d)) {
        return Long.toString((long) d);
      }
      return Double.toString(d);
    }
    return value.toString();
  }
}
