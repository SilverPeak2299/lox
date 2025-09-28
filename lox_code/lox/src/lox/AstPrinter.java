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
}