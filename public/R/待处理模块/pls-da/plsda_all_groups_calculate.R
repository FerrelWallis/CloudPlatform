# Title     : TODO
# Objective : TODO
# Created by: Administrator
# Created on: 2019/6/18

# library(MetaboAnalystR)
library(ropls)
library(plyr)
library(magrittr)
library(optparse)
library(tidyverse)

createWhenNoExist <- function(f) {
  !dir.exists(f) && dir.create(f)
}

option_list <- list(
  make_option("--i", default = "AllMet_Raw.txt", type = "character", help = "metabolite data file"),
  make_option("--g", default = "group.txt", type = "character", help = "sample group file"),
  make_option("--sc", default = "sample_color.txt", type = "character", help = "sample color file")
)
opt <- parse_args(OptionParser(option_list = option_list))

sampleInfo <- read_tsv(opt$g) %>%
  rename(SampleID = Sample) %>%
  select(c("SampleID", "ClassNote")) %>%
  mutate(ClassNote = as.character(ClassNote))

sampleIds <- sampleInfo %>%
  .$SampleID

data <-  read_tsv(opt$i) %>%
  select(c("Metabolite", sampleIds)) %>%
  gather("SampleID", "Value", -Metabolite) %>%
  spread(Metabolite, "Value") %>%
  inner_join(sampleInfo, by = c("SampleID")) %>%
  column_to_rownames("SampleID")

head(data)

x <- data %>% select(-c("ClassNote"))
y <- data$ClassNote %>%
  as.factor() #%>%
# as.numeric()

comp <- length(unique(y))
if (comp < 3) {
  comp <- 3
}

print(y)
crossValI <- min(nrow(x), 7)
crossValI
plsdaRs <- opls(x, y, predI = comp, plotL = F, crossvalI = crossValI)

summary(plsdaRs)

modelDf <- plsdaRs@modelDF %>%
  rownames_to_column("pcName") %>%
  slice(1:5) %>%
  mutate(pcName = str_replace(pcName, "p", "C")) %>%
  rename(` ` = pcName) %>%
  select(-c("Signif.", "Iter."))

print("in")
modelDf
print(plsdaRs@scoreMN)

plotData <- plsdaRs@scoreMN %>%
  as.data.frame() %>%
  rownames_to_column("SampleID") %>%
  select("SampleID", num_range("p", 1:5)) %>%
  rename_at(vars(-c("SampleID")), function(x) {
    str_replace(x, "p", "C")
  }) %>%
  column_to_rownames("SampleID")

parent <- "./"
createWhenNoExist(parent)
#参数
modelFileName <- paste0(parent, "/PLSDA_R2X_R2Y_Q2.csv")
#得分
dataFileName <- paste0(parent, "/PLSDA_Score.csv")

write.csv(plotData, dataFileName)
write_csv(modelDf, modelFileName)

vipVn = getVipVn(plsdaRs)
vipData <- data.frame(VIP = vipVn) %>%
  rownames_to_column("Metabolite")
inputData <- read_tsv(opt$i)
classData <- inputData %>%
  select(c("Metabolite"))
allData <- classData %>%
  inner_join(vipData, by = c("Metabolite")) %>%
  arrange(desc(VIP))

#vip
allFileName <- paste0(parent, "/PLSDA_VIP.csv")
write.csv(allData, allFileName, row.names = FALSE)




