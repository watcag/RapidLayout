import argparse
import glob
import os
from math import sqrt

import matplotlib.pyplot as plt
from matplotlib.pyplot import rcParams
from matplotlib.ticker import EngFormatter

rcParams['font.family'] = 'serif'
rcParams['font.serif'] = ['DejaVu Serif']

def read_convergence_data(path):
    steps = []
    spreads = []
    wirelengths = []
    with open(path) as file:
        for line in file:
            step, wirelength, spread = line.split()
            if "EA" in path or "SA" in path:
                wirelength = sqrt(float(wirelength))
            # print('time = %s, spread = %s, wirelength = %s' % (step, spread, wirelength))
            if float(step) > 30000: continue
            steps.append(float(step))
            spreads.append(float(spread))
            wirelengths.append(float(wirelength))
        if steps[-1] != 30000:
            steps.append(30000)
            spreads.append(spreads[-1])
            wirelengths.append(wirelengths[-1])
    return steps, wirelengths, spreads


def calc_error_bar_wr2s(path):
    runs = glob.glob(path)
    upper = []
    avg = []
    lower = []
    lengths = []
    data = []
    steps = []
    for run in runs:
        # read data of one run
        step, wirelength, sp = read_convergence_data(run)
        # collect score, score = wirelength^2 * bbox
        d = []
        for i in range(len(wirelength)):
            d.append(wirelength[i] * wirelength[i] * sp[i])
        data.append(d)
        lengths.append(len(d))
        if len(step) > len(steps):
            steps.clear()
            steps = step

    # make data of all runs of the same length
    longest_length = max(lengths)
    for line in data:
        while len(line) < longest_length:
            last_value = line[-1]
            line.append(last_value)

    # calculate the average, upper and lower
    for index in range(longest_length):
        this_run = []
        for run in range(len(runs)):
            this_run.append(data[run][index])
        this_upper = max(this_run)
        this_lower = min(this_run)
        this_avg = sum(this_run) / len(this_run)
        upper.append(this_upper)
        lower.append(this_lower)
        avg.append(this_avg)

    return steps, avg, upper, lower

def calc_error_bar_wr2(path):
    runs = glob.glob(path)
    upper = []
    avg = []
    lower = []
    lengths = []
    data = []
    steps = []
    for run in runs:
        # read data of one run
        step, wirelength, sp = read_convergence_data(run)
        # collect score, score = wirelength^2 * bbox
        d = []
        for i in range(len(wirelength)):
            d.append(wirelength[i] * wirelength[i])
        data.append(d)
        lengths.append(len(d))
        if len(step) > len(steps):
            steps.clear()
            steps = step

    # make data of all runs of the same length
    longest_length = max(lengths)
    for line in data:
        while len(line) < longest_length:
            last_value = line[-1]
            line.append(last_value)

    # calculate the average, upper and lower
    for index in range(longest_length):
        this_run = []
        for run in range(len(runs)):
            this_run.append(data[run][index])
        this_upper = max(this_run)
        this_lower = min(this_run)
        this_avg = sum(this_run) / len(this_run)
        upper.append(this_upper)
        lower.append(this_lower)
        avg.append(this_avg)

    return steps, avg, upper, lower

def calc_error_bar_s(path):
    runs = glob.glob(path)
    upper = []
    avg = []
    lower = []
    lengths = []
    data = []
    steps = []
    for run in runs:
        # read data of one run
        step, wirelength, sp = read_convergence_data(run)
        # collect score, score = wirelength^2 * bbox
        d = []
        for i in range(len(wirelength)):
            d.append(sp[i])
        data.append(d)
        lengths.append(len(d))
        if len(step) > len(steps):
            steps.clear()
            steps = step

    # make data of all runs of the same length
    longest_length = max(lengths)
    for line in data:
        while len(line) < longest_length:
            last_value = line[-1]
            line.append(last_value)

    # calculate the average, upper and lower
    for index in range(longest_length):
        this_run = []
        for run in range(len(runs)):
            this_run.append(data[run][index])
        this_upper = max(this_run)
        this_lower = min(this_run)
        this_avg = sum(this_run) / len(this_run)
        upper.append(this_upper)
        lower.append(this_lower)
        avg.append(this_avg)

    return steps, avg, upper, lower


