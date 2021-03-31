pacman::p_load(ropls, optparse, tidyverse, magrittr, dplyr, lazyopt)

arg <- c("-i", "F:/CloudPlatform/R/pls-da/PCA_table.txt",
         "-g", "F:/CloudPlatform/R/pls-da/PCA_group.txt",
         "-o", "F:/CloudPlatform/R/pls-da"
)

#是否在p值的正态近似中应用连续性校正
opt <- matrix(c("input", "i", 2, "character", "table file", "",
                "group", "g", 1, "character", "sample group file", "",
                "outpath", "o", 2, "character", "output path", ""
), byrow = TRUE, ncol = 6) %>% lazyopt()

input <- read.table(opt$input, sep="\t", head=T, check.names=F) %>% set_rownames(.[,1]) %>% .[,-1]
group <- read.table(opt$group, sep="\t", head=F, check.names=F) %>%  set_colnames(c("SampleID", "ClassNote"))

sampleInfo <- group %>% mutate(ClassNote = as.character(ClassNote))
sampleIds <- sampleInfo$SampleID

data <- input %>% t() %>% data.frame() %>% 
  rownames_to_column("SampleID") %>% 
  inner_join(sampleInfo, by = c("SampleID")) %>% 
  column_to_rownames("SampleID")

x <- data %>% select(-c("ClassNote"))
y <- data$ClassNote %>% as.factor()

comp <- length(levels(y))
if (comp < 3) { 
  comp <- 3
}

crossValI <- min(nrow(x), 7)
plsdaRs <- opls(x, y, predI = comp, plotL = F, crossvalI = crossValI)

modelDf <- plsdaRs@modelDF %>%
  rownames_to_column("pcName") %>%
  column_to_rownames("pcName") %>%
  select(-c("Signif.", "Iter."))

plotData <- plsdaRs@scoreMN %>% as.data.frame()

#得分
write.table(plotData, paste0(opt$outpath, "/PLSDA_score.xls"), sep="\t", col.names = NA, quote = F)
#percent

#参数
write.table(modelDf, paste0(opt$outpath, "/PLSDA_R2X_R2Y_Q2.xls"), sep="\t", col.names = NA, quote = F)

#作图sdev
modelDf$R2X <- modelDf %>% data.frame() %>% select(c("R2X")) %>% lapply(., function(x) paste0(x*100,"%")) %>% unlist()
write.table(modelDf %>% select(c("R2X")) %>% set_colnames(NULL), paste0(opt$outpath, "/PLSDA_sdev.xls"), sep="\t", col.names = NA, quote = F)

allData <- data.frame(VIP = getVipVn(plsdaRs)) %>% arrange(desc(VIP))

#vip变量投影重要度
write.table(allData, paste0(opt$outpath, "/PLSDA_VIP.xls"), sep="\t", col.names = NA, quote = F)

