import argparse
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import Patch
import numpy as np
import os
from matplotlib.pyplot import rcParams
rcParams['font.family'] = 'serif'
rcParams['font.serif'] = ['DejaVu Serif']

# dsp = 0, bram = 1, uram = 2
vu11p = [0, 1, 1, 0, 0, 0, 1, 0, 2,
         0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 2,
         0, 1, 0, 1, 0, 2,
         0, 0, 1, 0, 1, 0, 2,
         0, 0, 0, 0, 1, 0, 2,
         0, 0, 0, 0, 0, 1, 1, 0]

vu37p = [0, 1, 1, 0, 0, 0, 1, 0, 2,
         0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 2,
         0, 1, 0, 1, 0, 2,
         0, 0, 1, 0, 1, 0, 2,
         0, 0, 0, 0, 1, 0, 2,
         0, 0, 0, 0, 0, 1, 1, 0]

# Set up sizes
gap = 40

# define colors
dsp_col = '#F9E9A1'
bram_col = '#94f0f1'
uram_col = '#f2b1d8'
dsp_block = '#ebc600'
bram_block = '#5AB9EA'
uram_block  = '#bf4aa8'


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
    for type in vu11p:
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
    for type in vu11p:
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
    for type in vu11p:
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



def drawTiles(ax, sites, color, alpha=0.1):
    vertices = calcPolygon(sites)
    polygon = patches.Polygon(vertices, True, facecolor=color, edgecolor=color, alpha=alpha)
    ax.add_patch(polygon)

def drawTileHighlight(ax, sites):
    vertices = calcPolygon(sites)
    polygon = patches.Polygon(vertices, True, facecolor=(0,0,0,0.1), edgecolor='k', linewidth=2)
    ax.add_patch(polygon)


def drawBackGround(ax, width, height, gap):
    dsp_color = dsp_col
    bram_color = bram_col
    uram_color = uram_col
    x = 10  # starting point
    y = 10  # bottom-left y
    for type in vu11p:
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


def draw_png(filename, saveDir):
    dict = readXDC(filename)
    name = filename.split('/')[-1].split('.', 1)[0]
    if not os.path.exists(saveDir):
        os.makedirs(saveDir)

    keys = list(dict.keys())  # keys are integer, which is block index

    width = 560 + gap * (len(vu11p) - 1)
    height = 5414.4 if len(keys) > 80 else 1000
    fontsize = 80 if len(keys) > 80 else 40
    rcParams.update({'font.size': fontsize})

    # draw transparent polygons on a new image
    fig, ax = plt.subplots(figsize=(256,100))
    fig.set_size_inches(width / 100, height / 100)
    ax.set_xlim(0, width + 20)
    ax.set_ylim(0, height + 20)
    rect = patches.Rectangle((10, 10), width, height, linewidth=1, facecolor='#f6f6f6')
    ax.add_patch(rect)
    drawBackGround(ax, width, height, gap)
    for j in range(len(keys)):
        key = keys[j]
        entries = dict.get(key)
        sites = np.zeros(shape=(len(entries) * 4, 2))
        for pair in entries:
            site = pair[0]
            x = 0
            y = 0
            if site.startswith('DSP'):
                x, y = drawDSP(ax, site, gap, dsp_block)
            elif site.startswith('RAMB'):
                x, y = drawBRAM(ax, site, gap, bram_block)
            elif site.startswith('URAM'):
                x, y = drawURAM(ax, site, gap, uram_block)

            for index in range(4):
                sites[entries.index(pair) * 4 + index, 0] = x[index]
                sites[entries.index(pair) * 4 + index, 1] = y[index]
        drawTiles(ax, sites, '#000000')
    # add axis
    legend_elements = [
        Patch(facecolor=dsp_block, label='DSP48'),
        Patch(facecolor=bram_block, label='BRAM'),
        Patch(facecolor=uram_block, label='URAM')
    ]

    # ax.legend(handles=legend_elements, prop={'size':13})

    # plt.savefig(saveDir + name + '.png', dpi=50)
    # plt.close()
    plt.savefig(saveDir + name + '.pdf')
    plt.close()
    print(saveDir + name + '.pdf')
    return saveDir + name + '.pdf'


def draw_frame(ax, filename, iteration, method):

    dict = readXDC(filename)

    keys = list(dict.keys())  # keys are integer, which is block index

    width = 560 + gap * (len(vu11p) - 1)
    height = 5414.4 if len(keys) > 80 else 1000

    # draw transparent polygons on a new image
    ax.set_xlim(0, width + 20)
    ax.set_ylim(0, height + 20)
    rect = patches.Rectangle((10, 10), width, height, linewidth=1, facecolor='#f6f6f6')
    ax.add_patch(rect)
    drawBackGround(ax, width, height, gap)
    for j in range(len(keys)):
        key = keys[j]
        entries = dict.get(key)
        sites = np.zeros(shape=(len(entries) * 4, 2))
        for pair in entries:
            site = pair[0]
            x = 0
            y = 0
            if site.startswith('DSP'):
                x, y = drawDSP(ax, site, gap, dsp_block)
            elif site.startswith('RAMB'):
                x, y = drawBRAM(ax, site, gap, bram_block)
            elif site.startswith('URAM'):
                x, y = drawURAM(ax, site, gap, uram_block)

            for index in range(4):
                sites[entries.index(pair) * 4 + index, 0] = x[index]
                sites[entries.index(pair) * 4 + index, 1] = y[index]
        drawTiles(ax, sites, '#000000')

    ax.set_xlabel("iteration = {}".format(iteration), fontsize=20)
    ax.set_title(method, fontsize=30)

    ax.tick_params(
        axis='both',  # changes apply to the x-axis
        which='both',  # both major and minor ticks are affected
        bottom=False,  # ticks along the bottom edge are off
        top=False,  # ticks along the top edge are off
        left=False,
        right=False,
        labelleft=False,
        labelbottom=False  # labels along the bottom edge are off)
    )

    return ax


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Please input the result xdc file, visualization image output directory')
    parser.add_argument('filename', type=str, help='the result xdc file to visualize')
    parser.add_argument('dir', type=str, help='the output directory of visualization file')
    args = parser.parse_args()
    filename = args.filename
    saveDir = args.dir

    if not os.path.isdir(saveDir):
        os.makedirs(saveDir)

    draw_png(filename, saveDir)


