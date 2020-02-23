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


def draw_one_frame(saveDir, cma_xdc, ea_xdc, ear_xdc, sa_xdc,
                   cma_iter, ea_iter, ear_iter, sa_iter):
    fig, axs = plt.subplots(2, 2)
    fig.set_size_inches(10,5)

    ax1 = axs[0,0]
    ax2 = axs[0,1]
    ax3 = axs[1,0]
    ax4 = axs[1,1]

    # CMA, EA, EA-reduced, SA
    draw_frame(ax1, cma_xdc, cma_iter, "CMA-ES")
    draw_frame(ax2, ea_xdc, ea_iter, "NSGA-II")
    draw_frame(ax3, ear_xdc, ear_iter, "NSGA-II-reduced")
    draw_frame(ax4, sa_xdc, sa_iter, "Annealing")

    plt.tight_layout()
    plt.savefig(saveDir)
    print("saved temp frame: " + saveDir)


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
    step = int(len(sa_xdc) / frame)

    if len(cma_xdc) < frame * step:
        orig_len = len(cma_xdc)
        for i in range(orig_len, frame * step):
            cma_xdc.append(cma_xdc[orig_len-1])
    if len(ea_xdc) < frame * step:
        orig_len = len(ea_xdc)
        for i in range(orig_len, frame * step):
            ea_xdc.append(ea_xdc[orig_len-1])
    if len(ear_xdc) < frame * step:
        orig_len = len(ear_xdc)
        for i in range(orig_len, frame * step):
            ear_xdc.append(ear_xdc[orig_len-1])


    frames = []

    for i in range(frame):
        imgFile = saveDir + str(i) + ".png"
        draw_one_frame(imgFile, cma_xdc[step * i],
                       ea_xdc[step * i], ear_xdc[step * i],
                       sa_xdc[step * i],
                       i*step, i*step, i*step, i*step)
        frames.append(imageio.imread(imgFile))

    gifFile = root + "/visual/fused.gif"
    imageio.mimsave(gifFile, frames)
    optimize(gifFile)