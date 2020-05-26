library(ropls)
library(tibble)
library(magrittr)
library(dplyr)
library(stringr)
library(readr)
library(ggplot2)
library(ggrepel)
library(tidyr)
zscal = TRUE
group = FALSE

data <-read.table("__matrix__", header=T, quote="",com='', sep="	", row.names=1, check.names=F )
tmpDF = data
data = t(data)

if( zscal){
    data = scale(data)
}

crossValI <- min(nrow(data), 7)
pcaRs <- opls(data, plotL = F, crossvalI = crossValI)
df <- pcaRs@modelDF %>% as_tibble()
if (nrow(df) < 5) {
    pcaRs <- opls(data, plotL = F, crossvalI = crossValI, predI = 5)
}

parameterData <- pcaRs@modelDF %>%
    select(c(1 : 2)) %>%
    set_colnames(c("R2", "Cumulative R2")) %>%
    mutate(pcName = paste0("PC", 1 : n())) %>%
    filter(pcName %in% str_c("PC", 1 : 5)) %>%
    column_to_rownames("pcName") %>%
    t()
write.table(parameterData, "PCA_R2.txt", quote = FALSE, sep = "	")
write.csv(parameterData,  "PCA_R2.csv", quote = FALSE)

pcData <- pcaRs@scoreMN %>%
    as.data.frame() %>%
    rownames_to_column("SampleID") %>%
    select("SampleID", num_range("p", 1 : 5)) %>%
    rename_at(vars(- c("SampleID")), function(x){
        str_replace(x, "p", "PC")
    }) %>%
    column_to_rownames("SampleID")
write.table(pcData, "PCA_Score.txt", quote = FALSE, sep = "	")
write.csv(pcData, "PCA_Score.csv", quote = FALSE)


pcDataFileName <- "PCA_Score.csv"
pcData <- read_csv(pcDataFileName) %>%
    rename(SampleID = X1)
parameterFileName <- "PCA_R2.csv"
parameterData <- read.csv(parameterFileName, header = T, stringsAsFactors = F, comment.char = "")

pNames <- pcData %>%
    column_to_rownames("SampleID") %>%
    colnames()
