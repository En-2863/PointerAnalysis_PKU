package pku;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.*;

public class test {
    public test() {};

    private static final Logger logger = LogManager.getLogger(IRDumper.class);

    public void test1(){
        var preprocess = new PreprocessResult();
        var result = new PointerAnalysisResult();
        var world = World.get();
        var main = world.getMainMethod();
        Integer id = 0;
        
        for (Stmt stmt: main.getIR().getStmts()){

            if(stmt instanceof Invoke)
            {
                var exp = ((Invoke) stmt).getInvokeExp();
                if(exp instanceof InvokeStatic)
                {
                    var methodRef = ((InvokeStatic)exp).getMethodRef();
                    var className = methodRef.getDeclaringClass().getName();
                    var methodName = methodRef.getName();

                    if(className.equals("benchmark.internal.Benchmark")
                    || className.equals("benchmark.internal.BenchmarkN"))
                    {
                        if(methodName.equals("alloc"))
                        {
                            var lit = exp.getArg(0).getConstValue();
                            assert lit instanceof IntLiteral;
                            id = ((IntLiteral)lit).getNumber();
                        }
                    }

                }

            else if (stmt instanceof New){
                LValue Left = stmt.getDef().get();
                /*if (Left instanceof Var && id != 0) { // ignore unlabeled `new` stmts

                    PtrVar base1 = new PtrVar((Var)Left);
                    preprocess.WL.get((BaseVar)base1).add(id);

                    PtrVar base2 = new PtrVar((Var)Left);
                    preprocess.WL.get((BaseVar)base2).add(id+1);

                    for (Integer x : preprocess.WL.get((BaseVar)base1)){
                        logger.info("Integer: {}", x);
                        System.out.println(x);
                    }
                }*/
            }
        }
        
    }
}
}
