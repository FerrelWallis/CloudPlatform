pacman::p_load(lazyopt, ggpmisc, dplyr, magrittr, ggplot2)

args <- c("-i","F:/CloudPlatform/R/linearReg/XY.txt",
          "-o","F:/CloudPlatform/R/linearReg")

LM<-matrix(c("inputfile", "i",2, "character","inputfile path","",
             "outputfile","o",2,"character","outputfile path","",
             "main_title","mt",1,"character","main title","XY Linear Regression Plot",
             "main_size","ms",1,"numeric","main title size","20",
             "main_color","mc",1,"character","main title color","black",
             "x-axistitle","xt",1,"character","x-axis title","X",
             "y-axistitle","yt",1,"character","y-axis title","Y",
             "axis_color","ac",1,"character","axis color","black",
             "axis_textsize","ats",1,"numeric","axis text size","20",
             "axis_textcolor","atc",1,"character","axis text color","black",
             "axis_lablesize","als",1,"numeric","axis_lablesize","15",
             "axis_lablecolor","alc",1,"character","axis_lablecolor","black",
             "point_color","pc",1,"character","point color","#6699ff",
             "point_size","pz",1,"numeric","point size","4",
             "formula_color","fc",1,"character","formula color","black",
             "formula_size","fz",1,"numeric","formula size","5",
             "smooth_color","sc",1,"character","smooth color","red",
             "image_height","is",1,"numeric","The height of the picture","10",
             "image_width","iw",1,"numeric","the width of the picture","10",
             "resolution", "dpi", 1, "numeric", "Set the resolution to allow 72,96,300 or 600", "300",
             "imageformat", "if", 1, "character", "pdf,tiff,png", "pdf",
             "imageName", "in", 1, "character", "picture name", "XY"
), byrow = TRUE, ncol = 6)%>%lazyopt()

data=read.table(LM$inputfile,header=TRUE,check.names = FALSE)
X<-data[,1];Y=data[,2]
formula1=Y~X
XY<-ggplot(data,aes(x=X,y=Y))+
        geom_point(color=LM$point_color,size=LM$point_size)+
        geom_smooth(method='lm',se=FALSE,color=LM$smooth_color)+
        stat_poly_eq(aes(label = paste(..eq.label.., sep = "~~~")), 
                      label.x.npc = "right", label.y.npc = 0.15,
                      eq.with.lhs = "italic(hat(y))~`=`~",
                      eq.x.rhs = "~italic(x)",
                      formula = formula1, parse = TRUE, size = LM$formula_size,face="bold",color =LM$formula_color)+
        theme_minimal()+
        labs(x=LM$`x-axistitle`,y=LM$`y-axistitle`,title=LM$main_title,caption ='Linbo MicroClass')+
        theme(plot.title = element_text(hjust=0.5, size=LM$main_size, face='bold',color=LM$main_color),
              plot.caption = element_text(size=10,face="bold",color="black"),
              axis.title = element_text(size=LM$axis_textsize,color=LM$axis_textcolor,face="bold"),
              panel.background = element_rect(fill="white",color="white"),
              panel.grid = element_line(color="white"),
              axis.text = element_text(size=LM$axis_lablesize,color =LM$axis_lablecolor,face="bold"),
              axis.line=element_line(size=1,color=LM$axis_color))

paste0(LM$outputfile, "/", LM$imageName, ".", LM$imageformat) %>%
  ggsave(XY,width = LM$image_width,height = LM$image_height, dpi = LM$resolution)   

     
