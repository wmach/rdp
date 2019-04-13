# rdp
is a yet another parsers for arithmetic expressions written in Java  
## Recursive Descent Parser
___practice design patterns below___
- composite pattern
- state pattern
- visitor pattern
## State Transit Parser
___practice design patterns below___
- builder pattern
- composite pattern
- state pattern
- visitor pattern
## Reverse Polish Notation Parser  
file _windows batch_
```
@java -classpath rdp.jar RecursiveDescentParsing formula | java -classpath rpn.jar ReversePolishNotation
```
file _formula_
```
1+2*3+4
((5))
(1+2*3)+4
(1+2)*(3+4)
1+2*(3+4)
1+(2*3+4)
1*(2+2)+3*(4+5)
2*(1+2)+3*4
1*(2+2)+3*(4+5)*4*3
2*((1+3)+4)
11*12-12*12
83*5+74*6
```

__2019-04-13__
