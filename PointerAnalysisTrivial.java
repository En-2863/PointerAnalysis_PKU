package pku;


import java.io.*;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonSerializable.Base;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import soot.coffi.field_info;


public class PointerAnalysisTrivial extends ProgramAnalysis<PointerAnalysisResult> {
    public static final String ID = "pku-pta-trivial";

    public static final Logger logger = LogManager.getLogger(IRDumper.class);

    /**
     * Directory to dump Result.
     */
    private final File dumpPath = new File("result.txt");

    public PointerAnalysisTrivial(AnalysisConfig config) {
        super(config);
        if (dumpPath.exists()) {
            dumpPath.delete();
        }
    }

    @Override
    public PointerAnalysisResult analyze() {
        var preprocess = new PreprocessResult();
        var result = new PointerAnalysisResult();
        var main = World.get().getMainMethod();

        /*World.get().getClassHierarchy().applicationClasses().forEach(jclass->{
            logger.info("Analyzing class {}", jclass.getName());
            jclass.getDeclaredMethods().forEach(method->{
                if(!method.isAbstract())
                    preprocess.analysis(method.getIR());
            });
        });

        var objs = new TreeSet<>(preprocess.obj_ids.values());

        preprocess.test_pts.forEach((test_id, pt)->{
            result.put(test_id, objs);
        });*/
        preprocess.Debug(logger);
        AddReachable(preprocess, main);
        preprocess.Debug(logger);

        while (!preprocess.WL.isEmpty()) 
        {    
            for (BaseVar n : preprocess.WL.keySet())
            {
                // delta = pts - pt(n)
                logger.info("Processing: {}", n);
                Set<Integer> delta = new HashSet<Integer>();
                delta.addAll(preprocess.WL.get(n));
                if (!preprocess.PT.containsKey(n)){
                    preprocess.PT.put(n, new HashSet<Integer>());
                }
                delta.removeAll(preprocess.PT.get(n));

                preprocess.WL.remove(n);
                Propagate(preprocess, n, delta);

                if (n instanceof PtrVar) { //n represents a variable x
                    for (Integer obj : delta)
                    {
                        Var x = ((PtrVar)n).v;
                        logger.info("Processing obj: {}", obj);
                        x.getStoreFields().forEach(stmt->{ //x.f = y
                            logger.info("Stmt: {}", stmt);
                            if (preprocess.S.contains(stmt)){
                                logger.info("Contains.");
                                JField field = stmt.getFieldRef().resolve();
                                PtrVar right = new PtrVar(stmt.getRValue());
                                FieldRefVar fieldvar = new FieldRefVar(field, obj);
                                preprocess.AddEdge(right, fieldvar);
                            }
                        });
                        x.getLoadFields().forEach(stmt->{ //y = x.f
                            logger.info("Stmt: {}", stmt);
                            if (preprocess.S.contains(stmt)){
                                logger.info("Contains.");
                                JField field = stmt.getFieldRef().resolve();
                                PtrVar left = new PtrVar(stmt.getLValue());
                                FieldRefVar fieldvar = new FieldRefVar(field, obj);
                                preprocess.AddEdge(fieldvar, left);
                            }
                        });
                        ProcessCall(preprocess, (PtrVar)n, obj);
                    }
                }
                break;
            }
        }

        preprocess.Debug(logger);
        preprocess.test_pts.forEach((test_id, pt)->{
            PtrVar ptptr = new PtrVar(pt);
            result.put(test_id, new TreeSet<Integer>());
            preprocess.PT.get(ptptr).forEach(i->{
                result.get(test_id).add(i);
            });
        });

        dump(result);

        return result;
    }

    protected void AddReachable(PreprocessResult preprocess, JMethod method)
    {
        preprocess.analysis(method.getIR());
    }

    protected void Propagate(PreprocessResult preprocess, BaseVar n, Set<Integer>pts)
    {
        if (pts.isEmpty()) return;

        if (!preprocess.PT.containsKey(n)){
            preprocess.PT.put(n, new HashSet<Integer>());
        }
        preprocess.PT.get(n).addAll(pts);

        for (BaseVar s : preprocess.PFG.getSuccsOf(n)){
            if (!preprocess.WL.containsKey(s)){
                preprocess.WL.put(s, new HashSet<Integer>());
            }
            preprocess.WL.get(s).addAll(pts); // Different Implementation.
        }
    }

    protected void ProcessCall(PreprocessResult preprocess, PtrVar n, Integer obj)
    {
        Var x = n.v;
        x.getInvokes().forEach(stmt->{
            JMethod M = preprocess.resolveCallee(preprocess.OBJ.get(obj), stmt);
            Var Mthis = M.getIR().getThis();
            PtrVar mthis = new PtrVar(Mthis);
            /*if (Mthis == M){
                logger.info("warning!");
            }*/
            //if (stmt.isVirtual()){
                logger.info("Type: {}", (preprocess.OBJ.get(obj)));
                logger.info("Mthis: {}", Mthis);
                logger.info("M: {}", M);
                logger.info("Var: {}, Integ: {}", n, obj);
            //}
            MethodRefVar m = new MethodRefVar(M, stmt.getLineNumber());
            
            if (!preprocess.WL.containsKey(mthis)){
                preprocess.WL.put(mthis, new HashSet<Integer>());
            }
            preprocess.WL.get(mthis).add(obj);
            logger.info("mthis: {}", mthis);

            /*//Testing
            Mthis.getStoreFields().forEach(fieldstmt->{
                FieldRefVar thisfield = new FieldRefVar(
                                        fieldstmt.getFieldRef().resolve(), obj);
                FieldRefVar stmtfield = new FieldRefVar(
                                        fieldstmt.getFieldRef().resolve(), obj);
                preprocess.AddEdge(thisfield, stmtfield);
            });*/

            if (!preprocess.CG.contains(m)) {
                logger.info("Process call. Stmt: {}", stmt);
                preprocess.CG.add(m);
                preprocess.Debug(logger);
                AddReachable(preprocess, M);

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
                    preprocess.AddEdge(aptr, pptr);

                    /*pi.getStoreFields().forEach(fieldstmt->{
                        FieldRefVar thisfield = new FieldRefVar(
                                                fieldstmt.getFieldRef().resolve(), obj);
                        FieldRefVar stmtfield = new FieldRefVar(
                                                fieldstmt.getFieldRef().resolve(), obj);
                        preprocess.AddEdge(thisfield, stmtfield);
                    });*/
                }

                Var r = stmt.getLValue();
                if (r != null) {
                    M.getIR().getReturnVars().forEach(mret->{
                        PtrVar rptr = new PtrVar(r);
                        PtrVar mretptr = new PtrVar(mret);
                        preprocess.AddEdge(rptr, mretptr);
                    });
                }
            }
            //if (stmt.isVirtual()) 
            preprocess.Debug(logger);
        });
    }

    protected void dump(PointerAnalysisResult result) 
    {
        try (PrintStream out = new PrintStream(new FileOutputStream(dumpPath))) {
            out.println(result);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump", e);
        }
    }
}
