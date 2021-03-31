pacman::p_load(lazyopt, dplyr, magrittr, sqldf, tidyverse)

arg <- c("-i", "F:/CloudPlatform/R/lefse2.0/lefse_LDA.xls",
         "-o", "F:/CloudPlatform/R/lefse2.0/a-t.xls")

opt <- matrix(
  c("inputfile", "i", 2, "character", "inputfile path", "",
    "outputfile", "o", 2, "character", "outputfile path", ""),
  ncol = 6, byrow = TRUE) %>% lazyopt(arg = arg)


df <- read.delim(opt$inputfile, check.names = FALSE, header = FALSE)

  
#先进行筛选
checkname <- function(x) {
  temp <- unlist(strsplit(as.character(x),"\\."))  
  return(temp[length(temp)])
}

df <- df$V1 %>% 
  sapply(function(x) checkname(x)) %>%
  data.frame() %>%
  cbind(df,.) 


colnames(df)<-c("ida1","ida2","ida3","ida4","ida5","ida6")

df %<>% filter(ida6 != "Other", ida6 != "norank", ida6 != "uncultured")
df <- df[,-c(6)]
write.table(df, file =  opt$inputfile, sep = "\t", col.names = FALSE, row.names = FALSE, na = "", quote = FALSE)

df %<>% subset(ida3!="")
write.table(df, file = opt$outputfile, sep = "\t", col.names = FALSE, row.names = FALSE, quote = FALSE)