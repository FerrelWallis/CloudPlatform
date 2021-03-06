package utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;


public class ImageUtils {



    public static String IMAGE_TYPE_GIF = "gif";// 图形交换格式
    public static String IMAGE_TYPE_JPG = "jpg";// 联合照片专家组
    public static String IMAGE_TYPE_JPEG = "jpeg";// 联合照片专家组
    public static String IMAGE_TYPE_BMP = "bmp";// 英文Bitmap（位图）的简写，它是Windows操作系统中的标准图像文件格式
    public static String IMAGE_TYPE_PNG = "png";// 可移植网络图形
    public static String IMAGE_TYPE_PSD = "psd";// Photoshop的专用格式Photoshop



    public static void main(String[] args) {

//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\venn_副本_big.png", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\venn_smaller.png",  2, false);//测试OK
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\3.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\3.jpg", 2, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\合影原图.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\合影原图.jpg", 7, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子宣传原图.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子宣传原图.jpg", 2, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\微信图片_20190531084914.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\微信图片_20190531084914.jpg", 2, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\微信图片_20190531084946.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\微信图片_20190531084946.jpg", 7, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\ban2.png", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\ban2.png", 3, false);



        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\组合图1_or.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\组合图1_s.jpg", 7, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\组合图2_or.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\组合图2.jpg", 5, false);


//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\banner_egg1.jpeg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\banner_egg1.jpeg", 2, false);

//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (37)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (37)_small.jpg", 6, false);//测试OK
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (40)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (40)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (41)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (41)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (44)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (44)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (47)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (47)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (50)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (50)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (61)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (61)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (67)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (67)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (69)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (69)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (70)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (70)_small.jpg", 6, false);
//        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (1)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (1)_small.jpg", 6,false);


        //ImageUtils.scale("F:\\database\\fern\\images/"+ name+".jpg", "F:\\database\\fern\\images/"+ name+"_medium.jpg",  4, false);//测试OK


        /*       // 1-缩放图像：
        // 方法一：按比例缩放
        for(int i = 1;i<4;i++){
            String name = "5_" + i;
        ImageUtils.scale("C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\茄子艺术照 (1)_副本.jpg", "C:\\Users\\yingf\\Desktop\\学习文档\\图片\\挑选图片\\small\\茄子艺术照 (1)_small.jpg", 10, false);//测试OK
        // 方法二：按高度和宽度缩放
        //ImageUtils.scale("F:\\database\\fern\\images/"+ name+".jpg", "F:\\database\\fern\\images/"+ name+"_medium.jpg",  4, false);//测试OK
        }
       */


     //   ImageUtils.scale("D:\\workspace\\fern_database\\public\\images/fern4.png","D:\\workspace\\fern_database\\public\\images/fern4.jpg",2,false);


        // 2-切割图像：
        // 方法一：按指定起点坐标和宽高切割
   //     ImageUtils.cut("e:/abc.jpg", "e:/abc_cut.jpg", 0, 0, 400, 400 );//测试OK
        // 方法二：指定切片的行数和列数
     //   ImageUtils.cut2("e:/abc.jpg", "e:/", 2, 2 );//测试OK
        // 方法三：指定切片的宽度和高度
    //    ImageUtils.cut3("e:/abc.jpg", "e:/", 300, 300 );//测试OK


        // 3-图像类型转换：
     //   ImageUtils.convert("e:/abc.jpg", "GIF", "e:/abc_convert.gif");//测试OK


        // 4-彩色转黑白：
    //    ImageUtils.gray("e:/abc.jpg", "e:/abc_gray.jpg");//测试OK


        // 5-给图片添加文字水印：
        // 方法一：
      //  ImageUtils.pressText("Research Group of Diversity and Evolution of Ferns","F:\\database\\fern\\1\\species\\1/rna.jpg","F:\\database\\fern\\1\\species\\1/rna_1.jpg","宋体",Font.BOLD,Color.gray,180, 0, 1500, 0.5f);//测试OK
        // 方法二：
//        ImageUtils.pressText3("Research Group of Diversity and Evolution of Ferns", "F:\\database\\fern/rna_2.jpg","F:\\database\\fern/rna_2.jpg", "黑体", 36, Color.gray, 100, 0, 1500, 0.5f);//测试OK

