/**State Transite Parser*/
 *builder pattern
 *composite pattern
 *state pattern
 *visitor pattern
 */
import java.io.*;
import java.util.Stack;
final class Constants{
    public static final class Argument{
        public static int NUMBER_OF_ARGUMENTS = 1;
        public static int FILE_NAME = 0;
    }
}
public class StateTransientParsing031{
    static class ProcessState{
        public boolean isStdin = false;
    }
    static ProcessState PROCESS_STATE = new ProcessState();//入力が標準出力とファイルのどちらかを保持
    public static void main( final String[] args){
        StateTransientParsing031 stp = new StateTransientParsing031();
        if ( stp.validateArguments( args) == false ){
            stp.displayHelp();
            System.exit(1);
        }
        if ( stp.doOperation( args) == false ){
            System.exit(1);
        }
        System.exit(0);
    }
    public boolean doOperation( final String[] args){
        boolean rtn = true;
        ParseTree tree = null;
        StreamTokenizer st = null;
        Parser p = null;
        EvaluateVisitor ev = new EvaluateVisitor();
        try{
            BufferedReader br = null;
            if ( PROCESS_STATE.isStdin == true){
                br = new BufferedReader(new InputStreamReader(System.in));
            }else{
                br = new BufferedReader(new InputStreamReader(new FileInputStream( args[Constants.Argument.FILE_NAME])));
            }
            p = new Parser(st=new StreamTokenizer(br));
            for (;;){
                tree = p.parse();
                if (tree == null){
                    break;
                }
                System.out.println(tree + " = " + tree.accept(ev));
            }
        }catch(Exception e){
            e.printStackTrace();
            rtn = false;
        }
        return rtn;
    }
    public boolean validateArguments( final String[] args){
        if ( args.length < Constants.Argument.NUMBER_OF_ARGUMENTS ){
            PROCESS_STATE.isStdin = true;
        }else{
            PROCESS_STATE.isStdin = false;
            File file = new File(args[Constants.Argument.FILE_NAME]);
            return file.canRead();
        }
        return true;
    }
    public void displayHelp(){
        System.out.println(this.getClass().getName()+" [ formula file | standard output ]");
    }
}
interface ParseTree{
    public double accept(EvaluateVisitor EvaluateVisitor);
    public ParseTree getArg();
}
final class ParseTreeNumeric implements ParseTree{
    private double nval = 0;
    ParseTreeNumeric( double x){ nval=x;}
    public double getNval(){ return nval;}
    public double accept( EvaluateVisitor v){ return v.eval(this);}
    public ParseTree getArg(){ return this;}
    public String toString(){ return Integer.toString( (int)nval);}
}
final class ParseTreeBinary implements ParseTree{
    private Algorithm a = null;
    private ParseTree arg1 = null;
    private ParseTree arg2 = null;
    ParseTreeBinary( Algorithm a, ParseTree x){ this.a=a; this.arg1=x;}
    public Algorithm getAlgorithm(){ return this.a;}
    public ParseTree getArg(){ return this.arg1==null ? this.arg1 : this.arg2;}
    public ParseTree getArg1(){ return this.arg1;}
    public ParseTree getArg2(){ return this.arg2;}
    public void setArg2(ParseTree p){ this.arg2=p;}
    public void setAlgorithm( Algorithm a){ this.a=a;}
    public double accept( EvaluateVisitor v){ return v.eval(this);}
    public String toString(){ return "(" + arg1 + a + arg2 + ")";}
}

interface Algorithm{ public double doOperate(double d1, double d2);}
class AlgorithmAdd implements Algorithm{ public double doOperate(double d1, double d2){ return d1 + d2;} public String toString(){ return "+";}}
class AlgorithmSubtract implements Algorithm{ public double doOperate(double d1, double d2){ return d1 - d2;} public String toString(){ return "-";}}
class AlgorithmMultiply implements Algorithm{ public double doOperate(double d1, double d2){ return d1 * d2;} public String toString(){ return "*";}}
class AlgorithmDivide implements Algorithm{ public double doOperate(double d1, double d2){ return d1 / d2;} public String toString(){ return "/";}}

interface State{
    ParseTree read(ParseTree p) throws IOException, ParserException;
}

class ParserConstants{
    static final int EOF = StreamTokenizer.TT_EOF;
    static final int EOL = StreamTokenizer.TT_EOL;
    static final int NUMBER = StreamTokenizer.TT_NUMBER;
    static final int WORD = StreamTokenizer.TT_WORD;
}
class Parser{
    private State state = null;
    private Stack stack = null;
    private Stack tree_stack = null;
    private ExpressionState EXPR = null;
    private TermState TERM = null;
    private FactorState FACT = null;

    StreamTokenizer scanner = null;

