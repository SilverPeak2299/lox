package lox;

import java.util.List;

class Program {
  final List<Double> rainfallSeries;
  final List<Stmt> statements;
  Program(List<Double> rainfallSeries, List<Stmt> statements) {
    this.rainfallSeries = rainfallSeries;
    this.statements = statements;
  }
}
