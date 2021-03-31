#!usr/bin/perl -w

use strict;

die "usage:perl $0 <GO.enrich.xls> <Go.enrich.bar.pdf> <num> \n" unless @ARGV == 3;
open A,$ARGV[0];
<A>;
my $num=0;
while(<A>){
	chomp; $num++;
}
close A;
my $abc=$ARGV[2];
if($num < $ARGV[2]){
	$abc=$num;	
}
open O,">cmd.r";
print O "mycol = c(\"#458B74\", \"#AB82FF\", \"#FFA54F\")
dat<-read.table(\"$ARGV[0]\",sep=\"\\t\",header=T)
par(mar=c(5,2,1,2)+0.1)
pdf(\"$ARGV[1]\",width=12,height=10)
part<-dat[1:$abc,]
x<-as.matrix(data.frame(x=part\$namespace,y=log(part\$p_uncorrected,base=10)))
rownames(x) <-part[,3]
x <- x[order(x[,1]),]
g <-table(x[,1])
bcol <-c(rep(1,g[1]),rep(2,g[2]),rep(3,g[3]))
lpx <--as.numeric(x[,2])
ylim1<-round(max(lpx))+1
ylim0<-2*ylim1/5
xa <- barplot(lpx,beside=TRUE,space=0.1,col=mycol[bcol],axes=FALSE,ylim=c(-ylim0,ylim1),border=NA,plot=FALSE)
xlim1 <-max(xa)*1.02
xlim0 <--xlim1*0.15
xa <- barplot(lpx,beside=TRUE,space=0.1,col=mycol[bcol],axes=FALSE,ylim=c(-ylim0,ylim1),xlim=c(xlim0,xlim1*1.1),border=NA)
text(xa,y=-.1,srt=50,adj=1,labels=rownames(x),xpd=T,col=mycol[bcol],cex=0.5)
axis(side=2, at=0:ylim1,las=1,cex=0.8,pos=c(0,0))
axis(side=2, at=ylim1/2,labels =\"-log10(Pvalue)\" ,las=3,cex.axis=0.9,pos=c(0,0),mgp=c(10,4,0),font.axis=3)
dev.off()\n";
close O;
`R --no-save < cmd.r`;
#`rm cmd.r`;