    Parser(StreamTokenizer st){
        this.scanner = st;
        scanner.ordinaryChar( '-' );
        scanner.ordinaryChar( '/' );
        scanner.slashStarComments( true );
        scanner.slashSlashComments( true );
        scanner.eolIsSignificant( true );
        EXPR = new ExpressionState( this, st );
        TERM = new TermState( this, st );
        FACT = new FactorState( this, st );
        stack = new Stack();
        tree_stack = new Stack();
    }

    //getter
    public State getExpression(){ return this.EXPR;}
    public State getTerm(){ return this.TERM;}
    public State getFactor(){ return this.FACT;}
    public State getState(){ return this.state;}
    public State popState(){ return (State)stack.pop();}
    public ParseTree popTree(){ return (ParseTree)tree_stack.pop();}
    public int stateStackSearch(State s) { return stack.search(s);}
    public int treeStackSearch(ParseTree p) { return tree_stack.search(p);}

    //setter
    private void setState( State state){ this.state = state;}
    public void pushState(State s){ stack.push(s);}
    public void pushTree(ParseTree p){ tree_stack.push(p);}

    public boolean isEmptyState(){ return stack.empty();}
    public boolean isEmptyTree(){ return tree_stack.empty();}
    public State peekState(){ return (State)stack.peek();}
    public ParseTree peekTree(){ return (ParseTree)tree_stack.peek();}
    public int sizeState(){ return stack.size();}
    public int sizeTree(){ return tree_stack.size();}

    public void changeState( State state ){ this.state=state;}

