# Title     : TODO
# Objective : TODO
# Created by: xueyj
# Created on: 2020/10/14 0014
pacman::p_load(dplyr, magrittr, lazyopt)

my.merage <- function(leftdata, rightdata, byx, byy, type) {
  result <- NULL
  if (type == "in") { result <- merge(leftdata, rightdata, by.x = byx, by.y = byy, all = FALSE) }
  if (type == "all") { result <- merge(leftdata, rightdata, by.x = byx, by.y = byy, all = TRUE) }
  if (type == "left") { result <- merge(leftdata, rightdata, by.x = byx, by.y = byy, all.x = TRUE) }
  return(result)
}

opt <- matrix(c("inputfiles", "i", 2, "character", "inputfile path,split by ';'", "",
                "outputfile", "o", 2, "character", "output file path", "",
                "by", "b", 2, "character", "Set those  columns for comparison,split by ':'", "",
                "connecttype", "ct", 1, "character", "three type [left,in,all]", "all",
                "set_na", "sn", 1, "character", "Replace missing value", ""
), ncol = 6, byrow = TRUE) %>% lazyopt()
lines <- opt %$% lazyopt::fenge(by) %>% as.numeric()
filepaths <- opt %$% lazyopt::fenge(inputfiles, ";")
leftdata <- read.delim(filepaths[1], check.names = FALSE, header = TRUE)
a <- (leftdata %>% colnames())[lines[1]]
for (i in 2:length(filepaths)) {
  rightdata <- read.delim(filepaths[i], check.names = FALSE, header = TRUE)
  b <- (rightdata %>% colnames())[lines[i]]
  leftdata <- my.merage(leftdata, rightdata, a, b, opt$connecttype)
}
leftdata[is.na(leftdata)] <- ""
leftdata[leftdata == ""] <- opt$set_na
write.table(leftdata, file = opt$outputfile, sep = "\t", row.names = FALSE)


