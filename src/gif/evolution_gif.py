import glob, os
import imageio
from pygifsicle import optimize

from visual_placement import draw_png


def sorter(item):
    # sort by name
    name = item.split('/')[-1].split('.')[0]
    number = int(name)
    return number

def create_video():
    all_png = glob.glob('png/*.png')
    all_png = sorted(all_png, key=sorter)

    images = []
    for png in all_png:
        images.append(imageio.imread(png))
    # imageio.


if __name__ == "__main__":

     saveDir = 'png/CMA/'
     images = []
     all_xdc = glob.glob("evolution/CMA/*.xdc")
     all_xdc = sorted(all_xdc, key=sorter)

     for file in all_xdc:
         imgfile = draw_png(file, saveDir)
         images.append(imageio.imread(imgfile))
     imageio.mimsave('CMA.gif', images, loop=1)

     optimize('CMA.gif', loop=1)