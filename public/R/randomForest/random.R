pacman::p_load(lazyopt, ggrepel, ropls, pROC, egg, randomForest, Boruta, magrittr, optparse, gbm, tidyverse)

arg <- c("-i", "F:/CloudPlatform/R/randomForest/bar_table.txt",
         "-g", "F:/CloudPlatform/R/randomForest/bar_group.txt",
         "-o", "F:/CloudPlatform/R/randomForest"
)

opt <- matrix(c("inputpath", "i", 2, "character", "table file", "",
                "grouppath", "g", 2, "character", "sample group file", "",
                "outpath", "o", 2, "character", "output path", ""
), byrow = TRUE, ncol = 6) %>% lazyopt()


sampleInfo <- read.table(opt$grouppath, sep="\t", head=T, check.names=F) %>% set_colnames(c("SampleID", "ClassNote")) %>% data.frame()
sampleInfo$SampleID <- as.character(sampleInfo$SampleID)


originalData <- read.table(opt$inputpath, sep="\t", head=T, check.names=F)
colnames(originalData)[1] <- "Metabolite"

data <- originalData %>% set_rownames(.[,1]) %>% .[,-1] %>% t() %>% data.frame() %>% rownames_to_column(., "SampleID") %>%
  inner_join(sampleInfo, by = c("SampleID")) %>% mutate(ClassNote = factor(ClassNote, levels = unique(ClassNote))) %>% column_to_rownames("SampleID")

x <- data %>% select(-c("ClassNote"))
y <- data$ClassNote
rfRs <- randomForest(x, y, importance = T, proximity = TRUE, ntree = 500, mtry = 7)



varImp <- rfRs$importance
varImpDf <- varImp %>% as.data.frame() %>% rownames_to_column("Metabolite") %>%
  select(c("Metabolite", "MeanDecreaseGini")) %>% rename(VarImp = MeanDecreaseGini) %>%
  arrange(desc(VarImp)) %>%.[order(.[,2], decreasing = T),]

write.table(varImpDf, paste0(opt$outpath, "/VarImp.xls"), sep="\t", row.names = F, quote = F)

