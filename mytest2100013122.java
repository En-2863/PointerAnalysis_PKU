package test;

import benchmark.internal.BenchmarkN;
import benchmark.objects.A;



class B {
    public void f() {
        BenchmarkN.alloc(1);
        A a = new A();
        BenchmarkN.test(1, a);
    }
}

class C extends B {
    @Override
    public void f() {
        BenchmarkN.alloc(2);
        A a = new A();
        BenchmarkN.test(2, a);
    }
}

class D extends C{
    @Override
    public void f(){
        BenchmarkN.alloc(3);
        A a=new A();
        BenchmarkN.test(3,a);
    }
}

public class mytest2100013122 {

    public static class E {
        public B f;
        public E a;
    }

    public static E e1;
    public static int x;

    public static void r(int x) {
        if (x == 0)
            return;
        BenchmarkN.alloc(4);
        B b = new B();
        BenchmarkN.test(4, b);
        r(x - 1);
    }
    public static void main(String[] args) {
        B b = new B();
        b.f();
        B c = new C();
        c.f();
        D d=new D();
        d.f();
        r(3);

        int l = args.length;
        x = l;
        e1 = new E();
        E e2 = new E();
        BenchmarkN.alloc(5);
        B b1 = new B();
        BenchmarkN.alloc(6);
        B b2 = new B();
        e2.f = b1;
        e1.a = e2;
        BenchmarkN.test(5, e1.a.f);
        


    }
}
