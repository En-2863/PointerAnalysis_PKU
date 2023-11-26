package pku;


import java.util.HashMap;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeStatic;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.*;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.graph.*;

import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;

public class PreprocessResult {

    public Logger logger;

    public final Map<New, Integer> obj_ids;
    public final Map<Integer, Var> test_pts;

    public final SimpleGraph<BaseVar> PFG;
    public final Set<BaseVar> CG;
    public final Map<BaseVar, Set<Integer>> WL;
    public final Map<BaseVar, Set<Integer>> PT;
    public final Map<Integer, Integer> OBJ;
    public final Map<Integer, Type> OBJTYPE;
    public final Set<JMethod> RM;
    public final Set<Stmt> S;

    public Integer Newcnt = 0;
    public Integer Copycnt = 0;
    public Integer Herecnt = 0;

    public PreprocessResult(){
        obj_ids = new HashMap<New, Integer>();
        test_pts = new HashMap<Integer,Var>();

        PFG = new SimpleGraph<BaseVar>();
        CG = new HashSet<BaseVar>();
        WL = new HashMap<BaseVar, Set<Integer>>();
        PT = new HashMap<BaseVar, Set<Integer>>();
        OBJ = new HashMap<Integer, Integer>();
        OBJTYPE = new HashMap<Integer, Type>();
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
        Herecnt++;
        var stmts = ir.getStmts();
        Integer id = 0;

        JMethod method = ir.getMethod();
        if (RM.contains(method)) return;
        Herecnt++;

        RM.add(method);
        S.addAll(stmts);
        Herecnt++;

        for (var stmt : stmts) {

            if(stmt instanceof Invoke)
            {              
                var exp = ((Invoke) stmt).getInvokeExp();
                if(exp instanceof InvokeStatic)
                {
                    var methodRef = ((InvokeStatic)exp).getMethodRef();
                    var className = methodRef.getDeclaringClass().getName();
                    var methodName = methodRef.getName();
                    //JMethod expmethod = methodRef.resolve();
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
                    else{
                        ProcessCallStatic((Invoke)stmt);
                    }
                }
            }
            else if(stmt instanceof New)
            {
                LValue Left = stmt.getDef().get();
                if (Left instanceof Var){
                    PtrVar lbase = new PtrVar((Var)Left);
                    if (!WL.containsKey(lbase)) {
                        WL.put(lbase, new HashSet<Integer>());
                    }
                    WL.get(lbase).add(Newcnt);
                    OBJTYPE.put(Newcnt, ((New)stmt).getRValue().getType());
                    Newcnt++;
                }
                if(id!=0){ // ignore unlabeled `new` stmts
                    this.alloc((New)stmt, id);
                    OBJ.put(Newcnt - 1, id); // Very Important
                   
                }
            }
            else if(stmt instanceof Copy)
            {
                LValue Left = stmt.getDef().get();
                stmt.getUses().forEach(Right->{
                    PtrVar lbase = new PtrVar((Var)Left);
                    PtrVar rbase = new PtrVar((Var)Right);
                    logger.info("Copy: {} = {}", Left, Right);
                    AddEdge(rbase, lbase);
                });
                Copycnt++;
            }
            else if(stmt instanceof LoadField) // y = x.f
            {
                if (((LoadField)stmt).isStatic()){
                    FieldRef Right = ((LoadField)stmt).getFieldAccess().getFieldRef();
                    JClass RightClass = Right.getDeclaringClass();
                    JField RIghtField = Right.resolve();
                    StaticFieldRefVar RightVar = new StaticFieldRefVar(RIghtField, RightClass);
                    PtrVar LeftVar = new PtrVar(((LoadField)stmt).getLValue());
                    logger.info("LoadField: {} = {}", LeftVar, RightVar);
                    AddEdge(RightVar, LeftVar);
                }
            }
            else if(stmt instanceof StoreField) // x.f = y
            {
                if (((StoreField)stmt).isStatic()){
                    FieldRef Left = ((StoreField)stmt).getFieldAccess().getFieldRef();
                    JClass LeftClass = Left.getDeclaringClass();
                    JField LeftField = Left.resolve();
                    StaticFieldRefVar LeftVar = new StaticFieldRefVar(LeftField, LeftClass);
                    PtrVar RightVar = new PtrVar(((StoreField)stmt).getRValue());
                    logger.info("StoreField: {} = {}", LeftVar, RightVar);
                    AddEdge(RightVar, LeftVar);
                }
            }
        }
    }

