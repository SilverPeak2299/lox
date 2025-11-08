package lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class Interpreter implements Expr.Visitor<double[]>, Stmt.Visitor<Void> {

  private static final int NUMBER_OF_DAYS = 12;
  private static final double SECONDS_PER_DAY = 86400.0;
  private static final double DECAY_CONSTANT = Math.log(100.0) / 10.0;
  private static final double DECAY_FACTOR = Math.exp(-DECAY_CONSTANT);

  private final Map<String, double[]> values = new LinkedHashMap<>();
  private double[] rainfallSeries;
  private int numberOfDays;
  private String currentAssignment = null;

  void interpret(Program program) {
    prepareRainfall(program.rainfallSeries);
    values.clear();
    for (Stmt statement : program.statements) {
      statement.accept(this);
    }
    System.out.println("waterflow for river system:");
    System.out.println("----------------------------");
    printRiverSystem(program);
    System.out.println();
    printTables();
  }

  private void prepareRainfall(List<Double> rainfall) {
    numberOfDays = Math.max(NUMBER_OF_DAYS, rainfall.size());
    rainfallSeries = new double[numberOfDays];
    for (int i = 0; i < rainfall.size(); i++) {
      rainfallSeries[i] = rainfall.get(i);
    }
  }

  @Override
  public Void visitAssignStmt(Stmt.Assign stmt) {
    currentAssignment = stmt.name.lexeme;
    double[] result = evaluate(stmt.value);
    values.put(stmt.name.lexeme, result);
    currentAssignment = null;
    return null;
  }

  private double[] evaluate(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public double[] visitVariableExpr(Expr.Variable expr) {
    return cloneArray(lookupSeries(expr.name.lexeme));
  }

  @Override
  public double[] visitBinaryExpr(Expr.Binary expr) {
    if (expr.operator.type == TokenType.PLUS) {
      double[] left = expr.left.accept(this);
      if (expr.right instanceof Expr.Dam damExpr) {
        double[] damOut = evaluateDamWithInflow(damExpr, left);
        double[] result = new double[numberOfDays];
        for (int i = 0; i < numberOfDays; i++) {
          result[i] = left[i] + damOut[i];
        }
        return result;
      }
      double[] right = expr.right.accept(this);
      double[] result = new double[numberOfDays];
      for (int i = 0; i < numberOfDays; i++) {
        result[i] = left[i] + right[i];
      }
      return result;
    }
    throw new RuntimeError("Operator '" + expr.operator.lexeme + "' not supported in this context.");
  }

  @Override
  public double[] visitWaterflowExpr(Expr.Waterflow expr) {
    if (expr.area <= 0) {
      throw new RuntimeError("Catchment area must be greater than zero.");
    }
    return computeWaterflowSeries(expr.area);
  }

  @Override
  public double[] visitLiteralExpr(Expr.Literal expr) {
    if (expr.value instanceof Double) {
      return filledSeries((Double) expr.value);
    }
    throw new RuntimeError("Unsupported literal type: " + expr.value);
  }

  @Override
  public double[] visitDamExpr(Expr.Dam expr) {
    if (currentAssignment == null) {
      throw new RuntimeError("Dam expressions must be assigned to a variable.");
    }
    String inflowName = currentAssignment + "_inflow";
    double[] inflowSeries = values.get(inflowName);
    if (inflowSeries == null) {
      inflowSeries = new double[numberOfDays];
    }
    return evaluateDamWithInflow(expr, inflowSeries);
  }

  private double[] evaluateDamWithInflow(Expr.Dam damExpr, double[] inflowSeries) {
    double initialFill = evaluateScalar(damExpr.init);
    double capacity = evaluateScalar(damExpr.cap);
    if (capacity <= 0) {
      throw new RuntimeError("Dam capacity must be greater than zero.");
    }
    double fill = clamp(initialFill, 0.0, 1.0);
    double[] outflow = new double[numberOfDays];
    for (int day = 0; day < numberOfDays; day++) {
      double inflow = inflowSeries[day];
      double rainToday = rainfallSeries[day];
      double multiplier = evaluateDamRules(damExpr.rules, day, fill, inflow, rainToday);
      double out = inflow * multiplier;
      outflow[day] = out;
      double rainContribution = convertRainfallToFlow(rainToday);
      fill = clamp(fill + (inflow + rainContribution - out) / capacity, 0.0, 1.0);
    }
    return outflow;
  }

  private double evaluateDamRules(Expr.DamRules rules, int day, double fill, double inflow, double rainToday) {
    if (rules instanceof Expr.DamRules.Flow flowRule) {
      return evaluateDamValue(flowRule.value, day, fill, inflow, rainToday);
    }
    Expr.DamRules.IfFlow ifFlow = (Expr.DamRules.IfFlow) rules;
    Object condition = evaluateDamExpr(ifFlow.condition, day, fill, inflow, rainToday);
    if (!(condition instanceof Boolean)) {
      throw new RuntimeError("Dam rule condition must evaluate to boolean.");
    }
    if ((Boolean) condition) {
      return evaluateDamValue(ifFlow.value, day, fill, inflow, rainToday);
    }
    return evaluateDamRules(ifFlow.elseBranch, day, fill, inflow, rainToday);
  }

  private double evaluateDamValue(Expr expr, int day, double fill, double inflow, double rainToday) {
    Object result = evaluateDamExpr(expr, day, fill, inflow, rainToday);
    if (!(result instanceof Double)) {
      throw new RuntimeError("Dam rule flow value must evaluate to a number.");
    }
    return (Double) result;
  }

  private Object evaluateDamExpr(Expr expr, int day, double fill, double inflow, double rainToday) {
    if (expr instanceof Expr.Literal literal) {
      if (literal.value instanceof Double || literal.value instanceof Boolean) {
        return literal.value;
      }
      throw new RuntimeError("Unsupported literal in dam rule: " + literal.value);
    }
    if (expr instanceof Expr.Variable variable) {
      return lookupDamVariable(variable.name.lexeme, day, fill, inflow, rainToday);
    }
    if (expr instanceof Expr.Binary binary) {
      Object left = evaluateDamExpr(binary.left, day, fill, inflow, rainToday);
      Object right = evaluateDamExpr(binary.right, day, fill, inflow, rainToday);
      return switch (binary.operator.type) {
        case PLUS -> toDouble(left, binary.operator) + toDouble(right, binary.operator);
        case GREATER -> toDouble(left, binary.operator) > toDouble(right, binary.operator);
        default -> throw new RuntimeError("Operator '" + binary.operator.lexeme + "' not supported in dam rule.");
      };
    }
    if (expr instanceof Expr.Waterflow waterflow) {
      double[] series = computeWaterflowSeries(waterflow.area);
      return series[day];
    }
    throw new RuntimeError("Unsupported expression inside dam rule.");
  }

  private Object lookupDamVariable(String name, int day, double fill, double inflow, double rainToday) {
    switch (name) {
      case "fill":
        return fill;
      case "inflow":
        return inflow;
      case "rain_today":
        return rainToday;
      default:
        double[] series = values.get(name);
        if (series == null) {
          if ("rainfall".equals(name)) {
            return rainfallSeries[day];
          }
          throw new RuntimeError("Undefined variable '" + name + "' in dam rule.");
        }
        return series[day];
    }
  }

  private double evaluateScalar(Expr expr) {
    Object value = evaluateDamExpr(expr, 0, 0.0, 0.0, rainfallSeries[0]);
    if (!(value instanceof Double)) {
      throw new RuntimeError("Expected numeric value.");
    }
    return (Double) value;
  }

  private double[] lookupSeries(String name) {
    if ("rainfall".equals(name)) {
      double[] converted = new double[numberOfDays];
      for (int i = 0; i < numberOfDays; i++) {
        converted[i] = convertRainfallToFlow(rainfallSeries[i]);
      }
      return converted;
    }
    double[] series = values.get(name);
    if (series == null) {
      throw new RuntimeError("Undefined variable '" + name + "'.");
    }
    return series;
  }

  private double[] cloneArray(double[] data) {
    return Arrays.copyOf(data, data.length);
  }

  private double[] filledSeries(double value) {
    double[] result = new double[numberOfDays];
    Arrays.fill(result, value);
    return result;
  }

  private double[] computeWaterflowSeries(double area) {
    double[] flows = new double[numberOfDays];
    double releaseFactor = 1.0 - DECAY_FACTOR;
    for (int day = 0; day < numberOfDays; day++) {
      double total = 0.0;
      for (int sourceDay = 0; sourceDay <= day; sourceDay++) {
        double rain = rainfallSeries[sourceDay];
        double liters = rain * area;
        double weight = releaseFactor * Math.pow(DECAY_FACTOR, day - sourceDay);
        total += liters * weight;
      }
      flows[day] = total / SECONDS_PER_DAY;
    }
    return flows;
  }

  private double convertRainfallToFlow(double rainfallMm) {
    return rainfallMm / SECONDS_PER_DAY;
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private double toDouble(Object value, Token operator) {
    if (value instanceof Double d) {
      return d;
    }
    throw new RuntimeError("Operand of '" + operator.lexeme + "' must be a number.");
  }

  private void printRiverSystem(Program program) {
    Map<String, Expr> assignments = new LinkedHashMap<>();
    Set<String> referenced = new LinkedHashSet<>();

    for (Stmt stmt : program.statements) {
      if (stmt instanceof Stmt.Assign assign) {
        assignments.put(assign.name.lexeme, assign.value);
        collectReferencedVars(assign.value, referenced);
      }
    }

    List<String> roots = new ArrayList<>();
    for (String name : assignments.keySet()) {
      if (!referenced.contains(name)) {
        roots.add(name);
      }
    }

    if (roots.isEmpty()) {
      roots.addAll(assignments.keySet());
    }

    for (String root : roots) {
      System.out.println(root);
      Set<String> path = new HashSet<>();
      path.add(root);
      printExprTree(assignments.get(root), "", true, assignments, path);
    }
  }

  private void printExprTree(
      Expr expr,
      String prefix,
      boolean isLast,
      Map<String, Expr> assignments,
      Set<String> path) {
    String branch = isLast ? "└── " : "├── ";
    System.out.print(prefix + branch);
    String childPrefix = prefix + (isLast ? "    " : "│   ");
    if (expr instanceof Expr.Binary binary) {
      System.out.println("(" + binary.operator.lexeme + ")");
      printExprTree(binary.left, childPrefix, false, assignments, path);
      printExprTree(binary.right, childPrefix, true, assignments, path);
    } else if (expr instanceof Expr.Dam dam) {
      String initStr = formatDamScalar(dam.init);
      String capStr = formatDamScalar(dam.cap);
      System.out.println("dam(" + initStr + ", " + capStr + ")");
    } else if (expr instanceof Expr.Variable variable) {
      System.out.println(variable.name.lexeme);
      Expr nested = assignments.get(variable.name.lexeme);
      if (nested != null && path.add(variable.name.lexeme)) {
        printExprTree(nested, childPrefix, true, assignments, path);
        path.remove(variable.name.lexeme);
      }
    } else if (expr instanceof Expr.Waterflow waterflow) {
      System.out.println("(waterflow area=" + waterflow.area + ")");
    } else if (expr instanceof Expr.Literal literal) {
      System.out.println(formatLiteralValue(literal.value));
    } else {
      System.out.println(expr.getClass().getSimpleName());
    }
  }

  private void collectReferencedVars(Expr expr, Set<String> referenced) {
    if (expr instanceof Expr.Variable variable) {
      referenced.add(variable.name.lexeme);
    } else if (expr instanceof Expr.Binary binary) {
      collectReferencedVars(binary.left, referenced);
      collectReferencedVars(binary.right, referenced);
    } else if (expr instanceof Expr.Dam dam) {
      collectReferencedVars(dam.init, referenced);
      collectReferencedVars(dam.cap, referenced);
    }
  }

  private String formatDamScalar(Expr expr) {
    if (expr instanceof Expr.Literal literal && literal.value instanceof Double d) {
      return formatDouble(d);
    }
    if (expr instanceof Expr.Variable variable) {
      return variable.name.lexeme;
    }
    return "?";
  }

  private String formatLiteralValue(Object value) {
    if (value instanceof Double d) {
      return formatDouble(d);
    }
    return String.valueOf(value);
  }

  private String formatDouble(double value) {
    long longValue = (long) value;
    if (value == longValue) {
      return String.format(Locale.US, "%d", longValue);
    }
    return Double.toString(value);
  }

  private void printTables() {
    String header = formatHeader();
    System.out.println("Rainfall (mm) per day:");
    System.out.println("----------------------");
    System.out.println(header);
    System.out.println(formatRow("rainfall", rainfallSeries, 1));
    System.out.println();
    System.out.println("Flows (L/second)");
    System.out.println("----------------");
    System.out.println(header);
    for (Map.Entry<String, double[]> entry : values.entrySet()) {
      System.out.println(formatRow(entry.getKey(), entry.getValue(), 2));
    }
  }

  private String formatHeader() {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format(Locale.US, "%-12s", ""));
    for (int day = 0; day < numberOfDays; day++) {
      builder.append(String.format(Locale.US, "%8d", day));
    }
    return builder.toString();
  }

  private String formatRow(String label, double[] values, int decimals) {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format(Locale.US, "%-12s", label));
    String format = decimals == 1 ? "%8.1f" : "%8.2f";
    for (double value : values) {
      builder.append(String.format(Locale.US, format, value));
    }
    return builder.toString();
  }
}

