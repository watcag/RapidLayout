# RapidLayout：Fast Hard Block Placement of FPGA-optimized Systolic Arrays using Evolutionary Algorithms

We provide an end-to-end hard block placement and routing flow for
for systolic accelerators on FPGA, RapidLayout. RapidLayout is built
on top of [RapidWright](https://www.rapidwright.io/), an implementation
framework by Xilinx, [Opt4J](http://opt4j.sourceforge.net/), a Java meta-heuristic
optimization tool, and [Apache Commons Math](http://commons.apache.org/proper/commons-math/javadocs/api-3.4/org/apache/commons/math3/optim/nonlinear/scalar/noderiv/CMAESOptimizer.html)
library for CMA-ES.

RapidLayout is written in Java, accompanied with a set of python scripts for
placement visualization. 

## How to use RapidLayout

### Install Dependencies

- First thing we need is Java. To compile and run Java programs, we need *Java Development Kit* (JDK)
 1.8 or later, which can be downloaded [here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html). 
 Also make sure to add `$(YOUR_JDK_INSTALL_LOCATION)/jdk1.x.x_x/bin` path to `PATH` environment variable.
 
- To make life easier we use [Gradle](https://gradle.org/) build system, to build our Java program. Installation instruction can
be found [here](https://gradle.org/install/#manually). Also make sure to add `$(GRADLE_FOLDER/gradle-x.x/bin)` to `PATH` varible.

- Clone the repo, and download `data` and `jars` from [OneDrive](https://1drv.ms/u/s!An0eQqECDpELgc1gnYi2xqn51BA8mA?e=lXNXeT) or [Google Drive](https://drive.google.com/open?id=17LK4n57N6pnM3fwj0WF4kGqtcUtypi8d).
 Put these two folders inside RapidLayout project folder, like this:
 ```
    RapidLayout
      ├── README.md
      ├── build.gradle
      ├── data
      ├── gradle
      ├── gradlew
      ├── gradlew.bat
      ├── jars
      ├── result
      ├── src
      ├── tcl
      └── visual
``` 
- Then, we set up few other environment variables:
   * Configure RapidWright Path: 
   ```export RAPIDWRIGHT_PATH=<THE_REPO_PATH>```
   * Configure Vivado Path:
   `export VIVADO_PATH=PATH_TO_VIVADO/bin` e.g. `export VIVADO_PATH=/Vivado/2018.3/bin`
   * Add compiled Java files and jars to `CLASSPATH` variable: 
   
   ```export CLASSPATH=$RAPIDWRIGHT_PATH/bin:$(echo $RAPIDWRIGHT_PATH/jars/*.jar | tr ' ' ':')```

### Build Project

We build the Java project with gradle build:
```
gradle build -p $RAPIDWRIGHT_PATH
```

### Configuration
RapidLayout's work flow is configured with `config.properties` file in the project
root folder. The parameters are illustrated as follows.

- `device`: `vu11p`, `vu13p` are tested for now.
- `method`: method can be `CMA`, `EA`, `EA-reduced`,or `SA`, standing for 
CMA-ES, NSGA-II, NSGA-II with reduced genotype, and Simulated Annealing respectively.
- `optimization`: `true` or `false`. Whether to rerun the placement search process. 
When turned `false`, it will seek for a solution `xdc` file in the `result` folder.
- `rapidSynth`: `true` or `false`. RapidSynth saves synthesis time by replicating 
conv unit's Netlist. If enabled, only 1 conv block will be synthesized by Vivado,
and the rest will be replicated from it. Otherwise, all conv units will be synthesized 
by Vivado and no replication will happen.
- `SLRCopy`： `true` or `false`. If enabled, only one SLR will be routed by Vivado, then
 the other SLRs' routed physical netlist will be copied from it. SLR copying accelerates
 runtime by 5x-6x. 
- `autoPipeline`: `true` or `false`. If enabled, `pipelineDepth` will be ignored, and pipeline
depth for each data path will be determined by RapidLayout.
- `pipelineDepth`: integer, e.g. `4`. Effective only when `autoPipeline` is disabled. All
datapath will be pipelined with the specifed depth.
- `vivado_verbose`: `true` or `false`. Whether to print Vivado output information.
- `opt_visual`: `true` or `false`. Whether to display real-time visualization of objectives. 
This function is currently not available for CMA-ES. 
- `matplotlib_visual`: `true` or `false`. Whether to draw placement visualization for 
optimization results. 
- `collect_gif_data`: `true` or `false`. Collect placement evolutionary data to visualize
the evolving process for current method.
- `collect_converge_data`: `true` or `false`. Collect objective data of each iteration
for visualization.

### Run

To run the complete workflow, go to project root folder, then run:
```bash
    java -Xmx80G main.AutoPlacement
```

### License
This tool is distributed under MIT license.

Copyright (c) 2020 Niansong Zhang, Nachiket Kapre

<div style="text-align: justify;"> 
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
<br><br>
</div>


<div style="text-align: justify;"> 
<b>The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.</b>
<br><br>
</div>


<div style="text-align: justify;"> 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 </div>

