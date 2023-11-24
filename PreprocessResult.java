package pku;


import java.util.HashMap;

import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.SetQueue;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.graph.*;

import pascal.taie.ir.proginfo.FieldRef;

import java.util.Map;
import java.util.Set;

import java.util.HashSet;

public class PreprocessResult {

    public final Map<New, Integer> obj_ids;
    public final Map<Integer, Var> test_pts;

    public final SimpleGraph<BaseVar> PFG;
    public final SimpleGraph<BaseVar> CG;
    public final Map<BaseVar, Set<Integer>> WL;
    public final Map<BaseVar, Set<Integer>> PT;
    public final Set<JMethod> RM;
    public final Set<Stmt> S;

    public PreprocessResult(){
        obj_ids = new HashMap<New, Integer>();
        test_pts = new HashMap<Integer,Var>();

        PFG = new SimpleGraph<BaseVar>();
        CG = new SimpleGraph<BaseVar>();
        WL = new HashMap<BaseVar, Set<Integer>>();
        PT = new HashMap<BaseVar, Set<Integer>>();
        RM = new HashSet<JMethod>();
        S = new HashSet<Stmt>();
    }

    /**
     * Benchmark.alloc(id);
     * X x = new X;// stmt
     * @param stmt statement that allocates a new object
     * @param id id of the object allocated
     */
    public void alloc(New stmt, int id)
    {
        obj_ids.put(stmt, id);
    }
    /**
     * Benchmark.test(id, var)
     * @param id id of the testing
     * @param v the pointer/variable
     */
    public void test(int id, Var v)
    {
        test_pts.put(id, v);
    }
    /**
     *
     * @param stmt statement that allocates a new object
     * @return id of the object allocated
     */
    public int getObjIdAt(New stmt)
    {
        return obj_ids.get(stmt);
    }
    /**
     * @param id
     * @return the pointer/variable in Benchmark.test(id, var);
     */
    public Var getTestPt(int id)
    {
        return test_pts.get(id);
    }

    /**
     * analysis of a JMethod, the result storing in this
     * @param ir ir of a JMethod
     */
    public void analysis(IR ir) {
        var stmts = ir.getStmts();
        Integer id = 0;

        JMethod method = ir.getMethod();
        if (!RM.contains(method)) return;

        RM.add(method);
        S.addAll(stmts);

        for (var stmt : stmts) {

            if(stmt instanceof Invoke)
            {              
                var exp = ((Invoke) stmt).getInvokeExp();
                if(exp instanceof InvokeStatic)
                {
                    var methodRef = ((InvokeStatic)exp).getMethodRef();
                    var className = methodRef.getDeclaringClass().getName();
                    var methodName = methodRef.getName();
                    JMethod expmethod = methodRef.resolve();
                    if(className.equals("benchmark.internal.Benchmark")
                    || className.equals("benchmark.internal.BenchmarkN"))
                    {
                        if(methodName.equals("alloc"))
                        {
                            var lit = exp.getArg(0).getConstValue();
                            assert lit instanceof IntLiteral;
                            id = ((IntLiteral)lit).getNumber();
                        }
                        else if(methodName.equals("test"))
                        {
                            var lit = exp.getArg(0).getConstValue();
                            assert lit instanceof IntLiteral;
                            var test_id = ((IntLiteral)lit).getNumber();
                            var pt = exp.getArg(1);
                            this.test(test_id, pt);
                        }
                    }
                }
            }
            else if(stmt instanceof New)
            {
                if(id!=0){ // ignore unlabeled `new` stmts
                    this.alloc((New)stmt, id);

                    LValue Left = stmt.getDef().get();
                    PtrVar lbase = new PtrVar((Var)Left);
                    if (!WL.containsKey(lbase)){
                        WL.put(lbase, new HashSet<Integer>());
                    }
                    WL.get(lbase).add(id);
                }

            }
            else if(stmt instanceof Copy)
            {
                LValue Left = stmt.getDef().get();
                stmt.getUses().forEach(Right->{
                    PtrVar lbase = new PtrVar((Var)Left);
                    PtrVar rbase = new PtrVar((Var)Right);
                    AddEdge(rbase, lbase);
                });
            }
            else if(stmt instanceof LoadField) // y = x.f
            {
                if (((LoadField)stmt).isStatic()){
                    FieldRef Right = ((LoadField)stmt).getFieldAccess().getFieldRef();
                    JClass RightClass = Right.getDeclaringClass();
                    JField RIghtField = Right.resolve();
                    StaticFieldRefVar RightVar = new StaticFieldRefVar(RIghtField, RightClass);
                    PtrVar LeftVar = new PtrVar(((LoadField)stmt).getLValue());
                    AddEdge(RightVar, LeftVar);
                }
            }
            else if(stmt instanceof StoreField) // x.f = y
            {
                if (((StoreField)stmt).isStatic()){
                    FieldRef Left = ((LoadField)stmt).getFieldAccess().getFieldRef();
                    JClass LeftClass = Left.getDeclaringClass();
                    JField LeftField = Left.resolve();
                    StaticFieldRefVar LeftVar = new StaticFieldRefVar(LeftField, LeftClass);
                    PtrVar RightVar = new PtrVar(((StoreField)stmt).getRValue());
                    AddEdge(RightVar, LeftVar);
                }
            }
        }
    }

    public void AddEdge(BaseVar from, BaseVar to)
    {
        if (PFG.hasEdge(from, to)) return;
        
        PFG.addEdge(from, to);
        if (!PT.get(from).isEmpty()) {
            if (!WL.containsKey(to)){
                WL.put(to, new HashSet<Integer>());
            }
            WL.get(to).addAll(PT.get(from));
        }
    }
}
