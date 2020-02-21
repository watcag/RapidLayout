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

def read_convergence_data(path):
    steps = []
    spreads = []
    wirelengths = []
    with open(path) as file:
        i = 0
        for line in file:
            if i % 100 != 0:
                i += 1
                # continue
            step, wirelength, spread = line.split()
            # print('time = %s, spread = %s, wirelength = %s' % (time, spread, wirelength))
            steps.append(float(step))
            spreads.append(float(spread))
            wirelengths.append(float(wirelength))
            i += 1
    return steps, wirelengths, spreads


def calc_error_bar(path, ll, wl=0):
    runs = glob.glob(path)
    upper = []
    avg = []
    lower = []
    all = []
    lengths = []
    for run in runs:
        step, wirelength, sp = read_convergence_data(run)
        data = wirelength if wl == 1 else sp
        if wl == 1 and 'cma' not in path:
            for i, d in enumerate(data): data[i] = sqrt(d)
        all.append(data)
        lengths.append(len(data))
    all_array = np.array(all)
    cols = min(lengths)
    if ll == -1: ll = cols  # for SA, ll input -1
    for i in range(ll):
        this_iteration = []
        for each_run in all_array:
            if i < cols:
                this_iteration.append(each_run[i])
            else:
                this_iteration.append(each_run[-1])
        this_max = max(this_iteration)
        this_min = min(this_iteration)
        this_avg = sum(this_iteration) / len(this_iteration)
        avg.append(this_avg)
        upper.append(this_max)
        lower.append(this_min)

    it = range(len(avg))
    return it, avg, lower, upper


