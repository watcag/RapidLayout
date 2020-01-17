from typing import KeysView

import matplotlib.pyplot as plt
import matplotlib.patches as patches
from scipy.spatial import ConvexHull
import numpy as np
import os
import math
from colour import Color
import argparse
from pathlib import Path

# dsp = 0, bram = 1, uram = 2
types = [0, 1, 1, 0, 0, 0, 1, 0, 2,
         0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 2,
         0, 1, 0, 1, 0, 2,
         0, 0, 1, 0, 1, 0, 2,
         0, 0, 0, 0, 1, 0, 2,
         0, 0, 0, 0, 0, 1, 1, 0]


def readXDC(f):
    # a dict to put result in it
    dict = {}
    with open(f) as fp:
        line = fp.readline()
        while line:
            if not line.startswith('set_property'):
                line = fp.readline()
                continue
            line = line.replace('\t', ' ')
            site = line.split(' ')[2]
            cell = line.split('{')[1].split("}")[0]
            n = int(cell.split('[')[1].split(']')[0], 10)
            if dict.get(n) is None:
                dict[n] = []
                dict[n].append([site, cell])
            else:
                dict[n].append([site, cell])
            line = fp.readline()

        return dict


def drawDSP(ax, site, gap, color, yOffset=10):
    X = int(site.split('X', 1)[1].split('Y', 1)[0], 10)  # base 10
    Y = int(site.split('Y', 1)[1], 10)  # base 10
    count = 0
    x = 10
    for type in types:
        # move x
        if type == 0:  # DSP
            if count == X:
                break  # time to go!
            x += 10 + gap
            count += 1
        elif type == 1:  # BRAM
            x += 10 + gap
        elif type == 2:  # URAM
            x += 20 + gap

    y = Y * 19.2 + 2.1 + yOffset
    r = patches.Rectangle((x, y), 10, 15, facecolor=color)
    ax.add_patch(r)
    x_vertices = [x, x, x+10, x+10]
    y_vertices = [y, y+15, y, y+15]
    return x_vertices, y_vertices


def drawBRAM(ax, site, gap, color, yOffset=10):
    X = int(site.split('X', 1)[1].split('Y', 1)[0], 10)  # base 10
    Y = int(site.split('Y', 1)[1], 10)  # base 10
    count = 0
    x = 10
    for type in types:
        # move x
        if type == 0:  # DSP
            x += 10 + gap
        elif type == 1:  # BRAM
            if count == X:
                break  # time to go!
            x += 10 + gap
            count += 1
        elif type == 2:  # URAM
            x += 20 + gap

    y = Y * 18.8 + 0.9 + yOffset
    r = patches.Rectangle((x, y), 10, 17, facecolor=color)
    ax.add_patch(r)
    x_vertices = [x, x, x + 10, x + 10]
    y_vertices = [y, y + 17, y, y + 17]
    return x_vertices, y_vertices


def drawURAM(ax, site, gap, color, yOffset=10):
    X = int(site.split('X', 1)[1].split('Y', 1)[0], 10)  # base 10
    Y = int(site.split('Y', 1)[1], 10)  # base 10
    count = 0
    x = 10
    for type in types:
        # move x
        if type == 0:  # DSP
            x += 10 + gap
        elif type == 1:  # BRAM
            x += 10 + gap
        elif type == 2:  # URAM
            if count == X:
                break  # time to go!
            x += 20 + gap
            count += 1

    y = Y * 28.2 + 1.6 + yOffset
    r = patches.Rectangle((x, y), 20, 25, facecolor=color)
    ax.add_patch(r)
    x_vertices = [x, x, x + 20, x + 20]
    y_vertices = [y, y + 25, y, y + 25]
    return x_vertices, y_vertices


def calcPolygon(input):
    length = input.shape[0]
    columns = {}
    for i in range(length):
        x = input[i, 0]
        y = input[i, 1]
        if columns.get(x) is None:
            columns[x] = []
            columns[x].append(y)
        else:
            columns[x].append(y)

    up = []
    down = []
    sorted_keys = list(columns.keys())
    list.sort(sorted_keys)
    for key in sorted_keys:
        thisColumn = columns.get(key)
        list.sort(thisColumn)
        up.append([key, thisColumn[-1]])
        down.append([key, thisColumn[0]])

    output = []
    output += down
    up.reverse()
    output += up
    return output






def drawTiles(ax, sites, color):
    # add convex hull
    # hull = ConvexHull(sites)
    # simplices = hull.vertices
    # length = simplices.shape[0]
    # vertices = np.zeros(shape=(length, 2))
    # for i in range(len(hull.vertices)):
    #     simplex = simplices[i]
    #     vertices[i, 0] = sites[simplex,0]
    #     vertices[i, 1] = sites[simplex,1]
    vertices = calcPolygon(sites)
    polygon = patches.Polygon(vertices, True, facecolor=color, edgecolor=color, alpha=0.1)
    ax.add_patch(polygon)


