//再帰下降パーサー(Recursive Descent Parser)
import java.io.*;
public class RecursiveDescentParsing{
    static class State{
        public boolean isStdin = false;
    }
    static State STATE = new State();
    public static void main( final String[] args){
        RecursiveDescentParsing rdp = new RecursiveDescentParsing();
        if ( rdp.validateArguments( args) == false ){
            rdp.displayHelp();
            System.exit(1);
        }
        if ( rdp.doOperation( args) == false ){
            System.exit(1);
        }
        System.exit(0);
    }
    public boolean doOperation( final String[] args){
        boolean rtn = true;
        try{
            BufferedReader br = null;
            if ( this.STATE.isStdin == true){
                br=new BufferedReader(
                    new InputStreamReader(
                        System.in
                    )
                );
            }else{
                br=new BufferedReader(
                    new InputStreamReader(
                        new FileInputStream(
                            args[Constants.Argument.FILE_NAME]
                        )
                    )
                );
            }
            StreamTokenizer st = new StreamTokenizer(br);
            Parser p = new Parser(st);
            ParseTree tree = null;
            InterpretVisitor iv = new InterpretVisitor();
            for (;;){
                tree = p.parse();
                if (tree == null){
                    break;
                }
                System.out.println(tree.reversePolishNotation(""));
            }
        }catch(Exception e){
            System.err.println(e);
            rtn = false;
        }
        return rtn;
    }
    public boolean validateArguments( final String[] args){
        if ( args.length < Constants.Argument.NUMBER_OF_ARGUMENTS ){
            this.STATE.isStdin = true;
        }else{
            this.STATE.isStdin = false;
            File file = new File(args[Constants.Argument.FILE_NAME]);
            return file.canRead();
        }
        return true;
    }
    public void displayHelp(){
        System.err.println("RecursiveDescentParsing formula_file");
    }
}
final class Constants{
    public static final class Argument{
        public static int NUMBER_OF_ARGUMENTS = 1;
        public static int FILE_NAME = 0;
    }
}
interface ParseTree{
    public double accept(InterpretVisitor interpretVisitor);
    public String reversePolishNotation(String s);
}
final class ParseTreeNumeric implements ParseTree{
    private double nval;
    ParseTreeNumeric( double x){ nval=x;}
    public double getNval(){ return nval;}
    public double accept( InterpretVisitor v){ return v.eval(this);}
    public String reversePolishNotation(String s){ return s.concat(Integer.toString( (int)nval) + " ");}
    public String toString(){ return Integer.toString( (int)nval);}
}
final class ParseTreeBinary implements ParseTree{
    private char op;
    private ParseTree arg1, arg2;
    ParseTreeBinary( char c, ParseTree x, ParseTree y){ op=c; arg1=x; arg2=y;}
    public char getOp(){ return this.op;}
    public ParseTree getArg1(){ return this.arg1;}
    public ParseTree getArg2(){ return this.arg2;}
    public double accept( InterpretVisitor v){ return v.eval(this);}
    public String reversePolishNotation(String s){ return arg2.reversePolishNotation(arg1.reversePolishNotation(s)) + op + " ";}
    public String toString(){ return "(" + arg1 + op + arg2 + ")";}
}
class Parser{
    static final int EOF = StreamTokenizer.TT_EOF;
    static final int EOL = StreamTokenizer.TT_EOL;
    static final int NUMBER = StreamTokenizer.TT_NUMBER;
    static final int WORD = StreamTokenizer.TT_WORD;

    StreamTokenizer scanner;

    Parser(StreamTokenizer st){
        scanner = st;
        scanner.ordinaryChar('-');
        scanner.ordinaryChar('/');
        scanner.slashStarComments(true);
        scanner.slashSlashComments(true);
        scanner.eolIsSignificant(true);
    }

    ParseTree parse() throws IOException, ParserException{
        while (scanner.nextToken() == EOL){
            ;//skip empty lines
        }
        if (scanner.ttype == EOF){
            return null;        // end
        }else{
            scanner.pushBack();
            return expression();
        }
    }

    ParseTree expression() throws IOException, ParserException{
        ParseTree x, y;

        x = term();
        for (;;){
            switch (scanner.nextToken()){
            case EOF:
            case EOL:
            case ')':
                scanner.pushBack();
                return x;
            case '+':
                y = term();
                x = new ParseTreeBinary('+', x, y);
                break;
            case '-':
                y = term();
                x = new ParseTreeBinary('-', x, y);
                break;
            case NUMBER:
                throw new UnexpectedToken(scanner.nval);
            case WORD:
                throw new UnexpectedToken(scanner.sval);
            default:
                throw new UnexpectedToken( (char)scanner.ttype);
            }
        }
    }

    ParseTree term() throws IOException, ParserException{
        ParseTree x, y;

        x = primary();
        for (;;){
            switch (scanner.nextToken()){
            case EOF:
            case EOL:
            case ')':
            case '+':
            case '-':
                scanner.pushBack();
                return x;
            case '*':
                y = primary();
                x = new ParseTreeBinary('*', x, y);
                break;
            case '/':
                y = primary();
                x = new ParseTreeBinary('/', x, y);
                break;
            case NUMBER:
                throw new UnexpectedToken(scanner.nval);
            case WORD:
                throw new UnexpectedToken(scanner.sval);
            default:
                throw new UnexpectedToken( (char)scanner.ttype);
            }
        }
    }

    ParseTree primary() throws IOException, ParserException{
        ParseTree x;

        switch (scanner.nextToken()){
        case '(':
            x = expression();
            if (scanner.nextToken() == ')'){
                return x;
            }else{
                throw new SyntaxError("parenthes mismatch");
            }
        case NUMBER:
            return new ParseTreeNumeric(scanner.nval);
        default:
            throw new SyntaxError();
        }
    }
}
class InterpretVisitor{

    double eval(ParseTreeNumeric t){
        return t.getNval();
    }

    double eval(ParseTreeBinary t) throws InterpreterException{
        switch (t.getOp()) {
        case '+': return t.getArg1().accept(this) + t.getArg2().accept(this);
        case '-': return t.getArg1().accept(this) - t.getArg2().accept(this);
        case '*': return t.getArg1().accept(this) * t.getArg2().accept(this);
        case '/': return t.getArg1().accept(this) / t.getArg2().accept(this);
        default:
            throw new InterpreterException("Unknown operator: " + t.getOp());
        }
    }
}
class ParserException extends Exception{
    ParserException(){ super();}
    ParserException(String s){ super(s);}
}
class SyntaxError extends ParserException{
    SyntaxError(){ super();}
    SyntaxError(String s){ super(s);}
}
class UnexpectedToken extends ParserException{
    UnexpectedToken(char c){ super("" + c);}
    UnexpectedToken(double x){ super("" + x);}
    UnexpectedToken(String s){ super(s);}
    UnexpectedToken(char c, String s){ super(c + s + c);}
}
class InterpreterException extends RuntimeException{
    InterpreterException(String s){ super(s);}
}