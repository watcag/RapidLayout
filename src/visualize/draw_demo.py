import glob, os
import imageio
import matplotlib.pyplot as plt
from pygifsicle import optimize
from overall_visual import draw_frame


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


def draw_one_frame(saveDir, xdc, iter):
    fig, ax = plt.subplots()
    fig.set_size_inches(10,5)

    # CMA, EA, EA-reduced, SA
    draw_frame(ax, xdc, iter, "Demo Placement")

    plt.tight_layout()
    plt.savefig(saveDir)
    print("saved temp frame: " + saveDir)


if __name__ == "__main__":

    root = os.environ['RAPIDWRIGHT_PATH']
    saveDir = root + '/result/tmpFrames/'
    if not os.path.isdir(saveDir):
       os.makedirs(saveDir)
       print("[Python] created path: {}".format(saveDir))

    # glob all xdc files
    xdc = glob.glob(root + "/result/demo_gif_data/*.xdc")

    # sort them
    xdc = sorted(xdc, key=sorter)

    # how many frame do we want?
    frame = 10
    step = int(len(xdc) / frame)

    frames = []

    for i in range(frame):
        imgFile = saveDir + str(i) + ".png"
        draw_one_frame(imgFile, xdc[step * i], i*step)
        frames.append(imageio.imread(imgFile))

    gifFile = root + "/visual/demo.gif"
    imageio.mimsave(gifFile, frames)
    optimize(gifFile)
    print("[RapidLayout] GIF has been saved to : " + gifFile)
    os.rmdir(saveDir)