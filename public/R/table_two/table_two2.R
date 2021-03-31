pacman::p_load(dplyr, magrittr, lazyopt)
opt <- matrix(c("firstinputfile", "i1", 2, "character", "first inputfile path", "",
                "secondinputfile", "i2", 2, "character", "second inputfilepath", "",
                "outputfile", "o", 2, "character", "output file path", "",
                "by", "b", 2, "character", "Set those two columns for comparison", "1:1",
                "connecttype", "ct", 1, "character", "four types [left,right,in,all]", "all",
                "set_na", "sn", 1, "character", "Replace missing value", ""
), ncol = 6, byrow = TRUE) %>% lazyopt()
#left 左连接   right 右连接   in  内连接   all 全连接
firstData <- read.delim(opt$firstinputfile, check.names = FALSE, header = TRUE)
secondData <- read.delim(opt$secondinputfile, check.names = FALSE, header = TRUE)
lines <- opt %$% lazyopt::fenge(by) %>% as.numeric()
a <- (firstData %>% colnames())[lines[1]]
b <- (secondData %>% colnames())[lines[2]]
type <- opt %$% match.arg(connecttype, c("right", "left", "in", "all"))
if (type == "in") { result <- merge(firstData, secondData, by.x = a, by.y = b, all = FALSE) }
if (type == "all") { result <- merge(firstData, secondData, by.x = a, by.y = b, all = TRUE) }
if (type == "left") { result <- merge(firstData, secondData, by.x = a, by.y = b, all.x = TRUE) }
if (type == "right") { result <- merge(firstData, secondData, by.x = a, by.y = b, all.y = TRUE) }
result[is.na(result)] <- ""
result[result == ""] <- opt$set_na
write.table(result, file = opt$outputfile, sep = "\t", row.names = FALSE)