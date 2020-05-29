import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np
from mpl_toolkits.mplot3d import Axes3D
import glob
from matplotlib import cm
from matplotlib.pyplot import rcParams
from math import sqrt
from matplotlib.ticker import EngFormatter

rcParams['font.family'] = 'serif'
rcParams['font.serif'] = ['DejaVu Serif']



def read_data(path):
    data = []
    with open(path) as file:
        for line in file:
            impl_runtime, pipeline, freq, opt_runtime, size, wirelength = line.split()
            onerun = []
            onerun.append(float(pipeline))
            onerun.append(float(impl_runtime))
            onerun.append(float(freq))
            onerun.append(float(opt_runtime))
            onerun.append(float(size))
            onerun.append(float(wirelength))
            data.append(onerun)
    return data


# a helper function for freq_depth
# produce frequency data point at each depth
def MinMaxAvg(datum, depth):
    freqs = []
    for line in datum:
        if abs(line[0] - depth) < 1e-5:
            freqs.append(line[2])  # freq
    mini = min(freqs)
    maxi = max(freqs)
    avg = sum(freqs) / len(freqs)
    return mini, maxi, avg


def freq_depth():
    fig, ax = plt.subplots(1)
    plt.style.use('seaborn-paper')
    fig.set_size_inches(8, 5)
    ax.autoscale(True)


    cma_data = read_data('./eval/CMA_eval.txt')
    ea_data = read_data('./eval/EA_eval.txt')
    sa_data = read_data('./eval/SA_eval.txt')
    ga_data = read_data('./eval/GA_eval.txt')
    ma_data = read_data('./eval/Manual_eval.txt')
    ear_data = read_data('./eval/EA-reduced_eval.txt')

    stages = [0,1,2,3,4]
    data = [cma_data, ea_data, sa_data, ga_data, ma_data, ear_data]
    labels = ["CMA-ES", "NSGA-II", "Annealing", "GA", "Manual", "NSGA-II Reduced"]
    markers = ['*', "^", "o", "D", ">", "^"]
    color = ['#1f77b4', '#ff7f03', '#2ca02c', '#17becf', '#d62728', '#e377c2']

    for i, stage in enumerate(data):
        c = color[i]
        label = labels[i]
        marker = markers[i]
        datum = data[i]
        # I need min, max, avg at each depth
        avgs = []
        lower = []
        upper = []
        for d in stages:
            min, max, avg = MinMaxAvg(datum, d)
            avgs.append(avg)
            lower.append(min)
            upper.append(max)
        plt.plot(stages, avgs, marker=marker, label=label, markersize=15, color=c)
        plt.fill_between(stages, lower, upper, alpha=0.2, color=c)


    plt.axhline(650, color='red', linestyle='--')


    ax.legend(prop={'size': 16}, loc='upper center', frameon=False, bbox_to_anchor=(0.5, 1.21), ncol=3)
    ax.set_xlim([-0.2, 4.2])
    plt.xlabel("pipelining depth", fontsize=20)
    plt.ylabel("clock frequency (MHz)", fontsize=20)

    plt.xticks([0, 1, 2, 3, 4])
    ax.tick_params(axis='both', which='major', labelsize=16)
    plt.subplots_adjust(bottom=0.15)
    plt.savefig('frequency_depth.pdf')





if __name__ == "__main__":
    freq_depth()