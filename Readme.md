## 软件分析技术lab1 实验报告
### 组员及分工：</br>张伟 2100013122 算法搜集 代码编写 性能优化</br>寿晨宸 2100012945 测试样例编写与提交</br>于佳琨 2100013119 报告编写

### 项目github链接： https://github.com/En-2863/PointerAnalysis_PKU

## 1. 算法介绍
算法参考南京大学指针分析算法，基于非上下文敏感的指针分析。

#### 1.1 Solve($m^{entry}$)<br/>
&emsp;$WL=[ ],PFG=\{\},S=\{\},RM=\{\},CG =\{\}$<br/>
&emsp;AddReachable($m^{entry}$)<br/>
&emsp;while $WL$ is not empty $do$<br/>
&emsp;&emsp;remove $<n,pts>$ from $WL$<br/>
&emsp;&emsp;$Δ = pts – pt(n)$<br/>
&emsp;&emsp;Propagate($n, Δ$)<br/>
&emsp;&emsp;if $n$ represents a variable $x$ then<br/>
&emsp;&emsp;&emsp;foreach $o_i ∈ Δ$ do<br/>
&emsp;&emsp;&emsp;&emsp;foreach $x.f = y ∈ S $ do<br/>
&emsp;&emsp;&emsp;&emsp;&emsp;AddEdge($y, o_i.f$)<br/>
&emsp;&emsp;&emsp;&emsp;foreach $y = x.f ∈ S$ do <br/>
&emsp;&emsp;&emsp;&emsp;&emsp;AddEdge($o_i.f, y$)<br/>
&emsp;&emsp;&emsp;&emsp;ProcessCall($x, o_i$)<br/>

其中：$WL$代表工作列表， $PFG$代表指针流图， $S$代表可达语句集合，$S_m$代表方法 $m$ 中的语句集合，$RM$代表可达方法集合，$CG$代表调用图的边的集合。

#### 1.2 AddEdge($s,t$)<br/>
&emsp;if $s \rightarrow t ∉ PFG$ then<br/>
&emsp;&emsp;add $s \rightarrow t$ to $PFG$<br/>
&emsp;&emsp;if $pt(s)$ is not empty then<br/>
&emsp;&emsp;&emsp;add $<t,pt(s)>$ to $WL$

AddEdge函数：用于在指针流图中添加边，如果$s \rightarrow t $不在PFG中，添加$s \rightarrow t $， 然后通过检验确保$s$指向的所有object被$t$指向。

#### 1.3 Propagate($n,pts$)<br/>
&emsp;if $pts$ is not empty then<br/>
&emsp;&emsp;$pt(n) ⋃= pts$<br/>
&emsp;&emsp;foreach $n \rightarrow s ∈ PFG$ do<br/>
&emsp;&emsp;&emsp;add<$s,pts$> to $WL$<br/>

Propagate函数：将pts加入到n的points-to 关系集合中<,并将pts传播给$n$的后继结点.

#### 1.4 AddReachable($m$)<br/>
&emsp;if $m∉ RM$ then<br/>
&emsp;&emsp;add $m$ to $RM$<br/>
&emsp;&emsp;$S∪= S_m$<br/>
&emsp;&emsp;foreach $i: x = new$ &nbsp; $T() ∈ S_𝑚$ do<br/>
&emsp;&emsp;&emsp;add $<x, \{o_i\}>$ to $WL$<br/>
&emsp;&emsp;foreach $x = y ∈ S_𝑚$ do<br/>
&emsp;&emsp;&emsp;AddEdge($y, x$)

AddReachable函数: 根据method $m$添加新的reachable method and statements,首先判断$m$是否在$RM$中，如果需要补充，则将$m$添加到$RM$中，并将method $m$中的语句添加到可达语句集合$S$中,接下来对于$S_m$中的new语句进行处理，将对应的$<x,\{o_i\}>$加入工作集,最后处理赋值语句，为赋值语句添加相应的边。

