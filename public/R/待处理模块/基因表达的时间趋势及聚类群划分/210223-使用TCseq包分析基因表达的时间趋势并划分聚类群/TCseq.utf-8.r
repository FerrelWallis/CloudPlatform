#使用 Bioconductor 安装 TCseq 包
#install.packages('BiocManager')
#BiocManager::install('TCseq')

#加载 TCseq 包
library(TCseq)

#以本示例的蛋白表达矩阵为例，行为基因或蛋白名称，列为时间样本
#每一列是独立的时间单位，按时间顺序提前排列好，若存在生物学重复（即一个时间点对应多个样本时）建议提前取均值
protein <- read.delim('protein_exp.txt', row.names = 1, check.names = FALSE)
protein <- as.matrix(protein)

#聚类，详情 ?timeclust
#algo 用于指定聚类方法，例如基于 fuzzy c-means 的算法进行聚类，此时需要设定随机数种子，以避免再次运行时获得不同的结果
#k 用于指定期望获得的聚类群数量，例如这里预设为 10
#standardize 用于 z-score 标准化变量
set.seed(123)
cluster_num <- 10
tcseq_cluster <- timeclust(protein, algo = 'cm', k = cluster_num, standardize = TRUE)

#作图，详情 ?timeclustplot
#颜色、线宽、坐标轴、字体等细节可以在函数中调整，具体参数详见函数帮助
p <- timeclustplot(tcseq_cluster, value = 'z-score', cols = 3, 
    axis.line.size = 0.6, axis.title.size = 8, axis.text.size = 8, 
    title.size = 8, legend.title.size = 8, legend.text.size = 8)

#上述获得了 10 组聚类群
#如果绘制单个的聚类群，例如 claster 2，直接在作图结果中输入下标选取
p[2]


#查看每个蛋白所属的聚类群，展示前几个为例
head(tcseq_cluster@cluster)

#统计每个聚类群中各自包含的蛋白数量
table(tcseq_cluster@cluster)

#上述聚类过程中，通过计算 membership 值判断蛋白质所属的聚类群，以最大的 membership 值为准
#查看本次计算的各蛋白的 membership 值，展示前几个为例
head(tcseq_cluster@membership)

#上述聚类过程中，我们在聚类函数 timeclust() 中指定了对蛋白表达值的 z-score 标准化
#如果您想查看标准化后的表达值（也即绘制曲线图用的那个值，而非原始的蛋白表达值），展示前几个为例
head(tcseq_cluster@data)

#啥？您若问原始的蛋白表达值在哪里看？一开始的输入数据就是啊......

#最后，提取所有蛋白所属的聚类群，并和它们的原始表达值整合在一起
protein_cluster <- tcseq_cluster@cluster
protein_cluster <- cbind(protein[names(protein_cluster), ], protein_cluster)
head(protein_cluster)
write.table(protein_cluster, 'protein_cluster.txt', sep = '\t', col.names = NA, quote = FALSE)

