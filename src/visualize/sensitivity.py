import os

import matplotlib.pyplot as plt
import numpy as np
from matplotlib import cm
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.pyplot import rcParams

rcParams['font.family'] = 'serif'
rcParams['font.serif'] = ['DejaVu Serif']

def convert_data(orig, saving):
    population = []
    sigma = []
    tmp_wl = []
    with open(orig) as f:
        for line in f:
            items = line.split()
            population.append(int(items[2]))
            sigma.append(round(float(items[5]),1))
            tmp_wl.append(float(items[8]))
    rows = range(min(population), max(population), 10)
    cols = np.arange(min(sigma), max(sigma), 0.1)

    wirelength = np.zeros(shape=(len(rows), len(cols)))
    for i in range(len(rows)):
        for j in range(len(cols)):
            wirelength[i,j] = tmp_wl[i * len(cols) + j]
    np.savetxt(saving, wirelength)
    return rows, cols



"""
sensitivity data format:
    __________sigma_______
   |                      |
 population               |
   |       [wirelength]   | 
   |______________________|
   
"""

def cma_sensitivity():

    sensData = os.environ['RAPIDWRIGHT_PATH'] + "/result/cma_sensitivity.txt"
    sensDataTrans = os.environ['RAPIDWRIGHT_PATH'] + "/result/cma_sensitivity_trans.txt"
    saveDir = os.environ['RAPIDWRIGHT_PATH'] + "/visual"

    fig = plt.figure()
    plt.style.use('seaborn-paper')
    ax = fig.gca(projection='3d')

    population, sigma = convert_data(sensData, sensDataTrans)
    result = []
    with open(sensDataTrans) as file:
        for line in file:
            numbers = line.split()
            result.append(numbers)
    wl = np.array(result).astype(np.float)
    X, Y = np.meshgrid(sigma, population)
    ax.plot_surface(X, Y, wl, cmap=cm.viridis, linewidth=0, antialiased=False)

    ax.view_init(30, -70)

    ax.set_xlabel('sigma ', fontsize=15)
    ax.set_ylabel('population', fontsize=15)
    ax.set_zlabel('wirelength', fontsize=15)

    imgPath = saveDir + "/cma_sensitivity.pdf"

    plt.savefig(imgPath)


if __name__ == "__main__":
    cma_sensitivity()

