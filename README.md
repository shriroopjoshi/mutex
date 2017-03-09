# mutex

Implementation of Ricart-Agrawala distributed mutual exclusion algorithm

This project is completed as a part of Advanced Operating Systems course. Project contains two phases.
In first phase, all processes wait for 5-10 time units, and enter critical section. In second phase, even numbered processes wait for 45-50 tim units before entering critical section.
All process start and terminate by sending message to PZERO.
All configurations for the algorithm can be found in <code>resources/config.properties</code>.

### How to build

System requirements:
* Java (JDK/JRE 1.8)
* Apache Ant Runtime
* GSON

Steps to build:<br>
1. Place the GSON binary in <a href="lib">mutex/lib</a> directory<br>
2. Execute the following ant commands:<br>
&nbsp;&nbsp;&nbsp;<code>ant clean</code><br>
&nbsp;&nbsp;&nbsp;<code>ant</code> <br>

This will create an executable in <a href="build">mutex/build</a> directory<br>

### How to execute
1. Add IP address of PZERO to <code>processzero.host</code> in <a href="resources/config.properties">resources/config.properties</a> file<br>
2. Execute <a href="run.sh">run.sh</a> for starting process.<br>
&nbsp;&nbsp;&nbsp;To start PZERO:<br>
&nbsp;&nbsp;&nbsp;<code>./run.sh 0</code><br>
&nbsp;&nbsp;&nbsp;To start a process:<br>
&nbsp;&nbsp;&nbsp;<code>./run.sh</code>