cn <- combn(pNames, 2)
if (group ) {
    sampleInfo <- read_csv("__group__") %>%
        select(c("SampleID", "ClassNote")) %>%
        mutate(ClassNote = factor(ClassNote, levels = unique(ClassNote)))
    for (i in 1 : ncol(cn)) {
        row <- cn[, i]
        p1Name <- row[1]
        p2Name <- row[2]
        p1Index <- str_replace(p1Name, "PC", "")
        p2Index <- str_replace(p2Name, "PC", "")
        pc12 <- pcData %>%
            select(c("SampleID", p1Name, p2Name)) %>%
            set_colnames(c("SampleID", "p1", "p2")) %>%
            left_join(sampleInfo, by = c("SampleID"))
        
        impoPc1 <- parameterData[1, str_replace(p1Name, "p", "PC")]
        impoPc1 <- round(impoPc1 * 100, 2)
        impoPc2 <- parameterData[1, str_replace(p2Name, "p", "PC")]
        impoPc2 <- round(impoPc2 * 100, 2)
        p <- ggplot(pc12, mapping = aes(x = p1, y = p2, label = SampleID, color = ClassNote)) +
            xlab(paste0("PC", p1Index, "(", impoPc1, "%)", sep = "")) +
            ylab(paste0("PC", p2Index, "(", impoPc2, "%)", sep = "")) +
            theme_bw(base_size = 8.8, base_family = "Times") +
            theme(axis.text.x = element_text(size = 9, vjust = 0.5),
            axis.text.y = element_text(size = 8.8), legend.position = 'right',
            axis.title.y = element_text(size = 11), legend.margin = margin(t = 0.3, b = 0.1, unit = 'cm'),
            legend.text = element_text(size = 6), axis.title.x = element_text(size = 11),
            panel.grid.major = element_blank(), panel.grid.minor = element_blank(),
            ) +
             #0 line
            geom_vline(aes(xintercept = 0), colour = "#BEBEBE", linetype = "solid") +
            geom_hline(aes(yintercept = 0), colour = "#BEBEBE", linetype = "solid") +
            #point
            geom_point(aes(colour = factor(ClassNote)), size = 3, stroke = 0) +
            stat_ellipse(aes(fill = ClassNote), colour = NA, size = 0.3, level = 0.95, type = "norm",
            geom = "polygon", alpha = 0.2, show.legend = F) +
            geom_text_repel(segment.size=0.2,size = 2, family = "Times")
        fileName <- paste0("PC", p1Index, p2Index, "_Score_2D_Label.png")
        ggsave(fileName, p, width = 5, height = 4)
    }
    for (i in 1 : ncol(cn)) {
        row <- cn[, i]
        p1Name <- row[1]
        p2Name <- row[2]
        p1Index <- str_replace(p1Name, "PC", "")
        p2Index <- str_replace(p2Name, "PC", "")
        pc12 <- pcData %>%
            select(c("SampleID", p1Name, p2Name)) %>%
            set_colnames(c("SampleID", "p1", "p2")) %>%
            left_join(sampleInfo, by = c("SampleID"))
        
        impoPc1 <- parameterData[1, str_replace(p1Name, "p", "PC")]
        impoPc1 <- round(impoPc1 * 100, 2)
        impoPc2 <- parameterData[1, str_replace(p2Name, "p", "PC")]
        impoPc2 <- round(impoPc2 * 100, 2)
        p <- ggplot(pc12, mapping = aes(x = p1, y = p2, label = SampleID, color = ClassNote)) +
            xlab(paste0("PC", p1Index, "(", impoPc1, "%)", sep = "")) +
            ylab(paste0("PC", p2Index, "(", impoPc2, "%)", sep = "")) +
            theme_bw(base_size = 8.8, base_family = "Times") +
            theme(axis.text.x = element_text(size = 9, vjust = 0.5),
            axis.text.y = element_text(size = 8.8), legend.position = 'right',
            axis.title.y = element_text(size = 11), legend.margin = margin(t = 0.3, b = 0.1, unit = 'cm'),
            legend.text = element_text(size = 6), axis.title.x = element_text(size = 11),
            panel.grid.major = element_blank(), panel.grid.minor = element_blank(),
            ) +
             #0 line
            geom_vline(aes(xintercept = 0), colour = "#BEBEBE", linetype = "solid") +
            geom_hline(aes(yintercept = 0), colour = "#BEBEBE", linetype = "solid") +
            #point
            geom_point(aes(colour = factor(ClassNote)), size = 3, stroke = 0) +
            stat_ellipse(aes(fill = ClassNote), colour = NA, size = 0.3, level = 0.95, type = "norm",
            geom = "polygon", alpha = 0.2, show.legend = F) 
        fileName <- paste0("PC", p1Index, p2Index, "_Score_2D.png")
        ggsave(fileName, p, width = 5, height = 4)
    }     
}else{
    for (i in 1 : ncol(cn)) {
        row <- cn[, i]
        p1Name <- row[1]
        p2Name <- row[2]
        p1Index <- str_replace(p1Name, "PC", "")
        p2Index <- str_replace(p2Name, "PC", "")
        pc12 <- pcData %>%
            select(c("SampleID", p1Name, p2Name)) %>%
            set_colnames(c("SampleID", "p1", "p2")) 
        
        impoPc1 <- parameterData[1, str_replace(p1Name, "p", "PC")]
        impoPc1 <- round(impoPc1 * 100, 2)
        impoPc2 <- parameterData[1, str_replace(p2Name, "p", "PC")]
        impoPc2 <- round(impoPc2 * 100, 2)
        p <- ggplot(pc12, mapping = aes(x = p1, y = p2, label = SampleID)) +
            xlab(paste0("PC", p1Index, "(", impoPc1, "%)", sep = "")) +
            ylab(paste0("PC", p2Index, "(", impoPc2, "%)", sep = "")) +
            theme_bw(base_size = 8.8, base_family = "Times") +
            theme(axis.text.x = element_text(size = 9, vjust = 0.5),
            axis.text.y = element_text(size = 8.8), legend.position = 'right',
            axis.title.y = element_text(size = 11), legend.margin = margin(t = 0.3, b = 0.1, unit = 'cm'),
            legend.text = element_text(size = 6), axis.title.x = element_text(size = 11),
            panel.grid.major = element_blank(), panel.grid.minor = element_blank(),

            ) +
            #0 line
            geom_vline(aes(xintercept = 0), colour = "#BEBEBE", linetype = "solid") +
            geom_hline(aes(yintercept = 0), colour = "#BEBEBE", linetype = "solid") +
            #point
            geom_point(colour="blue", size = 3, stroke = 0) +
            geom_text_repel(segment.size=0.2,size = 2, family = "Times")
        fileName <- paste0("PC", p1Index, p2Index, "_Score_2D_Label.png")
        ggsave(fileName, p, width = 5, height = 4)
    }
}

lineData <- parameterData %>%
    column_to_rownames("X") %>%
    t() %>%
    as.data.frame() %>%
    select(c("R2")) %>%
    rename(value = R2) %>%
    mutate(value=value*100) %>%
    mutate(sum = cumsum(value)) %>%
    rownames_to_column("index")

plotData <- lineData %>%
    gather("Class", "value", - index)

labelData <- plotData %>%
    select(- c("Class")) %>%
    as.data.frame() %>%
    unique() %>%
    mutate(label = paste0(value, "%"))

p <- ggplot(plotData, aes(x = index, y = value)) +
    xlab("PC index") +
    ylab("Variance Explained (%)") +
    ggtitle("Scree plot") +
    theme_bw(base_size = 8.8, base_family = "Times") +
    theme(axis.text.x = element_text(size = 9, vjust = 0.5),
    axis.text.y = element_text(size = 8.8), legend.position = 'none',
    axis.title.y = element_text(size = 11), legend.margin = margin(t = 0.3, b = 0.1, unit = 'cm'),
    legend.text = element_text(size = 6), axis.title.x = element_text(size = 11),
    panel.grid.major.y = element_blank(), panel.grid.minor.x = element_blank(), panel.grid.minor.y = element_blank(),
    plot.title = element_text(hjust = 0.5, size = 13)
    ) +
    geom_line(aes(x = index, y = value, group = 1), lineData, size = 0.5, linetype = 1, color = "blue") +
    geom_line(aes(x = index, y = sum, group = 1), lineData, size = 0.5, linetype = 1, color = "green") +
    geom_point(size = 2, pch = 21, color = "red") +
    geom_text_repel(segment.size=0.2,data = labelData, aes(x = index, y = value, label = label), color = "black", size = 2, family = "Times")
ggsave("PCA_Screeplot.png", p, width = 5, height = 4)


