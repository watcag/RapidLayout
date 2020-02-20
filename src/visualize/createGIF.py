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


def draw_one_frame(saveDir, cma_xdc, ea_xdc, ear_xdc, sa_xdc):
    fig, (ax1, ax2, ax3, ax4) = plt.subplots(2, 2)
    # CMA, EA, EA-reduced, SA
    draw_frame(ax1, cma_xdc)
    draw_frame(ax2, ea_xdc)
    draw_frame(ax3, ear_xdc)
    draw_frame(ax4, sa_xdc)
    plt.savefig(saveDir)


if __name__ == "__main__":

    """
        Goal: juxtapose four methods' evolutionary in one gif, all with the same amout of frames
        maybe make if 2*2
    """
    root = os.environ['RAPIDWRIGHT_PATH']
    saveDir = root + '/result/tmpFrames/'
    if not os.path.isdir(saveDir):
       os.makedirs(saveDir)
       print("[Python] created path: {}".format(saveDir))

    # glob all xdc files
    cma_xdc = glob.glob(root + "/result/CMA_gif_data/*.xdc")
    ea_xdc = glob.glob(root + "/result/EA_gif_data/*.xdc")
    ear_xdc = glob.glob(root + "/result/EA-reduced_gif_data/*.xdc")
    sa_xdc = glob.glob(root + "/result/SA_gif_data/*.xdc")

    # sort them
    cma_xdc = sorted(cma_xdc, key=sorter)
    ea_xdc = sorted(ea_xdc, key=sorter)
    ear_xdc = sorted(ear_xdc, key=sorter)
    sa_xdc = sorted(sa_xdc, key=sorter)

    # how many frame do we want?
    frame = 10
    cma_step = int(len(cma_xdc) / frame)
    ea_step = int(len(ea_xdc) / frame)
    ear_step = int(len(ear_xdc) / frame)
    sa_step = int(len(sa_xdc) / frame)

    frames = []

    for i in range(frame):
        imgFile = saveDir + str(i) + ".pdf"
        draw_one_frame(imgFile, cma_xdc[cma_step * i],
                       ea_xdc[ea_step * i], ear_xdc[ea_step * i],
                       sa_xdc[sa_step * i])
        frames.append(imageio.imread(imgFile))

    gifFile = root + "/visual/fused.gif"
    imageio.mimsave(gifFile, frames)
    optimize(gifFile)