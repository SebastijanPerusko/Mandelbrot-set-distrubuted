import javafx.animation.AnimationTimer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import mpi.MPI;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;


public class Simulation extends Canvas {

    GraphicsContext gc;
    PixelWriter pwm;
    private int max;
    private double xMove;
    private double yMove;
    private double zoom;
    private int width;
    private int height;
    private Color[] colors;
    int[] ArrPixel;
    private String[] args_a;

    public Simulation(int width, int height, int max, double xMove, double yMove, double zoom){
        super(width,height);
        this.max = max;
        this.xMove = xMove;
        this.yMove = yMove;
        this.width = width;
        this.zoom = zoom;
        this.width = width;
        this.height = height;
        gc = this.getGraphicsContext2D();
        ColorsArray(max);

    }

    public void setMax(int max) {
        ColorsArray(max);
        this.max = max;
    }

    public void setxMove(double xMove) {
        this.xMove = xMove;
    }

    public void setyMove(double yMove) {
        this.yMove = yMove;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public void setWidthG(int width) {
        this.width = width;
        ArrPixel = new int[this.width * height];
    }

    public void setHeightG(int height) {
        this.height = height;
        ArrPixel = new int[width * this.height];
    }

    public int getMax() {
        return max;
    }

    public double getxMove() {
        return xMove;
    }

    public double getyMove() {
        return yMove;
    }

    public double getZoom() {
        return zoom;
    }

    public void start() {
        PixelWriter pw = gc.getPixelWriter();
        final WritablePixelFormat<IntBuffer> pixelFormat =
                PixelFormat.getIntArgbPreInstance();

        new AnimationTimer(){
            public void handle(long currentNanoTime){
                int chunk = height / (MPI.COMM_WORLD.Size()-1);
                int width_chunk = width;
                double zoom_d = zoom;
                double move_x_d = xMove;
                double move_y_d = yMove;
                double height_d = height;
                int[] buffer = new int[width * height];
                double[] a = {chunk, width_chunk, height_d, zoom_d, move_x_d, move_y_d};
                for (int i = 1; i < (MPI.COMM_WORLD.Size()); i++) {
                    MPI.COMM_WORLD.Send(a, 0, 6, MPI.DOUBLE, i, 1);
                }
                int[] c = new int[chunk*width_chunk];
                int[] last = new int[(chunk + (int)(height_d % chunk))*width_chunk];
                for (int i = 1; i < MPI.COMM_WORLD.Size(); i++) {
                    if(i < MPI.COMM_WORLD.Size() - 1){
                        MPI.COMM_WORLD.Recv(c, 0, chunk*width_chunk, MPI.INT, i, 1);
                        for (int j = 0; j < c.length; j++) {
                            buffer[j + (chunk*width_chunk*(i-1))] = c[j];
                        }
                    } else {
                        MPI.COMM_WORLD.Recv(last, 0, (int)(chunk + height_d % chunk)*width_chunk, MPI.INT, i, 1);
                        for (int j = 0; j < last.length; j++) {
                            buffer[j + (chunk*width_chunk*(i-1))] = last[j];
                        }
                    }

                }
                pw.setPixels(0, 0, width, height, pixelFormat, buffer, 0, width);
            }
        }.start();
    }


    public void start_testing() {
        PixelWriter pw = gc.getPixelWriter();
        final WritablePixelFormat<IntBuffer> pixelFormat =
                PixelFormat.getIntArgbPreInstance();

        int chunk = height / (MPI.
                COMM_WORLD.Size()-1);
        int width_chunk = width;
        double zoom_d = zoom;
        double move_x_d = xMove;
        double move_y_d = yMove;
        double height_d = height;
        int[] buffer = new int[1000*1000];
        double[] a = {chunk, width_chunk, height_d, zoom_d, move_x_d, move_y_d};
        for (int i = 1; i < (MPI.COMM_WORLD.Size()); i++) {
            MPI.COMM_WORLD.Send(a, 0, 6, MPI.DOUBLE, i, 1);
        }
        int[] c = new int[1000];
        for (int i = 1; i < MPI.COMM_WORLD.Size(); i++) {
            MPI.COMM_WORLD.Recv(c, 0, 1000, MPI.INT, i, 1);
            for (int j = 0; j < c.length; j++) {
                buffer[j] = c[j];
            }
        }
        pw.setPixels(0, 0, 1000, 1000, pixelFormat, buffer, 0, 1000);
    }

    public void work_test() {
        while(true) {
            double[] b = new double[6];
            int Iam = MPI.COMM_WORLD.Rank();
            int count = 0;

            MPI.COMM_WORLD.Recv(b, 0, 6, MPI.DOUBLE, 0, 1);
            int[] c = new int[1000];
            /*System.out.println("MPI n. " + (MPI.COMM_WORLD.Rank())
                + " calc " + b[1]*(MPI.COMM_WORLD.Rank() - 1) + " to " + b[1]*(MPI.COMM_WORLD.Rank()));*/
            for (int row = (int)(b[0]*(Iam-1)); row < (int)(b[0]*(Iam-1) + b[0]); row++) {
                for (int col = 0; col < b[1]; col++) {
                    c[Math.abs(count % 1000)] = mandelbrot(row, col, b[1], b[2], b[3], b[4], b[5]);
                    count++;
                }
            }
            MPI.COMM_WORLD.Send(c, 0, 1000, MPI.INT, 0, 1);
        }
    }

    public void work() {
        while(true) {
            double[] b = new double[6];
            int Iam = MPI.COMM_WORLD.Rank();
            int count = 0;

            MPI.COMM_WORLD.Recv(b, 0, 6, MPI.DOUBLE, 0, 1);
            int[] c = new int[(int)(b[1]*b[2])];
            /*System.out.println("MPI n. " + (MPI.COMM_WORLD.Rank())
                + " calc " + b[1]*(MPI.COMM_WORLD.Rank() - 1) + " to " + b[1]*(MPI.COMM_WORLD.Rank()));*/
            int end;
            if(Iam == MPI.COMM_WORLD.Size() - 1){
                end = (int)b[2];
            } else {
                end = (int) (b[0] * (Iam - 1) + b[0]);
            }
            for (int row = (int)(b[0]*(Iam-1)); row < end; row++) {
                for (int col = 0; col < b[1]; col++) {
                    c[count] = mandelbrot(row, col, b[1], b[2], b[3], b[4], b[5]);
                    count++;
                }
            }
            if(Iam == MPI.COMM_WORLD.Size() - 1){
                MPI.COMM_WORLD.Send(c, 0, ((int)((b[0] + b[2] % b[0])*b[1])), MPI.INT, 0, 1);
            } else {
                MPI.COMM_WORLD.Send(c, 0, (int)(b[0]*b[1]), MPI.INT, 0, 1);
            }

        }
    }

    private void ColorsArray(int max){
        List<Color> c = new ArrayList<Color>();
        double c1 = 32/(0.16*max);
        double c2 = 100/(0.16*max);
        double c3 = 103/(0.16*max);
        for(int i = 0; i < (int)(0.16*max);i++){
            c.add(Color.rgb((int)(c1*i), 7+(int)(c2*i), 100+(int)(c3*i)));
        }
        c1 = 205/(0.42*max);
        c2 = 148/(0.42*max);
        c3 = 52/(0.42*max);
        for(int i = (int)(0.16*max); i < (int)(0.42*max);i++){
            c.add(Color.rgb(32+(int)(c1*i), 107+(int)(c2*i), 203+(int)(c3*i)));
        }
        c1 = 18/(0.6425*max);
        c2 = -85/(0.6425*max);
        c3 = -255/(0.6425*max);
        for(int i = (int)(0.42*max); i < (int)(0.6425*max);i++){
            c.add(Color.rgb(237+(int)(c1*i), 255+(int)(c2*i), 255+(int)(c3*i)));
        }
        c1 = -255/(0.8575*max);
        c2 = -168/(0.8575*max);
        c3 = 0/(0.8575*max);
        for(int i = (int)(0.6425*max); i < (int)(0.8575*max);i++){
            c.add(Color.rgb(255+(int)(c1*i), 170+(int)(c2*i), (int)(c3*i)));
        }
        for(int i = (int)(0.8575*max); i < max;i++){
            c.add(Color.rgb(0, 2, 0));
        }

        colors = c.toArray(new Color[c.size()]);
    }

    public void save_img(Stage primaryStage){
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("png files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(primaryStage);

        if(file != null){
            try {
                WritableImage writableImage = new WritableImage(width, height);
                this.snapshot(null, writableImage);
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(writableImage, null);
                ImageIO.write(renderedImage, "png", file);
            } catch (IOException ex) {

            }
        }
    }

    private int toInt(Color c) {
        return
                (255  << 24) |
                        ((int) (c.getRed()   * 255) << 16) |
                        ((int) (c.getGreen() * 255) << 8)  |
                        ((int) (c.getBlue()  * 255));
    }

    public int mandelbrot(int row, int col, double width_d, double height_d, double zoom_d, double xMove_d, double yMove_d){
        double c_re = 0, c_im = 0;
        c_re = xMove_d + (col - width_d/2.0)*4.5/(width_d*zoom_d);
        c_im = yMove_d + (row - height_d/2.0)*4.5/(width_d*zoom_d);
        double x = 0, y = 0, x2 = 0, y2 = 0;
        int iteration = 0;
        while (x2 + y2 <= 4 && iteration < max) {
            y = 2 * x * y + c_im;
            x = x2 - y2 + c_re;
            x2 = x * x;
            y2 = y * y;
            iteration++;
        }
        if (iteration < max) return(toInt(colors[iteration]));
        else return(toInt(Color.rgb(35, 35, 35)));
    }

}