    public void AddEdge(BaseVar from, BaseVar to)
    {
        if (PFG.hasEdge(from, to)) return;

        PFG.addEdge(from, to);
        if (!PT.containsKey(from)){
            PT.put(from, new HashSet<Integer>());
        }
        if (!PT.get(from).isEmpty()) {
            if (!WL.containsKey(to)){
                WL.put(to, new HashSet<Integer>());
            }
            WL.get(to).addAll(PT.get(from));
        }
    }

    // From Nju expreiment source code.
    public JMethod resolveCallee(Type type, Invoke callSite) {
        MethodRef methodRef = callSite.getMethodRef();
        if (callSite.isInterface() || callSite.isVirtual()) {
            return World.get().getClassHierarchy()
                    .dispatch(type, methodRef);
        } else if (callSite.isSpecial()) {
            return World.get().getClassHierarchy()
                    .dispatch(methodRef.getDeclaringClass(), methodRef);
        } else if (callSite.isStatic()) {
            return methodRef.resolveNullable();
        } else {
            throw new AnalysisException("Cannot resolve Invoke: " + callSite);
        }
    }

    public void ProcessCallStatic(Invoke stmt)
    {
        JMethod M = stmt.getMethodRef().resolve();
        logger.info("M: {}", M);
        MethodRefVar m = new MethodRefVar(M, stmt.getLineNumber());

        if (!CG.contains(m)) {
            logger.info("Process call. Stmt: {}", stmt);
            CG.add(m);
            analysis(M.getIR());

            List<Var> a = stmt.getInvokeExp().getArgs();
            List<Var> p = M.getIR().getParams();
            Integer length = a.size();
            logger.info("Param size: {}", length);

            for(int i=0; i<length; i++){
                Var ai = a.get(i);
                Var pi = p.get(i);
                PtrVar aptr = new PtrVar(ai);
                PtrVar pptr = new PtrVar(pi);
                logger.info("Param from a: {}", aptr);
                logger.info("Param from p: {}", pptr);
                AddEdge(aptr, pptr);
            }
            Var r = stmt.getLValue();
            if (r != null) {
                M.getIR().getReturnVars().forEach(mret->{
                    PtrVar rptr = new PtrVar(r);
                    PtrVar mretptr = new PtrVar(mret);
                    logger.info("Param from a: {}", rptr);
                    logger.info("Param from p: {}", mretptr);
                    AddEdge(mretptr, rptr);
                });
            }
            Debug(logger);
        }
    }

    public void Debug(Logger log)
    {
        if (logger == null) logger = log;
        //logger.info("HereCnt: {}", Herecnt);
        //logger.info("NewCnt: {}", Newcnt);
        //logger.info("CopyCnt: {}", Copycnt);
        logger.info("In WL:");
        logger.info("Size: {}", WL.size());
        WL.keySet().forEach(x->{
            logger.info("Var: {}, Pt: {}", x.toString(), WL.get(x).toString());
        });

        logger.info("In PT:");
        logger.info("Size: {}", PT.size());
        PT.keySet().forEach(x->{
            logger.info("Var: {}, Pt: {}", x.toString(), PT.get(x).toString());
        });

        logger.info("In PFG:");
        PFG.getNodes().forEach(x1->{
            logger.info("Node: {}", x1);
            logger.info("Success:");           
            PFG.getSuccsOf(x1).forEach(x2->{
                logger.info("Success Node: {}", x2);
            });
        });
        logger.info("=======================");
    }
}
