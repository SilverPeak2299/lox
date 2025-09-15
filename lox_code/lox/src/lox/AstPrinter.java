package lox;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  String printProgram(Program program) {
    StringBuilder sb = new StringBuilder();
    sb.append("(program");
    for (Stmt s : program.statements) {
      sb.append("\n  ").append(s.accept(this));
    }
    sb.append("\n)");
    return sb.toString();
  }

  String printStmt(Stmt stmt) { return stmt.accept(this); }

  public String visitAssignStmt(Stmt.Assign stmt) {
    return stmt.name.lexeme + " = " + stmt.value.accept(this);
  }

  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  public String visitBinaryExpr(Expr.Binary expr) {
    return "(" + expr.operator.lexeme + " " +
           expr.left.accept(this) + " " +
           expr.right.accept(this) + ")";
  }

  public String visitWaterflowExpr(Expr.Waterflow expr) {
    return "waterflow(" + expr.first + "," + expr.second + ")";
  }
}