    ParseTree parse() throws IOException, ParserException{
        changeState( this.getExpression());
        ParseTree p = null;
        while ( true ){
            scanner.nextToken();
            if (( scanner.ttype == ParserConstants.EOL ) ||
                ( scanner.ttype == ParserConstants.EOF )){
                if (!isEmptyTree()){
                    scanner.pushBack();
                }else{
                    break;
                }
            }
            scanner.pushBack();
            p = state.read( p );
        }
        return p;
    }
}
class ExpressionState implements State{
    private Parser parser = null;
    private StreamTokenizer scanner = null;
    public ExpressionState( Parser parser, StreamTokenizer scanner ){
        this.parser = parser;
        this.scanner = scanner;
    }
    public ParseTree read( ParseTree p ) throws IOException, ParserException{
        ParseTreeBinary next_p = null;
        ParseTreeBinary prev_p = null;
        if (p==null){
            parser.changeState( parser.getTerm() );
            parser.pushState( this );
            parser.pushTree( p );
        }else{
            if (!parser.isEmptyTree()){
                prev_p = (ParseTreeBinary)parser.popTree();
            }
        }
        switch (scanner.nextToken()){
        case ParserConstants.EOF:
        case ParserConstants.EOL:
            scanner.pushBack();
            if ( !parser.isEmptyState() ){
                parser.changeState( parser.popState() );
            }
            if (p!=null){
                if (prev_p!=null){
                    prev_p.setArg2( p );
                    return prev_p;
                }else{
                    throw new SyntaxError();
                }
            }else{
                if (prev_p != null){
                    return p;
                }else{
                    throw new SyntaxError();
                }
            }
        case ParserConstants.NUMBER:
        case '(':
        case ')':
        case '*':
        case '/':
            scanner.pushBack();
            if (p==null){
                parser.changeState( parser.getTerm() );
                return p;
            }else{
                parser.changeState( parser.popState() );
                if (prev_p==null){
                    return p;
                }else{
                    ((ParseTreeBinary)prev_p).setArg2( p );
                    return prev_p;
                }
            }
        case '+':
            if (p == null){
                throw new UnexpectedToken(scanner.sval);
            }else{
                if (prev_p == null){
                    parser.changeState(parser.getTerm());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmAdd(), p));
                    return prev_p;
                }else{
                    prev_p.setArg2( p );
                    parser.changeState(parser.getTerm());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmAdd(), prev_p));
                    prev_p = null;
                    return prev_p;
                }
            }
        case '-':
            if (p == null){
                throw new UnexpectedToken(scanner.sval);
            }else{
                if (prev_p == null){
                    parser.changeState(parser.getTerm());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmSubtract(), p));
                    return prev_p;
                }else{
                    prev_p.setArg2( p );
                    parser.changeState(parser.getTerm());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmSubtract(), prev_p));
                    prev_p = null;
                    return prev_p;
                }
            }
        case ParserConstants.WORD:
            throw new UnexpectedToken(scanner.sval);
        default:
            throw new UnexpectedToken( (char)scanner.ttype);
        }
    }
}
class TermState implements State{
    private Parser parser = null;
    private StreamTokenizer scanner = null;
    public TermState( Parser parser, StreamTokenizer scanner ){
        this.parser = parser;
        this.scanner = scanner;
    }
    public ParseTree read( ParseTree p) throws IOException, ParserException{
        ParseTreeBinary next_p = null;
        ParseTreeBinary prev_p = null;

        if (p==null){
            parser.changeState( parser.getFactor() );
            parser.pushState( this );
            parser.pushTree( p );
        }else{
            if (!parser.isEmptyTree()){
                prev_p = (ParseTreeBinary)parser.popTree();
            }
        }

        switch (scanner.nextToken()){
        case ParserConstants.EOF:
        case ParserConstants.EOL:
            scanner.pushBack();
            if ( !parser.isEmptyState() ){
                parser.changeState( parser.popState() );
            }
            if (p==null){
                if (prev_p == null){
                    throw new SyntaxError();
                }else{
                    return p;
                }
            }else{
                if (prev_p==null){
                    return p;
                }else{
                    prev_p.setArg2( p );
                    return prev_p;
                }
            }
        case ParserConstants.NUMBER:
        case '(':
        case ')':
            scanner.pushBack();
            parser.changeState( parser.getFactor() );
            if (p==null){
                if (prev_p==null){
                    return p;
                }else{
                    return prev_p;
                }
            }else{
                if (prev_p==null){
                    parser.changeState( parser.popState());
                    return p;
                }else{
                    parser.changeState( parser.popState());
                    return prev_p;
                }
            }
        case '+':
        case '-':
            scanner.pushBack();
            parser.changeState( parser.popState() );
            if (p==null){
                throw new UnexpectedToken( (char)scanner.ttype);
            }else{
                if (prev_p==null){
                    return p;
                }else{
                    prev_p.setArg2( p );
                    return prev_p;
                }
            }
        case '*':
            if (p == null){
                throw new UnexpectedToken(scanner.sval);
            }else{
                if (prev_p == null){
                    parser.changeState(parser.getFactor());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmMultiply(), p));
                    return prev_p;
                }else{
                    prev_p.setArg2( p );
                    parser.changeState(parser.getFactor());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmMultiply(), prev_p));
                    prev_p = null;
                    return prev_p;
                }
            }
        case '/':
            if (p == null){
                throw new UnexpectedToken(scanner.sval);
            }else{
                if (prev_p == null){
                    parser.changeState(parser.getFactor());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmDivide(), p));
                    return prev_p;
                }else{
                    prev_p.setArg2( p );
                    parser.changeState(parser.getFactor());
                    parser.pushState( this );
                    parser.pushTree(next_p=new ParseTreeBinary(new AlgorithmDivide(), prev_p));
                    prev_p = null;
                    return prev_p;
                }
            }
        case ParserConstants.WORD:
            throw new UnexpectedToken(scanner.sval);
        default:
            throw new UnexpectedToken( (char)scanner.ttype);
        }
    }
}
class FactorState implements State{
    private Parser parser = null;
    private StreamTokenizer scanner = null;
    public FactorState( Parser parser, StreamTokenizer scanner ){
        this.parser = parser;
        this.scanner = scanner;
    }
    public ParseTree read( ParseTree p) throws IOException, ParserException{
        ParseTreeBinary prev_p = null;
        if (p!=null){
            if (!parser.isEmptyTree()){
                prev_p = (ParseTreeBinary)parser.popTree();
            }
        }
        switch (scanner.nextToken()){
        case ParserConstants.EOF:
        case ParserConstants.EOL:
            scanner.pushBack();
            if ( !parser.isEmptyState() ){
                parser.changeState( parser.popState() );
            }
            if (p==null){
                if (prev_p == null){
                    throw new SyntaxError();
                }else{
                    return p;
                }
            }else{
                if (prev_p==null){
                    return p;
                }else{
                    prev_p.setArg2( p );
                    return prev_p;
                }
            }
        case '(':
            if ( p==null){
                parser.changeState( parser.getExpression() );
                parser.pushState( this );
                parser.pushTree( p );
                return p;
            }else{
                throw new SyntaxError("parenthes mismatch");
            }
        case ')':
            if ( p==null ){
                if ( prev_p==null ){
                    throw new SyntaxError("parenthes mismatch.");
                }else{
                    return prev_p;
                }
            }else{
                parser.changeState( parser.popState() );
                if (prev_p==null){
                    return p;
                }else{
                    throw new SyntaxError("parenthes mismatch.");
                }
            }
        case ParserConstants.NUMBER:
            parser.changeState( (State)parser.popState() );
            return new ParseTreeNumeric(scanner.nval);
        case '+':
        case '-':
        case '*':
        case '/':
            scanner.pushBack();
            parser.changeState( parser.popState());
            if (p==null){
                if (prev_p==null){
                    return p;
                }else{
                    return prev_p;
                }
            }else{
                if (prev_p==null){
                    return p;
                }else{
                    prev_p.setArg2( p );
                    return prev_p;
                }
            }
        default:
            throw new SyntaxError();
        }
    }
}
class EvaluateVisitor{
    double eval(ParseTreeNumeric t){ return t.getNval();}
    double eval(ParseTreeBinary t){
        return t.getAlgorithm().doOperate(t.getArg1().accept(this), t.getArg2().accept(this));
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