def plot_convergence(savePath):

    resultDir = os.environ['RAPIDWRIGHT_PATH'] + "/result"

    plt.style.use('seaborn-paper')
    fig, (ax3, ax, ax2) = plt.subplots(1, 3)
    fig.set_size_inches(18, 4.7)
    ax.autoscale(True)

    ax.set_yscale('log')
    ax2.set_yscale('log')
    ax3.set_yscale('log')

    # plot bbox size
    linestyles = [':','-.', '--', '-', '--']
    runs_list = [resultDir + '/SA_convergence_data/*.txt',
                 resultDir + '/NSGA_convergence_data/*.txt',
                 resultDir + '/NSGAR_convergence_data/*.txt',
                 resultDir + '/CMA_convergence_data/*.txt',
                 resultDir + '/GA_convergence_data/*.txt']
    labels = ['SA', 'NSGA-II', 'NSGA-II(Red)', 'CMA-ES', 'GA']


    for idx in range(len(runs_list)):
        steps, avg, upper, lower = calc_error_bar_wr2(runs_list[idx])
        ax3.plot(steps, avg, linestyle=linestyles[idx], label=labels[idx])
        ax3.fill_between(steps, lower, upper, alpha=0.2)

    # ax.legend(prop={'size': 18})
    ax3.set_xlabel("iterations", fontsize=20)
    ax3.set_ylabel("$\mathrm{wirelength^2}$", fontsize=20)

    for idx in range(len(runs_list)):
        steps, avg, upper, lower = calc_error_bar_s(runs_list[idx])
        ax.plot(steps, avg, linestyle=linestyles[idx], label=labels[idx])
        ax.fill_between(steps, lower, upper, alpha=0.2)

    # ax.legend(prop={'size': 18})
    ax.set_xlabel("iterations", fontsize=20)
    ax.set_ylabel("max bbox size", fontsize=20)

    for idx in range(len(runs_list)):
        steps, avg, upper, lower = calc_error_bar_wr2s(runs_list[idx])
        ax2.plot(steps, avg, linestyle=linestyles[idx], label=labels[idx])
        ax2.fill_between(steps, lower, upper, alpha=0.2)

    # ax.legend(prop={'size': 18})
    ax2.set_xlabel("iterations", fontsize=20)
    ax2.set_ylabel("$\mathrm{BboxSize \\times wirelength^2}$", fontsize=20)


    handles, labels = ax.get_legend_handles_labels()
    fig.legend(handles, labels, loc='upper center', prop={'size': 20}, ncol=5, bbox_to_anchor=(0.5, 0.97),
               frameon=False)

    ax.tick_params(axis='both', which='major', labelsize=16)
    ax.tick_params(axis='both', which='minor', labelsize=16)
    ax2.tick_params(axis='both', which='major', labelsize=16)
    ax2.tick_params(axis='both', which='minor', labelsize=16)
    ax3.tick_params(axis='both', which='major', labelsize=16)
    ax3.tick_params(axis='both', which='minor', labelsize=16)


    formatter0 = EngFormatter(unit=' ')
    ax.xaxis.set_major_formatter(formatter0)
    ax2.xaxis.set_major_formatter(formatter0)
    ax3.xaxis.set_major_formatter(formatter0)


    plt.tight_layout()

    plt.subplots_adjust(bottom=0.15)
    plt.subplots_adjust(top=0.83)

    outputImg = savePath + "/convergence.pdf"
    plt.savefig(outputImg)
    print("Image Saved to: " + outputImg)



if __name__ == "__main__":

    root = os.environ['RAPIDWRIGHT_PATH']
    imgPath = root + "/visual"

    if not os.path.isdir(imgPath):
       os.makedirs(imgPath)
       print("[Python] created path: {}".format(imgPath))

    plot_convergence(imgPath)