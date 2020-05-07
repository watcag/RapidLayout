import argparse
import glob
import os
from math import sqrt

import matplotlib.pyplot as plt
import numpy as np
from matplotlib.pyplot import rcParams
from matplotlib.ticker import EngFormatter

rcParams['font.family'] = 'serif'
rcParams['font.serif'] = ['DejaVu Serif']

if __name__ == "__main__":
    plt.style.use('seaborn-paper')
    fig, ax = plt.subplots(1)
    fig.set_size_inches(5, 4)
    threads = [1,2,4,8,16,20]
    time = [130, 82, 56, 47, 43, 41]
    ax.plot(threads, time, marker='^',  markersize=15)
    ax.set_xlabel("number of threads", fontsize=20)
    ax.set_ylabel("runtime per iteration (ms)", fontsize=20)
    plt.xticks(threads)
    ax.tick_params(axis='both', which='major', labelsize=16)
    ax.tick_params(axis='both', which='minor', labelsize=16)
    plt.tight_layout()
    plt.savefig('/Users/zhangniansong/RapidWright/visual/multi-thread.pdf')