        // 6-给图片添加图片水印：
    //    ImageUtils.pressImage("e:/abc2.jpg", "e:/abc.jpg","e:/abc_pressImage.jpg", 0, 0, 0.5f);//测试OK
    }



    public final static void scale(String srcImageFile, String result,
                                   int scale, boolean flag) {
        try {
            BufferedImage src = ImageIO.read(new File(srcImageFile)); // 读入文件
            int width = src.getWidth(); // 得到源图宽
            int height = src.getHeight(); // 得到源图长
            if (flag) {// 放大
                width = width * scale;
                height = height * scale;
            } else {// 缩小
                width = width / scale;
                height = height / scale;
            }
            Image image = src.getScaledInstance(width, height,
                    Image.SCALE_DEFAULT);
            BufferedImage tag = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = tag.getGraphics();
            g.drawImage(image, 0, 0, null); // 绘制缩小后的图
            g.dispose();
            ImageIO.write(tag, "JPEG", new File(result));// 输出到文件流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public final static void scale2(String srcImageFile, String result, int height, int width, boolean bb) {
        try {
            double ratio = 0.0; // 缩放比例
            File f = new File(srcImageFile);
            BufferedImage bi = ImageIO.read(f);
            Image itemp = bi.getScaledInstance(width, height, bi.SCALE_SMOOTH);
            // 计算比例
            if ((bi.getHeight() > height) || (bi.getWidth() > width)) {
                if (bi.getHeight() > bi.getWidth()) {
                    ratio = (new Integer(height)).doubleValue()
                            / bi.getHeight();
                } else {
                    ratio = (new Integer(width)).doubleValue() / bi.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(AffineTransform
                        .getScaleInstance(ratio, ratio), null);
                itemp = op.filter(bi, null);
            }
            if (bb) {//补白
                BufferedImage image = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, width, height);
                if (width == itemp.getWidth(null))
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                else
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                g.dispose();
                itemp = image;
            }
            ImageIO.write((BufferedImage) itemp, "JPEG", new File(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public final static void cut(String srcImageFile, String result,
                                 int x, int y, int width, int height) {
        try {
            // 读取源图像
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            int srcWidth = bi.getHeight(); // 源图宽度
            int srcHeight = bi.getWidth(); // 源图高度
            if (srcWidth > 0 && srcHeight > 0) {
                Image image = bi.getScaledInstance(srcWidth, srcHeight,
                        Image.SCALE_DEFAULT);
                // 四个参数分别为图像起点坐标和宽高
                // 即: CropImageFilter(int x,int y,int width,int height)
                ImageFilter cropFilter = new CropImageFilter(x, y, width, height);
                Image img = Toolkit.getDefaultToolkit().createImage(
                        new FilteredImageSource(image.getSource(),
                                cropFilter));
                BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics g = tag.getGraphics();
                g.drawImage(img, 0, 0, width, height, null); // 绘制切割后的图
                g.dispose();
                // 输出为文件
                ImageIO.write(tag, "JPEG", new File(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public final static void cut2(String srcImageFile, String descDir,
                                  int rows, int cols) {
        try {
            if(rows<=0||rows>20) rows = 2; // 切片行数
            if(cols<=0||cols>20) cols = 2; // 切片列数
            // 读取源图像
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            int srcWidth = bi.getHeight(); // 源图宽度
            int srcHeight = bi.getWidth(); // 源图高度
            if (srcWidth > 0 && srcHeight > 0) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                int destWidth = srcWidth; // 每张切片的宽度
                int destHeight = srcHeight; // 每张切片的高度
                // 计算切片的宽度和高度
                if (srcWidth % cols == 0) {
                    destWidth = srcWidth / cols;
                } else {
                    destWidth = (int) Math.floor(srcWidth / cols) + 1;
                }
                if (srcHeight % rows == 0) {
                    destHeight = srcHeight / rows;
                } else {
                    destHeight = (int) Math.floor(srcWidth / rows) + 1;
                }
                // 循环建立切片
                // 改进的想法:是否可用多线程加快切割速度
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 四个参数分别为图像起点坐标和宽高
                        // 即: CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight,
                                destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(
                                new FilteredImageSource(image.getSource(),
                                        cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth,
                                destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        g.drawImage(img, 0, 0, null); // 绘制缩小后的图
                        g.dispose();
                        // 输出为文件
                        ImageIO.write(tag, "JPEG", new File(descDir
                                + "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public final static void cut3(String srcImageFile, String descDir,
                                  int destWidth, int destHeight) {
        try {
            if(destWidth<=0) destWidth = 200; // 切片宽度
            if(destHeight<=0) destHeight = 150; // 切片高度
            // 读取源图像
            BufferedImage bi = ImageIO.read(new File(srcImageFile));
            int srcWidth = bi.getHeight(); // 源图宽度
            int srcHeight = bi.getWidth(); // 源图高度
            if (srcWidth > destWidth && srcHeight > destHeight) {
                Image img;
                ImageFilter cropFilter;
                Image image = bi.getScaledInstance(srcWidth, srcHeight, Image.SCALE_DEFAULT);
                int cols = 0; // 切片横向数量
                int rows = 0; // 切片纵向数量
                // 计算切片的横向和纵向数量
                if (srcWidth % destWidth == 0) {
                    cols = srcWidth / destWidth;
                } else {
                    cols = (int) Math.floor(srcWidth / destWidth) + 1;
                }
                if (srcHeight % destHeight == 0) {
                    rows = srcHeight / destHeight;
                } else {
                    rows = (int) Math.floor(srcHeight / destHeight) + 1;
                }
                // 循环建立切片
                // 改进的想法:是否可用多线程加快切割速度
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        // 四个参数分别为图像起点坐标和宽高
                        // 即: CropImageFilter(int x,int y,int width,int height)
                        cropFilter = new CropImageFilter(j * destWidth, i * destHeight,
                                destWidth, destHeight);
                        img = Toolkit.getDefaultToolkit().createImage(
                                new FilteredImageSource(image.getSource(),
                                        cropFilter));
                        BufferedImage tag = new BufferedImage(destWidth,
                                destHeight, BufferedImage.TYPE_INT_RGB);
                        Graphics g = tag.getGraphics();
                        g.drawImage(img, 0, 0, null); // 绘制缩小后的图
                        g.dispose();
                        // 输出为文件
                        ImageIO.write(tag, "JPEG", new File(descDir
                                + "_r" + i + "_c" + j + ".jpg"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public final static void convert(String srcImageFile, String formatName, String destImageFile) {
        try {
            File f = new File(srcImageFile);
            f.canRead();
            f.canWrite();
            BufferedImage src = ImageIO.read(f);
            ImageIO.write(src, formatName, new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public final static void gray(String srcImageFile, String destImageFile) {
        try {
            BufferedImage src = ImageIO.read(new File(srcImageFile));
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ColorConvertOp op = new ColorConvertOp(cs, null);
            src = op.filter(src, null);
            ImageIO.write(src, "JPEG", new File(destImageFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public final static void pressText(String pressText,
                                       String srcImageFile, String destImageFile, String fontName,
                                       int fontStyle, Color color, int fontSize,int x,
                                       int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    alpha));
            // 在指定坐标绘制水印文字
            g.drawString(pressText, (width - (getLength(pressText) * fontSize))
                    / 2 + x, (height - fontSize) / 2 + y);
            g.dispose();
            ImageIO.write((BufferedImage) image, "JPEG", new File(destImageFile));// 输出到文件流
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public final static void pressText2(String pressText, String srcImageFile,String destImageFile,
                                        String fontName, int fontStyle, Color color, int fontSize, int x,
                                        int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    alpha));
            // 在指定坐标绘制水印文字
            g.drawString(pressText, (width - (getLength(pressText) * fontSize))
                    / 2 + x, (height - fontSize) / 2 + y);
            g.dispose();
            ImageIO.write((BufferedImage) image, "JPEG", new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final static void pressText3(String pressText, String srcImageFile,String destImageFile,
                                        String fontName, int fontStyle, Color color, int fontSize, int x,
                                        int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int width = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, width, height, null);
            g.setColor(color);
            g.setFont(new Font(fontName, fontStyle, fontSize));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    alpha));
            // 在指定坐标绘制水印文字
            g.drawString(pressText, width - (getLength(pressText) * fontSize), (2*height - fontSize) / 2 );
            g.dispose();
            ImageIO.write((BufferedImage) image, "JPEG", new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public final static void pressImage(String pressImg, String srcImageFile,String destImageFile,
                                        int x, int y, float alpha) {
        try {
            File img = new File(srcImageFile);
            Image src = ImageIO.read(img);
            int wideth = src.getWidth(null);
            int height = src.getHeight(null);
            BufferedImage image = new BufferedImage(wideth, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.drawImage(src, 0, 0, wideth, height, null);
            // 水印文件
            Image src_biao = ImageIO.read(new File(pressImg));
            int wideth_biao = src_biao.getWidth(null);
            int height_biao = src_biao.getHeight(null);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP,
                    alpha));
            g.drawImage(src_biao, (wideth - wideth_biao) / 2,
                    (height - height_biao) / 2, wideth_biao, height_biao, null);
            // 水印文件结束
            g.dispose();
            ImageIO.write((BufferedImage) image,  "JPEG", new File(destImageFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public final static int getLength(String text) {
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            if (new String(text.charAt(i) + "").getBytes().length > 1) {
                length += 2;
            } else {
                length += 1;
            }
        }
        return length / 2;
    }
}