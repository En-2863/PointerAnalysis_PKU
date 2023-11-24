package pku;

import java.util.Optional;

import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;

class BaseVar{
	public Var v;
	public Integer type;
	public Integer hash;
	//public isptr = 0;
	//public isfield = 1;
	//public ismethod = 2;
	//public isstaticfield = 3;
	//public isstaticmethod = 4;
	BaseVar(Integer t){
		type = t;
		hash = (int)(Math.random() * 10000);
	}

	BaseVar(Var a, Integer t) {
		v = a;
		type = t;
		hash = (int)(Math.random() * 10000);
	};

	@Override 
	public boolean equals(Object obj) {
		if (obj instanceof BaseVar){
			if (((BaseVar)obj).type == type) {
				if (type > 2) return true;
				if (((BaseVar)obj).v == v) return true;
			}
		}
		return false;
	}

    public int hashCode() {
		return hash;
    }
}

class PtrVar extends BaseVar{
	PtrVar(Var a) {
		super(a, 0);
	};
}

class FieldRefVar extends BaseVar{
	public JField field;
	public Integer o;
	FieldRefVar(Var a, JField f, Integer i) {
		super(a, 1);
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
}

class MethodRefVar extends BaseVar{
	public JMethod method;
	public Integer o;
	MethodRefVar(Var a) {
		super(a, 2);
	};

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj)){
			if (((MethodRefVar)obj).method == method &&
				((MethodRefVar)obj).o == o) return true;
		}
		return false;
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
}