def drawBackGround(ax, width, height, gap):
    dsp_color = '#cdd422'
    bram_color = '#94f0f1'
    uram_color = '#f2b1d8'
    x = 10  # starting point
    y = 10  # bottom-left y
    for type in types:
        r = patches.Rectangle
        if type == 0:  # DSP
            r = patches.Rectangle((x, y), 10, height, facecolor=dsp_color)
            x += 10 + gap
        elif type == 1:  # BRAM
            r = patches.Rectangle((x, y), 10, height, facecolor=bram_color)
            x += 10 + gap
        elif type == 2:  # URAM
            r = patches.Rectangle((x, y), 20, height, facecolor=uram_color)
            x += 20 + gap
        ax.add_patch(r)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Please input the result xdc file')
    parser.add_argument('filename', type=str, help='the result xdc file to visualize')
    args = parser.parse_args()
    filename = args.filename
    saveDir = str(Path.home()) + "/RapidWright/visual/"

    dict = readXDC(filename)
    name = filename.split('/')[-1].split('.', 1)[0]
    print(name)
    saveDir = saveDir + name + "/"
    print(saveDir)
    if not os.path.exists(saveDir):
        os.makedirs(saveDir)

    # Set up sizes
    gap = 40
    width = 560 + gap * (len(types) - 1)
    # height = 5414.4
    height = 1000

    keys = list(dict.keys()) # keys are integer, which is block index
    # highlight color
    red = Color("red")
    for i in range(len(keys)):

        # select which block to draw
        if i > 1:
            continue

        color = Color('red').get_rgb()
        # Create figure and axes
        fig, ax = plt.subplots(1)
        fig.set_size_inches(width / 100, height / 100)
        ax.set_xlim(0, width + 20)
        ax.set_ylim(0, height + 20)

        # Create a Rectangle Patch (board)
        rect = patches.Rectangle((10, 10), width, height, linewidth=1, facecolor='#dddddd')
        ax.add_patch(rect)

        # draw hard block column background
        drawBackGround(ax, width, height, gap)

        # draw un-highlighted blocks
        for j in range(len(keys)):
            key = keys[j]
            entries = dict.get(key)
            for pair in entries:
                site = pair[0]
                cell = pair[1]
                if site.startswith('DSP'):
                    drawDSP(ax, site, gap, '#ffdc6a')
                elif site.startswith('RAMB'):
                    drawBRAM(ax, site, gap, '#00c07f')
                elif site.startswith('URAM'):
                    drawURAM(ax, site, gap, '#bf4aa8')

        # draw the highlighted block
        key = keys[i]
        entries = dict.get(key)
        # this array stores vertices of each hard block rectangle
        sites = np.zeros(shape=(len(entries) * 4, 2))
        x = 0
        y = 0
        for pair in entries:
            site = pair[0]
            cell = pair[1]
            if site.startswith('DSP'):
                x,y = drawDSP(ax, site, gap, color)  # '#ffdc6a'
                # drawDSP(ax, site, '#ffdc6a')
            elif site.startswith('RAMB'):
                x,y = drawBRAM(ax, site, gap, color)  # '#8bf0ba'
                # drawBRAM(ax, site, '#00c07f')
            elif site.startswith('URAM'):
                x,y = drawURAM(ax, site, gap, color)  # ''#bf4aa8''
                # drawURAM(ax, site, '#bf4aa8')
            for index in range(4):
                sites[entries.index(pair) * 4 + index, 0] = x[index]
                sites[entries.index(pair) * 4 + index, 1] = y[index]
        drawTiles(ax, sites, '#000000')

        # plt.show()
        plt.savefig(saveDir + 'visualize-{}.png'.format(i))
        plt.close()
        print(saveDir + 'visualize-{}.png'.format(i))

    # draw transparent polygons on a new image
    fig, ax = plt.subplots(1)
    fig.set_size_inches(width / 100, height / 100)
    ax.set_xlim(0, width + 20)
    ax.set_ylim(0, height + 20)
    rect = patches.Rectangle((10, 10), width, height, linewidth=1, facecolor='#dddddd')
    ax.add_patch(rect)
    drawBackGround(ax, width, height, gap)
    for j in range(len(keys)):
        key = keys[j]
        entries = dict.get(key)
        sites = np.zeros(shape=(len(entries) * 4, 2))
        for pair in entries:
            site = pair[0]
            cell = pair[1]
            x = 0
            y = 0
            if site.startswith('DSP'):
                x, y = drawDSP(ax, site, gap, '#ffdc6a')
            elif site.startswith('RAMB'):
                x, y = drawBRAM(ax, site, gap, '#00c07f')
            elif site.startswith('URAM'):
                x, y = drawURAM(ax, site, gap, '#bf4aa8')

            for index in range(4):
                sites[entries.index(pair) * 4 + index, 0] = x[index]
                sites[entries.index(pair) * 4 + index, 1] = y[index]

        drawTiles(ax, sites, '#000000')

    plt.savefig(saveDir + 'visualize-all.png')
    plt.close()
    print(saveDir + 'visualize-all.png')
