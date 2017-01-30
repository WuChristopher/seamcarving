import edu.princeton.cs.algs4.Picture;

import java.awt.*;

/**
 * Created by Christopher on 21/01/2017.
 */
/**
 * SeamCarver is a content-aware picture tailoring tool.
 */
public class SeamCarver {

    private final static double borderEnergy = 1000.00;
    private int width;
    private int height;
    private int arrayWidth;
    private Picture picture;
    private int[] pixels; // pixel's position encoded with col and row in input picture
    private double[] energies;
    private boolean transposed;

    public SeamCarver(Picture picture) {
        if (picture == null) throw new NullPointerException();
        width = picture.width();
        height = picture.height();
        arrayWidth = width;
        this.picture = new Picture(picture);
        pixels = new int[height * width];
        energies = new double[width * height];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                pixels[row * width + col] = row * width + col;
            }
        }

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                energies[row * width + col] = calEnergy(col, row);
            }
        }
        transposed = false;
    }


    public Picture picture() {
        if (transposed) transpose();
        Picture picture = new Picture(width, height);
        int pixelCol;
        int pixelRow;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                pixelCol = pixels[row * arrayWidth + col] % this.picture.width();
                pixelRow = pixels[row * arrayWidth + col] / this.picture.width();
                picture.set(col, row, this.picture.get(pixelCol, pixelRow));
            }
        }
        return picture;
    }

    public int width() {
        if (!transposed) return width;
        else return height;
    }

    public int height() {
        if (!transposed) return height;
        else return width;
    }

    public double energy(int col, int row) {
        if (!transposed) {
            if (col < 0 || col >= width) throw new IndexOutOfBoundsException("col: " + col);
            if (row < 0 || row >= height) throw new IndexOutOfBoundsException("row: " + row);

            return energies[row * arrayWidth + col];
        } else {
            if (col < 0 || col >= height) throw new IndexOutOfBoundsException("col: " + col);
            if (row < 0 || row >= width) throw new IndexOutOfBoundsException("row: " + row);

            return energies[col * arrayWidth + row];
        }
    }

    private double calEnergy(int col, int row) {
        if (col < 0 || col >= width) throw new IndexOutOfBoundsException("col: " + col);
        if (row < 0 || row >= height) throw new IndexOutOfBoundsException("row: " + row);
        if (col == 0 || col == width - 1) return borderEnergy;
        if (row == 0 || row == height - 1) return borderEnergy;

        int ltCol, ltRow, rtCol, rtRow, upCol, upRow, loCol, loRow;

        // calculate the positions of related pixels in the input picture
        ltCol = pixels[row * arrayWidth + col - 1] % picture.width();
        ltRow = pixels[row * arrayWidth + col - 1] / picture.width();
        rtCol = pixels[row * arrayWidth + col + 1] % picture.width();
        rtRow = pixels[row * arrayWidth + col + 1] / picture.width();
        upCol = pixels[(row - 1) * arrayWidth + col] % picture.width();
        upRow = pixels[(row - 1) * arrayWidth + col] / picture.width();
        loCol = pixels[(row + 1) * arrayWidth + col] % picture.width();
        loRow = pixels[(row + 1) * arrayWidth + col] / picture.width();
        // acquire color from input picture
        Color leftPixelColor = picture.get(ltCol, ltRow);
        Color rightPixelColor = picture.get(rtCol, rtRow);
        Color upperPixelColor = picture.get(upCol, upRow);
        Color lowerPixelColor = picture.get(loCol, loRow);
        // calculate energy using dual energy function
        double xGradSquared = Math.pow(leftPixelColor.getRed() - rightPixelColor.getRed(), 2) +
                Math.pow(leftPixelColor.getGreen() - rightPixelColor.getGreen(), 2) +
                Math.pow(leftPixelColor.getBlue() - rightPixelColor.getBlue(), 2);
        double yGradSquared = Math.pow(upperPixelColor.getRed() - lowerPixelColor.getRed(), 2) +
                Math.pow(upperPixelColor.getGreen() - lowerPixelColor.getGreen(), 2) +
                Math.pow(upperPixelColor.getBlue() - lowerPixelColor.getBlue(), 2);
        return Math.sqrt(xGradSquared + yGradSquared);
    }

    /**
     * All the following methods only deal with not transposed picture,
     * so if the picture has been transposed, transpose it back
     */

    /**
     * Find the vertical seam with lowest energy
     */
    public int[] findVerticalSeam() {
        if (transposed) transpose();
        return findSeam();
    }

    /**
     * Find the horizontal seam with lowest energy, but deal with the transposed picture
     */
    public int[] findHorizontalSeam() {
        if (!transposed) transpose();
        return findSeam();
    }

    /**
     * Remove the vertical seam with lowest energy
     */
    public void removeVerticalSeam(int[] seam) {
        if (seam == null) throw new NullPointerException();
        if (transposed) transpose();
        removeSeam(seam);
    }

    /**
     * Remove the horizontal seam with lowest energy, but deal with the transposed picture
     */
    public void removeHorizontalSeam(int[] seam) {
        if (seam == null) throw new NullPointerException();
        if (!transposed) transpose();
        removeSeam(seam);
    }

    /**
     * Use DP or Shortest Paths to find the lowest accumulated energy pixel in the bottom row,
     * record the pixel directed to the current pixel in 'from' array just like 'edgeTo' in Graphs,
     * then rebuild the path from top row to bottom row with 'from' array.
     */
    private int[] findSeam() {
        // use two rows (but in one array) to record the previous and current rows of accumulation.
        double[] cumulatedE = new double[width * 2];
        int[] from = new int[width * height];
        int curRow, preRow;
        double[] cmp = new double[3];
        int idx;
        int[] seam = new int[height];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (row == 0) {
                    cumulatedE[col] = energies[col];
                    from[col] = 0;
                } else if (col == 0 && col != width - 1) {
                    curRow = row % 2;
                    preRow = (row - 1) % 2;
                    cmp[0] = cumulatedE[preRow * width + col];
                    cmp[1] = cumulatedE[preRow * width + col + 1];
                    idx = min(cmp, 2);
                    cumulatedE[curRow * width + col] = energies[row * arrayWidth + col] + cmp[idx];
                    from[row * width + col] = (row - 1) * width + col + idx;
                } else if (col == width - 1 && col != 0) {
                    curRow = row % 2;
                    preRow = (row - 1) % 2;
                    cmp[0] = cumulatedE[preRow * width + col - 1];
                    cmp[1] = cumulatedE[preRow * width + col];
                    idx = min(cmp, 2);
                    cumulatedE[curRow * width + col] = energies[row * arrayWidth + col] + cmp[idx];
                    from[row * width + col] = (row - 1) * width + col - 1 + idx;
                } else if (width == 1) {
                    curRow = row % 2;
                    preRow = (row - 1) % 2;
                    cumulatedE[curRow * width + col] = energies[row * arrayWidth + col] +
                            cumulatedE[preRow * width + col];
                    from[row * width + col] = (row - 1) * width + col;
                } else {
                    curRow = row % 2;
                    preRow = (row - 1) % 2;
                    cmp[0] = cumulatedE[preRow * width + col - 1];
                    cmp[1] = cumulatedE[preRow * width + col];
                    cmp[2] = cumulatedE[preRow * width + col + 1];
                    idx = min(cmp, 3);
                    cumulatedE[curRow * width + col] = energies[row * arrayWidth + col] + cmp[idx];
                    from[row * width + col] = (row - 1) * width + col - 1 + idx;
                }
            }
        }
        curRow = (height - 1) % 2;
        idx = min(cumulatedE, curRow * width, width);
        for (int i = height - 1; i >= 0 ; i--) {
            seam[i] = idx;
            if (i > 0) idx = from[i * width + idx] % width;
        }
        return seam;
    }


    /**
     * This method always removes the vertical seam, from top to bottom.
     */
    private void removeSeam(int[] seam) {
        // input sanity check
        if (width <= 1) throw new IllegalArgumentException("width: " + width);
        if (seam.length != height) throw new IllegalArgumentException("seam length wrong");
        for (int i = 0; i < seam.length - 1; i++) {
            if (seam[i] < 0 || seam[i] >= width)
                throw new IllegalArgumentException("seam[" + i + "]: " + seam[i] + ", width: " + width);
            if (Math.abs(seam[i] - seam[i + 1]) > 1)
                throw new IllegalArgumentException("diff between " + i + ", " + (i+1) + " > 1");
        }
        if (seam[seam.length - 1] < 0 || seam[seam.length - 1] >= width)
            throw new IllegalArgumentException("last input of seam out of range");
        // removing a vertical seam means width decreases
        width--;
        for (int row = 0; row < height; row++) {
            System.arraycopy(pixels, row * arrayWidth + seam[row] + 1, pixels,
                    row * arrayWidth + seam[row], width - seam[row]);
        }
        // update the energies array
        for (int row = 0; row < height; row++) {
            System.arraycopy(energies, row * arrayWidth + seam[row] + 1, energies,
                    row * arrayWidth + seam[row], width - seam[row]);
            if (seam[row] > 0)
                energies[row * arrayWidth + seam[row] - 1] = calEnergy(seam[row] - 1, row);
            if (seam[row] < width)
                energies[row * arrayWidth + seam[row]] = calEnergy(seam[row], row);
        }
    }


    private int min(double[] d, int len) {
        int idx = 0;
        double cmp = Double.POSITIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            if (d[i] < cmp) {
                cmp = d[i];
                idx = i;
            }
        }
        return idx;
    }

    private int min(double[] d, int offset, int len) {
        int idx = 0;
        double cmp = Double.POSITIVE_INFINITY;
        for (int i = 0; i < len; i++) {
            if (d[offset + i] < cmp) {
                cmp = d[offset + i];
                idx = i;
            }
        }
        return idx;
    }

    /**
     * Transpose the picture, now row is previous column, remember we don't actually mutate
     * the this.picture, just transpose energies and pixels.
     */
    private void transpose() {
        int[] pixelsT = new int[height * width];
        double[] energiesT = new double[height * width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                energiesT[col * height + row] = energies[row * arrayWidth + col];
                pixelsT[col * height + row] = pixels[row * arrayWidth + col];
            }
        }
        energies = energiesT;
        pixels = pixelsT;
        int tmp = width;
        width = height;
        height = tmp;
        arrayWidth = width;
        transposed = !transposed;
    }

    public static void main(String[] args) {
    }
}
