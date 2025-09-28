# The Table 
## What literal in your language represents a river that gets 10L/s of flow on the first day after 1mm of rainfall?
Cannot be represented directly. The language supplies:
1. A rainfall header `[d1,d2,...]` (1–10 daily rainfall depths in mm) at the start of the program.
2. A catchment literal `(Area(m²))` giving catchment area combined with the header the waterflow is calculated through exponential decay 

(With co-effecients such that 10 days after that rainfall 99% will have left the system. the remaining 1% will be ignored; this is due to the fact that exponential decay is asymptotic)

To approximate 10 L/s after 1 mm on day, choose area A so that the model’s day‑1 discharge equals 10 L/s.

## What symbol in your language is used to show two rivers combine?
`+` (binary operator).

Example: `r3 = r1 + r2;`

## Is the above symbol a "unary", "binary", or "literal"?
Binary operator.

## What folder is the "working folder" to compile your parser?
Project root: `lox` (sources: `src/lox`).

## What command(s) will compile your parser?
From project root:
```bash
javac src/lox/*.java
```

## In your language, how long does it take all the water to work through a river system after 1 day of rain?
Exponential decay; constants chosen so ~99% leaves by day 10.

## Does your language include statements or is it an expression language?
It includes statements (assignments ending with `;`).
Example: `c1 = (3000);`

## Which chapter of the book have you used as the starting point for your solution?
Chapter 6 baseline (scanning & parsing) adapted.

# Grammar
```
program        ::= rainfallHeader statement* EOF ;
rainfallHeader ::= "[" number ("," number)* "]" ;
statement      ::= IDENTIFIER "=" expr ";" ;
expr           ::= entity ("+" entity)*
                 | catchment ;
entity         ::= IDENTIFIER ;
catchment      ::= "(" NUMBER ")" ;
```

# Example Program
```
[2,2,0]
c1 = (3000);
r1 = c1;
c2 = (4000);
r2 = r1 + c2;
```

# Notes
- Rainfall header supplies series used for all catchments.
- Catchment literal now only encodes area; rainfall per day comes from header.
- `+` combines flows (associative).