def plot_convergence(savePath):

    resultDir = os.environ['RAPIDWRIGHT_PATH'] + "/result"

    plt.style.use('seaborn-paper')
    fig, (ax, ax2, ax3) = plt.subplots(1, 3)
    fig.set_size_inches(21, 4.7)
    ax.autoscale(True)

    ax.set_yscale('log')
    ax2.set_yscale('log')
    ax3.set_yscale('log')

    # wirelength^2
    it, sa_avg, sa_lower, sa_upper = calc_error_bar(resultDir + '/SA_convergence_data/*.txt', wl=1, ll=-1)
    for i, v in enumerate(sa_avg): sa_avg[i] = v * v
    for i, v in enumerate(sa_lower): sa_lower[i] = v * v
    for i, v in enumerate(sa_upper): sa_upper[i] = v * v

    ax.plot(it, sa_avg, linestyle=':', label="Annealing")
    ax.fill_between(it, sa_lower, sa_upper, alpha=0.2)
    ll = len(it)
    # NSGA-II, NSGA-II reduced, CMA-ES
    linestyles = ['-.', '--', '-']
    runs_list = [resultDir + '/EA_convergence_data/*.txt',
                 resultDir + '/EA-reduced_convergence_data/*.txt',
                 resultDir + '/cma_convergence_data/*.txt']
    labels = ['NSGA-II', 'NSGA-II(Red)', 'CMA-ES']

    for idx in range(len(runs_list)):
        it, avg, lower, upper = calc_error_bar(runs_list[idx], wl=1, ll=ll)
        for i, v in enumerate(avg): avg[i] = v * v
        for i, v in enumerate(lower): lower[i] = v * v
        for i, v in enumerate(upper): upper[i] = v * v
        ax.plot(it, avg, linestyle=linestyles[idx], label=labels[idx])
        ax.fill_between(it, lower, upper, alpha=0.2)

    # ax.legend(prop={'size': 18})
    ax.set_xlabel("iterations", fontsize=20)
    ax.set_ylabel("$\mathrm{wirelength^2}$", fontsize=20)

    ## bbox width
    ax2.autoscale(True)
    # Annealing
    it, sa_avg, sa_lower, sa_upper = calc_error_bar(resultDir + '/SA_convergence_data/*.txt', wl=0, ll=-1)
    ax2.plot(it, sa_avg, linestyle=':', label="Annealing")
    ax2.fill_between(it, sa_lower, sa_upper, alpha=0.2)
    ll = len(it)
    # NSGA-II, NSGA-II reduced, CMA-ES
    linestyles = ['-.', '--', '-']
    runs_list = [resultDir + '/EA_convergence_data/*.txt',
                 resultDir + '/EA-reduced_convergence_data/*.txt',
                 resultDir + '/cma_convergence_data/*.txt']
    labels = ['NSGA-II', 'NSGA-II(Red)', 'CMA-ES']

    for idx in range(len(runs_list)):
        it, avg, lower, upper = calc_error_bar(runs_list[idx], wl=0, ll=ll)
        ax2.plot(it, avg, linestyle=linestyles[idx], label=labels[idx])
        ax2.fill_between(it, lower, upper, alpha=0.2)

    ax2.set_xlabel("iterations", fontsize=20)
    ax2.set_ylabel("max bbox size", fontsize=20)

    # wirelength ^ 2 * bbox
    ax3.autoscale(True)
    it, sa_avg, sa_lower, sa_upper = calc_error_bar(resultDir + '/SA_convergence_data/*.txt', wl=0, ll=-1) # spread
    _,  sa_wl_avg, sa_wl_lower, sa_wl_upper = calc_error_bar(resultDir + '/SA_convergence_data/*.txt', wl=1, ll=-1) # wirelength
    for i, v in enumerate(sa_avg): sa_avg[i] = v * v * sa_wl_avg[i]
    for i, v in enumerate(sa_lower): sa_lower[i] = v * v * sa_wl_lower[i]
    for i, v in enumerate(sa_upper): sa_upper[i] = v * v * sa_wl_upper[i]

    ax3.plot(it, sa_avg, linestyle=':', label="Annealing")
    ax3.fill_between(it, sa_lower, sa_upper, alpha=0.2)
    ll = len(it)
    # NSGA-II, NSGA-II reduced, CMA-ES
    linestyles = ['-.', '--', '-']
    runs_list = [resultDir + '/EA_convergence_data/*.txt',
                 resultDir + '/EA-reduced_convergence_data/*.txt',
                 resultDir + '/cma_convergence_data/*.txt']
    labels = ['NSGA-II', 'NSGA-II(Red)', 'CMA-ES']

    for idx in range(len(runs_list)):
        it, avg, lower, upper = calc_error_bar(runs_list[idx], wl=0, ll=ll)
        _, wl_avg, wl_lower, wl_upper = calc_error_bar(runs_list[idx], wl=1, ll=ll)  # wirelength
        for i, v in enumerate(avg): avg[i] = v * v * wl_avg[i]
        for i, v in enumerate(lower): lower[i] = v * v * wl_lower[i]
        for i, v in enumerate(upper): upper[i] = v * v * wl_upper[i]
        ax3.plot(it, avg, linestyle=linestyles[idx], label=labels[idx])
        ax3.fill_between(it, lower, upper, alpha=0.2)

    ax3.set_xlabel("iterations", fontsize=20)
    ax3.set_ylabel("$\mathrm{BboxSize \\times wirelength^2}$", fontsize=20)


    handles, labels = ax3.get_legend_handles_labels()
    fig.legend(handles, labels, loc='upper center', prop={'size': 20}, ncol=4, bbox_to_anchor=(0.5, 0.94),
               frameon=False)

    ax.tick_params(axis='both', which='major', labelsize=16)
    ax.tick_params(axis='both', which='minor', labelsize=16)
    ax2.tick_params(axis='both', which='major', labelsize=16)
    ax2.tick_params(axis='both', which='minor', labelsize=16)
    ax3.tick_params(axis='both', which='major', labelsize=16)


    formatter0 = EngFormatter(unit=' ')
    ax.xaxis.set_major_formatter(formatter0)
    ax2.xaxis.set_major_formatter(formatter0)
    ax2.yaxis.set_major_formatter(formatter0)
    ax3.xaxis.set_major_formatter(formatter0)

    plt.tight_layout()

    plt.subplots_adjust(bottom=0.15)
    plt.subplots_adjust(top=0.83)

    outputImg = savePath + "/convergence.pdf"
    plt.savefig(outputImg)
    print("Image Saved to: " + outputImg)



if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='Please input image saving directory')
    parser.add_argument('imgPath', type=str, help='the image saving directory, no ending slash, e.g. ~/home/img')
    args = parser.parse_args()

    imgPath = args.imgPath

    if not os.path.isdir(imgPath):
       os.makedirs(imgPath)
       print("[Python] created path: {}".format(imgPath))

    plot_convergence(imgPath)