#### 1.5 ProcessCall($x, o_i$)<br/>
&emsp;foreach $l: r = x.k(a1,…,an) ∈ S$ do<br/>
&emsp;&emsp;$𝑚$ = Dispatch($o_i, k$)<br/>
&emsp;&emsp;add $<m_{this},{o_i}>$ to $WL$<br/>
&emsp;&emsp;if $l \rightarrow 𝑚 ∉ CG$ then<br/>
&emsp;&emsp;&emsp;add $l \rightarrow m$ to $CG$,<br/>
&emsp;&emsp;&emsp;AddReachable($m$)<br/>
&emsp;&emsp;&emsp;foreach parameter $p_i$ of $m$ do<br/>
&emsp;&emsp;&emsp;&emsp;AddEdge($a_i, p_i$)<br/>
&emsp;&emsp;&emsp;AddEdge($m_{ret}, r$)

ProcessCall函数：在实例内部对于每个调用语句进行相应操作，首先，使用 Dispatch 函数解析虚拟分派，确定调用 $k $在 $o_i$ 上的目标方法$m$，然后将目标方法$m$和对应的$o_i$添加到工作列表$WL$中，接下来判断 $l \rightarrow 𝑚$ 是否存在，如果不存在，将调用边 $l \rightarrow m$ 添加到调用图$CG$中，之后使用AddReachable函数根据method $m$添加新的reachable method and statements，最后对于$m$的每个参数$p_i$以及$m_{ret}$进行加边操作。


#### 1.6 算法流程如下：<br/>
对于$m^{entry}$,先利用AddReachable现将其设置为可达状态<br/>
在工作列表不为空的情况下，从工作列表中移除指针n及其的一个实例pts,<$n,pts$><br/>
对指针n进行传播处理，传播$Δ=pts-pt(n)$<br/>
如果n代表的是变量x<br/>
则对于 $Δ$ 中的每个实例 $o_i$ 进行以下操作：<br/>
对于每个 $x.f = y$ 的语句，添加从 $y$ 到 $o_i.f$ 的边;<br/>
对于每个 $y = x.f$ 的语句，添加从 $o_i.f$ 到 $y$ 的边;<br/>
调用 ProcessCall($x, o_i$) 处理函数调用

#### 1.7  针对静态字段、数组索引和静态方法调用，引入新的指针分析规则来处理

##### 1.7.1 静态字段：
处理方法：在静态字段和变量之间传值<br/><br/>
$Static$ &nbsp;$Store$:<br/>
语句为：$T.f=y$，规则为：$\frac{o_i∈pt(y)}{o_i∈pt(T.f)}$，
PFG边的表示为：$T.f \leftarrow y$<br/><br/>
$Static$ &nbsp;$Load$:<br/>
语句为：$y=T.f$，规则为：$\frac{o_i∈pt(T.f)}{o_i∈pt(y)}$，
PFG边的表示为：$y \leftarrow T.f$

##### 1.7.2 静态索引：
处理方法：<br/><br/>
$Array$ &nbsp;$Store$:<br/>
语句为：$x[i]=y$，规则为：$\frac{o_u∈pt(x),o_v∈pt(y)}{o_v∈pt(o_u[*])}$，
PFG边的表示为：$o_u[*] \leftarrow y$<br/><br/>
$Array$ &nbsp;$Load$:<br/>
语句为：$y=x[i]$，规则为：$\frac{o_u∈pt(x),o_v∈pt(o_u[*])}{o_v∈pt(y)}$，
PFG边的表示为：$ y\leftarrow o_u[*]$<br/><br/>

##### 1.7.3 静态方法：
处理方法：<br/><br/>
$Static$ &nbsp;$Call$:<br/>
语句为：$r = T.m(a1,...,an)$，规则为：$\frac{o_u∈pt(a_j),1\leq j\leq n ,o_v∈pt(m_{ret})}{o_u∈pt(m_{pj}),1\leq j\leq n,o_v∈pt(r)}$，<br/>
PFG边的表示为：$a_1 \rightarrow m_{p1}$ &nbsp; $...$  &nbsp;$a_n \rightarrow m_{pn}$ &nbsp; $,$ &nbsp;$r \rightarrow m_{ret}$<br/><br/>

## 2. 参考文献
https://cs.nju.edu.cn/tiantan/software-analysis/PTA-FD.pdf <br/>
https://tai-e.pascal-lab.net/pa5.html <br/>
https://blog.csdn.net/m0_53632564/article/details/127255320 <br/>




