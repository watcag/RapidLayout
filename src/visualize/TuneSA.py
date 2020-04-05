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

"""
this function calculates the error bar of one type of cooling schedule
"""
def calc_error_bar(path):
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



def plot_tuning(savePath):

    resultDir = os.environ['RAPIDWRIGHT_PATH'] + "/result"

    plt.style.use('seaborn-paper')
    fig, ax = plt.subplots()
    fig.set_size_inches(5, 3.5)
    ax.autoscale(True)
    ax.set_yscale('log')

    schedules = glob.glob(resultDir + '/TuneSA_convergence_data/*')
    linestyles = iter([':', '--', '-'])
    for schedule in schedules:
        ls = next(linestyles)
        sch_name = schedule.split("/")[-1]
        steps, avg, upper, lower = calc_error_bar(schedule + '/*.txt')
        ax.plot(steps, avg, label=sch_name, linestyle=ls)
        ax.fill_between(steps, lower, upper, alpha=0.2)


    ax.tick_params(axis='both', which='major', labelsize=16)
    ax.tick_params(axis='both', which='minor', labelsize=16)

    handles, labels = ax.get_legend_handles_labels()
    # fig.legend(handles, labels, loc='upper center', prop={'size': 20}, ncol=4, bbox_to_anchor=(0.5, 0.94),
    #            frameon=False)
    fig.legend(handles, labels, loc='upper right', prop={'size': 15}, frameon=False, bbox_to_anchor=(0.98, 0.99), ncol=1)

    formatter0 = EngFormatter(unit=' ')
    ax.xaxis.set_major_formatter(formatter0)

    ax.set_xlabel("steps", fontsize=15)
    ax.set_ylabel("$\mathrm{BboxSize \\times wirelength^2}$", fontsize=15)

    plt.tight_layout()

    # plt.subplots_adjust(bottom=0.15)
    #plt.subplots_adjust(top=0.83)

    outputImg = savePath + "/Annealing-Tuning.pdf"
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

    # plot_convergence(imgPath)
    plot_tuning(imgPath)