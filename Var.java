package pku;

import java.util.Optional;

import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

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
		return type;
    }
}

class PtrVar extends BaseVar{
	Type ty;

	PtrVar(Var a) {
		super(a, 0);
	};

	PtrVar(Var a, Type x) {
		super(a, 0);
		ty = x;
	};

	@Override
	public String toString(){
		return v.toString();
	}
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

	@Override
	public String toString(){
		return v.toString() + o.toString() + '.' + field.toString();
	}
}

class MethodRefVar extends BaseVar{
	public JMethod method;
	public Integer o;
	public Integer line;
	MethodRefVar(Var a, JMethod m, Integer i, Integer l) {
		super(a, 2);
		method = m;
		o = i;
		line = l;
	};

	@Override
	public boolean equals(Object obj) {
		if(super.equals(obj)){
			if (((MethodRefVar)obj).method == method &&
				((MethodRefVar)obj).o == o &&
				((MethodRefVar)obj).line == line) return true;
		}
		return false;
	}

	@Override
	public String toString(){
		return v.toString() + o.toString() + '.' + method.toString();
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