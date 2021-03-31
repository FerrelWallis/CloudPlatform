pacman::p_load(lazyopt, dplyr, magrittr, ggplot2)

args <- c("-i","http://210.22.121.250:35588/assets/files/CCA_otu.txt",
          "-i2","http://210.22.121.250:35588/assets/files/CCA_env.txt",
          "-cn","7",
          "-o","F:/CloudPlatform/R/multiLinear")

opt<-matrix(c("inputfile", "i1", 1, "character", "inputfile path", " ",
          "inputfile2", "i2", 1,"character", "inputfile path2", " ",
          "colnames","cn",2,"numeric","the selected column"," ",
          "outputfile","o",1,"character","outputfile path"," "), 
  byrow = TRUE, ncol = 6)%>%lazyopt()

OTU<-read.table(opt$inputfile,header=TRUE,check.names=FALSE)
A<-(read.table(opt$inputfile2,header=TRUE,check.names=FALSE))
rownames(A)=A[,1]
ENV=A[,-1]
rownames(OTU)<-OTU[,1]
NOTU=t(OTU[,-1])
NOTU[rownames(ENV),]
M=cbind(ENV,NOTU)
colnames(M)[opt$colnames] <- 'Target'
LM=lm(Target~.-Target,M)

write.table(LM$coefficients, paste0(opt$outputfile, "/multi_lin_reg.xls"), sep="\t", col.names = NA, quote=FALSE)
