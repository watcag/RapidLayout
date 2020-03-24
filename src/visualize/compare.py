import argparse
import os

import matplotlib.pyplot as plt
from matplotlib.pyplot import rcParams

rcParams['font.family'] = 'serif'
rcParams['font.serif'] = ['DejaVu Serif']

def read_data(path):

    """

    :param path: file path storing the performance specs
    :return: three lists, runtime, bbox size, wirelengths

    line format: "time size wirelength\n", separated by space

    """
    times = []
    spreads = []
    wirelengths = []
    with open(path) as file:
        for line in file:
            time, spread, wirelength = line.split()
            # print('time = %s, spread = %s, wirelength = %s' % (time, spread, wirelength))
            times.append(float(time))
            spreads.append(float(spread))
            wirelengths.append(float(wirelength))
    return times, spreads, wirelengths


def plot_comparison(dataPath, imagePath):

    """
    input:

    dataPath: text data path of four methods, no ending slash
    imagePath: image saving path, no ending slash

    function: plot figure 8(a)

    runtime - wirelength^2
    runtime - max bbox size
    runtime - wirelength^2 * max bbox size

    NOTICE:

    four txt file must be named: cma.txt, sa.txt, ea.txt, ea-reduced.txt


    """
    plt.style.use('seaborn-paper')

    # wirelength^2
    fig, (ax, ax2, ax3) = plt.subplots(1, 3)
    fig.set_size_inches(15, 4)
    cma_t, cma_s, cma_w = read_data(dataPath + '/CMA_perf.txt')
    sa_t, sa_s, sa_w = read_data(dataPath + '/SA_perf.txt')
    ea_t, ea_s, ea_w = read_data(dataPath + '/EA_perf.txt')
    ear_t, ear_s, ear_w = read_data(dataPath + '/EA-reduced_perf.txt')
    vpr_t, vpr_s, vpr_w = read_data(dataPath + '/VPR_perf.txt')

    for i, w in enumerate(cma_w):
        cma_w[i] = w * w
    for i, w in enumerate(sa_w):
        sa_w[i] = w * w
    for i, w in enumerate(ea_w):
        ea_w[i] = w * w
    for i, w in enumerate(ear_w):
        ear_w[i] = w * w
    for i, w in enumerate(vpr_w):
        vpr_w[i] = w * w

    ax.scatter(sa_w, sa_t, s=70, marker='o', label="Annealing", alpha=0.6)
    ax.scatter(ea_w, ea_t, s=70, marker='^', label="NSGA-II", alpha=0.6)
    ax.scatter(ear_w, ear_t, s=70, marker='^', label="NSGA-II(Red)", alpha=0.6)
    ax.scatter(cma_w, cma_t, s=100, marker='*', label="CMA-ES", alpha=0.6)
    ax.scatter(vpr_w, vpr_t, s=100, marker='*', label="VPR", alpha=0.6)
    ax.set_xlabel("$\mathrm{wirelength^2}$", fontsize=18)
    ax.set_ylabel("runtime (sec)", fontsize=18)

    # print average bbox width
    print('cma bbox width = %f' % (min(cma_s)))
    print('annealing bbox width = %f' % (min(sa_s)))
    print('NSGA-II bbox width = %f' % (min(ea_s)))
    print('NSGA-II reduced bbox width = %f' % (min(ear_s)))

    # bbox width
    # ax2.autoscale(True)
    # ax2.set_aspect(5 / 4)
    ax2.scatter(sa_s, sa_t, s=100, marker='o', label="Annealing", alpha=0.6)
    ax2.scatter(ea_s, ea_t, s=100, marker='^', label="NSGA-II", alpha=0.6)
    ax2.scatter(ear_s, ear_t, s=100, marker='^', label="NSGA-II(Red)", alpha=0.6)
    ax2.scatter(cma_s, cma_t, s=130, marker='*', label="CMA-ES", alpha=0.6)
    ax2.scatter(vpr_s, vpr_t, s=130, marker='*', label="VPR", alpha=0.6)
    ax2.set_xlabel("max bounding box size", fontsize=18)
    ax2.set_yticks([])

    # wl^2 * bbox
    for i, w in enumerate(cma_w):
        cma_w[i] = w * cma_s[i]
    for i, w in enumerate(sa_w):
        sa_w[i] = w * sa_s[i]
    for i, w in enumerate(ea_w):
        ea_w[i] = w * ea_s[i]
    for i, w in enumerate(ear_w):
        ear_w[i] = w * ear_s[i]
    for i, w in enumerate(vpr_w):
        vpr_w[i] = w * vpr_s[i]

    ax3.scatter(sa_w, sa_t, s=70, marker='o', label="Annealing", alpha=0.6)
    ax3.scatter(ea_w, ea_t, s=70, marker='^', label="NSGA-II", alpha=0.6)
    ax3.scatter(ear_w, ear_t, s=70, marker='^', label="NSGA-II(Red)", alpha=0.6)
    ax3.scatter(cma_w, cma_t, s=100, marker='*', label="CMA-ES", alpha=0.6)
    ax3.scatter(vpr_w, vpr_t, s=100, marker='*', label="VPR", alpha=0.6)
    ax3.set_xlabel("$\mathrm{wirelength^2 \\times BboxSize}$", fontsize=18)
    ax3.set_yticks([])

    handles, labels = ax2.get_legend_handles_labels()
    fig.legend(handles, labels, loc='upper center', prop={'size': 18}, ncol=5, bbox_to_anchor=(0.5, 0.97),
               frameon=False)

    ax.tick_params(axis='both', which='major', labelsize=16)
    ax2.tick_params(axis='x', which='major', labelsize=16)
    ax3.tick_params(axis='x', which='major', labelsize=16)
    plt.tight_layout()
    plt.subplots_adjust(bottom=0.17, top=0.85)

    savePath = imagePath + "/objective-runtime.pdf"
    plt.savefig(savePath)
    print("saved convergence plot to: {}".format(savePath))

if __name__ == "__main__":

    parser = argparse.ArgumentParser(
        description='Please input convergence data directory and the image saving directory')
    parser.add_argument('dataPath', type=str, help='the convergence data directory, no ending slash, e.g. ~/home/xxx/data')
    parser.add_argument('imgPath', type=str, help='the image saving directory, no ending slash, e.g. ~/home/img')
    args = parser.parse_args()

    dataPath = args.dataPath
    imgPath = args.imgPath

    print(dataPath)
    print(imgPath)

    if not os.path.isdir(dataPath):
        print("[Python] Invalid path: {}".format(dataPath))

    if not os.path.isdir(imgPath):
       os.makedirs(imgPath)
       print("[Python] created path: {}".format(imgPath))

    plot_comparison(dataPath, imgPath)