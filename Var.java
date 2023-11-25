package pku;

import java.util.Optional;

import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

class BaseVar{
	public Integer type;
	//public isptr = 0;
	//public isfield = 1;
	//public ismethod = 2;
	//public isstaticfield = 3;
	//public isstaticmethod = 4;
	BaseVar(Integer t){
		type = t;
	}

	@Override 
	public boolean equals(Object obj) {
		if (obj instanceof BaseVar){
			if (((BaseVar)obj).type == type) {
				return true;
			}
		}
		return false;
	}

    public int hashCode() {
		return type;
    }
}

class PtrVar extends BaseVar{
	public Var v;

	PtrVar(Var a) {
		super(0);
		v = a;
	};

	@Override
	public String toString(){
		return v.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj)){
			if (((PtrVar)obj).v == v) return true;
		}
		return false;
	} 
}

class FieldRefVar extends BaseVar{
	public JField field;
	public Integer o;
	FieldRefVar(JField f, Integer i) {
		super(1);
		field = f;
		o = i;
	};

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj)){
			if (((FieldRefVar)obj).field == field &&
				((FieldRefVar)obj).o == o) return true;
		}
		return false;
	}

	@Override
	public String toString(){
		return o.toString() + '.' + field.toString();
	}
}

class MethodRefVar extends BaseVar{
	public JMethod method;
	public Integer line;
	MethodRefVar(JMethod m, Integer l) {
		super(2);
		method = m;
		line = l;
	};

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj)){
			if (((MethodRefVar)obj).method == method &&
				((MethodRefVar)obj).line == line) return true;
		}
		return false;
	}

	@Override
	public String toString(){
		return line + " -> " + method.toString();
	}
}

class StaticFieldRefVar extends BaseVar{
	public JField field;
	public JClass cls;

	StaticFieldRefVar(JField f, JClass c){
		super(3);
		field = f;
		cls = c;
	}

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj)){
			if (((StaticFieldRefVar)obj).field == field &&
				((StaticFieldRefVar)obj).cls == cls) return true;
		}
		return false;
	}

	@Override
	public String toString(){
		return cls.toString() + '.' + field.toString();
	}
}

class StaticMethodRefVar extends BaseVar{
	public JMethod method;
	public JClass cls;

	StaticMethodRefVar(JMethod m, JClass c){
		super(4);
		method = m;
		cls = c;
	}

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj)){
			if (((StaticMethodRefVar)obj).method == method &&
				((StaticMethodRefVar)obj).cls == cls) return true;
		}
		return false;
	}

	@Override
	public String toString(){
		return cls.toString() + '.' + method.toString();
	}
}