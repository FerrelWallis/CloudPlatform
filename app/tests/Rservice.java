package tests;

//import org.rosuda.REngine.REXP;
//import org.rosuda.REngine.REXPMismatchException;
//import org.rosuda.REngine.Rserve.RConnection;
//import org.rosuda.REngine.Rserve.RserveException;
//
//import java.io.*;

public class Rservice {




    //java调R
//    public static void main(String[] args) {
//
//        try {
//            RConnection rc = new RConnection();
//            String vetor="c(1,2,3,4)";
//            rc.eval("meanVal<-mean("+vetor+")");
//
//            //System.out.println("the mean of given vector is="+mean);
//            double mean=rc.eval("meanVal").asDouble();
//            System.out.println("the mean of given vector is="+mean);
//            rc.eval("source('E:/R/Add.R')");
//            int num1=20;
//            int num2=10;
//            int sum=rc.eval("myAdd("+num1+","+num2+")").asInteger();
//            System.out.println("the sum="+sum);
//
//            int minus=rc.eval("minus("+num1+","+num2+")").asInteger();
//            System.out.println("the minus="+minus);
//
//            rc.close();
//        }catch (RuntimeException | RserveException | REXPMismatchException e){
//            e.printStackTrace();
//        }
//
//    }


    //调用perl
//        public static void main(String[] args) throws IOException {
//
//            StringBuffer resultStringBuffer = new StringBuffer();
//
//            String lineToRead = "";
//            int exitValue = 0;
//
//            try {
//
//                Process proc = Runtime.getRuntime().exec("perl F:\\CloudPlatform\\R\\pca\\plot-pca.pl");
//                InputStream inputStream = proc.getInputStream();
//                BufferedReader bufferedRreader = new BufferedReader(new InputStreamReader(inputStream));
//
//                // save first line
//                if ((lineToRead = bufferedRreader.readLine()) != null) {
//                    resultStringBuffer.append(lineToRead);
//                }
//
//                // save next lines
//                while ((lineToRead = bufferedRreader.readLine()) != null) {
//                    resultStringBuffer.append("\r\n");
//                    resultStringBuffer.append(lineToRead);
//                }
//
//                // Always reading STDOUT first, then STDERR, exitValue last
//                proc.waitFor(); // wait for reading STDOUT and STDERR over
//                exitValue = proc.exitValue();
//            } catch (Exception ex) {
//                resultStringBuffer = new StringBuffer("");
//                exitValue = 2;
//            }
//
//            System.out.println("exit:" + exitValue);
//
//            System.out.println(resultStringBuffer.toString());
//
//        }



}
