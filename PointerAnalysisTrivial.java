package pku;


import java.io.*;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonSerializable.Base;

import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;


public class PointerAnalysisTrivial extends ProgramAnalysis<PointerAnalysisResult> {
    public static final String ID = "pku-pta-trivial";

    private static final Logger logger = LogManager.getLogger(IRDumper.class);

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
        AddReachable(preprocess, main);

        while (!preprocess.WL.isEmpty()) 
        {    
            for (BaseVar n : preprocess.WL.keySet())
            {
                // delta = pts - pt(n)
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
                        x.getStoreFields().forEach(stmt->{ //x.f = y
                            JField field = stmt.getFieldRef().resolve();
                            PtrVar right = new PtrVar(stmt.getRValue());
                            FieldRefVar fieldvar = new FieldRefVar(x, field, obj);
                            preprocess.AddEdge(right, fieldvar);
                        });
                        x.getLoadFields().forEach(stmt->{ //y = x.f
                            JField field = stmt.getFieldRef().resolve();
                            PtrVar left = new PtrVar(stmt.getLValue());
                            FieldRefVar fieldvar = new FieldRefVar(x, field, obj);
                            preprocess.AddEdge(fieldvar, left);
                        });
                        ProcessCall(preprocess, x, obj);
                    }
                }
                break;
            }
        }

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

    protected void ProcessCall(PreprocessResult preprocess, Var x, Integer obj)
    {
        x.getInvokes().forEach(stmt->{
            
        });
    }

    protected void dump(PointerAnalysisResult result) {
        try (PrintStream out = new PrintStream(new FileOutputStream(dumpPath))) {
            out.println(result);
        } catch (FileNotFoundException e) {
            logger.warn("Failed to dump", e);
        }
    }

}
